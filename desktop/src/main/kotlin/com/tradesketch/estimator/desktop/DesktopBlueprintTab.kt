package com.tradesketch.estimator.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintParams
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.utils.DimensionParser
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BASE_PX_PER_MM = 0.065f
private const val TOOL_SELECT = "select"
private const val TOOL_DRAW = "draw"
private const val TOOL_BOX = "box"
private const val TOOL_DOOR = "door"
private const val TOOL_WINDOW = "window"
private const val TOOL_STAIR_UP = "stair_up"
private const val TOOL_STAIR_DOWN = "stair_down"
private const val TOOL_PAN = "pan"
private const val FLOOR_TAG_PREFIX = "floor:"
private const val FLOOR_GROUND_LEVEL = 0

private data class DesktopToolChipSpec(
    val key: String,
    val label: String,
    val openingType: OpeningType? = null
)

private data class DesktopFilterChipSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

private data class DesktopActionButtonSpec(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

private val desktopToolChipSpecs = listOf(
    DesktopToolChipSpec(key = TOOL_SELECT, label = "Select"),
    DesktopToolChipSpec(key = TOOL_DRAW, label = "Draw"),
    DesktopToolChipSpec(key = TOOL_BOX, label = "Box"),
    DesktopToolChipSpec(key = TOOL_DOOR, label = "Door", openingType = OpeningType.DOOR),
    DesktopToolChipSpec(key = TOOL_WINDOW, label = "Window", openingType = OpeningType.WINDOW),
    DesktopToolChipSpec(key = TOOL_STAIR_UP, label = "Stair Up", openingType = OpeningType.STAIR_UP),
    DesktopToolChipSpec(key = TOOL_STAIR_DOWN, label = "Stair Down", openingType = OpeningType.STAIR_DOWN),
    DesktopToolChipSpec(key = TOOL_PAN, label = "Pan")
)

private fun OpeningType.toolKey(): String = when (this) {
    OpeningType.DOOR -> TOOL_DOOR
    OpeningType.WINDOW -> TOOL_WINDOW
    OpeningType.STAIR_UP -> TOOL_STAIR_UP
    OpeningType.STAIR_DOWN -> TOOL_STAIR_DOWN
}

private fun OpeningType.label(): String = when (this) {
    OpeningType.DOOR -> "Door"
    OpeningType.WINDOW -> "Window"
    OpeningType.STAIR_UP -> "Stair Up"
    OpeningType.STAIR_DOWN -> "Stair Down"
}

private fun floorTag(level: Int): String = "$FLOOR_TAG_PREFIX$level"

private fun parseFloorLevelTag(tag: String?): Int? {
    val normalized = tag?.trim() ?: return null
    if (!normalized.startsWith(FLOOR_TAG_PREFIX)) return null
    val raw = normalized.removePrefix(FLOOR_TAG_PREFIX)
    if (raw.equals("lower", ignoreCase = true)) return FLOOR_GROUND_LEVEL
    if (raw.equals("upper", ignoreCase = true)) return FLOOR_GROUND_LEVEL + 1
    return raw.toIntOrNull()
}

private fun Set<String>.resolveFloorLevelOrDefault(defaultLevel: Int = FLOOR_GROUND_LEVEL): Int {
    val rawFloorTag = firstOrNull { tag -> tag.startsWith(FLOOR_TAG_PREFIX) }
    return parseFloorLevelTag(rawFloorTag) ?: defaultLevel
}

private fun WallSegment.isOnFloor(level: Int): Boolean {
    return tags.resolveFloorLevelOrDefault() == level
}

private fun com.tradesketch.estimator.domain.model.Room.isOnFloor(level: Int): Boolean {
    return tags.resolveFloorLevelOrDefault() == level
}

private fun BlueprintOpening.isOnFloor(level: Int, wallsById: Map<String, WallSegment>): Boolean {
    val inheritedFloor = wallsById[wallId]?.tags?.resolveFloorLevelOrDefault()
    val openingFloor = tags.resolveFloorLevelOrDefault(inheritedFloor ?: FLOOR_GROUND_LEVEL)
    return openingFloor == level
}

private fun Int.floorLabel(): String = when {
    this == FLOOR_GROUND_LEVEL -> "Ground"
    this > FLOOR_GROUND_LEVEL -> (this + 1).toString()
    this == -1 -> "Basement"
    else -> "Basement ${abs(this)}"
}

@Composable
fun DesktopBlueprintTab(
    state: DesktopAppState,
    openAddonsByDefault: Boolean,
    modifier: Modifier = Modifier
) {
    val project = state.selectedProject
    if (project == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No project selected")
                    Button(onClick = { state.createProjectFromTemplate(ProjectTemplate.BEDROOM, "My First Project") }) {
                        Text("Create Starter Project")
                    }
                }
            }
        }
        return
    }

    val document = project.authoritativeBlueprint()
    var tool by remember(project.id) { mutableStateOf(TOOL_DRAW) }
    var drawingStart by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var drawingPreview by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var boxStart by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var boxPreview by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var chainOrigin by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var chainWalls by remember(project.id) { mutableStateOf(true) }
    var detachedWalls by remember(project.id) { mutableStateOf(false) }
    var selectedWallId by remember(project.id) { mutableStateOf<String?>(null) }
    var selectedOpeningId by remember(project.id) { mutableStateOf<String?>(null) }
    var selectedFloor by remember(project.id) { mutableStateOf(FLOOR_GROUND_LEVEL) }
    var scale by remember(project.id) { mutableStateOf(1f) }
    var pan by remember(project.id) { mutableStateOf(Offset.Zero) }
    var snap by remember(project.id) { mutableStateOf(BlueprintSnapSettings()) }
    var lengthInput by remember(project.id) { mutableStateOf("") }
    var angleInput by remember(project.id) { mutableStateOf("") }
    var showAddons by remember(project.id) { mutableStateOf(openAddonsByDefault) }
    var showParams by remember(project.id) { mutableStateOf(false) }
    var showGuide by remember(project.id) { mutableStateOf(false) }
    var openingType by remember(project.id) { mutableStateOf(OpeningType.DOOR) }
    var customWidthFeet by remember(project.id) { mutableStateOf("3.0") }
    var customHeightFeet by remember(project.id) { mutableStateOf("7.0") }
    var customSillFeet by remember(project.id) { mutableStateOf("0.0") }
    var draggingOpeningType by remember(project.id) { mutableStateOf<OpeningType?>(null) }
    var draggingScreenPoint by remember(project.id) { mutableStateOf<Offset?>(null) }
    var workspaceRoot by remember(project.id) { mutableStateOf(Offset.Zero) }
    var canvasRoot by remember(project.id) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember(project.id) { mutableStateOf(Size.Zero) }
    var wallHeightFeet by remember(project.id, document.params.wallHeightMm) {
        mutableStateOf("%.2f".format(Millimeters(document.params.wallHeightMm).toFeet()))
    }
    var paintCoats by remember(project.id, document.params.paintCoats) {
        mutableStateOf(document.params.paintCoats.toString())
    }
    var wastePercent by remember(project.id, document.params.wasteFactorPercent) {
        mutableStateOf(document.params.wasteFactorPercent.toString())
    }

    LaunchedEffect(project.id, openAddonsByDefault) {
        showAddons = openAddonsByDefault
        showParams = false
        selectedFloor = FLOOR_GROUND_LEVEL
    }
    val allWalls = document.walls
    val wallsById = allWalls.associateBy { it.id }
    val floorWalls = allWalls.filter { wall -> wall.isOnFloor(selectedFloor) }
    val floorRooms = document.rooms.filter { room -> room.isOnFloor(selectedFloor) }
    val floorOpenings = document.openings.filter { opening ->
        opening.isOnFloor(selectedFloor, wallsById)
    }
    val floorDocument = document.copy(
        walls = floorWalls,
        rooms = floorRooms,
        openings = floorOpenings
    )

    LaunchedEffect(floorWalls, floorOpenings) {
        if (selectedWallId != null && floorWalls.none { it.id == selectedWallId }) {
            selectedWallId = null
        }
        if (selectedOpeningId != null && floorOpenings.none { it.id == selectedOpeningId }) {
            selectedOpeningId = null
        }
    }

    val wallLength = floorWalls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    val netArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(floorDocument).values.sum() -
        BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(floorDocument).values.sum()
    val selectedWall = selectedWallId?.let { id -> floorWalls.firstOrNull { it.id == id } }
    val selectedOpening = selectedOpeningId?.let { id -> floorOpenings.firstOrNull { it.id == id } }
    val openingDimensions: (OpeningType) -> Triple<Long, Long, Long> = { type ->
        val defaults = when (type) {
            OpeningType.DOOR -> Triple(
                Millimeters.fromFeet(3.0).value,
                Millimeters.fromFeet(7.0).value,
                0L
            )
            OpeningType.WINDOW -> Triple(
                Millimeters.fromFeet(4.0).value,
                Millimeters.fromFeet(4.0).value,
                Millimeters.fromFeet(3.0).value
            )
            OpeningType.STAIR_UP,
            OpeningType.STAIR_DOWN -> Triple(
                Millimeters.fromFeet(3.0).value,
                Millimeters.fromFeet(10.0).value,
                Millimeters.fromFeet(9.0).value
            )
        }
        Triple(
            DimensionParser.parseLengthToMillimeters(customWidthFeet)?.coerceAtLeast(1L) ?: defaults.first,
            DimensionParser.parseLengthToMillimeters(customHeightFeet)?.coerceAtLeast(1L) ?: defaults.second,
            DimensionParser.parseLengthToMillimeters(customSillFeet)?.coerceAtLeast(0L) ?: defaults.third
        )
    }
    val placeOpeningAtWorld: (PointMm, OpeningType) -> Unit = { worldPoint, type ->
        val nearest = floorWalls
            .map { it to BlueprintSnapMath.pointToWallDistanceMm(worldPoint, it) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= Millimeters.fromFeet(snap.thresholdFeet * 2).value }
            ?.first
        if (nearest != null) {
            val dims = openingDimensions(type)
            val opening = BlueprintSnapMath.placeOpeningAlongWall(
                wall = nearest,
                tapPointMm = worldPoint,
                widthMm = dims.first,
                heightMm = dims.second,
                sillMm = dims.third,
                type = type,
                openingId = UUID.randomUUID().toString()
            ).copy(
                tags = setOf(floorTag(selectedFloor))
            )
            state.updateBlueprintDocument(
                updated = document.copy(openings = document.openings + opening),
                label = "Add Opening"
            )
            selectedOpeningId = opening.id
            selectedWallId = null
        }
    }
    val finishAddonDrag: () -> Unit = {
        val type = draggingOpeningType
        val rootPoint = draggingScreenPoint
        if (type != null && rootPoint != null) {
            screenPointToWorldPoint(
                rootPoint = rootPoint,
                canvasRoot = canvasRoot,
                canvasSize = canvasSize,
                scale = scale,
                pan = pan
            )?.let { world ->
                placeOpeningAtWorld(world, type)
            }
        }
        draggingOpeningType = null
        draggingScreenPoint = null
    }
    val setTool: (String) -> Unit = { nextTool ->
        tool = nextTool
        if (nextTool != TOOL_DRAW) {
            drawingStart = null
            drawingPreview = null
            chainOrigin = null
        }
        if (nextTool != TOOL_BOX) {
            boxStart = null
            boxPreview = null
        }
    }
    val selectOpeningTool: (OpeningType) -> Unit = { type ->
        openingType = type
        setTool(type.toolKey())
    }
    val cancelCurrentAction: () -> Unit = {
        drawingStart = null
        drawingPreview = null
        chainOrigin = null
        boxStart = null
        boxPreview = null
        draggingOpeningType = null
        draggingScreenPoint = null
        selectedWallId = null
        selectedOpeningId = null
        if (tool != TOOL_SELECT && tool != TOOL_PAN) {
            tool = TOOL_DRAW
        }
    }
    val deleteSelectedGeometry: () -> Unit = {
        val openingId = selectedOpeningId
        if (openingId != null) {
            state.updateBlueprintDocument(
                updated = document.copy(
                    openings = document.openings.filterNot { it.id == openingId }
                ),
                label = "Delete Opening"
            )
            selectedOpeningId = null
        } else {
            val wallId = selectedWallId
            if (wallId != null) {
                val updatedWalls = document.walls.filterNot { it.id == wallId }
                val updatedOpenings = document.openings.filterNot { it.wallId == wallId }
                state.updateBlueprintDocument(
                    updated = document.copy(
                        walls = updatedWalls,
                        openings = updatedOpenings,
                        rooms = mergeDetectedRoomsForFloor(
                            existingRooms = document.rooms,
                            walls = updatedWalls,
                            floorLevel = selectedFloor
                        )
                    ),
                    label = "Delete Wall"
                )
                selectedWallId = null
            }
        }
    }
    fun applyToSelectedWall(
        label: String,
        transform: (WallSegment) -> WallSegment
    ) {
        val wallId = selectedWallId ?: return
        val current = document.walls.firstOrNull { wall -> wall.id == wallId } ?: return
        val updatedWall = transform(current)
        if (updatedWall == current) return
        val updatedWalls = document.walls.map { wall ->
            if (wall.id == wallId) updatedWall else wall
        }
        state.updateBlueprintDocument(
            updated = document.copy(
                walls = updatedWalls,
                rooms = mergeDetectedRoomsForFloor(
                    existingRooms = document.rooms,
                    walls = updatedWalls,
                    floorLevel = selectedFloor
                )
            ),
            label = label
        )
    }
    val updateBlueprintParams: (BlueprintParams) -> Unit = { updatedParams ->
        state.updateBlueprintDocument(
            document.copy(params = updatedParams),
            label = "Update Parameters"
        )
    }
    val snapChipSpecs = listOf(
        DesktopFilterChipSpec(
            label = "Grid",
            selected = snap.gridEnabled,
            onClick = { snap = snap.copy(gridEnabled = !snap.gridEnabled) }
        ),
        DesktopFilterChipSpec(
            label = "Angle",
            selected = snap.angleEnabled,
            onClick = { snap = snap.copy(angleEnabled = !snap.angleEnabled) }
        ),
        DesktopFilterChipSpec(
            label = "Endpoints",
            selected = snap.endpointEnabled,
            onClick = { snap = snap.copy(endpointEnabled = !snap.endpointEnabled) }
        ),
        DesktopFilterChipSpec(
            label = "Midpoints",
            selected = snap.midpointEnabled,
            onClick = { snap = snap.copy(midpointEnabled = !snap.midpointEnabled) }
        ),
        DesktopFilterChipSpec(
            label = "Closure",
            selected = snap.closureEnabled,
            onClick = { snap = snap.copy(closureEnabled = !snap.closureEnabled) }
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E192A))
            .onGloballyPositioned { workspaceRoot = it.positionInRoot() }
    ) {
        DesktopBlueprintCanvas(
            walls = floorWalls,
            openings = floorOpenings,
            tool = tool,
            snap = snap,
            scale = scale,
            pan = pan,
            drawingStart = drawingStart,
            drawingPreview = drawingPreview,
            boxStart = boxStart,
            boxPreview = boxPreview,
            selectedWallId = selectedWallId,
            selectedOpeningId = selectedOpeningId,
            onScaleChange = { scale = it.coerceIn(0.2f, 7f) },
            onPanChange = { pan = it },
            onCanvasLayout = { root, size ->
                canvasRoot = root
                canvasSize = size
            },
            onLivePointer = {
                if (drawingStart != null && tool == TOOL_DRAW) {
                    drawingPreview = it
                }
                if (boxStart != null && tool == TOOL_BOX) {
                    boxPreview = it
                }
            },
            onTap = { tap ->
                when (tool) {
                    TOOL_SELECT -> {
                        val thresholdMm = Millimeters.fromFeet(snap.thresholdFeet * 2.0).value
                        val nearestOpening = findNearestOpeningAtPoint(
                            point = tap,
                            openings = floorOpenings,
                            walls = floorWalls,
                            thresholdMm = thresholdMm
                        )
                        if (nearestOpening != null) {
                            selectedOpeningId = nearestOpening.id
                            selectedWallId = null
                        } else {
                            val nearestWall = findNearestWallAtPoint(
                                point = tap,
                                walls = floorWalls,
                                thresholdMm = thresholdMm
                            )
                            selectedWallId = nearestWall?.id
                            selectedOpeningId = null
                        }
                    }

                    TOOL_DRAW -> {
                        val snapped = BlueprintSnapMath.applySnapping(tap, drawingStart, snap, floorWalls)
                        if (drawingStart == null) {
                            val chained = if (chainWalls && !detachedWalls) floorWalls.lastOrNull()?.end else null
                            drawingStart = chained ?: snapped
                            drawingPreview = snapped
                            if (chainOrigin == null) chainOrigin = drawingStart
                        } else {
                            val start = drawingStart ?: return@DesktopBlueprintCanvas
                            var end = applyLengthAngleOverride(start, snapped, lengthInput, angleInput)
                            chainOrigin?.let { origin ->
                                BlueprintSnapMath.roomClosureSnap(
                                    candidateEnd = end,
                                    roomStart = origin,
                                    thresholdMm = Millimeters.fromFeet(snap.thresholdFeet).value
                                )?.let { end = it }
                            }
                            if (end != start) {
                                val newWall = WallSegment(
                                    id = UUID.randomUUID().toString(),
                                    start = start,
                                    end = end,
                                    height = Millimeters(document.params.wallHeightMm),
                                    thickness = Millimeters(document.params.defaultWallThicknessMm),
                                    tags = setOf("drawn", floorTag(selectedFloor))
                                )
                                val updatedWalls = document.walls + newWall
                                val rooms = mergeDetectedRoomsForFloor(
                                    existingRooms = document.rooms,
                                    walls = updatedWalls,
                                    floorLevel = selectedFloor
                                )
                                state.updateBlueprintDocument(
                                    updated = document.copy(walls = updatedWalls, rooms = rooms),
                                    label = "Add Wall"
                                )
                                selectedWallId = newWall.id
                                selectedOpeningId = null
                            }
                            val closed = chainOrigin != null && end == chainOrigin
                            if (chainWalls && !detachedWalls && !closed) {
                                drawingStart = end
                                drawingPreview = end
                            } else {
                                drawingStart = null
                                drawingPreview = null
                                chainOrigin = null
                            }
                        }
                    }

                    TOOL_BOX -> {
                        if (boxStart == null) {
                            boxStart = tap
                            boxPreview = tap
                        } else {
                            val start = boxStart ?: return@DesktopBlueprintCanvas
                            val walls = buildBoxWalls(
                                start = start,
                                end = tap,
                                params = document.params,
                                floorLevel = selectedFloor
                            )
                            if (walls.isNotEmpty()) {
                                val updatedWalls = document.walls + walls
                                state.updateBlueprintDocument(
                                    updated = document.copy(
                                        walls = updatedWalls,
                                        rooms = mergeDetectedRoomsForFloor(
                                            existingRooms = document.rooms,
                                            walls = updatedWalls,
                                            floorLevel = selectedFloor
                                        )
                                    ),
                                    label = "Add Box"
                                )
                                selectedWallId = walls.last().id
                                selectedOpeningId = null
                            }
                            boxStart = null
                            boxPreview = null
                        }
                    }

                    TOOL_DOOR,
                    TOOL_WINDOW,
                    TOOL_STAIR_UP,
                    TOOL_STAIR_DOWN -> {
                        placeOpeningAtWorld(tap, openingType)
                    }
                }
            }
        )

        Card(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC122035))) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Floor: ${selectedFloor.floorLabel()}", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { selectedFloor -= 1 },
                        modifier = Modifier.width(72.dp)
                    ) {
                        Text("Down")
                    }
                    Button(
                        onClick = { selectedFloor = FLOOR_GROUND_LEVEL },
                        modifier = Modifier.width(84.dp)
                    ) {
                        Text("Ground")
                    }
                    Button(
                        onClick = { selectedFloor += 1 },
                        modifier = Modifier.width(72.dp)
                    ) {
                        Text("Up")
                    }
                }
                Text("Rooms: ${floorRooms.size}", color = Color.White)
                Text("Walls: ${floorWalls.size}", color = Color.White)
                Text("Openings: ${floorOpenings.size}", color = Color.White)
                Text("Wall Length: ${"%.1f".format(wallLength)} ft", color = Color.White)
                Text("Net Area: ${"%.1f".format(netArea.coerceAtLeast(0.0))} sq ft", color = Color.White)
                selectedWall?.let { wall ->
                    Text(
                        "Selected Wall: ${"%.1f".format(Millimeters(wall.lengthMillimeters()).toFeet())} ft",
                        color = Color(0xFFFFE09B)
                    )
                }
                selectedOpening?.let { opening ->
                    Text(
                        "Selected ${opening.type.label()}: ${"%.1f".format(Millimeters(opening.widthMm).toFeet())} ft",
                        color = Color(0xFF9DF0FF)
                    )
                }
            }
        }
        if (selectedWall != null) {
            val nudgeStepMm = Millimeters.fromFeet(0.5).value
            val lengthStepMm = Millimeters.fromFeet(0.5).value
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 222.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1A2A42))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Wall Controls", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            applyToSelectedWall("Nudge Wall Left") { wall ->
                                wall.translateBy(dxMm = -nudgeStepMm, dyMm = 0L)
                            }
                        }) { Text("Left") }
                        Button(onClick = {
                            applyToSelectedWall("Nudge Wall Right") { wall ->
                                wall.translateBy(dxMm = nudgeStepMm, dyMm = 0L)
                            }
                        }) { Text("Right") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            applyToSelectedWall("Nudge Wall Up") { wall ->
                                wall.translateBy(dxMm = 0L, dyMm = nudgeStepMm)
                            }
                        }) { Text("Up") }
                        Button(onClick = {
                            applyToSelectedWall("Nudge Wall Down") { wall ->
                                wall.translateBy(dxMm = 0L, dyMm = -nudgeStepMm)
                            }
                        }) { Text("Down") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            applyToSelectedWall("Rotate Wall CCW") { wall ->
                                wall.rotateByDegrees(deltaDegrees = -5.0)
                            }
                        }) { Text("Rotate -5") }
                        Button(onClick = {
                            applyToSelectedWall("Rotate Wall CW") { wall ->
                                wall.rotateByDegrees(deltaDegrees = 5.0)
                            }
                        }) { Text("Rotate +5") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            applyToSelectedWall("Shorten Wall") { wall ->
                                wall.resizeByLengthDeltaMm(-lengthStepMm)
                            }
                        }) { Text("Length -6in") }
                        Button(onClick = {
                            applyToSelectedWall("Lengthen Wall") { wall ->
                                wall.resizeByLengthDeltaMm(lengthStepMm)
                            }
                        }) { Text("Length +6in") }
                    }
                }
            }
        }

        val liveStart = drawingStart
        val livePreview = drawingPreview
        if (liveStart != null && livePreview != null && tool == TOOL_DRAW) {
            val liveLen = Millimeters(BlueprintSnapMath.distanceMillimeters(liveStart, livePreview)).toFeet()
            val liveAngle = Math.toDegrees(
                atan2(
                    (livePreview.y - liveStart.y).toDouble(),
                    (livePreview.x - liveStart.x).toDouble()
                )
            )
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${"%.2f".format(liveLen)} ft @ ${"%.1f".format(liveAngle)}°")
                    OutlinedTextField(lengthInput, { lengthInput = it }, label = { Text("Len") }, singleLine = true, modifier = Modifier.width(90.dp))
                    OutlinedTextField(angleInput, { angleInput = it }, label = { Text("Ang") }, singleLine = true, modifier = Modifier.width(90.dp))
                }
            }
        }
        val liveBoxStart = boxStart
        val liveBoxPreview = boxPreview
        if (liveBoxStart != null && liveBoxPreview != null && tool == TOOL_BOX) {
            val widthFeet = Millimeters(abs(liveBoxPreview.x - liveBoxStart.x)).toFeet()
            val heightFeet = Millimeters(abs(liveBoxPreview.y - liveBoxStart.y)).toFeet()
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Box: ${"%.2f".format(widthFeet)} ft x ${"%.2f".format(heightFeet)} ft")
                }
            }
        }

        if (showParams) {
            Card(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Parameters")
                        Button(onClick = { showParams = false }) { Text("Hide") }
                    }
                    OutlinedTextField(
                        value = wallHeightFeet,
                        onValueChange = {
                            wallHeightFeet = it
                            it.toDoubleOrNull()?.let { value ->
                                updateBlueprintParams(
                                    document.params.copy(
                                        wallHeightMm = Millimeters.fromFeet(value).value
                                    )
                                )
                            }
                        },
                        label = { Text("Wall height (ft)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = paintCoats,
                        onValueChange = {
                            paintCoats = it
                            it.toIntOrNull()?.let { value ->
                                updateBlueprintParams(
                                    document.params.copy(
                                        paintCoats = value.coerceAtLeast(1)
                                    )
                                )
                            }
                        },
                        label = { Text("Paint coats") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wastePercent,
                        onValueChange = {
                            wastePercent = it
                            it.toDoubleOrNull()?.let { value ->
                                updateBlueprintParams(
                                    document.params.copy(
                                        wasteFactorPercent = value.coerceAtLeast(0.0)
                                    )
                                )
                            }
                        },
                        label = { Text("Waste (%)") },
                        singleLine = true
                    )
                    snapChipSpecs.chunked(2).forEach { chipRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            chipRow.forEach { chip ->
                                FilterChip(
                                    selected = chip.selected,
                                    onClick = chip.onClick,
                                    label = { Text(chip.label) }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            state.updateBlueprintDocument(
                                document.copy(
                                    rooms = mergeDetectedRoomsForFloor(
                                        existingRooms = document.rooms,
                                        walls = document.walls,
                                        floorLevel = selectedFloor
                                    )
                                )
                            )
                        }
                    ) { Text("Detect Rooms") }
                    Button(
                        onClick = {
                            val floorTagValue = floorTag(selectedFloor)
                            state.updateBlueprintDocument(
                                document.copy(
                                    rooms = document.rooms.map { room ->
                                        if (room.isOnFloor(selectedFloor)) {
                                            room.copy(tags = room.tags + "paint" + floorTagValue)
                                        } else {
                                            room
                                        }
                                    }
                                )
                            )
                        }
                    ) { Text("Scope + Paint") }
                }
            }
        } else {
            DesktopPanelLaunchButton(
                text = "Params",
                onClick = {
                    showParams = true
                    showAddons = false
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        }

        if (showAddons) {
            Card(modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Add-ons")
                    DesktopAddonDragButton(
                        label = OpeningType.DOOR.label(),
                        onClick = {
                            selectOpeningTool(OpeningType.DOOR)
                        },
                        onDragStart = { rootPoint ->
                            selectOpeningTool(OpeningType.DOOR)
                            draggingOpeningType = OpeningType.DOOR
                            draggingScreenPoint = rootPoint
                        },
                        onDrag = { rootPoint -> draggingScreenPoint = rootPoint },
                        onDragEnd = finishAddonDrag
                    )
                    DesktopAddonDragButton(
                        label = OpeningType.WINDOW.label(),
                        onClick = {
                            selectOpeningTool(OpeningType.WINDOW)
                        },
                        onDragStart = { rootPoint ->
                            selectOpeningTool(OpeningType.WINDOW)
                            draggingOpeningType = OpeningType.WINDOW
                            draggingScreenPoint = rootPoint
                        },
                        onDrag = { rootPoint -> draggingScreenPoint = rootPoint },
                        onDragEnd = finishAddonDrag
                    )
                    DesktopAddonDragButton(
                        label = OpeningType.STAIR_UP.label(),
                        onClick = {
                            selectOpeningTool(OpeningType.STAIR_UP)
                        },
                        onDragStart = { rootPoint ->
                            selectOpeningTool(OpeningType.STAIR_UP)
                            draggingOpeningType = OpeningType.STAIR_UP
                            draggingScreenPoint = rootPoint
                        },
                        onDrag = { rootPoint -> draggingScreenPoint = rootPoint },
                        onDragEnd = finishAddonDrag
                    )
                    DesktopAddonDragButton(
                        label = OpeningType.STAIR_DOWN.label(),
                        onClick = {
                            selectOpeningTool(OpeningType.STAIR_DOWN)
                        },
                        onDragStart = { rootPoint ->
                            selectOpeningTool(OpeningType.STAIR_DOWN)
                            draggingOpeningType = OpeningType.STAIR_DOWN
                            draggingScreenPoint = rootPoint
                        },
                        onDrag = { rootPoint -> draggingScreenPoint = rootPoint },
                        onDragEnd = finishAddonDrag
                    )
                    OutlinedTextField(
                        value = customWidthFeet,
                        onValueChange = { customWidthFeet = it },
                        label = { Text("Width") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customHeightFeet,
                        onValueChange = { customHeightFeet = it },
                        label = { Text("Height") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customSillFeet,
                        onValueChange = { customSillFeet = it },
                        label = { Text("Sill") },
                        singleLine = true
                    )
                    Button(onClick = { showAddons = false }) { Text("Hide") }
                }
            }
        } else {
            DesktopPanelLaunchButton(
                text = "Add-ons",
                onClick = {
                    showAddons = true
                    showParams = false
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)
            )
        }
        if (showGuide) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 210.dp, end = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xE11A2A43)
                )
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Desktop Guide", color = Color.White)
                    Text("• Select: click nearest wall/opening on current floor.", color = Color(0xFFD4E8FF))
                    Text("• Draw: click start then end. Chain continues from last corner.", color = Color(0xFFD4E8FF))
                    Text("• Box: click once to start rectangle, click again to finish.", color = Color(0xFFD4E8FF))
                    Text("• Door/Window/Stairs: click canvas or drag from Add-ons onto a wall.", color = Color(0xFFD4E8FF))
                    Text("• Mouse wheel zooms. Pan tool lets you drag the view.", color = Color(0xFFD4E8FF))
                    Text("• Floor controls scope all drawing/editing to one floor.", color = Color(0xFFD4E8FF))
                    Text("• Use Wall Controls for precise nudge, rotate, and length edits.", color = Color(0xFFD4E8FF))
                }
            }
        }

        if (draggingOpeningType != null && draggingScreenPoint != null) {
            val rootPoint = draggingScreenPoint ?: Offset.Zero
            val localPoint = rootPoint - workspaceRoot
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = (localPoint.x - 56f).roundToInt(),
                            y = (localPoint.y - 20f).roundToInt()
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = draggingOpeningType?.label().orEmpty(),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text("Drop on wall", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                desktopToolChipSpecs.forEach { spec ->
                    FilterChip(
                        selected = tool == spec.key,
                        onClick = {
                            spec.openingType?.let(selectOpeningTool) ?: run { setTool(spec.key) }
                        },
                        label = { Text(spec.label) }
                    )
                }
                listOf(
                    DesktopFilterChipSpec(
                        label = "Chain",
                        selected = chainWalls,
                        onClick = { chainWalls = !chainWalls }
                    ),
                    DesktopFilterChipSpec(
                        label = "Detached",
                        selected = detachedWalls,
                        onClick = { detachedWalls = !detachedWalls }
                    )
                ).forEach { chip ->
                    FilterChip(
                        selected = chip.selected,
                        onClick = chip.onClick,
                        label = { Text(chip.label) }
                    )
                }
                listOf(
                    DesktopActionButtonSpec(
                        label = "Undo",
                        enabled = state.canUndoBlueprint,
                        onClick = { state.undoBlueprint() }
                    ),
                    DesktopActionButtonSpec(
                        label = "Redo",
                        enabled = state.canRedoBlueprint,
                        onClick = { state.redoBlueprint() }
                    )
                ).forEach { action ->
                    Button(
                        onClick = action.onClick,
                        enabled = action.enabled
                    ) {
                        Text(action.label)
                    }
                }
                Button(
                    onClick = deleteSelectedGeometry,
                    enabled = selectedWallId != null || selectedOpeningId != null
                ) {
                    Text("Delete Selected")
                }
                Button(onClick = cancelCurrentAction) {
                    Text("Cancel")
                }
                Button(onClick = { showGuide = !showGuide }) {
                    Text(if (showGuide) "Hide Guide" else "Guide")
                }
            }
        }
    }
}

