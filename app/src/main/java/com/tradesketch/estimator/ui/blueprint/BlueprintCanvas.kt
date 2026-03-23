package com.tradesketch.estimator.ui.blueprint

import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.tradesketch.estimator.BuildConfig
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
import com.tradesketch.estimator.ui.viewmodel.isCurveDraftTool
import com.tradesketch.estimator.ui.viewmodel.isMeasuredArcTool
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

private val CORNER_ANGLE_HIGHLIGHT_LENGTH_MM = Millimeters.fromFeet(3.0).value
private const val CORNER_ANGLE_HIGHLIGHT_GLOW_WIDTH_PX = 12f
private const val CORNER_ANGLE_HIGHLIGHT_CORE_WIDTH_PX = 4.6f
private val CIRCLE_GUIDE_RING_COLOR = Color(0xFF6CE8FF)
private val CIRCLE_GUIDE_CORE_COLOR = Color(0xFFB5FBFF)
private val CIRCLE_SEGMENT_ACCENT_COLOR = Color(0xFF7EE6F4)
private val ARC_GUIDE_CHORD_COLOR = Color(0xFF6EE0FF)
private val ARC_GUIDE_HANDLE_COLOR = Color(0xFFFFC86D)
private val ARC_GUIDE_CORE_COLOR = Color(0xFFFFF3CC)
private const val ARC_GUIDE_CHIP_TEXT_SP = 9.2f
private const val ARC_GUIDE_CHIP_OFFSET_PX = 18f
private const val CIRCLE_GUIDE_CHIP_TEXT_SP = 9.4f
private const val CIRCLE_GUIDE_CHIP_OFFSET_PX = 18f
private val CORNER_ANGLE_BUCKET_COLORS = mapOf(
    15 to Color(0xFF87F07F),
    30 to Color(0xFF4FE6C8),
    45 to Color(0xFF73D9FF),
    60 to Color(0xFF7FAEFF),
    75 to Color(0xFFC58FFF),
    90 to Color(0xFFFFD05E),
    105 to Color(0xFFFFB06E),
    120 to Color(0xFFFF9468),
    135 to Color(0xFFFF84C3),
    150 to Color(0xFFE88FFF),
    165 to Color(0xFFFF98C5)
)

