package com.tradesketch.estimator.ui.screens

import com.tradesketch.estimator.domain.model.TakeoffInputMode
import com.tradesketch.estimator.ui.viewmodel.ExportScopeMode
import com.tradesketch.estimator.ui.viewmodel.TakeoffType
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportScreenStatusTest {

    @Test
    fun `status note prefers selected trade geometry when nothing is measured yet`() {
        assertEquals(
            "This trade has blueprint geometry, but there are no measured quantities yet.",
            exportStatusNote(
                inputMode = TakeoffInputMode.BLUEPRINT,
                exportScopeMode = ExportScopeMode.SINGLE_TRADE,
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
                exportScopeMode = ExportScopeMode.SINGLE_TRADE,
                hasMeasuredQuantities = false,
                hasBlueprintGeometry = true,
                hasSelectedTradeGeometry = false
            )
        )
    }

    @Test
    fun `status note uses included-trades wording for all included scope`() {
        assertEquals(
            "This project has geometry in the included trades, but there are no measured quantities yet.",
            exportStatusNote(
                inputMode = TakeoffInputMode.BLUEPRINT,
                exportScopeMode = ExportScopeMode.ALL_TRADES,
                hasMeasuredQuantities = false,
                hasBlueprintGeometry = true,
                hasSelectedTradeGeometry = true
            )
        )
    }

    @Test
    fun `trade menu favors populated trades and keeps the current manual selection visible`() {
        assertEquals(
            listOf(TakeoffType.CONCRETE, TakeoffType.PAINT, TakeoffType.DRYWALL),
            exportTradeMenuTypes(
                presentTradeLabels = listOf("Concrete", "Paint"),
                selectedType = TakeoffType.DRYWALL
            )
        )
    }
}
