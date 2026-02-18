package com.tradesketch.estimator.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.areaSqFt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object BlueprintExportManager {
    suspend fun saveBlueprintToDownloads(
        context: Context,
        projectName: String,
        spaces: List<Space>
    ): Uri? = withContext(Dispatchers.IO) {
        if (spaces.isEmpty()) return@withContext null
        val bitmap = renderBlueprintBitmap(projectName = projectName, spaces = spaces)
        val fileName = buildFileName(projectName = projectName, extension = "png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToPublicDownloads(context = context, bitmap = bitmap, fileName = fileName)
        } else {
            saveToAppDownloads(context = context, bitmap = bitmap, fileName = fileName)
        }
    }

    suspend fun createBlueprintShareIntent(
        context: Context,
        projectName: String,
        spaces: List<Space>
    ): Intent? = withContext(Dispatchers.IO) {
        if (spaces.isEmpty()) return@withContext null
        val bitmap = renderBlueprintBitmap(projectName = projectName, spaces = spaces)
        val cacheDir = File(context.cacheDir, "blueprint-share").apply { mkdirs() }
        val shareFile = File(cacheDir, buildFileName(projectName = projectName, extension = "png"))
        FileOutputStream(shareFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_SUBJECT, "$projectName Blueprint")
                putExtra(Intent.EXTRA_TEXT, "Blueprint export from TradeSketch")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share Blueprint"
        )
    }

    suspend fun saveBlueprintPdfToDownloads(
        context: Context,
        projectName: String,
        spaces: List<Space>
    ): Uri? = withContext(Dispatchers.IO) {
        if (spaces.isEmpty()) return@withContext null
        val fileName = buildFileName(projectName = projectName, extension = "pdf")
        val pdfBytes = renderBlueprintPdfBytes(projectName = projectName, spaces = spaces)
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

    suspend fun createBlueprintPdfShareIntent(
        context: Context,
        projectName: String,
        spaces: List<Space>
    ): Intent? = withContext(Dispatchers.IO) {
        if (spaces.isEmpty()) return@withContext null
        val cacheDir = File(context.cacheDir, "blueprint-share").apply { mkdirs() }
        val shareFile = File(cacheDir, buildFileName(projectName = projectName, extension = "pdf"))
        val pdfBytes = renderBlueprintPdfBytes(projectName = projectName, spaces = spaces)
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
                putExtra(Intent.EXTRA_SUBJECT, "$projectName Blueprint PDF")
                putExtra(Intent.EXTRA_TEXT, "Blueprint PDF export from TradeSketch")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share Blueprint PDF"
        )
    }

    fun renderBlueprintBitmap(
        projectName: String,
        spaces: List<Space>,
        widthPx: Int = 2200,
        heightPx: Int = 2200
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(1200)
        val safeHeight = heightPx.coerceAtLeast(1200)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 19, 35)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat(), backgroundPaint)

        val contentLeft = 90f
        val contentTop = 220f
        val contentRight = safeWidth - 90f
        val contentBottom = safeHeight - 90f
        val contentWidth = contentRight - contentLeft
        val contentHeight = contentBottom - contentTop
        val contentRect = RectF(contentLeft, contentTop, contentRight, contentBottom)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 196, 220)
            textSize = 34f
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("$projectName Blueprint", 90f, 90f, titlePaint)
        canvas.drawText("Exported $stamp", 90f, 142f, subtitlePaint)

        if (spaces.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(196, 208, 229)
                textSize = 44f
            }
            canvas.drawText("No spaces to render", 90f, 210f, emptyPaint)
            return bitmap
        }

        val footprints = spaces.map { space -> space to footprintRect(space) }
        val bounds = combinedBounds(footprints.map { it.second })
        val padWorld = max(8.0, max(bounds.width(), bounds.height()) * 0.1)
        val minX = bounds.left - padWorld
        val maxX = bounds.right + padWorld
        val minZ = bounds.top - padWorld
        val maxZ = bounds.bottom + padWorld
        val spanX = (maxX - minX).coerceAtLeast(1.0)
        val spanZ = (maxZ - minZ).coerceAtLeast(1.0)
        val scale = min(contentWidth / spanX, contentHeight / spanZ).toFloat()

        drawGrid(
            canvas = canvas,
            contentRect = contentRect,
            minX = minX,
            maxX = maxX,
            minZ = minZ,
            maxZ = maxZ,
            scale = scale
        )

        val overlapIds = overlappingSpaceIds(spaces)
        footprints.forEach { (space, rect) ->
            drawSpace(
                canvas = canvas,
                space = space,
                rect = rect,
                minX = minX,
                maxZ = maxZ,
                scale = scale,
                contentRect = contentRect,
                isOverlapping = space.id in overlapIds
            )
        }

        drawEnvelopeDimensions(
            canvas = canvas,
            minX = minX,
            maxX = maxX,
            minZ = minZ,
            maxZ = maxZ,
            scale = scale,
            contentRect = contentRect
        )

        drawLegend(canvas = canvas, x = safeWidth - 500f, y = 90f)

        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 214, 236)
            textSize = 30f
        }
        val totalArea = spaces.sumOf { it.geometry.areaSqFt() }
        canvas.drawText(
            "Spaces ${spaces.size}  •  Area ${"%.1f".format(totalArea)} sq ft",
            90f,
            safeHeight - 26f,
            summaryPaint
        )
        return bitmap
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun saveToPublicDownloads(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TradeSketch")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
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

private fun saveToAppDownloads(
    context: Context,
    bitmap: Bitmap,
    fileName: String
): Uri? {
    return try {
        val downloadsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "TradeSketch"
        ).apply { mkdirs() }
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
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

private fun renderBlueprintPdfBytes(
    projectName: String,
    spaces: List<Space>
): ByteArray {
    val document = PdfDocument()
    val pageWidth = 1654
    val pageHeight = 2339
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 19, 35)
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), backgroundPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        isFakeBoldText = true
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(188, 207, 235)
        textSize = 20f
    }
    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    canvas.drawText("$projectName Blueprint", 56f, 74f, titlePaint)
    canvas.drawText("Exported $stamp", 56f, 108f, subtitlePaint)

    val bitmap = BlueprintExportManager.renderBlueprintBitmap(
        projectName = projectName,
        spaces = spaces,
        widthPx = 1800,
        heightPx = 1800
    )
    val margin = 56
    val blueprintTop = 150
    val blueprintBottom = pageHeight - 170
    val blueprintRect = Rect(
        margin,
        blueprintTop,
        pageWidth - margin,
        blueprintBottom
    )
    canvas.drawBitmap(bitmap, null, blueprintRect, null)

    val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(205, 222, 245)
        textSize = 18f
    }
    val totalArea = spaces.sumOf { it.geometry.areaSqFt() }
    canvas.drawText(
        "Spaces ${spaces.size}  •  Area ${"%.1f".format(totalArea)} sq ft",
        56f,
        (pageHeight - 96).toFloat(),
        summaryPaint
    )
    canvas.drawText(
        "Generated by TradeSketch",
        56f,
        (pageHeight - 70).toFloat(),
        summaryPaint
    )

    document.finishPage(page)
    return ByteArrayOutputStream().use { output ->
        document.writeTo(output)
        document.close()
        output.toByteArray()
    }
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
    return "${cleanName}_blueprint_$stamp.$extension"
}

