package com.tradesketch.estimator.ui.blueprint

import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.ProjectTakeoffSession
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.TakeoffScope
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.ui.viewmodel.SettingsViewModel
import com.tradesketch.estimator.ui.viewmodel.scopedToTakeoffScope
import com.tradesketch.estimator.utils.DimensionParser
import java.util.UUID
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    initialShowAddons: Boolean = false,
    initialShowParams: Boolean = false,
    leftEdgeDialInset: Dp = 0.dp,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    tutorialMode: Boolean = false,
    onExitTutorialMode: () -> Unit = {},
    viewModel: BlueprintEditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val appSettings = settingsUiState.settings
    val configuration = LocalConfiguration.current
    val compactHeightWindow = configuration.screenHeightDp < 700

    var tool by remember { mutableStateOf(BlueprintDraftTool.DRAW_WALL) }
    var drawingStart by remember { mutableStateOf<PointMm?>(null) }
    var drawingPreview by remember { mutableStateOf<PointMm?>(null) }
    var boxStart by remember { mutableStateOf<PointMm?>(null) }
    var boxPreview by remember { mutableStateOf<PointMm?>(null) }
    var boxRotationRadians by remember { mutableStateOf(0.0) }
    var curveStart by remember { mutableStateOf<PointMm?>(null) }
    var curveEnd by remember { mutableStateOf<PointMm?>(null) }
    var curvePreviewPoint by remember { mutableStateOf<PointMm?>(null) }
    var circleCenter by remember { mutableStateOf<PointMm?>(null) }
    var circlePreviewEdge by remember { mutableStateOf<PointMm?>(null) }
    var chainOrigin by remember { mutableStateOf<PointMm?>(null) }
    var detachedWalls by remember { mutableStateOf(false) }
    var movingWallPreview by remember { mutableStateOf<WallSegment?>(null) }
    var pendingGrabSelection by rememberSaveable(projectId) { mutableStateOf(false) }
    var restartLineFromNearestWallStart by remember { mutableStateOf(false) }
    var snapSettings by remember {
        mutableStateOf(
            BlueprintSnapSettings(
                gridEnabled = appSettings.blueprintSnapGridEnabled,
                endpointEnabled = appSettings.blueprintSnapEndpointEnabled,
                midpointEnabled = appSettings.blueprintSnapMidpointEnabled,
                angleEnabled = appSettings.blueprintSnapAngleEnabled,
                closureEnabled = appSettings.blueprintSnapClosureEnabled,
                thresholdFeet = appSettings.blueprintSnapThresholdFeet.coerceIn(
                    MIN_SNAP_THRESHOLD_FEET,
                    MAX_SNAP_THRESHOLD_FEET
                )
            )
        )
    }
    var scale by remember { mutableFloatStateOf(0.82f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var activeOpeningPanel by rememberSaveable(projectId) {
        mutableStateOf(if (initialShowAddons) OpeningPanelType.DOORS else null)
    }
    var showParams by rememberSaveable(projectId) { mutableStateOf(initialShowParams) }
    var selectedDoorPreset by remember { mutableStateOf(doorPreset) }
    var selectedWindowPreset by remember { mutableStateOf(windowPreset) }
    var selectedStairUpPreset by remember { mutableStateOf(stairUpPreset) }
    var selectedStairDownPreset by remember { mutableStateOf(stairDownPreset) }
    var doorWidthFeet by remember { mutableStateOf("3.0") }
    var doorHeightFeet by remember { mutableStateOf("7.0") }
    var doorSillFeet by remember { mutableStateOf("0.0") }
    var windowWidthFeet by remember { mutableStateOf("4.0") }
    var windowHeightFeet by remember { mutableStateOf("4.0") }
    var windowSillFeet by remember { mutableStateOf("3.0") }
    var stairUpWidthFeet by remember { mutableStateOf("3.5") }
    var stairUpHeightFeet by remember { mutableStateOf("10.0") }
    var stairUpSillFeet by remember { mutableStateOf("0.0") }
    var stairDownWidthFeet by remember { mutableStateOf("3.5") }
    var stairDownHeightFeet by remember { mutableStateOf("10.0") }
    var stairDownSillFeet by remember { mutableStateOf("0.0") }
    var showDoorPresets by rememberSaveable(projectId) { mutableStateOf(false) }
    var showWindowPresets by rememberSaveable(projectId) { mutableStateOf(false) }
    var showStairUpPresets by rememberSaveable(projectId) { mutableStateOf(false) }
    var showStairDownPresets by rememberSaveable(projectId) { mutableStateOf(false) }
    var showRailHelp by rememberSaveable(projectId) { mutableStateOf(false) }
    var showGridScaleEditor by rememberSaveable(projectId) { mutableStateOf(false) }
    var showClearAllConfirm by rememberSaveable(projectId) { mutableStateOf(false) }
    var openingPointerWorld by remember { mutableStateOf<PointMm?>(null) }
    var canvasRoot by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var bottomRailBounds by remember { mutableStateOf<Rect?>(null) }
    var gridScaleBadgeBounds by remember { mutableStateOf<Rect?>(null) }
    var paramsPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var openingPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var selectionPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var floorSwitcherBounds by remember { mutableStateOf<Rect?>(null) }
    var clearAllButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var topStartStackBounds by remember { mutableStateOf<Rect?>(null) }
    var topEndStackBounds by remember { mutableStateOf<Rect?>(null) }
    var gridScaleEditorBounds by remember { mutableStateOf<Rect?>(null) }
    var railHelpBounds by remember { mutableStateOf<Rect?>(null) }
    var wallRotateButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var tutorialLeftControlsBounds by remember { mutableStateOf<Rect?>(null) }
    var tutorialCenterControlsBounds by remember { mutableStateOf<Rect?>(null) }
    var tutorialRightControlsBounds by remember { mutableStateOf<Rect?>(null) }
    var dualJoysticksEnabled by rememberSaveable(projectId) {
        mutableStateOf(appSettings.blueprintDualJoysticksEnabled)
    }
    val currentControlMode = blueprintControlMode(dualJoysticksEnabled)
    var lastTrackedOpenMode by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var cursorVisible by rememberSaveable(projectId) {
        mutableStateOf(appSettings.blueprintCursorVisible)
    }
    var cursorScale by rememberSaveable(projectId) {
        mutableFloatStateOf(appSettings.blueprintCursorScale.coerceIn(0.75f, 2.1f))
    }
    var joystickSensitivity by rememberSaveable(projectId) {
        mutableFloatStateOf(appSettings.blueprintJoystickSensitivity.coerceIn(0.55f, 2.2f))
    }
    var joystickDeadzone by rememberSaveable(projectId) {
        mutableFloatStateOf(appSettings.blueprintJoystickDeadzone.coerceIn(0.08f, 0.30f))
    }
    var rightJoystickVector by remember { mutableStateOf(Offset.Zero) }
    var leftJoystickVector by remember { mutableStateOf(Offset.Zero) }
    var rightJoystickPressed by remember { mutableStateOf(false) }
    var joystickCursorLocal by remember { mutableStateOf<Offset?>(null) }
    var edgeDialActiveTouchCount by remember { mutableIntStateOf(0) }
    var edgeDialInteracting by remember { mutableStateOf(false) }
    var edgeDialAngleRadians by remember { mutableStateOf<Double?>(null) }
    var edgeDialLengthMm by remember { mutableStateOf<Double?>(null) }
    var gridScaleInput by rememberSaveable(projectId) { mutableStateOf("1'") }
    var gridScaleFeetInput by rememberSaveable(projectId) { mutableStateOf("1") }
    var gridScaleInchesInput by rememberSaveable(projectId) { mutableStateOf("0") }
    var gridScaleCentimetersInput by rememberSaveable(projectId) { mutableStateOf("30") }
    var selectedFloor by rememberSaveable(projectId) { mutableStateOf(FLOOR_GROUND_LEVEL) }
    var hasTrackedFloorSelection by rememberSaveable(projectId) { mutableStateOf(false) }
    var tutorialStepIndex by rememberSaveable(projectId, tutorialMode, dualJoysticksEnabled) {
        mutableIntStateOf(0)
    }
    val resetCurveDraft = {
        curveStart = null
        curveEnd = null
        curvePreviewPoint = null
    }
    val resetCircleDraft = {
        circleCenter = null
        circlePreviewEdge = null
    }
    val rootView = LocalView.current
    val recordBlueprintMetric: (BlueprintMetricAction) -> Unit = { action ->
        settingsViewModel.recordTap(blueprintMetricKey(action, currentControlMode))
    }
    val controlTutorialSteps = remember(dualJoysticksEnabled) {
        blueprintControlTutorialSteps(dualJoysticksEnabled)
    }
    val currentTutorialStep = controlTutorialSteps.getOrNull(tutorialStepIndex)

    LaunchedEffect(projectId) { viewModel.setProjectId(projectId) }
    LaunchedEffect(projectId, currentControlMode) {
        blueprintScreenOpenedMetricKey(lastTrackedOpenMode, currentControlMode)?.let { metricKey ->
            settingsViewModel.recordTap(metricKey)
            lastTrackedOpenMode = currentControlMode.metricSuffix
        }
    }
    LaunchedEffect(projectId, initialShowAddons, initialShowParams) {
        activeOpeningPanel = if (initialShowAddons) OpeningPanelType.DOORS else null
        showParams = initialShowParams
        showRailHelp = false
        showGridScaleEditor = false
        showClearAllConfirm = false
        pendingGrabSelection = false
        boxStart = null
        boxPreview = null
        boxRotationRadians = 0.0
    }
    LaunchedEffect(
        appSettings.blueprintSnapGridEnabled,
        appSettings.blueprintSnapEndpointEnabled,
        appSettings.blueprintSnapMidpointEnabled,
        appSettings.blueprintSnapAngleEnabled,
        appSettings.blueprintSnapClosureEnabled,
        appSettings.blueprintSnapThresholdFeet
    ) {
        snapSettings = snapSettings.copy(
            gridEnabled = appSettings.blueprintSnapGridEnabled,
            endpointEnabled = appSettings.blueprintSnapEndpointEnabled,
            midpointEnabled = appSettings.blueprintSnapMidpointEnabled,
            angleEnabled = appSettings.blueprintSnapAngleEnabled,
            closureEnabled = appSettings.blueprintSnapClosureEnabled,
            thresholdFeet = appSettings.blueprintSnapThresholdFeet.coerceIn(
                MIN_SNAP_THRESHOLD_FEET,
                MAX_SNAP_THRESHOLD_FEET
            )
        )
    }
    LaunchedEffect(appSettings.blueprintDualJoysticksEnabled) {
        dualJoysticksEnabled = appSettings.blueprintDualJoysticksEnabled
    }
    LaunchedEffect(tutorialMode, dualJoysticksEnabled) {
        if (tutorialMode) {
            tutorialStepIndex = 0
            showRailHelp = false
            showParams = false
            showGridScaleEditor = false
            showClearAllConfirm = false
            activeOpeningPanel = null
            pendingGrabSelection = false
            movingWallPreview = null
            drawingStart = null
            drawingPreview = null
            boxStart = null
            boxPreview = null
            resetCurveDraft()
            resetCircleDraft()
            tool = BlueprintDraftTool.DRAW_WALL
        }
    }
    LaunchedEffect(appSettings.blueprintCursorVisible) {
        cursorVisible = appSettings.blueprintCursorVisible
    }
    LaunchedEffect(appSettings.blueprintCursorScale) {
        cursorScale = appSettings.blueprintCursorScale.coerceIn(0.75f, 2.1f)
    }
    LaunchedEffect(appSettings.blueprintJoystickSensitivity) {
        joystickSensitivity = appSettings.blueprintJoystickSensitivity.coerceIn(0.55f, 2.2f)
    }
    LaunchedEffect(appSettings.blueprintJoystickDeadzone) {
        joystickDeadzone = appSettings.blueprintJoystickDeadzone.coerceIn(0.08f, 0.30f)
    }
    LaunchedEffect(selectedFloor) {
        drawingStart = null
        drawingPreview = null
        boxStart = null
        boxPreview = null
        boxRotationRadians = 0.0
        resetCurveDraft()
        resetCircleDraft()
        chainOrigin = null
        movingWallPreview = null
        pendingGrabSelection = false
        restartLineFromNearestWallStart = false
        viewModel.selectWall(null)
        if (!hasTrackedFloorSelection) {
            hasTrackedFloorSelection = true
        }
    }
    LaunchedEffect(tool) {
        if (tool != BlueprintDraftTool.DRAW_BOX) {
            boxStart = null
            boxPreview = null
            boxRotationRadians = 0.0
        }
        if (tool != BlueprintDraftTool.DRAW_ARC) {
            resetCurveDraft()
        }
        if (tool != BlueprintDraftTool.DRAW_CIRCLE) {
            resetCircleDraft()
        }
    }
    if (uiState.isLoading || uiState.document == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val doc = uiState.document ?: return
    val takeoffSession = uiState.project?.takeoffSession ?: ProjectTakeoffSession()
    val currentScope = takeoffSession.selectedScope
    val scopedDoc = doc.scopedToTakeoffScope(currentScope)
    val overlayTopPadding = when {
        configuration.screenWidthDp < 420 -> 18.dp
        compactHeightWindow -> 10.dp
        else -> 12.dp
    }
    val topOverlaySpacing = if (compactHeightWindow) 4.dp else 6.dp
    val topStackMaxHeight = if (compactHeightWindow) 220.dp else 320.dp
    val compactHudTopStackWidth = when {
        configuration.screenWidthDp < 400 -> 170.dp
        configuration.screenWidthDp < 440 -> 182.dp
        compactHeightWindow -> 198.dp
        else -> 238.dp
    }
    val compactHudLiveWidth = when {
        configuration.screenWidthDp < 400 -> 168.dp
        configuration.screenWidthDp < 440 -> 178.dp
        else -> 194.dp
    }
    val stairWorkflowActive = scopedDoc.openings.any { it.type.isStair() } ||
        activeOpeningPanel == OpeningPanelType.STAIR_UP ||
        activeOpeningPanel == OpeningPanelType.STAIR_DOWN ||
        tool == BlueprintDraftTool.PLACE_STAIR_UP ||
        tool == BlueprintDraftTool.PLACE_STAIR_DOWN
    val movingDoc = movingWallPreview?.let { movingWall ->
        scopedDoc.copy(
            walls = scopedDoc.walls.map { wall ->
                if (wall.id == movingWall.id) movingWall else wall
            }
        )
    } ?: scopedDoc
    val wallsById = movingDoc.walls.associateBy { it.id }
    val renderedDoc = movingDoc.copy(
        walls = movingDoc.walls.filter { wall -> wall.isOnFloor(selectedFloor) },
        rooms = movingDoc.rooms.filter { room -> room.isOnFloor(selectedFloor) },
        openings = movingDoc.openings.filter { opening -> opening.isOnFloor(selectedFloor, wallsById) }
    )
    val selectedWall = uiState.selectedWallId?.let { id -> renderedDoc.walls.find { it.id == id } }
    val selectedOpening = uiState.selectedOpeningId?.let { id -> renderedDoc.openings.find { it.id == id } }
    val circleSelection = selectedWall?.let { wall ->
        circleSelectionInfo(
            document = doc,
            selectedWallId = wall.id
        )
    }
    val circleResizeStepMm = if (appSettings.useMetric) 150L else Millimeters.fromFeet(0.5).value
    LaunchedEffect(showParams) {
        if (!showParams) {
            paramsPanelBounds = null
        }
    }
    LaunchedEffect(activeOpeningPanel) {
        if (activeOpeningPanel == null) {
            openingPanelBounds = null
        }
    }
    LaunchedEffect(selectedWall?.id, selectedOpening?.id) {
        if (selectedWall == null && selectedOpening == null) {
            selectionPanelBounds = null
        }
    }
    LaunchedEffect(movingWallPreview?.id) {
        if (movingWallPreview == null) {
            wallRotateButtonBounds = null
        }
    }
    val menuHitBounds = buildList {
        bottomRailBounds?.let(::add)
        gridScaleBadgeBounds?.let(::add)
        clearAllButtonBounds?.let(::add)
        if (showParams) paramsPanelBounds?.let(::add)
        if (activeOpeningPanel != null) openingPanelBounds?.let(::add)
        if (selectedWall != null || selectedOpening != null) selectionPanelBounds?.let(::add)
        floorSwitcherBounds?.let(::add)
        if (showGridScaleEditor) gridScaleEditorBounds?.let(::add)
        if (showRailHelp) railHelpBounds?.let(::add)
        wallRotateButtonBounds?.let(::add)
    }
    val dispatchMenuTapAtCursor: (Offset) -> Boolean = { cursorLocal ->
        val rootPoint = canvasRoot + cursorLocal
        if (menuHitBounds.none { bounds -> bounds.contains(rootPoint) }) {
            false
        } else {
            performSyntheticTap(rootView, rootPoint)
        }
    }
    val wallAreaSqFt = BlueprintTakeoffCalculator.wallAreaByIdSqFt(scopedDoc).values.sum()
    val openingAreaSqFt = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(scopedDoc).values.sum()
    val squareFeet = (wallAreaSqFt - openingAreaSqFt).coerceAtLeast(0.0)
    val linearFeet = scopedDoc.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    val liveScopeQuantity = computeLiveScopeQuantity(
        document = scopedDoc,
        scope = currentScope,
        takeoffSession = takeoffSession
    )
    val resolvedPreset: (OpeningPreset) -> OpeningPreset = { base ->
        val widthInput = when (base.type) {
            OpeningType.DOOR -> doorWidthFeet
            OpeningType.WINDOW -> windowWidthFeet
            OpeningType.STAIR_UP -> stairUpWidthFeet
            OpeningType.STAIR_DOWN -> stairDownWidthFeet
        }
        val heightInput = when (base.type) {
            OpeningType.DOOR -> doorHeightFeet
            OpeningType.WINDOW -> windowHeightFeet
            OpeningType.STAIR_UP -> stairUpHeightFeet
            OpeningType.STAIR_DOWN -> stairDownHeightFeet
        }
        val sillInput = when (base.type) {
            OpeningType.DOOR -> doorSillFeet
            OpeningType.WINDOW -> windowSillFeet
            OpeningType.STAIR_UP -> stairUpSillFeet
            OpeningType.STAIR_DOWN -> stairDownSillFeet
        }
        base.copy(
            widthMm = DimensionParser.parseLengthToMillimeters(widthInput)
                ?.coerceAtLeast(1L)
                ?: base.widthMm,
            heightMm = DimensionParser.parseLengthToMillimeters(heightInput)
                ?.coerceAtLeast(1L)
                ?: base.heightMm,
            sillMm = DimensionParser.parseLengthToMillimeters(sillInput)
                ?.coerceAtLeast(0L)
                ?: base.sillMm
        )
    }
    val placementThresholdMm = Millimeters.fromFeet(snapSettings.thresholdFeet * 2).value.coerceAtLeast(1L)
    val findPlacementCandidate: (PointMm, OpeningPreset) -> OpeningPlacementCandidate? = { worldPoint, preset ->
        renderedDoc.walls
            .map { wall ->
                val t = BlueprintSnapMath.projectToWallT(worldPoint, wall).coerceIn(0.0, 1.0)
                val distance = BlueprintSnapMath.pointToWallDistanceMm(worldPoint, wall)
                Triple(wall, t, distance)
            }
            .minByOrNull { it.third }
            ?.takeIf { it.third <= placementThresholdMm }
            ?.let { nearest ->
                val swingTag = if (preset.type == OpeningType.DOOR) {
                    if (pointSideOfWall(worldPoint, nearest.first) >= 0.0) DOOR_SWING_POS_TAG else DOOR_SWING_NEG_TAG
                } else {
                    null
                }
                OpeningPlacementCandidate(
                    wall = nearest.first,
                    t = nearest.second,
                    snappedCenter = BlueprintSnapMath.pointOnWall(nearest.first, nearest.second),
                    swingTag = swingTag
                )
            }
    }
    val placeOpeningAtWorld: (PointMm, OpeningPreset) -> Unit = { worldPoint, base ->
        val preset = resolvedPreset(base)
        val candidate = findPlacementCandidate(worldPoint, preset)
        if (candidate != null) {
            viewModel.addOpening(
                BlueprintOpening(
                    id = UUID.randomUUID().toString(),
                    wallId = candidate.wall.id,
                    t = candidate.t,
                    widthMm = preset.widthMm,
                    heightMm = preset.heightMm,
                    sillMm = preset.sillMm,
                    type = preset.type,
                    tags = listOfNotNull(candidate.swingTag, selectedFloor.floorTag()).toSet()
                ).normalized()
            )
            recordBlueprintMetric(BlueprintMetricAction.OPENING_PLACED)
        }
    }
    val placementPreviewPreset = when (tool) {
        BlueprintDraftTool.PLACE_DOOR -> selectedDoorPreset
        BlueprintDraftTool.PLACE_WINDOW -> selectedWindowPreset
        BlueprintDraftTool.PLACE_STAIR_UP -> selectedStairUpPreset
        BlueprintDraftTool.PLACE_STAIR_DOWN -> selectedStairDownPreset
        else -> null
    }
    val dragPreview = placementPreviewPreset
        ?.let { preset ->
            openingPointerWorld?.let { worldPoint ->
                val resolved = resolvedPreset(preset)
                OpeningDragPreview(
                    preset = resolved,
                    rawWorldPoint = worldPoint,
                    placement = findPlacementCandidate(worldPoint, resolved)
                )
            }
        }
    val wallSelectionThresholdMm = (22f / (BASE_PX_PER_MM * scale))
        .roundToLong()
        .coerceIn(
            Millimeters.fromFeet(0.55).value,
            Millimeters.fromFeet(1.8).value
        )
    val rightSelectBoostActive = dualJoysticksEnabled && rightJoystickPressed
    val rightSelectRadiusPx = 44f * if (rightSelectBoostActive) JOYSTICK_SELECT_BOOST_MULTIPLIER else 1f
    val rightTapWallSelectionThresholdMm = (rightSelectRadiusPx / (BASE_PX_PER_MM * scale))
        .roundToLong()
        .coerceIn(
            Millimeters.fromFeet(if (rightSelectBoostActive) 1.65 else 1.2).value,
            Millimeters.fromFeet(if (rightSelectBoostActive) 3.0 else 2.4).value
        )
    val movingWallAutoSnapThresholdMm = Millimeters.fromFeet(
        (snapSettings.thresholdFeet * MOVING_WALL_AUTO_SNAP_THRESHOLD_MULTIPLIER).coerceAtLeast(0.2)
    ).value.coerceAtLeast(MOVING_WALL_AUTO_SNAP_MIN_THRESHOLD_MM)
    val openingSelectionThresholdMm = (58f / (BASE_PX_PER_MM * scale))
        .roundToLong()
        .coerceAtLeast(Millimeters.fromFeet(2.1).value)
    val nearestWallAt: (PointMm, Long) -> WallSegment? = { sample, thresholdMm ->
        renderedDoc.walls
            .map { it to BlueprintSnapMath.pointToWallDistanceMm(sample, it) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= thresholdMm }
            ?.first
    }
    val nearestOpeningAt: (PointMm) -> BlueprintOpening? = { sample ->
        renderedDoc.openings
            .mapNotNull { opening ->
                val wall = renderedDoc.walls.find { it.id == opening.wallId } ?: return@mapNotNull null
                val openingCenter = BlueprintSnapMath.pointOnWall(wall, opening.t)
                val distance = BlueprintSnapMath.distanceMillimeters(sample, openingCenter)
                opening to distance
            }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= openingSelectionThresholdMm }
            ?.first
    }
    val commitMovingWallPlacement: () -> Boolean = {
        val previewWall = movingWallPreview
        if (previewWall == null) {
            false
        } else {
            val currentWall = doc.walls.find { it.id == previewWall.id }
            if (currentWall != null && currentWall != previewWall) {
                viewModel.updateWall(previewWall.id, previewWall)
            }
            movingWallPreview = null
            drawingStart = null
            drawingPreview = null
            chainOrigin = null
            restartLineFromNearestWallStart = false
            viewModel.selectWall(null)
            true
        }
    }
    val cancelMovingWallPlacement: () -> Boolean = {
        val previewWall = movingWallPreview
        if (previewWall == null) {
            false
        } else {
            movingWallPreview = null
            drawingStart = null
            drawingPreview = null
            chainOrigin = null
            restartLineFromNearestWallStart = false
            viewModel.selectWall(null)
            true
        }
    }
    val lineSnappingEnabledForDraft = !(tool == BlueprintDraftTool.DRAW_WALL && detachedWalls)
    val draftSnapSettings = if (lineSnappingEnabledForDraft) {
        snapSettings
    } else {
        snapSettings.copy(
            endpointEnabled = false,
            midpointEnabled = false,
            closureEnabled = false
        )
    }
    val boxDraftSnapSettings = draftSnapSettings.copy(
        angleEnabled = false,
        closureEnabled = false
    )
    val draftSnapWalls = if (lineSnappingEnabledForDraft) renderedDoc.walls else emptyList()
    val draftIntersectionActive = if (
        tool == BlueprintDraftTool.DRAW_WALL &&
            drawingStart != null &&
            drawingPreview != null
    ) {
        isDraftEndpointIntersectingExistingGeometry(
            drawingStart = drawingStart ?: PointMm(0, 0),
            previewEnd = drawingPreview ?: PointMm(0, 0),
            walls = draftSnapWalls,
            chainOrigin = chainOrigin,
            thresholdMm = Millimeters.fromFeet(draftSnapSettings.thresholdFeet).value
                .coerceAtLeast(WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM)
        )
    } else {
        false
    }
    val handleLivePointerWorld: (PointMm) -> Unit = pointer@{ pointer ->
        if (
            edgeDialInteracting &&
            (
                (drawingStart != null && tool == BlueprintDraftTool.DRAW_WALL) ||
                    (boxStart != null && tool == BlueprintDraftTool.DRAW_BOX)
                )
        ) {
            return@pointer
        }
        openingPointerWorld = pointer
        if (drawingStart != null && tool == BlueprintDraftTool.DRAW_WALL) {
            var previewEnd = pointer
            if (draftSnapSettings.endpointEnabled) {
                previewEnd = snapToNearestWallEndpoint(
                    candidate = previewEnd,
                    walls = draftSnapWalls,
                    thresholdMm = (Millimeters.fromFeet(draftSnapSettings.thresholdFeet).value * 3L / 2L)
                        .coerceAtLeast(1L)
                )
            }
            if (draftSnapSettings.closureEnabled) {
                chainOrigin?.let { origin ->
                    BlueprintSnapMath.roomClosureSnap(
                        candidateEnd = previewEnd,
                        roomStart = origin,
                        thresholdMm = Millimeters.fromFeet(draftSnapSettings.thresholdFeet).value,
                        walls = draftSnapWalls
                    )?.let { previewEnd = it }
                }
            }
            drawingPreview = previewEnd
        }
        if (boxStart != null && tool == BlueprintDraftTool.DRAW_BOX) {
            var previewCorner = pointer
            if (boxDraftSnapSettings.endpointEnabled) {
                previewCorner = snapToNearestWallEndpoint(
                    candidate = previewCorner,
                    walls = draftSnapWalls,
                    thresholdMm = (Millimeters.fromFeet(boxDraftSnapSettings.thresholdFeet).value * 3L / 2L)
                        .coerceAtLeast(1L)
                )
            }
            boxPreview = previewCorner
        }
        if (curveStart != null && tool == BlueprintDraftTool.DRAW_ARC) {
            if (curveEnd == null) {
                var previewEnd = pointer
                if (draftSnapSettings.endpointEnabled) {
                    previewEnd = snapToNearestWallEndpoint(
                        candidate = previewEnd,
                        walls = draftSnapWalls,
                        thresholdMm = (Millimeters.fromFeet(draftSnapSettings.thresholdFeet).value * 3L / 2L)
                            .coerceAtLeast(1L)
                    )
                }
                curvePreviewPoint = previewEnd
            } else {
                curvePreviewPoint = pointer
            }
        }
        if (circleCenter != null && tool == BlueprintDraftTool.DRAW_CIRCLE) {
            circlePreviewEdge = pointer
        }
    }
    val handleTapWorld: (PointMm) -> Unit = { tap ->
        openingPointerWorld = tap
        if (tool != BlueprintDraftTool.SELECT) {
            pendingGrabSelection = false
        }
        if (dualJoysticksEnabled) {
            worldPointToCanvasLocal(
                worldPoint = tap,
                canvasSize = canvasSize,
                scale = scale,
                pan = pan
            )?.let { cursor -> joystickCursorLocal = cursor }
        }
        when (tool) {
            BlueprintDraftTool.DRAW_WALL -> {
                val activeMovingWall = movingWallPreview
                when {
                    activeMovingWall != null -> {
                        commitMovingWallPlacement()
                    }
                    drawingStart == null && selectedWall != null -> {
                        movingWallPreview = selectedWall
                        drawingStart = null
                        drawingPreview = null
                        chainOrigin = null
                        restartLineFromNearestWallStart = false
                        if (dualJoysticksEnabled) {
                            worldPointToCanvasLocal(
                                worldPoint = BlueprintSnapMath.pointOnWall(selectedWall, 0.5),
                                canvasSize = canvasSize,
                                scale = scale,
                                pan = pan
                            )?.let { wallCenter ->
                                joystickCursorLocal = Offset(
                                    x = wallCenter.x.coerceIn(0f, canvasSize.width),
                                    y = wallCenter.y.coerceIn(0f, canvasSize.height)
                                )
                            }
                        }
                    }
                    else -> {
                        val snappedTap = BlueprintSnapMath.applySnapping(
                            rawPoint = tap,
                            drawingStart = drawingStart,
                            settings = draftSnapSettings,
                            walls = draftSnapWalls
                        )
                        if (drawingStart == null) {
                            val startPoint = resolveDraftStartFromTap(
                                tap = tap,
                                snappedTap = snappedTap,
                                walls = draftSnapWalls,
                                scale = scale,
                                snapThresholdFeet = draftSnapSettings.thresholdFeet,
                                endpointSnappingEnabled = draftSnapSettings.endpointEnabled,
                                preferWallProjection = restartLineFromNearestWallStart
                            )
                            drawingStart = startPoint
                            drawingPreview = startPoint
                            chainOrigin = startPoint
                            restartLineFromNearestWallStart = false
                        } else {
                            val start = drawingStart
                            if (start != null) {
                                val end = resolveDraftWallCommitEnd(
                                    previewEnd = drawingPreview,
                                    snappedTap = snappedTap,
                                    walls = draftSnapWalls,
                                    snapThresholdFeet = draftSnapSettings.thresholdFeet,
                                    endpointSnappingEnabled = draftSnapSettings.endpointEnabled,
                                    closureEnabled = draftSnapSettings.closureEnabled,
                                    chainOrigin = chainOrigin
                                )
                                val wallCanBeAdded = canAddDraftedWall(
                                    document = renderedDoc,
                                    start = start,
                                    end = end,
                                    scale = scale
                                )
                                val detachedThisPlacement = detachedWalls
                                if (wallCanBeAdded) {
                                    viewModel.addWall(
                                        WallSegment(
                                            id = UUID.randomUUID().toString(),
                                            start = start,
                                            end = end,
                                            height = Millimeters(doc.params.wallHeightMm),
                                            thickness = Millimeters(doc.params.defaultWallThicknessMm),
                                            tags = setOf("drawn", currentScope.wallScopeTag(), selectedFloor.floorTag())
                                        )
                                    )
                                    recordBlueprintMetric(BlueprintMetricAction.WALL_PLACED)
                                    if (detachedThisPlacement) {
                                        detachedWalls = false
                                    }
                                }
                                val closed = wallCanBeAdded && chainOrigin != null && end == chainOrigin
                                if (!wallCanBeAdded) {
                                    drawingStart = start
                                    drawingPreview = start
                                } else if (!detachedThisPlacement && !closed) {
                                    drawingStart = end
                                    drawingPreview = end
                                } else {
                                    drawingStart = null
                                    drawingPreview = null
                                    chainOrigin = null
                                }
                            }
                        }
                    }
                }
            }
            BlueprintDraftTool.DRAW_BOX -> {
                val snappedTap = BlueprintSnapMath.applySnapping(
                    rawPoint = tap,
                    drawingStart = boxStart,
                    settings = boxDraftSnapSettings,
                    walls = draftSnapWalls
                )
                if (boxStart == null) {
                    boxStart = snappedTap
                    boxPreview = snappedTap
                } else {
                    val start = boxStart
                    if (start != null) {
                        val end = resolveDraftBoxCommitEnd(
                            previewEnd = boxPreview,
                            snappedTap = snappedTap,
                            walls = draftSnapWalls,
                            snapThresholdFeet = boxDraftSnapSettings.thresholdFeet,
                            endpointSnappingEnabled = boxDraftSnapSettings.endpointEnabled
                        )
                        val walls = buildDraftBoxWalls(
                            document = renderedDoc,
                            start = start,
                            end = end,
                            rotationRadians = boxRotationRadians,
                            scale = scale,
                            wallHeightMm = doc.params.wallHeightMm,
                            wallThicknessMm = doc.params.defaultWallThicknessMm,
                            tags = setOf("drawn", currentScope.wallScopeTag(), selectedFloor.floorTag())
                        )
                        if (walls.isNotEmpty()) {
                            viewModel.addWalls(walls)
                            recordBlueprintMetric(BlueprintMetricAction.BOX_PLACED)
                        }
                    }
                    boxStart = null
                    boxPreview = null
                }
            }
            BlueprintDraftTool.DRAW_ARC -> {
                val snappedTap = BlueprintSnapMath.applySnapping(
                    rawPoint = tap,
                    drawingStart = curveStart,
                    settings = draftSnapSettings,
                    walls = draftSnapWalls
                )
                when {
                    curveStart == null -> {
                        val startPoint = resolveDraftStartFromTap(
                            tap = tap,
                            snappedTap = snappedTap,
                            walls = draftSnapWalls,
                            scale = scale,
                            snapThresholdFeet = draftSnapSettings.thresholdFeet,
                            endpointSnappingEnabled = draftSnapSettings.endpointEnabled
                        )
                        curveStart = startPoint
                        curveEnd = null
                        curvePreviewPoint = startPoint
                    }
                    curveEnd == null -> {
                        val start = curveStart
                        if (start != null) {
                            val end = resolveDraftWallCommitEnd(
                                previewEnd = curvePreviewPoint,
                                snappedTap = snappedTap,
                                walls = draftSnapWalls,
                                snapThresholdFeet = draftSnapSettings.thresholdFeet,
                                endpointSnappingEnabled = draftSnapSettings.endpointEnabled,
                                closureEnabled = false,
                                chainOrigin = null
                            )
                            if (canAddDraftedWall(document = renderedDoc, start = start, end = end, scale = scale)) {
                                curveEnd = end
                                curvePreviewPoint = midpointBetween(start, end)
                            } else {
                                resetCurveDraft()
                            }
                        }
                    }
                    else -> {
                        val start = curveStart
                        val end = curveEnd
                        if (start != null && end != null) {
                            val control = curvePreviewPoint ?: tap
                            val walls = buildDraftArcWalls(
                                document = renderedDoc,
                                start = start,
                                end = end,
                                control = control,
                                scale = scale,
                                wallHeightMm = doc.params.wallHeightMm,
                                wallThicknessMm = doc.params.defaultWallThicknessMm,
                                tags = setOf(
                                    "drawn",
                                    CURVE_SHAPE_ARC_TAG,
                                    currentScope.wallScopeTag(),
                                    selectedFloor.floorTag()
                                )
                            )
                            if (walls.isNotEmpty()) {
                                viewModel.addWalls(walls)
                                recordBlueprintMetric(BlueprintMetricAction.WALL_PLACED)
                            }
                        }
                        resetCurveDraft()
                    }
                }
            }
            BlueprintDraftTool.DRAW_CIRCLE -> {
                if (circleCenter == null) {
                    val snappedTap = BlueprintSnapMath.applySnapping(
                        rawPoint = tap,
                        drawingStart = null,
                        settings = draftSnapSettings,
                        walls = draftSnapWalls
                    )
                    circleCenter = resolveDraftStartFromTap(
                        tap = tap,
                        snappedTap = snappedTap,
                        walls = draftSnapWalls,
                        scale = scale,
                        snapThresholdFeet = draftSnapSettings.thresholdFeet,
                        endpointSnappingEnabled = draftSnapSettings.endpointEnabled
                    )
                    circlePreviewEdge = circleCenter
                } else {
                    val center = circleCenter
                    if (center != null) {
                        val edge = circlePreviewEdge ?: tap
                        val walls = buildDraftCircleWalls(
                            document = renderedDoc,
                            center = center,
                            edge = edge,
                            scale = scale,
                            wallHeightMm = doc.params.wallHeightMm,
                            wallThicknessMm = doc.params.defaultWallThicknessMm,
                            tags = setOf(
                                "drawn",
                                CURVE_SHAPE_CIRCLE_TAG,
                                currentScope.wallScopeTag(),
                                selectedFloor.floorTag()
                            )
                        )
                        if (walls.isNotEmpty()) {
                            viewModel.addWalls(walls)
                            recordBlueprintMetric(BlueprintMetricAction.WALL_PLACED)
                        }
                    }
                    resetCircleDraft()
                }
            }
            BlueprintDraftTool.PLACE_DOOR,
            BlueprintDraftTool.PLACE_WINDOW,
            BlueprintDraftTool.PLACE_STAIR_UP,
            BlueprintDraftTool.PLACE_STAIR_DOWN -> {
                val basePreset = when (tool) {
                    BlueprintDraftTool.PLACE_DOOR -> selectedDoorPreset
                    BlueprintDraftTool.PLACE_WINDOW -> selectedWindowPreset
                    BlueprintDraftTool.PLACE_STAIR_UP -> selectedStairUpPreset
                    BlueprintDraftTool.PLACE_STAIR_DOWN -> selectedStairDownPreset
                    else -> tool.defaultPreset() ?: doorPreset
                }
                placeOpeningAtWorld(tap, basePreset)
            }
            BlueprintDraftTool.SELECT -> {
                val nearestWall = nearestWallAt(tap, wallSelectionThresholdMm)
                val nearestOpening = nearestOpeningAt(tap)
                when {
                    nearestOpening != null -> {
                        viewModel.selectOpening(nearestOpening.id)
                    }
                    nearestWall != null -> {
                        viewModel.selectWall(nearestWall.id)
                        if (pendingGrabSelection) {
                            movingWallPreview = nearestWall
                            drawingStart = null
                            drawingPreview = null
                            chainOrigin = null
                            restartLineFromNearestWallStart = false
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                    }
                    else -> {
                        viewModel.selectWall(null)
                    }
                }
            }
            else -> Unit
        }
    }
    val handleRightTapWorld: (PointMm) -> Unit = rightTap@{ tap ->
        if (dualJoysticksEnabled) {
            worldPointToCanvasLocal(
                worldPoint = tap,
                canvasSize = canvasSize,
                scale = scale,
                pan = pan
            )?.let { cursor -> joystickCursorLocal = cursor }
        }
        when {
            tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview != null -> {
                cancelMovingWallPlacement()
            }
            tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> {
                drawingStart = null
                drawingPreview = null
                chainOrigin = null
                restartLineFromNearestWallStart = true
            }
            tool == BlueprintDraftTool.DRAW_BOX -> {
                drawingStart = null
                drawingPreview = null
                chainOrigin = null
                restartLineFromNearestWallStart = false
                boxStart = null
                boxPreview = null
                pendingGrabSelection = false
                tool = BlueprintDraftTool.DRAW_WALL
            }
            tool == BlueprintDraftTool.DRAW_ARC -> {
                resetCurveDraft()
                pendingGrabSelection = false
                tool = BlueprintDraftTool.DRAW_WALL
            }
            tool == BlueprintDraftTool.DRAW_CIRCLE -> {
                resetCircleDraft()
                pendingGrabSelection = false
                tool = BlueprintDraftTool.DRAW_WALL
            }
            else -> {
                val currentSelectedWall = selectedWall
                if (currentSelectedWall != null) {
                    val selectedDistance = BlueprintSnapMath.pointToWallDistanceMm(tap, currentSelectedWall)
                    if (selectedDistance <= rightTapWallSelectionThresholdMm) {
                        viewModel.selectWall(null)
                        restartLineFromNearestWallStart = false
                        return@rightTap
                    }
                }
                val nearestWall = nearestWallAt(tap, rightTapWallSelectionThresholdMm)
                if (nearestWall != null) {
                    viewModel.selectWall(nearestWall.id)
                    restartLineFromNearestWallStart = false
                } else {
                    // No geometry to target; clear selection.
                    viewModel.selectWall(null)
                }
            }
        }
    }
    val rotatePickedUpWallClockwise: () -> Unit = rotate@{
        val movingWall = movingWallPreview ?: return@rotate
        val referenceWalls = renderedDoc.walls.filter { wall -> wall.id != movingWall.id }
        val rotatedWall = movingWall.rotateByDegreesIncrement(MOVING_WALL_ROTATE_INCREMENT_DEG)
        val snappedWall = snapMovedWallToNearbyWalls(
            wall = rotatedWall,
            referenceWalls = referenceWalls,
            thresholdMm = movingWallAutoSnapThresholdMm
        )
        movingWallPreview = snappedWall
        if (dualJoysticksEnabled) {
            worldPointToCanvasLocal(
                worldPoint = snappedWall.midpoint(),
                canvasSize = canvasSize,
                scale = scale,
                pan = pan
            )?.let { center ->
                joystickCursorLocal = Offset(
                    x = center.x.coerceIn(0f, canvasSize.width),
                    y = center.y.coerceIn(0f, canvasSize.height)
                )
            }
        }
    }
    val joystickCursorWorldPoint = if (canvasSize.width > 0f && canvasSize.height > 0f) {
        val cursorLocal = joystickCursorLocal ?: Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        screenPointToWorldPoint(
            rootPoint = canvasRoot + cursorLocal,
            canvasRoot = canvasRoot,
            canvasSize = canvasSize,
            scale = scale,
            pan = pan
        )
    } else {
        null
    }
    val latestLivePointerHandler by androidx.compose.runtime.rememberUpdatedState(handleLivePointerWorld)
    val latestRenderedDoc by androidx.compose.runtime.rememberUpdatedState(renderedDoc)
    val latestDraftSnapSettings by androidx.compose.runtime.rememberUpdatedState(draftSnapSettings)
    val latestLineSnappingEnabledForDraft by androidx.compose.runtime.rememberUpdatedState(lineSnappingEnabledForDraft)
    val dispatchLeftJoystickClick: () -> Unit = click@{
        if (!dualJoysticksEnabled) return@click
        val cursor = joystickCursorLocal ?: run {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f) return@click
            Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        }
        joystickCursorLocal = cursor
        if (dispatchMenuTapAtCursor(cursor)) return@click
        val worldTap = screenPointToWorldPoint(
            rootPoint = canvasRoot + cursor,
            canvasRoot = canvasRoot,
            canvasSize = canvasSize,
            scale = scale,
            pan = pan
        ) ?: return@click
        handleTapWorld(worldTap)
    }
    val dispatchRightJoystickClick: () -> Unit = click@{
        if (!dualJoysticksEnabled) return@click
        val cursor = joystickCursorLocal ?: run {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f) return@click
            Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        }
        joystickCursorLocal = cursor
        val worldTap = screenPointToWorldPoint(
            rootPoint = canvasRoot + cursor,
            canvasRoot = canvasRoot,
            canvasSize = canvasSize,
            scale = scale,
            pan = pan
        ) ?: return@click
        handleRightTapWorld(worldTap)
    }
    val gridScaleMinFeet = MIN_GRID_STEP_FEET
    val gridScaleMaxFeet = MAX_GRID_STEP_FEET
    val gridScaleMinInches = (gridScaleMinFeet * 12.0).roundToInt()
    val gridScaleMaxInches = (gridScaleMaxFeet * 12.0).roundToInt()
    val gridScaleMinCentimeters = Millimeters.fromFeet(gridScaleMinFeet).value / 10.0
    val gridScaleMaxCentimeters = Millimeters.fromFeet(gridScaleMaxFeet).value / 10.0
    val syncGridScaleAssistInputs: (Double) -> Unit = { stepFeet ->
        val clampedFeet = stepFeet.coerceIn(gridScaleMinFeet, gridScaleMaxFeet)
        val clampedInches = (clampedFeet * 12.0)
            .roundToInt()
            .coerceIn(gridScaleMinInches, gridScaleMaxInches)
        gridScaleFeetInput = (clampedInches / 12).toString()
        gridScaleInchesInput = (clampedInches % 12).toString()
        val centimeters = Millimeters.fromFeet(clampedFeet).value / 10.0
        gridScaleCentimetersInput = formatScaleCentimetersValue(centimeters)
    }
    val applyImperialAssistInputsToGridScaleInput: () -> Unit = apply@{
        val feet = gridScaleFeetInput.toIntOrNull() ?: return@apply
        val inches = gridScaleInchesInput.toIntOrNull() ?: return@apply
        val totalInches = (feet * 12 + inches.coerceAtLeast(0))
            .coerceIn(gridScaleMinInches, gridScaleMaxInches)
        val feetValue = totalInches / 12.0
        gridScaleInput = formatFeetInchesPrime(feetValue)
        syncGridScaleAssistInputs(feetValue)
    }
    val applyMetricAssistInputToGridScaleInput: () -> Unit = apply@{
        val centimeters = gridScaleCentimetersInput.toDoubleOrNull() ?: return@apply
        val clampedCentimeters = centimeters.coerceIn(gridScaleMinCentimeters, gridScaleMaxCentimeters)
        val feetValue = Millimeters((clampedCentimeters * 10.0).roundToLong()).toFeet()
        gridScaleInput = "${formatScaleCentimetersValue(clampedCentimeters)}cm"
        syncGridScaleAssistInputs(feetValue)
    }
    val nudgeGridScaleInches: (Int) -> Unit = { deltaInches ->
        val baseFeet = parsePrimeLengthToFeet(gridScaleInput) ?: run {
            val assistedFeet = gridScaleFeetInput.toIntOrNull()
            val assistedInches = gridScaleInchesInput.toIntOrNull()
            if (assistedFeet != null && assistedInches != null) {
                (assistedFeet * 12 + assistedInches.coerceAtLeast(0)) / 12.0
            } else {
                snapSettings.gridStepFeet
            }
        }
        val baseInches = (baseFeet * 12.0).roundToInt().coerceIn(gridScaleMinInches, gridScaleMaxInches)
        val nudgedInches = (baseInches + deltaInches).coerceIn(gridScaleMinInches, gridScaleMaxInches)
        val nudgedFeet = nudgedInches / 12.0
        gridScaleInput = formatFeetInchesPrime(nudgedFeet)
        syncGridScaleAssistInputs(nudgedFeet)
    }
    val nudgeGridScaleCentimeters: (Int) -> Unit = { deltaCentimeters ->
        val baseFeet = parsePrimeLengthToFeet(gridScaleInput) ?: run {
            gridScaleCentimetersInput.toDoubleOrNull()?.let { centimeters ->
                Millimeters((centimeters * 10.0).roundToLong()).toFeet()
            } ?: snapSettings.gridStepFeet
        }
        val baseCentimeters = (Millimeters.fromFeet(baseFeet).value / 10.0)
            .roundToInt()
            .coerceIn(gridScaleMinCentimeters.roundToInt(), gridScaleMaxCentimeters.roundToInt())
        val nudgedCentimeters = (baseCentimeters + deltaCentimeters)
            .coerceIn(gridScaleMinCentimeters.roundToInt(), gridScaleMaxCentimeters.roundToInt())
        val nudgedFeet = Millimeters((nudgedCentimeters * 10).toLong()).toFeet()
        gridScaleInput = "${nudgedCentimeters}cm"
        syncGridScaleAssistInputs(nudgedFeet)
    }
    val gridScaleLabel = formatGridScaleLabel(snapSettings.gridStepFeet, appSettings.useMetric)
    LaunchedEffect(showGridScaleEditor, appSettings.useMetric) {
        if (showGridScaleEditor) {
            val currentStepFeet = snapSettings.gridStepFeet.coerceIn(gridScaleMinFeet, gridScaleMaxFeet)
            gridScaleInput = if (appSettings.useMetric) {
                val centimeters = Millimeters.fromFeet(currentStepFeet).value / 10.0
                "${formatScaleCentimetersValue(centimeters)}cm"
            } else {
                formatFeetInchesPrime(currentStepFeet)
            }
            syncGridScaleAssistInputs(currentStepFeet)
        }
    }
    LaunchedEffect(dualJoysticksEnabled, canvasSize) {
        if (!dualJoysticksEnabled) {
            rightJoystickVector = Offset.Zero
            leftJoystickVector = Offset.Zero
            rightJoystickPressed = false
            joystickCursorLocal = null
            return@LaunchedEffect
        }
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return@LaunchedEffect
        val existing = joystickCursorLocal
        joystickCursorLocal = if (existing == null) {
            Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        } else {
            Offset(
                x = existing.x.coerceIn(0f, canvasSize.width),
                y = existing.y.coerceIn(0f, canvasSize.height)
            )
        }
    }
    val joystickFrameLoopActive = dualJoysticksEnabled && (
        leftJoystickVector != Offset.Zero ||
            rightJoystickVector != Offset.Zero ||
            movingWallPreview != null
        )
    LaunchedEffect(dualJoysticksEnabled, joystickFrameLoopActive) {
        if (!dualJoysticksEnabled || !joystickFrameLoopActive) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (
            dualJoysticksEnabled &&
                (
                    leftJoystickVector != Offset.Zero ||
                        rightJoystickVector != Offset.Zero ||
                        movingWallPreview != null
                    )
        ) {
            val frameNanos = withFrameNanos { it }
            val frameDeltaSec = if (lastFrameNanos == 0L) {
                1f / JOYSTICK_FRAME_RATE_BASE
            } else {
                ((frameNanos - lastFrameNanos) / 1_000_000_000f)
                    .coerceIn(JOYSTICK_MIN_FRAME_DELTA_SEC, JOYSTICK_MAX_FRAME_DELTA_SEC)
            }
            lastFrameNanos = frameNanos
            val size = canvasSize
            if (size.width > 0f && size.height > 0f) {
                var updatedPan = pan
                var updatedCursor = joystickCursorLocal ?: Offset(size.width / 2f, size.height / 2f)
                var updatedMovingWall = movingWallPreview
                val renderedDocSnapshot = latestRenderedDoc
                val rightInput = applyJoystickDeadzone(
                    input = rightJoystickVector,
                    deadzone = joystickDeadzone,
                    responseExponent = JOYSTICK_CURSOR_RESPONSE_EXPONENT
                )
                if (rightInput != Offset.Zero) {
                    val cursorStep = JOYSTICK_CURSOR_SPEED_PX_PER_SEC * joystickSensitivity * frameDeltaSec
                    val delta = Offset(
                        x = rightInput.x * cursorStep,
                        y = rightInput.y * cursorStep
                    )
                    if (tool == BlueprintDraftTool.DRAW_WALL && updatedMovingWall != null) {
                        val pxPerMm = (BASE_PX_PER_MM * scale).coerceAtLeast(0.0001f)
                        val dxMm = (delta.x / pxPerMm).roundToLong()
                        val dyMm = (-(delta.y) / pxPerMm).roundToLong()
                        val movingWall = updatedMovingWall
                        if (dxMm != 0L || dyMm != 0L) {
                            updatedMovingWall = movingWall?.translateBy(dxMm, dyMm)
                        }
                    } else {
                        // Raw/direct cursor movement: no edge autopan and no grid-pull assist.
                        updatedCursor = Offset(
                            x = (updatedCursor.x + delta.x).coerceIn(0f, size.width),
                            y = (updatedCursor.y + delta.y).coerceIn(0f, size.height)
                        )
                    }
                }
                val leftInput = applyJoystickDeadzone(
                    input = leftJoystickVector,
                    deadzone = joystickDeadzone,
                    responseExponent = JOYSTICK_PAN_RESPONSE_EXPONENT
                )
                if (leftInput != Offset.Zero) {
                    val leftMagnitude = hypot(leftInput.x.toDouble(), leftInput.y.toDouble()).toFloat().coerceIn(0f, 1f)
                    val panSpeed = (
                        JOYSTICK_PAN_SPEED_PX_PER_SEC +
                            (JOYSTICK_PAN_BOOST_PX_PER_SEC * leftMagnitude)
                        ) * joystickSensitivity
                    val panDelta = Offset(
                        x = -leftInput.x * panSpeed * frameDeltaSec,
                        y = -leftInput.y * panSpeed * frameDeltaSec
                    )
                    updatedPan += panDelta
                    // Keep cursor anchored to the same world point while panning,
                    // until it reaches a screen edge.
                    updatedCursor = Offset(
                        x = (updatedCursor.x + panDelta.x).coerceIn(0f, size.width),
                        y = (updatedCursor.y + panDelta.y).coerceIn(0f, size.height)
                    )
                }
                if (tool == BlueprintDraftTool.DRAW_WALL && updatedMovingWall != null && rightInput == Offset.Zero) {
                    val snappedMovingWall = snapMovedWallToNearbyWalls(
                        wall = updatedMovingWall,
                        referenceWalls = renderedDocSnapshot.walls.filter { wall -> wall.id != updatedMovingWall.id },
                        thresholdMm = movingWallAutoSnapThresholdMm
                    )
                    updatedMovingWall = snappedMovingWall
                }
                if (tool == BlueprintDraftTool.DRAW_WALL && updatedMovingWall != null) {
                    worldPointToCanvasLocal(
                        worldPoint = BlueprintSnapMath.pointOnWall(updatedMovingWall, 0.5),
                        canvasSize = size,
                        scale = scale,
                        pan = updatedPan
                    )?.let { wallCenter ->
                        updatedCursor = Offset(
                            x = wallCenter.x.coerceIn(0f, size.width),
                            y = wallCenter.y.coerceIn(0f, size.height)
                        )
                    }
                }
                pan = updatedPan
                joystickCursorLocal = updatedCursor
                movingWallPreview = updatedMovingWall
                val worldPoint = screenPointToWorldPoint(
                    rootPoint = canvasRoot + updatedCursor,
                    canvasRoot = canvasRoot,
                    canvasSize = size,
                    scale = scale,
                    pan = updatedPan
                )
                if (worldPoint != null) {
                    val currentDraftSnapSettings = latestDraftSnapSettings
                    val currentLineSnappingEnabled = latestLineSnappingEnabledForDraft
                    val snappedWorld = BlueprintSnapMath.applySnapping(
                        rawPoint = worldPoint,
                        drawingStart = drawingStart,
                        settings = currentDraftSnapSettings,
                        walls = if (currentLineSnappingEnabled) renderedDocSnapshot.walls else emptyList()
                    )
                    latestLivePointerHandler(snappedWorld)
                }
            }
        }
    }
    val leftControlsInset = leftEdgeDialInset.coerceAtLeast(0.dp)
    val centeredControlsInset = (leftControlsInset / 2f).coerceAtLeast(0.dp)
    val joystickRailPadding = if (dualJoysticksEnabled) 56.dp else 0.dp
    val panelBottomPadding = DEFAULT_PANEL_BOTTOM_PADDING + joystickRailPadding
    val helpBottomPadding = panelBottomPadding + 14.dp
    val density = LocalDensity.current
    val topStackMeasuredHeight = with(density) {
        maxOf(
            topStartStackBounds?.height ?: 0f,
            topEndStackBounds?.height ?: 0f
        ).toDp()
    }
    val useBottomDockedFloorSwitcher = !dualJoysticksEnabled && (
        compactHeightWindow ||
            configuration.screenWidthDp < 420 ||
            activeOpeningPanel != null ||
            topStackMeasuredHeight > 150.dp
        )
    val floorSwitcherTopPadding = overlayTopPadding + topStackMeasuredHeight + topOverlaySpacing
    val floorSwitcherBottomPadding = helpBottomPadding + if (compactHeightWindow) 8.dp else 12.dp
    val sharedBottomControlsPadding = 64.dp
    val controlStateLabel: String? = when {
        movingWallPreview != null -> "Picked Up"
        tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> "Draw"
        tool == BlueprintDraftTool.DRAW_BOX && boxStart != null -> "Box"
        tool == BlueprintDraftTool.DRAW_ARC && curveStart != null -> "Curve"
        tool == BlueprintDraftTool.DRAW_CIRCLE && circleCenter != null -> "Circle"
        selectedWall != null -> "Selected"
        else -> null
    }
    val showEdgeDials =
        (tool == BlueprintDraftTool.DRAW_WALL && (drawingStart != null || movingWallPreview != null)) ||
            tool == BlueprintDraftTool.DRAW_BOX
    LaunchedEffect(showEdgeDials) {
        if (!showEdgeDials) {
            edgeDialActiveTouchCount = 0
            edgeDialInteracting = false
            edgeDialAngleRadians = null
            edgeDialLengthMm = null
        }
    }
    val updateDraftPreviewFromDial: (PointMm) -> Unit = { preview ->
        drawingPreview = preview
        openingPointerWorld = preview
        if (dualJoysticksEnabled) {
            worldPointToCanvasLocal(
                worldPoint = preview,
                canvasSize = canvasSize,
                scale = scale,
                pan = pan
            )?.let { local ->
                joystickCursorLocal = Offset(
                    x = local.x.coerceIn(0f, canvasSize.width),
                    y = local.y.coerceIn(0f, canvasSize.height)
                )
            }
        }
    }
    val coordinateOverlayWorldPoint = if (dualJoysticksEnabled) {
        joystickCursorWorldPoint
    } else {
        openingPointerWorld
    }
    val centerCoordinateOverlay: (@Composable () -> Unit)? = coordinateOverlayWorldPoint?.let { worldPoint ->
        {
            CursorCoordinateOverlay(
                worldPoint = worldPoint,
                showRotate = movingWallPreview != null,
                onRotate = rotatePickedUpWallClockwise,
                useMetric = appSettings.useMetric,
                compact = true,
                rotateButtonModifier = Modifier.onGloballyPositioned {
                    wallRotateButtonBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
            )
        }
    }
    val handleUndo = { viewModel.undo() }
    val handleRedo = { viewModel.redo() }
    val handleZoomIn = { scale = (scale * 1.15f).coerceAtMost(MAX_BLUEPRINT_SCALE) }
    val handleZoomOut = { scale = (scale / 1.15f).coerceAtLeast(MIN_BLUEPRINT_SCALE) }
    val tutorialTargetBounds = when (currentTutorialStep?.target) {
        BlueprintControlTutorialTarget.BOTTOM_RAIL -> bottomRailBounds
        BlueprintControlTutorialTarget.TOUCH_LEFT_TOOLS,
        BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD -> tutorialLeftControlsBounds
        BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
        BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS -> tutorialCenterControlsBounds
        BlueprintControlTutorialTarget.TOUCH_RIGHT_TOOLS,
        BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD -> tutorialRightControlsBounds
        BlueprintControlTutorialTarget.FLOOR_SWITCHER -> floorSwitcherBounds
        BlueprintControlTutorialTarget.GRID_SCALE_BADGE -> gridScaleBadgeBounds
        BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON -> clearAllButtonBounds
        null -> null
    }
    val tutorialLeftVector = if (tutorialMode) {
        animatedTutorialJoystickVector(currentTutorialStep?.demoLeftVector ?: Offset.Zero)
    } else {
        Offset.Zero
    }
    val tutorialRightVector = if (tutorialMode) {
        animatedTutorialJoystickVector(currentTutorialStep?.demoRightVector ?: Offset.Zero)
    } else {
        Offset.Zero
    }
    val tutorialModeLabel = if (dualJoysticksEnabled) {
        "Dual joysticks control tour"
    } else {
        "Touch mode control tour"
    }
    val onEdgeDialInteractionChanged: (Boolean) -> Unit = { active ->
        edgeDialActiveTouchCount = (edgeDialActiveTouchCount + if (active) 1 else -1).coerceAtLeast(0)
        val currentlyActive = edgeDialActiveTouchCount > 0
        if (currentlyActive && !edgeDialInteracting) {
            when {
                tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview != null -> {
                    val pickedUpWall = movingWallPreview
                    if (pickedUpWall != null) {
                        val dx = (pickedUpWall.end.x - pickedUpWall.start.x).toDouble()
                        val dy = (pickedUpWall.end.y - pickedUpWall.start.y).toDouble()
                        edgeDialAngleRadians = atan2(dy, dx)
                        edgeDialLengthMm = pickedUpWall.lengthMillimeters().toDouble()
                    }
                }
                tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> {
                    val start = drawingStart
                    if (start != null) {
                        val current = drawingPreview ?: start
                        val dx = (current.x - start.x).toDouble()
                        val dy = (current.y - start.y).toDouble()
                        val lengthMm = hypot(dx, dy)
                        val resolvedLength = if (lengthMm <= 0.0001) {
                            defaultDraftDialLengthMm(scale)
                        } else {
                            lengthMm.coerceAtMost(DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble())
                        }
                        val resolvedAngle = if (lengthMm <= 0.0001) 0.0 else atan2(dy, dx)
                        edgeDialAngleRadians = resolvedAngle
                        edgeDialLengthMm = resolvedLength
                    }
                }
                tool == BlueprintDraftTool.DRAW_BOX -> {
                    edgeDialAngleRadians = boxRotationRadians
                    edgeDialLengthMm = null
                }
            }
        }
        edgeDialInteracting = currentlyActive
        if (!currentlyActive) {
            edgeDialAngleRadians = null
            edgeDialLengthMm = null
        }
    }
    val onAngleDialTicks: (Int) -> Unit = ticks@{ tickCount ->
        if (tickCount == 0) return@ticks
        when {
            tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview != null -> {
                val updated = movingWallPreview?.rotateByDialTicks(tickCount) ?: return@ticks
                movingWallPreview = updated
                edgeDialAngleRadians = atan2(
                    (updated.end.y - updated.start.y).toDouble(),
                    (updated.end.x - updated.start.x).toDouble()
                )
                edgeDialLengthMm = updated.lengthMillimeters().toDouble()
            }
            tool == BlueprintDraftTool.DRAW_BOX && boxStart != null && boxPreview != null -> {
                val start = boxStart ?: return@ticks
                val preview = boxPreview ?: return@ticks
                val rotated = rotateDraftBoxPreviewByDialTicks(
                    start = start,
                    currentPreview = preview,
                    currentRotationRadians = boxRotationRadians,
                    tickCount = tickCount
                )
                boxRotationRadians = rotated.rotationRadians
                boxPreview = rotated.preview
                openingPointerWorld = rotated.preview
                edgeDialAngleRadians = rotated.rotationRadians
            }
            tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview == null -> {
                val start = drawingStart ?: return@ticks
                val baseAngle = edgeDialAngleRadians ?: run {
                    val current = drawingPreview ?: start
                    atan2((current.y - start.y).toDouble(), (current.x - start.x).toDouble())
                }
                val baseLength = edgeDialLengthMm ?: run {
                    val current = drawingPreview ?: start
                    val measured = hypot(
                        (current.x - start.x).toDouble(),
                        (current.y - start.y).toDouble()
                    )
                    if (measured <= 0.0001) {
                        defaultDraftDialLengthMm(scale)
                    } else {
                        measured.coerceAtMost(DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble())
                    }
                }
                val nextAngle = baseAngle + Math.toRadians(tickCount * DRAW_EDGE_DIAL_ANGLE_STEP_DEGREES)
                edgeDialAngleRadians = nextAngle
                edgeDialLengthMm = baseLength
                val adjusted = pointFromPolarDraft(
                    start = start,
                    angleRadians = nextAngle,
                    lengthMm = baseLength
                )
                updateDraftPreviewFromDial(adjusted)
            }
        }
    }
    val onLengthDialTicks: (Int) -> Unit = ticks@{ tickCount ->
        if (tickCount == 0) return@ticks
        when {
            tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview != null -> {
                val updated = movingWallPreview?.resizeByDialTicks(tickCount) ?: return@ticks
                movingWallPreview = updated
                edgeDialAngleRadians = atan2(
                    (updated.end.y - updated.start.y).toDouble(),
                    (updated.end.x - updated.start.x).toDouble()
                )
                edgeDialLengthMm = updated.lengthMillimeters().toDouble()
            }
            tool == BlueprintDraftTool.DRAW_BOX && boxStart != null && boxPreview != null -> {
                val start = boxStart ?: return@ticks
                val preview = boxPreview ?: return@ticks
                val resized = resizeDraftBoxPreviewByDialTicks(
                    start = start,
                    currentPreview = preview,
                    currentRotationRadians = boxRotationRadians,
                    tickCount = tickCount,
                    scale = scale
                )
                boxPreview = resized
                openingPointerWorld = resized
            }
            tool == BlueprintDraftTool.DRAW_WALL && movingWallPreview == null -> {
                val start = drawingStart ?: return@ticks
                val baseAngle = edgeDialAngleRadians ?: run {
                    val current = drawingPreview ?: start
                    atan2((current.y - start.y).toDouble(), (current.x - start.x).toDouble())
                }
                val baseLength = edgeDialLengthMm ?: run {
                    val current = drawingPreview ?: start
                    val measured = hypot(
                        (current.x - start.x).toDouble(),
                        (current.y - start.y).toDouble()
                    )
                    if (measured <= 0.0001) {
                        defaultDraftDialLengthMm(scale)
                    } else {
                        measured.coerceAtMost(DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble())
                    }
                }
                val nextLength = (baseLength + (tickCount * DRAW_EDGE_DIAL_LENGTH_STEP_MM))
                    .coerceIn(
                        MIN_DRAW_WALL_LENGTH_MM.toDouble(),
                        DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble()
                    )
                edgeDialAngleRadians = baseAngle
                edgeDialLengthMm = nextLength
                val adjusted = pointFromPolarDraft(
                    start = start,
                    angleRadians = baseAngle,
                    lengthMm = nextLength
                )
                updateDraftPreviewFromDial(adjusted)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BLUEPRINT_BACKGROUND_TOP,
                        BLUEPRINT_BACKGROUND_BOTTOM
                    )
                )
            )
    ) {
        BlueprintCanvas(
            modifier = Modifier,
            document = renderedDoc,
            scope = currentScope,
            tool = tool,
            snapSettings = snapSettings,
            scale = scale,
            pan = pan,
            drawingStart = drawingStart,
            drawingPreview = drawingPreview,
            boxStart = boxStart,
            boxPreview = boxPreview,
            boxRotationRadians = boxRotationRadians,
            curveStart = curveStart,
            curveEnd = curveEnd,
            curvePreviewPoint = curvePreviewPoint,
            circleCenter = circleCenter,
            circlePreviewEdge = circlePreviewEdge,
            selectedWallId = uiState.selectedWallId,
            selectedOpeningId = uiState.selectedOpeningId,
            movingWallActive = movingWallPreview != null,
            cursorVisible = cursorVisible,
            cursorSizeScale = cursorScale,
            draftIntersectionActive = draftIntersectionActive,
            useMetric = appSettings.useMetric,
            lineSnappingEnabled = lineSnappingEnabledForDraft,
            dragPreview = dragPreview,
            onPanScaleChange = { updatedPan, updatedScale ->
                val panDelta = updatedPan - pan
                val transformed = panDelta.getDistance() > 0.5f || kotlin.math.abs(updatedScale - scale) > 0.001f
                if (dualJoysticksEnabled) {
                    joystickCursorLocal = joystickCursorLocal?.let { cursor ->
                        if (canvasSize.width > 0f && canvasSize.height > 0f) {
                            Offset(
                                x = (cursor.x + panDelta.x).coerceIn(0f, canvasSize.width),
                                y = (cursor.y + panDelta.y).coerceIn(0f, canvasSize.height)
                            )
                        } else {
                            cursor
                        }
                    }
                }
                pan = updatedPan
                scale = updatedScale.coerceIn(MIN_BLUEPRINT_SCALE, MAX_BLUEPRINT_SCALE)
            },
            onCanvasLayout = { root, size ->
                canvasRoot = root
                canvasSize = size
            },
            touchEnabled = !dualJoysticksEnabled,
            onTouchBlocked = {},
            virtualPointerScreenPoint = if (dualJoysticksEnabled) joystickCursorLocal else null,
            rightSelectBoostActive = rightSelectBoostActive,
            onLivePointerWorld = handleLivePointerWorld,
            onTapWorld = handleTapWorld
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = overlayTopPadding)
                .widthIn(max = compactHudLiveWidth)
                .heightIn(max = topStackMaxHeight)
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned {
                    topStartStackBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
            verticalArrangement = Arrangement.spacedBy(topOverlaySpacing)
        ) {
            LiveOverlay(
                liveScopeQuantity = liveScopeQuantity,
                squareFeet = squareFeet,
                linearFeet = linearFeet,
                selectedFloor = selectedFloor,
                useMetric = appSettings.useMetric
            )
            if (selectedWall != null || selectedOpening != null) {
                SelectionPanel(
                    selectedWall = selectedWall,
                    selectedOpening = selectedOpening,
                    circleSelection = circleSelection,
                    useMetric = appSettings.useMetric,
                    onCircleRadiusStep = { direction ->
                        selectedWall?.id?.let { selectedId ->
                            resizeSelectedCircle(
                                document = doc,
                                selectedWallId = selectedId,
                                radiusDeltaMm = direction * circleResizeStepMm
                            )?.let { updatedWalls ->
                                viewModel.replaceWalls(updatedWalls)
                            }
                        }
                    },
                    onCircleDiameterStep = { direction ->
                        selectedWall?.id?.let { selectedId ->
                            resizeSelectedCircle(
                                document = doc,
                                selectedWallId = selectedId,
                                radiusDeltaMm = direction * (circleResizeStepMm / 2L).coerceAtLeast(1L)
                            )?.let { updatedWalls ->
                                viewModel.replaceWalls(updatedWalls)
                            }
                        }
                    },
                    onDeselect = {
                        viewModel.selectWall(null)
                        viewModel.selectOpening(null)
                    },
                    modifier = Modifier.onGloballyPositioned {
                        selectionPanelBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = overlayTopPadding)
                .widthIn(max = compactHudTopStackWidth)
                .heightIn(max = topStackMaxHeight)
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned {
                    topEndStackBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(topOverlaySpacing)
        ) {
            ClearAllButton(
                onClick = { showClearAllConfirm = true },
                modifier = Modifier.onGloballyPositioned {
                    clearAllButtonBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
            )

            if (!dualJoysticksEnabled) {
                GridScaleBadge(
                    label = gridScaleLabel,
                    onClick = {
                        showGridScaleEditor = !showGridScaleEditor
                        if (showGridScaleEditor) {
                            val currentStepFeet = snapSettings.gridStepFeet.coerceIn(gridScaleMinFeet, gridScaleMaxFeet)
                            gridScaleInput = if (appSettings.useMetric) {
                                val centimeters = Millimeters.fromFeet(currentStepFeet).value / 10.0
                                "${formatScaleCentimetersValue(centimeters)}cm"
                            } else {
                                formatFeetInchesPrime(currentStepFeet)
                            }
                            syncGridScaleAssistInputs(currentStepFeet)
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onGloballyPositioned {
                            gridScaleBadgeBounds = Rect(it.positionInRoot(), it.size.toSize())
                        }
                )
            }

            ScopeSelector(
                scope = currentScope,
                onChangeScope = viewModel::updateTakeoffScope,
                minWidth = if (compactHeightWindow) 70.dp else 78.dp,
                minHeight = if (compactHeightWindow) 32.dp else 36.dp,
                horizontalPadding = if (compactHeightWindow) 7.dp else 8.dp,
                iconSize = if (compactHeightWindow) 11.dp else 12.dp,
                labelFontSize = if (compactHeightWindow) 9.5.sp else 10.5.sp
            )

            if (activeOpeningPanel != null) {
                OpeningAddonsPanel(
                    panelType = activeOpeningPanel ?: OpeningPanelType.DOORS,
                    selectedPreset = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> selectedDoorPreset
                        OpeningPanelType.WINDOWS -> selectedWindowPreset
                        OpeningPanelType.STAIR_UP -> selectedStairUpPreset
                        OpeningPanelType.STAIR_DOWN -> selectedStairDownPreset
                        null -> selectedDoorPreset
                    },
                    presets = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> doorPresets
                        OpeningPanelType.WINDOWS -> windowPresets
                        OpeningPanelType.STAIR_UP -> stairUpPresets
                        OpeningPanelType.STAIR_DOWN -> stairDownPresets
                        null -> doorPresets
                    },
                    customWidthFeet = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> doorWidthFeet
                        OpeningPanelType.WINDOWS -> windowWidthFeet
                        OpeningPanelType.STAIR_UP -> stairUpWidthFeet
                        OpeningPanelType.STAIR_DOWN -> stairDownWidthFeet
                        null -> doorWidthFeet
                    },
                    customHeightFeet = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> doorHeightFeet
                        OpeningPanelType.WINDOWS -> windowHeightFeet
                        OpeningPanelType.STAIR_UP -> stairUpHeightFeet
                        OpeningPanelType.STAIR_DOWN -> stairDownHeightFeet
                        null -> doorHeightFeet
                    },
                    customSillFeet = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> doorSillFeet
                        OpeningPanelType.WINDOWS -> windowSillFeet
                        OpeningPanelType.STAIR_UP -> stairUpSillFeet
                        OpeningPanelType.STAIR_DOWN -> stairDownSillFeet
                        null -> doorSillFeet
                    },
                    showPresets = when (activeOpeningPanel) {
                        OpeningPanelType.DOORS -> showDoorPresets
                        OpeningPanelType.WINDOWS -> showWindowPresets
                        OpeningPanelType.STAIR_UP -> showStairUpPresets
                        OpeningPanelType.STAIR_DOWN -> showStairDownPresets
                        null -> showDoorPresets
                    },
                    onTogglePresets = {
                        when (activeOpeningPanel) {
                            OpeningPanelType.DOORS -> showDoorPresets = !showDoorPresets
                            OpeningPanelType.WINDOWS -> showWindowPresets = !showWindowPresets
                            OpeningPanelType.STAIR_UP -> showStairUpPresets = !showStairUpPresets
                            OpeningPanelType.STAIR_DOWN -> showStairDownPresets = !showStairDownPresets
                            null -> Unit
                        }
                    },
                    onSelectPreset = { preset ->
                        when (preset.type) {
                            OpeningType.DOOR -> {
                                selectedDoorPreset = preset
                                doorWidthFeet = "%.2f".format(Millimeters(preset.widthMm).toFeet())
                                doorHeightFeet = "%.2f".format(Millimeters(preset.heightMm).toFeet())
                                doorSillFeet = "%.2f".format(Millimeters(preset.sillMm).toFeet())
                            }

                            OpeningType.WINDOW -> {
                                selectedWindowPreset = preset
                                windowWidthFeet = "%.2f".format(Millimeters(preset.widthMm).toFeet())
                                windowHeightFeet = "%.2f".format(Millimeters(preset.heightMm).toFeet())
                                windowSillFeet = "%.2f".format(Millimeters(preset.sillMm).toFeet())
                            }

                            OpeningType.STAIR_UP -> {
                                selectedStairUpPreset = preset
                                stairUpWidthFeet = "%.2f".format(Millimeters(preset.widthMm).toFeet())
                                stairUpHeightFeet = "%.2f".format(Millimeters(preset.heightMm).toFeet())
                                stairUpSillFeet = "%.2f".format(Millimeters(preset.sillMm).toFeet())
                            }

                            OpeningType.STAIR_DOWN -> {
                                selectedStairDownPreset = preset
                                stairDownWidthFeet = "%.2f".format(Millimeters(preset.widthMm).toFeet())
                                stairDownHeightFeet = "%.2f".format(Millimeters(preset.heightMm).toFeet())
                                stairDownSillFeet = "%.2f".format(Millimeters(preset.sillMm).toFeet())
                            }
                        }
                        tool = preset.type.toDraftTool()
                    },
                    onCustomWidthChange = { value ->
                        when (activeOpeningPanel) {
                            OpeningPanelType.DOORS -> doorWidthFeet = value
                            OpeningPanelType.WINDOWS -> windowWidthFeet = value
                            OpeningPanelType.STAIR_UP -> stairUpWidthFeet = value
                            OpeningPanelType.STAIR_DOWN -> stairDownWidthFeet = value
                            null -> Unit
                        }
                    },
                    onCustomHeightChange = { value ->
                        when (activeOpeningPanel) {
                            OpeningPanelType.DOORS -> doorHeightFeet = value
                            OpeningPanelType.WINDOWS -> windowHeightFeet = value
                            OpeningPanelType.STAIR_UP -> stairUpHeightFeet = value
                            OpeningPanelType.STAIR_DOWN -> stairDownHeightFeet = value
                            null -> Unit
                        }
                    },
                    onCustomSillChange = { value ->
                        when (activeOpeningPanel) {
                            OpeningPanelType.DOORS -> doorSillFeet = value
                            OpeningPanelType.WINDOWS -> windowSillFeet = value
                            OpeningPanelType.STAIR_UP -> stairUpSillFeet = value
                            OpeningPanelType.STAIR_DOWN -> stairDownSillFeet = value
                            null -> Unit
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onGloballyPositioned {
                            openingPanelBounds = Rect(it.positionInRoot(), it.size.toSize())
                        }
                )
            }

            if (!dualJoysticksEnabled) {
                GridScaleEditorPanel(
                    expanded = showGridScaleEditor,
                    useMetric = appSettings.useMetric,
                    value = gridScaleInput,
                    onValueChange = {
                        gridScaleInput = it
                        parsePrimeLengthToFeet(it)?.let(syncGridScaleAssistInputs)
                    },
                    feetValue = gridScaleFeetInput,
                    inchesValue = gridScaleInchesInput,
                    centimetersValue = gridScaleCentimetersInput,
                    onFeetValueChange = { value ->
                        gridScaleFeetInput = value.filter { it.isDigit() }.take(3)
                        applyImperialAssistInputsToGridScaleInput()
                    },
                    onInchesValueChange = { value ->
                        gridScaleInchesInput = value.filter { it.isDigit() }.take(3)
                        applyImperialAssistInputsToGridScaleInput()
                    },
                    onCentimetersValueChange = { value ->
                        gridScaleCentimetersInput = value.filter { it.isDigit() || it == '.' }.take(8)
                        applyMetricAssistInputToGridScaleInput()
                    },
                    onNudgeInches = nudgeGridScaleInches,
                    onNudgeCentimeters = nudgeGridScaleCentimeters,
                    onDismiss = { showGridScaleEditor = false },
                    onApply = {
                        val parsedFeet = parsePrimeLengthToFeet(gridScaleInput)
                        if (parsedFeet != null) {
                            val clampedFeet = parsedFeet.coerceIn(gridScaleMinFeet, gridScaleMaxFeet)
                            snapSettings = snapSettings.copy(
                                gridEnabled = true,
                                gridStepFeet = clampedFeet
                            )
                            syncGridScaleAssistInputs(clampedFeet)
                            settingsViewModel.updateBlueprintSnapDefaults(gridEnabled = true)
                            showGridScaleEditor = false
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onGloballyPositioned {
                            gridScaleEditorBounds = Rect(it.positionInRoot(), it.size.toSize())
                        }
                )
            }
        }

        if (!dualJoysticksEnabled) {
            FloorLevelSwitcher(
                level = selectedFloor,
                onSelect = { floor ->
                    selectedFloor = floor
                },
                compact = true,
                modifier = Modifier
                    .then(
                        if (useBottomDockedFloorSwitcher) {
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 8.dp + leftControlsInset,
                                    bottom = floorSwitcherBottomPadding
                                )
                                .navigationBarsPadding()
                        } else {
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = floorSwitcherTopPadding)
                        }
                    )
                    .onGloballyPositioned {
                        floorSwitcherBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
            )
        }

        ParamsPanel(
            expanded = showParams,
            scope = currentScope,
            activeTool = tool,
            params = doc.params,
            takeoffSession = takeoffSession,
            snapThresholdFeet = snapSettings.thresholdFeet,
            useMetric = appSettings.useMetric,
            onSelectDrawWallTool = {
                pendingGrabSelection = false
                movingWallPreview = null
                resetCurveDraft()
                resetCircleDraft()
                drawingStart = null
                drawingPreview = null
                chainOrigin = null
                boxStart = null
                boxPreview = null
                boxRotationRadians = 0.0
                tool = BlueprintDraftTool.DRAW_WALL
            },
            onSelectDrawArcTool = {
                pendingGrabSelection = false
                movingWallPreview = null
                drawingStart = null
                drawingPreview = null
                chainOrigin = null
                boxStart = null
                boxPreview = null
                boxRotationRadians = 0.0
                resetCircleDraft()
                resetCurveDraft()
                tool = BlueprintDraftTool.DRAW_ARC
            },
            onSelectDrawCircleTool = {
                pendingGrabSelection = false
                movingWallPreview = null
                drawingStart = null
                drawingPreview = null
                chainOrigin = null
                boxStart = null
                boxPreview = null
                boxRotationRadians = 0.0
                resetCurveDraft()
                resetCircleDraft()
                tool = BlueprintDraftTool.DRAW_CIRCLE
            },
            onParamsChange = viewModel::updateParams,
            onWallHeightChange = viewModel::updateWallHeight,
            onDrywallSheetAreaChange = { value -> viewModel.updateDrywallSessionParams(sheetAreaSqFt = value) },
            onDrywallWasteChange = { value -> viewModel.updateDrywallSessionParams(wastePercent = value) },
            onDrywallScrewsChange = { value -> viewModel.updateDrywallSessionParams(screwsPerSheet = value) },
            onDrywallMudRateChange = { value -> viewModel.updateDrywallSessionParams(mudGallonsPer100SqFt = value) },
            onDrywallIncludeCeilingsChange = { value -> viewModel.updateDrywallSessionParams(includeCeilings = value) },
            onConcreteDepthFeetChange = { value -> viewModel.updateConcreteSessionParams(thicknessFeet = value) },
            onConcreteWasteChange = { value -> viewModel.updateConcreteSessionParams(wastePercent = value) },
            onGravelDepthFeetChange = { value -> viewModel.updateGravelSessionParams(depthFeet = value) },
            onGravelDensityChange = { value -> viewModel.updateGravelSessionParams(densityTonsPerYard = value) },
            onGravelWasteChange = { value -> viewModel.updateGravelSessionParams(wastePercent = value) },
            onPaintCoverageChange = { value -> viewModel.updatePaintSessionParams(coverageSqFtPerGallon = value) },
            onPaintCoatsChange = { value -> viewModel.updatePaintSessionParams(coats = value) },
            onPaintWasteChange = { value -> viewModel.updatePaintSessionParams(wastePercent = value) },
            onSnapThresholdFeetChange = { value ->
                val clamped = value.coerceIn(MIN_SNAP_THRESHOLD_FEET, MAX_SNAP_THRESHOLD_FEET)
                snapSettings = snapSettings.copy(thresholdFeet = clamped)
                settingsViewModel.updateBlueprintSnapDefaults(thresholdFeet = clamped)
            },
            onScopeExpand = viewModel::expandScopeWithPaint,
            onDetectRooms = viewModel::ensureRoomDetection,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = panelBottomPadding)
                .navigationBarsPadding()
                .onGloballyPositioned {
                    paramsPanelBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirm = false },
                title = { Text("Clear all blueprint items?") },
                text = { Text("This removes all walls, openings, and rooms. You can still undo if needed.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (
                                scopedDoc.walls.isNotEmpty() ||
                                scopedDoc.openings.isNotEmpty() ||
                                scopedDoc.rooms.isNotEmpty()
                            ) {
                                recordBlueprintMetric(BlueprintMetricAction.CLEAR_ALL)
                            }
                            viewModel.clearAllGeometry()
                            drawingStart = null
                            drawingPreview = null
                            chainOrigin = null
                            showClearAllConfirm = false
                        }
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        val clearOpeningPlacementTool = {
            pendingGrabSelection = false
            if (
                tool == BlueprintDraftTool.PLACE_DOOR ||
                tool == BlueprintDraftTool.PLACE_WINDOW ||
                tool == BlueprintDraftTool.PLACE_STAIR_UP ||
                tool == BlueprintDraftTool.PLACE_STAIR_DOWN
            ) {
                tool = BlueprintDraftTool.DRAW_WALL
            }
        }
        val toggleBoxMode = {
            pendingGrabSelection = false
            movingWallPreview = null
            drawingStart = null
            drawingPreview = null
            resetCurveDraft()
            resetCircleDraft()
            chainOrigin = null
            restartLineFromNearestWallStart = false
            boxStart = null
            boxPreview = null
            boxRotationRadians = 0.0
            activeOpeningPanel = null
            showParams = false
            showRailHelp = false
            tool = if (tool == BlueprintDraftTool.DRAW_BOX) {
                BlueprintDraftTool.DRAW_WALL
            } else {
                BlueprintDraftTool.DRAW_BOX
            }
        }
        val toggleCircleMode = {
            pendingGrabSelection = false
            movingWallPreview = null
            drawingStart = null
            drawingPreview = null
            resetCurveDraft()
            resetCircleDraft()
            chainOrigin = null
            restartLineFromNearestWallStart = false
            boxStart = null
            boxPreview = null
            boxRotationRadians = 0.0
            activeOpeningPanel = null
            showParams = false
            showRailHelp = false
            tool = if (tool == BlueprintDraftTool.DRAW_CIRCLE) {
                BlueprintDraftTool.DRAW_WALL
            } else {
                BlueprintDraftTool.DRAW_CIRCLE
            }
        }
        val toggleDoorsPanel = {
            pendingGrabSelection = false
            val opening = activeOpeningPanel != OpeningPanelType.DOORS
            activeOpeningPanel = if (opening) OpeningPanelType.DOORS else null
            if (opening) {
                tool = BlueprintDraftTool.PLACE_DOOR
                showParams = false
                showRailHelp = false
            } else if (tool == BlueprintDraftTool.PLACE_DOOR) {
                tool = BlueprintDraftTool.DRAW_WALL
            }
        }
        val toggleWindowsPanel = {
            pendingGrabSelection = false
            val opening = activeOpeningPanel != OpeningPanelType.WINDOWS
            activeOpeningPanel = if (opening) OpeningPanelType.WINDOWS else null
            if (opening) {
                tool = BlueprintDraftTool.PLACE_WINDOW
                showParams = false
                showRailHelp = false
            } else if (tool == BlueprintDraftTool.PLACE_WINDOW) {
                tool = BlueprintDraftTool.DRAW_WALL
            }
        }
        val toggleStairUpPanel = {
            pendingGrabSelection = false
            val opening = activeOpeningPanel != OpeningPanelType.STAIR_UP
            activeOpeningPanel = if (opening) OpeningPanelType.STAIR_UP else null
            if (opening) {
                tool = BlueprintDraftTool.PLACE_STAIR_UP
                showParams = false
                showRailHelp = false
            } else if (tool == BlueprintDraftTool.PLACE_STAIR_UP) {
                tool = BlueprintDraftTool.DRAW_WALL
            }
        }
        val toggleStairDownPanel = {
            pendingGrabSelection = false
            val opening = activeOpeningPanel != OpeningPanelType.STAIR_DOWN
            activeOpeningPanel = if (opening) OpeningPanelType.STAIR_DOWN else null
            if (opening) {
                tool = BlueprintDraftTool.PLACE_STAIR_DOWN
                showParams = false
                showRailHelp = false
            } else if (tool == BlueprintDraftTool.PLACE_STAIR_DOWN) {
                tool = BlueprintDraftTool.DRAW_WALL
            }
        }
        val toggleParamsPanel = {
            pendingGrabSelection = false
            val opening = !showParams
            showParams = opening
            if (opening) {
                activeOpeningPanel = null
                showRailHelp = false
                clearOpeningPlacementTool()
            }
        }

        BlueprintBottomBar(
            canDeleteSelection = selectedWall != null || selectedOpening != null,
            detachedWalls = detachedWalls,
            boxModeEnabled = tool == BlueprintDraftTool.DRAW_BOX,
            circleModeEnabled = tool == BlueprintDraftTool.DRAW_CIRCLE,
            activePanel = activeOpeningPanel,
            paramsExpanded = showParams,
            onToggleDetached = {
                detachedWalls = !detachedWalls
            },
            onToggleBoxMode = toggleBoxMode,
            onToggleCircleMode = toggleCircleMode,
            onDeleteSelection = {
                when {
                    selectedOpening != null -> viewModel.deleteSelectedOpening()
                    selectedWall != null -> viewModel.deleteSelectedWall()
                }
            },
            onToggleDoors = toggleDoorsPanel,
            onToggleWindows = toggleWindowsPanel,
            onToggleStairUp = toggleStairUpPanel,
            onToggleStairDown = toggleStairDownPanel,
            onToggleParams = toggleParamsPanel,
            showHelp = showRailHelp,
            onToggleHelp = {
                val opening = !showRailHelp
                showRailHelp = opening
                if (opening) {
                    showParams = false
                    activeOpeningPanel = null
                    clearOpeningPlacementTool()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 8.dp + centeredControlsInset,
                    end = 8.dp + centeredControlsInset,
                    top = 4.dp,
                    bottom = 8.dp
                )
                .navigationBarsPadding()
                .imePadding()
                .onGloballyPositioned {
                    bottomRailBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        if (dualJoysticksEnabled) {
            DualJoystickOverlay(
                leftVector = if (tutorialMode) tutorialLeftVector else leftJoystickVector,
                rightVector = if (tutorialMode) tutorialRightVector else rightJoystickVector,
                onLeftVectorChange = { leftJoystickVector = it },
                onRightVectorChange = { rightJoystickVector = it },
                onLeftPressChange = { rightJoystickPressed = it },
                onRightPressChange = {},
                onLeftTap = dispatchRightJoystickClick,
                onRightTap = dispatchLeftJoystickClick,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                canZoomIn = scale < MAX_BLUEPRINT_SCALE,
                canZoomOut = scale > MIN_BLUEPRINT_SCALE,
                onUndo = handleUndo,
                onRedo = handleRedo,
                onZoomIn = handleZoomIn,
                onZoomOut = handleZoomOut,
                controlStateLabel = controlStateLabel,
                belowHistoryContent = centerCoordinateOverlay,
                leftPadModifier = Modifier.onGloballyPositioned {
                    tutorialLeftControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                centerControlsModifier = Modifier.onGloballyPositioned {
                    tutorialCenterControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                rightPadModifier = Modifier.onGloballyPositioned {
                    tutorialRightControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp + centeredControlsInset,
                        end = 12.dp + centeredControlsInset,
                        bottom = sharedBottomControlsPadding
                    )
                    .navigationBarsPadding()
            )
        } else {
            TouchModeQuickToolsOverlay(
                selectedMode = when {
                    pendingGrabSelection || movingWallPreview != null -> TouchToolMode.GRAB
                    tool == BlueprintDraftTool.SELECT -> TouchToolMode.SELECT
                    else -> TouchToolMode.DRAW
                },
                onSelectMode = {
                    pendingGrabSelection = false
                    movingWallPreview = null
                    drawingStart = null
                    drawingPreview = null
                    chainOrigin = null
                    restartLineFromNearestWallStart = false
                    tool = BlueprintDraftTool.SELECT
                },
                onDrawMode = {
                    pendingGrabSelection = false
                    if (movingWallPreview != null) {
                        commitMovingWallPlacement()
                    } else {
                        drawingStart = null
                        drawingPreview = null
                        chainOrigin = null
                    }
                    restartLineFromNearestWallStart = false
                    tool = BlueprintDraftTool.DRAW_WALL
                },
                onGrabMode = {
                    drawingStart = null
                    drawingPreview = null
                    chainOrigin = null
                    restartLineFromNearestWallStart = false
                    val wallToGrab = selectedWall ?: uiState.selectedWallId?.let { id ->
                        renderedDoc.walls.firstOrNull { wall -> wall.id == id }
                    }
                    if (wallToGrab != null) {
                        movingWallPreview = wallToGrab
                        pendingGrabSelection = false
                        tool = BlueprintDraftTool.DRAW_WALL
                    } else {
                        movingWallPreview = null
                        pendingGrabSelection = true
                        tool = BlueprintDraftTool.SELECT
                    }
                },
                onCancel = {
                    when {
                        movingWallPreview != null -> {
                            cancelMovingWallPlacement()
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                        tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> {
                            drawingStart = null
                            drawingPreview = null
                            chainOrigin = null
                            restartLineFromNearestWallStart = false
                            pendingGrabSelection = false
                        }
                        tool == BlueprintDraftTool.DRAW_BOX -> {
                            drawingStart = null
                            drawingPreview = null
                            chainOrigin = null
                            restartLineFromNearestWallStart = false
                            boxStart = null
                            boxPreview = null
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                        tool == BlueprintDraftTool.DRAW_ARC -> {
                            resetCurveDraft()
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                        tool == BlueprintDraftTool.DRAW_CIRCLE -> {
                            resetCircleDraft()
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                        pendingGrabSelection -> {
                            pendingGrabSelection = false
                            tool = BlueprintDraftTool.DRAW_WALL
                        }
                        uiState.selectedOpeningId != null -> {
                            viewModel.selectOpening(null)
                        }
                        uiState.selectedWallId != null -> {
                            viewModel.selectWall(null)
                        }
                        tool == BlueprintDraftTool.PLACE_DOOR ||
                            tool == BlueprintDraftTool.PLACE_WINDOW ||
                            tool == BlueprintDraftTool.PLACE_STAIR_UP ||
                            tool == BlueprintDraftTool.PLACE_STAIR_DOWN -> {
                            clearOpeningPlacementTool()
                            pendingGrabSelection = false
                        }
                    }
                },
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                canZoomIn = scale < MAX_BLUEPRINT_SCALE,
                canZoomOut = scale > MIN_BLUEPRINT_SCALE,
                onUndo = handleUndo,
                onRedo = handleRedo,
                onZoomIn = handleZoomIn,
                onZoomOut = handleZoomOut,
                controlStateLabel = controlStateLabel,
                belowHistoryContent = centerCoordinateOverlay,
                leftToolsModifier = Modifier.onGloballyPositioned {
                    tutorialLeftControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                centerControlsModifier = Modifier.onGloballyPositioned {
                    tutorialCenterControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                rightToolsModifier = Modifier.onGloballyPositioned {
                    tutorialRightControlsBounds = Rect(it.positionInRoot(), it.size.toSize())
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp + centeredControlsInset,
                        end = 12.dp + centeredControlsInset,
                        bottom = sharedBottomControlsPadding
                    )
                    .navigationBarsPadding()
            )
        }
        if (showEdgeDials) {
            DrawLineEdgeDialsOverlay(
                onAngleTicks = onAngleDialTicks,
                onLengthTicks = onLengthDialTicks,
                onInteractionChanged = onEdgeDialInteractionChanged,
                dualJoysticksEnabled = dualJoysticksEnabled,
                controlsBottomPadding = sharedBottomControlsPadding,
                leftInset = leftControlsInset,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(6f)
            )
        }
        if (dualJoysticksEnabled) {
            GridScaleEditorPanel(
                expanded = showGridScaleEditor,
                useMetric = appSettings.useMetric,
                value = gridScaleInput,
                onValueChange = {
                    gridScaleInput = it
                    parsePrimeLengthToFeet(it)?.let(syncGridScaleAssistInputs)
                },
                feetValue = gridScaleFeetInput,
                inchesValue = gridScaleInchesInput,
                centimetersValue = gridScaleCentimetersInput,
                onFeetValueChange = { value ->
                    gridScaleFeetInput = value.filter { it.isDigit() }.take(3)
                    applyImperialAssistInputsToGridScaleInput()
                },
                onInchesValueChange = { value ->
                    gridScaleInchesInput = value.filter { it.isDigit() }.take(3)
                    applyImperialAssistInputsToGridScaleInput()
                },
                onCentimetersValueChange = { value ->
                    gridScaleCentimetersInput = value.filter { it.isDigit() || it == '.' }.take(8)
                    applyMetricAssistInputToGridScaleInput()
                },
                onNudgeInches = nudgeGridScaleInches,
                onNudgeCentimeters = nudgeGridScaleCentimeters,
                onDismiss = { showGridScaleEditor = false },
                onApply = {
                    val parsedFeet = parsePrimeLengthToFeet(gridScaleInput)
                    if (parsedFeet != null) {
                        val clampedFeet = parsedFeet.coerceIn(gridScaleMinFeet, gridScaleMaxFeet)
                        snapSettings = snapSettings.copy(
                            gridEnabled = true,
                            gridStepFeet = clampedFeet
                        )
                        syncGridScaleAssistInputs(clampedFeet)
                        settingsViewModel.updateBlueprintSnapDefaults(gridEnabled = true)
                        showGridScaleEditor = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = sharedBottomControlsPadding + 122.dp)
                    .navigationBarsPadding()
                    .onGloballyPositioned {
                        gridScaleEditorBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
            )
        }

        RailHelpPanel(
            expanded = showRailHelp,
            dualJoysticksEnabled = dualJoysticksEnabled,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = helpBottomPadding)
                .navigationBarsPadding()
                .onGloballyPositioned {
                    railHelpBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )
        if (tutorialMode && currentTutorialStep != null) {
            BlueprintControlTutorialOverlay(
                modeLabel = tutorialModeLabel,
                step = currentTutorialStep,
                stepIndex = tutorialStepIndex,
                totalSteps = controlTutorialSteps.size,
                targetBounds = tutorialTargetBounds,
                onBack = {
                    tutorialStepIndex = (tutorialStepIndex - 1).coerceAtLeast(0)
                },
                onNext = {
                    if (tutorialStepIndex >= controlTutorialSteps.lastIndex) {
                        onExitTutorialMode()
                    } else {
                        tutorialStepIndex += 1
                    }
                },
                onSkip = onExitTutorialMode,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
            )
        }
    }
    onFullscreenBlueprintChanged(true)
}

internal fun screenPointToWorldPoint(
    rootPoint: Offset,
    canvasRoot: Offset,
    canvasSize: Size,
    scale: Float,
    pan: Offset
): PointMm? {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return null
    val local = rootPoint - canvasRoot
    if (local.x < 0f || local.y < 0f || local.x > canvasSize.width || local.y > canvasSize.height) {
        return null
    }
    val ppm = BASE_PX_PER_MM * scale
    return PointMm(
        x = ((local.x - canvasSize.width / 2f - pan.x) / ppm).roundToLong(),
        y = (-(local.y - canvasSize.height / 2f - pan.y) / ppm).roundToLong()
    )
}

internal fun worldPointToCanvasLocal(
    worldPoint: PointMm,
    canvasSize: Size,
    scale: Float,
    pan: Offset
): Offset? {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return null
    val ppm = BASE_PX_PER_MM * scale
    return Offset(
        x = (canvasSize.width / 2f + pan.x + (worldPoint.x * ppm)).coerceIn(0f, canvasSize.width),
        y = (canvasSize.height / 2f + pan.y - (worldPoint.y * ppm)).coerceIn(0f, canvasSize.height)
    )
}

internal fun parsePrimeLengthToFeet(input: String): Double? {
    val normalized = input
        .trim()
        .replace("''", "\"")
        .replace("’", "'")
        .replace("“", "\"")
        .replace("”", "\"")
    return DimensionParser.parseLengthToFeet(normalized)
}

internal fun formatLiveValue(value: Double, decimals: Int): String {
    val safe = if (value.isFinite()) value else 0.0
    return when (decimals.coerceIn(0, 3)) {
        0 -> "%.0f".format(safe)
        1 -> "%.1f".format(safe)
        2 -> "%.2f".format(safe)
        else -> "%.3f".format(safe)
    }
}

internal fun formatFeetInchesPrime(feet: Double): String {
    val totalInches = (feet.coerceAtLeast(0.0) * 12.0).roundToLong()
    val wholeFeet = totalInches / 12L
    val inches = totalInches % 12L
    return if (inches == 0L) {
        "${wholeFeet}'"
    } else {
        "${wholeFeet}' ${inches}\""
    }
}

internal fun formatSignedFeetInchesPrime(mm: Long): String {
    val sign = when {
        mm > 0L -> "+"
        mm < 0L -> "-"
        else -> ""
    }
    return sign + formatFeetInchesPrime(Millimeters(abs(mm)).toFeet())
}

internal fun formatScaleCentimetersValue(centimeters: Double): String {
    val safe = if (centimeters.isFinite()) centimeters.coerceAtLeast(0.0) else 0.0
    val whole = safe.roundToInt().toDouble()
    return if (abs(safe - whole) <= 0.05) {
        whole.toInt().toString()
    } else {
        formatLiveValue(safe, 1)
    }
}

internal fun formatGridScaleLabel(gridStepFeet: Double, useMetric: Boolean): String {
    if (!useMetric) return formatFeetInchesPrime(gridStepFeet)
    val centimeters = Millimeters.fromFeet(gridStepFeet).value / 10.0
    return "${formatScaleCentimetersValue(centimeters)} cm"
}

internal fun formatLengthDisplay(feet: Double, useMetric: Boolean): String {
    if (!useMetric) return formatFeetInchesPrime(feet)
    return formatLengthDisplay(mm = Millimeters.fromFeet(feet).value, useMetric = true)
}

internal fun formatLengthDisplay(mm: Long, useMetric: Boolean): String {
    if (!useMetric) return formatFeetInchesPrime(Millimeters(mm.coerceAtLeast(0L)).toFeet())
    val safeMm = mm.coerceAtLeast(0L)
    return if (safeMm >= 1000L) {
        "${formatLiveValue(safeMm / 1000.0, 2)} m"
    } else {
        "${formatLiveValue(safeMm / 10.0, 1)} cm"
    }
}

internal fun formatSignedLengthDisplay(mm: Long, useMetric: Boolean): String {
    if (!useMetric) return formatSignedFeetInchesPrime(mm)
    val sign = when {
        mm > 0L -> "+"
        mm < 0L -> "-"
        else -> ""
    }
    val absMm = abs(mm)
    val value = if (absMm >= 1000L) {
        "${formatLiveValue(absMm / 1000.0, 2)} m"
    } else {
        "${formatLiveValue(absMm / 10.0, 1)} cm"
    }
    return sign + value
}

internal fun defaultDraftDialLengthMm(scale: Float): Double {
    val clampedScale = scale.coerceIn(MIN_BLUEPRINT_SCALE, MAX_BLUEPRINT_SCALE)
    val visibleLengthMm = (MIN_DRAW_WALL_SCREEN_PX / (BASE_PX_PER_MM * clampedScale)).toDouble()
    return visibleLengthMm
        .coerceAtLeast(MIN_DRAW_WALL_LENGTH_MM.toDouble())
        .coerceAtMost(DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble())
}

internal fun rotateDraftPreviewByDialTicks(
    start: PointMm,
    currentPreview: PointMm,
    tickCount: Int,
    scale: Float
): PointMm {
    if (tickCount == 0) return currentPreview
    val dx = (currentPreview.x - start.x).toDouble()
    val dy = (currentPreview.y - start.y).toDouble()
    val currentLengthMm = hypot(dx, dy)
    val baseAngleRadians = if (currentLengthMm <= 0.0001) {
        0.0
    } else {
        atan2(dy, dx)
    }
    val lengthMm = if (currentLengthMm <= 0.0001) {
        defaultDraftDialLengthMm(scale)
    } else {
        currentLengthMm.coerceAtMost(DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble())
    }
    val angleDelta = Math.toRadians(tickCount * DRAW_EDGE_DIAL_ANGLE_STEP_DEGREES)
    val nextAngleRadians = baseAngleRadians + angleDelta
    return pointFromPolarDraft(
        start = start,
        angleRadians = nextAngleRadians,
        lengthMm = lengthMm
    )
}

internal fun resizeDraftPreviewByDialTicks(
    start: PointMm,
    currentPreview: PointMm,
    tickCount: Int,
    scale: Float
): PointMm {
    if (tickCount == 0) return currentPreview
    val dx = (currentPreview.x - start.x).toDouble()
    val dy = (currentPreview.y - start.y).toDouble()
    val currentLengthMm = hypot(dx, dy)
    val baseAngleRadians = if (currentLengthMm <= 0.0001) {
        0.0
    } else {
        atan2(dy, dx)
    }
    val baseLengthMm = if (currentLengthMm <= 0.0001) {
        defaultDraftDialLengthMm(scale)
    } else {
        currentLengthMm
    }
    val nextLengthMm = (baseLengthMm + (tickCount * DRAW_EDGE_DIAL_LENGTH_STEP_MM))
        .coerceIn(
            MIN_DRAW_WALL_LENGTH_MM.toDouble(),
            DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble()
        )
    return pointFromPolarDraft(
        start = start,
        angleRadians = baseAngleRadians,
        lengthMm = nextLengthMm
    )
}

internal data class DraftBoxRotationResult(
    val preview: PointMm,
    val rotationRadians: Double
)

internal fun projectDraftBoxDimensions(
    start: PointMm,
    oppositeCorner: PointMm,
    rotationRadians: Double
): Pair<Double, Double> {
    val dx = (oppositeCorner.x - start.x).toDouble()
    val dy = (oppositeCorner.y - start.y).toDouble()
    val cosTheta = cos(rotationRadians)
    val sinTheta = sin(rotationRadians)
    val widthMm = (dx * cosTheta) + (dy * sinTheta)
    val heightMm = (-dx * sinTheta) + (dy * cosTheta)
    return widthMm to heightMm
}

internal fun pointFromDraftBoxDimensions(
    origin: PointMm,
    rotationRadians: Double,
    widthMm: Double,
    heightMm: Double
): PointMm {
    val cosTheta = cos(rotationRadians)
    val sinTheta = sin(rotationRadians)
    val x = origin.x + (widthMm * cosTheta) + (heightMm * -sinTheta)
    val y = origin.y + (widthMm * sinTheta) + (heightMm * cosTheta)
    return PointMm(x = x.roundToLong(), y = y.roundToLong())
}

internal fun rotateDraftBoxPreviewByDialTicks(
    start: PointMm,
    currentPreview: PointMm,
    currentRotationRadians: Double,
    tickCount: Int
): DraftBoxRotationResult {
    if (tickCount == 0) {
        return DraftBoxRotationResult(preview = currentPreview, rotationRadians = currentRotationRadians)
    }
    val (widthMm, heightMm) = projectDraftBoxDimensions(
        start = start,
        oppositeCorner = currentPreview,
        rotationRadians = currentRotationRadians
    )
    val nextRotation = currentRotationRadians + Math.toRadians(tickCount * DRAW_EDGE_DIAL_ANGLE_STEP_DEGREES)
    val nextPreview = pointFromDraftBoxDimensions(
        origin = start,
        rotationRadians = nextRotation,
        widthMm = widthMm,
        heightMm = heightMm
    )
    return DraftBoxRotationResult(preview = nextPreview, rotationRadians = nextRotation)
}

internal fun resizeDraftBoxPreviewByDialTicks(
    start: PointMm,
    currentPreview: PointMm,
    currentRotationRadians: Double,
    tickCount: Int,
    scale: Float
): PointMm {
    if (tickCount == 0) return currentPreview
    var (widthMm, heightMm) = projectDraftBoxDimensions(
        start = start,
        oppositeCorner = currentPreview,
        rotationRadians = currentRotationRadians
    )
    if (abs(widthMm) <= 0.0001 && abs(heightMm) <= 0.0001) {
        val seedLength = defaultDraftDialLengthMm(scale)
        widthMm = seedLength
        heightMm = seedLength
    }
    val deltaMm = tickCount * DRAW_EDGE_DIAL_LENGTH_STEP_MM.toDouble()
    val widthSign = if (widthMm < 0.0) -1.0 else 1.0
    val heightSign = if (heightMm < 0.0) -1.0 else 1.0
    val nextWidthAbs = (abs(widthMm) + deltaMm)
        .coerceIn(
            MIN_DRAW_WALL_LENGTH_MM.toDouble(),
            DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble()
        )
    val nextHeightAbs = (abs(heightMm) + deltaMm)
        .coerceIn(
            MIN_DRAW_WALL_LENGTH_MM.toDouble(),
            DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble()
        )
    return pointFromDraftBoxDimensions(
        origin = start,
        rotationRadians = currentRotationRadians,
        widthMm = widthSign * nextWidthAbs,
        heightMm = heightSign * nextHeightAbs
    )
}

internal fun draftBoxCorners(
    start: PointMm,
    end: PointMm,
    rotationRadians: Double
): List<PointMm> {
    val (widthMm, heightMm) = projectDraftBoxDimensions(
        start = start,
        oppositeCorner = end,
        rotationRadians = rotationRadians
    )
    if (
        abs(widthMm) < MIN_DRAW_WALL_LENGTH_MM.toDouble() ||
            abs(heightMm) < MIN_DRAW_WALL_LENGTH_MM.toDouble()
    ) {
        return emptyList()
    }
    val cornerA = start
    val cornerB = pointFromDraftBoxDimensions(
        origin = cornerA,
        rotationRadians = rotationRadians,
        widthMm = widthMm,
        heightMm = 0.0
    )
    val cornerD = pointFromDraftBoxDimensions(
        origin = cornerA,
        rotationRadians = rotationRadians,
        widthMm = 0.0,
        heightMm = heightMm
    )
    val cornerC = pointFromDraftBoxDimensions(
        origin = cornerA,
        rotationRadians = rotationRadians,
        widthMm = widthMm,
        heightMm = heightMm
    )
    return listOf(cornerA, cornerB, cornerC, cornerD)
}

internal fun buildDraftBoxPreviewWalls(
    start: PointMm,
    end: PointMm,
    rotationRadians: Double
): List<WallSegment> {
    val corners = draftBoxCorners(
        start = start,
        end = end,
        rotationRadians = rotationRadians
    )
    if (corners.size != 4) return emptyList()
    val (cornerA, cornerB, cornerC, cornerD) = corners
    return listOf(
        WallSegment(id = "__preview_box_wall_1__", start = cornerA, end = cornerB),
        WallSegment(id = "__preview_box_wall_2__", start = cornerB, end = cornerC),
        WallSegment(id = "__preview_box_wall_3__", start = cornerC, end = cornerD),
        WallSegment(id = "__preview_box_wall_4__", start = cornerD, end = cornerA)
    )
}

internal const val CURVE_GROUP_TAG_PREFIX = "curve_group:"
internal const val CURVE_SHAPE_ARC_TAG = "curve_shape:arc"
internal const val CURVE_SHAPE_CIRCLE_TAG = "curve_shape:circle"

private const val MIN_GENERATED_CURVE_SEGMENT_MM = 35L
private const val ARC_PREVIEW_TARGET_SEGMENT_MM = 240.0
private const val ARC_COMMIT_TARGET_SEGMENT_MM = 420.0
private const val CIRCLE_PREVIEW_TARGET_SEGMENT_MM = 210.0
private const val CIRCLE_COMMIT_TARGET_SEGMENT_MM = 320.0
private const val MIN_CIRCLE_SEGMENTS = 12
private const val MAX_PREVIEW_CURVE_SEGMENTS = 40
private const val MAX_COMMIT_CURVE_SEGMENTS = 28

internal fun midpointBetween(a: PointMm, b: PointMm): PointMm {
    return PointMm(
        x = ((a.x + b.x) / 2.0).roundToLong(),
        y = ((a.y + b.y) / 2.0).roundToLong()
    )
}

internal fun buildDraftArcPreviewWalls(
    start: PointMm,
    end: PointMm?,
    previewPoint: PointMm
): List<WallSegment> {
    val previewPoints = if (end == null) {
        listOf(start, previewPoint)
    } else {
        buildQuadraticBezierPointChain(
            start = start,
            control = previewPoint,
            end = end,
            segmentCount = estimateCurveSegmentCount(
                curveLengthMm = approximateArcLengthMillimeters(start, end, previewPoint),
                targetSegmentMm = ARC_PREVIEW_TARGET_SEGMENT_MM,
                minSegments = 2,
                maxSegments = MAX_PREVIEW_CURVE_SEGMENTS
            )
        )
    }
    return buildPreviewWallsFromPointChain(
        idPrefix = "arc",
        points = previewPoints,
        closed = false
    )
}

internal fun buildDraftArcWalls(
    document: BlueprintDocument,
    start: PointMm,
    end: PointMm,
    control: PointMm,
    scale: Float,
    wallHeightMm: Long,
    wallThicknessMm: Long,
    tags: Set<String>
): List<WallSegment> {
    val pointChain = buildQuadraticBezierPointChain(
        start = start,
        control = control,
        end = end,
        segmentCount = estimateCurveSegmentCount(
            curveLengthMm = approximateArcLengthMillimeters(start, end, control),
            targetSegmentMm = ARC_COMMIT_TARGET_SEGMENT_MM,
            minSegments = 2,
            maxSegments = MAX_COMMIT_CURVE_SEGMENTS
        )
    )
    return buildCommittedCurveWallsFromPointChain(
        document = document,
        points = pointChain,
        closed = false,
        scale = scale,
        wallHeightMm = wallHeightMm,
        wallThicknessMm = wallThicknessMm,
        tags = tags + "${CURVE_GROUP_TAG_PREFIX}${UUID.randomUUID()}"
    )
}

internal fun buildDraftCirclePreviewWalls(
    center: PointMm,
    edge: PointMm
): List<WallSegment> {
    val pointChain = buildCirclePointChain(
        center = center,
        edge = edge,
        segmentCount = estimateCircleSegmentCount(
            curveLengthMm = approximateCircleCircumferenceMillimeters(center, edge),
            targetSegmentMm = CIRCLE_PREVIEW_TARGET_SEGMENT_MM,
            maxSegments = MAX_PREVIEW_CURVE_SEGMENTS
        )
    )
    return buildPreviewWallsFromPointChain(
        idPrefix = "circle",
        points = pointChain,
        closed = true
    )
}

internal fun buildDraftCircleWalls(
    document: BlueprintDocument,
    center: PointMm,
    edge: PointMm,
    scale: Float,
    wallHeightMm: Long,
    wallThicknessMm: Long,
    tags: Set<String>
): List<WallSegment> {
    val pointChain = buildCirclePointChain(
        center = center,
        edge = edge,
        segmentCount = estimateCircleSegmentCount(
            curveLengthMm = approximateCircleCircumferenceMillimeters(center, edge),
            targetSegmentMm = CIRCLE_COMMIT_TARGET_SEGMENT_MM,
            maxSegments = MAX_COMMIT_CURVE_SEGMENTS
        )
    )
    return buildCommittedCurveWallsFromPointChain(
        document = document,
        points = pointChain,
        closed = true,
        scale = scale,
        wallHeightMm = wallHeightMm,
        wallThicknessMm = wallThicknessMm,
        tags = tags + "${CURVE_GROUP_TAG_PREFIX}${UUID.randomUUID()}"
    )
}

private fun buildQuadraticBezierPointChain(
    start: PointMm,
    control: PointMm,
    end: PointMm,
    segmentCount: Int
): List<PointMm> {
    val safeSegments = segmentCount.coerceIn(1, MAX_PREVIEW_CURVE_SEGMENTS)
    return (0..safeSegments).map { index ->
        val t = index.toDouble() / safeSegments.toDouble()
        val inverseT = 1.0 - t
        PointMm(
            x = (
                (inverseT * inverseT * start.x.toDouble()) +
                    (2.0 * inverseT * t * control.x.toDouble()) +
                    (t * t * end.x.toDouble())
                ).roundToLong(),
            y = (
                (inverseT * inverseT * start.y.toDouble()) +
                    (2.0 * inverseT * t * control.y.toDouble()) +
                    (t * t * end.y.toDouble())
                ).roundToLong()
        )
    }
}

private fun buildCirclePointChain(
    center: PointMm,
    edge: PointMm,
    segmentCount: Int,
    snapSegments: Boolean = true
): List<PointMm> {
    val radiusMm = hypot(
        (edge.x - center.x).toDouble(),
        (edge.y - center.y).toDouble()
    )
    if (radiusMm < MIN_GENERATED_CURVE_SEGMENT_MM.toDouble()) return emptyList()
    val startAngleRadians = Math.PI / 2.0
    val safeSegments = if (snapSegments) {
        snapCircleSegmentCount(segmentCount, MAX_PREVIEW_CURVE_SEGMENTS)
    } else {
        segmentCount.coerceIn(6, MAX_PREVIEW_CURVE_SEGMENTS)
    }
    return (0 until safeSegments).map { index ->
        val angle = startAngleRadians + ((Math.PI * 2.0 * index) / safeSegments.toDouble())
        PointMm(
            x = (center.x + (cos(angle) * radiusMm)).roundToLong(),
            y = (center.y + (sin(angle) * radiusMm)).roundToLong()
        )
    }
}

private fun circleSelectionInfo(
    document: BlueprintDocument,
    selectedWallId: String
): CircleSelectionInfo? {
    val selectedWall = document.walls.firstOrNull { wall -> wall.id == selectedWallId } ?: return null
    if (CURVE_SHAPE_CIRCLE_TAG !in selectedWall.tags) return null
    val groupTag = selectedWall.curveGroupTag() ?: return null
    val groupWalls = document.walls.filter { wall -> wall.curveGroupTag() == groupTag }
    if (groupWalls.size < 3) return null
    val center = estimateCircleCenter(groupWalls)
    val radiusMm = estimateCircleRadius(groupWalls, center)
    return CircleSelectionInfo(
        radiusMm = radiusMm,
        diameterMm = radiusMm * 2L,
        segmentCount = groupWalls.size
    )
}

private fun resizeSelectedCircle(
    document: BlueprintDocument,
    selectedWallId: String,
    radiusDeltaMm: Long
): List<WallSegment>? {
    val selectedWall = document.walls.firstOrNull { wall -> wall.id == selectedWallId } ?: return null
    if (CURVE_SHAPE_CIRCLE_TAG !in selectedWall.tags) return null
    val groupTag = selectedWall.curveGroupTag() ?: return null
    val groupWalls = document.walls.filter { wall -> wall.curveGroupTag() == groupTag }
    if (groupWalls.size < 3) return null
    val center = estimateCircleCenter(groupWalls)
    val currentRadiusMm = estimateCircleRadius(groupWalls, center)
    val nextRadiusMm = (currentRadiusMm + radiusDeltaMm)
        .coerceAtLeast(MIN_GENERATED_CURVE_SEGMENT_MM * 4L)
    val orderedGroupWalls = groupWalls.sortedBy { wall ->
        circlePointOrderRadians(
            point = wall.start,
            center = center
        )
    }
    val updatedPoints = buildCirclePointChain(
        center = center,
        edge = PointMm(center.x, center.y + nextRadiusMm),
        segmentCount = orderedGroupWalls.size,
        snapSegments = false
    )
    val normalizedPoints = normalizeCurvePointChain(updatedPoints)
    if (normalizedPoints.size != orderedGroupWalls.size) return null
    val replacements = orderedGroupWalls.mapIndexed { index, wall ->
        wall.copy(
            start = normalizedPoints[index],
            end = normalizedPoints[(index + 1) % normalizedPoints.size]
        )
    }.associateBy { wall -> wall.id }
    return document.walls.map { wall ->
        replacements[wall.id] ?: wall
    }
}

private fun estimateCircleCenter(
    walls: List<WallSegment>
): PointMm {
    val points = walls.map { wall -> wall.start }
    val averageX = points.map { point -> point.x.toDouble() }.average().roundToLong()
    val averageY = points.map { point -> point.y.toDouble() }.average().roundToLong()
    return PointMm(averageX, averageY)
}

private fun estimateCircleRadius(
    walls: List<WallSegment>,
    center: PointMm
): Long {
    val points = walls.map { wall -> wall.start }
    return points.map { point ->
        hypot(
            (point.x - center.x).toDouble(),
            (point.y - center.y).toDouble()
        )
    }.average().roundToLong()
}

private fun circlePointOrderRadians(
    point: PointMm,
    center: PointMm
): Double {
    val angle = atan2(
        (point.y - center.y).toDouble(),
        (point.x - center.x).toDouble()
    )
    var relative = angle - (Math.PI / 2.0)
    while (relative < 0.0) relative += Math.PI * 2.0
    while (relative >= Math.PI * 2.0) relative -= Math.PI * 2.0
    return relative
}

private fun approximateArcLengthMillimeters(
    start: PointMm,
    end: PointMm,
    control: PointMm
): Double {
    val chord = hypot(
        (end.x - start.x).toDouble(),
        (end.y - start.y).toDouble()
    )
    val handleA = hypot(
        (control.x - start.x).toDouble(),
        (control.y - start.y).toDouble()
    )
    val handleB = hypot(
        (end.x - control.x).toDouble(),
        (end.y - control.y).toDouble()
    )
    return maxOf(chord, (handleA + handleB + chord) / 2.0)
}

private fun approximateCircleCircumferenceMillimeters(
    center: PointMm,
    edge: PointMm
): Double {
    val radiusMm = hypot(
        (edge.x - center.x).toDouble(),
        (edge.y - center.y).toDouble()
    )
    return radiusMm * Math.PI * 2.0
}

private fun estimateCurveSegmentCount(
    curveLengthMm: Double,
    targetSegmentMm: Double,
    minSegments: Int,
    maxSegments: Int
): Int {
    if (curveLengthMm <= 0.0) return minSegments
    return (curveLengthMm / targetSegmentMm)
        .roundToInt()
        .coerceIn(minSegments, maxSegments)
}

private fun estimateCircleSegmentCount(
    curveLengthMm: Double,
    targetSegmentMm: Double,
    maxSegments: Int
): Int {
    val estimated = estimateCurveSegmentCount(
        curveLengthMm = curveLengthMm,
        targetSegmentMm = targetSegmentMm,
        minSegments = MIN_CIRCLE_SEGMENTS,
        maxSegments = maxSegments
    )
    return snapCircleSegmentCount(estimated, maxSegments)
}

private fun snapCircleSegmentCount(
    segmentCount: Int,
    maxSegments: Int
): Int {
    val safeMax = (maxSegments / 4).coerceAtLeast(MIN_CIRCLE_SEGMENTS / 4) * 4
    val clamped = segmentCount.coerceIn(MIN_CIRCLE_SEGMENTS, safeMax)
    return ((clamped + 2) / 4) * 4
}

private fun buildPreviewWallsFromPointChain(
    idPrefix: String,
    points: List<PointMm>,
    closed: Boolean
): List<WallSegment> {
    val normalizedPoints = normalizeCurvePointChain(points)
    if (normalizedPoints.size < if (closed) 3 else 2) return emptyList()
    val segmentCount = if (closed) normalizedPoints.size else normalizedPoints.lastIndex
    return buildList {
        for (index in 0 until segmentCount) {
            val start = normalizedPoints[index]
            val end = normalizedPoints[(index + 1) % normalizedPoints.size]
            if (pointsNear(start, end)) continue
            add(
                WallSegment(
                    id = "__preview_${idPrefix}_wall_${index + 1}__",
                    start = start,
                    end = end
                )
            )
        }
    }
}

private fun buildCommittedCurveWallsFromPointChain(
    document: BlueprintDocument,
    points: List<PointMm>,
    closed: Boolean,
    scale: Float,
    wallHeightMm: Long,
    wallThicknessMm: Long,
    tags: Set<String>
): List<WallSegment> {
    val normalizedPoints = normalizeCurvePointChain(points)
    if (normalizedPoints.size < if (closed) 3 else 2) return emptyList()
    val segmentCount = if (closed) normalizedPoints.size else normalizedPoints.lastIndex
    val safeScale = scale.coerceIn(MIN_BLUEPRINT_SCALE, MAX_BLUEPRINT_SCALE)
    var workingDocument = document
    val acceptedWalls = mutableListOf<WallSegment>()
    for (index in 0 until segmentCount) {
        val start = normalizedPoints[index]
        val end = normalizedPoints[(index + 1) % normalizedPoints.size]
        if (!canAddGeneratedCurveWall(workingDocument, start, end, safeScale)) continue
        val wall = WallSegment(
            id = UUID.randomUUID().toString(),
            start = start,
            end = end,
            height = Millimeters(wallHeightMm),
            thickness = Millimeters(wallThicknessMm),
            tags = tags
        )
        acceptedWalls += wall
        workingDocument = workingDocument.copy(walls = workingDocument.walls + wall)
    }
    return acceptedWalls
}

private fun normalizeCurvePointChain(points: List<PointMm>): List<PointMm> {
    if (points.isEmpty()) return emptyList()
    val normalized = mutableListOf<PointMm>()
    points.forEach { point ->
        if (normalized.lastOrNull()?.let { pointsNear(it, point) } != true) {
            normalized += point
        }
    }
    if (
        normalized.size > 1 &&
            normalized.firstOrNull()?.let { first ->
                normalized.lastOrNull()?.let { last ->
                    pointsNear(first, last)
                }
            } == true
    ) {
        normalized.removeAt(normalized.lastIndex)
    }
    return normalized
}

private fun canAddGeneratedCurveWall(
    document: BlueprintDocument,
    start: PointMm,
    end: PointMm,
    scale: Float
): Boolean {
    val lengthMm = BlueprintSnapMath.distanceMillimeters(start, end)
    if (lengthMm < MIN_GENERATED_CURVE_SEGMENT_MM) return false
    val visibleLengthPx = lengthMm * BASE_PX_PER_MM * scale
    if (visibleLengthPx < 1.0f) return false
    return document.walls.none { wall ->
        wallMatchesEndpoints(wall, start, end)
    }
}

internal fun WallSegment.curveGroupTag(): String? {
    return tags.firstOrNull { tag -> tag.startsWith(CURVE_GROUP_TAG_PREFIX) }
}

internal fun WallSegment.sameCurveGroupAs(other: WallSegment): Boolean {
    val curveGroup = curveGroupTag() ?: return false
    return curveGroup == other.curveGroupTag()
}

internal fun pointFromPolarDraft(
    start: PointMm,
    angleRadians: Double,
    lengthMm: Double
): PointMm {
    return PointMm(
        x = (start.x + (cos(angleRadians) * lengthMm)).roundToLong(),
        y = (start.y + (sin(angleRadians) * lengthMm)).roundToLong()
    )
}

internal fun resolveDraftStartFromTap(
    tap: PointMm,
    snappedTap: PointMm,
    walls: List<WallSegment>,
    scale: Float,
    snapThresholdFeet: Double,
    endpointSnappingEnabled: Boolean,
    preferWallProjection: Boolean = false
): PointMm {
    if (walls.isEmpty()) return snappedTap

    if (preferWallProjection) {
        val nearestWall = walls
            .map { wall -> wall to BlueprintSnapMath.pointToWallDistanceMm(tap, wall) }
            .minByOrNull { it.second }
            ?.first
        if (nearestWall != null) {
            val t = BlueprintSnapMath.projectToWallT(tap, nearestWall).coerceIn(0.0, 1.0)
            return BlueprintSnapMath.pointOnWall(nearestWall, t)
        }
    }

    val thresholdMm = Millimeters.fromFeet(snapThresholdFeet).value.coerceAtLeast(1L)
    if (endpointSnappingEnabled) {
        val endpointSnapMm = (thresholdMm * 2L).coerceAtLeast(35L)
        val nearestEndpoint = walls.asSequence()
            .flatMap { wall -> sequenceOf(wall.start, wall.end) }
            .map { endpoint -> endpoint to BlueprintSnapMath.distanceMillimeters(tap, endpoint) }
            .minByOrNull { it.second }
        if (nearestEndpoint != null && nearestEndpoint.second <= endpointSnapMm) {
            return nearestEndpoint.first
        }
    }

    val tapRadiusMm = maxOf(
        thresholdMm,
        (44f / (BASE_PX_PER_MM * scale.coerceIn(MIN_BLUEPRINT_SCALE, MAX_BLUEPRINT_SCALE))).roundToLong()
    )
    val nearestWall = walls
        .map { wall -> wall to BlueprintSnapMath.pointToWallDistanceMm(tap, wall) }
        .minByOrNull { it.second }
        ?: return snappedTap
    if (nearestWall.second > tapRadiusMm) {
        return snappedTap
    }
    val wall = nearestWall.first
    val t = BlueprintSnapMath.projectToWallT(tap, wall).coerceIn(0.0, 1.0)
    return BlueprintSnapMath.pointOnWall(wall, t)
}

internal fun snapToNearestWallEndpoint(
    candidate: PointMm,
    walls: List<WallSegment>,
    thresholdMm: Long
): PointMm {
    if (walls.isEmpty() || thresholdMm <= 0L) return candidate
    val nearest = walls.asSequence()
        .flatMap { wall -> sequenceOf(wall.start, wall.end) }
        .map { endpoint -> endpoint to BlueprintSnapMath.distanceMillimeters(candidate, endpoint) }
        .minByOrNull { it.second }
        ?: return candidate
    return if (nearest.second <= thresholdMm) nearest.first else candidate
}

internal fun isDraftEndpointIntersectingExistingGeometry(
    drawingStart: PointMm,
    previewEnd: PointMm,
    walls: List<WallSegment>,
    chainOrigin: PointMm?,
    thresholdMm: Long
): Boolean {
    if (thresholdMm <= 0L) return false
    if (BlueprintSnapMath.distanceMillimeters(drawingStart, previewEnd) < MIN_DRAW_WALL_LENGTH_MM) {
        return false
    }

    val tolerance = thresholdMm.coerceAtLeast(WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM)
    val startGuard = (tolerance / 2L).coerceAtLeast(1L)

    if (
        chainOrigin != null &&
            BlueprintSnapMath.distanceMillimeters(previewEnd, chainOrigin) <= tolerance &&
            BlueprintSnapMath.distanceMillimeters(drawingStart, chainOrigin) > startGuard
    ) {
        return true
    }
    if (walls.isEmpty()) return false

    return walls.any { wall ->
        sequenceOf(wall.start, wall.end).any { endpoint ->
            BlueprintSnapMath.distanceMillimeters(previewEnd, endpoint) <= tolerance &&
                BlueprintSnapMath.distanceMillimeters(drawingStart, endpoint) > startGuard
        } || run {
            val projected = BlueprintSnapMath.pointOnWall(
                wall,
                BlueprintSnapMath.projectToWallT(previewEnd, wall).coerceIn(0.0, 1.0)
            )
            BlueprintSnapMath.distanceMillimeters(previewEnd, projected) <= tolerance &&
                BlueprintSnapMath.distanceMillimeters(drawingStart, projected) > startGuard
        }
    }
}

internal fun resolveDraftWallCommitEnd(
    previewEnd: PointMm?,
    snappedTap: PointMm,
    walls: List<WallSegment>,
    snapThresholdFeet: Double,
    endpointSnappingEnabled: Boolean,
    closureEnabled: Boolean,
    chainOrigin: PointMm?
): PointMm {
    previewEnd?.let { return it }

    var end = snappedTap
    if (endpointSnappingEnabled) {
        end = snapToNearestWallEndpoint(
            candidate = end,
            walls = walls,
            thresholdMm = (Millimeters.fromFeet(snapThresholdFeet).value * 3L / 2L)
                .coerceAtLeast(1L)
        )
    }
    if (closureEnabled) {
        chainOrigin?.let { origin ->
            BlueprintSnapMath.roomClosureSnap(
                candidateEnd = end,
                roomStart = origin,
                thresholdMm = Millimeters.fromFeet(snapThresholdFeet).value,
                walls = walls
            )?.let { end = it }
        }
    }
    return end
}

internal fun resolveDraftBoxCommitEnd(
    previewEnd: PointMm?,
    snappedTap: PointMm,
    walls: List<WallSegment>,
    snapThresholdFeet: Double,
    endpointSnappingEnabled: Boolean
): PointMm {
    previewEnd?.let { return it }
    if (!endpointSnappingEnabled) return snappedTap
    return snapToNearestWallEndpoint(
        candidate = snappedTap,
        walls = walls,
        thresholdMm = (Millimeters.fromFeet(snapThresholdFeet).value * 3L / 2L)
            .coerceAtLeast(1L)
    )
}

internal fun closestGravelMaterialPreset(densityTonsPerYard: Double): GravelMaterialPreset {
    return gravelMaterialPresets.minByOrNull { preset ->
        abs(preset.densityTonsPerYard - densityTonsPerYard)
    } ?: gravelMaterialPresets.first()
}

internal fun canAddDraftedWall(
    document: BlueprintDocument,
    start: PointMm,
    end: PointMm,
    scale: Float
): Boolean {
    val lengthMm = BlueprintSnapMath.distanceMillimeters(start, end)
    if (lengthMm < MIN_DRAW_WALL_LENGTH_MM) return false
    val lengthPx = lengthMm * BASE_PX_PER_MM * scale.coerceIn(MIN_BLUEPRINT_SCALE, MAX_BLUEPRINT_SCALE)
    if (lengthPx < MIN_DRAW_WALL_SCREEN_PX) return false
    return document.walls.none { wall ->
        wallMatchesEndpoints(wall, start, end)
    }
}

internal fun buildDraftBoxWalls(
    document: BlueprintDocument,
    start: PointMm,
    end: PointMm,
    rotationRadians: Double,
    scale: Float,
    wallHeightMm: Long,
    wallThicknessMm: Long,
    tags: Set<String>
): List<WallSegment> {
    val corners = draftBoxCorners(
        start = start,
        end = end,
        rotationRadians = rotationRadians
    )
    if (corners.size != 4) return emptyList()
    val (cornerA, cornerB, cornerC, cornerD) = corners
    val candidateEdges = listOf(
        cornerA to cornerB,
        cornerB to cornerC,
        cornerC to cornerD,
        cornerD to cornerA
    )

    var workingDocument = document
    val acceptedWalls = mutableListOf<WallSegment>()
    candidateEdges.forEach { (edgeStart, edgeEnd) ->
        if (!canAddDraftedWall(workingDocument, edgeStart, edgeEnd, scale)) return@forEach
        val wall = WallSegment(
            id = UUID.randomUUID().toString(),
            start = edgeStart,
            end = edgeEnd,
            height = Millimeters(wallHeightMm),
            thickness = Millimeters(wallThicknessMm),
            tags = tags
        )
        acceptedWalls += wall
        workingDocument = workingDocument.copy(walls = workingDocument.walls + wall)
    }
    return acceptedWalls
}

internal fun wallMatchesEndpoints(
    wall: WallSegment,
    start: PointMm,
    end: PointMm
): Boolean {
    val direct = pointsNear(wall.start, start) && pointsNear(wall.end, end)
    val reversed = pointsNear(wall.start, end) && pointsNear(wall.end, start)
    return direct || reversed
}

internal fun pointsNear(a: PointMm, b: PointMm): Boolean {
    return abs(a.x - b.x) <= WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM &&
        abs(a.y - b.y) <= WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM
}

internal fun snapMovedWallToNearbyWalls(
    wall: WallSegment,
    referenceWalls: List<WallSegment>,
    thresholdMm: Long
): WallSegment {
    if (referenceWalls.isEmpty() || thresholdMm <= 0L) return wall
    var bestDx = 0L
    var bestDy = 0L
    var bestDistance = Long.MAX_VALUE
    var bestPriority = Int.MAX_VALUE

    fun considerSnap(source: PointMm, target: PointMm, priority: Int) {
        val distance = BlueprintSnapMath.distanceMillimeters(source, target)
        if (distance > thresholdMm) return
        val dx = target.x - source.x
        val dy = target.y - source.y
        val better = when {
            priority < bestPriority -> true
            priority > bestPriority -> false
            distance < bestDistance -> true
            distance > bestDistance -> false
            else -> abs(dx) + abs(dy) < abs(bestDx) + abs(bestDy)
        }
        if (better) {
            bestPriority = priority
            bestDistance = distance
            bestDx = dx
            bestDy = dy
        }
    }

    referenceWalls.forEach { reference ->
        listOf(wall.start, wall.end).forEach { movingPoint ->
            considerSnap(movingPoint, reference.start, priority = 0)
            considerSnap(movingPoint, reference.end, priority = 0)
            val projectionT = BlueprintSnapMath.projectToWallT(movingPoint, reference).coerceIn(0.0, 1.0)
            val projection = BlueprintSnapMath.pointOnWall(reference, projectionT)
            considerSnap(movingPoint, projection, priority = 1)
        }
        val center = wall.midpoint()
        val centerProjectionT = BlueprintSnapMath.projectToWallT(center, reference).coerceIn(0.0, 1.0)
        val centerProjection = BlueprintSnapMath.pointOnWall(reference, centerProjectionT)
        considerSnap(center, centerProjection, priority = 2)
    }

    return if (bestPriority == Int.MAX_VALUE) {
        wall
    } else {
        wall.translateBy(bestDx, bestDy)
    }
}

internal fun WallSegment.rotateByDegreesIncrement(stepDegrees: Double): WallSegment {
    val currentAngle = angleDegrees()
    val targetAngle = currentAngle + stepDegrees
    val snappedAngle = round(targetAngle / MOVING_WALL_ROTATE_INCREMENT_DEG) * MOVING_WALL_ROTATE_INCREMENT_DEG
    val deltaDegrees = snappedAngle - currentAngle
    if (abs(deltaDegrees) <= 0.0001) return this
    val pivot = midpoint()
    val radians = Math.toRadians(deltaDegrees)
    fun rotatePoint(point: PointMm): PointMm {
        val translatedX = (point.x - pivot.x).toDouble()
        val translatedY = (point.y - pivot.y).toDouble()
        val rotatedX = (translatedX * cos(radians)) - (translatedY * sin(radians))
        val rotatedY = (translatedX * sin(radians)) + (translatedY * cos(radians))
        return PointMm(
            x = (pivot.x + rotatedX).roundToLong(),
            y = (pivot.y + rotatedY).roundToLong()
        )
    }
    return copy(
        start = rotatePoint(start),
        end = rotatePoint(end)
    )
}

internal fun WallSegment.rotateByDialTicks(tickCount: Int): WallSegment {
    if (tickCount == 0) return this
    val deltaDegrees = tickCount * DRAW_EDGE_DIAL_ANGLE_STEP_DEGREES
    if (abs(deltaDegrees) <= 0.0001) return this
    val pivot = midpoint()
    val radians = Math.toRadians(deltaDegrees)
    fun rotatePoint(point: PointMm): PointMm {
        val translatedX = (point.x - pivot.x).toDouble()
        val translatedY = (point.y - pivot.y).toDouble()
        val rotatedX = (translatedX * cos(radians)) - (translatedY * sin(radians))
        val rotatedY = (translatedX * sin(radians)) + (translatedY * cos(radians))
        return PointMm(
            x = (pivot.x + rotatedX).roundToLong(),
            y = (pivot.y + rotatedY).roundToLong()
        )
    }
    return copy(
        start = rotatePoint(start),
        end = rotatePoint(end)
    )
}

internal fun WallSegment.resizeByDialTicks(tickCount: Int): WallSegment {
    if (tickCount == 0) return this
    val currentLengthMm = lengthMillimeters().toDouble()
    val nextLengthMm = (currentLengthMm + (tickCount * DRAW_EDGE_DIAL_LENGTH_STEP_MM))
        .coerceIn(
            MIN_DRAW_WALL_LENGTH_MM.toDouble(),
            DRAW_EDGE_DIAL_MAX_LENGTH_MM.toDouble()
        )
    val angleRadians = atan2(
        (end.y - start.y).toDouble(),
        (end.x - start.x).toDouble()
    )
    val center = midpoint()
    val halfLengthMm = nextLengthMm / 2.0
    val nextStart = pointFromPolarDraft(
        start = center,
        angleRadians = angleRadians + Math.PI,
        lengthMm = halfLengthMm
    )
    val nextEnd = pointFromPolarDraft(
        start = center,
        angleRadians = angleRadians,
        lengthMm = halfLengthMm
    )
    return copy(start = nextStart, end = nextEnd)
}

internal fun WallSegment.translateBy(dxMm: Long, dyMm: Long): WallSegment {
    return copy(
        start = PointMm(x = start.x + dxMm, y = start.y + dyMm),
        end = PointMm(x = end.x + dxMm, y = end.y + dyMm)
    )
}

internal fun WallSegment.scopeFromTag(): TakeoffScope? = when {
    tags.contains(TakeoffScope.DRYWALL.wallScopeTag()) -> TakeoffScope.DRYWALL
    tags.contains(TakeoffScope.CONCRETE.wallScopeTag()) -> TakeoffScope.CONCRETE
    tags.contains(TakeoffScope.GRAVEL_MULCH.wallScopeTag()) -> TakeoffScope.GRAVEL_MULCH
    tags.contains(TakeoffScope.PAINT.wallScopeTag()) -> TakeoffScope.PAINT
    else -> null
}

internal fun BlueprintFloorLevel.floorTag(): String = "$FLOOR_TAG_PREFIX$this"

internal fun BlueprintFloorLevel.label(): String = when {
    this == FLOOR_GROUND_LEVEL -> "Ground"
    this > FLOOR_GROUND_LEVEL -> (this + 1).toString()
    this == -1 -> "Basement"
    else -> "Basement ${abs(this)}"
}

internal fun BlueprintFloorLevel.compactLabel(): String = when {
    this == FLOOR_GROUND_LEVEL -> "G"
    this > FLOOR_GROUND_LEVEL -> (this + 1).toString()
    this == -1 -> "B1"
    else -> "B${abs(this)}"
}

internal fun BlueprintFloorLevel.floorDisplayLabel(): String = "Floor: ${label()}"

internal fun parseFloorLevelTag(tag: String?): BlueprintFloorLevel? {
    val normalized = tag?.trim() ?: return null
    if (!normalized.startsWith(FLOOR_TAG_PREFIX)) return null
    if (normalized.equals(FLOOR_LEGACY_LOWER_TAG, ignoreCase = true)) return FLOOR_GROUND_LEVEL
    if (normalized.equals(FLOOR_LEGACY_UPPER_TAG, ignoreCase = true)) return FLOOR_GROUND_LEVEL + 1
    return normalized.removePrefix(FLOOR_TAG_PREFIX).toIntOrNull()
}

internal fun Set<String>.resolveFloorLevelOrDefault(
    defaultLevel: BlueprintFloorLevel = FLOOR_GROUND_LEVEL
): BlueprintFloorLevel {
    val rawFloorTag = firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) }
    return parseFloorLevelTag(rawFloorTag) ?: defaultLevel
}

internal fun WallSegment.isOnFloor(level: BlueprintFloorLevel): Boolean {
    val wallFloor = tags.resolveFloorLevelOrDefault()
    return wallFloor == level
}

internal fun Room.isOnFloor(level: BlueprintFloorLevel): Boolean {
    val roomFloor = tags.resolveFloorLevelOrDefault()
    return roomFloor == level
}

internal fun BlueprintOpening.isOnFloor(
    level: BlueprintFloorLevel,
    wallsById: Map<String, WallSegment>
): Boolean {
    val inheritedFloor = wallsById[wallId]?.tags?.resolveFloorLevelOrDefault()
    val openingFloor = tags.resolveFloorLevelOrDefault(inheritedFloor ?: FLOOR_GROUND_LEVEL)
    return openingFloor == level
}

internal fun OpeningType.isStair(): Boolean {
    return this == OpeningType.STAIR_UP || this == OpeningType.STAIR_DOWN
}

internal fun OpeningType.displayLabel(): String = when (this) {
    OpeningType.DOOR -> "Door"
    OpeningType.WINDOW -> "Window"
    OpeningType.STAIR_UP -> "Stair Up"
    OpeningType.STAIR_DOWN -> "Stair Down"
}

internal fun TakeoffScope.shortLabel(): String = when (this) {
    TakeoffScope.DRYWALL -> "Drywall"
    TakeoffScope.CONCRETE -> "Concrete"
    TakeoffScope.GRAVEL_MULCH -> "Gravel"
    TakeoffScope.PAINT -> "Paint"
}

internal fun TakeoffScope.railLabel(): String = when (this) {
    TakeoffScope.DRYWALL -> "Dry"
    TakeoffScope.CONCRETE -> "Conc"
    TakeoffScope.GRAVEL_MULCH -> "Gravel"
    TakeoffScope.PAINT -> "Paint"
}

internal fun TakeoffScope.wallColor(): Color = when (this) {
    TakeoffScope.DRYWALL -> Color(0xFFCFEAFF)
    TakeoffScope.CONCRETE -> Color(0xFFFFAF9E)
    TakeoffScope.GRAVEL_MULCH -> Color(0xFFFFE3A6)
    TakeoffScope.PAINT -> Color(0xFF95F2BB)
}

internal fun TakeoffScope.wallScopeTag(): String = when (this) {
    TakeoffScope.DRYWALL -> "${WALL_SCOPE_TAG_PREFIX}drywall"
    TakeoffScope.CONCRETE -> "${WALL_SCOPE_TAG_PREFIX}concrete"
    TakeoffScope.GRAVEL_MULCH -> "${WALL_SCOPE_TAG_PREFIX}gravel_mulch"
    TakeoffScope.PAINT -> "${WALL_SCOPE_TAG_PREFIX}paint"
}

internal fun TakeoffScope.icon(): ImageVector = when (this) {
    TakeoffScope.DRYWALL -> Icons.Filled.Architecture
    TakeoffScope.CONCRETE -> Icons.Filled.Straighten
    TakeoffScope.GRAVEL_MULCH -> Icons.Filled.Workspaces
    TakeoffScope.PAINT -> Icons.Filled.AutoFixHigh
}

internal fun TakeoffScope.next(): TakeoffScope = when (this) {
    TakeoffScope.DRYWALL -> TakeoffScope.CONCRETE
    TakeoffScope.CONCRETE -> TakeoffScope.GRAVEL_MULCH
    TakeoffScope.GRAVEL_MULCH -> TakeoffScope.PAINT
    TakeoffScope.PAINT -> TakeoffScope.DRYWALL
}

