package com.example.expressivelauncher.ui.theme

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.data.ColorSource
import com.example.expressivelauncher.data.MotionStyle
import com.example.expressivelauncher.ui.LocalClockStyle
import com.example.expressivelauncher.data.Settings
import com.example.expressivelauncher.data.ThemeMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Стили палитры для своего цвета; порядок важен — в настройках хранится индекс.
 * Первые два сохраняют оттенок акцента; Expressive его сдвигает, поэтому по умолчанию не используется.
 */
val PaletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Fidelity,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
    PaletteStyle.Rainbow,
    PaletteStyle.FruitSalad,
    PaletteStyle.Neutral,
    PaletteStyle.Monochrome,
)

val SeedColors = listOf(
    0xFF6750A4, 0xFF8E4EC6, 0xFFB5338A, 0xFFD32F5B, 0xFFE5484D, 0xFFF76B15,
    0xFFFFA000, 0xFFE2B714, 0xFF8BC34A, 0xFF30A46C, 0xFF12A594, 0xFF00A2C7,
    0xFF0090FF, 0xFF3E63DD, 0xFF5B5BD6, 0xFF795548, 0xFF607D8B, 0xFF1C1B1F,
).map { it.toInt() }

/**
 * Шкала скруглений Material 3 Expressive (4 · 8 · 12 · 16 · 20 · 28 · 32 · 48 dp).
 * [scale] — множитель из настроек: 0.5 — строже, 1.5 — мягче.
 */
fun expressiveShapes(scale: Float) = Shapes(
    extraSmall = RoundedCornerShape((4 * scale).dp),
    small = RoundedCornerShape((8 * scale).dp),
    medium = RoundedCornerShape((12 * scale).dp),
    large = RoundedCornerShape((16 * scale).dp),
    largeIncreased = RoundedCornerShape((20 * scale).dp),
    extraLarge = RoundedCornerShape((28 * scale).dp),
    extraLargeIncreased = RoundedCornerShape((32 * scale).dp),
    extraExtraLarge = RoundedCornerShape((48 * scale).dp),
)

/** Тема Lite: одинаковые умеренные скругления у всех компонентов. */
fun uniformShapes(corner: androidx.compose.ui.unit.Dp) = RoundedCornerShape(corner).let { r ->
    Shapes(
        extraSmall = RoundedCornerShape(corner / 2), small = r, medium = r, large = r,
        largeIncreased = r, extraLarge = r, extraLargeIncreased = r, extraExtraLarge = r,
    )
}

