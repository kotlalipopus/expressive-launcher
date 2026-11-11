@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.theme.accentColors
import com.example.expressivelauncher.ui.components.accentGlow
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.toPath
import io.github.fletchmckee.liquid.liquefiable
import com.example.expressivelauncher.data.displayLabel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.TransformOrigin
import com.example.expressivelauncher.ui.components.WakeWave
import androidx.compose.ui.graphics.toArgb
import com.example.expressivelauncher.ui.components.bouncyClickable
import com.example.expressivelauncher.ui.components.homeItemMotion
import com.example.expressivelauncher.ui.components.staggerIn
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect as AndroidRect
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Check
import com.example.expressivelauncher.ui.settings.Blob
import com.example.expressivelauncher.ui.settings.BlobBadge
import com.example.expressivelauncher.ui.settings.shape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.ClockStyle
import com.example.expressivelauncher.data.GestureAction
import com.example.expressivelauncher.data.GridLogic
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.ItemType
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.LocalHostView
import com.example.expressivelauncher.ui.components.HapticGroup
import com.example.expressivelauncher.ui.components.HapticKind
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.AppLabel
import com.example.expressivelauncher.ui.components.FrostedGlass
import com.example.expressivelauncher.ui.components.GlassPanel
import com.example.expressivelauncher.ui.components.Backdrop
import com.example.expressivelauncher.ui.components.glassEdge
import com.example.expressivelauncher.ui.components.LocalBackdrop
import com.example.expressivelauncher.ui.components.backdropSource
import com.example.expressivelauncher.ui.components.rememberBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.widthIn
import com.example.expressivelauncher.ui.components.toComposeShape
import com.example.expressivelauncher.ui.drawer.AppDrawer
import com.example.expressivelauncher.data.ClockAlign
import com.example.expressivelauncher.ui.LocalClockStyle
import androidx.compose.ui.layout.onSizeChanged
import com.example.expressivelauncher.data.PageTransition
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.components.animatedCell
import com.example.expressivelauncher.ui.components.iconLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(vm: LauncherViewModel, actions: HomeActions) {
    val settings = LocalSettings.current
    val repo = LocalRepo.current
    val hostView = LocalHostView.current
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val items by vm.items.collectAsState()
    val apps by vm.allApps.collectAsState()
    val appMap = remember(apps) { apps.associateBy { it.key } }
    val liveSettings by rememberUpdatedState(settings)
    val liveApps by rememberUpdatedState(appMap)

    val drawer = remember { DrawerState(scope) }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val drag = remember { DragState() }
    val selection = remember { SelectionState() }
    LaunchedEffect(items) { selection.retain(items.mapTo(HashSet()) { it.id }) }
    val geometry = remember { HomeGeometry() }
    val pressedState = remember { PressedState() }
    val wakeCount = actions.wakeSignal
    val wave = remember { WakeWave() }
    val portal = remember { LaunchPortal(scope) }
    val fx = LocalFx.current
    // Прогреваем морфинги заранее, чтобы первое нажатие на значок не тратило кадр на их построение
    LaunchedEffect(settings.iconShape) {
        withContext(Dispatchers.Default) {
            ShapeMorphs.window(settings.iconShape)
            ShapeMorphs.press(settings.iconShape)
        }
    }
    // Цвет стартового окна приложений со стола, из дока и папок читаем заранее: на тапе нет межпроцессных вызовов
    val night = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    LaunchedEffect(items, appMap, night) {
        withContext(Dispatchers.IO) {
            // Даём странице сначала собраться и загрузить значки: пачка межпроцессных вызовов
            // в это же время заметно отодвигала первый кадр
            delay(1200)
            for (item in items) {
                for (key in listOfNotNull(item.appKey) + item.children) appMap[key]?.let(repo::warmSplashColor)
                kotlinx.coroutines.yield()
            }
        }
    }
    // Когда значок возвращается из приложения, волна не нужна: стол должен стоять ровно, значок садится на своё место.
    // Взводим прямо в композиции: иначе один кадр значки успели бы показаться на месте и дёрнулись
    val runWave = remember(wakeCount) {
        val on = settings.wakeWave && !fx.lite && !portal.active
        if (on) wave.arm()
        on
    }
    LaunchedEffect(wakeCount) {
        if (!runWave) return@LaunchedEffect
        androidx.compose.animation.core.animate(0f, 1f, animationSpec = androidx.compose.animation.core.tween((WakeWave.TOTAL_MS / fx.speed).toInt(), easing = androidx.compose.animation.core.LinearEasing)) { v, _ -> wave.time = v }
    }
    val colorScheme = MaterialTheme.colorScheme
    val themeSpec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current

    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    var drawerMenu by remember { mutableStateOf<DrawerMenuTarget?>(null) }
    // Приложения, отмеченные в шторке, чтобы вынести их на стол разом
    var drawerPicked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var showHomeMenu by remember { mutableStateOf<Offset?>(null) }
    var showWidgets by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    /** Виджет, вокруг которого показана рамка изменения размера. */
    var resizeId by remember { mutableStateOf<String?>(null) }
    var searchRequest by remember { mutableIntStateOf(0) }

    // Стол размывается только под открытой папкой; меню по долгому нажатию открываются без размытия, чтобы не тормозить
    val accentBlurAll = themeSpec.accentChrome && settings.blurEverywhere
    val overlayTarget = when {
        openFolderId != null -> 1f
        themeSpec.accentChrome && (showWidgets || showWallpaperSheet) -> 1f
        accentBlurAll && showHomeMenu != null -> 1f
        else -> 0f
    }
    val overlayBlur by animateFloatAsState(overlayTarget, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "overlayBlur")
    val blurOn = settings.blurBackground && settings.blurStrength > 0f
    // Liquid Glass: стол под папкой и меню не размывается — стекло само преломляет то, что под ним, и значки
    // должны быть видны. Полноэкранное размытие с анимируемым радиусом давало пики по 15–50 мс на кадр
    val inWindowBlur = blurOn && fx.blur && android.os.Build.VERSION.SDK_INT >= 31 && !themeSpec.glass
    val homeBackdrop = rememberBackdrop()
    val drawerBackdrop = rememberBackdrop()

    val pageCount = GridLogic.pageCount(items)
    // Временная страница на время перетаскивания: состояние читается внутри лямбды,
    // иначе начало переноса перекомпоновывало бы весь экран
    val pagerState = rememberPagerState { pageCount + if (drag.item != null) 1 else 0 }

    fun openDrawer(search: Boolean = false) {
        if (search) searchRequest++
        drawer.animateTo(1f, spatial)
    }
    // Пружины перехода следуют настройкам; уже идущая анимация подхватит новые на следующем развороте
    portal.specs = launchSpecs(
        expressive = settings.motion == com.example.expressivelauncher.data.MotionStyle.EXPRESSIVE,
        fx = fx, closeScale = settings.closeAnimScale, slow = settings.debugSlowLaunch,
    )
    // Выход из композиции (смена конфигурации, уничтожение активности): ни одного скрытого значка не остаётся
    DisposableEffect(portal) { onDispose { portal.reset() } }
    // Где сейчас значок источника. Читается на каждом кадре: окно сворачивается в актуальное место значка,
    // даже если сменилась страница или экран повернули. null — значка не видно, окно уйдёт в центр и растворится
    portal.locate = { source, from ->
        fun visible(item: HomeItem): androidx.compose.ui.geometry.Rect? {
            if (drawer.progress > 0.5f) return null
            val s = geometry.settings
            val r = geometry.iconBounds(item, iconPxFor(item, geometry, density, s), labelPx(density, s))
            if (item.inDock) return r.takeIf { s.showDock }
            val shift = item.page - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
            if (abs(shift) >= 1f) return null
            // Сетка под окном чуть уменьшена (launchRecede): окно садится туда, где значок нарисован сейчас
            val k = 1f - RECEDE_SCALE * portal.desktopProgress()
            val c = geometry.grid.center
            val moved = r.translate(shift * geometry.grid.width, 0f)
            return androidx.compose.ui.geometry.Rect(
                c.x + (moved.left - c.x) * k, c.y + (moved.top - c.y) * k,
                c.x + (moved.right - c.x) * k, c.y + (moved.bottom - c.y) * k,
            )
        }
        when (source) {
            is LaunchSource.Home -> geometry.items.firstOrNull { it.id == source.itemId }?.let(::visible)?.let { LaunchAnchor(it) }
            // Из папки окно возвращается в её значок и растворяется в миниатюре
            is LaunchSource.Folder -> geometry.items.firstOrNull { it.id == source.folderId }?.let(::visible)
                ?.let { LaunchAnchor(androidx.compose.ui.geometry.Rect(it.center, it.width * 0.2f), fade = true) }
            LaunchSource.Free -> if (drawer.progress > 0.01f) LaunchAnchor(from, visibility = ((drawer.progress - 0.5f) * 2f).coerceIn(0f, 1f)) else null
        }
    }

    fun launch(app: AppInfo, bounds: androidx.compose.ui.geometry.Rect?, source: LaunchSource = LaunchSource.Free) {
        haptics.play(HapticKind.CLICK, HapticGroup.TOUCH)
        val view = hostView
        val w = view?.width ?: 0
        val h = view?.height ?: 0
        val sizePx = with(density) { 56.dp.toPx() }
        val from = bounds?.takeIf { it.width > 0f } ?: androidx.compose.ui.geometry.Rect(
            androidx.compose.ui.geometry.Offset(w / 2f, h * 0.8f), sizePx / 2f,
        )
        val rect = AndroidRect(from.left.toInt(), from.top.toInt(), from.right.toInt(), from.bottom.toInt())
        // Тема Lite и выключенная анимация: окно раскрывает система, просто и без своих слоёв поверх стола
        if (!settings.launchAnimation || !themeSpec.portalLaunch) {
            repo.launch(app, view, rect)
            return
        }
        portal.open(
            appKey = app.key,
            source = source,
            from = from,
            look = iconLook(repo.cachedIcon(app), settings, colorScheme, glass = themeSpec.glass),
            shape = com.example.expressivelauncher.data.AppStyles.of(app.key)?.shape ?: settings.iconShape,
            endColor = repo.cachedSplashColor(app)?.let { Color(it) }
                ?: null.also { scope.launch(Dispatchers.IO) { repo.warmSplashColor(app) } },
            pauses = { actions.pauseCount },
            // Вызывается, когда карточка-портал почти заполнила экран; окно приложения проявится поверх неё
            launch = { repo.launch(app, view, rect, portal = true) },
        )
    }
    val resumeSignal = actions.resumeSignal
    LaunchedEffect(resumeSignal) {
        if (resumeSignal > 0) portal.onResumed(actions.pauseCount)
    }

    // Соседи расступаются, если палец задержался над занятой клеткой: при быстром проносе над сеткой значки стоят на месте
    val draggedItem = drag.item
    // Палец перешёл на другую клетку или на кнопку сверху
    LaunchedEffect(draggedItem?.id) {
        val dragged = draggedItem ?: return@LaunchedEffect
        snapshotFlow {
            when (val t = geometry.dropTarget(drag.pointer, dragged)) {
                is DropTarget.Action -> t.action
                is DropTarget.Cell -> listOf(t.page, t.x, t.y, t.merge)
                null -> null
            }
        }.distinctUntilChanged().drop(1).collect { key ->
            if (key != null) haptics.play(if (key is DragAction) HapticKind.CLICK else HapticKind.TICK, HapticGroup.DRAG)
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect { haptics.play(HapticKind.TICK, HapticGroup.PAGES) }
    }
    LaunchedEffect(Unit) {
        // С запасом от краёв: пружина у края не должна щёлкать несколько раз
        var open = false
        snapshotFlow { drawer.progress }.collect { p ->
            if (open != (p > 0.5f) && (p > 0.9f || p < 0.1f)) {
                open = p > 0.5f
                haptics.play(if (open) HapticKind.CLICK else HapticKind.TICK, HapticGroup.DRAWER)
            }
        }
    }
    LaunchedEffect(Unit) {
        // Значок сел на своё место после закрытия приложения
        snapshotFlow { portal.active }.drop(1).collect { if (!it) haptics.play(HapticKind.TICK, HapticGroup.TOUCH) }
    }
    LaunchedEffect(draggedItem?.id) {
        if (draggedItem == null) {
            // Раскладка обновится на следующем кадре: снимаем предпросмотр, когда значки уже на новых местах
            delay(160)
            drag.preview = emptyMap()
            return@LaunchedEffect
        }
        if (drag.multi) {
            // У группы соседи не расступаются заранее: все выбранные значки летят вместе
            drag.preview = emptyMap()
            return@LaunchedEffect
        }
        snapshotFlow {
            (geometry.dropTarget(drag.pointer, draggedItem) as? DropTarget.Cell)?.let { drag.effective(it, geometry.items) }
                ?.let { Triple(it.page, it.x, it.y) to it.merge }
        }.distinctUntilChanged().collectLatest { key ->
            if (key == null) {
                delay(90)
                drag.preview = emptyMap()
                return@collectLatest
            }
            val (cell, merge) = key
            val current = geometry.items
            val occupant = GridLogic.itemAt(current, cell.first, cell.second, cell.third, draggedItem.id)
            if (merge && draggedItem.type == ItemType.APP && (occupant?.type == ItemType.APP || occupant?.type == ItemType.FOLDER)) {
                drag.preview = emptyMap() // здесь будет папка: сосед стоит на месте
                return@collectLatest
            }
            delay(120)
            val s = geometry.settings
            val dock = cell.first == HomeItem.DOCK
            val next = GridLogic.reflow(
                current, draggedItem.id, cell.first, cell.second, cell.third, draggedItem.w, draggedItem.h,
                if (dock) s.dockCount else s.columns, if (dock) 1 else s.rows,
            ).orEmpty()
            if (next.isNotEmpty() && next != drag.preview) haptics.play(HapticKind.TICK, HapticGroup.DRAG)
            drag.preview = next
        }
    }
    LaunchedEffect(drag.landing) {
        if (drag.landing != null) { delay(400); drag.landing = null }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenSize = androidx.compose.ui.geometry.Size(with(density) { maxWidth.toPx() }, screenHeightPx)
        val liveHeight by rememberUpdatedState(screenHeightPx)
        val fxState by rememberUpdatedState(fx)
        val topInsetPx = WindowInsets.systemBars.getTop(density).toFloat()
        val zoneH = with(density) { 60.dp.toPx() }
        val zoneW = with(density) { maxWidth.toPx() } / 3f
        geometry.items = items
        geometry.settings = settings
        geometry.page = pagerState.currentPage
        // Список зон не пересоздаётся на каждую композицию: он меняется только с размером экрана или вырезом
        geometry.zones = remember(maxWidth, zoneW, topInsetPx) {
            DRAG_ACTIONS.mapIndexed { i, a ->
                a to androidx.compose.ui.geometry.Rect(i * zoneW, topInsetPx, (i + 1) * zoneW, topInsetPx + zoneH)
            }
        }

        // ---------- Рабочий стол ----------
        // Liquid Glass: под поиском и меню приложений стол не исчезает, а сильно размывается — одним слоем целиком,
        // уровни размытия кэшируются, чтобы не создавать эффект на каждом кадре открытия
        // Отключено: размытие всего стола на каждом кадре открытия меню приложений было главным источником рывков.
        // Стол растворяется, а под меню остаются размытые обои (готовая копия, без размытия на кадре)
        val blurHomeUnderDrawer = false
        val homeBlurLevels = remember { arrayOfNulls<androidx.compose.ui.graphics.RenderEffect>(HOME_BLUR_LEVELS + 1) }
        val homeBlurMax = with(density) { (44.dp * settings.blurStrength).toPx() }
        LaunchedEffect(homeBlurMax) { homeBlurLevels.fill(null) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.wallpaperDim))
                .graphicsLayer {
                    val p = drawer.progress
                    // Стол отступает вглубь, когда открывается меню. Уход стола под окно приложения задают сетка и док
                    val scale = 1f - 0.06f * p
                    scaleX = scale
                    scaleY = scale
                    if (blurHomeUnderDrawer) {
                        val level = (p * HOME_BLUR_LEVELS).roundToInt().coerceIn(0, HOME_BLUR_LEVELS)
                        renderEffect = if (level == 0) null else homeBlurLevels[level] ?: androidx.compose.ui.graphics.BlurEffect(
                            homeBlurMax * level / HOME_BLUR_LEVELS, homeBlurMax * level / HOME_BLUR_LEVELS, androidx.compose.ui.graphics.TileMode.Clamp,
                        ).also { homeBlurLevels[level] = it }
                        alpha = 1f
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto
                    } else {
                        // Под открытой шторкой стол не рисуется совсем: обои за окном размывает система, а размывать
                        // ещё и весь стол на каждом кадре прокрутки списка слишком дорого
                        renderEffect = null
                        alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    }
                }
                // Отдельно отслеживаем только нажатие: значок вдавливается, пока палец на нём и не сдвинулся
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (drawer.progress > 0.02f || menu != null || openFolderId != null || showHomeMenu != null || showWidgets || showWallpaperSheet || AppCustomize.app != null) {
                            return@awaitEachGesture
                        }
                        val hit = geometry.hitTest(down.position)?.takeIf { it.type != ItemType.WIDGET } ?: return@awaitEachGesture
                        pressedState.id = hit.id
                        try {
                            while (true) {
                                val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                            }
                        } finally {
                            pressedState.id = null
                        }
                    }
                }
                // Обработчик живёт долго: без ключей он запускал бы приложения с настройками и цветами первого кадра
                .pointerInput(settings, fx, colorScheme, themeSpec) {
                    var lastEmptyTap = 0L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (drawer.progress > 0.02f || menu != null || openFolderId != null || showHomeMenu != null || showWidgets || showWallpaperSheet || AppCustomize.app != null) {
                            return@awaitEachGesture
                        }
                        val framed = resizeId?.let { id -> geometry.items.firstOrNull { it.id == id } }
                        if (framed != null) {
                            // Ручки лежат на краях рамки: касание рядом с ней достаётся им, мимо — закрывает рамку
                            val zone = geometry.itemBounds(framed).inflate(36.dp.toPx())
                            if (!zone.contains(down.position)) resizeId = null
                            return@awaitEachGesture
                        } else if (resizeId != null) {
                            resizeId = null
                            return@awaitEachGesture
                        }
                        val target = geometry.hitTest(down.position)
                        val onControl = target == null && geometry.isOnControl(down.position)
                        val slop = viewConfiguration.touchSlop

                        // Фаза 1: ждём долгое нажатие, отпускание или сдвиг
                        var outcome = 0 // 0 — долгое нажатие, 1 — тап, 2 — сдвиг
                        var moved = Offset.Zero
                        var released = false
                        var p1Time = down.uptimeMillis
                        var p1Pos = down.position
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val c = ev.changes.firstOrNull { it.id == down.id }
                                if (c == null || ev.changes.size > 1) { outcome = 2; break }
                                moved = c.position - down.position
                                released = !c.pressed
                                p1Time = c.uptimeMillis
                                p1Pos = c.position
                                if (moved.getDistance() > slop) { outcome = 2; break }
                                if (released) { outcome = 1; break }
                            }
                            true
                        }

                        if (outcome == 1) {
                            // Касание, остановившее прокрутку страниц, ничего не запускает; кнопки обрабатывают нажатие сами
                            if (onControl || pagerState.isScrollInProgress) return@awaitEachGesture
                            // В режиме выбора касание только отмечает значки, а не запускает приложение
                            if (selection.active) {
                                if (target == null) selection.clear()
                                else if (target.type == ItemType.APP || target.type == ItemType.FOLDER) {
                                    haptics.play(HapticKind.CLICK, HapticGroup.TOUCH)
                                    selection.toggle(target.id)
                                }
                                return@awaitEachGesture
                            }
                            val now = System.currentTimeMillis()
                            when {
                                target == null -> {
                                    if (now - lastEmptyTap < 320) {
                                        lastEmptyTap = 0
                                        actions.perform(liveSettings.doubleTap, { openDrawer() }, { openDrawer(true) })
                                    } else lastEmptyTap = now
                                }
                                target.type == ItemType.APP -> launch(
                                    liveApps[target.appKey] ?: return@awaitEachGesture,
                                    geometry.iconBounds(target, iconPxFor(target, geometry, density, liveSettings), labelPx(density, liveSettings)),
                                    LaunchSource.Home(target.id),
                                )
                                target.type == ItemType.FOLDER -> { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); openFolderId = target.id }
                                target.type == ItemType.CLOCK -> { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); actions.openClock() }
                                else -> Unit
                            }
                            return@awaitEachGesture
                        }

                        if (outcome == 2) {
                            // Вертикальный свайп обрабатываем сами; горизонтальный отдаём пейджеру
                            val vertical = abs(moved.y) > abs(moved.x) * 1.3f &&
                                target?.type != ItemType.WIDGET && !pagerState.isScrollInProgress
                            if (!vertical) return@awaitEachGesture
                            val up = moved.y < 0
                            val action = if (up) liveSettings.swipeUp else liveSettings.swipeDown
                            val follow = up && action == GestureAction.DRAWER
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            tracker.addPosition(p1Time, p1Pos)
                            var lastY = down.position.y + moved.y
                            var totalY = moved.y
                            if (follow) drawer.dragBy(-moved.y / (liveHeight * 0.8f))
                            var velocity = 0f
                            // Палец мог уже оторваться в первой фазе (события пришли пачкой) — тогда ждать больше нечего
                            if (!released) {
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                    tracker.addPosition(c.uptimeMillis, c.position)
                                    // смещение до точки отрыва тоже считаем: при редких событиях это большая часть жеста
                                    val dy = c.position.y - lastY
                                    lastY = c.position.y
                                    totalY += dy
                                    if (follow) drawer.dragBy(-dy / (liveHeight * 0.8f))
                                    if (!c.pressed) break
                                    c.consume()
                                }
                                velocity = -tracker.calculateVelocity().y / liveHeight
                            } else if (follow) {
                                velocity = if (up) 3f else -3f
                            }
                            if (follow) {
                                drawer.settle(velocity, spatial, openThreshold = 0.3f)
                            } else if (abs(totalY) > slop * 4 && (totalY < 0) == up) {
                                actions.perform(action, { openDrawer() }, { openDrawer(true) })
                            }
                            return@awaitEachGesture
                        }

                        // Долгое нажатие
                        if (onControl) return@awaitEachGesture
                        val selecting = selection.active
                        if (selecting) {
                            if (target == null) {
                                selection.clear()
                                return@awaitEachGesture
                            }
                            if (target.type != ItemType.APP && target.type != ItemType.FOLDER) return@awaitEachGesture
                            if (!selection.contains(target.id)) selection.add(target.id)
                            if (liveSettings.lockLayout) return@awaitEachGesture
                            haptics.play(HapticKind.HEAVY, HapticGroup.TOUCH)
                        } else {
                            haptics.play(HapticKind.HEAVY, HapticGroup.TOUCH)
                            if (target == null) {
                                showHomeMenu = down.position
                                return@awaitEachGesture
                            }
                            val bounds = geometry.itemBounds(target)
                            menu = MenuTarget(target, bounds)
                            if (target.type == ItemType.WIDGET && !liveSettings.lockLayout) resizeId = target.id
                            if (liveSettings.lockLayout) return@awaitEachGesture
                        }
                        val dragTarget = target ?: return@awaitEachGesture
                        var dragging = false
                        var edgeJob: kotlinx.coroutines.Job? = null
                        // Жест может оборваться: значок не должен остаться поднятым
                        try {
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                c.consume()
                                if (!c.pressed) {
                                    if (dragging) {
                                        val raw = geometry.dropTarget(c.position, dragTarget)
                                        // Группу не сливаем с папкой: несколько значков должны просто лечь рядом
                                        val t = if (drag.multi && raw is DropTarget.Cell) raw.copy(merge = false) else raw
                                        haptics.play(HapticKind.DROP, HapticGroup.DRAG)
                                        val group = drag.all
                                        val ids = drag.ids
                                        val groupIds = listOf(dragTarget.id) + drag.extra.map { it.id }
                                        // Значок долетает из точки отпускания, даже если вернулся на старое место
                                        if (!drag.multi && !(t is DropTarget.Action && t.action == DragAction.REMOVE)) {
                                            drag.landing = Landing(dragTarget.id, c.position)
                                        }
                                        when (t) {
                                            is DropTarget.Cell ->
                                                if (drag.multi) vm.dropMany(groupIds, t.page, t.x, t.y)
                                                else vm.drop(dragTarget.id, t.page, t.x, t.y, drag.effective(t, geometry.items).merge)
                                            is DropTarget.Action -> when (t.action) {
                                                DragAction.REMOVE ->
                                                    if (drag.multi) vm.removeItems(ids) else vm.removeItem(dragTarget.id)
                                                DragAction.APP_INFO ->
                                                    liveApps[dragTarget.appKey]?.let { repo.openAppInfo(it, null) }
                                                DragAction.UNINSTALL ->
                                                    if (drag.multi) group.forEach { g ->
                                                        liveApps[g.appKey]?.takeIf { !it.isSystem }?.let { repo.uninstall(it) }
                                                    } else liveApps[dragTarget.appKey]?.takeIf { !it.isSystem }?.let { repo.uninstall(it) }
                                            }
                                            null -> Unit
                                        }
                                        if (selecting) selection.clear()
                                    }
                                    break
                                }
                                if (!dragging && (c.position - down.position).getDistance() > slop * 1.5f) {
                                    dragging = true
                                    menu = null
                                    drag.item = dragTarget
                                    drag.extra = if (selecting) geometry.items.filter {
                                        it.id != dragTarget.id && selection.contains(it.id) &&
                                            (it.type == ItemType.APP || it.type == ItemType.FOLDER)
                                    } else emptyList()
                                    drag.preview = emptyMap()
                                    haptics.play(HapticKind.CLICK, HapticGroup.DRAG)
                                }
                                if (dragging) {
                                    drag.pointer = c.position
                                    // Второй палец во время переноса добавляет ещё один значок к группе
                                    val iconGroup = dragTarget.type == ItemType.APP || dragTarget.type == ItemType.FOLDER
                                    for (ch in ev.changes) {
                                        if (ch.id == down.id || !ch.pressed || ch.previousPressed) continue
                                        val add = geometry.hitTest(ch.position)
                                        if (iconGroup && add != null && (add.type == ItemType.APP || add.type == ItemType.FOLDER) &&
                                            add.id != dragTarget.id && !drag.contains(add.id)
                                        ) {
                                            drag.extra = drag.extra + add
                                            if (selecting) selection.add(add.id)
                                            haptics.play(HapticKind.CLICK, HapticGroup.DRAG)
                                        }
                                    }
                                    val g = geometry.grid
                                    val edge = when {
                                        !g.contains(c.position) -> 0
                                        c.position.x < g.left + 36.dp.toPx() -> -1
                                        c.position.x > g.right - 36.dp.toPx() -> 1
                                        else -> 0
                                    }
                                    if (edge == 0) { edgeJob?.cancel(); edgeJob = null }
                                    else if (edgeJob == null) {
                                        edgeJob = scope.launch {
                                            delay(550)
                                            while (true) {
                                                val next = pagerState.currentPage + edge
                                                if (next in 0 until pagerState.pageCount) pagerState.animateScrollToPage(next)
                                                delay(750)
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            edgeJob?.cancel()
                            drag.item = null
                            drag.extra = emptyList()
                        }
                    }
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    // Размываем только содержимое стола: меню и перетаскиваемый значок лежат выше и остаются чёткими
                    .then(
                        if (!inWindowBlur) Modifier else Modifier.graphicsLayer {
                            val radius = overlayBlur * 30.dp.toPx() * settings.blurStrength
                            renderEffect = if (radius > 0.5f) androidx.compose.ui.graphics.BlurEffect(radius, radius, androidx.compose.ui.graphics.TileMode.Decal) else null
                        }
                    )
                    // Стеклянные панели поверх стола (выбор, зоны переноса, меню) размывают именно это содержимое
                    .backdropSource(homeBackdrop)
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = settings.gridPadding.dp)
                        .onGloballyPositioned { geometry.grid = it.boundsInRoot() }
                        // Слой внутри измеренной области: геометрия касаний и цели закрытия не зависят от анимации
                        .launchRecede(portal),
                ) {
                    val cellW = maxWidth / settings.columns
                    val cellH = maxHeight / settings.rows
                    val byPage = remember(items) { items.filter { !it.inDock }.groupBy { it.page } }
                    val transition = if (fx.lite) PageTransition.SLIDE else settings.pageTransition
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { it },
                        userScrollEnabled = !drag.active,
                        // Соседняя страница остаётся в композиции: при перелистывании её виджеты и значки уже готовы
                        beyondViewportPageCount = 1,
                    ) { page ->
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val amount = abs(offset).coerceIn(0f, 1f)
                                if (amount == 0f) return@graphicsLayer
                                applyPageTransition(transition, offset.coerceIn(-1f, 1f), amount)
                            },
                        ) {
                            for (item in byPage[page].orEmpty()) {
                                key(item.id) {
                                    HomeCell(
                                        item = item, appMap = appMap, vm = vm,
                                        cellW = cellW, cellH = cellH,
                                        hidden = { drag.contains(item.id) },
                                        selection = selection,
                                        pressedState = pressedState,
                                        wave = wave,
                                        wakeCount = wakeCount,
                                        drag = drag,
                                        geometry = geometry,
                                        portal = portal,
                                    )
                                }
                            }
                        }
                    }
                }
                // Временная страница на время перетаскивания не считается: индикатор отнял бы высоту, и сетка прыгнула бы вверх
                if (settings.showPageIndicator && GridLogic.pageCount(items) > 1) {
                    Box(Modifier.launchRecede(portal, scaleBy = 0f)) { PageIndicator(pagerState.currentPage, pageCount) }
                }
                Box(Modifier.launchDim(portal, blur = inWindowBlur)) {
                    BottomDeck(items, appMap, geometry, drag, selection, portal, pressedState, wave, onSearch = { openDrawer(search = true) }, onApps = { openDrawer() }, onLaunchApp = { app, rect -> launch(app, rect) })
                }
                Spacer(Modifier.height(6.dp))
            }

            CompositionLocalProvider(LocalBackdrop provides homeBackdrop) {
            if (drag.active) DragLayer(drag, geometry, items, appMap)

            // ---------- Меню элемента ----------
            val m = menu
            if (m != null) {
                val openedAt = remember(m) { android.os.SystemClock.uptimeMillis() }
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitEachGesture {
                            val d = awaitFirstDown(requireUnconsumed = false)
                            // Второй палец или ладонь сразу после долгого нажатия не закрывают меню
                            if (d.uptimeMillis - openedAt < MENU_GUARD_MS) return@awaitEachGesture
                            menu = null
                            resizeId = null
                        }
                    },
                )
            }
            // Рамка изменения размера виджета: лежит над слоем закрытия меню, чтобы ручки ловили касания
            val resizing = resizeId?.let { id -> items.firstOrNull { it.id == id } }
            if (resizing != null && !drag.active && resizing.page == pagerState.currentPage) {
                WidgetResizeFrame(
                    item = resizing,
                    geometry = geometry,
                    columns = settings.columns,
                    rows = settings.rows,
                    onStart = { menu = null },
                    onBounds = { x, y, w, h ->
                        val ok = vm.setWidgetBounds(resizing.id, x, y, w, h)
                        if (ok) haptics.play(HapticKind.TICK, HapticGroup.DRAG)
                        ok
                    },
                )
            }
            LaunchedEffect(drag.active) { if (drag.active) resizeId = null }
            AnimatedAnchoredMenu(menu, { it.anchor }, screenSize) { t ->
                ItemMenu(
                    item = t.item,
                    app = t.item.appKey?.let(appMap::get),
                    vm = vm,
                    onDismiss = { menu = null },
                    onOpenFolder = { openFolderId = it },
                    onLaunchShortcut = { s -> repo.startShortcut(s, null) },
                    onChangeIcon = actions::pickAppIcon,
                    onSelect = { selection.add(t.item.id) },
                )
            }

            // ---------- Настройка значка приложения ----------
            val styled = AppCustomize.app
            // Из меню приложений: шторка уходит, панель настройки открывается над столом
            val closeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            LaunchedEffect(styled) { if (styled != null && drawer.progress > 0f) drawer.animateTo(0f, closeSpec) }
            AnimatedVisibility(
                visible = styled != null,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                val last = remember { arrayOfNulls<AppInfo>(1) }
                (styled ?: last[0])?.let { a ->
                    last[0] = a
                    AppStyleSheet(a, onChangeIcon = actions::pickAppIcon, onDismiss = { AppCustomize.app = null })
                }
            }

            // ---------- Своё меню обоев ----------
            AnimatedVisibility(
                visible = showWallpaperSheet,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                WallpaperSheet(vm, onDismiss = { showWallpaperSheet = false }, onSystemPicker = { showWallpaperSheet = false; actions.pickWallpaper() })
            }

            // ---------- Меню пустого места ----------
            AnimatedVisibility(
                visible = showHomeMenu != null,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                HomeMenu(
                    onDismiss = { showHomeMenu = null },
                    onWidgets = { showHomeMenu = null; showWidgets = true },
                    onWallpaper = { showHomeMenu = null; showWallpaperSheet = true },
                    onSettings = { showHomeMenu = null; actions.openSettings() },
                )
            }
            }
        }

        // ---------- Выбор нескольких значков ----------
        // Выделение читается внутри панели, а не в теле стола: отметка значка перекомпоновывает только её,
        // а сами клетки обновляются своими derivedStateOf
        SelectionBarHost(
            selection = selection,
            drag = drag,
            canShow = menu == null && showHomeMenu == null && openFolderId == null,
            backdrop = homeBackdrop,
            onRemove = { ids -> vm.removeItems(ids); selection.clear() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 8.dp),
        )

        // ---------- Шторка ----------
        // Меню держим в композиции: его первое построение на старте жеста давало заметный рывок.
        // Закрытое меню не рисуется (прозрачно и за пределами экрана) и не получает касаний.
        val appsLoaded by repo.loaded.collectAsState()
        if (appsLoaded) {
            AppDrawer(
                vm = vm,
                drawer = drawer,
                screenHeightPx = screenHeightPx,
                searchRequest = searchRequest,
                actions = actions,
                onLaunch = ::launch,
                onMenu = { app, rect -> drawerMenu = DrawerMenuTarget(app, rect) },
                picked = drawerPicked,
                onPick = { app ->
                    haptics.play(HapticKind.CLICK, HapticGroup.TOUCH)
                    drawerPicked = if (app.key in drawerPicked) drawerPicked - app.key else drawerPicked + app.key
                },
                onClearPicked = { drawerPicked = emptySet() },
                menuOpen = drawerMenu != null,
                drawerBackdrop = drawerBackdrop,
                onAddPicked = {
                    // Порядок — как в шторке, а не в порядке нажатий
                    val chosen = apps.filter { it.key in drawerPicked }.sortedBy { it.sortKey }
                    val page = vm.addApps(chosen)
                    drawerPicked = emptySet()
                    haptics.play(HapticKind.DROP, HapticGroup.DRAG)
                    drawer.animateTo(0f, spatial)
                    if (page != null) scope.launch { pagerState.animateScrollToPage(page) }
                },
            )
        }
        // Шторку закрыли: отметки не должны дожидаться следующего открытия
        LaunchedEffect(Unit) {
            snapshotFlow { drawer.progress <= 0.001f }.collect { closed -> if (closed) drawerPicked = emptySet() }
        }
        val glassTheme = themeSpec.glass
        LaunchedEffect(glassTheme) {
            // Обои размывает сама система, если устройство умеет. Включаем одним вызовом: это межпроцессная операция, а плавность обеспечивает система.
            // Liquid Glass: под папкой обои не размываются (стекло папки преломляет чёткий стол), под выбором виджетов — да
            snapshotFlow {
                val s = liveSettings
                val under = if (glassTheme) drawer.progress else maxOf(drawer.progress, overlayBlur)
                // Expressive: «Размытие везде» добавляет размытие под меню стола и меню значков;
                // без него — только под меню приложений, папкой, виджетами и обоями
                val menus = themeSpec.accentChrome && s.blurEverywhere && showHomeMenu != null
                val sheets = !glassTheme && (showWidgets || (themeSpec.accentChrome && showWallpaperSheet))
                if (!s.blurBackground || s.blurStrength <= 0f) 0
                else if (under > 0.06f || sheets || menus) (40f * s.blurStrength).roundToInt() else 0
            }.collect { step ->
                actions.setBlur(step)
            }
        }

        drawerMenu?.let { dm ->
            val openedAt = remember(dm) { android.os.SystemClock.uptimeMillis() }
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        val d = awaitFirstDown(requireUnconsumed = false)
                        if (d.uptimeMillis - openedAt >= MENU_GUARD_MS) drawerMenu = null
                    }
                },
            )
        }
        AnimatedAnchoredMenu(drawerMenu, { it.anchor }, screenSize) { dm ->
            // Меню значка в шторке лежит над размытым списком: снимок чёткого списка давал резкие сдвинутые значки
            // внутри стекла. Здесь стекло матовое — на размытой копии обоев, как остальные панели шторки
            CompositionLocalProvider(LocalBackdrop provides null) {
            DrawerAppMenu(
                app = dm.app,
                alreadyOnHome = vm.isOnHome(dm.app.key),
                vm = vm,
                onDismiss = { drawerMenu = null },
                onAdded = { drawerMenu = null; drawer.animateTo(0f, spatial) },
                onShortcut = { s -> repo.startShortcut(s, null) },
                onChangeIcon = actions::pickAppIcon,
                onSelect = { drawerPicked = drawerPicked + dm.app.key },
            )
            }
        }

        // ---------- Папка ----------
        val folder = items.firstOrNull { it.id == openFolderId && it.type == ItemType.FOLDER }
        // Папка распалась, когда из неё вынули предпоследнее приложение: пустое окно и размытие оставаться не должны
        LaunchedEffect(folder == null, openFolderId) { if (folder == null) openFolderId = null }
        var lastFolder by remember { mutableStateOf<HomeItem?>(null) }
        if (folder != null) lastFolder = folder
        CompositionLocalProvider(LocalBackdrop provides homeBackdrop) {
        AnimatedVisibility(
            visible = folder != null,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            val f = lastFolder
            if (f != null) {
                val bounds = geometry.itemBounds(f)
                FolderOverlay(
                    folder = f, appMap = appMap, vm = vm,
                    origin = TransformOrigin(
                        (bounds.center.x / screenSize.width).coerceIn(0f, 1f),
                        (bounds.center.y / screenSize.height).coerceIn(0f, 1f),
                    ),
                    onDismiss = { openFolderId = null },
                    onLaunch = { app, rect -> openFolderId = null; launch(app, rect, LaunchSource.Folder(f.id)) },
                    onAppInfo = { repo.openAppInfo(it, null) },
                )
            }
        }
        }

        // ---------- Выбор виджета ----------
        AnimatedVisibility(
            visible = showWidgets,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 4 },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 4 },
        ) {
            WidgetPicker(
                vm = vm,
                onDismiss = { showWidgets = false },
                onPick = { info ->
                    showWidgets = false
                    val cellWDp = with(density) { (geometry.cellW).toDp().value }.coerceAtLeast(40f)
                    val cellHDp = with(density) { (geometry.cellH).toDp().value }.coerceAtLeast(40f)
                    val w = ceil(info.minWidth / density.density / cellWDp).toInt().coerceIn(1, settings.columns)
                    val h = ceil(info.minHeight / density.density / cellHDp).toInt().coerceIn(1, settings.rows)
                    actions.addWidget(info, w, h, pagerState.currentPage)
                },
            )
        }

        // ---------- Назад ----------
        // Обработчик всегда включён: на самом столе «Назад» ничего не закрывает. Без этого система
        // завершала активность, и лаунчер на мгновение закрывался (или падал, если он не был домашним)
        BackHandler {
            when {
                menu != null -> menu = null
                drawerMenu != null -> drawerMenu = null
                drawerPicked.isNotEmpty() -> drawerPicked = emptySet()
                showHomeMenu != null -> showHomeMenu = null
                showWidgets -> showWidgets = false
                openFolderId != null -> openFolderId = null
                drawer.progress > 0f -> drawer.animateTo(0f, spatial)
                selection.active -> selection.clear()
                pagerState.currentPage != 0 -> scope.launch { pagerState.animateScrollToPage(0) }
                // Уже на первом экране: остаёмся на столе
                else -> Unit
            }
        }

        // Обои плывут вслед за страницами; цвет значков строки состояния зависит от того, что под ней
        LaunchedEffect(Unit) {
            snapshotFlow { (pagerState.currentPage + pagerState.currentPageOffsetFraction) to pagerState.pageCount }
                // Межпроцессный вызов на каждый кадр нельзя делать в главном потоке; пропущенные значения не важны.
                // В режиме энергосбережения обои не гоняем: сдвигаем их один раз, когда страница встала
                .conflate()
                .collect { (position, pages) -> withContext(Dispatchers.Default) { actions.setWallpaperScroll(position, pages) } }
        }
        // Стекло сдвигает свой кусок размытых обоев так же, как система сдвигает сами обои (параллакс без пересчёта размытия)
        LaunchedEffect(Unit) {
            snapshotFlow {
                val steps = (pagerState.pageCount - 1).coerceAtLeast(1)
                // Свои обои можно не листать: тогда они стоят по центру
                if (!com.example.expressivelauncher.ui.components.CustomWallpaper.parallaxOn) 0.5f
                else ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / steps).coerceIn(0f, 1f)
            }.collect { com.example.expressivelauncher.ui.components.WallpaperBlur.parallax = it }
        }
        val overSurface by remember { derivedStateOf { drawer.progress > 0.5f || showWidgets } }
        LaunchedEffect(overSurface) { actions.setOverSurface(overSurface) }

        LaunchPortalHost(portal)

        // Кнопка «Домой»
        val homePress = actions.homePressSignal
        LaunchedEffect(homePress) {
            if (homePress > 0) {
                // Вернулись из приложения (или нажали «Домой», пока оно ещё не показалось): окно сворачивается
                // в значок сразу, не дожидаясь, пока система вернёт лаунчеру фокус, — иначе между ними заметная пауза.
                // Меню приложений сейчас закроется, поэтому окно из него уходит в центр, а не в исчезающий список
                val launching = portal.active
                portal.close(keepFree = false)
                menu = null; drawerMenu = null; showHomeMenu = null; showWidgets = false; openFolderId = null
                drawerPicked = emptySet()
                selection.clear()
                drawer.animateTo(0f, spatial)
                // Пока окно летит в значок, страницу не листаем: значок должен остаться на месте
                if (actions.homeFromForeground && !launching) pagerState.animateScrollToPage(0)
            }
        }
    }
}

