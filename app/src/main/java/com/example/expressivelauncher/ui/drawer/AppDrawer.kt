@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.drawer

import com.example.expressivelauncher.ui.theme.accentColors
import com.example.expressivelauncher.data.displayLabel
import com.example.expressivelauncher.ui.settings.panelColor
import com.example.expressivelauncher.ui.components.glassEdge
import com.example.expressivelauncher.ui.components.ThemedSurface
import com.example.expressivelauncher.ui.components.accentGlow
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxHeight as fillHeight
import com.example.expressivelauncher.ui.settings.Blob
import com.example.expressivelauncher.ui.settings.BlobBadge
import com.example.expressivelauncher.ui.settings.shape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.DrawerSort
import com.example.expressivelauncher.data.DrawerStyle
import com.example.expressivelauncher.data.SearchEngines
import com.example.expressivelauncher.data.SearchLogic
import com.example.expressivelauncher.data.SearchPosition
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.components.HapticGroup
import com.example.expressivelauncher.ui.components.HapticKind
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.AppLabel
import com.example.expressivelauncher.ui.components.bouncyClickable
import com.example.expressivelauncher.ui.components.GlassPanel
import com.example.expressivelauncher.ui.components.glassEnabled
import com.example.expressivelauncher.ui.components.LocalBackdrop
import com.example.expressivelauncher.ui.components.backdropSource
import com.example.expressivelauncher.ui.components.rememberBackdrop
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import com.example.expressivelauncher.ui.home.DrawerState
import com.example.expressivelauncher.ui.home.HomeActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    drawer: DrawerState,
    screenHeightPx: Float,
    searchRequest: Int,
    actions: HomeActions,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    picked: Set<String>,
    onPick: (AppInfo) -> Unit,
    onClearPicked: () -> Unit,
    onAddPicked: () -> Unit,
    menuOpen: Boolean,
    /** Снимок списка: его размывают панель отмеченных приложений и меню приложения. */
    drawerBackdrop: com.example.expressivelauncher.ui.components.Backdrop,
) {
    val settings = LocalSettings.current
    val apps by vm.drawerApps.collectAsState()
    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<Float>()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    var overflow by remember { mutableStateOf(false) }

    val results = remember(query, apps) { SearchLogic.searchApps(apps, query) }
    val engine = SearchEngines[settings.searchEngine.coerceIn(SearchEngines.indices)]

    // Фокус на поиск: по нажатию на капсулу на рабочем столе или автоматически (настройка)
    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) { delay(120); runCatching { focus.requestFocus() } }
    }
    val focusManager = LocalFocusManager.current
    val autoKeyboard = settings.autoKeyboard
    LaunchedEffect(autoKeyboard) {
        snapshotFlow { drawer.progress >= 0.999f }.collect { open ->
            if (open && autoKeyboard) runCatching { focus.requestFocus() }
        }
    }
    // Меню остаётся в композиции и после закрытия, поэтому поиск сбрасываем сами
    LaunchedEffect(Unit) {
        snapshotFlow { drawer.progress <= 0.001f }.collect { closed ->
            if (closed) {
                query = ""
                overflow = false
                keyboard?.hide()
                focusManager.clearFocus(force = true)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
    // Клавиатура не должна висеть над анимацией запуска и ждать пользователя при возвращении
    // В режиме отметок касание и долгое нажатие только отмечают приложение
    val picking = picked.isNotEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current
    val accent = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.accentChrome
    val launchApp: (AppInfo, Rect?) -> Unit = { app, rect ->
        if (picking) onPick(app) else {
            com.example.expressivelauncher.data.RecentApps.record(context, app.key)
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            onLaunch(app, rect)
        }
    }
    val holdApp: (AppInfo, Rect) -> Unit = { app, rect -> if (picking) onPick(app) else onMenu(app, rect) }
    // Под меню приложения список размывается, как стол под шторкой
    val glass = glassEnabled()
    // Список под меню значка не размывается: размытие — на самой панели меню
    val menuBlur = 0f

    val connection = remember(screenHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && drawer.progress < 1f) {
                    drawer.dragBy(-available.y / (screenHeightPx * 0.8f))
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    drawer.dragBy(-available.y / (screenHeightPx * 0.8f))
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drawer.progress < 1f) {
                    drawer.settle(-available.y / screenHeightPx, spatial, openThreshold = 0.7f)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Инерция прокрутки, докатившаяся до верха списка, не должна закрывать полностью открытое меню
                if (available.y > 0f && drawer.progress < 1f) {
                    drawer.settle(-available.y / screenHeightPx, spatial, openThreshold = 0.7f)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val cs = MaterialTheme.colorScheme
    // Expressive: меню — насыщенный тон акцента (между тёмным и светлым фиксированными тонами), подписи светлые
    val ac = accentColors()
    val surface = if (accent) ac.sheet else cs.surfaceContainer
    val onDrawer = if (accent) ac.onSheet else cs.onSurface
    // Личные / рабочие приложения: переключатель появляется, только если есть рабочий профиль
    val hasWork = accent && apps.any { it.isWork }
    var workTab by remember { mutableStateOf(false) }
    val shownApps = remember(apps, hasWork, workTab) { if (hasWork) apps.filter { it.isWork == workTab } else apps }
    LaunchedEffect(Unit) { com.example.expressivelauncher.data.RecentApps.load(context) }
    val recentKeys = com.example.expressivelauncher.data.RecentApps.keys
    val suggestions = remember(apps, recentKeys) {
        val byKey = apps.associateBy { it.key }
        val recent = recentKeys.mapNotNull { byKey[it] }
        (recent + apps.filter { a -> recent.none { it.key == a.key } }).take(5)
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1f - drawer.progress) * size.height
                alpha = (drawer.progress * 4f).coerceAtMost(1f)
                // Углы подтягиваются по мере открытия
                val corner = (72f - 40f * drawer.progress).coerceAtLeast(0f).dp
                shape = RoundedCornerShape(topStart = corner, topEnd = corner)
                clip = true
            }
            // При размытии меню остаётся полупрозрачным, иначе эффекта просто не видно
            .background(
                surface.copy(
                    alpha = when {
                        // Liquid Glass: сквозь меню виден сильно размытый стол
                        com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass -> (settings.glassOpacity * 0.85f).coerceIn(0.15f, 0.8f)
                        accent && settings.blurBackground && !LocalFx.current.lite -> minOf(settings.drawerOpacity, 0.86f)
                        settings.blurBackground && !LocalFx.current.lite -> minOf(settings.drawerOpacity, 0.72f)
                        else -> settings.drawerOpacity
                    },
                ),
            )
            .nestedScroll(connection),
    ) {
        CompositionLocalProvider(LocalContentColor provides onDrawer, LocalPicked provides picked, LocalDrawerLabel provides if (accent) onDrawer else Color.Unspecified) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val radius = menuBlur * 22.dp.toPx() * settings.blurStrength
                    renderEffect = if (radius > 0.5f) androidx.compose.ui.graphics.BlurEffect(radius, radius, androidx.compose.ui.graphics.TileMode.Decal) else null
                }
                // Панель отмеченных приложений размывает список под собой
                .backdropSource(drawerBackdrop)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // «Ручка» и заголовок — зона для закрытия протягиванием
            Column(
                Modifier.draggable(
                    rememberDraggableState { drawer.dragBy(-it / (screenHeightPx * 0.8f)) },
                    Orientation.Vertical,
                    onDragStopped = { drawer.settle(-it / screenHeightPx, spatial, openThreshold = 0.7f) },
                ),
            ) {
                Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(36.dp).height(4.dp)
                            .graphicsLayer { scaleX = 1f + (1f - drawer.progress) * 1.6f }
                            .clip(CircleShape).background(if (accent) ac.onSheet.copy(alpha = 0.4f) else cs.outlineVariant),
                    )
                }
                if (!accent) AnimatedVisibility(
                    query.isBlank(),
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                    exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
                ) { DrawerHeader(apps.size) }
            }
            val onGo = { goSearch(query, results, vm, launchApp, actions, engine.url(query)) }
            val profileTabs: @Composable () -> Unit = {
                if (hasWork) AnimatedVisibility(query.isBlank()) { ProfileTabs(workTab) { workTab = it } }
            }
            if (settings.searchPosition == SearchPosition.TOP) {
                if (accent) {
                    AccentSearchBlock(
                        top = true, query, { query = it }, focus, overflow, { overflow = it }, vm, actions, onGo,
                        suggestions, launchApp, holdApp,
                    )
                    profileTabs()
                } else SearchField(query, { query = it }, focus, overflow, { overflow = it }, vm, actions, onGo = onGo)
            }
            AnimatedContent(
                targetState = query.isNotBlank(),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.94f)) togetherWith
                        (fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.97f))
                },
                label = "drawerContent",
            ) { searching ->
                if (searching) {
                    SearchResults(results, query, engine.name, launchApp, holdApp) { actions.openUrl(engine.url(query)) }
                } else {
                    AppsContent(shownApps, drawer, launchApp, holdApp)
                }
            }
            if (settings.searchPosition == SearchPosition.BOTTOM) {
                if (accent) {
                    profileTabs()
                    AccentSearchBlock(
                        top = false, query, { query = it }, focus, overflow, { overflow = it }, vm, actions, onGo,
                        suggestions, launchApp, holdApp,
                        modifier = Modifier.imePadding().windowInsetsPadding(WindowInsets.navigationBars),
                    )
                } else SearchField(
                    query, { query = it }, focus, overflow, { overflow = it }, vm, actions,
                    modifier = Modifier.imePadding().windowInsetsPadding(WindowInsets.navigationBars),
                    onGo = onGo,
                )
            } else {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
        // Панель отмеченных приложений: вынести их на стол разом
        AnimatedVisibility(
            picking,
            Modifier.align(Alignment.BottomCenter).imePadding().windowInsetsPadding(WindowInsets.navigationBars),
            enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.fastEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
        ) {
            var lastCount by remember { mutableStateOf(1) }
            if (picked.isNotEmpty()) lastCount = picked.size
            CompositionLocalProvider(LocalBackdrop provides drawerBackdrop) { PickedBar(lastCount, onAddPicked, onClearPicked) }
        }
        }
    }
}

