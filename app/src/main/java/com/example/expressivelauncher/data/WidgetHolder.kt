package com.example.expressivelauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/** Один AppWidgetHost на процесс. Слушает обновления, пока Activity на экране. */
class WidgetHolder(context: Context) {
    private val appContext = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val host = AppWidgetHost(appContext, HOST_ID)
    private var listening = false

    fun startListening() {
        if (listening) return
        listening = true
        runCatching { host.startListening() }
    }

    fun stopListening() {
        if (!listening) return
        listening = false
        runCatching { host.stopListening() }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()
    fun delete(id: Int) {
        views.remove(id)
        sentSizes.remove(id)
        runCatching { host.deleteAppWidgetId(id) }
    }
    fun info(id: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(id)
    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders

    /**
     * View виджета создаётся один раз и переиспользуется. Раньше она создавалась заново при каждом входе
     * в композицию, а страница стола выходит из композиции при перелистывании: каждое возвращение на страницу
     * заново раздувало RemoteViews (кадры по 25–35 мс в трассе) и будило провайдера новым размером.
     */
    private val views = HashMap<Int, AppWidgetHostView>()
    private val sentSizes = HashMap<Int, Long>()

    fun createView(context: Context, id: Int): AppWidgetHostView? {
        // View другой (уже уходящей) активности не переиспользуем: её контекст скоро умрёт вместе с ней
        views[id]?.takeIf { it.context === context }?.let { v ->
            // Прошлый контейнер Compose уже отпустил view; если нет — забираем
            (v.parent as? android.view.ViewGroup)?.removeView(v)
            return v
        }
        val info = info(id) ?: return null
        sentSizes.remove(id)
        return host.createView(context, id, info).also { views[id] = it }
    }

    /** Сообщает провайдеру размер, только если он изменился: каждое сообщение — это новый RemoteViews от провайдера. */
    fun shouldSendSize(id: Int, widthDp: Int, heightDp: Int): Boolean {
        val packed = (widthDp.toLong() shl 32) or (heightDp.toLong() and 0xFFFFFFFFL)
        if (sentSizes[id] == packed) return false
        sentSizes[id] = packed
        return true
    }

    /**
     * Активность [owner] уничтожается: её view держат её контекст, их убираем из кэша. Только её: новая активность
     * создаётся раньше, чем старая получает onDestroy, и уже успевает взять свои view — их трогать нельзя,
     * иначе виджеты на столе пропадают.
     */
    fun releaseViews(owner: Context) {
        val it = views.entries.iterator()
        while (it.hasNext()) {
            val (id, v) = it.next()
            if (v.context === owner) {
                (v.parent as? android.view.ViewGroup)?.removeView(v)
                it.remove()
                sentSizes.remove(id)
            }
        }
    }

    companion object {
        const val HOST_ID = 1024
    }
}
