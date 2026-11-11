package com.example.expressivelauncher.ui.components

import io.github.fletchmckee.liquid.liquid

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.gestures.awaitEachGesture

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.theme.LocalThemeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Размытые обои для стекла. Размываются ОДИН раз в фоновом потоке (при смене обоев, размера экрана или силы
 * размытия) и кэшируются аппаратным растром. Каждая стеклянная панель рисует свой кусок этого растра со смещением
 * по своей позиции на экране: на кадре нет ни одного прохода размытия и ни одной записи слоя.
 *
 * Раньше каждая панель размывала RenderEffect'ом и обои, и снимок всего стола, который ради этого каждый кадр
 * записывался в отдельный слой: в трассе ~4000 проходов размытия за 16 секунд листания.
 */
object WallpaperBlur {
    /** Готовый размытый растр и его геометрия; null — ещё не готов или стекло не используется. */
    class Frame(
        val image: ImageBitmap,
        /** Сколько пикселей растра на пиксель экрана. */
        val scale: Float,
        /** Ширина обоев в пикселях экрана: шире экрана, если обои листаются вместе со страницами. */
        val wallWidth: Float,
    )

    var frame by mutableStateOf<Frame?>(null)
        private set

    /**
     * Почти чёткие обои в половинном разрешении — то, что видно сквозь жидкое стекло (Liquid Glass).
     * null — картинки обоев нет (живые обои или нет доступа): стекло тогда прозрачное, без подделки
     * из цветов обоев, а толщину стекла выдают кромка и блики.
     */
    var clear by mutableStateOf<Frame?>(null)
        private set

    /** Положение обоев при листании страниц, 0..1: стекло сдвигает свой кусок так же, как система сдвигает обои. */
    var parallax by mutableFloatStateOf(0f)

    private var builtFor: String? = null
    /** Ключ сборки, которая идёт сейчас или уже готова: повторный запрос того же ничего не запускает. */
    private var wantedFor: String? = null
    private var job: kotlinx.coroutines.Job? = null

    /**
     * Сборка живёт в своей области, а не в эффекте Compose. Раньше её отменял перезапуск эффекта (смена обоев
     * перезапускает все эффекты стекла разом), а новый запрос в этот момент отбрасывался как «уже собирается»:
     * стекло и фон под меню навсегда оставались со старыми обоями.
     */
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate)

    /** Во сколько раз обои уменьшаются перед размытием: под размытием детали не видны, а растр в 36 раз меньше. */
    private const val DOWNSCALE = 6

    fun ensure(context: Context, screen: IntSize, strength: Float) {
        if (screen.width < 2 || screen.height < 2) return
        val key = "${WallpaperGlass.key}|${CustomWallpaper.active}|${CustomWallpaper.version}|${screen.width}x${screen.height}|${(strength * 4).roundToInt()}"
        if (key == builtFor || key == wantedFor) return
        // Новые параметры важнее: прежняя сборка отменяется, собирается по последним
        wantedFor = key
        job?.cancel()
        val app = context.applicationContext
        job = scope.launch {
            val built = withContext(Dispatchers.Default) {
                runCatching { build(app, screen, strength) }.onFailure { android.util.Log.w("LiquidGlass", "wallpaper blur failed", it) }.getOrNull()
            }
            if (wantedFor != key) return@launch
            if (built != null) {
                frame = built.first
                clear = built.second
                builtFor = key
            } else {
                wantedFor = null
            }
        }
    }

    private fun build(context: Context, screen: IntSize, strength: Float): Pair<Frame, Frame?> {
        val wm = WallpaperManager.getInstance(context)
        // Картинку обоев система отдаёт только с доступом ко всем файлам и только у статичных обоев.
        // У живых обоев её нет: тогда стекло видит размытый градиент из цветов обоев
        // Свои обои лаунчера важнее системных: именно их видно под стеклом
        val custom = if (CustomWallpaper.active) {
            CustomWallpaper.decode(CustomWallpaper.file(context), screen)?.let { android.graphics.drawable.BitmapDrawable(context.resources, it) }
        } else null
        val drawable = custom ?: if (WallpaperGlass.hasAccess() && wm.wallpaperInfo == null) {
            runCatching { wm.drawable }.getOrNull()
        } else null
        val sw: Int
        val sh: Int
        val wallWidth: Float
        val small: Bitmap
        var sharp: Frame? = null
        if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            // Система вписывает обои по высоте экрана; если они уже экрана — по ширине
            var dh = screen.height.toFloat()
            var dw = drawable.intrinsicWidth * dh / drawable.intrinsicHeight
            if (dw < screen.width) {
                dw = screen.width.toFloat()
                dh = drawable.intrinsicHeight * dw / drawable.intrinsicWidth
            }
            sw = max(1, (dw / DOWNSCALE).roundToInt())
            sh = max(1, (dh / DOWNSCALE).roundToInt())
            wallWidth = dw
            small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, sw, sh)
            drawable.draw(Canvas(small))
            // Для жидкого стекла: половинное разрешение и едва заметное смягчение — сквозь стекло всё видно чётко
            val cw = max(1, (dw / CLEAR_DOWNSCALE).roundToInt())
            val ch = max(1, (dh / CLEAR_DOWNSCALE).roundToInt())
            val clearBitmap = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, cw, ch)
            drawable.draw(Canvas(clearBitmap))
            val soft = (strength * 1.5f).roundToInt().coerceIn(0, 4)
            if (soft > 0) BoxBlur.blur(clearBitmap, soft)
            val clearReady = clearBitmap.copy(Bitmap.Config.HARDWARE, false) ?: clearBitmap
            sharp = Frame(clearReady.asImageBitmap(), scale = cw / dw, wallWidth = dw)
        } else {
            sw = max(1, screen.width / DOWNSCALE)
            sh = max(1, screen.height / DOWNSCALE)
            wallWidth = screen.width.toFloat()
            small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            val colors = runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
            val a = colors?.primaryColor?.toArgb() ?: 0xFF3A4A5A.toInt()
            val b = colors?.secondaryColor?.toArgb() ?: a
            val c = colors?.tertiaryColor?.toArgb() ?: b
            Canvas(small).drawPaint(Paint().apply {
                shader = LinearGradient(0f, 0f, sw * 0.4f, sh.toFloat(), intArrayOf(b, a, c), null, Shader.TileMode.CLAMP)
            })
        }
        val radius = (9f * strength).roundToInt().coerceIn(3, 22)
        BoxBlur.blur(small, radius)
        val ready = small.copy(Bitmap.Config.HARDWARE, false) ?: small
        android.util.Log.i("LiquidGlass", "wallpaper blurred: ${sw}x$sh r=$radius source=${if (drawable != null) "image" else "colors"}")
        return Frame(ready.asImageBitmap(), scale = sw / wallWidth, wallWidth = wallWidth) to sharp
    }

    /** Разрешение обоев под жидким стеклом: половина экрана — чётко, а памяти вчетверо меньше. */
    private const val CLEAR_DOWNSCALE = 2

    fun invalidate() {
        builtFor = null
        wantedFor = null
    }
}

