package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.ui.components.PrimaryActionButton
import com.tradesketch.estimator.ui.components.SecondaryActionButton

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.domain.model.openingsAreaSqFt
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import com.tradesketch.estimator.ui.viewmodel.ProjectDetailViewModel
import com.tradesketch.estimator.utils.BlueprintExportManager
import java.util.UUID
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyUiState by viewModel.historyUiState.collectAsState()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSpaceEditor by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<Space?>(null) }
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var showQuickRoomDialog by remember { mutableStateOf(false) }
    var quickRoomDialogKey by remember { mutableIntStateOf(0) }
    var layerFilter by rememberSaveable(projectId) { mutableStateOf(BlueprintLayerFilter.ALL) }
    var snapStepFeet by rememberSaveable(projectId) { mutableDoubleStateOf(1.0) }
    var fullScreenBlueprint by rememberSaveable(projectId) { mutableStateOf(true) }
    var isExportingBlueprint by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.recordTap("blueprint_screen_opened")
    }
    LaunchedEffect(fullScreenBlueprint, onFullscreenBlueprintChanged) {
        onFullscreenBlueprintChanged(fullScreenBlueprint)
    }

    Box(modifier = modifier) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.project != null -> {
                val project = uiState.project!!
                BlueprintContent(
                    project = project,
                    settings = uiState.settings,
                    onRequestAddSpace = {
                        viewModel.recordTap("blueprint_open_add_space")
                        showAddMethodDialog = true
                    },
                    onDirectAddSpace = { viewModel.addSpace(it) },
                    onQuickRoom = {
                        haptics.confirm()
                        viewModel.recordTap("blueprint_quick_room")
                        showQuickRoomDialog = true
                    },
                    onQuickAddWall = {
                        haptics.confirm()
                        viewModel.recordTap("blueprint_quick_wall")
                        val wallCount = project.spaces.count { it.geometry is Geometry.Wall } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Wall $wallCount",
                                geometry = Geometry.Wall(
                                    length = mmFromFeet(12.0),
                                    height = mmFromFeet(9.0)
                                )
                            )
                        )
                    },
                    onQuickAddSlab = {
                        haptics.confirm()
                        viewModel.recordTap("blueprint_quick_slab")
                        val slabCount = project.spaces.count { it.geometry is Geometry.Slab } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Slab $slabCount",
                                geometry = Geometry.Slab(
                                    length = mmFromFeet(12.0),
                                    width = mmFromFeet(10.0),
                                    thickness = mmFromFeet(0.33)
                                )
                            )
                        )
                    },
                    onQuickAddBed = {
                        haptics.confirm()
                        viewModel.recordTap("blueprint_quick_bed")
                        val bedCount = project.spaces.count { space ->
                            space.geometry !is Geometry.Wall &&
                                space.geometry !is Geometry.Slab &&
                                isLandscapeSpaceName(space.name)
                        } + 1
                        viewModel.addSpace(
                            Space(
                                id = UUID.randomUUID().toString(),
                                name = "Gravel/Mulch Bed $bedCount",
                                geometry = Geometry.Rect(
                                    length = mmFromFeet(14.0),
                                    width = mmFromFeet(6.0)
                                )
                            )
                        )
                    },
                    onEditSpace = { space ->
                        viewModel.recordTap("blueprint_edit_space")
                        editingSpace = space
                        showSpaceEditor = true
                    },
                    onDuplicateSpace = { viewModel.duplicateSpace(it) },
                    onDeleteSpace = { viewModel.deleteSpace(it) },
                    onUpdateSpaceTransform = { spaceId, transform ->
                        viewModel.updateSpaceTransform(spaceId, transform)
                    },
                    onUpdateSpace = { space ->
                        viewModel.updateSpace(space)
                    },
                    onAutoLayout = { viewModel.autoLayoutSpaces() },
                    onFlattenElevations = { viewModel.flattenElevations() },
                    onSnapToGrid = { stepFeet -> viewModel.snapLayoutToGrid(stepFeet) },
                    onOptimizeLayout = { viewModel.optimizeBlueprintLayout(snapStepFeet) },
                    onCenterLayout = { viewModel.centerLayoutAtOrigin() },
                    onAlignNorth = { viewModel.alignLayoutToCardinal() },
                    snapStepFeet = snapStepFeet,
                    onSnapStepChange = { snapStepFeet = it },
                    layerFilter = layerFilter,
                    onLayerFilterChange = { layerFilter = it },
                    fullScreenBlueprint = fullScreenBlueprint,
                    onToggleFullScreenBlueprint = { fullScreenBlueprint = !fullScreenBlueprint },
                    canUndo = historyUiState.canUndo,
                    canRedo = historyUiState.canRedo,
                    undoCount = historyUiState.undoCount,
                    redoCount = historyUiState.redoCount,
                    onUndo = { viewModel.undoBlueprintChange() },
                    onRedo = { viewModel.redoBlueprintChange() },
                    onDownloadBlueprint = {
                        val exportSpaces = spacesForLayer(project.spaces, layerFilter)
                        if (exportSpaces.isEmpty()) {
                            Toast.makeText(context, "No spaces in this layer to export.", Toast.LENGTH_SHORT).show()
                        } else if (!isExportingBlueprint) {
                            isExportingBlueprint = true
                            scope.launch {
                                try {
                                    val uri = BlueprintExportManager.saveBlueprintToDownloads(
                                        context = context,
                                        projectName = project.name,
                                        spaces = exportSpaces
                                    )
                                    if (uri != null) {
                                        haptics.confirm()
                                        Toast.makeText(
                                            context,
                                            "Blueprint downloaded to TradeSketch folder.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Could not save blueprint.", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isExportingBlueprint = false
                                }
                            }
                        }
                    },
                    onDownloadBlueprintPdf = {
                        val exportSpaces = spacesForLayer(project.spaces, layerFilter)
                        if (exportSpaces.isEmpty()) {
                            Toast.makeText(context, "No spaces in this layer to export.", Toast.LENGTH_SHORT).show()
                        } else if (!isExportingBlueprint) {
                            isExportingBlueprint = true
                            scope.launch {
                                try {
                                    val uri = BlueprintExportManager.saveBlueprintPdfToDownloads(
                                        context = context,
                                        projectName = project.name,
                                        spaces = exportSpaces
                                    )
                                    if (uri != null) {
                                        haptics.confirm()
                                        Toast.makeText(
                                            context,
                                            "Blueprint PDF downloaded to TradeSketch folder.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Could not save blueprint PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isExportingBlueprint = false
                                }
                            }
                        }
                    },
                    onShareBlueprint = {
                        val exportSpaces = spacesForLayer(project.spaces, layerFilter)
                        if (exportSpaces.isEmpty()) {
                            Toast.makeText(context, "No spaces in this layer to share.", Toast.LENGTH_SHORT).show()
                        } else if (!isExportingBlueprint) {
                            isExportingBlueprint = true
                            scope.launch {
                                try {
                                    val shareIntent = BlueprintExportManager.createBlueprintShareIntent(
                                        context = context,
                                        projectName = project.name,
                                        spaces = exportSpaces
                                    )
                                    if (shareIntent != null) {
                                        haptics.confirm()
                                        context.startActivity(shareIntent)
                                    } else {
                                        Toast.makeText(context, "Could not prepare share image.", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isExportingBlueprint = false
                                }
                            }
                        }
                    },
                    onShareBlueprintPdf = {
                        val exportSpaces = spacesForLayer(project.spaces, layerFilter)
                        if (exportSpaces.isEmpty()) {
                            Toast.makeText(context, "No spaces in this layer to share.", Toast.LENGTH_SHORT).show()
                        } else if (!isExportingBlueprint) {
                            isExportingBlueprint = true
                            scope.launch {
                                try {
                                    val shareIntent = BlueprintExportManager.createBlueprintPdfShareIntent(
                                        context = context,
                                        projectName = project.name,
                                        spaces = exportSpaces
                                    )
                                    if (shareIntent != null) {
                                        haptics.confirm()
                                        context.startActivity(shareIntent)
                                    } else {
                                        Toast.makeText(context, "Could not prepare blueprint PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isExportingBlueprint = false
                                }
                            }
                        }
                    },
                    isExportingBlueprint = isExportingBlueprint,
                    onOpenTakeoff = {
                        viewModel.recordTap("blueprint_open_takeoff")
                        onOpenTakeoff()
                    }
                )
            }
        }
    }

    if (showAddMethodDialog) {
        AddSpaceMethodDialog(
            onDismiss = { showAddMethodDialog = false },
            onQuickRoom = {
                haptics.confirm()
                showAddMethodDialog = false
                showQuickRoomDialog = true
            },
            onCustomSpace = {
                haptics.tap()
                showAddMethodDialog = false
                editingSpace = null
                showSpaceEditor = true
            }
        )
    }

    if (showQuickRoomDialog) {
        QuickRoomDialog(
            dialogKey = quickRoomDialogKey,
            suggestedRoomName = uiState.project?.let { project ->
                nextSuggestedRoomName(project.spaces)
            } ?: "Room 1",
            onDismiss = { showQuickRoomDialog = false },
            onSave = { spaces, continueToNextRoom ->
                haptics.confirm()
                viewModel.addSpaces(spaces)
                if (continueToNextRoom) {
                    quickRoomDialogKey += 1
                } else {
                    showQuickRoomDialog = false
                }
            }
        )
    }

    if (showSpaceEditor && uiState.project != null) {
        SpaceEditorDialog(
            initialSpace = editingSpace,
            onDismiss = { showSpaceEditor = false },
            onSave = { space ->
                haptics.confirm()
                if (editingSpace == null) {
                    viewModel.addSpace(space)
                } else {
                    viewModel.updateSpace(space)
                }
                showSpaceEditor = false
            }
        )
    }
}

@Composable
private fun BlueprintContent(
    project: Project,
    settings: Settings,
    onRequestAddSpace: () -> Unit,
    onDirectAddSpace: (Space) -> Unit,
    onQuickRoom: () -> Unit,
    onQuickAddWall: () -> Unit,
    onQuickAddSlab: () -> Unit,
    onQuickAddBed: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onUpdateSpaceTransform: (String, SpaceTransform) -> Unit,
    onUpdateSpace: (Space) -> Unit,
    onAutoLayout: () -> Unit,
    onFlattenElevations: () -> Unit,
    onSnapToGrid: (Double) -> Unit,
    onOptimizeLayout: () -> Unit,
    onCenterLayout: () -> Unit,
    onAlignNorth: () -> Unit,
    snapStepFeet: Double,
    onSnapStepChange: (Double) -> Unit,
    layerFilter: BlueprintLayerFilter,
    onLayerFilterChange: (BlueprintLayerFilter) -> Unit,
    fullScreenBlueprint: Boolean,
    onToggleFullScreenBlueprint: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    undoCount: Int,
    redoCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDownloadBlueprint: () -> Unit,
    onDownloadBlueprintPdf: () -> Unit,
    onShareBlueprint: () -> Unit,
    onShareBlueprintPdf: () -> Unit,
    isExportingBlueprint: Boolean,
    onOpenTakeoff: () -> Unit
) {
    val measuredArea = project.spaces.sumOf { it.geometry.areaSqFt() }
    val hasMeasuredArea = measuredArea > 0.0
    val wallCount = project.spaces.count { it.geometry is Geometry.Wall }
    val slabCount = project.spaces.count { it.geometry is Geometry.Slab }
    val roomCount = project.spaces.size - wallCount - slabCount
    val overlapCount = blueprintOverlapCount(project.spaces)
    val unplacedCount = project.spaces.count { it.transform == SpaceTransform() }
    val elevatedCount = project.spaces.count { abs(it.transform.yFeet) > 0.01 }
    val spacesInCurrentLayer = spacesForLayer(project.spaces, layerFilter).size
    val netArea = project.spaces.sumOf { (it.geometry.areaSqFt() - it.openingsAreaSqFt()).coerceAtLeast(0.0) }
    val envelope = blueprintEnvelope(project.spaces)
    val envelopeArea = envelope?.let {
        ((it.maxX - it.minX) * (it.maxZ - it.minZ)).coerceAtLeast(0.0)
    } ?: 0.0
    val coverageDensity = if (envelopeArea > 0.0) {
        ((measuredArea / envelopeArea) * 100.0).coerceIn(0.0, 100.0)
    } else {
        0.0
    }
    val longestSpan = envelope?.let {
        max(it.maxX - it.minX, it.maxZ - it.minZ)
    } ?: 0.0
    val readiness = blueprintReadinessScore(
        totalSpaces = project.spaces.size,
        measuredArea = measuredArea,
        overlapCount = overlapCount,
        unplacedCount = unplacedCount,
        elevatedCount = elevatedCount
    )
    if (fullScreenBlueprint) {
        Box(modifier = Modifier.fillMaxSize()) {
            ModelBuilder3DPanel(
                project = project,
                onAddSpace = onRequestAddSpace,
                onEditSpace = onEditSpace,
                onDuplicateSpace = onDuplicateSpace,
                onDeleteSpace = onDeleteSpace,
                onAutoLayout = onAutoLayout,
                onUpdateTransform = onUpdateSpaceTransform,
                onUpdateSpace = onUpdateSpace,
                immersiveMode = true,
                blueprintMode = true,
                calmModeEnabled = settings.calmModeEnabled,
                workflowAidsEnabled = settings.workflowAidsEnabled,
                preferredLayerFilter = layerFilter,
                onQuickRoom = onQuickRoom,
                onQuickAddWall = onQuickAddWall,
                onQuickAddSlab = onQuickAddSlab,
                onQuickAddBed = onQuickAddBed,
                onBlueprintLayerFilterChange = onLayerFilterChange,
                onDrawWallSegment = { startX, startZ, endX, endZ ->
                    val lengthFeet = hypot(endX - startX, endZ - startZ)
                    if (lengthFeet < 1.0) return@ModelBuilder3DPanel
                    val wallCount = project.spaces.count { it.geometry is Geometry.Wall } + 1
                    val centerX = (startX + endX) / 2.0
                    val centerZ = (startZ + endZ) / 2.0
                    val yawDegrees = Math.toDegrees(atan2((endZ - startZ), (endX - startX)))
                    val wall = Space(
                        id = UUID.randomUUID().toString(),
                        name = "Wall $wallCount",
                        geometry = Geometry.Wall(
                            length = mmFromFeet(lengthFeet),
                            height = mmFromFeet(9.0)
                        ),
                        transform = SpaceTransform(
                            xFeet = centerX,
                            yFeet = 0.0,
                            zFeet = centerZ,
                            yawDegrees = yawDegrees
                        )
                    )
                    onDirectAddSpace(wall)
                },
                onOptimizeLayout = onOptimizeLayout,
                onCenterLayout = onCenterLayout,
                onAlignNorth = onAlignNorth,
                onUndoBlueprint = onUndo,
                onRedoBlueprint = onRedo,
                canUndoBlueprint = canUndo,
                canRedoBlueprint = canRedo,
                onDownloadBlueprintPng = onDownloadBlueprint,
                onShareBlueprintPng = onShareBlueprint,
                onDownloadBlueprintPdf = onDownloadBlueprintPdf,
                onShareBlueprintPdf = onShareBlueprintPdf,
                fullScreenBlueprint = true,
                onToggleBlueprintFullscreen = onToggleFullScreenBlueprint,
                modifier = Modifier.fillMaxSize()
            )
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Need quick controls?",
                        style = MaterialTheme.typography.labelLarge
                    )
                    SecondaryActionButton(onClick = onToggleFullScreenBlueprint) {
                        Text("Open Control Panel")
                    }
                }
            }
        }
        return
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isWideLayout = maxWidth >= 920.dp
        val compactControls = maxWidth < 620.dp
        val compactControlMaxHeight = this.maxHeight * 0.56f

        if (isWideLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 340.dp, max = 440.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Text(
                                text = "Blueprint source of truth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BlueprintCommandCenterCard(
                        project = project,
                        calmModeEnabled = settings.calmModeEnabled,
                        workflowAidsEnabled = settings.workflowAidsEnabled,
                        readiness = readiness,
                        wallCount = wallCount,
                        slabCount = slabCount,
                        roomCount = roomCount,
                        measuredArea = measuredArea,
                        netArea = netArea,
                        envelopeArea = envelopeArea,
                        coverageDensity = coverageDensity,
                        longestSpan = longestSpan,
                        overlapCount = overlapCount,
                        unplacedCount = unplacedCount,
                        elevatedCount = elevatedCount,
                        hasMeasuredArea = hasMeasuredArea,
                        layerFilter = layerFilter,
                        onLayerFilterChange = onLayerFilterChange,
                        snapStepFeet = snapStepFeet,
                        onSnapStepChange = onSnapStepChange,
                        onOptimizeLayout = onOptimizeLayout,
                        onOpenTakeoff = onOpenTakeoff,
                        onRequestAddSpace = onRequestAddSpace,
                        onAutoLayout = onAutoLayout,
                        onFlattenElevations = onFlattenElevations,
                        onSnapToGrid = onSnapToGrid,
                        onCenterLayout = onCenterLayout,
                        onAlignNorth = onAlignNorth,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        undoCount = undoCount,
                        redoCount = redoCount,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onDownloadBlueprint = onDownloadBlueprint,
                        onDownloadBlueprintPdf = onDownloadBlueprintPdf,
                        onShareBlueprint = onShareBlueprint,
                        onShareBlueprintPdf = onShareBlueprintPdf,
                        spacesInCurrentLayer = spacesInCurrentLayer,
                        isExportingBlueprint = isExportingBlueprint,
                        onOpenFullscreenEditor = onToggleFullScreenBlueprint,
                        compactControls = false,
                        modifier = Modifier.fillMaxWidth(),
                        scrollContent = false
                    )
                }
                ModelBuilder3DPanel(
                    project = project,
                    onAddSpace = onRequestAddSpace,
                    onEditSpace = onEditSpace,
                    onDuplicateSpace = onDuplicateSpace,
                    onDeleteSpace = onDeleteSpace,
                    onAutoLayout = onAutoLayout,
                    onUpdateTransform = onUpdateSpaceTransform,
                    onUpdateSpace = onUpdateSpace,
                    immersiveMode = true,
                    blueprintMode = true,
                    calmModeEnabled = settings.calmModeEnabled,
                    workflowAidsEnabled = settings.workflowAidsEnabled,
                    preferredLayerFilter = layerFilter,
                    onQuickRoom = onQuickRoom,
                    onQuickAddWall = onQuickAddWall,
                    onQuickAddSlab = onQuickAddSlab,
                    onQuickAddBed = onQuickAddBed,
                    onBlueprintLayerFilterChange = onLayerFilterChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = "Blueprint source of truth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BlueprintCommandCenterCard(
                    project = project,
                    calmModeEnabled = settings.calmModeEnabled,
                    workflowAidsEnabled = settings.workflowAidsEnabled,
                    readiness = readiness,
                    wallCount = wallCount,
                    slabCount = slabCount,
                    roomCount = roomCount,
                    measuredArea = measuredArea,
                    netArea = netArea,
                    envelopeArea = envelopeArea,
                    coverageDensity = coverageDensity,
                    longestSpan = longestSpan,
                    overlapCount = overlapCount,
                    unplacedCount = unplacedCount,
                    elevatedCount = elevatedCount,
                    hasMeasuredArea = hasMeasuredArea,
                    layerFilter = layerFilter,
                    onLayerFilterChange = onLayerFilterChange,
                    snapStepFeet = snapStepFeet,
                    onSnapStepChange = onSnapStepChange,
                    onOptimizeLayout = onOptimizeLayout,
                    onOpenTakeoff = onOpenTakeoff,
                    onRequestAddSpace = onRequestAddSpace,
                    onAutoLayout = onAutoLayout,
                    onFlattenElevations = onFlattenElevations,
                    onSnapToGrid = onSnapToGrid,
                    onCenterLayout = onCenterLayout,
                    onAlignNorth = onAlignNorth,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    undoCount = undoCount,
                    redoCount = redoCount,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onDownloadBlueprint = onDownloadBlueprint,
                    onDownloadBlueprintPdf = onDownloadBlueprintPdf,
                    onShareBlueprint = onShareBlueprint,
                    onShareBlueprintPdf = onShareBlueprintPdf,
                    spacesInCurrentLayer = spacesInCurrentLayer,
                    isExportingBlueprint = isExportingBlueprint,
                    onOpenFullscreenEditor = onToggleFullScreenBlueprint,
                    compactControls = compactControls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = compactControlMaxHeight),
                    scrollContent = true
                )
                ModelBuilder3DPanel(
                    project = project,
                    onAddSpace = onRequestAddSpace,
                    onEditSpace = onEditSpace,
                    onDuplicateSpace = onDuplicateSpace,
                    onDeleteSpace = onDeleteSpace,
                    onAutoLayout = onAutoLayout,
                    onUpdateTransform = onUpdateSpaceTransform,
                    onUpdateSpace = onUpdateSpace,
                    immersiveMode = true,
                    blueprintMode = true,
                    calmModeEnabled = settings.calmModeEnabled,
                    workflowAidsEnabled = settings.workflowAidsEnabled,
                    preferredLayerFilter = layerFilter,
                    onQuickRoom = onQuickRoom,
                    onQuickAddWall = onQuickAddWall,
                    onQuickAddSlab = onQuickAddSlab,
                    onQuickAddBed = onQuickAddBed,
                    onBlueprintLayerFilterChange = onLayerFilterChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun BlueprintCommandCenterCard(
    project: Project,
    calmModeEnabled: Boolean,
    workflowAidsEnabled: Boolean,
    readiness: Int,
    wallCount: Int,
    slabCount: Int,
    roomCount: Int,
    measuredArea: Double,
    netArea: Double,
    envelopeArea: Double,
    coverageDensity: Double,
    longestSpan: Double,
    overlapCount: Int,
    unplacedCount: Int,
    elevatedCount: Int,
    hasMeasuredArea: Boolean,
    layerFilter: BlueprintLayerFilter,
    onLayerFilterChange: (BlueprintLayerFilter) -> Unit,
    snapStepFeet: Double,
    onSnapStepChange: (Double) -> Unit,
    onOptimizeLayout: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onRequestAddSpace: () -> Unit,
    onAutoLayout: () -> Unit,
    onFlattenElevations: () -> Unit,
    onSnapToGrid: (Double) -> Unit,
    onCenterLayout: () -> Unit,
    onAlignNorth: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    undoCount: Int,
    redoCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDownloadBlueprint: () -> Unit,
    onDownloadBlueprintPdf: () -> Unit,
    onShareBlueprint: () -> Unit,
    onShareBlueprintPdf: () -> Unit,
    spacesInCurrentLayer: Int,
    isExportingBlueprint: Boolean,
    onOpenFullscreenEditor: () -> Unit,
    compactControls: Boolean,
    modifier: Modifier = Modifier,
    scrollContent: Boolean = false
) {
    val needsLayoutFix = overlapCount > 0 || unplacedCount > 0 || elevatedCount > 0
    val hasLayerSpaces = spacesInCurrentLayer > 0
    val readinessLabel = when {
        readiness >= 90 -> "Ready for takeoff"
        readiness >= 70 -> "Nearly ready"
        readiness >= 45 -> "In progress"
        else -> "Needs setup"
    }
    val layoutStatus = when {
        project.spaces.isEmpty() -> "Add your first space (room, wall, or slab)."
        needsLayoutFix -> "Run Auto Fix Layout to clean collisions and placements."
        !hasMeasuredArea -> "Add measurable spaces before opening takeoff."
        else -> "Layout is ready for takeoff and pricing."
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val contentModifier = if (scrollContent) {
            Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier.padding(12.dp)
        }
        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Blueprint Command Center",
                style = MaterialTheme.typography.titleSmall
            )
            LinearProgressIndicator(
                progress = { (readiness / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Text(
                text = "Readiness $readiness/100 • $readinessLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = layoutStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlueprintMetric(label = "Walls", value = wallCount.toString(), modifier = Modifier.weight(1f))
                BlueprintMetric(label = "Slabs", value = slabCount.toString(), modifier = Modifier.weight(1f))
                BlueprintMetric(label = "Rooms", value = roomCount.toString(), modifier = Modifier.weight(1f))
            }
            Text(
                text = "Area mapped: ${"%.1f".format(measuredArea)} sq ft • Net area: ${"%.1f".format(netArea)} sq ft",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Envelope: ${"%.1f".format(envelopeArea)} sq ft • Density ${coverageDensity.toInt()}% • Span ${"%.1f".format(longestSpan)} ft",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlueprintLayerFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = filter == layerFilter,
                        onClick = { onLayerFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
            Text(
                text = "Current layer: ${layerFilter.label} • $spacesInCurrentLayer visible space(s).",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5, 1.0, 2.0).forEach { step ->
                    FilterChip(
                        selected = snapStepFeet == step,
                        onClick = { onSnapStepChange(step) },
                        label = { Text("${step}ft snap") }
                    )
                }
            }

            if (needsLayoutFix) {
                Text(
                    text = buildString {
                        append("Layout warnings:")
                        if (overlapCount > 0) append(" overlaps=$overlapCount")
                        if (unplacedCount > 0) append(" unplaced=$unplacedCount")
                        if (elevatedCount > 0) append(" elevated=$elevatedCount")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (compactControls) {
                PrimaryActionButton(
                    onClick = onOptimizeLayout,
                    enabled = project.spaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (needsLayoutFix) "Auto Fix Layout" else "Optimize Layout")
                }
                SecondaryActionButton(
                    onClick = onOpenTakeoff,
                    enabled = hasMeasuredArea,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to Takeoff")
                }
                SecondaryActionButton(
                    onClick = onRequestAddSpace,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Space")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryActionButton(
                        onClick = onOptimizeLayout,
                        enabled = project.spaces.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (needsLayoutFix) "Auto Fix Layout" else "Optimize Layout")
                    }
                    SecondaryActionButton(
                        onClick = onOpenTakeoff,
                        enabled = hasMeasuredArea,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue to Takeoff")
                    }
                }
                SecondaryActionButton(
                    onClick = onRequestAddSpace,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Space")
                }
            }

            if (compactControls) {
                SecondaryActionButton(
                    onClick = onAutoLayout,
                    enabled = project.spaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto Arrange")
                }
                SecondaryActionButton(
                    onClick = { onSnapToGrid(snapStepFeet) },
                    enabled = project.spaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Snap ${snapStepFeet}ft")
                }
                SecondaryActionButton(
                    onClick = onCenterLayout,
                    enabled = project.spaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Center Layout")
                }
                SecondaryActionButton(
                    onClick = onAlignNorth,
                    enabled = project.spaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Align North")
                }
                SecondaryActionButton(
                    onClick = onFlattenElevations,
                    enabled = elevatedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Flatten Elevation")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionButton(
                        onClick = onAutoLayout,
                        enabled = project.spaces.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Auto Arrange")
                    }
                    SecondaryActionButton(
                        onClick = { onSnapToGrid(snapStepFeet) },
                        enabled = project.spaces.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Snap ${snapStepFeet}ft")
                    }
                    SecondaryActionButton(
                        onClick = onCenterLayout,
                        enabled = project.spaces.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Center")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionButton(
                        onClick = onAlignNorth,
                        enabled = project.spaces.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Align North")
                    }
                    SecondaryActionButton(
                        onClick = onFlattenElevations,
                        enabled = elevatedCount > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Flatten Elevation")
                    }
                    SecondaryActionButton(
                        onClick = onOpenFullscreenEditor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fullscreen")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryActionButton(
                    onClick = onUndo,
                    enabled = canUndo && !isExportingBlueprint,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Undo ($undoCount)")
                }
                SecondaryActionButton(
                    onClick = onRedo,
                    enabled = canRedo && !isExportingBlueprint,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Redo ($redoCount)")
                }
            }

            if (compactControls) {
                SecondaryActionButton(
                    onClick = onShareBlueprintPdf,
                    enabled = hasLayerSpaces && !isExportingBlueprint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isExportingBlueprint) "Preparing..." else "Share Blueprint PDF")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionButton(
                        onClick = onDownloadBlueprint,
                        enabled = hasLayerSpaces && !isExportingBlueprint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isExportingBlueprint) "Preparing..." else "Download PNG")
                    }
                    SecondaryActionButton(
                        onClick = onShareBlueprint,
                        enabled = hasLayerSpaces && !isExportingBlueprint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isExportingBlueprint) "Preparing..." else "Share PNG")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionButton(
                        onClick = onDownloadBlueprintPdf,
                        enabled = hasLayerSpaces && !isExportingBlueprint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isExportingBlueprint) "Preparing..." else "Download PDF")
                    }
                    SecondaryActionButton(
                        onClick = onShareBlueprintPdf,
                        enabled = hasLayerSpaces && !isExportingBlueprint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isExportingBlueprint) "Preparing..." else "Share PDF")
                    }
                }
            }
            Text(
                text = if (hasLayerSpaces) {
                    "Current layer export includes $spacesInCurrentLayer space(s)."
                } else {
                    "No spaces available in the active layer for export."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (compactControls) {
                SecondaryActionButton(
                    onClick = onOpenFullscreenEditor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Fullscreen Drafting")
                }
            }
        }
    }
}
private fun mmFromFeet(feet: Double) = com.tradesketch.estimator.domain.model.Millimeters.fromFeet(feet)

