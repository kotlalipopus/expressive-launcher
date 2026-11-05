package com.example.expressivelauncher.data

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Счётчики уведомлений по пакетам для бейджей на иконках. */
object Badges {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()
    fun set(map: Map<String, Int>) { _counts.value = map }
}

class BadgeListenerService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() {
        Badges.set(emptyMap())
        // Оболочки с агрессивным энергосбережением (MIUI, ColorOS, EMUI) отвязывают слушателя — просим вернуть
        runCatching { requestRebind(android.content.ComponentName(this, BadgeListenerService::class.java)) }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = runCatching { activeNotifications }.getOrNull() ?: return
        Badges.set(
            list.asSequence()
                .filter { it.isClearable && it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY == 0 }
                .groupingBy { it.packageName }
                .eachCount()
        )
    }
}

/** Нужен для жестов «заблокировать экран» и «открыть шторку» там, где системные вызовы недоступны. */
class GestureAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: GestureAccessibilityService? = null

        fun lockScreen(): Boolean =
            Build.VERSION.SDK_INT >= 28 && instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        fun openNotifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true
    }
}