/** Тройное box-размытие (по виду близко к гауссову) в фоновом потоке, на маленьком растре — миллисекунды. */
private object BoxBlur {
    fun blur(bitmap: Bitmap, radius: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) {
            pass(px, tmp, w, h, radius, horizontal = true)
            pass(tmp, px, w, h, radius, horizontal = false)
        }
        bitmap.setPixels(px, 0, w, 0, 0, w, h)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (line in 0 until lines) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            fun at(i: Int): Int {
                val k = i.coerceIn(0, len - 1)
                return if (horizontal) src[line * w + k] else src[k * w + line]
            }
            for (i in -r..r) {
                val c = at(i)
                sa += c ushr 24; sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            for (i in 0 until len) {
                val out = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                if (horizontal) dst[line * w + i] = out else dst[i * w + line] = out
                val add = at(i + r + 1)
                val sub = at(i - r)
                sa += (add ushr 24) - (sub ushr 24)
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}

/**
 * Жидкое стекло (Android 13+, AGSL), как в iOS 26. Сквозь стекло обои видны ЧЁТКО — это не матовая панель.
 *  - центр чуть увеличивает то, что под ним (zoom < 1);
 *  - у кромки картинка преломляется: берётся снаружи панели, по круговому профилю толстого стекла;
 *  - преломление разлагает свет: красный, зелёный и синий сдвинуты по-разному — радужная кромка;
 *  - блики только на углах: яркий у верхнего-левого, слабее у нижнего-правого, к серединам сторон гаснут.
 * Без картинки обоев (живые обои) стекло прозрачное: системные обои видны сквозь окно лаунчера как есть,
 * а толщину выдают кромка, лёгкая тень с дальней от света стороны и блики.
 */
private const val LIQUID_AGSL = """
uniform shader image;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float bendPx;
uniform float zoom;
uniform float chroma;
uniform float spec;
uniform float useImage;
uniform float4 tint;
uniform float dp;
uniform shader under;
uniform float useUnder;
uniform float sheen;
uniform float overlay;

float sdRound(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// Что под стеклом в точке x: снимок стола поверх обоев (оба премультиплицированные)
half4 back(float2 x) {
    half4 c = image.eval(x);
    if (useUnder > 0.5) c = c + under.eval(x) * (1.0 - c.a);
    return c;
}

half4 main(float2 fc) {
    float2 hb = size * 0.5;
    float2 p = fc - hb;
    float r = min(radius, min(hb.x, hb.y));
    float2 q = abs(p) - hb + r;
    float2 qm = max(q, 0.0);
    float d = min(max(q.x, q.y), 0.0) + length(qm) - r;
    // Глубже полосы преломления — ровное стекло: одна выборка, без расчёта кромки. Это почти вся площадь панели,
    // поэтому шейдер стоит почти как обычная картинка
    if (d < -band - 2.0 * dp) {
        float top0 = clamp(0.5 - p.y / max(size.y, 1.0), 0.0, 1.0);
        float lift = sheen * 0.07 * top0 * top0;
        // Слой поверх своей подложки (карточки, пункты): внутри только мягкий свет, подложка и текст видны
        if (overlay > 0.5) return half4(half3(lift), half(lift));
        if (useImage > 0.5) {
            half4 c = back(hb + p * zoom);
            float ca = float(c.a) * (1.0 - tint.a) + tint.a;
            float oa = clamp(ca + lift, 0.0, 1.0);
            return half4(half3(min(float3(c.rgb) * (1.0 - tint.a) + tint.rgb * tint.a + lift, float3(oa))), half(oa));
        }
        float oa = clamp(tint.a + lift, 0.0, 1.0);
        return half4(half3(min(tint.rgb * tint.a + lift, float3(oa))), half(oa));
    }
    // Нормаль к кромке — формулой, а не четырьмя лишними расчётами расстояния
    float2 sg = float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
    float2 n;
    if (qm.x + qm.y > 0.0) n = normalize(qm) * sg;
    else n = q.x > q.y ? float2(sg.x, 0.0) : float2(0.0, sg.y);
    float t = clamp(1.0 + d / band, 0.0, 1.0);
    float bend = 1.0 - sqrt(max(1.0 - t * t, 0.0));
    float facing = dot(n, float2(-0.7071, -0.7071));
    float3 col;
    float a;
    if (useImage > 0.5) {
        float2 base = hb + p * zoom;
        float2 off = n * bend * bendPx;
        half4 mid = back(base + off);
        float3 img = float3(
            back(base + off * (1.0 + chroma)).r,
            mid.g,
            back(base + off * (1.0 - chroma)).b);
        img *= 1.0 + 0.12 * bend * facing;
        if (overlay > 0.5) {
            // Поверх подложки фон не преломляется: размытый фон у кромки выглядел грязным размазанным контуром.
            // Стекло здесь — чёткая грань, блики и свет (ниже)
            a = 0.0;
            col = float3(0.0);
        } else {
        // Картинка в премультиплицированном виде: под стеклом могут быть прозрачные места (живые обои)
        float ia = mid.a;
        a = ia * (1.0 - tint.a) + tint.a;
        col = (img * (1.0 - tint.a) + tint.rgb * tint.a) / max(a, 0.0001);
        }
    } else {
        float shade = bend * max(-facing, 0.0);
        if (overlay > 0.5) {
            a = shade * 0.25;
            col = float3(0.0);
        } else {
            a = clamp(tint.a + bend * 0.10 + shade * 0.10, 0.0, 1.0);
            col = mix(tint.rgb, float3(0.0), clamp(shade * 0.7, 0.0, 1.0));
        }
    }
    float rim = clamp(1.0 + d / (1.6 * dp), 0.0, 1.0);
    float corner = pow(max(facing, 0.0), 5.0) + 0.65 * pow(max(-facing, 0.0), 5.0);
    float glow = spec * (rim * (0.06 + 1.3 * corner) + bend * 0.32 * pow(max(facing, 0.0), 3.0)
        + bend * 0.14 * pow(max(-facing, 0.0), 3.0));
    // Мягкий свет сверху у матовых панелей: стекло «ловит» свет верхней частью
    float top = clamp(0.5 - p.y / max(size.y, 1.0), 0.0, 1.0);
    glow += sheen * 0.07 * top * top;
    // Слой поверх подложки: чёткая светлая грань по всему краю и лёгкое свечение толщи стекла у кромки
    if (overlay > 0.5) glow += spec * (rim * 0.22 + bend * 0.06 * (0.6 + 0.4 * facing));
    float outA = clamp(a + glow, 0.0, 1.0);
    float3 outC = min(col * a + float3(glow), float3(outA));
    return half4(half3(outC), half(outA));
}
"""

/** Позиция панели в окне: меняется редко, читается при рисовании. */
@Stable
private class GlassPosition {
    var offset by mutableStateOf(Offset.Zero)
}

/**
 * Поверхность в стиле текущей темы — единая для дока, папок, меню, поиска, виджетов, настроек.
 *  - Liquid Glass: прозрачное жидкое стекло ([LIQUID_AGSL]): чёткие обои с увеличением, преломлением
 *    и радужной кромкой, блики только на углах. Тонировка — едва заметная, только для читаемости.
 *  - Material 3 Expressive: матовая тональная панель на сильно размытых обоях, без бликов.
 *  - Material You Lite: сплошная непрозрачная поверхность, без размытия и полупрозрачности.
 * [overBlur] — под панелью уже размытый слой (открытая папка, меню приложений, настройки) или другое стекло:
 *  обои не подкладываются, видно то, что под панелью.
 * [color] — базовый цвет панели в темах Expressive и Lite; [alpha] — его плотность в Expressive.
 */
@Composable
fun LiquidSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    alpha: Float = 0.78f,
    overBlur: Boolean = false,
    shadow: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    /** Касания не проходят сквозь панель к тому, что под ней (как у Surface). */
    blockTouches: Boolean = true,
    /**
     * Матовое стекло для панелей с текстом (меню, плитки, всплывающие окна), как меню в iOS 26: под ним
     * размытые обои и плотнее тонировка, иначе сквозь прозрачное стекло текст сливается со значками под ним.
     */
    frosted: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .liquidGlass(shape, color, alpha, overBlur, shadow, frosted)
            // Содержимое не обрезается: вложенная жидкая кнопка (строка поиска в доке), когда её тянут,
            // выходит за край панели поверх интерфейса. Нажатие обрезается по форме внутри кнопки
            .then(if (blockTouches) Modifier.pointerInput(Unit) { } else Modifier),
        // Как у Surface: содержимое растягивается до размеров панели. Без этого строка поиска прилипала к верху
        // своей «таблетки», а плитки меню выравнивались каждая по-своему
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/**
 * Фон поверхности в стиле темы — то же стекло, что у [LiquidSurface], но модификатором: им становятся
 * секции и карточки настроек, строка поиска меню приложений, карточки онбординга (через [glassEdge]).
 * Содержимое не обрезается — это делает тот, кто вызывает.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    alpha: Float = 0.78f,
    overBlur: Boolean = false,
    shadow: Boolean = true,
    frosted: Boolean = false,
    /** Стекло поверх своей подложки элемента: кромка, преломление у края и блики, а середина прозрачна. */
    overlay: Boolean = false,
): Modifier {
    val spec = LocalThemeSpec.current
    val settings = LocalSettings.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glass = spec.glass
    // Настоящее жидкое стекло библиотеки liquid: преломляет реальные пиксели под собой (обои, а над столом — и стол).
    // Слой поверх своей подложки (карточки, кнопки) остаётся на своём шейдере — ему нужна только кромка
    val overHome = LocalBackdrop.current != null
    // Вложенное прозрачное стекло (строка поиска в доке, плитки в меню, чипсы часов) уже лежит на стекле
    // библиотеки — ему хватает лёгкого шейдера по картинке обоев, без своего слоя-эффекта на каждом кадре
    val nestedClear = overBlur && !frosted
    // Над столом (меню, папки) — снимок стола один раз и свой шейдер: библиотека на каждом кадре перерисовывала
    // в каждую панель меню весь стол с виджетами — пики до 60 мс на кадр
    val libState = if (glass && !overlay && !nestedClear && !overHome && Build.VERSION.SDK_INT >= 33) LocalWallLiquid.current else null
    if (libState != null) return libraryGlass(libState, shape, color, frosted, overBlur, dark, settings.glassOpacity, settings.glassHighlightStrength)
    if (!spec.lite) {
        val context = LocalContext.current
        val screen = LocalWindowInfo.current.containerSize
        LaunchedEffect(screen, settings.blurStrength, WallpaperGlass.key) { WallpaperBlur.ensure(context, screen, settings.blurStrength) }
    }
    val position = remember { GlassPosition() }
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    // Есть ли настоящая картинка обоев (а не градиент из их цветов при живых обоях)
    val realWallpaper = WallpaperBlur.clear != null
    val liquid = glass && Build.VERSION.SDK_INT >= 33
    // Над столом (меню, папка, меню обоев): стекло преломляет сам стол — его снимок делается один раз при
    // открытии панели и дальше рисуется как обычная картинка. Раньше каждая панель на каждом кадре заново
    // рисовала весь стол в свой слой и пропускала через эффект — медиана кадра в меню доходила до 15 мс
    val homeBackdrop = if (liquid) LocalBackdrop.current else null
    if (homeBackdrop != null) {
        androidx.compose.runtime.DisposableEffect(homeBackdrop) {
            homeBackdrop.attach()
            onDispose { homeBackdrop.detach() }
        }
        LaunchedEffect(homeBackdrop, homeBackdrop.recorded) { homeBackdrop.captureIfNeeded() }
    }
    val snapshot = homeBackdrop?.snapshot
    // Тонировка стекла: у прозрачных панелей — только для читаемости; у матовых — плотнее, а без картинки
    // обоев (живые обои) ещё плотнее: под панелью тогда видно значки и виджеты стола
    val tintAlpha = when {
        // Без картинки обоев (живые обои) линзе нечего преломлять — панели с текстом плотные
        frosted && !realWallpaper -> 0.84f
        // Меню над столом: прозрачное стекло, значки и виджеты под ним видны и изгибаются линзой;
        // тонировки ровно столько, чтобы читался текст
        frosted && homeBackdrop != null -> 0.42f
        frosted -> 0.58f
        homeBackdrop != null -> (settings.glassOpacity * 0.3f).coerceIn(0.05f, 0.45f)
        overBlur -> (settings.glassOpacity * 0.55f).coerceIn(0.03f, 0.5f)
        else -> (settings.glassOpacity * 0.3f).coerceIn(0.03f, 0.5f)
    }
    // Матовые панели тонируются цветом поверхности темы (глубокий тональный оттенок, а не серая муть);
    // прозрачное стекло — почти бесцветным
    // Матовые панели тонируются цветом своего контейнера: цвета текста на них рассчитаны именно на него
    val glassTint = (if (frosted) color else if (dark) Color(0xFF0C0D10) else Color.White).copy(alpha = tintAlpha)
    val fill: Color = when {
        spec.lite -> color
        glass -> glassTint
        else -> color.copy(alpha = alpha)
    }
    val strength = settings.glassHighlightStrength
    // Обои под панелью:
    //  - стекло над столом — чёткие обои под снимком стола;
    //  - матовое стекло поверх размытого фона (настройки, меню приложений, выбор виджетов) — готовая размытая копия;
    //  - вложенное прозрачное стекло (строка поиска в доке) и док — чёткие обои;
    //  - Expressive — размытые обои, кроме вложенных панелей
    val frame: WallpaperBlur.Frame? = when {
        spec.lite -> null
        !glass -> if (overBlur) null else WallpaperBlur.frame
        homeBackdrop != null -> WallpaperBlur.clear
        frosted -> if (realWallpaper) WallpaperBlur.frame else null
        else -> WallpaperBlur.clear
    }
    val zoom = when {
        frosted -> 1f
        // Над столом — заметное увеличение: значки под меню выглядят как сквозь толстую линзу
        homeBackdrop != null -> 0.9f
        // Вложенное прозрачное стекло — тоже линза, чуть слабее, чем у панели под ним
        overBlur -> 0.92f
        // Заметное увеличение всего, что под стеклом, — как у толстой линзы
        else -> 0.86f
    }
    // Матовые панели с текстом (настройки, меню, плитки) — спокойное дорогое стекло: без радужной кромки,
    // преломление только тонкой полосой у края, мягкий свет сверху. Радуга и сильная линза — у дока, поиска, часов
    val calm = frosted && homeBackdrop == null
    return this
        .then(if (frame != null || homeBackdrop != null) Modifier.onGloballyPositioned { position.offset = it.positionInWindow() } else Modifier)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val rounded = (outline as? Outline.Rounded)?.roundRect
            val radius = rounded?.topLeftCornerRadius?.x ?: 0f
            val path = (outline as? Outline.Generic)?.path?.asAndroidPath()
                ?: androidx.compose.ui.graphics.Path().apply { addOutline(outline) }.asAndroidPath()
            val rect = RectF(0f, 0f, size.width, size.height)
            fun android.graphics.Canvas.shape(paint: Paint) {
                if (rounded != null) drawRoundRect(rect, radius, radius, paint) else drawPath(path, paint)
            }
            val stroke = 1.dp.toPx()
            val inner = RectF(stroke / 2f, stroke / 2f, size.width - stroke / 2f, size.height - stroke / 2f)
            val shadowH = 8.dp.toPx()
            val shadowPaint = if (glass && shadow && !overBlur) Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, size.height, 0f, size.height + shadowH,
                    android.graphics.Color.argb(if (dark) 60 else 28, 0, 0, 0), 0, Shader.TileMode.CLAMP)
            } else null
            val shadowRect = RectF(radius * 0.5f, size.height - shadowH * 0.2f, size.width - radius * 0.5f, size.height + shadowH)
            val wallMatrix = Matrix()
            val wallShader = frame?.let { BitmapShader(it.image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            val snapMatrix = Matrix()
            val snapShader = snapshot?.let { BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            // Верхняя картинка — снимок стола, нижняя — обои; без снимка стола обои становятся верхней
            val top: Shader? = snapShader ?: wallShader
            val under: Shader? = if (snapShader != null) wallShader else null
            // Жидкое стекло одним шейдером: картинка, преломление, тонировка, кромка и блики — без промежуточных слоёв
            val liquidShader = if (liquid && Build.VERSION.SDK_INT >= 33) {
                RuntimeShader(LIQUID_AGSL).apply {
                    val empty = LinearGradient(0f, 0f, 1f, 0f, 0, 0, Shader.TileMode.CLAMP)
                    setInputShader("image", top ?: empty)
                    setInputShader("under", under ?: empty)
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("radius", radius)
                    val half = min(size.width, size.height) / 2f
                    // Линза на всю панель: полоса преломления почти до середины, а не тонкая кромка
                    setFloatUniform("band", (if (calm) min(18.dp.toPx(), half * 0.9f) else min(48.dp.toPx(), half * 0.95f)).coerceAtLeast(1f))
                    setFloatUniform("bendPx", if (calm) min(4.dp.toPx(), half * 0.7f) else min(26.dp.toPx(), half * 0.85f))
                    setFloatUniform("zoom", zoom)
                    setFloatUniform("chroma", if (calm) 0f else 0.5f)
                    setFloatUniform("spec", (strength * if (calm) 1.1f else 1f).coerceIn(0f, 1.5f))
                    setFloatUniform("overlay", if (overlay) 1f else 0f)
                    setFloatUniform("sheen", if (calm) 1f else 0f)
                    setFloatUniform("useImage", if (top != null) 1f else 0f)
                    setFloatUniform("useUnder", if (under != null) 1f else 0f)
                    setFloatUniform("tint", fill.red, fill.green, fill.blue, fill.alpha)
                    setFloatUniform("dp", 1.dp.toPx())
                }
            } else null
            val liquidPaint = liquidShader?.let { Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = it } }
            // Без AGSL (Android 12 и ниже) и в Expressive: картинка и заливка, у стекла — кромка с бликами на углах
            val imagePaint = if (liquidPaint == null) wallShader?.let { s -> Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { shader = s } } else null
            val fillPaint = if (liquidPaint == null) Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fill.toArgb() } else null
            val rimPaint = if (glass && liquidPaint == null) Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                shader = Specular.cornerRim(size.width, size.height, strength, rim = if (dark) 0.05f else 0.1f)
            } else null
            val glossPaint = if (glass && liquidPaint == null) Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = Specular.innerGloss(size.width, size.height, strength) } else null
            var lastX = Float.NaN
            var lastY = Float.NaN
            var lastShift = Float.NaN
            onDrawWithContent {
                drawIntoCanvas { cv ->
                    val c = cv.nativeCanvas
                    shadowPaint?.let { c.drawRoundRect(shadowRect, radius, radius, it) }
                    val pos = position.offset
                    val shift = frame?.let { WallpaperBlur.parallax * (it.wallWidth - screenWidth).coerceAtLeast(0f) } ?: 0f
                    // Матрицы обновляются только при сдвиге: на неподвижном столе кадр ничего не пересчитывает
                    if (pos.x != lastX || pos.y != lastY || shift != lastShift) {
                        lastX = pos.x
                        lastY = pos.y
                        lastShift = shift
                        if (wallShader != null && frame != null) {
                            // Пиксель растра b ↔ точка панели p: b = (p + позиция + сдвиг листания) · scale
                            wallMatrix.setScale(1f / frame.scale, 1f / frame.scale)
                            wallMatrix.postTranslate(-(pos.x + shift), -pos.y)
                            wallShader.setLocalMatrix(wallMatrix)
                        }
                        if (snapShader != null && homeBackdrop != null) {
                            snapMatrix.setTranslate(homeBackdrop.origin.x - pos.x, homeBackdrop.origin.y - pos.y)
                            snapShader.setLocalMatrix(snapMatrix)
                        }
                        if (liquidShader != null && Build.VERSION.SDK_INT >= 33) {
                            top?.let { liquidShader.setInputShader("image", it) }
                            under?.let { liquidShader.setInputShader("under", it) }
                        }
                    }
                    if (liquidPaint != null) {
                        // Слой поверх своей подложки рисуется после содержимого (ниже) — над контейнером кнопки
                        if (!overlay) c.shape(liquidPaint)
                    } else {
                        imagePaint?.let { c.shape(it) }
                        fillPaint?.let { c.shape(it) }
                        glossPaint?.let { c.shape(it) }
                    }
                }
                drawContent()
                if (overlay && liquidPaint != null) drawIntoCanvas { cv -> cv.nativeCanvas.shape(liquidPaint) }
                // Кромка поверх содержимого: блики на углах ловят свет и над ним
                if (rimPaint != null) drawIntoCanvas { cv ->
                    if (rounded != null) cv.nativeCanvas.drawRoundRect(inner, radius, radius, rimPaint) else cv.nativeCanvas.drawPath(path, rimPaint)
                }
            }
        }
}

