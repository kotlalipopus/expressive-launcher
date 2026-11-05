@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.ColorSource
import com.example.expressivelauncher.data.GlassIconStyle
import com.example.expressivelauncher.data.IconHighlight
import com.example.expressivelauncher.data.LauncherStyle
import com.example.expressivelauncher.data.Settings
import com.example.expressivelauncher.data.ThemeMode
import com.example.expressivelauncher.ui.components.glassEdge
import com.example.expressivelauncher.ui.theme.LauncherThemes
import com.example.expressivelauncher.ui.theme.LocalThemeSpec
import com.example.expressivelauncher.ui.theme.SeedColors
import com.example.expressivelauncher.ui.theme.ThemeOption
import com.example.expressivelauncher.ui.theme.ThemeSpec
import com.example.expressivelauncher.ui.theme.rememberThemeSwitch
import kotlin.math.roundToInt

/** Динамические цвета из обоев есть только на Android 12+. */
val dynamicColorSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Страница «Темы»: выбор темы, её собственные настройки и светлый/тёмный режим. */
@Composable
fun ThemesPage(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val switch = rememberThemeSwitch()
    SettingsSection(stringResource(R.string.themes_choose)) {
        ThemeCards(s.style, s) { spec -> switch { vm.updateSettings { it.copy(style = spec.style) } } }
    }
    ThemeOptionsSection(vm, LauncherThemes.of(s.style))
    SettingsSection(stringResource(R.string.appearance_mode)) { ThemeModeItem(vm) }
}

/** Карточки трёх тем с живым мини-столом в каждой; выбранная отмечена. */
@Composable
fun ThemeCards(selected: LauncherStyle, s: Settings, previewHeight: Dp = 150.dp, onSelect: (ThemeSpec) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (spec in LauncherThemes.all) {
            ThemeCard(spec, spec.style == selected, s, previewHeight) { onSelect(spec) }
        }
    }
}

@Composable
private fun ThemeCard(spec: ThemeSpec, selected: Boolean, s: Settings, previewHeight: Dp, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val current = LocalThemeSpec.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Морфинг карточки при нажатии и выборе — только в темах, где он есть
    val corner by animateDpAsState(
        when {
            !current.morph -> 16.dp
            pressed -> 40.dp
            selected -> 32.dp
            else -> 24.dp
        },
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "themeCardCorner",
    )
    val scale by animateFloatAsState(if (pressed && current.morph) 0.97f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "themeCardScale")
    val shape = RoundedCornerShape(corner.coerceAtLeast(0.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(panelColor(if (selected) cs.primaryContainer else cs.surfaceBright))
            .glassEdge(shape, colored = true)
            .border(if (selected) 3.dp else 0.dp, if (selected) cs.primary else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onClick() }
            .padding(10.dp),
    ) {
        MiniDesktop(spec, s, Modifier.fillMaxWidth().height(previewHeight))
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(spec.icon, null, tint = if (selected) cs.onPrimaryContainer else cs.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(spec.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                )
                Text(
                    stringResource(spec.tagline), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (selected) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant,
                )
            }
            if (selected) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Мини-рабочий стол в стиле темы [spec]: «обои», часы, ряд значков и док. Цвета — текущей схемы,
 * поэтому превью меняется вместе с режимом и акцентом. [s] даёт сетку, подписи и док.
 */
@Composable
fun MiniDesktop(spec: ThemeSpec, s: Settings, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val outer = when {
        spec.uniformCorner != null -> RoundedCornerShape(spec.uniformCorner)
        spec.glass -> RoundedCornerShape(28.dp)
        else -> RoundedCornerShape(24.dp)
    }
    val wallpaper = Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer))
    Box(modifier.clip(outer).background(wallpaper)) {
        if (spec.glass) {
            // Мягкие пятна «обоев» под стеклом: размытие видно по ним
            Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 10.dp).size(70.dp).clip(CircleShape).background(cs.tertiary.copy(alpha = 0.55f)))
            Box(Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(54.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.45f)))
        }
        MiniClock(spec, Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 10.dp))
        val columns = s.columns.coerceIn(3, 7)
        val glyphs = listOf(Icons.Rounded.Call, Icons.Rounded.Email, Icons.Rounded.PhotoCamera, Icons.Rounded.MusicNote, Icons.Rounded.Map, Icons.AutoMirrored.Rounded.Chat, Icons.Rounded.Palette)
        val iconSize = (30f * s.iconScale * 5f / columns.coerceAtLeast(4)).coerceIn(18f, 36f).dp
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (i in 0 until columns) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiniIcon(spec, i, glyphs[i % glyphs.size], iconSize)
                        if (s.showHomeLabels) {
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.size(iconSize * 0.8f, 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                        }
                    }
                }
            }
            if (s.showDock) MiniDock(spec, columns.coerceAtMost(5), iconSize, glyphs)
        }
    }
}

