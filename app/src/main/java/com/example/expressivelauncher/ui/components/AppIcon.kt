package com.example.expressivelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.toPath
import com.example.expressivelauncher.data.AppIcon
import com.example.expressivelauncher.data.Settings
import com.example.expressivelauncher.ui.home.ShapeMorphs
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.IconShape
import com.example.expressivelauncher.data.GlassIconStyle
import com.example.expressivelauncher.data.IconHighlight
import com.example.expressivelauncher.ui.LocalBadges
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.LocalWallpaperLight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconShape.toComposeShape(): Shape =
    when (this) {
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUIRCLE -> RoundedCornerShape(percent = 34)
        IconShape.ROUNDED -> RoundedCornerShape(percent = 22)
        IconShape.COOKIE_9 -> MaterialShapes.Cookie9Sided.toShape()
        IconShape.COOKIE_12 -> MaterialShapes.Cookie12Sided.toShape()
        IconShape.CLOVER_4 -> MaterialShapes.Clover4Leaf.toShape()
        IconShape.FLOWER -> MaterialShapes.Flower.toShape()
        IconShape.SUNNY -> MaterialShapes.Sunny.toShape()
        IconShape.PENTAGON -> MaterialShapes.Pentagon.toShape()
        IconShape.GEM -> MaterialShapes.Gem.toShape()
        IconShape.PILL -> MaterialShapes.Pill.toShape()
        IconShape.PUFFY -> MaterialShapes.Puffy.toShape()
        IconShape.CONTINUOUS -> com.example.expressivelauncher.ui.home.ContinuousSquircle.toShape()
    }

/** Как именно выглядит значок при текущих настройках; один расчёт на стол и на анимацию запуска. */
@Immutable
class IconLook(
    val image: ImageBitmap,
    val background: Color,
    val filter: ColorFilter?,
    val clip: Boolean,
    val accent: Int,
    /** Доля стороны, на которую рисунок отступает от края подложки (логотип на стеклянной плашке). */
    val inset: Float = 0f,
    /** Блики по кромке (тема Liquid Glass). */
    val highlight: IconHighlight = IconHighlight.OFF,
    val highlightStrength: Float = 0f,
    /** Ключ готового растра в [IconBitmaps]: одинаковый вид — один растр на всё приложение. */
    val cacheKey: String,
)

/**
 * [glass] — тема Liquid Glass: стиль и блики берутся из её настроек. Значки из icon pack
 * получают те же блики и стили поверх своей картинки.
 */