private data class BlueprintRect(
    val left: Double,
    val right: Double,
    val top: Double,
    val bottom: Double
) {
    fun width() = right - left
    fun height() = bottom - top
}

private fun combinedBounds(rects: List<BlueprintRect>): BlueprintRect {
    if (rects.isEmpty()) return BlueprintRect(-20.0, 20.0, -20.0, 20.0)
    var left = Double.POSITIVE_INFINITY
    var right = Double.NEGATIVE_INFINITY
    var top = Double.POSITIVE_INFINITY
    var bottom = Double.NEGATIVE_INFINITY
    rects.forEach { rect ->
        left = min(left, rect.left)
        right = max(right, rect.right)
        top = min(top, rect.top)
        bottom = max(bottom, rect.bottom)
    }
    return BlueprintRect(left, right, top, bottom)
}

private fun footprintRect(space: Space): BlueprintRect {
    val (width, depth) = footprintDimensions(space.geometry)
    val halfW = width / 2.0
    val halfD = depth / 2.0
    return BlueprintRect(
        left = space.transform.xFeet - halfW,
        right = space.transform.xFeet + halfW,
        top = space.transform.zFeet - halfD,
        bottom = space.transform.zFeet + halfD
    )
}

private fun drawGrid(
    canvas: Canvas,
    contentRect: RectF,
    minX: Double,
    maxX: Double,
    minZ: Double,
    maxZ: Double,
    scale: Float
) {
    val span = max(maxX - minX, maxZ - minZ)
    val spacing = when {
        span > 450.0 -> 40.0
        span > 250.0 -> 20.0
        span > 120.0 -> 10.0
        else -> 5.0
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 102, 129, 170)
        strokeWidth = 1.6f
    }
    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 170, 201, 247)
        strokeWidth = 2.6f
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(175, 190, 209, 237)
        textSize = 20f
    }

    var x = kotlin.math.floor(minX / spacing) * spacing
    while (x <= maxX) {
        val screenX = (contentRect.left + ((x - minX).toFloat() * scale))
        canvas.drawLine(screenX, contentRect.top, screenX, contentRect.bottom, linePaint)
        if (x.toInt() % 20 == 0) {
            canvas.drawText("${x.toInt()}ft", screenX + 4f, contentRect.bottom - 8f, labelPaint)
        }
        x += spacing
    }

    var z = kotlin.math.floor(minZ / spacing) * spacing
    while (z <= maxZ) {
        val screenY = (contentRect.bottom - ((z - minZ).toFloat() * scale))
        canvas.drawLine(contentRect.left, screenY, contentRect.right, screenY, linePaint)
        z += spacing
    }

    if (minX <= 0.0 && maxX >= 0.0) {
        val axisX = contentRect.left + ((0.0 - minX).toFloat() * scale)
        canvas.drawLine(axisX, contentRect.top, axisX, contentRect.bottom, axisPaint)
    }
    if (minZ <= 0.0 && maxZ >= 0.0) {
        val axisY = contentRect.bottom - ((0.0 - minZ).toFloat() * scale)
        canvas.drawLine(contentRect.left, axisY, contentRect.right, axisY, axisPaint)
    }
}

