package com.applimit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS-inspired palette: light, one accent tone (iOS system blue), soft greys.
 * The whole app deliberately stays in a light scheme to match the requested
 * "helle Farbpalette" look, even when the system is in dark mode.
 */
object AppLimitColors {
    val Accent = Color(0xFF0A84FF)       // iOS system blue
    val AccentPressed = Color(0xFF0060DF)
    val Background = Color(0xFFF2F2F7)   // iOS grouped-list background
    val Card = Color(0xFFFFFFFF)
    val Label = Color(0xFF1C1C1E)
    val SecondaryLabel = Color(0xFF8E8E93)
    val Separator = Color(0xFFE5E5EA)
    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFFF9F0A)
    val Danger = Color(0xFFFF3B30)
    val SwitchTrackOff = Color(0xFFE9E9EA)
}

private val LightColors = lightColorScheme(
    primary = AppLimitColors.Accent,
    onPrimary = Color.White,
    background = AppLimitColors.Background,
    onBackground = AppLimitColors.Label,
    surface = AppLimitColors.Card,
    onSurface = AppLimitColors.Label,
    error = AppLimitColors.Danger,
)

// SF-Pro-like feel using the platform's default sans (San Francisco on stock
// devices renders very close to this). Weights/spacing tuned to iOS.
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 0.37.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 0.36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = (-0.41).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = (-0.08).sp),
)

// iOS-typical corner radius ~12–16dp.
private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun AppLimitTheme(content: @Composable () -> Unit) {
    // Intentionally ignore isSystemInDarkTheme(): the design brief asks for a
    // consistently light, iOS-styled surface.
    @Suppress("UNUSED_VARIABLE")
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
