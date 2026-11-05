package com.example.expressivelauncher.data

import kotlin.math.abs

/** Чистые функции размещения на сетке — без Android-зависимостей, легко тестировать. */
object GridLogic {

    fun isFree(
        items: List<HomeItem>, page: Int, x: Int, y: Int, w: Int, h: Int,
        cols: Int, rows: Int, ignoreId: String? = null,
    ): Boolean {
        if (page == HomeItem.DOCK) {
            return x in 0 until cols && items.none { it.inDock && it.id != ignoreId && it.x == x }
        }
        if (x < 0 || y < 0 || x + w > cols || y + h > rows) return false
        return items.none {
            it.page == page && it.id != ignoreId &&
                x < it.x + it.w && it.x < x + w && y < it.y + it.h && it.y < y + h
        }
    }

    fun itemAt(items: List<HomeItem>, page: Int, x: Int, y: Int, ignoreId: String? = null): HomeItem? =
        items.firstOrNull {
            it.page == page && it.id != ignoreId &&
                x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h
        }

    /** Первая свободная область, начиная со [startPage]; страницы добавляются по необходимости. */
    fun findFree(
        items: List<HomeItem>, cols: Int, rows: Int, w: Int, h: Int,
        startPage: Int = 0, ignoreId: String? = null,
    ): Triple<Int, Int, Int> {
        val lastPage = (items.filter { !it.inDock }.maxOfOrNull { it.page } ?: 0) + 1
        for (p in startPage..lastPage) {
            for (y in 0..rows - h) for (x in 0..cols - w) {
                if (isFree(items, p, x, y, w, h, cols, rows, ignoreId)) return Triple(p, x, y)
            }
        }
        return Triple(lastPage + 1, 0, 0)
    }

    /** Ближайшая к (x, y) свободная ячейка на странице, либо null. */
    fun nearestFree(
        items: List<HomeItem>, page: Int, x: Int, y: Int, w: Int, h: Int,
        cols: Int, rows: Int, ignoreId: String?,
    ): Pair<Int, Int>? {
        if (page == HomeItem.DOCK) {
            return (0 until cols).filter { isFree(items, page, it, 0, 1, 1, cols, rows, ignoreId) }
                .minByOrNull { abs(it - x) }?.let { it to 0 }
        }
        var best: Pair<Int, Int>? = null
        var bestD = Int.MAX_VALUE
        for (yy in 0..rows - h) for (xx in 0..cols - w) {
            if (!isFree(items, page, xx, yy, w, h, cols, rows, ignoreId)) continue
            val d = abs(xx - x) * abs(xx - x) + abs(yy - y) * abs(yy - y)
            if (d < bestD) { bestD = d; best = xx to yy }
        }
        return best
    }

    /**
     * Куда сдвинуть соседей, чтобы освободить область под перетаскиваемый элемент [id].
     * Пустая карта — место и так свободно, null — освободить нельзя (страница заполнена или под пальцем часы/виджет).
     * Значки одной цепочкой съезжают к ближайшей пустой клетке, как при перестановке в списке;
     * большие элементы остаются на месте, а цепочка их обходит.
     */
    fun reflow(
        items: List<HomeItem>, id: String, page: Int, x: Int, y: Int, w: Int, h: Int,
        cols: Int, rows: Int,
    ): Map<String, Pair<Int, Int>>? {
        val others = items.filter { it.page == page && it.id != id }
        val blocked = others.filter { x < it.x + it.w && it.x < x + w && y < it.y + it.h && it.y < y + h }
        if (blocked.isEmpty()) return emptyMap()
        val dragged = items.firstOrNull { it.id == id }
        if (w == 1 && h == 1) {
            if (blocked.any { it.w > 1 || it.h > 1 }) return null
            return chainShift(others, dragged, page, y * cols + x, cols, rows)
        }
        // Большой элемент: каждого соседа переносим на ближайшее свободное место
        val placed = ArrayList(items.filter { it.id != id && blocked.none { b -> b.id == it.id } })
        placed += HomeItem("__target", ItemType.WIDGET, page, x, y, w, h)
        val moved = HashMap<String, Pair<Int, Int>>()
        for (b in blocked) {
            val spot = nearestFree(placed, page, b.x, b.y, b.w, b.h, cols, rows, null) ?: return null
            moved[b.id] = spot
            placed += b.copy(x = spot.first, y = spot.second)
        }
        return moved
    }

