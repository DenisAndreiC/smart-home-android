package com.denis.smarthome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartHomeDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFF80DEEA),
    secondary = Secondary,
    onSecondary = OnPrimary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFF80DEEA),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurface,
    error = ErrorColor,
    onError = Color.White,
    outline = Outline,
    outlineVariant = OutlineVariant,
)

private val SmartHomeLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF004D57),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF004A52),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF333333),
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFF555555),
    error = ErrorColor,
    onError = Color.White,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFCCCCCC),
)

@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (ThemeState.isDark) SmartHomeDarkColorScheme else SmartHomeLightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
