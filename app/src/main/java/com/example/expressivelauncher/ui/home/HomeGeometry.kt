package com.example.expressivelauncher.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.example.expressivelauncher.data.GridLogic
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.ItemType
import com.example.expressivelauncher.data.Settings
import kotlin.math.floor

enum class DragAction { REMOVE, APP_INFO, UNINSTALL }

sealed interface DropTarget {
    /** Ячейка сетки или дока. [rect] — область подсветки в координатах окна. */
    data class Cell(val page: Int, val x: Int, val y: Int, val merge: Boolean, val rect: Rect) : DropTarget
    data class Action(val action: DragAction, val rect: Rect) : DropTarget
}

/**
 * Геометрия главного экрана в координатах окна. Обновляется при каждой композиции, а не хранится как State:
 * жесты читают актуальные значения в момент события, не вызывая лишних рекомпозиций.
 */
class HomeGeometry {
    var items: List<HomeItem> = emptyList()
    var settings: Settings = Settings()
    var grid: Rect = Rect.Zero
    var dock: Rect = Rect.Zero
    var page: Int = 0
    val controls = HashMap<String, Rect>()
    var zones: List<Pair<DragAction, Rect>> = emptyList()
    /** Высота содержимого ячейки (значок и подпись) по факту измерения: подпись может занимать две строки. */
    val contentHeights = HashMap<String, Int>()

    val cellW: Float get() = grid.width / settings.columns
    val cellH: Float get() = grid.height / settings.rows

    /** Точка на кнопке поверх стола (строка поиска): такие касания не считаются касанием пустого места. */
    fun isOnControl(p: Offset): Boolean = controls.values.any { it.contains(p) }

    fun hitTest(p: Offset): HomeItem? {
        if (settings.showDock && dock.contains(p)) {
            val idx = floor((p.x - dock.left) / (dock.width / settings.dockCount)).toInt()
            return items.firstOrNull { it.inDock && it.x == idx }
        }
        if (grid.contains(p)) {
            val cx = ((p.x - grid.left) / cellW).toInt()
            val cy = ((p.y - grid.top) / cellH).toInt()
            return GridLogic.itemAt(items, page, cx, cy)
        }
        return null
    }

    fun itemBounds(item: HomeItem): Rect = if (item.inDock) {
        val sw = dock.width / settings.dockCount
        Rect(dock.left + item.x * sw, dock.top, dock.left + (item.x + 1) * sw, dock.bottom)
    } else {
        Rect(
            Offset(grid.left + item.x * cellW, grid.top + item.y * cellH),
            Size(item.w * cellW, item.h * cellH),
        )
    }

    /** Прямоугольник самого значка внутри ячейки: из него вырастает окно приложения. */
    fun iconBounds(item: HomeItem, iconPx: Float, labelPx: Float): Rect {
        val cell = itemBounds(item)
        val cx = cell.center.x
        val content = contentHeights[item.id]?.toFloat() ?: (iconPx + labelPx)
        val top = if (item.inDock) cell.center.y - iconPx / 2f else cell.center.y - content / 2f
        return Rect(cx - iconPx / 2f, top, cx + iconPx / 2f, top + iconPx)
    }

    fun dropTarget(p: Offset, item: HomeItem): DropTarget? {
        zones.firstOrNull { (action, r) ->
            r.contains(p) && (action == DragAction.REMOVE || item.type == ItemType.APP)
        }?.let { return DropTarget.Action(it.first, it.second) }

        // В доке рисуются только приложения и папки: виджет 1×1 там бы просто исчез
        if (settings.showDock && (item.type == ItemType.APP || item.type == ItemType.FOLDER) && dock.contains(p)) {
            val sw = dock.width / settings.dockCount
            val fx = (p.x - dock.left) / sw
            val idx = floor(fx).toInt().coerceIn(0, settings.dockCount - 1)
            val merge = item.type == ItemType.APP && (fx - idx) in 0.3f..0.7f
            return DropTarget.Cell(
                HomeItem.DOCK, idx, 0, merge,
                Rect(dock.left + idx * sw, dock.top, dock.left + (idx + 1) * sw, dock.bottom),
            )
        }
        if (grid.contains(p)) {
            val fx = (p.x - grid.left) / cellW - (item.w - 1) / 2f
            val fy = (p.y - grid.top) / cellH - (item.h - 1) / 2f
            val cx = floor(fx).toInt().coerceIn(0, settings.columns - item.w)
            val cy = floor(fy).toInt().coerceIn(0, settings.rows - item.h)
            val inX = fx - floor(fx)
            val inY = fy - floor(fy)
            val merge = item.w == 1 && item.h == 1 && inX in 0.3f..0.7f && inY in 0.3f..0.7f
            return DropTarget.Cell(
                page, cx, cy, merge,
                Rect(Offset(grid.left + cx * cellW, grid.top + cy * cellH), Size(item.w * cellW, item.h * cellH)),
            )
        }
        return null
    }
}
