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

    @Test
    fun exportFormats_shareConsistentEstimateIdentityWhenProvided() {
        val project = Project(
            id = "project-1",
            name = "South Garage"
        )
        val settings = Settings()
        val result = TakeoffResult(
            items = listOf(TakeoffLine(name = "Paint", quantity = 1.0, unit = "gallons")),
            totalCost = null
        )
        val generatedAt = 1_735_732_800_000L
        val estimateId = "TS-20250101-PROJECT1"

        val text = ExportFormatter.formatAsText(
            project = project,
            settings = settings,
            takeoffType = "Paint",
            result = result,
            generatedAtMillis = generatedAt,
            estimateId = estimateId
        )
        val csv = ExportFormatter.formatAsCSV(
            project = project,
            settings = settings,
            takeoffType = "Paint",
            result = result,
            generatedAtMillis = generatedAt,
            estimateId = estimateId
        )
        val json = ExportFormatter.formatAsJson(
            project = project,
            settings = settings,
            takeoffType = "Paint",
            result = result,
            generatedAtMillis = generatedAt,
            estimateId = estimateId
        )

        assertTrue(text.contains("Estimate ID: $estimateId"))
        assertTrue(csv.contains("\"$estimateId\""))
        assertTrue(json.contains("\"estimateId\": \"$estimateId\""))
        val formattedDate = Formatters.formatDate(generatedAt)
        assertTrue(text.contains("Date: $formattedDate"))
        assertTrue(csv.contains("\"$formattedDate\""))
        assertTrue(json.contains("\"generatedAt\": \"$formattedDate\""))
    }
}