private val LocalPicked = androidx.compose.runtime.compositionLocalOf<Set<String>> { emptySet() }

/** Цвет подписей значков в меню приложений; Unspecified — обычный цвет подписи. */
private val LocalDrawerLabel = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

/**
 * Expressive: блок поиска — тёмная панель тона акцента, как док на столе: строка поиска и ряд недавних приложений.
 * Сверху меню строка идёт первой, снизу (настройка «Строка поиска внизу») — последней, рядом с пальцем.
 */
@Composable
private fun AccentSearchBlock(
    top: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    focus: FocusRequester,
    overflow: Boolean,
    setOverflow: (Boolean) -> Unit,
    vm: LauncherViewModel,
    actions: HomeActions,
    onGo: () -> Unit,
    suggestions: List<AppInfo>,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ac = accentColors()
    val motion = MaterialTheme.motionScheme
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth()
            .accentGlow(shape, 0.6f)
            .clip(shape).background(ac.panel).padding(8.dp),
    ) {
        val field: @Composable () -> Unit = {
            SearchField(query, onQuery, focus, overflow, setOverflow, vm, actions, onGo = onGo, accent = true)
        }
        val row: @Composable () -> Unit = {
            AnimatedVisibility(
                query.isBlank() && suggestions.isNotEmpty(),
                enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    for (app in suggestions) key(app.key) {
                        SuggestedApp(app, onLaunch, onMenu, Modifier.weight(1f))
                    }
                }
            }
        }
        if (top) { field(); row() } else { row(); field() }
    }
}