@Composable
private fun MiniClock(spec: ThemeSpec, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    when {
        spec.glass -> Text("9:41", modifier, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
        spec.boldHeadlines -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary), contentAlignment = Alignment.Center) {
                Text("9", color = cs.onPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.size(4.dp))
            Box(Modifier.size(34.dp).clip(MaterialShapes.Clover4Leaf.toShape()).background(cs.tertiary), contentAlignment = Alignment.Center) {
                Text("41", color = cs.onTertiary, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        else -> Text("9:41", modifier, color = cs.onPrimaryContainer, fontSize = 26.sp, fontWeight = FontWeight.Normal)
    }
}

/** Значок превью: стеклянная плашка, выразительная форма или простой круг — по теме. */
@Composable
private fun MiniIcon(spec: ThemeSpec, index: Int, glyph: ImageVector, size: Dp) {
    val cs = MaterialTheme.colorScheme
    val expressiveShapes = remember {
        listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Pill, MaterialShapes.Flower, MaterialShapes.Sunny, MaterialShapes.Cookie12Sided, MaterialShapes.Puffy)
    }
    val shape: Shape
    val bg: Color
    val fg: Color
    when {
        spec.glass -> { shape = RoundedCornerShape(size * 0.3f); bg = Color.White.copy(alpha = 0.22f); fg = Color.White }
        // Lite: как значки стокового Pixel — белые круги с цветным рисунком
        spec.lite -> {
            shape = CircleShape; bg = Color.White
            fg = listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFF34A853), Color(0xFFFBBC05))[index % 4]
        }
        else -> {
            shape = expressiveShapes[index % expressiveShapes.size].toShape()
            val pal = listOf(cs.primary to cs.onPrimary, cs.secondaryContainer to cs.onSecondaryContainer, cs.tertiary to cs.onTertiary)
            val (b, f) = pal[index % pal.size]
            bg = b; fg = f
        }
    }
    Box(
        Modifier.size(size).clip(shape).background(bg)
            .then(if (spec.glass) Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(glyph, null, tint = fg, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun MiniDock(spec: ThemeSpec, count: Int, iconSize: Dp, glyphs: List<ImageVector>) {
    val cs = MaterialTheme.colorScheme
    val shape = when {
        spec.uniformCorner != null -> RoundedCornerShape(spec.uniformCorner)
        else -> RoundedCornerShape(50)
    }
    val bg = when {
        spec.glass -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.16f)))
        // Lite: у дока нет панели, значки прямо на обоях — как на рабочем столе в этой теме
        spec.lite -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        else -> Brush.verticalGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainerHigh))
    }
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg)
            .then(if (spec.glass) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), shape) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (i in 0 until count) MiniIcon(spec, i + 3, glyphs[(i + 3) % glyphs.size], iconSize * 0.85f)
    }
}

/** Светлый / тёмный / как в системе. Меняется с кросс-фейдом. */
@Composable
fun ThemeModeItem(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val switch = rememberThemeSwitch()
    ChoiceItem(
        Icons.Rounded.Contrast, stringResource(R.string.appearance_mode), null,
        listOf(
            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
            ThemeMode.LIGHT to stringResource(R.string.theme_light),
            ThemeMode.DARK to stringResource(R.string.theme_dark),
        ),
        s.themeMode,
    ) { v -> switch { vm.updateSettings { it.copy(themeMode = v) } } }
}

