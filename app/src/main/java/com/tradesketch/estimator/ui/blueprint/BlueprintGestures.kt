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
private fun ControlClusterShell(
    compact: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = if (compact) 10.dp else 12.dp,
    verticalPadding: Dp = if (compact) 10.dp else 12.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 24.dp else 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.15.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 10.dp else 14.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

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
    leftPadModifier: Modifier = Modifier,
    centerControlsModifier: Modifier = Modifier,
    rightPadModifier: Modifier = Modifier,
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
        val joystickBottomLift = if (compact) 10.dp else 14.dp
        val centerColumnBottom = if (compact) 20.dp else 30.dp
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
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = joystickBottomLift)
                    .then(leftPadModifier)
            )
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
                belowHistoryContent = belowHistoryContent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = centerColumnBottom)
                    .then(centerControlsModifier)
            )
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = joystickBottomLift)
                    .then(rightPadModifier)
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
    belowHistoryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
    dualJoysticksEnabled: Boolean,
    controlsBottomPadding: Dp,
    leftInset: Dp = 0.dp,
    angleCaption: String = "Angle",
    angleLabel: String = "±1°",
    lengthCaption: String = "Length",
    lengthLabel: String = "±1in",
    angleDialModifier: Modifier = Modifier,
    lengthDialModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 420.dp || maxHeight < 700.dp
        val ultraCompact = maxWidth < 360.dp
        val sidePadding = if (compact) 4.dp else 8.dp
        val joystickSize = when {
            ultraCompact -> 94.dp
            compact -> 104.dp
            else -> 126.dp
        }
        val dialWidth = when {
            ultraCompact -> 94.dp
            compact -> 104.dp
            else -> 122.dp
        }
        val dialVisualHeight = when {
            ultraCompact -> 30.dp
            compact -> 31.dp
            else -> 32.dp
        }
        val dialTouchHeight = when {
            ultraCompact -> 46.dp
            compact -> 48.dp
            else -> 50.dp
        }
        val dialTrackHeight = when {
            ultraCompact -> 9.dp
            compact -> 10.dp
            else -> 11.dp
        }
        val labelFontSize = if (compact) 7.5.sp else 8.5.sp
        val captionFontSize = if (compact) 7.5.sp else 8.5.sp
        val pxPerTick = if (compact) 8.5f else 9.5f
        val centerPull = when {
            ultraCompact -> 6.dp
            compact -> 8.dp
            else -> 10.dp
        }
        val dialHorizontalInset = sidePadding + ((joystickSize - dialWidth) / 2f) + leftInset + centerPull
        val rightDialHorizontalInset = sidePadding + ((joystickSize - dialWidth) / 2f) + centerPull
        val dialBottomPadding = controlsBottomPadding + when {
            dualJoysticksEnabled -> joystickSize + if (compact) 18.dp else 22.dp
            compact -> 102.dp
            else -> 110.dp
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = dialBottomPadding)
        ) {
            EdgeTickDial(
                caption = angleCaption,
                label = angleLabel,
                onTicks = onAngleTicks,
                onInteractionChanged = onInteractionChanged,
                pxPerTick = pxPerTick,
                visualHeight = dialVisualHeight,
                trackHeight = dialTrackHeight,
                labelFontSize = labelFontSize,
                captionFontSize = captionFontSize,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = dialHorizontalInset)
                    .width(dialWidth)
                    .height(dialTouchHeight)
                    .then(angleDialModifier)
            )
            EdgeTickDial(
                caption = lengthCaption,
                label = lengthLabel,
                onTicks = onLengthTicks,
                onInteractionChanged = onInteractionChanged,
                pxPerTick = pxPerTick,
                visualHeight = dialVisualHeight,
                trackHeight = dialTrackHeight,
                labelFontSize = labelFontSize,
                captionFontSize = captionFontSize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = rightDialHorizontalInset)
                    .width(dialWidth)
                    .height(dialTouchHeight)
                    .then(lengthDialModifier)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun EdgeTickDial(
    caption: String,
    label: String,
    onTicks: (Int) -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    pxPerTick: Float,
    visualHeight: Dp,
    trackHeight: Dp,
    labelFontSize: TextUnit,
    captionFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    var carryPx by remember { mutableFloatStateOf(0f) }
    var dialPhasePx by remember { mutableFloatStateOf(0f) }
    var lastDirection by remember { mutableIntStateOf(0) }
    var activePointerId by remember { mutableIntStateOf(MotionEvent.INVALID_POINTER_ID) }
    var lastPointerX by remember { mutableFloatStateOf(0f) }
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
    val dialShellColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    val dialBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val dialTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val dialAxisColor = MaterialTheme.colorScheme.onSurface
    val dialMinorColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dialMarkerColor = MaterialTheme.colorScheme.primary
    val dialLabelColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .sizeIn(minHeight = 44.dp)
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                            val index = event.actionIndex
                            if (index >= 0 && index < event.pointerCount) {
                                activePointerId = event.getPointerId(index)
                                lastPointerX = event.getX(index)
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
                        val currentX = event.getX(index)
                        val deltaX = (currentX - lastPointerX).coerceIn(-28f, 28f)
                        lastPointerX = currentX
                        if (abs(deltaX) >= 0.04f) {
                            val direction = when {
                                deltaX > 0f -> 1
                                deltaX < 0f -> -1
                                else -> 0
                            }
                            if (direction != 0 && lastDirection != 0 && direction != lastDirection) {
                                carryPx *= 0.25f
                            }
                            if (direction != 0) {
                                lastDirection = direction
                            }
                            carryPx += deltaX
                            dialPhasePx = normalizeDialPhase(dialPhasePx + deltaX, pxPerTick)
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
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualHeight),
            shape = RoundedCornerShape(16.dp),
            color = dialShellColor,
            border = BorderStroke(1.dp, dialBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = captionFontSize),
                        color = dialLabelColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                        color = dialMinorColor
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val leftInset = size.width * 0.035f
                    val rightInset = size.width * 0.035f
                    val lineLeft = leftInset
                    val lineRight = size.width - rightInset
                    val tickSpacing = pxPerTick.coerceAtLeast(1f)

                    drawRoundRect(
                        color = dialTrackColor,
                        topLeft = Offset(lineLeft, 1f),
                        size = androidx.compose.ui.geometry.Size(lineRight - lineLeft, size.height - 2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = 6f, y = 6f)
                    )
                    drawLine(
                        color = dialAxisColor,
                        start = Offset(lineLeft, centerY),
                        end = Offset(lineRight, centerY),
                        strokeWidth = 1.9f
                    )
                    var x = centerX + animatedPhasePx
                    while (x >= lineLeft - tickSpacing) {
                        val stepIndex = ((centerX - x) / tickSpacing).roundToInt()
                        val major = stepIndex % 5 == 0
                        val halfLen = if (major) size.height * 0.42f else size.height * 0.28f
                        drawLine(
                            color = if (major) dialAxisColor else dialMinorColor,
                            start = Offset(x, centerY - halfLen),
                            end = Offset(x, centerY + halfLen),
                            strokeWidth = if (major) 1.6f else 1.1f
                        )
                        x -= tickSpacing
                    }
                    x = centerX + animatedPhasePx + tickSpacing
                    while (x <= lineRight + tickSpacing) {
                        val stepIndex = ((centerX - x) / tickSpacing).roundToInt()
                        val major = stepIndex % 5 == 0
                        val halfLen = if (major) size.height * 0.42f else size.height * 0.28f
                        drawLine(
                            color = if (major) dialAxisColor else dialMinorColor,
                            start = Offset(x, centerY - halfLen),
                            end = Offset(x, centerY + halfLen),
                            strokeWidth = if (major) 1.6f else 1.1f
                        )
                        x += tickSpacing
                    }
                    drawLine(
                        color = dialMarkerColor,
                        start = Offset(centerX, 2f),
                        end = Offset(centerX, size.height - 2f),
                        strokeWidth = 2.1f
                    )
                    drawCircle(
                        color = dialAxisColor,
                        radius = 2.9f,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

private fun extractDialTickSteps(accumulatedPx: Float, pxPerTick: Float): Int {
    val safeTickPx = pxPerTick.coerceAtLeast(1f)
    val effectivePxPerTick = safeTickPx * 1.08f
    return when {
        accumulatedPx >= effectivePxPerTick -> floor(accumulatedPx / effectivePxPerTick).toInt().coerceAtMost(5)
        accumulatedPx <= -effectivePxPerTick -> -floor((-accumulatedPx) / effectivePxPerTick).toInt().coerceAtLeast(-5)
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
    leftToolsModifier: Modifier = Modifier,
    centerControlsModifier: Modifier = Modifier,
    rightToolsModifier: Modifier = Modifier,
    selectToolModifier: Modifier = Modifier,
    drawToolModifier: Modifier = Modifier,
    grabToolModifier: Modifier = Modifier,
    cancelToolModifier: Modifier = Modifier,
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
            ControlClusterShell(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
                    .then(leftToolsModifier),
                compact = compact,
                horizontalPadding = if (compact) 8.dp else 10.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
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
                        verticalSpacing = verticalSpacing,
                        modifier = selectToolModifier
                    )
                    TouchToolButton(
                        icon = Icons.Filled.BorderColor,
                        label = "Draw",
                        selected = selectedMode == TouchToolMode.DRAW,
                        onClick = onDrawMode,
                        buttonSize = toolButtonSize,
                        iconSize = toolIconSize,
                        labelFontSize = labelFontSize,
                        verticalSpacing = verticalSpacing,
                        modifier = drawToolModifier
                    )
                }
            }
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
                belowHistoryContent = belowHistoryContent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = centerColumnBottom)
                    .then(centerControlsModifier)
            )
            ControlClusterShell(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp)
                    .then(rightToolsModifier),
                compact = compact,
                horizontalPadding = if (compact) 8.dp else 10.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
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
                        verticalSpacing = verticalSpacing,
                        modifier = grabToolModifier
                    )
                    TouchToolButton(
                        icon = Icons.Filled.Close,
                        label = "Cancel",
                        selected = false,
                        onClick = onCancel,
                        buttonSize = toolButtonSize,
                        iconSize = toolIconSize,
                        labelFontSize = labelFontSize,
                        verticalSpacing = verticalSpacing,
                        modifier = cancelToolModifier
                    )
                }
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
    verticalSpacing: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.98f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
            },
            border = BorderStroke(
                width = if (selected) 1.3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
                }
            ),
            shadowElevation = if (selected) 8.dp else 4.dp,
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
                        MaterialTheme.colorScheme.onPrimaryContainer
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
    val joystickAxisGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val joystickAxisLine = MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
    val joystickLabelColor = MaterialTheme.colorScheme.onSurface
    val joystickOuterRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val joystickTapGuideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    Surface(
        modifier = modifier
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.82f)),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val axisInset = size.minDimension * 0.08f
                drawCircle(
                    color = joystickOuterRingColor,
                    radius = size.minDimension * 0.46f,
                    center = c,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = joystickAxisGlow,
                    radius = size.minDimension * 0.34f,
                    center = c
                )
                drawCircle(
                    color = joystickTapGuideColor,
                    radius = size.minDimension * tapZoneScale.coerceIn(0.24f, 0.88f) * 0.5f,
                    center = c,
                    style = Stroke(width = 1.5f)
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
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.98f),
                border = BorderStroke(1.15.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)),
                shadowElevation = 10.dp
            ) {}
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.74f)),
                shadowElevation = 0.dp,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = insideLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                    color = joystickLabelColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
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


