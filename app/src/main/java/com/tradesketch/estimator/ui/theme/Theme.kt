package com.tradesketch.estimator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val SleekDarkColors = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Midnight950,
    primaryContainer = Color(0xFF123146),
    onPrimaryContainer = Color(0xFFBEEBFF),
    secondary = EmberOrange,
    onSecondary = Midnight950,
    secondaryContainer = Color(0xFF4A2C11),
    onSecondaryContainer = Color(0xFFFFDFBF),
    tertiary = MintSignal,
    onTertiary = Midnight950,
    tertiaryContainer = Color(0xFF163A31),
    onTertiaryContainer = Color(0xFFBDFBE5),
    background = Midnight950,
    onBackground = Color(0xFFE2ECF7),
    surface = Midnight900,
    onSurface = Color(0xFFE2ECF7),
    surfaceVariant = Midnight850,
    onSurfaceVariant = SteelText,
    surfaceBright = Slate800,
    surfaceContainer = Midnight850,
    surfaceContainerHigh = Slate800,
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5A1F1F),
    onErrorContainer = Color(0xFFFFD9D6),
    outline = Slate700
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun TradeSketchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SleekDarkColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
