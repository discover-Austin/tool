package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.PrimaryTrade
import com.tradesketch.estimator.domain.model.TakeoffScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarterProjectFactoryTest {

    @Test
    fun createStarterProjectForTrade_createsBlankTradeScopedProject() {
        val project = createStarterProjectForTrade(
            trade = PrimaryTrade.CONCRETE,
            name = "My Concrete Project"
        )

        assertEquals("My Concrete Project", project.name)
        assertEquals(TakeoffScope.CONCRETE, project.takeoffSession.selectedScope)
        assertEquals(TakeoffPlaybook.BALANCED.name, project.takeoffSession.selectedPlaybook)
        assertTrue(project.blueprintDocument.walls.isEmpty())
        assertTrue(project.blueprintDocument.rooms.isEmpty())
        assertTrue(project.blueprintDocument.openings.isEmpty())
    }

    @Test
    fun starterProjectNameForTrade_returnsFriendlyBlankProjectNames() {
        assertEquals("My Drywall Project", starterProjectNameForTrade(PrimaryTrade.DRYWALL))
        assertEquals("My Project", starterProjectNameForTrade(PrimaryTrade.MULTI))
    }
}
