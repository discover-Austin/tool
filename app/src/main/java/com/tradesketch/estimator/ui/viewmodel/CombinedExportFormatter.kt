package com.tradesketch.estimator.ui.viewmodel

import com.tradesketch.estimator.domain.model.Project
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.hasMeasuredQuantities
import com.tradesketch.estimator.domain.model.nonZeroItems
import com.tradesketch.estimator.utils.EstimateIdentity
import com.tradesketch.estimator.utils.Formatters

internal data class CombinedExportSection(
    val takeoffTypeLabel: String,
    val result: TakeoffResult
)

internal object CombinedExportFormatter {
    private const val DISCLAIMER = "ESTIMATE ONLY - Verify quantities, measurements, and pricing with actual site conditions, local building codes, and material suppliers before purchasing or starting work."

    fun formatAsText(
        project: Project,
        settings: Settings,
        sections: List<CombinedExportSection>,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val timestamp = Formatters.formatDate(generatedAtMillis)
        return buildString {
            appendLine(settings.businessName.trim().ifBlank { "TradeSketch Estimator" })
            settings.businessPhone.trim().takeIf { it.isNotBlank() }?.let { appendLine("Phone: $it") }
            settings.businessEmail.trim().takeIf { it.isNotBlank() }?.let { appendLine("Email: $it") }
            settings.businessAddress.trim().takeIf { it.isNotBlank() }?.let { appendLine("Address: $it") }
            settings.businessLicense.trim().takeIf { it.isNotBlank() }?.let { appendLine("License: $it") }
            appendLine("=".repeat(50))
            appendLine()
            appendLine("Project: ${project.name}")
            appendLine("Estimate ID: $estimateId")
            appendLine("Export: All Present Trades")
            appendLine("Date: $timestamp")
            appendLine("Trades Included: ${sections.joinToString { it.takeoffTypeLabel }}")
            appendLine()
            if (sections.isEmpty()) {
                appendLine("No trade geometry is present in the blueprint yet.")
            } else {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        appendLine()
                        appendLine("=".repeat(50))
                        appendLine()
                    }
                    appendLine(section.takeoffTypeLabel.uppercase())
                    appendLine("-".repeat(50))
                    val items = section.result.nonZeroItems()
                    if (items.isEmpty()) {
                        appendLine("Geometry is present, but there are no measured quantities yet.")
                    } else {
                        items.forEach { item ->
                            appendLine("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                            item.unitCost?.let { cost ->
                                appendLine("  @ ${Formatters.formatMoney(cost)} each")
                            }
                            item.extendedCost?.let { ext ->
                                appendLine("  Total: ${Formatters.formatMoney(ext)}")
                            }
                        }
                    }
                    if (section.result.hasMeasuredQuantities()) {
                        appendLine()
                        section.result.materialSubtotal?.let { appendLine("Materials: ${Formatters.formatMoney(it)}") }
                        section.result.laborCost?.let { appendLine("Labor: ${Formatters.formatMoney(it)}") }
                        section.result.markupCost?.let { appendLine("Markup: ${Formatters.formatMoney(it)}") }
                        section.result.taxCost?.let { appendLine("Tax: ${Formatters.formatMoney(it)}") }
                        section.result.totalCost?.let { appendLine("TOTAL COST: ${Formatters.formatMoney(it)}") }
                    }
                }
            }
            appendLine()
            appendLine("-".repeat(50))
            appendLine(DISCLAIMER)
        }
    }

    fun formatAsSummary(
        project: Project,
        settings: Settings,
        sections: List<CombinedExportSection>,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        return buildString {
            appendLine(settings.businessName.trim().ifBlank { "TradeSketch Estimator" })
            appendLine("${project.name} - All Present Trades")
            appendLine("Estimate ID: $estimateId")
            if (sections.isEmpty()) {
                appendLine("No trade geometry is present in the blueprint yet.")
            } else {
                appendLine("Trades: ${sections.joinToString { it.takeoffTypeLabel }}")
                sections.forEach { section ->
                    appendLine()
                    appendLine(section.takeoffTypeLabel)
                    val items = section.result.nonZeroItems()
                    if (items.isEmpty()) {
                        appendLine("No measured quantities yet.")
                    } else {
                        items.forEach { item ->
                            append("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                            item.extendedCost?.let { ext ->
                                append(" (${Formatters.formatMoney(ext)})")
                            }
                            appendLine()
                        }
                    }
                    section.result.totalCost?.let { total ->
                        if (section.result.hasMeasuredQuantities()) {
                            appendLine("Total: ${Formatters.formatMoney(total)}")
                        }
                    }
                }
            }
        }
    }

    fun formatAsCSV(
        project: Project,
        settings: Settings,
        sections: List<CombinedExportSection>,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val timestamp = Formatters.formatDate(generatedAtMillis)
        return buildString {
            appendLine("Company,Phone,Email,Address,License")
            appendLine(
                listOf(
                    settings.businessName.trim().ifBlank { "TradeSketch Estimator" },
                    settings.businessPhone,
                    settings.businessEmail,
                    settings.businessAddress,
                    settings.businessLicense
                ).joinToString(separator = ",") { cell -> csvCell(cell) }
            )
            appendLine()
            appendLine("Project,Estimate ID,Export Scope,Date")
            appendLine(
                listOf(
                    project.name,
                    estimateId,
                    "All Present Trades",
                    timestamp
                ).joinToString(separator = ",") { cell -> csvCell(cell) }
            )
            appendLine()
            appendLine("Trade,Item,Quantity,Unit,Unit Cost,Extended Cost")
            if (sections.isEmpty()) {
                appendLine("${csvCell("None")},${csvCell("No trade geometry present")},,,," )
            } else {
                sections.forEach { section ->
                    val items = section.result.nonZeroItems()
                    if (items.isEmpty()) {
                        appendLine(
                            listOf(
                                csvCell(section.takeoffTypeLabel),
                                csvCell("No measured quantities yet"),
                                "",
                                "",
                                "",
                                ""
                            ).joinToString(separator = ",")
                        )
                    } else {
                        items.forEach { item ->
                            appendLine(
                                listOf(
                                    csvCell(section.takeoffTypeLabel),
                                    csvCell(item.name),
                                    item.quantity.toString(),
                                    csvCell(item.unit),
                                    csvCell(item.unitCost?.let { Formatters.formatMoney(it) }.orEmpty()),
                                    csvCell(item.extendedCost?.let { Formatters.formatMoney(it) }.orEmpty())
                                ).joinToString(separator = ",")
                            )
                        }
                    }
                    if (section.result.hasMeasuredQuantities()) {
                        section.result.materialSubtotal?.let { subtotal ->
                            appendLine("${csvCell(section.takeoffTypeLabel)},${csvCell("Materials")},,,,${csvCell(Formatters.formatMoney(subtotal))}")
                        }
                        section.result.laborCost?.let { labor ->
                            appendLine("${csvCell(section.takeoffTypeLabel)},${csvCell("Labor")},,,,${csvCell(Formatters.formatMoney(labor))}")
                        }
                        section.result.markupCost?.let { markup ->
                            appendLine("${csvCell(section.takeoffTypeLabel)},${csvCell("Markup")},,,,${csvCell(Formatters.formatMoney(markup))}")
                        }
                        section.result.taxCost?.let { tax ->
                            appendLine("${csvCell(section.takeoffTypeLabel)},${csvCell("Tax")},,,,${csvCell(Formatters.formatMoney(tax))}")
                        }
                        section.result.totalCost?.let { total ->
                            appendLine("${csvCell(section.takeoffTypeLabel)},${csvCell("TOTAL")},,,,${csvCell(Formatters.formatMoney(total))}")
                        }
                    }
                }
            }
        }
    }

    fun formatAsJson(
        project: Project,
        settings: Settings,
        sections: List<CombinedExportSection>,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String = EstimateIdentity.buildEstimateId(project, generatedAtMillis)
    ): String {
        val timestamp = Formatters.formatDate(generatedAtMillis)
        val tradesJson = sections.joinToString(separator = ",\n") { section ->
            val measured = section.result.hasMeasuredQuantities()
            val itemsJson = section.result.nonZeroItems().joinToString(separator = ",\n") { item ->
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
            val tracesJson = section.result.traces.joinToString(separator = ",\n") { trace ->
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
            """
            {
              "takeoffType": "${escapeJson(section.takeoffTypeLabel)}",
              "items": [
                $itemsJson
              ],
              "totals": {
                "materials": ${if (measured) section.result.materialSubtotal ?: "null" else "null"},
                "labor": ${if (measured) section.result.laborCost ?: "null" else "null"},
                "markup": ${if (measured) section.result.markupCost ?: "null" else "null"},
                "tax": ${if (measured) section.result.taxCost ?: "null" else "null"},
                "total": ${if (measured) section.result.totalCost ?: "null" else "null"}
              },
              "traceability": [
                $tracesJson
              ]
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
          "exportScope": "All Present Trades",
          "generatedAt": "${escapeJson(timestamp)}",
          "company": {
            "name": "${escapeJson(settings.businessName.trim().ifBlank { "TradeSketch Estimator" })}",
            "phone": "${escapeJson(settings.businessPhone)}",
            "email": "${escapeJson(settings.businessEmail)}",
            "address": "${escapeJson(settings.businessAddress)}",
            "license": "${escapeJson(settings.businessLicense)}"
          },
          "trades": [
            $tradesJson
          ],
          "disclaimer": "${escapeJson(DISCLAIMER)}"
        }
        """.trimIndent()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }

    private fun csvCell(rawValue: String): String {
        val escaped = rawValue.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
