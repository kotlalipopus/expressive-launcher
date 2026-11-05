@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.DrawerStyle
import com.example.expressivelauncher.data.GestureAction
import com.example.expressivelauncher.data.Permissions
import com.example.expressivelauncher.ui.components.WallpaperGlass
import com.example.expressivelauncher.ui.components.glassEdge
import com.example.expressivelauncher.ui.home.HomeActions
import com.example.expressivelauncher.ui.settings.ChoiceItem
import com.example.expressivelauncher.ui.settings.DynamicColorItems
import com.example.expressivelauncher.ui.settings.GroupItem
import com.example.expressivelauncher.ui.settings.LocalSectionCounter
import com.example.expressivelauncher.ui.settings.MiniDesktop
import com.example.expressivelauncher.ui.settings.SettingsSection
import com.example.expressivelauncher.ui.settings.SliderItem
import com.example.expressivelauncher.ui.settings.ThemeCards
import com.example.expressivelauncher.ui.settings.ThemeModeItem
import com.example.expressivelauncher.ui.settings.ToggleItem
import com.example.expressivelauncher.ui.settings.panelColor
import com.example.expressivelauncher.ui.theme.LauncherThemes
import com.example.expressivelauncher.ui.theme.LocalThemeSpec
import com.example.expressivelauncher.ui.theme.rememberThemeSwitch
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Версия первоначальной настройки. Добавили шаг — подняли версию и указали её в [OnboardingStep.since]:
 * тем, кто прошёл прошлую версию, покажутся только новые шаги.
 */
const val ONBOARDING_VERSION = 1

/** Шаги по порядку. [optional] — у шага есть «Пропустить». */
enum class OnboardingStep(val since: Int, val optional: Boolean) {
    WELCOME(1, false),
    THEME(1, false),
    APPEARANCE(1, true),
    HOME(1, true),
    BEHAVIOR(1, true),
    PERMISSIONS(1, true),
    DEFAULT_HOME(1, true),
    DONE(1, false),
    ;

    companion object {
        /** Всё — тем, кто настройку не проходил; после обновления — только новые шаги и финал. */
        fun forUser(passed: Int): List<OnboardingStep> =
            if (passed <= 0) entries.toList()
            else entries.filter { it.since > passed || it == DONE }
    }
}

/**
 * Онбординг поверх рабочего стола. Появляется при первом запуске и у обновившихся, кто его не проходил;
 * все выборы сразу пишутся в настройки, поэтому тема применяется на лету, а после закрытия посреди
 * настройки ничего не теряется. В конце растворяется с приближением, а стол встречает «волной» значков.
 */
@Composable
fun OnboardingHost(visible: Boolean, vm: LauncherViewModel, actions: HomeActions, onFinished: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(420)) + scaleOut(tween(480), targetScale = 1.12f),
    ) {
        // Секции онбординга считаются отдельно от настроек: каждая появляется по очереди
        CompositionLocalProvider(LocalSectionCounter provides remember { intArrayOf(0) }) {
            OnboardingScreen(vm, actions, onFinished)
        }
    }
}

@Composable
private fun OnboardingScreen(vm: LauncherViewModel, actions: HomeActions, onFinished: () -> Unit) {
    val s by vm.settings.collectAsState()
    val spec = LocalThemeSpec.current
    val cs = MaterialTheme.colorScheme
    // Набор шагов фиксируется при открытии: в конце версия станет новой, а экран ещё должен растворяться
    val steps = remember { OnboardingStep.forUser(s.onboardingVersion) }
    val pager = rememberPagerState(initialPage = s.onboardingStep.coerceIn(0, steps.lastIndex)) { steps.size }
    val scope = rememberCoroutineScope()
    var finished by remember { mutableStateOf(false) }

    // Текущий шаг сохраняется: закрыли приложение — при следующем запуске продолжим с него
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { p -> if (!finished) vm.updateSettings { it.copy(onboardingStep = p) } }
    }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    fun goTo(page: Int) {
        scope.launch { pager.animateScrollToPage(page.coerceIn(0, steps.lastIndex), animationSpec = spatial) }
    }
    fun finish() {
        if (finished) return
        finished = true
        vm.updateSettings { it.copy(onboardingVersion = ONBOARDING_VERSION, onboardingStep = 0) }
        onFinished()
    }
    // Системная «Назад» ведёт на предыдущий шаг; на первом шаге ничего не делает, а не закрывает онбординг
    BackHandler { if (pager.currentPage > 0) goTo(pager.currentPage - 1) }

    val glassBg = spec.glass
    val bg = when {
        // Стекло: окно размывает обои под онбордингом (стол скрыт), поверх — лёгкая тонировка
        glassBg && Build.VERSION.SDK_INT >= 31 -> cs.surface.copy(alpha = 0.34f)
        glassBg -> cs.surface.copy(alpha = 0.86f)
        else -> cs.surface
    }
    val step = steps[pager.currentPage]
    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            // Касания не проходят на рабочий стол под онбордингом
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressDots(pager, steps.size, Modifier.weight(1f))
                if (step.optional) {
                    TextButton(onClick = { goTo(pager.currentPage + 1) }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.onb_skip))
                    }
                }
            }
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
                key = { steps[it].name },
            ) { page ->
                // Соседний шаг уходит с лёгким сдвигом и прозрачностью — переход мягче простого листания
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - offset * 0.6f }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StepContent(steps[page], vm, actions, active = pager.settledPage == page)
                    Spacer(Modifier.height(16.dp))
                }
            }
            BottomBar(
                first = pager.currentPage == 0,
                step = step,
                onBack = { goTo(pager.currentPage - 1) },
                onNext = { if (pager.currentPage >= steps.lastIndex) finish() else goTo(pager.currentPage + 1) },
            )
        }
    }
}

