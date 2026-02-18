package com.tradesketch.estimator.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.calc.BlueprintTakeoffCalculator
import com.tradesketch.estimator.domain.calc.RoomLoopDetector
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.ProjectTemplate
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.domain.model.authoritativeBlueprint
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

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
    var tool by remember(project.id) { mutableStateOf("draw") }
    var drawingStart by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var drawingPreview by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var chainOrigin by remember(project.id) { mutableStateOf<PointMm?>(null) }
    var scale by remember(project.id) { mutableStateOf(1f) }
    var pan by remember(project.id) { mutableStateOf(Offset.Zero) }
    var snap by remember(project.id) { mutableStateOf(BlueprintSnapSettings()) }
    var lengthInput by remember(project.id) { mutableStateOf("") }
    var angleInput by remember(project.id) { mutableStateOf("") }
    var showAddons by remember(project.id) { mutableStateOf(openAddonsByDefault) }
    var openingType by remember(project.id) { mutableStateOf(OpeningType.DOOR) }
    var customWidthFeet by remember(project.id) { mutableStateOf("3.0") }
    var customHeightFeet by remember(project.id) { mutableStateOf("7.0") }
    var customSillFeet by remember(project.id) { mutableStateOf("0.0") }

    val wallLength = document.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    val netArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(document).values.sum() -
        BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(document).values.sum()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0E192A))) {
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
            onLivePointer = {
                if (drawingStart != null && tool == "draw") {
                    drawingPreview = it
                }
            },
            onTap = { tap ->
                if (tool == "draw") {
                    val snapped = BlueprintSnapMath.applySnapping(tap, drawingStart, snap, document.walls)
                    if (drawingStart == null) {
                        drawingStart = document.walls.lastOrNull()?.end ?: snapped
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
                            state.updateBlueprintDocument(document.copy(walls = updatedWalls, rooms = rooms))
                        }
                        val closed = chainOrigin != null && end == chainOrigin
                        if (!closed) {
                            drawingStart = end
                            drawingPreview = end
                        } else {
                            drawingStart = null
                            drawingPreview = null
                            chainOrigin = null
                        }
                    }
                } else if (tool == "door" || tool == "window") {
                    val nearest = document.walls
                        .map { it to BlueprintSnapMath.pointToWallDistanceMm(tap, it) }
                        .minByOrNull { it.second }
                        ?.takeIf { it.second <= Millimeters.fromFeet(snap.thresholdFeet * 2).value }
                        ?.first
                    if (nearest != null) {
                        val preset = if (openingType == OpeningType.DOOR) {
                            Triple(
                                customWidthFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: Millimeters.fromFeet(3.0).value,
                                customHeightFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: Millimeters.fromFeet(7.0).value,
                                customSillFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: 0L
                            )
                        } else {
                            Triple(
                                customWidthFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: Millimeters.fromFeet(4.0).value,
                                customHeightFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: Millimeters.fromFeet(4.0).value,
                                customSillFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value } ?: Millimeters.fromFeet(3.0).value
                            )
                        }
                        val opening = BlueprintSnapMath.placeOpeningAlongWall(
                            wall = nearest,
                            tapPointMm = tap,
                            widthMm = preset.first,
                            heightMm = preset.second,
                            sillMm = preset.third,
                            type = openingType,
                            openingId = UUID.randomUUID().toString()
                        )
                        state.updateBlueprintDocument(document.copy(openings = document.openings + opening))
                    }
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

        if (drawingStart != null && drawingPreview != null) {
            val liveLen = Millimeters(BlueprintSnapMath.distanceMillimeters(drawingStart!!, drawingPreview!!)).toFeet()
            val liveAngle = Math.toDegrees(atan2((drawingPreview!!.y - drawingStart!!.y).toDouble(), (drawingPreview!!.x - drawingStart!!.x).toDouble()))
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${"%.2f".format(liveLen)} ft @ ${"%.1f".format(liveAngle)}°")
                    OutlinedTextField(lengthInput, { lengthInput = it }, label = { Text("Len") }, singleLine = true, modifier = Modifier.width(90.dp))
                    OutlinedTextField(angleInput, { angleInput = it }, label = { Text("Ang") }, singleLine = true, modifier = Modifier.width(90.dp))
                }
            }
        }

        Card(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = snap.gridEnabled, onClick = { snap = snap.copy(gridEnabled = !snap.gridEnabled) }, label = { Text("Grid") })
                    FilterChip(selected = snap.angleEnabled, onClick = { snap = snap.copy(angleEnabled = !snap.angleEnabled) }, label = { Text("Angle") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = snap.endpointEnabled, onClick = { snap = snap.copy(endpointEnabled = !snap.endpointEnabled) }, label = { Text("Endpoints") })
                    FilterChip(selected = snap.midpointEnabled, onClick = { snap = snap.copy(midpointEnabled = !snap.midpointEnabled) }, label = { Text("Midpoints") })
                }
                FilterChip(selected = snap.closureEnabled, onClick = { snap = snap.copy(closureEnabled = !snap.closureEnabled) }, label = { Text("Closure") })
                Button(onClick = { state.updateBlueprintDocument(document.copy(rooms = RoomLoopDetector.detectRooms(document.walls))) }) { Text("Detect Rooms") }
                Button(onClick = { state.updateBlueprintDocument(document.copy(rooms = document.rooms.map { it.copy(tags = it.tags + "paint") })) }) { Text("Scope + Paint") }
            }
        }

        if (showAddons) {
            Card(modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Add-ons")
                    Button(onClick = { tool = "door"; openingType = OpeningType.DOOR }) { Text("Door") }
                    Button(onClick = { tool = "window"; openingType = OpeningType.WINDOW }) { Text("Window") }
                    OutlinedTextField(
                        value = customWidthFeet,
                        onValueChange = { customWidthFeet = it },
                        label = { Text("Width ft") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customHeightFeet,
                        onValueChange = { customHeightFeet = it },
                        label = { Text("Height ft") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customSillFeet,
                        onValueChange = { customSillFeet = it },
                        label = { Text("Sill ft") },
                        singleLine = true
                    )
                    Button(onClick = { showAddons = false }) { Text("Hide") }
                }
            }
        } else {
            Button(onClick = { showAddons = true }, modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)) {
                Text("Add-ons")
            }
        }

        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = tool == "draw", onClick = { tool = "draw" }, label = { Text("Draw") })
                FilterChip(selected = tool == "door", onClick = { tool = "door"; openingType = OpeningType.DOOR }, label = { Text("Door") })
                FilterChip(selected = tool == "window", onClick = { tool = "window"; openingType = OpeningType.WINDOW }, label = { Text("Window") })
                FilterChip(selected = tool == "pan", onClick = { tool = "pan" }, label = { Text("Pan") })
                Button(onClick = { state.undoBlueprint() }, enabled = state.canUndoBlueprint) { Text("Undo") }
                Button(onClick = { state.redoBlueprint() }, enabled = state.canRedoBlueprint) { Text("Redo") }
            }
        }
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
    onLivePointer: (PointMm) -> Unit,
    onTap: (PointMm) -> Unit
) {
    val basePxPerMm = 0.065f
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    fun worldToScreen(p: PointMm): Offset {
        val ppm = basePxPerMm * scale
        return Offset(canvasSize.width / 2f + pan.x + (p.x * ppm), canvasSize.height / 2f + pan.y - (p.y * ppm))
    }
    fun screenToWorld(p: Offset): PointMm {
        val ppm = basePxPerMm * scale
        return PointMm(((p.x - canvasSize.width / 2f - pan.x) / ppm).roundToLong(), (-(p.y - canvasSize.height / 2f - pan.y) / ppm).roundToLong())
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                val factor = if (scroll < 0f) 1.12f else 0.9f
                onScaleChange(scale * factor)
            }
            .pointerInput(tool) {
                detectDragGestures { change, dragAmount ->
                    if (tool == "pan") {
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
                        if (change.changedToUpIgnoreConsumed()) onTap(screenToWorld(change.position))
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
            val center = pointOnWall(wall, opening.t)
            val c = worldToScreen(center)
            val w = opening.widthMm * (basePxPerMm * scale)
            if (opening.type == OpeningType.DOOR) {
                drawArc(Color(0xFFE9C06F), -90f, 90f, false, Offset(c.x - w / 2f, c.y - w / 2f), Size(w, w), style = Stroke(width = 2f))
            } else {
                drawLine(Color(0xFFC2F7FF), Offset(c.x - w / 2f, c.y), Offset(c.x + w / 2f, c.y), strokeWidth = 3f)
            }
        }
        if (drawingStart != null && drawingPreview != null && tool == "draw") {
            drawLine(Color(0xFFFFB66E), worldToScreen(drawingStart), worldToScreen(drawingPreview), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

private fun pointOnWall(wall: WallSegment, t: Double): PointMm {
    val clamped = t.coerceIn(0.0, 1.0)
    return PointMm(
        x = wall.start.x + ((wall.end.x - wall.start.x) * clamped).roundToLong(),
        y = wall.start.y + ((wall.end.y - wall.start.y) * clamped).roundToLong()
    )
}

private fun applyLengthAngleOverride(start: PointMm, fallbackEnd: PointMm, lengthInputFeet: String, angleInputDegrees: String): PointMm {
    val dx = (fallbackEnd.x - start.x).toDouble()
    val dy = (fallbackEnd.y - start.y).toDouble()
    val fallbackLengthMm = hypot(dx, dy)
    val fallbackAngle = Math.toDegrees(atan2(dy, dx))
    val lengthMm = lengthInputFeet.toDoubleOrNull()?.let { Millimeters.fromFeet(it).value.toDouble() }?.coerceAtLeast(1.0) ?: fallbackLengthMm
    val angle = angleInputDegrees.toDoubleOrNull() ?: fallbackAngle
    val rad = Math.toRadians(angle)
    return PointMm(start.x + (cos(rad) * lengthMm).roundToLong(), start.y + (sin(rad) * lengthMm).roundToLong())
}

private fun PointerInputChange.changedToUpIgnoreConsumed(): Boolean = !pressed && previousPressed