/** Снимок слоя своих обоев лаунчера: его преломляет стекло поверх них (настройки, меню приложений). */
val LocalWallpaperBackdrop = androidx.compose.runtime.compositionLocalOf<Backdrop?> { null }

/**
 * Поверхность меню, плиток, поиска, диалогов: в Liquid Glass — жидкое стекло, в остальных темах — обычный
 * Surface Material 3 (тень — только если тема её допускает). Одна точка, где решается вид поверхности.
 */
@Composable
fun ThemedSurface(
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
    overBlur: Boolean = false,
    /** Тень под стеклом; у вложенных элементов (строка поиска в доке) её быть не должно. */
    shadow: Boolean = true,
    /** Матовое стекло (меню, плитки, окна с текстом); прозрачное — у строки поиска в доке. */
    frosted: Boolean = true,
    /** Expressive: панель матовая — на размытых обоях, а не просто полупрозрачная. Нужно размытие в настройках. */
    blurPanel: Boolean = false,
    onClick: (() -> Unit)? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val spec = LocalThemeSpec.current
    val panelBlur = blurPanel && spec.accentChrome && glassEnabled()
    if (spec.glass || panelBlur) {
        val click = if (onClick != null) Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = onClick,
        ) else Modifier
        LiquidSurface(
            shape, if (onClick != null && spec.glass) modifier.liquidPress() else modifier,
            color = color, alpha = 0.86f, overBlur = overBlur, shadow = shadow && (spec.glass || spec.shadows),
            contentColor = contentColor, frosted = frosted && spec.glass,
        ) {
            Box(Modifier.clip(shape).then(click), propagateMinConstraints = true) { content() }
        }
        return
    }
    val elevation = if (spec.shadows) shadowElevation else 0.dp
    if (onClick != null) {
        androidx.compose.material3.Surface(
            onClick = onClick, shape = shape, color = color, contentColor = contentColor,
            shadowElevation = elevation, interactionSource = interactionSource, modifier = modifier,
        ) { content() }
    } else {
        androidx.compose.material3.Surface(
            shape = shape, color = color, contentColor = contentColor, shadowElevation = elevation, modifier = modifier,
        ) { content() }
    }
}