@Composable
private fun ProgressDots(pager: PagerState, count: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == pager.currentPage
            val w by animateDpAsState(if (active) 24.dp else 8.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "onbDot")
            Box(
                Modifier.size(w, 8.dp).clip(CircleShape)
                    .background(if (i <= pager.currentPage) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun BottomBar(first: Boolean, step: OnboardingStep, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!first && step != OnboardingStep.DONE) {
            OutlinedButton(
                onClick = onBack,
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.onb_back))
            }
        }
        Spacer(Modifier.weight(1f))
        val label = when (step) {
            OnboardingStep.WELCOME -> R.string.onb_start
            OnboardingStep.DONE -> R.string.onb_to_home
            else -> R.string.onb_next
        }
        // Главное действие — крупная кнопка Expressive
        Button(
            onClick = onNext,
            shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight).then(with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }),
        ) {
            Text(stringResource(label), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            if (step != OnboardingStep.DONE) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
        }
    }
}

@Composable
private fun ColumnScope.StepHeader(title: Int, subtitle: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
    Text(stringResource(subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ColumnScope.StepContent(step: OnboardingStep, vm: LauncherViewModel, actions: HomeActions, active: Boolean) {
    when (step) {
        OnboardingStep.WELCOME -> WelcomeStep()
        OnboardingStep.THEME -> ThemeStep(vm)
        OnboardingStep.APPEARANCE -> AppearanceStep(vm)
        OnboardingStep.HOME -> HomeStep(vm)
        OnboardingStep.BEHAVIOR -> BehaviorStep(vm)
        OnboardingStep.PERMISSIONS -> PermissionsStep(actions)
        OnboardingStep.DEFAULT_HOME -> DefaultHomeStep(actions)
        OnboardingStep.DONE -> DoneStep(active)
    }
}

// ---------------------------------------------------------------- шаги

@Composable
private fun ColumnScope.WelcomeStep() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val spec = LocalThemeSpec.current
    val icon = remember {
        runCatching { context.packageManager.getApplicationIcon(context.packageName).toBitmap(256, 256).asImageBitmap() }.getOrNull()
    }
    // Логотип в выразительной форме медленно вращается; в Lite стоит на месте
    val spin = remember { Animatable(0f) }
    LaunchedEffect(spec.morph) {
        if (spec.morph) while (true) spin.animateTo(spin.value + 360f, tween(20000, easing = LinearEasing))
    }
    Spacer(Modifier.height(48.dp))
    Box(Modifier.align(Alignment.CenterHorizontally).size(168.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { rotationZ = spin.value }
                .clip(if (spec.lite) CircleShape else MaterialShapes.Cookie12Sided.toShape())
                .background(cs.primaryContainer),
        )
        if (icon != null) Image(icon, null, Modifier.size(104.dp).clip(CircleShape))
    }
    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.app_name), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displaySmall, color = cs.onSurface,
    )
    Text(
        stringResource(R.string.onb_welcome_sub), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
    )
}

