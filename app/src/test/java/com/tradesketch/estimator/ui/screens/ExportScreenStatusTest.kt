package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.domain.model.TakeoffInputMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportScreenStatusTest {

    @Test
    fun `status note prefers selected trade geometry when nothing is measured yet`() {
        assertEquals(
            "This trade has blueprint geometry, but there are no measured quantities yet.",
            exportStatusNote(
                inputMode = TakeoffInputMode.BLUEPRINT,
                hasMeasuredQuantities = false,
                hasBlueprintGeometry = true,
                hasSelectedTradeGeometry = true
            )
        )
    }

    @Test
    fun `status note falls back to project geometry when selected trade has none`() {
        assertEquals(
            "This project has geometry, but nothing matches the selected trade yet.",
            exportStatusNote(
                inputMode = TakeoffInputMode.BLUEPRINT,
                hasMeasuredQuantities = false,
                hasBlueprintGeometry = true,
                hasSelectedTradeGeometry = false
            )
        )
    }
}
