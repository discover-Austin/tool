package com.tradesketch.estimator.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import com.tradesketch.estimator.domain.model.hasMeasuredQuantities
import com.tradesketch.estimator.domain.model.nonZeroItems
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object EstimateExportManager {
    fun buildEstimatePdfBytes(
        projectId: String,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String? = null,
        blueprintDocument: BlueprintDocument? = null
    ): ByteArray {
        return renderEstimatePdfBytes(
            projectId = projectId,
            projectName = projectName,
            takeoffType = takeoffType,
            settings = settings,
            result = result,
            generatedAtMillis = generatedAtMillis,
            estimateId = estimateId,
            blueprintDocument = blueprintDocument
        )
    }

    suspend fun saveEstimatePdfToDownloads(
        context: Context,
        projectId: String,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String? = null,
        blueprintDocument: BlueprintDocument? = null
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        val fileName = ExportStorage.buildFileName(
            projectName = projectName,
            suffix = "estimate",
            extension = "pdf"
        )
        val pdfBytes = renderEstimatePdfBytes(
            projectId = projectId,
            projectName = projectName,
            takeoffType = takeoffType,
            settings = settings,
            result = result,
            generatedAtMillis = generatedAtMillis,
            estimateId = estimateId,
            blueprintDocument = blueprintDocument
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ExportStorage.saveBytesToPublicDownloads(
                context = context,
                bytes = pdfBytes,
                fileName = fileName,
                mimeType = "application/pdf"
            )
        } else {
            ExportStorage.saveBytesToAppDownloads(
                context = context,
                bytes = pdfBytes,
                fileName = fileName
            )
        }
    }

    suspend fun createEstimatePdfShareIntent(
        context: Context,
        projectId: String,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        generatedAtMillis: Long = System.currentTimeMillis(),
        estimateId: String? = null,
        blueprintDocument: BlueprintDocument? = null
    ): ExportResult<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDir = File(context.cacheDir, "estimate-share").apply { mkdirs() }
            val shareFile = File(
                cacheDir,
                ExportStorage.buildFileName(
                    projectName = projectName,
                    suffix = "estimate",
                    extension = "pdf"
                )
            )
            val pdfBytes = renderEstimatePdfBytes(
                projectId = projectId,
                projectName = projectName,
                takeoffType = takeoffType,
                settings = settings,
                result = result,
                generatedAtMillis = generatedAtMillis,
                estimateId = estimateId,
                blueprintDocument = blueprintDocument
            )
            require(pdfBytes.isNotEmpty()) { "Generated PDF was empty." }
            FileOutputStream(shareFile).use { output ->
                output.write(pdfBytes)
                output.flush()
            }
            require(shareFile.length() > 0L) { "Generated PDF file was empty." }
            ExportStorage.createShareIntent(
                context = context,
                shareFile = shareFile,
                mimeType = "application/pdf",
                subject = "$projectName Estimate",
                text = "Professional estimate generated from TradeSketch",
                chooserTitle = "Share Estimate PDF"
            )
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not prepare the estimate PDF for sharing.",
                    cause = error
                )
            }
        )
    }

    private fun renderEstimatePdfBytes(
        projectId: String,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        generatedAtMillis: Long,
        estimateId: String?,
        blueprintDocument: BlueprintDocument? = null
    ): ByteArray {
        val document = PdfDocument()
        try {
            val pageWidth = 1240
            val pageHeight = 1754
            val margin = 72f
            val contentWidth = pageWidth - (margin * 2f)
            val pageBottom = pageHeight - margin
            val timestamp = Formatters.formatDate(generatedAtMillis)
            val resolvedEstimateId = estimateId ?: EstimateIdentity.buildEstimateId(
                projectId = projectId,
                generatedAtMillis = generatedAtMillis
            )
            val companyName = settings.businessName.trim().ifBlank { "TradeSketch Estimator" }
            val contactLines = buildList {
                settings.businessPhone.trim().takeIf { it.isNotBlank() }?.let { add("Phone: $it") }
                settings.businessEmail.trim().takeIf { it.isNotBlank() }?.let { add("Email: $it") }
                settings.businessAddress.trim().takeIf { it.isNotBlank() }?.let { add(it) }
                settings.businessLicense.trim().takeIf { it.isNotBlank() }?.let { add("License: $it") }
            }

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(22, 34, 52)
                textSize = 36f
                isFakeBoldText = true
            }
            val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(35, 55, 82)
                textSize = 24f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 30, 30)
                textSize = 19f
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 30, 30)
                textSize = 19f
                isFakeBoldText = true
            }
            val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(88, 88, 88)
                textSize = 16f
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(190, 198, 212)
                strokeWidth = 1.6f
            }

            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = drawPageHeader(
                canvas = canvas,
                margin = margin,
                pageWidth = pageWidth.toFloat(),
                companyName = companyName,
                contactLines = contactLines,
                timestamp = timestamp,
                titlePaint = titlePaint,
                bodyPaint = bodyPaint,
                linePaint = linePaint
            )

            fun startNewPage() {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawPageHeader(
                    canvas = canvas,
                    margin = margin,
                    pageWidth = pageWidth.toFloat(),
                    companyName = companyName,
                    contactLines = contactLines,
                    timestamp = timestamp,
                    titlePaint = titlePaint,
                    bodyPaint = bodyPaint,
                    linePaint = linePaint
                )
            }

            fun ensureSpace(heightNeeded: Float) {
                if (y + heightNeeded > pageBottom - 80f) {
                    startNewPage()
                }
            }

            ensureSpace(130f)
            canvas.drawText("ESTIMATE", margin, y, headingPaint)
            y += 34f
            canvas.drawText("Project: $projectName", margin, y, bodyPaint)
            y += 24f
            canvas.drawText("Estimate ID: $resolvedEstimateId", margin, y, bodyPaint)
            y += 24f
            canvas.drawText("Trade: $takeoffType", margin, y, bodyPaint)
            y += 24f
            canvas.drawText("Prepared: $timestamp", margin, y, bodyPaint)
            y += 28f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 28f

            val blueprint = blueprintDocument
            val measuredItems = result.nonZeroItems()
            val hasBlueprintGeometry = blueprint != null && (
                blueprint.walls.isNotEmpty() ||
                    blueprint.rooms.isNotEmpty() ||
                    blueprint.openings.isNotEmpty()
                )
            if (hasBlueprintGeometry) {
                ensureSpace(404f)
                canvas.drawText("Blueprint Snapshot", margin, y, headingPaint)
                y += 22f
                val bitmap = BlueprintExportManager.renderBlueprintSnapshotBitmap(
                    document = blueprint,
                    widthPx = 1600,
                    heightPx = 560
                )
                try {
                    val targetBounds = RectF(margin, y, pageWidth - margin, y + 340f)
                    val fittedTarget = aspectFitBounds(
                        sourceWidth = bitmap.width,
                        sourceHeight = bitmap.height,
                        boundsLeft = targetBounds.left.toInt(),
                        boundsTop = targetBounds.top.toInt(),
                        boundsRight = targetBounds.right.toInt(),
                        boundsBottom = targetBounds.bottom.toInt()
                    )
                    val fittedRect = RectF(
                        fittedTarget.left.toFloat(),
                        fittedTarget.top.toFloat(),
                        fittedTarget.right.toFloat(),
                        fittedTarget.bottom.toFloat()
                    )
                    canvas.drawBitmap(bitmap, null, fittedRect, null)
                    y = targetBounds.bottom + 24f
                } finally {
                    bitmap.recycle()
                }
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 24f
            }

            ensureSpace(52f)
            canvas.drawText("Line Items", margin, y, headingPaint)
            y += 30f

            val colItem = margin
            val colQty = margin + (contentWidth * 0.55f)
            val colUnit = margin + (contentWidth * 0.70f)
            val colUnitCost = margin + (contentWidth * 0.80f)
            val colTotal = margin + (contentWidth * 0.92f)

            if (measuredItems.isEmpty()) {
                ensureSpace(56f)
                wrapText(
                    "No measured quantities yet. Draw in Blueprint or enter manual quantities in Materials & Pricing.",
                    bodyPaint,
                    contentWidth
                ).forEach { line ->
                    canvas.drawText(line, margin, y, bodyPaint)
                    y += 22f
                }
                y += 12f
            } else {
                canvas.drawText("Item", colItem, y, bodyBoldPaint)
                canvas.drawText("Qty", colQty, y, bodyBoldPaint)
                canvas.drawText("Unit", colUnit, y, bodyBoldPaint)
                canvas.drawText("Unit Cost", colUnitCost, y, bodyBoldPaint)
                canvas.drawText("Total", colTotal, y, bodyBoldPaint)
                y += 14f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 24f
                measuredItems.forEach { item ->
                    val rowHeight = estimateItemRowHeight(
                        itemName = item.name,
                        paint = bodyPaint,
                        maxItemWidth = contentWidth * 0.52f
                    )
                    if (y + rowHeight > pageBottom - 90f) {
                        startNewPage()
                        ensureSpace(120f)
                        canvas.drawText("Line Items (cont.)", margin, y, headingPaint)
                        y += 28f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                        y += 20f
                    }
                    y = drawLineItemRow(
                        canvas = canvas,
                        item = item,
                        y = y,
                        maxItemWidth = contentWidth * 0.52f,
                        colItem = colItem,
                        colQty = colQty,
                        colUnit = colUnit,
                        colUnitCost = colUnitCost,
                        colTotal = colTotal,
                        bodyPaint = bodyPaint,
                        linePaint = linePaint
                    )
                }
            }

            y += 8f
            ensureSpace(180f)
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 26f
            canvas.drawText("Cost Summary", margin, y, headingPaint)
            y += 30f

            if (result.hasMeasuredQuantities()) {
                y = drawSummaryLine(canvas, "Materials", result.materialSubtotal, y, margin, pageWidth.toFloat(), bodyPaint)
                y = drawSummaryLine(canvas, "Labor", result.laborCost, y, margin, pageWidth.toFloat(), bodyPaint)
                y = drawSummaryLine(canvas, "Markup", result.markupCost, y, margin, pageWidth.toFloat(), bodyPaint)
                y = drawSummaryLine(canvas, "Tax", result.taxCost, y, margin, pageWidth.toFloat(), bodyPaint)

                result.totalCost?.let { total ->
                    ensureSpace(36f)
                    canvas.drawText("TOTAL", margin, y, bodyBoldPaint)
                    val totalText = Formatters.formatMoney(total)
                    val totalWidth = bodyBoldPaint.measureText(totalText)
                    canvas.drawText(totalText, pageWidth - margin - totalWidth, y, bodyBoldPaint)
                    y += 30f
                }
            } else {
                ensureSpace(32f)
                canvas.drawText("Totals will appear after quantities are measured.", margin, y, bodyPaint)
                y += 28f
            }

            y += 10f
            ensureSpace(80f)
            wrapText(ExportFormatter.getDisclaimer(), smallPaint, contentWidth).forEach { line ->
                canvas.drawText(line, margin, y, smallPaint)
                y += 20f
            }

            val footerText = "Generated by TradeSketch"
            val footerWidth = smallPaint.measureText(footerText)
            canvas.drawText(footerText, pageWidth - margin - footerWidth, pageHeight - 36f, smallPaint)

            document.finishPage(page)
            return ByteArrayOutputStream().use { output ->
                document.writeTo(output)
                output.toByteArray()
            }
        } finally {
            document.close()
        }
    }

    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        margin: Float,
        pageWidth: Float,
        companyName: String,
        contactLines: List<String>,
        timestamp: String,
        titlePaint: Paint,
        bodyPaint: Paint,
        linePaint: Paint
    ): Float {
        canvas.drawColor(Color.WHITE)
        var y = margin
        canvas.drawText(companyName, margin, y, titlePaint)
        y += 30f
        contactLines.forEach { line ->
            canvas.drawText(line, margin, y, bodyPaint)
            y += 24f
        }
        canvas.drawText("Prepared $timestamp", margin, y, bodyPaint)
        y += 18f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        return y + 32f
    }

    private fun drawLineItemRow(
        canvas: android.graphics.Canvas,
        item: TakeoffLine,
        y: Float,
        maxItemWidth: Float,
        colItem: Float,
        colQty: Float,
        colUnit: Float,
        colUnitCost: Float,
        colTotal: Float,
        bodyPaint: Paint,
        linePaint: Paint
    ): Float {
        var rowY = y
        val wrappedName = wrapText(item.name, bodyPaint, maxItemWidth)
        wrappedName.forEachIndexed { index, line ->
            canvas.drawText(line, colItem, rowY + (index * 22f), bodyPaint)
        }
        canvas.drawText(Formatters.formatQuantity(item.quantity), colQty, rowY, bodyPaint)
        canvas.drawText(item.unit, colUnit, rowY, bodyPaint)
        item.unitCost?.let { unitCost ->
            canvas.drawText(Formatters.formatMoney(unitCost), colUnitCost, rowY, bodyPaint)
        }
        item.extendedCost?.let { ext ->
            canvas.drawText(Formatters.formatMoney(ext), colTotal, rowY, bodyPaint)
        }
        rowY += wrappedName.size * 22f
        canvas.drawLine(colItem, rowY + 6f, colTotal + 90f, rowY + 6f, linePaint)
        return rowY + 20f
    }

    private fun estimateItemRowHeight(
        itemName: String,
        paint: Paint,
        maxItemWidth: Float
    ): Float {
        val lineCount = wrapText(itemName, paint, maxItemWidth).size
        return (lineCount * 22f) + 24f
    }

    private fun drawSummaryLine(
        canvas: android.graphics.Canvas,
        label: String,
        value: Double?,
        y: Float,
        margin: Float,
        pageWidth: Float,
        paint: Paint
    ): Float {
        if (value == null) return y
        canvas.drawText(label, margin, y, paint)
        val valueText = Formatters.formatMoney(value)
        val valueWidth = paint.measureText(valueText)
        canvas.drawText(valueText, pageWidth - margin - valueWidth, y, paint)
        return y + 24f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotBlank()) {
                    lines += current
                }
                current = word
            }
        }
        if (current.isNotBlank()) {
            lines += current
        }
        return lines.ifEmpty { listOf(text) }
    }
}