@Composable
private fun ColumnScope.ThemeStep(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val switch = rememberThemeSwitch()
    StepHeader(R.string.onb_theme_title, R.string.onb_theme_sub)
    // Выбор применяется сразу: следующие шаги уже в выбранной теме
    ThemeCards(s.style, s, previewHeight = 156.dp) { spec -> switch { vm.updateSettings { it.copy(style = spec.style) } } }
    // Liquid Glass лучше всего выглядит со значками в стиле iOS 26: подсказка и выбор установленного набора
    if (s.style == com.example.expressivelauncher.data.LauncherStyle.LIQUID_GLASS) {
        SettingsSection(stringResource(R.string.onb_icon_pack_title)) {
            GroupItem {
                Text(
                    stringResource(R.string.onb_icon_pack_hint), Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            com.example.expressivelauncher.ui.settings.IconPackPicker(vm)
        }
    }
}

@Composable
private fun ColumnScope.AppearanceStep(vm: LauncherViewModel) {
    val spec = LocalThemeSpec.current
    StepHeader(R.string.onb_appearance_title, R.string.onb_appearance_sub)
    SettingsSection(null) {
        ThemeModeItem(vm)
        if (spec.colorOptions) DynamicColorItems(vm)
    }
}

@Composable
private fun ColumnScope.HomeStep(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    fun upd(f: (com.example.expressivelauncher.data.Settings) -> com.example.expressivelauncher.data.Settings) = vm.updateSettings(f)
    StepHeader(R.string.onb_home_title, R.string.onb_home_sub)
    // Живое превью: сетка, подписи и док меняются вместе с ползунками
    MiniDesktop(LauncherThemes.of(s.style), s, Modifier.fillMaxWidth().height(150.dp))
    SettingsSection(null) {
        SliderItem(Icons.Rounded.ViewColumn, stringResource(R.string.columns), s.columns.toFloat(), 3f..7f, { it.roundToInt().toString() }, steps = 3) { v ->
            upd { it.copy(columns = v.roundToInt()) }
        }
        SliderItem(Icons.Rounded.TableRows, stringResource(R.string.rows), s.rows.toFloat(), 4f..9f, { it.roundToInt().toString() }, steps = 4) { v ->
            upd { it.copy(rows = v.roundToInt()) }
        }
        SliderItem(Icons.Rounded.Interests, stringResource(R.string.icon_size), s.iconScale, 0.7f..1.3f, { "${(it * 100).roundToInt()}%" }, steps = 11) { v ->
            upd { it.copy(iconScale = v) }
        }
        ToggleItem(Icons.Rounded.TableRows, stringResource(R.string.home_labels), null, s.showHomeLabels) { v -> upd { it.copy(showHomeLabels = v) } }
        ToggleItem(Icons.Rounded.Dock, stringResource(R.string.show_dock), null, s.showDock) { v -> upd { it.copy(showDock = v) } }
    }
}

@Composable
private fun ColumnScope.BehaviorStep(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val spec = LocalThemeSpec.current
    fun upd(f: (com.example.expressivelauncher.data.Settings) -> com.example.expressivelauncher.data.Settings) = vm.updateSettings(f)
    val gestures = listOf(
        GestureAction.NONE to stringResource(R.string.gesture_none),
        GestureAction.DRAWER to stringResource(R.string.gesture_drawer),
        GestureAction.SEARCH to stringResource(R.string.gesture_search),
        GestureAction.NOTIFICATIONS to stringResource(R.string.gesture_notifications),
        GestureAction.LOCK to stringResource(R.string.gesture_lock),
        GestureAction.SETTINGS to stringResource(R.string.gesture_settings),
    )
    StepHeader(R.string.onb_behavior_title, R.string.onb_behavior_sub)
    SettingsSection(stringResource(R.string.onb_gestures)) {
        ChoiceItem(Icons.Rounded.Swipe, stringResource(R.string.swipe_up), null, gestures, s.swipeUp) { v -> upd { it.copy(swipeUp = v) } }
        ChoiceItem(Icons.Rounded.Swipe, stringResource(R.string.swipe_down), null, gestures, s.swipeDown) { v -> upd { it.copy(swipeDown = v) } }
        ChoiceItem(Icons.Rounded.TouchApp, stringResource(R.string.double_tap), null, gestures, s.doubleTap) { v -> upd { it.copy(doubleTap = v) } }
    }
    SettingsSection(stringResource(R.string.onb_animations)) {
        ToggleItem(
            Icons.Rounded.Animation, stringResource(R.string.launch_animation),
            stringResource(if (spec.portalLaunch) R.string.launch_animation_hint else R.string.onb_launch_lite_hint), s.launchAnimation,
        ) { v -> upd { it.copy(launchAnimation = v) } }
        SliderItem(Icons.Rounded.Speed, stringResource(R.string.anim_speed), s.animSpeed, 0.5f..2f, { "×" + String.format(java.util.Locale.US, "%.2f", it) }, steps = 5) { v ->
            upd { it.copy(animSpeed = v) }
        }
    }
    SettingsSection(stringResource(R.string.onb_drawer)) {
        ChoiceItem(
            Icons.Rounded.Apps, stringResource(R.string.drawer_style), null,
            listOf(DrawerStyle.GRID to stringResource(R.string.style_grid), DrawerStyle.LIST to stringResource(R.string.style_list)),
            s.drawerStyle,
        ) { v -> upd { it.copy(drawerStyle = v) } }
    }
}

/** Разрешение: что даёт, есть ли уже, кнопки «Разрешить» и «Пропустить». */
private class PermissionEntry(
    val key: String,
    val icon: ImageVector,
    val title: Int,
    val why: Int,
    val granted: () -> Boolean,
    val request: () -> Unit,
)

@Composable
private fun ColumnScope.PermissionsStep(actions: HomeActions) {
    val context = LocalContext.current
    // Окно снова в фокусе — вернулись из системных настроек: перечитываем, что уже выдано
    val focused = LocalWindowInfo.current.isWindowFocused
    val entries = remember {
        buildList {
            add(PermissionEntry("notif", Icons.Rounded.Notifications, R.string.perm_notifications, R.string.onb_perm_notifications_why,
                { Permissions.hasNotificationAccess(context) }, { actions.requestBadgeAccess() }))
            add(PermissionEntry("a11y", Icons.Rounded.Accessibility, R.string.a11y, R.string.onb_perm_a11y_why,
                { Permissions.hasAccessibility(context) }, { actions.openAccessibilitySettings() }))
            if (Build.VERSION.SDK_INT >= 30) {
                add(PermissionEntry("files", Icons.Rounded.Wallpaper, R.string.perm_files, R.string.onb_perm_files_why,
                    { Permissions.hasAllFilesAccess() }, { WallpaperGlass.requestAccess(context) }))
            }
        }
    }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(focused) { if (focused) refresh++ }
    var skipped by rememberSaveable { mutableStateOf(setOf<String>()) }
    StepHeader(R.string.onb_perm_title, R.string.onb_perm_sub)
    for (e in entries) {
        val granted = remember(refresh) { e.granted() }
        PermissionCard(e, granted, e.key in skipped, onSkip = { skipped = skipped + e.key })
    }
}

@Composable
private fun PermissionCard(e: PermissionEntry, granted: Boolean, skipped: Boolean, onSkip: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        Modifier.fillMaxWidth().clip(shape).background(panelColor(cs.surfaceContainerHigh)).glassEdge(shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).clip(MaterialShapes.Cookie4Sided.toShape())
                    .background(if (granted) cs.primary else cs.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (granted) Icons.Rounded.Check else e.icon, null, tint = if (granted) cs.onPrimary else cs.onSecondaryContainer)
            }
            Text(stringResource(e.title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        }
        Text(stringResource(e.why), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        when {
            granted -> Text(stringResource(R.string.onb_perm_granted), style = MaterialTheme.typography.labelLarge, color = cs.primary)
            skipped -> Text(stringResource(R.string.onb_perm_skipped), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = e.request, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.onb_allow)) }
                TextButton(onClick = onSkip, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.onb_skip)) }
            }
        }
    }
}

