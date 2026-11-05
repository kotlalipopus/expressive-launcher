@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.drawer

import com.example.expressivelauncher.data.displayLabel
import com.example.expressivelauncher.ui.settings.panelColor
import com.example.expressivelauncher.ui.components.glassEdge
import com.example.expressivelauncher.ui.components.ThemedSurface
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
    val launchApp: (AppInfo, Rect?) -> Unit = { app, rect ->
        if (picking) onPick(app) else {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            onLaunch(app, rect)
        }
    }
    val holdApp: (AppInfo, Rect) -> Unit = { app, rect -> if (picking) onPick(app) else onMenu(app, rect) }
    // Под меню приложения список размывается, как стол под шторкой
    val glass = glassEnabled()
    val menuBlur by animateFloatAsState(if (menuOpen && glass) 1f else 0f, motion.defaultEffectsSpec(), label = "drawerMenuBlur")

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

    val surface = MaterialTheme.colorScheme.surfaceContainer
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
                        settings.blurBackground && !LocalFx.current.lite -> minOf(settings.drawerOpacity, 0.72f)
                        else -> settings.drawerOpacity
                    },
                ),
            )
            .nestedScroll(connection),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface, LocalPicked provides picked) {
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
                            .clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                AnimatedVisibility(
                    query.isBlank(),
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                    exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
                ) { DrawerHeader(apps.size) }
            }
            if (settings.searchPosition == SearchPosition.TOP) {
                SearchField(
                    query, { query = it }, focus, overflow, { overflow = it }, vm, actions,
                    onGo = { goSearch(query, results, vm, launchApp, actions, engine.url(query)) },
                )
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
                    AppsContent(apps, drawer, launchApp, holdApp)
                }
            }
            if (settings.searchPosition == SearchPosition.BOTTOM) {
                SearchField(
                    query, { query = it }, focus, overflow, { overflow = it }, vm, actions,
                    modifier = Modifier.imePadding().windowInsetsPadding(WindowInsets.navigationBars),
                    onGo = { goSearch(query, results, vm, launchApp, actions, engine.url(query)) },
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

@Composable
private fun PickedBar(count: Int, onAdd: () -> Unit, onClear: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GlassPanel(
        shape = RoundedCornerShape(36.dp),
        color = cs.surfaceContainerHighest,
        glassAlpha = 0.74f,
        overBlur = true,
        elevation = 8.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
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
) {
    val settings = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    // Поисковая строка M3: «таблетка» 56 dp, значок поиска без подложки, справа — обычная кнопка-значок
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(56.dp)
            // Liquid Glass: стеклянная строка поиска поверх уже размытого фона меню (тон и кромка с бликами)
            .clip(CircleShape).background(panelColor(cs.surfaceContainerHigh)).glassEdge(CircleShape).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Rounded.Search, null, tint = cs.onSurface)
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGo() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, maxLines = 1)
                    inner()
                },
            )
        }
        Box {
            IconButton(onClick = { if (query.isNotEmpty()) onQuery("") else setOverflow(true) }, shapes = IconButtonDefaults.shapes()) {
                Crossfade(query.isNotEmpty(), label = "fieldAction") { typing ->
                    Icon(if (typing) Icons.Rounded.Close else Icons.Rounded.MoreVert, null, tint = cs.onSurfaceVariant)
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

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            state = state,
            // Под панелью отмеченных приложений последний ряд не должен прятаться
            contentPadding = PaddingValues(start = 12.dp, end = if (showScroller) 44.dp else 12.dp, top = 4.dp, bottom = if (LocalPicked.current.isNotEmpty()) 100.dp else 16.dp),
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
        if (showScroller) {
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
        if (settings.drawerLabels) if (com.example.expressivelauncher.data.AppStyles.of(app.key)?.hideLabel != true) AppLabel(app.displayLabel, Modifier.padding(top = 6.dp))
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
