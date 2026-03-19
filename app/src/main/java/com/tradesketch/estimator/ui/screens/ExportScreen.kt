package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.QuietActionButton
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.hasMeasuredQuantities
import com.tradesketch.estimator.domain.model.nonZeroItems
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.components.TitledSectionCard
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ExportActionResult
import com.tradesketch.estimator.ui.viewmodel.ExportViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.utils.ExportStorage
import com.tradesketch.estimator.utils.Formatters
import com.tradesketch.estimator.utils.defaultDeviceSaveHint
import kotlinx.coroutines.launch

private enum class EstimatePreviewPage(
    val label: String,
    val docHint: String
) {
    COST(
        label = "Cost",
        docHint = "Client pricing sheet with subtotals, total, and line-item cost detail."
    ),
    SHOPPING_LIST(
        label = "Shopping List",
        docHint = "Purchase-ready itemized list with quantities and optional cost guidance."
    ),
    BLUEPRINT(
        label = "Blueprint",
        docHint = "Geometry summary page with wall, room, opening, and area coverage detail."
    ),
    COMBINED_NO_BLUEPRINT(
        label = "Cost + Shopping List",
        docHint = "Combined cost sheet and shopping list without the blueprint page."
    )
}

@Composable
fun ExportScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenTakeoff: () -> Unit = {},
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reportFailure: (ExportActionResult) -> Unit = { result ->
        if (result is ExportActionResult.Failure) {
            viewModel.reportExternalFailure(result.message)
        }
    }
    var showScopeSelector by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(uiState.selectedType == null)
    }
    var isPreparingEstimatePdf by remember(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    var isPreparingBlueprintPdf by remember(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    var selectedPreviewPage by rememberSaveable(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(EstimatePreviewPage.COST)
    }
    val csvSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            reportFailure(viewModel.saveCsvToUri(uri))
        }
    }
    val jsonSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            reportFailure(viewModel.saveJsonToUri(uri))
        }
    }
    val pdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            reportFailure(viewModel.saveEstimatePdfToUri(uri))
        }
    }
    val blueprintPngSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            reportFailure(viewModel.saveBlueprintPngToUri(uri))
        }
    }
    val blueprintPdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            reportFailure(viewModel.saveBlueprintPdfToUri(uri))
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
        val sessionInputMode = uiState.project?.takeoffSession?.inputMode ?: TakeoffInputMode.BLUEPRINT
        val hasBlueprintGeometry = uiState.previewBlueprint?.hasGeometry() == true
        val hasMeasuredQuantities = uiState.result?.hasMeasuredQuantities() == true
        ExportProjectHeaderCard(
            projectName = uiState.project?.name.orEmpty(),
            businessName = uiState.settings.businessName.ifBlank { null },
            takeoffType = uiState.takeoffType.ifBlank {
                uiState.selectedType?.displayLabel ?: "Choose a trade"
            },
            result = uiState.result,
            inputMode = sessionInputMode,
            hasMeasuredQuantities = hasMeasuredQuantities,
            hasBlueprintGeometry = hasBlueprintGeometry
        )

        TitledSectionCard(
            title = "Trade",
            subtitle = "Choose which trade this export represents.",
            modifier = Modifier.animateContentSize()
        ) {
            val selectedType = uiState.selectedType
            if (selectedType != null && !showScopeSelector) {
                Text(
                    text = "Selected trade: ${selectedType.displayLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SecondaryActionButton(
                    onClick = { showScopeSelector = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Trade")
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
                        text = "Choose a trade to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.animateContentSize(),
            colors = appCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = appCardBorder(),
            elevation = appCardElevation()
        ) {
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
                    Text("Open Materials & Pricing")
                }
            }
        }

        uiState.result?.let { result ->
            ProfessionalPreviewDeck(
                takeoffType = uiState.takeoffType.ifBlank {
                    uiState.selectedType?.displayLabel ?: "Estimate"
                },
                result = result,
                blueprint = uiState.previewBlueprint,
                selectedPage = selectedPreviewPage,
                onSelectPage = { selectedPreviewPage = it },
                modifier = Modifier.animateContentSize()
            )

            Card(
                modifier = Modifier.animateContentSize(),
                colors = appCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = appCardBorder(accented = true),
                elevation = appCardElevation(raised = true)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Export Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (uiState.settings.businessName.isBlank()) {
                        Text(
                            text = "Tip: add your business details in Settings > Business Identity to include them on exported files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    PrimaryActionButton(
                        onClick = {
                            haptics.confirm()
                            viewModel.recordTap("export_share_full_report")
                            val intent = viewModel.createShareIntent(shareCsv = false)
                            launchExportIntent(
                                context = context,
                                intent = intent,
                                noTargetMessage = "No app available to share this report.",
                                onFailure = viewModel::reportExternalFailure
                            )
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
                                    when (val result = viewModel.createEstimatePdfShareIntent()) {
                                        is ExportActionResult.Success -> {
                                            launchExportIntent(
                                                context = context,
                                                intent = result.intent ?: return@launch,
                                                noTargetMessage = "No app available to share this PDF.",
                                                onFailure = viewModel::reportExternalFailure
                                            )
                                        }
                                        is ExportActionResult.Failure -> viewModel.reportExternalFailure(result.message)
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
                            haptics.confirm()
                            isPreparingBlueprintPdf = true
                            coroutineScope.launch {
                                try {
                                    when (val result = viewModel.createBlueprintPdfShareIntent()) {
                                        is ExportActionResult.Success -> {
                                            launchExportIntent(
                                                context = context,
                                                intent = result.intent ?: return@launch,
                                                noTargetMessage = "No app available to share this blueprint PDF.",
                                                onFailure = viewModel::reportExternalFailure
                                            )
                                        }
                                        is ExportActionResult.Failure -> viewModel.reportExternalFailure(result.message)
                                    }
                                } finally {
                                    isPreparingBlueprintPdf = false
                                }
                            }
                        },
                        enabled = !isPreparingBlueprintPdf && hasBlueprintGeometry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isPreparingBlueprintPdf) {
                                "Preparing Blueprint PDF..."
                            } else {
                                "Share Blueprint PDF"
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Text(
                        text = "Save to device",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = defaultDeviceSaveHint(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            isPreparingEstimatePdf = true
                            coroutineScope.launch {
                                try {
                                    when (val result = viewModel.saveEstimatePdfToDownloads()) {
                                        is ExportActionResult.Success -> Unit
                                        is ExportActionResult.Failure -> viewModel.reportExternalFailure(result.message)
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
                            isPreparingBlueprintPdf = true
                            coroutineScope.launch {
                                try {
                                    when (val result = viewModel.saveBlueprintPdfToDownloads()) {
                                        is ExportActionResult.Success -> Unit
                                        is ExportActionResult.Failure -> viewModel.reportExternalFailure(result.message)
                                    }
                                } finally {
                                    isPreparingBlueprintPdf = false
                                }
                            }
                        },
                        enabled = !isPreparingBlueprintPdf && hasBlueprintGeometry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isPreparingBlueprintPdf) {
                                "Preparing Blueprint PDF..."
                            } else {
                                "Download Blueprint PDF"
                            }
                        )
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            runCatching {
                                pdfSafLauncher.launch(
                                    ExportStorage.buildFileName(
                                        projectName = name,
                                        suffix = "estimate",
                                        extension = "pdf"
                                    )
                                )
                            }.onFailure {
                                val message = if (it is ActivityNotFoundException) {
                                    "No file picker found on this device."
                                } else {
                                    "Could not open save dialog."
                                }
                                viewModel.reportExternalFailure(message)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF As...")
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            runCatching {
                                blueprintPdfSafLauncher.launch(
                                    ExportStorage.buildFileName(
                                        projectName = name,
                                        suffix = if (uiState.blueprintExportShowGrid) {
                                            "blueprint-grid"
                                        } else {
                                            "blueprint-no-grid"
                                        },
                                        extension = "pdf"
                                    )
                                )
                            }.onFailure {
                                val message = if (it is ActivityNotFoundException) {
                                    "No file picker found on this device."
                                } else {
                                    "Could not open save dialog."
                                }
                                viewModel.reportExternalFailure(message)
                            }
                        },
                        enabled = hasBlueprintGeometry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.blueprintExportShowGrid) {
                                "Save Blueprint PDF As... (Grid)"
                            } else {
                                "Save Blueprint PDF As... (No Grid)"
                            }
                        )
                    }
                    Text(
                        text = "Blueprint PNG Grid",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.blueprintExportShowGrid,
                            onClick = {
                                haptics.tap()
                                viewModel.setBlueprintGridExport(true)
                            },
                            label = { Text("Grid On") }
                        )
                        FilterChip(
                            selected = !uiState.blueprintExportShowGrid,
                            onClick = {
                                haptics.tap()
                                viewModel.setBlueprintGridExport(false)
                            },
                            label = { Text("Grid Off") }
                        )
                    }
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            runCatching {
                                blueprintPngSafLauncher.launch(
                                    ExportStorage.buildFileName(
                                        projectName = name,
                                        suffix = if (uiState.blueprintExportShowGrid) {
                                            "blueprint-grid"
                                        } else {
                                            "blueprint-no-grid"
                                        },
                                        extension = "png"
                                    )
                                )
                            }.onFailure {
                                val message = if (it is ActivityNotFoundException) {
                                    "No file picker found on this device."
                                } else {
                                    "Could not open save dialog."
                                }
                                viewModel.reportExternalFailure(message)
                            }
                        },
                        enabled = hasBlueprintGeometry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.blueprintExportShowGrid) {
                                "Save Blueprint PNG As... (Grid)"
                            } else {
                                "Save Blueprint PNG As... (No Grid)"
                            }
                        )
                    }
                    if (!hasBlueprintGeometry) {
                        Text(
                            text = "Blueprint exports require at least one wall, room, or opening.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Text(
                        text = "Data Files",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    SecondaryActionButton(
                        onClick = {
                            val name = uiState.project?.name ?: "project"
                            runCatching {
                                csvSafLauncher.launch(
                                    ExportStorage.buildFileName(
                                        projectName = name,
                                        suffix = "quantities",
                                        extension = "csv"
                                    )
                                )
                            }.onFailure {
                                val message = if (it is ActivityNotFoundException) {
                                    "No file picker found on this device."
                                } else {
                                    "Could not open save dialog."
                                }
                                viewModel.reportExternalFailure(message)
                            }
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
                            runCatching {
                                jsonSafLauncher.launch(
                                    ExportStorage.buildFileName(
                                        projectName = name,
                                        suffix = "backup",
                                        extension = "json"
                                    )
                                )
                            }.onFailure {
                                val message = if (it is ActivityNotFoundException) {
                                    "No file picker found on this device."
                                } else {
                                    "Could not open save dialog."
                                }
                                viewModel.reportExternalFailure(message)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save JSON Backup")
                    }
                }
            }
        } ?: Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "No estimate data for this trade yet. Open Materials & Pricing to generate quantities.",
                    style = MaterialTheme.typography.bodyMedium
                )
                SecondaryActionButton(
                    onClick = {
                        viewModel.recordTap("export_open_takeoff")
                        onOpenTakeoff()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Materials & Pricing")
                }
            }
        }

        uiState.lastAction?.let { action ->
            Card(
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
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
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                },
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

private fun launchExportIntent(
    context: Context,
    intent: Intent,
    noTargetMessage: String,
    onFailure: (String) -> Unit
) {
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        val message = if (error is ActivityNotFoundException) {
            noTargetMessage
        } else {
            "Could not open share sheet."
        }
        onFailure(message)
    }
}

