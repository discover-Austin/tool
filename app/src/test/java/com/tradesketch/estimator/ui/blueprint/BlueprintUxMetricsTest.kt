package com.tradesketch.estimator.ui.blueprint

import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintUxMetricsTest {

    @Test
    fun `screen opened metric key is emitted only when the joystick mode changes`() {
        assertEquals(
            "blueprint_screen_opened_joystick",
            blueprintScreenOpenedMetricKey(
                lastTrackedModeSuffix = null,
                controlMode = BlueprintControlMode.JOYSTICK
            )
        )
        assertEquals(
            null,
            blueprintScreenOpenedMetricKey(
                lastTrackedModeSuffix = BlueprintControlMode.JOYSTICK.metricSuffix,
                controlMode = BlueprintControlMode.JOYSTICK
            )
        )
    }

    @Test
    fun `metric key uses joystick suffix for all blueprint actions`() {
        val controlMode = blueprintControlMode()

        assertEquals(
            "blueprint_screen_opened_joystick",
            blueprintMetricKey(BlueprintMetricAction.SCREEN_OPENED, controlMode)
        )
        assertEquals(
            "blueprint_wall_placed_joystick",
            blueprintMetricKey(BlueprintMetricAction.WALL_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_box_placed_joystick",
            blueprintMetricKey(BlueprintMetricAction.BOX_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_opening_placed_joystick",
            blueprintMetricKey(BlueprintMetricAction.OPENING_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_clear_all_joystick",
            blueprintMetricKey(BlueprintMetricAction.CLEAR_ALL, controlMode)
        )
    }
}
