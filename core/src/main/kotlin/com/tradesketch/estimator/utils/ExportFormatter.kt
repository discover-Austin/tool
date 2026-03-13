package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult

/**
 * Formats data for export (CSV, PDF, Share, Copy).
 */
object ExportFormatter {
    
    private const val DISCLAIMER = "ESTIMATE ONLY - Verify quantities, measurements, and pricing with actual site conditions, local building codes, and material suppliers before purchasing or starting work."
    
    /**
     * Format takeoff results as plain text for sharing or copying.
     */
    fun formatAsText(
        project: Project,
        settings: Settings,
        takeoffType: String,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val header = buildBusinessHeader(settings)
        val timestamp = Formatters.formatDate(generatedAtMillis)
        return buildString {
            appendLine(header.name)
            header.contactLines.forEach { appendLine(it) }
            appendLine("=" .repeat(50))
            appendLine()
            appendLine("Project: ${project.name}")
            appendLine("Estimate ID: $estimateId")
            appendLine("Takeoff Type: $takeoffType")
            appendLine("Date: $timestamp")
            appendLine()
            appendLine("QUANTITIES")
            appendLine("-" .repeat(50))
            result.items.forEach { item ->
                appendLine("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                item.unitCost?.let { cost ->
                    appendLine("  @ ${Formatters.formatMoney(cost)} each")
                }
                item.extendedCost?.let { ext ->
                    appendLine("  Total: ${Formatters.formatMoney(ext)}")
                }
            }
            appendLine()
            result.materialSubtotal?.let { subtotal ->
                appendLine("Materials: ${Formatters.formatMoney(subtotal)}")
            }
            result.laborCost?.let { labor ->
                appendLine("Labor: ${Formatters.formatMoney(labor)}")
            }
            result.markupCost?.let { markup ->
                appendLine("Markup: ${Formatters.formatMoney(markup)}")
            }
            result.taxCost?.let { tax ->
                appendLine("Tax: ${Formatters.formatMoney(tax)}")
            }
            result.totalCost?.let { total ->
                appendLine("TOTAL COST: ${Formatters.formatMoney(total)}")
                appendLine()
            }
            appendLine("-" .repeat(50))
            appendLine(DISCLAIMER)
        }
    }
    
    /**
     * Format takeoff results as CSV.
     */
    fun formatAsCSV(
        project: Project,
        settings: Settings,
        takeoffType: String,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val header = buildBusinessHeader(settings)
        val timestamp = Formatters.formatDate(generatedAtMillis)
        return buildString {
            appendLine("Company,Phone,Email,Address,License")
            appendLine(
                listOf(
                    header.name,
                    settings.businessPhone,
                    settings.businessEmail,
                    settings.businessAddress,
                    settings.businessLicense
                ).joinToString(separator = ",") { cell -> csvCell(cell) }
            )
            appendLine()
            appendLine("Project,Estimate ID,Takeoff Type,Date")
            appendLine(
                listOf(
                    project.name,
                    estimateId,
                    takeoffType,
                    timestamp
                ).joinToString(separator = ",") { cell -> csvCell(cell) }
            )
            appendLine()
            appendLine("Item,Quantity,Unit,Unit Cost,Extended Cost")
            result.items.forEach { item ->
                val unitCost = item.unitCost?.let { Formatters.formatMoney(it) } ?: ""
                val extCost = item.extendedCost?.let { Formatters.formatMoney(it) } ?: ""
                appendLine(
                    listOf(
                        csvCell(item.name),
                        item.quantity.toString(),
                        csvCell(item.unit),
                        csvCell(unitCost),
                        csvCell(extCost)
                    ).joinToString(separator = ",")
                )
            }
            result.materialSubtotal?.let { subtotal ->
                appendLine("${csvCell("Materials")},,,,${csvCell(Formatters.formatMoney(subtotal))}")
            }
            result.laborCost?.let { labor ->
                appendLine("${csvCell("Labor")},,,,${csvCell(Formatters.formatMoney(labor))}")
            }
            result.markupCost?.let { markup ->
                appendLine("${csvCell("Markup")},,,,${csvCell(Formatters.formatMoney(markup))}")
            }
            result.taxCost?.let { tax ->
                appendLine("${csvCell("Tax")},,,,${csvCell(Formatters.formatMoney(tax))}")
            }
            result.totalCost?.let { total ->
                appendLine()
                appendLine("${csvCell("TOTAL")},,,,${csvCell(Formatters.formatMoney(total))}")
            }
            appendLine()
            appendLine("Trace Metric,Value,Unit,Room ID,Wall ID,Opening ID")
            result.traces.forEach { trace ->
                appendLine(
                    listOf(
                        csvCell(trace.metric),
                        trace.value.toString(),
                        csvCell(trace.unit),
                        csvCell(trace.roomId.orEmpty()),
                        csvCell(trace.wallId.orEmpty()),
                        csvCell(trace.openingId.orEmpty())
                    ).joinToString(separator = ",")
                )
            }
            appendLine()
            appendLine(csvCell(DISCLAIMER))
        }
    }

