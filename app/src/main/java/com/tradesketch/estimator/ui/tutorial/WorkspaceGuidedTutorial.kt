package com.tradesketch.estimator.ui.tutorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.tradesketch.estimator.DetailTab
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.domain.model.WallSegment
import com.tradesketch.estimator.ui.blueprint.OpeningPanelType
import com.tradesketch.estimator.ui.components.ReferenceBlueprintBorder
import com.tradesketch.estimator.ui.components.ReferenceBlueprintGold
import com.tradesketch.estimator.ui.components.ReferenceBlueprintInk
import com.tradesketch.estimator.ui.components.ReferenceBlueprintMuted
import com.tradesketch.estimator.ui.components.ReferenceBlueprintNavy
import com.tradesketch.estimator.ui.components.ReferenceBlueprintPaperAlt
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import kotlin.math.roundToInt

internal data class GuidedTutorialProgress(
    val stepNumber: Int,
    val totalSteps: Int
)

internal sealed interface WorkspaceGuidedTutorialStep {
    val tab: DetailTab
    val title: String
    val message: String
    val supporting: String?
    val primaryActionLabel: String
}

internal enum class WorkspaceRailGuidedTutorialTarget {
    LEFT_RAIL
}

internal data class WorkspaceRailGuidedTutorialStep(
    val target: WorkspaceRailGuidedTutorialTarget,
    override val title: String,
    override val message: String,
    override val supporting: String? = null,
    override val primaryActionLabel: String = "Next"
) : WorkspaceGuidedTutorialStep {
    override val tab: DetailTab = DetailTab.BLUEPRINT
}

internal enum class BlueprintGuidedTutorialTarget {
    TOOL_RAIL,
    JOYSTICKS,
    OPENING_TOOLS
}

internal data class BlueprintGuidedTutorialDemoWall(
    val start: PointMm,
    val end: PointMm
)

internal data class BlueprintGuidedTutorialStep(
    val target: BlueprintGuidedTutorialTarget,
    override val title: String,
    override val message: String,
    override val supporting: String? = null,
    override val primaryActionLabel: String = "Next",
    val demoTool: BlueprintDraftTool = BlueprintDraftTool.DRAW_WALL,
    val demoOpeningPanel: OpeningPanelType? = null,
    val demoPlacementWalls: List<BlueprintGuidedTutorialDemoWall> = emptyList(),
    val demoDrawingStart: PointMm? = null,
    val demoDrawingPreview: PointMm? = null,
    val demoPointerWorld: PointMm? = null,
    val demoLeftVector: Offset = Offset.Zero,
    val demoRightVector: Offset = Offset.Zero,
    val demoFineLeftVector: Offset = Offset.Zero,
    val demoFineRightVector: Offset = Offset.Zero
) : WorkspaceGuidedTutorialStep {
    override val tab: DetailTab = DetailTab.BLUEPRINT
}

internal fun BlueprintGuidedTutorialStep.resolvedDemoTool(): BlueprintDraftTool {
    return when (demoOpeningPanel) {
        OpeningPanelType.DOORS -> BlueprintDraftTool.PLACE_DOOR
        OpeningPanelType.WINDOWS -> BlueprintDraftTool.PLACE_WINDOW
        OpeningPanelType.STAIR_UP -> BlueprintDraftTool.PLACE_STAIR_UP
        OpeningPanelType.STAIR_DOWN -> BlueprintDraftTool.PLACE_STAIR_DOWN
        null -> demoTool
    }
}

internal fun BlueprintGuidedTutorialStep.resolvedPlacementWalls(): List<WallSegment> {
    return demoPlacementWalls.mapIndexed { index, wall ->
        WallSegment(
            id = "__guided_tutorial_wall_$index",
            start = wall.start,
            end = wall.end
        )
    }
}

internal enum class MaterialsGuidedTutorialTarget {
    ESTIMATE_INPUTS,
    PRICING
}

internal data class MaterialsGuidedTutorialStep(
    val target: MaterialsGuidedTutorialTarget,
    override val title: String,
    override val message: String,
    override val supporting: String? = null,
    override val primaryActionLabel: String = "Next"
) : WorkspaceGuidedTutorialStep {
    override val tab: DetailTab = DetailTab.MATERIALS
}

internal enum class ExportGuidedTutorialTarget {
    PROJECT_SUMMARY,
    PREVIEW,
    PRIMARY_ACTIONS
}

internal data class ExportGuidedTutorialStep(
    val target: ExportGuidedTutorialTarget,
    override val title: String,
    override val message: String,
    override val supporting: String? = null,
    override val primaryActionLabel: String = "Next"
) : WorkspaceGuidedTutorialStep {
    override val tab: DetailTab = DetailTab.EXPORT
}

