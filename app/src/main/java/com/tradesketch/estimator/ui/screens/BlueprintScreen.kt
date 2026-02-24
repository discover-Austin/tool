package com.tradesketch.estimator.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
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

private data class OpeningPreset(
    val name: String,
    val type: OpeningType,
    val widthMm: Long,
    val heightMm: Long,
    val sillMm: Long
)

private enum class OpeningPanelType {
    DOORS,
    WINDOWS,
    STAIR_UP,
    STAIR_DOWN
}

private typealias BlueprintFloorLevel = Int

private data class OpeningPlacementCandidate(
    val wall: WallSegment,
    val t: Double,
    val snappedCenter: PointMm,
    val swingTag: String?
)

private data class OpeningDragPreview(
    val preset: OpeningPreset,
    val rawWorldPoint: PointMm,
    val placement: OpeningPlacementCandidate?
)

private data class WallLengthLabelSpec(
    val start: Offset,
    val end: Offset,
    val lengthFeet: Double,
    val color: Color
)

private data class RightAngleHint(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm,
    val highlighted: Boolean
)

private data class CornerAngleHint(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm,
    val angleDegrees: Double,
    val highlighted: Boolean
)

private fun openingPreset(
    name: String,
    type: OpeningType,
    widthFeet: Double,
    heightFeet: Double,
    sillFeet: Double
): OpeningPreset {
    return OpeningPreset(
        name = name,
        type = type,
        widthMm = Millimeters.fromFeet(widthFeet).value,
        heightMm = Millimeters.fromFeet(heightFeet).value,
        sillMm = Millimeters.fromFeet(sillFeet).value
    )
}

private val doorPresets = listOf(
    openingPreset("2'0\" x 6'8\"", OpeningType.DOOR, 2.0, 6.67, 0.0),
    openingPreset("2'4\" x 6'8\"", OpeningType.DOOR, 2.33, 6.67, 0.0),
    openingPreset("2'6\" x 6'8\"", OpeningType.DOOR, 2.5, 6.67, 0.0),
    openingPreset("2'8\" x 6'8\"", OpeningType.DOOR, 2.67, 6.67, 0.0),
    openingPreset("3'0\" x 6'8\"", OpeningType.DOOR, 3.0, 6.67, 0.0),
    openingPreset("3'0\" x 7'0\"", OpeningType.DOOR, 3.0, 7.0, 0.0),
    openingPreset("3'6\" x 7'0\"", OpeningType.DOOR, 3.5, 7.0, 0.0),
    openingPreset("4'0\" x 7'0\"", OpeningType.DOOR, 4.0, 7.0, 0.0),
    openingPreset("5'0\" x 7'0\" (double)", OpeningType.DOOR, 5.0, 7.0, 0.0),
    openingPreset("6'0\" x 6'8\" (double)", OpeningType.DOOR, 6.0, 6.67, 0.0),
    openingPreset("8'0\" x 7'0\" slider", OpeningType.DOOR, 8.0, 7.0, 0.0),
    openingPreset("9'0\" x 7'0\" slider", OpeningType.DOOR, 9.0, 7.0, 0.0),
    openingPreset("16'0\" x 7'0\" garage", OpeningType.DOOR, 16.0, 7.0, 0.0)
)

private val windowPresets = listOf(
    openingPreset("2'0\" x 2'0\"", OpeningType.WINDOW, 2.0, 2.0, 3.0),
    openingPreset("2'0\" x 3'0\"", OpeningType.WINDOW, 2.0, 3.0, 3.0),
    openingPreset("2'0\" x 4'0\"", OpeningType.WINDOW, 2.0, 4.0, 3.0),
    openingPreset("2'0\" x 5'0\"", OpeningType.WINDOW, 2.0, 5.0, 3.0),
    openingPreset("3'0\" x 2'0\"", OpeningType.WINDOW, 3.0, 2.0, 3.0),
    openingPreset("3'0\" x 3'0\"", OpeningType.WINDOW, 3.0, 3.0, 3.0),
    openingPreset("3'0\" x 4'0\"", OpeningType.WINDOW, 3.0, 4.0, 3.0),
    openingPreset("3'0\" x 5'0\"", OpeningType.WINDOW, 3.0, 5.0, 3.0),
    openingPreset("4'0\" x 3'0\"", OpeningType.WINDOW, 4.0, 3.0, 3.0),
    openingPreset("4'0\" x 4'0\"", OpeningType.WINDOW, 4.0, 4.0, 3.0),
    openingPreset("4'0\" x 5'0\"", OpeningType.WINDOW, 4.0, 5.0, 3.0),
    openingPreset("5'0\" x 4'0\"", OpeningType.WINDOW, 5.0, 4.0, 3.0),
    openingPreset("6'0\" x 4'0\"", OpeningType.WINDOW, 6.0, 4.0, 3.0)
)

private val stairUpPresets = listOf(
    openingPreset("3'0\" x 9'0\" straight", OpeningType.STAIR_UP, 3.0, 9.0, 0.0),
    openingPreset("3'6\" x 10'0\" straight", OpeningType.STAIR_UP, 3.5, 10.0, 0.0),
    openingPreset("4'0\" x 11'0\" L-stair", OpeningType.STAIR_UP, 4.0, 11.0, 0.0)
)

private val stairDownPresets = listOf(
    openingPreset("3'0\" x 9'0\" straight", OpeningType.STAIR_DOWN, 3.0, 9.0, 0.0),
    openingPreset("3'6\" x 10'0\" straight", OpeningType.STAIR_DOWN, 3.5, 10.0, 0.0),
    openingPreset("4'0\" x 11'0\" L-stair", OpeningType.STAIR_DOWN, 4.0, 11.0, 0.0)
)

private val doorPreset = doorPresets.first { it.name.startsWith("3'0\" x 7'0\"") }
private val windowPreset = windowPresets.first { it.name.startsWith("4'0\" x 4'0\"") }
private val stairUpPreset = stairUpPresets.first()
private val stairDownPreset = stairDownPresets.first()

private const val BASE_PX_PER_MM = 0.065f
private const val MIN_BLUEPRINT_SCALE = 0.2f
private const val MAX_BLUEPRINT_SCALE = 7.5f
private const val MIN_DRAW_WALL_LENGTH_MM = 1L
private const val MIN_DRAW_WALL_SCREEN_PX = 10f
private const val WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM = 30L
private val CANVAS_TAP_AIM_OFFSET_PX = Offset(0f, -68f)
private const val WALL_POINTER_RELEASE_HOLD_MS = 360L
private const val WALL_SCOPE_TAG_PREFIX = "trade_scope:"
private const val FLOOR_TAG_PREFIX = "floor:"
private const val FLOOR_GROUND_LEVEL = 0
private const val FLOOR_LEGACY_LOWER_TAG = "${FLOOR_TAG_PREFIX}lower"
private const val FLOOR_LEGACY_UPPER_TAG = "${FLOOR_TAG_PREFIX}upper"
private const val DOOR_SWING_POS_TAG = "door_swing:pos"
private const val DOOR_SWING_NEG_TAG = "door_swing:neg"
private const val WALL_LENGTH_LABEL_TEXT_SP = 11.2f
private const val WALL_LENGTH_LABEL_OFFSET_PX = 13f
private const val PARALLEL_MATCH_ANGLE_TOLERANCE_DEG = 2.0
private const val PARALLEL_MATCH_LENGTH_TOLERANCE_MM = 35L
private const val RIGHT_ANGLE_MARKER_SIZE_PX = 11f
private const val RIGHT_ANGLE_MARKER_TOLERANCE_DEG = 1.0
private const val RIGHT_ANGLE_LABEL_TEXT_SP = 6.8f
private const val CORNER_ANGLE_LABEL_OFFSET_PX = 20f
private const val JOYSTICK_FRAME_RATE_BASE = 60f
private const val JOYSTICK_CURSOR_SPEED_PX_PER_SEC = 6.4f * JOYSTICK_FRAME_RATE_BASE
private const val JOYSTICK_PAN_SPEED_PX_PER_SEC = 7f * JOYSTICK_FRAME_RATE_BASE
private const val JOYSTICK_PAN_BOOST_PX_PER_SEC = 10f * JOYSTICK_FRAME_RATE_BASE
private const val JOYSTICK_MIN_FRAME_DELTA_SEC = 1f / 120f
private const val JOYSTICK_MAX_FRAME_DELTA_SEC = 1f / 30f
private const val JOYSTICK_CURSOR_RESPONSE_EXPONENT = 1f
private const val JOYSTICK_PAN_RESPONSE_EXPONENT = 1f
private const val JOYSTICK_DEADZONE_DEFAULT = 0.08f
private const val JOYSTICK_SELECT_BOOST_MULTIPLIER = 1.65f
private const val MOVING_WALL_ROTATE_INCREMENT_DEG = 5.0
private const val MOVING_WALL_AUTO_SNAP_THRESHOLD_MULTIPLIER = 1.4
private const val MOVING_WALL_AUTO_SNAP_MIN_THRESHOLD_MM = 90L
private const val POINTER_LENS_ZOOM = 2.1f
private const val POINTER_LENS_RADIUS_PX = 52f
private val POINTER_LENS_OFFSET_PX = Offset(102f, -94f)
private val DEFAULT_PANEL_BOTTOM_PADDING = 130.dp
private val BLUEPRINT_BACKGROUND_TOP = Color(0xFF071A30)
private val BLUEPRINT_BACKGROUND_BOTTOM = Color(0xFF0E2A49)
private val BLUEPRINT_CANVAS_TOP = Color(0xFF051423)
private val BLUEPRINT_CANVAS_BOTTOM = Color(0xFF0C2845)
private val BLUEPRINT_CANVAS_GLOW = Color(0x1D67B7F2)
private val BLUEPRINT_TEXTURE_DIAGONAL_A = Color(0x1C88B8E8)
private val BLUEPRINT_TEXTURE_DIAGONAL_B = Color(0x11709BC8)
private val BLUEPRINT_TEXTURE_NOISE_DOT = Color(0x145C86B5)
private val GRID_MINOR_COLOR = Color(0x326D90AE)
private val GRID_MAJOR_COLOR = Color(0x7BA4C8E8)
private val GRID_FIVE_FOOT_COLOR = Color(0xD0D7EEFF)
private val GRID_AXIS_COLOR = Color(0xF0D5E9FF)
private val GEOMETRY_SELECTION_COLOR = Color(0xFFFFF2BF)
private val GEOMETRY_HALO_COLOR = Color(0xC0020A14)
private val GEOMETRY_DEPTH_SHADOW = Color(0xB000040A)
private val GEOMETRY_DEPTH_HIGHLIGHT = Color(0xA6E8F6FF)
private val GEOMETRY_SELECTION_PULSE = Color(0x99FFDFA0)
private val GEOMETRY_SNAP_PULSE = Color(0xB89ED8FF)
private val OPENING_DOOR_COLOR = Color(0xFFFFCF8C)
private val OPENING_WINDOW_COLOR = Color(0xFFB0EEFF)
private val OPENING_PREVIEW_DOOR_COLOR = Color(0xFFFFDEAE)
private val OPENING_PREVIEW_WINDOW_COLOR = Color(0xFFE0F9FF)
private val OPENING_STAIR_UP_COLOR = Color(0xFFB8F2C8)
private val OPENING_STAIR_DOWN_COLOR = Color(0xFFFFD6AE)
private val OPENING_PREVIEW_STAIR_UP_COLOR = Color(0xFFDAFFE4)
private val OPENING_PREVIEW_STAIR_DOWN_COLOR = Color(0xFFFFE7CF)
private val OPENING_INVALID_COLOR = Color(0xFFFF7171)
private val DRAFT_WALL_COLOR = Color(0xFFFFCA86)
private val WALL_LABEL_NEUTRAL_COLOR = Color(0xFFF0F8FF)
private val WALL_LABEL_ACTIVE_COLOR = Color(0xFFFFEDCA)

private data class BlueprintToolToggleSpec(
    val tool: BlueprintDraftTool,
    val icon: ImageVector,
    val contentDescription: String
)

private data class BlueprintIconActionSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val enabled: Boolean,
    val onClick: () -> Unit
)

private data class BlueprintIconToggleSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

private data class GravelMaterialPreset(
    val label: String,
    val densityTonsPerYard: Double
)

private val gravelMaterialPresets = listOf(
    GravelMaterialPreset(label = "Pea gravel", densityTonsPerYard = 1.30),
    GravelMaterialPreset(label = "Crushed stone", densityTonsPerYard = 1.45),
    GravelMaterialPreset(label = "River rock", densityTonsPerYard = 1.35),
    GravelMaterialPreset(label = "Mulch", densityTonsPerYard = 0.25)
)

private val blueprintToolToggleSpecs = listOf(
    BlueprintToolToggleSpec(
        tool = BlueprintDraftTool.SELECT,
        icon = Icons.Filled.AdsClick,
        contentDescription = "Select tool"
    ),
    BlueprintToolToggleSpec(
        tool = BlueprintDraftTool.DRAW_WALL,
        icon = Icons.Filled.BorderColor,
        contentDescription = "Draw wall tool"
    )
)

private fun OpeningType.toDraftTool(): BlueprintDraftTool = when (this) {
    OpeningType.DOOR -> BlueprintDraftTool.PLACE_DOOR
    OpeningType.WINDOW -> BlueprintDraftTool.PLACE_WINDOW
    OpeningType.STAIR_UP -> BlueprintDraftTool.PLACE_STAIR_UP
    OpeningType.STAIR_DOWN -> BlueprintDraftTool.PLACE_STAIR_DOWN
}

