package com.example.expressivelauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AlignHorizontalCenter
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.Padding
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.expressivelauncher.ui.components.glassEdge
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressivelauncher.BuildConfig
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppFont
import com.example.expressivelauncher.data.iconShapeFor
import com.example.expressivelauncher.data.withIconShape
import com.example.expressivelauncher.data.ClockAlign
import com.example.expressivelauncher.data.ClockStyle
import com.example.expressivelauncher.data.MotionStyle
import com.example.expressivelauncher.ui.LocalClockStyle
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.data.ColorSource
import com.example.expressivelauncher.data.DrawerSort
import com.example.expressivelauncher.data.DrawerStyle
import com.example.expressivelauncher.data.GestureAction
import com.example.expressivelauncher.data.SearchEngines
import com.example.expressivelauncher.data.SearchPosition
import com.example.expressivelauncher.data.Settings
import com.example.expressivelauncher.data.ThemeMode
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.toComposeShape
import com.example.expressivelauncher.ui.home.HomeActions
import com.example.expressivelauncher.ui.theme.PaletteStyles
import com.example.expressivelauncher.ui.theme.SeedColors
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.example.expressivelauncher.data.AppLanguage
import com.example.expressivelauncher.data.PageTransition
import com.example.expressivelauncher.data.HapticLevel
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.theme.tunedDamping
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class SettingsPage(
    val title: Int, val subtitle: Int, val icon: ImageVector, val parent: SettingsPage?,
) {
    HOME(R.string.home_settings, R.string.home_settings, Icons.Rounded.Home, null),
    THEMES(R.string.set_themes, R.string.set_themes_hint, Icons.Rounded.Palette, HOME),
    COLORS(R.string.set_colors, R.string.set_colors_hint, Icons.Rounded.ColorLens, HOME),
    FONTS(R.string.set_fonts, R.string.set_fonts_hint, Icons.Rounded.TextFields, HOME),
    MOTION(R.string.set_motion, R.string.set_motion_hint, Icons.Rounded.Animation, HOME),
    ICONS(R.string.set_icons, R.string.set_icons_hint, Icons.Rounded.Interests, HOME),
    ICON_THEMES(R.string.icon_themes, R.string.icon_themes_hint, Icons.Rounded.Style, ICONS),
    HOME_SCREEN(R.string.set_home, R.string.set_home_hint, Icons.Rounded.Dashboard, HOME),
    DRAWER(R.string.set_drawer, R.string.set_drawer_hint, Icons.Rounded.Apps, HOME),
    GESTURES(R.string.set_gestures, R.string.set_gestures_hint, Icons.Rounded.Gesture, HOME),
    HAPTICS(R.string.set_haptics, R.string.set_haptics_hint, Icons.Rounded.Vibration, HOME),
    BACKUP(R.string.set_backup, R.string.set_backup_hint, Icons.Rounded.Backup, HOME),
    ABOUT(R.string.about, R.string.about_hint, Icons.Rounded.Info, HOME),
    HIDDEN(R.string.set_hidden, R.string.set_hidden, Icons.Rounded.VisibilityOff, DRAWER),
    SEARCH_BAR(R.string.sb_title, R.string.sb_title_hint, Icons.Rounded.Search, HOME_SCREEN),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel, actions: HomeActions, onClose: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    BackHandler { page.parent?.let { page = it } ?: onClose() }
    val cs = MaterialTheme.colorScheme
    val fx = LocalFx.current
    // Liquid Glass: настройки лежат на размытых обоях (окно размывает их, стол под настройками скрыт).
    // Без размытия (Android ниже 12) фон плотнее, иначе текст читался бы на чётких обоях
    val glass = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
    val pageBg = if (glass) cs.surfaceContainer.copy(alpha = if (android.os.Build.VERSION.SDK_INT >= 31) 0.38f else 0.86f) else cs.surfaceContainer
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState.parent == initialState || initialState == SettingsPage.HOME
            val s = spring<IntOffset>(dampingRatio = tunedDamping(0.85f, fx.bounce), stiffness = 380f * fx.speed * fx.speed)
            if (forward) (slideInHorizontally(s) { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally(s) { -it / 6 } + fadeOut())
            else (slideInHorizontally(s) { -it / 6 } + fadeIn()) togetherWith (slideOutHorizontally(s) { it / 3 } + fadeOut())
        },
        label = "settingsPage",
        modifier = Modifier.fillMaxSize().background(pageBg),
    ) { p ->
        val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
            containerColor = if (glass) Color.Transparent else cs.surfaceContainer,
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(p.title), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = { p.parent?.let { page = it } ?: onClose() }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.padding(start = 8.dp).then(with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = com.example.expressivelauncher.ui.components.glassContainer(MaterialTheme.colorScheme.secondaryContainer))) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                        }
                    },
                    colors = if (glass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = cs.surfaceContainer.copy(alpha = 0.5f))
                    else TopAppBarDefaults.topAppBarColors(containerColor = cs.surfaceContainer, scrolledContainerColor = cs.surfaceContainer),
                    scrollBehavior = scroll,
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                CompositionLocalProvider(LocalSectionCounter provides remember(p) { intArrayOf(0) }) {
                when (p) {
                    SettingsPage.HOME -> MainPage(vm, actions, onClose) { page = it }
                    SettingsPage.THEMES -> ThemesPage(vm)
                    SettingsPage.COLORS -> ColorsPage(vm)
                    SettingsPage.FONTS -> FontsPage(vm, actions)
                    SettingsPage.MOTION -> MotionPage(vm, actions)
                    SettingsPage.ICONS -> IconsPage(vm, actions) { page = SettingsPage.ICON_THEMES }
                    SettingsPage.ICON_THEMES -> IconThemesPage(vm, actions)
                    SettingsPage.HOME_SCREEN -> HomeScreenPage(vm) { page = SettingsPage.SEARCH_BAR }
                    SettingsPage.SEARCH_BAR -> SearchBarPage(vm)
                    SettingsPage.DRAWER -> DrawerPage(vm) { page = SettingsPage.HIDDEN }
                    SettingsPage.GESTURES -> GesturesPage(vm, actions)
                    SettingsPage.HAPTICS -> HapticsPage(vm)
                    SettingsPage.BACKUP -> BackupPage(actions)
                    SettingsPage.ABOUT -> AboutPage(actions)
                    SettingsPage.HIDDEN -> HiddenPage(vm)
                }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- главная

private fun isDefaultHome(ctx: Context): Boolean {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val ri = ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
    return ri?.activityInfo?.packageName == ctx.packageName
}

private class SearchEntry(val title: Int, val page: SettingsPage)

private val searchIndex = listOf(
    R.string.set_themes to SettingsPage.THEMES, R.string.theme_glass to SettingsPage.THEMES, R.string.theme_expressive to SettingsPage.THEMES,
    R.string.theme_lite to SettingsPage.THEMES, R.string.dynamic_colors to SettingsPage.THEMES, R.string.glass_icons to SettingsPage.THEMES,
    R.string.onboarding_redo to SettingsPage.HOME,
    R.string.theme_mode to SettingsPage.COLORS, R.string.color_source to SettingsPage.COLORS, R.string.accent_color to SettingsPage.COLORS,
    R.string.palette_style to SettingsPage.COLORS, R.string.contrast to SettingsPage.COLORS, R.string.amoled to SettingsPage.COLORS,
    R.string.font_family to SettingsPage.FONTS, R.string.font_scale to SettingsPage.FONTS, R.string.font_weight to SettingsPage.FONTS,
    R.string.font_import to SettingsPage.FONTS, R.string.clock_font to SettingsPage.FONTS,
    R.string.corner_scale to SettingsPage.COLORS, R.string.motion to SettingsPage.MOTION,
    R.string.anim_speed to SettingsPage.MOTION, R.string.anim_bounce to SettingsPage.MOTION, R.string.page_transition to SettingsPage.MOTION,
    R.string.wake_wave to SettingsPage.MOTION, R.string.lite_power_save to SettingsPage.MOTION, R.string.blur_strength to SettingsPage.MOTION,
    R.string.duotone_icons to SettingsPage.ICONS, R.string.language to SettingsPage.HOME,
    R.string.clock_align to SettingsPage.HOME_SCREEN, R.string.clock_size to SettingsPage.HOME_SCREEN, R.string.grid_padding to SettingsPage.HOME_SCREEN,
    R.string.dock_height to SettingsPage.HOME_SCREEN, R.string.show_date to SettingsPage.HOME_SCREEN, R.string.show_battery to SettingsPage.HOME_SCREEN,
    R.string.label_lines to SettingsPage.ICONS,
    R.string.icon_themes to SettingsPage.ICONS, R.string.launch_animation to SettingsPage.MOTION,
    R.string.icon_shape to SettingsPage.ICONS, R.string.icon_size to SettingsPage.ICONS, R.string.themed_icons to SettingsPage.ICONS,
    R.string.home_labels to SettingsPage.ICONS, R.string.badges to SettingsPage.ICONS, R.string.label_size to SettingsPage.ICONS,
    R.string.columns to SettingsPage.HOME_SCREEN, R.string.rows to SettingsPage.HOME_SCREEN, R.string.dock to SettingsPage.HOME_SCREEN,
    R.string.dock_opacity to SettingsPage.HOME_SCREEN, R.string.dock_radius to SettingsPage.HOME_SCREEN, R.string.clock to SettingsPage.HOME_SCREEN,
    R.string.search_pill to SettingsPage.HOME_SCREEN, R.string.sb_customize to SettingsPage.SEARCH_BAR, R.string.lock_layout to SettingsPage.HOME_SCREEN, R.string.wallpaper_dim to SettingsPage.HOME_SCREEN,
    R.string.haptics to SettingsPage.HAPTICS, R.string.haptic_strength to SettingsPage.HAPTICS, R.string.close_anim to SettingsPage.MOTION, R.string.drawer_style to SettingsPage.DRAWER, R.string.sort to SettingsPage.DRAWER,
    R.string.fast_scroller to SettingsPage.DRAWER, R.string.set_hidden to SettingsPage.DRAWER, R.string.search_position to SettingsPage.DRAWER,
    R.string.search_engine to SettingsPage.DRAWER, R.string.blur to SettingsPage.MOTION, R.string.opacity to SettingsPage.DRAWER,
    R.string.swipe_up to SettingsPage.GESTURES, R.string.swipe_down to SettingsPage.GESTURES, R.string.double_tap to SettingsPage.GESTURES,
    R.string.export_layout to SettingsPage.BACKUP, R.string.import_layout to SettingsPage.BACKUP,
    R.string.permissions to SettingsPage.ABOUT, R.string.a11y to SettingsPage.GESTURES, R.string.perm_notifications to SettingsPage.ABOUT,
).map { SearchEntry(it.first, it.second) }

@Composable
private fun MainPage(vm: LauncherViewModel, actions: HomeActions, onClose: () -> Unit, go: (SettingsPage) -> Unit) {
    val s by vm.settings.collectAsState()
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    LaunchedEffect(focused) { if (focused) isDefault = isDefaultHome(context) }
    var query by rememberSaveable { mutableStateOf("") }

    // Автор и канал — сверху, как в Expressive Keyboard
    AuthorLine()
    // Поле поиска в стиле поисковой строки M3: заполненная «таблетка» без рамки
    val searchBg = panelColor(MaterialTheme.colorScheme.surfaceBright)
    TextField(
        value = query,
        onValueChange = { query = it },
        // Liquid Glass: поле — та же стеклянная «таблетка», что и секции (подложка прозрачна, см. panelColor)
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(CircleShape).glassEdge(CircleShape),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = searchBg, unfocusedContainerColor = searchBg,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        placeholder = { Text(stringResource(R.string.search_settings)) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Close, null) } },
        singleLine = true,
    )
    if (query.isNotBlank()) {
        val found = searchIndex.filter { stringResource(it.title).contains(query.trim(), ignoreCase = true) }
        SettingsSection(stringResource(R.string.found_count, found.size)) {
            if (found.isEmpty()) GroupItem {
                Text(stringResource(R.string.nothing_found), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            found.forEach { e ->
                ActionItem(e.page.icon, stringResource(e.title), stringResource(e.page.title), onClick = { go(e.page) })
            }
        }
        return
    }
    HeroCard(Blob.COOKIE_12, isDefault, actions::openDefaultLauncherSettings)
    val groups = listOf(
        R.string.cat_look to listOf(SettingsPage.THEMES, SettingsPage.COLORS, SettingsPage.MOTION, SettingsPage.FONTS, SettingsPage.ICONS, SettingsPage.HOME_SCREEN, SettingsPage.DRAWER),
        R.string.cat_behavior to listOf(SettingsPage.GESTURES, SettingsPage.HAPTICS),
        R.string.cat_other to listOf(SettingsPage.BACKUP, SettingsPage.ABOUT),
    )
    var i = 0
    for ((title, pages) in groups) {
        SettingsSection(stringResource(title)) {
            for (p in pages) { CategoryTile(p.icon, stringResource(p.title), stringResource(p.subtitle), i++) { go(p) } }
        }
    }
    SettingsSection(stringResource(R.string.onboarding_section)) {
        ActionItem(Icons.Rounded.RestartAlt, stringResource(R.string.onboarding_redo), stringResource(R.string.onboarding_redo_hint), onClick = {
            // Онбординг покажется поверх; настройки закрываем, чтобы в конце оказаться на рабочем столе
            vm.updateSettings { it.copy(onboardingVersion = 0, onboardingStep = 0) }
            onClose()
        })
    }
    SettingsSection(stringResource(R.string.language)) {
        ChoiceItem(
            Icons.Rounded.Language, stringResource(R.string.language), null,
            listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.RUSSIAN to "Русский",
                AppLanguage.ENGLISH to "English",
            ),
            s.language,
        ) { actions.setLanguage(it) }
    }
}

