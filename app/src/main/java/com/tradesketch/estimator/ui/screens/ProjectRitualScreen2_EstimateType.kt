package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.PrimaryTrade

@Composable
fun ProjectRitualScreen2_EstimateType(
    selectedTrade: PrimaryTrade?,
    onSelectTrade: (PrimaryTrade) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = estimateOptions()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Workspaces,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Project Ritual 2/2",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "What are you estimating?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Choose the primary estimate type. You can expand scope later from Blueprint.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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

        Button(
            onClick = onComplete,
            enabled = selectedTrade != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter Workspace")
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
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
            description = "Room footprint and thickness-based slab takeoff from blueprint loops."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.PAINT,
            title = "Paint",
            description = "Wall/opening-derived paint quantities with coat and waste controls."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.GRAVEL_MULCH,
            title = "Gravel / Mulch",
            description = "Room-scoped bed areas with depth, density, and waste assumptions."
        ),
        EstimateTypeOption(
            trade = PrimaryTrade.MULTI,
            title = "Multi-Trade",
            description = "Blueprint-first workflow with flexible scope expansion."
        )
    )
}
