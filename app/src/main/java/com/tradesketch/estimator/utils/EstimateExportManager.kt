package com.tradesketch.estimator.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.tradesketch.estimator.domain.model.Settings
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.TakeoffLine
import com.tradesketch.estimator.domain.model.TakeoffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EstimateExportManager {
    fun buildEstimatePdfBytes(
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        blueprintSpaces: List<Space> = emptyList()
    ): ByteArray {
        return renderEstimatePdfBytes(
            projectName = projectName,
            takeoffType = takeoffType,
            settings = settings,
            result = result,
            blueprintSpaces = blueprintSpaces
        )
    }

    suspend fun saveEstimatePdfToDownloads(
        context: Context,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        blueprintSpaces: List<Space> = emptyList()
    ): Uri? = withContext(Dispatchers.IO) {
        val fileName = buildFileName(projectName = projectName, extension = "pdf")
        val pdfBytes = renderEstimatePdfBytes(
            projectName = projectName,
            takeoffType = takeoffType,
            settings = settings,
            result = result,
            blueprintSpaces = blueprintSpaces
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBytesToPublicDownloads(
                context = context,
                bytes = pdfBytes,
                fileName = fileName,
                mimeType = "application/pdf"
            )
        } else {
            saveBytesToAppDownloads(
                context = context,
                bytes = pdfBytes,
                fileName = fileName
            )
        }
    }

    suspend fun createEstimatePdfShareIntent(
        context: Context,
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        blueprintSpaces: List<Space> = emptyList()
    ): Intent? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "estimate-share").apply { mkdirs() }
        val shareFile = File(cacheDir, buildFileName(projectName = projectName, extension = "pdf"))
        val pdfBytes = renderEstimatePdfBytes(
            projectName = projectName,
            takeoffType = takeoffType,
            settings = settings,
            result = result,
            blueprintSpaces = blueprintSpaces
        )
        FileOutputStream(shareFile).use { output ->
            output.write(pdfBytes)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "$projectName Estimate")
                putExtra(Intent.EXTRA_TEXT, "Professional estimate generated from TradeSketch")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share Estimate PDF"
        )
    }

    private fun renderEstimatePdfBytes(
        projectName: String,
        takeoffType: String,
        settings: Settings,
        result: TakeoffResult,
        blueprintSpaces: List<Space> = emptyList()
    ): ByteArray {
        val document = PdfDocument()
        val pageWidth = 1240
        val pageHeight = 1754
        val margin = 72f
        val contentWidth = pageWidth - (margin * 2f)
        val pageBottom = pageHeight - margin
        val timestamp = Formatters.formatDate(System.currentTimeMillis())
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
        canvas.drawText("Estimate Type: $takeoffType", margin, y, bodyPaint)
        y += 24f
        canvas.drawText("Prepared: $timestamp", margin, y, bodyPaint)
        y += 28f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 28f

        if (blueprintSpaces.isNotEmpty()) {
            ensureSpace(340f)
            canvas.drawText("Blueprint Snapshot", margin, y, headingPaint)
            y += 20f
            val bitmap = BlueprintExportManager.renderBlueprintBitmap(
                projectName = projectName,
                spaces = blueprintSpaces,
                widthPx = 1200,
                heightPx = 900
            )
            val targetLeft = margin
            val targetTop = y
            val targetRight = pageWidth - margin
            val targetBottom = y + 300f
            val targetRect = android.graphics.RectF(targetLeft, targetTop, targetRight, targetBottom)
            canvas.drawBitmap(bitmap, null, targetRect, null)
            y = targetBottom + 24f
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

        canvas.drawText("Item", colItem, y, bodyBoldPaint)
        canvas.drawText("Qty", colQty, y, bodyBoldPaint)
        canvas.drawText("Unit", colUnit, y, bodyBoldPaint)
        canvas.drawText("Unit Cost", colUnitCost, y, bodyBoldPaint)
        canvas.drawText("Total", colTotal, y, bodyBoldPaint)
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 24f

        result.items.forEach { item ->
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
                maxItemWidth = (contentWidth * 0.52f),
                colItem = colItem,
                colQty = colQty,
                colUnit = colUnit,
                colUnitCost = colUnitCost,
                colTotal = colTotal,
                bodyPaint = bodyPaint,
                linePaint = linePaint
            )
        }

        y += 8f
        ensureSpace(180f)
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 26f
        canvas.drawText("Cost Summary", margin, y, headingPaint)
        y += 30f

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
            document.close()
            output.toByteArray()
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
        rowY += (wrappedName.size * 22f)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBytesToPublicDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveBytesToAppDownloads(
        context: Context,
        bytes: ByteArray,
        fileName: String
    ): Uri? {
        return try {
            val downloadsDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "TradeSketch"
            ).apply { mkdirs() }
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(bytes)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun buildFileName(projectName: String, extension: String): String {
        val cleanName = projectName
            .trim()
            .ifBlank { "project" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .take(40)
            .ifBlank { "project" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${cleanName}_estimate_$stamp.$extension"
    }
}
