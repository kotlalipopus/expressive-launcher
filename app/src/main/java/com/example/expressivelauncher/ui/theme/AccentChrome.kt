package com.example.expressivelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct

/**
 * Цвета «хрома» темы Expressive (док, строка поиска, меню стола, меню приложений) — по макету Figma.
 *
 * Тона считаются сами в HCT из оттенка `primary` текущей схемы (Monet), а не берутся ролями схемы: роли
 * на разных версиях Android и стилях палитры (Vibrant, Expressive, спецификация 2025) дают разные тона —
 * `primaryContainer` бывает то тёмным, то «неоновым». Насыщенность ограничена, тона фиксированы, поэтому
 * пары «цвет / цвет на нём» всегда контрастны, а светлая и тёмная темы явно различаются.
 *
 * Светлая: тёмная насыщенная панель (тон 20), светлые элементы (тон 92), кнопки тона 40, фон меню тона 40.
 * Тёмная: тёмная панель (тон 17), глубокие элементы (тон 30) со светлым текстом, светлая «таблетка» (тон 88),
 * почти чёрный фон меню с оттенком акцента.
 */
@Immutable
class AccentColors(
    /** Насыщенная панель: док, блок поиска, меню стола. */
    val panel: Color,
    val onPanel: Color,
    /** Строка поиска, кнопки меню стола. */
    val field: Color,
    val onField: Color,
    /** «Таблетка» с кнопками справа в строке поиска, счётчик уведомлений. */
    val pill: Color,
    val onPill: Color,
    /** Фон меню приложений и подписи на нём. */
    val sheet: Color,
    val onSheet: Color,
    /** Неактивная кнопка группы «Личные / Рабочие». */
    val segment: Color,
    val onSegment: Color,
    /** Ползунок быстрой прокрутки и «капля» с буквой — на фоне [sheet]. */
    val thumb: Color,
    val onThumb: Color,
    val dark: Boolean,
)

@Composable
fun accentColors(): AccentColors {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.5f
    val primary = cs.primary
    return remember(primary, dark) { accentColorsFor(primary, dark) }
}

/** То же без Compose: для расчёта вида значков вне композиции. */
fun accentColorsFor(primary: Color, dark: Boolean): AccentColors {
    val seed = Hct.fromInt(primary.toArgb())
    val hue = seed.hue
    // Насыщенность ограничена: при палитрах Vibrant/Expressive крупные площади иначе «светятся»
    val c = seed.chroma.coerceAtMost(40.0)
    fun t(tone: Int, chroma: Double = c) = Color(Hct.from(hue, chroma, tone.toDouble()).toInt())
    return if (dark) AccentColors(
        panel = t(16, c * 0.5), onPanel = t(92, c * 0.3),
        field = t(46), onField = t(96, c * 0.2),
        pill = t(86, c * 0.5), onPill = t(15),
        sheet = t(7, c * 0.35), onSheet = t(92, c * 0.2),
        segment = t(26, c * 0.6), onSegment = t(90, c * 0.3),
        thumb = t(76, c * 0.7), onThumb = t(15),
        dark = true,
    ) else AccentColors(
        panel = t(20), onPanel = t(92, c * 0.6),
        field = t(92, c * 0.6), onField = t(15),
        pill = t(40), onPill = Color.White,
        sheet = t(40), onSheet = Color.White,
        segment = t(85, c * 0.6), onSegment = t(15),
        thumb = t(92, c * 0.6), onThumb = t(20),
        dark = false,
    )
}

/**
 * Плашка и рисунок тематических значков в режимах «Как док» (1) и «Как поиск» (2). Рисунок — не белый, а тон
 * акцента (Monet) с насыщенностью цвета, контрастный плашке: светлый на тёмной и тёмный на светлой.
 */
fun themedIconPair(primary: Color, dark: Boolean, mode: Int): Pair<Color, Color> {
    val a = accentColorsFor(primary, dark)
    val seed = Hct.fromInt(primary.toArgb())
    val c = seed.chroma.coerceAtMost(48.0)
    fun t(tone: Double) = Color(Hct.from(seed.hue, c, tone).toInt())
    return if (mode == 1) a.panel to t(78.0) else a.field to (if (dark) t(88.0) else t(32.0))
}

/**
 * Цвет фона тематического значка в режимах «Как док» (1) и «Как поиск» (2) — тот же, что у панели дока
 * и поля поиска. Его же можно выбрать цветом рисунка: в настройках это один и тот же цвет.
 */
fun themedIconColor(primary: Color, dark: Boolean, mode: Int): Color {
    val a = accentColorsFor(primary, dark)
    return if (mode == 1) a.panel else a.field
}

/** Тон акцента для элементов прямо на обоях: тёмный (25) на светлых обоях, светлый (88) на тёмных. */
@Composable
fun accentInk(lightBackground: Boolean): Color {
    val primary = MaterialTheme.colorScheme.primary
    return remember(primary, lightBackground) {
        val seed = Hct.fromInt(primary.toArgb())
        val c = seed.chroma.coerceAtMost(40.0)
        Color(Hct.from(seed.hue, if (lightBackground) c else c * 0.7, if (lightBackground) 25.0 else 88.0).toInt())
    }
}
