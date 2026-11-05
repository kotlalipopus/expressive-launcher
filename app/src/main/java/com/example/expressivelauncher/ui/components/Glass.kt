package com.example.expressivelauncher.ui.components

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.asAndroidPath
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.LocalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Обои, уменьшенные до размера окна: из них собирается «матовое стекло».
 * Картинку обоев Android отдаёт только приложению с доступом ко всем файлам; без него стекло размывает
 * лишь то, что нарисовал сам лаунчер (значки, виджеты, списки).
 */
object WallpaperGlass {
    internal val image = mutableStateOf<ImageBitmap?>(null)
    /** Растёт, когда стоит попробовать загрузить обои ещё раз: сменились обои или выдали доступ. */
    private val generation = mutableIntStateOf(0)
    private var loading = false

    fun invalidate() {
        image.value = null
        generation.intValue++
    }

    /** Без доступа ко всем файлам система не отдаёт картинку обоев. */
    fun hasAccess(): Boolean = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    /** Есть ли доступ сейчас; обновляется при возвращении в лаунчер, например из экрана разрешений. */
    val access = mutableStateOf(hasAccess())

    fun refreshAccess() {
        val now = hasAccess()
        if (now != access.value) {
            access.value = now
            invalidate()
        }
    }

    /** Экран системных настроек, где лаунчеру выдают доступ ко всем файлам. */
    fun requestAccess(context: Context) {
        if (Build.VERSION.SDK_INT < 30) return
        val specific = android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            android.net.Uri.parse("package:" + context.packageName),
        )
        runCatching { context.startActivity(specific) }.onFailure {
            runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }
    }

    internal val key: Int get() = generation.intValue

    /** Основной цвет обоев: его система отдаёт без разрешений, он подкладывается под стекло вместо картинки. */
    val baseColor = mutableStateOf<Color?>(null)

    suspend fun load(context: Context, size: IntSize) {
        if (image.value != null || loading || size.width < 2 || size.height < 2 || !hasAccess()) return
        loading = true
        try {
            // Под размытием детали не видны, а обои рисуются под каждой стеклянной панелью на каждом кадре:
            // половина разрешения экономит и память, и пропускную способность видеопамяти
            val w = (size.width * RESOLUTION_SCALE).toInt().coerceAtLeast(1)
            val h = (size.height * RESOLUTION_SCALE).toInt().coerceAtLeast(1)
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { WallpaperManager.getInstance(context).getDrawable()?.toBitmap(w, h) }.getOrNull()
            }
            if (bitmap != null) image.value = bitmap.asImageBitmap()
        } finally {
            loading = false
        }
    }

    private const val RESOLUTION_SCALE = 0.5f
}

/**
 * Снимок того, что лежит под стеклом: источник записывает себя в слой, стекло рисует этот слой размытым.
 * Слой — ссылка на список отрисовки, поэтому стекло видит изменения источника без лишней работы.
 * Запись включается только пока снимок кому-то нужен: у стекла есть счётчик читателей.
 */
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var recorded by mutableStateOf(false)

    /** Сколько стеклянных панелей сейчас читают снимок. 0 — источник рисуется напрямую, без лишней текстуры. */
    private var readers by mutableIntStateOf(0)
    internal val needed: Boolean get() = readers > 0

    internal fun attach() { readers++ }

    /**
     * Снимок стола для стекла над ним: делается один раз, когда открылась первая панель, и рисуется как обычная
     * картинка. Сбрасывается, когда панелей не осталось — следующая сделает свежий.
     */
    internal var snapshot by mutableStateOf<ImageBitmap?>(null)
        private set
    private var capturing = false

    internal suspend fun captureIfNeeded() {
        if (!recorded || snapshot != null || capturing) return
        capturing = true
        try {
            snapshot = runCatching { layer.toImageBitmap() }.getOrNull()
        } finally {
            capturing = false
        }
    }

    internal fun detach() {
        if (readers > 0) {
            readers--
            // Снимок остался без читателей: следующий кадр со стеклом должен записаться заново,
            // иначе стекло показало бы устаревшую картинку (например, другую страницу стола)
            if (readers == 0) {
                recorded = false
                snapshot = null
            }
        }
    }
}

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/**
 * Помечает содержимое, которое должно быть видно размытым под стеклом сверху.
 * Стекло, лежащее внутри источника, этот же [Backdrop] брать не должно: слой рисовал бы сам себя.
 */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        // Стекло закрыто или его нет: записывать весь экран в отдельную текстуру незачем,
        // иначе каждый кадр стола и шторки рисовался бы дважды
        if (!backdrop.needed) {
            drawContent()
            return@drawWithContent
        }
        backdrop.layer.record { this@drawWithContent.drawContent() }
        backdrop.recorded = true
        drawLayer(backdrop.layer)
    }