@Composable
private fun ColumnScope.DefaultHomeStep(actions: HomeActions) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val focused = LocalWindowInfo.current.isWindowFocused
    var isDefault by remember { mutableStateOf(Permissions.isDefaultHome(context)) }
    LaunchedEffect(focused) { if (focused) isDefault = Permissions.isDefaultHome(context) }
    StepHeader(R.string.onb_default_title, R.string.onb_default_sub)
    GroupItem {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(56.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(if (isDefault) cs.primary else cs.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (isDefault) Icons.Rounded.Check else Icons.Rounded.Home, null, tint = if (isDefault) cs.onPrimary else cs.onTertiaryContainer)
                }
                Text(
                    stringResource(if (isDefault) R.string.onb_default_done else R.string.perm_default_hint),
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface,
                )
            }
            if (!isDefault) {
                // Android 10+ — системный диалог выбора (RoleManager), раньше — экран «Приложения по умолчанию»
                Button(
                    onClick = { actions.openDefaultLauncherSettings() },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.onb_make_default)) }
            }
        }
    }
}

@Composable
private fun ColumnScope.DoneStep(active: Boolean) {
    val cs = MaterialTheme.colorScheme
    val spec = LocalThemeSpec.current
    // Галочка «выпрыгивает», когда шаг становится текущим
    val pop = remember { Animatable(0.4f) }
    val spatial = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(active) {
        if (active) { pop.snapTo(0.4f); pop.animateTo(1f, spatial) }
    }
    Spacer(Modifier.height(64.dp))
    Box(
        Modifier.align(Alignment.CenterHorizontally).size(150.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.4f) / 0.6f).coerceIn(0f, 1f) }
            .clip(if (spec.lite) CircleShape else MaterialShapes.SoftBurst.toShape())
            .background(cs.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(72.dp))
    }
    Spacer(Modifier.height(28.dp))
    Text(
        stringResource(R.string.onb_done_title), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displaySmall, color = cs.onSurface,
    )
    Text(
        stringResource(R.string.onb_done_sub), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
    )
}
