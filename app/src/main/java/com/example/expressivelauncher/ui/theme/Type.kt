package com.example.expressivelauncher.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em
import com.example.expressivelauncher.data.AppFont
import java.io.File

private val weights = listOf(
    FontWeight.Thin, FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium,
    FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black,
)

/** Имя файла выбранного пользователем шрифта во внутреннем хранилище. */
const val CUSTOM_FONT_FILE = "custom_font.ttf"

/** Compose кэширует шрифт по пути, поэтому у каждой версии файла своё имя, иначе новый шрифт не применится до перезапуска. */
fun customFontFile(filesDir: File, rev: Int): File =
    File(filesDir, "custom_font_$rev.ttf").takeIf { it.exists() } ?: File(filesDir, CUSTOM_FONT_FILE)

/**
 * Семейство для выбранного шрифта. Системные семейства задаются по имени на устройстве по всем весам,
 * поэтому сдвиг жирности работает и для них; у файла шрифта вес выставляется осью wght (у статичных шрифтов она игнорируется).
 */
fun resolveFontFamily(font: AppFont, filesDir: File, rev: Int): FontFamily = when {
    font == AppFont.SYSTEM -> FontFamily.Default
    font == AppFont.CUSTOM -> {
        val file = customFontFile(filesDir, rev)
        if (!file.exists()) FontFamily.Default
        else runCatching {
            FontFamily(weights.map { w -> Font(file, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight))) })
        }.getOrDefault(FontFamily.Default)
    }
    else -> FontFamily(weights.map { w -> Font(DeviceFontFamilyName(font.deviceName!!), w) })
}

private fun FontWeight.shifted(shift: Int) = FontWeight((weight + shift * 100).coerceIn(100, 900))

private fun TextStyle.styled(family: FontFamily, shift: Int, baseline: BaselineShift?) = copy(
    fontFamily = family,
    fontWeight = (fontWeight ?: FontWeight.Normal).shifted(shift),
    baselineShift = baseline,
)

/**
 * Поправка базовой линии для своего шрифта. Интерфейс центрирует текст по метрикам шрифта (ascent/descent),
 * а у многих шрифтов из интернета они несимметричны (большой descent или line gap): буквы оказываются выше
 * центра кнопок и строк. Сравниваем, где у шрифта видимый центр букв относительно центра его метрик, с системным
 * шрифтом, и сдвигаем текст на разницу. null — поправка не нужна.
 */
fun fontBaselineShift(file: File): BaselineShift? = runCatching {
    fun offset(t: android.graphics.Typeface): Pair<Float, Float> {
        val p = android.graphics.Paint().apply { typeface = t; textSize = 100f }
        val fm = p.fontMetrics
        val r = android.graphics.Rect()
        p.getTextBounds("Hxн0", 0, 4, r)
        return (r.exactCenterY() - (fm.ascent + fm.descent) / 2f) to fm.ascent
    }
    val (custom, ascent) = offset(android.graphics.Typeface.createFromFile(file))
    val (system, _) = offset(android.graphics.Typeface.DEFAULT)
    // Положительное — свой шрифт надо опустить. Сдвиг расширяет строку в ту же сторону, а центрирование
    // по высоте строки съедает половину, поэтому сдвигаем вдвое
    val down = (system - custom) * 2f
    if (kotlin.math.abs(down) < 2f || ascent >= 0f) null else BaselineShift(down / ascent)
}.getOrNull()

/**
 * Шкала типографики Material 3 Expressive: размеры и веса — из спецификации, меняются только шрифт и общий сдвиг жирности.
 * Обычные стили остаются лёгкими, чтобы выделенные (*Emphasized) действительно выделялись: так строится иерархия.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun expressiveTypography(family: FontFamily, shift: Int, boldHeadlines: Boolean = false, baseline: BaselineShift? = null): Typography {
    val b = Typography()
    fun TextStyle.s() = styled(family, shift, baseline)
    // Тема Expressive: заголовки крупные и плотные — обычные стили заголовков берут веса Emphasized
    fun pick(regular: TextStyle, emphasized: TextStyle) = (if (boldHeadlines) emphasized else regular).s()
    return Typography(
        displayLarge = pick(b.displayLarge, b.displayLargeEmphasized),
        displayMedium = pick(b.displayMedium, b.displayMediumEmphasized),
        displaySmall = pick(b.displaySmall, b.displaySmallEmphasized),
        headlineLarge = pick(b.headlineLarge, b.headlineLargeEmphasized),
        headlineMedium = pick(b.headlineMedium, b.headlineMediumEmphasized),
        headlineSmall = pick(b.headlineSmall, b.headlineSmallEmphasized),
        titleLarge = pick(b.titleLarge, b.titleLargeEmphasized),
        titleMedium = b.titleMedium.s(),
        titleSmall = b.titleSmall.s(),
        bodyLarge = b.bodyLarge.s(),
        bodyMedium = b.bodyMedium.s(),
        bodySmall = b.bodySmall.s(),
        labelLarge = b.labelLarge.s(),
        labelMedium = b.labelMedium.s(),
        labelSmall = b.labelSmall.s(),
        displayLargeEmphasized = b.displayLargeEmphasized.s(),
        displayMediumEmphasized = b.displayMediumEmphasized.s(),
        displaySmallEmphasized = b.displaySmallEmphasized.s(),
        headlineLargeEmphasized = b.headlineLargeEmphasized.s(),
        headlineMediumEmphasized = b.headlineMediumEmphasized.s(),
        headlineSmallEmphasized = b.headlineSmallEmphasized.s(),
        titleLargeEmphasized = b.titleLargeEmphasized.s(),
        titleMediumEmphasized = b.titleMediumEmphasized.s(),
        titleSmallEmphasized = b.titleSmallEmphasized.s(),
        bodyLargeEmphasized = b.bodyLargeEmphasized.s(),
        bodyMediumEmphasized = b.bodyMediumEmphasized.s(),
        bodySmallEmphasized = b.bodySmallEmphasized.s(),
        labelLargeEmphasized = b.labelLargeEmphasized.s(),
        labelMediumEmphasized = b.labelMediumEmphasized.s(),
        labelSmallEmphasized = b.labelSmallEmphasized.s(),
    )
}

/** Крупные часы на рабочем столе; семейство подставляется при использовании. */
fun clockTextStyle(family: FontFamily, shift: Int) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Bold.shifted(shift),
    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
    letterSpacing = (-0.04).em,
)