@Composable
fun isDarkFor(settings: Settings): Boolean = when (settings.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LauncherTheme(
    settings: Settings,
    fx: com.example.expressivelauncher.ui.Fx,
    spec: ThemeSpec = LauncherThemes.Expressive,
    content: @Composable () -> Unit,
) {
    val dark = isDarkFor(settings)
    val context = LocalContext.current
    val target = remember(settings.colorSource, settings.seedColor, settings.amoled, settings.paletteStyle, settings.contrast, dark) {
        resolveScheme(context, settings, dark)
    }
    // Смена системной темы, режима или акцента: цвета перетекают, а не перескакивают
    val scheme = animatedScheme(target, if (spec.lite) 160 else 320)
    val filesDir = context.filesDir
    val family = remember(settings.font, settings.customFontRev) { resolveFontFamily(settings.font, filesDir, settings.customFontRev) }
    val clockFamily = remember(settings.clockFontSame, settings.clockFont, family, settings.customFontRev) {
        if (settings.clockFontSame) family else resolveFontFamily(settings.clockFont, filesDir, settings.customFontRev)
    }
    // Свой шрифт с несимметричными метриками «всплывал» над центром строк — поправка базовой линии
    val baseline = remember(settings.font, settings.customFontRev) {
        if (settings.font == com.example.expressivelauncher.data.AppFont.CUSTOM) {
            customFontFile(filesDir, settings.customFontRev).takeIf { it.exists() }?.let(::fontBaselineShift)
        } else null
    }
    val typography = remember(family, settings.fontWeightShift, spec.boldHeadlines, baseline) {
        expressiveTypography(family, settings.fontWeightShift, spec.boldHeadlines, baseline)
    }
    val clockStyle = remember(clockFamily, settings.fontWeightShift) { clockTextStyle(clockFamily, settings.fontWeightShift) }
    val shapes = remember(settings.cornerScale, spec.uniformCorner) {
        spec.uniformCorner?.let { uniformShapes(it) } ?: expressiveShapes(settings.cornerScale)
    }
    val base = LocalDensity.current
    val density = remember(base, settings.fontScale) { Density(base.density, base.fontScale * settings.fontScale) }
    CompositionLocalProvider(LocalDensity provides density, LocalClockStyle provides clockStyle) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = remember(settings.motion, fx.speed, fx.bounce, spec.lite) {
                // Lite: только короткие tween без пружин и перелёта; остальные темы — пружины Material
                if (spec.lite) LiteMotionScheme(fx.speed)
                else TunedMotionScheme(if (settings.motion == MotionStyle.EXPRESSIVE) MotionScheme.expressive() else MotionScheme.standard(), fx.speed, fx.bounce)
            },
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}

/** Схема движения с настройками пользователя: все пружины интерфейса проходят через неё. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class TunedMotionScheme(private val base: MotionScheme, private val speed: Float, private val bounce: Float) : MotionScheme {
    private fun <T> tune(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (spec is SpringSpec<T>) spring(tunedDamping(spec.dampingRatio, bounce), spec.stiffness * speed * speed, spec.visibilityThreshold) else spec
    override fun <T> defaultSpatialSpec() = tune(base.defaultSpatialSpec<T>())
    override fun <T> fastSpatialSpec() = tune(base.fastSpatialSpec<T>())
    override fun <T> slowSpatialSpec() = tune(base.slowSpatialSpec<T>())
    override fun <T> defaultEffectsSpec() = tune(base.defaultEffectsSpec<T>())
    override fun <T> fastEffectsSpec() = tune(base.fastEffectsSpec<T>())
    override fun <T> slowEffectsSpec() = tune(base.slowEffectsSpec<T>())
}

/**
 * Движение темы Material You Lite: короткие fade/scale 150–200 мс, ни одной пружины и ни одного перелёта.
 * Длительности масштабируются системной настройкой скорости анимаций (MotionDurationScale), «Убрать анимации» их обнуляет.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class LiteMotionScheme(speed: Float) : MotionScheme {
    private val fast = (150 / speed).toInt().coerceIn(60, 300)
    private val normal = (200 / speed).toInt().coerceIn(80, 400)
    private val slow = (260 / speed).toInt().coerceIn(100, 500)
    private val easing = androidx.compose.animation.core.FastOutSlowInEasing
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(normal, easing = easing)
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(fast, easing = easing)
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(slow, easing = easing)
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(normal)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(fast)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = androidx.compose.animation.core.tween(slow)
}

/** [bounce]: 0 — совсем без перелёта, 1 — исходная упругость. */
fun tunedDamping(damping: Float, bounce: Float): Float = (1f - (1f - damping) * bounce).coerceIn(0.3f, 1f)

/** Единая точка расчёта схемы: её же использует предпросмотр палитры в настройках. */
fun resolveScheme(context: android.content.Context, settings: Settings, dark: Boolean): ColorScheme {
    val useSystem = settings.colorSource == ColorSource.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        useSystem && dark -> dynamicDarkColorScheme(context).amoled(settings.amoled)
        useSystem -> dynamicLightColorScheme(context)
        else -> dynamicColorScheme(
            seedColor = Color(settings.seedColor),
            isDark = dark,
            isAmoled = settings.amoled && dark,
            style = paletteStyleFor(settings),
            contrastLevel = settings.contrast.toDouble(),
        )
    }
}

/**
 * Стиль палитры для своего цвета. TonalSpot и яркие стили задают основному цвету насыщенность ~36 независимо
 * от выбранного: у тёмных и серых цветов насыщенности почти нет, оттенок у них почти случайный, и чёрный
 * превращался в фиолетовый, коричневый — в розовый. Такие цвета получают нейтральную палитру, а тёмные
 * насыщенные — Fidelity, которая сохраняет насыщенность самого цвета.
 */
fun paletteStyleFor(settings: Settings): PaletteStyle {
    val chosen = PaletteStyles.getOrElse(settings.paletteStyle) { PaletteStyle.TonalSpot }
    if (chosen == PaletteStyle.Neutral || chosen == PaletteStyle.Monochrome) return chosen
    val seed = com.materialkolor.hct.Hct.fromInt(settings.seedColor)
    return when {
        seed.chroma < 8.0 -> PaletteStyle.Monochrome
        seed.chroma < 18.0 -> PaletteStyle.Neutral
        chosen == PaletteStyle.TonalSpot && seed.tone < 45.0 -> PaletteStyle.Fidelity
        else -> chosen
    }
}

/** Выбран свой тёмный цвет: тематические значки получают тёмную плашку и в светлой теме, как сам цвет. */
fun prefersDarkPlates(settings: Settings): Boolean =
    settings.colorSource == ColorSource.CUSTOM && com.materialkolor.hct.Hct.fromInt(settings.seedColor).tone < 45.0

private fun ColorScheme.amoled(enabled: Boolean): ColorScheme =
    if (!enabled) this else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = Color(0xFF202020),
    )
