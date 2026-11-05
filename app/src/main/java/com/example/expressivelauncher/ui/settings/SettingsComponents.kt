@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.settings

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.SegmentedButton
import com.example.expressivelauncher.ui.components.glassEdge
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.compositionLocalOf
import androidx.graphics.shapes.Morph
import com.example.expressivelauncher.ui.components.MorphShape
import com.example.expressivelauncher.ui.components.staggerIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.IconShape
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.components.HapticGroup
import com.example.expressivelauncher.ui.components.HapticKind
import com.example.expressivelauncher.ui.components.toComposeShape

/** Пружина компонентов настроек: скорость и упругость берутся из настроек анимаций. */
@Composable
private fun <T> tunedSpring(damping: Float, stiffness: Float): androidx.compose.animation.core.SpringSpec<T> {
    val fx = com.example.expressivelauncher.ui.LocalFx.current
    return spring(com.example.expressivelauncher.ui.theme.tunedDamping(damping, fx.bounce), stiffness * fx.speed * fx.speed)
}

// ---------------------------------------------------------------- фигуры

/** Фигуры Material 3 Expressive для иконок разделов, «героя» и цветовых точек. */
enum class Blob { COOKIE_12, COOKIE_9, FLOWER, CLOVER, SUNNY, PENTAGON, GEM, PUFFY }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Blob.shape(): Shape = when (this) {
    Blob.COOKIE_12 -> MaterialShapes.Cookie12Sided.toShape()
    Blob.COOKIE_9 -> MaterialShapes.Cookie9Sided.toShape()
    Blob.FLOWER -> MaterialShapes.Flower.toShape()
    Blob.CLOVER -> MaterialShapes.Clover4Leaf.toShape()
    Blob.SUNNY -> MaterialShapes.Sunny.toShape()
    Blob.PENTAGON -> MaterialShapes.Pentagon.toShape()
    Blob.GEM -> MaterialShapes.Gem.toShape()
    Blob.PUFFY -> MaterialShapes.Puffy.toShape()
}

/** Цветная фигура с содержимым; вращается только фон, содержимое остаётся ровным. */
@Composable
fun BlobBadge(
    shape: Shape,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation() }.clip(shape).background(color))
        content()
    }
}

private fun Color.isLight(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) > 0.6f

// ---------------------------------------------------------------- «герой»

