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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import kotlin.math.roundToInt

internal enum class BlueprintControlTutorialTarget {
    BOTTOM_RAIL,
    CANVAS,
    TOUCH_LEFT_TOOLS,
    TOUCH_CENTER_CONTROLS,
    TOUCH_RIGHT_TOOLS,
    JOYSTICK_LEFT_PAD,
    JOYSTICK_CENTER_CONTROLS,
    JOYSTICK_RIGHT_PAD,
    EDGE_DIALS,
    OPENING_PANEL,
    PARAMS_PANEL,
    RAIL_HELP_PANEL,
    TOP_START_STACK,
    TOP_END_STACK,
    FLOOR_SWITCHER,
    GRID_SCALE_BADGE,
    CLEAR_ALL_BUTTON
}

internal data class BlueprintControlTutorialStep(
    val target: BlueprintControlTutorialTarget,
    val title: String,
    val message: String,
    val details: List<String> = emptyList(),
    val tryIt: String? = null,
    val demoTool: BlueprintDraftTool? = null,
    val demoOpeningPanel: OpeningPanelType? = null,
    val showParamsPanel: Boolean = false,
    val showHelpPanel: Boolean = false,
    val forceEdgeDials: Boolean = false,
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
                message = "This bottom rail is the fastest way to switch how you build and what you place.",
                details = listOf(
                    "Detached breaks the chain so the next wall starts fresh.",
                    "Box creates a rectangle from two corners.",
                    "Curve goes start, end, then shape the arc.",
                    "Circle goes center, then edge."
                ),
                tryIt = "Tap a rail icon and watch the highlight move with your current tool."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CANVAS,
                title = "Canvas Flow",
                message = "In dual-stick mode the cursor is your pointer, so placement is all about moving, confirming, and refining.",
                details = listOf(
                    "Move the cursor with the right pad, then confirm at the cursor.",
                    "Walls, boxes, curves, and circles all follow a start-to-finish placement flow.",
                    "Pan and zoom first, then place when the cursor is where you want it."
                ),
                tryIt = "Move the cursor into open space, then tap to start a wall and tap again to finish it."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
                title = "Left Pad",
                message = "The left pad moves the whole blueprint under the cursor for coarse positioning.",
                details = listOf(
                    "Use broad left-pad sweeps to pan across the job quickly.",
                    "Ease off pressure for smaller corrections near a snap point."
                ),
                tryIt = "Push the left pad in one direction and watch the entire drawing pan under the cursor.",
                demoLeftVector = Offset(-0.62f, 0.26f)
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
                title = "Right Pad",
                message = "The right pad is your aiming stick and action side.",
                details = listOf(
                    "Move the cursor precisely with the right pad.",
                    "Tap or press on the right side to place, confirm, or select at the cursor."
                ),
                tryIt = "Nudge the right pad a little, then tap on that side to place a point at the cursor.",
                demoRightVector = Offset(0.56f, -0.54f)
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
                title = "Zoom and History",
                message = "The center cluster keeps zoom and recovery within easy reach of both thumbs.",
                details = listOf(
                    "Zoom in before fine placements and zoom out to regain context.",
                    "Undo and Redo are the fastest way to clean up a draft without changing tools.",
                    "Coordinates below these buttons help you confirm where the cursor really is."
                ),
                tryIt = "Zoom in once, place something small, then use Undo and Redo to feel the recovery flow."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.EDGE_DIALS,
                title = "Precision Dials",
                message = "The side dials are for small, repeatable nudges when finger movement is too coarse.",
                details = listOf(
                    "Left dial adjusts angle. Right dial adjusts length.",
                    "Use them after you start a wall or while sizing a box.",
                    "Curve mode switches them to Shift and Bend after the end point is set."
                ),
                tryIt = "Drag the left dial for angle nudges and the right dial for length nudges while a draft is active.",
                demoTool = BlueprintDraftTool.DRAW_WALL,
                forceEdgeDials = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.OPENING_PANEL,
                title = "Doors, Windows, and Stairs",
                message = "Openings come from the rail, then you place them directly onto the wall you want.",
                details = listOf(
                    "Use the door, window, stair up, and stair down buttons on the rail.",
                    "Choose a preset or type custom width, height, and sill values.",
                    "Place the opening after the panel is set the way you want."
                ),
                tryIt = "Open a door preset here, then place it on an existing wall segment.",
                demoOpeningPanel = OpeningPanelType.DOORS
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.PARAMS_PANEL,
                title = "Params and Snaps",
                message = "This panel controls takeoff formulas and the drafting behavior behind your measurements.",
                details = listOf(
                    "Trade-specific settings live here for drywall, concrete, gravel, and paint.",
                    "Snap threshold and drafting tune-ups live here too.",
                    "Use Params when the shape is right but the quantity math needs adjustment."
                ),
                tryIt = "Change one snap or trade value here, then go back to the canvas and feel the difference.",
                showParamsPanel = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_START_STACK,
                title = "Live Totals",
                message = "The top-left stack is your running measurement readout while you work.",
                details = listOf(
                    "Live quantities update as you draw.",
                    "When you select a wall or opening, its edit card appears here too."
                ),
                tryIt = "Draw or select something and watch this stack react immediately."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_END_STACK,
                title = "Project and Trade",
                message = "The top-right stack keeps the current project context close at hand.",
                details = listOf(
                    "The project name stays visible here.",
                    "Trade changes which live totals and params you see.",
                    "Use the scope selector when you want wall, room, or ceiling context."
                ),
                tryIt = "Switch the trade or scope once and notice how the rest of the blueprint HUD updates."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.RAIL_HELP_PANEL,
                title = "Help Cheat Sheet",
                message = "The help button opens a quick reference for the rail and your current control mode.",
                details = listOf(
                    "Use it when you forget what an icon does.",
                    "It is especially handy when switching between touch and joystick control."
                ),
                tryIt = "Open Help, skim the legend, then close it and keep building without leaving the screen.",
                showHelpPanel = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Quick Reset",
                message = "Clear All is the nuclear option for the current blueprint layout.",
                details = listOf(
                    "Use Undo when you only need to back up a few moves.",
                    "Use Clear All when the whole sketch needs a restart."
                ),
                tryIt = "Use Undo first for small mistakes and save Clear All for full restarts."
            )
        )
    } else {
        listOf(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.BOTTOM_RAIL,
                title = "Build Rail",
                message = "This bottom rail is where you switch build modes, add openings, and open supporting tools.",
                details = listOf(
                    "Detached breaks the chain so the next wall starts fresh.",
                    "Box creates a rectangle from two corners.",
                    "Curve goes start, end, then shape the arc.",
                    "Circle goes center, then edge."
                ),
                tryIt = "Tap a rail icon and watch the active build mode change before you touch the canvas."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CANVAS,
                title = "Canvas Flow",
                message = "Most drafting happens right on the canvas, so the tap order matters.",
                details = listOf(
                    "Wall: tap start, then tap end. Keep chaining if Detached is off.",
                    "Box: tap the first corner, then the opposite corner.",
                    "Curve: tap start, tap end, then shape the arc before you confirm.",
                    "Circle: tap center, then set the radius from the edge."
                ),
                tryIt = "Tap two points on the canvas to place a simple wall, then try Box or Curve from the rail."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_LEFT_TOOLS,
                title = "Select and Draw",
                message = "These left tools control whether you are inspecting geometry or creating it.",
                details = listOf(
                    "Select lets you tap a wall or opening and inspect or edit it.",
                    "Draw returns you to normal wall and shape creation."
                ),
                tryIt = "Switch to Select, tap existing geometry, then switch back to Draw when you are ready to create more."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_RIGHT_TOOLS,
                title = "Grab and Cancel",
                message = "These right tools handle recovery and repositioning without leaving the blueprint.",
                details = listOf(
                    "Grab picks up a wall so you can place it somewhere else.",
                    "Cancel exits the current draft, pickup, or selection state."
                ),
                tryIt = "Use Cancel once during a draft so you know how to bail out fast when you need to."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
                title = "Zoom and History",
                message = "The center controls keep scale, history, and live coordinates in one place.",
                details = listOf(
                    "Zoom in for tight placements and zoom out for context.",
                    "Undo and Redo fix layout mistakes fast.",
                    "Coordinates under these buttons show the live pointer position."
                ),
                tryIt = "Zoom in near a corner, place something small, then Undo and Redo it."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.EDGE_DIALS,
                title = "Precision Dials",
                message = "The dials give you repeatable adjustments when a drag would be too sloppy.",
                details = listOf(
                    "Left dial adjusts angle. Right dial adjusts length.",
                    "Use them after you start a wall or while sizing a box.",
                    "Curve mode switches them to Shift and Bend after the end point is set."
                ),
                tryIt = "Start a wall, then drag the dials instead of dragging on the canvas for the fine adjustment.",
                demoTool = BlueprintDraftTool.DRAW_WALL,
                forceEdgeDials = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.OPENING_PANEL,
                title = "Doors, Windows, and Stairs",
                message = "Use the add-on panels when you need more than raw wall geometry.",
                details = listOf(
                    "Open the door, window, stair up, or stair down panel from the rail.",
                    "Pick a preset or enter custom width, height, and sill values.",
                    "Place the opening on the wall after the panel is configured."
                ),
                tryIt = "Open Doors here, choose a preset, then tap a wall to place it.",
                demoOpeningPanel = OpeningPanelType.DOORS
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.PARAMS_PANEL,
                title = "Params and Snaps",
                message = "This panel holds the trade formulas and snap behavior that shape your takeoff.",
                details = listOf(
                    "Trade-specific settings live here for drywall, concrete, gravel, and paint.",
                    "Snap threshold and drafting tune-ups live here too.",
                    "Use Params when the geometry is right but the quantity math needs tuning."
                ),
                tryIt = "Adjust one trade or snap setting here, then return to drawing and see what changed.",
                showParamsPanel = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_START_STACK,
                title = "Live Totals",
                message = "The top-left stack is your live measurement dashboard.",
                details = listOf(
                    "It updates while you draw so you can watch quantities change in real time.",
                    "Selection details show here too when you tap a wall or opening."
                ),
                tryIt = "Draw a wall or select one and watch the live totals react right away."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_END_STACK,
                title = "Project and Trade",
                message = "The top-right stack keeps project context and takeoff filters visible.",
                details = listOf(
                    "The project name stays up top while you work.",
                    "Trade changes which live totals and params you are editing.",
                    "The scope selector lets you switch wall, room, and ceiling context."
                ),
                tryIt = "Change the trade or scope once so you can feel how the takeoff context shifts."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                title = "Floor Switcher",
                message = "Use this control to move between floors without leaving blueprint mode.",
                details = listOf(
                    "Jump back to Ground when you want a clean reference point.",
                    "Switch floors before drawing if the next geometry belongs upstairs or downstairs."
                ),
                tryIt = "Flip to another floor and back so the control is familiar before you actually need it."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
                title = "Grid Scale",
                message = "Grid scale changes how big each reference block is and how the snap grid feels.",
                details = listOf(
                    "Tap here to tighten or loosen the grid.",
                    "Smaller grid steps help with precise layouts. Larger ones speed up rough blocking."
                ),
                tryIt = "Change the grid once and compare how snapping feels on the canvas."
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.RAIL_HELP_PANEL,
                title = "Help Cheat Sheet",
                message = "The help button opens a quick reminder for the rail and your current control setup.",
                details = listOf(
                    "Use it when you forget what an icon does.",
                    "It is the fastest in-app refresher once you are already mid-project."
                ),
                tryIt = "Open Help, glance through the legend, then close it and keep working.",
                showHelpPanel = true
            ),
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Quick Reset",
                message = "Clear All removes the current blueprint geometry when a simple undo is not enough.",
                details = listOf(
                    "Use Undo when you only need to back up a few moves.",
                    "Use Clear All when the whole sketch needs a restart."
                ),
                tryIt = "Remember this button is for full restarts, not everyday corrections."
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
        val cardHeightEstimate = 208.dp + (step.details.size * 18).dp + if (step.tryIt != null) 62.dp else 0.dp
        val maxCardHeight = (maxHeight - 24.dp).coerceAtLeast(180.dp)
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
                .widthIn(max = cardWidth)
                .heightIn(max = maxCardHeight),
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
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
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
                    if (step.details.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            step.details.forEach { detail ->
                                Text(
                                    text = "- $detail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    step.tryIt?.let { prompt ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Try This Now",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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
