package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.ui.components.CenteredLabelTrailingIcon
import com.tradesketch.estimator.ui.components.PrimaryActionButton

@Composable
fun ProjectRitualScreen2_EstimateType(
    selectedTrade: PrimaryTrade?,
    onSelectTrade: (PrimaryTrade) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = estimateOptions()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val topOffset = when {
            maxHeight > 840.dp -> 72.dp
            maxHeight > 700.dp -> 40.dp
            else -> 12.dp
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topOffset),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Workspaces,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.project_setup_step_2),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.what_are_you_estimating),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.choose_main_estimate_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options, key = { it.trade.name }) { option ->
                            val selected = selectedTrade == option.trade
                            Card(
                                onClick = { onSelectTrade(option.trade) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (selected) 4.dp else 1.dp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    PrimaryActionButton(
                        onClick = onComplete,
                        enabled = selectedTrade != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CenteredLabelTrailingIcon(
                            label = stringResource(R.string.open_project),
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private data class EstimateTypeOption(
    val trade: PrimaryTrade,
    val title: String,
    val description: String
)

private fun estimateOptions(): List<EstimateTypeOption> {
    return listOf(
        EstimateTypeOption(
            trade = PrimaryTrade.DRYWALL,
            title = "Drywall",
            description = "Wall and room coverage with sheets, screws, and joint compound."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.CONCRETE,
            title = "Concrete",
            description = "Slab quantities based on footprint and depth."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.PAINT,
            title = "Paint",
            description = "Paint quantities based on wall area, coats, and waste."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.GRAVEL_MULCH,
            title = "Gravel / Mulch",
            description = "Room-scoped bed areas with depth, density, and waste assumptions."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.MULTI,
            title = "Multi-Trade",
            description = "Flexible blueprint workflow across all trade types."
        )
    )
}