internal fun workspaceGuidedTutorialSteps(
    inputMode: TakeoffInputMode = TakeoffInputMode.BLUEPRINT
): List<WorkspaceGuidedTutorialStep> {
    return when (inputMode) {
        TakeoffInputMode.MANUAL -> manualWorkspaceGuidedTutorialSteps()
        TakeoffInputMode.BLUEPRINT -> blueprintWorkspaceGuidedTutorialSteps()
    }
}

private fun blueprintWorkspaceGuidedTutorialSteps(): List<WorkspaceGuidedTutorialStep> {
    return listOf(
        WorkspaceRailGuidedTutorialStep(
            target = WorkspaceRailGuidedTutorialTarget.LEFT_RAIL,
            title = "Navigate from the rail",
            message = "Home, new project, open, and the main workspace sections all live on the left rail.",
            supporting = "The tour will move tabs for you now, but this rail is how you move around later."
        ),
        BlueprintGuidedTutorialStep(
            target = BlueprintGuidedTutorialTarget.TOOL_RAIL,
            title = "Start on the rail",
            message = "Choose a drawing tool here, then place it on the plan.",
            supporting = "Wall is ready by default. Openings and quick settings stay on the same rail.",
            demoDrawingStart = GUIDED_WALL_DEMO_START,
            demoDrawingPreview = GUIDED_WALL_DEMO_END
        ),
        BlueprintGuidedTutorialStep(
            target = BlueprintGuidedTutorialTarget.JOYSTICKS,
            title = "Aim with confidence",
            message = "Right pads place the cursor. Left pads move the blueprint underneath it.",
            supporting = "The small pads are for fine positioning when you need a cleaner placement.",
            demoDrawingStart = GUIDED_WALL_DEMO_START,
            demoDrawingPreview = GUIDED_WALL_DEMO_END,
            demoRightVector = Offset(0.56f, -0.54f),
            demoFineRightVector = Offset(0.26f, -0.12f)
        ),
        BlueprintGuidedTutorialStep(
            target = BlueprintGuidedTutorialTarget.OPENING_TOOLS,
            title = "Openings land on walls",
            message = "Pick a door, window, or stair tool, line it up on a wall, then confirm the point.",
            supporting = "The preview shows where the opening will land before you place it.",
            primaryActionLabel = "Continue to Materials",
            demoOpeningPanel = OpeningPanelType.DOORS,
            demoPlacementWalls = listOf(GUIDED_OPENING_DEMO_WALL),
            demoPointerWorld = GUIDED_OPENING_DEMO_POINTER
        ),
        MaterialsGuidedTutorialStep(
            target = MaterialsGuidedTutorialTarget.ESTIMATE_INPUTS,
            title = "Tune the estimate",
            message = "Measurements, waste, and trade settings live here.",
            supporting = "Blueprint projects keep these values tied to the plan while you adjust the estimate."
        ),
        MaterialsGuidedTutorialStep(
            target = MaterialsGuidedTutorialTarget.PRICING,
            title = "Price it",
            message = "Unit costs, labor, markup, and tax build the job total.",
            supporting = "Make pricing changes here before you export or share the estimate.",
            primaryActionLabel = "Continue to Export"
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PROJECT_SUMMARY,
            title = "Check the snapshot",
            message = "Confirm the project, trade, and source before anything goes out.",
            supporting = "This is the quickest place to catch the wrong scope or an unfinished estimate."
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PREVIEW,
            title = "Review the output",
            message = "Flip through the preview before you share or save the file.",
            supporting = "A quick scan here catches scope and pricing mistakes before they leave the app."
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PRIMARY_ACTIONS,
            title = "Send the file",
            message = "Share the report or save the format you need.",
            supporting = "Any time the drawing or pricing changes, come back here for a fresh export.",
            primaryActionLabel = "Finish"
        )
    )
}

