package com.tradesketch.estimator.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object BlueprintExportManager {
    fun buildBlueprintPdfBytes(
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): ByteArray {
        if (!document.hasGeometry()) return ByteArray(0)
        return renderBlueprintPdfBytes(
            projectName = projectName,
            document = document,
            includeGrid = includeGrid
        )
    }

    suspend fun saveBlueprintToDownloads(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) return@withContext null
        val bitmap = renderBlueprintBitmap(
            projectName = projectName,
            document = document,
            includeGrid = includeGrid
        )
        val fileName = ExportStorage.buildFileName(
            projectName = projectName,
            suffix = "blueprint",
            extension = "png"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ExportStorage.saveBitmapToPublicDownloads(
                context = context,
                bitmap = bitmap,
                fileName = fileName
            )
        } else {
            ExportStorage.saveBitmapToAppDownloads(
                context = context,
                bitmap = bitmap,
                fileName = fileName
            )
        }
    }

    suspend fun createBlueprintShareIntent(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): Intent? = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) return@withContext null
        val bitmap = renderBlueprintBitmap(
            projectName = projectName,
            document = document,
            includeGrid = includeGrid
        )
        val cacheDir = File(context.cacheDir, "blueprint-share").apply { mkdirs() }
        val shareFile = File(
            cacheDir,
            ExportStorage.buildFileName(
                projectName = projectName,
                suffix = "blueprint",
                extension = "png"
            )
        )
        FileOutputStream(shareFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        ExportStorage.createShareIntent(
            context = context,
            shareFile = shareFile,
            mimeType = "image/png",
            subject = "$projectName Blueprint",
            text = "Blueprint export from TradeSketch",
            chooserTitle = "Share Blueprint"
        )
    }

    suspend fun saveBlueprintPdfToDownloads(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) return@withContext null
        val fileName = ExportStorage.buildFileName(
            projectName = projectName,
            suffix = "blueprint",
            extension = "pdf"
        )
        val pdfBytes = renderBlueprintPdfBytes(
            projectName = projectName,
            document = document,
            includeGrid = includeGrid
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

    suspend fun createBlueprintPdfShareIntent(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): Intent? = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) return@withContext null
        val cacheDir = File(context.cacheDir, "blueprint-share").apply { mkdirs() }
        val shareFile = File(
            cacheDir,
            ExportStorage.buildFileName(
                projectName = projectName,
                suffix = "blueprint",
                extension = "pdf"
            )
        )
        val pdfBytes = renderBlueprintPdfBytes(
            projectName = projectName,
            document = document,
            includeGrid = includeGrid
        )
        FileOutputStream(shareFile).use { output ->
            output.write(pdfBytes)
        }
        ExportStorage.createShareIntent(
            context = context,
            shareFile = shareFile,
            mimeType = "application/pdf",
            subject = "$projectName Blueprint PDF",
            text = "Blueprint PDF export from TradeSketch",
            chooserTitle = "Share Blueprint PDF"
        )
    }

    fun renderBlueprintBitmap(
        projectName: String,
        document: BlueprintDocument,
        widthPx: Int = 2200,
        heightPx: Int = 2200,
        includeGrid: Boolean = true
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

        val points = document.worldPoints()
        if (points.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(196, 208, 229)
                textSize = 44f
            }
            canvas.drawText("No blueprint geometry to render", 90f, 210f, emptyPaint)
            return bitmap
        }

        val contentLeft = 90f
        val contentTop = 220f
        val contentRight = safeWidth - 90f
        val contentBottom = safeHeight - 90f
        val contentRect = RectF(contentLeft, contentTop, contentRight, contentBottom)

        val bounds = points.bounds()
        val spanX = max(1.0, (bounds.maxX - bounds.minX).toDouble())
        val spanY = max(1.0, (bounds.maxY - bounds.minY).toDouble())
        val pad = max(1500.0, max(spanX, spanY) * 0.1)
        val minX = bounds.minX - pad
        val maxX = bounds.maxX + pad
        val minY = bounds.minY - pad
        val maxY = bounds.maxY + pad
        val worldWidth = max(1.0, maxX - minX)
        val worldHeight = max(1.0, maxY - minY)
        val scale = min(contentRect.width() / worldWidth.toFloat(), contentRect.height() / worldHeight.toFloat())

        val toScreen: (PointMm) -> ScreenPoint = { p ->
            ScreenPoint(
                x = contentRect.left + ((p.x - minX).toFloat() * scale),
                y = contentRect.bottom - ((p.y - minY).toFloat() * scale)
            )
        }

        if (includeGrid) {
            drawGrid(
                canvas = canvas,
                contentRect = contentRect,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                scale = scale
            )
        }
        drawRooms(canvas, document.rooms, toScreen)
        drawWalls(canvas, document.walls, toScreen, scale)
        drawOpenings(canvas, document, toScreen, scale)
        drawEnvelopeDimensions(
            canvas = canvas,
            contentRect = contentRect,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            scale = scale
        )

        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 214, 236)
            textSize = 30f
        }
        val totalWallFeet = document.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
        canvas.drawText(
            "Rooms ${document.rooms.size}  •  Walls ${document.walls.size}  •  Openings ${document.openings.size}  •  Wall length ${"%.1f".format(totalWallFeet)} ft",
            90f,
            safeHeight - 26f,
            summaryPaint
        )
        return bitmap
    }
}

