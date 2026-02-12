package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.components.AnimatedEntry
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ExportViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.utils.Formatters

@Composable
fun ExportScreen(
    projectId: String,
    onOpenModel: () -> Unit = {},
    onOpenBlueprint: () -> Unit = {},
    onOpenTakeoff: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val exportSteps = listOf(
        ExportFlowStep(
            label = "Scope",
            detail = uiState.selectedType?.displayLabel ?: "Choose trade",
            complete = uiState.selectedType != null
        ),
        ExportFlowStep(
            label = "Preview",
            detail = if (uiState.result != null) "Summary ready" else "Run takeoff first",
            complete = uiState.result != null
        ),
        ExportFlowStep(
            label = "Share",
            detail = if (uiState.result != null) "Copy or send output" else "Awaiting data",
            complete = uiState.result != null
        )
    )

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }

    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedEntry(delayMs = 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Export Center",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.project?.name ?: "No project selected",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Prepare a clean summary, full report, or CSV in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.animateContentSize()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Export Workflow",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exportSteps.forEach { step ->
                        ExportStepPill(
                            step = step,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.animateContentSize()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Flow Navigator",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenModel,
                        modifier = Modifier.width(105.dp)
                    ) {
                        Text("Model")
                    }
                    OutlinedButton(
                        onClick = onOpenBlueprint,
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Blueprint")
                    }
                    OutlinedButton(
                        onClick = onOpenTakeoff,
                        modifier = Modifier.width(110.dp)
                    ) {
                        Text("Takeoff")
                    }
                }
                Text(
                    text = "Finalize outputs here, then share summary/report/CSV with confidence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.animateContentSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Report Scope",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TakeoffType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedType == type,
                            onClick = {
                                haptics.tap()
                                viewModel.selectTakeoffType(type)
                            },
                            label = { Text(type.displayLabel) }
                        )
                    }
                }
            }
        }

        uiState.result?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.animateContentSize()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "${uiState.takeoffType} Export Snapshot",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${result.items.size} line items",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    result.totalCost?.let { total ->
                        Text(
                            text = "Grand total: ${Formatters.formatMoney(total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    result.materialSubtotal?.let { subtotal ->
                        Text(
                            text = "Materials: ${Formatters.formatMoney(subtotal)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(modifier = Modifier.animateContentSize()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Summary Preview",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.summaryContent.ifBlank { "No summary generated yet." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(modifier = Modifier.animateContentSize()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            haptics.confirm()
                            viewModel.copySummaryToClipboard()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Summary")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            viewModel.copyReportToClipboard()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Full Report")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            viewModel.copyCSVToClipboard()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy CSV")
                    }
                }
            }

            Card(modifier = Modifier.animateContentSize()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            haptics.confirm()
                            val intent = viewModel.createShareIntent(shareCsv = false)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Full Report")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            val intent = viewModel.createShareIntent(shareCsv = true)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share CSV")
                    }
                }
            }
        } ?: Card {
            Text(
                text = "No data available for this takeoff type yet. Run a takeoff first.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.lastAction?.let { action ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private val TakeoffType.displayLabel: String
    get() = when (this) {
        TakeoffType.DRYWALL -> "Drywall"
        TakeoffType.CONCRETE -> "Concrete"
        TakeoffType.GRAVEL_MULCH -> "Gravel/Mulch"
        TakeoffType.PAINT -> "Paint"
    }

@Composable
private fun ExportStepPill(
    step: ExportFlowStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (step.complete) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = step.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (step.complete) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (step.complete) "Ready" else "Pending",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class ExportFlowStep(
    val label: String,
    val detail: String,
    val complete: Boolean
)
