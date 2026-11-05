package com.example.expressivelauncher.ui.home

import android.graphics.Matrix as AndroidMatrix
import android.graphics.Path as AndroidPath
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import com.example.expressivelauncher.data.IconShape
import com.example.expressivelauncher.ui.Fx
import com.example.expressivelauncher.ui.LocalHostView
import com.example.expressivelauncher.ui.components.IconLook
import com.example.expressivelauncher.ui.theme.tunedDamping
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Фигура значка как многоугольник в квадрате 0..1: с неё начинается морфинг при запуске. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun IconShape.polygon(): RoundedPolygon = when (this) {
    IconShape.CIRCLE -> MaterialShapes.Circle
    IconShape.SQUIRCLE -> unitRect(0.68f)
    IconShape.ROUNDED -> unitRect(0.44f)
    IconShape.COOKIE_9 -> MaterialShapes.Cookie9Sided
    IconShape.COOKIE_12 -> MaterialShapes.Cookie12Sided
    IconShape.CLOVER_4 -> MaterialShapes.Clover4Leaf
    IconShape.FLOWER -> MaterialShapes.Flower
    IconShape.SUNNY -> MaterialShapes.Sunny
    IconShape.PENTAGON -> MaterialShapes.Pentagon
    IconShape.GEM -> MaterialShapes.Gem
    IconShape.PILL -> MaterialShapes.Pill
    IconShape.PUFFY -> MaterialShapes.Puffy
    IconShape.CONTINUOUS -> ContinuousSquircle
}

/**
 * Маска значков iOS: скруглённый квадрат с непрерывной кривизной (сглаживание углов), а не дуги окружности.
 * Радиус ≈ 22 % стороны, как у значков iPhone.
 */
val ContinuousSquircle: RoundedPolygon by lazy {
    RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.47f, smoothing = 0.62f)).normalized()
}

private fun unitRect(rounding: Float) = RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(rounding)).normalized()

/** Морфинги считаются один раз на фигуру: построение Morph заметно дороже одного кадра анимации. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ShapeMorphs {
    private val polygons = ConcurrentHashMap<IconShape, RoundedPolygon>()
    private val windows = ConcurrentHashMap<IconShape, Morph>()
    private val presses = ConcurrentHashMap<IconShape, Morph>()

    /** Скругление окна в момент окончания морфинга, в долях стороны. */
    const val WINDOW_CORNER = 0.28f
    private val windowPolygon by lazy { unitRect(WINDOW_CORNER * 2f) }
    private val squircle by lazy { unitRect(0.68f) }

    fun polygon(shape: IconShape): RoundedPolygon = polygons.getOrPut(shape) { shape.polygon() }
    fun window(shape: IconShape): Morph = windows.getOrPut(shape) { Morph(polygon(shape), windowPolygon) }

    /** Значок под пальцем: любая фигура мягко перетекает в круг, а круг — в скруглённый квадрат. */
    fun press(shape: IconShape): Morph = presses.getOrPut(shape) {
        Morph(polygon(shape), if (shape == IconShape.CIRCLE) squircle else MaterialShapes.Circle)
    }
}

// ---------- Машина состояний запуска ----------

/** Фаза перехода. Единственный источник правды: других флагов «открыто/закрывается» нет. */
enum class LaunchPhase { Idle, Opening, Open, Closing }

/** Откуда запустили приложение: туда же окно и вернётся. */
sealed interface LaunchSource {
    /** Значок на столе или в доке: он скрыт, пока окно на экране. */
    data class Home(val itemId: String) : LaunchSource
    /** Приложение из папки: окно возвращается в значок папки и растворяется в нём. */
    data class Folder(val folderId: String) : LaunchSource
    /** Меню приложений, поиск: значок есть только пока открыто меню. */
    data object Free : LaunchSource
}

/**
 * Куда сейчас смотрит значок: [rect] в координатах окна; [fade] — окно растворяется в конце пути;
 * [visibility] — насколько виден сам значок (меню приложений закрывается — значок в нём гаснет вместе с ним).
 */
