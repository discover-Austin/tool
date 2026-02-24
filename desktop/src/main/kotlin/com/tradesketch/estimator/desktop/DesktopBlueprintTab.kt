package com.tradesketch.estimator.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import com.tradesketch.estimator.utils.DimensionParser
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BASE_PX_PER_MM = 0.065f
private const val TOOL_SELECT = "select"
private const val TOOL_DRAW = "draw"
private const val TOOL_DOOR = "door"
private const val TOOL_WINDOW = "window"
private const val TOOL_PAN = "pan"
private const val TOOL_MEASURE = "measure"

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
    DesktopToolChipSpec(key = TOOL_DOOR, label = "Door", openingType = OpeningType.DOOR),
    DesktopToolChipSpec(key = TOOL_WINDOW, label = "Window", openingType = OpeningType.WINDOW),
    DesktopToolChipSpec(key = TOOL_PAN, label = "Pan"),
    DesktopToolChipSpec(key = TOOL_MEASURE, label = "Measure")
)

private fun OpeningType.toolKey(): String = when (this) {
    OpeningType.DOOR -> TOOL_DOOR
    OpeningType.WINDOW -> TOOL_WINDOW
    OpeningType.STAIR_UP -> TOOL_DRAW
    OpeningType.STAIR_DOWN -> TOOL_DRAW
}

