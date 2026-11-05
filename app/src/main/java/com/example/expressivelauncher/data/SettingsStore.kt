package com.example.expressivelauncher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Настройки в SharedPreferences; чтение один раз, дальше — StateFlow. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    /** На слабых устройствах по умолчанию облегчённая тема: без размытия и пружин. */
    private val defaultStyle = if (
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true
    ) LauncherStyle.LITE else LauncherStyle.EXPRESSIVE
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val new = transform(_state.value)
        if (new == _state.value) return
        _state.value = new
        save(new)
    }

    /** Все настройки в JSON с типами значений — для резервной копии. */
    fun exportJson(): org.json.JSONObject {
        val o = org.json.JSONObject()
        for ((k, v) in prefs.all) {
            val typed = when (v) {
                is Boolean -> org.json.JSONObject().put("b", v)
                is Int -> org.json.JSONObject().put("i", v)
                is Long -> org.json.JSONObject().put("l", v)
                is Float -> org.json.JSONObject().put("f", v.toDouble())
                is String -> org.json.JSONObject().put("s", v)
                is Set<*> -> org.json.JSONObject().put("ss", org.json.JSONArray(v.map { it.toString() }))
                else -> null
            } ?: continue
            o.put(k, typed)
        }
        return o
    }

    /** Восстанавливает настройки из [exportJson]; незнакомые и битые значения пропускаются. */
    fun importJson(o: org.json.JSONObject): Boolean = runCatching {
        val e = prefs.edit()
        for (k in o.keys()) {
            val t = o.optJSONObject(k) ?: continue
            when {
                t.has("b") -> e.putBoolean(k, t.getBoolean("b"))
                t.has("i") -> e.putInt(k, t.getInt("i"))
                t.has("l") -> e.putLong(k, t.getLong("l"))
                t.has("f") -> e.putFloat(k, t.getDouble("f").toFloat())
                t.has("s") -> e.putString(k, t.getString("s"))
                t.has("ss") -> { val a = t.getJSONArray("ss"); e.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet()) }
            }
        }
        e.commit()
        _state.value = load()
    }.isSuccess

    private inline fun <reified E : Enum<E>> enumOf(key: String, default: E): E =
        prefs.getString(key, null)?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            style = enumOf("style", defaultStyle),
            glassOpacity = prefs.getFloat("glassOpacity", d.glassOpacity),
            // Раньше был переключатель «стеклянные значки»: включённый становится прозрачным стилем
            glassIconStyle = enumOf("glassIconStyle", if (prefs.getBoolean("glassIcons", false)) GlassIconStyle.CLEAR else d.glassIconStyle),
            glassIconHighlight = enumOf("glassIconHighlight", d.glassIconHighlight),
            glassHighlightStrength = prefs.getFloat("glassHighlightStrength", d.glassHighlightStrength),
            glassIconShape = enumOf("glassIconShape", d.glassIconShape),
            liteIconShape = enumOf("liteIconShape", d.liteIconShape),
            customWallpaper = prefs.getBoolean("customWallpaper", d.customWallpaper),
            wallpaperBlur = prefs.getFloat("wallpaperBlur", d.wallpaperBlur),
            wallpaperParallax = prefs.getBoolean("wallpaperParallax", d.wallpaperParallax),
            onboardingVersion = prefs.getInt("onboardingVersion", d.onboardingVersion),
            onboardingStep = prefs.getInt("onboardingStep", d.onboardingStep),
            themeMode = enumOf("themeMode", d.themeMode),
            colorSource = enumOf("colorSource", d.colorSource),
            contrast = prefs.getFloat("contrast", d.contrast),
            seedColor = prefs.getInt("seedColor", d.seedColor),
            amoled = prefs.getBoolean("amoled", d.amoled),
            paletteStyle = prefs.getInt("paletteStyle", d.paletteStyle),
            cornerScale = prefs.getFloat("cornerScale", d.cornerScale),
            motion = enumOf("motion", d.motion),
            font = enumOf("font", d.font),
            fontScale = prefs.getFloat("fontScale", d.fontScale),
            fontWeightShift = prefs.getInt("fontWeightShift", d.fontWeightShift),
            clockFontSame = prefs.getBoolean("clockFontSame", d.clockFontSame),
            clockFont = enumOf("clockFont", d.clockFont),
            customFontName = prefs.getString("customFontName", d.customFontName) ?: "",
            customFontRev = prefs.getInt("customFontRev", d.customFontRev),
            dockHeight = prefs.getInt("dockHeight", d.dockHeight),
            gridPadding = prefs.getInt("gridPadding", d.gridPadding),
            labelLines = prefs.getInt("labelLines", d.labelLines),
            clockAlign = enumOf("clockAlign", d.clockAlign),
            clockScale = prefs.getFloat("clockScale", d.clockScale),
            showDate = prefs.getBoolean("showDate", d.showDate),
            showBattery = prefs.getBoolean("showBattery", d.showBattery),
            iconShape = enumOf("iconShape", d.iconShape),
            iconScale = prefs.getFloat("iconScale", d.iconScale),
            themedIcons = prefs.getBoolean("themedIcons", d.themedIcons),
            iconTheme = prefs.getString("iconTheme", d.iconTheme) ?: "",
            iconThemeMask = prefs.getBoolean("iconThemeMask", d.iconThemeMask),
            launchAnimation = prefs.getBoolean("launchAnimation", d.launchAnimation),
            animSpeed = prefs.getFloat("animSpeed", d.animSpeed),
            animBounce = prefs.getFloat("animBounce", d.animBounce),
            pageTransition = enumOf("pageTransition", d.pageTransition),
            wakeWave = prefs.getBoolean("wakeWave", d.wakeWave),
            liteOnPowerSave = prefs.getBoolean("liteOnPowerSave", d.liteOnPowerSave),
            blurStrength = prefs.getFloat("blurStrength", d.blurStrength),
            language = enumOf("language", d.language),
            duotoneIcons = prefs.getBoolean("duotoneIcons", d.duotoneIcons),
            showHomeLabels = prefs.getBoolean("showHomeLabels", d.showHomeLabels),
            labelScale = prefs.getFloat("labelScale", d.labelScale),
            showBadges = prefs.getBoolean("showBadges", d.showBadges),
            columns = prefs.getInt("columns", d.columns),
            rows = prefs.getInt("rows", d.rows),
            dockCount = prefs.getInt("dockCount", d.dockCount),
            showDock = prefs.getBoolean("showDock", d.showDock),
            dockBackground = prefs.getBoolean("dockBackground", d.dockBackground),
            dockOpacity = prefs.getFloat("dockOpacity", d.dockOpacity),
            dockRadius = prefs.getInt("dockRadius", d.dockRadius),
            clockStyle = enumOf("clockStyle", d.clockStyle),
            showSearchPill = prefs.getBoolean("showSearchPill", d.showSearchPill),
            searchGoogle = prefs.getBoolean("searchGoogle", d.searchGoogle),
            searchButtons = prefs.getString("searchButtons", null)?.let { v -> v.split('|').filter { it.isNotEmpty() } } ?: d.searchButtons,
            lockLayout = prefs.getBoolean("lockLayout", d.lockLayout),
            wallpaperDim = prefs.getFloat("wallpaperDim", d.wallpaperDim),
            showPageIndicator = prefs.getBoolean("showPageIndicator", d.showPageIndicator),
            drawerStyle = enumOf("drawerStyle", d.drawerStyle),
            drawerColumns = prefs.getInt("drawerColumns", d.drawerColumns),
            drawerSort = enumOf("drawerSort", d.drawerSort),
            drawerLabels = prefs.getBoolean("drawerLabels", d.drawerLabels),
            fastScroller = prefs.getBoolean("fastScroller", d.fastScroller),
            searchPosition = enumOf("searchPosition", d.searchPosition),
            drawerOpacity = prefs.getFloat("drawerOpacity", d.drawerOpacity),
            blurBackground = prefs.getBoolean("blurBackground", d.blurBackground),
            autoKeyboard = prefs.getBoolean("autoKeyboard", d.autoKeyboard),
            autoLaunchSingle = prefs.getBoolean("autoLaunchSingle", d.autoLaunchSingle),
            hiddenApps = prefs.getStringSet("hiddenApps", emptySet())?.toSet() ?: emptySet(),
            swipeUp = enumOf("swipeUp", d.swipeUp),
            swipeDown = enumOf("swipeDown", d.swipeDown),
            doubleTap = enumOf("doubleTap", d.doubleTap),
            haptics = prefs.getBoolean("haptics", d.haptics),
            hapticLevel = enumOf("hapticLevel", d.hapticLevel),
            hapticTouch = prefs.getBoolean("hapticTouch", d.hapticTouch),
            hapticDrag = prefs.getBoolean("hapticDrag", d.hapticDrag),
            hapticPages = prefs.getBoolean("hapticPages", d.hapticPages),
            hapticDrawer = prefs.getBoolean("hapticDrawer", d.hapticDrawer),
            closeAnimScale = prefs.getFloat("closeAnimScale", d.closeAnimScale),
            debugSlowLaunch = prefs.getBoolean("debugSlowLaunch", d.debugSlowLaunch),
            searchEngine = prefs.getInt("searchEngine", d.searchEngine),
        )
    }

    private fun save(s: Settings) {
        prefs.edit()
            .putString("style", s.style.name)
            .putFloat("glassOpacity", s.glassOpacity)
            .putString("glassIconStyle", s.glassIconStyle.name)
            .putString("glassIconHighlight", s.glassIconHighlight.name)
            .putFloat("glassHighlightStrength", s.glassHighlightStrength)
            .putString("glassIconShape", s.glassIconShape.name)
            .putString("liteIconShape", s.liteIconShape.name)
            .putBoolean("customWallpaper", s.customWallpaper)
            .putFloat("wallpaperBlur", s.wallpaperBlur)
            .putBoolean("wallpaperParallax", s.wallpaperParallax)
            .remove("glassIcons")
            .putInt("onboardingVersion", s.onboardingVersion)
            .putInt("onboardingStep", s.onboardingStep)
            .putString("themeMode", s.themeMode.name)
            .putString("colorSource", s.colorSource.name)
            .putFloat("contrast", s.contrast)
            .putInt("seedColor", s.seedColor)
            .putBoolean("amoled", s.amoled)
            .putInt("paletteStyle", s.paletteStyle)
            .putFloat("cornerScale", s.cornerScale)
            .putString("motion", s.motion.name)
            .putString("font", s.font.name)
            .putFloat("fontScale", s.fontScale)
            .putInt("fontWeightShift", s.fontWeightShift)
            .putBoolean("clockFontSame", s.clockFontSame)
            .putString("clockFont", s.clockFont.name)
            .putString("customFontName", s.customFontName)
            .putInt("customFontRev", s.customFontRev)
            .putInt("dockHeight", s.dockHeight)
            .putInt("gridPadding", s.gridPadding)
            .putInt("labelLines", s.labelLines)
            .putString("clockAlign", s.clockAlign.name)
            .putFloat("clockScale", s.clockScale)
            .putBoolean("showDate", s.showDate)
            .putBoolean("showBattery", s.showBattery)
            .putString("iconShape", s.iconShape.name)
            .putFloat("iconScale", s.iconScale)
            .putBoolean("themedIcons", s.themedIcons)
            .putString("iconTheme", s.iconTheme)
            .putBoolean("iconThemeMask", s.iconThemeMask)
            .putBoolean("launchAnimation", s.launchAnimation)
            .putFloat("animSpeed", s.animSpeed)
            .putFloat("animBounce", s.animBounce)
            .putString("pageTransition", s.pageTransition.name)
            .putBoolean("wakeWave", s.wakeWave)
            .putBoolean("liteOnPowerSave", s.liteOnPowerSave)
            .putFloat("blurStrength", s.blurStrength)
            .putString("language", s.language.name)
            .putBoolean("duotoneIcons", s.duotoneIcons)
            .putBoolean("showHomeLabels", s.showHomeLabels)
            .putFloat("labelScale", s.labelScale)
            .putBoolean("showBadges", s.showBadges)
            .putInt("columns", s.columns)
            .putInt("rows", s.rows)
            .putInt("dockCount", s.dockCount)
            .putBoolean("showDock", s.showDock)
            .putBoolean("dockBackground", s.dockBackground)
            .putFloat("dockOpacity", s.dockOpacity)
            .putInt("dockRadius", s.dockRadius)
            .putString("clockStyle", s.clockStyle.name)
            .putBoolean("showSearchPill", s.showSearchPill)
            .putBoolean("searchGoogle", s.searchGoogle)
            .putString("searchButtons", s.searchButtons.joinToString("|"))
            .putBoolean("lockLayout", s.lockLayout)
            .putFloat("wallpaperDim", s.wallpaperDim)
            .putBoolean("showPageIndicator", s.showPageIndicator)
            .putString("drawerStyle", s.drawerStyle.name)
            .putInt("drawerColumns", s.drawerColumns)
            .putString("drawerSort", s.drawerSort.name)
            .putBoolean("drawerLabels", s.drawerLabels)
            .putBoolean("fastScroller", s.fastScroller)
            .putString("searchPosition", s.searchPosition.name)
            .putFloat("drawerOpacity", s.drawerOpacity)
            .putBoolean("blurBackground", s.blurBackground)
            .putBoolean("autoKeyboard", s.autoKeyboard)
            .putBoolean("autoLaunchSingle", s.autoLaunchSingle)
            .putStringSet("hiddenApps", s.hiddenApps)
            .putString("swipeUp", s.swipeUp.name)
            .putString("swipeDown", s.swipeDown.name)
            .putString("doubleTap", s.doubleTap.name)
            .putBoolean("haptics", s.haptics)
            .putString("hapticLevel", s.hapticLevel.name)
            .putBoolean("hapticTouch", s.hapticTouch)
            .putBoolean("hapticDrag", s.hapticDrag)
            .putBoolean("hapticPages", s.hapticPages)
            .putBoolean("hapticDrawer", s.hapticDrawer)
            .putFloat("closeAnimScale", s.closeAnimScale)
            .putBoolean("debugSlowLaunch", s.debugSlowLaunch)
            .putInt("searchEngine", s.searchEngine)
            .apply()
    }
}
