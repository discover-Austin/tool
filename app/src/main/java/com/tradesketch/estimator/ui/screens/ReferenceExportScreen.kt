package com.tradesketch.estimator.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.nonZeroItems
import com.tradesketch.estimator.ui.components.ReferenceActionButton
import com.tradesketch.estimator.ui.components.ReferenceBlueprintBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintInk
import com.tradesketch.estimator.ui.components.ReferenceBlueprintMuted
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavy
import com.tradesketch.estimator.ui.components.ReferenceSectionFrame
import com.tradesketch.estimator.ui.components.ReferenceWorkspaceBackdrop
import com.tradesketch.estimator.ui.components.ReferenceWorksheetPanel
import com.tradesketch.estimator.ui.components.ReferenceWorksheetTitleBar
import com.tradesketch.estimator.ui.displayLabel
import com.tradesketch.estimator.ui.tutorial.ExportGuidedTutorialStep
import com.tradesketch.estimator.ui.tutorial.ExportGuidedTutorialTarget
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialBlipOverlay
import com.tradesketch.estimator.ui.tutorial.GuidedTutorialProgress
import com.tradesketch.estimator.ui.viewmodel.ExportActionResult
import com.tradesketch.estimator.ui.viewmodel.ExportStatusTone
import com.tradesketch.estimator.ui.viewmodel.ExportScopeMode
import com.tradesketch.estimator.ui.viewmodel.ExportViewModel
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import com.tradesketch.estimator.ui.viewmodel.shouldAutoClearExportStatus
import com.tradesketch.estimator.utils.ExportStorage
import com.tradesketch.estimator.utils.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ExportPreviewTab(val label: String) {
    COST("Cost"),
    SHOPPING("Shopping List"),
    BLUEPRINT("Blueprint")
}