@Composable
private fun BlueprintMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun blueprintReadinessScore(
    totalSpaces: Int,
    measuredArea: Double,
    overlapCount: Int,
    unplacedCount: Int,
    elevatedCount: Int
): Int {
    var score = 0
    if (totalSpaces > 0) score += 35
    if (measuredArea > 0.0) score += 25
    if (overlapCount == 0) score += 20
    if (unplacedCount == 0) score += 12
    if (elevatedCount == 0) score += 8
    return score.coerceIn(0, 100)
}

private fun nextSuggestedRoomName(spaces: List<Space>): String {
    val existingNames = spaces.map { it.name.trim() }.toSet()
    var index = 1
    while (true) {
        val candidate = "Room $index"
        if (candidate !in existingNames) return candidate
        index += 1
    }
}

private fun isLandscapeSpaceName(name: String): Boolean {
    val normalized = name.trim().lowercase()
    return normalized.contains("gravel") ||
        normalized.contains("mulch") ||
        normalized.contains("landscape") ||
        normalized.contains("bed")
}

private data class BlueprintFootprint(
    val minX: Double,
    val maxX: Double,
    val minZ: Double,
    val maxZ: Double
)

private fun blueprintOverlapCount(spaces: List<Space>): Int {
    if (spaces.size < 2) return 0
    val boxes = spaces.map { space ->
        space.id to footprintForSpace(space)
    }
    var count = 0
    for (i in 0 until boxes.lastIndex) {
        for (j in (i + 1) until boxes.size) {
            val a = boxes[i].second
            val b = boxes[j].second
            if (a.maxX >= b.minX && b.maxX >= a.minX && a.maxZ >= b.minZ && b.maxZ >= a.minZ) {
                count += 1
            }
        }
    }
    return count
}