private fun drawSpace(
    canvas: Canvas,
    space: Space,
    rect: BlueprintRect,
    minX: Double,
    maxZ: Double,
    scale: Float,
    contentRect: RectF,
    isOverlapping: Boolean
) {
    val lane = tradeLane(space.geometry)
    val fillColor = when (lane) {
        TradeLane.WALLS -> Color.argb(180, 79, 183, 243)
        TradeLane.SLABS -> Color.argb(180, 0, 200, 143)
        TradeLane.ROOMS -> Color.argb(170, 255, 183, 77)
    }
    val strokeColor = if (isOverlapping) {
        Color.rgb(255, 82, 82)
    } else {
        Color.rgb(225, 239, 255)
    }

    val left = contentRect.left + ((rect.left - minX).toFloat() * scale)
    val right = contentRect.left + ((rect.right - minX).toFloat() * scale)
    val top = contentRect.bottom - ((maxZ - rect.bottom).toFloat() * scale)
    val bottom = contentRect.bottom - ((maxZ - rect.top).toFloat() * scale)
    val shapeRect = RectF(left, top, right, bottom)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = if (isOverlapping) 5f else 2.5f
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(233, 244, 255)
        textSize = 26f
        isFakeBoldText = true
    }

    when (space.geometry) {
        is Geometry.Circle -> {
            canvas.drawOval(shapeRect, fillPaint)
            canvas.drawOval(shapeRect, strokePaint)
        }
        else -> {
            canvas.drawRect(shapeRect, fillPaint)
            canvas.drawRect(shapeRect, strokePaint)
        }
    }

    if (shapeRect.width() > 90f && shapeRect.height() > 40f) {
        val clippedName = space.name.take(22)
        canvas.drawText(clippedName, shapeRect.left + 10f, shapeRect.centerY() + 10f, labelPaint)
    }

    if (shapeRect.width() > 100f && shapeRect.height() > 56f) {
        val (widthFt, depthFt) = footprintDimensions(space.geometry)
        val dimensionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(214, 233, 255)
            textSize = 22f
        }
        val dimensionText = "${formatFeet(widthFt)} x ${formatFeet(depthFt)} ft"
        canvas.drawText(dimensionText, shapeRect.left + 10f, shapeRect.bottom - 8f, dimensionPaint)
    }
}

