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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import kotlin.math.roundToInt

internal enum class BlueprintControlTutorialTarget {
    BOTTOM_RAIL,
    CANVAS,
    DETACHED_BUTTON,
    BOX_BUTTON,
    MEASURED_ARC_BUTTON,
    SKETCH_CURVE_BUTTON,
    CIRCLE_BUTTON,
    DOORS_BUTTON,
    WINDOWS_BUTTON,
    STAIR_UP_BUTTON,
    STAIR_DOWN_BUTTON,
    PARAMS_BUTTON,
    HELP_BUTTON,
    TOUCH_SELECT_BUTTON,
    TOUCH_DRAW_BUTTON,
    TOUCH_GRAB_BUTTON,
    TOUCH_CANCEL_BUTTON,
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

internal data class BlueprintControlTutorialDemoWall(
    val start: PointMm,
    val end: PointMm
)

internal data class BlueprintControlTutorialStep(
    val target: BlueprintControlTutorialTarget,
    val title: String,
    val message: String,
    val details: List<String> = emptyList(),
    val seeIt: String? = null,
    val demoTool: BlueprintDraftTool? = null,
    val demoOpeningPanel: OpeningPanelType? = null,
    val showParamsPanel: Boolean = false,
    val showHelpPanel: Boolean = false,
    val showGridScaleEditor: Boolean = false,
    val forceEdgeDials: Boolean = false,
    val demoPlacementWalls: List<BlueprintControlTutorialDemoWall> = emptyList(),
    val demoDrawingStart: PointMm? = null,
    val demoDrawingPreview: PointMm? = null,
    val demoBoxStart: PointMm? = null,
    val demoBoxPreview: PointMm? = null,
    val demoBoxRotationRadians: Double = 0.0,
    val demoCurveStart: PointMm? = null,
    val demoCurveEnd: PointMm? = null,
    val demoCurvePreview: PointMm? = null,
    val demoCircleCenter: PointMm? = null,
    val demoCircleEdge: PointMm? = null,
    val demoPointerWorld: PointMm? = null,
    val demoPendingGrabSelection: Boolean = false,
    val demoLeftVector: Offset = Offset.Zero,
    val demoRightVector: Offset = Offset.Zero
)

internal fun BlueprintControlTutorialStep.resolvedDemoTool(): BlueprintDraftTool? {
    return demoTool ?: when (demoOpeningPanel) {
        OpeningPanelType.DOORS -> BlueprintDraftTool.PLACE_DOOR
        OpeningPanelType.WINDOWS -> BlueprintDraftTool.PLACE_WINDOW
        OpeningPanelType.STAIR_UP -> BlueprintDraftTool.PLACE_STAIR_UP
        OpeningPanelType.STAIR_DOWN -> BlueprintDraftTool.PLACE_STAIR_DOWN
        null -> null
    }
}

internal fun BlueprintControlTutorialStep.resolvedTutorialTool(): BlueprintDraftTool {
    return resolvedDemoTool() ?: when (target) {
        BlueprintControlTutorialTarget.TOUCH_SELECT_BUTTON,
        BlueprintControlTutorialTarget.TOUCH_GRAB_BUTTON -> BlueprintDraftTool.SELECT
        else -> BlueprintDraftTool.DRAW_WALL
    }
}

internal fun BlueprintControlTutorialStep.resolvedPlacementWalls(): List<WallSegment> {
    return demoPlacementWalls.mapIndexed { index, wall ->
        WallSegment(
            id = "__tutorial_demo_wall_$index",
            start = wall.start,
            end = wall.end
        )
    }
}

internal fun blueprintControlTutorialSteps(
    dualJoysticksEnabled: Boolean
): List<BlueprintControlTutorialStep> {
    val railSteps = commonRailTutorialSteps()
    val modeSteps = if (dualJoysticksEnabled) {
        joystickModeTutorialSteps()
    } else {
        touchModeTutorialSteps()
    }
    val hudSteps = commonHudTutorialSteps(includeGridScaleBadge = !dualJoysticksEnabled)
    return railSteps + modeSteps + hudSteps
}

private fun commonRailTutorialSteps(): List<BlueprintControlTutorialStep> {
    return listOf(
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.BOTTOM_RAIL,
            title = "Build Rail",
            message = "This rail is where drafting modes, placement panels, and the quick reference all live.",
            details = listOf(
                "The first cluster changes how geometry is drafted.",
                "The middle cluster arms openings and estimating panels.",
                "The last button opens the in-app legend."
            ),
            seeIt = "The rest of the tour isolates each feature one at a time so you can see what changes without treating the tutorial like a checklist."
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.DETACHED_BUTTON,
            title = "Detached",
            message = "Detached tells the next wall to start fresh instead of chaining from the last endpoint.",
            details = listOf(
                "Use it when the next segment belongs to a different run.",
                "Turn it back off when you want perimeter drafting to continue corner to corner."
            ),
            seeIt = "This wall preview stays self-contained, which is the detached behavior you want before starting a separate line.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.BOX_BUTTON,
            title = "Box",
            message = "Box drafts a rectangle from two corners and keeps the shape easy to size before you commit it.",
            details = listOf(
                "Tap the origin corner first and the opposite corner second.",
                "The edge dials can refine both size and rotation before the final tap."
            ),
            seeIt = "A box draft is already staged on the canvas so you can see the live outline and corner flow immediately.",
            demoTool = BlueprintDraftTool.DRAW_BOX,
            demoBoxStart = TUTORIAL_BOX_DEMO_START,
            demoBoxPreview = TUTORIAL_BOX_DEMO_END,
            demoBoxRotationRadians = Math.toRadians(8.0)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.MEASURED_ARC_BUTTON,
            title = "Measured Arc",
            message = "Measured Arc builds a true arc by locking in start, end, and rise.",
            details = listOf(
                "Use it when the span is fixed and the curve needs a measurable rise.",
                "The edge dials switch to sweep and rise once the end point is set."
            ),
            seeIt = "This step already shows a measured-arc preview so you can read the chord-to-rise relationship at a glance.",
            demoTool = BlueprintDraftTool.DRAW_MEASURED_ARC,
            demoCurveStart = TUTORIAL_ARC_DEMO_START,
            demoCurveEnd = TUTORIAL_ARC_DEMO_END,
            demoCurvePreview = TUTORIAL_MEASURED_ARC_DEMO_CONTROL
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.SKETCH_CURVE_BUTTON,
            title = "Sketch Curve",
            message = "Sketch Curve keeps the same start and end flow but lets you pull a freer bend.",
            details = listOf(
                "Use it when you care more about the shape than a measured radius.",
                "The edge dials switch to shift and bend after the span is set."
            ),
            seeIt = "This curve preview is intentionally looser so you can compare it against the measured arc behavior from the last step.",
            demoTool = BlueprintDraftTool.DRAW_SKETCH_CURVE,
            demoCurveStart = TUTORIAL_ARC_DEMO_START,
            demoCurveEnd = TUTORIAL_ARC_DEMO_END,
            demoCurvePreview = TUTORIAL_SKETCH_CURVE_DEMO_CONTROL
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.CIRCLE_BUTTON,
            title = "Circle",
            message = "Circle is a center-and-radius workflow for round pads, columns, and curved takeoff zones.",
            details = listOf(
                "Set the center first and size it from the edge.",
                "After placement, the selection panel exposes radius and diameter nudges."
            ),
            seeIt = "The canvas is already showing a live circle draft, which is the same preview you get before placing the final radius.",
            demoTool = BlueprintDraftTool.DRAW_CIRCLE,
            demoCircleCenter = TUTORIAL_CIRCLE_DEMO_CENTER,
            demoCircleEdge = TUTORIAL_CIRCLE_DEMO_EDGE
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.DOORS_BUTTON,
            title = "Doors",
            message = "Doors opens the panel for door presets and arms the current preset for wall placement.",
            details = listOf(
                "Preset size, custom width, height, and sill all live in the panel.",
                "Door swing is inferred from the side of the wall you approach."
            ),
            seeIt = "The Doors panel is open and a door is already snapped to the sample wall so you can see the placement posture.",
            demoOpeningPanel = OpeningPanelType.DOORS,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.WINDOWS_BUTTON,
            title = "Windows",
            message = "Windows uses the same placement flow, but the presets and sill defaults are tuned for fenestration.",
            details = listOf(
                "Window width, height, and sill can be preset or typed directly.",
                "The armed preview follows the wall until you confirm placement."
            ),
            seeIt = "The window preset is armed on the same sample wall here so you can compare it against the door workflow immediately.",
            demoOpeningPanel = OpeningPanelType.WINDOWS,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.STAIR_UP_BUTTON,
            title = "Stair Up",
            message = "Stair Up marks an upward stair opening directly on the wall where the run belongs.",
            details = listOf(
                "These presets track run and rise instead of simple opening height.",
                "Use it when the stair opening climbs away from the current floor."
            ),
            seeIt = "The stair-up preset is already armed so you can see that the workflow matches other openings while the labels change to run and rise.",
            demoOpeningPanel = OpeningPanelType.STAIR_UP,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.STAIR_DOWN_BUTTON,
            title = "Stair Down",
            message = "Stair Down captures openings that descend away from the current level.",
            details = listOf(
                "Use it when the cut belongs to a stair run dropping below the active floor.",
                "The placement behavior is the same, but the resulting opening type is different."
            ),
            seeIt = "This step shows the stair-down preset armed on the wall so you can recognize it before you need it in a live draft.",
            demoOpeningPanel = OpeningPanelType.STAIR_DOWN,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.PARAMS_BUTTON,
            title = "Params",
            message = "Params opens the trade formulas and snap tuning that sit behind your blueprint quantities.",
            details = listOf(
                "Trade-specific estimating controls live above room tools in the panel.",
                "Snap threshold and drafting defaults are adjusted here too."
            ),
            seeIt = "The Params panel is already open so you can scan the live estimating controls without leaving blueprint mode.",
            showParamsPanel = true
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.HELP_BUTTON,
            title = "Help",
            message = "Help opens the quick-reference legend for the rail and the active control mode.",
            details = listOf(
                "Use it when you need an icon refresher without leaving the screen.",
                "The contents adapt to touch vs dual-joystick controls."
            ),
            seeIt = "The help panel is open right now so you can see the kind of reminder it gives before closing it.",
            showHelpPanel = true
        )
    )
}

