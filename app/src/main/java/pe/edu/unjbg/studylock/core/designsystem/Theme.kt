package pe.edu.unjbg.studylock.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tipografía basada en la escala detectada en el diseño (Figma: Inter).
 * Para usar Inter real: coloca los .ttf en res/font y cambia [SlFontFamily].
 */
val SlFontFamily: FontFamily = FontFamily.SansSerif

val SlTypography = Typography(
    displayLarge = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 51.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 33.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = SlFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)

val SlShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val SlLightColors = lightColorScheme(
    primary = SlColors.Primary,
    onPrimary = SlColors.Surface,
    primaryContainer = SlColors.ChipBlue,
    onPrimaryContainer = SlColors.Primary,
    secondary = SlColors.Purple,
    onSecondary = SlColors.Surface,
    secondaryContainer = SlColors.Lavender,
    onSecondaryContainer = SlColors.Ink,
    tertiary = SlColors.Teal,
    onTertiary = SlColors.Surface,
    tertiaryContainer = SlColors.Mint,
    onTertiaryContainer = SlColors.Ink,
    background = SlColors.Background,
    onBackground = SlColors.Ink,
    surface = SlColors.Surface,
    onSurface = SlColors.Ink,
    surfaceVariant = SlColors.ChipBlue,
    onSurfaceVariant = SlColors.Muted,
    outline = SlColors.Border,
    outlineVariant = SlColors.Border,
    error = SlColors.Danger,
    onError = SlColors.Surface,
)

@Composable
fun StudyLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SlLightColors,
        typography = SlTypography,
        shapes = SlShapes,
        content = content,
    )
}