    fun formatAsJson(
        project: Project,
        settings: Settings,
        takeoffType: String,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val company = buildBusinessHeader(settings)
        val timestamp = Formatters.formatDate(generatedAtMillis)
        val itemsJson = result.items.joinToString(separator = ",\n") { item ->
            """
            {
              "name": "${escapeJson(item.name)}",
              "quantity": ${item.quantity},
              "unit": "${escapeJson(item.unit)}",
              "unitCost": ${item.unitCost ?: "null"},
              "extendedCost": ${item.extendedCost ?: "null"}
            }
            """.trimIndent()
        }
        val tracesJson = result.traces.joinToString(separator = ",\n") { trace ->
            """
            {
              "metric": "${escapeJson(trace.metric)}",
              "value": ${trace.value},
              "unit": "${escapeJson(trace.unit)}",
              "roomId": ${trace.roomId?.let { "\"${escapeJson(it)}\"" } ?: "null"},
              "wallId": ${trace.wallId?.let { "\"${escapeJson(it)}\"" } ?: "null"},
              "openingId": ${trace.openingId?.let { "\"${escapeJson(it)}\"" } ?: "null"}
            }
            """.trimIndent()
        }
        return """
        {
          "project": {
            "id": "${escapeJson(project.id)}",
            "name": "${escapeJson(project.name)}"
          },
          "estimateId": "${escapeJson(estimateId)}",
          "company": {
            "name": "${escapeJson(company.name)}",
            "phone": "${escapeJson(settings.businessPhone)}",
            "email": "${escapeJson(settings.businessEmail)}",
            "address": "${escapeJson(settings.businessAddress)}",
            "license": "${escapeJson(settings.businessLicense)}"
          },
          "takeoffType": "${escapeJson(takeoffType)}",
          "generatedAt": "${escapeJson(timestamp)}",
          "items": [
            $itemsJson
          ],
          "totals": {
            "materials": ${result.materialSubtotal ?: "null"},
            "labor": ${result.laborCost ?: "null"},
            "markup": ${result.markupCost ?: "null"},
            "tax": ${result.taxCost ?: "null"},
            "total": ${result.totalCost ?: "null"}
          },
          "traceability": [
            $tracesJson
          ],
          "disclaimer": "${escapeJson(DISCLAIMER)}"
        }
        """.trimIndent()
    }
    
    /**
     * Format takeoff results summary (short version for copy).
     */
    fun formatAsSummary(
        project: Project,
        settings: Settings,
        takeoffType: String,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val header = buildBusinessHeader(settings)
        return buildString {
            appendLine(header.name)
            appendLine("${project.name} - $takeoffType")
            appendLine("Estimate ID: $estimateId")
            result.items.forEach { item ->
                append("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                item.extendedCost?.let { ext ->
                    append(" (${Formatters.formatMoney(ext)})")
                }
                appendLine()
            }
            result.materialSubtotal?.let { subtotal ->
                appendLine("Materials: ${Formatters.formatMoney(subtotal)}")
            }
            result.laborCost?.let { labor ->
                appendLine("Labor: ${Formatters.formatMoney(labor)}")
            }
            result.markupCost?.let { markup ->
                appendLine("Markup: ${Formatters.formatMoney(markup)}")
            }
            result.taxCost?.let { tax ->
                appendLine("Tax: ${Formatters.formatMoney(tax)}")
            }
            result.totalCost?.let { total ->
                appendLine("Total: ${Formatters.formatMoney(total)}")
            }
        }
    }
    
    fun getDisclaimer(): String = DISCLAIMER

    private fun buildBusinessHeader(settings: Settings): BusinessHeader {
        val businessName = settings.businessName.trim().ifBlank { "TradeSketch Estimator" }
        val contactLines = buildList {
            settings.businessPhone.trim().takeIf { it.isNotBlank() }?.let { phone ->
                add("Phone: $phone")
            }
            settings.businessEmail.trim().takeIf { it.isNotBlank() }?.let { email ->
                add("Email: $email")
            }
            settings.businessAddress.trim().takeIf { it.isNotBlank() }?.let { address ->
                add("Address: $address")
            }
            settings.businessLicense.trim().takeIf { it.isNotBlank() }?.let { license ->
                add("License: $license")
            }
        }
        return BusinessHeader(
            name = businessName,
            contactLines = contactLines
        )
    }

    private data class BusinessHeader(
        val name: String,
        val contactLines: List<String>
    )

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    private fun csvCell(rawValue: String): String {
        val escaped = sanitizeCsvForSpreadsheet(rawValue).replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun sanitizeCsvForSpreadsheet(rawValue: String): String {
        val normalized = rawValue.replace("\r\n", "\n").replace('\r', '\n')
        val trimmed = normalized.trimStart()
        val isFormulaLike = trimmed.startsWith("=") ||
            trimmed.startsWith("+") ||
            trimmed.startsWith("-") ||
            trimmed.startsWith("@")
        return if (isFormulaLike) {
            "'$normalized"
        } else {
            normalized
        }
    }

}
