package com.example.expressivelauncher.data

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Xml
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Тема значков. [id]: `pack:<пакет>` — установленный набор (формат ADW/Nova),
 * `file:<папка>` — импортированный пользователем ZIP.
 */
@Immutable
data class IconThemeInfo(val id: String, val name: String, val imported: Boolean)

/** Разбор файла appfilter.xml: чистая логика, без Android-ресурсов. */
class AppFilter {
    val components = HashMap<String, String>()
    val packages = HashMap<String, String>()
    val backs = ArrayList<String>()
    var mask: String? = null
    var upon: String? = null
    var scale: Float = 1f

    fun addItem(component: String, drawable: String) {
        val (pkg, cls) = parseComponent(component) ?: return
        components["$pkg/$cls"] = drawable
        packages.putIfAbsent(pkg, drawable)
    }

    fun drawableFor(pkg: String, cls: String): String? = components["$pkg/$cls"] ?: packages[pkg]

    companion object {
        /** `ComponentInfo{пакет/класс}` → пакет и полное имя класса; короткую форму `.Main` раскрывает. */
        fun parseComponent(raw: String): Pair<String, String>? {
            val body = raw.trim().removePrefix("ComponentInfo{").removeSuffix("}")
            val slash = body.indexOf('/')
            if (slash <= 0 || slash == body.lastIndex) return null
            val pkg = body.substring(0, slash)
            val cls = body.substring(slash + 1)
            return pkg to if (cls.startsWith(".")) pkg + cls else cls
        }
    }
}

/** Загруженная тема: отдаёт исходные изображения значков. */
abstract class LoadedIconTheme(val filter: AppFilter) {
    abstract fun drawable(name: String): Drawable?

    /** Значок приложения из темы либо null. Файловая тема дополнительно ищет по имени пакета. */
    open fun iconFor(pkg: String, cls: String): Drawable? = filter.drawableFor(pkg, cls)?.let(::drawable)

    val hasShapeTreatment: Boolean get() = filter.backs.isNotEmpty() || filter.mask != null || filter.upon != null
}

private class PackTheme(private val pkg: String, private val res: Resources, filter: AppFilter) : LoadedIconTheme(filter) {
    @Suppress("DiscouragedApi")
    override fun drawable(name: String): Drawable? = runCatching {
        val id = res.getIdentifier(name, "drawable", pkg).takeIf { it != 0 }
            ?: res.getIdentifier(name, "mipmap", pkg).takeIf { it != 0 }
            ?: return null
        res.getDrawableForDensity(id, DisplayMetrics.DENSITY_XXXHIGH, null)
    }.getOrNull()
}

private class FileTheme(private val dir: File, private val res: Resources, filter: AppFilter) : LoadedIconTheme(filter) {
    private val files: Map<String, File> = dir.listFiles().orEmpty()
        .filter { it.extension.lowercase() in IMAGE_EXT }
        .associateBy { it.nameWithoutExtension.lowercase() }

    override fun drawable(name: String): Drawable? {
        val f = files[name.lowercase()] ?: return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_ICON_PX && bounds.outHeight / (sample * 2) >= MAX_ICON_PX) sample *= 2
            val bmp: Bitmap = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            BitmapDrawable(res, bmp)
        }.getOrNull()
    }

    override fun iconFor(pkg: String, cls: String): Drawable? =
        super.iconFor(pkg, cls) ?: drawable("$pkg/$cls".replace('/', '_')) ?: drawable(pkg)

    private companion object {
        const val MAX_ICON_PX = 384
    }
}

private val IMAGE_EXT = setOf("png", "webp", "jpg", "jpeg")

class IconThemeManager(private val context: Context) {
    private val root = File(context.filesDir, "icon_themes")
    private val _themes = MutableStateFlow<List<IconThemeInfo>>(emptyList())
    val themes: StateFlow<List<IconThemeInfo>> = _themes.asStateFlow()