/** Источник для стекла, лежащего поверх; null — под стеклом только обои. */
val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Стеклянная панель дока и индикатора страниц. Раньше размывала RenderEffect'ом обои и снимок стола
 * на каждом кадре; теперь — [LiquidSurface]: кусок общего размытого растра обоев, без размытия на кадре.
 * [blurRadius] оставлен для совместимости вызовов: силу размытия задают настройки темы.
 */
@Composable
fun FrostedGlass(
    shape: Shape,
    tint: Color,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidSurface(shape, modifier, color = tint.copy(alpha = 1f), alpha = tint.alpha, blockTouches = false, content = content)
}

/**
 * Край жидкого стекла для своих поверхностей (пункты настроек, карточки онбординга): кромка ≈1 dp
 * и спекулярные блики ТОЛЬКО на углах — яркий у верхнего-левого, слабее у нижнего-правого.
 * В остальных темах ничего не делает. Шейдеры создаются один раз на размер.
 */
@Composable
fun Modifier.glassEdge(shape: Shape, colored: Boolean = false): Modifier {
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    if (!spec.glass) return this
    // Android 13+ со своими обоями: панель — прозрачное жидкое стекло библиотеки, как в видео (подложка под ней
    // прозрачная, см. panelColor). Цветные карточки ([colored]) сохраняют свой цвет — на нём тёмные подписи —
    // и получают стекло слоем поверх: кромку и блики
    if (Build.VERSION.SDK_INT >= 33) {
        return if (!colored && LocalWallLiquid.current != null) this.liquidGlass(shape, shadow = false, frosted = true)
        else this.liquidGlass(shape, overBlur = true, shadow = false, frosted = true, overlay = true)
    }
    val strength = LocalSettings.current.glassHighlightStrength
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return this.drawWithCache {
        val path = androidx.compose.ui.graphics.Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }.asAndroidPath()
        val stroke = 1.dp.toPx()
        val rim = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
            shader = Specular.cornerRim(size.width, size.height, strength, rim = if (dark) 0.06f else 0.14f)
        }
        val gloss = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { shader = Specular.innerGloss(size.width, size.height, strength) }
        val k = 1f - stroke / size.width.coerceAtLeast(1f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        onDrawWithContent {
            drawContent()
            drawIntoCanvas {
                val c = it.nativeCanvas
                c.drawPath(path, gloss)
                c.save()
                c.scale(k, k, cx, cy)
                c.drawPath(path, rim)
                c.restore()
            }
        }
    }
}

/** Размытие разрешено настройками, режимом энергосбережения и версией Android. */
@Composable
fun glassEnabled(): Boolean {
    val s = LocalSettings.current
    return s.blurBackground && LocalFx.current.blur && s.blurStrength > 0f && Build.VERSION.SDK_INT >= 31
}

/**
 * Панель в стиле темы (меню, папка, панели режима редактирования): Liquid Glass — жидкое стекло,
 * Expressive — тональная матовая панель, Lite — сплошная поверхность. [overBlur] — под панелью уже размытый
 * слой (открытая папка поверх размытого стола): обои не подкладываются, видно размытое содержимое.
 */
@Composable
fun GlassPanel(
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    glassAlpha: Float = 0.7f,
    @Suppress("UNUSED_PARAMETER") solidAlpha: Float = 0.96f,
    @Suppress("UNUSED_PARAMETER") elevation: Dp = 0.dp,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = 26.dp,
    overBlur: Boolean = false,
    content: @Composable () -> Unit,
) {
    LiquidSurface(
        shape, modifier,
        color = color.copy(alpha = 1f),
        alpha = glassAlpha.coerceAtLeast(0.82f),
        overBlur = overBlur,
        contentColor = contentColor,
        // Меню, папки, панели с текстом: матовое стекло, чтобы текст не сливался с тем, что под ним
        frosted = true,
    ) { content() }
}
