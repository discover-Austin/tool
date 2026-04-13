package com.tradesketch.estimator

import org.junit.Assert.assertEquals
import org.junit.Test

class RootStageRoutingTest {

    @Test
    fun `first run without dismissal opens greeting`() {
        val stage = resolveRootStage(
            currentStage = RootStage.WELCOME,
            debugStageOverride = null,
            debugWorkspaceOverride = false,
            firstRun = true,
            firstOpenGreetingDismissed = false,
            forceTutorial = false,
            forceWorkspace = false
        )

        assertEquals(RootStage.GREETING, stage)
    }

    @Test
    fun `ritual stays active while first run state settles`() {
        val stage = resolveRootStage(
            currentStage = RootStage.RITUAL,
            debugStageOverride = null,
            debugWorkspaceOverride = false,
            firstRun = false,
            firstOpenGreetingDismissed = true,
            forceTutorial = false,
            forceWorkspace = false
        )

        assertEquals(RootStage.RITUAL, stage)
    }

    @Test
    fun `tutorial stage persists after project creation until dismissed`() {
        val stage = resolveRootStage(
            currentStage = RootStage.TUTORIAL,
            debugStageOverride = null,
            debugWorkspaceOverride = false,
            firstRun = false,
            firstOpenGreetingDismissed = true,
            forceTutorial = false,
            forceWorkspace = false
        )

        assertEquals(RootStage.TUTORIAL, stage)
    }

    @Test
    fun `workspace stage persists after tutorial completion`() {
        val stage = resolveRootStage(
            currentStage = RootStage.WORKSPACE,
            debugStageOverride = null,
            debugWorkspaceOverride = false,
            firstRun = false,
            firstOpenGreetingDismissed = true,
            forceTutorial = false,
            forceWorkspace = false
        )

        assertEquals(RootStage.WORKSPACE, stage)
    }
}
