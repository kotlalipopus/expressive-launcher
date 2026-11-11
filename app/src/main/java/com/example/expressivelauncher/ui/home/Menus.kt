@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.theme.accentColors
import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import com.example.expressivelauncher.ui.components.staggerIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.ItemType
import com.example.expressivelauncher.data.ShortcutEntry
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.LocalFx
import com.example.expressivelauncher.ui.LocalRepo
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.components.HapticGroup
import com.example.expressivelauncher.ui.components.HapticKind
import com.example.expressivelauncher.ui.components.GlassPanel
import com.example.expressivelauncher.ui.components.ThemedSurface
import com.example.expressivelauncher.ui.components.accentGlow
import kotlin.math.roundToInt

/** Размещает содержимое рядом с [anchor] (координаты окна): под ним, а если не помещается — над ним. */
@Composable
fun AnchoredBox(anchor: Rect, content: @Composable () -> Unit) {
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }
    val margin = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.roundToPx() }
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val placeable = measurables.first().measure(
            Constraints(maxWidth = constraints.maxWidth - 2 * margin, maxHeight = constraints.maxHeight - 2 * margin)
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = (anchor.center.x - placeable.width / 2f).roundToInt()
                .coerceIn(margin, (constraints.maxWidth - placeable.width - margin).coerceAtLeast(margin))
            val below = anchor.bottom + gap + placeable.height <= constraints.maxHeight - margin * 4
            val y = (if (below) anchor.bottom + gap else anchor.top - gap - placeable.height).roundToInt().coerceAtLeast(margin)
            placeable.place(x, y)
        }
    }
}

/** Меню вырастает из элемента, к которому привязано, и сворачивается обратно; [target] == null — закрыто. */
@Composable
fun <T : Any> AnimatedAnchoredMenu(target: T?, anchor: (T) -> Rect, screen: Size, content: @Composable (T) -> Unit) {
    var last by remember { mutableStateOf<T?>(null) }
    if (target != null) last = target
    val shown = last ?: return
    val a = anchor(shown)
    val origin = TransformOrigin((a.center.x / screen.width).coerceIn(0f, 1f), (a.center.y / screen.height).coerceIn(0f, 1f))
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = target != null,
        // Раскрывается из значка с лёгкой пружиной; закрывается коротко и без отскока — к нему уже не вернутся
        enter = scaleIn(motion.defaultSpatialSpec(), initialScale = 0.6f, transformOrigin = origin) + fadeIn(motion.fastEffectsSpec()),
        exit = scaleOut(androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutLinearInEasing), targetScale = 0.86f, transformOrigin = origin) +
            fadeOut(androidx.compose.animation.core.tween(140)),
    ) {
        AnchoredBox(a) { content(shown) }
    }
}

/** Пункт меню. [custom] — произвольная строка вместо обычного пункта (например, изменение размера виджета). */
private class MenuEntry(
    val text: String = "",
    val icon: ImageVector? = null,
    val image: ImageBitmap? = null,
    val destructive: Boolean = false,
    val custom: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit = {},
)

/**
 * Меню Material 3 Expressive: пункты разбиты на группы, группы стоят отдельными контейнерами с зазором,
 * у крайних групп внешние углы крупнее. Пункты — обычные значки и подписи, без цветных подложек.
 */