/** «Динамические цвета из обоев» и свой акцент, если они выключены или недоступны (Android ниже 12). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicColorItems(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val switch = rememberThemeSwitch()
    if (dynamicColorSupported) {
        ToggleItem(
            Icons.Rounded.Wallpaper, stringResource(R.string.dynamic_colors), stringResource(R.string.dynamic_colors_hint),
            s.colorSource == ColorSource.WALLPAPER,
        ) { on -> switch { vm.updateSettings { it.copy(colorSource = if (on) ColorSource.WALLPAPER else ColorSource.CUSTOM) } } }
    }
    if (s.colorSource == ColorSource.CUSTOM || !dynamicColorSupported) {
        GroupItem {
            Column(Modifier.padding(16.dp)) {
                ItemTitle(Icons.Rounded.Palette, stringResource(R.string.accent_color), null)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in SeedColors) {
                        ColorDot(Color(c), s.seedColor == c && s.colorSource == ColorSource.CUSTOM, size = 40.dp) {
                            switch { vm.updateSettings { it.copy(seedColor = c, colorSource = ColorSource.CUSTOM) } }
                        }
                    }
                }
            }
        }
    }
}

/** Настройки, которые есть у выбранной темы. */
@Composable
fun ThemeOptionsSection(vm: LauncherViewModel, spec: ThemeSpec) {
    val s by vm.settings.collectAsState()
    fun upd(f: (Settings) -> Settings) = vm.updateSettings(f)
    SettingsSection(stringResource(R.string.theme_settings_of, stringResource(spec.title))) {
        LauncherWallpaperItems(vm)
        if (ThemeOption.BLUR_STRENGTH in spec.options) {
            SliderItem(Icons.Rounded.BlurOn, stringResource(R.string.blur_strength), s.blurStrength, 0.25f..2f, { "${(it * 100).roundToInt()}%" }, steps = 6) { v ->
                upd { it.copy(blurStrength = v) }
            }
            if (Build.VERSION.SDK_INT < 31) InfoItem(stringResource(R.string.glass_no_blur_old_android))
        }
        if (ThemeOption.GLASS_OPACITY in spec.options) {
            // Показываем прозрачность, храним плотность тонировки
            SliderItem(Icons.Rounded.Opacity, stringResource(R.string.glass_transparency), 1f - s.glassOpacity, 0.2f..0.85f, { "${(it * 100).roundToInt()}%" }, steps = 12) { v ->
                upd { it.copy(glassOpacity = 1f - v) }
            }
        }
        if (ThemeOption.DYNAMIC_COLOR in spec.options || ThemeOption.ACCENT in spec.options) DynamicColorItems(vm)
        if (ThemeOption.ANIM_INTENSITY in spec.options) {
            SliderItem(Icons.Rounded.Animation, stringResource(R.string.anim_intensity), s.animBounce, 0f..2f, { "${(it * 100).roundToInt()}%" }, steps = 7) { v ->
                upd { it.copy(animBounce = v) }
            }
        }
        if (spec.lite) InfoItem(stringResource(R.string.theme_lite_info))
    }
    if (ThemeOption.GLASS_ICONS in spec.options) {
        // Отдельный раздел «Иконки»: блики, их сила, стиль, маска и набор значков
        SettingsSection(stringResource(R.string.glass_icons_section)) {
            ChoiceItem(
                Icons.Rounded.WaterDrop, stringResource(R.string.icon_highlights), stringResource(R.string.icon_highlights_hint),
                listOf(
                    IconHighlight.OFF to stringResource(R.string.highlight_off),
                    IconHighlight.CORNERS to stringResource(R.string.highlight_corners),
                    IconHighlight.FULL to stringResource(R.string.highlight_full),
                ),
                s.glassIconHighlight,
            ) { v -> upd { it.copy(glassIconHighlight = v) } }
            if (s.glassIconHighlight != IconHighlight.OFF) {
                SliderItem(Icons.Rounded.WaterDrop, stringResource(R.string.highlight_strength), s.glassHighlightStrength, 0.2f..1.2f, { "${(it * 100).roundToInt()}%" }, steps = 9) { v ->
                    upd { it.copy(glassHighlightStrength = v) }
                }
            }
            ChoiceItem(
                Icons.Rounded.Palette, stringResource(R.string.glass_icon_style), null,
                listOf(
                    GlassIconStyle.NORMAL to stringResource(R.string.icon_style_normal),
                    GlassIconStyle.CLEAR to stringResource(R.string.icon_style_clear),
                    GlassIconStyle.TINTED to stringResource(R.string.icon_style_tinted),
                ),
                s.glassIconStyle,
            ) { v -> upd { it.copy(glassIconStyle = v) } }
            GroupItem {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        stringResource(R.string.icon_mask), Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    )
                    ShapePicker(s.glassIconShape) { v -> upd { it.copy(glassIconShape = v) } }
                }
            }
            IconPackPicker(vm)
        }
    }
}

@Composable
private fun InfoItem(text: String) {
    GroupItem { Box(Modifier.padding(12.dp)) { InfoCard(Icons.Rounded.Palette, text) } }
}

/**
 * Выбор набора значков (icon pack формата ADW / Nova / Lawnchair, appfilter.xml). Наборы ищутся среди
 * установленных приложений; значков Apple в лаунчере нет — набор в стиле iOS 26 ставится из Google Play.
 * Приложения, которых нет в наборе, получают обычный значок с маской темы.
 */
