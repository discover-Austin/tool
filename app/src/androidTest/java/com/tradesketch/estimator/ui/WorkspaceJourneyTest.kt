package com.tradesketch.estimator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradesketch.estimator.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceJourneyTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun workspace_allows_tab_navigation_and_saved_projects_access() {
        ensureWorkspaceReady()

        if (rule.hasContentDescription("Saved")) {
            rule.onNodeWithContentDescription("Saved").performClick()
            rule.assertAnyTextDisplayed("Saved Projects")
            rule.onNodeWithText("Close").performClick()
        }

        rule.onNodeWithContentDescription("Materials").performClick()
        rule.assertAnyTextDisplayed("Input Method")

        rule.onNodeWithContentDescription("Export").performClick()
        rule.assertAnyTextDisplayed("Estimate Type")

        rule.onNodeWithContentDescription("Settings/About").performClick()
        rule.assertAnyTextDisplayed("Settings")
    }

    private fun ensureWorkspaceReady() {
        rule.waitForIdle()

        if (rule.hasText("Begin")) {
            rule.onNodeWithText("Begin").performClick()
        }
        if (rule.hasText("Project Setup 1 of 2")) {
            rule.onNodeWithText("Project name").performTextInput("QA Smoke Project")
            rule.onNodeWithText("Continue").performClick()
        }
        if (rule.hasText("Project Setup 2 of 2")) {
            rule.onNodeWithText("Drywall").performClick()
            rule.onNodeWithText("Open Project").performClick()
        }

        dismissInteractiveTourIfShown()

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.hasContentDescription("Blueprint") || rule.hasContentDescription("Saved")
        }
    }

    private fun dismissInteractiveTourIfShown() {
        if (rule.hasText("Interactive Tour") && rule.hasText("Skip")) {
            rule.onNodeWithText("Skip").performClick()
            rule.waitForIdle()
        }
    }

    private fun AndroidComposeTestRule<*, *>.hasText(value: String): Boolean {
        return onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
    }

    private fun AndroidComposeTestRule<*, *>.hasContentDescription(value: String): Boolean {
        return onAllNodesWithContentDescription(value).fetchSemanticsNodes().isNotEmpty()
    }

    private fun AndroidComposeTestRule<*, *>.assertAnyTextDisplayed(value: String) {
        assertTrue(
            "Expected at least one visible node with text '$value'.",
            onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