/** Живой мини-экран: форма, размер иконок, цвета, док и часы берутся из текущих настроек. */
@Composable
private fun LauncherPreview(s: Settings) {
    val cs = MaterialTheme.colorScheme
    val shape = s.iconShape.toComposeShape()
    val iconSize = (46 * s.iconScale).dp
    val icons = listOf(
        Triple(Icons.Rounded.Call, cs.primary, cs.onPrimary),
        Triple(Icons.Rounded.Email, cs.secondary, cs.onSecondary),
        Triple(Icons.Rounded.PhotoCamera, cs.tertiary, cs.onTertiary),
        Triple(Icons.Rounded.MusicNote, cs.primaryContainer, cs.onPrimaryContainer),
    )
    Box(
        Modifier.fillMaxWidth().height(250.dp).padding(10.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer.copy(alpha = 0.85f), cs.secondaryContainer))),
    ) {
        Column(Modifier.padding(start = 18.dp, top = 14.dp)) {
            if (s.clockStyle != ClockStyle.OFF) {
                Row {
                    Text("9:", style = MaterialTheme.typography.displayMedium, color = cs.onPrimaryContainer)
                    Text("41", style = MaterialTheme.typography.displayMedium, color = cs.primary)
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (s.showDock) 78.dp else 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            for ((icon, bg, fg) in icons) {
                Box(Modifier.size(iconSize).clip(shape).background(bg), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(iconSize * 0.5f))
                }
            }
        }
        if (s.showDock) Box(
            Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth().height(58.dp)
                .clip(RoundedCornerShape(minOf(s.dockRadius, 29).dp))
                .background(cs.surfaceContainer.copy(alpha = if (s.dockBackground) s.dockOpacity else 0f)),
        )
    }
}

