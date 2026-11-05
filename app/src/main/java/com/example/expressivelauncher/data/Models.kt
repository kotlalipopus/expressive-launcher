package com.example.expressivelauncher.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.runtime.Immutable

/** Приложение, доступное для запуска. [key] уникален и стабилен: `пакет/класс#серийный_номер_профиля`. */
@Immutable
data class AppInfo(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val user: UserHandle,
    val userSerial: Long,
    val isWork: Boolean,
    val installTime: Long,
    val isSystem: Boolean,
) {
    val component: ComponentName get() = ComponentName(packageName, className)
    val sortKey: String = label.lowercase()
    val letter: Char = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.let {
        if (it.isDigit()) '#' else it
    } ?: '#'
}

enum class ItemType { APP, FOLDER, WIDGET, CLOCK }

enum class HapticLevel { LIGHT, MEDIUM, STRONG }

/**
 * Элемент рабочего стола. [page] == [DOCK] означает док, тогда [x] — индекс в доке.
 */
@Immutable
data class HomeItem(
    val id: String,
    val type: ItemType,
    val page: Int,
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
    val appKey: String? = null,
    val title: String = "",
    val children: List<String> = emptyList(),
    val widgetId: Int = -1,
) {
    val inDock: Boolean get() = page == DOCK

    companion object {
        const val DOCK = -1
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ColorSource { WALLPAPER, CUSTOM }
enum class DrawerStyle { GRID, LIST }
enum class DrawerSort { ALPHABETICAL, RECENT }
enum class SearchPosition { TOP, BOTTOM }
enum class GestureAction { NONE, NOTIFICATIONS, DRAWER, LOCK, SETTINGS, SEARCH }
enum class ClockStyle { BIG, COMPACT, OFF }
enum class ClockAlign { START, CENTER, END }
enum class MotionStyle { EXPRESSIVE, STANDARD }
enum class PageTransition { DEPTH, SLIDE, CUBE, FADE }

/**
 * Тема лаунчера целиком: поверхности, формы, движение и эффекты. Описание каждой — в ui/theme/LauncherThemes.kt.
 * Хранится по имени, поэтому новые темы добавляются в конец без миграции.
 */
enum class LauncherStyle { LIQUID_GLASS, EXPRESSIVE, LITE }

/** Язык интерфейса; [tag] == null — как в системе. */
enum class AppLanguage(val tag: String?) { SYSTEM(null), RUSSIAN("ru"), ENGLISH("en") }


/** Шрифты интерфейса. Системные семейства берутся с устройства; [CUSTOM] — файл, выбранный пользователем. */
enum class AppFont(val deviceName: String?) {
    SYSTEM(null), CONDENSED("sans-serif-condensed"), SERIF("serif"), MONO("monospace"),
    CASUAL("casual"), CURSIVE("cursive"), CUSTOM(null)
}

/** Формы иконок. Имена сопоставляются с MaterialShapes в UI-слое. */
enum class IconShape {
    CIRCLE, SQUIRCLE, ROUNDED, COOKIE_9, COOKIE_12, CLOVER_4, FLOWER, SUNNY, PENTAGON, GEM, PILL, PUFFY,
    /** Скруглённый квадрат с непрерывной кривизной углов, как маска значков iOS. */
    CONTINUOUS,
}

/** Блики на значках темы Liquid Glass. */
enum class IconHighlight { OFF, CORNERS, FULL }

/** Стиль значков темы Liquid Glass: обычные, прозрачные (цветной логотип на стекле, как Clear в iOS 26), тонированные. */
enum class GlassIconStyle { NORMAL, CLEAR, TINTED }

@Immutable
data class Settings(
    // Тема и первоначальная настройка
    val style: LauncherStyle = LauncherStyle.EXPRESSIVE,
    val glassOpacity: Float = 0.42f, // тонировка стеклянных панелей в теме Liquid Glass
    val glassIconStyle: GlassIconStyle = GlassIconStyle.NORMAL,
    val glassIconHighlight: IconHighlight = IconHighlight.CORNERS,
    val glassHighlightStrength: Float = 0.7f,
    val glassIconShape: IconShape = IconShape.CONTINUOUS, // маска значков в Liquid Glass
    val liteIconShape: IconShape = IconShape.CIRCLE, // маска значков в Material You Lite
    val customWallpaper: Boolean = false,
    val wallpaperBlur: Float = 0f, // размытие своих обоев лаунчера, 0..1
    val wallpaperParallax: Boolean = true, // свои обои листаются вместе со страницами // лаунчер рисует свою картинку вместо системных обоев: её видит жидкое стекло
    val onboardingVersion: Int = 0, // какую версию первоначальной настройки пользователь прошёл; 0 — ни одну
    val onboardingStep: Int = 0, // на каком шаге её закрыли, чтобы продолжить с него же
    // Внешний вид
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.WALLPAPER,
    val contrast: Float = 0f,
    val seedColor: Int = 0xFF6750A4.toInt(),
    val amoled: Boolean = false,
    val paletteStyle: Int = 0, // индекс в списке стилей палитры
    val cornerScale: Float = 1f,
    val motion: MotionStyle = MotionStyle.EXPRESSIVE,
    val launchAnimation: Boolean = true,
    val animSpeed: Float = 1f, // множитель скорости всех анимаций
    val animBounce: Float = 1f, // 0 — без пружины, 1 — как задумано, больше — сильнее
    val pageTransition: PageTransition = PageTransition.DEPTH,
    val wakeWave: Boolean = true,
    val liteOnPowerSave: Boolean = true,
    val blurStrength: Float = 1f,
    val closeAnimScale: Float = 1f, // во сколько раз длиннее обычного значок возвращается на стол
    val debugSlowLaunch: Boolean = false, // отладка: запуск и закрытие приложений в 5 раз медленнее
    val language: AppLanguage = AppLanguage.SYSTEM,
    // Шрифты
    val font: AppFont = AppFont.SYSTEM,
    val fontScale: Float = 1f,
    val fontWeightShift: Int = 0, // сдвиг жирности в сотнях: -2..3
    val clockFontSame: Boolean = true,
    val clockFont: AppFont = AppFont.SYSTEM,
    val customFontName: String = "",
    val customFontRev: Int = 0,
    // Иконки
    val iconShape: IconShape = IconShape.COOKIE_9,
    val iconScale: Float = 1f,
    val themedIcons: Boolean = false,
    val duotoneIcons: Boolean = false, // значки без монохромного слоя окрашиваются в цвета темы
    val iconTheme: String = "", // пусто — системные иконки
    val iconThemeMask: Boolean = false, // обрезать значки темы выбранной формой
    val showHomeLabels: Boolean = true,
    val labelScale: Float = 1f,
    val showBadges: Boolean = true,
    // Рабочий стол
    val columns: Int = 5,
    val rows: Int = 6,
    val dockCount: Int = 5,
    val showDock: Boolean = true,
    val dockBackground: Boolean = true,
    val dockOpacity: Float = 0.82f,
    val dockRadius: Int = 36,
    val dockHeight: Int = 84,
    val gridPadding: Int = 0,
    val labelLines: Int = 1,
    val clockStyle: ClockStyle = ClockStyle.BIG,
    val clockAlign: ClockAlign = ClockAlign.START,
    val clockScale: Float = 1f,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val showSearchPill: Boolean = true,
    val searchGoogle: Boolean = true, // строка поиска открывает поиск Google, а не поиск по приложениям
    val searchButtons: List<String> = listOf("voice", "lens", "ai_mode"), // кнопки справа в строке поиска
    val lockLayout: Boolean = false,
    val wallpaperDim: Float = 0f,
    val showPageIndicator: Boolean = true,
    // Меню приложений
    val drawerStyle: DrawerStyle = DrawerStyle.GRID,
    val drawerColumns: Int = 4,
    val drawerSort: DrawerSort = DrawerSort.ALPHABETICAL,
    val drawerLabels: Boolean = true,
    val fastScroller: Boolean = true,
    val searchPosition: SearchPosition = SearchPosition.BOTTOM,
    val drawerOpacity: Float = 0.94f,
    val blurBackground: Boolean = true,
    val autoKeyboard: Boolean = false,
    val autoLaunchSingle: Boolean = true,
    val hiddenApps: Set<String> = emptySet(),
    // Жесты
    val swipeUp: GestureAction = GestureAction.DRAWER,
    val swipeDown: GestureAction = GestureAction.NOTIFICATIONS,
    val doubleTap: GestureAction = GestureAction.LOCK,
    // Прочее
    val haptics: Boolean = true,
    val hapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val hapticTouch: Boolean = true,
    val hapticDrag: Boolean = true,
    val hapticPages: Boolean = true,
    val hapticDrawer: Boolean = true,
    val searchEngine: Int = 0,
)

/** Форма значков выбранной темы: у Liquid Glass и Lite она своя, у Expressive — общая [Settings.iconShape]. */
fun Settings.iconShapeFor(style: LauncherStyle): IconShape = when (style) {
    LauncherStyle.LIQUID_GLASS -> glassIconShape
    LauncherStyle.LITE -> liteIconShape
    LauncherStyle.EXPRESSIVE -> iconShape
}

fun Settings.withIconShape(style: LauncherStyle, shape: IconShape): Settings = when (style) {
    LauncherStyle.LIQUID_GLASS -> copy(glassIconShape = shape)
    LauncherStyle.LITE -> copy(liteIconShape = shape)
    LauncherStyle.EXPRESSIVE -> copy(iconShape = shape)
}
