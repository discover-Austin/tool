package com.tradesketch.estimator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityRailBehaviorTest {

    @Test
    fun `manual rail expand arms auto collapse`() {
        assertTrue(nextRailAutoCollapseArmedAfterManualToggle(wasCollapsed = true))
    }

    @Test
    fun `manual rail collapse leaves auto collapse disarmed`() {
        assertFalse(nextRailAutoCollapseArmedAfterManualToggle(wasCollapsed = false))
    }

    @Test
    fun `auto collapse only runs while expanded armed and not in tutorial`() {
        assertTrue(
            shouldAutoCollapseRail(
                railCollapsed = false,
                railAutoCollapseArmed = true,
                tutorialMode = false,
                guidedTutorialActive = false
            )
        )
        assertFalse(
            shouldAutoCollapseRail(
                railCollapsed = true,
                railAutoCollapseArmed = true,
                tutorialMode = false,
                guidedTutorialActive = false
            )
        )
        assertFalse(
            shouldAutoCollapseRail(
                railCollapsed = false,
                railAutoCollapseArmed = false,
                tutorialMode = false,
                guidedTutorialActive = false
            )
        )
        assertFalse(
            shouldAutoCollapseRail(
                railCollapsed = false,
                railAutoCollapseArmed = true,
                tutorialMode = true,
                guidedTutorialActive = false
            )
        )
        assertFalse(
            shouldAutoCollapseRail(
                railCollapsed = false,
                railAutoCollapseArmed = true,
                tutorialMode = false,
                guidedTutorialActive = true
            )
        )
    }
}