private fun joystickModeTutorialSteps(): List<BlueprintControlTutorialStep> {
    return listOf(
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.CANVAS,
            title = "Canvas Flow",
            message = "In joystick mode the cursor becomes your pointer, so the whole drafting flow is move, confirm, then refine.",
            details = listOf(
                "The right pad aims the cursor.",
                "The left pad pans the world under the cursor.",
                "Draft previews still show up on the canvas before you confirm."
            ),
            seeIt = "This line preview is already staged so you can connect the cursor workflow to the geometry it produces.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
            title = "Left Pad",
            message = "The left pad pans the blueprint for coarse positioning without changing the current tool.",
            details = listOf(
                "Long pushes travel quickly across the plan.",
                "Light pressure makes smaller corrections near a snap point."
            ),
            seeIt = "The animated vector shows a live pan gesture, which is why the drawing shifts while the cursor stays relevant.",
            demoLeftVector = Offset(-0.62f, 0.26f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
            title = "Right Pad",
            message = "The right pad is the aiming side for cursor movement and confirm actions.",
            details = listOf(
                "Use small movement for precision and full movement for travel.",
                "Placement, confirmation, and selection happen at the cursor."
            ),
            seeIt = "The animated vector here represents the aiming motion that lines the cursor up before you place a point.",
            demoRightVector = Offset(0.56f, -0.54f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
            title = "Zoom and History",
            message = "The center cluster keeps zoom, undo, redo, and live coordinates easy to reach with either thumb.",
            details = listOf(
                "Zoom in before fine placements and back out to regain context.",
                "Undo and Redo are the fastest cleanup path when a draft step is wrong."
            ),
            seeIt = "This cluster does not change tools, which is why it stays centered while every other control mode changes around it."
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.EDGE_DIALS,
            title = "Precision Dials",
            message = "The edge dials are for small, repeatable nudges when stick movement would be too coarse.",
            details = listOf(
                "Left dial adjusts angle or sweep.",
                "Right dial adjusts length, rise, or bend depending on the active tool."
            ),
            seeIt = "A wall draft is already active so the dials are visible in their working state instead of hidden.",
            demoTool = BlueprintDraftTool.DRAW_WALL,
            forceEdgeDials = true,
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        )
    )
}

private fun touchModeTutorialSteps(): List<BlueprintControlTutorialStep> {
    return listOf(
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.CANVAS,
            title = "Canvas Flow",
            message = "Touch mode is still point-driven drafting, but the canvas is where start, end, and preview states all come together.",
            details = listOf(
                "Walls, boxes, arcs, curves, and circles all preview before they are committed.",
                "Two-finger pan and zoom stay available while you draft."
            ),
            seeIt = "The wall preview is already on screen here so you can focus on the draft feedback instead of performing the gesture.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.TOUCH_SELECT_BUTTON,
            title = "Select",
            message = "Select is the inspection mode for existing walls and openings.",
            details = listOf(
                "Use it when you want to inspect quantities or bring up the edit card.",
                "Selection is separate from drawing so you do not place new geometry by accident."
            ),
            seeIt = "This button is highlighted on its own because selection is a mode switch, not just another tap target on the canvas.",
            demoTool = BlueprintDraftTool.SELECT
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.TOUCH_DRAW_BUTTON,
            title = "Draw",
            message = "Draw returns the blueprint to normal wall and shape creation.",
            details = listOf(
                "Most rail tools ultimately feed back into draw mode on the canvas.",
                "You return here after cancelling or finishing most transient states."
            ),
            seeIt = "The draft preview is already active so you can recognize the draw-state visuals before starting your own layout.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.TOUCH_GRAB_BUTTON,
            title = "Grab",
            message = "Grab is the recovery path for relocating a wall instead of redrawing it.",
            details = listOf(
                "If nothing is selected yet, Grab becomes the next thing the app is waiting for.",
                "Once a wall is picked up, the canvas treats it like a live placement preview."
            ),
            seeIt = "This step puts Grab into its waiting state so you can recognize the mode before you need to reposition something quickly.",
            demoTool = BlueprintDraftTool.SELECT,
            demoPendingGrabSelection = true
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.TOUCH_CANCEL_BUTTON,
            title = "Cancel",
            message = "Cancel exits the current draft, pickup, or placement state without forcing you through completion.",
            details = listOf(
                "It is the fastest way out of a mistaken draft step.",
                "Use it before switching modes if the current state is already wrong."
            ),
            seeIt = "A live wall draft is staged behind this button so you can see exactly the kind of in-progress state Cancel is designed to clear.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
            title = "Zoom and History",
            message = "The center controls keep zoom, undo, redo, and live coordinates in one predictable place.",
            details = listOf(
                "Zoom first, then place precisely.",
                "Undo and Redo are faster than mode-switching when only the last move is wrong."
            ),
            seeIt = "This cluster stays stable while the tool buttons change, which makes it the safest place to recover orientation."
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.EDGE_DIALS,
            title = "Precision Dials",
            message = "The edge dials give you repeatable fine adjustments without dragging directly on the geometry.",
            details = listOf(
                "Use them after a draft is already active.",
                "The labels change with walls, boxes, measured arcs, and sketch curves."
            ),
            seeIt = "A wall draft is already active so the dials are shown in the exact state where they matter.",
            demoTool = BlueprintDraftTool.DRAW_WALL,
            forceEdgeDials = true,
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        )
    )
}

