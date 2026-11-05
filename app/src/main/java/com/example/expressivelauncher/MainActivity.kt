package com.example.expressivelauncher

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.expressivelauncher.data.AppLanguage
import com.example.expressivelauncher.ui.Fx
import com.example.expressivelauncher.ui.LocalFx
import java.util.Locale
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import com.example.expressivelauncher.ui.theme.themeFadeSource
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.AppFont
import com.example.expressivelauncher.data.Badges
import com.example.expressivelauncher.data.GestureAccessibilityService
import com.example.expressivelauncher.ui.LocalBadges
import com.example.expressivelauncher.ui.LocalHostView
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.LocalWallpaperLight
import io.github.fletchmckee.liquid.liquefiable
import com.example.expressivelauncher.ui.components.backdropSource
import com.example.expressivelauncher.ui.components.WallpaperGlass
import com.example.expressivelauncher.ui.home.HomeActions
import com.example.expressivelauncher.ui.home.HomeScreen
import com.example.expressivelauncher.ui.settings.SettingsScreen
import com.example.expressivelauncher.ui.theme.CUSTOM_FONT_FILE
import com.example.expressivelauncher.ui.theme.LauncherTheme
import com.example.expressivelauncher.ui.theme.isDarkFor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), HomeActions {
    private val vm: LauncherViewModel by viewModels()

    override var homePressSignal: Int by mutableIntStateOf(0)
    override var homeFromForeground: Boolean = false
    private val haptics by lazy { com.example.expressivelauncher.ui.components.Haptics(this) { vm.settings.value } }
    override var wakeSignal: Int by mutableIntStateOf(0)
    override var resumeSignal: Int by mutableIntStateOf(0)
    override var isPaused: Boolean = false
    override var pauseCount: Int = 0
    private var wallpaperLight by mutableStateOf(false)
    private var darkTheme = false
    private var overSurface = false
    private var pendingIconApp: AppInfo? = null
    private var colorsListener: WallpaperManager.OnColorsChangedListener? = null
    private var showSettings by mutableStateOf(false)
    /** Системная настройка «Убрать анимации» выключена; перечитывается при возвращении в лаунчер. */
    private var animatorsOn by mutableStateOf(android.animation.ValueAnimator.areAnimatorsEnabled())
    /** Размытие обоев за окном: его просят стол (меню приложений, папка) и стеклянные настройки/онбординг. */
    private var homeBlurDp = 0
    private var overlayBlurDp = 0
    /** Размыт ли фон за окном сейчас: так же размываются и свои обои лаунчера, которые лежат в самом окне. */
    private var behindBlurred by mutableStateOf(false)
    /** Размыт ли фон под меню приложений (его просит стол). */
    private var drawerBlurred by mutableStateOf(false)
    /** Яркость своих обоев лаунчера, если они включены и загружены; null — решают системные обои. */
    private var customWallpaperLight: Boolean? = null
    private var pendingWidget: PendingWidget? = null
    private var powerSave by mutableStateOf(false)
    override var crossWindowBlur: Boolean by mutableStateOf(false)
    private val blurListener = java.util.function.Consumer<Boolean> { crossWindowBlur = it }
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { readPowerSave() }
    }

    private companion object {
        const val REQ_CONFIGURE = 4711
        /** Смена языка пересоздаёт окно; пользователь должен остаться в настройках. */
        var reopenSettings = false
    }

    /** Язык применяем к контексту окна, а не меняем для всего процесса: так работает на всех версиях Android. */
    override fun attachBaseContext(base: Context) {
        val name = base.getSharedPreferences("settings", MODE_PRIVATE).getString("language", null)
        val tag = AppLanguage.entries.firstOrNull { it.name == name }?.tag
        val locale = if (tag != null) Locale.forLanguageTag(tag) else android.content.res.Resources.getSystem().configuration.locales[0]
        Locale.setDefault(locale)
        super.attachBaseContext(
            if (tag == null) base else base.createConfigurationContext(android.content.res.Configuration().apply { setLocale(locale) })
        )
    }

    override fun setLanguage(language: AppLanguage) {
        if (vm.settings.value.language == language) return
        vm.updateSettings { it.copy(language = language) }
        reopenSettings = true
        recreate()
    }

    private fun readPowerSave() {
        powerSave = getSystemService(PowerManager::class.java).isPowerSaveMode
    }

    private class PendingWidget(val id: Int, val info: AppWidgetProviderInfo, val spanX: Int, val spanY: Int, val page: Int)

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) lifecycleScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(vm.exportLayout().toByteArray()) } != null
            }.getOrDefault(false)
            toast(if (ok) R.string.export_done else R.string.backup_failed)
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            }
            val before = vm.settings.value.language
            val ok = json != null && vm.importLayout(json)
            toast(if (ok) R.string.import_settings_done else R.string.backup_failed)
            // Язык применяется к окну при создании: после восстановления другого языка окно пересоздаём
            if (ok && vm.settings.value.language != before) { reopenSettings = true; recreate() }
        }
    }

    private val fontLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            val name = withContext(Dispatchers.IO) { installFont(uri) }
            if (name != null) {
                vm.updateSettings { it.copy(font = AppFont.CUSTOM, customFontName = name, customFontRev = it.customFontRev + 1) }
            } else toast(R.string.font_failed)
        }
    }

    /** Копирует шрифт во внутреннее хранилище; возвращает имя файла или null, если это не шрифт. */
    private fun installFont(uri: Uri): String? = runCatching {
        val tmp = File(filesDir, "$CUSTOM_FONT_FILE.tmp")
        val rev = vm.settings.value.customFontRev + 1
        contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: return null
        if (Typeface.Builder(tmp).build() == null) { tmp.delete(); return null }
        filesDir.listFiles { f -> f.name.startsWith("custom_font") && f != tmp }?.forEach { it.delete() }
        tmp.renameTo(File(filesDir, "custom_font_$rev.ttf"))
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "font"
    }.getOrNull()

    private val iconThemeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            val theme = withContext(Dispatchers.IO) { vm.repo.themes.importZip(uri) }
            if (theme != null) {
                vm.updateSettings { it.copy(iconTheme = theme.id) }
                toast(R.string.icon_theme_imported)
            } else toast(R.string.icon_theme_failed)
        }
    }

    private val appIconLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val app = pendingIconApp
        pendingIconApp = null
        if (uri != null && app != null) lifecycleScope.launch {
            if (!vm.repo.setOverride(app, uri)) toast(R.string.icon_failed)
        }
    }

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val p = pendingWidget ?: return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) configureOrFinish(p) else cancelWidget(p)
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        // Возврат на стол: окно приложения растворяется, а не уезжает системной анимацией в сторону —
        // сворачивание в значок рисует сам лаунчер (LaunchPortal), и две анимации не спорят друг с другом
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        watchWallpaperColors()
        if (reopenSettings) { reopenSettings = false; showSettings = true }
        if (com.example.expressivelauncher.data.CrashLog.takeUnseen(this)) {
            com.example.expressivelauncher.ui.components.LauncherToast.show(getString(R.string.crash_restarted))
        }
        setContent {
            val raw by vm.settings.collectAsState()
            // Тема перекрывает то, что противоречит её характеру; весь интерфейс читает уже итоговые настройки
            val spec = com.example.expressivelauncher.ui.theme.LauncherThemes.of(raw.style)
            val settings = remember(raw, spec, animatorsOn) { spec.effective(raw, animatorsOn) }
            val lite = spec.lite || (powerSave && raw.liteOnPowerSave)
            val fx = remember(lite, settings.animSpeed, settings.animBounce, settings.blurBackground, animatorsOn) {
                Fx(
                    lite = lite,
                    blur = settings.blurBackground && !lite,
                    // «Убрать анимации»: пружины становятся почти мгновенными
                    speed = settings.animSpeed * (if (lite) 1.35f else 1f) * (if (animatorsOn) 1f else 4f),
                    bounce = if (lite) 0f else settings.animBounce,
                )
            }
            val fade = com.example.expressivelauncher.ui.theme.rememberThemeFade()
            val onboarding = raw.onboardingVersion < com.example.expressivelauncher.ui.onboarding.ONBOARDING_VERSION
            // Состояние, а не карта: обновление уведомлений не должно рекомпозировать весь стол
            val badges = Badges.counts.collectAsState()
            val dark = isDarkFor(settings)
            SideEffect {
                darkTheme = dark
                applySystemBars()
            }
            // Стеклянные настройки и онбординг лежат на размытых обоях: стол под ними не нужен
            val overGlass = spec.glass && (showSettings || onboarding)
            // Свои обои лаунчера: рисуются в окне, системные обои за ним не нужны (живые ещё и тратят батарею)
            val context = androidx.compose.ui.platform.LocalContext.current
            val customOn = settings.customWallpaper && remember(settings.customWallpaper, com.example.expressivelauncher.ui.components.CustomWallpaper.version) {
                com.example.expressivelauncher.ui.components.CustomWallpaper.hasFile(context)
            }
            val customImage = com.example.expressivelauncher.ui.components.CustomWallpaper.image
            androidx.compose.runtime.LaunchedEffect(customOn) {
                if (com.example.expressivelauncher.ui.components.CustomWallpaper.active != customOn) {
                    com.example.expressivelauncher.ui.components.CustomWallpaper.active = customOn
                    WallpaperGlass.invalidate()
                }
            }
            val customReady = customOn && customImage != null
            com.example.expressivelauncher.ui.components.CustomWallpaper.parallaxOn = settings.wallpaperParallax
            androidx.compose.runtime.LaunchedEffect(settings.wallpaperBlur) {
                // Ползунок двигают — пересобираем картинку, когда он остановился
                kotlinx.coroutines.delay(250)
                com.example.expressivelauncher.ui.components.CustomWallpaper.setBlur(settings.wallpaperBlur)
            }
            val customLight = com.example.expressivelauncher.ui.components.CustomWallpaper.light
            SideEffect {
                // Флаг показа системных обоев не снимаем даже со своими обоями: по нему Android считает окно
                // рабочим столом и раскрывает приложения из значка. Без флага лаунчер для системы — обычное
                // приложение, и запуск шёл стандартным сдвигом вбок (это и был рывок). Свою картинку рисуем поверх
                val light = if (customReady) customLight else null
                if (light != customWallpaperLight) {
                    customWallpaperLight = light
                    applySystemBars()
                }
            }
            val homeAlpha by androidx.compose.animation.core.animateFloatAsState(if (overGlass) 0f else 1f, label = "homeUnderGlass")
            androidx.compose.runtime.LaunchedEffect(overGlass, settings.blurStrength) {
                overlayBlurDp = if (overGlass) (48f * settings.blurStrength).toInt().coerceIn(8, 96) else 0
                applyBlur()
            }
            // Настоящее жидкое стекло (Android 13+) — только когда лаунчер сам рисует обои: пикселей живых обоев у него нет
            val glassLib = spec.glass && customOn && Build.VERSION.SDK_INT >= 33
            val wallLiquid = io.github.fletchmckee.liquid.rememberLiquidState()
            val homeLiquid = io.github.fletchmckee.liquid.rememberLiquidState()
            LauncherTheme(settings, fx, spec) {
                CompositionLocalProvider(
                    com.example.expressivelauncher.ui.components.LocalWallLiquid provides (if (glassLib) wallLiquid else null),
                    com.example.expressivelauncher.ui.components.LocalHomeLiquid provides (if (glassLib) homeLiquid else null),
                    com.example.expressivelauncher.ui.theme.LocalThemeSpec provides spec,
                    com.example.expressivelauncher.ui.theme.LocalThemeFade provides fade,
                    LocalFx provides fx,
                    com.example.expressivelauncher.ui.LocalHaptics provides haptics,
                    LocalRepo provides vm.repo,
                    LocalSettings provides settings,
                    LocalBadges provides badges,
                    LocalHostView provides window.decorView,
                    LocalWallpaperLight provides (if (customReady) customLight else wallpaperLight),
                ) {
                    Box(Modifier.fillMaxSize().themeFadeSource(fade)) {
                        if (customOn) com.example.expressivelauncher.ui.components.LauncherWallpaper(
                            // Под настройками обои не размываются: стекло панелей должно преломлять чёткую картинку,
                            // а не размытое пятно. Размытие — только под меню приложений
                            blurred = drawerBlurred,
                            // Обои — источник пикселей для жидкого стекла (библиотека liquid): их преломляют
                            // док, поиск, часы, настройки, меню приложений и панели поверх стола
                            modifier = Modifier.fillMaxSize()
                                .then(if (glassLib) Modifier.liquefiable(wallLiquid) else Modifier),
                        )
                        // Под настройками — лёгкое затемнение чёткой картинки вместо размытия: заголовки читаются,
                        // а стекло панелей преломляет саму картинку (затемнение не входит в источник стекла)
                        if (customOn) Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - homeAlpha }.then(Modifier.drawBehind { drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.32f)) }))
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = homeAlpha }) {
                            HomeScreen(vm, this@MainActivity)
                        }
                        AnimatedVisibility(
                            visible = showSettings,
                            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.9f) +
                                slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 10 },
                            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                                scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.94f),
                        ) {
                            SettingsScreen(vm, this@MainActivity) { closeSettings() }
                        }
                        com.example.expressivelauncher.ui.onboarding.OnboardingHost(
                            visible = onboarding,
                            vm = vm,
                            actions = this@MainActivity,
                            onFinished = { wakeSignal++ },
                        )
                        com.example.expressivelauncher.ui.components.LauncherToastHost()
                        com.example.expressivelauncher.ui.theme.ThemeFadeOverlay(fade)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.widgets.startListening()
        readPowerSave()
        ContextCompat.registerReceiver(this, powerReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= 31) {
            crossWindowBlur = windowManager.isCrossWindowBlurEnabled
            windowManager.addCrossWindowBlurEnabledListener(blurListener)
        }
        wakeSignal++
    }

    override fun onResume() {
        super.onResume()
        // Доступ к обоям могли выдать или отозвать, пока лаунчер был в фоне
        WallpaperGlass.refreshAccess()
        animatorsOn = android.animation.ValueAnimator.areAnimatorsEnabled()
        if (isPaused) {
            isPaused = false
            resumeSignal++
        }
    }

    override fun onPause() {
        isPaused = true
        pauseCount++
        super.onPause()
    }

    override fun onStop() {
        unregisterReceiver(powerReceiver)
        if (Build.VERSION.SDK_INT >= 31) windowManager.removeCrossWindowBlurEnabledListener(blurListener)
        vm.widgets.stopListening()
        vm.flushLayout()
        super.onStop()
    }

    override fun onDestroy() {
        // Кэшированные view виджетов держат контекст этой активности
        vm.widgets.releaseViews(this)
        colorsListener?.let { runCatching { WallpaperManager.getInstance(this).removeOnColorsChangedListener(it) } }
        super.onDestroy()
    }

    // ---------- Обои и системные панели ----------

    private fun watchWallpaperColors() {
        if (Build.VERSION.SDK_INT < 31) return
        val wm = WallpaperManager.getInstance(this)
        fun read(colors: WallpaperColors?) {
            wallpaperLight = colors != null && colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
            // Обои могли сменить: матовое стекло должно подхватить новые, а не показывать старые
            WallpaperGlass.invalidate()
            WallpaperGlass.baseColor.value = colors?.primaryColor?.toArgb()?.let { androidx.compose.ui.graphics.Color(it) }
            applySystemBars()
        }
        lifecycleScope.launch {
            val colors = withContext(Dispatchers.IO) { runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull() }
            read(colors)
        }
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) runOnUiThread { read(colors) }
        }
        colorsListener = listener
        runCatching { wm.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
    }

    /** На поверхности приложения цвет значков следует теме, на обоях — яркости самих обоев. */
    private fun applySystemBars() {
        val lightBars = if (overSurface || showSettings) !darkTheme else (customWallpaperLight ?: wallpaperLight)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }

    private fun closeSettings() {
        showSettings = false
        applySystemBars()
    }

    override fun setOverSurface(over: Boolean) {
        overSurface = over
        applySystemBars()
    }

    @Volatile private var wallpaperSteps = -1

    override fun setWallpaperScroll(position: Float, pages: Int) {
        val token = window.decorView.windowToken ?: return
        runCatching {
            val wm = WallpaperManager.getInstance(this)
            val steps = (pages - 1).coerceAtLeast(1)
            if (steps != wallpaperSteps) {
                wallpaperSteps = steps
                wm.setWallpaperOffsetSteps(1f / steps, 0f)
            }
            wm.setWallpaperOffsets(token, (position / steps).coerceIn(0f, 1f), 0.5f)
        }
    }

    override fun pickIconTheme() {
        runCatching { iconThemeLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) }
    }

    override fun pickAppIcon(app: AppInfo) {
        pendingIconApp = app
        runCatching { appIconLauncher.launch(arrayOf("image/*")) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            closeSettings()
            // Вернулись из приложения: стол остаётся на той странице, где значок, иначе анимация закрытия улетит мимо
            homeFromForeground = !isPaused
            homePressSignal++
        }
    }

    // ---------- Виджеты ----------

    override fun addWidget(info: AppWidgetProviderInfo, spanX: Int, spanY: Int, page: Int) {
        val id = vm.widgets.allocateId()
        val p = PendingWidget(id, info, spanX, spanY, page)
        pendingWidget = p
        if (vm.widgets.manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrFinish(p)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            runCatching { bindLauncher.launch(intent) }.onFailure { cancelWidget(p) }
        }
    }

    private fun configureOrFinish(p: PendingWidget) {
        if (p.info.configure == null) {
            finishWidget(p)
            return
        }
        runCatching {
            vm.widgets.host.startAppWidgetConfigureActivityForResult(this, p.id, 0, REQ_CONFIGURE, null)
        }.onFailure { finishWidget(p) }
    }

    @Deprecated("Конфигурация виджета возвращает результат только через onActivityResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CONFIGURE) return
        val p = pendingWidget ?: return
        if (resultCode == Activity.RESULT_OK) finishWidget(p) else cancelWidget(p)
    }

    private fun finishWidget(p: PendingWidget) {
        vm.addWidget(p.id, p.spanX, p.spanY, p.page)
        pendingWidget = null
    }

    private fun cancelWidget(p: PendingWidget) {
        vm.widgets.delete(p.id)
        pendingWidget = null
    }

    // ---------- HomeActions ----------

    override fun openSettings() {
        showSettings = true
        applySystemBars()
    }

    override fun pickWallpaper() {
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.home_wallpaper)))
        }
    }

    override fun expandNotifications() {
        val ok = runCatching {
            val bar = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(bar)
        }.isSuccess
        if (!ok && !GestureAccessibilityService.openNotifications()) toast(R.string.need_a11y)
    }

    override fun lockScreen() {
        if (!GestureAccessibilityService.lockScreen()) {
            toast(R.string.need_a11y)
            openAccessibilitySettings()
        }
    }

    override fun openClock() {
        try {
            startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) { /* нет часов — молча */ }
    }

    override fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) { toast(R.string.no_browser) }
    }

    override fun setBlur(radiusDp: Int) {
        homeBlurDp = radiusDp
        applyBlur()
    }

    private fun applyBlur() {
        val wanted = maxOf(homeBlurDp, overlayBlurDp)
        behindBlurred = wanted > 0
        drawerBlurred = homeBlurDp > 0
        // Со своими обоями за окном ничего не видно: размывать системой то, что закрыто картинкой лаунчера,
        // — лишняя работа видеокарты на каждом кадре (свои обои размываются готовой копией, см. LauncherWallpaper)
        val radiusDp = if (com.example.expressivelauncher.ui.components.CustomWallpaper.active) 0 else wanted
        if (Build.VERSION.SDK_INT >= 31) {
            // setBackgroundBlurRadius здесь не подходит: система применяет его только к полупрозрачным окнам,
            // а лаунчер таким быть не должен. Размытие «за окном» работает и так, а плавно включает его сама система
            runCatching {
                val params = window.attributes
                params.blurBehindRadius = (radiusDp * resources.displayMetrics.density).toInt()
                // Полностью прозрачный слой затемнения система не рисует, а вместе с ним пропадает и размытие
                params.dimAmount = 0.04f
                val flags = WindowManager.LayoutParams.FLAG_BLUR_BEHIND or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                params.flags = if (radiusDp > 0) params.flags or flags else params.flags and flags.inv()
                window.attributes = params
            }
        }
    }

    override fun requestBadgeAccess() {
        if (com.example.expressivelauncher.data.Permissions.hasNotificationAccess(this)) return
        // Android 11+ открывает сразу страницу нужной службы; некоторые оболочки её не знают — тогда общий список
        val me = android.content.ComponentName(this, com.example.expressivelauncher.data.BadgeListenerService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, me.flattenToString())
        } else null
        val ok = detail != null && runCatching { startActivity(detail) }.isSuccess
        if (!ok) runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    override fun openDefaultLauncherSettings() {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val roles = getSystemService(RoleManager::class.java)
                if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                    startActivity(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                }
            }
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    override fun openAccessibilitySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    override fun exportLayout() { exportLauncher.launch("expressive-launcher-layout.json") }
    override fun importLayout() { importLauncher.launch(arrayOf("application/json", "*/*")) }
    override fun pickFont() { fontLauncher.launch(arrayOf("font/*", "application/x-font-ttf", "application/octet-stream", "*/*")) }

    private fun toast(res: Int) = runOnUiThread { com.example.expressivelauncher.ui.components.LauncherToast.show(getString(res)) }
}