fun iconLook(icon: AppIcon?, settings: Settings, colors: ColorScheme, glass: Boolean = false): IconLook? {
    icon ?: return null
    val id = System.identityHashCode(icon)
    val highlight = if (glass) settings.glassIconHighlight else IconHighlight.OFF
    val strength = settings.glassHighlightStrength
    val masked = !(icon.custom && !settings.iconThemeMask)
    val dark = colors.surface.luminance() < 0.5f
    val hl = "$highlight|$strength"
    if (glass && settings.glassIconStyle != GlassIconStyle.NORMAL) {
        val clear = settings.glassIconStyle == GlassIconStyle.CLEAR
        // Прозрачные: цветной логотип на светлом или тёмном стекле. Тонированные: монохромный глиф акцентного
        // цвета на тёмной (в светлой теме — светлой) тонированной плашке, как Tinted в iOS 26
        val plate = when {
            clear && dark -> Color.White.copy(alpha = 0.16f)
            clear -> Color.White.copy(alpha = 0.46f)
            dark -> androidx.compose.ui.graphics.lerp(Color.Black, colors.primary, 0.22f).copy(alpha = 0.9f)
            else -> colors.primaryContainer.copy(alpha = 0.92f)
        }
        val mono = icon.mono
        val glyph = if (dark) colors.primary else colors.onPrimaryContainer
        val image: ImageBitmap
        val filter: ColorFilter?
        val inset: Float
        when {
            clear -> { image = icon.fg ?: icon.image; filter = null; inset = if (icon.fg != null) 0f else 0.16f }
            mono != null -> { image = mono; filter = ColorFilter.tint(glyph); inset = 0f }
            else -> { image = icon.image; filter = duotoneFilter(plate.copy(alpha = 1f), glyph); inset = 0.12f }
        }
        return IconLook(
            image = image, background = plate, filter = filter, clip = true, accent = plate.copy(alpha = 1f).toArgb(),
            inset = inset, highlight = highlight, highlightStrength = strength,
            cacheKey = "g$id|${settings.glassIconStyle}|${plate.toArgb()}|${glyph.toArgb()}|$hl",
        )
    }
    // Тематическими становятся все значки, у которых есть монохромный слой: свой у приложения или синтезированный
    val themed = settings.themedIcons && icon.mono != null
    val duotone = !themed && settings.duotoneIcons
    // Цвета тематических значков — по «фиксированным» тонам палитры (одинаковым в светлой и тёмной теме), как у Pixel:
    // в тёмной теме — тёмная акцентная плашка (тон 30) и светлый рисунок (тон 90), в светлой — наоборот.
    // Контраст тонов 30 и 90 высокий: рисунок хорошо виден. Раньше плашка и рисунок были близких тонов
    val themedBg = if (dark) colors.onPrimaryFixedVariant else colors.primaryFixed
    val themedFg = if (dark) colors.primaryFixed else colors.onPrimaryFixedVariant
    val bg = when {
        themed -> themedBg
        duotone -> colors.primaryContainer
        icon.legacy -> colors.surfaceContainerHighest
        else -> colors.surfaceVariant
    }
    return IconLook(
        image = if (themed) icon.mono!! else icon.image,
        background = bg,
        filter = when {
            themed -> ColorFilter.tint(themedFg)
            duotone -> duotoneFilter(colors.primaryContainer, colors.onPrimaryContainer)
            else -> null
        },
        clip = masked,
        accent = if (themed || duotone) bg.toArgb() else icon.accent,
        highlight = highlight,
        highlightStrength = strength,
        cacheKey = "n$id|$themed|$duotone|${bg.toArgb()}|${themedFg.toArgb()}|${colors.onPrimaryContainer.toArgb()}|$masked|$hl",
    )
}

/** Переводит значок в два цвета темы: тёмные места — в [dark], светлые — в [light]. */
private fun duotoneFilter(dark: Color, light: Color): ColorFilter {
    fun row(d: Float, l: Float): List<Float> {
        val k = l - d
        return listOf(k * 0.3f, k * 0.59f, k * 0.11f, 0f, d * 255f)
    }
    val m = row(dark.red, light.red) + row(dark.green, light.green) + row(dark.blue, light.blue) + listOf(0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m.toFloatArray()))
}

/**
 * Готовые растры значков: один на вид и размер, на всё приложение. Значок каждый кадр рисуется одной
 * картинкой. Раньше он рисовался сложным контуром с шейдером, и Skia на каждом кадре растеризовала маску
 * на процессоре и заново грузила её в видеопамять: в трассе ~1200 загрузок текстур в секунду.
 * Растры аппаратные (Bitmap.createBitmap(Picture)) и Java-кучу не занимают.
 */
object IconBitmaps {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** Один фоновый поток для рендера значков: значки идут по очереди, видеокарта не получает пачку задач разом. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val renderDispatcher = kotlinx.coroutines.Dispatchers.Default.limitedParallelism(1)

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bitmap: ImageBitmap) { cache.put(key, bitmap) }
    fun clear() = cache.evictAll()

    /** Рисует [draw] в аппаратный растр. До Android 9 такого API нет — тогда null и значок рисуется вживую. */
    fun render(w: Int, h: Int, draw: (android.graphics.Canvas) -> Unit): ImageBitmap? {
        if (android.os.Build.VERSION.SDK_INT < 28 || w <= 0 || h <= 0) return null
        val picture = android.graphics.Picture()
        draw(picture.beginRecording(w, h))
        picture.endRecording()
        return runCatching { android.graphics.Bitmap.createBitmap(picture).asImageBitmap() }.getOrNull()
    }
}

