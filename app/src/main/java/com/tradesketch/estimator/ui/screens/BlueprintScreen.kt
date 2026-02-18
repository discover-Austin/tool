package com.tradesketch.estimator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import com.tradesketch.estimator.ui.viewmodel.BlueprintEditorViewModel
import com.tradesketch.estimator.utils.DimensionParser
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

private data class OpeningPreset(
    val name: String,
    val type: OpeningType,
    val widthMm: Long,
    val heightMm: Long,
    val sillMm: Long
)

private val doorPreset = OpeningPreset(
    name = "Door 3'x7'",
    type = OpeningType.DOOR,
    widthMm = Millimeters.fromFeet(3.0).value,
    heightMm = Millimeters.fromFeet(7.0).value,
    sillMm = 0L
)

private val windowPreset = OpeningPreset(
    name = "Window 4'x4'",
    type = OpeningType.WINDOW,
    widthMm = Millimeters.fromFeet(4.0).value,
    heightMm = Millimeters.fromFeet(4.0).value,
    sillMm = Millimeters.fromFeet(3.0).value
)

@Composable
fun BlueprintScreen(
    projectId: String,
    modifier: Modifier = Modifier,
    onOpenTakeoff: () -> Unit = {},
    onFullscreenBlueprintChanged: (Boolean) -> Unit = {},
    viewModel: BlueprintEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var tool by remember { mutableStateOf(BlueprintDraftTool.DRAW_WALL) }
    var drawingStart by remember { mutableStateOf<PointMm?>(null) }
    var drawingPreview by remember { mutableStateOf<PointMm?>(null) }
    var chainOrigin by remember { mutableStateOf<PointMm?>(null) }
    var chainWalls by remember { mutableStateOf(true) }
    var detachedWalls by remember { mutableStateOf(false) }
    var snapSettings by remember { mutableStateOf(BlueprintSnapSettings()) }
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var lengthInputFeet by remember { mutableStateOf("") }
    var angleInputDegrees by remember { mutableStateOf("") }
    var showAddons by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf<OpeningPreset?>(null) }
    var customWidthFeet by remember { mutableStateOf("3.0") }
    var customHeightFeet by remember { mutableStateOf("7.0") }
    var customSillFeet by remember { mutableStateOf("0.0") }

    LaunchedEffect(projectId) { viewModel.setProjectId(projectId) }
    if (uiState.isLoading || uiState.document == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val doc = uiState.document!!
    val wallArea = BlueprintTakeoffCalculator.wallAreaByIdSqFt(doc).values.sum()
    val openingArea = BlueprintTakeoffCalculator.openingAreaByWallIdSqFt(doc).values.sum()
    val netArea = (wallArea - openingArea).coerceAtLeast(0.0)
    val wallLengthFeet = doc.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0C1728))) {
        BlueprintCanvas(
            document = doc,
            tool = tool,
            snapSettings = snapSettings,
            scale = scale,
            pan = pan,
            drawingStart = drawingStart,
            drawingPreview = drawingPreview,
            onPanScaleChange = { updatedPan, updatedScale ->
                pan = updatedPan
                scale = updatedScale.coerceIn(0.2f, 7.5f)
            },
            onLivePointerWorld = {
                if (drawingStart != null && tool == BlueprintDraftTool.DRAW_WALL) {
                    drawingPreview = it
                }
            },
            onTapWorld = { tap ->
                when (tool) {
                    BlueprintDraftTool.DRAW_WALL -> {
                        val snappedTap = BlueprintSnapMath.applySnapping(
                            rawPoint = tap,
                            drawingStart = drawingStart,
                            settings = snapSettings,
                            walls = doc.walls
                        )
                        if (drawingStart == null) {
                            val chained = if (chainWalls && !detachedWalls) doc.walls.lastOrNull()?.end else null
                            drawingStart = chained ?: snappedTap
                            drawingPreview = snappedTap
                            if (chainOrigin == null) chainOrigin = drawingStart
                        } else {
                            val start = drawingStart ?: return@BlueprintCanvas
                            var end = applyLengthAngleOverride(
                                start = start,
                                fallbackEnd = snappedTap,
                                lengthInputFeet = lengthInputFeet,
                                angleInputDegrees = angleInputDegrees
                            )
                            if (snapSettings.closureEnabled) {
                                chainOrigin?.let { origin ->
                                    BlueprintSnapMath.roomClosureSnap(
                                        candidateEnd = end,
                                        roomStart = origin,
                                        thresholdMm = Millimeters.fromFeet(snapSettings.thresholdFeet).value
                                    )?.let { end = it }
                                }
                            }
                            if (end != start) {
                                viewModel.addWall(
                                    WallSegment(
                                        id = UUID.randomUUID().toString(),
                                        start = start,
                                        end = end,
                                        height = Millimeters(doc.params.wallHeightMm),
                                        thickness = Millimeters(doc.params.defaultWallThicknessMm),
                                        tags = setOf("drawn")
                                    )
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
                    }
                    BlueprintDraftTool.PLACE_DOOR,
                    BlueprintDraftTool.PLACE_WINDOW -> {
                        val basePreset = selectedPreset ?: if (tool == BlueprintDraftTool.PLACE_DOOR) doorPreset else windowPreset
                        val preset = basePreset.copy(
                            widthMm = DimensionParser.parseLengthToMillimeters(customWidthFeet)
                                ?.coerceAtLeast(1L)
                                ?: basePreset.widthMm,
                            heightMm = DimensionParser.parseLengthToMillimeters(customHeightFeet)
                                ?.coerceAtLeast(1L)
                                ?: basePreset.heightMm,
                            sillMm = DimensionParser.parseLengthToMillimeters(customSillFeet)
                                ?.coerceAtLeast(0L)
                                ?: basePreset.sillMm
                        )
                        val nearestWall = doc.walls
                            .map { it to BlueprintSnapMath.pointToWallDistanceMm(tap, it) }
                            .minByOrNull { it.second }
                            ?.takeIf { it.second <= Millimeters.fromFeet(snapSettings.thresholdFeet * 2).value }
                            ?.first
                        if (nearestWall != null) {
                            viewModel.addOpening(
                                BlueprintSnapMath.placeOpeningAlongWall(
                                    wall = nearestWall,
                                    tapPointMm = tap,
                                    widthMm = preset.widthMm,
                                    heightMm = preset.heightMm,
                                    sillMm = preset.sillMm,
                                    type = preset.type,
                                    openingId = UUID.randomUUID().toString()
                                )
                            )
                        }
                    }
                    else -> Unit
                }
            }
        )

        LiveOverlay(
            doc = doc,
            wallLengthFeet = wallLengthFeet,
            netArea = netArea,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        )

        DrawingInputPanel(
            drawingStart = drawingStart,
            drawingPreview = drawingPreview,
            lengthInputFeet = lengthInputFeet,
            angleInputDegrees = angleInputDegrees,
            onLengthChange = { lengthInputFeet = it },
            onAngleChange = { angleInputDegrees = it },
            onLock = {
                val start = drawingStart ?: return@DrawingInputPanel
                val preview = drawingPreview ?: return@DrawingInputPanel
                drawingPreview = applyLengthAngleOverride(start, preview, lengthInputFeet, angleInputDegrees)
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )

        ParamsPanel(
            params = doc.params,
            snap = snapSettings,
            onParamsChange = viewModel::updateParams,
            onSnapChange = { snapSettings = it },
            onScopeExpand = viewModel::expandScopeWithPaint,
            onDetectRooms = viewModel::ensureRoomDetection,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        )

        AddonsDrawer(
            expanded = showAddons,
            selectedPreset = selectedPreset,
            onToggle = { showAddons = !showAddons },
            onSelectPreset = {
                selectedPreset = it
                tool = if (it.type == OpeningType.DOOR) BlueprintDraftTool.PLACE_DOOR else BlueprintDraftTool.PLACE_WINDOW
            },
            customWidthFeet = customWidthFeet,
            customHeightFeet = customHeightFeet,
            customSillFeet = customSillFeet,
            onCustomWidthChange = { customWidthFeet = it },
            onCustomHeightChange = { customHeightFeet = it },
            onCustomSillChange = { customSillFeet = it },
            modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp)
        )

        Toolbar(
            tool = tool,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            chainWalls = chainWalls,
            detachedWalls = detachedWalls,
            onSelectTool = { tool = it; if (it != BlueprintDraftTool.DRAW_WALL) { drawingStart = null; drawingPreview = null; chainOrigin = null } },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onToggleChain = { chainWalls = !chainWalls },
            onToggleDetached = { detachedWalls = !detachedWalls },
            onOpenTakeoff = onOpenTakeoff,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }
    onFullscreenBlueprintChanged(true)
}

@Composable
private fun BlueprintCanvas(
    document: BlueprintDocument,
    tool: BlueprintDraftTool,
    snapSettings: BlueprintSnapSettings,
    scale: Float,
    pan: Offset,
    drawingStart: PointMm?,
    drawingPreview: PointMm?,
    onPanScaleChange: (Offset, Float) -> Unit,
    onLivePointerWorld: (PointMm) -> Unit,
    onTapWorld: (PointMm) -> Unit
) {
    val basePxPerMm = 0.065f
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    fun worldToScreen(p: PointMm): Offset {
        val ppm = basePxPerMm * scale
        return Offset(canvasSize.width / 2f + pan.x + (p.x * ppm), canvasSize.height / 2f + pan.y - (p.y * ppm))
    }
    fun screenToWorld(p: Offset): PointMm {
        val ppm = basePxPerMm * scale
        return PointMm(
            ((p.x - canvasSize.width / 2f - pan.x) / ppm).roundToLong(),
            (-(p.y - canvasSize.height / 2f - pan.y) / ppm).roundToLong()
        )
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
            .pointerInput(tool, scale, pan, drawingStart, snapSettings, document.walls) {
                detectTransformGestures { _, panDelta, zoom, _ ->
                    onPanScaleChange(pan + panDelta, scale * zoom)
                }
            }
            .pointerInput(tool, scale, pan, drawingStart, snapSettings, document.walls) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        onLivePointerWorld(
                            BlueprintSnapMath.applySnapping(
                                rawPoint = screenToWorld(change.position),
                                drawingStart = drawingStart,
                                settings = snapSettings,
                                walls = document.walls
                            )
                        )
                        if (change.changedToUpIgnoreConsumed()) onTapWorld(screenToWorld(change.position))
                    }
                }
            }
    ) {
        canvasSize = size
        drawLine(Color(0xFF2C5072), worldToScreen(PointMm(-70_000, 0)), worldToScreen(PointMm(70_000, 0)), strokeWidth = 1.4f)
        drawLine(Color(0xFF2C5072), worldToScreen(PointMm(0, -70_000)), worldToScreen(PointMm(0, 70_000)), strokeWidth = 1.4f)
        document.walls.forEach { wall ->
            drawLine(Color(0xFFA2D6FF), worldToScreen(wall.start), worldToScreen(wall.end), strokeWidth = 3f, cap = StrokeCap.Round)
        }
        document.openings.forEach { opening ->
            val wall = document.walls.firstOrNull { it.id == opening.wallId } ?: return@forEach
            val center = pointOnWall(wall, opening.t)
            val c = worldToScreen(center)
            val w = opening.widthMm * (basePxPerMm * scale)
            if (opening.type == OpeningType.DOOR) {
                drawArc(Color(0xFFE7BE73), -90f, 90f, false, Offset(c.x - w / 2f, c.y - w / 2f), Size(w, w), style = Stroke(width = 2f))
            } else {
                drawLine(Color(0xFFC5F5FF), Offset(c.x - w / 2f, c.y), Offset(c.x + w / 2f, c.y), strokeWidth = 3f)
                drawPath(
                    path = Path().apply {
                        moveTo(c.x - w / 6f, c.y - 6f)
                        lineTo(c.x + w / 6f, c.y + 6f)
                    },
                    color = Color(0xFFC5F5FF),
                    style = Stroke(width = 2f)
                )
            }
        }
        if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            drawLine(Color(0xFFFFB46D), worldToScreen(drawingStart), worldToScreen(drawingPreview), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

@Composable private fun Toolbar(
    tool: BlueprintDraftTool, canUndo: Boolean, canRedo: Boolean, chainWalls: Boolean, detachedWalls: Boolean,
    onSelectTool: (BlueprintDraftTool) -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onToggleChain: () -> Unit,
    onToggleDetached: () -> Unit, onOpenTakeoff: () -> Unit, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                BlueprintDraftTool.SELECT to "Select",
                BlueprintDraftTool.DRAW_WALL to "Draw",
                BlueprintDraftTool.PLACE_DOOR to "Door",
                BlueprintDraftTool.PLACE_WINDOW to "Window",
                BlueprintDraftTool.PAN to "Pan",
                BlueprintDraftTool.MEASURE to "Measure"
            ).forEach { (t, label) -> FilterChip(selected = tool == t, onClick = { onSelectTool(t) }, label = { Text(label) }) }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = chainWalls, onClick = onToggleChain, label = { Text("Chain") })
            FilterChip(selected = detachedWalls, onClick = onToggleDetached, label = { Text("Detached") })
            IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.Filled.Undo, contentDescription = null) }
            IconButton(onClick = onRedo, enabled = canRedo) { Icon(Icons.Filled.Redo, contentDescription = null) }
            Button(onClick = onOpenTakeoff) { Text("Materials") }
        }
    }
}

