package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CompactActionMinHeight = 48.dp
private val CompactActionContentPadding = PaddingValues(
    horizontal = 12.dp,
    vertical = 10.dp
)

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CompactActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = CompactActionContentPadding,
        content = content
    )
}

@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CompactActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = CompactActionContentPadding,
        content = content
    )
}

@Composable
fun QuietActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CompactActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = CompactActionContentPadding,
        content = content
    )
}

@Composable
fun DangerActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CompactActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        contentPadding = CompactActionContentPadding,
        content = content
    )
}