/**
 * Жидкое нажатие, как у стекла iOS 26: зажатая кнопка чуть раздувается, тянется за пальцем с сопротивлением
 * и вытягивается в сторону движения, а после отпускания пружинит на место с покачиванием.
 * Касания не перехватываются — нажатие и прокрутка работают как обычно.
 */
@Composable
fun Modifier.liquidPress(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state = remember { LiquidPressState() }
    // Позиция пальца пишется в состояние, а одна корутина на жест переносит её в аниматаблы.
    // Раньше на каждое событие движения запускались четыре корутины — по одной на snapTo.
    val follow = remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxShift = with(density) { 14.dp.toPx() }
    val stretchRange = with(density) { 90.dp.toPx() }
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                // Резиновое сопротивление: чем дальше палец, тем слабее тянется
                fun rubber(v: Float) = maxShift * kotlin.math.tanh(v / (maxShift * 4f))
                follow.value = Offset.Zero
                val followJob = scope.launch {
                    androidx.compose.runtime.snapshotFlow { follow.value }.collect { d ->
                        state.dx.snapTo(rubber(d.x))
                        state.dy.snapTo(rubber(d.y))
                        state.sx.snapTo(kotlin.math.tanh(kotlin.math.abs(d.x) / stretchRange))
                        state.sy.snapTo(kotlin.math.tanh(kotlin.math.abs(d.y) / stretchRange))
                    }
                }
                scope.launch { state.press.animateTo(1f, androidx.compose.animation.core.spring(0.55f, 600f)) }
                try {
                    while (true) {
                        val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        follow.value = c.position - down.position
                    }
                } finally {
                    followJob.cancel()
                }
                val back = androidx.compose.animation.core.spring<Float>(0.42f, 380f)
                scope.launch { state.press.animateTo(0f, back) }
                scope.launch { state.dx.animateTo(0f, back) }
                scope.launch { state.dy.animateTo(0f, back) }
                scope.launch { state.sx.animateTo(0f, back) }
                scope.launch { state.sy.animateTo(0f, back) }
            }
        }
        .graphicsLayer {
            val p = state.press.value
            translationX = state.dx.value
            translationY = state.dy.value
            // Вытягивается по направлению движения и сужается поперёк, как капля
            scaleX = 1f + 0.05f * p + 0.14f * state.sx.value - 0.06f * state.sy.value
            scaleY = 1f + 0.05f * p + 0.14f * state.sy.value - 0.06f * state.sx.value
        }
}

