@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.components.blurredWallpaperBackground
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.example.expressivelauncher.LauncherViewModel
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.HomeItem
import com.example.expressivelauncher.data.WidgetHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Виджет внутри ячейки. View берётся из кэша [WidgetHolder]: перелистывание страниц её не пересоздаёт. */
@Composable
fun HomeWidget(item: HomeItem, holder: WidgetHolder, modifier: Modifier = Modifier) {
    // Подложки под виджетом нет ни в одной теме: виджет сам рисует свой фон со своим скруглением,
    // и любая плита под ним торчала из-за углов и краёв (особенно у виджетов необычной формы, как часы Pixel)
    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    val s = com.example.expressivelauncher.ui.LocalSettings.current
    val glint = if (spec.glass && s.glassIconHighlight != com.example.expressivelauncher.data.IconHighlight.OFF)
        GlintSpec(s.glassHighlightStrength, s.glassIconHighlight == com.example.expressivelauncher.data.IconHighlight.FULL) else null
    WidgetView(item, holder, modifier, glint)
}

/**
 * Кромка Liquid Glass по контуру виджета (Android 13+). Пиксель виджета, у которого «снаружи» со стороны
 * света (сверху-слева) прозрачно, — это край фигуры: он светлеет. Ярче у верхнего-левого и нижнего-правого
 * краёв, как блики на значках; режим «полная обводка» светит по всему контуру.
 */
private const val WIDGET_EDGE_AGSL = """
uniform shader content;
uniform float2 size;
uniform float w;
uniform float k;
uniform float full;
// За границей View — пусто: у прямоугольных виджетов край совпадает с границей и тоже должен светиться
float alphaAt(float2 q) {
    if (q.x < 0.0 || q.y < 0.0 || q.x > size.x || q.y > size.y) return 0.0;
    return float(content.eval(q).a);
}

half4 main(float2 p) {
    half4 c = content.eval(p);
    if (c.a < 0.01) return c;
    float a = float(c.a);
    float tl = alphaAt(p + float2(-w, -w));
    float br = alphaAt(p + float2(w, w));
    float tl2 = alphaAt(p + float2(-w * 0.5, -w * 0.5));
    float edgeTL = a * (1.0 - min(tl, tl2));
    float edgeBR = a * (1.0 - br);
    // Как у значков: блик только у верхнего-левого и нижнего-правого углов, к серединам сторон гаснет
    float2 uv = p / max(size, float2(1.0));
    float nearTL = clamp(1.0 - (uv.x + uv.y) * 1.25, 0.0, 1.0);
    float nearBR = clamp((uv.x + uv.y) * 1.25 - 1.25, 0.0, 1.0);
    float g = k * (edgeTL * nearTL * nearTL * 1.1 + edgeBR * nearBR * nearBR * 0.6);
    if (full > 0.5) {
        float l = alphaAt(p + float2(-w, w));
        float r = alphaAt(p + float2(w, -w));
        g = max(g, k * 0.45 * a * (1.0 - min(l, r)));
    }
    g = clamp(g, 0.0, 1.0);
    return half4(c.rgb + half3(g * a) * (1.0 - c.rgb), c.a);
}
"""

@androidx.annotation.RequiresApi(33)
private fun widgetEdgeEffect(spec: GlintSpec, density: Float, width: Int, height: Int): android.graphics.RenderEffect {
    val shader = android.graphics.RuntimeShader(WIDGET_EDGE_AGSL).apply {
        setFloatUniform("size", width.toFloat(), height.toFloat())
        setFloatUniform("w", 1.4f * density)
        setFloatUniform("k", (spec.strength * 1.2f).coerceIn(0f, 1.4f))
        setFloatUniform("full", if (spec.full) 1f else 0f)
    }
    return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
}

/** Блики Liquid Glass поверх виджета; null — без бликов. */
private data class GlintSpec(val strength: Float, val full: Boolean)

/** Скругление фона виджетов в системе (Android 12+), иначе 16 dp: обрезка совпадает с фоном самого виджета. */
@Composable
private fun widgetCorner(): androidx.compose.ui.unit.Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(context, density) {
        val px = if (Build.VERSION.SDK_INT >= 31) {
            runCatching { context.resources.getDimension(android.R.dimen.system_app_widget_background_radius) }.getOrNull()
        } else null
        px?.takeIf { it > 0f }?.let { with(density) { it.toDp() } } ?: 16.dp
    }
}