private fun manualWorkspaceGuidedTutorialSteps(): List<WorkspaceGuidedTutorialStep> {
    return listOf(
        MaterialsGuidedTutorialStep(
            target = MaterialsGuidedTutorialTarget.ESTIMATE_INPUTS,
            title = "Start with the numbers",
            message = "Enter the known measurements for this trade here.",
            supporting = "Manual workflow skips drawing and goes straight to estimating."
        ),
        MaterialsGuidedTutorialStep(
            target = MaterialsGuidedTutorialTarget.PRICING,
            title = "Set the price",
            message = "Unit costs, labor, markup, and tax shape the final total.",
            supporting = "Adjust pricing here before you share or save the estimate.",
            primaryActionLabel = "Continue to Export"
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PROJECT_SUMMARY,
            title = "Check the snapshot",
            message = "Confirm the project, trade, and manual input source before you send anything.",
            supporting = "This is the fastest way to catch the wrong trade or stale numbers."
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PREVIEW,
            title = "Review the output",
            message = "Use the preview to sanity-check the estimate before it leaves the app.",
            supporting = "A quick check here helps you catch pricing mistakes early."
        ),
        ExportGuidedTutorialStep(
            target = ExportGuidedTutorialTarget.PRIMARY_ACTIONS,
            title = "Send the file",
            message = "Share the report or save the format you need.",
            supporting = "Come back here any time the estimate changes.",
            primaryActionLabel = "Finish"
        )
    )
}