@Stable
private class LiquidPressState {
    val press = androidx.compose.animation.core.Animatable(0f)
    val dx = androidx.compose.animation.core.Animatable(0f)
    val dy = androidx.compose.animation.core.Animatable(0f)
    val sx = androidx.compose.animation.core.Animatable(0f)
    val sy = androidx.compose.animation.core.Animatable(0f)
}

/**
 * Кнопка Material в теме Liquid Glass: стеклянная грань с бликами поверх полупрозрачного контейнера и жидкое
 * нажатие. В остальных темах ничего не меняет. Контейнер делается прозрачнее через [glassContainer].
 */
@Composable
fun Modifier.glassControl(shape: Shape = androidx.compose.foundation.shape.CircleShape): Modifier {
    if (!LocalThemeSpec.current.glass || Build.VERSION.SDK_INT < 33) return this
    return this.liquidPress().liquidGlass(shape, overBlur = true, shadow = false, frosted = true, overlay = true)
}

/** Цвет контейнера кнопки: в Liquid Glass — полупрозрачный, сквозь него виден фон. */
@Composable
fun glassContainer(color: Color): Color =
    if (LocalThemeSpec.current.glass && Build.VERSION.SDK_INT >= 33) color.copy(alpha = 0.38f) else color

/** Стеклянный вид кнопок Material в Liquid Glass: `with(GlassControls) { Modifier.control() }`. */
object GlassControls {
    @Composable
    fun Modifier.control(shape: Shape = androidx.compose.foundation.shape.CircleShape): Modifier = this.glassControl(shape)
}


