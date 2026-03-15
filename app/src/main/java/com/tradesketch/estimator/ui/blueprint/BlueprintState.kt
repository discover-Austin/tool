package com.tradesketch.estimator.ui.blueprint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool

internal data class OpeningPreset(
    val name: String,
    val type: OpeningType,
    val widthMm: Long,
    val heightMm: Long,
    val sillMm: Long
)

internal enum class OpeningPanelType {
    DOORS,
    WINDOWS,
    STAIR_UP,
    STAIR_DOWN
}

internal typealias BlueprintFloorLevel = Int

internal data class OpeningPlacementCandidate(
    val wall: WallSegment,
    val t: Double,
    val snappedCenter: PointMm,
    val swingTag: String?
)

internal data class OpeningDragPreview(
    val preset: OpeningPreset,
    val rawWorldPoint: PointMm,
    val placement: OpeningPlacementCandidate?
)

internal data class WallLengthLabelSpec(
    val start: Offset,
    val end: Offset,
    val lengthFeet: Double,
    val color: Color
)

internal data class RightAngleHint(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm,
    val highlighted: Boolean
)

internal data class CornerAngleHint(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm,
    val angleDegrees: Double,
    val highlighted: Boolean
)

internal data class LiveScopeQuantity(
    val tradeLabel: String,
    val value: String
)