@Composable
internal fun GuidedTutorialBlipOverlay(
    title: String,
    message: String,
    supporting: String?,
    progress: GuidedTutorialProgress,
    targetBounds: List<Rect> = emptyList(),
    primaryActionLabel: String = "Next",
    minimumTopClearance: Dp = 12.dp,
    safeStartInset: Dp = 0.dp,
    preferBottomPlacement: Boolean = false,
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overlayBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                overlayBoundsInRoot = Rect(it.positionInRoot(), it.size.toSize())
            }
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val transition = rememberInfiniteTransition(label = "guided_tutorial_highlight")
        val highlightAlpha by transition.animateFloat(
            initialValue = 0.34f,
            targetValue = 0.88f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "guided_tutorial_highlight_alpha"
        )
        var measuredCardWidthPx by remember(progress.stepNumber) { mutableIntStateOf(0) }
        var measuredCardHeightPx by remember(progress.stepNumber) { mutableIntStateOf(0) }
        val highlightPaddingPx = with(density) { 8.dp.toPx() }
        val localizedTargetBounds = remember(targetBounds, overlayBoundsInRoot) {
            normalizeTutorialRectsToOverlaySpace(
                targetBounds = targetBounds,
                overlayBoundsInRoot = overlayBoundsInRoot
            )
        }
        val highlightRects = remember(localizedTargetBounds, highlightPaddingPx) {
            localizedTargetBounds.map { it.inflate(highlightPaddingPx) }
        }
        val anchorBounds = remember(highlightRects) { mergeTutorialRects(highlightRects) }
        val cardWidth = maxWidth.minus(24.dp).coerceAtMost(284.dp)
        val resolvedCardWidthPx = if (measuredCardWidthPx > 0) {
            measuredCardWidthPx
        } else {
            with(density) { cardWidth.roundToPx() }
        }
        val resolvedCardHeightPx = if (measuredCardHeightPx > 0) {
            measuredCardHeightPx
        } else {
            with(density) { 188.dp.roundToPx() }
        }
        val overlayOffset = remember(
            anchorBounds,
            highlightRects.size,
            maxWidth,
            maxHeight,
            resolvedCardWidthPx,
            resolvedCardHeightPx,
            minimumTopClearance,
            safeStartInset,
            preferBottomPlacement
        ) {
            guidedTutorialCardOffset(
                targetRect = anchorBounds,
                targetCount = highlightRects.size,
                viewportWidthPx = with(density) { maxWidth.roundToPx() },
                viewportHeightPx = with(density) { maxHeight.roundToPx() },
                cardWidthPx = resolvedCardWidthPx,
                cardHeightPx = resolvedCardHeightPx,
                minimumTopClearancePx = with(density) { minimumTopClearance.roundToPx() },
                safeStartPaddingPx = with(density) { safeStartInset.roundToPx() },
                preferBottomPlacement = preferBottomPlacement
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            highlightRects.forEach { rect ->
                drawRoundRect(
                    color = ReferenceBlueprintGold.copy(alpha = 0.10f),
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f),
                    style = Fill
                )
                drawRoundRect(
                    color = ReferenceBlueprintGold.copy(alpha = highlightAlpha),
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f),
                    style = Stroke(width = 4f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .padding(12.dp)
                .widthIn(max = 292.dp)
                .heightIn(min = 150.dp)
                .onGloballyPositioned {
                    measuredCardWidthPx = it.size.width
                    measuredCardHeightPx = it.size.height
                }
                .offset { overlayOffset },
            shape = MaterialTheme.shapes.large,
            color = ReferenceBlueprintPaperAlt.copy(alpha = 0.985f),
            border = BorderStroke(1.2.dp, ReferenceBlueprintBorder.copy(alpha = 0.84f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ReferenceBlueprintNavy,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Guided Tour ${progress.stepNumber}/${progress.totalSteps}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ReferenceBlueprintInk
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReferenceBlueprintInk
                    )
                    supporting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = ReferenceBlueprintMuted
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.widthIn(min = 44.dp))
                    }
                    Surface(
                        onClick = onNext,
                        shape = MaterialTheme.shapes.small,
                        color = ReferenceBlueprintNavy,
                        border = BorderStroke(1.dp, ReferenceBlueprintGold.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = primaryActionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

internal fun mergeTutorialRects(rects: List<Rect>): Rect? {
    return rects.reduceOrNull { first, second ->
        Rect(
            left = minOf(first.left, second.left),
            top = minOf(first.top, second.top),
            right = maxOf(first.right, second.right),
            bottom = maxOf(first.bottom, second.bottom)
        )
    }
}

internal fun normalizeTutorialRectsToOverlaySpace(
    targetBounds: List<Rect>,
    overlayBoundsInRoot: Rect?
): List<Rect> {
    val overlayBounds = overlayBoundsInRoot ?: return targetBounds
    return targetBounds.map { rect ->
        Rect(
            left = rect.left - overlayBounds.left,
            top = rect.top - overlayBounds.top,
            right = rect.right - overlayBounds.left,
            bottom = rect.bottom - overlayBounds.top
        )
    }
}

internal fun guidedTutorialCardOffset(
    targetRect: Rect?,
    targetCount: Int,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    cardWidthPx: Int,
    cardHeightPx: Int,
    minimumTopClearancePx: Int,
    safeStartPaddingPx: Int = 0,
    preferBottomPlacement: Boolean = false
): IntOffset {
    val horizontalPaddingPx = 12
    val verticalPaddingPx = 12
    val anchorSpacingPx = 10
    val topClearancePx = maxOf(verticalPaddingPx, minimumTopClearancePx)
    if (targetRect == null) {
        return IntOffset(
            x = horizontalPaddingPx,
            y = topClearancePx
        )
    }

    val minCardX = maxOf(horizontalPaddingPx, safeStartPaddingPx)
    val maxCardX = maxOf(
        minCardX,
        viewportWidthPx - cardWidthPx - horizontalPaddingPx
    )
    val targetCenterX = targetRect.center.x.roundToInt()
    val wideTarget = targetCount > 1 || targetRect.width >= viewportWidthPx * 0.58f
    val x = if (wideTarget) {
        if (targetCenterX >= viewportWidthPx / 2) {
            minCardX
        } else {
            maxCardX
        }
    } else {
        val desiredX = targetCenterX - (cardWidthPx / 2)
        desiredX.coerceIn(
            minCardX,
            maxCardX
        )
    }
    val lowerScreenTarget = targetRect.bottom.roundToInt() >= (viewportHeightPx * 0.52f).roundToInt()
    val shouldPinTop = wideTarget || lowerScreenTarget
    val aboveY = targetRect.top.roundToInt() - cardHeightPx - anchorSpacingPx
    val belowY = targetRect.bottom.roundToInt() + anchorSpacingPx
    val maxCardY = maxOf(
        topClearancePx,
        viewportHeightPx - cardHeightPx - verticalPaddingPx
    )
    val bottomPinnedY = maxCardY
    val canUseBottomPinnedSlot = preferBottomPlacement &&
        bottomPinnedY >= targetRect.bottom.roundToInt() + anchorSpacingPx
    val y = when {
        canUseBottomPinnedSlot -> bottomPinnedY
        preferBottomPlacement && aboveY >= topClearancePx -> aboveY
        shouldPinTop -> topClearancePx
        aboveY >= topClearancePx -> aboveY
        else -> belowY.coerceIn(
            topClearancePx,
            maxCardY
        )
    }
    return IntOffset(x = x, y = y)
}

private fun Rect.inflate(padding: Float): Rect {
    return Rect(
        left = left - padding,
        top = top - padding,
        right = right + padding,
        bottom = bottom + padding
    )
}

private val GUIDED_WALL_DEMO_START = PointMm(x = -4_200L, y = -1_100L)
private val GUIDED_WALL_DEMO_END = PointMm(x = 3_600L, y = 1_850L)
private val GUIDED_OPENING_DEMO_WALL = BlueprintGuidedTutorialDemoWall(
    start = PointMm(x = -4_600L, y = -2_500L),
    end = PointMm(x = 4_600L, y = -2_500L)
)
private val GUIDED_OPENING_DEMO_POINTER = PointMm(x = 300L, y = -2_500L)