class LaunchAnchor(val rect: Rect, val fade: Boolean = false, val visibility: Float = 1f)

/** Пружины перехода. [slow] — во сколько раз медленнее обычного (отладочный режим ×5). */
class LaunchSpecs(
    val open: AnimationSpec<Float>,
    val close: AnimationSpec<Float>,
    val away: AnimationSpec<Float>,
    val slow: Float,
)

/** Куда летит окно при сворачивании. */
internal enum class AnchorMode {
    /** Следит за значком на столе: страница, док, поворот экрана. */
    LIVE,
    /** Точка, из которой запустили (папка, меню приложений). */
    FIXED,
    /** Значка не видно: окно уходит в центр и растворяется. */
    CENTER,
}

/**
 * Один переход «значок ⇄ окно». Сам объект служит токеном: корутина, которая закончилась позже,
 * чем переход сменился, видит, что она уже не текущая, и ничего не трогает.
 */
@Stable
class LaunchFlight internal constructor(
    val appKey: String,
    val source: LaunchSource,
    val look: IconLook?,
    internal val iconPolygon: RoundedPolygon,
    internal val morph: Morph,
    internal val from: Rect,
    internal var pauseMark: Int,
    internal var launch: () -> Boolean,
    /** Фон стартового окна приложения: к нему приходит карточка, чтобы окно проявилось без вспышки. */
    internal val endColor: Color?,
) {
    internal val anim = Animatable(0f, visibilityThreshold = 0.0005f)
    val progress: Float get() = anim.value
    var phase by mutableStateOf(LaunchPhase.Opening)
        internal set
    internal var mode = if (source is LaunchSource.Home) AnchorMode.LIVE else AnchorMode.FIXED
    internal var fade = false
    internal var lastAnchor = from
    internal var lastVisibility = 1f
    internal var job: Job? = null
    /** Приложение ещё предстоит запустить на этом открытии: запуск ждёт, пока карточка почти заполнит экран. */
    internal var needsLaunch = true
}

/**
 * Анимация запуска в окне самого лаунчера. Чужое окно сторонний лаунчер двигать не может (RemoteAnimation есть
 * только у системного), а свою анимацию для окна другой задачи Android 13+ игнорирует. Поэтому морфинг значка в окно
 * рисуем сами, а приложение запускаем, когда карточка почти заполнила экран ([LAUNCH_AT]): система лишь проявляет
 * его окно поверх карточки. При возвращении система убирает окно приложения, а под ним карточка сворачивается
 * в значок на его нынешнем месте. Открыть и сразу закрыть — значит не запустить приложение вовсе.
 *
 * Все параметры кадра — функции одного [LaunchFlight.progress], которым управляет пружина. Прерывание в любую
 * сторону продолжает пружину с текущего значения и текущей скорости, поэтому картинка всегда непрерывна.
 * [current] — открывается, открыто или закрывается; [leaving] — прошлые приложения, которые
 * досворачиваются в свои значки, пока открывается новое.
 */
@Stable
class LaunchPortal(private val scope: CoroutineScope) {
    var current by mutableStateOf<LaunchFlight?>(null)
        private set
    /** Прошлые окна, которые досворачиваются в свои значки. Обычно одно; при очень быстром переключении больше. */
    val leaving: List<LaunchFlight> get() = away
    private val away = mutableStateListOf<LaunchFlight>()

    val phase: LaunchPhase get() = current?.phase ?: LaunchPhase.Idle
    val active: Boolean get() = current != null || away.isNotEmpty()

    /** Актуальная позиция значка источника; null — значок не виден. Стол обновляет её на каждой композиции. */
    var locate: (LaunchSource, Rect) -> LaunchAnchor? = { _, _ -> null }
    var specs: LaunchSpecs = defaultSpecs()

    /** Значок стола скрыт, пока из него выросло окно или окно в него возвращается. */
    fun conceals(itemId: String): Boolean = current.hides(itemId) || away.any { it.hides(itemId) }

    private fun LaunchFlight?.hides(itemId: String): Boolean {
        val s = this?.source
        return s is LaunchSource.Home && s.itemId == itemId
    }

