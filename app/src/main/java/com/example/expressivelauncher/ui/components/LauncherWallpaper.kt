package com.example.expressivelauncher.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Свои обои лаунчера. Живые обои (например, Pixel AI) рисует система за окном, и их пикселей приложение не получает:
 * жидкому стеклу нечего преломлять. Свою картинку лаунчер рисует сам под столом, а стекло берёт те же пиксели.
 * Картинка копируется в память приложения, поэтому доступ к галерее после выбора не нужен.
 */
object CustomWallpaper {
    private const val FILE = "launcher_wallpaper.img"

    fun file(context: Context) = File(context.filesDir, FILE)

    /** Растёт при каждой смене картинки: по нему перечитываются растр и кэш стекла. */
    var version by mutableIntStateOf(0)
        private set

    /** Включены ли свои обои прямо сейчас (настройка включена и файл есть). Задаёт MainActivity. */
    var active by mutableStateOf(false)

    /** Листаются ли обои вместе со страницами. Задаёт MainActivity из настроек. */
    var parallaxOn by mutableStateOf(true)

    /** Размытие картинки, 0..1. Запекается в растр: фон и стекло видят одну и ту же размытую картинку. */
    var blur = 0f
        private set

    fun setBlur(value: Float) {
        val v = (value * 20f).roundToInt() / 20f
        if (v == blur) return
        blur = v
        changed()
    }

    /** Растр под размер экрана; null — ещё не загружен. */
    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    /** Светлая ли картинка: от этого зависит цвет значков в строке состояния и подписей. */
    var light by mutableStateOf(false)
        private set

    private var loadedKey: String? = null

    fun hasFile(context: Context) = file(context).let { it.exists() && it.length() > 0 }

