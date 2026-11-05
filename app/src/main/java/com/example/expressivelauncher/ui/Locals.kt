package com.example.expressivelauncher.ui

import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.expressivelauncher.data.AppRepository
import com.example.expressivelauncher.data.Settings

val LocalRepo = staticCompositionLocalOf<AppRepository> { error("AppRepository not provided") }
val LocalHaptics = staticCompositionLocalOf<com.example.expressivelauncher.ui.components.Haptics> { error("Haptics not provided") }
val LocalSettings = compositionLocalOf { Settings() }

/**
 * Итоговые параметры эффектов с учётом режима энергосбережения.
 * [lite] — упрощённая графика, [blur] — разрешено размытие внутри окна, [speed] и [bounce] — настройка анимаций.
 */
@androidx.compose.runtime.Immutable
data class Fx(val lite: Boolean = false, val blur: Boolean = true, val speed: Float = 1f, val bounce: Float = 1f)

val LocalFx = compositionLocalOf { Fx() }
/**
 * Счётчики непрочитанных по приложениям. Отдаётся состоянием, а не готовой картой: уведомление
 * приходит часто, и его обновление должно перерисовывать только значки, а не весь лаунчер.
 */
val LocalBadges = compositionLocalOf<androidx.compose.runtime.State<Map<String, Int>>> { EmptyBadges }
private val EmptyBadges = androidx.compose.runtime.mutableStateOf<Map<String, Int>>(emptyMap())
/** Стиль текста часов: шрифт и жирность уже применены, размер задаёт место использования. */
val LocalClockStyle = compositionLocalOf { androidx.compose.ui.text.TextStyle.Default }
val LocalHostView = staticCompositionLocalOf<View?> { null }
/** Обои светлые: подписи и значки строки состояния на столе должны быть тёмными. */
val LocalWallpaperLight = compositionLocalOf { false }
