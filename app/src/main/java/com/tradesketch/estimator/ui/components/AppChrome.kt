package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun appCardColors(
    containerColor: Color = MaterialTheme.colorScheme.surface
): CardColors = CardDefaults.cardColors(
    containerColor = containerColor,
    contentColor = MaterialTheme.colorScheme.onSurface
)

@Composable
fun appCardBorder(
    accented: Boolean = false,
    color: Color = MaterialTheme.colorScheme.outline
): BorderStroke = BorderStroke(
    width = if (accented) 1.5.dp else 1.15.dp,
    color = color.copy(alpha = if (accented) 0.98f else 0.86f)
)

@Composable
fun appCardElevation(raised: Boolean = false) = CardDefaults.cardElevation(
    defaultElevation = if (raised) 3.5.dp else 1.25.dp,
    pressedElevation = if (raised) 2.dp else 0.5.dp
)

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
    errorPlaceholderColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
fun appFilterChipBorder(
    selected: Boolean,
    enabled: Boolean = true
): BorderStroke = BorderStroke(
    width = if (selected) 1.15.dp else 1.dp,
    color = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.76f)
    }
)

@Composable
fun appFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceBright,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
    disabledSelectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
)
