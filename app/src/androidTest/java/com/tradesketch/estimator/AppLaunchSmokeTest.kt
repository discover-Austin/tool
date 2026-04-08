package com.tradesketch.estimator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_reachesEitherOnboardingOrWorkspaceFlow() {
        rule.waitForIdle()

        if (rule.hasText("Begin")) {
            rule.onNodeWithText("Begin").performClick()
            rule.onNodeWithText("Project Setup 1 of 2").assertIsDisplayed()
            return
        }

        if (rule.hasContentDescription("Open")) {
            rule.onNodeWithContentDescription("Open").performClick()
            rule.onNodeWithText("Saved Projects").assertIsDisplayed()
            return
        }

        assertTrue(
            "Expected onboarding or workspace markers to be visible.",
            rule.hasText("Project Setup 1 of 2") ||
                rule.hasText("Blueprint") ||
                rule.hasText("Settings")
        )
    }

    private fun AndroidComposeTestRule<*, *>.hasText(value: String): Boolean {
        return onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
    }

    private fun AndroidComposeTestRule<*, *>.hasContentDescription(value: String): Boolean {
        return onAllNodesWithContentDescription(value).fetchSemanticsNodes().isNotEmpty()
    }
}
