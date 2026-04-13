package com.tradesketch.estimator.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class WelcomeScreenHeroModeTest {

    @Test
    fun `cold start hero uses the faster-start headline`() {
        assertEquals(
            "Start a new estimate.",
            welcomeHeroHeadline(WelcomeHeroMode.COLD_START)
        )
    }

    @Test
    fun `returning home hero focuses on resuming work`() {
        assertEquals(
            "Back to your projects.",
            welcomeHeroHeadline(WelcomeHeroMode.RETURNING_HOME)
        )
    }
}
