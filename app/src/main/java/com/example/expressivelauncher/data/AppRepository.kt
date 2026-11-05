package com.example.expressivelauncher.data

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Иконка без маски: форму накладывает UI, поэтому её можно менять на лету.
 * [custom] — готовый значок из темы со своей формой; [accent] — средний цвет для анимации запуска (0, если неизвестен).
 */
@Immutable
class AppIcon(
    val image: ImageBitmap,
    val legacy: Boolean,
    val mono: ImageBitmap?,
    val custom: Boolean = false,
    val accent: Int = 0,
    /** Передний слой adaptive-иконки без фона: цветной логотип для прозрачных значков Liquid Glass. */
    val fg: ImageBitmap? = null,
    /**
     * [mono] дало само приложение (системный монохромный слой Android 13+), а не синтезировал лаунчер.
     * Тематическими значками становятся только такие: синтезированный рисунок у части приложений выходил кривым.
     */
    val monoNative: Boolean = false,
)

@Immutable
class ShortcutEntry(val info: ShortcutInfo, val label: String, val icon: ImageBitmap?)

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    val themes: IconThemeManager,
    initialTheme: String,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Быстрый поиск по ключу; пересобирается вместе со списком. */
    @Volatile
    var byKey: Map<String, AppInfo> = emptyMap()
        private set

    /** Растёт, когда значки нужно перечитать: сменилась тема или обновилось приложение. */
    var iconEpoch by mutableIntStateOf(0)
        private set

    private val iconPx = (context.resources.displayMetrics.density * 56f * 1.25f).toInt().coerceIn(128, 224)

    // Растры лежат в видеопамяти, в куче остаются только монохромные слои, поэтому ограничиваем число значков
    private val cache = LruCache<String, AppIcon>(MAX_CACHED_ICONS)

    /** Описания активностей из последнего обхода: повторный запрос на каждый значок — это межпроцессный вызов. */
    @Volatile
    private var activityInfos: Map<String, LauncherActivityInfo> = emptyMap()

    /** Время обновления пакета: один запрос на приложение вместо запроса на каждый значок и тему. */
    private val updatedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val overridesDir = File(context.filesDir, "icon_overrides")
    @Volatile
    private var overrideKeys: Set<String> = emptySet()

    @Volatile
    var iconThemeId: String = initialTheme
        private set
    @Volatile
    private var theme: LoadedIconTheme? = null

    private var refreshJob: Job? = null
    private var preloadJob: Job? = null
    private var themeJob: Job? = null
    private val preloadDispatcher = Dispatchers.Default.limitedParallelism(2)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            purgeIcons(packageName)
            refreshThemes()
            scheduleRefresh()
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            updatedAt.remove(packageName)
            refreshThemes()
            scheduleRefresh()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            purgeIcons(packageName)
            scheduleRefresh()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            packageNames.forEach(::purgeIcons)
            scheduleRefresh()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = scheduleRefresh()
    }

    /** Темы и свои значки прочитаны: до этого значки не рисуем, иначе они мелькнули бы без темы. */
    private val iconsReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    init {
        launcherApps.registerCallback(callback)
        // Список приложений не ждёт тем значков: на холодном старте стол появляется сразу, а не через секунду
        scheduleRefresh(0)
        scope.launch(Dispatchers.Default) {
            // Кэш прошлой версии значков больше не читается
            runCatching { listOf("icons_v2", "icons_v3", "icons_v4", "icons_v5", "icons_v6", "icons_v7").forEach { File(context.cacheDir, it).takeIf { d -> d.exists() }?.deleteRecursively() } }
            overrideKeys = overridesDir.list()?.map { it.removeSuffix(".png") }?.toSet().orEmpty()
            themes.refresh()
            if (iconThemeId.isNotEmpty()) theme = themes.load(iconThemeId)
            iconsReady.complete(Unit)
        }
    }

    private fun scheduleRefresh(delayMs: Long = 250) {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.Default) {
            delay(delayMs)
            val list = queryApps()
            byKey = list.associateBy { it.key }
            _apps.value = list
            _loaded.value = true
            bumpEpoch()
            preloadIcons(list)
        }
    }

    private suspend fun bumpEpoch() = withContext(Dispatchers.Main) { iconEpoch++ }

    private fun queryApps(): List<AppInfo> {
        val result = ArrayList<AppInfo>(256)
        val infos = HashMap<String, LauncherActivityInfo>(256)
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val isWork = user != android.os.Process.myUserHandle()
            for (info in launcherApps.getActivityList(null, user)) {
                val cn = info.componentName
                val key = "${cn.packageName}/${cn.className}#$serial"
                infos[key] = info
                result += AppInfo(
                    key = key,
                    packageName = cn.packageName,
                    className = cn.className,
                    label = info.label?.toString().orEmpty().ifBlank { cn.packageName },
                    user = user,
                    userSerial = serial,
                    isWork = isWork,
                    installTime = info.firstInstallTime,
                    isSystem = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }
        result.sortWith(compareBy({ it.sortKey }, { it.userSerial }))
        activityInfos = infos
        return result
    }

    /** Пакет реально удалён: отключённые и временно недоступные приложения сюда не попадают. */
    fun isPackageGone(pkg: String, userSerial: Long): Boolean {
        val user = userManager.getUserForSerialNumber(userSerial) ?: return true
        return runCatching { launcherApps.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES, user) }.isFailure
    }

    // ---------- Иконки ----------

    fun cachedIcon(app: AppInfo): AppIcon? = cache.get(app.key)

    suspend fun icon(app: AppInfo): AppIcon? {
        cache.get(app.key)?.let { return it }
        iconsReady.await()
        return withContext(Dispatchers.Default) { loadIcon(app) }
    }

    private fun loadIcon(app: AppInfo): AppIcon? {
        cache.get(app.key)?.let { return it }
        return runCatching {
            val icon = loadOverride(app) ?: run {
                val dk = diskKey(app)
                dk?.let(::readDisk) ?: (loadThemed(app) ?: render(systemDrawable(app) ?: return null)).also { if (dk != null) writeDisk(dk, it) }
            }
            cache.put(app.key, icon)
            icon
        }.getOrNull()
    }

    // ---------- Дисковый кэш значков: на холодном старте значки читаются готовыми, а не рисуются заново ----------

    // v9: в meta добавлен признак собственного монохромного слоя — старый кэш пересоздаётся
    private val diskDir = File(context.cacheDir, "icons_v9")

    /** Ключ меняется вместе с темой значков, обновлением приложения и размером — устаревший файл просто не найдётся. */
    private fun diskKey(app: AppInfo): String? {
        val updated = updatedAt.getOrPut(app.packageName) {
            runCatching { context.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime }.getOrDefault(-1L)
        }
        if (updated < 0) return null
        return sha("${app.key}|$iconThemeId|$updated|$iconPx")
    }

    private fun readDisk(key: String): AppIcon? = runCatching {
        val meta = File(diskDir, "$key.meta")
        if (!meta.exists()) return null
        val parts = meta.readText().split(',')
        val (legacy, custom, accent) = parts
        val monoNative = parts.getOrNull(3) == "1"
        val image = BitmapFactory.decodeFile(File(diskDir, "$key.png").path) ?: return null
        val monoFile = File(diskDir, "$key.m.png")
        val mono = if (monoFile.exists()) BitmapFactory.decodeFile(monoFile.path)?.let { m -> m.extractAlpha().also { m.recycle() } } else null
        val fgFile = File(diskDir, "$key.f.png")
        val fg = if (fgFile.exists()) BitmapFactory.decodeFile(fgFile.path)?.toDisplay() else null
        AppIcon(image.toDisplay(), legacy = legacy == "1", mono = mono?.asImageBitmap(), custom = custom == "1", accent = accent.toInt(), fg = fg, monoNative = monoNative)
    }.getOrNull()

    /** WebP без потерь кодируется заметно быстрее PNG и меньше весит: запись кэша не отнимает кадры. */
    private fun iconFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.PNG

    private fun writeDisk(key: String, icon: AppIcon) {
        runCatching {
            diskDir.mkdirs()
            val format = iconFormat()
            val image = icon.image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
            File(diskDir, "$key.png").outputStream().buffered(64 * 1024).use { image.compress(format, 100, it) }
            image.recycle()
            icon.mono?.let { m ->
                val src = m.asAndroidBitmap()
                val argb = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                Canvas(argb).drawBitmap(src, 0f, 0f, Paint().apply { color = android.graphics.Color.BLACK })
                File(diskDir, "$key.m.png").outputStream().buffered(64 * 1024).use { argb.compress(format, 100, it) }
                argb.recycle()
            }
            icon.fg?.let { f ->
                val soft = f.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
                File(diskDir, "$key.f.png").outputStream().buffered(64 * 1024).use { soft.compress(format, 100, it) }
                soft.recycle()
            }
            // meta пишется последним: по нему файл считается готовым
            File(diskDir, "$key.meta").writeText("${if (icon.legacy) 1 else 0},${if (icon.custom) 1 else 0},${icon.accent},${if (icon.monoNative) 1 else 0}")
        }
    }

    private fun systemDrawable(app: AppInfo): Drawable? {
        val info = activityInfos[app.key]
            ?: launcherApps.getActivityList(app.packageName, app.user).firstOrNull { it.componentName.className == app.className }
        return info?.getIcon(0)
    }

    private fun loadThemed(app: AppInfo): AppIcon? {
        val t = theme ?: return null
        t.iconFor(app.packageName, app.className)?.let { return renderPackIcon(it) }
        if (!t.hasShapeTreatment) return null
        return runCatching { composeWithTheme(t, app) }.getOrNull()
    }

    private fun renderPackIcon(d: Drawable): AppIcon {
        if (d is AdaptiveIconDrawable) return render(d)
        val bmp = createBitmap(iconPx, iconPx)
        d.setBounds(0, 0, iconPx, iconPx)
        d.draw(Canvas(bmp))
        return AppIcon(bmp.toDisplay(), legacy = false, mono = null, custom = true, accent = bmp.accent())
    }

    /** Значка в теме нет: применяем её подложку, маску и накладку к обычной иконке (как в наборах ADW/Nova). */
    private fun composeWithTheme(t: LoadedIconTheme, app: AppInfo): AppIcon? {
        val source = systemDrawable(app) ?: return null
        val s = iconPx
        val inner = if (source is AdaptiveIconDrawable) renderLayers(s, source.background, source.foreground) else createBitmap(s, s).also {
            source.setBounds(0, 0, s, s)
            source.draw(Canvas(it))
        }
        val out = createBitmap(s, s)
        val c = Canvas(out)
        val backs = t.filter.backs
        if (backs.isNotEmpty()) t.drawable(backs[(app.packageName.hashCode() and Int.MAX_VALUE) % backs.size])?.let {
            it.setBounds(0, 0, s, s)
            it.draw(c)
        }
        val inset = ((1f - t.filter.scale) * s / 2f).toInt()
        c.drawBitmap(inner, null, Rect(inset, inset, s - inset, s - inset), Paint(Paint.FILTER_BITMAP_FLAG))
        t.filter.mask?.let(t::drawable)?.let { m ->
            val layer = createBitmap(s, s)
            m.setBounds(0, 0, s, s)
            m.draw(Canvas(layer))
            c.drawBitmap(layer, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) })
        }
        t.filter.upon?.let(t::drawable)?.let {
            it.setBounds(0, 0, s, s)
            it.draw(c)
        }
        return AppIcon(out.toDisplay(), legacy = false, mono = null, custom = true, accent = out.accent())
    }

    private fun render(d: Drawable): AppIcon {
        val s = iconPx
        if (d is AdaptiveIconDrawable) {
            val full = renderLayers(s, d.background, d.foreground)
            val fg = renderLayers(s, null, d.foreground)
            val native = if (Build.VERSION.SDK_INT >= 33) d.monochrome?.let { MonoSynth.fixSystem(renderLayers(s, null, it, alphaOnly = true)) } else null
            // Синтезированный слой оставляем для «тонированных» значков Liquid Glass, тематическими он не станет
            val mono = native ?: runCatching { MonoSynth.fromAdaptive(fg, full) }.getOrNull()
            return AppIcon(full.toDisplay(), legacy = false, mono = mono?.asImageBitmap(), accent = full.accent(), fg = fg.toDisplay(), monoNative = native != null)
        }
        // Обычная иконка: вписываем с полями, фон дорисует UI
        val bmp = createBitmap(s, s)
        val inset = (s * 0.16f).toInt()
        d.setBounds(inset, inset, s - inset, s - inset)
        d.draw(Canvas(bmp))
        val mono = runCatching { MonoSynth.fromLegacy(bmp) }.getOrNull()
        return AppIcon(bmp.toDisplay(), legacy = true, mono = mono?.asImageBitmap(), accent = bmp.accent())
    }

    /** Аппаратный растр не занимает кучу и рисуется без повторной загрузки текстуры. */
    private fun Bitmap.toDisplay(): ImageBitmap =
        (copy(Bitmap.Config.HARDWARE, false) ?: this).asImageBitmap()

    private fun Bitmap.accent(): Int = runCatching {
        val px = Bitmap.createScaledBitmap(this, 1, 1, true).getPixel(0, 0)
        if (android.graphics.Color.alpha(px) < 24) 0 else px or (0xFF shl 24)
    }.getOrDefault(0)

    /** Слои adaptive-иконки лежат в области 108dp, видимая часть — центральные 72dp. */
    private fun renderLayers(size: Int, bg: Drawable?, fg: Drawable?, alphaOnly: Boolean = false): Bitmap {
        val bmp = if (alphaOnly) Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8) else createBitmap(size, size)
        val c = Canvas(bmp)
        val pad = (size * 0.25f).toInt()
        for (layer in arrayOf(bg, fg)) {
            layer ?: continue
            layer.setBounds(-pad, -pad, size + pad, size + pad)
            layer.draw(c)
        }
        return bmp
    }

    private fun purgeIcons(pkg: String) {
        updatedAt.remove(pkg)
        for (k in cache.snapshot().keys) if (k.startsWith("$pkg/")) cache.remove(k)
    }

    private fun preloadIcons(list: List<AppInfo>) {
        preloadJob?.cancel()
        preloadJob = scope.launch(preloadDispatcher) {
            iconsReady.await()
            // Первые кадры и анимация входа важнее: чтение сотен значков в этот момент только отнимало бы
            // процессор у интерфейса. Видимые значки стол и шторка загрузят сами, остальное — после паузы.
            delay(PRELOAD_DELAY_MS)
            for (a in list.take(PRELOAD_LIMIT)) {
                ensureActive()
                if (cache.get(a.key) == null) loadIcon(a)
            }
        }
    }

    // ---------- Темы значков ----------

    fun setIconTheme(id: String) {
        if (id == iconThemeId) return
        iconThemeId = id
        themeJob?.cancel()
        themeJob = scope.launch(Dispatchers.Default) {
            theme = if (id.isEmpty()) null else themes.load(id)
            cache.evictAll()
            bumpEpoch()
            preloadIcons(_apps.value)
        }
    }

    /** Значки для карточки темы в настройках; в общий кэш не попадают. */
    suspend fun previewIcons(id: String, limit: Int = 5): List<ImageBitmap> = withContext(Dispatchers.Default) {
        val t = (if (id == iconThemeId) theme else themes.load(id)) ?: return@withContext emptyList()
        val out = ArrayList<ImageBitmap>(limit)
        for (a in _apps.value) {
            if (out.size >= limit) break
            ensureActive()
            val d = t.iconFor(a.packageName, a.className) ?: continue
            runCatching { out += renderPackIcon(d).image }
        }
        out
    }

    fun refreshThemes() { scope.launch(Dispatchers.Default) { themes.refresh() } }

    // ---------- Свой значок приложения ----------

    private fun sha(key: String) = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun overrideFile(key: String) = File(overridesDir, sha(key) + ".png")

    fun hasOverride(app: AppInfo): Boolean = sha(app.key) in overrideKeys

    private fun loadOverride(app: AppInfo): AppIcon? {
        if (sha(app.key) !in overrideKeys) return null
        val bmp = BitmapFactory.decodeFile(overrideFile(app.key).path) ?: return null
        return AppIcon(bmp.toDisplay(), legacy = false, mono = null, accent = bmp.accent())
    }

    /** Обрезает картинку по центру в квадрат и сохраняет как значок приложения. */
    suspend fun setOverride(app: AppInfo, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= iconPx) sample *= 2
            val src = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("not an image")
            val side = minOf(src.width, src.height)
            val out = createBitmap(iconPx, iconPx)
            Canvas(out).drawBitmap(
                src,
                Rect((src.width - side) / 2, (src.height - side) / 2, (src.width + side) / 2, (src.height + side) / 2),
                Rect(0, 0, iconPx, iconPx),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
            overridesDir.mkdirs()
            overrideFile(app.key).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            overrideKeys = overrideKeys + sha(app.key)
        }.isSuccess
        if (ok) { cache.remove(app.key); bumpEpoch() }
        ok
    }

    fun clearOverride(app: AppInfo) {
        overrideFile(app.key).delete()
        overrideKeys = overrideKeys - sha(app.key)
        cache.remove(app.key)
        iconEpoch++
    }

    // ---------- Запуск и действия ----------

    /** Окно приложения раскрывается из [bounds]. Возвращает false, если запустить не удалось. */
    /**
     * [portal] — анимацию рисует сам лаунчер, и приложение запускается, когда карточка-портал уже на весь экран.
     * Окно «вырастает» из полного экрана, то есть система только проявляет его поверх карточки. Свою анимацию
     * (makeCustomAnimation) Android 13+ для окна другой задачи не применяет. Иначе окно раскрывается системой из значка.
     */
    fun launch(app: AppInfo, view: View?, bounds: Rect?, portal: Boolean = false): Boolean = runCatching {
        val opts = when {
            portal && view != null && view.width > 0 ->
                ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.width, view.height).toBundle()
            view != null && bounds != null ->
                ActivityOptions.makeScaleUpAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
            else -> null
        }
        launcherApps.startMainActivity(app.component, app.user, bounds, opts)
    }.isSuccess

    // ---------- Цвет стартового окна ----------

    /** Ключ — приложение и ночной режим: у тёмной и светлой темы сплэш разный. Значение 0 — цвет узнать не удалось. */
    private val splashColors = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private fun splashKey(app: AppInfo): String =
        app.key + "|" + (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)

    /** Цвет фона, с которым система покажет окно приложения; null — ещё не посчитан или неизвестен. Без IPC. */
    fun cachedSplashColor(app: AppInfo): Int? = splashColors[splashKey(app)]?.takeIf { it != 0 }

    /**
     * Читает из темы приложения фон сплэша (Android 12+) или окна. Карточка запуска приходит к этому цвету,
     * и окно приложения проявляется поверх без светлой или тёмной вспышки. Вызывать не в главном потоке.
     */
    fun warmSplashColor(app: AppInfo) {
        val key = splashKey(app)
        if (splashColors.containsKey(key)) return
        splashColors[key] = runCatching { resolveSplashColor(app) }.getOrNull() ?: 0
    }

    private fun resolveSplashColor(app: AppInfo): Int? {
        val info = context.packageManager.getActivityInfo(app.component, 0)
        val themeRes = info.themeResource.takeIf { it != 0 } ?: info.applicationInfo.theme
        if (themeRes == 0) return null
        val pkg = context.createPackageContext(app.packageName, 0)
        pkg.setTheme(themeRes)
        val tv = android.util.TypedValue()
        fun rawColor(attr: Int): Int? {
            if (!pkg.theme.resolveAttribute(attr, tv, true)) return null
            if (tv.type in android.util.TypedValue.TYPE_FIRST_COLOR_INT..android.util.TypedValue.TYPE_LAST_COLOR_INT) return tv.data
            if (tv.resourceId == 0) return null
            // Ссылка на селектор цвета или на цветную картинку
            runCatching { pkg.getColor(tv.resourceId) }.getOrNull()?.let { return it }
            return when (val d = runCatching { pkg.getDrawable(tv.resourceId) }.getOrNull()) {
                is android.graphics.drawable.ColorDrawable -> d.color
                else -> if (Build.VERSION.SDK_INT >= 29 && d is android.graphics.drawable.ColorStateListDrawable) d.colorStateList.defaultColor else null
            }
        }
        // Прозрачный или полупрозрачный цвет система не использует как фон сплэша: идём дальше по цепочке
        fun color(attr: Int): Int? = rawColor(attr)?.takeIf { android.graphics.Color.alpha(it) == 0xFF }
        val splash = if (Build.VERSION.SDK_INT >= 31) color(android.R.attr.windowSplashScreenBackground) else null
        return splash ?: color(android.R.attr.windowBackground) ?: color(android.R.attr.colorBackground)
    }

    fun openAppInfo(app: AppInfo, bounds: Rect?) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, bounds, null) }
    }

    fun uninstall(app: AppInfo) {
        val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    suspend fun shortcuts(app: AppInfo): List<ShortcutEntry> = withContext(Dispatchers.Default) {
        runCatching {
            if (!launcherApps.hasShortcutHostPermission()) return@runCatching emptyList()
            val q = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setActivity(app.component)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            val dpi = context.resources.displayMetrics.densityDpi
            launcherApps.getShortcuts(q, app.user).orEmpty()
                .filter { it.isEnabled }
                .take(5)
                .map { s ->
                    val icon = runCatching {
                        launcherApps.getShortcutIconDrawable(s, dpi)?.toBitmap(96, 96)?.asImageBitmap()
                    }.getOrNull()
                    ShortcutEntry(s, (s.shortLabel ?: s.id).toString(), icon)
                }
        }.getOrDefault(emptyList())
    }

    fun startShortcut(s: ShortcutInfo, bounds: Rect?) {
        runCatching { launcherApps.startShortcut(s, bounds, null) }
    }

    /** Приложение основного профиля для действия; если обработчик не выбран, берём первого подходящего. */
    fun findDefault(intent: Intent): AppInfo? {
        val pm = context.packageManager
        val candidates = listOfNotNull(pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo) +
            runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo } }.getOrDefault(emptyList())
        val mine = byKey.values.filter { !it.isWork }
        for (c in candidates) {
            mine.firstOrNull { it.packageName == c.packageName && it.className == c.name }?.let { return it }
            mine.firstOrNull { it.packageName == c.packageName }?.let { return it }
        }
        return null
    }

    private companion object {
        const val MAX_CACHED_ICONS = 400
        /** Сколько значков читаем заранее: остальные подгружаются при прокрутке, когда они нужны. */
        const val PRELOAD_LIMIT = 160
        const val PRELOAD_DELAY_MS = 900L
    }
}
