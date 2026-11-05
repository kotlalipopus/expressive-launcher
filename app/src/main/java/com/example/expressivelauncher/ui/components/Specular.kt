package com.example.expressivelauncher.ui.components

import android.graphics.ComposeShader
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.min

/**
 * Кромка и блики жидкого стекла (как в iOS 26). Шейдеры зависят только от размера и силы,
 * поэтому создаются один раз в кэше отрисовки, а не на каждом кадре.
 */
object Specular {
    private fun white(alpha: Float): Int = android.graphics.Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), 255, 255, 255)

    /**
     * Шейдер для обводки ≈1 dp: тонкая полупрозрачная линия [rim] по всему краю и спекулярные блики
     * ТОЛЬКО на углах — яркий у верхнего-левого, слабее у нижнего-правого. К серединам сторон блики гаснут:
     * радиус блика — меньшая сторона, поэтому у длинной панели середина стороны остаётся тонкой линией.
     */
    fun cornerRim(w: Float, h: Float, strength: Float, rim: Float): Shader {
        val s = strength.coerceIn(0f, 1.5f)
        // Блик занимает только угол: около половины короткой стороны, дальше остаётся едва заметная линия
        val reach = min(w, h) * 0.55f
        val topLeft = RadialGradient(
            0f, 0f, reach,
            intArrayOf(white(1f * s), white(0.5f * s), white(0.12f * s), white(rim)),
            floatArrayOf(0f, 0.25f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        val bottomRight = RadialGradient(
            w, h, reach * 0.8f,
            intArrayOf(white(0.6f * s), white(0.25f * s), white(0f)),
            floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP,
        )
        return ComposeShader(topLeft, bottomRight, PorterDuff.Mode.ADD)
    }

    /** Едва заметное внутреннее свечение у верхнего-левого угла: стекло «ловит» свет, а не просто обведено. */
    fun innerGloss(w: Float, h: Float, strength: Float): Shader =
        RadialGradient(
            0f, 0f, min(w, h) * 1.1f,
            intArrayOf(white(0.16f * strength), white(0.04f * strength), white(0f)),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
}
