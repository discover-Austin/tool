package com.tradesketch.estimator.ui.blueprint

import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintUxMetricsTest {

    @Test
    fun `screen opened metric key is emitted only when the control mode changes`() {
        assertEquals(
            "blueprint_screen_opened_touch",
            blueprintScreenOpenedMetricKey(
                lastTrackedModeSuffix = null,
                controlMode = BlueprintControlMode.TOUCH
            )
        )
        assertEquals(
            null,
            blueprintScreenOpenedMetricKey(
                lastTrackedModeSuffix = BlueprintControlMode.TOUCH.metricSuffix,
                controlMode = BlueprintControlMode.TOUCH
            )
        )
        assertEquals(
            "blueprint_screen_opened_joystick",
            blueprintScreenOpenedMetricKey(
                lastTrackedModeSuffix = BlueprintControlMode.TOUCH.metricSuffix,
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
    fun `metric key uses touch suffix for all blueprint actions`() {
        val controlMode = blueprintControlMode(dualJoysticksEnabled = false)

        assertEquals(
            "blueprint_screen_opened_touch",
            blueprintMetricKey(BlueprintMetricAction.SCREEN_OPENED, controlMode)
        )
        assertEquals(
            "blueprint_wall_placed_touch",
            blueprintMetricKey(BlueprintMetricAction.WALL_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_box_placed_touch",
            blueprintMetricKey(BlueprintMetricAction.BOX_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_opening_placed_touch",
            blueprintMetricKey(BlueprintMetricAction.OPENING_PLACED, controlMode)
        )
        assertEquals(
            "blueprint_clear_all_touch",
            blueprintMetricKey(BlueprintMetricAction.CLEAR_ALL, controlMode)
        )
    }

    @Test
    fun `metric key uses joystick suffix for all blueprint actions`() {
        val controlMode = blueprintControlMode(dualJoysticksEnabled = true)

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
