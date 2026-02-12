package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import com.tradesketch.estimator.domain.model.areaSqFt
import com.tradesketch.estimator.ui.components.rememberAppHaptics
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun ModelBuilder3DPanel(
    project: Project,
    onAddSpace: () -> Unit,
    onEditSpace: (Space) -> Unit,
    onDuplicateSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onAutoLayout: () -> Unit,
    onUpdateTransform: (String, SpaceTransform) -> Unit,
    immersiveMode: Boolean = false,
    blueprintMode: Boolean = false,
    workspaceSeed: Int = 0,
    preferredTopLock: Boolean? = null,
    preferredShowDimensions: Boolean? = null,
    onQuickRoom: (() -> Unit)? = null,
    onQuickAddWall: (() -> Unit)? = null,
    onQuickAddSlab: (() -> Unit)? = null,
    onOpenModel: (() -> Unit)? = null,
    onOpenTakeoff: (() -> Unit)? = null,
    onOpenExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
    var showHud by rememberSaveable(project.id) { mutableStateOf(true) }
    var snapYaw by rememberSaveable(project.id) { mutableStateOf(true) }
    var lockTopView by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultTopLock)
    }
    var showDimensions by rememberSaveable(project.id, blueprintMode, workspaceSeed) {
        mutableStateOf(defaultShowDimensions)
    }
    var showControlPanel by rememberSaveable(project.id, immersiveMode, workspaceSeed) {
        mutableStateOf(!immersiveMode)
    }
    val useMicroToolbar = blueprintMode && immersiveMode
    var dockExpanded by rememberSaveable(project.id, workspaceSeed) { mutableStateOf(false) }

    val selectedSpace = project.spaces.find { it.id == selectedSpaceId }
    var draftTransform by remember(selectedSpace?.id) {
        mutableStateOf(selectedSpace?.transform ?: SpaceTransform())
    }

    LaunchedEffect(project.spaces, selectedSpaceId) {
        if (selectedSpaceId != null && project.spaces.none { it.id == selectedSpaceId }) {
            selectedSpaceId = project.spaces.firstOrNull()?.id
        } else if (selectedSpaceId == null && project.spaces.isNotEmpty()) {
            selectedSpaceId = project.spaces.first().id
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

    val previewSpaces = remember(project.spaces, selectedSpaceId, draftTransform) {
        project.spaces.map { space ->
            if (space.id == selectedSpaceId) {
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

    val workspaceScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val allowPanelScroll = !immersiveMode || showControlPanel
    val canvasHeight = when {
        immersiveMode && !showControlPanel -> 700.dp
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
        cameraPitch = defaultCameraPitch
        cameraYaw = defaultCameraYaw
        cameraZoom = frameZoomForModel(project.spaces)
        panX = 0f
        panY = 0f
    }
    val runAutoLayout = {
        onAutoLayout()
        frameModel()
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
                            "Blueprint mode: top-view locked for precise layout, spacing, and dimensions."
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
                    text = "Objects: ${project.spaces.size} | Active: ${selectedSpace?.name ?: "None"}",
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            val canvasGestureModifier = if (showControlPanel) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceBright,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .then(canvasGestureModifier)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(canvasHeight)) {
                    drawScene(
                        spaces = previewSpaces,
                        selectedSpaceId = selectedSpaceId,
                        cameraState = cameraState,
                        showGrid = showGrid,
                        blueprintMode = blueprintMode,
                        showDimensions = showDimensions
                    )
                }
                if (blueprintMode) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(canvasHeight)) {
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
                if (useMicroToolbar) {
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
                                onOpenTakeoff?.let { action ->
                                    onOpenModel?.let { modelAction ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                haptics.tap()
                                                modelAction()
                                            },
                                            label = { Text("Model") }
                                        )
                                    }
                                    FilterChip(
                                        selected = false,
                                        enabled = project.spaces.isNotEmpty(),
                                        onClick = {
                                            haptics.tap()
                                            action()
                                        },
                                        label = { Text("Takeoff") }
                                    )
                                }
                                onOpenExport?.let { action ->
                                    FilterChip(
                                        selected = false,
                                        enabled = project.spaces.isNotEmpty(),
                                        onClick = {
                                            haptics.tap()
                                            action()
                                        },
                                        label = { Text("Export") }
                                    )
                                }
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
                            text = "Objects: ${project.spaces.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Active: ${selectedSpace?.name ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                if (showHud && selectedSpace != null) {
                    ActiveObjectHud(
                        space = selectedSpace,
                        transform = draftTransform,
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

        if (showControlPanel && activeControlTab == BuilderControlTab.OBJECTS) {
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
                    Text(
                        text = "${filteredSpaces.size} visible object(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                val isSelected = space.id == selectedSpaceId
                                val laneLabel = modelTradeLaneForSpace(space).label
                                Card(
                                    onClick = {
                                        haptics.tap()
                                        selectedSpaceId = space.id
                                        if (activeControlTab == BuilderControlTab.OBJECTS) {
                                            activeControlTab = BuilderControlTab.INSPECTOR
                                        }
                                    },
                                    colors = if (isSelected) {
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                                text = "${modelLabel(space)} · $laneLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            haptics.tap()
                                            onEditSpace(space)
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = {
                                            haptics.tap()
                                            onDuplicateSpace(space.id)
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                                        }
                                        IconButton(onClick = {
                                            haptics.confirm()
                                            onDeleteSpace(space.id)
                                        }) {
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

        if (showControlPanel && activeControlTab == BuilderControlTab.INSPECTOR && selectedSpace != null) {
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
                        Button(
                            onClick = {
                                haptics.confirm()
                                onUpdateTransform(selectedSpace.id, draftTransform)
                            },
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
        } else if (activeControlTab == BuilderControlTab.INSPECTOR) {
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

private fun snapDegrees(value: Double, increment: Double): Double {
    if (increment <= 0.0) return value
    return kotlin.math.round(value / increment) * increment
}

private enum class BuilderControlTab(val label: String) {
    SCENE("Scene"),
    OBJECTS("Objects"),
    INSPECTOR("Inspector")
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
    cameraState: CameraState,
    showGrid: Boolean,
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

    val center = sceneCenter(spaces)
    if (showGrid) {
        drawGrid(center, cameraState)
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
            isSelected = model.space.id == selectedSpaceId,
            blueprintMode = blueprintMode,
            showDimensions = showDimensions
        )
    }
}

private fun DrawScope.drawGrid(
    sceneCenter: Vec3,
    cameraState: CameraState
) {
    val span = 14
    val spacing = 8f
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
        val topPath = Path().apply {
            moveTo(points[4].x, points[4].y)
            lineTo(points[5].x, points[5].y)
            lineTo(points[6].x, points[6].y)
            lineTo(points[7].x, points[7].y)
            close()
        }
        drawPath(
            path = topPath,
            color = Color(0xFF5E8DFF).copy(alpha = 0.22f)
        )
        drawPath(
            path = topPath,
            color = if (isSelected) Color(0xFF90CAF9) else Color(0xFFC9DAFF),
            style = Stroke(width = if (isSelected) 3.4f else 1.9f)
        )
        if (showDimensions) {
            val labelCenter = Offset(
                x = (points[4].x + points[5].x + points[6].x + points[7].x) / 4f,
                y = (points[4].y + points[5].y + points[6].y + points[7].y) / 4f
            )
            drawBlueprintLabel(
                text = "${"%.1f".format(model.dims.width)}' x ${"%.1f".format(model.dims.depth)}'",
                center = labelCenter,
                emphasized = isSelected
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

private fun modelLabel(space: Space): String {
    val area = space.geometry.areaSqFt()
    val type = when (space.geometry) {
        is Geometry.Wall -> "Wall"
        is Geometry.Rect -> "Room"
        is Geometry.Slab -> "Slab"
        is Geometry.Circle -> "Circle"
        is Geometry.LShape -> "L-shape"
    }
    return "$type · ${"%.1f".format(area)} sq ft"
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