@Composable
private fun DesktopPanelLaunchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text)
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DesktopBlueprintCanvas(
    walls: List<WallSegment>,
    openings: List<BlueprintOpening>,
    tool: String,
    snap: BlueprintSnapSettings,
    scale: Float,
    pan: Offset,
    drawingStart: PointMm?,
    drawingPreview: PointMm?,
    boxStart: PointMm?,
    boxPreview: PointMm?,
    selectedWallId: String?,
    selectedOpeningId: String?,
    onScaleChange: (Float) -> Unit,
    onPanChange: (Offset) -> Unit,
    onCanvasLayout: (Offset, Size) -> Unit,
    onLivePointer: (PointMm) -> Unit,
    onTap: (PointMm) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    fun worldToScreen(p: PointMm): Offset {
        val ppm = BASE_PX_PER_MM * scale
        return Offset(canvasSize.width / 2f + pan.x + (p.x * ppm), canvasSize.height / 2f + pan.y - (p.y * ppm))
    }
    fun screenToWorld(p: Offset): PointMm {
        val ppm = BASE_PX_PER_MM * scale
        return PointMm(((p.x - canvasSize.width / 2f - pan.x) / ppm).roundToLong(), (-(p.y - canvasSize.height / 2f - pan.y) / ppm).roundToLong())
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                onCanvasLayout(it.positionInRoot(), it.size.toSize())
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                val factor = if (scroll < 0f) 1.12f else 0.9f
                onScaleChange(scale * factor)
            }
            .pointerInput(tool) {
                detectDragGestures { change, dragAmount ->
                    if (tool == TOOL_PAN) {
                        onPanChange(pan + dragAmount)
                        change.consume()
                    }
                }
            }
            .pointerInput(tool, scale, pan, snap, walls, drawingStart, boxStart) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        val worldPoint = screenToWorld(change.position)
                        val snapAnchor = when (tool) {
                            TOOL_DRAW -> drawingStart
                            TOOL_BOX -> boxStart
                            else -> null
                        }
                        val snappedPoint = BlueprintSnapMath.applySnapping(worldPoint, snapAnchor, snap, walls)
                        onLivePointer(snappedPoint)
                        if (change.changedToUpIfUnconsumed()) {
                            onTap(snappedPoint)
                        }
                    }
                }
            }
    ) {
        canvasSize = size

        if (snap.gridEnabled) {
            val gridStepMm = Millimeters.fromFeet(snap.gridStepFeet).value.coerceAtLeast(1L)
            val gridSpacingPx = (gridStepMm * (BASE_PX_PER_MM * scale)).coerceAtLeast(10f)
            if (gridSpacingPx <= 220f) {
                val originX = size.width / 2f + pan.x
                val originY = size.height / 2f + pan.y
                var x = originX % gridSpacingPx
                while (x < size.width) {
                    drawLine(
                        color = Color(0x2C9BC3E8),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridSpacingPx
                }
                var y = originY % gridSpacingPx
                while (y < size.height) {
                    drawLine(
                        color = Color(0x2C9BC3E8),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacingPx
                }
            }
        }

        drawLine(Color(0xFF2E5479), worldToScreen(PointMm(-70_000, 0)), worldToScreen(PointMm(70_000, 0)), strokeWidth = 1.4f)
        drawLine(Color(0xFF2E5479), worldToScreen(PointMm(0, -70_000)), worldToScreen(PointMm(0, 70_000)), strokeWidth = 1.4f)
        walls.forEach { wall ->
            val isSelected = wall.id == selectedWallId
            drawLine(
                color = if (isSelected) Color(0xFFFFD37A) else Color(0xFFA9D9FF),
                start = worldToScreen(wall.start),
                end = worldToScreen(wall.end),
                strokeWidth = if (isSelected) 5.2f else 3f,
                cap = StrokeCap.Round
            )
        }
        openings.forEach { opening ->
            val wall = walls.firstOrNull { it.id == opening.wallId } ?: return@forEach
            val center = BlueprintSnapMath.pointOnWall(wall, opening.t)
            val c = worldToScreen(center)
            val w = opening.widthMm * (BASE_PX_PER_MM * scale)
            val isSelected = opening.id == selectedOpeningId
            when (opening.type) {
                OpeningType.DOOR -> {
                    drawArc(
                        color = if (isSelected) Color(0xFFFFE09B) else Color(0xFFE9C06F),
                        startAngle = -90f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(c.x - w / 2f, c.y - w / 2f),
                        size = Size(w, w),
                        style = Stroke(width = if (isSelected) 3f else 2f)
                    )
                }

                OpeningType.WINDOW -> {
                    drawLine(
                        color = if (isSelected) Color(0xFF9EF4FF) else Color(0xFFC2F7FF),
                        start = Offset(c.x - w / 2f, c.y),
                        end = Offset(c.x + w / 2f, c.y),
                        strokeWidth = if (isSelected) 4.2f else 3f
                    )
                }

                OpeningType.STAIR_UP -> {
                    drawLine(
                        color = if (isSelected) Color(0xFFD2FFDB) else Color(0xFF8DE0A1),
                        start = Offset(c.x - w / 2f, c.y),
                        end = Offset(c.x + w / 2f, c.y),
                        strokeWidth = if (isSelected) 4.2f else 3f
                    )
                    drawLine(
                        color = if (isSelected) Color(0xFFD2FFDB) else Color(0xFF8DE0A1),
                        start = Offset(c.x, c.y + 8f),
                        end = Offset(c.x, c.y - 8f),
                        strokeWidth = 2f
                    )
                }

                OpeningType.STAIR_DOWN -> {
                    drawLine(
                        color = if (isSelected) Color(0xFFFFDEBA) else Color(0xFFFFB677),
                        start = Offset(c.x - w / 2f, c.y),
                        end = Offset(c.x + w / 2f, c.y),
                        strokeWidth = if (isSelected) 4.2f else 3f
                    )
                    drawLine(
                        color = if (isSelected) Color(0xFFFFDEBA) else Color(0xFFFFB677),
                        start = Offset(c.x - 8f, c.y),
                        end = Offset(c.x + 8f, c.y),
                        strokeWidth = 2f
                    )
                }
            }
        }
        if (drawingStart != null && drawingPreview != null && tool == TOOL_DRAW) {
            drawLine(Color(0xFFFFB66E), worldToScreen(drawingStart), worldToScreen(drawingPreview), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        if (boxStart != null && boxPreview != null && tool == TOOL_BOX) {
            val topLeft = PointMm(
                x = min(boxStart.x, boxPreview.x),
                y = max(boxStart.y, boxPreview.y)
            )
            val topRight = PointMm(
                x = max(boxStart.x, boxPreview.x),
                y = max(boxStart.y, boxPreview.y)
            )
            val bottomLeft = PointMm(
                x = min(boxStart.x, boxPreview.x),
                y = min(boxStart.y, boxPreview.y)
            )
            val bottomRight = PointMm(
                x = max(boxStart.x, boxPreview.x),
                y = min(boxStart.y, boxPreview.y)
            )
            drawLine(Color(0xFFFFB66E), worldToScreen(topLeft), worldToScreen(topRight), strokeWidth = 3f)
            drawLine(Color(0xFFFFB66E), worldToScreen(topRight), worldToScreen(bottomRight), strokeWidth = 3f)
            drawLine(Color(0xFFFFB66E), worldToScreen(bottomRight), worldToScreen(bottomLeft), strokeWidth = 3f)
            drawLine(Color(0xFFFFB66E), worldToScreen(bottomLeft), worldToScreen(topLeft), strokeWidth = 3f)
        }
    }
}

@Composable
private fun DesktopAddonDragButton(
    label: String,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootOffset = it.positionInRoot() }
            .pointerInput(label) {
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
            }
    ) {
        Text(label)
    }
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

private fun buildBoxWalls(
    start: PointMm,
    end: PointMm,
    params: BlueprintParams,
    floorLevel: Int
): List<WallSegment> {
    val left = min(start.x, end.x)
    val right = max(start.x, end.x)
    val bottom = min(start.y, end.y)
    val top = max(start.y, end.y)
    if ((right - left) < 50L || (top - bottom) < 50L) return emptyList()

    val a = PointMm(left, bottom)
    val b = PointMm(right, bottom)
    val c = PointMm(right, top)
    val d = PointMm(left, top)
    val thickness = Millimeters(params.defaultWallThicknessMm)
    val height = Millimeters(params.wallHeightMm)
    return listOf(
        a to b,
        b to c,
        c to d,
        d to a
    ).map { (from, to) ->
        WallSegment(
            id = UUID.randomUUID().toString(),
            start = from,
            end = to,
            thickness = thickness,
            height = height,
            tags = setOf("box", floorTag(floorLevel))
        )
    }
}

private fun findNearestWallAtPoint(
    point: PointMm,
    walls: List<WallSegment>,
    thresholdMm: Long
): WallSegment? {
    return walls
        .map { it to BlueprintSnapMath.pointToWallDistanceMm(point, it) }
        .minByOrNull { (_, distance) -> distance }
        ?.takeIf { (_, distance) -> distance <= thresholdMm }
        ?.first
}

private fun findNearestOpeningAtPoint(
    point: PointMm,
    openings: List<BlueprintOpening>,
    walls: List<WallSegment>,
    thresholdMm: Long
): BlueprintOpening? {
    return openings
        .mapNotNull { opening ->
            val wall = walls.firstOrNull { it.id == opening.wallId } ?: return@mapNotNull null
            val center = BlueprintSnapMath.pointOnWall(wall, opening.t)
            val distance = BlueprintSnapMath.distanceMillimeters(point, center)
            opening to distance
        }
        .minByOrNull { (_, distance) -> distance }
        ?.takeIf { (_, distance) -> distance <= thresholdMm }
        ?.first
}

private fun mergeDetectedRoomsForFloor(
    existingRooms: List<Room>,
    walls: List<WallSegment>,
    floorLevel: Int
): List<Room> {
    val floorWalls = walls.filter { wall -> wall.isOnFloor(floorLevel) }
    val detectedFloorRooms = RoomLoopDetector.detectRooms(floorWalls).map { room ->
        room.copy(tags = room.tags + floorTag(floorLevel))
    }
    val otherRooms = existingRooms.filterNot { room -> room.isOnFloor(floorLevel) }
    return otherRooms + detectedFloorRooms
}

private fun WallSegment.translateBy(dxMm: Long, dyMm: Long): WallSegment {
    return copy(
        start = PointMm(x = start.x + dxMm, y = start.y + dyMm),
        end = PointMm(x = end.x + dxMm, y = end.y + dyMm)
    )
}

private fun WallSegment.rotateByDegrees(deltaDegrees: Double): WallSegment {
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

private fun WallSegment.resizeByLengthDeltaMm(deltaMm: Long): WallSegment {
    if (deltaMm == 0L) return this
    val currentLengthMm = lengthMillimeters().toDouble().coerceAtLeast(1.0)
    val nextLengthMm = (currentLengthMm + deltaMm.toDouble()).coerceAtLeast(1.0)
    val angleRadians = atan2(
        (end.y - start.y).toDouble(),
        (end.x - start.x).toDouble()
    )
    val center = midpoint()
    val halfLengthMm = nextLengthMm / 2.0
    val nextStart = PointMm(
        x = (center.x + cos(angleRadians + Math.PI) * halfLengthMm).roundToLong(),
        y = (center.y + sin(angleRadians + Math.PI) * halfLengthMm).roundToLong()
    )
    val nextEnd = PointMm(
        x = (center.x + cos(angleRadians) * halfLengthMm).roundToLong(),
        y = (center.y + sin(angleRadians) * halfLengthMm).roundToLong()
    )
    return copy(start = nextStart, end = nextEnd)
}

private fun applyLengthAngleOverride(start: PointMm, fallbackEnd: PointMm, lengthInputFeet: String, angleInputDegrees: String): PointMm {
    val dx = (fallbackEnd.x - start.x).toDouble()
    val dy = (fallbackEnd.y - start.y).toDouble()
    val fallbackLengthMm = hypot(dx, dy)
    val fallbackAngle = Math.toDegrees(atan2(dy, dx))
    val lengthMm = DimensionParser.parseLengthToMillimeters(lengthInputFeet)
        ?.toDouble()
        ?.coerceAtLeast(1.0)
        ?: fallbackLengthMm
    val angle = DimensionParser.parseAngleDegrees(angleInputDegrees) ?: fallbackAngle
    val rad = Math.toRadians(angle)
    return PointMm(start.x + (cos(rad) * lengthMm).roundToLong(), start.y + (sin(rad) * lengthMm).roundToLong())
}

private fun PointerInputChange.changedToUpIfUnconsumed(): Boolean = !isConsumed && !pressed && previousPressed
