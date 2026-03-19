package com.tradesketch.estimator.ui.blueprint

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal enum class BlueprintControlTutorialTarget {
    BOTTOM_RAIL,
    TOUCH_LEFT_TOOLS,
    TOUCH_CENTER_CONTROLS,
    TOUCH_RIGHT_TOOLS,
    JOYSTICK_LEFT_PAD,
    JOYSTICK_CENTER_CONTROLS,
    JOYSTICK_RIGHT_PAD,
    FLOOR_SWITCHER,
    GRID_SCALE_BADGE,
    CLEAR_ALL_BUTTON
}

internal data class BlueprintControlTutorialStep(
    val target: BlueprintControlTutorialTarget,
    val title: String,
    val message: String,
    val demoLeftVector: Offset = Offset.Zero,
    val demoRightVector: Offset = Offset.Zero
)

internal fun blueprintControlTutorialSteps(
    dualJoysticksEnabled: Boolean
): List<BlueprintControlTutorialStep> {
    return if (dualJoysticksEnabled) {
        listOf(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.BOTTOM_RAIL,
                title = "Build Rail",
                message = "Use this rail to open box mode, doors, windows, stairs, params, scope, and help."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
                title = "Left Pad",
                message = "The left pad pans the blueprint. A quick tap or press on this side handles alternate actions.",
                demoLeftVector = Offset(-0.62f, 0.26f)
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
                title = "Right Pad",
                message = "The right pad moves the cursor. Tap here to place, confirm, or select at the cursor.",
                demoRightVector = Offset(0.56f, -0.54f)
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
                title = "Zoom and History",
                message = "Zoom stays in the middle with Undo and Redo so both thumbs can recover or tighten placement."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Quick Reset",
                message = "Clear All wipes the current geometry. Use it carefully, and lean on Undo when you only need to back up one move."
            )
        )
    } else {
        listOf(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.BOTTOM_RAIL,
                title = "Build Rail",
                message = "Use this rail to open box mode, doors, windows, stairs, params, scope, and help."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_LEFT_TOOLS,
                title = "Select and Draw",
                message = "Select inspects existing walls and openings. Draw is the normal wall-layout mode."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_RIGHT_TOOLS,
                title = "Grab and Cancel",
                message = "Grab picks up a wall so you can move it. Cancel exits the current action or clears the active selection."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
                title = "Zoom and History",
                message = "Zoom tightens placement. Undo and Redo fix mistakes without changing tools."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                title = "Floor Switcher",
                message = "Move up or down floors here, or jump back to Ground before you keep drawing."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
                title = "Grid Scale",
                message = "Tap here to change the reference block size when you want a different grid spacing."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Quick Reset",
                message = "Clear All wipes the current geometry. Use it carefully, and lean on Undo when you only need to back up one move."
            )
        )
    }
}

@Composable
internal fun animatedTutorialJoystickVector(baseVector: Offset): Offset {
    if (baseVector == Offset.Zero) return Offset.Zero
    val transition = rememberInfiniteTransition(label = "blueprint_tutorial_joystick_demo")
    val amplitude by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blueprint_tutorial_joystick_amplitude"
    )
    return Offset(
        x = baseVector.x * amplitude,
        y = baseVector.y * amplitude
    )
}

@Composable
internal fun BlueprintControlTutorialOverlay(
    modeLabel: String,
    step: BlueprintControlTutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    targetBounds: Rect?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val transition = rememberInfiniteTransition(label = "blueprint_tutorial_highlight")
        val highlightAlpha by transition.animateFloat(
            initialValue = 0.42f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blueprint_tutorial_highlight_alpha"
        )
        val density = LocalDensity.current
        val cardWidth = maxWidth.minus(24.dp).coerceAtMost(320.dp)
        val cardHeightEstimate = 208.dp
        val targetRect = remember(targetBounds) {
            targetBounds?.inflate(12f)
        }
        val tooltipOffset = remember(targetRect, maxWidth, maxHeight, cardWidth, cardHeightEstimate, density) {
            with(density) {
                val horizontalPadding = 12.dp
                val verticalPadding = 12.dp
                val horizontalPaddingPx = horizontalPadding.roundToPx()
                val verticalPaddingPx = verticalPadding.roundToPx()
                if (targetRect == null) {
                    IntOffset(
                        x = horizontalPaddingPx,
                        y = verticalPaddingPx
                    )
                } else {
                    val maxCardX = (maxWidth - cardWidth - horizontalPadding).roundToPx()
                    val targetCenterX = targetRect.center.x.roundToInt()
                    val desiredX = targetCenterX - (cardWidth.roundToPx() / 2)
                    val clampedX = desiredX.coerceIn(
                        horizontalPaddingPx,
                        maxCardX.coerceAtLeast(horizontalPaddingPx)
                    )
                    val aboveY = targetRect.top.roundToInt() - cardHeightEstimate.roundToPx() - 14.dp.roundToPx()
                    val belowY = targetRect.bottom.roundToInt() + 14.dp.roundToPx()
                    val maxCardY = (maxHeight - cardHeightEstimate - verticalPadding).roundToPx()
                    val resolvedY = if (aboveY >= verticalPaddingPx) {
                        aboveY
                    } else {
                        belowY.coerceIn(
                            verticalPaddingPx,
                            maxCardY.coerceAtLeast(verticalPaddingPx)
                        )
                    }
                    IntOffset(x = clampedX, y = resolvedY)
                }
            }
        }
        val highlightBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.62f))
            targetRect?.let { rect ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = highlightBorderColor.copy(alpha = 0.18f),
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 12.dp.toPx())
                )
                drawRoundRect(
                    color = highlightBorderColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(12.dp)
                .offset { tooltipOffset }
                .widthIn(max = cardWidth),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Step ${stepIndex + 1} of $totalSteps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = modeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (stepIndex + 1f) / totalSteps.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = step.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onBack,
                            enabled = stepIndex > 0
                        ) {
                            Text("Back")
                        }
                        Button(onClick = onNext) {
                            Text(if (stepIndex == totalSteps - 1) "Finish" else "Next")
                        }
                    }
                }
            }
        }
    }
}

private fun Rect.inflate(padding: Float): Rect {
    return Rect(
        left = left - padding,
        top = top - padding,
        right = right + padding,
        bottom = bottom + padding
    )
}
