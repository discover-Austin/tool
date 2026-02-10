package com.tradesketch.estimator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    secondary = SafetyOrange,
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimary,
    secondary = SafetyOrange,
    background = Color(0xFF101113),
    surface = Color(0xFF1A1B1F)
)

@Composable
fun TradeSketchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
