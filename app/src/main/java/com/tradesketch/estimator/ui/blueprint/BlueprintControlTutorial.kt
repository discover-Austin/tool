package com.tradesketch.estimator.ui.blueprint

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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
    JOYSTICK_LEFT_PAD,
    JOYSTICK_FINE_LEFT_PAD,
    JOYSTICK_CENTER_CONTROLS,
    JOYSTICK_FINE_RIGHT_PAD,
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
    val demoRightVector: Offset = Offset.Zero,
    val demoFineLeftVector: Offset = Offset.Zero,
    val demoFineRightVector: Offset = Offset.Zero
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
    return resolvedDemoTool() ?: BlueprintDraftTool.DRAW_WALL
}

internal fun BlueprintControlTutorialStep.hasLiveExample(): Boolean {
    return demoPlacementWalls.isNotEmpty() ||
        demoDrawingStart != null ||
        demoDrawingPreview != null ||
        demoBoxStart != null ||
        demoBoxPreview != null ||
        demoCurveStart != null ||
        demoCurveEnd != null ||
        demoCurvePreview != null ||
        demoCircleCenter != null ||
        demoCircleEdge != null ||
        demoPointerWorld != null ||
        demoPendingGrabSelection ||
        showParamsPanel ||
        showHelpPanel ||
        showGridScaleEditor ||
        forceEdgeDials ||
        demoLeftVector != Offset.Zero ||
        demoRightVector != Offset.Zero ||
        demoFineLeftVector != Offset.Zero ||
        demoFineRightVector != Offset.Zero
}

internal fun BlueprintControlTutorialStep.prefersPinnedTopTooltip(): Boolean {
    return hasLiveExample() || when (target) {
        BlueprintControlTutorialTarget.CANVAS,
        BlueprintControlTutorialTarget.BOTTOM_RAIL,
        BlueprintControlTutorialTarget.OPENING_PANEL,
        BlueprintControlTutorialTarget.PARAMS_PANEL,
        BlueprintControlTutorialTarget.RAIL_HELP_PANEL,
        BlueprintControlTutorialTarget.EDGE_DIALS -> true

        else -> false
    }
}