/** Верхняя карточка: показывает, назначен ли лаунчер основным, и вращает фигуру, пока нет. */
@Composable
fun HeroCard(blob: Blob, isDefault: Boolean, onMakeDefault: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isDefault) {
        if (!isDefault) while (true) rotation.animateTo(rotation.value + 360f, tween(16000, easing = LinearEasing))
        else rotation.animateTo(rotation.value + 45f, spring(dampingRatio = 0.5f, stiffness = 120f))
    }
    val container by animateColorAsState(if (isDefault) cs.primaryContainer else cs.tertiaryContainer, label = "heroBg")
    val onContainer = if (isDefault) cs.onPrimaryContainer else cs.onTertiaryContainer
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(panelColor(container))
            .glassEdge(MaterialTheme.shapes.extraLarge, colored = true).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BlobBadge(blob.shape(), if (isDefault) cs.primary else cs.tertiary, 84.dp, rotation = { rotation.value % 360f }) {
                Icon(
                    if (isDefault) Icons.Rounded.Check else Icons.Rounded.Home,
                    null, tint = if (isDefault) cs.onPrimary else cs.onTertiary, modifier = Modifier.size(38.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isDefault) R.string.hero_ready else R.string.hero_almost),
                    style = MaterialTheme.typography.headlineSmallEmphasized, color = onContainer,
                )
                Text(
                    stringResource(if (isDefault) R.string.hero_ready_sub else R.string.hero_almost_sub),
                    style = MaterialTheme.typography.bodyMedium, color = onContainer.copy(alpha = 0.8f),
                )
            }
        }
        AnimatedVisibility(
            visible = !isDefault,
            enter = expandVertically(tunedSpring(0.8f, 400f)) + fadeIn(),
            exit = shrinkVertically(tunedSpring(0.9f, 400f)) + fadeOut(),
        ) {
            // Кнопка среднего размера Expressive (56 dp)
            Button(
                onClick = onMakeDefault,
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.fillMaxWidth().heightIn(ButtonDefaults.MediumContainerHeight).then(with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }),
            ) {
                Text(stringResource(R.string.hero_action), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
}

// ---------------------------------------------------------------- группы и пункты

/**
 * Фон пунктов и карточек настроек. В Liquid Glass — полупрозрачное стекло: настройки лежат на размытых обоях
 * (окно размывает их само), сквозь пункты видно цвет обоев. В остальных темах — сплошной цвет.
 */
@Composable
fun panelColor(base: Color): Color =
    when {
        !com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass -> base
        // Android 13+: под пунктом уже жидкое стекло секции или карточки ([glassEdge]) — лишь лёгкий оттенок
        // Стекло — слоем поверх ([glassEdge]). Нейтральные панели полупрозрачны (фон за ними виден), цветные
        // контейнеры плотнее — на них тёмные подписи «on»-цветов
        // Нейтральная подложка под прозрачным стеклом библиотеки не нужна — его тонировка и даёт фон
        android.os.Build.VERSION.SDK_INT >= 33 && base.isNeutralSurface() && com.example.expressivelauncher.ui.components.LocalWallLiquid.current != null -> Color.Transparent
        android.os.Build.VERSION.SDK_INT >= 33 -> base.copy(alpha = if (base.isNeutralSurface()) 0.42f else 0.75f)
        else -> base.copy(alpha = 0.52f)
    }

/** Нейтральная поверхность темы (не цветной контейнер): у неё светлые подписи, её можно делать прозрачнее. */
@Composable
private fun Color.isNeutralSurface(): Boolean {
    val cs = MaterialTheme.colorScheme
    return this == cs.surfaceBright || this == cs.surface || this == cs.surfaceContainer || this == cs.surfaceContainerLow ||
        this == cs.surfaceContainerHigh || this == cs.surfaceContainerHighest || this == cs.surfaceContainerLowest || this == cs.surfaceVariant
}

/** Морфинг углов при нажатии есть только в темах с выразительными формами. */
@Composable
private fun pressCorner(pressed: Boolean, label: String): Dp {
    val morph = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.morph
    val corner by animateDpAsState(if (pressed && morph) 20.dp else 4.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = label)
    return corner.coerceAtLeast(0.dp)
}

/** Считает секции на странице, чтобы они появлялись друг за другом. */
val LocalSectionCounter = compositionLocalOf { intArrayOf(0) }

/** Группа: крупные скругления снаружи, мелкие между пунктами. */
@Composable
fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    val counter = LocalSectionCounter.current
    val order = remember { counter[0]++ }
    Column(Modifier.staggerIn(order, stepMs = 40L), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                title,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        // Сегментированный список: внешние углы крупные, между пунктами — малые углы и зазор 2 dp.
        // Liquid Glass (Android 13+): одна стеклянная плита, пункты разделены тонкими линиями, как в iOS
        val glassSlab = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass && android.os.Build.VERSION.SDK_INT >= 33
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.largeIncreased)
                .then(if (glassSlab) Modifier.background(panelColor(MaterialTheme.colorScheme.surfaceBright)) else Modifier)
                .glassEdge(MaterialTheme.shapes.largeIncreased),
            verticalArrangement = Arrangement.spacedBy(if (glassSlab) 0.dp else 2.dp),
            content = content,
        )
    }
}

/** Пункт группы: при нажатии углы «набухают» — морфинг формы Expressive. */
@Composable
fun GroupItem(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHaptics.current
    val corner = pressCorner(pressed, "itemCorner")
    val glassSlab = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass && android.os.Build.VERSION.SDK_INT >= 33
    val divider = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Box(
        Modifier.fillMaxWidth()
            .then(
                if (glassSlab) Modifier.drawBehind {
                    // Тонкая линия-разделитель сверху, с отступом под значок пункта
                    val inset = 72.dp.toPx()
                    drawLine(divider, androidx.compose.ui.geometry.Offset(inset, 0f), androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), 0f), strokeWidth = 0.6.dp.toPx())
                } else Modifier.clip(RoundedCornerShape(corner)).background(panelColor(MaterialTheme.colorScheme.surfaceBright))
            )
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = LocalIndication.current) { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onClick() }
                else Modifier
            ),
    ) { content() }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(Modifier.size(40.dp).clip(MaterialShapes.Cookie4Sided.toShape()).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Фигуры значков категорий: каждая категория — своя форма Expressive, они чередуются. */
private val categoryShapes = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Flower,
    MaterialShapes.Pentagon, MaterialShapes.Cookie6Sided, MaterialShapes.Gem, MaterialShapes.Puffy,
    MaterialShapes.SoftBurst, MaterialShapes.Cookie12Sided,
)

