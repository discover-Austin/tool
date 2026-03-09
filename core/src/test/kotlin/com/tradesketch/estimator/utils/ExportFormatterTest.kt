package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import kotlin.test.Test
import kotlin.test.assertTrue

class ExportFormatterTest {

    @Test
    fun formatAsCSV_escapesQuotesNewlinesAndFormulaPrefixes() {
        val project = Project(
            id = "project-1",
            name = "=SUM(A1:A2)"
        )
        val settings = Settings(
            businessName = "Acme \"Prime\"",
            businessAddress = "Line1\nLine2"
        )
        val result = TakeoffResult(
            items = listOf(
                TakeoffLine(
                    name = "Paint \"Premium\", Exterior",
                    quantity = 2.0,
                    unit = "gallons",
                    unitCost = 42.5
                )
            ),
            totalCost = 85.0,
            materialSubtotal = 85.0
        )

        val csv = ExportFormatter.formatAsCSV(
            project = project,
            settings = settings,
            takeoffType = "Paint",
            result = result
        )

        assertTrue(csv.contains("\"Acme \"\"Prime\"\"\""))
        assertTrue(csv.contains("\"Line1\nLine2\""))
        assertTrue(csv.contains("\"Paint \"\"Premium\"\", Exterior\""))
        assertTrue(csv.contains("\"'=SUM(A1:A2)\""))
    }
}