    /** Насколько стол ушёл под окно: 0 — стол как обычно, 1 — окно на весь экран. */
    fun desktopProgress(): Float {
        var d = current?.progress ?: 0f
        for (f in away) d = max(d, f.progress)
        return d.coerceIn(0f, 1f)
    }

    /**
     * Тап по значку. Повторный тап по тому же значку во время открытия ничего не делает, во время закрытия
     * разворачивает пружину обратно. Тап по другому значку отправляет текущее окно в свой значок и открывает новое.
     * [pauses] — сколько раз лаунчер уходил в паузу: так видно, показалось ли приложение.
     */
    fun open(
        appKey: String,
        source: LaunchSource,
        from: Rect,
        look: IconLook?,
        shape: IconShape,
        endColor: Color?,
        pauses: () -> Int,
        launch: () -> Boolean,
    ) {
        val cur = current
        if (cur != null && cur.appKey == appKey && cur.source == source) {
            // Повторный тап во время открытия ничего не делает: второго запуска и второй анимации нет
            if (cur.phase != LaunchPhase.Closing) return
            cur.launch = launch
            runOpen(cur, pauses)
            return
        }
        // Нажали значок приложения, которое ещё досворачивается: разворачиваем его же пружину, а не начинаем заново
        val back = away.firstOrNull { it.appKey == appKey && it.source == source }
        if (back != null) {
            away.remove(back)
            if (cur != null) sendAway(cur)
            back.launch = launch
            current = back
            runOpen(back, pauses)
            return
        }
        if (cur != null) sendAway(cur)
        val f = LaunchFlight(appKey, source, look, ShapeMorphs.polygon(shape), ShapeMorphs.window(shape), from, pauses(), launch, endColor)
        current = f
        runOpen(f, pauses)
    }

    /** «Домой» или «Назад» из приложения. [keepFree] — меню приложений останется открытым, окно можно вернуть в него. */
    fun close(keepFree: Boolean = true) {
        val f = current ?: return
        if (f.phase == LaunchPhase.Closing) return
        startClosing(f, specs.close, keepFree)
    }

    /** Лаунчер снова на переднем плане. Закрываем, только если он уходил в паузу уже после тапа. */
    fun onResumed(pauses: Int) {
        val f = current ?: return
        if (pauses > f.pauseMark) close()
    }

    /** Мгновенно убирает всё: значки видимы, стол в исходном состоянии. */
    fun reset() {
        current?.let(::finish)
        away.toList().forEach(::finish)
    }

    private fun runOpen(f: LaunchFlight, pauses: () -> Int) {
        f.phase = LaunchPhase.Opening
        // Каждое открытие запускает приложение заново: после «Домой» оно в фоне, его нужно вернуть
        f.needsLaunch = true
        f.pauseMark = pauses()
        val onFrame: () -> Unit = {
            if (f.needsLaunch && f.anim.value >= LAUNCH_AT && current === f && f.phase == LaunchPhase.Opening) {
                f.needsLaunch = false
                f.pauseMark = pauses()
                // Не запустилось — карточка сворачивается обратно. Через диспетчер: не перезапускаем пружину из её же кадра
                if (!f.launch()) scope.launch { if (current === f) close() }
            }
        }
        animate(f, 1f, specs.open, onFrame) {
            if (current !== f || f.phase != LaunchPhase.Opening) return@animate
            f.phase = LaunchPhase.Open
            // Приложение могло ничего не показать (активность-заглушка): не оставляем пустую карточку
            delay((TRAMPOLINE_TIMEOUT_MS * specs.slow).toLong())
            if (current === f && f.phase == LaunchPhase.Open && pauses() == f.pauseMark) close()
        }
    }

    private fun startClosing(f: LaunchFlight, spec: AnimationSpec<Float>, keepFree: Boolean) {
        // Цель меняем, только когда окно почти на весь экран: на середине пути смена цели дала бы скачок
        if (f.anim.value >= RETARGET_FROM) {
            val anchor = locate(f.source, f.from)?.takeIf { keepFree || f.source !is LaunchSource.Free }
            if (anchor == null) {
                f.mode = AnchorMode.CENTER
                f.fade = true
            } else {
                f.mode = AnchorMode.LIVE
                f.fade = anchor.fade
                f.lastAnchor = anchor.rect
            }
        }
        f.phase = LaunchPhase.Closing
        animate(f, 0f, spec) { finish(f) }
    }