@Composable
private fun WidgetView(item: HomeItem, holder: WidgetHolder, modifier: Modifier, glint: GlintSpec? = null) {
    val density = LocalDensity.current
    var sizeDp by remember { mutableStateOf(0 to 0) }
    val corner = widgetCorner()
    val cornerPx = with(density) { corner.toPx() }
    AndroidView(
        factory = { ctx -> holder.createView(ctx, item.widgetId) ?: View(ctx) },
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
            .onSizeChanged { sizeDp = with(density) { it.width.toDp().value.toInt() to it.height.toDp().value.toInt() } },
        // View живёт в кэше дольше композиции: при выходе из неё ничего не разрушаем
        onRelease = { },
        update = { v ->
            // Блики по контуру виджета, как у значков: эффект рендера на самом View находит край фигуры
            // по прозрачности его пикселей — у фигурных часов и погоды кромка повторяет фигуру
            if (Build.VERSION.SDK_INT >= 33) {
                // Эффект знает размер View; пересоздаётся, когда меняются блики или размер
                val key = glint?.let { Triple(it, v.width, v.height) }
                if (v.getTag(R.id.widget_glint) != key) {
                    v.setTag(R.id.widget_glint, key)
                    v.setRenderEffect(if (glint != null && v.width > 0 && v.height > 0) widgetEdgeEffect(glint, density.density, v.width, v.height) else null)
                }
            }
            val (w, h) = sizeDp
            if (v is android.appwidget.AppWidgetHostView && w > 0 && h > 0 && holder.shouldSendSize(item.widgetId, w, h)) {
                if (Build.VERSION.SDK_INT >= 31) v.updateAppWidgetSize(Bundle(), listOf(SizeF(w.toFloat(), h.toFloat())))
                else @Suppress("DEPRECATION") v.updateAppWidgetSize(null, w, h, w, h)
            }
        },
    )
}

private class WidgetGroup(val pkg: String, val label: String, val providers: List<Pair<String, AppWidgetProviderInfo>>)

@Composable
fun WidgetPicker(vm: LauncherViewModel, onDismiss: () -> Unit, onPick: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    val groups by produceState(emptyList<WidgetGroup>()) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            vm.widgets.providers()
                .groupBy { it.provider.packageName }
                .map { (pkg, list) ->
                    val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                    WidgetGroup(pkg, label, list.map { it.loadLabel(pm) to it }.sortedBy { it.first.lowercase() })
                }
                .sortedBy { it.label.lowercase() }
        }
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    // Liquid Glass: экран выбора лежит прямо на размытых обоях (лёгкое затемнение для читаемости), а группы —
    // жидкое стекло. Полноэкранная стеклянная панель была бы лишним слоем на каждом кадре прокрутки
    val glassTheme = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
    val accent = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.accentChrome
    val blurUnder = com.example.expressivelauncher.ui.components.glassEnabled()
    val container: @Composable (@Composable () -> Unit) -> Unit = { inner ->
        if (glassTheme) androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().blurredWallpaperBackground(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f), scrim = 0.32f, sharp = true)
                .pointerInput(Unit) { },
        ) {
            // Без Surface цвет текста по умолчанию чёрный: задаём цвет темы, иначе заголовок не виден на тёмном фоне
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) { inner() }
        }
        else if (accent) androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (blurUnder) 0.72f else 0.96f)).pointerInput(Unit) { },
        ) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) { inner() }
        }
        else com.example.expressivelauncher.ui.components.ThemedSurface(androidx.compose.ui.graphics.RectangleShape, MaterialTheme.colorScheme.surfaceContainer, Modifier.fillMaxSize()) { inner() }
    }
    container {
        Column(Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes(), modifier = with(com.example.expressivelauncher.ui.components.GlassControls) { Modifier.control() }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = com.example.expressivelauncher.ui.components.glassContainer(MaterialTheme.colorScheme.secondaryContainer))) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_widgets), style = MaterialTheme.typography.headlineMediumEmphasized)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(groups, key = { it.pkg }) { g ->
                    val open = expanded == g.pkg
                    val motion = MaterialTheme.motionScheme
                    val arrow by animateFloatAsState(if (open) 180f else 0f, motion.defaultSpatialSpec(), label = "arrow")
                    com.example.expressivelauncher.ui.components.ThemedSurface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceBright,
                        overBlur = true,
                        modifier = Modifier.animateItem(),
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().clickable { expanded = if (open) null else g.pkg }.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(g.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        pluralStringResource(R.plurals.widgets_count, g.providers.size, g.providers.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Rounded.ExpandMore, null, Modifier.graphicsLayer { rotationZ = arrow })
                            }
                            AnimatedVisibility(
                                open,
                                enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                                exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for ((label, info) in g.providers) WidgetCard(label, info) { onPick(info) }
                                    Spacer(Modifier.size(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(label: String, info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val preview by produceState<Bitmap?>(null, info) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                // Вписываем с сохранением пропорций: фиксированный размер растягивал предпросмотр
                (info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0))?.let { d ->
                    val iw = d.intrinsicWidth.coerceAtLeast(1)
                    val ih = d.intrinsicHeight.coerceAtLeast(1)
                    val k = minOf(480f / iw, 300f / ih)
                    d.toBitmap((iw * k).toInt().coerceAtLeast(1), (ih * k).toInt().coerceAtLeast(1))
                }
            }.getOrNull()
        }
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 160.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                preview?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.heightIn(max = 160.dp)) }
            }
            Text(label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