    /** Ищет установленные наборы и импортированные темы. Вызывать с фонового потока. */
    fun refresh() {
        val pm = context.packageManager
        val packs = PACK_ACTIONS.flatMap { action ->
            runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList())
        }.distinctBy { it.activityInfo.packageName }.map {
            IconThemeInfo("pack:${it.activityInfo.packageName}", it.loadLabel(pm).toString(), imported = false)
        }
        val files = root.listFiles().orEmpty().filter { it.isDirectory }.map { dir ->
            val name = runCatching { JSONObject(File(dir, META).readText()).optString("name") }.getOrNull().orEmpty()
            IconThemeInfo("file:${dir.name}", name.ifBlank { dir.name }, imported = true)
        }
        _themes.value = (files + packs).sortedBy { it.name.lowercase() }
    }

    fun load(id: String): LoadedIconTheme? = runCatching {
        when {
            id.startsWith("pack:") -> {
                val pkg = id.removePrefix("pack:")
                val res = context.packageManager.getResourcesForApplication(pkg)
                PackTheme(pkg, res, readPackFilter(pkg, res))
            }
            id.startsWith("file:") -> {
                val dir = File(root, id.removePrefix("file:").let(::safeName))
                if (!dir.isDirectory) return null
                val xml = File(dir, FILTER)
                val filter = if (xml.exists()) xml.inputStream().use(::parseStream) else AppFilter()
                FileTheme(dir, context.resources, filter)
            }
            else -> null
        }
    }.getOrNull()

    private fun readPackFilter(pkg: String, res: Resources): AppFilter {
        @Suppress("DiscouragedApi")
        val xmlId = res.getIdentifier("appfilter", "xml", pkg)
        if (xmlId != 0) return res.getXml(xmlId).use { parse(it) }
        return res.assets.open(FILTER).use(::parseStream)
    }

    private fun parseStream(input: InputStream): AppFilter {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)
        return parse(parser)
    }

    private fun parse(p: XmlPullParser): AppFilter {
        val f = AppFilter()
        fun attr(name: String) = p.getAttributeValue(null, name)
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG) when (p.name) {
                "item" -> {
                    val c = attr("component")
                    val d = attr("drawable")
                    if (c != null && d != null) f.addItem(c, d)
                }
                "iconback" -> (0 until p.attributeCount).mapTo(f.backs) { p.getAttributeValue(it) }
                "iconmask" -> f.mask = attr("img1") ?: f.mask
                "iconupon" -> f.upon = attr("img1") ?: f.upon
                "scale" -> attr("factor")?.toFloatOrNull()?.let { f.scale = it.coerceIn(0.1f, 2f) }
            }
            p.next()
        }
        return f
    }

    /**
     * Импортирует ZIP с темой. Пути внутри архива отбрасываются: файл сохраняется только под своим
     * очищенным именем, поэтому записать что-либо за пределы папки темы нельзя. Возвращает тему или null.
     */
    fun importZip(uri: Uri): IconThemeInfo? {
        val dir = File(root, UUID.randomUUID().toString())
        val ok = runCatching {
            dir.mkdirs()
            var total = 0L
            var count = 0
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val base = entry.name.substringAfterLast('/').substringAfterLast('\\')
                        val ext = base.substringAfterLast('.', "").lowercase()
                        val keep = ext in IMAGE_EXT || base.equals(FILTER, true) || base.equals(THEME_JSON, true)
                        if (!keep || base.startsWith(".")) continue
                        check(++count <= MAX_FILES) { "too many files" }
                        val target = File(dir, safeName(if (ext in IMAGE_EXT) base else base.lowercase()))
                        target.outputStream().use { out ->
                            val buf = ByteArray(16 * 1024)
                            var entryBytes = 0L
                            while (true) {
                                val n = zip.read(buf)
                                if (n < 0) break
                                entryBytes += n
                                total += n
                                check(entryBytes <= MAX_FILE_BYTES && total <= MAX_TOTAL_BYTES) { "archive too large" }
                                out.write(buf, 0, n)
                            }
                        }
                    }
                }
            } ?: error("cannot open")
            check(dir.listFiles().orEmpty().any { it.extension.lowercase() in IMAGE_EXT }) { "no icons" }
            val filterFile = File(dir, FILTER)
            if (filterFile.exists()) filterFile.inputStream().use(::parseStream) // повреждённый XML отклоняем сразу
            val declared = runCatching { JSONObject(File(dir, THEME_JSON).readText()).optString("name") }.getOrNull().orEmpty()
            File(dir, META).writeText(JSONObject().put("name", declared.ifBlank { displayName(uri) }).toString())
        }.isSuccess
        if (!ok) {
            dir.deleteRecursively()
            return null
        }
        refresh()
        return IconThemeInfo("file:${dir.name}", _themes.value.firstOrNull { it.id == "file:${dir.name}" }?.name ?: dir.name, true)
    }

    fun delete(id: String) {
        if (!id.startsWith("file:")) return
        File(root, safeName(id.removePrefix("file:"))).deleteRecursively()
        refresh()
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.').orEmpty().ifBlank { "Theme" }

    private fun safeName(name: String) = name.replace(Regex("[^A-Za-z0-9._-]"), "_").trimStart('.')

    private companion object {
        const val FILTER = "appfilter.xml"
        const val THEME_JSON = "theme.json"
        const val META = ".meta.json"
        const val MAX_FILES = 20_000
        const val MAX_FILE_BYTES = 8L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 400L * 1024 * 1024
        val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme", "com.anddoes.launcher.THEME", "app.lawnchair.icons.THEMED_ICON",
        )
    }
}