@Composable
private fun SuggestedApp(app: AppInfo, onLaunch: (AppInfo, Rect?) -> Unit, onMenu: (AppInfo, Rect) -> Unit, modifier: Modifier) {
    val iconSize = (56 * LocalSettings.current.iconScale).dp
    val iconPx = with(LocalDensity.current) { iconSize.toPx() }
    Box(
        modifier.bouncyClickable(
            { r -> onLaunch(app, Rect(r.center.x - iconPx / 2f, r.center.y - iconPx / 2f, r.center.x + iconPx / 2f, r.center.y + iconPx / 2f)) },
            { r -> onMenu(app, r) },
        ),
        contentAlignment = Alignment.Center,
    ) { AppIconImage(app, iconSize) }
}

/** Личные / рабочие приложения — соединённая группа кнопок M3 Expressive. */
@Composable
private fun ProfileTabs(work: Boolean, onChange: (Boolean) -> Unit) {
    val ac = accentColors()
    val haptics = LocalHaptics.current
    val colors = androidx.compose.material3.ToggleButtonDefaults.colors(
        containerColor = ac.segment,
        contentColor = ac.onSegment,
        checkedContainerColor = ac.pill,
        checkedContentColor = ac.onPill,
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        listOf(false to R.string.profile_personal, true to R.string.profile_work).forEachIndexed { i, (isWork, label) ->
            androidx.compose.material3.ToggleButton(
                checked = work == isWork,
                onCheckedChange = { haptics.play(HapticKind.TICK, HapticGroup.TOUCH); onChange(isWork) },
                shapes = if (i == 0) androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                else androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes(),
                colors = colors,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
            ) { Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        }
    }
}

@Composable
private fun PickedBar(count: Int, onAdd: () -> Unit, onClear: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GlassPanel(
        shape = RoundedCornerShape(36.dp),
        color = cs.surfaceContainerHighest,
        glassAlpha = 0.74f,
        overBlur = true,
        elevation = 8.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().accentGlow(RoundedCornerShape(36.dp))
            // Касания по панели не должны доходить до значков под ней
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onClear, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Close, stringResource(R.string.selection_clear)) }
            Text(
                stringResource(R.string.selection_count, count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Button(onClick = onAdd, shapes = ButtonDefaults.shapes(), modifier = with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }) {
                Icon(Icons.Rounded.Home, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.menu_add_home), maxLines = 1)
            }
        }
    }
}

