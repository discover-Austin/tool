package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.R
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.ui.components.CenteredLabelTrailingIcon
import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation

@Composable
fun ProjectRitualScreen2_EstimateType(
    selectedTrade: PrimaryTrade?,
    onSelectTrade: (PrimaryTrade) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = estimateOptions()
    val paperColor = Color(0xFFF3F0E8)
    val paperBorder = Color(0xFF9AAAB5)
    val headerStart = Color(0xFF173B53)
    val headerEnd = Color(0xFF0C2535)
    val accent = Color(0xFF2B7396)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(paperColor)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val topOffset = when {
            maxHeight > 840.dp -> 72.dp
            maxHeight > 700.dp -> 40.dp
            else -> 12.dp
        }
        val headerHeight = if (maxHeight > 700.dp) 220.dp else 170.dp
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(Brush.verticalGradient(listOf(headerStart, headerEnd)))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topOffset),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp),
                    colors = appCardColors(containerColor = paperColor),
                    border = BorderStroke(width = 1.dp, color = paperBorder),
                    elevation = appCardElevation(raised = true)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(headerStart, headerEnd)))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.Transparent,
                                border = BorderStroke(width = 1.dp, color = Color.White.copy(alpha = 0.36f))
                            ) {
                                Text(
                                    text = stringResource(R.string.project_setup_step_2),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = headerStart,
                                border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Workspaces,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.what_are_you_estimating),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Choose the default trade focus for the project. Multi-Trade keeps the full blueprint workflow open across all trades.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.choose_main_estimate_type),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4B5F6F)
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
                                        colors = appCardColors(
                                            containerColor = if (selected) Color.White else paperColor
                                        ),
                                        border = BorderStroke(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) accent else paperBorder
                                        ),
                                        elevation = appCardElevation(raised = selected),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = option.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF14232F)
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF4B5F6F)
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