    private fun chainShift(
        others: List<HomeItem>, dragged: HomeItem?, page: Int, target: Int, cols: Int, rows: Int,
    ): Map<String, Pair<Int, Int>>? {
        val total = cols * rows
        val icons = HashMap<Int, HomeItem>()
        val solid = HashSet<Int>()
        for (o in others) {
            if (o.w == 1 && o.h == 1) { icons[o.y * cols + o.x] = o; continue }
            for (yy in o.y until o.y + o.h) for (xx in o.x until o.x + o.w) solid += yy * cols + xx
        }
        fun free(i: Int) = i in 0 until total && i !in solid && i !in icons
        // При равном расстоянии идём в сторону, откуда взяли значок: так он как будто меняется местами с соседями
        val source = dragged?.takeIf { it.page == page }?.let { it.y * cols + it.x }
        val forwardFirst = source == null || source >= target
        var dir = 0
        for (d in 1 until total) {
            val fwd = free(target + d)
            val back = free(target - d)
            if (fwd || back) {
                dir = when {
                    fwd && back -> if (forwardFirst) 1 else -1
                    fwd -> 1
                    else -> -1
                }
                break
            }
        }
        if (dir == 0) return null
        val moved = HashMap<String, Pair<Int, Int>>()
        var from = target
        while (true) {
            val icon = icons[from] ?: break
            var to = from + dir
            while (to in solid) to += dir
            moved[icon.id] = (to % cols) to (to / cols)
            from = to
        }
        return moved
    }

    fun pageCount(items: List<HomeItem>): Int =
        ((items.filter { !it.inDock }.maxOfOrNull { it.page } ?: 0) + 1).coerceAtLeast(1)

    /** Приводит раскладку к новым размерам сетки: вылезшие за границы элементы переносятся в свободные места. */
    fun refit(items: List<HomeItem>, cols: Int, rows: Int, dockCount: Int): List<HomeItem> {
        val out = ArrayList<HomeItem>(items.size)
        val pending = ArrayList<HomeItem>()
        for (it in items) {
            val fits = if (it.inDock) it.x < dockCount
            else it.x + it.w <= cols && it.y + it.h <= rows
            if (fits) out += it else pending += it
        }
        for (it in pending) {
            if (it.inDock) {
                val free = nearestFree(out, HomeItem.DOCK, it.x, 0, 1, 1, dockCount, 1, null)
                if (free != null) { out += it.copy(x = free.first); continue }
                val (p, x, y) = findFree(out, cols, rows, 1, 1)
                out += it.copy(page = p, x = x, y = y)
            } else {
                val w = it.w.coerceAtMost(cols)
                val h = it.h.coerceAtMost(rows)
                val (p, x, y) = findFree(out, cols, rows, w, h)
                out += it.copy(page = p, x = x, y = y, w = w, h = h)
            }
        }
        return out
    }

    /** Убирает пустые страницы между заполненными, сохраняя порядок. */
    fun compactPages(items: List<HomeItem>): List<HomeItem> {
        val used = items.filter { !it.inDock }.map { it.page }.distinct().sorted()
        if (used.isEmpty() || used == (0 until used.size).toList()) return items
        val map = used.withIndex().associate { (i, p) -> p to i }
        return items.map { if (it.inDock) it else it.copy(page = map.getValue(it.page)) }
    }

    /**
     * Ставит часы на всю ширину сетки в ряд [y] страницы [page]; элементы, оказавшиеся под ними, переезжают на свободные места.
     */
    fun placeClock(items: List<HomeItem>, clock: HomeItem, cols: Int, rows: Int, page: Int, y: Int, h: Int): List<HomeItem> {
        val height = h.coerceIn(1, rows)
        val top = y.coerceIn(0, rows - height)
        val placed = clock.copy(page = page, x = 0, y = top, w = cols, h = height)
        val others = items.filter { it.id != clock.id }
        val (blocked, free) = others.partition {
            it.page == page && it.y < top + height && top < it.y + it.h
        }
        val out = ArrayList(free)
        out += placed
        for (b in blocked) {
            val (p, nx, ny) = findFree(out, cols, rows, b.w, b.h, startPage = page)
            out += b.copy(page = p, x = nx, y = ny)
        }
        return out
    }

    /**
     * Сверяет раскладку со списком приложений: убирает удалённые, а если приложение переименовало
     * стартовую активность — переносит ключ на новую. Недоступные, но не удалённые приложения остаются на месте.
     */
    fun reconcile(
        items: List<HomeItem>,
        installed: Set<String>,
        isGone: (pkg: String, serial: Long) -> Boolean,
    ): List<HomeItem> {
        val byPackage = installed.groupBy { keyPackage(it) to keySerial(it) }
        fun resolve(key: String): String? {
            if (key in installed) return key
            val pkg = keyPackage(key)
            val serial = keySerial(key)
            byPackage[pkg to serial]?.firstOrNull()?.let { return it }
            return if (isGone(pkg, serial)) null else key
        }
        return items.mapNotNull { item ->
            when (item.type) {
                ItemType.APP -> item.appKey?.let(::resolve)?.let { item.copy(appKey = it) }
                ItemType.FOLDER -> dissolveFolder(item.copy(children = item.children.mapNotNull(::resolve).distinct()))
                else -> item
            }
        }
    }

    /** Пустая папка исчезает, папка с одним приложением превращается в это приложение. */
    fun dissolveFolder(item: HomeItem): HomeItem? {
        if (item.type != ItemType.FOLDER) return item
        return when (item.children.size) {
            0 -> null
            1 -> item.copy(type = ItemType.APP, appKey = item.children[0], children = emptyList(), title = "")
            else -> item
        }
    }

    private fun keyPackage(key: String) = key.substringBefore('/')
    private fun keySerial(key: String) = key.substringAfterLast('#').toLongOrNull() ?: 0L
}