// ---------------------------------------------------------------- цвета

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsPage(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    val canWallpaper = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val cs = MaterialTheme.colorScheme
    var picker by remember { mutableStateOf(false) }
    val custom = s.colorSource == ColorSource.CUSTOM || !canWallpaper
    val context = LocalContext.current

    SettingsSection(stringResource(R.string.preview)) { GroupItem { LauncherPreview(s) } }

    SettingsSection(stringResource(R.string.color_source)) {
        if (canWallpaper) {
            ChoiceItem(
                Icons.Rounded.Wallpaper, stringResource(R.string.color_source), null,
                listOf(ColorSource.WALLPAPER to stringResource(R.string.source_wallpaper), ColorSource.CUSTOM to stringResource(R.string.source_custom)),
                s.colorSource,
            ) { v -> upd { it.copy(colorSource = v) } }
        } else {
            GroupItem { Box(Modifier.padding(12.dp)) { InfoCard(Icons.Rounded.Info, stringResource(R.string.wallpaper_colors_unavailable)) } }
        }
    }

    val seeds by produceState(emptyList<Int>()) {
        value = withContext(Dispatchers.IO) { wallpaperSeeds(context) }
    }
    if (seeds.isNotEmpty()) SettingsSection(stringResource(R.string.wallpaper_seeds)) {
        GroupItem {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            ) {
                for (c in seeds) {
                    ColorDot(Color(c), custom && c == s.seedColor, 58.dp) { upd { it.copy(colorSource = ColorSource.CUSTOM, seedColor = c) } }
                }
            }
        }
    }

    SettingsSection(stringResource(R.string.accent_color)) {
        GroupItem {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (c in SeedColors) {
                    // любой выбранный цвет сразу включает «свой цвет» — не нужно искать переключатель
                    ColorDot(Color(c), custom && c == s.seedColor) { upd { it.copy(colorSource = ColorSource.CUSTOM, seedColor = c) } }
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton({ picker = true }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Edit, null, tint = Color.White) }
                }
            }
        }
        ColorRow(stringResource(R.string.accent_color), Color(s.seedColor)) { picker = true }
    }

    SettingsSection(stringResource(R.string.palette_preview)) {
        GroupItem {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (c in listOf(cs.primary, cs.secondary, cs.tertiary, cs.primaryContainer, cs.secondaryContainer, cs.tertiaryContainer)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(c))
                }
            }
        }
    }

    SettingsSection(stringResource(R.string.palette_style)) {
        val names = listOf(
            R.string.palette_tonal, R.string.palette_exact, R.string.palette_vibrant, R.string.palette_expressive,
            R.string.palette_rainbow, R.string.palette_fruit, R.string.palette_neutral, R.string.palette_mono,
        )
        ChoiceItem(
            Icons.Rounded.Palette, stringResource(R.string.palette_style), if (!custom) stringResource(R.string.palette_custom_only) else null,
            PaletteStyles.indices.map { it to stringResource(names[it]) }, s.paletteStyle,
        ) { v -> upd { it.copy(colorSource = ColorSource.CUSTOM, paletteStyle = v) } }
        SliderItem(Icons.Rounded.Contrast, stringResource(R.string.contrast), s.contrast, 0f..1f, { "${(it * 100).roundToInt()}%" }) { v ->
            upd { it.copy(colorSource = ColorSource.CUSTOM, contrast = v) }
        }
    }

    SettingsSection(stringResource(R.string.interface_section)) {
        SliderItem(Icons.Rounded.RoundedCorner, stringResource(R.string.corner_scale), s.cornerScale, 0.5f..1.5f, { "${(it * 100).roundToInt()}%" }) { v ->
            upd { it.copy(cornerScale = v) }
        }
    }

    SettingsSection(stringResource(R.string.theme_mode)) {
        ChoiceItem(
            Icons.Rounded.Brightness6, stringResource(R.string.theme_mode), null,
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
            ),
            s.themeMode,
        ) { v -> upd { it.copy(themeMode = v) } }
        ToggleItem(Icons.Rounded.DarkMode, stringResource(R.string.amoled), stringResource(R.string.amoled_hint), s.amoled) { v -> upd { it.copy(amoled = v) } }
    }

    if (picker) ColorPickerDialog(
        initial = s.seedColor,
        onDismiss = { picker = false },
        onPick = { c -> picker = false; upd { it.copy(colorSource = ColorSource.CUSTOM, seedColor = c) } },
    )
}