private fun spacesForLayer(
    spaces: List<Space>,
    layerFilter: BlueprintLayerFilter
): List<Space> {
    return when (layerFilter) {
        BlueprintLayerFilter.ALL -> spaces
        BlueprintLayerFilter.WALLS -> spaces.filter { it.geometry is Geometry.Wall }
        BlueprintLayerFilter.SLABS -> spaces.filter { it.geometry is Geometry.Slab }
        BlueprintLayerFilter.ROOMS -> spaces.filter {
            it.geometry !is Geometry.Wall && it.geometry !is Geometry.Slab
        }
    }
}

private fun blueprintEnvelope(spaces: List<Space>): BlueprintFootprint? {
    if (spaces.isEmpty()) return null
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY
    spaces.forEach { space ->
        val footprint = footprintForSpace(space)
        minX = kotlin.math.min(minX, footprint.minX)
        maxX = kotlin.math.max(maxX, footprint.maxX)
        minZ = kotlin.math.min(minZ, footprint.minZ)
        maxZ = kotlin.math.max(maxZ, footprint.maxZ)
    }
    return BlueprintFootprint(minX = minX, maxX = maxX, minZ = minZ, maxZ = maxZ)
}

private fun footprintForSpace(space: Space): BlueprintFootprint {
    val (width, depth) = footprintDimensions(space.geometry)
    val halfW = width / 2.0
    val halfD = depth / 2.0
    return BlueprintFootprint(
        minX = space.transform.xFeet - halfW,
        maxX = space.transform.xFeet + halfW,
        minZ = space.transform.zFeet - halfD,
        maxZ = space.transform.zFeet + halfD
    )
}

private fun footprintDimensions(geometry: Geometry): Pair<Double, Double> {
    return when (geometry) {
        is Geometry.Rect -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Slab -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Wall -> geometry.length.toFeet() to 0.75
        is Geometry.Circle -> {
            val diameter = geometry.radius.toFeet() * 2.0
            diameter to diameter
        }
        is Geometry.LShape -> {
            val width = max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet())
            val depth = max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet())
            width to depth
        }
    }
}