@Composable
internal fun BlueprintCanvas(
    modifier: Modifier = Modifier,
    document: BlueprintDocument,
    scope: TakeoffScope,
    tool: BlueprintDraftTool,
    snapSettings: BlueprintSnapSettings,
    scale: Float,
    pan: Offset,
    drawingStart: PointMm?,
    drawingPreview: PointMm?,
    boxStart: PointMm?,
    boxPreview: PointMm?,
    boxRotationRadians: Double,
    curveStart: PointMm?,
    curveEnd: PointMm?,
    curvePreviewPoint: PointMm?,
    circleCenter: PointMm?,
    circlePreviewEdge: PointMm?,
    selectedWallId: String?,
    selectedOpeningId: String?,
    selectedMeasuredArc: ArcSelectionInfo?,
    movingWallActive: Boolean,
    cursorVisible: Boolean,
    cursorSizeScale: Float,
    draftIntersectionActive: Boolean,
    useMetric: Boolean,
    lineSnappingEnabled: Boolean,
    dragPreview: OpeningDragPreview?,
    tutorialGuideWalls: List<WallSegment> = emptyList(),
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
    val selectionHighlightActive = selectedWallId != null || selectedOpeningId != null || movingWallActive
    val draftHighlightActive = draftIntersectionActive ||
        (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) ||
        (tool == BlueprintDraftTool.DRAW_BOX && boxStart != null && boxPreview != null) ||
        (tool.isCurveDraftTool() && curveStart != null && curvePreviewPoint != null) ||
        (tool == BlueprintDraftTool.DRAW_CIRCLE && circleCenter != null && circlePreviewEdge != null)
    val selectionPulse by animateFloatAsState(
        targetValue = if (selectionHighlightActive) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "selection-emphasis"
    )
    val snapPulse by animateFloatAsState(
        targetValue = if (draftHighlightActive) 1f else 0f,
        animationSpec = tween(durationMillis = if (draftIntersectionActive) 120 else 180),
        label = "draft-emphasis"
    )
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
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size < 2) continue
                        var prevCenterX = 0f
                        var prevCenterY = 0f
                        var currentCenterX = 0f
                        var currentCenterY = 0f
                        pressed.forEach { change ->
                            prevCenterX += change.previousPosition.x
                            prevCenterY += change.previousPosition.y
                            currentCenterX += change.position.x
                            currentCenterY += change.position.y
                        }
                        val count = pressed.size.toFloat()
                        val previousCentroid = Offset(prevCenterX / count, prevCenterY / count)
                        val currentCentroid = Offset(currentCenterX / count, currentCenterY / count)
                        val panDelta = currentCentroid - previousCentroid
                        var previousSpan = 0f
                        var currentSpan = 0f
                        pressed.forEach { change ->
                            previousSpan += hypot(
                                (change.previousPosition.x - previousCentroid.x).toDouble(),
                                (change.previousPosition.y - previousCentroid.y).toDouble()
                            ).toFloat()
                            currentSpan += hypot(
                                (change.position.x - currentCentroid.x).toDouble(),
                                (change.position.y - currentCentroid.y).toDouble()
                            ).toFloat()
                        }
                        previousSpan /= count
                        currentSpan /= count
                        val zoom = if (previousSpan > 0.0001f) {
                            (currentSpan / previousSpan).coerceIn(0.85f, 1.15f)
                        } else {
                            1f
                        }
                        if (panDelta != Offset.Zero || zoom != 1f) {
                            onPanScaleChange(pan + panDelta, scale * zoom)
                            event.changes.forEach { pointer -> pointer.consume() }
                        }
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
            .pointerInput(
                tool,
                scale,
                pan,
                drawingStart,
                boxStart,
                snapSettings,
                document.walls,
                touchEnabled,
                lineSnappingEnabled
            ) {
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
                    var gestureHadMultitouch = false
                    fun aimedPosition(raw: Offset): Offset {
                        return Offset(
                            x = (raw.x + CANVAS_TAP_AIM_OFFSET_PX.x).coerceIn(0f, canvasSize.width),
                            y = (raw.y + CANVAS_TAP_AIM_OFFSET_PX.y).coerceIn(0f, canvasSize.height)
                        )
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedTouchCount = event.changes.count { it.pressed }
                        if (pressedTouchCount >= 2) {
                            gestureHadMultitouch = true
                            continue
                        }
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.changedToDownIgnoreConsumed()) {
                            draggedDistancePx = 0f
                            gestureHadMultitouch = false
                        }
                        if (change.pressed && change.previousPressed) {
                            val delta = change.positionChange()
                            draggedDistancePx += hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                        }
                        val aimed = aimedPosition(change.position)
                        val drawingDraft =
                            tool == BlueprintDraftTool.DRAW_WALL ||
                                tool == BlueprintDraftTool.DRAW_BOX ||
                                tool.isCurveDraftTool() ||
                                tool == BlueprintDraftTool.DRAW_CIRCLE
                        drawWallPointerScreenPoint = if (drawingDraft && change.pressed) {
                            drawWallPointerHideAtMs = null
                            aimed
                        } else {
                            drawWallPointerScreenPoint
                        }
                        val rawWorld = screenToWorld(aimed)
                        val effectiveSnapSettings = if (lineSnappingEnabled) {
                            snapSettings
                        } else {
                            snapSettings.copy(
                                endpointEnabled = false,
                                midpointEnabled = false,
                                closureEnabled = false
                            )
                        }
                        val effectiveSnapWalls = if (lineSnappingEnabled) document.walls else emptyList()
                        val snappedWorld = BlueprintSnapMath.applySnapping(
                            rawPoint = rawWorld,
                            drawingStart = drawingStart,
                            settings = effectiveSnapSettings,
                            walls = effectiveSnapWalls
                        )
                        onLivePointerWorld(snappedWorld)
                        if (
                            change.changedToUpIgnoreConsumed() &&
                                !gestureHadMultitouch &&
                                draggedDistancePx < 10f &&
                                !change.isConsumed
                        ) {
                            onTapWorld(screenToWorld(aimed))
                        }
                        if (!drawingDraft) {
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
            val majorStepMm = Millimeters.fromFeet(snapSettings.gridStepFeet.coerceAtLeast(MIN_GRID_STEP_FEET)).value
                .coerceAtLeast(MIN_GRID_STEP_MM)
                .toDouble()
            val subdivisionsPerMajor = if (useMetric) 10L else 12L
            val minorStepMm = (majorStepMm / subdivisionsPerMajor.toDouble()).coerceAtLeast(1.0)
            val showMinorSubgrid = (minorStepMm * ppm) >= 1.1f
            val drawStepMm = if (showMinorSubgrid) minorStepMm else majorStepMm
            val linesPerMajor = if (showMinorSubgrid) subdivisionsPerMajor else 1L
            val emphasizedMajorEvery = if (useMetric) 10L else 5L

            val leftWorldX = screenToWorld(Offset(0f, 0f)).x.toDouble()
            val rightWorldX = screenToWorld(Offset(size.width, 0f)).x.toDouble()
            val topWorldY = screenToWorld(Offset(0f, 0f)).y.toDouble()
            val bottomWorldY = screenToWorld(Offset(0f, size.height)).y.toDouble()
            val minWorldX = minOf(leftWorldX, rightWorldX)
            val maxWorldX = maxOf(leftWorldX, rightWorldX)
            val minWorldY = minOf(topWorldY, bottomWorldY)
            val maxWorldY = maxOf(topWorldY, bottomWorldY)

            fun gridLineStyle(index: Long): Pair<Color, Float> {
                val isMajorLine = index % linesPerMajor == 0L
                if (!isMajorLine) return GRID_MINOR_COLOR to 0.62f
                val majorIndex = index / linesPerMajor
                val isEmphasisLine = majorIndex % emphasizedMajorEvery == 0L
                return if (isEmphasisLine) {
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
        val precomputeStartNs = if (BuildConfig.DEBUG) SystemClock.elapsedRealtimeNanos() else 0L
        val previewWall = if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            WallSegment(
                id = "__preview_wall__",
                start = drawingStart,
                end = drawingPreview
            )
        } else {
            null
        }
        val previewBoxWalls = if (tool == BlueprintDraftTool.DRAW_BOX && boxStart != null && boxPreview != null) {
            buildDraftBoxPreviewWalls(
                start = boxStart,
                end = boxPreview,
                rotationRadians = boxRotationRadians
            )
        } else {
            emptyList()
        }
        val previewArcWalls = if (tool.isCurveDraftTool() && curveStart != null && curvePreviewPoint != null) {
            if (tool.isMeasuredArcTool()) {
                buildMeasuredArcPreviewWalls(
                    start = curveStart,
                    end = curveEnd,
                    previewPoint = curvePreviewPoint
                )
            } else {
                buildDraftArcPreviewWalls(
                    start = curveStart,
                    end = curveEnd,
                    previewPoint = curvePreviewPoint
                )
            }
        } else {
            emptyList()
        }
        val previewCircleWalls = if (tool == BlueprintDraftTool.DRAW_CIRCLE && circleCenter != null && circlePreviewEdge != null) {
            buildDraftCirclePreviewWalls(
                center = circleCenter,
                edge = circlePreviewEdge
            )
        } else {
            emptyList()
        }
        val wallsForAngleHints = document.walls + listOfNotNull(previewWall) + previewBoxWalls
        val cornerAngleHints = collectCornerAngleHints(
            walls = wallsForAngleHints,
            highlightedWallId = previewWall?.id ?: previewBoxWalls.firstOrNull()?.id
        )
        val rightAngleCandidates = mutableListOf<RightAngleHint>()
        val nonRightCornerAngleHints = mutableListOf<CornerAngleHint>()
        cornerAngleHints.forEach { hint ->
            if (abs(hint.angleDegrees - 90.0) <= RIGHT_ANGLE_MARKER_TOLERANCE_DEG) {
                rightAngleCandidates += RightAngleHint(
                    corner = hint.corner,
                    legA = hint.legA,
                    legB = hint.legB,
                    highlighted = hint.highlighted
                )
            } else {
                nonRightCornerAngleHints += hint
            }
        }
        val rightAngleHints = collapseRightAngleHints(rightAngleCandidates)
        val roomWallsById = document.walls.associateBy { it.id }
        document.rooms.forEach { room ->
            val roomScope = resolveVisibleRoomTradeScope(
                room = room,
                wallsById = roomWallsById,
                activeScope = scope
            ) ?: return@forEach
            val roomStyle = roomScope.roomFillStyle(active = roomScope == scope) ?: return@forEach
            drawTradeRoomSurface(
                polygon = room.polygon.map(::worldToScreen),
                style = roomStyle
            )
        }
        tutorialGuideWalls.forEach { wall ->
            drawStyledWallSegment(
                start = worldToScreen(wall.start),
                end = worldToScreen(wall.end),
                color = OPENING_PREVIEW_WINDOW_COLOR.copy(alpha = 0.68f),
                strokeWidth = 3.6f,
                roundedCaps = false,
                selected = false,
                pulse = 0f
            )
        }
        val wallLengthLabels = collectCommittedWallLengthLabels(
            document = document,
            selectedWallId = selectedWallId,
            worldToScreen = ::worldToScreen
        ).toMutableList()
        document.walls.forEach { wall ->
            val isSelected = wall.id == selectedWallId
            val color = if (isSelected) {
                GEOMETRY_SELECTION_COLOR
            } else {
                resolveWallDisplayColor(
                    wall = wall,
                    activeScope = scope
                )
            }
            val parallelMatch = wallHasParallelLengthMatch(wall, document.walls)
            val isCircleSegment = wall.tags.contains(CURVE_SHAPE_CIRCLE_TAG)
            val isCurveSegment = wall.curveGroupTag() != null
            val strokeWidth = if (isSelected) 6.4f else if (parallelMatch) 5.2f else 4.4f
            val wallStartScreen = worldToScreen(wall.start)
            val wallEndScreen = worldToScreen(wall.end)
            drawStyledWallSegment(
                start = wallStartScreen,
                end = wallEndScreen,
                color = color,
                strokeWidth = strokeWidth,
                roundedCaps = isCurveSegment,
                selected = isSelected,
                pulse = selectionPulse
            )
            if (isCircleSegment) {
                drawCircleWallAccent(
                    start = wallStartScreen,
                    end = wallEndScreen,
                    selected = isSelected
                )
            }
        }
        if (BuildConfig.DEBUG) {
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - precomputeStartNs) / 1_000_000.0
            if (elapsedMs > 8.0) {
                Log.d(
                    "BlueprintPerf",
                    "canvasPrecomputeMs=${"%.1f".format(elapsedMs)} walls=${document.walls.size} openings=${document.openings.size}"
                )
            }
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
                strokeWidth = if (previewParallelMatch) 5.6f else 4.8f,
                roundedCaps = false,
                selected = true,
                pulse = snapPulse
            )
            wallLengthLabels += WallLengthLabelSpec(
                start = draftStart,
                end = draftEnd,
                lengthMm = BlueprintSnapMath.distanceMillimeters(drawingStart, drawingPreview),
                color = WALL_LABEL_ACTIVE_COLOR
            )
        }
        if (tool == BlueprintDraftTool.DRAW_BOX && previewBoxWalls.isNotEmpty()) {
            previewBoxWalls.forEachIndexed { index, wall ->
                val draftStart = worldToScreen(wall.start)
                val draftEnd = worldToScreen(wall.end)
                drawStyledWallSegment(
                    start = draftStart,
                    end = draftEnd,
                    color = DRAFT_WALL_COLOR,
                    strokeWidth = 4.8f,
                    roundedCaps = false,
                    selected = true,
                    pulse = snapPulse
                )
                if (index == 0 || index == 1) {
                    wallLengthLabels += WallLengthLabelSpec(
                        start = draftStart,
                        end = draftEnd,
                        lengthMm = wall.lengthMillimeters(),
                        color = WALL_LABEL_ACTIVE_COLOR
                    )
                }
            }
        }
        if (tool.isCurveDraftTool() && previewArcWalls.isNotEmpty()) {
            previewArcWalls.forEach { wall ->
                val draftStart = worldToScreen(wall.start)
                val draftEnd = worldToScreen(wall.end)
                drawStyledWallSegment(
                    start = draftStart,
                    end = draftEnd,
                    color = DRAFT_WALL_COLOR,
                    strokeWidth = 4.8f,
                    roundedCaps = true,
                    selected = true,
                    pulse = snapPulse
                )
            }
            if (curveStart != null && curveEnd == null && curvePreviewPoint != null) {
                wallLengthLabels += WallLengthLabelSpec(
                    start = worldToScreen(curveStart),
                    end = worldToScreen(curvePreviewPoint),
                    lengthMm = BlueprintSnapMath.distanceMillimeters(curveStart, curvePreviewPoint),
                    color = WALL_LABEL_ACTIVE_COLOR
                )
            }
        }
        if (tool == BlueprintDraftTool.DRAW_CIRCLE && previewCircleWalls.isNotEmpty()) {
            previewCircleWalls.forEach { wall ->
                val draftStart = worldToScreen(wall.start)
                val draftEnd = worldToScreen(wall.end)
                drawStyledWallSegment(
                    start = draftStart,
                    end = draftEnd,
                    color = DRAFT_WALL_COLOR,
                    strokeWidth = 4.6f,
                    roundedCaps = true,
                    selected = true,
                    pulse = snapPulse
                )
                drawCircleWallAccent(
                    start = draftStart,
                    end = draftEnd,
                    selected = true,
                    accentAlpha = 0.42f + (0.18f * snapPulse)
                )
            }
        }
        cornerAngleHints.forEach { hint ->
            drawCornerAngleLegHighlight(
                hint = hint,
                worldToScreen = ::worldToScreen
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
        if (tool.isCurveDraftTool() && curveStart != null && curveEnd != null && curvePreviewPoint != null) {
            if (tool.isMeasuredArcTool()) {
                drawMeasuredArcDraftGuide(
                    start = curveStart,
                    end = curveEnd,
                    control = curvePreviewPoint,
                    worldToScreen = ::worldToScreen,
                    useMetric = useMetric,
                    pulse = snapPulse
                )
            } else {
                drawArcDraftGuide(
                    start = curveStart,
                    end = curveEnd,
                    control = curvePreviewPoint,
                    worldToScreen = ::worldToScreen,
                    useMetric = useMetric,
                    pulse = snapPulse
                )
            }
        }
        if (tool == BlueprintDraftTool.DRAW_CIRCLE && circleCenter != null && circlePreviewEdge != null) {
            drawCircleDraftGuide(
                center = circleCenter,
                edge = circlePreviewEdge,
                worldToScreen = ::worldToScreen,
                useMetric = useMetric,
                pulse = snapPulse
            )
        }
        selectedMeasuredArc?.let { selection ->
            drawMeasuredArcSelectionGuide(
                selection = selection,
                worldToScreen = ::worldToScreen,
                useMetric = useMetric,
                pulse = selectionPulse
            )
        }
        wallLengthLabels.forEach { label ->
            drawWallLengthLabel(
                start = label.start,
                end = label.end,
                lengthMm = label.lengthMm,
                useMetric = useMetric,
                color = label.color,
                prefix = label.prefix
            )
        }
        val pointer = if (virtualPointerScreenPoint != null) {
            virtualPointerScreenPoint
        } else if (
            tool == BlueprintDraftTool.DRAW_WALL ||
            tool == BlueprintDraftTool.DRAW_BOX ||
            tool.isCurveDraftTool() ||
            tool == BlueprintDraftTool.DRAW_CIRCLE
        ) {
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
                boostActive = rightSelectBoostActive,
                intersectionActive = draftIntersectionActive,
                intersectionPulseProgress = snapPulse
            )
        }
        if (tool == BlueprintDraftTool.DRAW_WALL && drawingStart != null && drawingPreview != null) {
            drawPrecisionPulse(
                center = worldToScreen(drawingPreview),
                progress = snapPulse,
                color = if (draftIntersectionActive) GEOMETRY_INTERSECTION_PULSE else GEOMETRY_SNAP_PULSE,
                baseRadius = if (draftIntersectionActive) 13f else 11f,
                maxRadius = if (draftIntersectionActive) 33f else 28f
            )
        }
        if (tool == BlueprintDraftTool.DRAW_BOX && boxPreview != null) {
            drawPrecisionPulse(
                center = worldToScreen(boxPreview),
                progress = snapPulse,
                color = GEOMETRY_SNAP_PULSE,
                baseRadius = 11f,
                maxRadius = 28f
            )
        }
        if (tool.isCurveDraftTool() && curvePreviewPoint != null) {
            drawPrecisionPulse(
                center = worldToScreen(curvePreviewPoint),
                progress = snapPulse,
                color = GEOMETRY_SNAP_PULSE,
                baseRadius = 11f,
                maxRadius = 28f
            )
        }
        if (tool == BlueprintDraftTool.DRAW_CIRCLE && circlePreviewEdge != null) {
            drawPrecisionPulse(
                center = worldToScreen(circlePreviewEdge),
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
            tool == BlueprintDraftTool.DRAW_BOX && boxStart != null -> CursorGlyph.PENCIL
            tool.isCurveDraftTool() && curveStart != null -> CursorGlyph.PENCIL
            tool == BlueprintDraftTool.DRAW_CIRCLE && circleCenter != null -> CursorGlyph.PENCIL
            selectedWallId != null -> CursorGlyph.HAND_POINTER
            else -> CursorGlyph.ARROW
        }
        if (cursorVisible) {
            pointer?.let {
                drawCursorGlyph(
                    position = it,
                    glyph = cursorGlyph,
                    sizeScale = cursorSizeScale,
                    highlighted = draftIntersectionActive
                )
            }
        }
    }
}

@Composable
internal fun computeLiveScopeQuantity(
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
                tradeLabel = "Drywall Sheets",
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
                tradeLabel = "Concrete Volume",
                value = "${formatLiveValue(yards, 2)} cubic yards"
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
                tradeLabel = "Material Quantity",
                value = "${formatLiveValue(tons, 2)} tons / ${formatLiveValue(yards, 2)} cubic yards"
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
                tradeLabel = "Paint Quantity",
                value = "${formatLiveValue(gallons, 2)} gallons"
            )
        }
    }
}