/** Кисти одного значка заданного размера: создаются в кэше отрисовки, а не на каждом кадре. */
internal class IconPainter(private val look: IconLook, private val w: Float, private val h: Float, private val stroke: Float) {
    private val bg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = look.background.toArgb() }
    private val image = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        val bitmap = look.image.asAndroidBitmap()
        if (look.clip) {
            shader = android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP).apply {
                setLocalMatrix(android.graphics.Matrix().apply { setScale(w / bitmap.width, h / bitmap.height) })
            }
        }
        colorFilter = look.filter?.asAndroidColorFilter()
    }
    private val rim: android.graphics.Paint? = when (look.highlight) {
        IconHighlight.OFF -> null
        IconHighlight.CORNERS -> android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
            // Только углы: без постоянной линии по контуру, между бликами кромки нет совсем
            shader = Specular.cornerRim(w, h, look.highlightStrength, rim = 0f)
        }
        IconHighlight.FULL -> android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
            color = android.graphics.Color.argb((0.6f * look.highlightStrength * 255f).toInt().coerceIn(0, 255), 255, 255, 255)
        }
    }
    private val gloss: android.graphics.Paint? = if (look.highlight == IconHighlight.CORNERS) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { shader = Specular.innerGloss(w, h, look.highlightStrength) }
    } else null
    private val dst = android.graphics.RectF(0f, 0f, w, h)

    fun draw(c: android.graphics.Canvas, outline: android.graphics.Path) {
        if (!look.clip) {
            // Значок из icon pack без маски: своя форма, блики всё равно по маске темы
            c.drawBitmap(look.image.asAndroidBitmap(), null, dst, image)
        } else {
            c.drawPath(outline, bg)
            if (look.inset > 0f) {
                val k = 1f - 2f * look.inset
                c.save()
                c.scale(k, k, w / 2f, h / 2f)
                c.drawPath(outline, image)
                c.restore()
            } else {
                c.drawPath(outline, image)
            }
            gloss?.let { c.drawPath(outline, it) }
        }
        rim?.let {
            // Обводка целиком внутри значка: контур чуть уменьшается на толщину линии
            val k = 1f - stroke / w
            c.save()
            c.scale(k, k, w / 2f, h / 2f)
            c.drawPath(outline, it)
            c.restore()
        }
    }
}

/**
 * Иконка приложения: растр + выбранная форма + тематические цвета или стиль Liquid Glass.
 * В обычном состоянии рисуется одной готовой картинкой из [IconBitmaps]. Контур рисуется вживую
 * только пока фигура морфится под пальцем.
 * [morph] — прогресс морфинга фигуры под пальцем; читается только при рисовании.
 */