/** Основные цвета текущих обоев: из них можно собрать палитру вручную, как в «Обоях и стиле» на Android. */
private fun wallpaperSeeds(context: Context): List<Int> = runCatching {
    if (Build.VERSION.SDK_INT < 27) return emptyList()
    val c = android.app.WallpaperManager.getInstance(context).getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM) ?: return emptyList()
    listOfNotNull(c.primaryColor, c.secondaryColor, c.tertiaryColor).map { it.toArgb() or (0xFF shl 24) }.distinct()
}.getOrDefault(emptyList())

// ---------------------------------------------------------------- анимации

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MotionPage(vm: LauncherViewModel, actions: HomeActions) {
    val s by vm.settings.collectAsState()
    val fx = LocalFx.current
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    val cs = MaterialTheme.colorScheme

    // Живой пример: фигура прыгает при каждом касании и сразу показывает выбранные скорость и упругость
    var demo by remember { mutableStateOf(false) }
    val demoScale by androidx.compose.animation.core.animateFloatAsState(if (demo) 1.25f else 0.8f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "demoScale")
    val demoSpin by androidx.compose.animation.core.animateFloatAsState(if (demo) 180f else 0f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "demoSpin")
    SettingsSection(stringResource(R.string.preview)) {
        GroupItem(onClick = { demo = !demo }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BlobBadge(
                    Blob.COOKIE_12.shape(), cs.primary, 96.dp,
                    modifier = Modifier.graphicsLayer { scaleX = demoScale; scaleY = demoScale },
                    rotation = { demoSpin },
                ) { Icon(Icons.Rounded.TouchApp, null, tint = cs.onPrimary, modifier = Modifier.size(36.dp)) }
                Text(stringResource(R.string.anim_demo), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
    }
    SettingsSection(stringResource(R.string.motion)) {
        ChoiceItem(
            Icons.Rounded.Animation, stringResource(R.string.motion), stringResource(R.string.motion_hint),
            listOf(MotionStyle.EXPRESSIVE to stringResource(R.string.motion_expressive), MotionStyle.STANDARD to stringResource(R.string.motion_standard)),
            s.motion,
        ) { v -> upd { it.copy(motion = v) } }
        SliderItem(Icons.Rounded.Speed, stringResource(R.string.anim_speed), s.animSpeed, 0.5f..2f, { "×" + String.format(Locale.US, "%.2f", it) }, steps = 5) { v -> upd { it.copy(animSpeed = v) } }
        SliderItem(Icons.Rounded.Waves, stringResource(R.string.anim_bounce), s.animBounce, 0f..2f, { "${(it * 100).roundToInt()}%" }, steps = 7) { v -> upd { it.copy(animBounce = v) } }
    }
    SettingsSection(stringResource(R.string.set_home)) {
        ChoiceItem(
            Icons.Rounded.Swipe, stringResource(R.string.page_transition), null,
            listOf(
                PageTransition.DEPTH to stringResource(R.string.page_depth),
                PageTransition.SLIDE to stringResource(R.string.page_slide),
                PageTransition.CUBE to stringResource(R.string.page_cube),
                PageTransition.FADE to stringResource(R.string.page_fade),
            ),
            s.pageTransition,
        ) { v -> upd { it.copy(pageTransition = v) } }
        ToggleItem(Icons.Rounded.RocketLaunch, stringResource(R.string.launch_animation), stringResource(R.string.launch_animation_hint), s.launchAnimation) { v -> upd { it.copy(launchAnimation = v) } }
        SliderItem(Icons.Rounded.Speed, stringResource(R.string.close_anim), s.closeAnimScale, 0.5f..2f, { "×" + String.format(Locale.US, "%.2f", it) }, steps = 5) { v -> upd { it.copy(closeAnimScale = v) } }
        ToggleItem(Icons.Rounded.Speed, stringResource(R.string.debug_slow_launch), stringResource(R.string.debug_slow_launch_hint), s.debugSlowLaunch) { v -> upd { it.copy(debugSlowLaunch = v) } }
        ToggleItem(Icons.Rounded.Waves, stringResource(R.string.wake_wave), stringResource(R.string.wake_wave_hint), s.wakeWave) { v -> upd { it.copy(wakeWave = v) } }
    }
    SettingsSection(stringResource(R.string.blur)) {
        ToggleItem(Icons.Rounded.BlurOn, stringResource(R.string.blur), stringResource(R.string.blur_hint), s.blurBackground) { v -> upd { it.copy(blurBackground = v) } }
        SliderItem(Icons.Rounded.BlurOn, stringResource(R.string.blur_strength), s.blurStrength, 0.25f..2f, { "${(it * 100).roundToInt()}%" }, steps = 6) { v -> upd { it.copy(blurStrength = v) } }
        // Картинку обоев Android отдаёт только с доступом ко всем файлам: без него док и панели размывают лишь значки
        if (Build.VERSION.SDK_INT >= 31 && !com.example.expressivelauncher.ui.components.WallpaperGlass.access.value) {
            val context = androidx.compose.ui.platform.LocalContext.current
            ActionItem(
                Icons.Rounded.Wallpaper, stringResource(R.string.blur_wallpaper_access), stringResource(R.string.blur_wallpaper_access_hint),
                onClick = { com.example.expressivelauncher.ui.components.WallpaperGlass.requestAccess(context) },
            )
        }
        GroupItem {
            Box(Modifier.padding(12.dp)) {
                InfoCard(
                    Icons.Rounded.Info,
                    stringResource(
                        when {
                            Build.VERSION.SDK_INT < 31 -> R.string.blur_status_old
                            actions.crossWindowBlur -> R.string.blur_status_full
                            else -> R.string.blur_status_app_only
                        }
                    ),
                )
            }
        }
    }
    SettingsSection(stringResource(R.string.performance)) {
        ToggleItem(Icons.Rounded.BatterySaver, stringResource(R.string.lite_power_save), stringResource(R.string.lite_power_save_hint), s.liteOnPowerSave) { v -> upd { it.copy(liteOnPowerSave = v) } }
        if (fx.lite) GroupItem { Box(Modifier.padding(12.dp)) { InfoCard(Icons.Rounded.BatterySaver, stringResource(R.string.lite_active)) } }
    }
}

// ---------------------------------------------------------------- шрифты

@Composable
private fun FontsPage(vm: LauncherViewModel, actions: HomeActions) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    val cs = MaterialTheme.colorScheme
    val names = mapOf(
        AppFont.SYSTEM to stringResource(R.string.font_system), AppFont.CONDENSED to stringResource(R.string.font_condensed),
        AppFont.SERIF to stringResource(R.string.font_serif), AppFont.MONO to stringResource(R.string.font_mono),
        AppFont.CASUAL to stringResource(R.string.font_casual), AppFont.CURSIVE to stringResource(R.string.font_cursive),
        AppFont.CUSTOM to stringResource(R.string.font_custom),
    )
    val available = AppFont.entries.filter { it != AppFont.CUSTOM || s.customFontName.isNotBlank() }.map { it to names.getValue(it) }

    SettingsSection(stringResource(R.string.preview)) {
        GroupItem {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("12:45", style = LocalClockStyle.current.copy(fontSize = 56.sp), color = cs.primary)
                Text(stringResource(R.string.font_sample_title), style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                Text(stringResource(R.string.font_sample_body), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                Text("Aa Бб 0123456789", style = MaterialTheme.typography.labelLarge, color = cs.tertiary)
            }
        }
    }
    SettingsSection(stringResource(R.string.font_family)) {
        ChoiceItem(Icons.Rounded.TextFields, stringResource(R.string.font_family), null, available, s.font) { v -> upd { it.copy(font = v) } }
        ActionItem(
            Icons.Rounded.UploadFile, stringResource(R.string.font_import),
            s.customFontName.ifBlank { stringResource(R.string.font_import_hint) }, actions::pickFont,
        )
    }
    SettingsSection(stringResource(R.string.font_style)) {
        SliderItem(Icons.Rounded.FormatSize, stringResource(R.string.font_scale), s.fontScale, 0.85f..1.3f, { "${(it * 100).roundToInt()}%" }, steps = 8) { v ->
            upd { it.copy(fontScale = v) }
        }
        SliderItem(
            Icons.Rounded.FormatBold, stringResource(R.string.font_weight), s.fontWeightShift.toFloat(), -2f..3f,
            { v -> v.roundToInt().let { if (it > 0) "+${it * 100}" else (it * 100).toString() } }, steps = 4,
        ) { v -> upd { it.copy(fontWeightShift = v.roundToInt()) } }
    }
    SettingsSection(stringResource(R.string.clock)) {
        ToggleItem(Icons.Rounded.Schedule, stringResource(R.string.clock_font_same), null, s.clockFontSame) { v -> upd { it.copy(clockFontSame = v) } }
        if (!s.clockFontSame) {
            ChoiceItem(Icons.Rounded.TextFields, stringResource(R.string.clock_font), null, available, s.clockFont) { v -> upd { it.copy(clockFont = v) } }
        }
    }
}

// ---------------------------------------------------------------- иконки

@Composable
private fun IconsPage(vm: LauncherViewModel, actions: HomeActions, openThemes: () -> Unit) {
    val s by vm.settings.collectAsState()
    val themes by vm.iconThemes.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    SettingsSection(stringResource(R.string.preview)) { GroupItem { LauncherPreview(s) } }
    SettingsSection(stringResource(R.string.icon_themes)) {
        ActionItem(
            Icons.Rounded.Style, stringResource(R.string.icon_themes),
            themes.firstOrNull { it.id == s.iconTheme }?.name ?: stringResource(R.string.icon_theme_system),
            openThemes,
        )
    }
    // Форма значков своя у каждой темы: Liquid Glass — маска iOS, Lite — круг, Expressive — выбор пользователя
    SettingsSection(stringResource(R.string.icon_shape)) {
        GroupItem {
            ShapePicker(s.iconShapeFor(s.style)) { v -> upd { it.withIconShape(it.style, v) } }
        }
    }
    SettingsSection(stringResource(R.string.set_icons)) {
        SliderItem(Icons.Rounded.Interests, stringResource(R.string.icon_size), s.iconScale, 0.7f..1.3f, { "${(it * 100).roundToInt()}%" }, steps = 11) { v ->
            upd { it.copy(iconScale = v) }
        }
        SliderItem(Icons.Rounded.FormatSize, stringResource(R.string.label_size), s.labelScale, 0.8f..1.3f, { "${(it * 100).roundToInt()}%" }, steps = 4) { v ->
            upd { it.copy(labelScale = v) }
        }
        ChoiceItem(
            Icons.Rounded.FormatLineSpacing, stringResource(R.string.label_lines), null,
            listOf(1 to stringResource(R.string.lines_one), 2 to stringResource(R.string.lines_two)), s.labelLines,
        ) { v -> upd { it.copy(labelLines = v) } }
        ToggleItem(
            Icons.Rounded.InvertColors, stringResource(R.string.themed_icons),
            stringResource(if (Build.VERSION.SDK_INT >= 33) R.string.themed_icons_hint else R.string.themed_icons_unsupported),
            s.themedIcons, enabled = Build.VERSION.SDK_INT >= 33,
        ) { v -> upd { it.copy(themedIcons = v) } }
        ToggleItem(Icons.Rounded.ColorLens, stringResource(R.string.duotone_icons), stringResource(R.string.duotone_icons_hint), s.duotoneIcons) { v -> upd { it.copy(duotoneIcons = v) } }
        ToggleItem(Icons.Rounded.TableRows, stringResource(R.string.home_labels), null, s.showHomeLabels) { v -> upd { it.copy(showHomeLabels = v) } }
        ToggleItem(Icons.AutoMirrored.Rounded.ViewList, stringResource(R.string.drawer_labels), null, s.drawerLabels) { v -> upd { it.copy(drawerLabels = v) } }
        ToggleItem(Icons.Rounded.Notifications, stringResource(R.string.badges), stringResource(R.string.badges_hint), s.showBadges) { v ->
            upd { it.copy(showBadges = v) }
            if (v) actions.requestBadgeAccess()
        }
    }
}

// ---------------------------------------------------------------- темы значков

@Composable
private fun IconThemesPage(vm: LauncherViewModel, actions: HomeActions) {
    val s by vm.settings.collectAsState()
    val themes by vm.iconThemes.collectAsState()
    val repo = LocalRepo.current
    LaunchedEffect(Unit) { repo.refreshThemes() }
    fun select(id: String) = vm.updateSettings { it.copy(iconTheme = id) }

    SettingsSection(null) {
        ActionItem(Icons.Rounded.UploadFile, stringResource(R.string.icon_theme_import), stringResource(R.string.icon_theme_import_hint), actions::pickIconTheme)
    }
    SettingsSection(stringResource(R.string.icon_themes)) {
        IconThemeCard(null, stringResource(R.string.icon_theme_system), s.iconTheme.isEmpty(), onDelete = null) { select("") }
        for (t in themes) {
            IconThemeCard(
                t.id, t.name, s.iconTheme == t.id,
                onDelete = if (t.imported) ({
                    if (s.iconTheme == t.id) select("")
                    vm.deleteIconTheme(t.id)
                }) else null,
            ) { select(t.id) }
        }
    }
    SettingsSection(null) {
        ToggleItem(Icons.Rounded.Interests, stringResource(R.string.icon_theme_mask), stringResource(R.string.icon_theme_mask_hint), s.iconThemeMask) { v ->
            vm.updateSettings { it.copy(iconThemeMask = v) }
        }
        GroupItem { Box(Modifier.padding(12.dp)) { InfoCard(Icons.Rounded.Info, stringResource(R.string.icon_theme_format)) } }
    }
}

@Composable
private fun IconThemeCard(id: String?, name: String, selected: Boolean, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val repo = LocalRepo.current
    val previews by produceState(emptyList<ImageBitmap>(), id) {
        value = if (id != null) repo.previewIcons(id) else emptyList()
    }
    GroupItem(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BlobBadge(CircleShape, if (selected) cs.primary else cs.secondaryContainer, 36.dp) {
                    Icon(if (selected) Icons.Rounded.Check else Icons.Rounded.Style, null, tint = if (selected) cs.onPrimary else cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (onDelete != null) IconButton(onDelete, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Delete, stringResource(R.string.menu_uninstall), tint = cs.onSurfaceVariant) }
            }
            if (previews.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (b in previews) Image(b, null, Modifier.size(48.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- рабочий стол

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun HomeScreenPage(vm: LauncherViewModel, onSearchBar: () -> Unit) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    var confirm by remember { mutableStateOf(false) }
    SettingsSection(stringResource(R.string.preview)) { GroupItem { LauncherPreview(s) } }
    SettingsSection(stringResource(R.string.grid)) {
        SliderItem(Icons.Rounded.ViewColumn, stringResource(R.string.columns), s.columns.toFloat(), 3f..7f, { it.roundToInt().toString() }, steps = 3) { v -> upd { it.copy(columns = v.roundToInt()) } }
        SliderItem(Icons.Rounded.TableRows, stringResource(R.string.rows), s.rows.toFloat(), 4f..9f, { it.roundToInt().toString() }, steps = 4) { v -> upd { it.copy(rows = v.roundToInt()) } }
        SliderItem(Icons.Rounded.Padding, stringResource(R.string.grid_padding), s.gridPadding.toFloat(), 0f..32f, { "${it.roundToInt()} dp" }) { v -> upd { it.copy(gridPadding = v.roundToInt()) } }
    }
    SettingsSection(stringResource(R.string.sb_title)) {
        ToggleItem(Icons.Rounded.Search, stringResource(R.string.search_pill), null, s.showSearchPill) { v -> upd { it.copy(showSearchPill = v) } }
        ActionItem(Icons.Rounded.Search, stringResource(R.string.sb_customize), stringResource(R.string.sb_title_hint), onSearchBar)
    }
    SettingsSection(stringResource(R.string.dock)) {
        ToggleItem(Icons.Rounded.Dock, stringResource(R.string.show_dock), null, s.showDock) { v -> upd { it.copy(showDock = v) } }
        SliderItem(Icons.Rounded.GridView, stringResource(R.string.dock_count), s.dockCount.toFloat(), 3f..6f, { it.roundToInt().toString() }, steps = 2) { v -> upd { it.copy(dockCount = v.roundToInt()) } }
        ToggleItem(Icons.Rounded.Palette, stringResource(R.string.dock_background), null, s.dockBackground) { v -> upd { it.copy(dockBackground = v) } }
        SliderItem(Icons.Rounded.Opacity, stringResource(R.string.dock_opacity), s.dockOpacity, 0.2f..1f, { "${(it * 100).roundToInt()}%" }) { v -> upd { it.copy(dockOpacity = v) } }
        SliderItem(Icons.Rounded.Height, stringResource(R.string.dock_height), s.dockHeight.toFloat(), 64f..120f, { "${it.roundToInt()} dp" }) { v -> upd { it.copy(dockHeight = v.roundToInt()) } }
        SliderItem(Icons.Rounded.RoundedCorner, stringResource(R.string.dock_radius), s.dockRadius.toFloat(), 0f..44f, { "${it.roundToInt()} dp" }) { v -> upd { it.copy(dockRadius = v.roundToInt()) } }
    }
    SettingsSection(stringResource(R.string.set_home)) {
        ChoiceItem(
            Icons.Rounded.Schedule, stringResource(R.string.clock), null,
            listOf(
                ClockStyle.BIG to stringResource(R.string.clock_big),
                ClockStyle.COMPACT to stringResource(R.string.clock_compact),
                ClockStyle.OFF to stringResource(R.string.clock_off),
            ),
            s.clockStyle,
        ) { v -> upd { it.copy(clockStyle = v) } }
        ChoiceItem(
            Icons.Rounded.AlignHorizontalCenter, stringResource(R.string.clock_align), null,
            listOf(
                ClockAlign.START to stringResource(R.string.align_start),
                ClockAlign.CENTER to stringResource(R.string.align_center),
                ClockAlign.END to stringResource(R.string.align_end),
            ),
            s.clockAlign,
        ) { v -> upd { it.copy(clockAlign = v) } }
        SliderItem(Icons.Rounded.FormatSize, stringResource(R.string.clock_size), s.clockScale, 0.7f..1.3f, { "${(it * 100).roundToInt()}%" }) { v -> upd { it.copy(clockScale = v) } }
        ToggleItem(Icons.Rounded.CalendarToday, stringResource(R.string.show_date), null, s.showDate) { v -> upd { it.copy(showDate = v) } }
        ToggleItem(Icons.Rounded.BatteryFull, stringResource(R.string.show_battery), null, s.showBattery) { v -> upd { it.copy(showBattery = v) } }
        ToggleItem(Icons.Rounded.Tune, stringResource(R.string.page_indicator), null, s.showPageIndicator) { v -> upd { it.copy(showPageIndicator = v) } }
        ToggleItem(Icons.Rounded.Lock, stringResource(R.string.lock_layout), stringResource(R.string.lock_layout_hint), s.lockLayout) { v -> upd { it.copy(lockLayout = v) } }
        SliderItem(Icons.Rounded.Wallpaper, stringResource(R.string.wallpaper_dim), s.wallpaperDim, 0f..0.7f, { "${(it * 100).roundToInt()}%" }) { v -> upd { it.copy(wallpaperDim = v) } }
    }
    SettingsSection(null) {
        GroupItem {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(Icons.Rounded.Restore)
                Text(stringResource(R.string.reset_layout), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (!confirm) FilledTonalButton(onClick = { confirm = true }, shapes = ButtonDefaults.shapes(), modifier = with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = com.example.expressivelauncher.ui.components.glassContainer(MaterialTheme.colorScheme.secondaryContainer))) { Text(stringResource(R.string.reset)) }
                else Button(onClick = { vm.resetLayout(); confirm = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
            }
        }
    }
}

// ---------------------------------------------------------------- меню приложений

@Composable
private fun DrawerPage(vm: LauncherViewModel, openHidden: () -> Unit) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    SettingsSection(stringResource(R.string.layout)) {
        ChoiceItem(
            Icons.AutoMirrored.Rounded.ViewList, stringResource(R.string.drawer_style), null,
            listOf(DrawerStyle.GRID to stringResource(R.string.style_grid), DrawerStyle.LIST to stringResource(R.string.style_list)),
            s.drawerStyle,
        ) { v -> upd { it.copy(drawerStyle = v) } }
        SliderItem(Icons.Rounded.ViewColumn, stringResource(R.string.columns), s.drawerColumns.toFloat(), 3f..6f, { it.roundToInt().toString() }, steps = 2) { v -> upd { it.copy(drawerColumns = v.roundToInt()) } }
        ChoiceItem(
            Icons.Rounded.SortByAlpha, stringResource(R.string.sort), null,
            listOf(DrawerSort.ALPHABETICAL to stringResource(R.string.sort_alpha), DrawerSort.RECENT to stringResource(R.string.sort_recent)),
            s.drawerSort,
        ) { v -> upd { it.copy(drawerSort = v) } }
        ToggleItem(Icons.Rounded.Swipe, stringResource(R.string.fast_scroller), null, s.fastScroller) { v -> upd { it.copy(fastScroller = v) } }
        ActionItem(Icons.Rounded.VisibilityOff, stringResource(R.string.set_hidden), pluralStringResource(R.plurals.hidden_count, s.hiddenApps.size, s.hiddenApps.size), openHidden)
    }
    SettingsSection(stringResource(R.string.search)) {
        ChoiceItem(
            Icons.Rounded.Search, stringResource(R.string.search_position), null,
            listOf(SearchPosition.BOTTOM to stringResource(R.string.pos_bottom), SearchPosition.TOP to stringResource(R.string.pos_top)),
            s.searchPosition,
        ) { v -> upd { it.copy(searchPosition = v) } }
        ChoiceItem(Icons.Rounded.Public, stringResource(R.string.search_engine), null, SearchEngines.indices.map { it to SearchEngines[it].name }, s.searchEngine) { v ->
            upd { it.copy(searchEngine = v) }
        }
        ToggleItem(Icons.Rounded.Keyboard, stringResource(R.string.auto_keyboard), null, s.autoKeyboard) { v -> upd { it.copy(autoKeyboard = v) } }
        ToggleItem(Icons.Rounded.TouchApp, stringResource(R.string.auto_launch), stringResource(R.string.auto_launch_hint), s.autoLaunchSingle) { v -> upd { it.copy(autoLaunchSingle = v) } }
    }
    SettingsSection(stringResource(R.string.background)) {
        SliderItem(Icons.Rounded.Opacity, stringResource(R.string.opacity), s.drawerOpacity, 0.5f..1f, { "${(it * 100).roundToInt()}%" }) { v -> upd { it.copy(drawerOpacity = v) } }
    }
}

// ---------------------------------------------------------------- жесты

@Composable
private fun GesturesPage(vm: LauncherViewModel, actions: HomeActions) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    val options = listOf(
        GestureAction.NONE to stringResource(R.string.gesture_none),
        GestureAction.DRAWER to stringResource(R.string.gesture_drawer),
        GestureAction.SEARCH to stringResource(R.string.gesture_search),
        GestureAction.NOTIFICATIONS to stringResource(R.string.gesture_notifications),
        GestureAction.LOCK to stringResource(R.string.gesture_lock),
        GestureAction.SETTINGS to stringResource(R.string.gesture_settings),
    )
    SettingsSection(stringResource(R.string.set_gestures)) {
        ChoiceItem(Icons.Rounded.Swipe, stringResource(R.string.swipe_up), null, options, s.swipeUp) { v -> upd { it.copy(swipeUp = v) } }
        ChoiceItem(Icons.Rounded.Swipe, stringResource(R.string.swipe_down), null, options, s.swipeDown) { v -> upd { it.copy(swipeDown = v) } }
        ChoiceItem(Icons.Rounded.TouchApp, stringResource(R.string.double_tap), null, options, s.doubleTap) { v -> upd { it.copy(doubleTap = v) } }
    }
    PermissionsSection(actions, onlyGestures = true)
}

/**
 * Состояние разрешений: включено ли то, без чего не работают бейджи, жесты, ярлыки и размытие обоев.
 * Перечитывается каждый раз, когда окно снова в фокусе — например, после возврата из системных настроек.
 */
@Composable
private fun PermissionsSection(actions: HomeActions, onlyGestures: Boolean = false) {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(focused) { if (focused) tick++ }
    val P = com.example.expressivelauncher.data.Permissions
    val home = remember(tick) { P.isDefaultHome(context) }
    val notif = remember(tick) { P.hasNotificationAccess(context) }
    val a11y = remember(tick) { P.hasAccessibility(context) }
    val files = remember(tick) { P.hasAllFilesAccess() }
    val shortcuts = remember(tick) { P.hasShortcutAccess(context) }
    SettingsSection(stringResource(R.string.permissions)) {
        if (!onlyGestures) {
            PermissionItem(Icons.Rounded.Home, stringResource(R.string.perm_default), stringResource(R.string.perm_default_hint), home, actions::openDefaultLauncherSettings)
            PermissionItem(Icons.Rounded.Notifications, stringResource(R.string.perm_notifications), stringResource(R.string.perm_notifications_hint), notif, actions::requestBadgeAccess)
        }
        PermissionItem(Icons.Rounded.Android, stringResource(R.string.a11y), stringResource(R.string.a11y_hint), a11y, actions::openAccessibilitySettings)
        if (!onlyGestures) {
            PermissionItem(Icons.Rounded.Apps, stringResource(R.string.perm_shortcuts), stringResource(R.string.perm_shortcuts_hint), shortcuts, actions::openDefaultLauncherSettings)
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                PermissionItem(Icons.Rounded.Wallpaper, stringResource(R.string.perm_files), stringResource(R.string.perm_files_hint), files) {
                    com.example.expressivelauncher.ui.components.WallpaperGlass.requestAccess(context)
                }
            }
            val bg = remember { P.backgroundSettingsIntent(context) }
            if (bg != null) {
                ActionItem(Icons.Rounded.BatteryFull, stringResource(R.string.perm_background), stringResource(R.string.perm_background_hint), onClick = {
                    runCatching { context.startActivity(bg) }
                })
            }
        }
    }
}

/** Пункт разрешения: справа — «таблетка» состояния, зелёная по смыслу (основной цвет), если всё включено. */
@Composable
private fun PermissionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, granted: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (granted) cs.primaryContainer else cs.errorContainer, label = "permBg")
    val fg = if (granted) cs.onPrimaryContainer else cs.onErrorContainer
    ActionItem(icon, title, subtitle, onClick = onClick, trailing = {
        Row(
            Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(if (granted) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = fg, modifier = Modifier.size(16.dp))
            Text(stringResource(if (granted) R.string.perm_on else R.string.perm_off), style = MaterialTheme.typography.labelMedium, color = fg)
        }
    })
}

// ---------------------------------------------------------------- резервная копия и о программе

@Composable
private fun BackupPage(actions: HomeActions) {
    SettingsSection(stringResource(R.string.set_backup)) {
        ActionItem(Icons.Rounded.Upload, stringResource(R.string.export_layout), stringResource(R.string.export_hint), actions::exportLayout)
        ActionItem(Icons.Rounded.Download, stringResource(R.string.import_layout), stringResource(R.string.import_hint), actions::importLayout)
    }
}

/** Автор и канал — синие ссылки на Telegram (как в Expressive Keyboard). */
@Composable
private fun AuthorLine() {
    val context = LocalContext.current
    val blue = Color(0xFF2AABEE)
    val label = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    @Composable
    fun Link(title: String, handle: String) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = label, color = muted, maxLines = 1, softWrap = false)
            Text(
                "@$handle", style = label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = blue, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/$handle")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Link(stringResource(R.string.author), "Shahedeer")
        Link(stringResource(R.string.channel), "materialyouboss")
    }
}

@Composable
private fun AboutPage(actions: HomeActions) {
    AboutCard()
    AuthorLine()
    CrashSection()
    PermissionsSection(actions)
    SettingsSection(stringResource(R.string.device)) {
        GroupItem {
            Text(
                com.example.expressivelauncher.data.Permissions.vendorSkin + " · " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Отчёт о сбоях: показывается, только если лаунчер падал или система его выгружала. */
@Composable
private fun CrashSection() {
    val context = LocalContext.current
    val C = com.example.expressivelauncher.data.CrashLog
    var rev by remember { mutableIntStateOf(0) }
    val exits = remember(rev) { C.exits(context) }
    val stored = remember(rev) { C.stored(context) }
    if (exits.isEmpty() && stored == null) return
    val report = { C.report(context, BuildConfig.VERSION_NAME) }
    SettingsSection(stringResource(R.string.crash_title)) {
        GroupItem {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.crash_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (e in exits.take(5)) Text("• $e", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                stored?.lineSequence()?.drop(1)?.firstOrNull()?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        ActionItem(Icons.Rounded.Share, stringResource(R.string.crash_send), null, onClick = {
            runCatching {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report())
                context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        ActionItem(Icons.Rounded.ContentCopy, stringResource(R.string.crash_copy), null, onClick = {
            runCatching {
                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", report()))
                com.example.expressivelauncher.ui.components.LauncherToast.show(context.getString(R.string.crash_copied))
            }
        })
        ActionItem(Icons.Rounded.DeleteSweep, stringResource(R.string.crash_clear), null, onClick = {
            C.clear(context)
            rev++
        })
    }
}

@Composable
private fun AboutCard() {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(cs.primaryContainer).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlobBadge(Blob.COOKIE_12.shape(), cs.primary, 96.dp) { Icon(Icons.Rounded.Home, null, tint = cs.onPrimary, modifier = Modifier.size(44.dp)) }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmallEmphasized, color = cs.onPrimaryContainer)
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
        Text(stringResource(R.string.about_desc), style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun HiddenPage(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val apps by vm.allApps.collectAsState()
    SettingsSection(null) {
        for (a in apps) {
            GroupItem(onClick = {
                vm.updateSettings { it.copy(hiddenApps = if (a.key in it.hiddenApps) it.hiddenApps - a.key else it.hiddenApps + a.key) }
            }) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AppIconImage(a, 40.dp, showBadge = false)
                    Text(a.label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    val hidden = a.key in s.hiddenApps
                    Switch(
                        hidden,
                        { on -> vm.updateSettings { it.copy(hiddenApps = if (on) it.hiddenApps + a.key else it.hiddenApps - a.key) } },
                        thumbContent = if (hidden) {
                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize)) }
                        } else null,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- вибрация

@Composable
private fun HapticsPage(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val haptics = LocalHaptics.current
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    if (!haptics.available) {
        InfoCard(Icons.Rounded.Info, stringResource(R.string.haptic_missing))
    }
    SettingsSection(null) {
        ToggleItem(Icons.Rounded.Vibration, stringResource(R.string.haptics), null, s.haptics) { v ->
            upd { it.copy(haptics = v) }
            if (v) haptics.preview(s.hapticLevel)
        }
        ChoiceItem(
            Icons.Rounded.Waves, stringResource(R.string.haptic_strength), null,
            listOf(
                HapticLevel.LIGHT to stringResource(R.string.haptic_light),
                HapticLevel.MEDIUM to stringResource(R.string.haptic_medium),
                HapticLevel.STRONG to stringResource(R.string.haptic_strong),
            ),
            s.hapticLevel,
        ) { v ->
            upd { it.copy(hapticLevel = v) }
            if (s.haptics) haptics.preview(v)
        }
    }
    SettingsSection(stringResource(R.string.haptic_events)) {
        ToggleItem(Icons.Rounded.TouchApp, stringResource(R.string.haptic_touch), stringResource(R.string.haptic_touch_hint), s.hapticTouch, enabled = s.haptics) { v -> upd { it.copy(hapticTouch = v) } }
        ToggleItem(Icons.Rounded.OpenWith, stringResource(R.string.haptic_drag), stringResource(R.string.haptic_drag_hint), s.hapticDrag, enabled = s.haptics) { v -> upd { it.copy(hapticDrag = v) } }
        ToggleItem(Icons.Rounded.Swipe, stringResource(R.string.haptic_pages), stringResource(R.string.haptic_pages_hint), s.hapticPages, enabled = s.haptics) { v -> upd { it.copy(hapticPages = v) } }
        ToggleItem(Icons.Rounded.Apps, stringResource(R.string.haptic_drawer), stringResource(R.string.haptic_drawer_hint), s.hapticDrawer, enabled = s.haptics) { v -> upd { it.copy(hapticDrawer = v) } }
    }
}
