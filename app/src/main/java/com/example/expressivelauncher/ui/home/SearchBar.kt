package com.example.expressivelauncher.ui.home

import com.example.expressivelauncher.ui.theme.accentColors
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expressivelauncher.R
import com.example.expressivelauncher.data.AppInfo
import com.example.expressivelauncher.ui.LocalHaptics
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.components.AppIconImage
import com.example.expressivelauncher.ui.components.HapticGroup
import com.example.expressivelauncher.ui.components.HapticKind

/**
 * Кнопки справа в строке поиска. Встроенные хранятся по имени, приложения — как "app:<ключ приложения>".
 */
enum class SearchTool(val id: String, val title: Int) {
    VOICE("voice", R.string.sb_voice),
    LENS("lens", R.string.sb_lens),
    AI_MODE("ai_mode", R.string.sb_ai_mode),
    GEMINI("gemini", R.string.sb_gemini),
    APPS("apps", R.string.all_apps),
    ;

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
    }
}

const val APP_PREFIX = "app:"

/** Известные ИИ-помощники: в настройках строки поиска они стоят первыми среди приложений. */
val AiPackages = listOf(
    "com.google.android.apps.bard", "com.openai.chatgpt", "com.anthropic.claude", "ai.perplexity.app.android",
    "com.deepseek.chat", "com.microsoft.copilot", "ai.x.grok", "com.alibaba.qwen", "ru.yandex.searchplugin",
    "com.yandex.alice", "ru.sberbank.gigachat", "com.mistral.lechat",
)

private const val GOOGLE_APP = "com.google.android.googlequicksearchbox"
private const val LENS_APP = "com.google.ar.lens"

/** Запуск встроенных действий строки поиска; каждое по возможности через приложение Google, иначе через браузер. */
object SearchLaunch {
    private fun Context.tryStart(vararg intents: Intent?): Boolean {
        for (i in intents) {
            i ?: continue
            if (runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return true
        }
        return false
    }

    private fun Context.installed(pkg: String) =
        runCatching { packageManager.getPackageInfo(pkg, 0) }.isSuccess

    /** Поиск Google. false — открыть нечем, пусть откроется поиск по приложениям. */
    fun google(ctx: Context): Boolean = ctx.tryStart(
        Intent("android.search.action.GLOBAL_SEARCH").setPackage(GOOGLE_APP),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")),
    )

    /** Выбранный поисковик: его приложение, если стоит, иначе сайт. Google — системный поиск Google. */
    fun engine(ctx: Context, e: com.example.expressivelauncher.data.SearchEngine): Boolean {
        if (e.name == "Google") return google(ctx)
        val app = e.packages.firstNotNullOfOrNull { ctx.packageManager.getLaunchIntentForPackage(it) }
        return ctx.tryStart(app, Intent(Intent.ACTION_VIEW, Uri.parse(e.home)))
    }

    fun run(ctx: Context, tool: SearchTool, openApps: () -> Unit) {
        val ok = when (tool) {
            // Классический голосовой поиск Google, как микрофон в строке поиска Pixel. VOICE_SEARCH_HANDS_FREE и
            // WEB_SEARCH там, где помощником выбран Gemini, открывают Gemini, поэтому они только запасные
            SearchTool.VOICE -> ctx.tryStart(
                Intent("com.google.android.googlequicksearchbox.action.CLASSIC_GSA_VOICE_SEARCH").setPackage(GOOGLE_APP),
                Intent("android.speech.action.WEB_SEARCH").setPackage(GOOGLE_APP),
                Intent("android.speech.action.WEB_SEARCH"),
            )
            // Ссылка googleapp://lens у сторонних приложений открывается и сразу закрывается, а ошибки не даёт:
            // запасные варианты после неё никогда не срабатывали. Сначала приложение «Google Объектив»
            SearchTool.LENS -> ctx.tryStart(
                ctx.packageManager.getLaunchIntentForPackage(LENS_APP),
                Intent().setClassName(GOOGLE_APP, "com.google.android.apps.lens.MainActivity"),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/")),
            )
            SearchTool.AI_MODE -> ctx.tryStart(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?udm=50")).setPackage(GOOGLE_APP),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?udm=50")),
            )
            SearchTool.GEMINI -> ctx.tryStart(
                if (ctx.installed("com.google.android.apps.bard")) ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard") else null,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app")),
            )
            SearchTool.APPS -> { openApps(); true }
        }
        if (!ok) com.example.expressivelauncher.ui.components.LauncherToast.show(ctx.getString(R.string.no_browser))
    }
}