@Composable
fun AppIconImage(
    app: AppInfo,
    size: Dp,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
    morph: (() -> Float)? = null,
) {
    val settings = LocalSettings.current
    val repo = LocalRepo.current
    // Своя форма и размер этого приложения (настройка значка), иначе — общие
    val appStyle = com.example.expressivelauncher.data.AppStyles.of(app.key)
    val shape = (appStyle?.shape ?: settings.iconShape).toComposeShape()
    val styleScale = appStyle?.scale ?: 1f
    val epoch = repo.iconEpoch
    // Состояние привязано к ключу приложения; при смене темы старый значок остаётся, пока не готов новый
    var loaded by remember(app.key) { mutableStateOf(repo.cachedIcon(app)) }
    LaunchedEffect(app.key, epoch) { loaded = repo.cachedIcon(app) ?: repo.icon(app) ?: loaded }
    val icon = loaded
    val colors = MaterialTheme.colorScheme
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    // Тема Lite: формы значков под пальцем не морфятся
    val pressMorph = if (spec.morph) morph else null
    val look = remember(
        icon, settings.themedIcons, settings.duotoneIcons, settings.iconThemeMask, colors, spec.glass,
        settings.glassIconStyle, settings.glassIconHighlight, settings.glassHighlightStrength,
    ) { iconLook(icon, settings, colors, spec.glass) }
    val iconShape = appStyle?.shape ?: settings.iconShape
    val badge = if (showBadge && settings.showBadges) LocalBadges.current.value[app.packageName] ?: 0 else 0
    val density = androidx.compose.ui.platform.LocalDensity.current
    val px = with(density) { size.toPx() }
    val key = look?.let { "${it.cacheKey}|$iconShape|${px.toInt()}x${px.toInt()}" }
    // Готовый растр рендерится в фоне (одним потоком по очереди): при прокрутке меню приложений главный поток
    // не ждёт видеокарту ради каждого нового значка. Пока растра нет, значок рисуется контуром, как раньше
    // Растр хранится вместе со своим ключом вида. Состояние produceState переживает смену ключа, и раньше
    // после смены темы значок на сетке стола видел «растр уже есть» и навсегда оставался в старом стиле
    val rendered by androidx.compose.runtime.produceState(key?.let { k -> IconBitmaps.get(k)?.let { k to it } }, key) {
        val k = key ?: return@produceState
        if (value?.first == k) return@produceState
        IconBitmaps.get(k)?.let { value = k to it; return@produceState }
        val l = look ?: return@produceState
        val bitmap = kotlinx.coroutines.withContext(IconBitmaps.renderDispatcher) {
            IconBitmaps.get(k) ?: run {
                val outline = shape.createOutline(androidx.compose.ui.geometry.Size(px, px), androidx.compose.ui.unit.LayoutDirection.Ltr, density)
                val path = Path().apply { addOutline(outline) }.asAndroidPath()
                val painter = IconPainter(l, px, px, with(density) { 1.dp.toPx() })
                IconBitmaps.render(px.toInt(), px.toInt()) { painter.draw(it, path) }?.also { IconBitmaps.put(k, it) }
            }
        }
        if (bitmap != null) value = k to bitmap
    }
    // Растр от прошлого вида не показывается: пока новый не готов, значок рисуется контуром уже в новом стиле
    val ready = rendered?.takeIf { it.first == key }?.second

    Box(
        modifier.size(size)
            .then(if (styleScale != 1f) Modifier.graphicsLayer { scaleX = styleScale; scaleY = styleScale } else Modifier)
            .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(this@drawWithCache.size, layoutDirection, this@drawWithCache)) }.asAndroidPath()
            val w = this.size.width
            val h = this.size.height
            val l = look
            val painter = l?.let { IconPainter(it, w, h, 1.dp.toPx()) }
            val bitmap = ready
            val morphPath = android.graphics.Path()
            val morphMatrix = android.graphics.Matrix().apply { setScale(w, h) }
            onDrawBehind {
                if (painter == null) return@onDrawBehind
                val m = pressMorph?.invoke() ?: 0f
                if (m <= 0.001f && bitmap != null) {
                    drawImage(bitmap)
                    return@onDrawBehind
                }
                val outline = if (m > 0.001f) {
                    morphPath.rewind()
                    ShapeMorphs.press(iconShape).toPath(m.coerceIn(0f, 1f), morphPath)
                    morphPath.transform(morphMatrix)
                    morphPath
                } else path
                drawIntoCanvas { painter.draw(it.nativeCanvas, outline) }
            }
        },
    ) {
        if (app.isWork) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.34f)
                    .clip(CircleShape)
                    .background(colors.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Work, null, tint = colors.onTertiary, modifier = Modifier.size(size * 0.22f))
            }
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(colors.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 9) "9+" else badge.toString(),
                    color = colors.onError,
                    fontSize = (size.value * 0.16f).sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private val labelShadow = Shadow(Color.Black.copy(alpha = 0.55f), blurRadius = 6f)

@Composable
fun AppLabel(text: String, modifier: Modifier = Modifier, onWallpaper: Boolean = false, lines: Int = 1) {
    val lightWallpaper = LocalWallpaperLight.current
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelMedium.let {
            val scaled = it.copy(fontSize = it.fontSize * LocalSettings.current.labelScale)
            if (onWallpaper && !lightWallpaper) scaled.copy(shadow = labelShadow) else scaled
        },
        color = when {
            !onWallpaper -> MaterialTheme.colorScheme.onSurface
            lightWallpaper -> Color(0xFF1B1B1F)
            else -> Color.White
        },
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}
