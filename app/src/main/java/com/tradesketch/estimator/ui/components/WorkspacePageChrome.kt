package com.tradesketch.estimator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WorkspacePageHeaderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = appCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = appCardBorder(accented = true),
        elevation = appCardElevation(raised = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            leadingContent?.let {
                Box(contentAlignment = Alignment.TopStart) {
                    it()
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                eyebrow?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailingContent?.let {
                Box(contentAlignment = Alignment.TopEnd) {
                    it()
                }
            }
        }
    }
}

@Composable
fun WorkspaceHeaderBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Back",
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceBright
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.86f else 0.42f)
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = label,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun WorkspaceCompactPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onBack: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.82f)
        ),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onBack?.let { backAction ->
                    WorkspaceHeaderBackButton(
                        onClick = backAction,
                        label = "Back"
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WorkspaceSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    showDivider: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
        }
    }
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        color = when {
            !enabled -> ReferenceBlueprintPaperAlt.copy(alpha = 0.62f)
            selected -> ReferenceBlueprintNavy
            else -> Color.White.copy(alpha = 0.94f)
        },
        border = BorderStroke(
            width = if (selected) 1.2.dp else 1.dp,
            color = when {
                !enabled -> ReferenceBlueprintBorder.copy(alpha = 0.32f)
                selected -> ReferenceBlueprintBorder
                else -> ReferenceBlueprintBorder.copy(alpha = 0.78f)
            }
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                !enabled -> ReferenceBlueprintMuted.copy(alpha = 0.66f)
                selected -> Color.White
                else -> ReferenceBlueprintInk
            }
        )
    }
}

@Composable
private fun TechnicalTogglePill(
    checked: Boolean,
    enabled: Boolean = true
) {
    Surface(
        color = if (checked) {
            ReferenceBlueprintNavy
        } else {
            Color.White.copy(alpha = 0.96f)
        },
        border = BorderStroke(
            if (checked) 1.15.dp else 1.dp,
            when {
                !enabled -> ReferenceBlueprintBorder.copy(alpha = 0.4f)
                checked -> ReferenceBlueprintGoldBorder.copy(alpha = 0.92f)
                else -> ReferenceBlueprintBorder.copy(alpha = 0.72f)
            }
        ),
        shadowElevation = if (checked && enabled) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (checked) ReferenceBlueprintGold else ReferenceBlueprintPaperAlt,
                border = BorderStroke(1.dp, if (checked) ReferenceBlueprintGoldBorder else ReferenceBlueprintBorder.copy(alpha = 0.45f))
            ) {
                Box(modifier = Modifier.padding(4.dp))
            }
            Text(
                text = if (checked) "On" else "Off",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked) Color.White else ReferenceBlueprintMuted
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = { onCheckedChange(!checked) },
            modifier = Modifier.fillMaxWidth(),
            color = if (checked) {
                Color(0xFFE9E2CF).copy(alpha = 0.98f)
            } else {
                Color(0xFFF8F6EF).copy(alpha = 0.98f)
            },
            border = BorderStroke(
                if (checked) 1.2.dp else 1.dp,
                if (checked) {
                    ReferenceBlueprintSteel
                } else {
                    ReferenceBlueprintBorder.copy(alpha = 0.74f)
                }
            ),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (checked) ReferenceBlueprintGold else ReferenceBlueprintNavy.copy(alpha = 0.36f),
                    border = BorderStroke(
                        1.dp,
                        if (checked) {
                            ReferenceBlueprintGoldBorder
                        } else {
                            ReferenceBlueprintBorder.copy(alpha = 0.48f)
                        }
                    )
                ) {
                    Box(modifier = Modifier.padding(5.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ReferenceBlueprintInk
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                }
                TechnicalTogglePill(checked = checked)
            }
        }
        if (showDivider) {
            HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.18f))
        }
    }
}

@Composable
fun SettingSliderRow(
    title: String,
    summary: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    showDivider: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8F6EF).copy(alpha = 0.98f),
            border = BorderStroke(1.1.dp, ReferenceBlueprintBorder.copy(alpha = 0.82f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ReferenceBlueprintInk
                    )
                    Surface(
                        color = ReferenceBlueprintPaperAlt,
                        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.82f))
                    ) {
                        Text(
                            text = valueLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ReferenceBlueprintNavy
                        )
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = ReferenceBlueprintMuted
                )
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    enabled = enabled,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = ReferenceBlueprintNavy,
                        activeTrackColor = ReferenceBlueprintNavy,
                        inactiveTrackColor = ReferenceBlueprintBorder.copy(alpha = 0.34f),
                        activeTickColor = ReferenceBlueprintGold,
                        inactiveTickColor = ReferenceBlueprintBorder.copy(alpha = 0.22f),
                        disabledThumbColor = ReferenceBlueprintMuted.copy(alpha = 0.42f),
                        disabledActiveTrackColor = ReferenceBlueprintMuted.copy(alpha = 0.28f),
                        disabledInactiveTrackColor = ReferenceBlueprintMuted.copy(alpha = 0.16f)
                    )
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.18f))
        }
    }
}