internal fun DrawScope.drawBlueprintTexturePattern() {
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

internal fun DrawScope.drawTradeRoomSurface(
    polygon: List<Offset>,
    style: BlueprintRoomFillStyle
) {
    if (polygon.size < 3) return
    val path = Path().apply {
        moveTo(polygon.first().x, polygon.first().y)
        polygon.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
        close()
    }
    val minX = polygon.minOf { it.x }
    val minY = polygon.minOf { it.y }
    val maxX = polygon.maxOf { it.x }
    val maxY = polygon.maxOf { it.y }

    drawPath(
        path = path,
        color = style.fillColor
    )
    when (style.pattern) {
        BlueprintRoomFillPattern.CONCRETE_HATCH -> drawConcreteRoomPattern(
            path = path,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            color = style.patternColor
        )
        BlueprintRoomFillPattern.GRAVEL_PEBBLES -> drawGravelRoomPattern(
            path = path,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
            color = style.patternColor
        )
    }
    drawPath(
        path = path,
        color = style.outlineColor,
        style = Stroke(width = 1.8f)
    )
}

internal fun DrawScope.drawConcreteRoomPattern(
    path: Path,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    color: Color
) {
    val roomHeight = (maxY - minY).coerceAtLeast(1f)
    clipPath(path) {
        var x = minX - roomHeight
        while (x <= maxX + roomHeight) {
            drawLine(
                color = color,
                start = Offset(x, minY - 6f),
                end = Offset(x + roomHeight + 12f, maxY + 6f),
                strokeWidth = 1.1f
            )
            x += 24f
        }
    }
}

internal fun DrawScope.drawGravelRoomPattern(
    path: Path,
    minX: Float,
    minY: Float,
    maxX: Float,
    maxY: Float,
    color: Color
) {
    clipPath(path) {
        var row = 0
        var y = minY - 8f
        while (y <= maxY + 8f) {
            var column = 0
            var x = minX - 10f + if (row % 2 == 0) 0f else 11f
            while (x <= maxX + 10f) {
                val largePebble = (row + column) % 3 == 0
                drawCircle(
                    color = if (largePebble) color else color.copy(alpha = color.alpha * 0.8f),
                    radius = if (largePebble) 2.2f else 1.45f,
                    center = Offset(x, y)
                )
                x += 20f
                column += 1
            }
            y += 18f
            row += 1
        }
    }
}

internal fun DrawScope.drawStyledWallSegment(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    roundedCaps: Boolean,
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
    val strokeCap = if (roundedCaps) StrokeCap.Round else StrokeCap.Square

    drawLine(
        color = GEOMETRY_DEPTH_SHADOW,
        start = start + shadowOffset,
        end = end + shadowOffset,
        strokeWidth = strokeWidth + 4.6f + pulseBoost,
        cap = strokeCap
    )
    drawLine(
        color = GEOMETRY_HALO_COLOR,
        start = start,
        end = end,
        strokeWidth = strokeWidth + 3.2f + (pulseBoost * 0.45f),
        cap = strokeCap
    )
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth + (pulseBoost * 0.2f),
        cap = strokeCap
    )
    drawLine(
        color = GEOMETRY_DEPTH_HIGHLIGHT.copy(alpha = if (selected) 0.82f else 0.42f),
        start = start + highlightOffset,
        end = end + highlightOffset,
        strokeWidth = (strokeWidth * 0.34f).coerceAtLeast(1.15f),
        cap = strokeCap
    )
    drawLine(
        color = GEOMETRY_CORE_HIGHLIGHT.copy(alpha = if (selected) 0.84f else 0.56f),
        start = start,
        end = end,
        strokeWidth = (strokeWidth * 0.22f).coerceAtLeast(1.25f),
        cap = strokeCap
    )
    if (selected) {
        drawLine(
            color = GEOMETRY_SELECTION_PULSE.copy(alpha = 0.24f + (0.3f * pulse)),
            start = start,
            end = end,
            strokeWidth = strokeWidth + 5.2f + (pulseBoost * 0.6f),
            cap = strokeCap
        )
    }
}

