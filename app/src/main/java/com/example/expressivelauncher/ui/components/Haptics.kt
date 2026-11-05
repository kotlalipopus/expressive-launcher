package com.example.expressivelauncher.ui.components

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.expressivelauncher.data.HapticLevel
import com.example.expressivelauncher.data.Settings

/** Характер отклика: от едва заметного щелчка до двойного удара при отпускании значка. */
enum class HapticKind { TICK, CLICK, HEAVY, DROP }

/** К какой группе настроек относится событие. */
enum class HapticGroup { TOUCH, DRAG, PAGES, DRAWER }

/**
 * Вибрация лаунчера. Настройки читает в момент события, поэтому её можно вызывать из долгоживущих обработчиков жестов.
 * Где есть качественный мотор, играет системные примитивы, иначе короткий импульс.
 */
class Haptics(context: Context, private val settings: () -> Settings) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else context.getSystemService(Vibrator::class.java)
    }.getOrNull()?.takeIf { it.hasVibrator() }

    val available: Boolean get() = vibrator != null

    private val cache = HashMap<Pair<HapticKind, HapticLevel>, VibrationEffect>()
    private var lastTick = 0L

    fun play(kind: HapticKind, group: HapticGroup) {
        val s = settings()
        if (!s.haptics) return
        val on = when (group) {
            HapticGroup.TOUCH -> s.hapticTouch
            HapticGroup.DRAG -> s.hapticDrag
            HapticGroup.PAGES -> s.hapticPages
            HapticGroup.DRAWER -> s.hapticDrawer
        }
        if (on) vibrate(kind, s.hapticLevel)
    }

    /** Показывает силу вибрации при выборе в настройках. */
    fun preview(level: HapticLevel) = vibrate(HapticKind.HEAVY, level)

    private fun vibrate(kind: HapticKind, level: HapticLevel) {
        val v = vibrator ?: return
        if (kind == HapticKind.TICK) {
            // Быстрая череда щелчков слилась бы в сплошное жужжание
            val now = SystemClock.uptimeMillis()
            if (now - lastTick < MIN_TICK_GAP_MS) return
            lastTick = now
        }
        runCatching { v.vibrate(cache.getOrPut(kind to level) { effect(v, kind, level) }) }
    }

    private fun effect(v: Vibrator, kind: HapticKind, level: HapticLevel): VibrationEffect {
        val power = when (level) {
            HapticLevel.LIGHT -> 0.45f
            HapticLevel.MEDIUM -> 0.75f
            HapticLevel.STRONG -> 1f
        }
        if (Build.VERSION.SDK_INT >= 30 && v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val c = VibrationEffect.startComposition()
            when (kind) {
                HapticKind.TICK -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, power * 0.7f)
                HapticKind.CLICK -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, power * 0.7f)
                HapticKind.HEAVY -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, power)
                HapticKind.DROP -> {
                    c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, power)
                    c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, power * 0.6f, 50)
                }
            }
            return c.compose()
        }
        val (ms, share) = when (kind) {
            HapticKind.TICK -> 10L to 0.5f
            HapticKind.CLICK -> 16L to 0.7f
            HapticKind.HEAVY -> 28L to 1f
            HapticKind.DROP -> 22L to 1f
        }
        // Мотор без регулировки амплитуды отличает силу только длительностью импульса
        return if (v.hasAmplitudeControl()) VibrationEffect.createOneShot(ms, (255 * power * share).toInt().coerceIn(1, 255))
        else VibrationEffect.createOneShot((ms * (0.5f + power)).toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
    }

    private companion object {
        const val MIN_TICK_GAP_MS = 35L
    }
}