@Composable
fun IconPackPicker(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val themes by vm.iconThemes.collectAsState()
    val repo = com.example.expressivelauncher.ui.LocalRepo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { repo.refreshThemes() }
    val packs = themes.filter { it.id.startsWith("pack:") }
    fun select(id: String) = vm.updateSettings { it.copy(iconTheme = id) }
    GroupItem(onClick = { select("") }) {
        PackRow(stringResource(R.string.icon_theme_system), s.iconTheme.isEmpty())
    }
    for (p in packs) {
        GroupItem(onClick = { select(p.id) }) { PackRow(p.name, s.iconTheme == p.id) }
    }
    ActionItem(
        Icons.Rounded.Search, stringResource(R.string.icon_pack_find), stringResource(R.string.icon_pack_find_hint),
        onClick = {
            val q = android.net.Uri.encode("iOS 26 icon pack")
            val market = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=$q&c=apps"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            val web = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/search?q=$q&c=apps"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(market) }.onFailure { runCatching { context.startActivity(web) } }
        },
    )
}

@Composable
private fun PackRow(name: String, selected: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (selected) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * «Обои лаунчера»: своя картинка, которую лаунчер рисует сам. Только её жидкое стекло может преломлять:
 * пикселей живых обоев приложению система не даёт. Картинка выбирается системным выбором фото —
 * без разрешений на доступ к файлам.
 */
@Composable
fun LauncherWallpaperItems(vm: LauncherViewModel) {
    val s by vm.settings.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val version = com.example.expressivelauncher.ui.components.CustomWallpaper.version
    val hasFile = remember(version) { com.example.expressivelauncher.ui.components.CustomWallpaper.hasFile(context) }
    val failed = stringResource(R.string.custom_wallpaper_failed)
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            if (com.example.expressivelauncher.ui.components.CustomWallpaper.save(context, uri)) {
                vm.updateSettings { it.copy(customWallpaper = true) }
            } else {
                com.example.expressivelauncher.ui.components.LauncherToast.show(failed)
            }
        }
    }
    val launchPicker = {
        pick.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    ToggleItem(
        Icons.Rounded.Wallpaper, stringResource(R.string.custom_wallpaper), stringResource(R.string.custom_wallpaper_hint),
        s.customWallpaper && hasFile,
    ) { on ->
        if (on && !hasFile) launchPicker() else vm.updateSettings { it.copy(customWallpaper = on) }
    }
    ActionItem(Icons.Rounded.Palette, stringResource(R.string.custom_wallpaper_pick), stringResource(R.string.custom_wallpaper_pick_hint), onClick = launchPicker)
    if (hasFile && s.customWallpaper) {
        // Правка своих обоев: затемнение, размытие (запекается в картинку — стекло видит то же), листание
        SliderItem(Icons.Rounded.Opacity, stringResource(R.string.wallpaper_dim), s.wallpaperDim, 0f..0.7f, { "${(it * 100).roundToInt()}%" }) { v ->
            vm.updateSettings { it.copy(wallpaperDim = v) }
        }
        SliderItem(Icons.Rounded.BlurOn, stringResource(R.string.wallpaper_blur), s.wallpaperBlur, 0f..1f, { "${(it * 100).roundToInt()}%" }, steps = 9) { v ->
            vm.updateSettings { it.copy(wallpaperBlur = v) }
        }
        ToggleItem(Icons.Rounded.Wallpaper, stringResource(R.string.wallpaper_parallax), stringResource(R.string.wallpaper_parallax_hint), s.wallpaperParallax) { on ->
            vm.updateSettings { it.copy(wallpaperParallax = on) }
        }
    }
    if (hasFile) {
        // Системные обои = фото лаунчера: при закрытии приложения система рисует свои обои за сжимающимся окном,
        // и если они другие (например, живые), они мелькают на долю секунды. Только рабочий стол, не экран блокировки
        val done = stringResource(R.string.custom_wallpaper_to_system_done)
        val notSet = stringResource(R.string.custom_wallpaper_to_system_failed)
        ActionItem(
            Icons.Rounded.Wallpaper, stringResource(R.string.custom_wallpaper_to_system), stringResource(R.string.custom_wallpaper_to_system_hint),
            onClick = {
                scope.launch {
                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            com.example.expressivelauncher.ui.components.CustomWallpaper.file(context).inputStream().use {
                                android.app.WallpaperManager.getInstance(context).setStream(it, null, true, android.app.WallpaperManager.FLAG_SYSTEM)
                            }
                        }.isSuccess
                    }
                    com.example.expressivelauncher.ui.components.LauncherToast.show(if (ok) done else notSet)
                }
            },
        )
    }
}