internal fun DrawScope.drawPrecisionPulse(
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

internal fun DrawScope.drawOpeningOnWall(
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

internal fun DrawScope.drawFloatingOpeningPreview(
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

internal enum class CursorGlyph {
    ARROW,
    PENCIL,
    HAND_POINTER,
    GRAB
}

internal fun DrawScope.drawCursorGlyph(
    position: Offset,
    glyph: CursorGlyph,
    sizeScale: Float,
    highlighted: Boolean
) {
    drawCursorAnchorBlip(
        position = position,
        sizeScale = sizeScale,
        highlighted = highlighted
    )
    when (glyph) {
        CursorGlyph.ARROW -> drawArrowCursor(position, sizeScale)
        CursorGlyph.PENCIL -> drawPencilCursor(position, sizeScale)
        CursorGlyph.HAND_POINTER -> drawHandPointerCursor(position, sizeScale)
        CursorGlyph.GRAB -> drawGrabHandCursor(position, sizeScale)
    }
}

internal fun DrawScope.drawCursorAnchorBlip(
    position: Offset,
    sizeScale: Float,
    highlighted: Boolean
) {
    val clampedScale = sizeScale.coerceIn(0.75f, 2.1f)
    val glowRadius = 13f * clampedScale
    val ringRadius = 5.1f * clampedScale
    val dotRadius = 2.35f * clampedScale
    val glowColor = if (highlighted) {
        Color(0x66FFE27A)
    } else {
        Color(0x3E7EB1D8)
    }
    val ringColor = if (highlighted) {
        Color(0xFFFFE27A)
    } else {
        Color(0xFFB9D8F0)
    }
    val fillColor = if (highlighted) {
        Color(0xFFFFF1CB)
    } else {
        Color(0xFFF7FBFF)
    }

    drawCircle(
        color = glowColor,
        radius = glowRadius,
        center = position
    )
    drawCircle(
        color = ringColor.copy(alpha = if (highlighted) 0.96f else 0.78f),
        radius = ringRadius,
        center = position,
        style = Stroke(width = (1.45f * clampedScale).coerceAtLeast(1.1f))
    )
    drawCircle(
        color = fillColor,
        radius = dotRadius,
        center = position
    )
}

internal fun DrawScope.drawArrowCursor(position: Offset, sizeScale: Float) {
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

internal fun DrawScope.drawPencilCursor(position: Offset, sizeScale: Float) {
    fun s(value: Float): Float = value * sizeScale
    val tip = position
    val tail = Offset(position.x + s(26f), position.y + s(28f))
    val bodyStart = Offset(position.x + s(2.4f), position.y + s(2.6f))
    val bodyEnd = Offset(tail.x - s(3.8f), tail.y - s(4.1f))
    drawLine(
        color = Color(0xB7050A12),
        start = bodyStart,
        end = tail,
        strokeWidth = s(8.2f).coerceAtLeast(2f),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFDD74),
        start = bodyStart,
        end = bodyEnd,
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
        color = Color(0xFF1F1408),
        radius = s(1.7f).coerceAtLeast(0.85f),
        center = tip
    )
    drawCircle(
        color = Color(0xFFD56E8D),
        radius = s(2.6f).coerceAtLeast(1.2f),
        center = tail
    )
    drawCircle(
        color = Color(0xFFE9A7BC),
        radius = s(1.7f).coerceAtLeast(0.8f),
        center = tail
    )
}

internal fun DrawScope.drawHandPointerCursor(position: Offset, sizeScale: Float) {
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

internal fun DrawScope.drawGrabHandCursor(position: Offset, sizeScale: Float) {
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

internal fun DrawScope.drawSelectionMagnifier(
    pointer: Offset,
    nearestWall: WallSegment?,
    nearestPoint: PointMm?,
    worldToScreen: (PointMm) -> Offset,
    boostActive: Boolean,
    intersectionActive: Boolean,
    intersectionPulseProgress: Float
) {
    val radius = POINTER_LENS_RADIUS_PX
    val lensCenter = Offset(
        x = (pointer.x + POINTER_LENS_OFFSET_PX.x).coerceIn(radius + 6f, size.width - radius - 6f),
        y = (pointer.y + POINTER_LENS_OFFSET_PX.y).coerceIn(radius + 6f, size.height - radius - 6f)
    )
    if (intersectionActive) {
        val pulseRadius = radius + 10f + (intersectionPulseProgress * 10f)
        drawCircle(
            color = GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.18f + (0.10f * (1f - intersectionPulseProgress))),
            radius = pulseRadius,
            center = lensCenter
        )
        drawCircle(
            color = GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.28f),
            radius = radius + 3.5f,
            center = lensCenter
        )
    }
    drawLine(
        color = if (intersectionActive) {
            GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.72f)
        } else {
            Color(0x88ACD7FF)
        },
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
            color = when {
                intersectionActive -> Color(0xF129415A)
                boostActive -> Color(0xEF0D243B)
                else -> Color(0xDE0A1A2C)
            },
            radius = radius,
            center = lensCenter
        )
        if (intersectionActive) {
            drawCircle(
                color = GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.14f + (0.10f * intersectionPulseProgress)),
                radius = radius - 4f,
                center = lensCenter
            )
        }
        val gridColor = when {
            intersectionActive -> Color(0x78FFE694)
            boostActive -> Color(0x528FD2FF)
            else -> Color(0x3E7FAACD)
        }
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
                color = when {
                    intersectionActive -> GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.95f)
                    boostActive -> Color(0xFFFFEA9C)
                    else -> Color(0xFFFFE3AF)
                },
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
                color = when {
                    intersectionActive -> GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.98f)
                    boostActive -> Color(0xFFFFE27A)
                    else -> Color(0xFFFFF1CB)
                },
                radius = if (intersectionActive) 6.1f else 5.3f,
                center = point
            )
        }
        drawCircle(
            color = if (intersectionActive) {
                GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.54f)
            } else {
                Color(0x4CC8E9FF)
            },
            radius = if (intersectionActive) 9f else 7f,
            center = lensCenter,
            style = Stroke(width = if (intersectionActive) 2.4f else 1.8f)
        )
        if (intersectionActive) {
            drawCircle(
                color = Color(0xEFFFF9D8),
                radius = 4.4f + (intersectionPulseProgress * 1.8f),
                center = lensCenter
            )
        }
    }
    if (intersectionActive) {
        drawCircle(
            color = Color(0xE8FFF8D0),
            radius = radius - 3.4f,
            center = lensCenter,
            style = Stroke(width = 1.4f)
        )
    }
    drawCircle(
        color = when {
            intersectionActive -> GEOMETRY_INTERSECTION_PULSE.copy(alpha = 0.96f)
            boostActive -> Color(0xFF8FD8FF)
            else -> Color(0xB38AB4D8)
        },
        radius = radius,
        center = lensCenter,
        style = Stroke(
            width = when {
                intersectionActive -> 4.2f
                boostActive -> 2.8f
                else -> 2.2f
            }
        )
    )
}

