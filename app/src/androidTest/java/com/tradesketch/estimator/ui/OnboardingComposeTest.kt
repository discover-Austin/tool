package com.tradesketch.estimator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.ui.screens.ProjectRitualScreen1_Name
import com.tradesketch.estimator.ui.screens.ProjectRitualScreen2_EstimateType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingComposeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun projectNameStep_requiresNameBeforeContinueEnabled() {
        var projectName by mutableStateOf("")
        rule.setContent {
            ProjectRitualScreen1_Name(
                projectName = projectName,
                onProjectNameChange = { value -> projectName = value },
                onContinue = {}
            )
        }

        rule.onNodeWithText("Continue").assertIsNotEnabled()
        rule.onNodeWithText("Project name").performTextInput("Kitchen Remodel")
        rule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun estimateTypeStep_enablesOpenProjectAfterSelection() {
        var selectedTrade by mutableStateOf<PrimaryTrade?>(null)
        rule.setContent {
            ProjectRitualScreen2_EstimateType(
                selectedTrade = selectedTrade,
                onSelectTrade = { trade -> selectedTrade = trade },
                onComplete = {}
            )
        }

        rule.onNodeWithText("Open Project").assertIsNotEnabled()
        rule.onNodeWithText("Drywall").performClick()
        rule.onNodeWithText("Open Project").assertIsEnabled()
    }
}