@Composable
private fun ExportProjectHeaderCard(
    projectName: String,
    businessName: String?,
    takeoffType: String,
    result: TakeoffResult?,
    inputMode: TakeoffInputMode,
    hasMeasuredQuantities: Boolean,
    hasBlueprintGeometry: Boolean
) {
    val estimateSource = when {
        inputMode == TakeoffInputMode.MANUAL -> "Manual quantities"
        hasBlueprintGeometry -> "Blueprint geometry"
        else -> "No measured scope yet"
    }
    val statusText = when {
        result == null -> "Waiting for quantities"
        hasMeasuredQuantities -> "Preview ready"
        else -> "Needs measurements"
    }
    val statusNote = when {
        inputMode == TakeoffInputMode.MANUAL && hasMeasuredQuantities ->
            "This estimate is using manual quantities, so the blueprint can stay blank."
        !hasMeasuredQuantities && hasBlueprintGeometry ->
            "This project has geometry, but nothing matches the selected trade yet."
        !hasMeasuredQuantities ->
            "Draw in Blueprint or enter manual quantities in Materials & Pricing to generate an estimate."
        else -> null
    }
    Card(
        colors = appCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = appCardBorder(accented = true),
        elevation = appCardElevation(raised = true)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Export Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            PreviewMetricRow(
                label = "Project",
                value = projectName.ifBlank { "Untitled project" }
            )
            PreviewMetricRow(
                label = "Business",
                value = businessName ?: "Not set. Add your business name in Settings to include it on exported files."
            )
            PreviewMetricRow(
                label = "Trade",
                value = takeoffType
            )
            PreviewMetricRow(
                label = "Estimate Source",
                value = estimateSource
            )
            PreviewMetricRow(
                label = "Generated",
                value = Formatters.formatDateTime(System.currentTimeMillis()),
                showDivider = false
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (result == null || !hasMeasuredQuantities) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            statusNote?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (result != null) {
                PreviewMetricRow(
                    label = "Line Items",
                    value = result.nonZeroItems().size.toString(),
                    showDivider = hasMeasuredQuantities && result.totalCost != null
                )
                result.totalCost?.takeIf { hasMeasuredQuantities }?.let {
                    PreviewMetricRow(
                        label = "Estimated Total",
                        value = Formatters.formatMoney(it),
                        emphasize = true,
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalPreviewDeck(
    takeoffType: String,
    result: TakeoffResult,
    blueprint: BlueprintDocument?,
    selectedPage: EstimatePreviewPage,
    onSelectPage: (EstimatePreviewPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageIndex = EstimatePreviewPage.entries.indexOf(selectedPage)
    val hasPrevious = pageIndex > 0
    val hasNext = pageIndex < EstimatePreviewPage.entries.lastIndex

    Card(
        colors = appCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = appCardBorder(accented = true),
        elevation = appCardElevation(raised = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val pageNumber = pageIndex + 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$takeoffType Professional Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Page $pageNumber/${EstimatePreviewPage.entries.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                )
            }
            Text(
                text = selectedPage.docHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EstimatePreviewPage.entries.forEach { page ->
                    FilterChip(
                        selected = selectedPage == page,
                        onClick = { onSelectPage(page) },
                        label = { Text(page.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (hasPrevious) {
                            onSelectPage(EstimatePreviewPage.entries[pageIndex - 1])
                        }
                    },
                    enabled = hasPrevious
                ) {
                    Text("Previous Page")
                }
                TextButton(
                    onClick = {
                        if (hasNext) {
                            onSelectPage(EstimatePreviewPage.entries[pageIndex + 1])
                        }
                    },
                    enabled = hasNext
                ) {
                    Text("Next Page")
                }
            }

            Card(
                colors = appCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = appCardBorder(),
                elevation = appCardElevation()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPage.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Detailed Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    when (selectedPage) {
                        EstimatePreviewPage.COST -> CostPreviewPage(result = result)
                        EstimatePreviewPage.SHOPPING_LIST -> ShoppingListPreviewPage(result = result)
                        EstimatePreviewPage.BLUEPRINT -> BlueprintPreviewPage(blueprint = blueprint)
                        EstimatePreviewPage.COMBINED_NO_BLUEPRINT -> CombinedNoBlueprintPreviewPage(result = result)
                    }
                }
            }
        }
    }
}

@Composable
private fun CostPreviewPage(result: TakeoffResult) {
    val measuredItems = result.nonZeroItems()
    PreviewPageTitle(
        title = "Cost Sheet",
        subtitle = "Client-facing total and detailed cost breakout."
    )
    PreviewMetricRow(label = "Line Items", value = measuredItems.size.toString())
    if (!result.hasMeasuredQuantities()) {
        Text(
            text = "No measured quantities yet. Draw in Blueprint or enter manual quantities in Materials & Pricing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    result.materialSubtotal?.let {
        PreviewMetricRow(label = "Material Subtotal", value = Formatters.formatMoney(it))
    }
    result.laborCost?.let {
        PreviewMetricRow(label = "Labor", value = Formatters.formatMoney(it))
    }
    result.markupCost?.let {
        PreviewMetricRow(label = "Markup", value = Formatters.formatMoney(it))
    }
    result.taxCost?.let {
        PreviewMetricRow(label = "Tax", value = Formatters.formatMoney(it))
    }
    result.totalCost?.let {
        PreviewMetricRow(
            label = "Estimated Total",
            value = Formatters.formatMoney(it),
            emphasize = true
        )
    } ?: Text(
        text = "Pricing values are not fully configured, so a total cannot be shown yet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val pricedItems = measuredItems.filter { it.extendedCost != null }
    if (pricedItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Detailed Cost Breakdown",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        PreviewTableHeader(
            left = "Item",
            middle = "Qty x Unit",
            right = "Extended"
        )
        val sortedItems = pricedItems.sortedByDescending { it.extendedCost ?: 0.0 }
        sortedItems.forEachIndexed { index, line ->
            PreviewCostLineRow(
                line = line,
                showDivider = index < sortedItems.lastIndex
            )
        }
    } else {
        Text(
            text = "No priced line items are available yet for a detailed cost table.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShoppingListPreviewPage(result: TakeoffResult) {
    val measuredItems = result.nonZeroItems()
    PreviewPageTitle(
        title = "Shopping List",
        subtitle = "Purchase-ready quantities based on this estimate."
    )
    if (measuredItems.isEmpty()) {
        Text(
            text = "No measured line items yet. Generate quantities in Blueprint or Materials first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        PreviewTableHeader(
            left = "Item",
            middle = "Quantity",
            right = "Est. Cost"
        )
        measuredItems.forEachIndexed { index, line ->
            PreviewShoppingLineRow(
                line = line,
                index = index + 1,
                showDivider = index < measuredItems.lastIndex
            )
        }
    }
}

@Composable
private fun BlueprintPreviewPage(blueprint: BlueprintDocument?) {
    PreviewPageTitle(
        title = "Blueprint Sheet",
        subtitle = "Geometry summary and room-by-room footprint."
    )
    if (blueprint == null) {
        Text(
            text = "No blueprint data is available for this project yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val totalWallFeet = blueprint.walls.sumOf { it.lengthFeet() }
    val totalRoomArea = blueprint.rooms.sumOf { it.areaSqFt() }

    PreviewMetricRow(label = "Walls", value = blueprint.walls.size.toString())
    PreviewMetricRow(label = "Rooms", value = blueprint.rooms.size.toString())
    PreviewMetricRow(label = "Openings", value = blueprint.openings.size.toString())
    PreviewMetricRow(label = "Total Wall Length", value = "${Formatters.formatQuantity(totalWallFeet)} ft")
    PreviewMetricRow(label = "Room Floor Area", value = "${Formatters.formatQuantity(totalRoomArea)} sq ft")

    if (blueprint.rooms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Room Breakdown",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        val roomRows = blueprint.rooms.take(10)
        roomRows.forEachIndexed { index, room ->
            PreviewMetricRow(
                label = room.name,
                value = "${Formatters.formatQuantity(room.areaSqFt())} sq ft",
                showDivider = index < roomRows.lastIndex
            )
        }
    }
}

@Composable
private fun CombinedNoBlueprintPreviewPage(result: TakeoffResult) {
    val measuredItems = result.nonZeroItems()
    PreviewPageTitle(
        title = "Combined Estimate Sheet",
        subtitle = "Cost + Shopping List combined, blueprint intentionally excluded."
    )
    result.totalCost?.takeIf { result.hasMeasuredQuantities() }?.let {
        PreviewMetricRow(
            label = "Estimated Total",
            value = Formatters.formatMoney(it),
            emphasize = true
        )
    }
    result.materialSubtotal?.takeIf { result.hasMeasuredQuantities() }?.let {
        PreviewMetricRow(label = "Materials", value = Formatters.formatMoney(it))
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Included Shopping Items",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium
    )
    if (measuredItems.isEmpty()) {
        Text(
            text = "No measured shopping list items are available yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        PreviewTableHeader(
            left = "Item",
            middle = "Qty",
            right = "Unit"
        )
        measuredItems.forEachIndexed { index, line ->
            PreviewShoppingLineRow(
                line = line,
                compact = true,
                index = index + 1,
                showDivider = index < measuredItems.lastIndex
            )
        }
    }
}

@Composable
private fun PreviewMetricRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    showDivider: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = if (emphasize) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun PreviewTableHeader(
    left: String,
    middle: String,
    right: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = middle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = right,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
private fun PreviewCostLineRow(
    line: TakeoffLine,
    showDivider: Boolean = true
) {
    val extendedCost = line.extendedCost ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${Formatters.formatQuantity(line.quantity)} ${line.unit}" +
                        (line.unitCost?.let { " @ ${Formatters.formatMoney(it)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = Formatters.formatMoney(extendedCost),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun PreviewShoppingLineRow(
    line: TakeoffLine,
    compact: Boolean = false,
    index: Int? = null,
    showDivider: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val itemName = if (index != null) "$index. ${line.name}" else line.name
                Text(
                    text = itemName,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (compact) FontWeight.Medium else FontWeight.SemiBold
                )
                if (!compact) {
                    Text(
                        text = "Qty: ${Formatters.formatQuantity(line.quantity)} ${line.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (compact) {
                    "${Formatters.formatQuantity(line.quantity)} ${line.unit}"
                } else {
                    line.extendedCost?.let { Formatters.formatMoney(it) } ?: "--"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun PreviewPageTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun BlueprintDocument.hasGeometry(): Boolean {
    return walls.isNotEmpty() || rooms.isNotEmpty() || openings.isNotEmpty()
}