    /** Окно A досворачивается быстрой пружиной, пока открывается B; каждое уходящее окно доводится до конца. */
    private fun sendAway(f: LaunchFlight) {
        if (current === f) current = null
        away.add(f)
        startClosing(f, specs.away, keepFree = true)
    }

    private fun finish(f: LaunchFlight) {
        f.job?.cancel()
        f.job = null
        f.phase = LaunchPhase.Idle
        if (current === f) current = null
        away.remove(f)
    }

    /**
     * Единственное место, где запускается пружина. Скорость берём до отмены прошлой анимации:
     * отменённый Animatable обнуляет её, и разворот начинался бы с места, а не с текущего движения.
     */
    private fun animate(
        f: LaunchFlight,
        target: Float,
        spec: AnimationSpec<Float>,
        onFrame: (() -> Unit)? = null,
        then: suspend () -> Unit,
    ) {
        val velocity = f.anim.velocity
        f.job?.cancel()
        f.job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            f.anim.animateTo(target, spec, initialVelocity = velocity) { onFrame?.invoke() }
            then()
        }
    }

    /** Где сейчас значок, к которому привязано окно. Вызывается при отрисовке. */
    internal fun anchorOf(f: LaunchFlight, size: Size): Rect = when (f.mode) {
        AnchorMode.CENTER -> Rect(Offset(size.width / 2f, size.height / 2f), f.from.width / 2f)
        AnchorMode.FIXED -> f.from
        AnchorMode.LIVE -> locate(f.source, f.from)?.let {
            f.lastAnchor = it.rect
            f.lastVisibility = it.visibility.coerceIn(0f, 1f)
            it.rect
        } ?: f.lastAnchor
    }

    private companion object {
        const val TRAMPOLINE_TIMEOUT_MS = 2500L
        const val RETARGET_FROM = 0.9f
        /** Доля пути, на которой запускается приложение: карточка уже почти на весь экран. */
        const val LAUNCH_AT = 0.85f
    }
}

/** Во сколько раз медленнее отладочный режим. */
const val DEBUG_SLOW_FACTOR = 5f

/**
 * Пружины перехода. Жёсткость делится на квадрат замедления: время пружины обратно пропорционально корню из жёсткости.
 * Открытие ≈ 350–400 мс с лёгким перелётом, закрытие ≈ 300–380 мс со значком, который садится с отскоком.
 */
fun launchSpecs(expressive: Boolean, fx: Fx, closeScale: Float, slow: Boolean): LaunchSpecs {
    val k = if (slow) DEBUG_SLOW_FACTOR else 1f
    val speed2 = fx.speed * fx.speed / (k * k)
    return LaunchSpecs(
        open = spring(dampingRatio = if (expressive) tunedDamping(0.78f, fx.bounce) else 1f, stiffness = 300f * speed2, visibilityThreshold = 0.0005f),
        close = spring(
            dampingRatio = if (expressive) tunedDamping(0.72f, fx.bounce) else 1f,
            stiffness = 280f * speed2 / (closeScale * closeScale),
            visibilityThreshold = 0.0005f,
        ),
        // Прошлое окно уходит быстро и без отскока: внимание уже на новом приложении
        away = spring(dampingRatio = 1f, stiffness = 900f * speed2, visibilityThreshold = 0.0005f),
        slow = k / fx.speed,
    )
}

private fun defaultSpecs() = launchSpecs(expressive = true, fx = Fx(), closeScale = 1f, slow = false)

// ---------- Стол под окном ----------

/** Насколько уменьшается сетка, когда окно на весь экран. */
const val RECEDE_SCALE = 0.08f