private fun commonHudTutorialSteps(includeGridScaleBadge: Boolean): List<BlueprintControlTutorialStep> {
    return buildList {
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_START_STACK,
                title = "Live Totals",
                message = "The top-left stack is the running quantity dashboard while you work.",
                details = listOf(
                    "It reacts to geometry changes without leaving blueprint mode.",
                    "Selection details also appear in this lane when there is something to inspect."
                ),
                seeIt = "Even on a blank starter project, this stack shows you where live takeoff feedback is always going to appear."
            )
        )
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_END_STACK,
                title = "Project and Trade",
                message = "The top-right stack keeps scope, trade context, and destructive actions visible without covering the canvas center.",
                details = listOf(
                    "Scope changes which part of the takeoff you are working in.",
                    "Project context and control chips stay grouped here."
                ),
                seeIt = "This lane is intentionally compact because it is the place for context switches rather than drafting gestures."
            )
        )
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                title = "Floor Switcher",
                message = "Floor controls let you move between levels without leaving blueprint mode.",
                details = listOf(
                    "Use it before drawing when the next geometry belongs on another level.",
                    "Ground is always one tap away."
                ),
                seeIt = "This control stays separate from the main rail because floor changes affect which blueprint data is in scope."
            )
        )
        if (includeGridScaleBadge) {
            add(
                BlueprintControlTutorialStep(
                    target = BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
                    title = "Grid Scale",
                    message = "Grid scale changes the size of each reference block and the feel of snapping on the canvas.",
                    details = listOf(
                        "Smaller steps support tighter layouts.",
                        "Larger steps help with rough blocking and fast framing."
                    ),
                    seeIt = "The grid editor is already open here so you can see the control it expands into without interrupting the tour.",
                    showGridScaleEditor = true
                )
            )
        }
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Clear All",
                message = "Clear All is the full-reset control for the current blueprint layout.",
                details = listOf(
                    "Use Undo when only the last few moves are wrong.",
                    "Use Clear All when the entire sketch needs to restart."
                ),
                seeIt = "This sits outside the main rail on purpose because it is a recovery action, not part of the normal drafting rhythm."
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
    minimumTopClearance: Dp = 0.dp,
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
        val cardHeightEstimate = 208.dp + (step.details.size * 18).dp + if (step.seeIt != null) 62.dp else 0.dp
        val maxCardHeight = (maxHeight - 24.dp).coerceAtLeast(180.dp)
        val compactWindow = maxWidth < 420.dp || maxHeight < 720.dp
        val targetRect = remember(targetBounds) { targetBounds?.inflate(12f) }
        val tooltipOffset = remember(
            targetRect,
            maxWidth,
            maxHeight,
            cardWidth,
            cardHeightEstimate,
            maxCardHeight,
            compactWindow,
            minimumTopClearance,
            density
        ) {
            with(density) {
                blueprintTutorialTooltipOffset(
                    targetRect = targetRect,
                    viewportWidthPx = maxWidth.roundToPx(),
                    viewportHeightPx = maxHeight.roundToPx(),
                    cardWidthPx = cardWidth.roundToPx(),
                    cardHeightPx = cardHeightEstimate.roundToPx().coerceAtMost(maxCardHeight.roundToPx()),
                    horizontalPaddingPx = 12.dp.roundToPx(),
                    verticalPaddingPx = 12.dp.roundToPx(),
                    minimumTopClearancePx = minimumTopClearance.roundToPx(),
                    anchorSpacingPx = 14.dp.roundToPx(),
                    compactWindow = compactWindow
                )
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stepIndex) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )

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
                    step.seeIt?.let { prompt ->
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
                                    text = "In Action",
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

internal fun blueprintTutorialTooltipOffset(
    targetRect: Rect?,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    cardWidthPx: Int,
    cardHeightPx: Int,
    horizontalPaddingPx: Int,
    verticalPaddingPx: Int,
    minimumTopClearancePx: Int,
    anchorSpacingPx: Int,
    compactWindow: Boolean
): IntOffset {
    val topClearancePx = maxOf(verticalPaddingPx, minimumTopClearancePx)
    if (targetRect == null) {
        return IntOffset(x = horizontalPaddingPx, y = topClearancePx)
    }
    val maxCardX = viewportWidthPx - cardWidthPx - horizontalPaddingPx
    val targetCenterX = targetRect.center.x.roundToInt()
    val desiredX = targetCenterX - (cardWidthPx / 2)
    val clampedX = desiredX.coerceIn(
        horizontalPaddingPx,
        maxOf(horizontalPaddingPx, maxCardX)
    )
    val lowerScreenTarget =
        targetRect.bottom.roundToInt() >= (viewportHeightPx * 0.52f).roundToInt()
    val preferPinnedTop =
        lowerScreenTarget ||
            (
                compactWindow &&
                    targetRect.center.y >= viewportHeightPx * 0.34f
                )
    val aboveY = targetRect.top.roundToInt() - cardHeightPx - anchorSpacingPx
    val belowY = targetRect.bottom.roundToInt() + anchorSpacingPx
    val maxCardY = viewportHeightPx - cardHeightPx - verticalPaddingPx
    val resolvedY = when {
        preferPinnedTop -> topClearancePx
        aboveY >= topClearancePx -> aboveY
        else -> belowY.coerceIn(
            topClearancePx,
            maxOf(topClearancePx, maxCardY)
        )
    }
    return IntOffset(x = clampedX, y = resolvedY)
}

private fun Rect.inflate(padding: Float): Rect {
    return Rect(
        left = left - padding,
        top = top - padding,
        right = right + padding,
        bottom = bottom + padding
    )
}

private val TUTORIAL_WALL_DEMO_START = PointMm(x = -4_200L, y = -1_100L)
private val TUTORIAL_WALL_DEMO_END = PointMm(x = 3_600L, y = 1_850L)
private val TUTORIAL_BOX_DEMO_START = PointMm(x = -3_900L, y = -2_200L)
private val TUTORIAL_BOX_DEMO_END = PointMm(x = 2_800L, y = 1_300L)
private val TUTORIAL_ARC_DEMO_START = PointMm(x = -4_800L, y = -1_200L)
private val TUTORIAL_ARC_DEMO_END = PointMm(x = 4_600L, y = -1_200L)
private val TUTORIAL_MEASURED_ARC_DEMO_CONTROL = PointMm(x = 0L, y = 2_600L)
private val TUTORIAL_SKETCH_CURVE_DEMO_CONTROL = PointMm(x = 1_400L, y = 2_100L)
private val TUTORIAL_CIRCLE_DEMO_CENTER = PointMm(x = 0L, y = -300L)
private val TUTORIAL_CIRCLE_DEMO_EDGE = PointMm(x = 3_200L, y = -300L)
private val TUTORIAL_OPENING_DEMO_WALL = BlueprintControlTutorialDemoWall(
    start = PointMm(x = -5_200L, y = -2_500L),
    end = PointMm(x = 5_200L, y = -2_500L)
)
private val TUTORIAL_OPENING_DEMO_POINTER = PointMm(x = 300L, y = -2_500L)
