package com.example.expressivelauncher

import android.app.Application
import com.example.expressivelauncher.data.AppRepository
import com.example.expressivelauncher.data.IconThemeManager
import com.example.expressivelauncher.data.LayoutStore
import com.example.expressivelauncher.data.SettingsStore
import com.example.expressivelauncher.data.WidgetHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LauncherApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings by lazy { SettingsStore(this) }
    val layout by lazy { LayoutStore(this, appScope) }
    val repo by lazy { AppRepository(this, appScope, IconThemeManager(this), settings.state.value.iconTheme) }
    val widgets by lazy { WidgetHolder(this) }

    override fun onCreate() {
        super.onCreate()
        com.example.expressivelauncher.data.CrashLog.install(this)
        com.example.expressivelauncher.data.AppStyles.load(this)
    }
}
