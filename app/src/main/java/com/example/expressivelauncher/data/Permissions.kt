package com.example.expressivelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings as SysSettings

/**
 * Проверки доступа, от которых зависят функции лаунчера. Читаются из системных настроек,
 * а не из состояния своих служб: на MIUI/HyperOS, One UI и ColorOS служба может быть включена,
 * но ещё не привязана после перезапуска процесса.
 */
object Permissions {

    fun isDefaultHome(ctx: Context): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = runCatching { ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) }.getOrNull()
        return ri?.activityInfo?.packageName == ctx.packageName
    }

    /** Доступ к уведомлениям (бейджи на значках). */
    fun hasNotificationAccess(ctx: Context): Boolean {
        val enabled = runCatching { SysSettings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") }.getOrNull().orEmpty()
        val me = ComponentName(ctx, BadgeListenerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** Служба специальных возможностей (блокировка экрана и шторка жестом). */
    fun hasAccessibility(ctx: Context): Boolean {
        if (GestureAccessibilityService.instance != null) return true
        val enabled = runCatching {
            SysSettings.Secure.getString(ctx.contentResolver, SysSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        val me = ComponentName(ctx, GestureAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** Доступ ко всем файлам: без него система не отдаёт картинку обоев для размытия. */
    fun hasAllFilesAccess(): Boolean = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    /** Доступ к ярлыкам приложений есть только у лаунчера по умолчанию. */
    fun hasShortcutAccess(ctx: Context): Boolean = runCatching {
        ctx.getSystemService(android.content.pm.LauncherApps::class.java).hasShortcutHostPermission()
    }.getOrDefault(false)

    /** Оболочка производителя: от неё зависят обходные пути (автозапуск, ограничения фона). */
    val vendorSkin: String by lazy {
        fun prop(name: String): String = runCatching {
            @Suppress("PrivateApi")
            Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as String
        }.getOrDefault("")
        val m = Build.MANUFACTURER.lowercase()
        when {
            prop("ro.mi.os.version.name").isNotEmpty() -> "HyperOS"
            prop("ro.miui.ui.version.name").isNotEmpty() -> "MIUI"
            m == "samsung" -> "One UI"
            prop("ro.build.version.opporom").isNotEmpty() || m == "oppo" || m == "realme" ->
                if (m == "oneplus") "OxygenOS" else "ColorOS"
            m == "oneplus" -> "OxygenOS"
            prop("ro.build.version.emui").isNotEmpty() || m == "huawei" || m == "honor" -> if (m == "honor") "MagicOS" else "EMUI"
            m == "vivo" -> "OriginOS / Funtouch"
            m == "google" -> "Pixel"
            else -> "Android"
        } + " · Android " + Build.VERSION.RELEASE
    }

    /** Экран, где оболочка разрешает автозапуск и работу в фоне; null — у оболочки такого нет. */
    fun backgroundSettingsIntent(ctx: Context): Intent? {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        )
        val pm = ctx.packageManager
        for (c in candidates) {
            val i = Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { pm.resolveActivity(i, 0) }.getOrNull() != null) return i
        }
        return null
    }
}
