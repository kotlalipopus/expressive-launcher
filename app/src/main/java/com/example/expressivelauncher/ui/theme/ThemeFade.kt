package com.example.expressivelauncher.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Кросс-фейд при смене темы: перед применением снимается кадр со старой темой, затем тема меняется,
 * а снимок поверх растворяется. Так плавно меняется всё сразу — цвета, стекло, формы, шрифты, —
 * без двух копий интерфейса в композиции.
 *
 * Снимок пишется только по запросу (два кадра), в обычное время корень рисуется напрямую.
 * Переключения выстраиваются в очередь: изменение применяется всегда, даже если следующее пришло раньше,
 * чем закончился снимок. Снимок включает и предыдущий растворяющийся снимок, поэтому при быстром
 * переключении по кругу картинка не прыгает.
 */
@Stable
class ThemeFade internal constructor(private val scope: CoroutineScope, internal val layer: GraphicsLayer) {
    internal var capture by mutableStateOf(false)
    internal var overlay by mutableStateOf<ImageBitmap?>(null)
    internal val alpha = Animatable(0f)
    private val mutex = Mutex()
    private var fadeJob: Job? = null

    /** Применяет [apply] (обычно запись настроек) с кросс-фейдом. [durationMs] — длительность растворения. */
    fun switch(durationMs: Int = 320, apply: () -> Unit) {
        scope.launch {
            mutex.withLock {
                var shot: ImageBitmap? = null
                capture = true
                try {
                    // Первый кадр записывает содержимое в слой, второй гарантирует, что запись завершена
                    withFrameNanos { }
                    withFrameNanos { }
                    shot = runCatching { layer.toImageBitmap() }.getOrNull()
                } finally {
                    capture = false
                    apply()
                }
                if (shot == null || durationMs <= 0) return@withLock
                fadeJob?.cancel()
                overlay = shot
                fadeJob = scope.launch {
                    try {
                        alpha.snapTo(1f)
                        alpha.animateTo(0f, tween(durationMs))
                    } finally {
                        // Снимок убирается и при отмене: следующий уже лежит в overlay
                        if (overlay === shot) overlay = null
                    }
                }
            }
        }
    }
}

@Composable
fun rememberThemeFade(): ThemeFade {
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    return remember(scope, layer) { ThemeFade(scope, layer) }
}

/** null — смена применяется сразу (например, в превью). */
val LocalThemeFade = staticCompositionLocalOf<ThemeFade?> { null }

/** Корень, который снимается для кросс-фейда. Растворяющийся снимок рисуется внутри него же. */
fun Modifier.themeFadeSource(fade: ThemeFade): Modifier = drawWithContent {
    if (fade.capture) {
        fade.layer.record { this@drawWithContent.drawContent() }
        drawLayer(fade.layer)
    } else {
        drawContent()
    }
}

/** Растворяющийся снимок старой темы; кладётся последним в корень. Касания не перехватывает. */
@Composable
fun ThemeFadeOverlay(fade: ThemeFade) {
    val shot = fade.overlay ?: return
    Canvas(Modifier.fillMaxSize()) {
        drawImage(shot, alpha = fade.alpha.value.coerceIn(0f, 1f))
    }
}

/**
 * Цветовая схема, которая плавно перетекает к [target]. Нужна там, где снимок снять заранее нельзя:
 * система переключила светлую/тёмную тему, сменились обои.
 */
@Composable
fun animatedScheme(target: ColorScheme, durationMs: Int): ColorScheme {
    val from = remember { mutableStateOf(target) }
    val to = remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target === to.value) return@LaunchedEffect
        // Начинаем с того, что на экране сейчас, даже если прошлый переход не закончился
        from.value = lerpScheme(from.value, to.value, progress.value)
        to.value = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs))
    }
    val p = progress.value
    return if (p >= 1f) to.value else lerpScheme(from.value, to.value, p)
}

private fun lerpScheme(a: ColorScheme, b: ColorScheme, t: Float): ColorScheme {
    if (t <= 0f) return a
    if (t >= 1f) return b
    return b.copy(
        primary = lerp(a.primary, b.primary, t),
        onPrimary = lerp(a.onPrimary, b.onPrimary, t),
        primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
        onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, t),
        inversePrimary = lerp(a.inversePrimary, b.inversePrimary, t),
        secondary = lerp(a.secondary, b.secondary, t),
        onSecondary = lerp(a.onSecondary, b.onSecondary, t),
        secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, t),
        onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, t),
        tertiary = lerp(a.tertiary, b.tertiary, t),
        onTertiary = lerp(a.onTertiary, b.onTertiary, t),
        tertiaryContainer = lerp(a.tertiaryContainer, b.tertiaryContainer, t),
        onTertiaryContainer = lerp(a.onTertiaryContainer, b.onTertiaryContainer, t),
        background = lerp(a.background, b.background, t),
        onBackground = lerp(a.onBackground, b.onBackground, t),
        surface = lerp(a.surface, b.surface, t),
        onSurface = lerp(a.onSurface, b.onSurface, t),
        surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, t),
        onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
        surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
        inverseSurface = lerp(a.inverseSurface, b.inverseSurface, t),
        inverseOnSurface = lerp(a.inverseOnSurface, b.inverseOnSurface, t),
        error = lerp(a.error, b.error, t),
        onError = lerp(a.onError, b.onError, t),
        errorContainer = lerp(a.errorContainer, b.errorContainer, t),
        onErrorContainer = lerp(a.onErrorContainer, b.onErrorContainer, t),
        outline = lerp(a.outline, b.outline, t),
        outlineVariant = lerp(a.outlineVariant, b.outlineVariant, t),
        scrim = lerp(a.scrim, b.scrim, t),
        surfaceBright = lerp(a.surfaceBright, b.surfaceBright, t),
        surfaceDim = lerp(a.surfaceDim, b.surfaceDim, t),
        surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, t),
        surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
        surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
        surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, t),
        surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
    )
}

/** Применить изменение темы с кросс-фейдом; без источника — сразу. */
@Composable
fun rememberThemeSwitch(): (apply: () -> Unit) -> Unit {
    val fade = LocalThemeFade.current
    return remember(fade) { { apply -> if (fade != null) fade.switch(apply = apply) else apply() } }
}
