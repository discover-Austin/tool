package com.tradesketch.estimator.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.ui.screens.ProjectTypePlateOption
import com.tradesketch.estimator.ui.screens.ProjectTypePlateScreen
import com.tradesketch.estimator.ui.screens.WelcomeScreenPro
import com.tradesketch.estimator.ui.theme.TradeSketchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeAndProjectTypeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun welcomeSavedProjectsCard_showsEmptyStateWhenNoProjectsExist() {
        rule.setContent {
            TradeSketchTheme {
                WelcomeScreenPro(
                    onBegin = {},
                    savedProjects = emptyList()
                )
            }
        }

        rule.onNodeWithText("No projects saved yet.").assertIsDisplayed()
    }

    @Test
    fun welcomeSavedProjectsCard_listsProjectsAndOpensSelection() {
        var openedProjectId: String? = null
        val projects = listOf(
            Project(id = "project-1", name = "Kitchen Remodel"),
            Project(id = "project-2", name = "Garage Slab")
        )

        rule.setContent {
            TradeSketchTheme {
                WelcomeScreenPro(
                    onBegin = {},
                    savedProjects = projects,
                    onOpenSavedProject = { openedProjectId = it }
                )
            }
        }

        rule.onNodeWithText("Garage Slab").performClick()

        rule.runOnIdle {
            assertEquals("project-2", openedProjectId)
        }
    }

    @Test
    fun projectTypePlateScreen_usesOneTapCardsWithoutContinueButton() {
        var selectedOption: ProjectTypePlateOption? = null

        rule.setContent {
            TradeSketchTheme {
                ProjectTypePlateScreen(
                    onSelectOption = { selectedOption = it },
                    onBack = {}
                )
            }
        }

        rule.onAllNodesWithText("Continue").assertCountEquals(0)
        rule.onNodeWithText("Manual Entry").performClick()

        rule.runOnIdle {
            assertEquals(ProjectTypePlateOption.MANUAL, selectedOption)
        }
    }
}