private fun BlueprintDraftTool.defaultPreset(): OpeningPreset? = when (this) {
    BlueprintDraftTool.PLACE_DOOR -> doorPreset
    BlueprintDraftTool.PLACE_WINDOW -> windowPreset
    BlueprintDraftTool.PLACE_STAIR_UP -> stairUpPreset
    BlueprintDraftTool.PLACE_STAIR_DOWN -> stairDownPreset
    else -> null
}

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    initialShowAddons: Boolean = false,
    initialShowParams: Boolean = false,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    viewModel: BlueprintEditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val appSettings = settingsUiState.settings

    var tool by remember { mutableStateOf(BlueprintDraftTool.DRAW_WALL) }
    var drawingStart by remember { mutableStateOf<PointMm?>(null) }
    var drawingPreview by remember { mutableStateOf<PointMm?>(null) }
    var chainOrigin by remember { mutableStateOf<PointMm?>(null) }
    var detachedWalls by remember { mutableStateOf(false) }
    var movingWallPreview by remember { mutableStateOf<WallSegment?>(null) }
    var restartLineFromNearestWallStart by remember { mutableStateOf(false) }
    var snapSettings by remember {
        mutableStateOf(
            BlueprintSnapSettings(
                gridEnabled = appSettings.blueprintSnapGridEnabled,
                endpointEnabled = appSettings.blueprintSnapEndpointEnabled,
                midpointEnabled = appSettings.blueprintSnapMidpointEnabled,
                angleEnabled = appSettings.blueprintSnapAngleEnabled,
                closureEnabled = appSettings.blueprintSnapClosureEnabled
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
    var panelBeforeDrag by rememberSaveable(projectId) { mutableStateOf<OpeningPanelType?>(null) }
    var draggingPreset by remember { mutableStateOf<OpeningPreset?>(null) }
    var draggingScreenPoint by remember { mutableStateOf<Offset?>(null) }
    var canvasRoot by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var bottomRailBounds by remember { mutableStateOf<Rect?>(null) }
    var gridScaleBadgeBounds by remember { mutableStateOf<Rect?>(null) }
    var paramsPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var openingPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var selectionPanelBounds by remember { mutableStateOf<Rect?>(null) }
    var floorSwitcherBounds by remember { mutableStateOf<Rect?>(null) }
    var clearAllButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var gridScaleEditorBounds by remember { mutableStateOf<Rect?>(null) }
    var railHelpBounds by remember { mutableStateOf<Rect?>(null) }
    var wallRotateButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var dualJoysticksEnabled by rememberSaveable(projectId) {
        mutableStateOf(appSettings.blueprintDualJoysticksEnabled)
    }
    var largeCursorEnabled by rememberSaveable(projectId) {
        mutableStateOf(appSettings.blueprintLargeCursorEnabled)
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
    var blockedTouchAttempts by rememberSaveable(projectId) { mutableIntStateOf(0) }
    var showJoystickTouchDialog by rememberSaveable(projectId) { mutableStateOf(false) }
    var gridScaleInput by rememberSaveable(projectId) { mutableStateOf("1'") }
    var selectedFloor by rememberSaveable(projectId) { mutableStateOf(FLOOR_GROUND_LEVEL) }
    val rootView = LocalView.current

    LaunchedEffect(projectId) { viewModel.setProjectId(projectId) }
    LaunchedEffect(projectId, initialShowAddons, initialShowParams) {
        activeOpeningPanel = if (initialShowAddons) OpeningPanelType.DOORS else null
        showParams = initialShowParams
        showRailHelp = false
        showGridScaleEditor = false
        showClearAllConfirm = false
        panelBeforeDrag = null
    }
    LaunchedEffect(
        appSettings.blueprintSnapGridEnabled,
        appSettings.blueprintSnapEndpointEnabled,
        appSettings.blueprintSnapMidpointEnabled,
        appSettings.blueprintSnapAngleEnabled,
        appSettings.blueprintSnapClosureEnabled
    ) {
        snapSettings = snapSettings.copy(
            gridEnabled = appSettings.blueprintSnapGridEnabled,
            endpointEnabled = appSettings.blueprintSnapEndpointEnabled,
            midpointEnabled = appSettings.blueprintSnapMidpointEnabled,
            angleEnabled = appSettings.blueprintSnapAngleEnabled,
            closureEnabled = appSettings.blueprintSnapClosureEnabled
        )
    }
    LaunchedEffect(appSettings.blueprintDualJoysticksEnabled) {
        dualJoysticksEnabled = appSettings.blueprintDualJoysticksEnabled
    }
    LaunchedEffect(appSettings.blueprintLargeCursorEnabled) {
        largeCursorEnabled = appSettings.blueprintLargeCursorEnabled
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
        chainOrigin = null
        movingWallPreview = null
        restartLineFromNearestWallStart = false
        viewModel.selectWall(null)
    }
    if (uiState.isLoading || uiState.document == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val doc = uiState.document ?: return
    val stairWorkflowActive = doc.openings.any { it.type.isStair() } ||
        activeOpeningPanel == OpeningPanelType.STAIR_UP ||
        activeOpeningPanel == OpeningPanelType.STAIR_DOWN ||
        tool == BlueprintDraftTool.PLACE_STAIR_UP ||
        tool == BlueprintDraftTool.PLACE_STAIR_DOWN
    LaunchedEffect(stairWorkflowActive) {
        if (!stairWorkflowActive && selectedFloor != FLOOR_GROUND_LEVEL) {
            selectedFloor = FLOOR_GROUND_LEVEL
        }
        if (!stairWorkflowActive) {
            floorSwitcherBounds = null
        }
    }
    val movingDoc = movingWallPreview?.let { movingWall ->
        doc.copy(
            walls = doc.walls.map { wall ->
                if (wall.id == movingWall.id) movingWall else wall
            }
        )
    } ?: doc
    val wallsById = movingDoc.walls.associateBy { it.id }
    val renderedDoc = movingDoc.copy(
        walls = movingDoc.walls.filter { wall -> wall.isOnFloor(selectedFloor) },
        rooms = movingDoc.rooms.filter { room -> room.isOnFloor(selectedFloor) },
        openings = movingDoc.openings.filter { opening -> opening.isOnFloor(selectedFloor, wallsById) }
    )
    val selectedWall = uiState.selectedWallId?.let { id -> renderedDoc.walls.find { it.id == id } }
    val selectedOpening = uiState.selectedOpeningId?.let { id -> renderedDoc.openings.find { it.id == id } }
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
        if (stairWorkflowActive) floorSwitcherBounds?.let(::add)
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
    val wallArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(renderedDoc).values.sum()
    val openingArea = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(renderedDoc).values.sum()
    val netArea = (wallArea - openingArea).coerceAtLeast(0.0)
    val wallLengthFeet = renderedDoc.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    val takeoffSession = uiState.project?.takeoffSession ?: ProjectTakeoffSession()
    val currentScope = takeoffSession.selectedScope
    val liveScopeQuantity = computeLiveScopeQuantity(
        document = renderedDoc,
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
        }
    }
    val dragPreview = draggingPreset
        ?.let { preset ->
            draggingScreenPoint
                ?.let { point ->
                    val worldPoint = screenPointToWorldPoint(
                        rootPoint = point,
                        canvasRoot = canvasRoot,
                        canvasSize = canvasSize,
                        scale = scale,
                        pan = pan
                    ) ?: return@let null
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
    val handleLivePointerWorld: (PointMm) -> Unit = { pointer ->
        if (drawingStart != null && tool == BlueprintDraftTool.DRAW_WALL) {
            var previewEnd = pointer
            if (snapSettings.endpointEnabled) {
                previewEnd = snapToNearestWallEndpoint(
                    candidate = previewEnd,
                    walls = renderedDoc.walls,
                    thresholdMm = (Millimeters.fromFeet(snapSettings.thresholdFeet).value * 3L / 2L)
                        .coerceAtLeast(1L)
                )
            }
            if (snapSettings.closureEnabled) {
                chainOrigin?.let { origin ->
                    BlueprintSnapMath.roomClosureSnap(
                        candidateEnd = previewEnd,
                        roomStart = origin,
                        thresholdMm = Millimeters.fromFeet(snapSettings.thresholdFeet).value,
                        walls = renderedDoc.walls
                    )?.let { previewEnd = it }
                }
            }
            drawingPreview = previewEnd
        }
    }
    val handleTapWorld: (PointMm) -> Unit = { tap ->
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
                            settings = snapSettings,
                            walls = renderedDoc.walls
                        )
                        if (drawingStart == null) {
                            val startPoint = resolveDraftStartFromTap(
                                tap = tap,
                                snappedTap = snappedTap,
                                walls = renderedDoc.walls,
                                scale = scale,
                                snapThresholdFeet = snapSettings.thresholdFeet,
                                endpointSnappingEnabled = snapSettings.endpointEnabled,
                                preferWallProjection = restartLineFromNearestWallStart
                            )
                            drawingStart = startPoint
                            drawingPreview = startPoint
                            chainOrigin = startPoint
                            restartLineFromNearestWallStart = false
                        } else {
                            val start = drawingStart
                            if (start != null) {
                                var end = snappedTap
                                if (snapSettings.endpointEnabled) {
                                    end = snapToNearestWallEndpoint(
                                        candidate = end,
                                        walls = renderedDoc.walls,
                                        thresholdMm = (Millimeters.fromFeet(snapSettings.thresholdFeet).value * 3L / 2L)
                                            .coerceAtLeast(1L)
                                    )
                                }
                                if (snapSettings.closureEnabled) {
                                    chainOrigin?.let { origin ->
                                        BlueprintSnapMath.roomClosureSnap(
                                            candidateEnd = end,
                                            roomStart = origin,
                                            thresholdMm = Millimeters.fromFeet(snapSettings.thresholdFeet).value,
                                            walls = renderedDoc.walls
                                        )?.let { end = it }
                                    }
                                }
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
                tool = BlueprintDraftTool.DRAW_WALL
            }
            BlueprintDraftTool.SELECT -> {
                val nearestWall = nearestWallAt(tap, wallSelectionThresholdMm)
                val nearestOpening = nearestOpeningAt(tap)
                when {
                    nearestOpening != null -> viewModel.selectOpening(nearestOpening.id)
                    nearestWall != null -> viewModel.selectWall(nearestWall.id)
                    else -> viewModel.selectWall(null)
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
    val gridScaleLabel = formatFeetInchesPrime(snapSettings.gridStepFeet)
    LaunchedEffect(showGridScaleEditor) {
        if (showGridScaleEditor) {
            gridScaleInput = gridScaleLabel
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
    LaunchedEffect(dualJoysticksEnabled) {
        if (!dualJoysticksEnabled) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (dualJoysticksEnabled) {
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
                    val snappedWorld = BlueprintSnapMath.applySnapping(
                        rawPoint = worldPoint,
                        drawingStart = drawingStart,
                        settings = snapSettings,
                        walls = renderedDocSnapshot.walls
                    )
                    latestLivePointerHandler(snappedWorld)
                }
            }
        }
    }
    val joystickRailPadding = if (dualJoysticksEnabled) 56.dp else 0.dp
    val panelBottomPadding = DEFAULT_PANEL_BOTTOM_PADDING + joystickRailPadding
    val helpBottomPadding = panelBottomPadding + 14.dp
    val controlStateLabel: String? = when {
        movingWallPreview != null -> "Picked Up"
        tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> "Draw"
        selectedWall != null -> "Selected"
        else -> null
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
            selectedWallId = uiState.selectedWallId,
            selectedOpeningId = uiState.selectedOpeningId,
            movingWallActive = movingWallPreview != null,
            cursorSizeScale = if (largeCursorEnabled) 1.45f else 1f,
            dragPreview = dragPreview,
            onPanScaleChange = { updatedPan, updatedScale ->
                val panDelta = updatedPan - pan
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
            onTouchBlocked = {
                blockedTouchAttempts += 1
                if (blockedTouchAttempts >= 3) {
                    showJoystickTouchDialog = true
                }
            },
            virtualPointerScreenPoint = if (dualJoysticksEnabled) joystickCursorLocal else null,
            rightSelectBoostActive = rightSelectBoostActive,
            onLivePointerWorld = handleLivePointerWorld,
            onTapWorld = handleTapWorld
        )

        LiveOverlay(
            doc = renderedDoc,
            wallLengthFeet = wallLengthFeet,
            netArea = netArea,
            currentScope = currentScope,
            liveScopeQuantity = liveScopeQuantity,
            selectedFloor = selectedFloor,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        )
        if (stairWorkflowActive) {
            FloorLevelSwitcher(
                level = selectedFloor,
                onSelect = { floor ->
                    selectedFloor = floor
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 100.dp)
                    .onGloballyPositioned {
                        floorSwitcherBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
            )
        }

        ClearAllButton(
            onClick = { showClearAllConfirm = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp)
                .onGloballyPositioned {
                    clearAllButtonBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        ParamsPanel(
            expanded = showParams,
            scope = currentScope,
            params = doc.params,
            takeoffSession = takeoffSession,
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
                onStartDragPreset = { preset, rootPoint ->
                    when (preset.type) {
                        OpeningType.DOOR -> selectedDoorPreset = preset
                        OpeningType.WINDOW -> selectedWindowPreset = preset
                        OpeningType.STAIR_UP -> selectedStairUpPreset = preset
                        OpeningType.STAIR_DOWN -> selectedStairDownPreset = preset
                    }
                    tool = preset.type.toDraftTool()
                    draggingPreset = preset
                    draggingScreenPoint = rootPoint
                    panelBeforeDrag = activeOpeningPanel
                    activeOpeningPanel = null
                },
                onDragPreset = { rootPoint ->
                    draggingScreenPoint = rootPoint
                },
                onEndDragPreset = {
                    val preset = draggingPreset
                    val rootPoint = draggingScreenPoint
                    if (preset != null && rootPoint != null) {
                        screenPointToWorldPoint(
                            rootPoint = rootPoint,
                            canvasRoot = canvasRoot,
                            canvasSize = canvasSize,
                            scale = scale,
                            pan = pan
                        )?.let { world ->
                            placeOpeningAtWorld(world, preset)
                        }
                    }
                    draggingPreset = null
                    draggingScreenPoint = null
                    activeOpeningPanel = panelBeforeDrag
                    panelBeforeDrag = null
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
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = panelBottomPadding)
                    .navigationBarsPadding()
                    .onGloballyPositioned {
                        openingPanelBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
            )
        }

        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirm = false },
                title = { Text("Clear all blueprint items?") },
                text = { Text("This removes all walls, openings, and rooms. You can still undo if needed.") },
                confirmButton = {
                    TextButton(
                        onClick = {
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
        if (showJoystickTouchDialog && dualJoysticksEnabled) {
            AlertDialog(
                onDismissRequest = { showJoystickTouchDialog = false },
                title = { Text("Touch Input Is Off") },
                text = { Text("Dual joysticks are active, so canvas touch is disabled. Turn joysticks off to use finger touch?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dualJoysticksEnabled = false
                            settingsViewModel.updateBlueprintControlDefaults(dualJoysticksEnabled = false)
                            blockedTouchAttempts = 0
                            showJoystickTouchDialog = false
                        }
                    ) {
                        Text("Turn Off Joysticks")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            blockedTouchAttempts = 0
                            showJoystickTouchDialog = false
                        }
                    ) {
                        Text("Keep Joysticks")
                    }
                }
            )
        }

        // Selection Panel - show when an item is selected
        if (selectedWall != null || selectedOpening != null) {
            SelectionPanel(
                selectedWall = selectedWall,
                selectedOpening = selectedOpening,
                onDeselect = { viewModel.selectWall(null) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 186.dp)
                    .onGloballyPositioned {
                        selectionPanelBounds = Rect(it.positionInRoot(), it.size.toSize())
                    }
            )
        }

        val toggleDoorsPanel = {
            val opening = activeOpeningPanel != OpeningPanelType.DOORS
            activeOpeningPanel = if (opening) OpeningPanelType.DOORS else null
            if (opening) {
                showParams = false
                showRailHelp = false
            }
        }
        val toggleWindowsPanel = {
            val opening = activeOpeningPanel != OpeningPanelType.WINDOWS
            activeOpeningPanel = if (opening) OpeningPanelType.WINDOWS else null
            if (opening) {
                showParams = false
                showRailHelp = false
            }
        }
        val toggleStairUpPanel = {
            val opening = activeOpeningPanel != OpeningPanelType.STAIR_UP
            activeOpeningPanel = if (opening) OpeningPanelType.STAIR_UP else null
            if (opening) {
                showParams = false
                showRailHelp = false
            }
        }
        val toggleStairDownPanel = {
            val opening = activeOpeningPanel != OpeningPanelType.STAIR_DOWN
            activeOpeningPanel = if (opening) OpeningPanelType.STAIR_DOWN else null
            if (opening) {
                showParams = false
                showRailHelp = false
            }
        }
        val toggleParamsPanel = {
            val opening = !showParams
            showParams = opening
            if (opening) {
                activeOpeningPanel = null
                showRailHelp = false
            }
        }

        BlueprintBottomBar(
            canDeleteSelection = selectedWall != null || selectedOpening != null,
            detachedWalls = detachedWalls,
            scope = currentScope,
            activePanel = activeOpeningPanel,
            paramsExpanded = showParams,
            onToggleDetached = {
                detachedWalls = !detachedWalls
            },
            onDeleteSelection = {
                when {
                    selectedOpening != null -> viewModel.deleteSelectedOpening()
                    selectedWall != null -> viewModel.deleteSelectedWall()
                }
            },
            onChangeScope = viewModel::updateTakeoffScope,
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
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                .navigationBarsPadding()
                .imePadding()
                .onGloballyPositioned {
                    bottomRailBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        if (dualJoysticksEnabled) {
            DualJoystickOverlay(
                leftVector = leftJoystickVector,
                rightVector = rightJoystickVector,
                onLeftVectorChange = { leftJoystickVector = it },
                onRightVectorChange = { rightJoystickVector = it },
                onRightPressChange = { rightJoystickPressed = it },
                onLeftTap = dispatchLeftJoystickClick,
                onRightTap = dispatchRightJoystickClick,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                canZoomIn = scale < MAX_BLUEPRINT_SCALE,
                canZoomOut = scale > MIN_BLUEPRINT_SCALE,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onZoomIn = { scale = (scale * 1.15f).coerceAtMost(MAX_BLUEPRINT_SCALE) },
                onZoomOut = { scale = (scale / 1.15f).coerceAtLeast(MIN_BLUEPRINT_SCALE) },
                controlStateLabel = controlStateLabel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = joystickRailPadding)
                    .navigationBarsPadding()
            )
        }
        CursorCoordinateOverlay(
            worldPoint = joystickCursorWorldPoint,
            showRotate = movingWallPreview != null,
            onRotate = rotatePickedUpWallClockwise,
            rotateButtonModifier = Modifier.onGloballyPositioned {
                wallRotateButtonBounds = Rect(it.positionInRoot(), it.size.toSize())
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = helpBottomPadding + 4.dp)
                .navigationBarsPadding()
        )

        GridScaleBadge(
            label = gridScaleLabel,
            onClick = {
                showGridScaleEditor = !showGridScaleEditor
                if (showGridScaleEditor) {
                    gridScaleInput = gridScaleLabel
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 56.dp)
                .navigationBarsPadding()
                .onGloballyPositioned {
                    gridScaleBadgeBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        GridScaleEditorPanel(
            expanded = showGridScaleEditor,
            value = gridScaleInput,
            onValueChange = { gridScaleInput = it },
            onDismiss = { showGridScaleEditor = false },
            onApply = {
                val parsedFeet = parsePrimeLengthToFeet(gridScaleInput)
                if (parsedFeet != null) {
                    snapSettings = snapSettings.copy(
                        gridEnabled = true,
                        gridStepFeet = parsedFeet.coerceIn(0.25, 20.0)
                    )
                    settingsViewModel.updateBlueprintSnapDefaults(gridEnabled = true)
                    showGridScaleEditor = false
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 90.dp)
                .navigationBarsPadding()
                .onGloballyPositioned {
                    gridScaleEditorBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )

        RailHelpPanel(
            expanded = showRailHelp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = helpBottomPadding)
                .navigationBarsPadding()
                .onGloballyPositioned {
                    railHelpBounds = Rect(it.positionInRoot(), it.size.toSize())
                }
        )
    }
    onFullscreenBlueprintChanged(true)
}

@Composable
private fun BlueprintCanvas(
    modifier: Modifier = Modifier,
    document: BlueprintDocument,
    scope: TakeoffScope,
    tool: BlueprintDraftTool,
    snapSettings: BlueprintSnapSettings,
    scale: Float,
    pan: Offset,
    drawingStart: PointMm?,
    drawingPreview: PointMm?,
    selectedWallId: String?,
    selectedOpeningId: String?,
    movingWallActive: Boolean,
    cursorSizeScale: Float,
    dragPreview: OpeningDragPreview?,
    touchEnabled: Boolean,
    onTouchBlocked: () -> Unit,
    virtualPointerScreenPoint: Offset?,
    rightSelectBoostActive: Boolean,
    onPanScaleChange: (Offset, Float) -> Unit,
    onCanvasLayout: (Offset, Size) -> Unit,
    onLivePointerWorld: (PointMm) -> Unit,
    onTapWorld: (PointMm) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var drawWallPointerScreenPoint by remember { mutableStateOf<Offset?>(null) }
    var drawWallPointerHideAtMs by remember { mutableStateOf<Long?>(null) }
    val canvasPulseTransition = rememberInfiniteTransition(label = "blueprint-canvas-pulse")
    val selectionPulse = canvasPulseTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "selection-pulse"
    ).value
    val snapPulse = canvasPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snap-pulse"
    ).value
    LaunchedEffect(drawWallPointerHideAtMs) {
        val hideAt = drawWallPointerHideAtMs ?: return@LaunchedEffect
        val remaining = (hideAt - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        if (remaining > 0L) {
            delay(remaining)
        }
        if (drawWallPointerHideAtMs == hideAt && SystemClock.uptimeMillis() >= hideAt) {
            drawWallPointerScreenPoint = null
            drawWallPointerHideAtMs = null
        }
    }
    fun worldToScreen(p: PointMm): Offset {
        val ppm = BASE_PX_PER_MM * scale
        return Offset(canvasSize.width / 2f + pan.x + (p.x * ppm), canvasSize.height / 2f + pan.y - (p.y * ppm))
    }
    fun screenToWorld(p: Offset): PointMm {
        val ppm = BASE_PX_PER_MM * scale
        return PointMm(
            ((p.x - canvasSize.width / 2f - pan.x) / ppm).roundToLong(),
            (-(p.y - canvasSize.height / 2f - pan.y) / ppm).roundToLong()
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                onCanvasLayout(it.positionInRoot(), it.size.toSize())
            }
            .pointerInput(scale, pan, touchEnabled) {
                if (!touchEnabled) return@pointerInput
                detectTransformGestures { _, panDelta, zoom, _ ->
                    val updatedScale = scale * zoom
                    if (panDelta != Offset.Zero || zoom != 1f) {
                        val updatedPan = pan + panDelta
                        onPanScaleChange(updatedPan, updatedScale)
                    }
                }
            }
            .pointerInput(tool, pan, scale, touchEnabled) {
                if (!touchEnabled) return@pointerInput
                if (tool == BlueprintDraftTool.SELECT) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPanScaleChange(pan + dragAmount, scale)
                    }
                }
            }
            .pointerInput(tool, scale, pan, drawingStart, snapSettings, document.walls, touchEnabled) {
                awaitPointerEventScope {
                    if (!touchEnabled) {
                        drawWallPointerScreenPoint = null
                        drawWallPointerHideAtMs = null
                        while (true) {
                            val event = awaitPointerEvent()
                            var blockedTouch = false
                            event.changes.forEach { change ->
                                if (change.changedToDownIgnoreConsumed()) {
                                    blockedTouch = true
                                }
                                change.consume()
                            }
                            if (blockedTouch) {
                                onTouchBlocked()
                            }
                        }
                    }
                    var draggedDistancePx = 0f
                    fun aimedPosition(raw: Offset): Offset {
                        return Offset(
                            x = (raw.x + CANVAS_TAP_AIM_OFFSET_PX.x).coerceIn(0f, canvasSize.width),
                            y = (raw.y + CANVAS_TAP_AIM_OFFSET_PX.y).coerceIn(0f, canvasSize.height)
                        )
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.changedToDownIgnoreConsumed()) {
                            draggedDistancePx = 0f
                        }
                        if (change.pressed && change.previousPressed) {
                            val delta = change.positionChange()
                            draggedDistancePx += hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                        }
                        val aimed = aimedPosition(change.position)
                        drawWallPointerScreenPoint = if (tool == BlueprintDraftTool.DRAW_WALL && change.pressed) {
                            drawWallPointerHideAtMs = null
                            aimed
                        } else {
                            drawWallPointerScreenPoint
                        }
                        val rawWorld = screenToWorld(aimed)
                        val snappedWorld = BlueprintSnapMath.applySnapping(
                            rawPoint = rawWorld,
                            drawingStart = drawingStart,
                            settings = snapSettings,
                            walls = document.walls
                        )
                        onLivePointerWorld(snappedWorld)
                        if (change.changedToUpIgnoreConsumed() && draggedDistancePx < 10f && !change.isConsumed) {
                            onTapWorld(screenToWorld(aimed))
                        }
                        if (tool != BlueprintDraftTool.DRAW_WALL) {
                            drawWallPointerScreenPoint = null
                            drawWallPointerHideAtMs = null
                        } else if ((change.changedToUpIgnoreConsumed() || !change.pressed) && drawWallPointerScreenPoint != null) {
                            drawWallPointerHideAtMs = SystemClock.uptimeMillis() + WALL_POINTER_RELEASE_HOLD_MS
                        }
                    }
                }
            }
    ) {
        canvasSize = size
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    BLUEPRINT_CANVAS_TOP,
                    BLUEPRINT_CANVAS_BOTTOM
                )
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    BLUEPRINT_CANVAS_GLOW,
                    Color.Transparent
                ),
                center = Offset(size.width * 0.52f, size.height * 0.18f),
                radius = (size.width + size.height) * 0.65f
            )
        )
        drawBlueprintTexturePattern()
        val ppm = BASE_PX_PER_MM * scale
        if (snapSettings.gridEnabled && ppm > 0f) {
            val footStepMm = Millimeters.fromFeet(snapSettings.gridStepFeet.coerceAtLeast(0.1)).value
                .coerceAtLeast(50L)
                .toDouble()
            val inchStepMm = (footStepMm / 12.0).coerceAtLeast(1.0)
            val showInchSubgrid = (inchStepMm * ppm) >= 3f
            val drawStepMm = if (showInchSubgrid) inchStepMm else footStepMm
            val linesPerFoot = if (showInchSubgrid) 12L else 1L

            val leftWorldX = screenToWorld(Offset(0f, 0f)).x.toDouble()
            val rightWorldX = screenToWorld(Offset(size.width, 0f)).x.toDouble()
            val topWorldY = screenToWorld(Offset(0f, 0f)).y.toDouble()
            val bottomWorldY = screenToWorld(Offset(0f, size.height)).y.toDouble()
            val minWorldX = minOf(leftWorldX, rightWorldX)
            val maxWorldX = maxOf(leftWorldX, rightWorldX)
            val minWorldY = minOf(topWorldY, bottomWorldY)
            val maxWorldY = maxOf(topWorldY, bottomWorldY)

            fun gridLineStyle(index: Long): Pair<Color, Float> {
                val isFootLine = index % linesPerFoot == 0L
                if (!isFootLine) return GRID_MINOR_COLOR to 0.62f
                val footIndex = index / linesPerFoot
                val isFiveFoot = footIndex % 5L == 0L
                return if (isFiveFoot) {
                    GRID_FIVE_FOOT_COLOR to 1.75f
                } else {
                    GRID_MAJOR_COLOR to 1.2f
                }
            }

            var xIndex = floor(minWorldX / drawStepMm).toLong()
            var worldX = xIndex.toDouble() * drawStepMm
            while (worldX <= maxWorldX + drawStepMm) {
                val sx = size.width / 2f + pan.x + (worldX * ppm).toFloat()
                if (sx in -2f..(size.width + 2f)) {
                    val (lineColor, lineWidth) = gridLineStyle(xIndex)
                    drawLine(
                        color = lineColor,
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = lineWidth
                    )
                }
                xIndex += 1L
                worldX += drawStepMm
            }

            var yIndex = floor(minWorldY / drawStepMm).toLong()
            var worldY = yIndex.toDouble() * drawStepMm
            while (worldY <= maxWorldY + drawStepMm) {
                val sy = size.height / 2f + pan.y - (worldY * ppm).toFloat()
                if (sy in -2f..(size.height + 2f)) {
                    val (lineColor, lineWidth) = gridLineStyle(yIndex)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = lineWidth
                    )
                }
                yIndex += 1L
                worldY += drawStepMm
            }
        }
        drawLine(
            color = GRID_AXIS_COLOR,
            start = worldToScreen(PointMm(-70_000, 0)),
            end = worldToScreen(PointMm(70_000, 0)),
            strokeWidth = 2.05f
        )
        drawLine(
            color = GRID_AXIS_COLOR,
            start = worldToScreen(PointMm(0, -70_000)),
            end = worldToScreen(PointMm(0, 70_000)),
            strokeWidth = 2.05f
        )
        val previewWall = if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            WallSegment(
                id = "__preview_wall__",
                start = drawingStart,
                end = drawingPreview
            )
        } else {
            null
        }
        val wallsWithPreview = previewWall?.let { document.walls + it } ?: document.walls
        val cornerAngleHints = collectCornerAngleHints(
            walls = wallsWithPreview,
            highlightedWallId = previewWall?.id
        )
        val rightAngleHints = cornerAngleHints
            .asSequence()
            .filter { abs(it.angleDegrees - 90.0) <= RIGHT_ANGLE_MARKER_TOLERANCE_DEG }
            .map { hint ->
                RightAngleHint(
                    corner = hint.corner,
                    legA = hint.legA,
                    legB = hint.legB,
                    highlighted = hint.highlighted
                )
            }
            .toList()
        val nonRightCornerAngleHints = cornerAngleHints
            .filter { abs(it.angleDegrees - 90.0) > RIGHT_ANGLE_MARKER_TOLERANCE_DEG }
        val scopeWallColor = scope.wallColor()
        val wallLengthLabels = mutableListOf<WallLengthLabelSpec>()
        document.walls.forEach { wall ->
            val isSelected = wall.id == selectedWallId
            val wallColor = wall.scopeFromTag()?.wallColor() ?: scopeWallColor
            val color = if (isSelected) GEOMETRY_SELECTION_COLOR else wallColor
            val parallelMatch = wallHasParallelLengthMatch(wall, document.walls)
            val strokeWidth = if (isSelected) 5.8f else if (parallelMatch) 4.6f else 3.8f
            val wallStartScreen = worldToScreen(wall.start)
            val wallEndScreen = worldToScreen(wall.end)
            drawStyledWallSegment(
                start = wallStartScreen,
                end = wallEndScreen,
                color = color,
                strokeWidth = strokeWidth,
                selected = isSelected,
                pulse = selectionPulse
            )
            wallLengthLabels += WallLengthLabelSpec(
                start = wallStartScreen,
                end = wallEndScreen,
                lengthFeet = Millimeters(wall.lengthMillimeters()).toFeet(),
                color = if (isSelected) WALL_LABEL_ACTIVE_COLOR else WALL_LABEL_NEUTRAL_COLOR
            )
        }
        document.openings.forEach { opening ->
            val wall = document.walls.firstOrNull { it.id == opening.wallId } ?: return@forEach
            val isSelected = opening.id == selectedOpeningId
            val color = if (isSelected) {
                GEOMETRY_SELECTION_COLOR
            } else {
                when (opening.type) {
                    OpeningType.DOOR -> OPENING_DOOR_COLOR
                    OpeningType.WINDOW -> OPENING_WINDOW_COLOR
                    OpeningType.STAIR_UP -> OPENING_STAIR_UP_COLOR
                    OpeningType.STAIR_DOWN -> OPENING_STAIR_DOWN_COLOR
                }
            }
            drawOpeningOnWall(
                worldToScreen = ::worldToScreen,
                wall = wall,
                t = opening.t,
                widthMm = opening.widthMm,
                type = opening.type,
                swingTag = opening.doorSwingTag(),
                color = color,
                emphasized = isSelected,
                emphasisPulse = if (isSelected) selectionPulse else 0f
            )
        }
        dragPreview?.let { preview ->
            val placement = preview.placement
            if (placement != null) {
                val snappedColor = when (preview.preset.type) {
                    OpeningType.DOOR -> OPENING_PREVIEW_DOOR_COLOR
                    OpeningType.WINDOW -> OPENING_PREVIEW_WINDOW_COLOR
                    OpeningType.STAIR_UP -> OPENING_PREVIEW_STAIR_UP_COLOR
                    OpeningType.STAIR_DOWN -> OPENING_PREVIEW_STAIR_DOWN_COLOR
                }
                drawOpeningOnWall(
                    worldToScreen = ::worldToScreen,
                    wall = placement.wall,
                    t = placement.t,
                    widthMm = preview.preset.widthMm,
                    type = preview.preset.type,
                    swingTag = placement.swingTag,
                    color = snappedColor,
                    emphasized = false,
                    emphasisPulse = 0f
                )
            } else {
                val center = worldToScreen(preview.rawWorldPoint)
                val widthPx = (preview.preset.widthMm * (BASE_PX_PER_MM * scale)).coerceIn(28f, 156f)
                drawFloatingOpeningPreview(
                    center = center,
                    widthPx = widthPx,
                    type = preview.preset.type,
                    color = OPENING_INVALID_COLOR
                )
            }
        }
        if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            val draftStart = worldToScreen(drawingStart)
            val draftEnd = worldToScreen(drawingPreview)
            val previewParallelMatch = previewWall?.let { wallHasParallelLengthMatch(it, document.walls) } == true
            drawStyledWallSegment(
                start = draftStart,
                end = draftEnd,
                color = DRAFT_WALL_COLOR,
                strokeWidth = if (previewParallelMatch) 4.9f else 4.1f,
                selected = true,
                pulse = snapPulse
            )
            wallLengthLabels += WallLengthLabelSpec(
                start = draftStart,
                end = draftEnd,
                lengthFeet = Millimeters(BlueprintSnapMath.distanceMillimeters(drawingStart, drawingPreview)).toFeet(),
                color = WALL_LABEL_ACTIVE_COLOR
            )
        }
        rightAngleHints.forEach { hint ->
            drawRightAngleHint(
                hint = hint,
                worldToScreen = ::worldToScreen,
                scale = scale
            )
        }
        nonRightCornerAngleHints.forEach { hint ->
            drawCornerAngleLabel(
                hint = hint,
                worldToScreen = ::worldToScreen,
                scale = scale
            )
        }
        wallLengthLabels.forEach { label ->
            drawWallLengthLabel(
                start = label.start,
                end = label.end,
                lengthFeet = label.lengthFeet,
                color = label.color
            )
        }
        val pointer = if (virtualPointerScreenPoint != null) {
            virtualPointerScreenPoint
        } else if (tool == BlueprintDraftTool.DRAW_WALL) {
            drawWallPointerScreenPoint
        } else {
            null
        }
        if (pointer != null && virtualPointerScreenPoint != null) {
            val pointerWorld = screenToWorld(pointer)
            val nearestProjection = document.walls
                .map { wall ->
                    val t = BlueprintSnapMath.projectToWallT(pointerWorld, wall).coerceIn(0.0, 1.0)
                    val projected = BlueprintSnapMath.pointOnWall(wall, t)
                    Triple(wall, projected, BlueprintSnapMath.distanceMillimeters(pointerWorld, projected))
                }
                .minByOrNull { it.third }
                ?.takeIf { it.third <= Millimeters.fromFeet(5.0).value }
            drawSelectionMagnifier(
                pointer = pointer,
                nearestWall = nearestProjection?.first,
                nearestPoint = nearestProjection?.second,
                worldToScreen = ::worldToScreen,
                boostActive = rightSelectBoostActive
            )
        }
        if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            drawPrecisionPulse(
                center = worldToScreen(drawingPreview),
                progress = snapPulse,
                color = GEOMETRY_SNAP_PULSE,
                baseRadius = 11f,
                maxRadius = 28f
            )
        }
        if (selectedWallId != null || movingWallActive) {
            pointer?.let { currentPointer ->
                drawPrecisionPulse(
                    center = currentPointer,
                    progress = selectionPulse,
                    color = GEOMETRY_SELECTION_PULSE,
                    baseRadius = 8f,
                    maxRadius = 20f
                )
            }
        }
        val cursorGlyph = when {
            movingWallActive -> CursorGlyph.GRAB
            tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null -> CursorGlyph.PENCIL
            selectedWallId != null -> CursorGlyph.HAND_POINTER
            else -> CursorGlyph.ARROW
        }
        pointer?.let { drawCursorGlyph(it, cursorGlyph, cursorSizeScale) }
    }
}

@Composable
private fun BlueprintBottomBar(
    canDeleteSelection: Boolean,
    detachedWalls: Boolean,
    scope: TakeoffScope,
    activePanel: OpeningPanelType?,
    paramsExpanded: Boolean,
    onToggleDetached: () -> Unit,
    onDeleteSelection: () -> Unit,
    onChangeScope: (TakeoffScope) -> Unit,
    onToggleDoors: () -> Unit,
    onToggleWindows: () -> Unit,
    onToggleStairUp: () -> Unit,
    onToggleStairDown: () -> Unit,
    onToggleParams: () -> Unit,
    showHelp: Boolean,
    onToggleHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(40.dp)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlimIconAction(
                icon = Icons.Filled.Delete,
                contentDescription = "Delete selected",
                enabled = canDeleteSelection,
                onClick = onDeleteSelection
            )
            BarDivider()
            listOf(
                BlueprintIconToggleSpec(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = "Detached walls",
                    selected = detachedWalls,
                    onClick = onToggleDetached
                )
            ).forEach { toggle ->
                SlimIconToggle(
                    icon = toggle.icon,
                    contentDescription = toggle.contentDescription,
                    selected = toggle.selected,
                    onClick = toggle.onClick
                )
            }
            BarDivider()
            SlimIconToggle(
                icon = Icons.Filled.DoorFront,
                contentDescription = "Doors panel",
                selected = activePanel == OpeningPanelType.DOORS,
                onClick = onToggleDoors
            )
            SlimIconToggle(
                icon = Icons.Filled.Window,
                contentDescription = "Windows panel",
                selected = activePanel == OpeningPanelType.WINDOWS,
                onClick = onToggleWindows
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Stair up panel",
                selected = activePanel == OpeningPanelType.STAIR_UP,
                onClick = onToggleStairUp
            )
            SlimIconToggle(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Stair down panel",
                selected = activePanel == OpeningPanelType.STAIR_DOWN,
                onClick = onToggleStairDown
            )
            SlimIconToggle(
                icon = Icons.Filled.Tune,
                contentDescription = "Params panel",
                selected = paramsExpanded,
                onClick = onToggleParams
            )
            BarDivider()
            ScopeSelector(
                scope = scope,
                onChangeScope = onChangeScope
            )
            SlimIconToggle(
                icon = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Rail help",
                selected = showHelp,
                onClick = onToggleHelp
            )
        }
    }
}

@Composable
private fun SlimIconToggle(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    }
    val tint = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        modifier = modifier.size(26.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SlimIconAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 26.dp,
    iconSize: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val container = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    }
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier.size(buttonSize)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ScopeSelector(
    scope: TakeoffScope,
    onChangeScope: (TakeoffScope) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onChangeScope(scope.next()) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
        modifier = modifier.height(23.dp).widthIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = scope.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = scope.railLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun BarDivider() {
    Spacer(
        modifier = Modifier
            .height(22.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.32f))
    )
}

@Composable
private fun ClearAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.52f)),
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Clear all blueprint items",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Clear All",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FloorLevelSwitcher(
    level: BlueprintFloorLevel,
    onSelect: (BlueprintFloorLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = level.floorDisplayLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlimIconAction(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Lower floor",
                    enabled = true,
                    onClick = { onSelect(level - 1) },
                    buttonSize = 30.dp,
                    iconSize = 15.dp
                )
                SlimIconAction(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Upper floor",
                    enabled = true,
                    onClick = { onSelect(level + 1) },
                    buttonSize = 30.dp,
                    iconSize = 15.dp
                )
            }
            Surface(
                onClick = { onSelect(FLOOR_GROUND_LEVEL) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
            ) {
                Text(
                    text = "Go Ground",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DualJoystickOverlay(
    leftVector: Offset,
    rightVector: Offset,
    onLeftVectorChange: (Offset) -> Unit,
    onRightVectorChange: (Offset) -> Unit,
    onRightPressChange: (Boolean) -> Unit,
    onLeftTap: () -> Unit,
    onRightTap: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    controlStateLabel: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        JoystickPad(
            insideLabel = "Scroll/Start/Pickup",
            vector = leftVector,
            onVectorChange = onLeftVectorChange,
            onTap = onLeftTap,
            tapZoneScale = 0.90f,
            tapMoveThresholdPx = 34f,
            centerTapRequired = false
        )
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (controlStateLabel != null) {
                ControlStateHud(
                    stateLabel = controlStateLabel
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SlimIconAction(
                    icon = Icons.Filled.Add,
                    contentDescription = "Zoom in",
                    enabled = canZoomIn,
                    onClick = onZoomIn,
                    buttonSize = 36.dp,
                    iconSize = 16.dp
                )
                SlimIconAction(
                    icon = Icons.Filled.Remove,
                    contentDescription = "Zoom out",
                    enabled = canZoomOut,
                    onClick = onZoomOut,
                    buttonSize = 36.dp,
                    iconSize = 16.dp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SlimIconAction(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    enabled = canUndo,
                    onClick = onUndo,
                    buttonSize = 34.dp,
                    iconSize = 15.dp
                )
                SlimIconAction(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    enabled = canRedo,
                    onClick = onRedo,
                    buttonSize = 34.dp,
                    iconSize = 15.dp
                )
            }
        }
        JoystickPad(
            insideLabel = "Cursor/Cancel/Select",
            vector = rightVector,
            onVectorChange = onRightVectorChange,
            onTap = onRightTap,
            onPressChange = onRightPressChange,
            tapZoneScale = 0.92f,
            tapMoveThresholdPx = 38f,
            centerTapRequired = false
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun JoystickPad(
    insideLabel: String,
    vector: Offset,
    onVectorChange: (Offset) -> Unit,
    onTap: (() -> Unit)?,
    onPressChange: ((Boolean) -> Unit)? = null,
    tapZoneScale: Float = 0.44f,
    tapMoveThresholdPx: Float = 10f,
    centerTapRequired: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxRadiusPx = with(LocalDensity.current) { 40.dp.toPx() }
    val padSizePx = with(LocalDensity.current) { 126.dp.toPx() }
    val selectTapRadiusPx = if (onTap != null) {
        maxRadiusPx * tapZoneScale.coerceIn(0.20f, 0.90f)
    } else {
        0f
    }
    val center = Offset(padSizePx / 2f, padSizePx / 2f)
    var activePointerId by remember { mutableIntStateOf(MotionEvent.INVALID_POINTER_ID) }
    var downTimeMs by remember { mutableStateOf(0L) }
    var downPosition by remember { mutableStateOf(Offset.Zero) }
    var downToCenter by remember { mutableFloatStateOf(0f) }
    var maxDisplacementFromDown by remember { mutableFloatStateOf(0f) }
    var tapCandidate by remember(onTap, centerTapRequired, selectTapRadiusPx) { mutableStateOf(false) }
    fun toVector(position: Offset): Offset {
        val delta = position - center
        val distance = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
        if (distance <= 0.0001f) return Offset.Zero
        val clampedDistance = distance.coerceAtMost(maxRadiusPx)
        val nx = delta.x / distance
        val ny = delta.y / distance
        return Offset(
            x = (nx * (clampedDistance / maxRadiusPx)).coerceIn(-1f, 1f),
            y = (ny * (clampedDistance / maxRadiusPx)).coerceIn(-1f, 1f)
        )
    }
    fun resetPointerState() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        tapCandidate = false
        maxDisplacementFromDown = 0f
        onVectorChange(Offset.Zero)
        onPressChange?.invoke(false)
    }
    DisposableEffect(onPressChange) {
        onDispose { resetPointerState() }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(126.dp)
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                                return@pointerInteropFilter true
                            }
                            val index = event.actionIndex
                            if (index < 0 || index >= event.pointerCount) {
                                resetPointerState()
                                return@pointerInteropFilter true
                            }
                            activePointerId = event.getPointerId(index)
                            downTimeMs = event.eventTime
                            downPosition = Offset(event.getX(index), event.getY(index))
                            maxDisplacementFromDown = 0f
                            downToCenter = hypot(
                                (downPosition.x - center.x).toDouble(),
                                (downPosition.y - center.y).toDouble()
                            ).toFloat()
                            tapCandidate = onTap != null &&
                                (!centerTapRequired || downToCenter <= selectTapRadiusPx)
                            onPressChange?.invoke(true)
                            if (tapCandidate) {
                                onVectorChange(Offset.Zero)
                            } else {
                                onVectorChange(toVector(downPosition))
                            }
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                                return@pointerInteropFilter true
                            }
                            val index = event.findPointerIndex(activePointerId)
                            if (index < 0 || index >= event.pointerCount) {
                                resetPointerState()
                                return@pointerInteropFilter true
                            }
                            val position = Offset(event.getX(index), event.getY(index))
                            val displacementFromDown = position - downPosition
                            val displacementMag = hypot(
                                displacementFromDown.x.toDouble(),
                                displacementFromDown.y.toDouble()
                            ).toFloat()
                            if (displacementMag > maxDisplacementFromDown) {
                                maxDisplacementFromDown = displacementMag
                            }
                            if (tapCandidate && maxDisplacementFromDown < tapMoveThresholdPx) {
                                onVectorChange(Offset.Zero)
                            } else {
                                tapCandidate = false
                                onVectorChange(toVector(position))
                            }
                            true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP -> {
                            if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                                resetPointerState()
                                return@pointerInteropFilter true
                            }
                            val index = event.actionIndex
                            if (index < 0 || index >= event.pointerCount) {
                                resetPointerState()
                                return@pointerInteropFilter true
                            }
                            val pointerId = event.getPointerId(index)
                            if (pointerId != activePointerId) {
                                return@pointerInteropFilter true
                            }
                            val upPosition = Offset(event.getX(index), event.getY(index))
                            val upToCenter = hypot(
                                (upPosition.x - center.x).toDouble(),
                                (upPosition.y - center.y).toDouble()
                            ).toFloat()
                            val pressDurationMs = (event.eventTime - downTimeMs).coerceAtLeast(0L)
                            val isTap = tapCandidate &&
                                maxDisplacementFromDown < tapMoveThresholdPx &&
                                onTap != null
                            val lenientQuickTap = onTap != null &&
                                pressDurationMs <= 420L &&
                                maxDisplacementFromDown < (tapMoveThresholdPx * 2.8f) &&
                                upToCenter <= (maxRadiusPx * 0.74f) &&
                                (!centerTapRequired || downToCenter <= (selectTapRadiusPx * 1.35f))
                            resetPointerState()
                            if (isTap || lenientQuickTap) {
                                onTap.invoke()
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            resetPointerState()
                            true
                        }

                        else -> activePointerId != MotionEvent.INVALID_POINTER_ID
                    }
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color(0x225A86B5),
                        radius = size.minDimension * 0.34f,
                        center = c
                    )
                    drawLine(
                        color = Color(0x2C5A86B5),
                        start = Offset(c.x, 10f),
                        end = Offset(c.x, size.height - 10f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x2C5A86B5),
                        start = Offset(10f, c.y),
                        end = Offset(size.width - 10f, c.y),
                        strokeWidth = 1f
                    )
                }
                Text(
                    text = insideLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color(0xFFC5D8EC).copy(alpha = 0.52f),
                    modifier = Modifier.align(Alignment.Center)
                )
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                        .offset {
                            IntOffset(
                                x = (vector.x * maxRadiusPx).roundToInt(),
                                y = (vector.y * maxRadiusPx).roundToInt()
                            )
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {}
            }
        }
    }
}

