package com.tradesketch.estimator.ui.blueprint

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.tradesketch.estimator.ui.viewmodel.BlueprintDraftTool
import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintControlTutorialTest {

    @Test
    fun `touch mode tutorial steps spotlight touch controls in order`() {
        val steps = blueprintControlTutorialSteps(dualJoysticksEnabled = false)

        assertEquals(
            listOf(
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
                BlueprintControlTutorialTarget.CANVAS,
                BlueprintControlTutorialTarget.TOUCH_SELECT_BUTTON,
                BlueprintControlTutorialTarget.TOUCH_DRAW_BUTTON,
                BlueprintControlTutorialTarget.TOUCH_GRAB_BUTTON,
                BlueprintControlTutorialTarget.TOUCH_CANCEL_BUTTON,
                BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
                BlueprintControlTutorialTarget.EDGE_DIALS,
                BlueprintControlTutorialTarget.TOP_START_STACK,
                BlueprintControlTutorialTarget.TOP_END_STACK,
                BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                BlueprintControlTutorialTarget.GRID_SCALE_BADGE,
                BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON
            ),
            steps.map(BlueprintControlTutorialStep::target)
        )
    }

    @Test
    fun `joystick mode tutorial steps spotlight joystick controls in order`() {
        val steps = blueprintControlTutorialSteps(dualJoysticksEnabled = true)

        assertEquals(
            listOf(
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
                BlueprintControlTutorialTarget.CANVAS,
                BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
                BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
                BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
                BlueprintControlTutorialTarget.EDGE_DIALS,
                BlueprintControlTutorialTarget.TOP_START_STACK,
                BlueprintControlTutorialTarget.TOP_END_STACK,
                BlueprintControlTutorialTarget.FLOOR_SWITCHER,
                BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON
            ),
            steps.map(BlueprintControlTutorialStep::target)
        )
    }

    @Test
    fun `opening panel tutorial step resolves to the matching placement tool`() {
        val openingStep = blueprintControlTutorialSteps(dualJoysticksEnabled = false)
            .first { it.target == BlueprintControlTutorialTarget.DOORS_BUTTON }

        assertEquals(OpeningPanelType.DOORS, openingStep.demoOpeningPanel)
        assertEquals(BlueprintDraftTool.PLACE_DOOR, openingStep.resolvedDemoTool())
        assertEquals(BlueprintDraftTool.PLACE_DOOR, openingStep.resolvedTutorialTool())
    }

    @Test
    fun `non-opening tutorial steps resolve to a neutral draw tool`() {
        val paramsStep = blueprintControlTutorialSteps(dualJoysticksEnabled = false)
            .first { it.target == BlueprintControlTutorialTarget.PARAMS_BUTTON }
        val helpStep = blueprintControlTutorialSteps(dualJoysticksEnabled = false)
            .first { it.target == BlueprintControlTutorialTarget.HELP_BUTTON }

        assertEquals(BlueprintDraftTool.DRAW_WALL, paramsStep.resolvedTutorialTool())
        assertEquals(BlueprintDraftTool.DRAW_WALL, helpStep.resolvedTutorialTool())
    }

    @Test
    fun `touch grab tutorial step resolves to select mode`() {
        val grabStep = blueprintControlTutorialSteps(dualJoysticksEnabled = false)
            .first { it.target == BlueprintControlTutorialTarget.TOUCH_GRAB_BUTTON }

        assertEquals(BlueprintDraftTool.SELECT, grabStep.resolvedTutorialTool())
    }

    @Test
    fun `tooltip pinned top respects occupied top clearance`() {
        val offset = blueprintTutorialTooltipOffset(
            targetRect = Rect(left = 40f, top = 1_320f, right = 160f, bottom = 1_440f),
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 320,
            cardHeightPx = 260,
            horizontalPaddingPx = 12,
            verticalPaddingPx = 12,
            minimumTopClearancePx = 180,
            anchorSpacingPx = 14,
            compactWindow = false
        )

        assertEquals(IntOffset(x = 12, y = 180), offset)
    }

    @Test
    fun `canvas tutorial step prefers top pinned side anchored tooltip`() {
        val canvasStep = blueprintControlTutorialSteps(dualJoysticksEnabled = false)
            .first { it.target == BlueprintControlTutorialTarget.CANVAS }

        assertEquals(true, canvasStep.hasLiveExample())
        assertEquals(true, canvasStep.prefersPinnedTopTooltip())
        assertEquals(true, canvasStep.prefersSideAnchoredTooltip())
    }

    @Test
    fun `wide demo targets anchor tutorial card to the side`() {
        val offset = blueprintTutorialTooltipOffset(
            targetRect = Rect(left = 0f, top = 260f, right = 1080f, bottom = 1760f),
            viewportWidthPx = 1080,
            viewportHeightPx = 1920,
            cardWidthPx = 300,
            cardHeightPx = 240,
            horizontalPaddingPx = 12,
            verticalPaddingPx = 12,
            minimumTopClearancePx = 160,
            anchorSpacingPx = 14,
            compactWindow = false,
            preferPinnedTop = true,
            preferSidePlacement = true
        )

        assertEquals(IntOffset(x = 12, y = 160), offset)
    }
}