/** Сетка уходит под окно: растворяется и чуть уменьшается. Только свойства слоя, без relayout. */
fun Modifier.launchRecede(portal: LaunchPortal, scaleBy: Float = RECEDE_SCALE): Modifier = graphicsLayer {
    val d = portal.desktopProgress()
    val s = 1f - scaleBy * d
    scaleX = s
    scaleY = s
    alpha = 1f - d
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

/** Док затемняется и размывается, обои вокруг остаются видны. [blur] — размытие внутри окна доступно. */
fun Modifier.launchDim(portal: LaunchPortal, blur: Boolean): Modifier = this
    .graphicsLayer {
        val d = portal.desktopProgress()
        alpha = 1f - 0.4f * d
        val radius = if (blur) 10.dp.toPx() * d else 0f
        renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
        // Отдельный слой нужен только во время перехода: затемнение ложится лишь на нарисованные пиксели дока
        compositingStrategy = if (d > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithContent {
        drawContent()
        val d = portal.desktopProgress()
        if (d > 0f) drawRect(Color.Black, alpha = 0.35f * d, blendMode = BlendMode.SrcAtop)
    }

// ---------- Отрисовка ----------

private fun smooth(from: Float, to: Float, x: Float): Float {
    val k = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Часть пути, на которой фигура значка становится скруглённым прямоугольником окна. */
private const val MORPH_END = 0.25f
/** Насколько значок вырастает сам, прежде чем начать превращаться в окно. */
private const val GROW = 0.14f
/** Сжатие значка на перелёте пружины в конце закрытия (прогресс меньше нуля): отскок. */
private const val DIP = 2.5f

@Composable
fun LaunchPortalOverlay(portal: LaunchPortal) {
    val surface = MaterialTheme.colorScheme.surface
    val fallback = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current
    val host = LocalHostView.current
    val iconTargetPx = with(density) { 112.dp.toPx() }
    // Итоговая карточка повторяет скругление дисплея; если система его не сообщает — как у iPhone
    val cornerPx = remember(host, density) {
        val display = if (android.os.Build.VERSION.SDK_INT >= 31) {
            host?.rootWindowInsets?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat()
        } else null
        val min = with(density) { 16.dp.toPx() }
        display?.takeIf { it >= min } ?: with(density) { 44.dp.toPx() }
    }
    val tools = remember { PortalTools() }

    // Без pointerInput: касания проходят на стол, и значок можно нажать снова в любой момент
    Canvas(Modifier.fillMaxSize()) {
        // Стол почти не темнеет: обои остаются видны, стол уходит в глубину (масштаб и прозрачность задаёт HomeScreen)
        drawRect(Color.Black, alpha = 0.12f * portal.desktopProgress())
        // Уходящее окно садится в стол, который гаснет под новым окном: гаснет вместе с ним, без вспышки значка
        val dim = 1f - (portal.current?.progress ?: 0f).coerceIn(0f, 1f)
        for (f in portal.leaving) drawFlight(portal, f, tools, surface, fallback, iconTargetPx, cornerPx, dim)
        portal.current?.let { drawFlight(portal, it, tools, surface, fallback, iconTargetPx, cornerPx, 1f) }
    }
}

private class PortalTools {
    val path = AndroidPath()
    val iconPath = AndroidPath()
    val tmp = AndroidPath()
    val matrix = AndroidMatrix()
    val composePath = path.asComposePath()
    val composeIconPath = iconPath.asComposePath()
}

private fun DrawScope.drawFlight(
    portal: LaunchPortal,
    f: LaunchFlight,
    t: PortalTools,
    surface: Color,
    fallback: Color,
    iconTargetPx: Float,
    cornerPx: Float,
    dim: Float,
) {
    val p = f.progress
    val anchor = portal.anchorOf(f, size)
    val look = f.look
    val accent = look?.accent?.takeIf { it != 0 }?.let { Color(it) } ?: fallback
    // Значок, куда летит окно, исчез вместе с меню приложений: окно гаснет вместе с ним, а не висит на столе
    val visibility = if (f.mode == AnchorMode.LIVE) f.lastVisibility else 1f
    val alpha = (if (f.fade) smooth(0f, 0.35f, p) else 1f) * dim * visibility
    if (alpha <= 0f) return

    // 1. Значок сначала заметно растёт сам; на перелёте в конце закрытия — чуть сжимается (отскок)
    val grow = if (p >= 0f) 1f + GROW * smooth(0f, 0.25f, p) else (1f + p * DIP).coerceAtLeast(0.6f)
    val halfW = anchor.width / 2f * grow
    val halfH = anchor.height / 2f * grow
    val cx = anchor.center.x
    val cy = anchor.center.y

    // 2. Затем позиция и размер перетекают к окну; степень > 1 оставляет начало пути за ростом значка
    val m = ((p - 0.06f) / 0.94f).coerceIn(0f, 1.03f).pow(1.35f)
    val left = lerpF(cx - halfW, 0f, m)
    val top = lerpF(cy - halfH, 0f, m)
    val right = lerpF(cx + halfW, size.width, m)
    val bottom = lerpF(cy + halfH, size.height, m)
    val w = right - left
    val h = bottom - top
    if (w <= 0f || h <= 0f) return

    // Свободная форма значка (из набора) не получает подложку чужой фигуры: контейнер проявляется чуть позже
    val clip = look?.clip != false
    val containerAlpha = (if (clip) 1f else smooth(0f, 0.2f, m)) * alpha
    // Содержимое значка перетекает в «содержимое приложения»: цвет значка → поверхность, на которую ляжет окно
    val container = lerp(accent, f.endColor ?: surface, smooth(0.2f, 0.8f, m))
    if (containerAlpha > 0f) {
        t.path.rewind()
        if (m < MORPH_END) {
            f.morph.toPath(smooth(0f, 1f, m / MORPH_END), t.path)
            t.matrix.reset()
            t.matrix.setScale(w, h)
            t.matrix.postTranslate(left, top)
            t.path.transform(t.matrix)
        } else {
            // Скругление окна плавно уходит к скруглению дисплея: резкого перехода в прямоугольник нет
            val k = smooth(0f, 1f, (m - MORPH_END) / (1f - MORPH_END))
            val rx = lerpF(w * ShapeMorphs.WINDOW_CORNER, cornerPx, k)
            val ry = lerpF(h * ShapeMorphs.WINDOW_CORNER, cornerPx, k)
            t.path.addRoundRect(left, top, right, bottom, rx, ry, AndroidPath.Direction.CW)
        }
        drawPath(t.composePath, container, alpha = containerAlpha)
    }

    // 3. Значок внутри растёт вместе с окном и растворяется; одна формула для открытия и закрытия
    val iconAlpha = (1f - smooth(0.2f, 0.6f, m)) * alpha
    val image = look?.image ?: return
    if (iconAlpha <= 0f) return
    val iconSize = min(min(w, h), lerpF(anchor.width * grow, iconTargetPx, smooth(0f, 1f, m)))
    val topLeft = Offset((left + right) / 2f - iconSize / 2f, (top + bottom) / 2f - iconSize / 2f)
    val draw: () -> Unit = {
        if (clip) drawRect(look.background, topLeft, Size(iconSize, iconSize), alpha = iconAlpha)
        drawImage(
            image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            // Значок в стеклянной подложке: рисунок меньше плашки, как на столе
            dstOffset = IntOffset((topLeft.x + iconSize * look.inset).toInt(), (topLeft.y + iconSize * look.inset).toInt()),
            dstSize = IntSize((iconSize * (1f - 2f * look.inset)).toInt(), (iconSize * (1f - 2f * look.inset)).toInt()),
            alpha = iconAlpha,
            colorFilter = look.filter,
            filterQuality = FilterQuality.Medium,
        )
    }
    if (clip) {
        t.iconPath.rewind()
        t.tmp.rewind()
        f.iconPolygon.toPath(t.tmp)
        t.matrix.reset()
        t.matrix.setScale(iconSize, iconSize)
        t.matrix.postTranslate(topLeft.x, topLeft.y)
        t.tmp.transform(t.matrix, t.iconPath)
        clipPath(t.composeIconPath) { draw() }
    } else {
        draw()
    }
}