@Composable
fun ItemTitle(icon: ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ToggleItem(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val haptics = LocalHaptics.current
    GroupItem(onClick = if (enabled) ({ onChange(!checked) }) else null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f)) { ItemTitle(icon, title, subtitle) }
            Switch(
                checked = checked,
                onCheckedChange = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onChange(it) },
                enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
            )
        }
    }
}

@Composable
fun ActionItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    GroupItem(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f)) { ItemTitle(icon, title, subtitle) }
            if (trailing != null) trailing()
            else Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Пока ползунок тянут, значение локальное; в настройки пишется дросселированно и при отпускании. */
@Composable
fun SliderItem(
    icon: ImageVector,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    steps: Int = 0,
    onCommit: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var last by remember { mutableLongStateOf(0L) }
    val haptics = LocalHaptics.current
    val commit by rememberUpdatedState(onCommit)
    // Состояние ползунка Material 3 Expressive: значение живёт в нём, пока ползунок тянут
    val state = remember(range, steps) { SliderState(value.coerceIn(range), steps, range) }
    val onChange: (Float) -> Unit = { v ->
        dragging = true
        if (steps > 0 && v != state.value) haptics.play(HapticKind.TICK, HapticGroup.TOUCH)
        state.value = v
        val now = android.os.SystemClock.uptimeMillis()
        if (now - last > 90L) { last = now; commit(v) }
    }
    // Пока ползунок тянут, запоздавшее значение из настроек не должно отбрасывать его назад
    LaunchedEffect(value) { if (!dragging) state.value = value.coerceIn(range) }
    val local = state.value
    GroupItem {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(icon)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(format(local), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Slider(
                state = state,
                onValueChange = onChange,
                onValueChangeFinished = { dragging = false; commit(state.value) },
                modifier = Modifier.padding(start = 56.dp),
            )
        }
    }
}

/**
 * Связанная группа кнопок (connected button group) Material 3 Expressive для выбора одного варианта:
 * крайние кнопки скруглены снаружи, выбранная становится «таблеткой».
 */