private fun drawGrid(
    canvas: Canvas,
    contentRect: RectF,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    scale: Float
) {
    val span = max(maxX - minX, maxY - minY)
    val stepMm = when {
        span > 300_000 -> 25_000.0
        span > 150_000 -> 10_000.0
        span > 60_000 -> 5_000.0
        span > 30_000 -> 2_000.0
        else -> 1_000.0
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

    var x = floor(minX / stepMm) * stepMm
    while (x <= maxX) {
        val screenX = contentRect.left + ((x - minX).toFloat() * scale)
        canvas.drawLine(screenX, contentRect.top, screenX, contentRect.bottom, linePaint)
        val mmValue = x.roundToLong()
        if (mmValue % 10_000L == 0L) {
            canvas.drawText(Millimeters(mmValue).toFeet().toInt().toString() + "ft", screenX + 4f, contentRect.bottom - 8f, labelPaint)
        }
        x += stepMm
    }

    var y = floor(minY / stepMm) * stepMm
    while (y <= maxY) {
        val screenY = contentRect.bottom - ((y - minY).toFloat() * scale)
        canvas.drawLine(contentRect.left, screenY, contentRect.right, screenY, linePaint)
        y += stepMm
    }

    if (minX <= 0.0 && maxX >= 0.0) {
        val axisX = contentRect.left + ((0.0 - minX).toFloat() * scale)
        canvas.drawLine(axisX, contentRect.top, axisX, contentRect.bottom, axisPaint)
    }
    if (minY <= 0.0 && maxY >= 0.0) {
        val axisY = contentRect.bottom - ((0.0 - minY).toFloat() * scale)
        canvas.drawLine(contentRect.left, axisY, contentRect.right, axisY, axisPaint)
    }
}

private fun drawRooms(
    canvas: Canvas,
    rooms: List<Room>,
    toScreen: (PointMm) -> ScreenPoint
) {
    val roomFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(84, 255, 183, 77)
    }
    val roomStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(220, 255, 216, 156)
        strokeWidth = 2.2f
    }
    val roomLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(241, 249, 255)
        textSize = 26f
    }

    rooms.forEach { room ->
        if (room.polygon.size < 3) return@forEach
        val path = Path()
        room.polygon.forEachIndexed { index, point ->
            val screen = toScreen(point)
            if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
        }
        path.close()
        canvas.drawPath(path, roomFillPaint)
        canvas.drawPath(path, roomStrokePaint)

        val centroid = room.polygon.centroid()
        val centroidScreen = toScreen(centroid)
        val label = room.name.ifBlank { room.id }
        canvas.drawText(label.take(22), centroidScreen.x + 6f, centroidScreen.y - 6f, roomLabelPaint)
    }
}