/** Отметка на значке в режиме выбора: приложение уменьшается, в углу появляется галочка. */
@Composable
private fun PickedIcon(app: AppInfo, size: androidx.compose.ui.unit.Dp) {
    val on = app.key in LocalPicked.current
    val cs = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val scale by animateFloatAsState(if (on) 0.82f else 1f, motion.fastSpatialSpec(), label = "pickScale")
    Box {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { AppIconImage(app, size) }
        AnimatedVisibility(
            on,
            Modifier.align(Alignment.TopEnd),
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun goSearch(
    query: String, results: List<AppInfo>, vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect?) -> Unit, actions: HomeActions, webUrl: String,
) {
    // Настройка «запуск по клавише Поиск» отключает запуск совсем, а не только при неполном совпадении
    val launch = vm.settings.value.autoLaunchSingle && results.isNotEmpty() &&
        (results.size == 1 || results.first().sortKey.startsWith(query.trim().lowercase()))
    if (launch) onLaunch(results.first(), null) else actions.openUrl(webUrl)
}

@Composable
private fun DrawerHeader(count: Int) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.all_apps),
            Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMediumEmphasized,
        )
        ThemedSurface(CircleShape, cs.primaryContainer, contentColor = cs.onPrimaryContainer, overBlur = true) {
            Text(count.toString(), Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQuery: (String) -> Unit,
    focus: FocusRequester,
    overflow: Boolean,
    setOverflow: (Boolean) -> Unit,
    vm: LauncherViewModel,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    onGo: () -> Unit,
    /** Expressive: светлая строка в тёмном блоке, справа — группа кнопок (голос и меню) тёмной «таблеткой». */
    accent: Boolean = false,
) {
    val settings = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = SearchEngines[settings.searchEngine.coerceIn(SearchEngines.indices)]
    val ac = accentColors()
    val onField = if (accent) ac.onField else cs.onSurface
    val hintColor = if (accent) ac.onField.copy(alpha = 0.8f) else cs.onSurfaceVariant
    val actionTint = if (accent) ac.onPill else cs.onSurfaceVariant
    // Поисковая строка M3: «таблетка» 56 dp, значок поиска без подложки, справа — обычная кнопка-значок
    Row(
        (if (accent) modifier else modifier.padding(horizontal = 16.dp, vertical = 10.dp)).fillMaxWidth().height(56.dp)
            // Liquid Glass: стеклянная строка поиска поверх уже размытого фона меню (тон и кромка с бликами)
            .clip(CircleShape)
            .then(if (accent) Modifier.background(ac.field) else Modifier.background(panelColor(cs.surfaceContainerHigh)).glassEdge(CircleShape))
            .padding(start = 16.dp, end = if (accent) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (accent) 12.dp else 16.dp),
    ) {
        if (accent && settings.searchGoogle) com.example.expressivelauncher.ui.home.EngineMark(engine, Modifier.size(26.dp), com.example.expressivelauncher.ui.home.themedGlyphs(ac.field))
        else Icon(Icons.Rounded.Search, null, tint = onField)
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = onField),
                cursorBrush = SolidColor(if (accent) onField else cs.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGo() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(
                        if (accent && settings.searchGoogle) stringResource(R.string.drawer_search_engine, engine.name) else stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge, color = hintColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    inner()
                },
            )
        }
        Row(
            if (accent) Modifier.height(44.dp).clip(CircleShape).background(ac.pill) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
        if (accent && query.isEmpty()) {
            IconButton(
                onClick = { com.example.expressivelauncher.ui.home.SearchLaunch.run(context, com.example.expressivelauncher.ui.home.SearchTool.VOICE) {} },
                shapes = IconButtonDefaults.shapes(),
            ) { Icon(Icons.Rounded.Mic, stringResource(R.string.sb_voice), tint = actionTint) }
        }
        Box {
            IconButton(onClick = { if (query.isNotEmpty()) onQuery("") else setOverflow(true) }, shapes = IconButtonDefaults.shapes()) {
                Crossfade(query.isNotEmpty(), label = "fieldAction") { typing ->
                    Icon(if (typing) Icons.Rounded.Close else Icons.Rounded.MoreVert, null, tint = actionTint)
                }
            }
            DropdownMenu(expanded = overflow, onDismissRequest = { setOverflow(false) }, shape = MaterialTheme.shapes.large) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (settings.drawerStyle == DrawerStyle.GRID) R.string.drawer_as_list else R.string.drawer_as_grid)) },
                    onClick = {
                        setOverflow(false)
                        vm.updateSettings { it.copy(drawerStyle = if (it.drawerStyle == DrawerStyle.GRID) DrawerStyle.LIST else DrawerStyle.GRID) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (settings.drawerSort == DrawerSort.ALPHABETICAL) R.string.sort_recent else R.string.sort_alpha)) },
                    onClick = {
                        setOverflow(false)
                        vm.updateSettings { it.copy(drawerSort = if (it.drawerSort == DrawerSort.ALPHABETICAL) DrawerSort.RECENT else DrawerSort.ALPHABETICAL) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_settings)) },
                    onClick = { setOverflow(false); actions.openSettings() },
                )
            }
        }
        }
    }
}