internal fun DrawScope.drawRightAngleHint(
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
    val color = cornerAngleAccentColor(
        angleDegrees = 90.0,
        highlighted = hint.highlighted
    )
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

internal fun DrawScope.drawCornerAngleLabel(
    hint: CornerAngleHint,
    worldToScreen: (PointMm) -> Offset,
    scale: Float
) {
    val labelWorld = labelPointInsideAngle(
        corner = hint.corner,
        legA = hint.displayLegA,
        legB = hint.displayLegB,
        scale = scale
    ) ?: return
    val labelScreen = worldToScreen(labelWorld)
    val labelText = formatAngleLabel(hint.angleDegrees)
    val textColor = cornerAngleAccentColor(
        angleDegrees = hint.angleDegrees,
        highlighted = hint.highlighted
    )
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

internal fun DrawScope.drawCornerAngleLegHighlight(
    hint: CornerAngleHint,
    worldToScreen: (PointMm) -> Offset
) {
    val legA = unitStepFrom(hint.corner, hint.legA, CORNER_ANGLE_HIGHLIGHT_LENGTH_MM) ?: return
    val legB = unitStepFrom(hint.corner, hint.legB, CORNER_ANGLE_HIGHLIGHT_LENGTH_MM) ?: return
    val color = cornerAngleAccentColor(
        angleDegrees = hint.angleDegrees,
        highlighted = hint.highlighted
    )
    val glowAlpha = if (hint.highlighted) 0.34f else 0.22f
    val coreAlpha = if (hint.highlighted) 0.96f else 0.74f
    val cornerScreen = worldToScreen(hint.corner)
    val legAScreen = worldToScreen(legA)
    val legBScreen = worldToScreen(legB)
    listOf(legAScreen, legBScreen).forEach { endpoint ->
        drawLine(
            color = color.copy(alpha = glowAlpha),
            start = cornerScreen,
            end = endpoint,
            strokeWidth = CORNER_ANGLE_HIGHLIGHT_GLOW_WIDTH_PX,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = coreAlpha),
            start = cornerScreen,
            end = endpoint,
            strokeWidth = CORNER_ANGLE_HIGHLIGHT_CORE_WIDTH_PX,
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        color = color.copy(alpha = if (hint.highlighted) 0.9f else 0.72f),
        radius = if (hint.highlighted) 4.2f else 3.6f,
        center = cornerScreen
    )
}

internal fun DrawScope.drawCircleWallAccent(
    start: Offset,
    end: Offset,
    selected: Boolean,
    accentAlpha: Float = if (selected) 0.30f else 0.18f
) {
    drawLine(
        color = CIRCLE_SEGMENT_ACCENT_COLOR.copy(alpha = accentAlpha),
        start = start,
        end = end,
        strokeWidth = if (selected) 8.4f else 6.4f,
        cap = StrokeCap.Square
    )
    drawLine(
        color = CIRCLE_GUIDE_CORE_COLOR.copy(alpha = if (selected) 0.76f else 0.52f),
        start = start,
        end = end,
        strokeWidth = if (selected) 1.8f else 1.4f,
        cap = StrokeCap.Square
    )
}

internal fun DrawScope.drawCircleDraftGuide(
    center: PointMm,
    edge: PointMm,
    worldToScreen: (PointMm) -> Offset,
    useMetric: Boolean,
    pulse: Float
) {
    val radiusMm = BlueprintSnapMath.distanceMillimeters(center, edge)
    if (radiusMm < 35L) return
    val centerScreen = worldToScreen(center)
    val edgeScreen = worldToScreen(edge)
    val radiusPx = hypot(
        (edgeScreen.x - centerScreen.x).toDouble(),
        (edgeScreen.y - centerScreen.y).toDouble()
    ).toFloat()
    val dx = edgeScreen.x - centerScreen.x
    val dy = edgeScreen.y - centerScreen.y
    val lineMagnitude = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val nx = -dy / lineMagnitude
    val ny = dx / lineMagnitude
    val midpoint = Offset(
        x = (centerScreen.x + edgeScreen.x) / 2f,
        y = (centerScreen.y + edgeScreen.y) / 2f
    )
    val labelPoint = Offset(
        x = midpoint.x + (nx * CIRCLE_GUIDE_CHIP_OFFSET_PX),
        y = midpoint.y + (ny * CIRCLE_GUIDE_CHIP_OFFSET_PX)
    )
    val labelText = buildString {
        append("R ")
        append(formatLengthDisplay(mm = radiusMm, useMetric = useMetric))
        append(" | D ")
        append(formatLengthDisplay(mm = radiusMm * 2L, useMetric = useMetric))
    }

    drawCircle(
        color = CIRCLE_GUIDE_RING_COLOR.copy(alpha = 0.16f + (0.10f * pulse)),
        radius = radiusPx + (3.6f * pulse),
        center = centerScreen,
        style = Stroke(width = 2.2f)
    )
    drawLine(
        color = CIRCLE_GUIDE_RING_COLOR.copy(alpha = 0.30f + (0.18f * pulse)),
        start = centerScreen,
        end = edgeScreen,
        strokeWidth = 8.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = CIRCLE_GUIDE_CORE_COLOR.copy(alpha = 0.86f),
        start = centerScreen,
        end = edgeScreen,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = CIRCLE_GUIDE_CORE_COLOR.copy(alpha = 0.92f),
        radius = 4.8f,
        center = centerScreen
    )
    drawCircle(
        color = CIRCLE_GUIDE_RING_COLOR.copy(alpha = 0.84f),
        radius = 6.4f + pulse,
        center = centerScreen,
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        color = CIRCLE_GUIDE_CORE_COLOR.copy(alpha = 0.78f),
        radius = 4.2f,
        center = edgeScreen
    )
    drawContext.canvas.nativeCanvas.apply {
        val textSizePx = CIRCLE_GUIDE_CHIP_TEXT_SP * density
        val baselineY = labelPoint.y + (textSizePx * 0.34f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CIRCLE_GUIDE_CORE_COLOR.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.FILL
        }
        val textWidth = fillPaint.measureText(labelText)
        val padX = textSizePx * 0.56f
        val padYTop = textSizePx * 0.96f
        val padYBottom = textSizePx * 0.34f
        val chipRect = RectF(
            labelPoint.x - (textWidth / 2f) - padX,
            baselineY - padYTop,
            labelPoint.x + (textWidth / 2f) + padX,
            baselineY + padYBottom
        )
        val chipRadius = textSizePx * 0.56f
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xDE071522).toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipFillPaint)
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CIRCLE_GUIDE_RING_COLOR.copy(alpha = 0.86f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipStrokePaint)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0x8C07131F).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText(labelText, labelPoint.x + 0.6f, baselineY + 0.6f, shadowPaint)
        drawText(labelText, labelPoint.x, baselineY, fillPaint)
    }
}

