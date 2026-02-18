package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Opening
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal enum class BlueprintLayerFilter(val label: String) {
    ALL("All"),
    WALLS("Walls"),
    SLABS("Slabs"),
    ROOMS("Rooms")
}

@Composable
internal fun ModelBuilder3DPanel(
    project: Project,
    onAddSpace: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onAutoLayout: () -> Unit,
    onUpdateTransform: (String, SpaceTransform) -> Unit,
    onUpdateSpace: ((Space) -> Unit)? = null,
    modifier: Modifier = Modifier,
    immersiveMode: Boolean = false,
    blueprintMode: Boolean = false,
    calmModeEnabled: Boolean = false,
    workflowAidsEnabled: Boolean = false,
    workspaceSeed: Int = 0,
    preferredTopLock: Boolean? = null,
    preferredShowDimensions: Boolean? = null,
    preferredLayerFilter: BlueprintLayerFilter? = null,
    onQuickRoom: (() -> Unit)? = null,
    onQuickAddWall: (() -> Unit)? = null,
    onQuickAddSlab: (() -> Unit)? = null,
    onQuickAddBed: (() -> Unit)? = null,
    onDrawWallSegment: ((Double, Double, Double, Double) -> Unit)? = null,
    onOptimizeLayout: (() -> Unit)? = null,
    onCenterLayout: (() -> Unit)? = null,
    onAlignNorth: (() -> Unit)? = null,
    onUndoBlueprint: (() -> Unit)? = null,
    onRedoBlueprint: (() -> Unit)? = null,
    canUndoBlueprint: Boolean = false,
    canRedoBlueprint: Boolean = false,
    onDownloadBlueprintPng: (() -> Unit)? = null,
    onShareBlueprintPng: (() -> Unit)? = null,
    onDownloadBlueprintPdf: (() -> Unit)? = null,
    onShareBlueprintPdf: (() -> Unit)? = null,
    onBlueprintLayerFilterChange: ((BlueprintLayerFilter) -> Unit)? = null,
    fullScreenBlueprint: Boolean = false,
    onToggleBlueprintFullscreen: (() -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    val defaultTopLock = preferredTopLock ?: blueprintMode
    val defaultShowDimensions = preferredShowDimensions ?: blueprintMode
    val defaultCameraPitch = if (blueprintMode && defaultTopLock) 75f else 30f
    val defaultCameraYaw = if (blueprintMode && defaultTopLock) 0f else -38f
    val defaultCameraZoom = if (blueprintMode && defaultTopLock) 1.15f else 1.05f
    var selectedSpaceId by remember(project.id) {
        mutableStateOf(project.spaces.firstOrNull()?.id)
    }
    var activeControlTab by rememberSaveable(project.id) { mutableStateOf(BuilderControlTab.SCENE) }
    var cameraPitch by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultCameraPitch)
    }
    var cameraYaw by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultCameraYaw)
    }
    var cameraZoom by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultCameraZoom)
    }
    var panX by rememberSaveable(project.id) { mutableStateOf(0f) }
    var panY by rememberSaveable(project.id) { mutableStateOf(0f) }
    var nudgeStepFt by rememberSaveable(project.id) { mutableStateOf(1f) }
    var objectQuery by rememberSaveable(project.id) { mutableStateOf("") }
    var objectTradeFilter by rememberSaveable(project.id) { mutableStateOf(ModelTradeFilter.ALL) }
    var showGrid by rememberSaveable(project.id) { mutableStateOf(true) }
    var gridSpacingFeet by rememberSaveable(project.id, blueprintMode) {
        mutableStateOf(if (blueprintMode) 4f else 8f)
    }
    var showHud by rememberSaveable(project.id) { mutableStateOf(true) }
    var snapYaw by rememberSaveable(project.id) { mutableStateOf(true) }
    val proControlsEnabled = !calmModeEnabled || workflowAidsEnabled
    var showAdvancedSceneControls by rememberSaveable(
        project.id,
        blueprintMode,
        workspaceSeed,
        calmModeEnabled,
        workflowAidsEnabled
    ) {
        mutableStateOf(proControlsEnabled && !calmModeEnabled)
    }
    var showObjectSearchFilters by rememberSaveable(
        project.id,
        blueprintMode,
        workspaceSeed,
        calmModeEnabled,
        workflowAidsEnabled
    ) {
        mutableStateOf(proControlsEnabled && !calmModeEnabled)
    }
    var showInspectorPrecisionTools by rememberSaveable(
        project.id,
        blueprintMode,
        workspaceSeed,
        calmModeEnabled,
        workflowAidsEnabled
    ) {
        mutableStateOf(proControlsEnabled && !calmModeEnabled)
    }
    var lockTopView by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultTopLock)
    }
    var showDimensions by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultShowDimensions)
    }
    var showControlPanel by rememberSaveable(project.id, immersiveMode, blueprintMode, workspaceSeed) {
        mutableStateOf(!immersiveMode && !blueprintMode)
    }
    val useMicroToolbar = blueprintMode && immersiveMode
    val forceCanvasPriority = useMicroToolbar && fullScreenBlueprint
    val panelVisible = showControlPanel && !forceCanvasPriority
    var dockExpanded by rememberSaveable(project.id, workspaceSeed) { mutableStateOf(false) }
    var railSection by rememberSaveable(project.id, workspaceSeed) { mutableStateOf(BlueprintRailSection.CORE) }
    var blueprintTool by rememberSaveable(project.id) { mutableStateOf(BlueprintCanvasTool.NAVIGATE) }
    var drawWallStart by remember(project.id) { mutableStateOf<GroundPoint?>(null) }
    var drawWallPreview by remember(project.id) { mutableStateOf<GroundPoint?>(null) }
    var wallChainMode by rememberSaveable(project.id) { mutableStateOf(true) }
    var wallOrthoLock by rememberSaveable(project.id) { mutableStateOf(true) }
    var wallAngleSnapDegrees by rememberSaveable(project.id) { mutableStateOf(15f) }
    var wallAnchorSnapFeet by rememberSaveable(project.id) { mutableStateOf(0.75f) }
    var canvasPixelSize by remember(project.id) { mutableStateOf(IntSize.Zero) }
    var multiSelectMode by rememberSaveable(project.id) { mutableStateOf(false) }
    var isolateSelection by rememberSaveable(project.id) { mutableStateOf(false) }
    var keepWallLengthLocked by rememberSaveable(project.id) { mutableStateOf(false) }
    var keepWallAngleLocked by rememberSaveable(project.id) { mutableStateOf(false) }
    var selectedSpaceIds by remember(project.id) { mutableStateOf(setOf<String>()) }
    var lockedSpaceIds by remember(project.id) { mutableStateOf(setOf<String>()) }
    var hiddenSpaceIds by remember(project.id) { mutableStateOf(setOf<String>()) }
    var hiddenTradeLanes by remember(project.id) { mutableStateOf(setOf<ModelTradeLane>()) }
    var marqueeSelection by remember(project.id) { mutableStateOf<MarqueeSelectionState?>(null) }
    var wallEditDrag by remember(project.id) { mutableStateOf<WallEditDragState?>(null) }
    var spaceMoveDrag by remember(project.id) { mutableStateOf<SpaceMoveDragState?>(null) }

    val selectedSpace = project.spaces.find { it.id == selectedSpaceId }
    var draftTransform by remember(selectedSpace?.id) {
        mutableStateOf(selectedSpace?.transform ?: SpaceTransform())
    }
    val selectedSpaceLocked = selectedSpace?.let { it.id in lockedSpaceIds } == true
    val effectiveSelectedIds = remember(selectedSpaceId, selectedSpaceIds, multiSelectMode) {
        val active = selectedSpaceId?.let { setOf(it) } ?: emptySet()
        if (multiSelectMode) {
            selectedSpaceIds + active
        } else {
            active
        }
    }

    LaunchedEffect(project.spaces, selectedSpaceId) {
        if (selectedSpaceId != null && project.spaces.none { it.id == selectedSpaceId }) {
            selectedSpaceId = project.spaces.firstOrNull()?.id
        } else if (selectedSpaceId == null && project.spaces.isNotEmpty()) {
            selectedSpaceId = project.spaces.first().id
        }
    }

    LaunchedEffect(project.spaces) {
        val validIds = project.spaces.map { it.id }.toSet()
        selectedSpaceIds = selectedSpaceIds.filterTo(mutableSetOf()) { it in validIds }.toSet()
        lockedSpaceIds = lockedSpaceIds.filterTo(mutableSetOf()) { it in validIds }.toSet()
        hiddenSpaceIds = hiddenSpaceIds.filterTo(mutableSetOf()) { it in validIds }.toSet()
        if (spaceMoveDrag?.let { it.spaceId !in validIds } == true) {
            spaceMoveDrag = null
        }
    }

    LaunchedEffect(multiSelectMode, selectedSpaceId) {
        if (!multiSelectMode) {
            selectedSpaceIds = selectedSpaceId?.let { setOf(it) } ?: emptySet()
        } else {
            selectedSpaceId?.let { activeId ->
                if (activeId !in selectedSpaceIds) {
                    selectedSpaceIds = selectedSpaceIds + activeId
                }
            }
        }
    }

    LaunchedEffect(selectedSpace?.id, selectedSpace?.transform) {
        draftTransform = selectedSpace?.transform ?: SpaceTransform()
    }

    LaunchedEffect(blueprintMode, lockTopView) {
        if (blueprintMode && lockTopView) {
            cameraPitch = 75f
            cameraYaw = 0f
        }
    }

    LaunchedEffect(preferredTopLock, preferredShowDimensions, workspaceSeed) {
        preferredTopLock?.let { topLock ->
            lockTopView = topLock
            if (topLock) {
                cameraPitch = 75f
                cameraYaw = 0f
            }
        }
        preferredShowDimensions?.let { show ->
            showDimensions = show
        }
    }

    LaunchedEffect(preferredLayerFilter, project.id) {
        preferredLayerFilter?.let { filter ->
            objectTradeFilter = filter.toModelTradeFilter()
        }
    }

    LaunchedEffect(blueprintMode, objectTradeFilter, onBlueprintLayerFilterChange) {
        if (blueprintMode) {
            onBlueprintLayerFilterChange?.invoke(objectTradeFilter.toBlueprintLayerFilter())
        }
    }

    LaunchedEffect(blueprintTool, project.id) {
        if (blueprintTool != BlueprintCanvasTool.DRAW_WALL) {
            drawWallStart = null
            drawWallPreview = null
        }
        if (blueprintTool != BlueprintCanvasTool.NAVIGATE) {
            wallEditDrag = null
            marqueeSelection = null
            spaceMoveDrag = null
        }
    }

    LaunchedEffect(blueprintMode, project.spaces.size) {
        if (blueprintMode && project.spaces.isEmpty() && blueprintTool != BlueprintCanvasTool.DRAW_WALL) {
            blueprintTool = BlueprintCanvasTool.DRAW_WALL
        }
    }

    LaunchedEffect(forceCanvasPriority) {
        if (forceCanvasPriority && showControlPanel) {
            showControlPanel = false
        }
    }

    LaunchedEffect(selectedSpaceId) {
        if (wallEditDrag?.spaceId != selectedSpaceId) {
            wallEditDrag = null
        }
        if (spaceMoveDrag?.spaceId != selectedSpaceId) {
            spaceMoveDrag = null
        }
    }

    LaunchedEffect(multiSelectMode) {
        if (!multiSelectMode) {
            marqueeSelection = null
        } else {
            spaceMoveDrag = null
        }
    }

    LaunchedEffect(proControlsEnabled) {
        if (!proControlsEnabled) {
            multiSelectMode = false
            isolateSelection = false
            keepWallLengthLocked = false
            keepWallAngleLocked = false
            marqueeSelection = null
        }
    }

    val previewSpaces = remember(project.spaces, selectedSpaceId, draftTransform, wallEditDrag) {
        project.spaces.map { space ->
            val activeWallDrag = wallEditDrag
            if (activeWallDrag?.spaceId == space.id) {
                activeWallDrag.previewSpace
            } else if (space.id == selectedSpaceId) {
                space.copy(transform = draftTransform)
            } else {
                space
            }
        }
    }

    val cameraState = remember(cameraPitch, cameraYaw, cameraZoom, panX, panY) {
        CameraState(
            pitchDeg = cameraPitch,
            yawDeg = cameraYaw,
            zoom = cameraZoom,
            panX = panX,
            panY = panY
        )
    }
    val filteredSpaces = remember(project.spaces, objectQuery, objectTradeFilter) {
        val normalizedQuery = objectQuery.trim().lowercase()
        project.spaces.filter { space ->
            val tradeMatch = objectTradeFilter.lane == null ||
                objectTradeFilter.lane == modelTradeLaneForSpace(space)
            val queryMatch = normalizedQuery.isBlank() ||
                space.name.lowercase().contains(normalizedQuery) ||
                modelLabel(space).lowercase().contains(normalizedQuery)
            tradeMatch && queryMatch
        }
    }
    val visiblePreviewSpaces = remember(
        previewSpaces,
        hiddenSpaceIds,
        hiddenTradeLanes,
        isolateSelection,
        effectiveSelectedIds
    ) {
        previewSpaces.filter { space ->
            if (space.id in hiddenSpaceIds) return@filter false
            if (modelTradeLaneForSpace(space) in hiddenTradeLanes) return@filter false
            if (isolateSelection) {
                effectiveSelectedIds.isNotEmpty() && space.id in effectiveSelectedIds
            } else {
                true
            }
        }
    }
    val sceneSpaces = remember(visiblePreviewSpaces, objectTradeFilter, blueprintMode) {
        if (!blueprintMode || objectTradeFilter == ModelTradeFilter.ALL) {
            visiblePreviewSpaces
        } else {
            visiblePreviewSpaces.filter { space ->
                objectTradeFilter.lane == modelTradeLaneForSpace(space)
            }
        }
    }
    val activeSceneCenter = remember(sceneSpaces, project.spaces) {
        val baseSpaces = if (sceneSpaces.isNotEmpty()) sceneSpaces else project.spaces
        sceneCenter(baseSpaces)
    }
    val wallAnchorPoints = remember(project.spaces) {
        collectWallAnchorPoints(project.spaces)
    }
    val wallEditAnchorPoints = remember(project.spaces, selectedSpaceId) {
        collectWallAnchorPoints(project.spaces.filter { it.id != selectedSpaceId })
    }
    val displayedSelectedSpace = remember(previewSpaces, selectedSpaceId) {
        previewSpaces.find { it.id == selectedSpaceId }
    }
    val displayedSelectedWall = displayedSelectedSpace?.takeIf { it.geometry is Geometry.Wall }

    val workspaceScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val allowPanelScroll = !immersiveMode || panelVisible
    val canvasHeight = when {
        immersiveMode && !panelVisible -> 700.dp
        immersiveMode -> 610.dp
        else -> 420.dp
    }
    val objectListHeight = if (immersiveMode) 250.dp else 220.dp
    val resetCamera = {
        cameraPitch = defaultCameraPitch
        cameraYaw = defaultCameraYaw
        cameraZoom = defaultCameraZoom
        panX = 0f
        panY = 0f
        if (blueprintMode) {
            lockTopView = defaultTopLock
        }
    }
    val frameModel = {
        val frameSpaces = if (blueprintMode && objectTradeFilter != ModelTradeFilter.ALL) {
            sceneSpaces
        } else {
            project.spaces
        }
        cameraPitch = defaultCameraPitch
        cameraYaw = defaultCameraYaw
        cameraZoom = frameZoomForModel(frameSpaces)
        panX = 0f
        panY = 0f
    }
    val runAutoLayout = {
        onAutoLayout()
        frameModel()
    }
    val cancelWallDraft = {
        drawWallStart = null
        drawWallPreview = null
    }
    val resolveWallPoint = { rawPoint: GroundPoint, startPoint: GroundPoint? ->
        val basePoint = snapGroundPoint(
            point = rawPoint,
            stepFeet = nudgeStepFt.toDouble().coerceAtLeast(0.25)
        )
        val constrained = if (startPoint != null) {
            applyWallDirectionConstraint(
                start = startPoint,
                rawPoint = basePoint,
                orthoLock = wallOrthoLock,
                angleSnapDegrees = wallAngleSnapDegrees.toDouble()
            )
        } else {
            basePoint
        }
        snapPointToNearestAnchor(
            point = constrained,
            anchors = wallAnchorPoints,
            thresholdFeet = wallAnchorSnapFeet.toDouble().coerceAtLeast(0.25)
        )
    }
    val resolveTappedSpace: (Offset) -> Space? = { tapOffset ->
        if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) {
            null
        } else {
            val viewport = Size(
                width = canvasPixelSize.width.toFloat(),
                height = canvasPixelSize.height.toFloat()
            )
            val groundPoint = screenToGroundPoint(
                tapOffset = tapOffset,
                viewport = viewport,
                sceneCenter = activeSceneCenter,
                cameraState = cameraState
            )
            if (groundPoint == null) {
                null
            } else {
                findSpaceAtGroundPoint(
                    point = groundPoint,
                    spaces = sceneSpaces
                )
            }
        }
    }
    val resolveWallDragPreview = { dragState: WallEditDragState, rawPoint: GroundPoint ->
        resolveWallEndpointDrag(
            dragState = dragState,
            rawPoint = rawPoint,
            gridStepFeet = nudgeStepFt.toDouble().coerceAtLeast(0.25),
            orthoLock = wallOrthoLock,
            angleSnapDegrees = wallAngleSnapDegrees.toDouble(),
            anchorSnapFeet = wallAnchorSnapFeet.toDouble().coerceAtLeast(0.25),
            anchors = wallEditAnchorPoints,
            otherWalls = project.spaces.filter { it.id != dragState.spaceId && it.geometry is Geometry.Wall },
            keepLength = keepWallLengthLocked,
            keepAngle = keepWallAngleLocked
        )
    }
    val resolveSpaceMovePreview = { movingSpace: Space, rawCenter: GroundPoint ->
        resolveDraggedSpaceTransform(
            movingSpace = movingSpace,
            rawCenter = rawCenter,
            otherSpaces = project.spaces.filter { it.id != movingSpace.id },
            gridStepFeet = nudgeStepFt.toDouble().coerceAtLeast(0.25)
        )
    }
    val toggleSpaceLock: (String) -> Unit = { spaceId ->
        lockedSpaceIds = if (spaceId in lockedSpaceIds) {
            lockedSpaceIds - spaceId
        } else {
            lockedSpaceIds + spaceId
        }
    }
    val toggleSpaceVisibility: (String) -> Unit = { spaceId ->
        hiddenSpaceIds = if (spaceId in hiddenSpaceIds) {
            hiddenSpaceIds - spaceId
        } else {
            hiddenSpaceIds + spaceId
        }
    }
    val toggleLaneVisibility: (ModelTradeLane) -> Unit = { lane ->
        hiddenTradeLanes = if (lane in hiddenTradeLanes) {
            hiddenTradeLanes - lane
        } else {
            hiddenTradeLanes + lane
        }
    }
    val placeWallSegment = { startPoint: GroundPoint, endPoint: GroundPoint ->
        val segmentLength = hypot(endPoint.xFeet - startPoint.xFeet, endPoint.zFeet - startPoint.zFeet)
        if (segmentLength < 1.0) {
            haptics.tap()
            if (!wallChainMode) {
                cancelWallDraft()
            } else {
                drawWallPreview = drawWallStart
            }
        } else {
            onDrawWallSegment?.invoke(
                startPoint.xFeet,
                startPoint.zFeet,
                endPoint.xFeet,
                endPoint.zFeet
            )
            haptics.confirm()
            if (wallChainMode) {
                drawWallStart = endPoint
                drawWallPreview = endPoint
            } else {
                cancelWallDraft()
            }
        }
    }
    val activateDrawWallMode = {
        blueprintTool = BlueprintCanvasTool.DRAW_WALL
        lockTopView = true
        cameraPitch = 75f
        cameraYaw = 0f
        cancelWallDraft()
    }

    Column(
        modifier = if (allowPanelScroll) {
            modifier.verticalScroll(workspaceScroll)
        } else {
            modifier
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!useMicroToolbar) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (showControlPanel) "Controls Open" else "Canvas Priority",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        showControlPanel = !showControlPanel
                    }
                ) {
                    Text(if (showControlPanel) "Hide Controls" else "Show Controls")
                }
            }
        }

        if (showControlPanel) {
            Card(
                modifier = Modifier.animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Text(
                    text = if (blueprintMode) "Blueprint Studio" else "3D Builder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (immersiveMode) {
                    Text(
                        text = if (blueprintMode) "Blueprint canvas active" else "Immersive workspace active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (blueprintMode) {
                        if (lockTopView) {
                            if (proControlsEnabled) {
                                "Blueprint mode: top-view locked for precise layout. Drag rooms to move/snap, drag wall handles to reshape, and double-tap any object for full details."
                            } else {
                                "Blueprint mode: top-view locked for fast field edits. Drag rooms to move/snap, tap walls to add openings, then continue to takeoff."
                            }
                        } else {
                            "Blueprint mode: orbit unlocked for spatial checks, layering, and alignment."
                        }
                    } else {
                        "Pinch to zoom, drag to orbit. Use the inspector to place and rotate each space."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Objects: ${project.spaces.size} | Active: ${displayedSelectedSpace?.name ?: "None"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onAddSpace()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Space")
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            runAutoLayout()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto Arrange")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BuilderControlTab.entries.forEach { tab ->
                        FilterChip(
                            selected = activeControlTab == tab,
                            onClick = {
                                haptics.tap()
                                activeControlTab = tab
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
                if (activeControlTab == BuilderControlTab.SCENE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                resetCamera()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                frameModel()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Frame Model")
                        }
                        OutlinedButton(
                            onClick = {
                                selectedSpace?.let { focusSpace ->
                                    haptics.tap()
                                    cameraPitch = 22f
                                    cameraZoom = focusZoomForSpace(focusSpace)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedSpace != null
                        ) {
                            Text("Focus Active")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            showAdvancedSceneControls = !showAdvancedSceneControls
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showAdvancedSceneControls) {
                                "Hide Advanced Scene Controls"
                            } else {
                                "Show Advanced Scene Controls"
                            }
                        )
                    }
                    if (showAdvancedSceneControls) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ViewPresetChip(
                                label = "Iso",
                                selected = cameraPitch in 24f..36f && cameraYaw in -46f..-30f,
                                onClick = {
                                    haptics.tap()
                                    cameraPitch = 30f
                                    cameraYaw = -38f
                                }
                            )
                            ViewPresetChip(
                                label = "Front",
                                selected = cameraPitch in 8f..18f && cameraYaw in -8f..8f,
                                onClick = {
                                    haptics.tap()
                                    cameraPitch = 12f
                                    cameraYaw = 0f
                                }
                            )
                            ViewPresetChip(
                                label = "Top",
                                selected = cameraPitch in 72f..80f,
                                onClick = {
                                    haptics.tap()
                                    cameraPitch = 75f
                                    cameraYaw = 0f
                                }
                            )
                            ViewPresetChip(
                                label = "Side",
                                selected = cameraPitch in 8f..18f && cameraYaw in 82f..98f,
                                onClick = {
                                    haptics.tap()
                                    cameraPitch = 12f
                                    cameraYaw = 90f
                                }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showGrid,
                                onClick = {
                                    haptics.tap()
                                    showGrid = !showGrid
                                },
                                label = { Text("Grid") }
                            )
                            FilterChip(
                                selected = showHud,
                                onClick = {
                                    haptics.tap()
                                    showHud = !showHud
                                },
                                label = { Text("HUD") }
                            )
                            FilterChip(
                                selected = snapYaw,
                                onClick = {
                                    haptics.tap()
                                    snapYaw = !snapYaw
                                },
                                label = { Text("Snap Yaw") }
                            )
                            if (blueprintMode) {
                                FilterChip(
                                    selected = lockTopView,
                                    onClick = {
                                        haptics.tap()
                                        lockTopView = !lockTopView
                                    },
                                    label = { Text("Lock Top") }
                                )
                                FilterChip(
                                    selected = showDimensions,
                                    onClick = {
                                        haptics.tap()
                                        showDimensions = !showDimensions
                                    },
                                    label = { Text("Dimensions") }
                                )
                            }
                            FilterChip(
                                selected = immersiveMode,
                                onClick = {},
                                label = { Text(if (immersiveMode) "Immersive Canvas" else "Compact Canvas") }
                            )
                        }
                        if (showGrid) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2f, 4f, 8f, 12f, 16f).forEach { spacing ->
                                    FilterChip(
                                        selected = gridSpacingFeet == spacing,
                                        onClick = {
                                            haptics.tap()
                                            gridSpacingFeet = spacing
                                        },
                                        label = { Text("Grid ${spacing.toInt()}ft") }
                                    )
                                }
                            }
                        }
                        TransformSlider(
                            label = "Camera Orbit",
                            value = cameraYaw,
                            valueRange = -180f..180f,
                            onValueChange = { cameraYaw = it },
                            suffix = "deg"
                        )
                        TransformSlider(
                            label = "Camera Tilt",
                            value = cameraPitch,
                            valueRange = -75f..75f,
                            onValueChange = { cameraPitch = it },
                            suffix = "deg"
                        )
                        TransformSlider(
                            label = "Camera Zoom",
                            value = cameraZoom,
                            valueRange = 0.5f..3.8f,
                            onValueChange = { cameraZoom = it },
                            suffix = "x"
                        )
                    }
                } else if (activeControlTab == BuilderControlTab.OBJECTS) {
                    Text(
                        text = "Objects tab: filter by trade and search names to quickly pick what to edit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Inspector tab: fine-tune transform, color, and alignment for the active object.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
        }

        val fillCanvasViewport = fullScreenBlueprint && !panelVisible
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillCanvasViewport) Modifier.weight(1f) else Modifier)
                .animateContentSize()
        ) {
            val allowCanvasTransform = !(
                blueprintMode &&
                    (
                        (blueprintTool == BlueprintCanvasTool.DRAW_WALL &&
                            onDrawWallSegment != null &&
                            lockTopView) ||
                            (blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                                lockTopView &&
                                (multiSelectMode || wallEditDrag != null || spaceMoveDrag != null))
                        )
                )
            val canvasGestureModifier = if (panelVisible || !allowCanvasTransform) {
                Modifier
            } else {
                Modifier.pointerInput(project.id, lockTopView, blueprintMode) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        cameraZoom = (cameraZoom * zoom).coerceIn(0.5f, 3.8f)
                        if (blueprintMode && lockTopView) {
                            panX += pan.x
                            panY += pan.y
                            cameraPitch = 75f
                            cameraYaw = 0f
                        } else {
                            cameraYaw += (pan.x * 0.24f) + (rotation * 45f)
                            cameraPitch = (cameraPitch - (pan.y * 0.24f)).coerceIn(-75f, 75f)
                        }
                    }
                }
            }
            val spaceTapModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                lockTopView
            ) {
                Modifier.pointerInput(
                    project.id,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    canvasPixelSize,
                    activeSceneCenter,
                    sceneSpaces,
                    selectedSpaceId,
                    multiSelectMode,
                    selectedSpaceIds,
                    lockedSpaceIds
                ) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val tappedSpace = resolveTappedSpace(tapOffset) ?: return@detectTapGestures
                            selectedSpaceId = tappedSpace.id
                            selectedSpaceIds = if (multiSelectMode) {
                                selectedSpaceIds + tappedSpace.id
                            } else {
                                setOf(tappedSpace.id)
                            }
                            if (activeControlTab == BuilderControlTab.OBJECTS) {
                                activeControlTab = BuilderControlTab.INSPECTOR
                            }
                            if (tappedSpace.id in lockedSpaceIds) {
                                haptics.tap()
                                return@detectTapGestures
                            }
                            haptics.confirm()
                            onEditSpace(tappedSpace)
                        },
                        onTap = { tapOffset ->
                            val tappedSpace = resolveTappedSpace(tapOffset) ?: return@detectTapGestures
                            selectedSpaceId = tappedSpace.id
                            val wasSelected = tappedSpace.id in selectedSpaceIds
                            selectedSpaceIds = if (multiSelectMode) {
                                if (wasSelected) {
                                    (selectedSpaceIds - tappedSpace.id).ifEmpty { setOf(tappedSpace.id) }
                                } else {
                                    selectedSpaceIds + tappedSpace.id
                                }
                            } else {
                                setOf(tappedSpace.id)
                            }
                            if (activeControlTab == BuilderControlTab.OBJECTS) {
                                activeControlTab = BuilderControlTab.INSPECTOR
                            }
                            if (
                                !multiSelectMode &&
                                tappedSpace.geometry is Geometry.Wall &&
                                tappedSpace.id !in lockedSpaceIds
                            ) {
                                haptics.confirm()
                                onEditSpace(tappedSpace)
                            } else {
                                haptics.tap()
                            }
                        }
                    )
                }
            } else {
                Modifier
            }
            val marqueeSelectionModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                lockTopView &&
                multiSelectMode
            ) {
                Modifier.pointerInput(
                    project.id,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    canvasPixelSize,
                    activeSceneCenter,
                    sceneSpaces,
                    multiSelectMode,
                    selectedSpaceIds
                ) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startOffset ->
                            marqueeSelection = MarqueeSelectionState(
                                start = startOffset,
                                end = startOffset
                            )
                            haptics.tap()
                        },
                        onDragCancel = {
                            marqueeSelection = null
                        },
                        onDragEnd = {
                            val marquee = marqueeSelection ?: return@detectDragGesturesAfterLongPress
                            marqueeSelection = null
                            if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGesturesAfterLongPress
                            val selectionRect = marquee.normalizedRect()
                            val viewport = Size(
                                width = canvasPixelSize.width.toFloat(),
                                height = canvasPixelSize.height.toFloat()
                            )
                            val capturedIds = sceneSpaces.mapNotNull { space ->
                                val projectedCenter = projectPoint(
                                    point = Vec3(
                                        x = space.transform.xFeet.toFloat(),
                                        y = 0f,
                                        z = space.transform.zFeet.toFloat()
                                    ),
                                    sceneCenter = activeSceneCenter,
                                    cameraState = cameraState,
                                    viewport = viewport
                                ) ?: return@mapNotNull null
                                if (selectionRect.contains(projectedCenter.screen)) {
                                    space.id
                                } else {
                                    null
                                }
                            }.toSet()
                            if (capturedIds.isNotEmpty()) {
                                selectedSpaceIds = selectedSpaceIds + capturedIds
                                selectedSpaceId = capturedIds.first()
                                if (activeControlTab == BuilderControlTab.OBJECTS) {
                                    activeControlTab = BuilderControlTab.INSPECTOR
                                }
                                haptics.confirm()
                            }
                        }
                    ) { change, _ ->
                        marqueeSelection = marqueeSelection?.copy(end = change.position)
                            ?: MarqueeSelectionState(
                                start = change.position,
                                end = change.position
                            )
                        change.consume()
                    }
                }
            } else {
                Modifier
            }
            val spaceMoveDragModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                lockTopView &&
                !multiSelectMode
            ) {
                Modifier.pointerInput(
                    project.id,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    nudgeStepFt,
                    canvasPixelSize,
                    activeSceneCenter,
                    sceneSpaces,
                    selectedSpaceId,
                    selectedSpaceIds,
                    lockedSpaceIds
                ) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                            val activeSpace = resolveTappedSpace(startOffset) ?: selectedSpace ?: return@detectDragGestures
                            if (activeSpace.id in lockedSpaceIds || activeSpace.geometry is Geometry.Wall) {
                                return@detectDragGestures
                            }
                            val viewport = Size(
                                width = canvasPixelSize.width.toFloat(),
                                height = canvasPixelSize.height.toFloat()
                            )
                            val groundPoint = screenToGroundPoint(
                                tapOffset = startOffset,
                                viewport = viewport,
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState
                            ) ?: return@detectDragGestures
                            selectedSpaceId = activeSpace.id
                            selectedSpaceIds = setOf(activeSpace.id)
                            val sourceTransform = activeSpace.transform
                            draftTransform = sourceTransform
                            spaceMoveDrag = SpaceMoveDragState(
                                spaceId = activeSpace.id,
                                spaceName = activeSpace.name,
                                sourceTransform = sourceTransform,
                                workingTransform = sourceTransform,
                                grabOffsetXFeet = groundPoint.xFeet - sourceTransform.xFeet,
                                grabOffsetZFeet = groundPoint.zFeet - sourceTransform.zFeet,
                                snapKind = SpaceMoveSnapKind.NONE
                            )
                            if (activeControlTab == BuilderControlTab.OBJECTS) {
                                activeControlTab = BuilderControlTab.INSPECTOR
                            }
                            haptics.tap()
                        },
                        onDragCancel = {
                            val dragState = spaceMoveDrag
                            if (dragState != null && dragState.spaceId == selectedSpaceId) {
                                draftTransform = dragState.sourceTransform
                            }
                            spaceMoveDrag = null
                        },
                        onDragEnd = {
                            val dragState = spaceMoveDrag ?: return@detectDragGestures
                            onUpdateTransform(dragState.spaceId, dragState.workingTransform)
                            if (dragState.spaceId == selectedSpaceId) {
                                draftTransform = dragState.workingTransform
                            }
                            spaceMoveDrag = null
                            haptics.confirm()
                        }
                    ) { change, _ ->
                        if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                        val dragState = spaceMoveDrag ?: return@detectDragGestures
                        val movingSpace = project.spaces.find { it.id == dragState.spaceId } ?: return@detectDragGestures
                        val viewport = Size(
                            width = canvasPixelSize.width.toFloat(),
                            height = canvasPixelSize.height.toFloat()
                        )
                        val pointerGround = screenToGroundPoint(
                            tapOffset = change.position,
                            viewport = viewport,
                            sceneCenter = activeSceneCenter,
                            cameraState = cameraState
                        ) ?: return@detectDragGestures
                        val rawCenter = GroundPoint(
                            xFeet = pointerGround.xFeet - dragState.grabOffsetXFeet,
                            zFeet = pointerGround.zFeet - dragState.grabOffsetZFeet
                        )
                        val preview = resolveSpaceMovePreview(
                            movingSpace,
                            rawCenter
                        )
                        spaceMoveDrag = dragState.copy(
                            workingTransform = preview.transform,
                            snapKind = preview.snapKind
                        )
                        if (dragState.spaceId == selectedSpaceId) {
                            draftTransform = preview.transform
                        }
                        change.consume()
                    }
                }
            } else {
                Modifier
            }
            val wallHandleDragModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                lockTopView &&
                !multiSelectMode &&
                displayedSelectedWall != null &&
                onUpdateSpace != null &&
                !selectedSpaceLocked
            ) {
                Modifier.pointerInput(
                    project.id,
                    selectedSpaceId,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    nudgeStepFt,
                    wallOrthoLock,
                    wallAngleSnapDegrees,
                    wallAnchorSnapFeet,
                    keepWallLengthLocked,
                    keepWallAngleLocked,
                    canvasPixelSize,
                    activeSceneCenter,
                    displayedSelectedWall
                ) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                            val activeWall = displayedSelectedWall ?: return@detectDragGestures
                            val viewport = Size(
                                width = canvasPixelSize.width.toFloat(),
                                height = canvasPixelSize.height.toFloat()
                            )
                            val handle = findNearestWallHandle(
                                wall = activeWall,
                                tapOffset = startOffset,
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState,
                                viewport = viewport
                            ) ?: return@detectDragGestures
                            wallEditDrag = createWallEditDragState(
                                wall = activeWall,
                                activeHandle = handle
                            )
                            haptics.tap()
                        },
                        onDragCancel = {
                            wallEditDrag = null
                        },
                        onDragEnd = {
                            val dragState = wallEditDrag ?: return@detectDragGestures
                            onUpdateSpace.invoke(dragState.previewSpace)
                            draftTransform = dragState.previewSpace.transform
                            wallEditDrag = null
                            haptics.confirm()
                        }
                    ) { change, _ ->
                        if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                        val dragState = wallEditDrag ?: return@detectDragGestures
                        val viewport = Size(
                            width = canvasPixelSize.width.toFloat(),
                            height = canvasPixelSize.height.toFloat()
                        )
                        val rawPoint = screenToGroundPoint(
                            tapOffset = change.position,
                            viewport = viewport,
                            sceneCenter = activeSceneCenter,
                            cameraState = cameraState
                        ) ?: return@detectDragGestures
                        val updatedDrag = resolveWallDragPreview(dragState, rawPoint)
                        wallEditDrag = updatedDrag
                        draftTransform = updatedDrag.previewSpace.transform
                        change.consume()
                    }
                }
            } else {
                Modifier
            }
            val wallTapModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.DRAW_WALL &&
                onDrawWallSegment != null &&
                lockTopView
            ) {
                Modifier.pointerInput(
                    project.id,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    nudgeStepFt,
                    canvasPixelSize,
                    activeSceneCenter
                ) {
                    detectTapGestures { tapOffset ->
                        if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectTapGestures
                        val viewport = Size(
                            width = canvasPixelSize.width.toFloat(),
                            height = canvasPixelSize.height.toFloat()
                        )
                        val groundPoint = screenToGroundPoint(
                            tapOffset = tapOffset,
                            viewport = viewport,
                            sceneCenter = activeSceneCenter,
                            cameraState = cameraState
                        ) ?: return@detectTapGestures
                        val pendingStart = drawWallStart
                        if (pendingStart == null) {
                            val startPoint = resolveWallPoint(groundPoint, null)
                            drawWallStart = startPoint
                            drawWallPreview = startPoint
                            haptics.tap()
                        } else {
                            val endPoint = resolveWallPoint(groundPoint, pendingStart)
                            drawWallPreview = endPoint
                            placeWallSegment(pendingStart, endPoint)
                        }
                    }
                }
            } else {
                Modifier
            }
            val wallDragModifier = if (
                blueprintMode &&
                blueprintTool == BlueprintCanvasTool.DRAW_WALL &&
                onDrawWallSegment != null &&
                lockTopView
            ) {
                Modifier.pointerInput(
                    project.id,
                    blueprintTool,
                    lockTopView,
                    cameraPitch,
                    cameraYaw,
                    cameraZoom,
                    panX,
                    panY,
                    nudgeStepFt,
                    canvasPixelSize,
                    activeSceneCenter
                ) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                            val viewport = Size(
                                width = canvasPixelSize.width.toFloat(),
                                height = canvasPixelSize.height.toFloat()
                            )
                            val point = screenToGroundPoint(
                                tapOffset = startOffset,
                                viewport = viewport,
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState
                            ) ?: return@detectDragGestures
                            val currentStart = drawWallStart
                            val normalizedStart = if (currentStart != null && wallChainMode) {
                                currentStart
                            } else {
                                resolveWallPoint(point, null)
                            }
                            drawWallStart = normalizedStart
                            drawWallPreview = normalizedStart
                            haptics.tap()
                        },
                        onDragCancel = {
                            if (!wallChainMode) {
                                cancelWallDraft()
                            } else {
                                drawWallPreview = drawWallStart
                            }
                        },
                        onDragEnd = {
                            val pendingStart = drawWallStart
                            val pendingEnd = drawWallPreview
                            if (pendingStart != null && pendingEnd != null) {
                                placeWallSegment(pendingStart, pendingEnd)
                            }
                        }
                    ) { change, _ ->
                        if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectDragGestures
                        val viewport = Size(
                            width = canvasPixelSize.width.toFloat(),
                            height = canvasPixelSize.height.toFloat()
                        )
                        val point = screenToGroundPoint(
                            tapOffset = change.position,
                            viewport = viewport,
                            sceneCenter = activeSceneCenter,
                            cameraState = cameraState
                        ) ?: return@detectDragGestures
                        val pendingStart = drawWallStart ?: resolveWallPoint(point, null).also {
                            drawWallStart = it
                        }
                        drawWallPreview = resolveWallPoint(point, pendingStart)
                        change.consume()
                    }
                }
            } else {
                Modifier
            }
            val canvasSurfaceModifier = if (fillCanvasViewport) {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            }
            Box(
                modifier = canvasSurfaceModifier
                    .onSizeChanged { canvasPixelSize = it }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceBright,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .then(canvasGestureModifier)
                    .then(marqueeSelectionModifier)
                    .then(spaceTapModifier)
                    .then(wallHandleDragModifier)
                    .then(spaceMoveDragModifier)
                    .then(wallTapModifier)
                    .then(wallDragModifier)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawScene(
                        spaces = sceneSpaces,
                        selectedSpaceId = selectedSpaceId,
                        selectedSpaceIds = effectiveSelectedIds,
                        cameraState = cameraState,
                        showGrid = showGrid,
                        gridSpacingFeet = gridSpacingFeet,
                        blueprintMode = blueprintMode,
                        showDimensions = showDimensions
                    )
                }
                if (blueprintMode) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val crossHalf = if (lockTopView) 12f else 8f
                        val crossColor = if (lockTopView) {
                            Color(0x66EAF2FF)
                        } else {
                            Color(0x66B0BEC5)
                        }
                        drawLine(
                            color = crossColor,
                            start = Offset(center.x - crossHalf, center.y),
                            end = Offset(center.x + crossHalf, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = crossColor,
                            start = Offset(center.x, center.y - crossHalf),
                            end = Offset(center.x, center.y + crossHalf),
                            strokeWidth = 2f
                        )
                    }
                }
                if (blueprintMode && showHud) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BlueprintLegendPill(label = "Room", color = ROOM_BLUEPRINT_COLOR)
                                BlueprintLegendPill(label = "Wall", color = WALL_BLUEPRINT_COLOR)
                                BlueprintLegendPill(label = "Slab", color = SLAB_BLUEPRINT_COLOR)
                                BlueprintLegendPill(label = "Bed", color = BED_BLUEPRINT_COLOR)
                            }
                            Text(
                                text = "Striped = drywall + paint",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (
                    blueprintMode &&
                    blueprintTool == BlueprintCanvasTool.DRAW_WALL &&
                    drawWallStart != null
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val start = drawWallStart ?: return@Canvas
                        val projectedStart = projectPoint(
                            point = Vec3(
                                x = start.xFeet.toFloat(),
                                y = 0f,
                                z = start.zFeet.toFloat()
                            ),
                            sceneCenter = activeSceneCenter,
                            cameraState = cameraState,
                            viewport = size
                        ) ?: return@Canvas
                        val preview = drawWallPreview
                        if (preview != null) {
                            val projectedEnd = projectPoint(
                                point = Vec3(
                                    x = preview.xFeet.toFloat(),
                                    y = 0f,
                                    z = preview.zFeet.toFloat()
                                ),
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState,
                                viewport = size
                            )
                            if (projectedEnd != null) {
                                drawLine(
                                    color = Color(0xFF4FC3F7),
                                    start = projectedStart.screen,
                                    end = projectedEnd.screen,
                                    strokeWidth = 4f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = Color(0xFF4FC3F7),
                                    radius = 7f,
                                    center = projectedEnd.screen
                                )
                            }
                        }
                        drawCircle(
                            color = Color(0xFF4FC3F7),
                            radius = 9f,
                            center = projectedStart.screen
                        )
                        drawCircle(
                            color = Color(0xFFF0F7FF),
                            radius = 4f,
                            center = projectedStart.screen
                        )
                    }
                }
                if (
                    blueprintMode &&
                    lockTopView &&
                    blueprintTool == BlueprintCanvasTool.NAVIGATE
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        marqueeSelection?.let { marquee ->
                            val selectionRect = marquee.normalizedRect()
                            drawRect(
                                color = Color(0x334FC3F7),
                                topLeft = Offset(selectionRect.left, selectionRect.top),
                                size = Size(selectionRect.width, selectionRect.height)
                            )
                            drawRect(
                                color = Color(0xFF4FC3F7),
                                topLeft = Offset(selectionRect.left, selectionRect.top),
                                size = Size(selectionRect.width, selectionRect.height),
                                style = Stroke(width = 2f)
                            )
                        }
                        val activeWall = displayedSelectedWall
                        if (activeWall != null) {
                            val endpoints = wallEndpoints(activeWall) ?: return@Canvas
                            val projectedStart = projectGroundPoint(
                                point = endpoints.start,
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState,
                                viewport = size
                            ) ?: return@Canvas
                            val projectedEnd = projectGroundPoint(
                                point = endpoints.end,
                                sceneCenter = activeSceneCenter,
                                cameraState = cameraState,
                                viewport = size
                            ) ?: return@Canvas
                            drawLine(
                                color = Color(0xFF80DEEA),
                                start = projectedStart.screen,
                                end = projectedEnd.screen,
                                strokeWidth = if (wallEditDrag != null) 4f else 2.4f,
                                cap = StrokeCap.Round
                            )
                            drawWallHandle(
                                center = projectedStart.screen,
                                active = wallEditDrag?.activeHandle == WallEndpointHandle.START
                            )
                            drawWallHandle(
                                center = projectedEnd.screen,
                                active = wallEditDrag?.activeHandle == WallEndpointHandle.END
                            )
                            wallEditDrag?.let { drag ->
                                val projectedRaw = projectGroundPoint(
                                    point = drag.rawPoint,
                                    sceneCenter = activeSceneCenter,
                                    cameraState = cameraState,
                                    viewport = size
                                )
                                val projectedResolved = projectGroundPoint(
                                    point = drag.resolvedPoint,
                                    sceneCenter = activeSceneCenter,
                                    cameraState = cameraState,
                                    viewport = size
                                )
                                val projectedFixed = projectGroundPoint(
                                    point = drag.fixedPoint,
                                    sceneCenter = activeSceneCenter,
                                    cameraState = cameraState,
                                    viewport = size
                                )
                                if (projectedFixed != null && projectedResolved != null) {
                                    drawLine(
                                        color = Color(0xFF29B6F6),
                                        start = projectedFixed.screen,
                                        end = projectedResolved.screen,
                                        strokeWidth = 4.5f,
                                        cap = StrokeCap.Round
                                    )
                                }
                                if (projectedRaw != null && projectedResolved != null) {
                                    drawLine(
                                        color = Color(0xFFB0BEC5),
                                        start = projectedRaw.screen,
                                        end = projectedResolved.screen,
                                        strokeWidth = 2.2f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
                if (
                    blueprintMode &&
                    lockTopView &&
                    blueprintTool == BlueprintCanvasTool.NAVIGATE &&
                    (displayedSelectedWall != null || wallEditDrag != null || spaceMoveDrag != null || multiSelectMode)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val dragState = wallEditDrag
                            val moveDragState = spaceMoveDrag
                            val guidance = if (dragState != null) {
                                val snapLabel = dragState.snapType?.label?.let { " • $it" } ?: ""
                                "Wall edit: ${"%.1f".format(dragState.lengthFeet)} ft @ ${dragState.angleDegrees.toInt()}deg$snapLabel"
                            } else if (moveDragState != null) {
                                val snapLabel = moveDragState.snapKind.label?.let { " • $it snap" } ?: ""
                                "Move ${moveDragState.spaceName}: X ${"%.1f".format(moveDragState.workingTransform.xFeet)}' · Z ${"%.1f".format(moveDragState.workingTransform.zFeet)}'$snapLabel"
                            } else if (displayedSelectedWall != null && !multiSelectMode) {
                                "Wall edit: drag endpoint handles to reshape."
                            } else if (multiSelectMode) {
                                "Multi-select: tap objects, long-press drag to marquee."
                            } else {
                                "Canvas controls active."
                            }
                            Text(
                                text = guidance,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (proControlsEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = multiSelectMode,
                                        onClick = {
                                            haptics.tap()
                                            multiSelectMode = !multiSelectMode
                                            if (!multiSelectMode) {
                                                marqueeSelection = null
                                            }
                                        },
                                        label = { Text("Multi Select") }
                                    )
                                    FilterChip(
                                        selected = isolateSelection,
                                        onClick = {
                                            haptics.tap()
                                            isolateSelection = !isolateSelection
                                        },
                                        label = { Text("Isolate") }
                                    )
                                    FilterChip(
                                        selected = keepWallLengthLocked,
                                        onClick = {
                                            haptics.tap()
                                            keepWallLengthLocked = !keepWallLengthLocked
                                        },
                                        enabled = displayedSelectedWall != null,
                                        label = { Text("Fix Length") }
                                    )
                                    FilterChip(
                                        selected = keepWallAngleLocked,
                                        onClick = {
                                            haptics.tap()
                                            keepWallAngleLocked = !keepWallAngleLocked
                                        },
                                        enabled = displayedSelectedWall != null,
                                        label = { Text("Fix Angle") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.tap()
                                            hiddenSpaceIds = emptySet()
                                            hiddenTradeLanes = emptySet()
                                        },
                                        label = { Text("Show All") }
                                    )
                                }
                            } else {
                                Text(
                                    text = "Enable Workflow Aids in Settings for multi-select, isolate, and constraint tools.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val selectedWall = displayedSelectedWall
                            if (
                                selectedWall != null &&
                                onUpdateSpace != null &&
                                selectedWall.id !in lockedSpaceIds
                            ) {
                                val openingCount = selectedWall.openings.sumOf { it.count.coerceAtLeast(0) }
                                Text(
                                    text = "Wall openings: $openingCount total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            onUpdateSpace.invoke(
                                                selectedWall.withOpeningPreset(
                                                    widthFeet = 3.0,
                                                    heightFeet = 7.0
                                                )
                                            )
                                        },
                                        label = { Text("Door 3x7") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            onUpdateSpace.invoke(
                                                selectedWall.withOpeningPreset(
                                                    widthFeet = 5.0,
                                                    heightFeet = 7.0
                                                )
                                            )
                                        },
                                        label = { Text("Door 5x7") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            onUpdateSpace.invoke(
                                                selectedWall.withOpeningPreset(
                                                    widthFeet = 3.0,
                                                    heightFeet = 4.0
                                                )
                                            )
                                        },
                                        label = { Text("Window 3x4") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            onUpdateSpace.invoke(
                                                selectedWall.withOpeningPreset(
                                                    widthFeet = 4.0,
                                                    heightFeet = 5.0
                                                )
                                            )
                                        },
                                        label = { Text("Window 4x5") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.tap()
                                            onUpdateSpace.invoke(selectedWall.copy(openings = emptyList()))
                                        },
                                        enabled = selectedWall.openings.isNotEmpty(),
                                        label = { Text("Clear") }
                                    )
                                }
                            }
                        }
                    }
                }
                if (useMicroToolbar && fullScreenBlueprint) {
                    BlueprintIconRail(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp),
                        expanded = dockExpanded,
                        onToggleExpanded = {
                            haptics.tap()
                            dockExpanded = !dockExpanded
                        },
                        section = railSection,
                        onCycleSection = {
                            haptics.tap()
                            railSection = railSection.next()
                        },
                        activeLayerLabel = objectTradeFilter.label,
                        onCycleLayer = {
                            haptics.tap()
                            objectTradeFilter = when (objectTradeFilter) {
                                ModelTradeFilter.ALL -> ModelTradeFilter.DRYWALL
                                ModelTradeFilter.DRYWALL -> ModelTradeFilter.CONCRETE
                                ModelTradeFilter.CONCRETE -> ModelTradeFilter.ROOMS
                                ModelTradeFilter.ROOMS -> ModelTradeFilter.ALL
                            }
                        },
                        lockTopView = lockTopView,
                        onToggleTopView = {
                            haptics.tap()
                            lockTopView = !lockTopView
                            if (lockTopView) {
                                cameraPitch = 75f
                                cameraYaw = 0f
                            }
                        },
                        wallChainMode = wallChainMode,
                        onToggleWallChain = {
                            haptics.tap()
                            wallChainMode = !wallChainMode
                            if (!wallChainMode) {
                                drawWallStart = null
                                drawWallPreview = null
                            }
                        },
                        wallOrthoLock = wallOrthoLock,
                        onToggleWallOrtho = {
                            haptics.tap()
                            wallOrthoLock = !wallOrthoLock
                        },
                        wallAngleSnapLabel = "${wallAngleSnapDegrees.toInt()}deg",
                        onCycleWallAngleSnap = {
                            haptics.tap()
                            wallAngleSnapDegrees = when (wallAngleSnapDegrees.toInt()) {
                                15 -> 30f
                                30 -> 45f
                                45 -> 90f
                                else -> 15f
                            }
                        },
                        wallAnchorSnapLabel = "${wallAnchorSnapFeet}ft",
                        onCycleWallAnchorSnap = {
                            haptics.tap()
                            wallAnchorSnapFeet = when (wallAnchorSnapFeet) {
                                0.5f -> 0.75f
                                0.75f -> 1.0f
                                1.0f -> 1.5f
                                else -> 0.5f
                            }
                        },
                        onCancelWallDraw = {
                            haptics.tap()
                            cancelWallDraft()
                        },
                        selectedTool = blueprintTool,
                        onSelectTool = { tool ->
                            haptics.tap()
                            blueprintTool = tool
                            if (tool == BlueprintCanvasTool.DRAW_WALL) {
                                railSection = BlueprintRailSection.DRAFT
                                activateDrawWallMode()
                            }
                        },
                        showGrid = showGrid,
                        onToggleGrid = {
                            haptics.tap()
                            showGrid = !showGrid
                        },
                        showDimensions = showDimensions,
                        onToggleDimensions = {
                            haptics.tap()
                            showDimensions = !showDimensions
                        },
                        onUndo = onUndoBlueprint,
                        onRedo = onRedoBlueprint,
                        canUndo = canUndoBlueprint,
                        canRedo = canRedoBlueprint,
                        onAddSpace = {
                            haptics.confirm()
                            onAddSpace()
                        },
                        onQuickRoom = onQuickRoom?.let { action ->
                            {
                                haptics.confirm()
                                action()
                            }
                        },
                        onQuickAddWall = onQuickAddWall?.let { action ->
                            {
                                haptics.confirm()
                                action()
                            }
                        },
                        onQuickAddSlab = onQuickAddSlab?.let { action ->
                            {
                                haptics.confirm()
                                action()
                            }
                        },
                        onQuickAddBed = onQuickAddBed?.let { action ->
                            {
                                haptics.confirm()
                                action()
                            }
                        },
                        onAutoLayout = {
                            haptics.confirm()
                            runAutoLayout()
                        },
                        onOptimize = onOptimizeLayout?.let { action ->
                            {
                                haptics.confirm()
                                action()
                                frameModel()
                            }
                        },
                        onCenterLayout = onCenterLayout?.let { action ->
                            {
                                haptics.tap()
                                action()
                            }
                        },
                        onAlignNorth = onAlignNorth?.let { action ->
                            {
                                haptics.tap()
                                action()
                            }
                        },
                        onFrame = {
                            haptics.tap()
                            frameModel()
                        },
                        onReset = {
                            haptics.tap()
                            resetCamera()
                        },
                        onDownloadPng = onDownloadBlueprintPng,
                        onSharePng = onShareBlueprintPng,
                        onDownloadPdf = onDownloadBlueprintPdf,
                        onSharePdf = onShareBlueprintPdf,
                        onToggleFullscreen = onToggleBlueprintFullscreen,
                        isFullscreen = fullScreenBlueprint
                    )
                    if (blueprintTool == BlueprintCanvasTool.DRAW_WALL) {
                        val previewLengthFeet = drawWallStart?.let { start ->
                            drawWallPreview?.let { end ->
                                hypot(end.xFeet - start.xFeet, end.zFeet - start.zFeet)
                            }
                        }
                        val previewAngleDegrees = drawWallStart?.let { start ->
                            drawWallPreview?.let { end ->
                                normalizeAngleDegrees(
                                    Math.toDegrees(
                                        atan2(
                                            end.zFeet - start.zFeet,
                                            end.xFeet - start.xFeet
                                        )
                                    )
                                )
                            }
                        }
                        val drawModeSummary = buildString {
                            append(if (wallChainMode) "Chain" else "Single")
                            append(" • ")
                            append(if (wallOrthoLock) "Ortho" else "Angle ${wallAngleSnapDegrees.toInt()}deg")
                            append(" • Anchor ${wallAnchorSnapFeet}ft")
                        }
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = if (drawWallStart == null) {
                                        "Draw Wall: tap start/end or drag and release"
                                    } else if (previewLengthFeet != null && previewLengthFeet > 0.01) {
                                        if (previewAngleDegrees != null) {
                                            "Draw Wall: ${"%.1f".format(previewLengthFeet)} ft @ ${previewAngleDegrees.toInt()}deg"
                                        } else {
                                            "Draw Wall: ${"%.1f".format(previewLengthFeet)} ft"
                                        }
                                    } else {
                                        "Draw Wall: tap end point to place wall"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = drawModeSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (useMicroToolbar) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = dockExpanded,
                                onClick = {
                                    haptics.tap()
                                    dockExpanded = !dockExpanded
                                },
                                label = { Text(if (dockExpanded) "Tools" else "Tools+") }
                            )
                            if (dockExpanded) {
                                FilterChip(
                                    selected = showControlPanel,
                                    onClick = {
                                        haptics.tap()
                                        showControlPanel = !showControlPanel
                                    },
                                    label = { Text(if (showControlPanel) "Panels" else "Canvas") }
                                )
                                FilterChip(
                                    selected = lockTopView,
                                    onClick = {
                                        haptics.tap()
                                        lockTopView = !lockTopView
                                        if (lockTopView) {
                                            showDimensions = true
                                            cameraPitch = 75f
                                            cameraYaw = 0f
                                        }
                                    },
                                    label = { Text(if (lockTopView) "2D" else "3D") }
                                )
                                FilterChip(
                                    selected = showDimensions,
                                    onClick = {
                                        haptics.tap()
                                        showDimensions = !showDimensions
                                    },
                                    label = { Text("Dims") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.confirm()
                                        onAddSpace()
                                    },
                                    label = { Text("Add") }
                                )
                                onQuickRoom?.let { action ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            action()
                                        },
                                        label = { Text("Room") }
                                    )
                                }
                                onQuickAddWall?.let { action ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            action()
                                        },
                                        label = { Text("Wall") }
                                    )
                                }
                                onQuickAddSlab?.let { action ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            action()
                                        },
                                        label = { Text("Slab") }
                                    )
                                }
                                onQuickAddBed?.let { action ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.confirm()
                                            action()
                                        },
                                        label = { Text("Bed") }
                                    )
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.confirm()
                                        runAutoLayout()
                                    },
                                    label = { Text("Auto") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.tap()
                                        frameModel()
                                    },
                                    label = { Text("Fit") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.tap()
                                        resetCamera()
                                    },
                                    label = { Text("Reset") }
                                )
                                if (allowPanelScroll && workspaceScroll.value > 0) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            haptics.tap()
                                            scope.launch {
                                                workspaceScroll.animateScrollTo(0)
                                            }
                                        },
                                        label = { Text("Top") }
                                    )
                                }
                                if (showControlPanel) {
                                    BuilderControlTab.entries.forEach { tab ->
                                        FilterChip(
                                            selected = activeControlTab == tab,
                                            onClick = {
                                                haptics.tap()
                                                activeControlTab = tab
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                    FilterChip(
                                        selected = showGrid,
                                        onClick = {
                                            haptics.tap()
                                            showGrid = !showGrid
                                        },
                                        label = { Text("Grid") }
                                    )
                                    FilterChip(
                                        selected = showHud,
                                        onClick = {
                                            haptics.tap()
                                            showHud = !showHud
                                        },
                                        label = { Text("HUD") }
                                    )
                                }
                            }
                        }
                    }
                } else {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = showControlPanel,
                                onClick = {
                                    haptics.tap()
                                    showControlPanel = !showControlPanel
                                },
                                label = { Text(if (showControlPanel) "Panels On" else "Panels Off") }
                            )
                            if (allowPanelScroll && workspaceScroll.value > 0) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.tap()
                                        scope.launch {
                                            workspaceScroll.animateScrollTo(0)
                                        }
                                    },
                                    label = { Text("Top") }
                                )
                            }
                        }
                        if (!showControlPanel) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.confirm()
                                        onAddSpace()
                                    },
                                    label = { Text("Add") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.tap()
                                        frameModel()
                                    },
                                    label = { Text("Fit") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.tap()
                                        resetCamera()
                                    },
                                    label = { Text("Reset") }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        haptics.confirm()
                                        runAutoLayout()
                                    },
                                    label = { Text("Auto Arrange") }
                                )
                            }
                        } else {
                            Text(
                                text = "Use panels for detailed scene/object edits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (lockTopView) "Mode: 2D Plan" else "Mode: 3D Orbit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (blueprintMode) {
                                "Visible: ${sceneSpaces.size} / ${project.spaces.size}"
                            } else {
                                "Objects: ${project.spaces.size}"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (blueprintMode) {
                            Text(
                                text = "Layer: ${objectTradeFilter.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Active: ${displayedSelectedSpace?.name ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (blueprintMode && lockTopView) {
                            Text(
                                text = if (blueprintTool == BlueprintCanvasTool.DRAW_WALL) {
                                    "Canvas: tap or drag to place wall segments."
                                } else if (multiSelectMode) {
                                    "Canvas: tap to select, long-press drag for marquee, double-tap to edit."
                                } else if (!proControlsEnabled) {
                                    "Canvas: tap to select. Tap walls to edit."
                                } else {
                                    "Canvas: tap to select. Tap walls or double-tap any object to edit."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (blueprintMode && showDimensions) {
                            Text(
                                text = "Dimensions On",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                }
                if (showHud && displayedSelectedSpace != null) {
                    ActiveObjectHud(
                        space = displayedSelectedSpace,
                        transform = displayedSelectedSpace.transform,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .width(220.dp)
                    )
                }
            }
        }

        if (project.spaces.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "No spaces in this model yet",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create your first space and it will appear in the 3D builder.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return
        }

        if (panelVisible && activeControlTab == BuilderControlTab.OBJECTS) {
            Card {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Model Objects",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (proControlsEnabled) {
                                "${filteredSpaces.size} object(s) • ${hiddenSpaceIds.size} hidden • ${lockedSpaceIds.size} locked"
                            } else if (hiddenSpaceIds.isNotEmpty() || lockedSpaceIds.isNotEmpty()) {
                                "${filteredSpaces.size} object(s) • ${hiddenSpaceIds.size} hidden • ${lockedSpaceIds.size} locked"
                            } else {
                                "${filteredSpaces.size} object(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                showObjectSearchFilters = !showObjectSearchFilters
                            }
                        ) {
                            Text(if (showObjectSearchFilters) "Hide Filters" else "Show Filters")
                        }
                    }
                    if (proControlsEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = multiSelectMode,
                                onClick = {
                                    haptics.tap()
                                    multiSelectMode = !multiSelectMode
                                    if (!multiSelectMode) {
                                        selectedSpaceIds = selectedSpaceId?.let { setOf(it) } ?: emptySet()
                                    }
                                },
                                label = { Text("Multi Select") }
                            )
                            FilterChip(
                                selected = isolateSelection,
                                onClick = {
                                    haptics.tap()
                                    isolateSelection = !isolateSelection
                                },
                                label = { Text("Isolate") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    haptics.tap()
                                    hiddenSpaceIds = hiddenSpaceIds + effectiveSelectedIds
                                },
                                enabled = effectiveSelectedIds.isNotEmpty(),
                                label = { Text("Hide Selected") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    haptics.tap()
                                    hiddenSpaceIds = emptySet()
                                },
                                enabled = hiddenSpaceIds.isNotEmpty(),
                                label = { Text("Show Hidden") }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModelTradeLane.entries.forEach { lane ->
                                val hiddenLane = lane in hiddenTradeLanes
                                FilterChip(
                                    selected = !hiddenLane,
                                    onClick = {
                                        haptics.tap()
                                        toggleLaneVisibility(lane)
                                    },
                                    label = {
                                        Text(if (hiddenLane) "${lane.label} Off" else lane.label)
                                    }
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    haptics.tap()
                                    hiddenTradeLanes = emptySet()
                                },
                                enabled = hiddenTradeLanes.isNotEmpty(),
                                label = { Text("Show Lanes") }
                            )
                        }
                    } else {
                        Text(
                            text = "Workflow Aids adds isolate, lane visibility, and multi-select controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showObjectSearchFilters) {
                        OutlinedTextField(
                            value = objectQuery,
                            onValueChange = { objectQuery = it },
                            label = { Text("Find by name or type") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModelTradeFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = objectTradeFilter == filter,
                                    onClick = {
                                        haptics.tap()
                                        objectTradeFilter = filter
                                    },
                                    label = { Text(filter.label) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Filters hidden to keep this list focused. Show filters when you need to narrow results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (filteredSpaces.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "No matching objects",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Try another trade filter or clear the search field.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(objectListHeight)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredSpaces.forEach { space ->
                                val isSelected = space.id in effectiveSelectedIds
                                val isLocked = space.id in lockedSpaceIds
                                val isHidden = space.id in hiddenSpaceIds
                                val laneLabel = modelTradeLaneForSpace(space).label
                                Card(
                                    onClick = {
                                        haptics.tap()
                                        selectedSpaceId = space.id
                                        selectedSpaceIds = if (multiSelectMode) {
                                            if (space.id in selectedSpaceIds) {
                                                (selectedSpaceIds - space.id).ifEmpty { setOf(space.id) }
                                            } else {
                                                selectedSpaceIds + space.id
                                            }
                                        } else {
                                            setOf(space.id)
                                        }
                                        if (activeControlTab == BuilderControlTab.OBJECTS) {
                                            activeControlTab = BuilderControlTab.INSPECTOR
                                        }
                                    },
                                    colors = if (isSelected) {
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else if (isHidden) {
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    } else {
                                        CardDefaults.cardColors()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(
                                                    color = colorFromHex(space.transform.colorHex),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = space.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = buildString {
                                                    append("${modelLabel(space)} · $laneLabel")
                                                    if (proControlsEnabled || isLocked || isHidden) {
                                                        if (isLocked) append(" · Locked")
                                                        if (isHidden) append(" · Hidden")
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (proControlsEnabled || isLocked || isHidden) {
                                            IconButton(onClick = {
                                                haptics.tap()
                                                toggleSpaceLock(space.id)
                                            }) {
                                                Icon(
                                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = if (isLocked) "Unlock" else "Lock"
                                                )
                                            }
                                            IconButton(onClick = {
                                                haptics.tap()
                                                toggleSpaceVisibility(space.id)
                                            }) {
                                                Icon(
                                                    imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (isHidden) "Show" else "Hide"
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            if (isLocked) return@IconButton
                                            haptics.tap()
                                            onEditSpace(space)
                                        }, enabled = !isLocked) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = {
                                            if (isLocked) return@IconButton
                                            haptics.tap()
                                            onDuplicateSpace(space.id)
                                        }, enabled = !isLocked) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                                        }
                                        IconButton(onClick = {
                                            if (isLocked) return@IconButton
                                            haptics.confirm()
                                            onDeleteSpace(space.id)
                                        }, enabled = !isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (panelVisible && activeControlTab == BuilderControlTab.INSPECTOR && selectedSpace != null) {
            Card {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Transform Inspector: ${selectedSpace.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (selectedSpaceLocked) {
                        Text(
                            text = "This object is locked. Unlock it in the Objects tab to apply edits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TransformSlider(
                        label = "X Position",
                        value = draftTransform.xFeet.toFloat(),
                        valueRange = -120f..120f,
                        onValueChange = {
                            draftTransform = draftTransform.copy(xFeet = it.toDouble())
                        }
                    )
                    TransformSlider(
                        label = "Elevation",
                        value = draftTransform.yFeet.toFloat(),
                        valueRange = 0f..40f,
                        onValueChange = {
                            draftTransform = draftTransform.copy(yFeet = it.toDouble())
                        }
                    )
                    TransformSlider(
                        label = "Z Position",
                        value = draftTransform.zFeet.toFloat(),
                        valueRange = -120f..120f,
                        onValueChange = {
                            draftTransform = draftTransform.copy(zFeet = it.toDouble())
                        }
                    )
                    TransformSlider(
                        label = "Yaw",
                        value = draftTransform.yawDegrees.toFloat(),
                        valueRange = -180f..180f,
                        onValueChange = {
                            val nextYaw = if (snapYaw) {
                                snapDegrees(it.toDouble(), 15.0)
                            } else {
                                it.toDouble()
                            }
                            draftTransform = draftTransform.copy(yawDegrees = nextYaw)
                        },
                        suffix = "deg"
                    )
                    if (snapYaw) {
                        Text(
                            text = "Yaw snapping is active (15deg increments).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modelColorPalette.forEach { colorHex ->
                            val selected = draftTransform.colorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        color = colorFromHex(colorHex),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptics.tap()
                                        draftTransform = draftTransform.copy(colorHex = colorHex)
                                    }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            showInspectorPrecisionTools = !showInspectorPrecisionTools
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showInspectorPrecisionTools) {
                                "Hide Precision Tools"
                            } else {
                                "Show Precision Tools"
                            }
                        )
                    }
                    if (showInspectorPrecisionTools) {
                        Text(
                            text = "Nudge Step",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.25f, 0.5f, 1f, 2f).forEach { step ->
                                FilterChip(
                                    selected = nudgeStepFt == step,
                                    onClick = {
                                        haptics.tap()
                                        nudgeStepFt = step
                                    },
                                    label = { Text("${"%.2f".format(step).trimEnd('0').trimEnd('.')} ft") }
                                )
                            }
                        }
                        Text(
                            text = "Quick Nudge",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        xFeet = draftTransform.xFeet - nudgeStepFt
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("X-")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        xFeet = draftTransform.xFeet + nudgeStepFt
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("X+")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        zFeet = draftTransform.zFeet - nudgeStepFt
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Z-")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        zFeet = draftTransform.zFeet + nudgeStepFt
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Z+")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        yFeet = (draftTransform.yFeet - nudgeStepFt).coerceAtLeast(0.0)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Lower")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        yFeet = draftTransform.yFeet + nudgeStepFt
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Raise")
                            }
                        }
                        Text(
                            text = "Quick Yaw Snap",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(-90.0, 0.0, 90.0, 180.0).forEach { yaw ->
                                OutlinedButton(
                                    onClick = {
                                        haptics.tap()
                                        draftTransform = draftTransform.copy(yawDegrees = yaw)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${yaw.toInt()}deg")
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(
                                        xFeet = 0.0,
                                        zFeet = 0.0
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Center")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    draftTransform = draftTransform.copy(yFeet = 0.0)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ground")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    val step = nudgeStepFt.toDouble()
                                    draftTransform = draftTransform.copy(
                                        xFeet = snapValue(draftTransform.xFeet, step),
                                        yFeet = snapValue(draftTransform.yFeet, step).coerceAtLeast(0.0),
                                        zFeet = snapValue(draftTransform.zFeet, step),
                                        yawDegrees = snapDegrees(draftTransform.yawDegrees, 15.0)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Snap")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.confirm()
                                onUpdateTransform(selectedSpace.id, draftTransform)
                            },
                            enabled = !selectedSpaceLocked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply Transform")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                draftTransform = SpaceTransform(colorHex = draftTransform.colorHex)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        } else if (panelVisible && activeControlTab == BuilderControlTab.INSPECTOR) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "No active object",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open Objects tab and select a space to use the inspector.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveObjectHud(
    space: Space,
    transform: SpaceTransform,
    modifier: Modifier = Modifier
) {
    val geometrySummary = when (val geometry = space.geometry) {
        is Geometry.Wall -> "Length ${"%.1f".format(geometry.length.toFeet())}' • Height ${"%.1f".format(geometry.height.toFeet())}'"
        is Geometry.Slab -> "L ${"%.1f".format(geometry.length.toFeet())}' • W ${"%.1f".format(geometry.width.toFeet())}' • T ${"%.2f".format(geometry.thickness.toFeet())}'"
        is Geometry.Rect -> "L ${"%.1f".format(geometry.length.toFeet())}' • W ${"%.1f".format(geometry.width.toFeet())}'"
        is Geometry.Circle -> "Radius ${"%.1f".format(geometry.radius.toFeet())}'"
        is Geometry.LShape -> {
            val a = geometry.rectA
            val b = geometry.rectB
            "A ${"%.1f".format(a.length.toFeet())}'x${"%.1f".format(a.width.toFeet())}' • B ${"%.1f".format(b.length.toFeet())}'x${"%.1f".format(b.width.toFeet())}'"
        }
    }
    val quantitySummary = when (val geometry = space.geometry) {
        is Geometry.Slab -> {
            val volumeCuFt = geometry.length.toFeet() * geometry.width.toFeet() * geometry.thickness.toFeet()
            val yards = volumeCuFt / 27.0
            "Qty: ${"%.1f".format(volumeCuFt)} cu ft • ${"%.2f".format(yards)} yd3"
        }
        else -> "Qty: ${"%.1f".format(space.geometry.areaSqFt())} sq ft"
    }
    val openingSummary = if (space.openings.isEmpty()) {
        null
    } else {
        val total = space.openings.sumOf { it.count.coerceAtLeast(0) }
        "Openings: $total"
    }
    val paintCoats = Regex("(\\d+)\\s*coats?").find(space.name.lowercase())
        ?.groupValues
        ?.getOrNull(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Active: ${space.name}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = modelLabel(space),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = geometrySummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = quantitySummary,
                style = MaterialTheme.typography.bodySmall
            )
            if (openingSummary != null) {
                Text(
                    text = openingSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (paintCoats != null) {
                Text(
                    text = "Paint coats: $paintCoats",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "X ${"%.1f".format(transform.xFeet)}  Y ${"%.1f".format(transform.yFeet)}  Z ${"%.1f".format(transform.zFeet)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Yaw ${"%.0f".format(transform.yawDegrees)}deg",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private data class ModelBounds(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float
)

private fun modelBounds(spaces: List<Space>): ModelBounds {
    if (spaces.isEmpty()) {
        return ModelBounds(minX = -5f, maxX = 5f, minZ = -5f, maxZ = 5f)
    }
    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY
    spaces.forEach { space ->
        val dims = dimensions(space.geometry)
        val halfW = dims.width / 2f
        val halfD = dims.depth / 2f
        val x = space.transform.xFeet.toFloat()
        val z = space.transform.zFeet.toFloat()
        minX = min(minX, x - halfW)
        maxX = max(maxX, x + halfW)
        minZ = min(minZ, z - halfD)
        maxZ = max(maxZ, z + halfD)
    }
    return ModelBounds(
        minX = minX,
        maxX = maxX,
        minZ = minZ,
        maxZ = maxZ
    )
}

private fun frameZoomForModel(spaces: List<Space>): Float {
    if (spaces.isEmpty()) return 1.05f
    val bounds = modelBounds(spaces)
    val span = max(bounds.maxX - bounds.minX, bounds.maxZ - bounds.minZ).coerceAtLeast(16f)
    return (150f / span).coerceIn(0.55f, 3.8f)
}

private fun focusZoomForSpace(space: Space): Float {
    val dims = dimensions(space.geometry)
    val span = max(dims.width, dims.depth).coerceAtLeast(6f)
    return (90f / span).coerceIn(0.75f, 3.8f)
}

private fun overlappingSpaceIds(spaces: List<Space>): Set<String> {
    if (spaces.size < 2) return emptySet()
    val overlaps = mutableSetOf<String>()
    for (i in 0 until spaces.lastIndex) {
        val a = spaces[i]
        val aDims = dimensions(a.geometry)
        val aMinX = a.transform.xFeet - (aDims.width / 2.0)
        val aMaxX = a.transform.xFeet + (aDims.width / 2.0)
        val aMinZ = a.transform.zFeet - (aDims.depth / 2.0)
        val aMaxZ = a.transform.zFeet + (aDims.depth / 2.0)
        for (j in (i + 1) until spaces.size) {
            val b = spaces[j]
            val bDims = dimensions(b.geometry)
            val bMinX = b.transform.xFeet - (bDims.width / 2.0)
            val bMaxX = b.transform.xFeet + (bDims.width / 2.0)
            val bMinZ = b.transform.zFeet - (bDims.depth / 2.0)
            val bMaxZ = b.transform.zFeet + (bDims.depth / 2.0)
            val intersects = aMaxX >= bMinX &&
                bMaxX >= aMinX &&
                aMaxZ >= bMinZ &&
                bMaxZ >= aMinZ
            if (intersects) {
                overlaps += a.id
                overlaps += b.id
            }
        }
    }
    return overlaps
}

private fun snapDegrees(value: Double, increment: Double): Double {
    if (increment <= 0.0) return value
    return kotlin.math.round(value / increment) * increment
}

private fun snapValue(value: Double, step: Double): Double {
    if (step <= 0.0) return value
    return kotlin.math.round(value / step) * step
}

private fun snapGroundPoint(point: GroundPoint, stepFeet: Double): GroundPoint {
    val safeStep = stepFeet.coerceAtLeast(0.25)
    return GroundPoint(
        xFeet = snapValue(point.xFeet, safeStep),
        zFeet = snapValue(point.zFeet, safeStep)
    )
}

private fun normalizeAngleDegrees(angle: Double): Double {
    var normalized = angle % 360.0
    if (normalized < 0.0) normalized += 360.0
    return normalized
}

private fun applyWallDirectionConstraint(
    start: GroundPoint,
    rawPoint: GroundPoint,
    orthoLock: Boolean,
    angleSnapDegrees: Double
): GroundPoint {
    val dx = rawPoint.xFeet - start.xFeet
    val dz = rawPoint.zFeet - start.zFeet
    if (kotlin.math.abs(dx) < 0.0001 && kotlin.math.abs(dz) < 0.0001) {
        return rawPoint
    }
    if (orthoLock) {
        return if (kotlin.math.abs(dx) >= kotlin.math.abs(dz)) {
            rawPoint.copy(zFeet = start.zFeet)
        } else {
            rawPoint.copy(xFeet = start.xFeet)
        }
    }
    if (angleSnapDegrees <= 0.0) return rawPoint
    val length = hypot(dx, dz)
    if (length < 0.0001) return rawPoint
    val incrementRad = Math.toRadians(angleSnapDegrees)
    val snappedAngle = kotlin.math.round(atan2(dz, dx) / incrementRad) * incrementRad
    return GroundPoint(
        xFeet = start.xFeet + (kotlin.math.cos(snappedAngle) * length),
        zFeet = start.zFeet + (kotlin.math.sin(snappedAngle) * length)
    )
}

private fun snapPointToNearestAnchor(
    point: GroundPoint,
    anchors: List<GroundPoint>,
    thresholdFeet: Double
): GroundPoint {
    if (anchors.isEmpty()) return point
    var nearest: GroundPoint? = null
    var nearestDistance = Double.POSITIVE_INFINITY
    anchors.forEach { anchor ->
        val distance = hypot(anchor.xFeet - point.xFeet, anchor.zFeet - point.zFeet)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearest = anchor
        }
    }
    return if (nearest != null && nearestDistance <= thresholdFeet) {
        nearest!!
    } else {
        point
    }
}

private fun collectWallAnchorPoints(spaces: List<Space>): List<GroundPoint> {
    if (spaces.isEmpty()) return emptyList()
    val anchors = mutableListOf<GroundPoint>()
    spaces.forEach { space ->
        val wall = space.geometry as? Geometry.Wall ?: return@forEach
        val halfLength = wall.length.toFeet() / 2.0
        val yawRad = Math.toRadians(space.transform.yawDegrees)
        val dx = kotlin.math.cos(yawRad) * halfLength
        val dz = kotlin.math.sin(yawRad) * halfLength
        anchors += GroundPoint(
            xFeet = space.transform.xFeet - dx,
            zFeet = space.transform.zFeet - dz
        )
        anchors += GroundPoint(
            xFeet = space.transform.xFeet + dx,
            zFeet = space.transform.zFeet + dz
        )
    }
    return anchors
}

private fun screenToGroundPoint(
    tapOffset: Offset,
    viewport: Size,
    sceneCenter: Vec3,
    cameraState: CameraState
): GroundPoint? {
    if (viewport.width <= 1f || viewport.height <= 1f) return null
    val pitch = Math.toRadians(cameraState.pitchDeg.toDouble()).toFloat()
    val sinPitch = sin(pitch)
    val cosPitch = cos(pitch)
    val cameraDistance = 170f
    val worldScale = 11f
    val base = worldScale * cameraDistance * cameraState.zoom
    val cx = viewport.width * 0.5f
    val cy = viewport.height * 0.56f
    val dx = tapOffset.x - cx - cameraState.panX
    val dy = tapOffset.y - cy - cameraState.panY
    val denominator = (sinPitch * base) + (dy * cosPitch)
    if (kotlin.math.abs(denominator) < 0.0001f) return null
    val zLocal = (dy * cameraDistance) / denominator
    val xLocal = (dx * (cameraDistance - (zLocal * cosPitch))) / base
    if (!xLocal.isFinite() || !zLocal.isFinite()) return null
    return GroundPoint(
        xFeet = (xLocal + sceneCenter.x).toDouble(),
        zFeet = (zLocal + sceneCenter.z).toDouble()
    )
}

private fun findSpaceAtGroundPoint(
    point: GroundPoint,
    spaces: List<Space>
): Space? {
    if (spaces.isEmpty()) return null
    return spaces
        .mapNotNull { space ->
            scoreGroundTapForSpace(
                point = point,
                space = space
            )?.let { score ->
                space to score
            }
        }
        .minByOrNull { it.second }
        ?.first
}

private fun scoreGroundTapForSpace(
    point: GroundPoint,
    space: Space
): Double? {
    val dims = dimensions(space.geometry)
    val halfWidth = dims.width / 2f
    val halfDepth = dims.depth / 2f
    val alongPadding = if (space.geometry is Geometry.Wall) 1.0f else 0.55f
    val crossPadding = if (space.geometry is Geometry.Wall) 1.15f else 0.55f
    val maxX = halfWidth + alongPadding
    val maxZ = halfDepth + crossPadding
    val (localX, localZ) = toLocalGroundPoint(
        point = point,
        space = space
    )
    if (kotlin.math.abs(localX) > maxX || kotlin.math.abs(localZ) > maxZ) {
        return null
    }
    val normalizedX = kotlin.math.abs(localX) / maxX.coerceAtLeast(0.001f)
    val normalizedZ = kotlin.math.abs(localZ) / maxZ.coerceAtLeast(0.001f)
    var score = (normalizedX + normalizedZ).toDouble()
    if (space.geometry is Geometry.Wall) {
        score -= 0.25
    }
    return score
}

private fun toLocalGroundPoint(
    point: GroundPoint,
    space: Space
): Pair<Float, Float> {
    val dx = (point.xFeet - space.transform.xFeet).toFloat()
    val dz = (point.zFeet - space.transform.zFeet).toFloat()
    val yaw = Math.toRadians(space.transform.yawDegrees).toFloat()
    val cosYaw = cos(yaw)
    val sinYaw = sin(yaw)
    val localX = (dx * cosYaw) + (dz * sinYaw)
    val localZ = (-dx * sinYaw) + (dz * cosYaw)
    return localX to localZ
}

private data class MarqueeSelectionState(
    val start: Offset,
    val end: Offset
) {
    fun normalizedRect(): Rect {
        val left = min(start.x, end.x)
        val top = min(start.y, end.y)
        val right = max(start.x, end.x)
        val bottom = max(start.y, end.y)
        return Rect(left = left, top = top, right = right, bottom = bottom)
    }
}

private enum class WallEndpointHandle {
    START,
    END
}

private enum class WallSnapType(val label: String) {
    GRID("Grid Snap"),
    ANCHOR("Anchor Snap"),
    INTERSECTION("Intersection Trim"),
    LENGTH("Length Locked"),
    ANGLE("Angle Locked")
}

private data class WallEndpoints(
    val start: GroundPoint,
    val end: GroundPoint
)

private data class WallEditDragState(
    val spaceId: String,
    val activeHandle: WallEndpointHandle,
    val fixedPoint: GroundPoint,
    val rawPoint: GroundPoint,
    val resolvedPoint: GroundPoint,
    val previewSpace: Space,
    val originalLengthFeet: Double,
    val originalAngleDegrees: Double,
    val lengthFeet: Double,
    val angleDegrees: Double,
    val snapType: WallSnapType?
)

private fun wallEndpoints(space: Space): WallEndpoints? {
    val wall = space.geometry as? Geometry.Wall ?: return null
    val halfLength = wall.length.toFeet() / 2.0
    val yawRad = Math.toRadians(space.transform.yawDegrees)
    val dx = cos(yawRad) * halfLength
    val dz = sin(yawRad) * halfLength
    return WallEndpoints(
        start = GroundPoint(
            xFeet = space.transform.xFeet - dx,
            zFeet = space.transform.zFeet - dz
        ),
        end = GroundPoint(
            xFeet = space.transform.xFeet + dx,
            zFeet = space.transform.zFeet + dz
        )
    )
}

private fun projectGroundPoint(
    point: GroundPoint,
    sceneCenter: Vec3,
    cameraState: CameraState,
    viewport: Size
): ProjectedPoint? {
    return projectPoint(
        point = Vec3(
            x = point.xFeet.toFloat(),
            y = 0f,
            z = point.zFeet.toFloat()
        ),
        sceneCenter = sceneCenter,
        cameraState = cameraState,
        viewport = viewport
    )
}

private fun createWallEditDragState(
    wall: Space,
    activeHandle: WallEndpointHandle
): WallEditDragState {
    val endpoints = wallEndpoints(wall) ?: error("Wall expected for edit drag")
    val fixed = if (activeHandle == WallEndpointHandle.START) endpoints.end else endpoints.start
    val moving = if (activeHandle == WallEndpointHandle.START) endpoints.start else endpoints.end
    val lengthFeet = pointDistance(fixed, moving)
    val angle = normalizeAngleDegrees(
        Math.toDegrees(
            atan2(
                moving.zFeet - fixed.zFeet,
                moving.xFeet - fixed.xFeet
            )
        )
    )
    val baseAngle = normalizeAngleDegrees(
        Math.toDegrees(
            atan2(
                endpoints.end.zFeet - endpoints.start.zFeet,
                endpoints.end.xFeet - endpoints.start.xFeet
            )
        )
    )
    return WallEditDragState(
        spaceId = wall.id,
        activeHandle = activeHandle,
        fixedPoint = fixed,
        rawPoint = moving,
        resolvedPoint = moving,
        previewSpace = wall,
        originalLengthFeet = lengthFeet,
        originalAngleDegrees = baseAngle,
        lengthFeet = lengthFeet,
        angleDegrees = angle,
        snapType = null
    )
}

private fun findNearestWallHandle(
    wall: Space,
    tapOffset: Offset,
    sceneCenter: Vec3,
    cameraState: CameraState,
    viewport: Size,
    handleRadiusPx: Float = 28f
): WallEndpointHandle? {
    val endpoints = wallEndpoints(wall) ?: return null
    val startProjected = projectGroundPoint(
        point = endpoints.start,
        sceneCenter = sceneCenter,
        cameraState = cameraState,
        viewport = viewport
    ) ?: return null
    val endProjected = projectGroundPoint(
        point = endpoints.end,
        sceneCenter = sceneCenter,
        cameraState = cameraState,
        viewport = viewport
    ) ?: return null
    val startDistance = (startProjected.screen - tapOffset).getDistance()
    val endDistance = (endProjected.screen - tapOffset).getDistance()
    val bestDistance = min(startDistance, endDistance)
    if (bestDistance > handleRadiusPx) return null
    return if (startDistance <= endDistance) {
        WallEndpointHandle.START
    } else {
        WallEndpointHandle.END
    }
}

private fun resolveWallEndpointDrag(
    dragState: WallEditDragState,
    rawPoint: GroundPoint,
    gridStepFeet: Double,
    orthoLock: Boolean,
    angleSnapDegrees: Double,
    anchorSnapFeet: Double,
    anchors: List<GroundPoint>,
    otherWalls: List<Space>,
    keepLength: Boolean,
    keepAngle: Boolean
): WallEditDragState {
    var snapType: WallSnapType? = null
    var resolved = snapGroundPoint(rawPoint, gridStepFeet)
    if (pointDistance(rawPoint, resolved) > 0.001) {
        snapType = WallSnapType.GRID
    }
    if (keepAngle) {
        val fixedToMovingAngle = if (dragState.activeHandle == WallEndpointHandle.END) {
            dragState.originalAngleDegrees
        } else {
            normalizeAngleDegrees(dragState.originalAngleDegrees + 180.0)
        }
        val desiredLength = if (keepLength) {
            dragState.originalLengthFeet
        } else {
            pointDistance(dragState.fixedPoint, resolved).coerceAtLeast(1.0)
        }
        val radians = Math.toRadians(fixedToMovingAngle)
        resolved = GroundPoint(
            xFeet = dragState.fixedPoint.xFeet + (cos(radians) * desiredLength),
            zFeet = dragState.fixedPoint.zFeet + (sin(radians) * desiredLength)
        )
        snapType = if (keepLength) {
            WallSnapType.LENGTH
        } else {
            WallSnapType.ANGLE
        }
    } else {
        resolved = applyWallDirectionConstraint(
            start = dragState.fixedPoint,
            rawPoint = resolved,
            orthoLock = orthoLock,
            angleSnapDegrees = angleSnapDegrees
        )
        if (pointDistance(rawPoint, resolved) > 0.001) {
            snapType = WallSnapType.ANGLE
        }
        if (keepLength) {
            val currentAngle = atan2(
                resolved.zFeet - dragState.fixedPoint.zFeet,
                resolved.xFeet - dragState.fixedPoint.xFeet
            )
            resolved = GroundPoint(
                xFeet = dragState.fixedPoint.xFeet + (cos(currentAngle) * dragState.originalLengthFeet),
                zFeet = dragState.fixedPoint.zFeet + (sin(currentAngle) * dragState.originalLengthFeet)
            )
            snapType = WallSnapType.LENGTH
        }
    }
    val anchorSnapped = snapPointToNearestAnchor(
        point = resolved,
        anchors = anchors,
        thresholdFeet = anchorSnapFeet
    )
    if (pointDistance(anchorSnapped, resolved) > 0.001) {
        resolved = anchorSnapped
        snapType = WallSnapType.ANCHOR
    }
    val intersectionSnapped = snapPointToNearestWallIntersection(
        fixedPoint = dragState.fixedPoint,
        movingPoint = resolved,
        otherWalls = otherWalls,
        thresholdFeet = anchorSnapFeet * 1.5
    )
    if (intersectionSnapped != null) {
        resolved = intersectionSnapped
        snapType = WallSnapType.INTERSECTION
    }
    val unclampedLength = pointDistance(dragState.fixedPoint, resolved)
    val lengthFeet = unclampedLength.coerceAtLeast(1.0)
    if (lengthFeet > unclampedLength + 0.0001) {
        val direction = atan2(
            resolved.zFeet - dragState.fixedPoint.zFeet,
            resolved.xFeet - dragState.fixedPoint.xFeet
        )
        resolved = GroundPoint(
            xFeet = dragState.fixedPoint.xFeet + (cos(direction) * lengthFeet),
            zFeet = dragState.fixedPoint.zFeet + (sin(direction) * lengthFeet)
        )
    }
    val start = if (dragState.activeHandle == WallEndpointHandle.END) {
        dragState.fixedPoint
    } else {
        resolved
    }
    val end = if (dragState.activeHandle == WallEndpointHandle.END) {
        resolved
    } else {
        dragState.fixedPoint
    }
    val yawDegrees = normalizeAngleDegrees(
        Math.toDegrees(
            atan2(
                end.zFeet - start.zFeet,
                end.xFeet - start.xFeet
            )
        )
    )
    val centerX = (start.xFeet + end.xFeet) / 2.0
    val centerZ = (start.zFeet + end.zFeet) / 2.0
    val wallGeometry = dragState.previewSpace.geometry as? Geometry.Wall
    val previewSpace = if (wallGeometry != null) {
        dragState.previewSpace.copy(
            geometry = Geometry.Wall(
                length = com.tradesketch.estimator.domain.model.Millimeters.fromFeet(lengthFeet),
                height = wallGeometry.height
            ),
            transform = dragState.previewSpace.transform.copy(
                xFeet = centerX,
                zFeet = centerZ,
                yawDegrees = yawDegrees
            )
        )
    } else {
        dragState.previewSpace
    }
    return dragState.copy(
        rawPoint = rawPoint,
        resolvedPoint = resolved,
        previewSpace = previewSpace,
        lengthFeet = lengthFeet,
        angleDegrees = yawDegrees,
        snapType = snapType
    )
}

private fun snapPointToNearestWallIntersection(
    fixedPoint: GroundPoint,
    movingPoint: GroundPoint,
    otherWalls: List<Space>,
    thresholdFeet: Double
): GroundPoint? {
    var nearest: GroundPoint? = null
    var nearestDistance = Double.POSITIVE_INFINITY
    otherWalls.forEach { wall ->
        val endpoints = wallEndpoints(wall) ?: return@forEach
        val intersection = segmentIntersection(
            aStart = fixedPoint,
            aEnd = movingPoint,
            bStart = endpoints.start,
            bEnd = endpoints.end
        ) ?: return@forEach
        val distance = pointDistance(intersection, movingPoint)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearest = intersection
        }
    }
    return if (nearest != null && nearestDistance <= thresholdFeet) {
        nearest
    } else {
        null
    }
}

private fun segmentIntersection(
    aStart: GroundPoint,
    aEnd: GroundPoint,
    bStart: GroundPoint,
    bEnd: GroundPoint
): GroundPoint? {
    val x1 = aStart.xFeet
    val y1 = aStart.zFeet
    val x2 = aEnd.xFeet
    val y2 = aEnd.zFeet
    val x3 = bStart.xFeet
    val y3 = bStart.zFeet
    val x4 = bEnd.xFeet
    val y4 = bEnd.zFeet
    val denominator = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4))
    if (kotlin.math.abs(denominator) < 0.000001) {
        return null
    }
    val t = (((x1 - x3) * (y3 - y4)) - ((y1 - y3) * (x3 - x4))) / denominator
    val u = (((x1 - x3) * (y1 - y2)) - ((y1 - y3) * (x1 - x2))) / denominator
    if (t !in 0.0..1.0 || u !in 0.0..1.0) {
        return null
    }
    return GroundPoint(
        xFeet = x1 + (t * (x2 - x1)),
        zFeet = y1 + (t * (y2 - y1))
    )
}

private fun pointDistance(a: GroundPoint, b: GroundPoint): Double {
    return hypot(a.xFeet - b.xFeet, a.zFeet - b.zFeet)
}

private fun DrawScope.drawWallHandle(
    center: Offset,
    active: Boolean
) {
    drawCircle(
        color = if (active) Color(0xFF0288D1) else Color(0xFF4FC3F7),
        radius = if (active) 9.5f else 8f,
        center = center
    )
    drawCircle(
        color = Color(0xFFF1F8FF),
        radius = 4f,
        center = center
    )
}

private enum class BuilderControlTab(val label: String) {
    SCENE("Scene"),
    OBJECTS("Objects"),
    INSPECTOR("Inspector")
}

private enum class BlueprintCanvasTool(val label: String) {
    NAVIGATE("Navigate"),
    DRAW_WALL("Draw Wall")
}

private enum class BlueprintRailSection(val label: String) {
    CORE("Core"),
    DRAFT("Draft"),
    REVIEW("Review"),
    DELIVER("Deliver");

    fun next(): BlueprintRailSection {
        return entries[(ordinal + 1) % entries.size]
    }
}

private data class GroundPoint(
    val xFeet: Double,
    val zFeet: Double
)

private enum class SpaceMoveSnapKind(val label: String?) {
    NONE(label = null),
    GRID(label = "Grid"),
    EDGE(label = "Edge"),
    CENTER(label = "Center")
}

private enum class AxisSnapHint {
    NONE,
    EDGE,
    CENTER
}

private data class SpaceMovePreview(
    val transform: SpaceTransform,
    val snapKind: SpaceMoveSnapKind
)

private data class SpaceMoveDragState(
    val spaceId: String,
    val spaceName: String,
    val sourceTransform: SpaceTransform,
    val workingTransform: SpaceTransform,
    val grabOffsetXFeet: Double,
    val grabOffsetZFeet: Double,
    val snapKind: SpaceMoveSnapKind
)

private data class SpaceFootprint(
    val centerX: Double,
    val centerZ: Double,
    val halfX: Double,
    val halfZ: Double
) {
    val minX: Double get() = centerX - halfX
    val maxX: Double get() = centerX + halfX
    val minZ: Double get() = centerZ - halfZ
    val maxZ: Double get() = centerZ + halfZ
}

private fun resolveDraggedSpaceTransform(
    movingSpace: Space,
    rawCenter: GroundPoint,
    otherSpaces: List<Space>,
    gridStepFeet: Double
): SpaceMovePreview {
    val baseTransform = movingSpace.transform.copy(
        xFeet = snapValue(rawCenter.xFeet, gridStepFeet),
        zFeet = snapValue(rawCenter.zFeet, gridStepFeet)
    )
    if (otherSpaces.isEmpty()) {
        return SpaceMovePreview(
            transform = baseTransform,
            snapKind = SpaceMoveSnapKind.GRID
        )
    }

    val movingFootprint = footprintForGeometry(
        geometry = movingSpace.geometry,
        transform = baseTransform
    )
    var snappedX = baseTransform.xFeet
    var snappedZ = baseTransform.zFeet
    var xHint = AxisSnapHint.NONE
    var zHint = AxisSnapHint.NONE
    var bestXDistance = 1.1
    var bestZDistance = 1.1

    otherSpaces.forEach { other ->
        val otherFootprint = footprintForGeometry(other.geometry, other.transform)
        val xCandidates = listOf(
            otherFootprint.centerX to AxisSnapHint.CENTER,
            (otherFootprint.minX + movingFootprint.halfX) to AxisSnapHint.EDGE,
            (otherFootprint.maxX + movingFootprint.halfX) to AxisSnapHint.EDGE,
            (otherFootprint.minX - movingFootprint.halfX) to AxisSnapHint.EDGE,
            (otherFootprint.maxX - movingFootprint.halfX) to AxisSnapHint.EDGE
        )
        xCandidates.forEach { (candidate, hint) ->
            val delta = kotlin.math.abs(candidate - baseTransform.xFeet)
            if (delta <= bestXDistance) {
                bestXDistance = delta
                snappedX = candidate
                xHint = hint
            }
        }

        val zCandidates = listOf(
            otherFootprint.centerZ to AxisSnapHint.CENTER,
            (otherFootprint.minZ + movingFootprint.halfZ) to AxisSnapHint.EDGE,
            (otherFootprint.maxZ + movingFootprint.halfZ) to AxisSnapHint.EDGE,
            (otherFootprint.minZ - movingFootprint.halfZ) to AxisSnapHint.EDGE,
            (otherFootprint.maxZ - movingFootprint.halfZ) to AxisSnapHint.EDGE
        )
        zCandidates.forEach { (candidate, hint) ->
            val delta = kotlin.math.abs(candidate - baseTransform.zFeet)
            if (delta <= bestZDistance) {
                bestZDistance = delta
                snappedZ = candidate
                zHint = hint
            }
        }
    }

    val snapKind = when {
        xHint == AxisSnapHint.CENTER || zHint == AxisSnapHint.CENTER -> SpaceMoveSnapKind.CENTER
        xHint == AxisSnapHint.EDGE || zHint == AxisSnapHint.EDGE -> SpaceMoveSnapKind.EDGE
        else -> SpaceMoveSnapKind.GRID
    }

    return SpaceMovePreview(
        transform = baseTransform.copy(
            xFeet = snappedX,
            zFeet = snappedZ
        ),
        snapKind = snapKind
    )
}

private fun footprintForGeometry(
    geometry: Geometry,
    transform: SpaceTransform
): SpaceFootprint {
    val dims = dimensions(geometry)
    val halfWidth = dims.width.toDouble() / 2.0
    val halfDepth = dims.depth.toDouble() / 2.0
    val yawRadians = Math.toRadians(transform.yawDegrees)
    val cosYaw = kotlin.math.abs(kotlin.math.cos(yawRadians))
    val sinYaw = kotlin.math.abs(kotlin.math.sin(yawRadians))
    val halfX = (cosYaw * halfWidth) + (sinYaw * halfDepth)
    val halfZ = (sinYaw * halfWidth) + (cosYaw * halfDepth)
    return SpaceFootprint(
        centerX = transform.xFeet,
        centerZ = transform.zFeet,
        halfX = halfX.coerceAtLeast(0.2),
        halfZ = halfZ.coerceAtLeast(0.2)
    )
}

private enum class ModelTradeLane(val label: String) {
    DRYWALL("Drywall"),
    CONCRETE("Concrete"),
    ROOMS("Rooms")
}

private enum class ModelTradeFilter(
    val label: String,
    val lane: ModelTradeLane?
) {
    ALL(label = "All", lane = null),
    DRYWALL(label = "Drywall", lane = ModelTradeLane.DRYWALL),
    CONCRETE(label = "Concrete", lane = ModelTradeLane.CONCRETE),
    ROOMS(label = "Rooms", lane = ModelTradeLane.ROOMS)
}

private fun modelTradeLaneForSpace(space: Space): ModelTradeLane {
    return when (space.geometry) {
        is Geometry.Wall -> ModelTradeLane.DRYWALL
        is Geometry.Slab -> ModelTradeLane.CONCRETE
        else -> ModelTradeLane.ROOMS
    }
}

private fun BlueprintLayerFilter.toModelTradeFilter(): ModelTradeFilter {
    return when (this) {
        BlueprintLayerFilter.ALL -> ModelTradeFilter.ALL
        BlueprintLayerFilter.WALLS -> ModelTradeFilter.DRYWALL
        BlueprintLayerFilter.SLABS -> ModelTradeFilter.CONCRETE
        BlueprintLayerFilter.ROOMS -> ModelTradeFilter.ROOMS
    }
}

private fun ModelTradeFilter.toBlueprintLayerFilter(): BlueprintLayerFilter {
    return when (this) {
        ModelTradeFilter.ALL -> BlueprintLayerFilter.ALL
        ModelTradeFilter.DRYWALL -> BlueprintLayerFilter.WALLS
        ModelTradeFilter.CONCRETE -> BlueprintLayerFilter.SLABS
        ModelTradeFilter.ROOMS -> BlueprintLayerFilter.ROOMS
    }
}

@Composable
private fun BlueprintIconRail(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    section: BlueprintRailSection,
    onCycleSection: (() -> Unit)?,
    activeLayerLabel: String,
    onCycleLayer: (() -> Unit)?,
    lockTopView: Boolean,
    onToggleTopView: (() -> Unit)?,
    wallChainMode: Boolean,
    onToggleWallChain: (() -> Unit)?,
    wallOrthoLock: Boolean,
    onToggleWallOrtho: (() -> Unit)?,
    wallAngleSnapLabel: String,
    onCycleWallAngleSnap: (() -> Unit)?,
    wallAnchorSnapLabel: String,
    onCycleWallAnchorSnap: (() -> Unit)?,
    onCancelWallDraw: (() -> Unit)?,
    selectedTool: BlueprintCanvasTool,
    onSelectTool: (BlueprintCanvasTool) -> Unit,
    showGrid: Boolean,
    onToggleGrid: () -> Unit,
    showDimensions: Boolean,
    onToggleDimensions: () -> Unit,
    onUndo: (() -> Unit)?,
    onRedo: (() -> Unit)?,
    canUndo: Boolean,
    canRedo: Boolean,
    onAddSpace: () -> Unit,
    onQuickRoom: (() -> Unit)?,
    onQuickAddWall: (() -> Unit)?,
    onQuickAddSlab: (() -> Unit)?,
    onQuickAddBed: (() -> Unit)?,
    onAutoLayout: () -> Unit,
    onOptimize: (() -> Unit)?,
    onCenterLayout: (() -> Unit)?,
    onAlignNorth: (() -> Unit)?,
    onFrame: () -> Unit,
    onReset: () -> Unit,
    onDownloadPng: (() -> Unit)?,
    onSharePng: (() -> Unit)?,
    onDownloadPdf: (() -> Unit)?,
    onSharePdf: (() -> Unit)?,
    onToggleFullscreen: (() -> Unit)?,
    isFullscreen: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlueprintRailAction(
                icon = if (expanded) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                label = if (expanded) "Collapse" else "Expand",
                expanded = expanded,
                onClick = onToggleExpanded
            )
            BlueprintRailAction(
                icon = Icons.Default.OpenWith,
                label = "Navigate",
                selected = selectedTool == BlueprintCanvasTool.NAVIGATE,
                expanded = expanded,
                onClick = { onSelectTool(BlueprintCanvasTool.NAVIGATE) }
            )
            BlueprintRailAction(
                icon = Icons.Default.Architecture,
                label = "Draw Wall",
                selected = selectedTool == BlueprintCanvasTool.DRAW_WALL,
                expanded = expanded,
                onClick = { onSelectTool(BlueprintCanvasTool.DRAW_WALL) }
            )
            BlueprintRailAction(
                icon = Icons.Default.Add,
                label = "Add Space",
                expanded = expanded,
                onClick = onAddSpace
            )
            BlueprintRailAction(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "Undo",
                enabled = canUndo && onUndo != null,
                expanded = expanded,
                onClick = { onUndo?.invoke() }
            )
            BlueprintRailAction(
                icon = Icons.Default.FitScreen,
                label = "Frame View",
                expanded = expanded,
                onClick = onFrame
            )
            onToggleFullscreen?.let { action ->
                BlueprintRailAction(
                    icon = Icons.Default.FitScreen,
                    label = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                    expanded = expanded,
                    onClick = action
                )
            }
            BlueprintRailAction(
                icon = Icons.Default.ChevronRight,
                label = "Section: ${section.label}",
                expanded = expanded,
                onClick = { onCycleSection?.invoke() },
                enabled = onCycleSection != null
            )
            if (!expanded) {
                if (section == BlueprintRailSection.DELIVER) {
                    val collapsedDeliverAction = onSharePdf
                        ?: onDownloadPdf
                        ?: onSharePng
                        ?: onDownloadPng
                    val collapsedDeliverIcon = when {
                        onSharePdf != null || onSharePng != null -> Icons.Default.Share
                        onDownloadPdf != null -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.ContentCopy
                    }
                    val collapsedDeliverLabel = when {
                        onSharePdf != null -> "Share PDF"
                        onDownloadPdf != null -> "Download PDF"
                        onSharePng != null -> "Share PNG"
                        else -> "Download PNG"
                    }
                    if (collapsedDeliverAction != null) {
                        BlueprintRailAction(
                            icon = collapsedDeliverIcon,
                            label = collapsedDeliverLabel,
                            expanded = false,
                            onClick = collapsedDeliverAction
                        )
                    }
                }
                return@Column
            }

            when (section) {
                BlueprintRailSection.CORE -> {
                    BlueprintRailAction(
                        icon = Icons.Default.GridOn,
                        label = "Layer: $activeLayerLabel",
                        expanded = true,
                        onClick = { onCycleLayer?.invoke() },
                        enabled = onCycleLayer != null
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.Straighten,
                        label = if (lockTopView) "Top Locked" else "Top Free",
                        selected = lockTopView,
                        expanded = true,
                        onClick = { onToggleTopView?.invoke() },
                        enabled = onToggleTopView != null
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.GridOn,
                        label = "Grid",
                        selected = showGrid,
                        expanded = true,
                        onClick = onToggleGrid
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.Straighten,
                        label = "Dimensions",
                        selected = showDimensions,
                        expanded = true,
                        onClick = onToggleDimensions
                    )
                    BlueprintRailAction(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        label = "Redo",
                        enabled = canRedo && onRedo != null,
                        expanded = true,
                        onClick = { onRedo?.invoke() }
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.AutoFixHigh,
                        label = "Auto Arrange",
                        expanded = true,
                        onClick = onAutoLayout
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.OpenWith,
                        label = "Reset Camera",
                        expanded = true,
                        onClick = onReset
                    )
                }

                BlueprintRailSection.DRAFT -> {
                    BlueprintRailAction(
                        icon = Icons.Default.Polyline,
                        label = if (wallChainMode) "Chain On" else "Chain Off",
                        selected = wallChainMode,
                        expanded = true,
                        onClick = { onToggleWallChain?.invoke() },
                        enabled = selectedTool == BlueprintCanvasTool.DRAW_WALL && onToggleWallChain != null
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.Architecture,
                        label = if (wallOrthoLock) "Ortho On" else "Ortho Off",
                        selected = wallOrthoLock,
                        expanded = true,
                        onClick = { onToggleWallOrtho?.invoke() },
                        enabled = selectedTool == BlueprintCanvasTool.DRAW_WALL && onToggleWallOrtho != null
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.Straighten,
                        label = "Angle $wallAngleSnapLabel",
                        expanded = true,
                        onClick = { onCycleWallAngleSnap?.invoke() },
                        enabled = selectedTool == BlueprintCanvasTool.DRAW_WALL && onCycleWallAngleSnap != null
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.GridOn,
                        label = "Anchor $wallAnchorSnapLabel",
                        expanded = true,
                        onClick = { onCycleWallAnchorSnap?.invoke() },
                        enabled = selectedTool == BlueprintCanvasTool.DRAW_WALL && onCycleWallAnchorSnap != null
                    )
                    if (selectedTool == BlueprintCanvasTool.DRAW_WALL) {
                        onCancelWallDraw?.let { action ->
                            BlueprintRailAction(
                                icon = Icons.Default.Delete,
                                label = "Cancel Draw",
                                expanded = true,
                                onClick = action
                            )
                        }
                    }
                    onQuickRoom?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Polyline,
                            label = "Quick Room",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onQuickAddWall?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Architecture,
                            label = "Quick Wall",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onQuickAddSlab?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.FitScreen,
                            label = "Quick Slab",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onQuickAddBed?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.GridOn,
                            label = "Quick Bed",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onCenterLayout?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.CenterFocusStrong,
                            label = "Center Layout",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onAlignNorth?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Straighten,
                            label = "Align North",
                            expanded = true,
                            onClick = action
                        )
                    }
                }

                BlueprintRailSection.REVIEW -> {
                    BlueprintRailAction(
                        icon = Icons.Default.GridOn,
                        label = "Layer: $activeLayerLabel",
                        expanded = true,
                        onClick = { onCycleLayer?.invoke() },
                        enabled = onCycleLayer != null
                    )
                    BlueprintRailAction(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        label = "Undo",
                        enabled = canUndo && onUndo != null,
                        expanded = true,
                        onClick = { onUndo?.invoke() }
                    )
                    BlueprintRailAction(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        label = "Redo",
                        enabled = canRedo && onRedo != null,
                        expanded = true,
                        onClick = { onRedo?.invoke() }
                    )
                    BlueprintRailAction(
                        icon = Icons.Default.AutoFixHigh,
                        label = "Auto Arrange",
                        expanded = true,
                        onClick = onAutoLayout
                    )
                    onOptimize?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.AutoFixHigh,
                            label = "Optimize Layout",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onCenterLayout?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.CenterFocusStrong,
                            label = "Center Layout",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onAlignNorth?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Straighten,
                            label = "Align North",
                            expanded = true,
                            onClick = action
                        )
                    }
                    BlueprintRailAction(
                        icon = Icons.Default.OpenWith,
                        label = "Reset Camera",
                        expanded = true,
                        onClick = onReset
                    )
                }

                BlueprintRailSection.DELIVER -> {
                    onDownloadPng?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.ContentCopy,
                            label = "Download PNG",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onSharePng?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Share,
                            label = "Share PNG",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onDownloadPdf?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.PictureAsPdf,
                            label = "Download PDF",
                            expanded = true,
                            onClick = action
                        )
                    }
                    onSharePdf?.let { action ->
                        BlueprintRailAction(
                            icon = Icons.Default.Share,
                            label = "Share PDF",
                            expanded = true,
                            onClick = action
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun BlueprintRailAction(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (expanded) 10.dp else 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        if (expanded) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun BlueprintLegendPill(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
                .border(width = 1.dp, color = Color(0xCCF5FAFF), shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ViewPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun TransformSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    suffix: String = "ft"
) {
    Column {
        Text(
            text = "$label: ${"%.1f".format(value)} $suffix",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

private data class CameraState(
    val pitchDeg: Float,
    val yawDeg: Float,
    val zoom: Float,
    val panX: Float,
    val panY: Float
)

private data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
)

private data class ProjectedPoint(
    val screen: Offset,
    val depth: Float
)

private data class Face(
    val points: List<Offset>,
    val depth: Float,
    val color: Color
)

private fun DrawScope.drawScene(
    spaces: List<Space>,
    selectedSpaceId: String?,
    selectedSpaceIds: Set<String>,
    cameraState: CameraState,
    showGrid: Boolean,
    gridSpacingFeet: Float,
    blueprintMode: Boolean,
    showDimensions: Boolean
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                if (blueprintMode) Color(0xFF0F1D3A) else Color(0xFFFAFCFF),
                if (blueprintMode) Color(0xFF1A2B52) else Color(0xFFF0F4F8)
            )
        )
    )
    if (spaces.isEmpty()) {
        return
    }

    val overlappingIds = if (blueprintMode) {
        overlappingSpaceIds(spaces)
    } else {
        emptySet()
    }

    val center = sceneCenter(spaces)
    if (showGrid) {
        drawGrid(center, cameraState, gridSpacingFeet.coerceAtLeast(1f))
    }

    val solidModels = spaces.map { space ->
        val dims = dimensions(space.geometry)
        val modelCenterY = space.transform.yFeet.toFloat() + (dims.height / 2f)
        val projectedCenter = projectPoint(
            point = Vec3(
                x = space.transform.xFeet.toFloat(),
                y = modelCenterY,
                z = space.transform.zFeet.toFloat()
            ),
            sceneCenter = center,
            cameraState = cameraState,
            viewport = size
        )
        SolidModel(
            space = space,
            dims = dims,
            depth = projectedCenter?.depth ?: -9999f
        )
    }.sortedBy { it.depth }

    solidModels.forEach { model ->
        drawSpaceModel(
            model = model,
            sceneCenter = center,
            cameraState = cameraState,
            isSelected = model.space.id == selectedSpaceId || model.space.id in selectedSpaceIds,
            isOverlapping = model.space.id in overlappingIds,
            blueprintMode = blueprintMode,
            showDimensions = showDimensions
        )
    }
}

private fun DrawScope.drawGrid(
    sceneCenter: Vec3,
    cameraState: CameraState,
    spacingFeet: Float
) {
    val span = 14
    val spacing = spacingFeet
    for (i in -span..span) {
        val major = i % 4 == 0
        val color = if (major) Color(0xFFC8D2DE) else Color(0xFFDCE3EB)
        val thickness = if (major) 1.35f else 0.8f

        val a = projectPoint(
            point = Vec3(
                sceneCenter.x + (i * spacing),
                0f,
                sceneCenter.z - (span * spacing)
            ),
            sceneCenter = sceneCenter,
            cameraState = cameraState,
            viewport = size
        )?.screen
        val b = projectPoint(
            point = Vec3(
                sceneCenter.x + (i * spacing),
                0f,
                sceneCenter.z + (span * spacing)
            ),
            sceneCenter = sceneCenter,
            cameraState = cameraState,
            viewport = size
        )?.screen
        if (a != null && b != null) {
            drawLine(
                color = color,
                start = a,
                end = b,
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }
    }

    for (j in -span..span) {
        val major = j % 4 == 0
        val color = if (major) Color(0xFFC8D2DE) else Color(0xFFDCE3EB)
        val thickness = if (major) 1.35f else 0.8f

        val a = projectPoint(
            point = Vec3(
                sceneCenter.x - (span * spacing),
                0f,
                sceneCenter.z + (j * spacing)
            ),
            sceneCenter = sceneCenter,
            cameraState = cameraState,
            viewport = size
        )?.screen
        val b = projectPoint(
            point = Vec3(
                sceneCenter.x + (span * spacing),
                0f,
                sceneCenter.z + (j * spacing)
            ),
            sceneCenter = sceneCenter,
            cameraState = cameraState,
            viewport = size
        )?.screen
        if (a != null && b != null) {
            drawLine(
                color = color,
                start = a,
                end = b,
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }
    }
}

private data class SolidModel(
    val space: Space,
    val dims: ModelDimensions,
    val depth: Float
)

private data class ModelDimensions(
    val width: Float,
    val depth: Float,
    val height: Float
)

private fun DrawScope.drawSpaceModel(
    model: SolidModel,
    sceneCenter: Vec3,
    cameraState: CameraState,
    isSelected: Boolean,
    isOverlapping: Boolean,
    blueprintMode: Boolean,
    showDimensions: Boolean
) {
    val transform = model.space.transform
    val yaw = transform.yawDegrees.toFloat()
    val w = model.dims.width / 2f
    val d = model.dims.depth / 2f
    val h = model.dims.height
    val yBase = transform.yFeet.toFloat()

    fun corner(localX: Float, localY: Float, localZ: Float): Vec3 {
        val (rx, rz) = rotateLocal(localX, localZ, yaw)
        return Vec3(
            x = rx + transform.xFeet.toFloat(),
            y = localY + yBase,
            z = rz + transform.zFeet.toFloat()
        )
    }

    val corners3d = listOf(
        corner(-w, 0f, -d),
        corner(w, 0f, -d),
        corner(w, 0f, d),
        corner(-w, 0f, d),
        corner(-w, h, -d),
        corner(w, h, -d),
        corner(w, h, d),
        corner(-w, h, d)
    )

    val projected = corners3d.map {
        projectPoint(it, sceneCenter, cameraState, size)
    }
    if (projected.any { it == null }) return

    val points = projected.map { it!!.screen }
    if (blueprintMode) {
        val style = blueprintRenderStyleForSpace(
            space = model.space,
            isSelected = isSelected,
            isOverlapping = isOverlapping
        )
        val topPath = Path().apply {
            moveTo(points[4].x, points[4].y)
            lineTo(points[5].x, points[5].y)
            lineTo(points[6].x, points[6].y)
            lineTo(points[7].x, points[7].y)
            close()
        }
        drawPath(
            path = topPath,
            color = style.fillColor
        )
        if (style.stripeColor != null) {
            val stripeCount = 9
            repeat(stripeCount) { index ->
                val t = index / (stripeCount - 1).toFloat()
                val stripeStart = lerpOffset(points[4], points[7], t)
                val stripeEnd = lerpOffset(points[5], points[6], t)
                drawLine(
                    color = style.stripeColor,
                    start = stripeStart,
                    end = stripeEnd,
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
            }
        }
        drawPath(
            path = topPath,
            color = style.strokeColor,
            style = Stroke(width = if (isSelected) 3.4f else 2.1f)
        )
        val labelCenter = Offset(
            x = (points[4].x + points[5].x + points[6].x + points[7].x) / 4f,
            y = (points[4].y + points[5].y + points[6].y + points[7].y) / 4f
        )
        drawBlueprintSpaceLabel(
            title = model.space.name,
            subtitle = if (showDimensions) blueprintDimensionsLabel(model) else null,
            center = labelCenter,
            emphasized = isSelected || isOverlapping,
            labelColor = style.labelColor
        )
        if (model.space.geometry is Geometry.Wall && model.space.openings.isNotEmpty()) {
            val wallStart = midpoint(points[4], points[7])
            val wallEnd = midpoint(points[5], points[6])
            drawWallOpeningMarkers(
                openings = model.space.openings,
                lineStart = wallStart,
                lineEnd = wallEnd,
                emphasized = isSelected || isOverlapping
            )
        }
        return
    }

    val color = colorFromHex(transform.colorHex)
    val top = shade(color, 1.08f).copy(alpha = 0.95f)
    val left = shade(color, 0.92f).copy(alpha = 0.9f)
    val right = shade(color, 0.77f).copy(alpha = 0.9f)
    val front = shade(color, 0.85f).copy(alpha = 0.84f)
    val back = shade(color, 0.68f).copy(alpha = 0.84f)

    val faces = listOf(
        Face(listOf(points[4], points[5], points[6], points[7]), depthOf(projected, 4, 5, 6, 7), top),
        Face(listOf(points[0], points[3], points[7], points[4]), depthOf(projected, 0, 3, 7, 4), left),
        Face(listOf(points[1], points[2], points[6], points[5]), depthOf(projected, 1, 2, 6, 5), right),
        Face(listOf(points[3], points[2], points[6], points[7]), depthOf(projected, 3, 2, 6, 7), front),
        Face(listOf(points[0], points[1], points[5], points[4]), depthOf(projected, 0, 1, 5, 4), back)
    ).sortedBy { it.depth }

    faces.forEach { face ->
        val path = Path().apply {
            moveTo(face.points.first().x, face.points.first().y)
            for (i in 1 until face.points.size) {
                lineTo(face.points[i].x, face.points[i].y)
            }
            close()
        }
        drawPath(path = path, color = face.color)
        drawPath(
            path = path,
            color = Color(0xFF2F3B47).copy(alpha = 0.22f),
            style = Stroke(width = 1.1f)
        )
    }

    if (isSelected) {
        val outlinePath = Path().apply {
            moveTo(points[4].x, points[4].y)
            lineTo(points[5].x, points[5].y)
            lineTo(points[6].x, points[6].y)
            lineTo(points[7].x, points[7].y)
            close()
        }
        drawPath(
            path = outlinePath,
            color = Color(0xFF1E88E5),
            style = Stroke(width = 3f)
        )
    }
}

private fun depthOf(projected: List<ProjectedPoint?>, vararg idx: Int): Float {
    var total = 0f
    idx.forEach { total += projected[it]?.depth ?: 0f }
    return total / idx.size
}

private fun DrawScope.drawBlueprintLabel(
    text: String,
    center: Offset,
    emphasized: Boolean
) {
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(
            if (emphasized) 240 else 210,
            232,
            240,
            255
        )
        textSize = if (emphasized) 28f else 24f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    drawContext.canvas.nativeCanvas.drawText(
        text,
        center.x,
        center.y,
        textPaint
    )
}

private data class BlueprintRenderStyle(
    val fillColor: Color,
    val strokeColor: Color,
    val labelColor: Color,
    val stripeColor: Color? = null
)

private fun blueprintRenderStyleForSpace(
    space: Space,
    isSelected: Boolean,
    isOverlapping: Boolean
): BlueprintRenderStyle {
    if (isOverlapping) {
        return BlueprintRenderStyle(
            fillColor = OVERLAP_BLUEPRINT_COLOR.copy(alpha = if (isSelected) 0.34f else 0.28f),
            strokeColor = Color(0xFFFFD2C7),
            labelColor = Color(0xFFFFF1ED)
        )
    }
    val base = when {
        space.geometry is Geometry.Wall -> WALL_BLUEPRINT_COLOR
        space.geometry is Geometry.Slab -> SLAB_BLUEPRINT_COLOR
        isLandscapeSpace(space) -> BED_BLUEPRINT_COLOR
        else -> ROOM_BLUEPRINT_COLOR
    }
    return BlueprintRenderStyle(
        fillColor = base.copy(alpha = if (isSelected) 0.36f else 0.24f),
        strokeColor = if (isSelected) Color(0xFFE4F4FF) else shade(base, 1.25f).copy(alpha = 0.95f),
        labelColor = Color(0xFFEAF3FF),
        stripeColor = if (isDrywallSpace(space) && isPaintedSpace(space)) {
            DRYWALL_PAINT_STRIPE_COLOR.copy(alpha = 0.65f)
        } else {
            null
        }
    )
}

private fun blueprintDimensionsLabel(model: SolidModel): String {
    return when (model.space.geometry) {
        is Geometry.Wall -> {
            val doors = model.space.openings
                .filter { classifyOpening(it) == BlueprintOpeningMarker.DOOR }
                .sumOf { it.count.coerceAtLeast(0) }
            val windows = model.space.openings
                .filter { classifyOpening(it) == BlueprintOpeningMarker.WINDOW }
                .sumOf { it.count.coerceAtLeast(0) }
            buildString {
                append("${"%.1f".format(model.dims.width)}' wall")
                if (doors > 0 || windows > 0) {
                    append(" · ")
                    if (doors > 0) {
                        append("D$doors")
                    }
                    if (windows > 0) {
                        if (doors > 0) append(" ")
                        append("W$windows")
                    }
                }
            }
        }
        else -> "${"%.1f".format(model.dims.width)}' x ${"%.1f".format(model.dims.depth)}'"
    }
}

private fun lerpOffset(start: Offset, end: Offset, t: Float): Offset {
    val clamped = t.coerceIn(0f, 1f)
    return Offset(
        x = start.x + ((end.x - start.x) * clamped),
        y = start.y + ((end.y - start.y) * clamped)
    )
}

private fun DrawScope.drawBlueprintSpaceLabel(
    title: String,
    subtitle: String?,
    center: Offset,
    emphasized: Boolean,
    labelColor: Color
) {
    val titlePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(
            if (emphasized) 246 else 228,
            (labelColor.red * 255).toInt().coerceIn(0, 255),
            (labelColor.green * 255).toInt().coerceIn(0, 255),
            (labelColor.blue * 255).toInt().coerceIn(0, 255)
        )
        textSize = if (emphasized) 28f else 24f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    val subtitlePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(
            if (emphasized) 224 else 198,
            (labelColor.red * 255).toInt().coerceIn(0, 255),
            (labelColor.green * 255).toInt().coerceIn(0, 255),
            (labelColor.blue * 255).toInt().coerceIn(0, 255)
        )
        textSize = if (emphasized) 22f else 18f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
    }
    val maxTitle = title.take(36)
    val subtitleText = subtitle?.take(44)
    val titleY = if (subtitleText == null) center.y else center.y - 4f
    drawContext.canvas.nativeCanvas.drawText(
        maxTitle,
        center.x,
        titleY,
        titlePaint
    )
    if (!subtitleText.isNullOrBlank()) {
        drawContext.canvas.nativeCanvas.drawText(
            subtitleText,
            center.x,
            center.y + 20f,
            subtitlePaint
        )
    }
}

private fun DrawScope.drawWallOpeningMarkers(
    openings: List<com.tradesketch.estimator.domain.model.Opening>,
    lineStart: Offset,
    lineEnd: Offset,
    emphasized: Boolean
) {
    if (openings.isEmpty()) return
    val delta = lineEnd - lineStart
    val lineLength = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
    if (lineLength < 20f) return

    val unitX = delta.x / lineLength
    val unitY = delta.y / lineLength
    val normalX = -unitY
    val normalY = unitX

    var doorCount = 0
    var windowCount = 0
    val markers = mutableListOf<BlueprintOpeningMarker>()
    openings.forEach { opening ->
        val type = classifyOpening(opening)
        val count = opening.count.coerceAtLeast(0)
        if (type == BlueprintOpeningMarker.DOOR) {
            doorCount += count
        } else {
            windowCount += count
        }
        repeat(count.coerceAtMost(10 - markers.size).coerceAtLeast(0)) {
            markers += type
        }
    }
    if (doorCount + windowCount <= 0) return
    if (markers.isEmpty()) {
        markers += if (doorCount > 0) BlueprintOpeningMarker.DOOR else BlueprintOpeningMarker.WINDOW
    }

    val markerSpan = (lineLength * 0.72f).coerceIn(12f, lineLength - 8f)
    val firstOffset = (lineLength - markerSpan) / 2f
    val markerGap = if (markers.size <= 1) 0f else markerSpan / (markers.size - 1).toFloat()
    markers.forEachIndexed { index, marker ->
        val along = if (markers.size == 1) {
            lineLength / 2f
        } else {
            firstOffset + (markerGap * index)
        }
        val center = Offset(
            x = lineStart.x + (unitX * along),
            y = lineStart.y + (unitY * along)
        )
        val color = when (marker) {
            BlueprintOpeningMarker.DOOR -> if (emphasized) Color(0xFFFFCC80) else Color(0xFFFFE0B2)
            BlueprintOpeningMarker.WINDOW -> if (emphasized) Color(0xFF81D4FA) else Color(0xFFB3E5FC)
        }
        val stroke = if (marker == BlueprintOpeningMarker.DOOR) 2.8f else 2.2f
        val halfAlong = if (marker == BlueprintOpeningMarker.DOOR) 8f else 6.5f
        drawLine(
            color = color,
            start = Offset(
                x = center.x - (unitX * halfAlong),
                y = center.y - (unitY * halfAlong)
            ),
            end = Offset(
                x = center.x + (unitX * halfAlong),
                y = center.y + (unitY * halfAlong)
            ),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        if (marker == BlueprintOpeningMarker.DOOR) {
            val spread = 3.2f
            drawLine(
                color = color,
                start = Offset(
                    x = center.x - (unitX * halfAlong) + (normalX * spread),
                    y = center.y - (unitY * halfAlong) + (normalY * spread)
                ),
                end = Offset(
                    x = center.x + (unitX * halfAlong) + (normalX * spread),
                    y = center.y + (unitY * halfAlong) + (normalY * spread)
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        } else {
            val wing = 4.2f
            drawLine(
                color = color,
                start = Offset(
                    x = center.x - (normalX * wing),
                    y = center.y - (normalY * wing)
                ),
                end = Offset(
                    x = center.x + (normalX * wing),
                    y = center.y + (normalY * wing)
                ),
                strokeWidth = stroke * 0.75f,
                cap = StrokeCap.Round
            )
        }
    }

    val hiddenCount = (doorCount + windowCount) - markers.size
    val label = buildString {
        if (doorCount > 0) append("D$doorCount")
        if (windowCount > 0) {
            if (isNotBlank()) append(" ")
            append("W$windowCount")
        }
        if (hiddenCount > 0) {
            append(" +$hiddenCount")
        }
    }
    if (label.isNotBlank()) {
        val labelCenter = Offset(
            x = ((lineStart.x + lineEnd.x) / 2f) + (normalX * 14f),
            y = ((lineStart.y + lineEnd.y) / 2f) + (normalY * 14f)
        )
        drawBlueprintLabel(
            text = label,
            center = labelCenter,
            emphasized = emphasized
        )
    }
}

private enum class BlueprintOpeningMarker {
    DOOR,
    WINDOW
}

private fun classifyOpening(opening: com.tradesketch.estimator.domain.model.Opening): BlueprintOpeningMarker {
    return if (opening.height.toFeet() >= 6.0) {
        BlueprintOpeningMarker.DOOR
    } else {
        BlueprintOpeningMarker.WINDOW
    }
}

private fun midpoint(a: Offset, b: Offset): Offset {
    return Offset(
        x = (a.x + b.x) / 2f,
        y = (a.y + b.y) / 2f
    )
}

private fun sceneCenter(spaces: List<Space>): Vec3 {
    if (spaces.isEmpty()) return Vec3(0f, 0f, 0f)
    val x = spaces.map { it.transform.xFeet.toFloat() }.average().toFloat()
    val y = spaces.map { it.transform.yFeet.toFloat() }.average().toFloat()
    val z = spaces.map { it.transform.zFeet.toFloat() }.average().toFloat()
    return Vec3(x = x, y = y, z = z)
}

private fun projectPoint(
    point: Vec3,
    sceneCenter: Vec3,
    cameraState: CameraState,
    viewport: Size
): ProjectedPoint? {
    var x = point.x - sceneCenter.x
    var y = point.y - sceneCenter.y
    var z = point.z - sceneCenter.z

    val yaw = Math.toRadians(cameraState.yawDeg.toDouble()).toFloat()
    val pitch = Math.toRadians(cameraState.pitchDeg.toDouble()).toFloat()

    val yawCos = cos(yaw)
    val yawSin = sin(yaw)
    val x1 = x * yawCos - z * yawSin
    val z1 = x * yawSin + z * yawCos

    val pitchCos = cos(pitch)
    val pitchSin = sin(pitch)
    val y1 = y * pitchCos - z1 * pitchSin
    val z2 = y * pitchSin + z1 * pitchCos

    val cameraDistance = 170f
    val depthSafe = cameraDistance - z2
    if (depthSafe < 10f) return null

    val perspective = (cameraDistance / depthSafe) * cameraState.zoom
    val scale = 11f
    val sx = viewport.width * 0.5f + (x1 * scale * perspective) + cameraState.panX
    val sy = viewport.height * 0.56f - (y1 * scale * perspective) + cameraState.panY

    return ProjectedPoint(
        screen = Offset(sx, sy),
        depth = z2
    )
}

private fun rotateLocal(x: Float, z: Float, yawDegrees: Float): Pair<Float, Float> {
    val yaw = Math.toRadians(yawDegrees.toDouble()).toFloat()
    val cos = cos(yaw)
    val sin = sin(yaw)
    val outX = x * cos - z * sin
    val outZ = x * sin + z * cos
    return outX to outZ
}

private fun dimensions(geometry: Geometry): ModelDimensions {
    return when (geometry) {
        is Geometry.Rect -> ModelDimensions(
            width = geometry.length.toFeet().toFloat(),
            depth = geometry.width.toFeet().toFloat(),
            height = 8f
        )
        is Geometry.Wall -> ModelDimensions(
            width = geometry.length.toFeet().toFloat(),
            depth = 0.75f,
            height = geometry.height.toFeet().toFloat()
        )
        is Geometry.Slab -> ModelDimensions(
            width = geometry.length.toFeet().toFloat(),
            depth = geometry.width.toFeet().toFloat(),
            height = max(geometry.thickness.toFeet().toFloat(), 0.25f)
        )
        is Geometry.Circle -> {
            val d = (geometry.radius.toFeet() * 2.0).toFloat()
            ModelDimensions(width = d, depth = d, height = 7f)
        }
        is Geometry.LShape -> {
            val width = max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet()).toFloat()
            val depth = max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet()).toFloat()
            ModelDimensions(width = width, depth = depth, height = 8f)
        }
    }
}

private val ROOM_BLUEPRINT_COLOR = Color(0xFF6FA7FF)
private val WALL_BLUEPRINT_COLOR = Color(0xFFE2A268)
private val SLAB_BLUEPRINT_COLOR = Color(0xFF7FC39A)
private val BED_BLUEPRINT_COLOR = Color(0xFFC7A171)
private val OVERLAP_BLUEPRINT_COLOR = Color(0xFFFF7A59)
private val DRYWALL_PAINT_STRIPE_COLOR = Color(0xFF365F83)

private fun isLandscapeSpace(space: Space): Boolean {
    val name = space.name.lowercase()
    return name.contains("gravel") ||
        name.contains("mulch") ||
        name.contains("landscape") ||
        name.contains("bed")
}

private fun isDrywallSpace(space: Space): Boolean {
    return space.geometry is Geometry.Wall || space.name.lowercase().contains("drywall")
}

private fun isPaintedSpace(space: Space): Boolean {
    val name = space.name.lowercase()
    return name.contains("paint") ||
        name.contains("primer") ||
        name.contains("coat")
}

private fun Space.withOpeningPreset(
    widthFeet: Double,
    heightFeet: Double
): Space {
    val width = Millimeters.fromFeet(widthFeet)
    val height = Millimeters.fromFeet(heightFeet)
    val existingIndex = openings.indexOfFirst { opening ->
        kotlin.math.abs(opening.width.toFeet() - widthFeet) <= 0.1 &&
            kotlin.math.abs(opening.height.toFeet() - heightFeet) <= 0.1
    }
    val updatedOpenings = openings.toMutableList()
    if (existingIndex >= 0) {
        val existing = updatedOpenings[existingIndex]
        updatedOpenings[existingIndex] = existing.copy(count = existing.count + 1)
    } else {
        updatedOpenings += Opening(
            width = width,
            height = height,
            count = 1
        )
    }
    return copy(openings = updatedOpenings)
}

private fun modelLabel(space: Space): String {
    val area = space.geometry.areaSqFt()
    val type = when (space.geometry) {
        is Geometry.Wall -> "Wall"
        is Geometry.Rect -> "Room"
        is Geometry.Slab -> "Slab"
        is Geometry.Circle -> "Circle"
        is Geometry.LShape -> "L-shape"
    }
    val openingCount = space.openings.sumOf { it.count.coerceAtLeast(0) }
    return if (space.geometry is Geometry.Wall && openingCount > 0) {
        "$type · ${"%.1f".format(area)} sq ft · $openingCount opening(s)"
    } else {
        "$type · ${"%.1f".format(area)} sq ft"
    }
}

private fun colorFromHex(hex: Long): Color = Color(hex)

private fun shade(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

private val modelColorPalette = listOf(
    0xFF4E79A7L,
    0xFFE15759L,
    0xFF76B7B2L,
    0xFFF28E2BL,
    0xFF59A14FL,
    0xFFEDC948L,
    0xFFAF7AA1L
)
