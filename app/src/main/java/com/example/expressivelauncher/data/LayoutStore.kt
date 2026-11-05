package com.example.expressivelauncher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Раскладка рабочего стола. Хранится в JSON, запись с задержкой, чтобы не писать на каждый кадр перетаскивания. */
class LayoutStore(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "layout.json")
    private val prefs = context.getSharedPreferences("layout_meta", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<HomeItem>> = _items.asStateFlow()
    private var saveJob: Job? = null

    var seeded: Boolean
        get() = prefs.getBoolean("seeded", false)
        set(v) = prefs.edit().putBoolean("seeded", v).apply()

    fun update(transform: (List<HomeItem>) -> List<HomeItem>) {
        val old = _items.value
        val new = transform(old)
        if (new == old) return
        _items.value = new
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(400)
            write(new)
        }
    }

    /** Сохраняет немедленно: процесс лаунчера могут завершить сразу после ухода с экрана. */
    fun flush() {
        if (saveJob?.isActive != true) return
        saveJob?.cancel()
        val snapshot = _items.value
        scope.launch(Dispatchers.IO) { write(snapshot) }
    }

    /** Запись через временный файл: сбой посреди записи не оставит пользователя с пустым столом. */
    @Synchronized
    private fun write(items: List<HomeItem>) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(encode(items))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }

    fun exportJson(): String = encode(_items.value)

    /** Разбирает файл раскладки. Идентификаторы виджетов привязаны к устройству, поэтому их не переносим. */
    fun parseImport(json: String): List<HomeItem>? = runCatching {
        decode(json).filter { it.type != ItemType.WIDGET }
    }.getOrNull()

    fun exportObject(): JSONObject = JSONObject(encode(_items.value))

    /** Повреждённый файл не должен оставить пустой стол: тогда берём прошлую удачную копию. */
    private fun load(): List<HomeItem> {
        val backup = File(file.parentFile, "${file.name}.bak")
        val main = runCatching { if (file.exists()) decode(file.readText()) else null }.getOrNull()
        if (main != null) {
            runCatching { file.copyTo(backup, overwrite = true) }
            return main
        }
        val restored = runCatching { if (backup.exists()) decode(backup.readText()) else null }.getOrNull() ?: return emptyList()
        runCatching { backup.copyTo(file, overwrite = true) }
        return restored
    }

    private fun encode(items: List<HomeItem>): String {
        val arr = JSONArray()
        for (i in items) {
            arr.put(JSONObject().apply {
                put("id", i.id); put("t", i.type.name)
                put("p", i.page); put("x", i.x); put("y", i.y); put("w", i.w); put("h", i.h)
                i.appKey?.let { put("k", it) }
                if (i.title.isNotEmpty()) put("n", i.title)
                if (i.children.isNotEmpty()) put("c", JSONArray(i.children))
                if (i.widgetId >= 0) put("wid", i.widgetId)
            })
        }
        return JSONObject().put("v", 1).put("items", arr).toString()
    }

    private fun decode(json: String): List<HomeItem> {
        val arr = JSONObject(json).getJSONArray("items")
        // Один испорченный или незнакомый элемент (например, из новой версии) не должен ронять всю раскладку
        return (0 until arr.length()).mapNotNull { idx -> runCatching {
            val o = arr.getJSONObject(idx)
            val c = o.optJSONArray("c")
            HomeItem(
                id = o.getString("id"),
                type = ItemType.valueOf(o.getString("t")),
                page = o.getInt("p"), x = o.getInt("x"), y = o.getInt("y"),
                w = o.optInt("w", 1), h = o.optInt("h", 1),
                appKey = o.optString("k").ifEmpty { null },
                title = o.optString("n"),
                children = if (c == null) emptyList() else List(c.length()) { c.getString(it) },
                widgetId = o.optInt("wid", -1),
            )
        }.getOrNull() }
    }
}
