package com.example.expressivelauncher.ui.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.GestureAction
import com.example.expressivelauncher.data.HomeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Прогресс шторки приложений: 0 — закрыта, 1 — открыта. Чтение в graphicsLayer не вызывает рекомпозиций. */
@Stable
class DrawerState(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set
    private var job: Job? = null

    val isOpen: Boolean get() = progress > 0.5f

    fun dragBy(delta: Float) {
        job?.cancel()
        progress = (progress + delta).coerceIn(0f, 1f)
    }

    /**
     * [velocity] в долях шторки в секунду; положительная — вверх (открытие).
     * Если жест медленный, шторка открывается при прогрессе не меньше [openThreshold].
     */
    fun settle(velocity: Float, spec: AnimationSpec<Float>, openThreshold: Float = 0.5f) {
        val open = when {
            abs(velocity) > 1.5f -> velocity > 0
            else -> progress >= openThreshold
        }
        animateTo(if (open) 1f else 0f, spec, velocity)
    }

    fun animateTo(target: Float, spec: AnimationSpec<Float>, velocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            animate(progress, target, velocity, spec) { v, _ -> progress = v }
        }
    }
}

/** Что сейчас перетаскивается. Позиция пальца хранится отдельно, чтобы двигать призрак без рекомпозиции. */
@Stable
class DragState {
    var item by mutableStateOf<HomeItem?>(null)
    /** Остальные элементы группы, если значков несколько: основной [item] летит под пальцем, они — вместе с ним. */
    var extra by mutableStateOf<List<HomeItem>>(emptyList())
    var pointer by mutableStateOf(Offset.Zero)
    /** Где соседи стоят сейчас, пока палец держит значок над занятой клеткой: id → (x, y). */
    var preview by mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap())
    /** Отпущенный элемент: долетает до клетки из точки, где его отпустили. */
    var landing by mutableStateOf<Landing?>(null)
    val active: Boolean get() = item != null
    val multi: Boolean get() = extra.isNotEmpty()
    val all: List<HomeItem> get() = listOfNotNull(item) + extra
    val ids: Set<String> get() = all.mapTo(HashSet()) { it.id }

    fun contains(id: String): Boolean = item?.id == id || extra.any { it.id == id }

    /** Сосед, который уже отъехал, папку создать не может: под пальцем теперь пустая клетка. */
    fun effective(t: DropTarget.Cell, items: List<HomeItem>): DropTarget.Cell {
        if (!t.merge) return t
        val occupant = com.example.expressivelauncher.data.GridLogic.itemAt(items, t.page, t.x, t.y, item?.id)
        return if (occupant == null || occupant.id in preview) t.copy(merge = false) else t
    }
}

/** Выбранные значки для переноса сразу нескольких приложений. */
@Stable
class SelectionState {
    var ids by mutableStateOf<Set<String>>(emptySet())
        private set
    val active: Boolean get() = ids.isNotEmpty()

    fun toggle(id: String) {
        ids = if (id in ids) ids - id else ids + id
    }

    fun add(id: String) {
        ids = ids + id
    }

    fun retain(alive: Set<String>) {
        if (ids.any { it !in alive }) ids = ids.filterTo(HashSet()) { it in alive }
    }

    fun contains(id: String): Boolean = id in ids

    fun clear() {
        ids = emptySet()
    }
}

class Landing(val id: String, val point: Offset)

/** Контекстное меню у элемента на рабочем столе. */
data class MenuTarget(val item: HomeItem, val anchor: Rect)

/** Меню приложения из шторки. */
data class DrawerMenuTarget(val app: AppInfo, val anchor: Rect)

/** Платформенные действия, которые UI не может выполнить сам. */
interface HomeActions {
    /** Растёт при каждом нажатии «Домой»: сигнал вернуться на главную страницу. */
    val homePressSignal: Int
    /** «Домой» нажали, уже находясь на столе, а не вернувшись из приложения. */
    val homeFromForeground: Boolean
    /** Растёт при каждом возвращении на рабочий стол: запускает волну значков. */
    val wakeSignal: Int
    /** Растёт, когда лаунчер снова на переднем плане после паузы: приложение закрыли. */
    val resumeSignal: Int
    val isPaused: Boolean
    /** Сколько раз лаунчер уходил в паузу: по нему видно, показалось ли запущенное приложение. */
    val pauseCount: Int
    /** Система разрешает размывать то, что под окном (обои). В режиме энергосбережения Android это отключает. */
    val crossWindowBlur: Boolean
    fun setLanguage(language: com.example.expressivelauncher.data.AppLanguage)
    fun setWallpaperScroll(position: Float, pages: Int)
    /** Под строкой состояния сейчас не обои, а поверхность приложения. */
    fun setOverSurface(over: Boolean)
    fun pickIconTheme()
    fun pickAppIcon(app: AppInfo)
    fun openSettings()
    fun addWidget(info: android.appwidget.AppWidgetProviderInfo, spanX: Int, spanY: Int, page: Int)
    fun pickWallpaper()
    fun expandNotifications()
    fun lockScreen()
    fun openClock()
    fun openUrl(url: String)
    fun setBlur(radiusDp: Int)
    fun requestBadgeAccess()
    fun openDefaultLauncherSettings()
    fun openAccessibilitySettings()
    fun exportLayout()
    fun importLayout()
    fun pickFont()
    fun perform(action: GestureAction, openDrawer: () -> Unit, openSearch: () -> Unit) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.NOTIFICATIONS -> expandNotifications()
            GestureAction.DRAWER -> openDrawer()
            GestureAction.LOCK -> lockScreen()
            GestureAction.SETTINGS -> openSettings()
            GestureAction.SEARCH -> openSearch()
        }
    }
}
