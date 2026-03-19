package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    width = if (accented) 1.35.dp else 1.1.dp,
    color = color.copy(alpha = if (accented) 1f else 0.92f)
)

@Composable
fun appCardElevation(raised: Boolean = false) = CardDefaults.cardElevation(
    defaultElevation = if (raised) 9.dp else 4.dp,
    pressedElevation = if (raised) 5.dp else 2.dp
)

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.98f),
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    errorPlaceholderColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
    cursorColor = MaterialTheme.colorScheme.primary
)
