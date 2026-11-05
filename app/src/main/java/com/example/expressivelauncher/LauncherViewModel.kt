package com.example.expressivelauncher

import android.app.Application
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings as SysSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.ClockStyle
import com.example.expressivelauncher.data.DrawerSort
import com.example.expressivelauncher.data.GridLogic
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.ItemType
import com.example.expressivelauncher.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val la = app as LauncherApp
    val repo = la.repo
    val widgets = la.widgets
    private val settingsStore = la.settings
    private val layout = la.layout

    val settings: StateFlow<Settings> = settingsStore.state
    val items: StateFlow<List<HomeItem>> = layout.items
    val allApps: StateFlow<List<AppInfo>> = repo.apps
    val iconThemes = repo.themes.themes

    /** Приложения для меню: без скрытых, в выбранном порядке. */
    val drawerApps: StateFlow<List<AppInfo>> = combine(
        repo.apps,
        settings.map { it.hiddenApps to it.drawerSort }.distinctUntilChanged(),
    ) { apps, (hidden, sort) ->
        val visible = apps.filter { it.key !in hidden }
        if (sort == DrawerSort.RECENT) visible.sortedByDescending { it.installTime } else visible
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            settings.map { Triple(it.columns, it.rows, it.dockCount) }.distinctUntilChanged().collect { (c, r, d) ->
                layout.update { GridLogic.compactPages(GridLogic.refit(it, c, r, d)) }
                syncClock()
            }
        }
        viewModelScope.launch {
            repo.loaded.first { it }
            if (!layout.seeded) { seedDefaults(); layout.seeded = true }
            settings.map { it.clockStyle }.distinctUntilChanged().collect { syncClock() }
        }
        viewModelScope.launch {
            settings.map { it.iconTheme }.distinctUntilChanged().collect(repo::setIconTheme)
        }
        // Выбранный набор значков могли удалить: возвращаемся к системным, иначе в списке не будет отмечено ничего
        viewModelScope.launch {
            repo.loaded.first { it }
            iconThemes.collect { list ->
                val id = settings.value.iconTheme
                if (id.isNotEmpty() && list.none { it.id == id }) updateSettings { it.copy(iconTheme = "") }
            }
        }
        // Приложения могли удалить, пока лаунчер не работал: при каждом обновлении списка сверяем с ним раскладку
        viewModelScope.launch {
            repo.loaded.first { it }
            repo.apps.collect { reconcile() }
        }
    }

    private fun clockHeight(style: ClockStyle) = if (style == ClockStyle.COMPACT) 1 else 2

    /** Часы — обычный элемент сетки: их можно перетащить или убрать. Размер следует за настройками. */
    private fun syncClock() {
        val s = settings.value
        layout.update { list ->
            val clock = list.firstOrNull { it.type == ItemType.CLOCK }
            val h = clockHeight(s.clockStyle)
            when {
                s.clockStyle == ClockStyle.OFF -> GridLogic.compactPages(list.filter { it.type != ItemType.CLOCK })
                clock == null -> GridLogic.placeClock(list, HomeItem(newId(), ItemType.CLOCK, 0, 0, 0), s.columns, s.rows, 0, 0, h)
                clock.w == s.columns && clock.h == h && clock.y + clock.h <= s.rows -> list
                else -> GridLogic.placeClock(list, clock, s.columns, s.rows, clock.page, clock.y, h)
            }
        }
    }

    private suspend fun reconcile() {
        val installed = repo.byKey.keys
        val current = items.value
        // Всё считаем в фоне, включая проверки пакетов: запрос к PackageManager в главном потоке — заметный рывок
        val cleaned = withContext(Dispatchers.Default) {
            GridLogic.reconcile(current, installed, repo::isPackageGone)
        }
        if (cleaned != current) layout.update { GridLogic.compactPages(cleaned) }
        // Виджет, у которого пропал поставщик, рисовал бы пустое место
        val dead = items.value.filter { it.type == ItemType.WIDGET && widgets.info(it.widgetId) == null }.map { it.id }.toSet()
        if (dead.isNotEmpty()) layout.update { list -> GridLogic.compactPages(list.filter { it.id !in dead }) }
    }

    fun updateSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    // ---------- Размещение ----------

    private fun newId() = UUID.randomUUID().toString()

    fun isOnHome(appKey: String): Boolean = items.value.any {
        it.appKey == appKey || appKey in it.children
    }

    fun addApp(app: AppInfo, page: Int = 0) {
        val s = settings.value
        layout.update { list ->
            val (p, x, y) = GridLogic.findFree(list, s.columns, s.rows, 1, 1, startPage = page)
            list + HomeItem(newId(), ItemType.APP, p, x, y, appKey = app.key)
        }
    }

    /**
     * Несколько приложений из шторки сразу: ложатся подряд на свободные места, уже лежащие на столе пропускаются.
     * Возвращает страницу, куда попало первое, или null, если добавлять было нечего.
     */
    fun addApps(apps: List<AppInfo>): Int? {
        val s = settings.value
        var first: Int? = null
        layout.update { list ->
            first = null
            var out = list
            var start = 0
            for (app in apps) {
                if (out.any { it.appKey == app.key || app.key in it.children }) continue
                val (p, x, y) = GridLogic.findFree(out, s.columns, s.rows, 1, 1, startPage = start)
                if (first == null) first = p
                start = p
                out = out + HomeItem(newId(), ItemType.APP, p, x, y, appKey = app.key)
            }
            out
        }
        return first
    }

    /** Переносит или объединяет в папку. [merge] — палец в центре занятой ячейки. */
    fun drop(id: String, page: Int, x: Int, y: Int, merge: Boolean) {
        val s = settings.value
        layout.update { list ->
            val item = list.firstOrNull { it.id == id } ?: return@update list
            val dock = page == HomeItem.DOCK
            val cols = if (dock) s.dockCount else s.columns
            val rows = if (dock) 1 else s.rows
            val tx = x.coerceIn(0, (cols - item.w).coerceAtLeast(0))
            val ty = if (dock) 0 else y.coerceIn(0, (rows - item.h).coerceAtLeast(0))
            val occupant = GridLogic.itemAt(list, page, tx, ty, ignoreId = id)

            if (merge && occupant != null && item.type == ItemType.APP && (occupant.type == ItemType.APP || occupant.type == ItemType.FOLDER)) {
                val merged = if (occupant.type == ItemType.APP) {
                    list.filter { it.id != id && it.id != occupant.id } + HomeItem(
                        newId(), ItemType.FOLDER, occupant.page, occupant.x, occupant.y,
                        children = listOf(occupant.appKey!!, item.appKey!!),
                    )
                } else {
                    list.filter { it.id != id }.map {
                        if (it.id == occupant.id) it.copy(children = it.children + item.appKey!!) else it
                    }
                }
                return@update GridLogic.compactPages(merged)
            }
            // Соседи расступаются ровно так же, как показывал предпросмотр во время перетаскивания
            val moved = GridLogic.reflow(list, id, page, tx, ty, item.w, item.h, cols, rows)
            val spot = if (moved != null) tx to ty
            else GridLogic.nearestFree(list, page, tx, ty, item.w, item.h, cols, rows, id) ?: return@update list
            // Последний элемент могли унести со страницы: пустая страница не должна оставаться
            GridLogic.compactPages(list.map {
                val shifted = moved?.get(it.id)
                when {
                    it.id == id -> it.copy(page = page, x = spot.first, y = spot.second)
                    shifted != null -> it.copy(x = shifted.first, y = shifted.second)
                    else -> it
                }
            })
        }
    }

    fun removeItem(id: String) {
        val item = items.value.firstOrNull { it.id == id }
        if (item?.type == ItemType.WIDGET) widgets.delete(item.widgetId)
        if (item?.type == ItemType.CLOCK) { updateSettings { it.copy(clockStyle = ClockStyle.OFF) }; return }
        layout.update { GridLogic.compactPages(it.filter { i -> i.id != id }) }
    }

    /**
     * Переносит сразу несколько значков. Основной элемент встаёт под палец, остальные стараются
     * сохранить взаимное расположение; если места нет — ищут ближайшее свободное.
     */
    fun dropMany(ids: List<String>, page: Int, x: Int, y: Int) {
        if (ids.isEmpty()) return
        val s = settings.value
        layout.update { list ->
            val group = ids.mapNotNull { id -> list.firstOrNull { it.id == id } }
            if (group.isEmpty()) return@update list
            val anchor = group.first()
            val dock = page == HomeItem.DOCK
            val cols = if (dock) s.dockCount else s.columns
            val rows = if (dock) 1 else s.rows
            var working = list.filter { it.id !in ids }

            if (dock) {
                for (g in group) {
                    val spot = GridLogic.nearestFree(working, HomeItem.DOCK, x, 0, 1, 1, cols, 1, null)
                    if (spot != null) {
                        working = working + g.copy(page = HomeItem.DOCK, x = spot.first, y = 0)
                    } else {
                        val (p, nx, ny) = GridLogic.findFree(working, s.columns, s.rows, g.w, g.h, startPage = 0)
                        working = working + g.copy(page = p, x = nx, y = ny)
                    }
                }
            } else {
                val samePage = group.all { it.page == anchor.page && !it.inDock }
                val dx = if (samePage) x - anchor.x else 0
                val dy = if (samePage) y - anchor.y else 0
                for (g in group) {
                    val wantX = (if (samePage) g.x + dx else x).coerceIn(0, (cols - g.w).coerceAtLeast(0))
                    val wantY = (if (samePage) g.y + dy else y).coerceIn(0, (rows - g.h).coerceAtLeast(0))
                    val spot = if (GridLogic.isFree(working, page, wantX, wantY, g.w, g.h, cols, rows)) {
                        wantX to wantY
                    } else {
                        GridLogic.nearestFree(working, page, wantX, wantY, g.w, g.h, cols, rows, null)
                    }
                    if (spot != null) {
                        working = working + g.copy(page = page, x = spot.first, y = spot.second)
                    } else {
                        val (p, nx, ny) = GridLogic.findFree(working, cols, rows, g.w, g.h, startPage = page)
                        working = working + g.copy(page = p, x = nx, y = ny)
                    }
                }
            }
            GridLogic.compactPages(working)
        }
    }

    fun removeItems(ids: Set<String>) {
        if (ids.isEmpty()) return
        val list = items.value
        list.filter { it.id in ids && it.type == ItemType.WIDGET }.forEach { widgets.delete(it.widgetId) }
        if (list.any { it.id in ids && it.type == ItemType.CLOCK }) updateSettings { it.copy(clockStyle = ClockStyle.OFF) }
        layout.update { GridLogic.compactPages(it.filter { i -> i.id !in ids }) }
    }

    fun renameFolder(id: String, title: String) = layout.update { list ->
        list.map { if (it.id == id) it.copy(title = title) else it }
    }

    fun removeFromFolder(folderId: String, appKey: String) {
        val s = settings.value
        layout.update { list ->
            val folder = list.firstOrNull { it.id == folderId } ?: return@update list
            val rest = folder.children - appKey
            var out = list.map { if (it.id == folderId) it.copy(children = rest) else it }
            val (p, x, y) = GridLogic.findFree(out, s.columns, s.rows, 1, 1, startPage = folder.page.coerceAtLeast(0))
            out = out + HomeItem(newId(), ItemType.APP, p, x, y, appKey = appKey)
            GridLogic.compactPages(out.mapNotNull(GridLogic::dissolveFolder))
        }
    }

    /** Переносит значок в док. Док заполнен — ничего не меняет и сообщает об этом (раньше кнопка молча не работала). */
    fun moveToDock(id: String) {
        val s = settings.value
        var full = false
        layout.update { list ->
            val target = list.firstOrNull { it.id == id } ?: return@update list
            if (target.type != ItemType.APP && target.type != ItemType.FOLDER) return@update list
            val free = GridLogic.nearestFree(list, HomeItem.DOCK, 0, 0, 1, 1, s.dockCount, 1, id)
            if (free == null) { full = true; return@update list }
            GridLogic.compactPages(list.map { if (it.id == id) it.copy(page = HomeItem.DOCK, x = free.first, y = 0) else it })
        }
        if (full) com.example.expressivelauncher.ui.components.LauncherToast.show(getApplication<Application>().getString(R.string.dock_full))
    }

    fun addWidget(widgetId: Int, w: Int, h: Int, page: Int) {
        val s = settings.value
        layout.update { list ->
            val cw = w.coerceIn(1, s.columns)
            val ch = h.coerceIn(1, s.rows)
            val (p, x, y) = GridLogic.findFree(list, s.columns, s.rows, cw, ch, startPage = page)
            list + HomeItem(newId(), ItemType.WIDGET, p, x, y, cw, ch, widgetId = widgetId)
        }
    }

    fun resizeWidget(id: String, dw: Int, dh: Int) {
        val s = settings.value
        layout.update { list ->
            val it0 = list.firstOrNull { it.id == id } ?: return@update list
            val nw = (it0.w + dw).coerceIn(1, s.columns)
            val nh = (it0.h + dh).coerceIn(1, s.rows)
            if (GridLogic.isFree(list, it0.page, it0.x, it0.y, nw, nh, s.columns, s.rows, id))
                list.map { i -> if (i.id == id) i.copy(w = nw, h = nh) else i }
            else list
        }
    }

    /**
     * Новые границы виджета (рамка изменения размера, как в One UI): соседние значки расступаются,
     * как при перетаскивании. Возвращает false, если на странице не хватает места.
     */
    fun setWidgetBounds(id: String, x: Int, y: Int, w: Int, h: Int): Boolean {
        val s = settings.value
        var ok = false
        layout.update { list ->
            val it0 = list.firstOrNull { it.id == id } ?: return@update list
            if (x < 0 || y < 0 || w < 1 || h < 1 || x + w > s.columns || y + h > s.rows) return@update list
            if (it0.x == x && it0.y == y && it0.w == w && it0.h == h) { ok = true; return@update list }
            val moves = GridLogic.reflow(list, id, it0.page, x, y, w, h, s.columns, s.rows) ?: return@update list
            ok = true
            list.map { i ->
                val m = moves[i.id]
                when {
                    i.id == id -> i.copy(x = x, y = y, w = w, h = h)
                    m != null -> i.copy(x = m.first, y = m.second)
                    else -> i
                }
            }
        }
        return ok
    }

    /** Резервная копия: настройки вместе с раскладкой. */
    fun exportLayout(): String = org.json.JSONObject()
        .put("v", 2)
        .put("settings", settingsStore.exportJson())
        .put("layout", layout.exportObject())
        .toString(2)

    /** Файл могли сохранить на другом устройстве: подгоняем под текущую сетку и убираем чужие приложения. */
    fun importLayout(json: String): Boolean {
        // Новый формат — объект с настройками и раскладкой, старый — только раскладка
        val root = runCatching { org.json.JSONObject(json) }.getOrNull() ?: return false
        val layoutJson = root.optJSONObject("layout")?.toString() ?: json
        val parsed = layout.parseImport(layoutJson) ?: return false
        root.optJSONObject("settings")?.let { if (!settingsStore.importJson(it)) return false }
        val s = settings.value
        items.value.filter { it.type == ItemType.WIDGET }.forEach { widgets.delete(it.widgetId) }
        val installed = repo.byKey.keys
        layout.update {
            GridLogic.compactPages(GridLogic.refit(GridLogic.reconcile(parsed, installed, repo::isPackageGone), s.columns, s.rows, s.dockCount))
        }
        syncClock()
        return true
    }

    fun resetLayout() {
        items.value.filter { it.type == ItemType.WIDGET }.forEach { widgets.delete(it.widgetId) }
        layout.update { emptyList() }
        seedDefaults()
        syncClock()
    }

    fun flushLayout() = layout.flush()

    fun deleteIconTheme(id: String) {
        viewModelScope.launch(Dispatchers.IO) { repo.themes.delete(id) }
    }

    private fun seedDefaults() {
        val s = settings.value
        fun main(cat: String) = Intent(Intent.ACTION_MAIN).addCategory(cat)
        val dock = listOfNotNull(
            repo.findDefault(Intent(Intent.ACTION_DIAL)),
            repo.findDefault(main(Intent.CATEGORY_APP_MESSAGING)),
            repo.findDefault(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com"))),
            repo.findDefault(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            repo.findDefault(main(Intent.CATEGORY_APP_MUSIC)),
        ).distinctBy { it.key }.take(s.dockCount)
        val home = listOfNotNull(
            repo.findDefault(main(Intent.CATEGORY_APP_GALLERY)),
            repo.findDefault(main(Intent.CATEGORY_APP_CALENDAR)),
            repo.findDefault(main(Intent.CATEGORY_APP_MAPS)),
            repo.findDefault(main(Intent.CATEGORY_APP_EMAIL)),
            repo.findDefault(main(Intent.CATEGORY_APP_CALCULATOR)),
            repo.findDefault(Intent(SysSettings.ACTION_SETTINGS)),
        ).distinctBy { it.key }.filter { h -> dock.none { it.key == h.key } }
        layout.update {
            var list = it
            dock.forEachIndexed { i, a ->
                list = list + HomeItem(newId(), ItemType.APP, HomeItem.DOCK, i, 0, appKey = a.key)
            }
            // Верхние ряды заняты часами, поэтому иконки кладём ближе к низу сетки
            home.forEachIndexed { i, a ->
                list = list + HomeItem(newId(), ItemType.APP, 0, i % s.columns, s.rows - 1 - i / s.columns, appKey = a.key)
            }
            list
        }
    }
}