private fun drawWalls(
    canvas: Canvas,
    walls: List<WallSegment>,
    toScreen: (PointMm) -> ScreenPoint,
    scale: Float
) {
    val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(79, 183, 243)
        strokeCap = Paint.Cap.ROUND
    }
    val wallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(225, 239, 255)
        textSize = 21f
        isFakeBoldText = true
    }

    walls.forEach { wall ->
        val start = toScreen(wall.start)
        val end = toScreen(wall.end)
        wallPaint.strokeWidth = (wall.thicknessMm.toFloat() * scale).coerceIn(2.0f, 16.0f)
        canvas.drawLine(start.x, start.y, end.x, end.y, wallPaint)

        val midpoint = ScreenPoint(
            x = (start.x + end.x) / 2f,
            y = (start.y + end.y) / 2f
        )
        val lengthFeet = Millimeters(wall.lengthMillimeters()).toFeet()
        canvas.drawText("${"%.1f".format(lengthFeet)}ft", midpoint.x + 6f, midpoint.y - 6f, wallLabelPaint)
    }
}

private fun drawOpenings(
    canvas: Canvas,
    document: BlueprintDocument,
    toScreen: (PointMm) -> ScreenPoint,
    scale: Float
) {
    val openingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(255, 244, 214)
        strokeWidth = 4f
    }
    val doorArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(255, 184, 108)
        strokeWidth = 3f
    }

    val wallsById = document.walls.associateBy { it.id }
    document.openings.forEach { opening ->
        val wall = wallsById[opening.wallId] ?: return@forEach
        val wallLengthMm = wall.lengthMillimeters().toDouble().coerceAtLeast(1.0)
        val halfT = (opening.widthMm / 2.0) / wallLengthMm
        val startT = (opening.t - halfT).coerceIn(0.0, 1.0)
        val endT = (opening.t + halfT).coerceIn(0.0, 1.0)
        val startPoint = BlueprintSnapMath.pointOnWall(wall, startT)
        val endPoint = BlueprintSnapMath.pointOnWall(wall, endT)
        val centerPoint = BlueprintSnapMath.pointOnWall(wall, opening.t.coerceIn(0.0, 1.0))
        val start = toScreen(startPoint)
        val end = toScreen(endPoint)
        val center = toScreen(centerPoint)

        canvas.drawLine(start.x, start.y, end.x, end.y, openingPaint)

        when (opening.type) {
            OpeningType.DOOR -> {
                drawDoorArc(
                    canvas = canvas,
                    wall = wall,
                    opening = opening,
                    hinge = start,
                    center = center,
                    arcPaint = doorArcPaint,
                    scale = scale
                )
            }

            OpeningType.WINDOW -> {
                drawWindowBreak(canvas = canvas, start = start, end = end, paint = doorArcPaint)
            }

            OpeningType.STAIR_UP,
            OpeningType.STAIR_DOWN -> {
                drawStairBreak(
                    canvas = canvas,
                    start = start,
                    end = end,
                    paint = doorArcPaint,
                    upDirection = opening.type == OpeningType.STAIR_UP
                )
            }
        }
    }
}

