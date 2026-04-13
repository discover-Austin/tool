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
    onSecondaryContainer = Color(0xFF1C2F41),
    tertiary = Oxblood760,
    onTertiary = Bone50,
    tertiaryContainer = Color(0xFFDBE4EC),
    onTertiaryContainer = Color(0xFF1E3143),
    background = Obsidian950,
    onBackground = Oxblood820,
    surface = Bone50,
    onSurface = Oxblood820,
    surfaceDim = Obsidian900,
    surfaceBright = Bone50,
    surfaceContainerLowest = Bone50,
    surfaceContainerLow = Bone120,
    surfaceContainer = Obsidian990,
    surfaceContainerHigh = Obsidian950,
    surfaceContainerHighest = Obsidian860,
    surfaceVariant = Color(0xFFD3DDE6),
    onSurfaceVariant = SlateText,
    error = Color(0xFFA73A30),
    onError = Bone50,
    errorContainer = Color(0xFFF4D9D5),
    onErrorContainer = Color(0xFF611913),
    outline = Color(0xFF5F7587),
    outlineVariant = Color(0xFF8EA1B2),
    scrim = Color(0x660A1722)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
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
