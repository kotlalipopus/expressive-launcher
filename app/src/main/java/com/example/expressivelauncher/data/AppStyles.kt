package com.example.expressivelauncher.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONObject

/**
 * Свой вид одного приложения: форма значка, размер, название и подпись. null — как у всех (из общих настроек).
 * Картинка значка хранится отдельно — это «Сменить значок» в [AppRepository].
 */
data class AppStyle(
    val shape: IconShape? = null,
    /** Масштаб значка, 0.6..1.4; 1 — как у всех. */
    val scale: Float = 1f,
    val label: String? = null,
    val hideLabel: Boolean = false,
) {
    val isDefault: Boolean get() = shape == null && scale == 1f && label.isNullOrBlank() && !hideLabel
}

/** Настройки вида приложений по ключу приложения. Состояние Compose: изменения сразу видны на столе и в меню. */
object AppStyles {
    private const val PREFS = "app_styles"
    private val map = mutableStateMapOf<String, AppStyle>()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            val json = (value as? String)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: continue
            map[key] = AppStyle(
                shape = json.optString("shape").takeIf { it.isNotEmpty() }?.let { n -> IconShape.entries.firstOrNull { it.name == n } },
                scale = json.optDouble("scale", 1.0).toFloat().coerceIn(0.6f, 1.4f),
                label = json.optString("label").takeIf { it.isNotBlank() },
                hideLabel = json.optBoolean("hideLabel", false),
            )
        }
    }

    fun of(key: String): AppStyle? = map[key]

    fun set(context: Context, key: String, style: AppStyle) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (style.isDefault) {
            map.remove(key)
            prefs.remove(key)
        } else {
            map[key] = style
            prefs.putString(key, JSONObject().apply {
                style.shape?.let { put("shape", it.name) }
                put("scale", style.scale.toDouble())
                style.label?.takeIf { it.isNotBlank() }?.let { put("label", it) }
                put("hideLabel", style.hideLabel)
            }.toString())
        }
        prefs.apply()
    }
}

/** Название приложения с учётом своего имени. */
val AppInfo.displayLabel: String get() = AppStyles.of(key)?.label ?: label