internal fun DrawScope.drawArcDraftGuide(
    start: PointMm,
    end: PointMm,
    control: PointMm,
    worldToScreen: (PointMm) -> Offset,
    useMetric: Boolean,
    pulse: Float
) {
    val projection = projectArcDraftControl(
        start = start,
        end = end,
        control = control
    ) ?: return
    val measurements = measureArcDraft(
        start = start,
        end = end,
        control = control
    )
    if (measurements.spanMm < MIN_DRAW_WALL_LENGTH_MM) return
    val startScreen = worldToScreen(start)
    val endScreen = worldToScreen(end)
    val controlScreen = worldToScreen(control)
    val axisScreen = worldToScreen(projection.axisPoint)
    val handleDx = controlScreen.x - axisScreen.x
    val handleDy = controlScreen.y - axisScreen.y
    val handleMagnitude = hypot(handleDx.toDouble(), handleDy.toDouble()).toFloat()
    val chordDx = endScreen.x - startScreen.x
    val chordDy = endScreen.y - startScreen.y
    val chordMagnitude = hypot(chordDx.toDouble(), chordDy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val labelNormalX = if (handleMagnitude > 0.001f) {
        -handleDy / handleMagnitude
    } else {
        -chordDy / chordMagnitude
    }
    val labelNormalY = if (handleMagnitude > 0.001f) {
        handleDx / handleMagnitude
    } else {
        chordDx / chordMagnitude
    }
    val labelAnchor = Offset(
        x = (axisScreen.x + controlScreen.x) / 2f,
        y = (axisScreen.y + controlScreen.y) / 2f
    )
    val labelPoint = Offset(
        x = labelAnchor.x + (labelNormalX * ARC_GUIDE_CHIP_OFFSET_PX),
        y = labelAnchor.y + (labelNormalY * ARC_GUIDE_CHIP_OFFSET_PX)
    )
    val labelText = buildString {
        append("Curve ")
        append(formatLengthDisplay(mm = measurements.arcLengthMm, useMetric = useMetric))
        append(" | Span ")
        append(formatLengthDisplay(mm = measurements.spanMm, useMetric = useMetric))
        append(" | Bend ")
        append(formatLengthDisplay(mm = measurements.bendMm, useMetric = useMetric))
        append(" | Turn ")
        append(formatAngleLabel(measurements.turnDegrees))
    }

    drawLine(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.22f + (0.10f * pulse)),
        start = startScreen,
        end = endScreen,
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.28f + (0.14f * pulse)),
        start = axisScreen,
        end = controlScreen,
        strokeWidth = 8.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        start = axisScreen,
        end = controlScreen,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.88f),
        radius = 3.8f,
        center = axisScreen
    )
    drawCircle(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.9f),
        radius = 5.8f + pulse,
        center = controlScreen,
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        radius = 4.2f,
        center = controlScreen
    )
    drawContext.canvas.nativeCanvas.apply {
        val textSizePx = ARC_GUIDE_CHIP_TEXT_SP * density
        val baselineY = labelPoint.y + (textSizePx * 0.34f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_CORE_COLOR.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.FILL
        }
        val textWidth = fillPaint.measureText(labelText)
        val padX = textSizePx * 0.56f
        val padYTop = textSizePx * 0.96f
        val padYBottom = textSizePx * 0.34f
        val chipRect = RectF(
            labelPoint.x - (textWidth / 2f) - padX,
            baselineY - padYTop,
            labelPoint.x + (textWidth / 2f) + padX,
            baselineY + padYBottom
        )
        val chipRadius = textSizePx * 0.56f
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xDE201106).toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipFillPaint)
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.84f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipStrokePaint)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0x8C120A05).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText(labelText, labelPoint.x + 0.6f, baselineY + 0.6f, shadowPaint)
        drawText(labelText, labelPoint.x, baselineY, fillPaint)
    }
}

internal fun DrawScope.drawMeasuredArcDraftGuide(
    start: PointMm,
    end: PointMm,
    control: PointMm,
    worldToScreen: (PointMm) -> Offset,
    useMetric: Boolean,
    pulse: Float
) {
    val riseMm = measuredArcRiseFromControl(
        start = start,
        end = end,
        control = control
    )
    val geometry = measureMeasuredArcGeometry(
        start = start,
        end = end,
        riseMm = riseMm
    ) ?: return
    val measurements = measureMeasuredArcDraft(
        start = start,
        end = end,
        riseMm = riseMm
    )
    if (measurements.chordMm < MIN_DRAW_WALL_LENGTH_MM) return
    val startScreen = worldToScreen(start)
    val endScreen = worldToScreen(end)
    val midpointScreen = worldToScreen(geometry.midpoint)
    val arcMidpointScreen = worldToScreen(geometry.arcMidpoint)
    val handleDx = arcMidpointScreen.x - midpointScreen.x
    val handleDy = arcMidpointScreen.y - midpointScreen.y
    val handleMagnitude = hypot(handleDx.toDouble(), handleDy.toDouble()).toFloat()
    val chordDx = endScreen.x - startScreen.x
    val chordDy = endScreen.y - startScreen.y
    val chordMagnitude = hypot(chordDx.toDouble(), chordDy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val labelNormalX = if (handleMagnitude > 0.001f) {
        -handleDy / handleMagnitude
    } else {
        -chordDy / chordMagnitude
    }
    val labelNormalY = if (handleMagnitude > 0.001f) {
        handleDx / handleMagnitude
    } else {
        chordDx / chordMagnitude
    }
    val labelAnchor = Offset(
        x = (midpointScreen.x + arcMidpointScreen.x) / 2f,
        y = (midpointScreen.y + arcMidpointScreen.y) / 2f
    )
    val labelPoint = Offset(
        x = labelAnchor.x + (labelNormalX * ARC_GUIDE_CHIP_OFFSET_PX),
        y = labelAnchor.y + (labelNormalY * ARC_GUIDE_CHIP_OFFSET_PX)
    )
    val labelText = buildString {
        append("Arc ")
        append(formatLengthDisplay(mm = measurements.arcLengthMm, useMetric = useMetric))
        append(" | Chord ")
        append(formatLengthDisplay(mm = measurements.chordMm, useMetric = useMetric))
        append(" | Rise ")
        append(formatSignedLengthDisplay(mm = measurements.riseMm, useMetric = useMetric))
        append(" | Radius ")
        append(formatLengthDisplay(mm = measurements.radiusMm, useMetric = useMetric))
        append(" | Sweep ")
        append(formatAngleLabel(measurements.sweepDegrees))
    }

    drawLine(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.22f + (0.10f * pulse)),
        start = startScreen,
        end = endScreen,
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.28f + (0.14f * pulse)),
        start = midpointScreen,
        end = arcMidpointScreen,
        strokeWidth = 8.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        start = midpointScreen,
        end = arcMidpointScreen,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.88f),
        radius = 3.8f,
        center = midpointScreen
    )
    drawCircle(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.9f),
        radius = 5.8f + pulse,
        center = arcMidpointScreen,
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        radius = 4.2f,
        center = arcMidpointScreen
    )
    drawContext.canvas.nativeCanvas.apply {
        val textSizePx = ARC_GUIDE_CHIP_TEXT_SP * density
        val baselineY = labelPoint.y + (textSizePx * 0.34f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_CORE_COLOR.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.FILL
        }
        val textWidth = fillPaint.measureText(labelText)
        val padX = textSizePx * 0.56f
        val padYTop = textSizePx * 0.96f
        val padYBottom = textSizePx * 0.34f
        val chipRect = RectF(
            labelPoint.x - (textWidth / 2f) - padX,
            baselineY - padYTop,
            labelPoint.x + (textWidth / 2f) + padX,
            baselineY + padYBottom
        )
        val chipRadius = textSizePx * 0.56f
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xDE201106).toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipFillPaint)
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.84f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipStrokePaint)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0x8C120A05).toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        drawText(labelText, labelPoint.x + 0.6f, baselineY + 0.6f, shadowPaint)
        drawText(labelText, labelPoint.x, baselineY, fillPaint)
    }
}