    /** Копирует выбранную картинку к себе. false — открыть не удалось. */
    suspend fun save(context: Context, uri: Uri): Boolean {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(context.filesDir, "$FILE.tmp")
                context.contentResolver.openInputStream(uri)!!.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                // Проверяем, что это картинка, до замены прежних обоев
                decode(tmp, IntSize(64, 64)) ?: error("not an image")
                if (!tmp.renameTo(file(context))) { tmp.copyTo(file(context), overwrite = true); tmp.delete() }
                true
            }.getOrDefault(false)
        }
        if (ok) changed()
        return ok
    }

    private fun changed() {
        version++
        loadedKey = null
        image = null
        WallpaperGlass.invalidate()
        WallpaperBlur.invalidate()
    }

    suspend fun load(context: Context, screen: IntSize) {
        if (screen.width < 2 || screen.height < 2) return
        val key = "$version|${screen.width}x${screen.height}"
        if (key == loadedKey && image != null) return
        val decoded = withContext(Dispatchers.IO) { decode(file(context), screen) } ?: return
        light = withContext(Dispatchers.Default) { luminance(decoded) > 0.6f }
        image = (decoded.copy(Bitmap.Config.HARDWARE, false) ?: decoded).asImageBitmap()
        loadedKey = key
    }

    /**
     * Картинка, уменьшенная так, чтобы покрыть экран (не больше исходной). ImageDecoder (Android 9+) сам
     * поворачивает фото по EXIF; на Android 8.1 — BitmapFactory с прореживанием.
     */
    fun decode(file: File, screen: IntSize): Bitmap? = decodeRaw(file, screen)?.let { softened(it) }

    /** Размытие [blur]: уменьшить, размыть и растянуть обратно — гладко и за десятки миллисекунд в фоне. */
    private fun softened(b: Bitmap): Bitmap {
        val k = blur
        if (k <= 0.01f || b.width < 8 || b.height < 8) return b
        val small = Bitmap.createScaledBitmap(b, max(4, b.width / 4), max(4, b.height / 4), true)
            .let { if (it.isMutable) it else it.copy(Bitmap.Config.ARGB_8888, true) }
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        val r = (k * 14f).roundToInt().coerceAtLeast(1)
        repeat(3) { boxPass(px, small.width, small.height, r, true); boxPass(px, small.width, small.height, r, false) }
        small.setPixels(px, 0, small.width, 0, 0, small.width, small.height)
        return Bitmap.createScaledBitmap(small, b.width, b.height, true)
    }

    private fun boxPass(px: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val out = IntArray(len)
        val div = 2 * r + 1
        for (line in 0 until lines) {
            fun idx(i: Int): Int { val k = i.coerceIn(0, len - 1); return if (horizontal) line * w + k else k * w + line }
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -r..r) { val c = px[idx(i)]; sa += c ushr 24; sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF }
            for (i in 0 until len) {
                out[i] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val a = px[idx(i + r + 1)]; val s = px[idx(i - r)]
                sa += (a ushr 24) - (s ushr 24); sr += ((a shr 16) and 0xFF) - ((s shr 16) and 0xFF)
                sg += ((a shr 8) and 0xFF) - ((s shr 8) and 0xFF); sb += (a and 0xFF) - (s and 0xFF)
            }
            for (i in 0 until len) px[idx(i)] = out[i]
        }
    }

    private fun decodeRaw(file: File, screen: IntSize): Bitmap? = runCatching {
        if (!file.exists()) return null
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val iw = info.size.width.toFloat()
                val ih = info.size.height.toFloat()
                val k = min(1f, max(screen.width / iw, screen.height / ih))
                if (k < 1f) decoder.setTargetSize(max(1, (iw * k).roundToInt()), max(1, (ih * k).roundToInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= screen.width && bounds.outHeight / (sample * 2) >= screen.height) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    private fun luminance(b: Bitmap): Float {
        val small = Bitmap.createScaledBitmap(b, 16, 16, true)
        var sum = 0f
        for (y in 0 until 16) for (x in 0 until 16) {
            val c = small.getPixel(x, y)
            sum += (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
        }
        return sum / 256f
    }

    /**
     * Где картинка лежит на экране: вписана по высоте, а если от этого уже экрана — по ширине.
     * Ровно так же её раскладывает кэш стекла ([WallpaperBlur]), поэтому стекло и фон совпадают пиксель в пиксель.
     */
    fun fit(iw: Float, ih: Float, sw: Float, sh: Float): Pair<Float, Float> {
        var dh = sh
        var dw = iw * dh / ih
        if (dw < sw) {
            dw = sw
            dh = ih * dw / iw
        }
        return dw to dh
    }
}

/**
 * Свои обои под столом. Листаются вместе со страницами, как системные ([WallpaperBlur.parallax]).
 * [blurred] — под меню приложений, папкой или настройками: поверх плавно проявляется уже размытая копия
 * из кэша стекла, без размытия на каждом кадре.
 */
@Composable
fun LauncherWallpaper(blurred: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val screen = LocalWindowInfo.current.containerSize
    LaunchedEffect(CustomWallpaper.version, screen) { CustomWallpaper.load(context, screen) }
    // Размытая копия для фона под меню и настройками нужна во всех темах, даже где нет стеклянных панелей
    val strength = com.example.expressivelauncher.ui.LocalSettings.current.blurStrength
    LaunchedEffect(CustomWallpaper.version, screen, strength, WallpaperGlass.key) { WallpaperBlur.ensure(context, screen, strength) }
    val image = CustomWallpaper.image ?: return
    val blurAmount by animateFloatAsState(if (blurred) 1f else 0f, label = "wallpaperBlur")
    Canvas(modifier) {
        val (dw, dh) = CustomWallpaper.fit(image.width.toFloat(), image.height.toFloat(), size.width, size.height)
        val x = -WallpaperBlur.parallax * (dw - size.width).coerceAtLeast(0f)
        val offset = IntOffset(x.roundToInt(), 0)
        val dst = IntSize(dw.roundToInt(), dh.roundToInt())
        drawImage(image, dstOffset = offset, dstSize = dst, filterQuality = FilterQuality.Low)
        val frame = WallpaperBlur.frame
        if (blurAmount > 0.01f && frame != null) {
            drawImage(
                frame.image,
                dstOffset = IntOffset((-WallpaperBlur.parallax * (frame.wallWidth - size.width).coerceAtLeast(0f)).roundToInt(), 0),
                dstSize = IntSize(frame.wallWidth.roundToInt(), (frame.image.height / frame.scale).roundToInt()),
                alpha = blurAmount,
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

/**
 * Непрозрачный фон экрана поверх стола (выбор виджетов): размытые обои из кэша стекла и затемнение.
 * Стол под таким экраном не просвечивает. Без картинки обоев — сплошной цвет [fallback].
 */
fun Modifier.blurredWallpaperBackground(fallback: androidx.compose.ui.graphics.Color, scrim: Float = 0.35f, sharp: Boolean = false): Modifier =
    this.drawBehind {
        val frame = WallpaperBlur.frame
        val image = CustomWallpaper.image
        if (sharp && image != null) {
            // Чёткие обои лаунчера с затемнением (как под настройками): стекло панелей преломляет саму картинку
            val (dw, dh) = CustomWallpaper.fit(image.width.toFloat(), image.height.toFloat(), size.width, size.height)
            val x = -WallpaperBlur.parallax * (dw - size.width).coerceAtLeast(0f)
            drawImage(image, dstOffset = IntOffset(x.roundToInt(), 0), dstSize = IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.Low)
            drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = scrim))
        } else if (frame != null) {
            val w = frame.wallWidth
            val h = frame.image.height / frame.scale
            // Покрыть экран целиком: вписанная по высоте картинка может быть ниже экрана — растягиваем
            val k = maxOf(1f, size.height / h, size.width / w)
            val x = -WallpaperBlur.parallax * (w * k - size.width).coerceAtLeast(0f)
            drawImage(
                frame.image,
                dstOffset = IntOffset(x.roundToInt(), 0),
                dstSize = IntSize((w * k).roundToInt(), (h * k).roundToInt()),
                filterQuality = FilterQuality.Low,
            )
            drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = scrim))
        } else {
            drawRect(fallback)
        }
    }