/** Источник пикселей для стекла: обои лаунчера. null — своих обоев нет (живые обои), стекло на своём шейдере. */
val LocalWallLiquid = androidx.compose.runtime.compositionLocalOf<io.github.fletchmckee.liquid.LiquidState?> { null }

/** Источник для стекла поверх стола (меню, папки, панели): обои и сам стол. */
val LocalHomeLiquid = androidx.compose.runtime.compositionLocalOf<io.github.fletchmckee.liquid.LiquidState?> { null }

/**
 * Жидкое стекло библиотеки liquid. Прозрачное стекло (док, поиск, часы, папки) — сильная линза с изгибом,
 * радужным разложением и светлой кромкой. Матовое (настройки, меню, плитки) — то же стекло с размытием
 * и тонировкой цветом контейнера, чтобы читался текст.
 */
@Composable
private fun Modifier.libraryGlass(
    state: io.github.fletchmckee.liquid.LiquidState,
    shape: Shape,
    color: Color,
    frosted: Boolean,
    nested: Boolean,
    dark: Boolean,
    opacity: Float,
    highlight: Float,
): Modifier {
    // Параметры шейдера библиотеки заданы в долях меньшей стороны панели. Одинаковые доли давали у высокого дока
    // сдвиг ~90 dp у кромки (странное сжатие), а у тонкой строки поиска — почти ничего. Поэтому линзу задаём
    // в dp и пересчитываем под размер каждой панели: везде одинаковое, как в iOS, преломление у края
    val density = androidx.compose.ui.platform.LocalDensity.current
    var minDim by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val band = with(density) { (if (frosted) 26.dp else 32.dp).toPx() }
    val shift = with(density) { (if (frosted) 12.dp else if (nested) 14.dp else 18.dp).toPx() }
    val rim = with(density) { 5.dp.toPx() }
    return this
        .onSizeChanged { minDim = kotlin.math.min(it.width, it.height).toFloat() }
        .liquid(state) {
            this.shape = shape
            frost = 0.dp
            val m = minDim.coerceAtLeast(1f)
            // Полоса линзы — не глубже половины панели, чтобы середина оставалась ровной
            refraction = (band / m).coerceIn(0.05f, 0.5f)
            curve = (shift / m).coerceIn(0.02f, 0.45f)
            edge = (rim / m * highlight.coerceIn(0.3f, 1.4f)).coerceIn(0.005f, 0.2f)
            dispersion = if (frosted) 0.3f else 0.45f
            saturation = if (frosted) 1.15f else 1.3f
            contrast = 1.05f
            tint = if (frosted) color.copy(alpha = (opacity * 1.1f).coerceIn(0.35f, 0.55f))
            else (if (dark) Color.Black else Color.White).copy(alpha = (opacity * 0.25f).coerceIn(0.03f, 0.25f))
        }
        // Блики на углах, как у значков, — у всех стеклянных панелей, включая настройки
        .cornerGlints(shape, highlight)
}

/**
 * Блики на углах, как у значков: тонкая кромка, яркая у верхнего-левого угла и слабее у нижнего-правого,
 * к серединам сторон гаснет. Для прозрачного стекла (док, поиск, часы, папки) поверх эффекта библиотеки.
 */
private fun Modifier.cornerGlints(shape: Shape, strength: Float): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: (size.minDimension / 2f)
    val stroke = 1.5.dp.toPx()
    val rect = RectF(stroke / 2f, stroke / 2f, size.width - stroke / 2f, size.height - stroke / 2f)
    val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        shader = Specular.cornerRim(size.width, size.height, (strength * 1.3f).coerceIn(0f, 1.5f), rim = 0f)
    }
    val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = Specular.innerGloss(size.width, size.height, strength * 0.7f) }
    val fill = RectF(0f, 0f, size.width, size.height)
    onDrawWithContent {
        drawIntoCanvas { it.nativeCanvas.drawRoundRect(fill, radius, radius, gloss) }
        drawContent()
        drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, rim) }
    }
}