private fun drawDoorArc(
    canvas: Canvas,
    wall: WallSegment,
    opening: BlueprintOpening,
    hinge: ScreenPoint,
    center: ScreenPoint,
    arcPaint: Paint,
    scale: Float
) {
    val radiusPx = (opening.widthMm.toFloat() * scale).coerceAtLeast(12f)
    val arcRect = RectF(
        hinge.x - radiusPx,
        hinge.y - radiusPx,
        hinge.x + radiusPx,
        hinge.y + radiusPx
    )
    val wallAngle = Math.toDegrees(
        atan2(
            (wall.end.y - wall.start.y).toDouble(),
            (wall.end.x - wall.start.x).toDouble()
        )
    ).toFloat()
    val sweep = 90f
    val startAngle = wallAngle
    canvas.drawArc(arcRect, startAngle, sweep, false, arcPaint)
    canvas.drawLine(hinge.x, hinge.y, center.x, center.y, arcPaint)
}

private fun drawWindowBreak(
    canvas: Canvas,
    start: ScreenPoint,
    end: ScreenPoint,
    paint: Paint
) {
    val centerX = (start.x + end.x) / 2f
    val centerY = (start.y + end.y) / 2f
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val normalX = -dy / length
    val normalY = dx / length
    val marker = 10f
    canvas.drawLine(
        centerX - normalX * marker,
        centerY - normalY * marker,
        centerX + normalX * marker,
        centerY + normalY * marker,
        paint
    )
}

private fun drawStairBreak(
    canvas: Canvas,
    start: ScreenPoint,
    end: ScreenPoint,
    paint: Paint,
    upDirection: Boolean
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val nx = -dy / length
    val ny = dx / length
    val depth = (length * 0.42f).coerceIn(8f, 18f)

    val a = start
    val b = end
    val c = ScreenPoint(x = end.x + (nx * depth), y = end.y + (ny * depth))
    val d = ScreenPoint(x = start.x + (nx * depth), y = start.y + (ny * depth))

    fun drawEdge(from: ScreenPoint, to: ScreenPoint) {
        canvas.drawLine(from.x, from.y, to.x, to.y, paint)
    }
    drawEdge(a, b)
    drawEdge(b, c)
    drawEdge(c, d)
    drawEdge(d, a)

    val stepCount = 4
    for (index in 1 until stepCount) {
        val t = index.toFloat() / stepCount.toFloat()
        val sx = a.x + ((d.x - a.x) * t)
        val sy = a.y + ((d.y - a.y) * t)
        val ex = b.x + ((c.x - b.x) * t)
        val ey = b.y + ((c.y - b.y) * t)
        canvas.drawLine(sx, sy, ex, ey, paint)
    }

    val direction = if (upDirection) 1f else -1f
    val ux = dx / length
    val uy = dy / length
    val centerX = (a.x + b.x + c.x + d.x) / 4f
    val centerY = (a.y + b.y + c.y + d.y) / 4f
    val arrowHalf = (length * 0.2f).coerceIn(6f, 12f)
    val fromX = centerX - (ux * arrowHalf * direction)
    val fromY = centerY - (uy * arrowHalf * direction)
    val toX = centerX + (ux * arrowHalf * direction)
    val toY = centerY + (uy * arrowHalf * direction)
    canvas.drawLine(fromX, fromY, toX, toY, paint)
    val head = 5f
    canvas.drawLine(
        toX,
        toY,
        toX - (ux * head * direction) + (nx * head * 0.65f),
        toY - (uy * head * direction) + (ny * head * 0.65f),
        paint
    )
    canvas.drawLine(
        toX,
        toY,
        toX - (ux * head * direction) - (nx * head * 0.65f),
        toY - (uy * head * direction) - (ny * head * 0.65f),
        paint
    )
}

