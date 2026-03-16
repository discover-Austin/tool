package com.tradesketch.estimator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TradeSketchColors = lightColorScheme(
    primary = SignalBlue,
    onPrimary = Bone50,
    primaryContainer = SignalMist,
    onPrimaryContainer = SignalBlueDeep,
    secondary = SteelBlue,
    onSecondary = Bone50,
    secondaryContainer = SteelBlueSoft,
    onSecondaryContainer = Color(0xFF21364E),
    tertiary = Oxblood760,
    onTertiary = Bone50,
    tertiaryContainer = Color(0xFFE3EAF2),
    onTertiaryContainer = Color(0xFF293847),
    background = Obsidian990,
    onBackground = Oxblood820,
    surface = Bone50,
    onSurface = Oxblood820,
    surfaceDim = Obsidian950,
    surfaceBright = Bone50,
    surfaceContainerLowest = Bone50,
    surfaceContainerLow = Bone120,
    surfaceContainer = Obsidian950,
    surfaceContainerHigh = Obsidian900,
    surfaceContainerHighest = Obsidian860,
    surfaceVariant = Color(0xFFE8EDF4),
    onSurfaceVariant = SlateText,
    error = Color(0xFFBF3B31),
    onError = Bone50,
    errorContainer = Color(0xFFF8DAD7),
    onErrorContainer = Color(0xFF6A1912),
    outline = Color(0xFFC2CDD8),
    outlineVariant = Color(0xFFDBE2EA),
    scrim = Color(0x55000000)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(30.dp)
)

@Composable
fun TradeSketchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TradeSketchColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