@Composable
fun <T> ButtonGroup(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val haptics = LocalHaptics.current
    // Тема Lite: стандартные сегментированные кнопки Material 3, без морфинга форм
    if (com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.lite) {
        androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (id, label) ->
                SegmentedButton(
                    selected = id == selected,
                    onClick = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onSelect(id) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { i, (id, label) ->
            val isSel = id == selected
            ToggleButton(
                checked = isSel,
                onCheckedChange = { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onSelect(id) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun <T> ChoiceItem(icon: ImageVector, title: String, subtitle: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    GroupItem {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemTitle(icon, title, subtitle)
            // много вариантов в одну строку не помещаются — по три в ряд
            if (options.size > 3) options.chunked(3).forEach { ButtonGroup(it, selected, onSelect) }
            else ButtonGroup(options, selected, onSelect)
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(cs.secondaryContainer).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = cs.onSecondaryContainer)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer)
    }
}

/** Плитка категории на главной: значок в цветном круге, цвета чередуются (как в настройках Android 16). */
@Composable
fun CategoryTile(icon: ImageVector, title: String, subtitle: String, index: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (index % 3) {
        0 -> cs.primaryContainer to cs.onPrimaryContainer
        1 -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    val pressed by interaction.collectIsPressedAsState()
    val corner = pressCorner(pressed, "tileCorner")
    // При нажатии фигура значка доворачивается и чуть подрастает (только в темах с морфингом)
    val morph = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.morph
    val rot by animateFloatAsState(if (pressed && morph) 30f else 0f, tunedSpring(0.5f, 300f), label = "tileBlobRot")
    val grow by animateFloatAsState(if (pressed && morph) 1.1f else 1f, tunedSpring(0.5f, 500f), label = "tileBlobScale")
    val blob = remember(index) { categoryShapes[index % categoryShapes.size] }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(corner)).background(panelColor(cs.surfaceBright))
            .clickable(interactionSource = interaction, indication = LocalIndication.current) { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BlobBadge(blob.toShape(), bg, 44.dp, Modifier.graphicsLayer { scaleX = grow; scaleY = grow }, rotation = { rot }) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = cs.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- выбор формы иконок

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShapePicker(selected: IconShape, onSelect: (IconShape) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (shape in IconShape.entries) {
            val on = shape == selected
            val scale by animateFloatAsState(if (on) 1f else 0.84f, tunedSpring(0.5f, 500f), label = "shapeScale")
            val rot by animateFloatAsState(if (on) 45f else 0f, tunedSpring(0.6f, 200f), label = "shapeRot")
            BlobBadge(
                shape.toComposeShape(),
                if (on) cs.primary else cs.secondaryContainer,
                58.dp,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onSelect(shape) },
                rotation = { rot },
            ) { if (on) Icon(Icons.Rounded.Check, null, tint = cs.onPrimary) }
        }
    }
}

// ---------------------------------------------------------------- выбор цвета

private val colorDotMorph by lazy { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }

/** Цвет в виде «печеньки» с галочкой, если выбран. */
@Composable
fun ColorDot(color: Color, selected: Boolean, size: Dp = 46.dp, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, tunedSpring(0.5f, 500f), label = "dotScale")
    val rot by animateFloatAsState(if (selected && com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.morph) 40f else 0f, tunedSpring(0.6f, 200f), label = "dotRot")
    // Круг плавно перетекает в «печеньку», а не подменяется
    // В теме Lite форма не морфится: выбранный цвет — тот же круг с галочкой
    val canMorph = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.morph
    val morph by animateFloatAsState(if (selected && canMorph) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "dotMorph")
    BlobBadge(
        remember(morph) { MorphShape(colorDotMorph, morph) },
        color, size,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { haptics.play(HapticKind.CLICK, HapticGroup.TOUCH); onClick() },
        rotation = { rot },
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = if (color.isLight()) Color.Black else Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Полоса с градиентом и бегунком: тап или перетаскивание задают значение 0..1. */
@Composable
fun GradientBar(value: Float, brush: Brush, thumb: Color, onChange: (Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    Box(
        Modifier.fillMaxWidth().height(32.dp).clip(CircleShape).background(brush)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    cb((down.position.x / w).coerceIn(0f, 1f))
                    while (true) {
                        val ch = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        cb((ch.position.x / w).coerceIn(0f, 1f))
                    }
                }
            }
            .drawWithContent {
                drawContent()
                val r = size.height / 2f
                val x = r + (size.width - 2 * r) * value.coerceIn(0f, 1f)
                drawCircle(Color.White, r - 3.dp.toPx(), Offset(x, r))
                drawCircle(thumb, r - 7.dp.toPx(), Offset(x, r))
            },
    )
}

private fun hsv(h: Float, s: Float, v: Float): Color = Color(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var s by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(start[2]) }
    val color = hsv(h, s, v)
    var hex by remember { mutableStateOf(String.format("%06X", color.toArgb() and 0xFFFFFF)) }
    fun syncHex() { hex = String.format("%06X", hsv(h, s, v).toArgb() and 0xFFFFFF) }
    val cs = MaterialTheme.colorScheme
    // Liquid Glass: диалог — стекло (полупрозрачный тон и кромка с бликами на углах)
    val glass = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
    val dialogShape = AlertDialogDefaults.shape
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.glassEdge(dialogShape),
        containerColor = if (glass) cs.surfaceContainerHigh.copy(alpha = 0.86f) else AlertDialogDefaults.containerColor,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BlobBadge(Blob.COOKIE_12.shape(), color, 72.dp) {
                        Text("Aa", color = if (color.isLight()) Color.Black else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { t ->
                            val clean = t.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hex = clean
                            if (clean.length == 6) {
                                val tmp = FloatArray(3)
                                AndroidColor.colorToHSV((0xFF000000 or clean.toLong(16)).toInt(), tmp)
                                h = tmp[0]; s = tmp[1]; v = tmp[2]
                            }
                        },
                        prefix = { Text("#") },
                        singleLine = true,
                        label = { Text("HEX") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(stringResource(R.string.hue), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                GradientBar(h / 360f, remember { Brush.horizontalGradient((0..6).map { hsv(it * 60f, 1f, 1f) }) }, hsv(h, 1f, 1f)) { h = it * 360f; syncHex() }
                Text(stringResource(R.string.saturation), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                GradientBar(s, Brush.horizontalGradient(listOf(hsv(h, 0f, v), hsv(h, 1f, v))), color) { s = it; syncHex() }
                Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                GradientBar(v, Brush.horizontalGradient(listOf(Color.Black, hsv(h, s, 1f))), color) { v = it; syncHex() }
            }
        },
        confirmButton = { Button(onClick = { onPick(color.toArgb() or (0xFF shl 24)) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.done)) } },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Строка «текущий цвет» с кнопкой правки. */
@Composable
fun ColorRow(title: String, color: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    GroupItem(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(color).border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text("#" + String.format("%06X", color.toArgb() and 0xFFFFFF), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            Icon(Icons.Rounded.Edit, null, tint = cs.onSurfaceVariant)
        }
    }
}