private fun OpeningType.label(): String = when (this) {
    OpeningType.DOOR -> "Door"
    OpeningType.WINDOW -> "Window"
    OpeningType.STAIR_UP -> "Stair Up"
    OpeningType.STAIR_DOWN -> "Stair Down"
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
    var chainOrigin by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var chainWalls by remember(project.id) { mutableStateOf(true) }
    var detachedWalls by remember(project.id) { mutableStateOf(false) }
    var scale by remember(project.id) { mutableStateOf(1f) }
    var pan by remember(project.id) { mutableStateOf(Offset.Zero) }
    var snap by remember(project.id) { mutableStateOf(BlueprintSnapSettings()) }
    var lengthInput by remember(project.id) { mutableStateOf("") }
    var angleInput by remember(project.id) { mutableStateOf("") }
    var showAddons by remember(project.id) { mutableStateOf(openAddonsByDefault) }
    var showParams by remember(project.id) { mutableStateOf(false) }
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
    }

    val wallLength = document.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    val netArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(document).values.sum() -
        BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(document).values.sum()
    val openingDimensions: (OpeningType) -> Triple<Long, Long, Long> = { type ->
        val defaults = if (type == OpeningType.DOOR) {
            Triple(
                Millimeters.fromFeet(3.0).value,
                Millimeters.fromFeet(7.0).value,
                0L
            )
        } else {
            Triple(
                Millimeters.fromFeet(4.0).value,
                Millimeters.fromFeet(4.0).value,
                Millimeters.fromFeet(3.0).value
            )
        }
        Triple(
            DimensionParser.parseLengthToMillimeters(customWidthFeet)?.coerceAtLeast(1L) ?: defaults.first,
            DimensionParser.parseLengthToMillimeters(customHeightFeet)?.coerceAtLeast(1L) ?: defaults.second,
            DimensionParser.parseLengthToMillimeters(customSillFeet)?.coerceAtLeast(0L) ?: defaults.third
        )
    }
    val placeOpeningAtWorld: (PointMm, OpeningType) -> Unit = { worldPoint, type ->
        val nearest = document.walls
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
            )
            state.updateBlueprintDocument(
                updated = document.copy(openings = document.openings + opening),
                label = "Add Opening"
            )
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
    val selectOpeningTool: (OpeningType) -> Unit = { type ->
        openingType = type
        tool = type.toolKey()
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
            walls = document.walls,
            openings = document.openings,
            tool = tool,
            snap = snap,
            scale = scale,
            pan = pan,
            drawingStart = drawingStart,
            drawingPreview = drawingPreview,
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
            },
            onTap = { tap ->
                if (tool == TOOL_DRAW) {
                    val snapped = BlueprintSnapMath.applySnapping(tap, drawingStart, snap, document.walls)
                    if (drawingStart == null) {
                        val chained = if (chainWalls && !detachedWalls) document.walls.lastOrNull()?.end else null
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
                            val updatedWalls = document.walls + WallSegment(
                                id = UUID.randomUUID().toString(),
                                start = start,
                                end = end,
                                height = Millimeters(document.params.wallHeightMm),
                                thickness = Millimeters(document.params.defaultWallThicknessMm),
                                tags = setOf("drawn")
                            )
                            val rooms = RoomLoopDetector.detectRooms(updatedWalls)
                            state.updateBlueprintDocument(
                                updated = document.copy(walls = updatedWalls, rooms = rooms),
                                label = "Add Wall"
                            )
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
                } else if (tool == TOOL_DOOR || tool == TOOL_WINDOW) {
                    placeOpeningAtWorld(tap, openingType)
                }
            }
        )

        Card(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC122035))) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Rooms: ${document.rooms.size}", color = Color.White)
                Text("Walls: ${document.walls.size}", color = Color.White)
                Text("Openings: ${document.openings.size}", color = Color.White)
                Text("Wall Length: ${"%.1f".format(wallLength)} ft", color = Color.White)
                Text("Net Area: ${"%.1f".format(netArea.coerceAtLeast(0.0))} sq ft", color = Color.White)
            }
        }

        val liveStart = drawingStart
        val livePreview = drawingPreview
        if (liveStart != null && livePreview != null) {
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
                                document.copy(rooms = RoomLoopDetector.detectRooms(document.walls))
                            )
                        }
                    ) { Text("Detect Rooms") }
                    Button(
                        onClick = {
                            state.updateBlueprintDocument(
                                document.copy(rooms = document.rooms.map { it.copy(tags = it.tags + "paint") })
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
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                desktopToolChipSpecs.forEach { spec ->
                    FilterChip(
                        selected = tool == spec.key,
                        onClick = {
                            spec.openingType?.let(selectOpeningTool) ?: run { tool = spec.key }
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
            .pointerInput(tool, scale, pan, snap, walls, drawingStart) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        onLivePointer(BlueprintSnapMath.applySnapping(screenToWorld(change.position), drawingStart, snap, walls))
                        if (change.changedToUpIfUnconsumed()) onTap(screenToWorld(change.position))
                    }
                }
            }
    ) {
        canvasSize = size
        drawLine(Color(0xFF2E5479), worldToScreen(PointMm(-70_000, 0)), worldToScreen(PointMm(70_000, 0)), strokeWidth = 1.4f)
        drawLine(Color(0xFF2E5479), worldToScreen(PointMm(0, -70_000)), worldToScreen(PointMm(0, 70_000)), strokeWidth = 1.4f)
        walls.forEach { wall -> drawLine(Color(0xFFA9D9FF), worldToScreen(wall.start), worldToScreen(wall.end), strokeWidth = 3f, cap = StrokeCap.Round) }
        openings.forEach { opening ->
            val wall = walls.firstOrNull { it.id == opening.wallId } ?: return@forEach
            val center = BlueprintSnapMath.pointOnWall(wall, opening.t)
            val c = worldToScreen(center)
            val w = opening.widthMm * (BASE_PX_PER_MM * scale)
            if (opening.type == OpeningType.DOOR) {
                drawArc(Color(0xFFE9C06F), -90f, 90f, false, Offset(c.x - w / 2f, c.y - w / 2f), Size(w, w), style = Stroke(width = 2f))
            } else {
                drawLine(Color(0xFFC2F7FF), Offset(c.x - w / 2f, c.y), Offset(c.x + w / 2f, c.y), strokeWidth = 3f)
            }
        }
        if (drawingStart != null && drawingPreview != null && tool == TOOL_DRAW) {
            drawLine(Color(0xFFFFB66E), worldToScreen(drawingStart), worldToScreen(drawingPreview), strokeWidth = 3f, cap = StrokeCap.Round)
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
