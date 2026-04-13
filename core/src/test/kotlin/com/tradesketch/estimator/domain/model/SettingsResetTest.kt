package com.tradesketch.estimator.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsResetTest {

    @Test
    fun resettableDefaults_preservesOnboardingAndTutorialProgress() {
        val current = Settings.DEFAULT.copy(
            firstRun = false,
            hasCompletedTradeOnboarding = true,
            hasCompletedAppTutorial = true,
            businessName = "Acme Interiors"
        )

        val reset = current.resettableDefaults()

        assertFalse(reset.firstRun)
        assertTrue(reset.hasCompletedTradeOnboarding)
        assertTrue(reset.hasCompletedAppTutorial)
    }

    @Test
    fun resettableDefaults_restoresEditableValuesToFactoryDefaults() {
        val current = Settings.DEFAULT.copy(
            reducedMotionEnabled = false,
            useMetric = true,
            businessName = "Acme Interiors",
            businessPhone = "555-111-2222",
            defaultWastePercent = 18.0
        )

        val reset = current.resettableDefaults()

        assertEquals(Settings.DEFAULT.reducedMotionEnabled, reset.reducedMotionEnabled)
        assertEquals(Settings.DEFAULT.useMetric, reset.useMetric)
        assertEquals(Settings.DEFAULT.businessName, reset.businessName)
        assertEquals(Settings.DEFAULT.businessPhone, reset.businessPhone)
        assertEquals(Settings.DEFAULT.defaultWastePercent, reset.defaultWastePercent)
    }
}