internal fun DrawScope.drawMeasuredArcSelectionGuide(
    selection: ArcSelectionInfo,
    worldToScreen: (PointMm) -> Offset,
    useMetric: Boolean,
    pulse: Float
) {
    if (selection.kind != CurveSelectionKind.MEASURED_ARC) return
    val chordStart = selection.guideChordStart ?: return
    val chordEnd = selection.guideChordEnd ?: return
    val chordMidpoint = selection.guideChordMidpoint ?: return
    val arcMidpoint = selection.guideArcMidpoint ?: return
    val center = selection.guideCenter ?: return
    val radiusMm = selection.radiusMm ?: return
    if (radiusMm <= 0L) return

    val startScreen = worldToScreen(chordStart)
    val endScreen = worldToScreen(chordEnd)
    val midpointScreen = worldToScreen(chordMidpoint)
    val arcMidpointScreen = worldToScreen(arcMidpoint)
    val centerScreen = worldToScreen(center)
    val radiusDx = arcMidpointScreen.x - centerScreen.x
    val radiusDy = arcMidpointScreen.y - centerScreen.y
    val radiusLengthPx = hypot(radiusDx.toDouble(), radiusDy.toDouble()).toFloat().coerceAtLeast(0.001f)
    val radiusMidpoint = Offset(
        x = (centerScreen.x + arcMidpointScreen.x) / 2f,
        y = (centerScreen.y + arcMidpointScreen.y) / 2f
    )
    val labelPoint = Offset(
        x = radiusMidpoint.x + ((-radiusDy / radiusLengthPx) * ARC_GUIDE_CHIP_OFFSET_PX),
        y = radiusMidpoint.y + ((radiusDx / radiusLengthPx) * ARC_GUIDE_CHIP_OFFSET_PX)
    )
    val labelText = "R ${formatLengthDisplay(mm = radiusMm, useMetric = useMetric)}"

    drawLine(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.18f + (0.08f * pulse)),
        start = startScreen,
        end = endScreen,
        strokeWidth = 2.0f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.24f + (0.10f * pulse)),
        start = centerScreen,
        end = arcMidpointScreen,
        strokeWidth = 7.6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        start = centerScreen,
        end = arcMidpointScreen,
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = ARC_GUIDE_CHORD_COLOR.copy(alpha = 0.84f),
        radius = 3.6f,
        center = midpointScreen
    )
    drawCircle(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.82f),
        radius = 5.2f + (pulse * 0.8f),
        center = centerScreen,
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.9f),
        radius = 3.4f,
        center = centerScreen
    )
    drawCircle(
        color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.86f),
        radius = 5.0f + (pulse * 0.7f),
        center = arcMidpointScreen,
        style = Stroke(width = 1.6f)
    )
    drawCircle(
        color = ARC_GUIDE_CORE_COLOR.copy(alpha = 0.92f),
        radius = 3.6f,
        center = arcMidpointScreen
    )
    drawContext.canvas.nativeCanvas.apply {
        val textSizePx = ARC_GUIDE_CHIP_TEXT_SP * density
        val baselineY = labelPoint.y + (textSizePx * 0.34f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_CORE_COLOR.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
            style = Paint.Style.FILL
        }
        val textWidth = fillPaint.measureText(labelText)
        val padX = textSizePx * 0.48f
        val padYTop = textSizePx * 0.94f
        val padYBottom = textSizePx * 0.30f
        val chipRect = RectF(
            labelPoint.x - (textWidth / 2f) - padX,
            baselineY - padYTop,
            labelPoint.x + (textWidth / 2f) + padX,
            baselineY + padYBottom
        )
        val chipRadius = textSizePx * 0.54f
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xD7180C05).toArgb()
            style = Paint.Style.FILL
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipFillPaint)
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ARC_GUIDE_HANDLE_COLOR.copy(alpha = 0.82f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        drawRoundRect(chipRect, chipRadius, chipRadius, chipStrokePaint)
        drawText(labelText, labelPoint.x, baselineY, fillPaint)
    }
}

private fun cornerAngleAccentColor(
    angleDegrees: Double,
    highlighted: Boolean
): Color {
    val snappedBucket = ((angleDegrees / 15.0).roundToInt() * 15).coerceIn(15, 165)
    val base = CORNER_ANGLE_BUCKET_COLORS[snappedBucket] ?: Color(0xFFAFC6DF)
    return if (highlighted) {
        base.copy(alpha = 0.98f)
    } else {
        base.copy(alpha = 0.84f)
    }
}

internal fun wallHasParallelLengthMatch(target: WallSegment, walls: List<WallSegment>): Boolean {
    val targetLengthMm = target.lengthMillimeters()
    if (targetLengthMm <= 0L) return false
    val targetAngle = target.angleDegrees()
    return walls.any { other ->
        other.id != target.id &&
            parallelAngleDeltaDegrees(targetAngle, other.angleDegrees()) <= PARALLEL_MATCH_ANGLE_TOLERANCE_DEG &&
            abs(other.lengthMillimeters() - targetLengthMm) <= PARALLEL_MATCH_LENGTH_TOLERANCE_MM
    }
}

internal fun collectCornerAngleHints(
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
            if (wallA.sameCurveGroupAs(wallB)) continue
            val shared = sharedCorner(wallA, wallB) ?: continue
            val rawAngle = cornerAngleDegrees(
                corner = shared.corner,
                legA = shared.legA,
                legB = shared.legB
            )
            val displaySpec = displayAngleSpec(
                corner = shared.corner,
                legA = shared.legA,
                legB = shared.legB,
                rawAngleDegrees = rawAngle
            )
            val angle = displaySpec.angleDegrees
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
                highlighted = wallA.id == highlightedWallId || wallB.id == highlightedWallId,
                displayLegA = displaySpec.displayLegA,
                displayLegB = displaySpec.displayLegB
            )
        }
    }
    return hints
}

internal fun collapseRightAngleHints(hints: List<RightAngleHint>): List<RightAngleHint> {
    if (hints.size < 2) return hints
    val preferredByCorner = linkedMapOf<String, RightAngleHint>()
    hints.forEach { candidate ->
        val cornerBucket = "${candidate.corner.x / 5L}:${candidate.corner.y / 5L}"
        val existing = preferredByCorner[cornerBucket]
        if (existing == null || shouldReplaceRightAngleHint(existing, candidate)) {
            preferredByCorner[cornerBucket] = candidate
        }
    }
    return preferredByCorner.values.toList()
}

internal fun shouldReplaceRightAngleHint(
    existing: RightAngleHint,
    candidate: RightAngleHint
): Boolean {
    if (!existing.highlighted && candidate.highlighted) return true
    if (existing.highlighted && !candidate.highlighted) return false

    val existingStrength = rightAngleHintStrength(existing)
    val candidateStrength = rightAngleHintStrength(candidate)
    return candidateStrength > existingStrength
}

internal fun rightAngleHintStrength(hint: RightAngleHint): Long {
    val lenASq = distanceSquaredMm(hint.corner, hint.legA)
    val lenBSq = distanceSquaredMm(hint.corner, hint.legB)
    return minOf(lenASq, lenBSq)
}

internal fun distanceSquaredMm(a: PointMm, b: PointMm): Long {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return (dx * dx) + (dy * dy)
}

internal data class SharedCorner(
    val corner: PointMm,
    val legA: PointMm,
    val legB: PointMm
)

internal data class DisplayAngleSpec(
    val angleDegrees: Double,
    val displayLegA: PointMm,
    val displayLegB: PointMm
)

internal fun sharedCorner(a: WallSegment, b: WallSegment): SharedCorner? {
    fun mid(p: PointMm, q: PointMm): PointMm {
        return PointMm(
            x = ((p.x + q.x) / 2.0).roundToLong(),
            y = ((p.y + q.y) / 2.0).roundToLong()
        )
    }
    fun legFromCorner(
        displayCorner: PointMm,
        sharedPoint: PointMm,
        wallEndPoint: PointMm
    ): PointMm {
        return PointMm(
            x = displayCorner.x + (wallEndPoint.x - sharedPoint.x),
            y = displayCorner.y + (wallEndPoint.y - sharedPoint.y)
        )
    }
    return when {
        pointsNear(a.start, b.start) -> {
            val corner = mid(a.start, b.start)
            SharedCorner(
                corner = corner,
                legA = legFromCorner(corner, a.start, a.end),
                legB = legFromCorner(corner, b.start, b.end)
            )
        }
        pointsNear(a.start, b.end) -> {
            val corner = mid(a.start, b.end)
            SharedCorner(
                corner = corner,
                legA = legFromCorner(corner, a.start, a.end),
                legB = legFromCorner(corner, b.end, b.start)
            )
        }
        pointsNear(a.end, b.start) -> {
            val corner = mid(a.end, b.start)
            SharedCorner(
                corner = corner,
                legA = legFromCorner(corner, a.end, a.start),
                legB = legFromCorner(corner, b.start, b.end)
            )
        }
        pointsNear(a.end, b.end) -> {
            val corner = mid(a.end, b.end)
            SharedCorner(
                corner = corner,
                legA = legFromCorner(corner, a.end, a.start),
                legB = legFromCorner(corner, b.end, b.start)
            )
        }
        else -> null
    }
}