private fun drawLegend(canvas: Canvas, x: Float, y: Float) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 9, 28, 57)
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 140, 176, 220)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        isFakeBoldText = true
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(210, 226, 248)
        textSize = 24f
    }
    val legendRect = RectF(x, y, x + 380f, y + 190f)
    canvas.drawRoundRect(legendRect, 18f, 18f, bgPaint)
    canvas.drawRoundRect(legendRect, 18f, 18f, borderPaint)
    canvas.drawText("Legend", x + 18f, y + 38f, titlePaint)

    drawLegendItem(canvas, x + 18f, y + 68f, Color.rgb(79, 183, 243), "Walls", labelPaint)
    drawLegendItem(canvas, x + 18f, y + 108f, Color.rgb(0, 200, 143), "Slabs", labelPaint)
    drawLegendItem(canvas, x + 18f, y + 148f, Color.rgb(255, 183, 77), "Rooms", labelPaint)
}

private fun drawLegendItem(
    canvas: Canvas,
    x: Float,
    y: Float,
    color: Int,
    label: String,
    textPaint: Paint
) {
    val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }
    canvas.drawRect(x, y, x + 22f, y + 22f, swatchPaint)
    canvas.drawText(label, x + 34f, y + 18f, textPaint)
}

private fun drawEnvelopeDimensions(
    canvas: Canvas,
    minX: Double,
    maxX: Double,
    minZ: Double,
    maxZ: Double,
    scale: Float,
    contentRect: RectF
) {
    val dimLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 146, 187, 240)
        strokeWidth = 2.2f
    }
    val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(215, 231, 250)
        textSize = 21f
        isFakeBoldText = true
    }
    val widthFt = (maxX - minX).coerceAtLeast(0.0)
    val depthFt = (maxZ - minZ).coerceAtLeast(0.0)
    val widthPx = (widthFt * scale.toDouble()).toFloat()
    val depthPx = (depthFt * scale.toDouble()).toFloat()

    val x1 = contentRect.left
    val x2 = contentRect.left + widthPx
    val y = contentRect.top - 16f
    canvas.drawLine(x1, y, x2, y, dimLinePaint)
    canvas.drawLine(x1, y - 8f, x1, y + 8f, dimLinePaint)
    canvas.drawLine(x2, y - 8f, x2, y + 8f, dimLinePaint)
    canvas.drawText(
        "${formatFeet(widthFt)} ft",
        ((x1 + x2) / 2f) - 34f,
        y - 6f,
        dimTextPaint
    )

    val vx = contentRect.right + 16f
    val vy1 = contentRect.bottom - depthPx
    val vy2 = contentRect.bottom
    canvas.drawLine(vx, vy1, vx, vy2, dimLinePaint)
    canvas.drawLine(vx - 8f, vy1, vx + 8f, vy1, dimLinePaint)
    canvas.drawLine(vx - 8f, vy2, vx + 8f, vy2, dimLinePaint)
    canvas.drawText(
        "${formatFeet(depthFt)} ft",
        vx + 10f,
        ((vy1 + vy2) / 2f),
        dimTextPaint
    )
}

private fun formatFeet(value: Double): String {
    return if (value >= 100) {
        "%.0f".format(value)
    } else {
        "%.1f".format(value)
    }
}

private enum class TradeLane {
    WALLS,
    SLABS,
    ROOMS
}

private fun tradeLane(geometry: Geometry): TradeLane {
    return when (geometry) {
        is Geometry.Wall -> TradeLane.WALLS
        is Geometry.Slab -> TradeLane.SLABS
        else -> TradeLane.ROOMS
    }
}

private fun overlappingSpaceIds(spaces: List<Space>): Set<String> {
    if (spaces.size < 2) return emptySet()
    val overlaps = mutableSetOf<String>()
    for (i in 0 until spaces.lastIndex) {
        val a = spaces[i]
        val aBox = footprintRect(a)
        for (j in (i + 1) until spaces.size) {
            val b = spaces[j]
            val bBox = footprintRect(b)
            val intersects = aBox.right >= bBox.left &&
                bBox.right >= aBox.left &&
                aBox.bottom >= bBox.top &&
                bBox.bottom >= aBox.top
            if (intersects) {
                overlaps += a.id
                overlaps += b.id
            }
        }
    }
    return overlaps
}

private fun footprintDimensions(geometry: Geometry): Pair<Double, Double> {
    return when (geometry) {
        is Geometry.Rect -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Slab -> geometry.length.toFeet() to geometry.width.toFeet()
        is Geometry.Wall -> geometry.length.toFeet() to 0.75
        is Geometry.Circle -> {
            val diameter = geometry.radius.toFeet() * 2.0
            diameter to diameter
        }
        is Geometry.LShape -> {
            val width = max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet())
            val depth = max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet())
            width to depth
        }
    }
}
