package com.yourcompany.tradesketch.utils

import com.yourcompany.tradesketch.domain.model.Project
import com.yourcompany.tradesketch.domain.model.TakeoffResult

/**
 * Formats data for export (CSV, PDF, Share, Copy).
 */
object ExportFormatter {
    
    private const val DISCLAIMER = "ESTIMATE ONLY - Verify quantities, measurements, and pricing with actual site conditions, local building codes, and material suppliers before purchasing or starting work."
    
    /**
     * Format takeoff results as plain text for sharing or copying.
     */
    fun formatAsText(project: Project, takeoffType: String, result: TakeoffResult): String {
        return buildString {
            appendLine("TradeSketch Estimator")
            appendLine("=" .repeat(50))
            appendLine()
            appendLine("Project: ${project.name}")
            appendLine("Takeoff Type: $takeoffType")
            appendLine("Date: ${Formatters.formatDate(System.currentTimeMillis())}")
            appendLine()
            appendLine("QUANTITIES")
            appendLine("-" .repeat(50))
            result.items.forEach { item ->
                appendLine("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                item.unitCost?.let { cost ->
                    appendLine("  @ ${Formatters.formatMoney(Money(cost.toLong()))} each")
                }
                item.extendedCost?.let { ext ->
                    appendLine("  Total: ${Formatters.formatMoney(Money(ext.toLong()))}")
                }
            }
            appendLine()
            result.totalCost?.let { total ->
                appendLine("TOTAL COST: ${Formatters.formatMoney(Money(total.toLong()))}")
                appendLine()
            }
            appendLine("-" .repeat(50))
            appendLine(DISCLAIMER)
        }
    }
    
    /**
     * Format takeoff results as CSV.
     */
    fun formatAsCSV(project: Project, takeoffType: String, result: TakeoffResult): String {
        return buildString {
            appendLine("Project,Takeoff Type,Date")
            appendLine("\"${project.name}\",\"$takeoffType\",\"${Formatters.formatDate(System.currentTimeMillis())}\"")
            appendLine()
            appendLine("Item,Quantity,Unit,Unit Cost,Extended Cost")
            result.items.forEach { item ->
                val unitCost = item.unitCost?.let { Formatters.formatMoney(Money(it.toLong())) } ?: ""
                val extCost = item.extendedCost?.let { Formatters.formatMoney(Money(it.toLong())) } ?: ""
                appendLine("\"${item.name}\",${item.quantity},\"${item.unit}\",\"$unitCost\",\"$extCost\"")
            }
            result.totalCost?.let { total ->
                appendLine()
                appendLine("TOTAL,,,,\"${Formatters.formatMoney(Money(total.toLong()))}\"")
            }
            appendLine()
            appendLine("\"$DISCLAIMER\"")
        }
    }
    
    /**
     * Format takeoff results summary (short version for copy).
     */
    fun formatAsSummary(project: Project, takeoffType: String, result: TakeoffResult): String {
        return buildString {
            appendLine("${project.name} - $takeoffType")
            result.items.forEach { item ->
                append("${item.name}: ${Formatters.formatQuantity(item.quantity)} ${item.unit}")
                item.extendedCost?.let { ext ->
                    append(" (${Formatters.formatMoney(Money(ext.toLong()))})")
                }
                appendLine()
            }
            result.totalCost?.let { total ->
                appendLine("Total: ${Formatters.formatMoney(Money(total.toLong()))}")
            }
        }
    }
    
    fun getDisclaimer(): String = DISCLAIMER
}