// ---------- Клетки ----------

@Composable
private fun HomeCell(
    item: HomeItem,
    appMap: Map<String, AppInfo>,
    vm: LauncherViewModel,
    cellW: androidx.compose.ui.unit.Dp,
    cellH: androidx.compose.ui.unit.Dp,
    hidden: () -> Boolean,
    selection: SelectionState,
    pressedState: PressedState,
    wave: WakeWave,
    wakeCount: Int,
    drag: DragState,
    geometry: HomeGeometry,
    portal: LaunchPortal,
) {
    val settings = LocalSettings.current
    val density = LocalDensity.current
    // Каждое состояние читается только своей клеткой: нажатие, выделение и перетаскивание соседа
    // не перекомпоновывают страницу целиком
    val selected by remember(item) { derivedStateOf { selection.contains(item.id) } }
    val pressed by remember(item) { derivedStateOf { pressedState.id == item.id } }
    val landing by remember(item.id) { derivedStateOf { drag.landing?.takeIf { it.id == item.id } } }
    // Позиция меняется, когда соседи расступаются: клетка перерисовывается, только если сдвинулась она сама
    val slot by remember(item) { derivedStateOf { drag.preview[item.id] ?: (item.x to item.y) } }
    val (x, y) = slot
    val target = with(density) { Offset((cellW * x).toPx(), (cellH * y).toPx()) }
    val morph by animateFloatAsState(if (pressed) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pressMorph")
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .animatedCell(target, landing?.let { l ->
                {
                    with(density) {
                        Offset(
                            l.point.x - geometry.grid.left - (cellW * item.w).toPx() / 2f,
                            l.point.y - geometry.grid.top - (cellH * item.h).toPx() / 2f,
                        )
                    }
                }
            }, landing)
            .size(cellW * item.w, cellH * item.h)
            .then(
                if (selected) Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.primary.copy(alpha = 0.16f))
                    .border(2.dp, colors.primary.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                else Modifier
            )
            .homeItemMotion(pressed, hidden, wave, order = item.x + item.y),
        contentAlignment = Alignment.Center,
    ) {
        when (item.type) {
            ItemType.APP -> appMap[item.appKey]?.let { app ->
                AppCell(app, cellW, cellH, settings.showHomeLabels, { morph }, { portal.conceals(item.id) }) { geometry.contentHeights[item.id] = it }
            }
            ItemType.FOLDER -> FolderCell(
                item, appMap, homeIconSize(cellW, cellH, settings.iconScale, settings.showHomeLabels),
                labelWidth = if (settings.showHomeLabels) cellW else null,
            )
            ItemType.WIDGET -> HomeWidget(item, vm.widgets, Modifier.fillMaxSize().padding(4.dp))
            ItemType.CLOCK -> ClockCard(settings.clockStyle, wakeCount)
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun homeIconSize(cellW: androidx.compose.ui.unit.Dp, cellH: androidx.compose.ui.unit.Dp, scale: Float, labels: Boolean) =
    (min(cellW.value * 0.74f, cellH.value * if (labels) 0.62f else 0.78f) * scale).dp

/** Размер значка в пикселях, как он нарисован: нужен, чтобы анимация запуска стартовала ровно из него. */
private fun iconPxFor(item: HomeItem, geometry: HomeGeometry, density: androidx.compose.ui.unit.Density, s: com.example.expressivelauncher.data.Settings): Float = with(density) {
    if (item.inDock) dockIconSize(s, if (geometry.dock.width > 0f) geometry.dock.width.toDp() else null).toPx()
    else homeIconSize(geometry.cellW.toDp(), geometry.cellH.toDp(), s.iconScale, s.showHomeLabels).toPx()
}

private fun labelPx(density: androidx.compose.ui.unit.Density, s: com.example.expressivelauncher.data.Settings): Float = with(density) {
    if (s.showHomeLabels) 4.dp.toPx() + (16.sp * s.labelScale).toPx() else 0f
}

private const val DOCK_ICON_DP = 56

/**
 * Значок дока: растёт вместе с «Размером значков», но не шире своей ячейки — иначе крупные значки
 * наезжали друг на друга и вылезали за подложку дока.
 */
private fun dockIconSize(s: com.example.expressivelauncher.data.Settings, dockWidth: androidx.compose.ui.unit.Dp?): androidx.compose.ui.unit.Dp {
    val wanted = DOCK_ICON_DP * s.iconScale
    val slot = dockWidth?.let { it.value / s.dockCount.coerceAtLeast(1) - 8f } ?: wanted
    return min(wanted, slot).coerceAtLeast(24f).dp
}

/** Поля над и под значком внутри дока. */
private const val DOCK_ICON_PAD_DP = 6f

/** Зоны действий при перетаскивании: слева направо, как они лежат на экране. */
private val DRAG_ACTIONS = listOf(DragAction.REMOVE, DragAction.APP_INFO, DragAction.UNINSTALL)

/** Сколько уровней размытия стола под меню приложений кэшируется (Liquid Glass). */
private const val HOME_BLUR_LEVELS = 12

/** Как уходит страница при перелистывании; [offset] положителен у страницы слева от текущей. */
private fun androidx.compose.ui.graphics.GraphicsLayerScope.applyPageTransition(transition: PageTransition, offset: Float, amount: Float) {
    when (transition) {
        PageTransition.SLIDE -> Unit
        PageTransition.DEPTH -> {
            val scale = 1f - 0.1f * amount
            scaleX = scale
            scaleY = scale
            alpha = 1f - 0.4f * amount
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            rotationY = offset * 12f
            cameraDistance = 24f * density
        }
        PageTransition.CUBE -> {
            transformOrigin = TransformOrigin(if (offset > 0f) 1f else 0f, 0.5f)
            rotationY = -offset * 55f
            cameraDistance = 16f * density
        }
        PageTransition.FADE -> {
            // Страница стоит на месте и растворяется, пока следующая проявляется поверх
            translationX = offset * size.width
            alpha = 1f - amount
            val scale = 1f - 0.06f * amount
            scaleX = scale
            scaleY = scale
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
        }
    }
}

@Composable
private fun AppCell(
    app: AppInfo, cellW: androidx.compose.ui.unit.Dp, cellH: androidx.compose.ui.unit.Dp, labels: Boolean,
    morph: () -> Float, concealed: () -> Boolean, onHeight: (Int) -> Unit,
) {
    val s = LocalSettings.current
    Column(
        Modifier.onSizeChanged { onHeight(it.height) },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        // Пока окно приложения выросло из значка, скрыт только он сам: подпись остаётся на месте
        AppIconImage(app, homeIconSize(cellW, cellH, s.iconScale, labels), Modifier.graphicsLayer { alpha = if (concealed()) 0f else 1f }, morph = morph)
        if (labels) {
            Spacer(Modifier.height(4.dp))
            // Подпись можно скрыть у одного приложения: место под ней остаётся, сетка не прыгает
            val hidden = com.example.expressivelauncher.data.AppStyles.of(app.key)?.hideLabel == true
            AppLabel(app.displayLabel, Modifier.width(cellW).graphicsLayer { alpha = if (hidden) 0f else 1f }, onWallpaper = true, lines = s.labelLines)
        }
    }
}

@Composable
internal fun FolderCell(
    item: HomeItem,
    appMap: Map<String, AppInfo>,
    size: androidx.compose.ui.unit.Dp,
    labelWidth: androidx.compose.ui.unit.Dp? = null,
) {
    val s = LocalSettings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val glass = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
        // Liquid Glass: плитка папки — стекло; скруглённый прямоугольник рисуется аналитически, без маски контура
        Box(
            if (glass) Modifier.size(size) else Modifier.size(size).clip(s.iconShape.toComposeShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (glass) com.example.expressivelauncher.ui.components.LiquidSurface(
                androidx.compose.foundation.shape.RoundedCornerShape(percent = 26), Modifier.matchParentSize(), shadow = false, blockTouches = false,
            ) { }
            val mini = size * 0.36f
            Column(verticalArrangement = Arrangement.spacedBy(size * 0.06f)) {
                for (row in item.children.take(4).chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.06f)) {
                        for (k in row) {
                            val app = appMap[k]
                            if (app != null) AppIconImage(app, mini, showBadge = false) else Spacer(Modifier.size(mini))
                        }
                    }
                }
            }
        }
        if (labelWidth != null) {
            Spacer(Modifier.height(4.dp))
            AppLabel(item.title.ifBlank { stringResource(R.string.folder_default_name) }, Modifier.width(labelWidth), onWallpaper = true)
        }
    }
}

// ---------- Док, индикатор, поиск ----------

/** Нижняя карточка: значки дока и строка поиска живут в одной тональной поверхности. */
@Composable
private fun BottomDeck(
    items: List<HomeItem>,
    appMap: Map<String, AppInfo>,
    geometry: HomeGeometry,
    drag: DragState,
    selection: SelectionState,
    portal: LaunchPortal,
    pressedState: PressedState,
    wave: WakeWave,
    onSearch: () -> Unit,
    onApps: () -> Unit,
    onLaunchApp: (AppInfo, androidx.compose.ui.geometry.Rect?) -> Unit,
) {
    val s = LocalSettings.current
    val fx = LocalFx.current
    if (!s.showDock && !s.showSearchPill) return
    val dockItems = remember(items) { items.filter { it.inDock } }
    val shape = RoundedCornerShape(s.dockRadius.dp)
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    // Expressive: насыщенная панель тёмного фиксированного тона акцента, матовая — только с «Размытием везде»
    val tint = (if (spec.accentChrome) accentColors().panel else MaterialTheme.colorScheme.surfaceContainerHigh)
        // Expressive: док насыщенный, почти сплошной — полупрозрачность на обоях делала цвет мутным
        .copy(alpha = if (spec.accentChrome && !s.blurEverywhere) maxOf(s.dockOpacity, 0.95f) else s.dockOpacity)
    val glass = s.dockBackground && s.blurBackground && fx.blur && s.blurStrength > 0f && (!spec.accentChrome || s.blurEverywhere)

    // Свой размер значка (настройка приложения) увеличивает его слоем: док выше на самый крупный из них
    val styleScale = dockItems.maxOfOrNull { it.appKey?.let { k -> com.example.expressivelauncher.data.AppStyles.of(k)?.scale } ?: 1f }
        ?.coerceAtLeast(1f) ?: 1f
    val dockRow: @Composable (Modifier) -> Unit = { rowModifier ->
                BoxWithConstraints(rowModifier) {
                    val slotW = maxWidth / s.dockCount
                    val dockIcon = dockIconSize(s, maxWidth)
                    // Ряд дока не ниже значка с полями: при крупных значках док растёт, а не обрезает их
                    val rowH = maxOf(s.dockHeight.toFloat(), dockIcon.value * styleScale + 2 * DOCK_ICON_PAD_DP).dp
                    Box(Modifier.fillMaxWidth().height(rowH).onGloballyPositioned { geometry.dock = it.boundsInRoot() }) {
                    val density = LocalDensity.current
                    for (item in dockItems) {
                        key(item.id) {
                            // Состояния читаются только своей ячейкой дока: нажатие одной не перекомпоновывает остальные
                            val pressed by remember(item.id) { derivedStateOf { pressedState.id == item.id } }
                            val selected by remember(item.id) { derivedStateOf { selection.contains(item.id) } }
                            val landing by remember(item.id) { derivedStateOf { drag.landing?.takeIf { it.id == item.id } } }
                            val slot by remember(item) { derivedStateOf { drag.preview[item.id]?.first ?: item.x } }
                            val morph by animateFloatAsState(if (pressed) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pressMorph")
                            Box(
                                Modifier
                                    .animatedCell(with(density) { Offset((slotW * slot).toPx(), 0f) }, landing?.let { l ->
                                        {
                                            with(density) {
                                                Offset(l.point.x - geometry.dock.left - slotW.toPx() / 2f, l.point.y - geometry.dock.center.y)
                                            }
                                        }
                                    }, landing)
                                    .size(slotW, rowH)
                                    .then(
                                        if (selected) Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                                        else Modifier
                                    )
                                    .homeItemMotion(pressed, { drag.contains(item.id) }, wave, order = s.rows + item.x, concealed = { portal.conceals(item.id) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                when (item.type) {
                                    ItemType.APP -> appMap[item.appKey]?.let { AppIconImage(it, dockIcon, morph = { morph }) }
                                    ItemType.FOLDER -> FolderCell(item, appMap, dockIcon)
                                    else -> Unit
                                }
                                if (selected) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp)
                                            .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    }
                }
    }
    val searchBar: @Composable (Modifier) -> Unit = { barModifier ->
            if (s.showSearchPill) {
                PixelSearchBar(
                    appMap, onSearch, onApps, onLaunchApp,
                    barModifier.onGloballyPositioned { geometry.controls["search"] = it.boundsInRoot() },
                )
            } else {
                geometry.controls.remove("search")
            }
    }
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            if (s.showDock) dockRow(Modifier.fillMaxWidth())
            searchBar(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = if (s.showDock) 0.dp else 8.dp))
        }
    }

    val deckModifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
        .then(if (s.dockBackground) Modifier.accentGlow(shape, 0.45f) else Modifier)
    if (glass) {
        FrostedGlass(shape, tint, (28f * s.blurStrength).dp, deckModifier) { content() }
    } else {
        Column(
            deckModifier
                .clip(shape)
                .then(if (s.dockBackground) Modifier.background(tint) else Modifier)
                // Liquid Glass без размытия (Android ниже 12): полупрозрачная подложка с краем стекла
                .then(if (s.dockBackground) Modifier.glassEdge(shape) else Modifier),
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageIndicator(current: Int, count: Int) {
    val s = LocalSettings.current
    val fx = LocalFx.current
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    val glass = s.blurBackground && fx.blur && s.blurStrength > 0f && !spec.accentChrome
    val tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    // Expressive: точки прямо на обоях, тоном акцента, контрастным к обоям (тёмный тон на светлых, светлый на тёмных)
    val lightWall = com.example.expressivelauncher.ui.LocalWallpaperLight.current
    val cs = MaterialTheme.colorScheme
    val accentOn = com.example.expressivelauncher.ui.theme.accentInk(lightWall)
    val dots: @Composable () -> Unit = {
        repeat(count) { i ->
            val selected = i == current
            val w by animateFloatAsState(
                if (selected) 24f else 8f,
                MaterialTheme.motionScheme.defaultSpatialSpec(), label = "dot",
            )
            val dot by animateColorAsState(
                when {
                    spec.accentChrome -> if (selected) accentOn else accentOn.copy(alpha = 0.45f)
                    selected -> cs.primary
                    else -> cs.onSurfaceVariant.copy(alpha = 0.5f)
                },
                MaterialTheme.motionScheme.fastEffectsSpec(), label = "dotColor",
            )
            Box(Modifier.padding(horizontal = 3.dp).size(w.dp, 8.dp).clip(CircleShape).background(dot))
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glass) {
            FrostedGlass(
                CircleShape, tint, (18f * s.blurStrength).dp,
                Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) { dots() }
            }
        } else {
            // Lite: просто точки на обоях, без подложки — как на стоковом Pixel
            val lite = spec.lite || spec.accentChrome
            Row(
                (if (lite) Modifier else Modifier.clip(CircleShape).background(tint)).padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { dots() }
        }
    }
}

/**
 * Панель выбранных значков. Читает [SelectionState] и [DragState] сама: изменение выделения не заставляет
 * перекомпоновываться весь рабочий стол, только эту панель.
 */
@Composable
private fun SelectionBarHost(
    selection: SelectionState,
    drag: DragState,
    canShow: Boolean,
    backdrop: Backdrop?,
    onRemove: (Set<String>) -> Unit,
    modifier: Modifier,
) {
    if (!canShow || drag.active || !selection.active) return
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        SelectionBar(
            count = selection.ids.size,
            onClear = { selection.clear() },
            onRemove = { onRemove(selection.ids) },
            modifier = modifier,
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    GlassPanel(
        shape = RoundedCornerShape(32.dp),
        color = cs.surfaceContainerHigh,
        glassAlpha = 0.7f,
        elevation = 6.dp,
        // Панель перехватывает касания: тап по ней не должен запускать значок под ней
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
        },
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.padding(end = 4.dp).widthIn(max = 220.dp)) {
                Text(stringResource(R.string.selection_count, count), style = MaterialTheme.typography.titleSmall)
                // Без подсказки перенос группы не найти: он начинается с долгого нажатия на любой выбранный значок
                Text(
                    stringResource(R.string.selection_drag_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.selection_remove), tint = cs.error)
            }
            IconButton(onClick = onClear, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Close, stringResource(R.string.selection_clear))
            }
        }
    }
}

// ---------- Слой перетаскивания ----------

@Composable
private fun DragLayer(drag: DragState, geometry: HomeGeometry, items: List<HomeItem>, appMap: Map<String, AppInfo>) {
    val item = drag.item ?: return
    val group = drag.all
    val density = LocalDensity.current
    val s = LocalSettings.current
    // Цель под пальцем зависит от его координаты, но палец движется внутри клетки десятки раз в секунду.
    // derivedStateOf пересчитывает её на каждое движение, а композиция повторяется только при смене клетки.
    val target by remember(item.id, items) {
        derivedStateOf {
            val raw = geometry.dropTarget(drag.pointer, item)
            when {
                drag.multi && raw is DropTarget.Cell -> raw.copy(merge = false)
                raw is DropTarget.Cell -> drag.effective(raw, items)
                else -> raw
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val allApps = group.all { it.type == ItemType.APP }

    // Панель действий сверху
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).height(60.dp).padding(horizontal = 8.dp, vertical = 4.dp).staggerIn(0),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        geometry.zones.forEach { (action, _) ->
            val systemApp = action == DragAction.UNINSTALL && group.any { appMap[it.appKey]?.isSystem == true }
            if ((action != DragAction.REMOVE && !allApps) || systemApp) {
                Spacer(Modifier.weight(1f))
                return@forEach
            }
            val hot = (target as? DropTarget.Action)?.action == action
            val (icon, label) = when (action) {
                DragAction.REMOVE -> Icons.Rounded.Close to R.string.drag_remove
                DragAction.APP_INFO -> Icons.Rounded.Info to R.string.drag_info
                DragAction.UNINSTALL -> Icons.Rounded.Delete to R.string.menu_uninstall
            }
            // Стекло тонируется основным цветом, когда палец над зоной: размытие под ней остаётся
            val zoneColor by animateColorAsState(if (hot) colors.primary else colors.surfaceContainerHigh, motion.fastEffectsSpec(), label = "zone")
            val zoneAlpha by animateFloatAsState(if (hot) 0.95f else 0.68f, motion.fastEffectsSpec(), label = "zoneAlpha")
            val zoneScale by animateFloatAsState(if (hot) 1.06f else 1f, motion.fastSpatialSpec(), label = "zoneScale")
            GlassPanel(
                shape = CircleShape,
                color = zoneColor,
                glassAlpha = zoneAlpha,
                solidAlpha = if (hot) 1f else 0.94f,
                contentColor = if (hot) colors.onPrimary else colors.onSurface,
                modifier = Modifier.weight(1f).fillMaxSize().graphicsLayer { scaleX = zoneScale; scaleY = zoneScale },
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }

    // Подсветка ячейки. Локальная переменная нужна ещё и для smart cast: target — делегированное свойство.
    val cellTarget = target as? DropTarget.Cell
    if (cellTarget != null) {
        val occupied = GridLogic.itemAt(items, cellTarget.page, cellTarget.x, cellTarget.y, item.id)
        val folderHint = cellTarget.merge && occupied != null && item.type == ItemType.APP && (occupied.type == ItemType.APP || occupied.type == ItemType.FOLDER)
        // Подсветка плавно перетекает между ячейками, а над значком округляется: сюда ляжет папка
        val cellOffset by animateIntOffsetAsState(IntOffset(cellTarget.rect.left.roundToInt(), cellTarget.rect.top.roundToInt()), motion.fastSpatialSpec(), label = "cell")
        val corner by animateDpAsState(if (folderHint) 56.dp else 24.dp, motion.fastSpatialSpec(), label = "cellCorner")
        val cellShape = RoundedCornerShape(corner.coerceAtLeast(0.dp))
        Box(
            Modifier
                .offset { cellOffset }
                .size(with(density) { cellTarget.rect.width.toDp() }, with(density) { cellTarget.rect.height.toDp() })
                .padding(4.dp)
                .clip(cellShape)
                .background(colors.primary.copy(alpha = if (folderHint) 0.34f else 0.18f))
                .border(2.dp, colors.primary.copy(alpha = 0.7f), cellShape),
        )
    }

    // Призрак под пальцем
    val ghostSize = if (item.type == ItemType.WIDGET || item.type == ItemType.CLOCK) null else 64.dp
    val ghostPx = with(density) { (ghostSize ?: 0.dp).toPx() }
    // Призрак приподнимается и наклоняется в сторону движения. Позиция и наклон читаются в фазах размещения
    // и отрисовки, поэтому движение пальца не вызывает рекомпозиций — только перерисовку одного слоя.
    val lift = remember { androidx.compose.animation.core.Animatable(1f) }
    val liftSpec = motion.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) { lift.animateTo(1.14f, liftSpec) }
    // Сглаженный курсор для наклона: цель обновляется подпиской, поэтому движение пальца
    // не вызывает рекомпозиций, а наклон считается по отставанию сглаженной точки от настоящей
    val laggedX = remember { androidx.compose.animation.core.Animatable(drag.pointer.x) }
    LaunchedEffect(drag.item?.id) {
        if (drag.item == null) {
            laggedX.snapTo(drag.pointer.x)
            return@LaunchedEffect
        }
        snapshotFlow { drag.pointer.x }.collectLatest { x ->
            laggedX.animateTo(x, androidx.compose.animation.core.spring(stiffness = 1500f, dampingRatio = 1f, visibilityThreshold = 0.5f))
        }
    }
    Box(
        Modifier
            .offset {
                val p = drag.pointer
                if (ghostSize != null) IntOffset((p.x - ghostPx / 2).roundToInt(), (p.y - ghostPx * 0.8f).roundToInt())
                else IntOffset(
                    (p.x - geometry.cellW * item.w / 2).roundToInt(),
                    (p.y - geometry.cellH * item.h / 2).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = lift.value
                scaleY = lift.value
                val p = drag.pointer
                rotationZ = ((p.x - laggedX.value) * 0.05f).coerceIn(-14f, 14f)
                shadowElevation = 24f
                shape = CircleShape
                alpha = 0.95f
            },
    ) {
        when (item.type) {
            ItemType.APP -> appMap[item.appKey]?.let { AppIconImage(it, ghostSize!! * s.iconScale) }
            ItemType.FOLDER -> FolderCell(item, appMap, ghostSize!! * s.iconScale)
            ItemType.WIDGET, ItemType.CLOCK -> Box(
                Modifier
                    .size(with(density) { (geometry.cellW * item.w).toDp() }, with(density) { (geometry.cellH * item.h).toDp() })
                    .padding(4.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(colors.primaryContainer.copy(alpha = 0.85f)),
            )
        }
        if (drag.multi) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    group.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimary,
                )
            }
        }
    }
}

/** Портал рисуется только пока активен: в обычное время на столе нет лишнего слоя. */
@Composable
private fun LaunchPortalHost(portal: LaunchPortal) {
    if (portal.active) LaunchPortalOverlay(portal)
}

private enum class Edge { LEFT, TOP, RIGHT, BOTTOM }

/**
 * Рамка изменения размера виджета в духе One UI: четыре ручки по серединам сторон.
 * Рамка идёт за пальцем плавно, а виджет перескакивает по клеткам сетки; соседи расступаются.
 */
@Composable
private fun WidgetResizeFrame(
    item: HomeItem,
    geometry: HomeGeometry,
    columns: Int,
    rows: Int,
    onStart: () -> Unit,
    onBounds: (x: Int, y: Int, w: Int, h: Int) -> Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val current by rememberUpdatedState(item)
    val cellW = geometry.cellW
    val cellH = geometry.cellH
    fun rectOf(i: HomeItem) = androidx.compose.ui.geometry.Rect(
        geometry.grid.left + i.x * cellW, geometry.grid.top + i.y * cellH,
        geometry.grid.left + (i.x + i.w) * cellW, geometry.grid.top + (i.y + i.h) * cellH,
    )
    var live by remember(item.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val snapped = rectOf(item)
    // Когда палец отпущен, рамка мягко садится на клетки
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.geometry.Rect>()
    val shown by androidx.compose.animation.core.animateValueAsState(
        live ?: snapped, androidx.compose.ui.geometry.Rect.VectorConverter, spec, label = "resizeFrame",
    )
    val r = if (live != null) live!! else shown
    val pad = with(density) { 4.dp.toPx() }
    val appear = remember { androidx.compose.animation.core.Animatable(0.85f) }
    LaunchedEffect(item.id) { appear.animateTo(1f, androidx.compose.animation.core.spring(0.55f, 600f)) }

    Box(
        Modifier
            .offset { IntOffset((r.left + pad).roundToInt(), (r.top + pad).roundToInt()) }
            .size(with(density) { (r.width - 2 * pad).coerceAtLeast(1f).toDp() }, with(density) { (r.height - 2 * pad).coerceAtLeast(1f).toDp() })
            .graphicsLayer { scaleX = appear.value; scaleY = appear.value; alpha = ((appear.value - 0.85f) / 0.15f).coerceIn(0f, 1f) }
            .border(3.dp, colors.primary, RoundedCornerShape(24.dp)),
    )

    val handle = 30.dp
    val handlePx = with(density) { handle.toPx() }
    for (edge in Edge.entries) {
        val cx = when (edge) { Edge.LEFT -> r.left + pad; Edge.RIGHT -> r.right - pad; else -> r.center.x }
        val cy = when (edge) { Edge.TOP -> r.top + pad; Edge.BOTTOM -> r.bottom - pad; else -> r.center.y }
        Box(
            Modifier
                .offset { IntOffset((cx - handlePx).roundToInt(), (cy - handlePx).roundToInt()) }
                // Зона касания вдвое больше видимой ручки: попасть пальцем легко
                .size(handle * 2)
                // У края экрана ручку не должен перехватывать системный жест «Назад»
                .systemGestureExclusion()
                .pointerInput(item.id, edge) {
                    var start = androidx.compose.ui.geometry.Rect.Zero
                    detectDragGestures(
                        onDragStart = {
                            onStart()
                            start = rectOf(current)
                            live = start
                        },
                        onDragEnd = { live = null },
                        onDragCancel = { live = null },
                    ) { change, delta ->
                        change.consume()
                        val l = live ?: return@detectDragGestures
                        val minW = cellW * 0.8f
                        val minH = cellH * 0.8f
                        val next = when (edge) {
                            Edge.LEFT -> l.copy(left = (l.left + delta.x).coerceIn(geometry.grid.left, l.right - minW))
                            Edge.RIGHT -> l.copy(right = (l.right + delta.x).coerceIn(l.left + minW, geometry.grid.right))
                            Edge.TOP -> l.copy(top = (l.top + delta.y).coerceIn(geometry.grid.top, l.bottom - minH))
                            Edge.BOTTOM -> l.copy(bottom = (l.bottom + delta.y).coerceIn(l.top + minH, geometry.grid.bottom))
                        }
                        live = next
                        val it0 = current
                        var x = it0.x; var y = it0.y; var w = it0.w; var h = it0.h
                        when (edge) {
                            Edge.LEFT -> { val nx = ((next.left - geometry.grid.left) / cellW).roundToInt().coerceIn(0, it0.x + it0.w - 1); w = it0.x + it0.w - nx; x = nx }
                            Edge.RIGHT -> w = ((next.right - geometry.grid.left) / cellW).roundToInt().coerceIn(it0.x + 1, columns) - it0.x
                            Edge.TOP -> { val ny = ((next.top - geometry.grid.top) / cellH).roundToInt().coerceIn(0, it0.y + it0.h - 1); h = it0.y + it0.h - ny; y = ny }
                            Edge.BOTTOM -> h = ((next.bottom - geometry.grid.top) / cellH).roundToInt().coerceIn(it0.y + 1, rows) - it0.y
                        }
                        if (x != it0.x || y != it0.y || w != it0.w || h != it0.h) onBounds(x, y, w, h)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val active = live != null
            val size by animateDpAsState(if (active) 22.dp else 18.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "handle")
            Box(
                Modifier
                    .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                    .size(size)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(3.dp, colors.onPrimary, CircleShape),
            )
        }
    }
}
