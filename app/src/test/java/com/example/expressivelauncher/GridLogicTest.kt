package com.example.expressivelauncher

import com.example.expressivelauncher.data.AppFilter
import com.example.expressivelauncher.data.GridLogic
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridLogicTest {
    private fun app(id: String, page: Int, x: Int, y: Int) =
        HomeItem(id, ItemType.APP, page, x, y, appKey = "pkg.$id/Main#0")

    private fun widget(id: String, page: Int, x: Int, y: Int, w: Int, h: Int) =
        HomeItem(id, ItemType.WIDGET, page, x, y, w, h, widgetId = 1)

    @Test
    fun isFree_respectsBoundsAndOverlap() {
        val items = listOf(widget("w", 0, 1, 1, 2, 2))
        assertFalse(GridLogic.isFree(items, 0, 2, 2, 1, 1, cols = 4, rows = 4))
        assertTrue(GridLogic.isFree(items, 0, 3, 3, 1, 1, cols = 4, rows = 4))
        assertFalse("за правой границей", GridLogic.isFree(emptyList(), 0, 3, 0, 2, 1, cols = 4, rows = 4))
        assertTrue("сам элемент не мешает себе", GridLogic.isFree(items, 0, 1, 1, 2, 2, 4, 4, ignoreId = "w"))
    }

    @Test
    fun findFree_fillsFirstPageThenOpensNewOne() {
        // сетка 2x2, первая страница заполнена
        val full = listOf(app("a", 0, 0, 0), app("b", 0, 1, 0), app("c", 0, 0, 1), app("d", 0, 1, 1))
        assertEquals(Triple(1, 0, 0), GridLogic.findFree(full, 2, 2, 1, 1))
        // свободное место остаётся на первой странице
        assertEquals(Triple(0, 1, 1), GridLogic.findFree(full.dropLast(1), 2, 2, 1, 1))
    }

    @Test
    fun nearestFree_picksClosestCell() {
        val items = listOf(app("a", 0, 1, 1))
        // ячейка (1,1) занята: ближайшие свободные — соседние, при равенстве побеждает первая в порядке обхода
        assertEquals(1 to 0, GridLogic.nearestFree(items, 0, 1, 1, 1, 1, cols = 3, rows = 3, ignoreId = null))
        val full = (0 until 2).flatMap { y -> (0 until 2).map { x -> app("i$x$y", 0, x, y) } }
        assertNull(GridLogic.nearestFree(full, 0, 0, 0, 1, 1, 2, 2, null))
    }

    @Test
    fun dock_usesSingleRow() {
        val dock = listOf(app("a", HomeItem.DOCK, 0, 0))
        assertFalse(GridLogic.isFree(dock, HomeItem.DOCK, 0, 0, 1, 1, cols = 4, rows = 1))
        assertEquals(1 to 0, GridLogic.nearestFree(dock, HomeItem.DOCK, 0, 0, 1, 1, 4, 1, null))
    }

    @Test
    fun refit_movesItemsThatNoLongerFit() {
        val items = listOf(app("a", 0, 0, 0), app("b", 0, 3, 3))
        val out = GridLogic.refit(items, cols = 2, rows = 2, dockCount = 4)
        val b = out.first { it.id == "b" }
        assertTrue("b должен оказаться внутри сетки 2x2", b.x < 2 && b.y < 2)
        assertEquals(out.size, out.map { Triple(it.page, it.x, it.y) }.distinct().size)
    }

    @Test
    fun refit_clampsOversizedWidget() {
        val out = GridLogic.refit(listOf(widget("w", 0, 0, 0, 5, 5)), cols = 3, rows = 3, dockCount = 4)
        assertEquals(3, out.single().w)
        assertEquals(3, out.single().h)
    }

    @Test
    fun compactPages_removesEmptyMiddlePages() {
        val items = listOf(app("a", 0, 0, 0), app("b", 3, 0, 0), app("d", HomeItem.DOCK, 0, 0))
        val out = GridLogic.compactPages(items)
        assertEquals(listOf(0, 1, HomeItem.DOCK), out.map { it.page })
    }

    @Test
    fun pageCount_ignoresDock() {
        assertEquals(1, GridLogic.pageCount(emptyList()))
        assertEquals(3, GridLogic.pageCount(listOf(app("a", 2, 0, 0), app("d", HomeItem.DOCK, 0, 0))))
    }

    @Test
    fun placeClock_movesCoveredItemsAndKeepsThemAll() {
        val items = listOf(app("top", 0, 1, 0), app("second", 0, 2, 1), app("low", 0, 0, 3))
        val clock = HomeItem("c", ItemType.CLOCK, 0, 0, 0)
        val out = GridLogic.placeClock(items, clock, cols = 4, rows = 4, page = 0, y = 0, h = 2)
        assertEquals(4, out.size)
        val placed = out.first { it.id == "c" }
        assertEquals(4, placed.w)
        assertEquals(2, placed.h)
        for (i in out.filter { it.id != "c" && it.page == 0 }) assertTrue("${i.id} под часами", i.y >= 2)
        assertEquals("низ не трогаем", 3, out.first { it.id == "low" }.y)
    }

    @Test
    fun reconcile_removesGoneRemapsRenamedKeepsDisabled() {
        val items = listOf(
            app("gone", 0, 0, 0),
            HomeItem("r", ItemType.APP, 0, 1, 0, appKey = "pkg.r/Old#0"),
            HomeItem("d", ItemType.APP, 0, 2, 0, appKey = "pkg.d/Main#0"),
        )
        val out = GridLogic.reconcile(items, setOf("pkg.r/New#0")) { pkg, _ -> pkg == "pkg.gone" }
        assertEquals(listOf("r", "d"), out.map { it.id })
        assertEquals("pkg.r/New#0", out[0].appKey)
        assertEquals("отключённое приложение остаётся", "pkg.d/Main#0", out[1].appKey)
    }

    @Test
    fun reconcile_dissolvesFolderLeftWithOneApp() {
        val folder = HomeItem("f", ItemType.FOLDER, 0, 0, 0, children = listOf("a/M#0", "b/M#0"))
        val out = GridLogic.reconcile(listOf(folder), setOf("a/M#0")) { _, _ -> true }
        assertEquals(ItemType.APP, out.single().type)
        assertEquals("a/M#0", out.single().appKey)
    }

    @Test
    fun appFilter_parsesComponentForms() {
        val f = AppFilter()
        f.addItem("ComponentInfo{com.foo/com.foo.Main}", "foo")
        f.addItem("ComponentInfo{com.bar/.Start}", "bar")
        f.addItem("broken", "x")
        assertEquals("foo", f.drawableFor("com.foo", "com.foo.Main"))
        assertEquals("bar", f.drawableFor("com.bar", "com.bar.Start"))
        assertEquals("запасной вариант по пакету", "foo", f.drawableFor("com.foo", "com.foo.Other"))
        assertNull(f.drawableFor("com.none", "A"))
    }
    @Test
    fun reflow_shiftsChainTowardNearestGap() {
        // Ряд: A B C _ ; тащим D со второго ряда на место A — все сдвигаются вправо до пустой клетки
        val items = listOf(app("a", 0, 0, 0), app("b", 0, 1, 0), app("c", 0, 2, 0), app("d", 0, 3, 1))
        val moved = GridLogic.reflow(items, "d", 0, 0, 0, 1, 1, cols = 4, rows = 4)!!
        assertEquals(mapOf("a" to (1 to 0), "b" to (2 to 0), "c" to (3 to 0)), moved)
    }

    @Test
    fun reflow_movesTowardDraggedIconSource() {
        // Значок A перетаскивают на место C: B и C сдвигаются назад в освободившуюся клетку
        val items = listOf(app("a", 0, 0, 0), app("b", 0, 1, 0), app("c", 0, 2, 0), app("d", 0, 3, 0))
        val moved = GridLogic.reflow(items, "a", 0, 2, 0, 1, 1, cols = 4, rows = 1)!!
        assertEquals(mapOf("c" to (1 to 0), "b" to (0 to 0)), moved)
    }

    @Test
    fun reflow_chainWrapsToNextRowAndSkipsWidgets() {
        val items = listOf(app("a", 0, 1, 0), widget("w", 0, 0, 1, 2, 1), app("z", 0, 3, 3))
        val full = items + listOf(app("f", 0, 0, 0))
        val moved = GridLogic.reflow(full, "z", 0, 1, 0, 1, 1, cols = 2, rows = 4)!!
        assertEquals("обходит виджет во втором ряду", mapOf("a" to (0 to 2)), moved)
    }

    @Test
    fun reflow_freeCellNeedsNothing_andBlockedByWidget() {
        val items = listOf(widget("w", 0, 0, 0, 2, 2), app("a", 0, 3, 3))
        assertEquals(emptyMap<String, Pair<Int, Int>>(), GridLogic.reflow(items, "a", 0, 2, 2, 1, 1, 4, 4))
        assertNull("виджет значок не сдвигает", GridLogic.reflow(items, "a", 0, 0, 0, 1, 1, 4, 4))
    }

    @Test
    fun reflow_fullPageReturnsNull() {
        val items = listOf(app("a", 0, 0, 0), app("b", 0, 1, 0))
        assertNull(GridLogic.reflow(items + app("c", 1, 0, 0), "c", 0, 0, 0, 1, 1, cols = 2, rows = 1))
    }

    @Test
    fun reflow_worksInDock() {
        val items = listOf(app("a", HomeItem.DOCK, 0, 0), app("b", HomeItem.DOCK, 1, 0), app("c", 0, 0, 0))
        assertEquals(mapOf("a" to (0 to 0).let { 1 to 0 }, "b" to (2 to 0)), GridLogic.reflow(items, "c", HomeItem.DOCK, 0, 0, 1, 1, cols = 4, rows = 1))
    }
}