internal fun BlueprintControlTutorialStep.prefersSideAnchoredTooltip(): Boolean {
    return hasLiveExample() || when (target) {
        BlueprintControlTutorialTarget.CANVAS,
        BlueprintControlTutorialTarget.BOTTOM_RAIL,
        BlueprintControlTutorialTarget.OPENING_PANEL,
        BlueprintControlTutorialTarget.PARAMS_PANEL,
        BlueprintControlTutorialTarget.RAIL_HELP_PANEL,
        BlueprintControlTutorialTarget.EDGE_DIALS -> true

        else -> false
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

internal fun blueprintControlTutorialSteps(): List<BlueprintControlTutorialStep> {
    return commonRailTutorialSteps() +
        joystickModeTutorialSteps() +
        commonHudTutorialSteps(
            includeFloorSwitcher = true,
            includeGridScaleBadge = true
        )
}

private fun commonRailTutorialSteps(): List<BlueprintControlTutorialStep> {
    return listOf(
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.BOTTOM_RAIL,
            title = "Build Rail",
            message = "This bottom bar is the main tool bar for drawing.",
            details = listOf(
                "The left side changes how you draw.",
                "The middle buttons open doors, windows, stairs, and settings.",
                "The help button explains the icons."
            ),
            seeIt = "The next steps point at one button at a time so you can learn it without guessing."
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.DETACHED_BUTTON,
            title = "Detached",
            message = "Use Detached when the next wall should start by itself.",
            details = listOf(
                "Good for a separate wall run.",
                "Turn it off when you want walls to keep chaining together."
            ),
            seeIt = "This preview stands on its own, which is exactly what Detached does.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.BOX_BUTTON,
            title = "Box",
            message = "Box draws a rectangle fast.",
            details = listOf(
                "Tap the first corner.",
                "Move to the opposite corner and tap again."
            ),
            seeIt = "A sample box is already on screen so you can see the shape before trying it.",
            demoTool = BlueprintDraftTool.DRAW_BOX,
            demoBoxStart = TUTORIAL_BOX_DEMO_START,
            demoBoxPreview = TUTORIAL_BOX_DEMO_END,
            demoBoxRotationRadians = Math.toRadians(8.0)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.MEASURED_ARC_BUTTON,
            title = "Measured Arc",
            message = "Measured Arc makes a curved wall from a start, an end, and a rise.",
            details = listOf(
                "Use it when you want a real measured curve.",
                "After you set the span, the dials change sweep and rise."
            ),
            seeIt = "This sample arc shows the curve before you place it.",
            demoTool = BlueprintDraftTool.DRAW_MEASURED_ARC,
            demoCurveStart = TUTORIAL_ARC_DEMO_START,
            demoCurveEnd = TUTORIAL_ARC_DEMO_END,
            demoCurvePreview = TUTORIAL_MEASURED_ARC_DEMO_CONTROL
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.SKETCH_CURVE_BUTTON,
            title = "Sketch Curve",
            message = "Sketch Curve makes a looser curve.",
            details = listOf(
                "Use it when the shape matters more than exact math.",
                "After you set the span, the dials change shift and bend."
            ),
            seeIt = "This sample curve bends more freely than Measured Arc.",
            demoTool = BlueprintDraftTool.DRAW_SKETCH_CURVE,
            demoCurveStart = TUTORIAL_ARC_DEMO_START,
            demoCurveEnd = TUTORIAL_ARC_DEMO_END,
            demoCurvePreview = TUTORIAL_SKETCH_CURVE_DEMO_CONTROL
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.CIRCLE_BUTTON,
            title = "Circle",
            message = "Circle draws round shapes from a center point.",
            details = listOf(
                "Tap the center first.",
                "Move out to set the size and tap again."
            ),
            seeIt = "The circle preview is already visible so you can see the two-step flow.",
            demoTool = BlueprintDraftTool.DRAW_CIRCLE,
            demoCircleCenter = TUTORIAL_CIRCLE_DEMO_CENTER,
            demoCircleEdge = TUTORIAL_CIRCLE_DEMO_EDGE
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.DOORS_BUTTON,
            title = "Doors",
            message = "Doors opens door sizes and gets a door ready to place on a wall.",
            details = listOf(
                "Pick a preset or type the size.",
                "Move to a wall and tap to place it."
            ),
            seeIt = "The door panel is open and a sample wall is ready.",
            demoOpeningPanel = OpeningPanelType.DOORS,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.WINDOWS_BUTTON,
            title = "Windows",
            message = "Windows works like Doors, but for windows.",
            details = listOf(
                "Pick a preset or type the size.",
                "Move to a wall and tap to place it."
            ),
            seeIt = "The window tool is armed on the sample wall so you can compare it with Doors.",
            demoOpeningPanel = OpeningPanelType.WINDOWS,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.STAIR_UP_BUTTON,
            title = "Stair Up",
            message = "Stair Up marks stairs that go up from this floor.",
            details = listOf(
                "Use it when the stairs go up from the level you are on.",
                "Place it on the wall just like other openings."
            ),
            seeIt = "This step shows Stair Up armed on the sample wall.",
            demoOpeningPanel = OpeningPanelType.STAIR_UP,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.STAIR_DOWN_BUTTON,
            title = "Stair Down",
            message = "Stair Down marks stairs that go down from this floor.",
            details = listOf(
                "Use it when the stairs drop below the level you are on.",
                "Place it on the wall just like other openings."
            ),
            seeIt = "This step shows Stair Down armed on the sample wall.",
            demoOpeningPanel = OpeningPanelType.STAIR_DOWN,
            demoPlacementWalls = listOf(TUTORIAL_OPENING_DEMO_WALL),
            demoPointerWorld = TUTORIAL_OPENING_DEMO_POINTER
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.PARAMS_BUTTON,
            title = "Params",
            message = "Params opens the main drawing settings.",
            details = listOf(
                "Change snap distance here.",
                "Change the default wall height here."
            ),
            seeIt = "The panel is already open so you can see the settings before you need them.",
            showParamsPanel = true
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.HELP_BUTTON,
            title = "Help",
            message = "Help shows a quick legend for the buttons on screen.",
            details = listOf(
                "Use it when you forget what an icon means.",
                "The help text changes for touch and joystick mode."
            ),
            seeIt = "The help panel is open in this step so you can see what it looks like.",
            showHelpPanel = true
        )
    )
}

private fun joystickModeTutorialSteps(): List<BlueprintControlTutorialStep> {
    return listOf(
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.CANVAS,
            title = "Canvas Flow",
            message = "In joystick mode, the big right pad aims, the big left pad pans, and the mini pads handle tiny nudges.",
            details = listOf(
                "Move the cursor with the right pad, then tap it to confirm at that point.",
                "Use the left pad to move the canvas under the cursor.",
                "The mini pads beside zoom help when you need slower, tighter movement."
            ),
            seeIt = "The sample wall shows the preview you line up before you commit it.",
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
            title = "Pan / Alt",
            message = "The big left pad is your fast pan control, and its tap or press helps you back out of the current state.",
            details = listOf(
                "Push it to move the canvas.",
                "Tap it to cancel a draft, clear a picked-up wall, or clear selection.",
                "Press and hold it when you want wall targeting to feel more forgiving."
            ),
            seeIt = "The moving arrow shows the broad pan motion from the big pad.",
            demoLeftVector = Offset(-0.62f, 0.26f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_FINE_LEFT_PAD,
            title = "Fine Pan",
            message = "The small left mini pad nudges the canvas in slower, shorter moves.",
            details = listOf(
                "Use it after zooming in.",
                "It is for framing and tiny alignment, not long travel."
            ),
            seeIt = "This mini pad sits beside zoom so fine movement stays near the center controls.",
            demoFineLeftVector = Offset(-0.46f, 0.18f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
            title = "Zoom and History",
            message = "This center stack keeps zoom, undo, redo, and the live state label together.",
            details = listOf(
                "Zoom in before precise work.",
                "Undo and redo fix quick mistakes.",
                "The state label tells you when you are drawing, boxing, or selecting."
            ),
            seeIt = "This group stays in the middle because you may need it at any time."
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_FINE_RIGHT_PAD,
            title = "Fine Cursor",
            message = "The small right mini pad micro-aims the cursor without overshooting.",
            details = listOf(
                "Use it to line up corners, openings, and wall picks.",
                "It moves slower than the big right pad."
            ),
            seeIt = "This mini pad sits beside zoom so precise cursor moves stay close to the center controls.",
            demoFineRightVector = Offset(0.42f, -0.22f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
            title = "Cursor / Select",
            message = "The big right pad moves the cursor, and a tap confirms or selects at that point.",
            details = listOf(
                "Move first, then tap to place corners, boxes, openings, and selections.",
                "Use small moves before you commit."
            ),
            seeIt = "The moving arrow shows the aiming motion from the big right pad.",
            demoRightVector = Offset(0.56f, -0.54f)
        ),
        BlueprintControlTutorialStep(
            target = BlueprintControlTutorialTarget.EDGE_DIALS,
            title = "Precision Dials",
            message = "After a draft is on screen, the dials reshape it while the mini pads keep positioning precise.",
            details = listOf(
                "Left dial changes angle or sweep.",
                "Right dial changes length, rise, or bend.",
                "Use the mini pads first if the cursor or canvas needs a tiny nudge."
            ),
            seeIt = "A sample wall is active so the dials are visible right now.",
            demoTool = BlueprintDraftTool.DRAW_WALL,
            forceEdgeDials = true,
            demoDrawingStart = TUTORIAL_WALL_DEMO_START,
            demoDrawingPreview = TUTORIAL_WALL_DEMO_END
        )
    )
}

private fun commonHudTutorialSteps(
    includeFloorSwitcher: Boolean,
    includeGridScaleBadge: Boolean
): List<BlueprintControlTutorialStep> {
    return buildList {
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_START_STACK,
                title = "Live Totals",
                message = "The top-left area shows live numbers while you draw.",
                details = listOf(
                    "The numbers change when the drawing changes.",
                    "You may also see details for the item you selected."
                ),
                seeIt = "This is where live drawing feedback always shows up."
            )
        )
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.TOP_END_STACK,
                title = "Project Controls",
                message = "The top-right corner keeps save, trade scope, and reset actions together.",
                details = listOf(
                    "Tap the trade chip to cycle drywall, concrete, gravel, and paint.",
                    "Save and clear stay here instead of covering the canvas."
                ),
                seeIt = "Think of this corner as the project-control corner."
            )
        )
        if (includeFloorSwitcher) {
            add(
                BlueprintControlTutorialStep(
                    target = BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                    title = "Floor Switcher",
                    message = "Use this to switch floors without leaving the drawing.",
                    details = listOf(
                        "Pick the floor before you draw on it.",
                        "Ground is always one tap away."
                    ),
                    seeIt = "It stays separate because changing floors changes which drawing you are editing."
                )
            )
        }
        if (includeGridScaleBadge) {
            add(
                BlueprintControlTutorialStep(
                    target = BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
                    title = "Grid Scale",
                    message = "Grid Scale changes how big the grid feels on the canvas.",
                    details = listOf(
                        "Smaller steps help with tighter layouts.",
                        "Larger steps help with rough layout work."
                    ),
                    seeIt = "The grid editor is open in this step so you can see what this badge opens.",
                    showGridScaleEditor = true
                )
            )
        }
        add(
            BlueprintControlTutorialStep(
                target = BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON,
                title = "Clear All",
                message = "Clear All wipes the current drawing so you can start over.",
                details = listOf(
                    "Use Undo if only the last few moves were wrong.",
                    "Use Clear All if the whole sketch needs a restart."
                ),
                seeIt = "This sits outside the main tool bar because it is a reset action."
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
    targetBounds: List<Rect> = emptyList(),
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
        var measuredCardWidthPx by remember(stepIndex) { mutableIntStateOf(0) }
        var measuredCardHeightPx by remember(stepIndex) { mutableIntStateOf(0) }
        val cardWidth = maxWidth.minus(20.dp).coerceAtMost(272.dp)
        val cardHeightEstimate = 108.dp + (step.details.size * 14).dp
        val maxCardHeight = (maxHeight - 20.dp).coerceAtLeast(128.dp)
        val compactWindow = maxWidth < 420.dp || maxHeight < 720.dp
        val targetRects = remember(targetBounds, step.target, density) {
            val highlightPaddingPx = with(density) { tutorialHighlightPadding(step.target).toPx() }
            val highlightYOffsetPx = with(density) { tutorialHighlightVerticalOffset(step.target).toPx() }
            val highlightBottomTrimPx = with(density) { tutorialHighlightBottomTrim(step.target).toPx() }
            targetBounds.map {
                it.inflate(highlightPaddingPx)
                    .offsetBy(dy = -highlightYOffsetPx)
                    .trimBottom(highlightBottomTrimPx)
            }
        }
        val targetRect = remember(targetRects) {
            targetRects.reduceOrNull(::mergeTutorialRects)
        }
        val resolvedCardWidthPx = with(density) {
            measuredCardWidthPx.takeIf { it > 0 } ?: cardWidth.roundToPx()
        }
        val resolvedCardHeightPx = with(density) {
            measuredCardHeightPx.takeIf { it > 0 }
                ?: cardHeightEstimate.roundToPx().coerceAtMost(maxCardHeight.roundToPx())
        }
        val tooltipOffset = remember(
            targetRect,
            maxWidth,
            maxHeight,
            resolvedCardWidthPx,
            resolvedCardHeightPx,
            compactWindow,
            minimumTopClearance,
            density
        ) {
            with(density) {
                blueprintTutorialTooltipOffset(
                    targetRect = targetRect,
                    viewportWidthPx = maxWidth.roundToPx(),
                    viewportHeightPx = maxHeight.roundToPx(),
                    cardWidthPx = resolvedCardWidthPx,
                    cardHeightPx = resolvedCardHeightPx,
                    horizontalPaddingPx = 12.dp.roundToPx(),
                    verticalPaddingPx = 12.dp.roundToPx(),
                    minimumTopClearancePx = minimumTopClearance.roundToPx(),
                    anchorSpacingPx = 10.dp.roundToPx(),
                    compactWindow = compactWindow,
                    preferPinnedTop = step.prefersPinnedTopTooltip(),
                    preferSidePlacement = step.prefersSideAnchoredTooltip()
                )
            }
        }
        val highlightBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
        ) {
            targetRects.forEach { rect ->
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
                .offset { tooltipOffset }
                .widthIn(max = cardWidth)
                .heightIn(max = maxCardHeight)
                .onGloballyPositioned { coordinates ->
                    measuredCardWidthPx = coordinates.size.width
                    measuredCardHeightPx = coordinates.size.height
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                        }
                    }
                    Text(
                        text = step.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (step.details.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            step.details.forEach { detail ->
                                Text(
                                    text = "• $detail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
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
    compactWindow: Boolean,
    preferPinnedTop: Boolean = false,
    preferSidePlacement: Boolean = false
): IntOffset {
    val topClearancePx = maxOf(verticalPaddingPx, minimumTopClearancePx)
    if (targetRect == null) {
        return IntOffset(x = horizontalPaddingPx, y = topClearancePx)
    }
    val maxCardX = viewportWidthPx - cardWidthPx - horizontalPaddingPx
    val targetCenterX = targetRect.center.x.roundToInt()
    val wideTarget = targetRect.width >= viewportWidthPx * 0.58f
    val clampedX = if (preferSidePlacement || wideTarget) {
        if (targetCenterX >= viewportWidthPx / 2) {
            horizontalPaddingPx
        } else {
            maxOf(horizontalPaddingPx, maxCardX)
        }
    } else {
        val desiredX = targetCenterX - (cardWidthPx / 2)
        desiredX.coerceIn(
            horizontalPaddingPx,
            maxOf(horizontalPaddingPx, maxCardX)
        )
    }
    val lowerScreenTarget =
        targetRect.bottom.roundToInt() >= (viewportHeightPx * 0.52f).roundToInt()
    val shouldPinTop =
        preferPinnedTop ||
            lowerScreenTarget ||
            (
                compactWindow &&
                    targetRect.center.y >= viewportHeightPx * 0.34f
                )
    val aboveY = targetRect.top.roundToInt() - cardHeightPx - anchorSpacingPx
    val belowY = targetRect.bottom.roundToInt() + anchorSpacingPx
    val maxCardY = viewportHeightPx - cardHeightPx - verticalPaddingPx
    val resolvedY = when {
        shouldPinTop -> topClearancePx
        aboveY >= topClearancePx -> aboveY
        else -> belowY.coerceIn(
            topClearancePx,
            maxOf(topClearancePx, maxCardY)
        )
    }
    return IntOffset(x = clampedX, y = resolvedY)
}

private fun tutorialHighlightPadding(target: BlueprintControlTutorialTarget): Dp {
    return when (target) {
        BlueprintControlTutorialTarget.CANVAS,
        BlueprintControlTutorialTarget.BOTTOM_RAIL,
        BlueprintControlTutorialTarget.OPENING_PANEL,
        BlueprintControlTutorialTarget.PARAMS_PANEL,
        BlueprintControlTutorialTarget.RAIL_HELP_PANEL,
        BlueprintControlTutorialTarget.TOP_START_STACK,
        BlueprintControlTutorialTarget.TOP_END_STACK -> 8.dp

        BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
        BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
        BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
        BlueprintControlTutorialTarget.EDGE_DIALS,
        BlueprintControlTutorialTarget.FLOOR_SWITCHER,
        BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
        BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON -> 6.dp

        else -> 4.dp
    }
}

private fun tutorialHighlightVerticalOffset(target: BlueprintControlTutorialTarget): Dp {
    return when (target) {
        BlueprintControlTutorialTarget.CANVAS -> 8.dp
        BlueprintControlTutorialTarget.TOP_START_STACK,
        BlueprintControlTutorialTarget.TOP_END_STACK -> 8.dp

        BlueprintControlTutorialTarget.BOTTOM_RAIL,
        BlueprintControlTutorialTarget.DETACHED_BUTTON,
        BlueprintControlTutorialTarget.BOX_BUTTON,
        BlueprintControlTutorialTarget.MEASURED_ARC_BUTTON,
        BlueprintControlTutorialTarget.SKETCH_CURVE_BUTTON,
        BlueprintControlTutorialTarget.CIRCLE_BUTTON,
        BlueprintControlTutorialTarget.DOORS_BUTTON,
        BlueprintControlTutorialTarget.WINDOWS_BUTTON,
        BlueprintControlTutorialTarget.STAIR_UP_BUTTON,
        BlueprintControlTutorialTarget.STAIR_DOWN_BUTTON,
        BlueprintControlTutorialTarget.PARAMS_BUTTON,
        BlueprintControlTutorialTarget.HELP_BUTTON,
        BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
        BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
        BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
        BlueprintControlTutorialTarget.EDGE_DIALS,
        BlueprintControlTutorialTarget.FLOOR_SWITCHER,
        BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
        BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON -> 14.dp

        else -> 12.dp
    }
}

private fun tutorialHighlightBottomTrim(target: BlueprintControlTutorialTarget): Dp {
    return when (target) {
        BlueprintControlTutorialTarget.CANVAS -> 0.dp
        BlueprintControlTutorialTarget.TOP_START_STACK,
        BlueprintControlTutorialTarget.TOP_END_STACK -> 4.dp

        BlueprintControlTutorialTarget.BOTTOM_RAIL,
        BlueprintControlTutorialTarget.DETACHED_BUTTON,
        BlueprintControlTutorialTarget.BOX_BUTTON,
        BlueprintControlTutorialTarget.MEASURED_ARC_BUTTON,
        BlueprintControlTutorialTarget.SKETCH_CURVE_BUTTON,
        BlueprintControlTutorialTarget.CIRCLE_BUTTON,
        BlueprintControlTutorialTarget.DOORS_BUTTON,
        BlueprintControlTutorialTarget.WINDOWS_BUTTON,
        BlueprintControlTutorialTarget.STAIR_UP_BUTTON,
        BlueprintControlTutorialTarget.STAIR_DOWN_BUTTON,
        BlueprintControlTutorialTarget.PARAMS_BUTTON,
        BlueprintControlTutorialTarget.HELP_BUTTON,
        BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
        BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
        BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
        BlueprintControlTutorialTarget.EDGE_DIALS,
        BlueprintControlTutorialTarget.FLOOR_SWITCHER,
        BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
        BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON -> 10.dp

        else -> 6.dp
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

private fun Rect.offsetBy(dx: Float = 0f, dy: Float = 0f): Rect {
    return Rect(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy
    )
}

private fun Rect.trimBottom(trim: Float): Rect {
    if (trim <= 0f) return this
    return Rect(
        left = left,
        top = top,
        right = right,
        bottom = maxOf(top, bottom - trim)
    )
}

private fun mergeTutorialRects(first: Rect, second: Rect): Rect {
    return Rect(
        left = minOf(first.left, second.left),
        top = minOf(first.top, second.top),
        right = maxOf(first.right, second.right),
        bottom = maxOf(first.bottom, second.bottom)
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
