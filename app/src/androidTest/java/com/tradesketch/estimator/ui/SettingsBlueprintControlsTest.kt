package com.tradesketch.estimator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.ui.screens.BlueprintControlsCard
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBlueprintControlsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun blueprintControls_alwaysShowsJoystickTuning() {
        var settings by mutableStateOf(
            Settings.DEFAULT.copy(
                blueprintJoystickSensitivity = 1.31f,
                blueprintJoystickDeadzone = 0.14f
            )
        )

        rule.setContent {
            TradeSketchTheme {
                BlueprintControlsCard(
                    settings = settings,
                    useMetric = false,
                    onUpdateBlueprintSnapDefaults = { grid, endpoint, midpoint, angle, closure, threshold ->
                        settings = settings.updatedBlueprintSnap(
                            grid = grid,
                            endpoint = endpoint,
                            midpoint = midpoint,
                            angle = angle,
                            closure = closure,
                            threshold = threshold
                        )
                    },
                    onUpdateBlueprintControlDefaults = { sensitivity, deadzone, cursorVisible, cursorScale ->
                        settings = settings.updatedBlueprintControls(
                            joystickSensitivity = sensitivity,
                            joystickDeadzone = deadzone,
                            cursorVisible = cursorVisible,
                            cursorScale = cursorScale
                        )
                    },
                    onHapticTap = {}
                )
            }
        }

        assertTextPresent("Joystick sensitivity: 1.31x")
        assertTextPresent("Joystick deadzone: 14%")
        assertTextAbsent("Touch mode")
        assertTextAbsent("Dual joysticks")
    }

    @Test
    fun blueprintControls_hidesModeSectionButKeepsJoystickDefaultsVisible() {
        var settings by mutableStateOf(
            Settings.DEFAULT.copy(
                blueprintJoystickSensitivity = 1.42f,
                blueprintJoystickDeadzone = 0.18f
            )
        )

        rule.setContent {
            TradeSketchTheme {
                BlueprintControlsCard(
                    settings = settings,
                    useMetric = false,
                    onUpdateBlueprintSnapDefaults = { grid, endpoint, midpoint, angle, closure, threshold ->
                        settings = settings.updatedBlueprintSnap(
                            grid = grid,
                            endpoint = endpoint,
                            midpoint = midpoint,
                            angle = angle,
                            closure = closure,
                            threshold = threshold
                        )
                    },
                    onUpdateBlueprintControlDefaults = { sensitivity, deadzone, cursorVisible, cursorScale ->
                        settings = settings.updatedBlueprintControls(
                            joystickSensitivity = sensitivity,
                            joystickDeadzone = deadzone,
                            cursorVisible = cursorVisible,
                            cursorScale = cursorScale
                        )
                    },
                    onHapticTap = {}
                )
            }
        }

        assertTextPresent("Joystick sensitivity: 1.42x")
        assertTextPresent("Joystick deadzone: 18%")
        assertTextAbsent("Control mode")
    }

    private fun assertTextPresent(value: String) {
        assertTrue(
            "Expected at least one node with text '$value'.",
            rule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun assertTextAbsent(value: String) {
        assertTrue(
            "Expected no nodes with text '$value'.",
            rule.onAllNodesWithText(value).fetchSemanticsNodes().isEmpty()
        )
    }
}

private fun Settings.updatedBlueprintSnap(
    grid: Boolean? = null,
    endpoint: Boolean? = null,
    midpoint: Boolean? = null,
    angle: Boolean? = null,
    closure: Boolean? = null,
    threshold: Double? = null
): Settings {
    return copy(
        blueprintSnapGridEnabled = grid ?: blueprintSnapGridEnabled,
        blueprintSnapEndpointEnabled = endpoint ?: blueprintSnapEndpointEnabled,
        blueprintSnapMidpointEnabled = midpoint ?: blueprintSnapMidpointEnabled,
        blueprintSnapAngleEnabled = angle ?: blueprintSnapAngleEnabled,
        blueprintSnapClosureEnabled = closure ?: blueprintSnapClosureEnabled,
        blueprintSnapThresholdFeet = threshold ?: blueprintSnapThresholdFeet
    )
}

private fun Settings.updatedBlueprintControls(
    joystickSensitivity: Float? = null,
    joystickDeadzone: Float? = null,
    cursorVisible: Boolean? = null,
    cursorScale: Float? = null
): Settings {
    return copy(
        blueprintJoystickSensitivity = joystickSensitivity ?: blueprintJoystickSensitivity,
        blueprintJoystickDeadzone = joystickDeadzone ?: blueprintJoystickDeadzone,
        blueprintCursorVisible = cursorVisible ?: blueprintCursorVisible,
        blueprintCursorScale = cursorScale ?: blueprintCursorScale
    )
}
