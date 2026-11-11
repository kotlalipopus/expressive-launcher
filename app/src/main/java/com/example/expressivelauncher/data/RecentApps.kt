package com.example.expressivelauncher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Недавно запущенные приложения (ключи, новые первыми): из них строка предложений в меню приложений Expressive. */
object RecentApps {
    private const val PREFS = "recent_apps"
    private const val KEY = "keys"
    private const val MAX = 16

    var keys by mutableStateOf<List<String>>(emptyList())
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        keys = raw.split('\n').filter { it.isNotBlank() }
    }

    fun record(context: Context, key: String) {
        load(context)
        keys = (listOf(key) + keys.filter { it != key }).take(MAX)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, keys.joinToString("\n")).apply()
    }
}
