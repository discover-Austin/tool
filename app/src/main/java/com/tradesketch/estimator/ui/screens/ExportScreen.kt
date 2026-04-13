package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton
import com.tradesketch.estimator.ui.components.appCardBorder
import com.tradesketch.estimator.ui.components.appCardColors
import com.tradesketch.estimator.ui.components.appCardElevation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.tradesketch.estimator.ui.components.AppFilterChip
import com.tradesketch.estimator.ui.components.WorkspaceCompactPageHeader
import com.tradesketch.estimator.ui.components.WorkspaceSectionHeading
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ExportActionResult
import com.tradesketch.estimator.ui.viewmodel.ExportStatusTone
import com.tradesketch.estimator.ui.viewmodel.ExportScopeMode
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
    onBack: (() -> Unit)? = null,
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
    val openSaveDialog: (String, String, (String) -> Unit) -> Unit = { fileName, pendingMessage, launch ->
        viewModel.announceActionInProgress(pendingMessage)
        runCatching {
            launch(fileName)
        }.onFailure { error ->
            val message = if (error is ActivityNotFoundException) {
                "No file picker found on this device."
            } else {
                "Could not open save dialog."
            }
            viewModel.reportExternalFailure(message)
        }
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
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving CSV to the selected location...")
            reportFailure(viewModel.saveCsvToUri(uri))
        }
    }
    val jsonSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving the JSON backup to the selected location...")
            reportFailure(viewModel.saveJsonToUri(uri))
        }
    }
    val pdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving the estimate PDF to the selected location...")
            reportFailure(viewModel.saveEstimatePdfToUri(uri))
        }
    }
    val blueprintPngSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving the blueprint PNG to the selected location...")
            reportFailure(viewModel.saveBlueprintPngToUri(uri))
        }
    }
    val blueprintPdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving the blueprint PDF to the selected location...")
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

    val sessionInputMode = uiState.project?.takeoffSession?.inputMode ?: TakeoffInputMode.BLUEPRINT
    val hasBlueprintGeometry = uiState.projectBlueprint?.hasGeometry() == true
    val hasMeasuredQuantities = uiState.result?.hasMeasuredQuantities() == true
    val currentScopeLabel = exportScopeLabel(
        exportScopeMode = uiState.exportScopeMode,
        selectedType = uiState.selectedType
    )
    val availableTradeTypes = exportTradeMenuTypes(
        presentTradeLabels = uiState.presentTradeLabels,
        selectedType = uiState.selectedType
    )

    Box(modifier = modifier.fillMaxSize()) {
        WorkspaceCompactPageHeader(
            title = "Export",
            supportingText = "Switch the export scope in the summary card, then share the matching preview and files.",
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 102.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExportProjectHeaderCard(
                projectName = uiState.project?.name.orEmpty(),
                businessName = uiState.settings.businessName.ifBlank { null },
                exportScopeMode = uiState.exportScopeMode,
                selectedType = uiState.selectedType,
                availableTradeTypes = availableTradeTypes,
                generatedAtMillis = uiState.generatedAtMillis,
                result = uiState.result,
                inputMode = sessionInputMode,
                hasMeasuredQuantities = hasMeasuredQuantities,
                hasBlueprintGeometry = hasBlueprintGeometry,
                hasSelectedTradeGeometry = uiState.selectedTradeHasGeometry,
                onSelectAllTrades = viewModel::selectAllTrades,
                onSelectTrade = viewModel::selectTakeoffType
            )

            uiState.result?.let { result ->
                ProfessionalPreviewDeck(
                    takeoffType = if (uiState.exportScopeMode == ExportScopeMode.ALL_TRADES) {
                        currentScopeLabel
                    } else {
                        uiState.takeoffType.ifBlank {
                            uiState.selectedType?.displayLabel ?: "Estimate"
                        }
                    },
                    result = result,
                    blueprint = uiState.selectedTradeBlueprint,
                    useMetric = uiState.settings.useMetric,
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
                        WorkspaceSectionHeading(
                            title = "Share Report",
                            detail = exportReportScopeDetail(
                                exportScopeMode = uiState.exportScopeMode,
                                scopeLabel = currentScopeLabel,
                                presentTradeLabels = uiState.presentTradeLabels
                            ),
                            showDivider = false
                        )
                        PrimaryActionButton(
                            onClick = {
                                haptics.confirm()
                                viewModel.announceActionInProgress("Opening the report share sheet...")
                                viewModel.recordTap("export_share_full_report")
                                val intent = viewModel.createShareIntent(shareCsv = false)
                                launchExportIntent(
                                    context = context,
                                    intent = intent,
                                    noTargetMessage = "No app available to share this report.",
                                    onFailure = viewModel::reportExternalFailure,
                                    onSuccess = {
                                        viewModel.reportExternalSuccess("Share sheet opened for the report.")
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Report")
                        }
                        SecondaryActionButton(
                            onClick = {
                                haptics.confirm()
                                viewModel.announceActionInProgress("Preparing the estimate PDF for sharing...")
                                isPreparingEstimatePdf = true
                                coroutineScope.launch {
                                    try {
                                        when (val result = viewModel.createEstimatePdfShareIntent()) {
                                            is ExportActionResult.Success -> {
                                                launchExportIntent(
                                                    context = context,
                                                    intent = result.intent ?: return@launch,
                                                    noTargetMessage = "No app available to share this PDF.",
                                                    onFailure = viewModel::reportExternalFailure,
                                                    onSuccess = {
                                                        viewModel.reportExternalSuccess("Share sheet opened for the estimate PDF.")
                                                    }
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
                            Text(if (isPreparingEstimatePdf) "Preparing PDF..." else "Share PDF")
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        WorkspaceSectionHeading(
                            title = "Share Blueprint",
                            detail = if (hasBlueprintGeometry) {
                                exportBlueprintScopeDetail(
                                    exportScopeMode = uiState.exportScopeMode,
                                    scopeLabel = currentScopeLabel
                                )
                            } else {
                                "Blueprint exports require at least one wall, room, or opening."
                            },
                            showDivider = false
                        )
                        SecondaryActionButton(
                            onClick = {
                                haptics.confirm()
                                viewModel.announceActionInProgress("Preparing the blueprint PDF for sharing...")
                                isPreparingBlueprintPdf = true
                                coroutineScope.launch {
                                    try {
                                        when (val result = viewModel.createBlueprintPdfShareIntent()) {
                                            is ExportActionResult.Success -> {
                                                launchExportIntent(
                                                    context = context,
                                                    intent = result.intent ?: return@launch,
                                                    noTargetMessage = "No app available to share this blueprint PDF.",
                                                    onFailure = viewModel::reportExternalFailure,
                                                    onSuccess = {
                                                        viewModel.reportExternalSuccess("Share sheet opened for the blueprint PDF.")
                                                    }
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
                    WorkspaceSectionHeading(
                        title = "Save Report Files",
                        detail = defaultDeviceSaveHint(),
                        showDivider = false
                    )
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            viewModel.announceActionInProgress("Saving the estimate PDF to device...")
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
                        Text(if (isPreparingEstimatePdf) "Preparing PDF..." else "Download PDF")
                    }
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            val name = uiState.project?.name ?: "project"
                            openSaveDialog(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "estimate",
                                    extension = "pdf"
                                ),
                                "Opening the save dialog for the estimate PDF...",
                                pdfSafLauncher::launch
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF As...")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    WorkspaceSectionHeading(
                        title = "Save Blueprint Files",
                        detail = if (hasBlueprintGeometry) {
                            exportBlueprintScopeDetail(
                                exportScopeMode = uiState.exportScopeMode,
                                scopeLabel = currentScopeLabel
                            )
                        } else {
                            "Blueprint exports require at least one wall, room, or opening."
                        },
                        showDivider = false
                    )
                    Text(
                        text = "Blueprint Grid Overlay",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterChip(
                            selected = uiState.blueprintExportShowGrid,
                            onClick = {
                                haptics.tap()
                                viewModel.setBlueprintGridExport(true)
                            },
                            label = "Grid On"
                        )
                        AppFilterChip(
                            selected = !uiState.blueprintExportShowGrid,
                            onClick = {
                                haptics.tap()
                                viewModel.setBlueprintGridExport(false)
                            },
                            label = "Grid Off"
                        )
                    }
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            viewModel.announceActionInProgress("Saving the blueprint PDF to device...")
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
                            haptics.confirm()
                            val name = uiState.project?.name ?: "project"
                            openSaveDialog(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = if (uiState.blueprintExportShowGrid) {
                                        "blueprint-grid"
                                    } else {
                                        "blueprint-no-grid"
                                    },
                                    extension = "pdf"
                                ),
                                "Opening the save dialog for the blueprint PDF...",
                                blueprintPdfSafLauncher::launch
                            )
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
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            val name = uiState.project?.name ?: "project"
                            openSaveDialog(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = if (uiState.blueprintExportShowGrid) {
                                        "blueprint-grid"
                                    } else {
                                        "blueprint-no-grid"
                                    },
                                    extension = "png"
                                ),
                                "Opening the save dialog for the blueprint PNG...",
                                blueprintPngSafLauncher::launch
                            )
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    WorkspaceSectionHeading(
                        title = "Data Files",
                        detail = exportDataScopeDetail(
                            exportScopeMode = uiState.exportScopeMode,
                            scopeLabel = currentScopeLabel
                        ),
                        showDivider = false
                    )
                    SecondaryActionButton(
                        onClick = {
                            haptics.confirm()
                            val name = uiState.project?.name ?: "project"
                            openSaveDialog(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "quantities",
                                    extension = "csv"
                                ),
                                "Opening the save dialog for the CSV export...",
                                csvSafLauncher::launch
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
                            haptics.confirm()
                            val name = uiState.project?.name ?: "project"
                            openSaveDialog(
                                ExportStorage.buildFileName(
                                    projectName = name,
                                    suffix = "backup",
                                    extension = "json"
                                ),
                                "Opening the save dialog for the JSON backup...",
                                jsonSafLauncher::launch
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save JSON Backup")
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
                        text = "Need to revise quantities or pricing first?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Jump back to Materials & Pricing, make the change, then return here to export the updated estimate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        } ?: Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "No estimate data for $currentScopeLabel yet. Open Materials & Pricing to generate quantities.",
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

        val exportStatus = uiState.status
        when {
            exportStatus?.tone == ExportStatusTone.INFO -> {
                ExportFeedbackCard(
                    message = exportStatus.message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    inProgress = true
                )
            }

            exportStatus?.tone == ExportStatusTone.ERROR || uiState.error != null -> {
                ExportFeedbackCard(
                    message = exportStatus?.message ?: uiState.error.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    isError = true
                )
            }

            exportStatus?.tone == ExportStatusTone.SUCCESS -> {
                ExportFeedbackCard(
                    message = exportStatus.message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
    }
}

private fun launchExportIntent(
    context: Context,
    intent: Intent,
    noTargetMessage: String,
    onFailure: (String) -> Unit,
    onSuccess: () -> Unit = {}
) {
    runCatching {
        context.startActivity(intent)
        onSuccess()
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
private fun ExportFeedbackCard(
    message: String,
    modifier: Modifier = Modifier,
    inProgress: Boolean = false,
    isError: Boolean = false
) {
    Card(
        modifier = modifier.semantics {
            liveRegion = if (isError) {
                LiveRegionMode.Assertive
            } else {
                LiveRegionMode.Polite
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isError -> MaterialTheme.colorScheme.errorContainer
                inProgress -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                inProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                !isError -> {
                    Icon(Icons.Default.TaskAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExportProjectHeaderCard(
    projectName: String,
    businessName: String?,
    exportScopeMode: ExportScopeMode,
    selectedType: TakeoffType?,
    availableTradeTypes: List<TakeoffType>,
    generatedAtMillis: Long?,
    result: TakeoffResult?,
    inputMode: TakeoffInputMode,
    hasMeasuredQuantities: Boolean,
    hasBlueprintGeometry: Boolean,
    hasSelectedTradeGeometry: Boolean,
    onSelectAllTrades: () -> Unit,
    onSelectTrade: (TakeoffType) -> Unit
) {
    val scopeLabel = exportScopeLabel(
        exportScopeMode = exportScopeMode,
        selectedType = selectedType
    )
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
    val statusNote = exportStatusNote(
        inputMode = inputMode,
        exportScopeMode = exportScopeMode,
        hasMeasuredQuantities = hasMeasuredQuantities,
        hasBlueprintGeometry = hasBlueprintGeometry,
        hasSelectedTradeGeometry = hasSelectedTradeGeometry
    )
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
                text = "Current Export",
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
                label = "Estimate Source",
                value = estimateSource
            )
            PreviewMetricRow(
                label = "Generated",
                value = Formatters.formatDateTime(generatedAtMillis ?: System.currentTimeMillis()),
                showDivider = false
            )
            SummaryTradeDropdown(
                label = scopeLabel,
                selectedScopeMode = exportScopeMode,
                selectedType = selectedType,
                availableTradeTypes = availableTradeTypes,
                onSelectAll = onSelectAllTrades,
                onSelectTrade = onSelectTrade
            )
            Text(
                text = when {
                    inputMode == TakeoffInputMode.MANUAL && exportScopeMode == ExportScopeMode.ALL_TRADES ->
                        "Manual entry is active. All included combines every populated manual trade in this preview."
                    inputMode == TakeoffInputMode.MANUAL ->
                        "Manual entry is active. Preview and exports stay specific to this trade selection."
                    exportScopeMode == ExportScopeMode.ALL_TRADES ->
                        "Preview and exports include every populated trade in the project."
                    else ->
                        "Preview and exports stay specific to this trade selection."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

internal fun exportStatusNote(
    inputMode: TakeoffInputMode,
    exportScopeMode: ExportScopeMode,
    hasMeasuredQuantities: Boolean,
    hasBlueprintGeometry: Boolean,
    hasSelectedTradeGeometry: Boolean
): String? = when {
    inputMode == TakeoffInputMode.MANUAL && hasMeasuredQuantities ->
        "This estimate is using manual quantities, so the blueprint can stay blank."
    !hasMeasuredQuantities && exportScopeMode == ExportScopeMode.ALL_TRADES && hasBlueprintGeometry ->
        "This project has geometry in the included trades, but there are no measured quantities yet."
    !hasMeasuredQuantities && hasSelectedTradeGeometry ->
        "This trade has blueprint geometry, but there are no measured quantities yet."
    !hasMeasuredQuantities && hasBlueprintGeometry ->
        "This project has geometry, but nothing matches the selected trade yet."
    !hasMeasuredQuantities ->
        "Draw in Blueprint or enter manual quantities in Materials & Pricing to generate an estimate."
    else -> null
}

@Composable
private fun SummaryTradeDropdown(
    label: String,
    selectedScopeMode: ExportScopeMode,
    selectedType: TakeoffType?,
    availableTradeTypes: List<TakeoffType>,
    onSelectAll: () -> Unit,
    onSelectTrade: (TakeoffType) -> Unit
) {
    var expanded by rememberSaveable(label, selectedScopeMode, selectedType?.name) {
        mutableStateOf(false)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Trade Scope",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExportTradeMenuItem(
                label = "All Included",
                selected = selectedScopeMode == ExportScopeMode.ALL_TRADES,
                onClick = {
                    onSelectAll()
                    expanded = false
                }
            )
            availableTradeTypes.forEach { type ->
                ExportTradeMenuItem(
                    label = type.displayLabel,
                    selected = selectedScopeMode == ExportScopeMode.SINGLE_TRADE && selectedType == type,
                    onClick = {
                        onSelectTrade(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExportTradeMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick
    )
}

private fun exportScopeLabel(
    exportScopeMode: ExportScopeMode,
    selectedType: TakeoffType?
): String {
    return if (exportScopeMode == ExportScopeMode.ALL_TRADES) {
        "All Included"
    } else {
        selectedType?.displayLabel ?: "Choose a trade"
    }
}

internal fun exportTradeMenuTypes(
    presentTradeLabels: List<String>,
    selectedType: TakeoffType?
): List<TakeoffType> {
    val presentTradeTypes = TakeoffType.entries.filter { type ->
        presentTradeLabels.contains(type.displayLabel)
    }
    return when {
        presentTradeTypes.isEmpty() -> TakeoffType.entries.toList()
        selectedType != null && selectedType !in presentTradeTypes -> (presentTradeTypes + selectedType).distinct()
        else -> presentTradeTypes
    }
}

private fun exportReportScopeDetail(
    exportScopeMode: ExportScopeMode,
    scopeLabel: String,
    presentTradeLabels: List<String>
): String {
    return if (exportScopeMode == ExportScopeMode.ALL_TRADES) {
        if (presentTradeLabels.isEmpty()) {
            "All included will populate as blueprint geometry or manual trade entries are added."
        } else {
            "Preview and report entries are grouped by trade for ${presentTradeLabels.joinToString()}."
        }
    } else {
        "Preview and report entries are specific to $scopeLabel."
    }
}

private fun exportBlueprintScopeDetail(
    exportScopeMode: ExportScopeMode,
    scopeLabel: String
): String {
    return if (exportScopeMode == ExportScopeMode.ALL_TRADES) {
        "Blueprint PDF and PNG exports include every populated trade in the current scope."
    } else {
        "Blueprint PDF and PNG exports stay specific to $scopeLabel."
    }
}

private fun exportDataScopeDetail(
    exportScopeMode: ExportScopeMode,
    scopeLabel: String
): String {
    return if (exportScopeMode == ExportScopeMode.ALL_TRADES) {
        "CSV and JSON include every populated trade from blueprint or manual entry."
    } else {
        "CSV and JSON include only the $scopeLabel entries."
    }
}

@Composable
private fun ProfessionalPreviewDeck(
    takeoffType: String,
    result: TakeoffResult,
    blueprint: BlueprintDocument?,
    useMetric: Boolean,
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
                    AppFilterChip(
                        selected = selectedPage == page,
                        onClick = { onSelectPage(page) },
                        label = page.label
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
                        EstimatePreviewPage.BLUEPRINT -> BlueprintPreviewPage(
                            blueprint = blueprint,
                            useMetric = useMetric
                        )
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
private fun BlueprintPreviewPage(
    blueprint: BlueprintDocument?,
    useMetric: Boolean
) {
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
    PreviewMetricRow(label = "Total Wall Length", value = Formatters.formatLength(totalWallFeet, useMetric))
    PreviewMetricRow(label = "Room Floor Area", value = Formatters.formatArea(totalRoomArea, useMetric))

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
                value = Formatters.formatArea(room.areaSqFt(), useMetric),
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