internal fun openingPreset(
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

internal val doorPresets = listOf(
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

internal val windowPresets = listOf(
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

internal val stairUpPresets = listOf(
    openingPreset("3'0\" x 9'0\" straight", OpeningType.STAIR_UP, 3.0, 9.0, 0.0),
    openingPreset("3'6\" x 10'0\" straight", OpeningType.STAIR_UP, 3.5, 10.0, 0.0),
    openingPreset("4'0\" x 11'0\" L-stair", OpeningType.STAIR_UP, 4.0, 11.0, 0.0)
)

internal val stairDownPresets = listOf(
    openingPreset("3'0\" x 9'0\" straight", OpeningType.STAIR_DOWN, 3.0, 9.0, 0.0),
    openingPreset("3'6\" x 10'0\" straight", OpeningType.STAIR_DOWN, 3.5, 10.0, 0.0),
    openingPreset("4'0\" x 11'0\" L-stair", OpeningType.STAIR_DOWN, 4.0, 11.0, 0.0)
)

internal val doorPreset = doorPresets.first { it.name.startsWith("3'0\" x 7'0\"") }
internal val windowPreset = windowPresets.first { it.name.startsWith("4'0\" x 4'0\"") }
internal val stairUpPreset = stairUpPresets.first()
internal val stairDownPreset = stairDownPresets.first()

internal const val BASE_PX_PER_MM = 0.065f
internal const val MIN_BLUEPRINT_SCALE = 0.2f
internal const val MAX_BLUEPRINT_SCALE = 7.5f
internal const val MIN_GRID_STEP_FEET = 0.0328084 // 1 centimeter
internal const val MAX_GRID_STEP_FEET = 20.0
internal const val MIN_GRID_STEP_MM = 10L
internal const val MIN_DRAW_WALL_LENGTH_MM = 1L
internal const val MIN_DRAW_WALL_SCREEN_PX = 10f
internal const val DRAW_EDGE_DIAL_ANGLE_STEP_DEGREES = 1.0
internal val DRAW_EDGE_DIAL_LENGTH_STEP_MM = Millimeters.fromInches(1.0).value
internal const val DRAW_EDGE_DIAL_MAX_LENGTH_MM = 1_000_000L
internal const val WALL_DUPLICATE_ENDPOINT_TOLERANCE_MM = 30L
internal val CANVAS_TAP_AIM_OFFSET_PX = Offset(0f, -68f)
internal const val WALL_POINTER_RELEASE_HOLD_MS = 360L
internal const val WALL_SCOPE_TAG_PREFIX = "trade_scope:"
internal const val FLOOR_TAG_PREFIX = "floor:"
internal const val FLOOR_GROUND_LEVEL = 0
internal const val FLOOR_LEGACY_LOWER_TAG = "${FLOOR_TAG_PREFIX}lower"
internal const val FLOOR_LEGACY_UPPER_TAG = "${FLOOR_TAG_PREFIX}upper"
internal const val DOOR_SWING_POS_TAG = "door_swing:pos"
internal const val DOOR_SWING_NEG_TAG = "door_swing:neg"
internal const val WALL_LENGTH_LABEL_TEXT_SP = 11.2f
internal const val WALL_LENGTH_LABEL_OFFSET_PX = 13f
internal const val PARALLEL_MATCH_ANGLE_TOLERANCE_DEG = 2.0
internal const val PARALLEL_MATCH_LENGTH_TOLERANCE_MM = 35L
internal const val RIGHT_ANGLE_MARKER_SIZE_PX = 11f
internal const val RIGHT_ANGLE_MARKER_TOLERANCE_DEG = 1.0
internal const val RIGHT_ANGLE_LABEL_TEXT_SP = 6.8f
internal const val CORNER_ANGLE_LABEL_OFFSET_PX = 20f
internal const val MIN_SNAP_THRESHOLD_FEET = 0.2
internal const val MAX_SNAP_THRESHOLD_FEET = 2.0
internal const val JOYSTICK_FRAME_RATE_BASE = 60f
internal const val JOYSTICK_CURSOR_SPEED_PX_PER_SEC = 5.2f * JOYSTICK_FRAME_RATE_BASE
internal const val JOYSTICK_PAN_SPEED_PX_PER_SEC = 7f * JOYSTICK_FRAME_RATE_BASE
internal const val JOYSTICK_PAN_BOOST_PX_PER_SEC = 10f * JOYSTICK_FRAME_RATE_BASE
internal const val JOYSTICK_MIN_FRAME_DELTA_SEC = 1f / 120f
internal const val JOYSTICK_MAX_FRAME_DELTA_SEC = 1f / 30f
internal const val JOYSTICK_CURSOR_RESPONSE_EXPONENT = 1.35f
internal const val JOYSTICK_PAN_RESPONSE_EXPONENT = 1f
internal const val JOYSTICK_DEADZONE_DEFAULT = 0.08f
internal const val JOYSTICK_SELECT_BOOST_MULTIPLIER = 1.65f
internal const val MOVING_WALL_ROTATE_INCREMENT_DEG = 5.0
internal const val MOVING_WALL_AUTO_SNAP_THRESHOLD_MULTIPLIER = 1.4
internal const val MOVING_WALL_AUTO_SNAP_MIN_THRESHOLD_MM = 90L
internal const val POINTER_LENS_ZOOM = 2.1f
internal const val POINTER_LENS_RADIUS_PX = 52f
internal val POINTER_LENS_OFFSET_PX = Offset(102f, -94f)
internal val DEFAULT_PANEL_BOTTOM_PADDING = 130.dp
internal val BLUEPRINT_BACKGROUND_TOP = Color(0xFF071A30)
internal val BLUEPRINT_BACKGROUND_BOTTOM = Color(0xFF0E2A49)
internal val BLUEPRINT_CANVAS_TOP = Color(0xFF051423)
internal val BLUEPRINT_CANVAS_BOTTOM = Color(0xFF0C2845)
internal val BLUEPRINT_CANVAS_GLOW = Color(0x1D67B7F2)
internal val BLUEPRINT_TEXTURE_DIAGONAL_A = Color(0x1488B8E8)
internal val BLUEPRINT_TEXTURE_DIAGONAL_B = Color(0x0C709BC8)
internal val BLUEPRINT_TEXTURE_NOISE_DOT = Color(0x0F5C86B5)
internal val GRID_MINOR_COLOR = Color(0x326D90AE)
internal val GRID_MAJOR_COLOR = Color(0x7BA4C8E8)
internal val GRID_FIVE_FOOT_COLOR = Color(0xD0D7EEFF)
internal val GRID_AXIS_COLOR = Color(0xF0D5E9FF)
internal val GEOMETRY_SELECTION_COLOR = Color(0xFFFFF2BF)
internal val GEOMETRY_HALO_COLOR = Color(0xDE020A14)
internal val GEOMETRY_DEPTH_SHADOW = Color(0xCC00040A)
internal val GEOMETRY_DEPTH_HIGHLIGHT = Color(0xA6E8F6FF)
internal val GEOMETRY_CORE_HIGHLIGHT = Color(0xFFF4FBFF)
internal val GEOMETRY_SELECTION_PULSE = Color(0x99FFDFA0)
internal val GEOMETRY_SNAP_PULSE = Color(0xB89ED8FF)
internal val GEOMETRY_INTERSECTION_PULSE = Color(0xD8FFE06B)
internal val OPENING_DOOR_COLOR = Color(0xFFFFCF8C)
internal val OPENING_WINDOW_COLOR = Color(0xFFB0EEFF)
internal val OPENING_PREVIEW_DOOR_COLOR = Color(0xFFFFDEAE)
internal val OPENING_PREVIEW_WINDOW_COLOR = Color(0xFFE0F9FF)
internal val OPENING_STAIR_UP_COLOR = Color(0xFFB8F2C8)
internal val OPENING_STAIR_DOWN_COLOR = Color(0xFFFFD6AE)
internal val OPENING_PREVIEW_STAIR_UP_COLOR = Color(0xFFDAFFE4)
internal val OPENING_PREVIEW_STAIR_DOWN_COLOR = Color(0xFFFFE7CF)
internal val OPENING_INVALID_COLOR = Color(0xFFFF7171)
internal val DRAFT_WALL_COLOR = Color(0xFFFFCA86)
internal val WALL_LABEL_NEUTRAL_COLOR = Color(0xFFF0F8FF)
internal val WALL_LABEL_ACTIVE_COLOR = Color(0xFFFFEDCA)

internal data class BlueprintToolToggleSpec(
    val tool: BlueprintDraftTool,
    val icon: ImageVector,
    val contentDescription: String
)

internal data class BlueprintIconActionSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val enabled: Boolean,
    val onClick: () -> Unit
)

internal data class BlueprintIconToggleSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

internal data class GravelMaterialPreset(
    val label: String,
    val densityTonsPerYard: Double
)

internal val gravelMaterialPresets = listOf(
    GravelMaterialPreset(label = "Pea gravel", densityTonsPerYard = 1.30),
    GravelMaterialPreset(label = "Crushed stone", densityTonsPerYard = 1.45),
    GravelMaterialPreset(label = "River rock", densityTonsPerYard = 1.35),
    GravelMaterialPreset(label = "Mulch", densityTonsPerYard = 0.25)
)

internal val blueprintToolToggleSpecs = listOf(
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

internal fun OpeningType.toDraftTool(): BlueprintDraftTool = when (this) {
    OpeningType.DOOR -> BlueprintDraftTool.PLACE_DOOR
    OpeningType.WINDOW -> BlueprintDraftTool.PLACE_WINDOW
    OpeningType.STAIR_UP -> BlueprintDraftTool.PLACE_STAIR_UP
    OpeningType.STAIR_DOWN -> BlueprintDraftTool.PLACE_STAIR_DOWN
}

internal fun BlueprintDraftTool.defaultPreset(): OpeningPreset? = when (this) {
    BlueprintDraftTool.PLACE_DOOR -> doorPreset
    BlueprintDraftTool.PLACE_WINDOW -> windowPreset
    BlueprintDraftTool.PLACE_STAIR_UP -> stairUpPreset
    BlueprintDraftTool.PLACE_STAIR_DOWN -> stairDownPreset
    else -> null
}
