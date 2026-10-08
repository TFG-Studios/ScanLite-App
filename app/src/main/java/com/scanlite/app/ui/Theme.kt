package com.scanlite.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.scanlite.app.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF0097B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F4FA),
    onPrimaryContainer = Color(0xFF00424F),
    secondary = Color(0xFF4F7480),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F4FA),
    onSecondaryContainer = Color(0xFF00424F),
    background = Color(0xFFF2F6F8),
    onBackground = Color(0xFF0E1A1D),
    surface = Color.White,
    onSurface = Color(0xFF0E1A1D),
    surfaceVariant = Color(0xFFE6EEF1),
    onSurfaceVariant = Color(0xFF5B6B70),
    outline = Color(0xFFC3D0D5),
    outlineVariant = Color(0xFFDCE5E8),
    error = Color(0xFFFF3B30),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE6EEF1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD6EE),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF0B3B45),
    onPrimaryContainer = Color(0xFFBDF1FA),
    secondary = Color(0xFF8FB4BF),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0B3B45),
    onSecondaryContainer = Color(0xFFBDF1FA),
    background = Color(0xFF050607),
    onBackground = Color(0xFFEAF1F3),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFEAF1F3),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF9AA7AB),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = Color(0xFFFF453A),
    surfaceContainerLowest = Color(0xFF050607),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF232326),
    surfaceContainerHighest = Color(0xFF2C2C2E),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val AppFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_medium, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = AppFont, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = AppFont, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = AppFont, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = AppFont, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = AppFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = AppFont, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = AppFont, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = AppFont, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = AppFont, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ScanTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