/**
 * Строка поиска внизу стола, как на Pixel: слева логотип поиска, нажатие открывает поиск Google
 * (или поиск по приложениям), справа — свои кнопки: голос, Lens, режим ИИ, любые приложения.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelSearchBar(
    appMap: Map<String, AppInfo>,
    onAppsSearch: () -> Unit,
    onApps: () -> Unit,
    onLaunchApp: (AppInfo, Rect?) -> Unit,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val s = LocalSettings.current
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val engine = com.example.expressivelauncher.data.SearchEngines[s.searchEngine.coerceIn(com.example.expressivelauncher.data.SearchEngines.indices)]
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pillScale")
    // Внутри дока строка — концентрична ему: радиус дока минус отступ 8 dp, иначе углы строки и дока
    // расходятся и строка выглядит кривой. Без подложки дока — обычная «таблетка»
    val insideDock = s.showDock && s.dockBackground
    val shape = if (insideDock && !com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.accentChrome) androidx.compose.foundation.shape.RoundedCornerShape((s.dockRadius - 8).coerceAtLeast(12).dp) else CircleShape
    // Expressive: строка — светлый фиксированный тон акцента, текст и группа кнопок — тёмный тон
    val accent = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.accentChrome
    val ac = accentColors()
    val container = if (accent) ac.field else cs.surfaceContainerHigh
    val onContainer = if (accent) ac.onField else cs.onSurface
    // В Liquid Glass — стеклянная «таблетка» внутри стеклянного дока: обои второй раз не подкладываются,
    // тени нет (её обрезал край дока, и низ строки выглядел неровным)
    com.example.expressivelauncher.ui.components.ThemedSurface(
        onClick = {
            haptics.play(HapticKind.CLICK, HapticGroup.TOUCH)
            if (!s.searchGoogle || !SearchLaunch.engine(context, engine)) onAppsSearch()
        },
        shape = shape,
        color = container,
        contentColor = onContainer,
        interactionSource = source,
        overBlur = insideDock,
        shadow = false,
        frosted = false,
        modifier = modifier.fillMaxWidth().height(56.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Expressive: «G» и значки кнопок — в тонах палитры, как тематическая строка поиска Pixel
            val themed = if (accent) themedGlyphs(container) else null
            if (s.searchGoogle) EngineMark(engine, Modifier.size(26.dp), themed)
            else Icon(Icons.Rounded.Search, null, Modifier.size(24.dp))
            Text(
                if (!s.searchGoogle) stringResource(R.string.search_hint)
                else if (engine.name == "Google") stringResource(R.string.sb_hint_google)
                else stringResource(R.string.sb_hint_engine, engine.name),
                color = if (accent) onContainer else cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
            for (id in s.searchButtons) key(id) {
                val tool = SearchTool.of(id)
                val app = if (tool == null && id.startsWith(APP_PREFIX)) appMap[id.removePrefix(APP_PREFIX)] else null
                if (tool != null) {
                    ToolButton(onClick = {
                        haptics.play(HapticKind.CLICK, HapticGroup.TOUCH)
                        SearchLaunch.run(context, tool, onApps)
                    }) { ToolGlyph(tool, themed = themed) }
                } else if (app != null) {
                    val bounds = remember { arrayOf<Rect?>(null) }
                    ToolButton(
                        onClick = { onLaunchApp(app, bounds[0]) },
                        modifier = Modifier.onGloballyPositioned { bounds[0] = it.boundsInRoot() },
                    ) { AppIconImage(app, 30.dp, showBadge = false) }
                }
            }
            }
        }
    }
}

@Composable
private fun ToolButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent, modifier = modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** Значок встроенной кнопки; в настройках рисуется тем же кодом. */
@Composable
fun ToolGlyph(tool: SearchTool, size: Dp = 24.dp, tint: Color? = null, themed: ThemedGlyphs? = null) {
    val cs = MaterialTheme.colorScheme
    val mono = tint ?: themed?.deep ?: cs.onSurfaceVariant
    val accent = tint ?: themed?.main ?: cs.primary
    when (tool) {
        SearchTool.VOICE -> if (themed != null) MicMark(Modifier.size(size), themed) else Icon(Icons.Rounded.Mic, stringResource(tool.title), Modifier.size(size), tint = mono)
        SearchTool.LENS -> if (themed != null) ThemedLensMark(Modifier.size(size), themed) else LensMark(Modifier.size(size), tint)
        SearchTool.GEMINI -> Icon(Icons.Rounded.AutoAwesome, stringResource(tool.title), Modifier.size(size), tint = accent)
        SearchTool.APPS -> Icon(Icons.Rounded.Apps, stringResource(tool.title), Modifier.size(size), tint = mono)
        SearchTool.AI_MODE -> Box(
            Modifier.clip(CircleShape).padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "AI", color = accent, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Значок поисковика: у Google — многоцветная «G», у остальных — буква в круге фирменного цвета. */
@Composable
fun EngineMark(engine: com.example.expressivelauncher.data.SearchEngine, modifier: Modifier, themed: ThemedGlyphs? = null) {
    if (engine.name == "Google") { GoogleMark(modifier, themed); return }
    Box(modifier.clip(CircleShape).background(themed?.deep ?: Color(engine.color)), contentAlignment = Alignment.Center) {
        Text(engine.mark, color = themed?.light ?: Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Тона значков строки поиска в теме Expressive, как у тематической строки Pixel: «G», микрофон и Lens
 * не фирменных цветов, а в нескольких тонах акцента. [main] — основной (красная часть «G»), [soft] — светлый
 * (жёлтая), [second] — тон третичного цвета (зелёная), [deep] — глубокий (синяя). [light] — для текста на [deep].
 * Тона подбираются к фону строки: тёмные на светлой, светлые на тёмной.
 */
@androidx.compose.runtime.Immutable
class ThemedGlyphs(val main: Color, val soft: Color, val second: Color, val deep: Color, val light: Color)

@Composable
fun themedGlyphs(background: Color): ThemedGlyphs {
    val cs = MaterialTheme.colorScheme
    val primary = cs.primary
    val tertiary = cs.tertiary
    val onLight = background.luminance() > 0.35f
    return remember(primary, tertiary, onLight) {
        val p = com.materialkolor.hct.Hct.fromInt(primary.toArgb())
        val t = com.materialkolor.hct.Hct.fromInt(tertiary.toArgb())
        val pc = p.chroma.coerceAtMost(48.0)
        val tc = t.chroma.coerceAtMost(40.0)
        fun h(hue: Double, chroma: Double, tone: Double) = Color(com.materialkolor.hct.Hct.from(hue, chroma, tone).toInt())
        if (onLight) ThemedGlyphs(
            main = h(p.hue, pc, 42.0), soft = h(p.hue, pc * 0.45, 70.0), second = h(t.hue, tc, 48.0),
            deep = h(p.hue, pc * 0.9, 28.0), light = h(p.hue, pc * 0.3, 95.0),
        ) else ThemedGlyphs(
            main = h(p.hue, pc * 0.8, 80.0), soft = h(p.hue, pc * 0.35, 93.0), second = h(t.hue, tc * 0.8, 82.0),
            deep = h(p.hue, pc * 0.5, 88.0), light = h(p.hue, pc * 0.9, 25.0),
        )
    }
}

/** Микрофон в тонах палитры: капсула — третичным тоном, подставка — глубоким. */
@Composable
private fun MicMark(modifier: Modifier, c: ThemedGlyphs) {
    Canvas(modifier) {
        val w = size.minDimension
        val ox = (size.width - w) / 2f
        val oy = (size.height - w) / 2f
        val capW = w * 0.34f
        val capH = w * 0.52f
        drawRoundRect(
            c.second, Offset(ox + (w - capW) / 2f, oy + w * 0.06f), Size(capW, capH),
            androidx.compose.ui.geometry.CornerRadius(capW / 2f),
        )
        val st = w * 0.09f
        val line = Stroke(st, cap = StrokeCap.Round)
        val arcW = w * 0.58f
        drawArc(c.deep, 0f, 180f, false, Offset(ox + (w - arcW) / 2f, oy + w * 0.28f), Size(arcW, w * 0.46f), style = line)
        drawLine(c.deep, Offset(ox + w / 2f, oy + w * 0.74f), Offset(ox + w / 2f, oy + w * 0.92f), st, StrokeCap.Round)
    }
}

/** Lens в тонах палитры: углы рамки разными тонами, круг в центре и точка, как у тематического значка Pixel. */
@Composable
private fun ThemedLensMark(modifier: Modifier, c: ThemedGlyphs) {
    Canvas(modifier) {
        val w = size.minDimension
        val st = w * 0.11f
        val pad = st / 2f + w * 0.06f
        val corner = w * 0.3f
        val line = Stroke(st, cap = StrokeCap.Round)
        drawArc(c.second, 180f, 90f, false, Offset(pad, pad), Size(corner * 2, corner * 2), style = line)
        drawArc(c.main, 270f, 90f, false, Offset(w - pad - corner * 2, pad), Size(corner * 2, corner * 2), style = line)
        drawArc(c.soft, 90f, 90f, false, Offset(pad, w - pad - corner * 2), Size(corner * 2, corner * 2), style = line)
        drawCircle(c.second, w * 0.16f, Offset(w / 2f, w / 2f))
        drawCircle(c.main, w * 0.085f, Offset(w - pad - w * 0.08f, w - pad - w * 0.08f))
    }
}

/** Многоцветная «G» поиска, нарисованная дугами: синяя перекладина, красная, жёлтая и зелёная четверти. */
@Composable
fun GoogleMark(modifier: Modifier, themed: ThemedGlyphs? = null) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = w * 0.2f
        val r = (w - stroke) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val box = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        fun arc(color: Color, start: Float, sweep: Float) = drawArc(
            color, start, sweep, false, box.topLeft, Size(box.width, box.height), style = Stroke(stroke, cap = StrokeCap.Butt),
        )
        val red = themed?.main ?: Color(0xFFEA4335)
        val yellow = themed?.soft ?: Color(0xFFFBBC05)
        val green = themed?.second ?: Color(0xFF34A853)
        val blue = themed?.deep ?: Color(0xFF4285F4)
        arc(red, -150f, 105f)    // красная — сверху
        arc(yellow, 150f, 60f)   // жёлтая — слева
        arc(green, 45f, 105f)    // зелёная — снизу
        arc(blue, -10f, 55f)     // синяя — справа
        // Перекладина
        drawRect(blue, Offset(c.x, c.y - stroke / 2f), Size(r + stroke / 2f, stroke))
    }
}

/** Значок Lens: углы видоискателя и точка. */
@Composable
private fun LensMark(modifier: Modifier, tintOverride: Color? = null) {
    val cs = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.minDimension
        val st = w * 0.11f
        val pad = st / 2f + w * 0.06f
        val corner = w * 0.3f
        val line = Stroke(st, cap = StrokeCap.Round)
        val tint = tintOverride ?: cs.onSurfaceVariant
        // Три угла рамки, четвёртый (правый нижний) — цветная точка, как у Lens
        drawArc(tint, 180f, 90f, false, Offset(pad, pad), Size(corner * 2, corner * 2), style = line)
        drawArc(tint, 270f, 90f, false, Offset(w - pad - corner * 2, pad), Size(corner * 2, corner * 2), style = line)
        drawArc(tint, 90f, 90f, false, Offset(pad, w - pad - corner * 2), Size(corner * 2, corner * 2), style = line)
        drawCircle(tintOverride ?: cs.primary, w * 0.16f, Offset(w / 2f, w / 2f))
        drawCircle(tint, w * 0.085f, Offset(w - pad - w * 0.08f, w - pad - w * 0.08f))
    }
}