@Composable
private fun GridScaleBadge(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)),
        modifier = modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Text(
                text = "1 block = $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GridScaleEditorPanel(
    expanded: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.width(158.dp).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Grid", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Step") },
                singleLine = true,
                placeholder = { Text("1'") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Close") }
                TextButton(onClick = onApply) { Text("Set") }
            }
        }
    }
}

@Composable
private fun ParamsPanel(
    expanded: Boolean,
    scope: TakeoffScope,
    params: BlueprintParams,
    takeoffSession: ProjectTakeoffSession,
    onParamsChange: (BlueprintParams) -> Unit,
    onWallHeightChange: (Long) -> Unit,
    onDrywallSheetAreaChange: (Double) -> Unit,
    onDrywallWasteChange: (Double) -> Unit,
    onDrywallScrewsChange: (Int) -> Unit,
    onDrywallMudRateChange: (Double) -> Unit,
    onDrywallIncludeCeilingsChange: (Boolean) -> Unit,
    onConcreteDepthFeetChange: (Double) -> Unit,
    onConcreteWasteChange: (Double) -> Unit,
    onGravelDepthFeetChange: (Double) -> Unit,
    onGravelDensityChange: (Double) -> Unit,
    onGravelWasteChange: (Double) -> Unit,
    onPaintCoverageChange: (Double) -> Unit,
    onPaintCoatsChange: (Int) -> Unit,
    onPaintWasteChange: (Double) -> Unit,
    onScopeExpand: () -> Unit,
    onDetectRooms: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    var heightFt by remember(params.wallHeightMm) { mutableStateOf("%.2f".format(Millimeters(params.wallHeightMm).toFeet())) }
    var drywallSheetArea by remember(takeoffSession.drywall.sheetAreaSqFt) { mutableStateOf(takeoffSession.drywall.sheetAreaSqFt.toString()) }
    var drywallWaste by remember(takeoffSession.drywall.wastePercent) { mutableStateOf(takeoffSession.drywall.wastePercent.toString()) }
    var drywallScrews by remember(takeoffSession.drywall.screwsPerSheet) { mutableStateOf(takeoffSession.drywall.screwsPerSheet.toString()) }
    var drywallMudRate by remember(takeoffSession.drywall.mudGallonsPer100SqFt) { mutableStateOf(takeoffSession.drywall.mudGallonsPer100SqFt.toString()) }
    var concreteDepthInches by remember(takeoffSession.concrete.thicknessFeet) { mutableStateOf("%.2f".format(takeoffSession.concrete.thicknessFeet * 12.0)) }
    var concreteWaste by remember(takeoffSession.concrete.wastePercent) { mutableStateOf(takeoffSession.concrete.wastePercent.toString()) }
    var gravelDepthInches by remember(takeoffSession.gravel.depthFeet) { mutableStateOf("%.2f".format(takeoffSession.gravel.depthFeet * 12.0)) }
    var gravelDensity by remember(takeoffSession.gravel.densityTonsPerYard) { mutableStateOf("%.2f".format(takeoffSession.gravel.densityTonsPerYard)) }
    var gravelWaste by remember(takeoffSession.gravel.wastePercent) { mutableStateOf(takeoffSession.gravel.wastePercent.toString()) }
    var paintCoverage by remember(takeoffSession.paint.coverageSqFtPerGallon) { mutableStateOf(takeoffSession.paint.coverageSqFtPerGallon.toString()) }
    var paintCoats by remember(takeoffSession.paint.coats) { mutableStateOf(takeoffSession.paint.coats.toString()) }
    var paintWaste by remember(takeoffSession.paint.wastePercent) { mutableStateOf(takeoffSession.paint.wastePercent.toString()) }
    var selectedGravelType by remember(takeoffSession.gravel.densityTonsPerYard) {
        mutableStateOf(closestGravelMaterialPreset(takeoffSession.gravel.densityTonsPerYard).label)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)) {
        Card(
            modifier = Modifier
                .width(278.dp)
                .heightIn(max = 548.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
        ) {
            Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${scope.shortLabel()} parameters", style = MaterialTheme.typography.titleSmall)
                }

                when (scope) {
                    TakeoffScope.DRYWALL -> {
                        OutlinedTextField(
                            value = heightFt,
                            onValueChange = {
                                heightFt = it
                                it.toDoubleOrNull()?.let { value ->
                                    onWallHeightChange(Millimeters.fromFeet(value).value)
                                }
                            },
                            label = { Text("Wall height (ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallSheetArea,
                            onValueChange = {
                                drywallSheetArea = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallSheetAreaChange(value)
                                }
                            },
                            label = { Text("Sheet size (sq ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallWaste,
                            onValueChange = {
                                drywallWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallWasteChange(value)
                                    onParamsChange(params.copy(wasteFactorPercent = value.coerceAtLeast(0.0)))
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallScrews,
                            onValueChange = {
                                drywallScrews = it
                                it.toIntOrNull()?.let { value ->
                                    onDrywallScrewsChange(value)
                                }
                            },
                            label = { Text("Screws per sheet") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = drywallMudRate,
                            onValueChange = {
                                drywallMudRate = it
                                it.toDoubleOrNull()?.let { value ->
                                    onDrywallMudRateChange(value)
                                }
                            },
                            label = { Text("Mud gallons / 100 sq ft") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = takeoffSession.drywall.includeCeilings,
                            onClick = { onDrywallIncludeCeilingsChange(!takeoffSession.drywall.includeCeilings) },
                            label = { Text(if (takeoffSession.drywall.includeCeilings) "Ceilings included" else "Ceilings excluded") }
                        )
                    }

                    TakeoffScope.CONCRETE -> {
                        OutlinedTextField(
                            value = concreteDepthInches,
                            onValueChange = {
                                concreteDepthInches = it
                                it.toDoubleOrNull()?.let { value ->
                                    val depthFeet = (value / 12.0).coerceAtLeast(0.0)
                                    onConcreteDepthFeetChange(depthFeet)
                                    onParamsChange(params.copy(concreteThicknessMm = Millimeters.fromFeet(depthFeet).value))
                                }
                            },
                            label = { Text("Depth (in)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = concreteWaste,
                            onValueChange = {
                                concreteWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onConcreteWasteChange(value)
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TakeoffScope.GRAVEL_MULCH -> {
                        OutlinedTextField(
                            value = gravelDepthInches,
                            onValueChange = {
                                gravelDepthInches = it
                                it.toDoubleOrNull()?.let { value ->
                                    val depthFeet = (value / 12.0).coerceAtLeast(0.0)
                                    onGravelDepthFeetChange(depthFeet)
                                    onParamsChange(params.copy(bedDepthMm = Millimeters.fromFeet(depthFeet).value))
                                }
                            },
                            label = { Text("Depth (in)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Material type", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            gravelMaterialPresets.forEach { preset ->
                                FilterChip(
                                    selected = selectedGravelType == preset.label,
                                    onClick = {
                                        selectedGravelType = preset.label
                                        gravelDensity = "%.2f".format(preset.densityTonsPerYard)
                                        onGravelDensityChange(preset.densityTonsPerYard)
                                    },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = gravelDensity,
                            onValueChange = {
                                gravelDensity = it
                                it.toDoubleOrNull()?.let { value ->
                                    onGravelDensityChange(value)
                                    selectedGravelType = closestGravelMaterialPreset(value).label
                                }
                            },
                            label = { Text("Tons per yard³") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gravelWaste,
                            onValueChange = {
                                gravelWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onGravelWasteChange(value)
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TakeoffScope.PAINT -> {
                        OutlinedTextField(
                            value = heightFt,
                            onValueChange = {
                                heightFt = it
                                it.toDoubleOrNull()?.let { value ->
                                    onWallHeightChange(Millimeters.fromFeet(value).value)
                                }
                            },
                            label = { Text("Wall height (ft)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintCoats,
                            onValueChange = {
                                paintCoats = it
                                it.toIntOrNull()?.let { value ->
                                    onPaintCoatsChange(value)
                                    onParamsChange(params.copy(paintCoats = value.coerceAtLeast(1)))
                                }
                            },
                            label = { Text("Coats") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintCoverage,
                            onValueChange = {
                                paintCoverage = it
                                it.toDoubleOrNull()?.let { value ->
                                    onPaintCoverageChange(value)
                                }
                            },
                            label = { Text("Coverage (sq ft / gal)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = paintWaste,
                            onValueChange = {
                                paintWaste = it
                                it.toDoubleOrNull()?.let { value ->
                                    onPaintWasteChange(value)
                                    onParamsChange(params.copy(wasteFactorPercent = value.coerceAtLeast(0.0)))
                                }
                            },
                            label = { Text("Waste (%)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("Room tools", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = onDetectRooms,
                        label = { Text("Detect Rooms") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onScopeExpand,
                        label = { Text("Tag Rooms") }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpeningAddonsPanel(
    panelType: OpeningPanelType,
    selectedPreset: OpeningPreset,
    presets: List<OpeningPreset>,
    customWidthFeet: String,
    customHeightFeet: String,
    customSillFeet: String,
    showPresets: Boolean,
    onTogglePresets: () -> Unit,
    onSelectPreset: (OpeningPreset) -> Unit,
    onStartDragPreset: (OpeningPreset, Offset) -> Unit,
    onDragPreset: (Offset) -> Unit,
    onEndDragPreset: () -> Unit,
    onCustomWidthChange: (String) -> Unit,
    onCustomHeightChange: (String) -> Unit,
    onCustomSillChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val type = when (panelType) {
        OpeningPanelType.DOORS -> OpeningType.DOOR
        OpeningPanelType.WINDOWS -> OpeningType.WINDOW
        OpeningPanelType.STAIR_UP -> OpeningType.STAIR_UP
        OpeningPanelType.STAIR_DOWN -> OpeningType.STAIR_DOWN
    }
    val panelTitle = when (panelType) {
        OpeningPanelType.DOORS -> "Doors"
        OpeningPanelType.WINDOWS -> "Windows"
        OpeningPanelType.STAIR_UP -> "Stairs Up"
        OpeningPanelType.STAIR_DOWN -> "Stairs Down"
    }
    val heightLabel = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "Run"
        else -> "Height"
    }
    val sillLabel = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "Rise"
        else -> "Sill"
    }
    val widthHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "3', 4ft, 1200mm"
        else -> "3', 3.5ft, 900mm"
    }
    val heightHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "9', 10ft, 3000mm"
        else -> "7', 7ft, 2100mm"
    }
    val sillHint = when (type) {
        OpeningType.STAIR_UP,
        OpeningType.STAIR_DOWN -> "0', 1ft, 300mm"
        else -> "0', 3ft, 900mm"
    }
    val customPreset = OpeningPreset(
        name = when (type) {
            OpeningType.DOOR -> "Custom Door"
            OpeningType.WINDOW -> "Custom Window"
            OpeningType.STAIR_UP -> "Custom Stair Up"
            OpeningType.STAIR_DOWN -> "Custom Stair Down"
        },
        type = type,
        widthMm = DimensionParser.parseLengthToMillimeters(customWidthFeet)?.coerceAtLeast(1L) ?: selectedPreset.widthMm,
        heightMm = DimensionParser.parseLengthToMillimeters(customHeightFeet)?.coerceAtLeast(1L) ?: selectedPreset.heightMm,
        sillMm = DimensionParser.parseLengthToMillimeters(customSillFeet)?.coerceAtLeast(0L) ?: selectedPreset.sillMm
    )
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)) {
        Column(
            modifier = Modifier.width(214.dp).padding(7.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(panelTitle, style = MaterialTheme.typography.titleSmall)
                FilterChip(
                    selected = showPresets,
                    onClick = onTogglePresets,
                    label = { Text("Presets") }
                )
            }
            Text(
                "Set exact size, then drag onto a wall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = customWidthFeet,
                onValueChange = onCustomWidthChange,
                label = { Text("Width") },
                singleLine = true,
                supportingText = { Text(widthHint, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customHeightFeet,
                onValueChange = onCustomHeightChange,
                label = { Text(heightLabel) },
                singleLine = true,
                supportingText = { Text(heightHint, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customSillFeet,
                onValueChange = onCustomSillChange,
                label = { Text(sillLabel) },
                singleLine = true,
                supportingText = { Text(sillHint, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth()
            )
            AddonPresetCard(
                preset = customPreset,
                selected = false,
                icon = {
                    Icon(
                        imageVector = when (type) {
                            OpeningType.DOOR -> Icons.Filled.DoorFront
                            OpeningType.WINDOW -> Icons.Filled.Window
                            OpeningType.STAIR_UP -> Icons.Filled.KeyboardArrowUp
                            OpeningType.STAIR_DOWN -> Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                },
                onClick = { onSelectPreset(customPreset) },
                onDragStart = { rootPoint -> onStartDragPreset(customPreset, rootPoint) },
                onDrag = onDragPreset,
                onDragEnd = onEndDragPreset
            )
            if (showPresets) {
                presets.forEach { preset ->
                    AddonPresetCard(
                        preset = preset,
                        selected = selectedPreset == preset,
                        icon = {
                            Icon(
                                imageVector = when (preset.type) {
                                    OpeningType.DOOR -> Icons.Filled.DoorFront
                                    OpeningType.WINDOW -> Icons.Filled.Window
                                    OpeningType.STAIR_UP -> Icons.Filled.KeyboardArrowUp
                                    OpeningType.STAIR_DOWN -> Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null
                            )
                        },
                        onClick = { onSelectPreset(preset) },
                        onDragStart = { rootPoint -> onStartDragPreset(preset, rootPoint) },
                        onDrag = onDragPreset,
                        onDragEnd = onEndDragPreset
                    )
                }
            }
        }
    }
}

@Composable
private fun RailHelpPanel(
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (!expanded) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.width(314.dp).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text("Rail Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Fast controls for drawing, snapping, and takeoff flow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RailHelpLine(title = "Delete", detail = "Removes selected wall/opening.")
            RailHelpLine(title = "Select", detail = "Tap walls/openings to edit or remove.")
            RailHelpLine(title = "Draw", detail = "Tap start and end points to create walls.")
            RailHelpLine(title = "Trade", detail = "Cycles drywall, concrete, gravel, and paint.")
            RailHelpLine(title = "Chain", detail = "Continue from last clicked corner on each wall.")
            RailHelpLine(title = "Split", detail = "Detach next wall from the current chain.")
            RailHelpLine(title = "Doors/Windows/Stairs", detail = "Open panel, size it, and drag onto walls.")
            RailHelpLine(title = "Floor", detail = "Step floors up/down: Ground, 2, 3... and Basement levels.")
            RailHelpLine(title = "Params", detail = "Toggles snaps, joystick behavior, and scope settings.")
            RailHelpLine(title = "Undo/Redo + Zoom", detail = "Left side of the top rail.")
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            ) {
                Text(
                    text = "Tip: With dual joysticks on, use right stick for cursor and left for pan/select.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RailHelpLine(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$title:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddonPresetCard(
    preset: OpeningPreset,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    Card(
        modifier = Modifier
            .onGloballyPositioned { rootOffset = it.positionInRoot() }
            .pointerInput(preset) {
                detectDragGestures(
                    onDragStart = { start ->
                        onDragStart(rootOffset + start)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, _ ->
                        onDrag(rootOffset + change.position)
                        change.consume()
                    }
                )
            },
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column {
                Text(preset.name, style = MaterialTheme.typography.bodySmall)
                Text(
                    when (preset.type) {
                        OpeningType.DOOR -> "Door swing arc"
                        OpeningType.WINDOW -> "Window break"
                        OpeningType.STAIR_UP -> "Stair opening up"
                        OpeningType.STAIR_DOWN -> "Stair opening down"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectionPanel(
    selectedWall: WallSegment?,
    selectedOpening: BlueprintOpening?,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 188.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Button(onClick = onDeselect, modifier = Modifier.padding(0.dp).height(30.dp)) { Text("×") }
            }

            when {
                selectedWall != null -> {
                    Text("Wall", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Length: ${formatFeetInchesPrime(Millimeters(selectedWall.lengthMillimeters()).toFeet())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Height: ${formatFeetInchesPrime(selectedWall.height.toFeet())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Use trash icon in bottom bar to delete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                selectedOpening != null -> {
                    Text(selectedOpening.type.displayLabel(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Width: ${formatFeetInchesPrime(Millimeters(selectedOpening.widthMm).toFeet())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${if (selectedOpening.type.isStair()) "Run" else "Height"}: ${formatFeetInchesPrime(Millimeters(selectedOpening.heightMm).toFeet())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (selectedOpening.type.isStair()) {
                        Text(
                            "Rise: ${formatFeetInchesPrime(Millimeters(selectedOpening.sillMm).toFeet())}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "Use trash icon in bottom bar to delete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveOverlay(
    doc: BlueprintDocument,
    wallLengthFeet: Double,
    netArea: Double,
    currentScope: TakeoffScope,
    liveScopeQuantity: LiveScopeQuantity,
    selectedFloor: BlueprintFloorLevel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 178.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD0081B31)),
        border = BorderStroke(1.dp, Color(0x628CC8FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live",
                    color = Color(0xFFC2E2FF),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x553D6F98),
                    border = BorderStroke(1.dp, Color(0x6B8FC2EB))
                ) {
                    Text(
                        text = selectedFloor.label(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color(0xFFEAF6FF),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            LiveMetricRow(label = "Scope", value = currentScope.shortLabel())
            LiveMetricRow(label = "Perimeter", value = formatFeetInchesPrime(wallLengthFeet))
            LiveMetricRow(label = "Net Wall Area", value = "${formatLiveValue(netArea, 1)} sq ft")
            LiveMetricRow(label = "Rooms/Openings", value = "${doc.rooms.size} / ${doc.openings.size}")
            LiveMetricRow(
                label = "Qty (${liveScopeQuantity.label})",
                value = liveScopeQuantity.value,
                emphasize = true
            )
        }
    }
}

@Composable
private fun LiveMetricRow(
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
            color = if (emphasize) Color(0xFFD1E9FF) else Color(0xFFAED4F3),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color(0xFFF6FBFF),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun CursorCoordinateOverlay(
    worldPoint: PointMm?,
    showRotate: Boolean,
    onRotate: () -> Unit,
    rotateButtonModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    if (worldPoint == null) return
    Column(
        modifier = modifier.widthIn(min = 122.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showRotate) {
            SlimIconAction(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = "Rotate picked wall +5 degrees",
                enabled = true,
                onClick = onRotate,
                buttonSize = 30.dp,
                iconSize = 14.dp,
                modifier = rotateButtonModifier
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xC9091A2E))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "Coords",
                    color = Color(0xFFB8DBFF),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "X ${formatSignedFeetInchesPrime(worldPoint.x)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Y ${formatSignedFeetInchesPrime(worldPoint.y)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ControlStateHud(
    stateLabel: String,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "control-state-hud")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 920, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "control-state-hud-pulse"
        ).value
    val (containerColor, borderColor, textColor) = when (stateLabel) {
        "Draw" -> Triple(Color(0xD9F3D35E), Color(0xFFFFE088), Color(0xFF201605))
        "Selected" -> Triple(Color(0xD93A6EA9), Color(0xFF7DC1FF), Color(0xFFF0F8FF))
        "Picked Up" -> Triple(Color(0xD93B7A51), Color(0xFF86E7A8), Color(0xFFF3FFF7))
        else -> Triple(Color(0xB9223347), Color(0x5A6F8EAC), Color(0xFF9FB5CB))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor.copy(alpha = 0.83f + (pulse * 0.09f)),
        border = BorderStroke((1f + (0.35f * pulse)).dp, borderColor.copy(alpha = 0.72f + (pulse * 0.28f)))
    ) {
        Text(
            text = stateLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ControlStateChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val container = if (active) {
        Color(0xFF1F6FAE)
    } else {
        Color(0x52304D68)
    }
    val textColor = if (active) Color(0xFFEFFFFF) else Color(0xFFAECBE4)
    Surface(
        color = container,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) Color(0xFF74C0FF) else Color(0x663C5A79)
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private data class LiveScopeQuantity(
    val label: String,
    val value: String
)

private fun computeLiveScopeQuantity(
    document: BlueprintDocument,
    scope: TakeoffScope,
    takeoffSession: ProjectTakeoffSession
): LiveScopeQuantity {
    return when (scope) {
        TakeoffScope.DRYWALL -> {
            val sheets = BlueprintTakeoffCalculator.drywallTakeoff(
                document = document,
                sheetAreaSqFt = takeoffSession.drywall.sheetAreaSqFt,
                wastePercent = takeoffSession.drywall.wastePercent,
                screwsPerSheet = takeoffSession.drywall.screwsPerSheet,
                mudGallonsPer100SqFt = takeoffSession.drywall.mudGallonsPer100SqFt,
                includeCeilings = takeoffSession.drywall.includeCeilings
            ).items.firstOrNull { it.name == "Drywall sheets" }?.quantity ?: 0.0
            LiveScopeQuantity(
                label = "sheets",
                value = "${formatLiveValue(sheets, 0)} sheets"
            )
        }

        TakeoffScope.CONCRETE -> {
            val yards = BlueprintTakeoffCalculator.concreteTakeoff(
                document = document,
                thicknessFeet = takeoffSession.concrete.thicknessFeet,
                wastePercent = takeoffSession.concrete.wastePercent
            ).items.firstOrNull()?.quantity ?: 0.0
            LiveScopeQuantity(
                label = "volume",
                value = "${formatLiveValue(yards, 2)} yd^3"
            )
        }

        TakeoffScope.GRAVEL_MULCH -> {
            val takeoff = BlueprintTakeoffCalculator.gravelMulchTakeoff(
                document = document,
                depthFeet = takeoffSession.gravel.depthFeet,
                densityTonsPerYard = takeoffSession.gravel.densityTonsPerYard,
                wastePercent = takeoffSession.gravel.wastePercent
            )
            val tons = takeoff.items.firstOrNull { it.unit.contains("tons", ignoreCase = true) }?.quantity ?: 0.0
            val yards = takeoff.items.firstOrNull { it.unit.contains("yards", ignoreCase = true) }?.quantity ?: 0.0
            LiveScopeQuantity(
                label = "material",
                value = "${formatLiveValue(tons, 2)} tons (${formatLiveValue(yards, 2)} yd^3)"
            )
        }

        TakeoffScope.PAINT -> {
            val gallons = BlueprintTakeoffCalculator.paintTakeoff(
                document = document,
                coverageSqFtPerGallon = takeoffSession.paint.coverageSqFtPerGallon,
                coats = takeoffSession.paint.coats,
                wastePercent = takeoffSession.paint.wastePercent
            ).items.firstOrNull()?.quantity ?: 0.0
            LiveScopeQuantity(
                label = "coverage",
                value = "${formatLiveValue(gallons, 2)} gallons"
            )
        }
    }
}

private fun DrawScope.drawBlueprintTexturePattern() {
    val diagonalSpacing = 32f
    var startX = -size.height
    while (startX <= size.width + size.height) {
        drawLine(
            color = BLUEPRINT_TEXTURE_DIAGONAL_A,
            start = Offset(startX, 0f),
            end = Offset(startX + size.height, size.height),
            strokeWidth = 1f
        )
        startX += diagonalSpacing
    }
    var reverseStartX = 0f
    while (reverseStartX <= size.width + size.height) {
        drawLine(
            color = BLUEPRINT_TEXTURE_DIAGONAL_B,
            start = Offset(reverseStartX, 0f),
            end = Offset(reverseStartX - size.height, size.height),
            strokeWidth = 0.8f
        )
        reverseStartX += diagonalSpacing * 1.45f
    }
    val dotSpacing = 70f
    var x = 20f
    while (x < size.width) {
        var y = 16f
        while (y < size.height) {
            drawCircle(
                color = BLUEPRINT_TEXTURE_NOISE_DOT,
                radius = 1.05f,
                center = Offset(x, y)
            )
            y += dotSpacing
        }
        x += dotSpacing
    }
}

private fun DrawScope.drawStyledWallSegment(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    selected: Boolean,
    pulse: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val magnitude = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val nx = -dy / magnitude
    val ny = dx / magnitude
    val shadowOffset = Offset(nx * 1.5f, ny * 1.5f)
    val highlightOffset = Offset(-nx * 0.8f, -ny * 0.8f)
    val pulseBoost = if (selected) (1.4f * pulse) else 0f

    drawLine(
        color = GEOMETRY_DEPTH_SHADOW,
        start = start + shadowOffset,
        end = end + shadowOffset,
        strokeWidth = strokeWidth + 3.8f + pulseBoost,
        cap = StrokeCap.Square
    )
    drawLine(
        color = GEOMETRY_HALO_COLOR,
        start = start,
        end = end,
        strokeWidth = strokeWidth + 2.4f + (pulseBoost * 0.45f),
        cap = StrokeCap.Square
    )
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth + (pulseBoost * 0.2f),
        cap = StrokeCap.Square
    )
    drawLine(
        color = GEOMETRY_DEPTH_HIGHLIGHT.copy(alpha = if (selected) 0.82f else 0.42f),
        start = start + highlightOffset,
        end = end + highlightOffset,
        strokeWidth = (strokeWidth * 0.34f).coerceAtLeast(1.15f),
        cap = StrokeCap.Square
    )
    if (selected) {
        drawLine(
            color = GEOMETRY_SELECTION_PULSE.copy(alpha = 0.24f + (0.3f * pulse)),
            start = start,
            end = end,
            strokeWidth = strokeWidth + 5.2f + (pulseBoost * 0.6f),
            cap = StrokeCap.Square
        )
    }
}

private fun DrawScope.drawPrecisionPulse(
    center: Offset,
    progress: Float,
    color: Color,
    baseRadius: Float,
    maxRadius: Float
) {
    val clamped = progress.coerceIn(0f, 1f)
    val radius = baseRadius + ((maxRadius - baseRadius) * clamped)
    val alpha = (1f - clamped).coerceIn(0f, 1f)
    drawCircle(
        color = color.copy(alpha = 0.62f * alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 1.7f)
    )
    drawCircle(
        color = color.copy(alpha = 0.2f * alpha),
        radius = radius * 0.55f,
        center = center
    )
}

private fun DrawScope.drawOpeningOnWall(
    worldToScreen: (PointMm) -> Offset,
    wall: WallSegment,
    t: Double,
    widthMm: Long,
    type: OpeningType,
    swingTag: String?,
    color: Color,
    emphasized: Boolean,
    emphasisPulse: Float = 0f
) {
    val center = BlueprintSnapMath.pointOnWall(wall, t.coerceIn(0.0, 1.0))
    val wallDx = (wall.end.x - wall.start.x).toDouble()
    val wallDy = (wall.end.y - wall.start.y).toDouble()
    val wallLength = hypot(wallDx, wallDy)
    if (wallLength <= 0.0001) return

    val halfWidth = widthMm.coerceAtLeast(1L) / 2.0
    val ux = wallDx / wallLength
    val uy = wallDy / wallLength
    val nx = -uy
    val ny = ux

    val hinge = PointMm(
        x = (center.x - (ux * halfWidth)).roundToLong(),
        y = (center.y - (uy * halfWidth)).roundToLong()
    )
    val latch = PointMm(
        x = (center.x + (ux * halfWidth)).roundToLong(),
        y = (center.y + (uy * halfWidth)).roundToLong()
    )

    val hingeScreen = worldToScreen(hinge)
    val latchScreen = worldToScreen(latch)
    val emphasisBoost = if (emphasized) (0.6f * emphasisPulse.coerceIn(0f, 1f)) else 0f
    val baseStroke = if (emphasized) 4.6f + emphasisBoost else 3.6f
    val haloColor = GEOMETRY_HALO_COLOR

    if (type == OpeningType.WINDOW) {
        drawLine(haloColor, hingeScreen, latchScreen, strokeWidth = baseStroke + 2.6f, cap = StrokeCap.Round)
        drawLine(color, hingeScreen, latchScreen, strokeWidth = baseStroke, cap = StrokeCap.Round)
        val axisX = latchScreen.x - hingeScreen.x
        val axisY = latchScreen.y - hingeScreen.y
        val axisLength = hypot(axisX.toDouble(), axisY.toDouble()).toFloat().coerceAtLeast(1f)
        val normalX = -axisY / axisLength
        val normalY = axisX / axisLength
        val centerScreen = Offset(
            x = (hingeScreen.x + latchScreen.x) / 2f,
            y = (hingeScreen.y + latchScreen.y) / 2f
        )
        val slashHalf = min(axisLength * 0.22f, 12f).coerceAtLeast(6f)
        drawLine(
            color = haloColor,
            start = Offset(centerScreen.x - (normalX * slashHalf), centerScreen.y - (normalY * slashHalf)),
            end = Offset(centerScreen.x + (normalX * slashHalf), centerScreen.y + (normalY * slashHalf)),
            strokeWidth = if (emphasized) 6f else 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(centerScreen.x - (normalX * slashHalf), centerScreen.y - (normalY * slashHalf)),
            end = Offset(centerScreen.x + (normalX * slashHalf), centerScreen.y + (normalY * slashHalf)),
            strokeWidth = if (emphasized) 3.9f else 3f,
            cap = StrokeCap.Round
        )
        return
    }

    if (type == OpeningType.STAIR_UP || type == OpeningType.STAIR_DOWN) {
        val axisX = latchScreen.x - hingeScreen.x
        val axisY = latchScreen.y - hingeScreen.y
        val axisLength = hypot(axisX.toDouble(), axisY.toDouble()).toFloat().coerceAtLeast(1f)
        val axisUx = axisX / axisLength
        val axisUy = axisY / axisLength
        val normalX = -axisUy
        val normalY = axisUx
        val depth = min(axisLength * 0.65f, 26f).coerceAtLeast(12f)
        fun lerp(start: Offset, end: Offset, t: Float): Offset = Offset(
            x = start.x + ((end.x - start.x) * t),
            y = start.y + ((end.y - start.y) * t)
        )
        val a = hingeScreen
        val b = latchScreen
        val c = Offset(x = b.x + (normalX * depth), y = b.y + (normalY * depth))
        val d = Offset(x = a.x + (normalX * depth), y = a.y + (normalY * depth))
        val outlineHalo = if (emphasized) 6.2f else 5.2f
        val outline = if (emphasized) 4.1f else 3.2f
        listOf(a to b, b to c, c to d, d to a).forEach { (start, end) ->
            drawLine(color = haloColor, start = start, end = end, strokeWidth = outlineHalo, cap = StrokeCap.Round)
            drawLine(color = color, start = start, end = end, strokeWidth = outline, cap = StrokeCap.Round)
        }
        val stepCount = 5
        for (index in 1 until stepCount) {
            val tStep = index / stepCount.toFloat()
            val start = lerp(a, d, tStep)
            val end = lerp(b, c, tStep)
            drawLine(color = haloColor, start = start, end = end, strokeWidth = outlineHalo - 0.8f, cap = StrokeCap.Round)
            drawLine(color = color, start = start, end = end, strokeWidth = outline - 0.9f, cap = StrokeCap.Round)
        }
        val center = Offset(x = (a.x + b.x + c.x + d.x) / 4f, y = (a.y + b.y + c.y + d.y) / 4f)
        val direction = if (type == OpeningType.STAIR_UP) 1f else -1f
        val arrowHalf = (axisLength * 0.2f).coerceIn(7f, 15f)
        val from = Offset(
            x = center.x - (axisUx * arrowHalf * direction),
            y = center.y - (axisUy * arrowHalf * direction)
        )
        val to = Offset(
            x = center.x + (axisUx * arrowHalf * direction),
            y = center.y + (axisUy * arrowHalf * direction)
        )
        drawLine(color = haloColor, start = from, end = to, strokeWidth = if (emphasized) 5.4f else 4.2f, cap = StrokeCap.Round)
        drawLine(color = color, start = from, end = to, strokeWidth = if (emphasized) 3.3f else 2.5f, cap = StrokeCap.Round)
        val head = 6.8f
        val left = Offset(
            x = to.x - (axisUx * head * direction) + (normalX * head * 0.7f),
            y = to.y - (axisUy * head * direction) + (normalY * head * 0.7f)
        )
        val right = Offset(
            x = to.x - (axisUx * head * direction) - (normalX * head * 0.7f),
            y = to.y - (axisUy * head * direction) - (normalY * head * 0.7f)
        )
        drawLine(color = haloColor, start = to, end = left, strokeWidth = if (emphasized) 5f else 4f, cap = StrokeCap.Round)
        drawLine(color = haloColor, start = to, end = right, strokeWidth = if (emphasized) 5f else 4f, cap = StrokeCap.Round)
        drawLine(color = color, start = to, end = left, strokeWidth = if (emphasized) 3.2f else 2.3f, cap = StrokeCap.Round)
        drawLine(color = color, start = to, end = right, strokeWidth = if (emphasized) 3.2f else 2.3f, cap = StrokeCap.Round)
        return
    }

    val swingSide = if (swingTag == DOOR_SWING_NEG_TAG) -1.0 else 1.0
    val openPoint = PointMm(
        x = (hinge.x + (nx * swingSide * widthMm.coerceAtLeast(1L))).roundToLong(),
        y = (hinge.y + (ny * swingSide * widthMm.coerceAtLeast(1L))).roundToLong()
    )
    val openScreen = worldToScreen(openPoint)

    drawLine(
        color = haloColor,
        start = hingeScreen,
        end = latchScreen,
        strokeWidth = if (emphasized) 6.6f else 5.6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = haloColor,
        start = hingeScreen,
        end = openScreen,
        strokeWidth = if (emphasized) 6.6f else 5.6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = hingeScreen,
        end = latchScreen,
        strokeWidth = if (emphasized) 4.4f else 3.4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = hingeScreen,
        end = openScreen,
        strokeWidth = if (emphasized) 4.4f else 3.4f,
        cap = StrokeCap.Round
    )

    val radius = hypot(
        (latchScreen.x - hingeScreen.x).toDouble(),
        (latchScreen.y - hingeScreen.y).toDouble()
    ).toFloat()
    val startAngle = atan2(
        (latchScreen.y - hingeScreen.y).toDouble(),
        (latchScreen.x - hingeScreen.x).toDouble()
    )
    val endAngle = atan2(
        (openScreen.y - hingeScreen.y).toDouble(),
        (openScreen.x - hingeScreen.x).toDouble()
    )
    var delta = endAngle - startAngle
    while (delta > Math.PI) delta -= Math.PI * 2.0
    while (delta < -Math.PI) delta += Math.PI * 2.0

    val path = Path()
    val segments = 14
    for (index in 0..segments) {
        val ratio = index.toDouble() / segments.toDouble()
        val angle = startAngle + (delta * ratio)
        val x = hingeScreen.x + (cos(angle) * radius).toFloat()
        val y = hingeScreen.y + (sin(angle) * radius).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        color = haloColor,
        style = Stroke(width = if (emphasized) 5.8f else 4.6f)
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = if (emphasized) 3.6f else 2.8f)
    )
}

private fun DrawScope.drawFloatingOpeningPreview(
    center: Offset,
    widthPx: Float,
    type: OpeningType,
    color: Color
) {
    val half = widthPx / 2f
    val haloColor = GEOMETRY_HALO_COLOR
    if (type == OpeningType.WINDOW) {
        drawLine(
            color = haloColor,
            start = Offset(center.x - half, center.y),
            end = Offset(center.x + half, center.y),
            strokeWidth = 6.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x - half, center.y),
            end = Offset(center.x + half, center.y),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        val slashHalf = min(half * 0.28f, 10f).coerceAtLeast(6f)
        drawLine(
            color = haloColor,
            start = Offset(center.x - slashHalf, center.y - slashHalf),
            end = Offset(center.x + slashHalf, center.y + slashHalf),
            strokeWidth = 5.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x - slashHalf, center.y - slashHalf),
            end = Offset(center.x + slashHalf, center.y + slashHalf),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
        return
    }

    if (type == OpeningType.STAIR_UP || type == OpeningType.STAIR_DOWN) {
        val depth = (widthPx * 0.58f).coerceIn(12f, 30f)
        fun lerp(start: Offset, end: Offset, t: Float): Offset = Offset(
            x = start.x + ((end.x - start.x) * t),
            y = start.y + ((end.y - start.y) * t)
        )
        val a = Offset(center.x - half, center.y)
        val b = Offset(center.x + half, center.y)
        val c = Offset(center.x + half, center.y - depth)
        val d = Offset(center.x - half, center.y - depth)
        listOf(a to b, b to c, c to d, d to a).forEach { (start, end) ->
            drawLine(haloColor, start, end, strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(color, start, end, strokeWidth = 3.7f, cap = StrokeCap.Round)
        }
        val stepCount = 5
        for (index in 1 until stepCount) {
            val tStep = index / stepCount.toFloat()
            val start = lerp(a, d, tStep)
            val end = lerp(b, c, tStep)
            drawLine(haloColor, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(color, start, end, strokeWidth = 2.8f, cap = StrokeCap.Round)
        }
        val direction = if (type == OpeningType.STAIR_UP) 1f else -1f
        val arrowHalf = (widthPx * 0.2f).coerceIn(7f, 15f)
        val arrowCenterY = center.y - (depth * 0.5f)
        val from = Offset(center.x - (arrowHalf * direction), arrowCenterY)
        val to = Offset(center.x + (arrowHalf * direction), arrowCenterY)
        drawLine(haloColor, from, to, strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(color, from, to, strokeWidth = 3f, cap = StrokeCap.Round)
        val head = 7f
        val up = Offset(to.x - (head * direction), to.y - (head * 0.7f))
        val down = Offset(to.x - (head * direction), to.y + (head * 0.7f))
        drawLine(haloColor, to, up, strokeWidth = 4.6f, cap = StrokeCap.Round)
        drawLine(haloColor, to, down, strokeWidth = 4.6f, cap = StrokeCap.Round)
        drawLine(color, to, up, strokeWidth = 2.7f, cap = StrokeCap.Round)
        drawLine(color, to, down, strokeWidth = 2.7f, cap = StrokeCap.Round)
        return
    }

    val hinge = Offset(center.x - half, center.y)
    val latch = Offset(center.x + half, center.y)
    val open = Offset(center.x - half, center.y - widthPx)
    drawLine(haloColor, hinge, latch, strokeWidth = 6.2f, cap = StrokeCap.Round)
    drawLine(haloColor, hinge, open, strokeWidth = 6.2f, cap = StrokeCap.Round)
    drawLine(color, hinge, latch, strokeWidth = 3.6f, cap = StrokeCap.Round)
    drawLine(color, hinge, open, strokeWidth = 3.6f, cap = StrokeCap.Round)

    val path = Path()
    val startAngle = 0.0
    val endAngle = -Math.PI / 2.0
    val segments = 12
    for (index in 0..segments) {
        val ratio = index.toDouble() / segments.toDouble()
        val angle = startAngle + ((endAngle - startAngle) * ratio)
        val x = hinge.x + (cos(angle) * widthPx).toFloat()
        val y = hinge.y + (sin(angle) * widthPx).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(path = path, color = haloColor, style = Stroke(width = 4.8f))
    drawPath(path = path, color = color, style = Stroke(width = 2.8f))
}

private enum class CursorGlyph {
    ARROW,
    PENCIL,
    HAND_POINTER,
    GRAB
}

private fun DrawScope.drawCursorGlyph(position: Offset, glyph: CursorGlyph, sizeScale: Float) {
    when (glyph) {
        CursorGlyph.ARROW -> drawArrowCursor(position, sizeScale)
        CursorGlyph.PENCIL -> drawPencilCursor(position, sizeScale)
        CursorGlyph.HAND_POINTER -> drawHandPointerCursor(position, sizeScale)
        CursorGlyph.GRAB -> drawGrabHandCursor(position, sizeScale)
    }
}

private fun DrawScope.drawArrowCursor(position: Offset, sizeScale: Float) {
    fun s(value: Float): Float = value * sizeScale
    val pointer = Path().apply {
        moveTo(position.x, position.y)
        lineTo(position.x + s(14f), position.y + s(33f))
        lineTo(position.x + s(18f), position.y + s(22f))
        lineTo(position.x + s(28f), position.y + s(28f))
        lineTo(position.x + s(31f), position.y + s(22f))
        lineTo(position.x + s(21f), position.y + s(17f))
        lineTo(position.x + s(28f), position.y + s(10f))
        close()
    }
    drawPath(
        path = pointer,
        color = Color(0xAA000000),
        style = Stroke(width = s(2.4f).coerceAtLeast(1.2f))
    )
    drawPath(
        path = pointer,
        color = Color(0xFFF9FCFF)
    )
}

private fun DrawScope.drawPencilCursor(position: Offset, sizeScale: Float) {
    fun s(value: Float): Float = value * sizeScale
    val tip = position
    val tail = Offset(position.x + s(26f), position.y + s(28f))
    drawLine(
        color = Color(0xB7050A12),
        start = tip,
        end = tail,
        strokeWidth = s(8.2f).coerceAtLeast(2f),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFDD74),
        start = tip,
        end = tail,
        strokeWidth = s(5.3f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF4D2B06),
        start = tip,
        end = Offset(position.x + s(6.8f), position.y + s(7.3f)),
        strokeWidth = s(3.2f).coerceAtLeast(1.1f),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFFFDF6E3),
        radius = s(2.2f).coerceAtLeast(1f),
        center = tip
    )
}

private fun DrawScope.drawHandPointerCursor(position: Offset, sizeScale: Float) {
    fun s(value: Float): Float = value * sizeScale
    val palmCenter = Offset(position.x + s(14f), position.y + s(21f))
    drawCircle(
        color = Color(0xB708111C),
        radius = s(11.3f).coerceAtLeast(3f),
        center = palmCenter
    )
    drawCircle(
        color = Color(0xFFDEE9F7),
        radius = s(9.2f).coerceAtLeast(2.5f),
        center = palmCenter
    )
    drawRect(
        color = Color(0xB708111C),
        topLeft = Offset(position.x + s(10f), position.y + s(2f)),
        size = Size(s(8f), s(20.5f))
    )
    drawRect(
        color = Color(0xFFDEE9F7),
        topLeft = Offset(position.x + s(11f), position.y + s(3f)),
        size = Size(s(6f), s(18.5f))
    )
    drawCircle(
        color = Color(0xFFDEE9F7),
        radius = s(4.4f).coerceAtLeast(1.2f),
        center = Offset(position.x + s(7.4f), position.y + s(18.5f))
    )
}

private fun DrawScope.drawGrabHandCursor(position: Offset, sizeScale: Float) {
    fun s(value: Float): Float = value * sizeScale
    val fistCenter = Offset(position.x + s(14f), position.y + s(16f))
    drawCircle(
        color = Color(0xB708111C),
        radius = s(12f).coerceAtLeast(3f),
        center = fistCenter
    )
    drawCircle(
        color = Color(0xFFE4F4EA),
        radius = s(9.6f).coerceAtLeast(2.5f),
        center = fistCenter
    )
    val knuckleColor = Color(0xFFB7E3C4)
    drawCircle(color = knuckleColor, radius = s(2.6f).coerceAtLeast(0.9f), center = Offset(position.x + s(7.2f), position.y + s(12.4f)))
    drawCircle(color = knuckleColor, radius = s(2.6f).coerceAtLeast(0.9f), center = Offset(position.x + s(12.8f), position.y + s(10.7f)))
    drawCircle(color = knuckleColor, radius = s(2.6f).coerceAtLeast(0.9f), center = Offset(position.x + s(18.3f), position.y + s(11.1f)))
    drawCircle(color = knuckleColor, radius = s(2.6f).coerceAtLeast(0.9f), center = Offset(position.x + s(22.9f), position.y + s(13.2f)))
}

private fun DrawScope.drawSelectionMagnifier(
    pointer: Offset,
    nearestWall: WallSegment?,
    nearestPoint: PointMm?,
    worldToScreen: (PointMm) -> Offset,
    boostActive: Boolean
) {
    val radius = POINTER_LENS_RADIUS_PX
    val lensCenter = Offset(
        x = (pointer.x + POINTER_LENS_OFFSET_PX.x).coerceIn(radius + 6f, size.width - radius - 6f),
        y = (pointer.y + POINTER_LENS_OFFSET_PX.y).coerceIn(radius + 6f, size.height - radius - 6f)
    )
    drawLine(
        color = Color(0x88ACD7FF),
        start = pointer,
        end = lensCenter,
        strokeWidth = if (boostActive) 2.6f else 2f
    )

    val lensPath = Path().apply {
        addOval(
            Rect(
                left = lensCenter.x - radius,
                top = lensCenter.y - radius,
                right = lensCenter.x + radius,
                bottom = lensCenter.y + radius
            )
        )
    }
    clipPath(lensPath) {
        drawCircle(
            color = if (boostActive) Color(0xEF0D243B) else Color(0xDE0A1A2C),
            radius = radius,
            center = lensCenter
        )
        val gridColor = if (boostActive) Color(0x528FD2FF) else Color(0x3E7FAACD)
        drawLine(
            color = gridColor,
            start = Offset(lensCenter.x - radius, lensCenter.y),
            end = Offset(lensCenter.x + radius, lensCenter.y),
            strokeWidth = 1.1f
        )
        drawLine(
            color = gridColor,
            start = Offset(lensCenter.x, lensCenter.y - radius),
            end = Offset(lensCenter.x, lensCenter.y + radius),
            strokeWidth = 1.1f
        )
        val zoom = if (boostActive) POINTER_LENS_ZOOM + 0.25f else POINTER_LENS_ZOOM
        fun toLens(point: Offset): Offset {
            return Offset(
                x = lensCenter.x + ((point.x - pointer.x) * zoom),
                y = lensCenter.y + ((point.y - pointer.y) * zoom)
            )
        }
        nearestWall?.let { wall ->
            val start = toLens(worldToScreen(wall.start))
            val end = toLens(worldToScreen(wall.end))
            drawLine(
                color = Color(0xC804111E),
                start = start,
                end = end,
                strokeWidth = if (boostActive) 8.6f else 7.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = if (boostActive) Color(0xFFFFEA9C) else Color(0xFFFFE3AF),
                start = start,
                end = end,
                strokeWidth = if (boostActive) 4.6f else 4f,
                cap = StrokeCap.Round
            )
        }
        nearestPoint?.let { projected ->
            val point = toLens(worldToScreen(projected))
            drawCircle(
                color = Color(0xAB0A1A2C),
                radius = 11f,
                center = point
            )
            drawCircle(
                color = if (boostActive) Color(0xFFFFE27A) else Color(0xFFFFF1CB),
                radius = 5.3f,
                center = point
            )
        }
        drawCircle(
            color = Color(0x4CC8E9FF),
            radius = 7f,
            center = lensCenter,
            style = Stroke(width = 1.8f)
        )
    }
    drawCircle(
        color = if (boostActive) Color(0xFF8FD8FF) else Color(0xB38AB4D8),
        radius = radius,
        center = lensCenter,
        style = Stroke(width = if (boostActive) 2.8f else 2.2f)
    )
}

private fun DrawScope.drawRightAngleHint(
    hint: RightAngleHint,
    worldToScreen: (PointMm) -> Offset,
    scale: Float
) {
    val mmPerPx = 1.0 / (BASE_PX_PER_MM * scale).coerceAtLeast(0.0001f)
    val markerSizeMm = (RIGHT_ANGLE_MARKER_SIZE_PX * mmPerPx).roundToLong().coerceAtLeast(40L)
    val legA = unitStepFrom(hint.corner, hint.legA, markerSizeMm) ?: return
    val legB = unitStepFrom(hint.corner, hint.legB, markerSizeMm) ?: return
    val boxCorner = PointMm(
        x = legA.x + (legB.x - hint.corner.x),
        y = legA.y + (legB.y - hint.corner.y)
    )
    val cornerScreen = worldToScreen(hint.corner)
    val legAScreen = worldToScreen(legA)
    val legBScreen = worldToScreen(legB)
    val boxScreen = worldToScreen(boxCorner)
    val color = if (hint.highlighted) Color(0xFFFFCD86) else Color(0xFF8FD0FF)
    val stroke = if (hint.highlighted) 2.2f else 1.7f
    drawLine(
        color = color,
        start = legAScreen,
        end = boxScreen,
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = legBScreen,
        end = boxScreen,
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    val labelPoint = Offset(
        x = (cornerScreen.x + legAScreen.x + legBScreen.x + boxScreen.x) / 4f,
        y = (cornerScreen.y + legAScreen.y + legBScreen.y + boxScreen.y) / 4f
    )
    val textSizePx = RIGHT_ANGLE_LABEL_TEXT_SP * density
    drawCircle(
        color = Color(0x64081527),
        radius = textSizePx * 0.86f,
        center = labelPoint
    )
    drawContext.canvas.nativeCanvas.apply {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0x8C0A1322).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText("90°", labelPoint.x + 0.6f, labelPoint.y + (textSizePx * 0.32f) + 0.6f, shadowPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.copy(alpha = 0.9f).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText("90°", labelPoint.x, labelPoint.y + (textSizePx * 0.32f), textPaint)
    }
}

private fun DrawScope.drawCornerAngleLabel(
    hint: CornerAngleHint,
    worldToScreen: (PointMm) -> Offset,
    scale: Float
) {
    val labelWorld = labelPointInsideAngle(
        corner = hint.corner,
        legA = hint.legA,
        legB = hint.legB,
        scale = scale
    ) ?: return
    val labelScreen = worldToScreen(labelWorld)
    val labelText = formatAngleLabel(hint.angleDegrees)
    val textColor = if (hint.highlighted) Color(0xFFFFD08A) else Color(0xFFAFC6DF)
    val textSizePx = RIGHT_ANGLE_LABEL_TEXT_SP * density
    drawCircle(
        color = Color(0x43081527),
        radius = textSizePx * 0.86f,
        center = labelScreen
    )
    drawContext.canvas.nativeCanvas.apply {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0x8C0A1322).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText(labelText, labelScreen.x + 0.6f, labelScreen.y + (textSizePx * 0.34f) + 0.6f, shadowPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = textColor.copy(alpha = 0.82f).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText(labelText, labelScreen.x, labelScreen.y + (textSizePx * 0.34f), textPaint)
    }
}

private fun wallHasParallelLengthMatch(target: WallSegment, walls: List<WallSegment>): Boolean {
    val targetLengthMm = target.lengthMillimeters()
    if (targetLengthMm <= 0L) return false
    val targetAngle = target.angleDegrees()
    return walls.any { other ->
        other.id != target.id &&
            parallelAngleDeltaDegrees(targetAngle, other.angleDegrees()) <= PARALLEL_MATCH_ANGLE_TOLERANCE_DEG &&
            abs(other.lengthMillimeters() - targetLengthMm) <= PARALLEL_MATCH_LENGTH_TOLERANCE_MM
    }
}

private fun collectCornerAngleHints(
    walls: List<WallSegment>,
    highlightedWallId: String?
): List<CornerAngleHint> {
    if (walls.size < 2) return emptyList()
    val hints = mutableListOf<CornerAngleHint>()
    val seen = mutableSetOf<String>()
    for (index in 0 until walls.lastIndex) {
        val wallA = walls[index]
        for (otherIndex in index + 1 until walls.size) {
            val wallB = walls[otherIndex]
            val shared = sharedCorner(wallA, wallB) ?: continue
            val angle = cornerAngleDegrees(
                corner = shared.corner,
                legA = shared.legA,
                legB = shared.legB
            )
            if (angle <= 5.0 || angle >= 175.0) continue
            val cornerBucket = "${shared.corner.x / 5L}:${shared.corner.y / 5L}"
            val pairKey = if (wallA.id < wallB.id) {
                "${wallA.id}|${wallB.id}|$cornerBucket"
            } else {
                "${wallB.id}|${wallA.id}|$cornerBucket"
            }
            if (!seen.add(pairKey)) continue
            hints += CornerAngleHint(
                corner = shared.corner,
                legA = shared.legA,
                legB = shared.legB,
                angleDegrees = angle,
                highlighted = wallA.id == highlightedWallId || wallB.id == highlightedWallId
            )
        }
    }
    return hints
}

private data class SharedCorner(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm
)

private fun sharedCorner(a: WallSegment, b: WallSegment): SharedCorner? {
    fun mid(p: PointMm, q: PointMm): PointMm {
        return PointMm(
            x = ((p.x + q.x) / 2.0).roundToLong(),
            y = ((p.y + q.y) / 2.0).roundToLong()
        )
    }
    return when {
        pointsNear(a.start, b.start) -> SharedCorner(mid(a.start, b.start), a.end, b.end)
        pointsNear(a.start, b.end) -> SharedCorner(mid(a.start, b.end), a.end, b.start)
        pointsNear(a.end, b.start) -> SharedCorner(mid(a.end, b.start), a.start, b.end)
        pointsNear(a.end, b.end) -> SharedCorner(mid(a.end, b.end), a.start, b.start)
        else -> null
    }
}

private fun rightAngleDeltaDegrees(
    corner: PointMm,
    legA: PointMm,
    legB: PointMm
): Double {
    return abs(90.0 - cornerAngleDegrees(corner, legA, legB))
}

private fun cornerAngleDegrees(
    corner: PointMm,
    legA: PointMm,
    legB: PointMm
): Double {
    val ax = (legA.x - corner.x).toDouble()
    val ay = (legA.y - corner.y).toDouble()
    val bx = (legB.x - corner.x).toDouble()
    val by = (legB.y - corner.y).toDouble()
    val magA = hypot(ax, ay)
    val magB = hypot(bx, by)
    if (magA <= 0.0001 || magB <= 0.0001) return 180.0
    val dot = (ax * bx) / (magA * magB)
    val cross = ((ax * by) - (ay * bx)) / (magA * magB)
    return Math.toDegrees(abs(atan2(cross, dot)))
}

private fun labelPointInsideAngle(
    corner: PointMm,
    legA: PointMm,
    legB: PointMm,
    scale: Float
): PointMm? {
    val ax = (legA.x - corner.x).toDouble()
    val ay = (legA.y - corner.y).toDouble()
    val bx = (legB.x - corner.x).toDouble()
    val by = (legB.y - corner.y).toDouble()
    val magA = hypot(ax, ay)
    val magB = hypot(bx, by)
    if (magA <= 0.0001 || magB <= 0.0001) return null
    val uxA = ax / magA
    val uyA = ay / magA
    val uxB = bx / magB
    val uyB = by / magB
    val bisectorX = uxA + uxB
    val bisectorY = uyA + uyB
    val bisectorMag = hypot(bisectorX, bisectorY)
    if (bisectorMag <= 0.0001) return null
    val mmPerPx = 1.0 / (BASE_PX_PER_MM * scale).coerceAtLeast(0.0001f)
    val offsetMm = (CORNER_ANGLE_LABEL_OFFSET_PX * mmPerPx).roundToLong().coerceAtLeast(95L)
    return PointMm(
        x = corner.x + ((bisectorX / bisectorMag) * offsetMm).roundToLong(),
        y = corner.y + ((bisectorY / bisectorMag) * offsetMm).roundToLong()
    )
}

private fun formatAngleLabel(angle: Double): String {
    val roundedHalf = (round(angle * 2.0) / 2.0).coerceAtLeast(0.0)
    val nearestInt = roundedHalf.roundToLong().toDouble()
    return if (abs(roundedHalf - nearestInt) <= 0.05) {
        "${nearestInt.roundToLong()}°"
    } else {
        "${"%.1f".format(roundedHalf)}°"
    }
}

private fun parallelAngleDeltaDegrees(a: Double, b: Double): Double {
    val delta = abs(normalizedAngleDeltaDegrees(a, b))
    return minOf(delta, abs(180.0 - delta))
}

private fun normalizedAngleDeltaDegrees(from: Double, to: Double): Double {
    var delta = normalizeAngleDegrees(to) - normalizeAngleDegrees(from)
    while (delta > 180.0) delta -= 360.0
    while (delta < -180.0) delta += 360.0
    return delta
}

private fun normalizeAngleDegrees(value: Double): Double {
    val wrapped = value % 360.0
    return if (wrapped < 0.0) wrapped + 360.0 else wrapped
}

private fun unitStepFrom(
    origin: PointMm,
    toward: PointMm,
    lengthMm: Long
): PointMm? {
    val dx = (toward.x - origin.x).toDouble()
    val dy = (toward.y - origin.y).toDouble()
    val magnitude = hypot(dx, dy)
    if (magnitude <= 0.0001) return null
    val ux = dx / magnitude
    val uy = dy / magnitude
    return PointMm(
        x = origin.x + (ux * lengthMm).roundToLong(),
        y = origin.y + (uy * lengthMm).roundToLong()
    )
}

private fun DrawScope.drawWallLengthLabel(
    start: Offset,
    end: Offset,
    lengthFeet: Double,
    color: Color
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val linePx = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (linePx < 20f) return

    val midpoint = Offset(
        x = (start.x + end.x) / 2f,
        y = (start.y + end.y) / 2f
    )
    val nx = -dy / linePx
    val ny = dx / linePx
    val labelPoint = Offset(
        x = midpoint.x + (nx * WALL_LENGTH_LABEL_OFFSET_PX),
        y = midpoint.y + (ny * WALL_LENGTH_LABEL_OFFSET_PX)
    )

    val text = formatFeetInchesPrime(lengthFeet)
    val textSizePx = WALL_LENGTH_LABEL_TEXT_SP * density
    drawContext.canvas.nativeCanvas.apply {
        val baselineY = labelPoint.y + (textSizePx * 0.34f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.FILL
        }
        val textWidth = fillPaint.measureText(text)
        val padX = textSizePx * 0.44f
        val padYTop = textSizePx * 0.95f
        val padYBottom = textSizePx * 0.28f
        val chipRect = RectF(
            labelPoint.x - (textWidth / 2f) - padX,
            baselineY - padYTop,
            labelPoint.x + (textWidth / 2f) + padX,
            baselineY + padYBottom
        )
        val chipRadius = textSizePx * 0.5f
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0xDE081321).toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipFillPaint)
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0xF15B86AF).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.9f
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipStrokePaint)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0x78111F32).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.STROKE
            strokeWidth = textSizePx * 0.48f
            strokeJoin = Paint.Join.ROUND
        }
        drawText(text, labelPoint.x, baselineY, glowPaint)
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color(0xE013263A).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.STROKE
            strokeWidth = textSizePx * 0.28f
            strokeJoin = Paint.Join.ROUND
        }
        drawText(text, labelPoint.x, baselineY, outlinePaint)
        drawText(text, labelPoint.x, baselineY, fillPaint)
    }
}

private fun BlueprintOpening.doorSwingTag(): String? = when {
    tags.contains(DOOR_SWING_NEG_TAG) -> DOOR_SWING_NEG_TAG
    tags.contains(DOOR_SWING_POS_TAG) -> DOOR_SWING_POS_TAG
    else -> null
}

private fun pointSideOfWall(point: PointMm, wall: WallSegment): Double {
    val wallX = (wall.end.x - wall.start.x).toDouble()
    val wallY = (wall.end.y - wall.start.y).toDouble()
    val pointX = (point.x - wall.start.x).toDouble()
    val pointY = (point.y - wall.start.y).toDouble()
    return (wallX * pointY) - (wallY * pointX)
}

private fun screenPointToWorldPoint(
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

private fun worldPointToCanvasLocal(
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

private fun parsePrimeLengthToFeet(input: String): Double? {
    val normalized = input
        .trim()
        .replace("''", "\"")
        .replace("’", "'")
        .replace("“", "\"")
        .replace("”", "\"")
    return DimensionParser.parseLengthToFeet(normalized)
}

private fun formatLiveValue(value: Double, decimals: Int): String {
    val safe = if (value.isFinite()) value else 0.0
    return when (decimals.coerceIn(0, 3)) {
        0 -> "%.0f".format(safe)
        1 -> "%.1f".format(safe)
        2 -> "%.2f".format(safe)
        else -> "%.3f".format(safe)
    }
}

private fun formatFeetInchesPrime(feet: Double): String {
    val totalInches = (feet.coerceAtLeast(0.0) * 12.0).roundToLong()
    val wholeFeet = totalInches / 12L
    val inches = totalInches % 12L
    return if (inches == 0L) {
        "${wholeFeet}'"
    } else {
        "${wholeFeet}' ${inches}\""
    }
}

private fun formatSignedFeetInchesPrime(mm: Long): String {
    val sign = when {
        mm > 0L -> "+"
        mm < 0L -> "-"
        else -> ""
    }
    return sign + formatFeetInchesPrime(Millimeters(abs(mm)).toFeet())
}

private fun applyCursorGridAssist(
    cursorLocal: Offset,
    canvasSize: Size,
    scale: Float,
    pan: Offset,
    snapSettings: BlueprintSnapSettings,
    assistStrength: Float,
    assistRadiusPx: Float
): Offset {
    if (!snapSettings.gridEnabled) return cursorLocal
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return cursorLocal
    val ppm = BASE_PX_PER_MM * scale
    if (ppm <= 0.0001f) return cursorLocal
    val stepMm = Millimeters.fromFeet(snapSettings.gridStepFeet.coerceAtLeast(0.1))
        .value
        .coerceAtLeast(50L)
        .toDouble()
    val worldX = (cursorLocal.x - canvasSize.width / 2f - pan.x) / ppm
    val worldY = (-(cursorLocal.y - canvasSize.height / 2f - pan.y)) / ppm
    val gridX = (round(worldX / stepMm) * stepMm).roundToLong()
    val gridY = (round(worldY / stepMm) * stepMm).roundToLong()
    val snappedLocal = worldPointToCanvasLocal(
        worldPoint = PointMm(gridX, gridY),
        canvasSize = canvasSize,
        scale = scale,
        pan = pan
    ) ?: return cursorLocal
    val dx = snappedLocal.x - cursorLocal.x
    val dy = snappedLocal.y - cursorLocal.y
    val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (distance <= 0.0001f || distance > assistRadiusPx) return cursorLocal
    val pull = (((assistRadiusPx - distance) / assistRadiusPx).coerceIn(0f, 1f) * assistStrength)
        .coerceIn(0f, 0.65f)
    return Offset(
        x = (cursorLocal.x + (dx * pull)).coerceIn(0f, canvasSize.width),
        y = (cursorLocal.y + (dy * pull)).coerceIn(0f, canvasSize.height)
    )
}

private fun applyJoystickDeadzone(
    input: Offset,
    deadzone: Float = JOYSTICK_DEADZONE_DEFAULT,
    responseExponent: Float = 1f
): Offset {
    val magnitude = hypot(input.x.toDouble(), input.y.toDouble()).toFloat()
    val clampedDeadzone = deadzone.coerceIn(0f, 0.95f)
    if (magnitude <= clampedDeadzone || magnitude <= 0.0001f) return Offset.Zero
    val normalizedX = input.x / magnitude
    val normalizedY = input.y / magnitude
    val adjustedMagnitude = ((magnitude - clampedDeadzone) / (1f - clampedDeadzone))
        .coerceIn(0f, 1f)
        .pow(responseExponent.coerceAtLeast(0.01f))
    return Offset(
        x = normalizedX * adjustedMagnitude,
        y = normalizedY * adjustedMagnitude
    )
}

private fun resolveDraftStartFromTap(
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

private fun snapToNearestWallEndpoint(
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

private fun closestGravelMaterialPreset(densityTonsPerYard: Double): GravelMaterialPreset {
    return gravelMaterialPresets.minByOrNull { preset ->
        abs(preset.densityTonsPerYard - densityTonsPerYard)
    } ?: gravelMaterialPresets.first()
}

private fun canAddDraftedWall(
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

private fun wallMatchesEndpoints(
    wall: WallSegment,
    start: PointMm,
    end: PointMm
): Boolean {
    val direct = pointsNear(wall.start, start) && pointsNear(wall.end, end)
    val reversed = pointsNear(wall.start, end) && pointsNear(wall.end, start)
    return direct || reversed
}

private fun pointsNear(a: PointMm, b: PointMm): Boolean {
    return abs(a.x - b.x) <= WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM &&
        abs(a.y - b.y) <= WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM
}

private fun performSyntheticTap(rootView: android.view.View, pointInRoot: Offset): Boolean {
    if (rootView.width <= 2 || rootView.height <= 2) return false
    val clampedX = pointInRoot.x.coerceIn(1f, rootView.width.toFloat() - 1f)
    val clampedY = pointInRoot.y.coerceIn(1f, rootView.height.toFloat() - 1f)
    val downTime = SystemClock.uptimeMillis()
    val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, clampedX, clampedY, 0)
    val up = MotionEvent.obtain(downTime, downTime + 16L, MotionEvent.ACTION_UP, clampedX, clampedY, 0)
    val downHandled = rootView.dispatchTouchEvent(down)
    val upHandled = rootView.dispatchTouchEvent(up)
    down.recycle()
    up.recycle()
    return downHandled || upHandled
}

private fun snapMovedWallToNearbyWalls(
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

private fun WallSegment.rotateByDegreesIncrement(stepDegrees: Double): WallSegment {
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

private fun WallSegment.translateBy(dxMm: Long, dyMm: Long): WallSegment {
    return copy(
        start = PointMm(x = start.x + dxMm, y = start.y + dyMm),
        end = PointMm(x = end.x + dxMm, y = end.y + dyMm)
    )
}

private fun WallSegment.scopeFromTag(): TakeoffScope? = when {
    tags.contains(TakeoffScope.DRYWALL.wallScopeTag()) -> TakeoffScope.DRYWALL
    tags.contains(TakeoffScope.CONCRETE.wallScopeTag()) -> TakeoffScope.CONCRETE
    tags.contains(TakeoffScope.GRAVEL_MULCH.wallScopeTag()) -> TakeoffScope.GRAVEL_MULCH
    tags.contains(TakeoffScope.PAINT.wallScopeTag()) -> TakeoffScope.PAINT
    else -> null
}

private fun BlueprintFloorLevel.floorTag(): String = "$FLOOR_TAG_PREFIX$this"

private fun BlueprintFloorLevel.label(): String = when {
    this == FLOOR_GROUND_LEVEL -> "Ground"
    this > FLOOR_GROUND_LEVEL -> (this + 1).toString()
    this == -1 -> "Basement"
    else -> "Basement ${abs(this)}"
}

private fun BlueprintFloorLevel.floorDisplayLabel(): String = "Floor: ${label()}"

private fun parseFloorLevelTag(tag: String?): BlueprintFloorLevel? {
    val normalized = tag?.trim() ?: return null
    if (!normalized.startsWith(FLOOR_TAG_PREFIX)) return null
    if (normalized.equals(FLOOR_LEGACY_LOWER_TAG, ignoreCase = true)) return FLOOR_GROUND_LEVEL
    if (normalized.equals(FLOOR_LEGACY_UPPER_TAG, ignoreCase = true)) return FLOOR_GROUND_LEVEL + 1
    return normalized.removePrefix(FLOOR_TAG_PREFIX).toIntOrNull()
}

private fun Set<String>.resolveFloorLevelOrDefault(
    defaultLevel: BlueprintFloorLevel = FLOOR_GROUND_LEVEL
): BlueprintFloorLevel {
    val rawFloorTag = firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) }
    return parseFloorLevelTag(rawFloorTag) ?: defaultLevel
}

private fun WallSegment.isOnFloor(level: BlueprintFloorLevel): Boolean {
    val wallFloor = tags.resolveFloorLevelOrDefault()
    return wallFloor == level
}

private fun Room.isOnFloor(level: BlueprintFloorLevel): Boolean {
    val roomFloor = tags.resolveFloorLevelOrDefault()
    return roomFloor == level
}

private fun BlueprintOpening.isOnFloor(
    level: BlueprintFloorLevel,
    wallsById: Map<String, WallSegment>
): Boolean {
    val inheritedFloor = wallsById[wallId]?.tags?.resolveFloorLevelOrDefault()
    val openingFloor = tags.resolveFloorLevelOrDefault(inheritedFloor ?: FLOOR_GROUND_LEVEL)
    return openingFloor == level
}

private fun OpeningType.isStair(): Boolean {
    return this == OpeningType.STAIR_UP || this == OpeningType.STAIR_DOWN
}

private fun OpeningType.displayLabel(): String = when (this) {
    OpeningType.DOOR -> "Door"
    OpeningType.WINDOW -> "Window"
    OpeningType.STAIR_UP -> "Stair Up"
    OpeningType.STAIR_DOWN -> "Stair Down"
}

private fun TakeoffScope.shortLabel(): String = when (this) {
    TakeoffScope.DRYWALL -> "Drywall"
    TakeoffScope.CONCRETE -> "Concrete"
    TakeoffScope.GRAVEL_MULCH -> "Gravel"
    TakeoffScope.PAINT -> "Paint"
}

private fun TakeoffScope.railLabel(): String = when (this) {
    TakeoffScope.DRYWALL -> "Dry"
    TakeoffScope.CONCRETE -> "Conc"
    TakeoffScope.GRAVEL_MULCH -> "Gravel"
    TakeoffScope.PAINT -> "Paint"
}

private fun TakeoffScope.wallColor(): Color = when (this) {
    TakeoffScope.DRYWALL -> Color(0xFF7ABEFF)
    TakeoffScope.CONCRETE -> Color(0xFFFF8E78)
    TakeoffScope.GRAVEL_MULCH -> Color(0xFFFFD173)
    TakeoffScope.PAINT -> Color(0xFF6BE59C)
}

private fun TakeoffScope.wallScopeTag(): String = when (this) {
    TakeoffScope.DRYWALL -> "${WALL_SCOPE_TAG_PREFIX}drywall"
    TakeoffScope.CONCRETE -> "${WALL_SCOPE_TAG_PREFIX}concrete"
    TakeoffScope.GRAVEL_MULCH -> "${WALL_SCOPE_TAG_PREFIX}gravel_mulch"
    TakeoffScope.PAINT -> "${WALL_SCOPE_TAG_PREFIX}paint"
}

private fun TakeoffScope.icon(): ImageVector = when (this) {
    TakeoffScope.DRYWALL -> Icons.Filled.Architecture
    TakeoffScope.CONCRETE -> Icons.Filled.Straighten
    TakeoffScope.GRAVEL_MULCH -> Icons.Filled.Workspaces
    TakeoffScope.PAINT -> Icons.Filled.AutoFixHigh
}

private fun TakeoffScope.next(): TakeoffScope = when (this) {
    TakeoffScope.DRYWALL -> TakeoffScope.CONCRETE
    TakeoffScope.CONCRETE -> TakeoffScope.GRAVEL_MULCH
    TakeoffScope.GRAVEL_MULCH -> TakeoffScope.PAINT
    TakeoffScope.PAINT -> TakeoffScope.DRYWALL
}