// ---------- Результаты поиска ----------

@Composable
private fun SearchResults(
    results: List<AppInfo>,
    query: String,
    engineName: String,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    onWeb: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (results.isNotEmpty()) item(key = "apps") {
            ThemedSurface(MaterialTheme.shapes.extraLarge, cs.surfaceContainerHigh, overBlur = true, modifier = Modifier.animateItem().animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())) {
                Column(Modifier.padding(8.dp)) { for (a in results) key(a.key) { AppRow(a, onLaunch, onMenu) } }
            }
        }
        item(key = "web") {
            ThemedSurface(MaterialTheme.shapes.extraLarge, cs.tertiaryContainer, overBlur = true, modifier = Modifier.animateItem()) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onWeb).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BlobBadge(CircleShape, cs.tertiary, 48.dp) { Icon(Icons.Rounded.Public, null, tint = cs.onTertiary) }
                    Text(
                        stringResource(R.string.search_web, query.trim(), engineName),
                        style = MaterialTheme.typography.titleMedium, color = cs.onTertiaryContainer, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------- Все приложения ----------

/** Все приложения одной сеткой без разделов; порядок по буквам, а сами буквы остаются на полосе быстрой прокрутки. */
@Composable
private fun AppsContent(apps: List<AppInfo>, drawer: DrawerState, onLaunch: (AppInfo, Rect?) -> Unit, onMenu: (AppInfo, Rect) -> Unit) {
    val settings = LocalSettings.current
    val haptics = LocalHaptics.current
    val alphabetical = settings.drawerSort == DrawerSort.ALPHABETICAL
    // Сначала буква: название вроде «(Заметки)» иначе встало бы вне своей буквы, и полоса вела бы не туда
    val list = remember(apps, alphabetical) {
        if (alphabetical) apps.sortedWith(compareBy({ it.letter }, { it.sortKey })) else apps
    }
    val letters = remember(list, alphabetical) { if (alphabetical) list.map { it.letter }.distinct() else emptyList() }
    val isGrid = settings.drawerStyle == DrawerStyle.GRID
    val columns = if (isGrid) settings.drawerColumns else 1
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) {
        snapshotFlow { drawer.progress <= 0.001f }.collect { closed -> if (closed) state.scrollToItem(0) }
    }
    var active by remember { mutableStateOf<Char?>(null) }
    var bubble by remember { mutableStateOf(' ') }
    val current by remember(list) { derivedStateOf { list.getOrNull(state.firstVisibleItemIndex)?.letter } }
    val showScroller = settings.fastScroller && letters.size > 1
    val accent = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.accentChrome

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            state = state,
            // Под панелью отмеченных приложений последний ряд не должен прятаться
            contentPadding = PaddingValues(start = 12.dp, end = if (showScroller) (if (accent) 24.dp else 44.dp) else 12.dp, top = 4.dp, bottom = if (LocalPicked.current.isNotEmpty()) 100.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(list, key = { _, app -> app.key }) { i, app ->
                // Ряды догоняют шторку друг за другом: каскад идёт за пальцем. В открытом меню расчёта нет совсем
                val motion = Modifier.animateItem().graphicsLayer {
                    val rest = 1f - drawer.progress
                    if (rest <= 0f) return@graphicsLayer
                    val depth = ((i - state.firstVisibleItemIndex) / columns).coerceIn(0, 6) + 1
                    translationY = rest * depth * 56.dp.toPx()
                }
                if (isGrid) AppTile(app, onLaunch, onMenu, motion) else AppRow(app, onLaunch, onMenu, motion)
            }
        }
        if (showScroller && accent) {
            AccentScroller(
                letters, active, state, list.size, Modifier.align(Alignment.CenterEnd),
                onLetter = { c ->
                    if (c != active) {
                        active = c
                        haptics.play(HapticKind.TICK, HapticGroup.DRAWER)
                        val idx = list.indexOfFirst { it.letter == c }
                        if (idx >= 0) scope.launch { state.scrollToItem(idx) }
                    }
                },
                onEnd = { active = null },
            )
        } else if (showScroller) {
            FastScroller(
                letters, active, current, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                onLetter = { c ->
                    if (c != active) {
                        active = c
                        bubble = c
                        haptics.play(HapticKind.TICK, HapticGroup.DRAWER)
                        val idx = list.indexOfFirst { it.letter == c }
                        if (idx >= 0) scope.launch { state.scrollToItem(idx) }
                    }
                },
                onEnd = { active = null },
            )
            val motion = MaterialTheme.motionScheme
            val bubbleSpin by animateFloatAsState(letters.indexOf(bubble) * 40f, motion.defaultSpatialSpec(), label = "bubbleSpin")
            AnimatedVisibility(
                active != null,
                Modifier.align(Alignment.Center),
                enter = scaleIn(motion.defaultSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
            ) {
                BlobBadge(Blob.COOKIE_12.shape(), MaterialTheme.colorScheme.primary, 104.dp, rotation = { bubbleSpin }) {
                    Text(bubble.toString(), style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: AppInfo, onLaunch: (AppInfo, Rect?) -> Unit, onMenu: (AppInfo, Rect) -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalSettings.current
    val iconSize = (56 * settings.iconScale).dp
    val density = LocalDensity.current
    val iconPx = with(density) { iconSize.toPx() }
    val topPx = with(density) { 10.dp.toPx() }
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .bouncyClickable(
                { r -> onLaunch(app, Rect(r.center.x - iconPx / 2f, r.top + topPx, r.center.x + iconPx / 2f, r.top + topPx + iconPx)) },
                { r -> onMenu(app, r) },
            )
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PickedIcon(app, iconSize)
        if (settings.drawerLabels) if (com.example.expressivelauncher.data.AppStyles.of(app.key)?.hideLabel != true) AppLabel(app.displayLabel, Modifier.padding(top = 6.dp), color = LocalDrawerLabel.current)
    }
}

@Composable
private fun AppRow(app: AppInfo, onLaunch: (AppInfo, Rect?) -> Unit, onMenu: (AppInfo, Rect) -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalSettings.current
    val iconSize = (46 * settings.iconScale).dp
    val density = LocalDensity.current
    val iconPx = with(density) { iconSize.toPx() }
    val startPx = with(density) { 12.dp.toPx() }
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .bouncyClickable(
                { r -> onLaunch(app, Rect(r.left + startPx, r.center.y - iconPx / 2f, r.left + startPx + iconPx, r.center.y + iconPx / 2f)) },
                { r -> onMenu(app, r) },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickedIcon(app, iconSize)
        Spacer(Modifier.width(16.dp))
        Text(app.displayLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Вертикальный контейнер с буквами: касание и протягивание прокручивают к секции, активная буква подсвечена. */
@Composable
private fun FastScroller(letters: List<Char>, active: Char?, current: Char?, modifier: Modifier, onLetter: (Char) -> Unit, onEnd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var heightPx by remember { mutableStateOf(1f) }
    fun at(y: Float) {
        val i = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        onLetter(letters[i])
    }
    GlassPanel(
        shape = CircleShape,
        color = cs.surfaceContainerHighest,
        glassAlpha = 0.72f,
        overBlur = true,
        solidAlpha = 0.9f,
        modifier = modifier.fillMaxHeight(0.78f).width(32.dp),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(letters) {
                // Своя обработка вместо жеста протягивания: тот срабатывал только после сдвига, и касание по букве ничего не делало
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    at(down.position.y)
                    try {
                        while (true) {
                            val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            c.consume()
                            at(c.position.y)
                        }
                    } finally {
                        onEnd()
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (c in letters) {
            val on = c == active
            val motion = MaterialTheme.motionScheme
            val grow by animateFloatAsState(if (on) 1.35f else 1f, motion.fastSpatialSpec(), label = "letter")
            val letterBg by animateColorAsState(if (on) cs.primary else Color.Transparent, motion.fastEffectsSpec(), label = "letterBg")
            Box(
                // Букв может быть больше, чем помещается по 24dp (латиница и кириллица): строки сжимаются
                Modifier.weight(1f, fill = false).widthIn(max = 24.dp).heightIn(max = 24.dp).aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .graphicsLayer { scaleX = grow; scaleY = grow; translationX = (grow - 1f) * -32.dp.toPx() }
                    .clip(CircleShape).background(letterBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    c.toString(), style = MaterialTheme.typography.labelSmall,
                    // Заголовков больше нет: полоса показывает, на какой букве сейчас список
                    fontWeight = if (on || c == current) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (on) cs.onPrimary else if (c == current) cs.primary else cs.onSurfaceVariant,
                )
            }
        }
    }
    }
}

/**
 * Expressive: тонкий ползунок у края, как в макете. Показывает положение в списке; при протягивании рядом
 * с ним появляется «капля» с буквой, к которой ведёт палец.
 */
@Composable
private fun AccentScroller(
    letters: List<Char>, active: Char?, state: LazyGridState, total: Int, modifier: Modifier,
    onLetter: (Char) -> Unit, onEnd: () -> Unit,
) {
    val ac = accentColors()
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    var heightPx by remember { mutableStateOf(1f) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    val thumbH = 64.dp
    val thumbPx = with(density) { thumbH.toPx() }
    val listFraction by remember(total) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo.size
            (state.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    val fraction = touchY?.let { (it / heightPx).coerceIn(0f, 1f) } ?: listFraction
    val width by animateFloatAsState(if (touchY != null) 12f else 8f, motion.fastSpatialSpec(), label = "thumbW")
    fun at(y: Float) {
        touchY = y
        val i = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        onLetter(letters[i])
    }
    Box(modifier.fillMaxHeight(0.86f).width(112.dp)) {
        // Полоса касания — только у самого края, остальное место лишь под «каплю»
        Box(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(letters) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        at(down.position.y)
                        try {
                            while (true) {
                                val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                c.consume()
                                at(c.position.y)
                            }
                        } finally {
                            touchY = null
                            onEnd()
                        }
                    }
                },
        ) {
            Box(
                Modifier.align(Alignment.TopCenter)
                    .graphicsLayer { translationY = fraction * (heightPx - thumbPx) }
                    .width(width.dp).height(thumbH).clip(CircleShape).background(ac.thumb),
            )
        }
        AnimatedVisibility(
            active != null,
            Modifier.align(Alignment.TopEnd)
                .graphicsLayer { translationY = fraction * (heightPx - thumbPx) + thumbPx / 2f - 32.dp.toPx() }
                .padding(end = 32.dp),
            enter = scaleIn(motion.fastSpatialSpec(), transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastSpatialSpec(), transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)) + fadeOut(motion.fastEffectsSpec()),
        ) {
            var last by remember { mutableStateOf(' ') }
            if (active != null) last = active
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                // Капля: круг с одним острым углом, повёрнутым к ползунку
                Box(
                    Modifier.size(52.dp).graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(topStartPercent = 50, topEndPercent = 12, bottomEndPercent = 50, bottomStartPercent = 50))
                        .background(ac.thumb),
                )
                Text(last.toString(), style = MaterialTheme.typography.headlineSmallEmphasized, color = ac.onThumb)
            }
        }
    }
}
