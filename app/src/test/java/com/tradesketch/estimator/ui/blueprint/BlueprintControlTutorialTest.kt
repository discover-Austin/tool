package com.tradesketch.estimator.ui.blueprint

import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintControlTutorialTest {

    @Test
    fun `touch mode tutorial steps spotlight touch controls in order`() {
        val steps = blueprintControlTutorialSteps(dualJoysticksEnabled = false)

        assertEquals(
            listOf(
                BlueprintControlTutorialTarget.BOTTOM_RAIL,
                BlueprintControlTutorialTarget.TOUCH_LEFT_TOOLS,
                BlueprintControlTutorialTarget.TOUCH_RIGHT_TOOLS,
                BlueprintControlTutorialTarget.TOUCH_CENTER_CONTROLS,
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
                BlueprintControlTutorialTarget.JOYSTICK_LEFT_PAD,
                BlueprintControlTutorialTarget.JOYSTICK_RIGHT_PAD,
                BlueprintControlTutorialTarget.JOYSTICK_CENTER_CONTROLS,
                BlueprintControlTutorialTarget.CLEAR_ALL_BUTTON
            ),
            steps.map(BlueprintControlTutorialStep::target)
        )
    }
}
