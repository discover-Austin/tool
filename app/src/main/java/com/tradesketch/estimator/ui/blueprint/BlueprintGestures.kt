package com.tradesketch.estimator.ui.blueprint

import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

@Composable
internal fun DualJoystickOverlay(
    leftVector: Offset,
    rightVector: Offset,
    onLeftVectorChange: (Offset) -> Unit,
    onRightVectorChange: (Offset) -> Unit,
    onLeftPressChange: ((Boolean) -> Unit)? = null,
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
    belowHistoryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 420.dp
        val ultraCompact = maxWidth < 360.dp
        val sidePadding = if (compact) 4.dp else 8.dp
        val joystickSize = when {
            ultraCompact -> 94.dp
            compact -> 104.dp
            else -> 126.dp
        }
        val knobSize = when {
            ultraCompact -> 38.dp
            compact -> 42.dp
            else -> 50.dp
        }
        val labelFontSize = if (compact) 7.sp else 8.sp
        val centerColumnBottom = if (compact) 14.dp else 24.dp
        val controlRowSpacing = if (compact) 5.dp else 6.dp
        val zoomButtonSize = if (compact) 32.dp else 36.dp
        val zoomIconSize = if (compact) 14.dp else 16.dp
        val historyButtonSize = if (compact) 30.dp else 34.dp
        val historyIconSize = if (compact) 13.dp else 15.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePadding)
        ) {
            JoystickPad(
                insideLabel = "Pan / Alt",
                vector = leftVector,
                onVectorChange = onLeftVectorChange,
                onTap = onLeftTap,
                onPressChange = onLeftPressChange,
                tapZoneScale = 0.90f,
                tapMoveThresholdPx = if (compact) 30f else 34f,
                centerTapRequired = false,
                padSize = joystickSize,
                knobSize = knobSize,
                labelFontSize = labelFontSize,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = centerColumnBottom),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CenteredOverlayControls(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    canZoomIn = canZoomIn,
                    canZoomOut = canZoomOut,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    controlStateLabel = controlStateLabel,
                    compact = compact,
                    controlRowSpacing = controlRowSpacing,
                    zoomButtonSize = zoomButtonSize,
                    zoomIconSize = zoomIconSize,
                    historyButtonSize = historyButtonSize,
                    historyIconSize = historyIconSize,
                    belowHistoryContent = belowHistoryContent
                )
            }
            JoystickPad(
                insideLabel = "Cursor / Select",
                vector = rightVector,
                onVectorChange = onRightVectorChange,
                onTap = onRightTap,
                onPressChange = onRightPressChange,
                tapZoneScale = 0.92f,
                tapMoveThresholdPx = if (compact) 34f else 38f,
                centerTapRequired = false,
                padSize = joystickSize,
                knobSize = knobSize,
                labelFontSize = labelFontSize,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun CenteredOverlayControls(
    canUndo: Boolean,
    canRedo: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    controlStateLabel: String?,
    compact: Boolean,
    controlRowSpacing: Dp,
    zoomButtonSize: Dp,
    zoomIconSize: Dp,
    historyButtonSize: Dp,
    historyIconSize: Dp,
    belowHistoryContent: (@Composable () -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        if (controlStateLabel != null) {
            ControlStateHud(
                stateLabel = controlStateLabel
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(controlRowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlimIconAction(
                icon = Icons.Filled.Add,
                contentDescription = "Zoom in",
                enabled = canZoomIn,
                onClick = onZoomIn,
                buttonSize = zoomButtonSize,
                iconSize = zoomIconSize
            )
            SlimIconAction(
                icon = Icons.Filled.Remove,
                contentDescription = "Zoom out",
                enabled = canZoomOut,
                onClick = onZoomOut,
                buttonSize = zoomButtonSize,
                iconSize = zoomIconSize
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(controlRowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlimIconAction(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                enabled = canUndo,
                onClick = onUndo,
                buttonSize = historyButtonSize,
                iconSize = historyIconSize
            )
            SlimIconAction(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                enabled = canRedo,
                onClick = onRedo,
                buttonSize = historyButtonSize,
                iconSize = historyIconSize
            )
        }
        belowHistoryContent?.let { content ->
            Box(
                modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun DrawLineEdgeDialsOverlay(
    onAngleTicks: (Int) -> Unit,
    onLengthTicks: (Int) -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
    leftInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 400.dp || maxHeight < 700.dp
        val stripWidth = if (compact) 20.dp else 24.dp
        val stripHeight = if (compact) 232.dp else 300.dp
        val sideInset = if (compact) 3.dp else 5.dp
        val bodyLabelFontSize = if (compact) 7.sp else 8.sp
        val captionFontSize = if (compact) 8.sp else 9.sp
        val pxPerTick = if (compact) 11f else 12f
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = (if (compact) 112.dp else 142.dp) + bottomInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.padding(start = sideInset + leftInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EdgeTickDial(
                    label = "1deg",
                    onTicks = onAngleTicks,
                    onInteractionChanged = onInteractionChanged,
                    pxPerTick = pxPerTick,
                    labelFontSize = bodyLabelFontSize,
                    modifier = Modifier
                        .width(stripWidth)
                        .height(stripHeight)
                )
                Text(
                    text = "Angle ±1°",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = captionFontSize),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier.padding(end = sideInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EdgeTickDial(
                    label = "1in",
                    onTicks = onLengthTicks,
                    onInteractionChanged = onInteractionChanged,
                    pxPerTick = pxPerTick,
                    labelFontSize = bodyLabelFontSize,
                    modifier = Modifier
                        .width(stripWidth)
                        .height(stripHeight)
                )
                Text(
                    text = "Length ±1in",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = captionFontSize),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun EdgeTickDial(
    label: String,
    onTicks: (Int) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    pxPerTick: Float,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    var carryPx by remember { mutableFloatStateOf(0f) }
    var dialPhasePx by remember { mutableFloatStateOf(0f) }
    var lastDirection by remember { mutableIntStateOf(0) }
    var activePointerId by remember { mutableIntStateOf(MotionEvent.INVALID_POINTER_ID) }
    var lastPointerY by remember { mutableFloatStateOf(0f) }
    val animatedPhasePx by animateFloatAsState(
        targetValue = dialPhasePx,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "edge-dial-phase"
    )
    val haptics = LocalHapticFeedback.current
    fun resetDial(emitInteractionEnd: Boolean) {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        carryPx = 0f
        dialPhasePx = 0f
        lastDirection = 0
        if (emitInteractionEnd) {
            onInteractionChanged(false)
        }
    }
    DisposableEffect(onInteractionChanged) {
        onDispose {
            if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                resetDial(emitInteractionEnd = true)
            }
        }
    }
    val dialShellColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val dialBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)
    val dialTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val dialAxisColor = MaterialTheme.colorScheme.onSurface
    val dialMinorColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dialMarkerColor = MaterialTheme.colorScheme.primary
    val dialLabelColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.pointerInteropFilter { event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                        val index = event.actionIndex
                        if (index >= 0 && index < event.pointerCount) {
                            activePointerId = event.getPointerId(index)
                            lastPointerY = event.getY(index)
                            carryPx = 0f
                            dialPhasePx = 0f
                            lastDirection = 0
                            onInteractionChanged(true)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) return@pointerInteropFilter false
                    val index = event.findPointerIndex(activePointerId)
                    if (index < 0 || index >= event.pointerCount) {
                        resetDial(emitInteractionEnd = true)
                        return@pointerInteropFilter true
                    }
                    val currentY = event.getY(index)
                    val deltaY = (currentY - lastPointerY).coerceIn(-20f, 20f)
                    lastPointerY = currentY
                    if (abs(deltaY) >= 0.04f) {
                        val direction = when {
                            deltaY < 0f -> 1
                            deltaY > 0f -> -1
                            else -> 0
                        }
                        if (direction != 0 && lastDirection != 0 && direction != lastDirection) {
                            carryPx *= 0.25f
                        }
                        if (direction != 0) {
                            lastDirection = direction
                        }
                        carryPx -= deltaY
                        dialPhasePx = normalizeDialPhase(dialPhasePx - deltaY, pxPerTick)
                        val steps = extractDialTickSteps(carryPx, pxPerTick)
                        if (steps != 0) {
                            onTicks(steps)
                            carryPx -= steps * pxPerTick
                            repeat(abs(steps).coerceIn(1, 2)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) return@pointerInteropFilter false
                    val index = event.actionIndex
                    if (index < 0 || index >= event.pointerCount) {
                        resetDial(emitInteractionEnd = true)
                        return@pointerInteropFilter true
                    }
                    val pointerId = event.getPointerId(index)
                    if (pointerId == activePointerId) {
                        resetDial(emitInteractionEnd = true)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                        resetDial(emitInteractionEnd = true)
                    }
                    true
                }
                else -> activePointerId != MotionEvent.INVALID_POINTER_ID
            }
        },
        shape = RoundedCornerShape(12.dp),
        color = dialShellColor,
        border = BorderStroke(1.2.dp, dialBorderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val topInset = size.height * 0.04f
                val bottomInset = size.height * 0.04f
                val centerY = size.height / 2f
                val lineTop = topInset
                val lineBottom = size.height - bottomInset
                val tickSpacing = pxPerTick.coerceAtLeast(1f)

                drawRoundRect(
                    color = dialTrackColor,
                    topLeft = Offset(1f, lineTop),
                    size = androidx.compose.ui.geometry.Size(size.width - 2f, lineBottom - lineTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = 6f, y = 6f)
                )
                drawLine(
                    color = dialAxisColor,
                    start = Offset(centerX, lineTop),
                    end = Offset(centerX, lineBottom),
                    strokeWidth = 1.9f
                )
                var y = centerY + animatedPhasePx
                while (y >= lineTop - tickSpacing) {
                    val stepIndex = ((centerY - y) / tickSpacing).roundToInt()
                    val major = stepIndex % 5 == 0
                    val halfLen = if (major) size.width * 0.42f else size.width * 0.28f
                    drawLine(
                        color = if (major) dialAxisColor else dialMinorColor,
                        start = Offset(centerX - halfLen, y),
                        end = Offset(centerX + halfLen, y),
                        strokeWidth = if (major) 1.6f else 1.1f
                    )
                    y -= tickSpacing
                }
                y = centerY + animatedPhasePx + tickSpacing
                while (y <= lineBottom + tickSpacing) {
                    val stepIndex = ((centerY - y) / tickSpacing).roundToInt()
                    val major = stepIndex % 5 == 0
                    val halfLen = if (major) size.width * 0.42f else size.width * 0.28f
                    drawLine(
                        color = if (major) dialAxisColor else dialMinorColor,
                        start = Offset(centerX - halfLen, y),
                        end = Offset(centerX + halfLen, y),
                        strokeWidth = if (major) 1.6f else 1.1f
                    )
                    y += tickSpacing
                }
                drawLine(
                    color = dialMarkerColor,
                    start = Offset(2f, centerY),
                    end = Offset(size.width - 2f, centerY),
                    strokeWidth = 2.1f
                )
                drawCircle(
                    color = dialAxisColor,
                    radius = 2.9f,
                    center = Offset(centerX, centerY)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                color = dialLabelColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
            )
        }
    }
}

private fun extractDialTickSteps(accumulatedPx: Float, pxPerTick: Float): Int {
    val safeTickPx = pxPerTick.coerceAtLeast(1f)
    val effectivePxPerTick = safeTickPx * 1.2f
    return when {
        accumulatedPx >= effectivePxPerTick -> floor(accumulatedPx / effectivePxPerTick).toInt().coerceAtMost(4)
        accumulatedPx <= -effectivePxPerTick -> -floor((-accumulatedPx) / effectivePxPerTick).toInt().coerceAtLeast(-4)
        else -> 0
    }
}

private fun normalizeDialPhase(phasePx: Float, pxPerTick: Float): Float {
    val safeTickPx = pxPerTick.coerceAtLeast(1f)
    var normalized = phasePx % safeTickPx
    if (normalized > safeTickPx / 2f) normalized -= safeTickPx
    if (normalized < -safeTickPx / 2f) normalized += safeTickPx
    return normalized
}

internal enum class TouchToolMode {
    SELECT,
    DRAW,
    GRAB
}

@Composable
internal fun TouchModeQuickToolsOverlay(
    selectedMode: TouchToolMode,
    onSelectMode: () -> Unit,
    onDrawMode: () -> Unit,
    onGrabMode: () -> Unit,
    onCancel: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    controlStateLabel: String?,
    belowHistoryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 420.dp
        val ultraCompact = maxWidth < 360.dp
        val sidePadding = if (compact) 4.dp else 8.dp
        val toolButtonSize = when {
            ultraCompact -> 34.dp
            compact -> 38.dp
            else -> 42.dp
        }
        val toolIconSize = when {
            ultraCompact -> 14.dp
            compact -> 16.dp
            else -> 18.dp
        }
        val labelFontSize = if (compact) 7.sp else 8.sp
        val verticalSpacing = if (compact) 2.dp else 3.dp
        val controlRowSpacing = if (compact) 5.dp else 6.dp
        val zoomButtonSize = if (compact) 32.dp else 36.dp
        val zoomIconSize = if (compact) 14.dp else 16.dp
        val historyButtonSize = if (compact) 30.dp else 34.dp
        val historyIconSize = if (compact) 13.dp else 15.dp
        val centerColumnBottom = if (compact) 14.dp else 24.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePadding)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TouchToolButton(
                    icon = Icons.Filled.AdsClick,
                    label = "Select",
                    selected = selectedMode == TouchToolMode.SELECT,
                    onClick = onSelectMode,
                    buttonSize = toolButtonSize,
                    iconSize = toolIconSize,
                    labelFontSize = labelFontSize,
                    verticalSpacing = verticalSpacing
                )
                TouchToolButton(
                    icon = Icons.Filled.BorderColor,
                    label = "Draw",
                    selected = selectedMode == TouchToolMode.DRAW,
                    onClick = onDrawMode,
                    buttonSize = toolButtonSize,
                    iconSize = toolIconSize,
                    labelFontSize = labelFontSize,
                    verticalSpacing = verticalSpacing
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = centerColumnBottom),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CenteredOverlayControls(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    canZoomIn = canZoomIn,
                    canZoomOut = canZoomOut,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    controlStateLabel = controlStateLabel,
                    compact = compact,
                    controlRowSpacing = controlRowSpacing,
                    zoomButtonSize = zoomButtonSize,
                    zoomIconSize = zoomIconSize,
                    historyButtonSize = historyButtonSize,
                    historyIconSize = historyIconSize,
                    belowHistoryContent = belowHistoryContent
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TouchToolButton(
                    icon = Icons.Filled.Workspaces,
                    label = "Grab",
                    selected = selectedMode == TouchToolMode.GRAB,
                    onClick = onGrabMode,
                    buttonSize = toolButtonSize,
                    iconSize = toolIconSize,
                    labelFontSize = labelFontSize,
                    verticalSpacing = verticalSpacing
                )
                TouchToolButton(
                    icon = Icons.Filled.Close,
                    label = "Cancel",
                    selected = false,
                    onClick = onCancel,
                    buttonSize = toolButtonSize,
                    iconSize = toolIconSize,
                    labelFontSize = labelFontSize,
                    verticalSpacing = verticalSpacing
                )
            }
        }
    }
}

@Composable
private fun TouchToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 46.dp,
    iconSize: Dp = 19.dp,
    labelFontSize: TextUnit = 9.sp,
    verticalSpacing: Dp = 4.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            },
            border = BorderStroke(
                width = if (selected) 1.3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
                }
            ),
            modifier = Modifier
                .size(buttonSize)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun JoystickPad(
    insideLabel: String,
    vector: Offset,
    onVectorChange: (Offset) -> Unit,
    onTap: (() -> Unit)?,
    onPressChange: ((Boolean) -> Unit)? = null,
    tapZoneScale: Float = 0.44f,
    tapMoveThresholdPx: Float = 10f,
    centerTapRequired: Boolean = true,
    padSize: Dp = 126.dp,
    knobSize: Dp = 50.dp,
    labelFontSize: TextUnit = 8.sp,
    modifier: Modifier = Modifier
) {
    val maxRadiusPx = with(LocalDensity.current) { (padSize * 0.317f).toPx() }
    val padSizePx = with(LocalDensity.current) { padSize.toPx() }
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
    val joystickAxisGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val joystickAxisLine = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val joystickLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(padSize)
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
                    val axisInset = size.minDimension * 0.08f
                    drawCircle(
                        color = joystickAxisGlow,
                        radius = size.minDimension * 0.34f,
                        center = c
                    )
                    drawLine(
                        color = joystickAxisLine,
                        start = Offset(c.x, axisInset),
                        end = Offset(c.x, size.height - axisInset),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = joystickAxisLine,
                        start = Offset(axisInset, c.y),
                        end = Offset(size.width - axisInset, c.y),
                        strokeWidth = 1f
                    )
                }
                Text(
                    text = insideLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                    color = joystickLabelColor,
                    modifier = Modifier.align(Alignment.Center)
                )
                Surface(
                    modifier = Modifier
                        .size(knobSize)
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
internal fun applyCursorGridAssist(
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
    val stepMm = Millimeters.fromFeet(snapSettings.gridStepFeet.coerceAtLeast(MIN_GRID_STEP_FEET))
        .value
        .coerceAtLeast(MIN_GRID_STEP_MM)
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

internal fun applyJoystickDeadzone(
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

internal fun performSyntheticTap(rootView: android.view.View, pointInRoot: Offset): Boolean {
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


