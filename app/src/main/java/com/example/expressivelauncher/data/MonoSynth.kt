package com.example.expressivelauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Монохромный слой для тематических значков у приложений, которые его не дали
 * (как «тематические значки для всех приложений» в Android 16).
 *
 * Рисунок ищется так:
 *  - есть сплошная «плашка» (квадрат, круг) — рисунок всё, что заметно отличается от её цвета;
 *  - плашки нет (логотип на прозрачном) — берётся силуэт логотипа, а мелкие детали другого цвета
 *    внутри него вырезаются (трубка в пузыре WhatsApp, буква на кнопке);
 * После этого рисунок обрезается по своим границам и вписывается в одинаковую для всех значков область,
 * чтобы сгенерированные значки были того же размера, что и системные монохромные.
 */
object MonoSynth {
    /** Доля стороны значка, которую занимает рисунок; у системных монохромных значков примерно столько же. */
    private const val GLYPH = 0.52f

    /** [fg] — передний слой adaptive-иконки на прозрачном, [full] — значок целиком. */
    fun fromAdaptive(fg: Bitmap, full: Bitmap): Bitmap? {
        val s = fg.width
        val px = IntArray(s * s)
        fg.getPixels(px, 0, s, 0, 0, s, s)
        var opaque = 0
        for (p in px) if (Color.alpha(p) > 160) opaque++
        // Передний слой закрывает почти весь значок — рисунок ищем на значке целиком, как на плашке
        return if (opaque > s * s * 0.72f) fromPlate(full) ?: silhouette(full) else silhouette(fg)
    }

    /**
     * Свой тематический (монохромный) слой приложения показывается как есть, даже залитый: так его задумал автор.
     * Раньше залитый слой переворачивался, и значок выглядел не так, как в стоковом лаунчере. Чистится только
     * «длинная тень» шаблонной иконки Android Studio.
     */
    fun fixSystem(mono: Bitmap): Bitmap {
        val s = mono.width
        if (mono.config != Bitmap.Config.ALPHA_8) return mono
        val stride = mono.rowBytes
        val raw = ByteArray(stride * mono.height)
        mono.copyPixelsToBuffer(java.nio.ByteBuffer.wrap(raw))
        val a = ByteArray(s * s)
        for (y in 0 until s) System.arraycopy(raw, y * stride, a, y * s, s)
        return cutShadow(mono, a, s)
    }

    /**
     * Шаблонная иконка Android Studio отдаёт в монохромный слой передний слой целиком, вместе с «длинной тенью»:
     * на плашке она выглядит грязной диагональной полосой. Если полупрозрачная заливка занимает заметную часть
     * рисунка, оставляем только плотные места. Аккуратные двухтоновые глифы (полупрозрачны лишь мелкие детали
     * и края) не трогаем.
     */
    private fun cutShadow(mono: Bitmap, a: ByteArray, s: Int): Bitmap {
        var drawn = 0; var faint = 0
        for (v in a) {
            val x = v.toInt() and 0xFF
            if (x > 16) drawn++
            if (x in 17..150) faint++
        }
        if (drawn == 0 || faint < drawn * 0.3f) return mono
        val out = IntArray(s * s)
        for (i in a.indices) {
            val x = (a[i].toInt() and 0xFF) / 255f
            val k = ((x - 0.6f) / 0.3f).coerceIn(0f, 1f)
            out[i] = ((k * k * (3f - 2f * k)) * 255f).toInt() shl 24
        }
        return normalize(out, s) ?: mono
    }

    /** Обычная (не adaptive) иконка. */
    fun fromLegacy(bmp: Bitmap): Bitmap? = fromPlate(bmp) ?: silhouette(bmp)

    private class Bounds(val l: Int, val t: Int, val r: Int, val b: Int) {
        val w get() = r - l + 1
        val h get() = b - t + 1
    }

    private fun bounds(alpha: (Int) -> Int, s: Int, threshold: Int): Bounds? {
        var l = s; var t = s; var r = -1; var b = -1
        for (y in 0 until s) for (x in 0 until s) {
            if (alpha(y * s + x) > threshold) {
                if (x < l) l = x
                if (x > r) r = x
                if (y < t) t = y
                if (y > b) b = y
            }
        }
        return if (r < 0) null else Bounds(l, t, r, b)
    }

