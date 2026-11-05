package com.example.expressivelauncher.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.LauncherStyle
import com.example.expressivelauncher.data.MotionStyle
import com.example.expressivelauncher.data.PageTransition
import com.example.expressivelauncher.data.Settings

/**
 * Описание темы лаунчера: всё, чем темы отличаются друг от друга. Экраны не проверяют, какая тема выбрана,
 * они читают токены отсюда ([LocalThemeSpec]) и итоговые настройки ([effective]), поэтому новая тема —
 * это ещё один объект в [LauncherThemes.all] и значение в [LauncherStyle].
 */
@Immutable
data class ThemeSpec(
    val style: LauncherStyle,
    val title: Int,
    /** Подпись в одну строку на карточке выбора. */
    val tagline: Int,
    val icon: ImageVector,
    /** Панели (док, папки, меню, поиск, настройки) — жидкое стекло: сильное размытие, обводка и блик. */
    val glass: Boolean,
    /** Прозрачность светлой обводки по краю стекла; 0 — без обводки. */
    val outlineAlpha: Float,
    /** Сила блика-градиента сверху стекла; 0 — без блика. */
    val sheenAlpha: Float,
    /** Морфинг форм кнопок, карточек и значков при нажатии и выборе. */
    val morph: Boolean,
    /** Крупные выразительные заголовки (веса Emphasized). */
    val boldHeadlines: Boolean,
    /** Одинаковые скругления всех компонентов; null — шкала Expressive с множителем из настроек. */
    val uniformCorner: Dp?,
    /** Тени под панелями и меню. */
    val shadows: Boolean,
    /** Упрощённая графика: без размытия, без пружин, короткие fade/scale. */
    val lite: Boolean,
    /** Своя анимация «значок → окно» (LaunchPortal); иначе система раскрывает окно из значка. */
    val portalLaunch: Boolean,
    /** У темы есть выбор «динамические цвета из обоев / свой акцент». */
    val colorOptions: Boolean,
    /** Свои настройки темы на странице «Темы». */
    val options: List<ThemeOption>,
) {
    /**
     * Настройки, по которым рисуется интерфейс в этой теме. Выбор пользователя не теряется: тема лишь
     * перекрывает то, что противоречит её характеру, а при смене темы снова действуют его значения.
     * [animatorsOn] — системная настройка «Убрать анимации» выключена.
     */
    fun effective(s: Settings, animatorsOn: Boolean): Settings {
        val themed = when (style) {
            LauncherStyle.LIQUID_GLASS -> s.copy(
                iconShape = s.glassIconShape,
                blurBackground = true,
                blurStrength = s.blurStrength.coerceAtLeast(0.25f),
                dockBackground = true,
                dockOpacity = s.glassOpacity,
                drawerOpacity = (s.glassOpacity + 0.3f).coerceAtMost(0.9f),
                cornerScale = s.cornerScale * 1.3f,
                dockRadius = s.dockRadius.coerceAtLeast(32),
                motion = MotionStyle.EXPRESSIVE,
                // Стекло «плывёт», а не прыгает: пружины мягче, чем в Expressive
                animBounce = s.animBounce * 0.6f,
            )
            // Выразительные формы крупнее: док — «таблетка», скругления шкалы Expressive мягче
            LauncherStyle.EXPRESSIVE -> s.copy(
                motion = MotionStyle.EXPRESSIVE,
                cornerScale = s.cornerScale.coerceAtLeast(1.15f),
            )
            LauncherStyle.LITE -> s.copy(
                iconShape = s.liteIconShape,
                blurBackground = false,
                motion = MotionStyle.STANDARD,
                animBounce = 0f,
                wakeWave = false,
                pageTransition = PageTransition.SLIDE,
                // Как стоковый Pixel: значки дока прямо на обоях, без панели; под ними отдельная сплошная
                // строка поиска. Все поверхности непрозрачные, скругление умеренное и одинаковое
                dockBackground = false,
                dockOpacity = 1f,
                dockRadius = 20,
                drawerOpacity = 1f,
                // Тематические фигуры значков (печенье и т.п.) — черта Expressive; в Lite значки круглые
                // (iconShape = liteIconShape выше), без двухцветной перекраски
                duotoneIcons = false,
            )
        }
        // «Убрать анимации» в системе: всё, что можно, происходит сразу
        return if (animatorsOn) themed else themed.copy(launchAnimation = false, wakeWave = false, pageTransition = PageTransition.SLIDE)
    }
}

/** Настройки, которые показываются под выбором темы на странице «Темы». */
enum class ThemeOption { BLUR_STRENGTH, GLASS_OPACITY, GLASS_ICONS, DYNAMIC_COLOR, ACCENT, ANIM_INTENSITY }

object LauncherThemes {
    val LiquidGlass = ThemeSpec(
        style = LauncherStyle.LIQUID_GLASS,
        title = R.string.theme_glass,
        tagline = R.string.theme_glass_tagline,
        icon = Icons.Rounded.BlurOn,
        glass = true,
        outlineAlpha = 0.34f,
        sheenAlpha = 0.16f,
        morph = true,
        boldHeadlines = false,
        uniformCorner = null,
        shadows = false,
        lite = false,
        portalLaunch = true,
        colorOptions = false,
        options = listOf(ThemeOption.BLUR_STRENGTH, ThemeOption.GLASS_OPACITY, ThemeOption.GLASS_ICONS),
    )

    val Expressive = ThemeSpec(
        style = LauncherStyle.EXPRESSIVE,
        title = R.string.theme_expressive,
        tagline = R.string.theme_expressive_tagline,
        icon = Icons.Rounded.AutoAwesome,
        glass = false,
        outlineAlpha = 0f,
        sheenAlpha = 0f,
        morph = true,
        boldHeadlines = true,
        uniformCorner = null,
        shadows = true,
        lite = false,
        portalLaunch = true,
        colorOptions = true,
        options = listOf(ThemeOption.DYNAMIC_COLOR, ThemeOption.ACCENT, ThemeOption.ANIM_INTENSITY),
    )

    val Lite = ThemeSpec(
        style = LauncherStyle.LITE,
        title = R.string.theme_lite,
        tagline = R.string.theme_lite_tagline,
        icon = Icons.Rounded.Bolt,
        glass = false,
        outlineAlpha = 0f,
        sheenAlpha = 0f,
        morph = false,
        boldHeadlines = false,
        uniformCorner = 16.dp,
        shadows = false,
        lite = true,
        portalLaunch = false,
        colorOptions = true,
        options = listOf(ThemeOption.DYNAMIC_COLOR, ThemeOption.ACCENT),
    )

    val all = listOf(LiquidGlass, Expressive, Lite)

    fun of(style: LauncherStyle): ThemeSpec = all.firstOrNull { it.style == style } ?: Expressive
}

val LocalThemeSpec = compositionLocalOf { LauncherThemes.Expressive }
