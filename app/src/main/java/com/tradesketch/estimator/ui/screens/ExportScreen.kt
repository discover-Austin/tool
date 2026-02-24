package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.components.TitledSectionCard
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ExportViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.utils.ExportStorage
import com.tradesketch.estimator.utils.Formatters
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenTakeoff: () -> Unit = {},
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showScopeSelector by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(uiState.selectedType == null)
    }
    var isPreparingEstimatePdf by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    val csvSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open CSV output stream")
            output.use { stream ->
                stream.write(viewModel.csvContent().toByteArray())
            }
        }.onSuccess {
            Toast.makeText(context, "CSV exported.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Could not export CSV.", Toast.LENGTH_SHORT).show()
        }
    }
    val jsonSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val output = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open JSON output stream")
            output.use { stream ->
                stream.write(viewModel.jsonContent().toByteArray())
            }
        }.onSuccess {
            Toast.makeText(context, "JSON exported.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Could not export JSON.", Toast.LENGTH_SHORT).show()
        }
    }
    val pdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bytes = viewModel.buildEstimatePdfBytes() ?: return@launch
            runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to open PDF output stream")
                output.use { stream ->
                    stream.write(bytes)
                }
            }.onSuccess {
                Toast.makeText(context, "PDF exported.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not export PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.recordTap("export_screen_opened")
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
        TitledSectionCard(
            title = "Estimate Type",
            subtitle = "Set which trade this export represents.",
            modifier = Modifier.animateContentSize()
        ) {
            val selectedType = uiState.selectedType
            if (selectedType != null && !showScopeSelector) {
                Text(
                    text = "Selected type: ${selectedType.displayLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SecondaryActionButton(
                    onClick = { showScopeSelector = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Estimate Type")
                }
            } else {
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
                                viewModel.recordTap("export_select_scope")
                                viewModel.selectTakeoffType(type)
                                showScopeSelector = false
                            },
                            label = { Text(type.displayLabel) }
                        )
                    }
                }
                if (selectedType != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuietActionButton(
                        onClick = { showScopeSelector = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select an estimate type to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.animateContentSize()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Need to adjust quantities or pricing?",
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = {
                        haptics.tap()
                        viewModel.recordTap("export_open_takeoff")
                        onOpenTakeoff()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Takeoff")
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
                        text = "${uiState.takeoffType} Estimate Summary",
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
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (uiState.settings.businessName.isBlank()) {
                        Text(
                            text = "Tip: add your business details in Settings > Business Identity for branded exports.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PrimaryActionButton(
                        onClick = {
                            haptics.confirm()
                            viewModel.recordTap("export_share_full_report")
                            val intent = viewModel.createShareIntent(shareCsv = false)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Full Report")
                    }
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            isPreparingEstimatePdf = true
                            coroutineScope.launch {
                                try {
                                    val uri = viewModel.saveEstimatePdfToDownloads()
                                    if (uri == null) {
                                        Toast.makeText(context, "Could not save estimate PDF.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Estimate PDF saved to TradeSketch folder.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    isPreparingEstimatePdf = false
                                }
                            }
                        },
                        enabled = !isPreparingEstimatePdf,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPreparingEstimatePdf) "Preparing PDF..." else "Download Estimate PDF")
                    }
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            isPreparingEstimatePdf = true
                            coroutineScope.launch {
                                try {
                                    val intent = viewModel.createEstimatePdfShareIntent()
                                    if (intent == null) {
                                        Toast.makeText(context, "Could not prepare estimate PDF.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        context.startActivity(intent)
                                    }
                                } finally {
                                    isPreparingEstimatePdf = false
                                }
                            }
                        },
                        enabled = !isPreparingEstimatePdf,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPreparingEstimatePdf) "Preparing PDF..." else "Share Estimate PDF")
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            csvSafLauncher.launch(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "quantities",
                                    extension = "csv"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save CSV")
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            jsonSafLauncher.launch(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "backup",
                                    extension = "json"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save JSON Backup")
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            pdfSafLauncher.launch(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "estimate",
                                    extension = "pdf"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF")
                    }
                }
            }
        } ?: Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "No estimate data for this type yet. Open Takeoff to generate quantities.",
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = {
                        viewModel.recordTap("export_open_takeoff")
                        onOpenTakeoff()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Takeoff")
                }
            }
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