    /** Значок на сплошной плашке: цвет плашки берём по кольцу у её края. null — плашки нет. */
    private fun fromPlate(src: Bitmap): Bitmap? {
        val s = src.width
        val px = IntArray(s * s)
        src.getPixels(px, 0, s, 0, 0, s, s)
        val box = bounds({ Color.alpha(px[it]) }, s, 200) ?: return null
        // Плашка — непрозрачная фигура почти на весь значок
        if (box.w < s * 0.5f || box.h < s * 0.5f) return null
        val cx = (box.l + box.r) / 2f
        val cy = (box.t + box.b) / 2f
        val rx = box.w / 2f * 0.86f
        val ry = box.h / 2f * 0.86f
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        val samples = IntArray(96)
        for (i in 0 until 96) {
            val a = i * Math.PI * 2 / 96
            val x = (cx + rx * kotlin.math.cos(a)).toInt().coerceIn(0, s - 1)
            val y = (cy + ry * kotlin.math.sin(a)).toInt().coerceIn(0, s - 1)
            val p = px[y * s + x]
            samples[i] = p
            if (Color.alpha(p) > 200) {
                r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
            }
        }
        if (n < 80) return null
        val br = (r / n).toInt(); val bg = (g / n).toInt(); val bb = (b / n).toInt()
        // Край пёстрый (градиент, фото) — сплошной плашки нет
        var spread = 0
        for (p in samples) if (Color.alpha(p) > 200 && diff(p, br, bg, bb) > 0.16f) spread++
        if (spread > 24) return null
        val out = IntArray(s * s)
        var covered = 0
        for (i in px.indices) {
            val p = px[i]
            val a = Color.alpha(p) / 255f
            val t = ((diff(p, br, bg, bb) - 0.09f) / 0.2f).coerceIn(0f, 1f)
            val v = a * t * t * (3f - 2f * t)
            if (v > 0.5f) covered++
            out[i] = ((v * 255f).toInt() shl 24)
        }
        // Рисунок одного цвета с плашкой — различить нечего
        if (covered < s * s / 60) return null
        return normalize(out, s)
    }

    /** Логотип на прозрачном: силуэт, из которого вырезаны мелкие детали другого цвета. */
    private fun silhouette(src: Bitmap): Bitmap? {
        val s = src.width
        val px = IntArray(s * s)
        src.getPixels(px, 0, s, 0, 0, s, s)
        // Основной цвет логотипа — средний по непрозрачным точкам
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (p in px) if (Color.alpha(p) > 200) { r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++ }
        if (n == 0) return null
        var mr = (r / n).toInt(); var mg = (g / n).toInt(); var mb = (b / n).toInt()
        // Уточняем: среднее только по точкам, близким к среднему (иначе пёстрый логотип даёт серую кашу)
        r = 0; g = 0; b = 0
        var m = 0
        for (p in px) if (Color.alpha(p) > 200 && diff(p, mr, mg, mb) < 0.22f) { r += Color.red(p); g += Color.green(p); b += Color.blue(p); m++ }
        if (m > n / 4) { mr = (r / m).toInt(); mg = (g / m).toInt(); mb = (b / m).toInt() }
        var other = 0
        for (p in px) if (Color.alpha(p) > 200 && diff(p, mr, mg, mb) > 0.3f) other++
        // Детали вырезаем, только если их немного: у многоцветного логотипа (Gmail, Фото) остаётся силуэт
        val carve = other in (n / 40)..(n * 35 / 100)
        val out = IntArray(s * s)
        for (i in px.indices) {
            val p = px[i]
            // Полупрозрачные тени («длинная тень» у старых значков) в рисунок не попадают
            // Порог высокий: тень у шаблонного значка Android доходит до половины непрозрачности и давала грязную полосу
            val a = ((Color.alpha(p) / 255f - 0.6f) / 0.3f).coerceIn(0f, 1f)
            var v = a * a * (3f - 2f * a)
            if (carve && v > 0f) {
                val t = ((diff(p, mr, mg, mb) - 0.2f) / 0.15f).coerceIn(0f, 1f)
                v *= 1f - t * t * (3f - 2f * t)
            }
            out[i] = ((v * 255f).toInt() shl 24)
        }
        return normalize(out, s)
    }

    /** Обрезает рисунок по границам и вписывает его по центру в одинаковую для всех значков область. */
    private fun normalize(mask: IntArray, s: Int): Bitmap? {
        val box = bounds({ mask[it] ushr 24 }, s, 40) ?: return null
        val src = Bitmap.createBitmap(mask, s, s, Bitmap.Config.ARGB_8888)
        val target = s * GLYPH
        // Вытянутые рисунки (надписи) вписываем по длинной стороне, но не крупнее полутора квадратных
        val k = target / max(box.w, box.h).toFloat() * if (box.w.toFloat() / box.h > 1.6f || box.h.toFloat() / box.w > 1.6f) 1.25f else 1f
        val w = box.w * k
        val h = box.h * k
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ALPHA_8)
        Canvas(out).drawBitmap(
            src,
            android.graphics.Rect(box.l, box.t, box.r + 1, box.b + 1),
            RectF((s - w) / 2f, (s - h) / 2f, (s + w) / 2f, (s + h) / 2f),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        src.recycle()
        return out
    }

    private fun diff(p: Int, r: Int, g: Int, b: Int): Float {
        val dr = Color.red(p) - r
        val dg = Color.green(p) - g
        val db = Color.blue(p) - b
        return sqrt((dr * dr + dg * dg + db * db) / 3f) / 255f
    }
}
