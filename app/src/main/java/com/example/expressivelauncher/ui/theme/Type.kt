package com.example.expressivelauncher.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

private fun TextStyle.styled(family: FontFamily, shift: Int) = copy(
    fontFamily = family,
    fontWeight = (fontWeight ?: FontWeight.Normal).shifted(shift),
)

/**
 * Шкала типографики Material 3 Expressive: размеры и веса — из спецификации, меняются только шрифт и общий сдвиг жирности.
 * Обычные стили остаются лёгкими, чтобы выделенные (*Emphasized) действительно выделялись: так строится иерархия.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun expressiveTypography(family: FontFamily, shift: Int, boldHeadlines: Boolean = false): Typography {
    val b = Typography()
    fun TextStyle.s() = styled(family, shift)
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