@Composable
private fun DrawingInputPanel(
    drawingStart: PointMm?, drawingPreview: PointMm?, lengthInputFeet: String, angleInputDegrees: String,
    onLengthChange: (String) -> Unit, onAngleChange: (String) -> Unit, onLock: () -> Unit, modifier: Modifier = Modifier
) {
    if (drawingStart == null || drawingPreview == null) return
    val lengthFt = Millimeters(BlueprintSnapMath.distanceMillimeters(drawingStart, drawingPreview)).toFeet()
    val angle = Math.toDegrees(atan2((drawingPreview.y - drawingStart.y).toDouble(), (drawingPreview.x - drawingStart.x).toDouble()))
    
    // Parse input to show feedback
    val parsedLengthMm = DimensionParser.parseLengthToMillimeters(lengthInputFeet)
    val parsedAngle = DimensionParser.parseAngleDegrees(angleInputDegrees)
    val lengthValid = lengthInputFeet.isBlank() || parsedLengthMm != null
    val angleValid = angleInputDegrees.isBlank() || parsedAngle != null
    
    Card(modifier = modifier.widthIn(max = 620.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Live: ${"%.2f".format(lengthFt)} ft @ ${"%.1f".format(angle)}°", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = lengthInputFeet,
                        onValueChange = onLengthChange,
                        label = { Text("Length") },
                        singleLine = true,
                        isError = !lengthValid,
                        supportingText = {
                            if (lengthValid && parsedLengthMm != null) {
                                Text("= ${"%.2f".format(Millimeters(parsedLengthMm).toFeet())} ft", style = MaterialTheme.typography.bodySmall)
                            } else if (lengthValid) {
                                Text("12' 6\", 12.5ft, 3800mm, 3.8m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = angleInputDegrees,
                        onValueChange = onAngleChange,
                        label = { Text("Angle") },
                        singleLine = true,
                        isError = !angleValid,
                        supportingText = {
                            if (angleValid && parsedAngle != null) {
                                Text("= ${"%.1f".format(parsedAngle)}°", style = MaterialTheme.typography.bodySmall)
                            } else if (angleValid) {
                                Text("45, 45°, 45deg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(onClick = onLock, enabled = lengthValid && angleValid) { Text("Lock") }
            }
        }
    }
}

@Composable
private fun ParamsPanel(
    params: BlueprintParams, snap: BlueprintSnapSettings, onParamsChange: (BlueprintParams) -> Unit,
    onSnapChange: (BlueprintSnapSettings) -> Unit, onScopeExpand: () -> Unit, onDetectRooms: () -> Unit, modifier: Modifier = Modifier
) {
    var heightFt by remember(params.wallHeightMm) { mutableStateOf("%.2f".format(Millimeters(params.wallHeightMm).toFeet())) }
    var coats by remember(params.paintCoats) { mutableStateOf(params.paintCoats.toString()) }
    var waste by remember(params.wasteFactorPercent) { mutableStateOf(params.wasteFactorPercent.toString()) }
    Card(modifier = modifier.width(300.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))) {
        Column(Modifier.padding(10.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Parameters", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(heightFt, { heightFt = it; it.toDoubleOrNull()?.let { v -> onParamsChange(params.copy(wallHeightMm = Millimeters.fromFeet(v).value)) } }, label = { Text("Wall height (ft)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(coats, { coats = it; it.toIntOrNull()?.let { v -> onParamsChange(params.copy(paintCoats = v.coerceAtLeast(1))) } }, label = { Text("Paint coats") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(waste, { waste = it; it.toDoubleOrNull()?.let { v -> onParamsChange(params.copy(wasteFactorPercent = v.coerceAtLeast(0.0))) } }, label = { Text("Waste (%)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Snap toggles", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(snap.gridEnabled, { onSnapChange(snap.copy(gridEnabled = !snap.gridEnabled)) }, label = { Text("Grid") })
                FilterChip(snap.angleEnabled, { onSnapChange(snap.copy(angleEnabled = !snap.angleEnabled)) }, label = { Text("Angle") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(snap.endpointEnabled, { onSnapChange(snap.copy(endpointEnabled = !snap.endpointEnabled)) }, label = { Text("Endpoints") })
                FilterChip(snap.midpointEnabled, { onSnapChange(snap.copy(midpointEnabled = !snap.midpointEnabled)) }, label = { Text("Midpoints") })
            }
            FilterChip(snap.closureEnabled, { onSnapChange(snap.copy(closureEnabled = !snap.closureEnabled)) }, label = { Text("Room closure") })
            Button(onClick = onDetectRooms, modifier = Modifier.fillMaxWidth()) { Text("Detect Rooms") }
            Button(onClick = onScopeExpand, modifier = Modifier.fillMaxWidth()) { Text("Scope Expansion: Paint") }
        }
    }
}

@Composable
private fun AddonsDrawer(
    expanded: Boolean,
    selectedPreset: OpeningPreset?,
    onToggle: () -> Unit,
    onSelectPreset: (OpeningPreset) -> Unit,
    customWidthFeet: String,
    customHeightFeet: String,
    customSillFeet: String,
    onCustomWidthChange: (String) -> Unit,
    onCustomHeightChange: (String) -> Unit,
    onCustomSillChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)) {
        if (!expanded) {
            Button(onClick = onToggle) { Text("Add-ons") }
        } else {
            Column(Modifier.width(220.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add-ons", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = onToggle) { Text("Hide") }
                }
                Text("Choose preset, set size, then tap or drag onto a wall.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AddonPresetCard(doorPreset, selectedPreset == doorPreset, { Icon(Icons.Filled.DoorFront, contentDescription = null) }) { onSelectPreset(doorPreset) }
                AddonPresetCard(windowPreset, selectedPreset == windowPreset, { Icon(Icons.Filled.Window, contentDescription = null) }) { onSelectPreset(windowPreset) }
                OutlinedTextField(
                    value = customWidthFeet,
                    onValueChange = onCustomWidthChange,
                    label = { Text("Width") },
                    singleLine = true,
                    supportingText = { Text("3', 3.5ft, 900mm", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customHeightFeet,
                    onValueChange = onCustomHeightChange,
                    label = { Text("Height") },
                    singleLine = true,
                    supportingText = { Text("7', 7ft, 2100mm", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customSillFeet,
                    onValueChange = onCustomSillChange,
                    label = { Text("Sill") },
                    singleLine = true,
                    supportingText = { Text("0', 3ft, 900mm", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AddonPresetCard(preset: OpeningPreset, selected: Boolean, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column {
                Text(preset.name)
                Text(if (preset.type == OpeningType.DOOR) "Door swing arc" else "Window break", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LiveOverlay(doc: BlueprintDocument, wallLengthFeet: Double, netArea: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xCC111F32))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Live Quantities", color = Color(0xFFB7D8FF), fontWeight = FontWeight.SemiBold)
            Text("Rooms: ${doc.rooms.size}", color = Color.White)
            Text("Walls: ${doc.walls.size}", color = Color.White)
            Text("Openings: ${doc.openings.size}", color = Color.White)
            Text("Wall Length: ${"%.1f".format(wallLengthFeet)} ft", color = Color.White)
            Text("Net Wall Area: ${"%.1f".format(netArea)} sq ft", color = Color.White)
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
    val fallbackLength = hypot(dx, dy)
    val fallbackAngle = Math.toDegrees(atan2(dy, dx))
    
    // Use DimensionParser to support multiple formats: "12' 6\"", "12.5ft", "3800mm", "3.8m"
    val lengthMm = DimensionParser.parseLengthToMillimeters(lengthInputFeet)?.toDouble()?.coerceAtLeast(1.0) ?: fallbackLength
    val angleDeg = DimensionParser.parseAngleDegrees(angleInputDegrees) ?: fallbackAngle
    
    val rad = Math.toRadians(angleDeg)
    return PointMm(start.x + (cos(rad) * lengthMm).roundToLong(), start.y + (sin(rad) * lengthMm).roundToLong())
}

private fun PointerInputChange.changedToUpIgnoreConsumed(): Boolean = !pressed && previousPressed
