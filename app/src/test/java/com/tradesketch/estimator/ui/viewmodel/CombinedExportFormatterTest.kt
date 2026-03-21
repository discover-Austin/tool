package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CombinedExportFormatterTest {

    private val project = Project(
        id = "project-1",
        name = "Shared Blueprint Test"
    )

    @Test
    fun `csv only includes passed trade sections`() {
        val csv = CombinedExportFormatter.formatAsCSV(
            project = project,
            settings = Settings.DEFAULT,
            sections = listOf(
                CombinedExportSection(
                    takeoffTypeLabel = "Concrete",
                    result = TakeoffResult(
                        items = listOf(TakeoffLine(name = "Concrete", quantity = 3.5, unit = "cubic yards")),
                        totalCost = 577.5,
                        materialSubtotal = 577.5
                    )
                )
            ),
            generatedAtMillis = 0L,
            estimateId = "EST-1"
        )

        assertContains(csv, "\"Concrete\",\"Concrete\",3.5,\"cubic yards\"")
        assertFalse(csv.contains("Drywall"))
        assertFalse(csv.contains("Paint"))
    }

    @Test
    fun `csv omits zero-dollar totals when a present trade has no measured quantities`() {
        val csv = CombinedExportFormatter.formatAsCSV(
            project = project,
            settings = Settings.DEFAULT,
            sections = listOf(
                CombinedExportSection(
                    takeoffTypeLabel = "Concrete",
                    result = TakeoffResult(
                        items = emptyList(),
                        totalCost = 0.0,
                        materialSubtotal = 0.0,
                        laborCost = 0.0,
                        markupCost = 0.0,
                        taxCost = 0.0
                    )
                )
            ),
            generatedAtMillis = 0L,
            estimateId = "EST-2"
        )

        assertContains(csv, "\"Concrete\",\"No measured quantities yet\",,,,")
        assertFalse(csv.contains("\"Concrete\",\"TOTAL\""))
        assertFalse(csv.contains("$0.00"))
    }
}