@Composable
internal fun ReferenceExportScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenTakeoff: () -> Unit = {},
    guidedTutorialStep: ExportGuidedTutorialStep? = null,
    guidedTutorialProgress: GuidedTutorialProgress? = null,
    onGuidedTutorialBack: (() -> Unit)? = null,
    onGuidedTutorialNext: (() -> Unit)? = null,
    onGuidedTutorialSkip: (() -> Unit)? = null,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPreviewTab by rememberSaveable(projectId) { mutableStateOf(ExportPreviewTab.COST) }
    var isPreparingEstimatePdf by remember(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    var isPreparingBlueprintPdf by remember(projectId, uiState.selectedType?.name ?: "none") {
        mutableStateOf(false)
    }
    var summaryBounds by remember { mutableStateOf<Rect?>(null) }
    var previewBounds by remember { mutableStateOf<Rect?>(null) }
    var primaryActionsBounds by remember { mutableStateOf<Rect?>(null) }
    var titleBarBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current

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

    val csvSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) {
            viewModel.clearPendingAction()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            viewModel.announceActionInProgress("Saving CSV...")
            viewModel.saveCsvToUri(uri)
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
            viewModel.announceActionInProgress("Saving JSON backup...")
            viewModel.saveJsonToUri(uri)
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
            viewModel.announceActionInProgress("Saving estimate PDF...")
            viewModel.saveEstimatePdfToUri(uri)
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
            viewModel.announceActionInProgress("Saving blueprint PNG...")
            viewModel.saveBlueprintPngToUri(uri)
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
            viewModel.announceActionInProgress("Saving blueprint PDF...")
            viewModel.saveBlueprintPdfToUri(uri)
        }
    }

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.recordTap("export_screen_opened")
    }

    LaunchedEffect(uiState.status?.message, uiState.status?.tone) {
        val status = uiState.status ?: return@LaunchedEffect
        if (shouldAutoClearExportStatus(status)) {
            delay(2600)
            if (uiState.status == status) {
                viewModel.clearLastAction()
            }
        }
    }

    val sessionInputMode = uiState.project?.takeoffSession?.inputMode ?: TakeoffInputMode.BLUEPRINT
    val hasMeasuredQuantities = uiState.result?.nonZeroItems()?.isNotEmpty() == true
    val hasBlueprintGeometry = uiState.projectBlueprint?.let {
        it.walls.isNotEmpty() || it.rooms.isNotEmpty() || it.openings.isNotEmpty()
    } == true
    val selectedTradeLabel = if (uiState.exportScopeMode == ExportScopeMode.ALL_TRADES) {
        "All Trades"
    } else {
        uiState.selectedType?.displayLabel ?: "Choose trade"
    }
    val tutorialTargetBounds: List<Rect> = when (guidedTutorialStep?.target) {
        ExportGuidedTutorialTarget.PROJECT_SUMMARY -> listOfNotNull(summaryBounds)
        ExportGuidedTutorialTarget.PREVIEW -> listOfNotNull(previewBounds)
        ExportGuidedTutorialTarget.PRIMARY_ACTIONS -> listOfNotNull(primaryActionsBounds)
        null -> emptyList()
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        ReferenceWorkspaceBackdrop(
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 48.dp, end = 12.dp, bottom = 12.dp)
            ) {
                ReferenceWorksheetPanel(modifier = Modifier.fillMaxSize()) {
                ReferenceWorksheetTitleBar(
                    title = "Export Summary",
                    subtitle = "Preview, share, and save the current estimate.",
                    onBack = onBack,
                    modifier = Modifier.onGloballyPositioned {
                        titleBarBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                ReferenceSectionFrame(
                    title = "Project Summary",
                    modifier = Modifier.onGloballyPositioned {
                        summaryBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                ) {
                    Text(
                        text = "Project, trade, source, status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                    SummaryGridRow(
                        leftLabel = "Project",
                        leftValue = uiState.project?.name.orEmpty().ifBlank { "Untitled project" },
                        rightLabel = "Source",
                        rightValue = when {
                            sessionInputMode == TakeoffInputMode.MANUAL -> "Manual"
                            hasBlueprintGeometry -> "Blueprint geometry"
                            else -> "No geometry yet"
                        }
                    )
                    SummaryGridRow(
                        leftLabel = "Business",
                        leftValue = uiState.settings.businessName.ifBlank { "TradeSketch Estimator" },
                        rightLabel = "Generated",
                        rightValue = Formatters.formatDateTime(
                            uiState.generatedAtMillis ?: System.currentTimeMillis()
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryTradeDropdown(
                            label = selectedTradeLabel,
                            selectedScopeMode = uiState.exportScopeMode,
                            selectedType = uiState.selectedType,
                            onSelectAll = viewModel::selectAllTrades,
                            onSelectTrade = viewModel::selectTakeoffType,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryTotalPill(
                            total = uiState.result?.totalCost,
                            ready = hasMeasuredQuantities
                        )
                    }
                    Text(
                        text = if (hasMeasuredQuantities) {
                            "Preview ready"
                        } else {
                            "No quantities yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasMeasuredQuantities) ReferenceBlueprintInk else ReferenceBlueprintMuted
                    )
                }

                ReferenceSectionFrame(
                    title = "Preview",
                    modifier = Modifier.onGloballyPositioned {
                        previewBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                ) {
                    Text(
                        text = "Review the export output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                    Surface(
                        color = ReferenceBlueprintNavy,
                        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.82f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportPreviewTab.entries.forEach { tab ->
                                ReferenceExportChip(
                                    label = tab.label,
                                    selected = selectedPreviewTab == tab,
                                    onClick = { selectedPreviewTab = tab }
                                )
                            }
                        }
                    }
                    ExportPreviewSurface(
                        uiState = uiState,
                        selectedPreviewTab = selectedPreviewTab
                    )
                }

                ReferenceSectionFrame(
                    title = "Primary Actions",
                    modifier = Modifier.onGloballyPositioned {
                        primaryActionsBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                ) {
                    Text(
                        text = "Share or save the current report.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                    ActionGridRow(
                        left = {
                            ReferenceActionButton(
                                text = "Share Report",
                                onClick = {
                                    viewModel.announceActionInProgress("Opening share sheet...")
                                    val intent = viewModel.createShareIntent(shareCsv = false)
                                    launchExportIntent(
                                        context = context,
                                        intent = intent,
                                        noTargetMessage = "No app available to share this report.",
                                        onFailure = viewModel::reportExternalFailure,
                                        onSuccess = {
                                            viewModel.reportExternalSuccess("Share sheet opened.")
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                emphasize = true
                            )
                        },
                        right = {
                            ReferenceActionButton(
                                text = if (isPreparingEstimatePdf) {
                                    "Preparing PDF..."
                                } else {
                                    "Download PDF"
                                },
                                onClick = {
                                    viewModel.announceActionInProgress("Preparing estimate PDF...")
                                    isPreparingEstimatePdf = true
                                    coroutineScope.launch {
                                        try {
                                            viewModel.saveEstimatePdfToDownloads()
                                        } finally {
                                            isPreparingEstimatePdf = false
                                        }
                                    }
                                },
                                enabled = !isPreparingEstimatePdf,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )

                    ActionGridRow(
                        left = {
                            ReferenceActionButton(
                                text = if (isPreparingEstimatePdf) {
                                    "Preparing PDF..."
                                } else {
                                    "Share PDF"
                                },
                                onClick = {
                                    viewModel.announceActionInProgress("Preparing estimate PDF...")
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
                                                            viewModel.reportExternalSuccess("Share sheet opened.")
                                                        }
                                                    )
                                                }
                                                is ExportActionResult.Failure -> Unit
                                            }
                                        } finally {
                                            isPreparingEstimatePdf = false
                                        }
                                    }
                                },
                                enabled = !isPreparingEstimatePdf,
                                modifier = Modifier.weight(1f)
                            )
                        },
                        right = {
                            ReferenceActionButton(
                                text = "Save PDF As...",
                                onClick = {
                                    val name = uiState.project?.name ?: "project"
                                    openSaveDialog(
                                        ExportStorage.buildFileName(
                                            projectName = name,
                                            suffix = "estimate",
                                            extension = "pdf"
                                        ),
                                        "Opening save dialog...",
                                        pdfSafLauncher::launch
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )
                }
                ReferenceSectionFrame(title = "Additional Files") {
                    Text(
                        text = "Blueprint and data exports.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                    Text(
                        text = "Grid Overlay",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ReferenceBlueprintInk
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReferenceExportChip(
                            label = "Grid On",
                            selected = uiState.blueprintExportShowGrid,
                            onClick = { viewModel.setBlueprintGridExport(true) }
                        )
                        ReferenceExportChip(
                            label = "Grid Off",
                            selected = !uiState.blueprintExportShowGrid,
                            onClick = { viewModel.setBlueprintGridExport(false) }
                        )
                    }

                    ActionGridRow(
                        left = {
                            ReferenceActionButton(
                                text = if (isPreparingBlueprintPdf) {
                                    "Preparing Blueprint..."
                                } else {
                                    "Share Blueprint"
                                },
                                onClick = {
                                    viewModel.announceActionInProgress("Preparing blueprint PDF...")
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
                                                            viewModel.reportExternalSuccess("Share sheet opened.")
                                                        }
                                                    )
                                                }
                                                is ExportActionResult.Failure -> Unit
                                            }
                                        } finally {
                                            isPreparingBlueprintPdf = false
                                        }
                                    }
                                },
                                enabled = hasBlueprintGeometry && !isPreparingBlueprintPdf,
                                modifier = Modifier.weight(1f)
                            )
                        },
                        right = {
                            ReferenceActionButton(
                                text = "Download Blueprint",
                                onClick = {
                                    viewModel.announceActionInProgress("Preparing blueprint PDF...")
                                    isPreparingBlueprintPdf = true
                                    coroutineScope.launch {
                                        try {
                                            viewModel.saveBlueprintPdfToDownloads()
                                        } finally {
                                            isPreparingBlueprintPdf = false
                                        }
                                    }
                                },
                                enabled = hasBlueprintGeometry && !isPreparingBlueprintPdf,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )

                    ActionGridRow(
                        left = {
                            ReferenceActionButton(
                                text = "Blueprint PDF As...",
                                onClick = {
                                    val name = uiState.project?.name ?: "project"
                                    openSaveDialog(
                                        ExportStorage.buildFileName(
                                            projectName = name,
                                            suffix = if (uiState.blueprintExportShowGrid) "blueprint-grid" else "blueprint-no-grid",
                                            extension = "pdf"
                                        ),
                                        "Opening save dialog...",
                                        blueprintPdfSafLauncher::launch
                                    )
                                },
                                enabled = hasBlueprintGeometry,
                                modifier = Modifier.weight(1f)
                            )
                        },
                        right = {
                            ReferenceActionButton(
                                text = "Blueprint PNG As...",
                                onClick = {
                                    val name = uiState.project?.name ?: "project"
                                    openSaveDialog(
                                        ExportStorage.buildFileName(
                                            projectName = name,
                                            suffix = if (uiState.blueprintExportShowGrid) "blueprint-grid" else "blueprint-no-grid",
                                            extension = "png"
                                        ),
                                        "Opening save dialog...",
                                        blueprintPngSafLauncher::launch
                                    )
                                },
                                enabled = hasBlueprintGeometry,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )

                    ActionGridRow(
                        left = {
                            ReferenceActionButton(
                                text = "Save CSV",
                                onClick = {
                                    val name = uiState.project?.name ?: "project"
                                    openSaveDialog(
                                        ExportStorage.buildFileName(
                                            projectName = name,
                                            suffix = "quantities",
                                            extension = "csv"
                                        ),
                                        "Opening save dialog...",
                                        csvSafLauncher::launch
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        },
                        right = {
                            ReferenceActionButton(
                                text = "Save Project JSON",
                                onClick = {
                                    val name = uiState.project?.name ?: "project"
                                    openSaveDialog(
                                        ExportStorage.buildFileName(
                                            projectName = name,
                                            suffix = "backup",
                                            extension = "json"
                                        ),
                                        "Opening save dialog...",
                                        jsonSafLauncher::launch
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )
                }

                ReferenceSectionFrame(title = "Return") {
                    Text(
                        text = "Make changes before exporting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReferenceBlueprintMuted
                    )
                    ReferenceActionButton(
                        text = "Open Materials & Pricing",
                        onClick = onOpenTakeoff,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
                (uiState.status?.let { status ->
                    status.message to status.tone
                } ?: uiState.error?.let { error ->
                    error to ExportStatusTone.ERROR
                })?.let { (message, tone) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 18.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BannerSurface(
                            text = message,
                            tone = tone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        )
                    }
                }
            }
            if (
                guidedTutorialStep != null &&
                    guidedTutorialProgress != null &&
                    onGuidedTutorialNext != null &&
                    onGuidedTutorialSkip != null
            ) {
                GuidedTutorialBlipOverlay(
                    title = guidedTutorialStep.title,
                    message = guidedTutorialStep.message,
                    supporting = guidedTutorialStep.supporting,
                    progress = guidedTutorialProgress,
                    targetBounds = tutorialTargetBounds,
                    primaryActionLabel = guidedTutorialStep.primaryActionLabel,
                    minimumTopClearance = with(density) {
                        (((titleBarBounds?.bottom ?: 0f).toDp()) + 12.dp).coerceAtLeast(12.dp)
                    },
                    preferBottomPlacement = true,
                    onBack = onGuidedTutorialBack,
                    onNext = onGuidedTutorialNext,
                    onSkip = onGuidedTutorialSkip,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
}

@Composable
private fun SummaryGridRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryMetricCell(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f)
        )
        SummaryMetricCell(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ReferenceBlueprintMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = ReferenceBlueprintInk
        )
    }
}

@Composable
private fun SummaryTradeDropdown(
    label: String,
    selectedScopeMode: ExportScopeMode,
    selectedType: TakeoffType?,
    onSelectAll: () -> Unit,
    onSelectTrade: (TakeoffType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.8f)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Trade",
                        style = MaterialTheme.typography.labelSmall,
                        color = ReferenceBlueprintMuted
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ReferenceBlueprintInk
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ReferenceBlueprintNavy
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExportTradeMenuItem(
                label = "All Trades",
                selected = selectedScopeMode == ExportScopeMode.ALL_TRADES,
                onClick = {
                    onSelectAll()
                    expanded = false
                }
            )
            TakeoffType.entries.forEach { type ->
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
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) ReferenceBlueprintNavy else ReferenceBlueprintInk
            )
        },
        onClick = onClick
    )
}

@Composable
private fun SummaryTotalPill(
    total: Double?,
    ready: Boolean
) {
    Surface(
        color = ReferenceBlueprintNavy,
        border = BorderStroke(1.15.dp, Color(0xFFD19A21))
    ) {
        Text(
            text = if (ready && total != null) Formatters.formatMoney(total) else "Preview Pending",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ReferenceExportChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color.White.copy(alpha = 0.98f) else ReferenceBlueprintNavy.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.8f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) ReferenceBlueprintInk else Color.White
        )
    }
}

@Composable
private fun ActionGridRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        left()
        right()
    }
}

@Composable
private fun ExportPreviewSurface(
    uiState: com.tradesketch.estimator.ui.viewmodel.ExportUiState,
    selectedPreviewTab: ExportPreviewTab
) {
    Surface(
        color = ReferenceBlueprintNavy,
        border = BorderStroke(1.2.dp, ReferenceBlueprintBorder.copy(alpha = 0.95f)),
        shadowElevation = 2.dp
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, top = 5.dp, end = 5.dp, bottom = 6.dp),
            color = Color(0xFFF6F3E9),
            border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.74f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                when (selectedPreviewTab) {
                    ExportPreviewTab.COST -> CostPreviewBlock(uiState)
                    ExportPreviewTab.SHOPPING -> ShoppingPreviewBlock(uiState)
                    ExportPreviewTab.BLUEPRINT -> BlueprintPreviewBlock(uiState)
                }
            }
        }
    }
}

@Composable
private fun CostPreviewBlock(
    uiState: com.tradesketch.estimator.ui.viewmodel.ExportUiState
) {
    val result = uiState.result
    Text(
        text = "${uiState.takeoffType.ifBlank { uiState.selectedType?.displayLabel ?: "Estimate" }} Cost Sheet",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ReferenceBlueprintInk
    )
    if (result == null || result.nonZeroItems().isEmpty()) {
        Text(
            text = "No quantities yet.",
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted
        )
        return
    }
    PreviewMetricRow("Material Subtotal", result.materialSubtotal?.let(Formatters::formatMoney) ?: "--")
    PreviewMetricRow("Labor", result.laborCost?.let(Formatters::formatMoney) ?: "--")
    PreviewMetricRow("Markup", result.markupCost?.let(Formatters::formatMoney) ?: "--")
    PreviewMetricRow("Tax", result.taxCost?.let(Formatters::formatMoney) ?: "--")
    PreviewMetricRow(
        "Estimated Total",
        result.totalCost?.let(Formatters::formatMoney) ?: "--",
        emphasize = true
    )
    HorizontalDivider(color = ReferenceBlueprintBorder.copy(alpha = 0.3f))
    result.nonZeroItems().take(5).forEach { line ->
        PreviewMetricRow(
            line.name,
            "${Formatters.formatQuantity(line.quantity)} ${line.unit}"
        )
    }
}

@Composable
private fun ShoppingPreviewBlock(
    uiState: com.tradesketch.estimator.ui.viewmodel.ExportUiState
) {
    val items = uiState.result?.nonZeroItems().orEmpty()
    Text(
        text = "Shopping List",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ReferenceBlueprintInk
    )
    if (items.isEmpty()) {
        Text(
            text = "No line items yet.",
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted
        )
        return
    }
    items.take(8).forEachIndexed { index, line ->
        PreviewMetricRow(
            label = "${index + 1}. ${line.name}",
            value = "${Formatters.formatQuantity(line.quantity)} ${line.unit}"
        )
    }
}

@Composable
private fun BlueprintPreviewBlock(
    uiState: com.tradesketch.estimator.ui.viewmodel.ExportUiState
) {
    val blueprint = uiState.selectedTradeBlueprint
    Text(
        text = "Blueprint",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ReferenceBlueprintInk
    )
    if (blueprint == null) {
        Text(
            text = "No blueprint geometry yet.",
            style = MaterialTheme.typography.bodySmall,
            color = ReferenceBlueprintMuted
        )
        return
    }
    PreviewMetricRow("Walls", blueprint.walls.size.toString())
    PreviewMetricRow("Rooms", blueprint.rooms.size.toString())
    PreviewMetricRow("Openings", blueprint.openings.size.toString())
    PreviewMetricRow("Trade", uiState.takeoffType.ifBlank { uiState.selectedType?.displayLabel ?: "Estimate" })
}

@Composable
private fun PreviewMetricRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasize) ReferenceBlueprintInk else ReferenceBlueprintMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = ReferenceBlueprintInk
        )
    }
}

@Composable
private fun BannerSurface(
    text: String,
    tone: ExportStatusTone = ExportStatusTone.SUCCESS,
    modifier: Modifier = Modifier
) {
    val containerColor = when (tone) {
        ExportStatusTone.INFO -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
        ExportStatusTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
        ExportStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
    }
    val contentColor = when (tone) {
        ExportStatusTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        ExportStatusTone.SUCCESS -> MaterialTheme.colorScheme.onTertiaryContainer
        ExportStatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        border = BorderStroke(1.dp, ReferenceBlueprintBorder.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
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