@Composable
private fun ExpressiveMenu(groups: List<List<MenuEntry>>) {
    val filled = groups.filter { it.isNotEmpty() }
    val haptics = LocalHaptics.current
    val cs = MaterialTheme.colorScheme
    Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 200.dp, max = 280.dp), verticalArrangement = Arrangement.spacedBy(MenuGroupGap)) {
        filled.forEachIndexed { gi, entries ->
            // Liquid Glass — стеклянные группы меню; в остальных темах сплошная поверхность по спецификации
            ThemedSurface(
                shape = menuGroupShape(gi, filled.size),
                color = cs.surfaceContainerLow,
                contentColor = cs.onSurface,
                shadowElevation = 3.dp,
                blurPanel = false,
                modifier = Modifier.fillMaxWidth().accentGlow(menuGroupShape(gi, filled.size)),
            ) {
                Column(Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
                    entries.forEachIndexed { i, e ->
                        if (e.custom != null) e.custom.invoke()
                        else {
                            val tint = if (e.destructive) cs.error else cs.onSurfaceVariant
                            DropdownMenuItem(
                                text = { Text(e.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); e.onClick() },
                                shape = menuItemShape(i, entries.size),
                                leadingIcon = {
                                    when {
                                        e.image != null -> Image(e.image, null, Modifier.size(24.dp).clip(CircleShape))
                                        e.icon != null -> Icon(e.icon, null, tint = tint)
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (e.destructive) cs.error else cs.onSurface,
                                    leadingIconColor = tint,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Зазор между группами меню. */
private val MenuGroupGap = 4.dp

/** Группа меню: снаружи крупные скругления 24 dp, на стыках между группами — 10 dp. */
private fun menuGroupShape(index: Int, count: Int): RoundedCornerShape {
    val big = 24.dp; val small = 10.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Пункт меню: подсветка нажатия повторяет форму группы, крайние пункты скруглены сильнее. */
private fun menuItemShape(index: Int, count: Int): RoundedCornerShape {
    val big = 20.dp; val small = 8.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Ярлыки приложения (долгое нажатие): загружаются в фоне, до загрузки группы нет. */
@Composable
private fun shortcutEntries(app: AppInfo, onShortcut: (ShortcutInfo) -> Unit, onDismiss: () -> Unit): List<MenuEntry> {
    val repo = LocalRepo.current
    val shortcuts by produceState(emptyList<ShortcutEntry>(), app.key) { value = repo.shortcuts(app) }
    return shortcuts.map { s ->
        MenuEntry(s.label, icon = Icons.AutoMirrored.Rounded.OpenInNew, image = s.icon) { onShortcut(s.info); onDismiss() }
    }
}

/** Свой значок для одного приложения: сменить и вернуть обратно. */
@Composable
private fun iconOverrideEntries(app: AppInfo, onChangeIcon: (AppInfo) -> Unit, onDismiss: () -> Unit): List<MenuEntry> {
    val repo = LocalRepo.current
    return listOfNotNull(
        MenuEntry(stringResource(R.string.menu_customize), Icons.Rounded.Tune) { AppCustomize.app = app; onDismiss() },
        // «Сменить значок» и «Вернуть значок» — внутри «Настроить значок», в меню не дублируются
    )
}

/** Меню элемента на рабочем столе. */
@Composable
fun ItemMenu(
    item: HomeItem,
    app: AppInfo?,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onLaunchShortcut: (ShortcutInfo) -> Unit,
    onChangeIcon: (AppInfo) -> Unit,
    onSelect: (() -> Unit)? = null,
) {
    val repo = LocalRepo.current
    val shortcuts = if (item.type == ItemType.APP && app != null) shortcutEntries(app, onLaunchShortcut, onDismiss) else emptyList()
    val overrides = if (item.type == ItemType.APP && app != null) iconOverrideEntries(app, onChangeIcon, onDismiss) else emptyList()
    val main = buildList {
        when (item.type) {
            ItemType.APP -> if (app != null) {
                add(MenuEntry(stringResource(R.string.menu_app_info), Icons.Rounded.Info) { repo.openAppInfo(app, null); onDismiss() })
                if (!item.inDock) add(MenuEntry(stringResource(R.string.menu_to_dock), Icons.Rounded.Home) { vm.moveToDock(item.id); onDismiss() })
                addAll(overrides)
            }
            ItemType.FOLDER -> add(MenuEntry(stringResource(R.string.menu_open), Icons.AutoMirrored.Rounded.OpenInNew) { onOpenFolder(item.id); onDismiss() })
            ItemType.WIDGET -> {
                val w = stringResource(R.string.widget_width)
                val h = stringResource(R.string.widget_height)
                add(MenuEntry(custom = { ResizeRow(w, { vm.resizeWidget(item.id, -1, 0) }, { vm.resizeWidget(item.id, 1, 0) }) }))
                add(MenuEntry(custom = { ResizeRow(h, { vm.resizeWidget(item.id, 0, -1) }, { vm.resizeWidget(item.id, 0, 1) }) }))
            }
            ItemType.CLOCK -> Unit
        }
        if (onSelect != null && (item.type == ItemType.APP || item.type == ItemType.FOLDER)) {
            add(MenuEntry(stringResource(R.string.menu_select_more), Icons.Rounded.SelectAll) { onDismiss(); onSelect() })
        }
    }
    // Необратимые действия — отдельной группой в конце, цветом ошибки
    val danger = buildList {
        if (item.type == ItemType.APP && app != null && !app.isSystem) {
            add(MenuEntry(stringResource(R.string.menu_uninstall), Icons.Rounded.Delete, destructive = true) { repo.uninstall(app); onDismiss() })
        }
        add(MenuEntry(stringResource(R.string.menu_remove), Icons.Rounded.Close, destructive = true) { vm.removeItem(item.id); onDismiss() })
    }
    ExpressiveMenu(listOf(shortcuts, main, danger))
}

@Composable
private fun ResizeRow(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        FilledTonalIconButton(onClick = onMinus, shapes = IconButtonDefaults.shapes(), modifier = with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = com.example.expressivelauncher.ui.components.glassContainer(MaterialTheme.colorScheme.secondaryContainer))) { Icon(Icons.Rounded.Remove, null) }
        Spacer(Modifier.size(4.dp))
        FilledTonalIconButton(onClick = onPlus, shapes = IconButtonDefaults.shapes(), modifier = with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = com.example.expressivelauncher.ui.components.glassContainer(MaterialTheme.colorScheme.secondaryContainer))) { Icon(Icons.Rounded.Add, null) }
    }
}

/** Меню приложения в шторке. */
@Composable
fun DrawerAppMenu(
    app: AppInfo,
    alreadyOnHome: Boolean,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
    onAdded: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onChangeIcon: (AppInfo) -> Unit,
    onSelect: () -> Unit,
) {
    val repo = LocalRepo.current
    val shortcuts = shortcutEntries(app, onShortcut, onDismiss)
    val overrides = iconOverrideEntries(app, onChangeIcon, onDismiss)
    val main = buildList {
        if (!alreadyOnHome) add(MenuEntry(stringResource(R.string.menu_add_home), Icons.Rounded.Home) { vm.addApp(app); onAdded() })
        add(MenuEntry(stringResource(R.string.menu_select_more), Icons.Rounded.SelectAll) { onDismiss(); onSelect() })
        add(MenuEntry(stringResource(R.string.menu_hide), Icons.Rounded.VisibilityOff) {
            vm.updateSettings { it.copy(hiddenApps = it.hiddenApps + app.key) }; onDismiss()
        })
        addAll(overrides)
        add(MenuEntry(stringResource(R.string.menu_app_info), Icons.Rounded.Info) { repo.openAppInfo(app, null); onDismiss() })
    }
    val danger = if (!app.isSystem) {
        listOf(MenuEntry(stringResource(R.string.menu_uninstall), Icons.Rounded.Delete, destructive = true) { repo.uninstall(app); onDismiss() })
    } else emptyList()
    ExpressiveMenu(listOf(shortcuts, main, danger))
}

/** Сколько после открытия меню касание фона ещё не закрывает его. */
const val MENU_GUARD_MS = 350L

/**
 * Меню долгого нажатия на пустом месте — по приёмам Expressive: одно главное действие (виджеты) крупнее
 * и залито основным цветом, остальные тональные и круглые — контраст цвета и формы задаёт иерархию.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityScope.HomeMenu(onDismiss: () -> Unit, onWidgets: () -> Unit, onWallpaper: () -> Unit, onSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    // Меню появляется, пока палец ещё на экране: случайное касание ладонью или вторым пальцем
    // в первые мгновения не должно его закрывать
    val openedAt = remember { android.os.SystemClock.uptimeMillis() }
    Box(
        Modifier.fillMaxSize().background(cs.scrim.copy(alpha = if (spec.accentChrome) 0.45f else 0.32f)).clickable(interactionSource = null, indication = null) {
            if (android.os.SystemClock.uptimeMillis() - openedAt > MENU_GUARD_MS) onDismiss()
        },
    )
    val slide = Modifier.animateEnterExit(
        enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
        exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it },
    )
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars), contentAlignment = Alignment.BottomCenter) {
        if (spec.accentChrome) {
            AccentHomeMenu(onWidgets, onWallpaper, onSettings, Modifier.padding(horizontal = 12.dp, vertical = 4.dp).widthIn(max = 480.dp).fillMaxWidth().then(slide))
            return@Box
        }
        ThemedSurface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = cs.surfaceContainer,
            contentColor = cs.onSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.padding(16.dp).fillMaxWidth().then(slide),
        ) {
            // Liquid Glass: три одинаковые стеклянные плитки, как в iOS; в Lite главная — крупнее и залита цветом
            val glassTheme = spec.glass
            Row(Modifier.padding(12.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeMenuTile(
                    Icons.Rounded.Widgets, stringResource(R.string.home_widgets), cs.primary, cs.onPrimary,
                    rest = 28.dp, pressed = 16.dp, emphasized = !glassTheme, onClick = onWidgets,
                    modifier = Modifier.weight(if (glassTheme) 1f else 1.4f).fillMaxHeight().staggerIn(0),
                )
                HomeMenuTile(
                    Icons.Rounded.Wallpaper, stringResource(R.string.home_wallpaper), cs.secondaryContainer, cs.onSecondaryContainer,
                    rest = 28.dp, pressed = 16.dp, emphasized = false, onClick = onWallpaper,
                    modifier = Modifier.weight(1f).fillMaxHeight().staggerIn(1),
                )
                HomeMenuTile(
                    Icons.Rounded.Settings, stringResource(R.string.home_settings), cs.tertiaryContainer, cs.onTertiaryContainer,
                    rest = 28.dp, pressed = 16.dp, emphasized = false, onClick = onSettings,
                    modifier = Modifier.weight(1f).fillMaxHeight().staggerIn(2),
                )
            }
        }
    }
}

/**
 * Меню стола в Expressive: насыщенная панель тёмного тона акцента со свечением, внутри — три кнопки во всю
 * ширину, собранные в группу (снаружи крупные углы 28 dp, на стыках 8 dp, как у сгруппированных кнопок M3
 * Expressive). Кнопка под пальцем скругляется сильнее — морфинг формы. Размеры — как у кнопки Medium/Large:
 * высота 72 dp, значок 28 dp, подпись titleLarge.
 */
@Composable
private fun AccentHomeMenu(onWidgets: () -> Unit, onWallpaper: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    val ac = accentColors()
    val shape = RoundedCornerShape(36.dp)
    val s = LocalSettings.current
    val blurUnder = s.blurEverywhere && s.blurBackground && s.blurStrength > 0f && LocalFx.current.blur && android.os.Build.VERSION.SDK_INT >= 31
    Box(
        modifier
            .accentGlow(shape)
            .clip(shape)
            // Полупрозрачная — только поверх размытия («Размытие везде»), иначе сквозь неё читался бы док
            .background(ac.panel.copy(alpha = if (blurUnder) 0.78f else 0.97f))
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val items = listOf(
                Triple(Icons.Rounded.Widgets, R.string.home_widgets, onWidgets),
                Triple(Icons.Rounded.Wallpaper, R.string.home_wallpaper, onWallpaper),
                Triple(Icons.Rounded.Settings, R.string.home_settings, onSettings),
            )
            items.forEachIndexed { i, (icon, label, click) ->
                AccentMenuButton(icon, stringResource(label), i, items.size, click, Modifier.staggerIn(i))
            }
        }
    }
}

@Composable
private fun AccentMenuButton(icon: ImageVector, text: String, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier) {
    val ac = accentColors()
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val haptics = LocalHaptics.current
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.Dp>()
    val outer = 28.dp; val inner = 8.dp; val pressedCorner = 36.dp
    val top by animateDpAsState(if (isPressed) pressedCorner else if (index == 0) outer else inner, spatial, label = "top")
    val bottom by animateDpAsState(if (isPressed) pressedCorner else if (index == count - 1) outer else inner, spatial, label = "bottom")
    Surface(
        onClick = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onClick() },
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = ac.field,
        contentColor = ac.onField,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().height(72.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Плитка меню: при нажатии углы сжимаются (морфинг формы Expressive). */
@Composable
private fun HomeMenuTile(
    icon: ImageVector, text: String,
    container: Color, onContainer: Color,
    rest: androidx.compose.ui.unit.Dp, pressed: androidx.compose.ui.unit.Dp, emphasized: Boolean,
    onClick: () -> Unit, modifier: Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val haptics = LocalHaptics.current
    val corner by animateDpAsState(if (isPressed) pressed else rest, MaterialTheme.motionScheme.fastSpatialSpec(), label = "tileCorner")
    val glass = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
    // В Liquid Glass плитки — стекло поверх стеклянной панели меню: обои не подкладываются второй раз
    ThemedSurface(
        onClick = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onClick() },
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = if (glass) MaterialTheme.colorScheme.onSurface else onContainer,
        interactionSource = source,
        overBlur = true,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, modifier = Modifier.size(if (emphasized) 32.dp else 24.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                text,
                style = if (emphasized) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
