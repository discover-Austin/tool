package com.tradesketch.estimator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ConstructionBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE7FF),
    onPrimaryContainer = Color(0xFF00174A),
    secondary = SignalOrange,
    onSecondary = Color(0xFF2A1600),
    secondaryContainer = Color(0xFFFFE3C0),
    onSecondaryContainer = Color(0xFF3A2400),
    tertiary = TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1F2EA),
    onTertiaryContainer = Color(0xFF002D28),
    background = Color(0xFFF3F6FA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182231),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = SteelGray,
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFFB7C1D1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF02246F),
    primaryContainer = Color(0xFF17357E),
    onPrimaryContainer = Color(0xFFDEE7FF),
    secondary = Color(0xFFFFB955),
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Color(0xFF5F4000),
    onSecondaryContainer = Color(0xFFFFE3C0),
    tertiary = Color(0xFF70D7C8),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFFC1F2EA),
    background = Color(0xFF0E131C),
    onBackground = Color(0xFFE3EAF6),
    surface = Color(0xFF131A25),
    onSurface = Color(0xFFE3EAF6),
    surfaceVariant = Color(0xFF273040),
    onSurfaceVariant = Color(0xFFC1CAD8),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8793A7)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun TradeSketchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