internal fun rightAngleDeltaDegrees(
    corner: PointMm,
    legA: PointMm,
    legB: PointMm
): Double {
    return abs(90.0 - cornerAngleDegrees(corner, legA, legB))
}

internal fun cornerAngleDegrees(
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
    val dot = ((ax * bx) + (ay * by)) / (magA * magB)
    val clampedDot = dot.coerceIn(-1.0, 1.0)
    val cross = ((ax * by) - (ay * bx)) / (magA * magB)
    return Math.toDegrees(abs(atan2(cross, clampedDot)))
}

internal fun displayAngleSpec(
    corner: PointMm,
    legA: PointMm,
    legB: PointMm,
    rawAngleDegrees: Double
): DisplayAngleSpec {
    if (rawAngleDegrees <= 90.0) {
        return DisplayAngleSpec(
            angleDegrees = rawAngleDegrees,
            displayLegA = legA,
            displayLegB = legB
        )
    }
    return DisplayAngleSpec(
        angleDegrees = 180.0 - rawAngleDegrees,
        displayLegA = mirroredLegAcrossCorner(corner, legA),
        displayLegB = legB
    )
}

internal fun mirroredLegAcrossCorner(
    corner: PointMm,
    leg: PointMm
): PointMm {
    return PointMm(
        x = corner.x - (leg.x - corner.x),
        y = corner.y - (leg.y - corner.y)
    )
}

internal fun labelPointInsideAngle(
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

internal fun formatAngleLabel(angle: Double): String {
    val roundedHalf = (round(angle * 2.0) / 2.0).coerceAtLeast(0.0)
    val nearestInt = roundedHalf.roundToLong().toDouble()
    return if (abs(roundedHalf - nearestInt) <= 0.05) {
        "${nearestInt.roundToLong()}°"
    } else {
        "${"%.1f".format(roundedHalf)}°"
    }
}

internal fun parallelAngleDeltaDegrees(a: Double, b: Double): Double {
    val delta = abs(normalizedAngleDeltaDegrees(a, b))
    return minOf(delta, abs(180.0 - delta))
}

internal fun normalizedAngleDeltaDegrees(from: Double, to: Double): Double {
    var delta = normalizeAngleDegrees(to) - normalizeAngleDegrees(from)
    while (delta > 180.0) delta -= 360.0
    while (delta < -180.0) delta += 360.0
    return delta
}

internal fun normalizeAngleDegrees(value: Double): Double {
    val wrapped = value % 360.0
    return if (wrapped < 0.0) wrapped + 360.0 else wrapped
}

internal fun unitStepFrom(
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

internal fun DrawScope.drawWallLengthLabel(
    start: Offset,
    end: Offset,
    lengthMm: Long,
    useMetric: Boolean,
    color: Color,
    prefix: String? = null
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

    val text = buildString {
        prefix?.takeIf { it.isNotBlank() }?.let {
            append(it)
            append(' ')
        }
        append(formatLengthDisplay(mm = lengthMm, useMetric = useMetric))
    }
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

internal fun collectCommittedWallLengthLabels(
    document: BlueprintDocument,
    selectedWallId: String?,
    worldToScreen: (PointMm) -> Offset
): List<WallLengthLabelSpec> {
    if (document.walls.isEmpty()) return emptyList()
    val curveGroupsByTag = document.walls
        .mapNotNull { wall -> wall.curveGroupTag()?.let { groupTag -> groupTag to wall } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    val emittedCurveGroups = mutableSetOf<String>()
    return buildList {
        document.walls.forEach { wall ->
            val curveGroupTag = wall.curveGroupTag()
            if (curveGroupTag == null) {
                add(
                    WallLengthLabelSpec(
                        start = worldToScreen(wall.start),
                        end = worldToScreen(wall.end),
                        lengthMm = wall.lengthMillimeters(),
                        color = if (wall.id == selectedWallId) WALL_LABEL_ACTIVE_COLOR else WALL_LABEL_NEUTRAL_COLOR
                    )
                )
                return@forEach
            }
            if (!emittedCurveGroups.add(curveGroupTag)) return@forEach
            val groupWalls = curveGroupsByTag[curveGroupTag].orEmpty()
            if (groupWalls.isEmpty()) return@forEach
            when {
                groupWalls.any { CURVE_SHAPE_CIRCLE_TAG in it.tags } -> {
                    // Closed curves read better from the selection card than a ring of tiny segment labels.
                }
                groupWalls.any { CURVE_SHAPE_ARC_TAG in it.tags } -> {
                    buildArcGroupLengthLabelSpec(
                        groupWalls = groupWalls,
                        selectedWallId = selectedWallId,
                        worldToScreen = worldToScreen
                    )?.let(::add)
                }
                else -> {
                    val firstWall = groupWalls.first()
                    val lastWall = groupWalls.last()
                    add(
                        WallLengthLabelSpec(
                            start = worldToScreen(firstWall.start),
                            end = worldToScreen(lastWall.end),
                            lengthMm = groupWalls.sumOf { it.lengthMillimeters() },
                            color = if (groupWalls.any { it.id == selectedWallId }) {
                                WALL_LABEL_ACTIVE_COLOR
                            } else {
                                WALL_LABEL_NEUTRAL_COLOR
                            }
                        )
                    )
                }
            }
        }
    }
}

internal fun buildArcGroupLengthLabelSpec(
    groupWalls: List<WallSegment>,
    selectedWallId: String?,
    worldToScreen: (PointMm) -> Offset
): WallLengthLabelSpec? {
    val firstWall = groupWalls.firstOrNull() ?: return null
    val lastWall = groupWalls.lastOrNull() ?: return null
    val storedMeasuredArcLengthMm = groupWalls.firstNotNullOfOrNull { wall ->
        wall.tags.measuredArcDraftMeasurements()?.arcLengthMm
    }
    val storedSketchArcLengthMm = groupWalls.firstNotNullOfOrNull { wall ->
        wall.tags.curveArcDraftMeasurements()?.arcLengthMm
    }
    val bendReferencePoint = groupWalls[groupWalls.lastIndex / 2].midpoint()
    val (labelStart, labelEnd) = orientCurveChordTowardBend(
        start = firstWall.start,
        end = lastWall.end,
        bendReference = bendReferencePoint
    )
    val labelPrefix = if (storedMeasuredArcLengthMm != null) "Arc" else "Curve"
    return WallLengthLabelSpec(
        start = worldToScreen(labelStart),
        end = worldToScreen(labelEnd),
        lengthMm = storedMeasuredArcLengthMm ?: storedSketchArcLengthMm ?: groupWalls.sumOf { it.lengthMillimeters() },
        color = if (groupWalls.any { it.id == selectedWallId }) {
            WALL_LABEL_ACTIVE_COLOR
        } else {
            WALL_LABEL_NEUTRAL_COLOR
        },
        prefix = labelPrefix
    )
}

internal fun orientCurveChordTowardBend(
    start: PointMm,
    end: PointMm,
    bendReference: PointMm
): Pair<PointMm, PointMm> {
    val cross = ((end.x - start.x) * (bendReference.y - start.y)) -
        ((end.y - start.y) * (bendReference.x - start.x))
    return if (cross < 0L) {
        end to start
    } else {
        start to end
    }
}

internal fun BlueprintOpening.doorSwingTag(): String? = when {
    tags.contains(DOOR_SWING_NEG_TAG) -> DOOR_SWING_NEG_TAG
    tags.contains(DOOR_SWING_POS_TAG) -> DOOR_SWING_POS_TAG
    else -> null
}

internal fun pointSideOfWall(point: PointMm, wall: WallSegment): Double {
    val wallX = (wall.end.x - wall.start.x).toDouble()
    val wallY = (wall.end.y - wall.start.y).toDouble()
    val pointX = (point.x - wall.start.x).toDouble()
    val pointY = (point.y - wall.start.y).toDouble()
    return (wallX * pointY) - (wallY * pointX)
}