private fun drawEnvelopeDimensions(
    canvas: Canvas,
    contentRect: RectF,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    scale: Float
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
    val widthMm = (maxX - minX).coerceAtLeast(0.0)
    val heightMm = (maxY - minY).coerceAtLeast(0.0)
    val widthPx = (widthMm * scale).toFloat()
    val heightPx = (heightMm * scale).toFloat()

    val x1 = contentRect.left
    val x2 = contentRect.left + widthPx
    val y = contentRect.top - 16f
    canvas.drawLine(x1, y, x2, y, dimLinePaint)
    canvas.drawLine(x1, y - 8f, x1, y + 8f, dimLinePaint)
    canvas.drawLine(x2, y - 8f, x2, y + 8f, dimLinePaint)
    canvas.drawText("${"%.1f".format(Millimeters(widthMm.roundToLong()).toFeet())} ft", ((x1 + x2) / 2f) - 36f, y - 6f, dimTextPaint)

    val vx = contentRect.right + 16f
    val vy1 = contentRect.bottom - heightPx
    val vy2 = contentRect.bottom
    canvas.drawLine(vx, vy1, vx, vy2, dimLinePaint)
    canvas.drawLine(vx - 8f, vy1, vx + 8f, vy1, dimLinePaint)
    canvas.drawLine(vx - 8f, vy2, vx + 8f, vy2, dimLinePaint)
    canvas.drawText("${"%.1f".format(Millimeters(heightMm.roundToLong()).toFeet())} ft", vx + 10f, ((vy1 + vy2) / 2f), dimTextPaint)
}

private fun renderBlueprintPdfBytes(
    projectName: String,
    document: BlueprintDocument,
    includeGrid: Boolean
): ByteArray {
    val pdfDocument = PdfDocument()
    val pageWidth = 1654
    val pageHeight = 2339
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    canvas.drawColor(Color.rgb(10, 19, 35))
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
        document = document,
        widthPx = 1800,
        heightPx = 1800,
        includeGrid = includeGrid
    )
    val margin = 56
    val blueprintTop = 150
    val blueprintBottom = pageHeight - 170
    val blueprintRect = Rect(margin, blueprintTop, pageWidth - margin, blueprintBottom)
    canvas.drawBitmap(bitmap, null, blueprintRect, null)

    val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(205, 222, 245)
        textSize = 18f
    }
    val totalWallFeet = document.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    canvas.drawText(
        "Rooms ${document.rooms.size}  •  Walls ${document.walls.size}  •  Openings ${document.openings.size}  •  Wall length ${"%.1f".format(totalWallFeet)} ft",
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

    pdfDocument.finishPage(page)
    return ByteArrayOutputStream().use { output ->
        pdfDocument.writeTo(output)
        pdfDocument.close()
        output.toByteArray()
    }
}

private data class WorldBounds(
    val minX: Long,
    val maxX: Long,
    val minY: Long,
    val maxY: Long
)

private data class ScreenPoint(
    val x: Float,
    val y: Float
)

private fun BlueprintDocument.hasGeometry(): Boolean {
    return walls.isNotEmpty() || rooms.any { it.polygon.size >= 3 }
}

private fun BlueprintDocument.worldPoints(): List<PointMm> {
    return buildList {
        walls.forEach { wall ->
            add(wall.start)
            add(wall.end)
        }
        rooms.forEach { room ->
            addAll(room.polygon)
        }
    }
}

private fun List<PointMm>.bounds(): WorldBounds {
    var minX = Long.MAX_VALUE
    var maxX = Long.MIN_VALUE
    var minY = Long.MAX_VALUE
    var maxY = Long.MIN_VALUE
    forEach { point ->
        minX = min(minX, point.x)
        maxX = max(maxX, point.x)
        minY = min(minY, point.y)
        maxY = max(maxY, point.y)
    }
    if (isEmpty()) {
        return WorldBounds(minX = -1000, maxX = 1000, minY = -1000, maxY = 1000)
    }
    return WorldBounds(minX = minX, maxX = maxX, minY = minY, maxY = maxY)
}

private fun List<PointMm>.centroid(): PointMm {
    if (isEmpty()) return PointMm(0, 0)
    val sumX = sumOf { it.x }
    val sumY = sumOf { it.y }
    return PointMm(
        x = ceil(sumX.toDouble() / size.toDouble()).toLong(),
        y = ceil(sumY.toDouble() / size.toDouble()).toLong()
    )
}

