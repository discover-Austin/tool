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
import android.os.Build
import com.tradesketch.estimator.domain.calc.BlueprintSnapMath
import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

internal object BlueprintExportManager {
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
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) {
            return@withContext ExportResult.Failure("Add at least one wall, room, or opening before exporting a blueprint.")
        }
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
        try {
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
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun createBlueprintShareIntent(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): ExportResult<Intent> = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) {
            return@withContext ExportResult.Failure("Add at least one wall, room, or opening before exporting a blueprint.")
        }
        runCatching {
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
            try {
                FileOutputStream(shareFile).use { output ->
                    val wroteImage = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.flush()
                    require(wroteImage) { "Failed to compress blueprint PNG." }
                }
                require(shareFile.length() > 0L) { "Generated blueprint PNG was empty." }
                ExportStorage.createShareIntent(
                    context = context,
                    shareFile = shareFile,
                    mimeType = "image/png",
                    subject = "$projectName Blueprint",
                    text = "Blueprint export from TradeSketch",
                    chooserTitle = "Share Blueprint"
                )
            } finally {
                bitmap.recycle()
            }
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not prepare the blueprint image for sharing.",
                    cause = error
                )
            }
        )
    }

    suspend fun saveBlueprintPdfToDownloads(
        context: Context,
        projectName: String,
        document: BlueprintDocument,
        includeGrid: Boolean = true
    ): ExportResult<SavedExport> = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) {
            return@withContext ExportResult.Failure("Add at least one wall, room, or opening before exporting a blueprint.")
        }
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
    ): ExportResult<Intent> = withContext(Dispatchers.IO) {
        if (!document.hasGeometry()) {
            return@withContext ExportResult.Failure("Add at least one wall, room, or opening before exporting a blueprint.")
        }
        runCatching {
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
            require(pdfBytes.isNotEmpty()) { "Generated blueprint PDF was empty." }
            FileOutputStream(shareFile).use { output ->
                output.write(pdfBytes)
                output.flush()
            }
            require(shareFile.length() > 0L) { "Generated blueprint PDF file was empty." }
            ExportStorage.createShareIntent(
                context = context,
                shareFile = shareFile,
                mimeType = "application/pdf",
                subject = "$projectName Blueprint PDF",
                text = "Blueprint PDF export from TradeSketch",
                chooserTitle = "Share Blueprint PDF"
            )
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error ->
                ExportResult.Failure(
                    userMessage = "Could not prepare the blueprint PDF for sharing.",
                    cause = error
                )
            }
        )
    }
    fun renderBlueprintBitmap(
        projectName: String,
        document: BlueprintDocument,
        widthPx: Int = 1800,
        heightPx: Int = 1800,
        includeGrid: Boolean = true
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(1000)
        val safeHeight = heightPx.coerceAtLeast(1000)
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

        drawBlueprintScene(
            canvas = canvas,
            document = document,
            points = points,
            viewportRect = contentRect,
            includeGrid = includeGrid
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

    internal fun renderBlueprintSnapshotBitmap(
        document: BlueprintDocument,
        widthPx: Int = 1600,
        heightPx: Int = 560,
        includeGrid: Boolean = true
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(1200)
        val safeHeight = heightPx.coerceAtLeast(420)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 19, 35)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat(), backgroundPaint)

        val points = document.worldPoints()
        if (points.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(196, 208, 229)
                textSize = 38f
            }
            canvas.drawText("No blueprint geometry to render", 44f, (safeHeight / 2f) + 14f, emptyPaint)
            return bitmap
        }

        drawBlueprintScene(
            canvas = canvas,
            document = document,
            points = points,
            viewportRect = RectF(52f, 42f, safeWidth - 52f, safeHeight - 44f),
            includeGrid = includeGrid
        )
        return bitmap
    }
}

private fun drawBlueprintScene(
    canvas: Canvas,
    document: BlueprintDocument,
    points: List<PointMm>,
    viewportRect: RectF,
    includeGrid: Boolean
) {
    if (points.isEmpty()) return
    val layout = resolveBlueprintRenderFrame(
        contentLeft = viewportRect.left,
        contentTop = viewportRect.top,
        contentRight = viewportRect.right,
        contentBottom = viewportRect.bottom,
        points = points
    )

    if (includeGrid) {
        drawGrid(
            canvas = canvas,
            contentRect = layout.frameRect,
            minX = layout.worldMinX,
            maxX = layout.worldMaxX,
            minY = layout.worldMinY,
            maxY = layout.worldMaxY,
            scale = layout.scale
        )
    }
    drawRooms(canvas, document.rooms, layout::toScreen)
    drawWalls(canvas, document.walls, layout::toScreen, layout.scale)
    drawOpenings(canvas, document, layout::toScreen, layout.scale)
    drawEnvelopeDimensions(
        canvas = canvas,
        geometryRect = layout.geometryRect,
        safeRect = viewportRect,
        widthMm = (layout.geometryBounds.maxX - layout.geometryBounds.minX).toDouble().coerceAtLeast(0.0),
        heightMm = (layout.geometryBounds.maxY - layout.geometryBounds.minY).toDouble().coerceAtLeast(0.0)
    )
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
    geometryRect: RectF,
    safeRect: RectF,
    widthMm: Double,
    heightMm: Double
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
    val widthLabel = "${"%.1f".format(Millimeters(widthMm.roundToLong()).toFeet())} ft"
    val heightLabel = "${"%.1f".format(Millimeters(heightMm.roundToLong()).toFeet())} ft"
    val x1 = geometryRect.left
    val x2 = geometryRect.right
    val y = max(safeRect.top + 18f, geometryRect.top - 26f)
    canvas.drawLine(x1, y, x2, y, dimLinePaint)
    canvas.drawLine(x1, y - 8f, x1, y + 8f, dimLinePaint)
    canvas.drawLine(x2, y - 8f, x2, y + 8f, dimLinePaint)
    val widthTextWidth = dimTextPaint.measureText(widthLabel)
    val widthTextX = (((x1 + x2) / 2f) - (widthTextWidth / 2f))
        .coerceIn(safeRect.left + 12f, safeRect.right - widthTextWidth - 12f)
    val widthTextY = max(safeRect.top + dimTextPaint.textSize + 4f, y - 8f)
    canvas.drawText(widthLabel, widthTextX, widthTextY, dimTextPaint)

    val vx = min(safeRect.right - 24f, geometryRect.right + 28f)
    val vy1 = geometryRect.top
    val vy2 = geometryRect.bottom
    canvas.drawLine(vx, vy1, vx, vy2, dimLinePaint)
    canvas.drawLine(vx - 8f, vy1, vx + 8f, vy1, dimLinePaint)
    canvas.drawLine(vx - 8f, vy2, vx + 8f, vy2, dimLinePaint)
    val heightTextWidth = dimTextPaint.measureText(heightLabel)
    val heightTextX = min(safeRect.right - heightTextWidth - 12f, vx + 12f)
    val heightTextY = ((vy1 + vy2) / 2f)
        .coerceIn(safeRect.top + dimTextPaint.textSize + 8f, safeRect.bottom - 12f)
    canvas.drawText(heightLabel, heightTextX, heightTextY, dimTextPaint)
}

internal data class FloatRectBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

internal data class BlueprintPdfLayout(
    val contentRect: FloatRectBounds,
    val headerAccentRect: FloatRectBounds,
    val titleBaseline: Float,
    val subtitleBaseline: Float,
    val headerDividerY: Float,
    val drawingFrameRect: FloatRectBounds,
    val drawingViewportRect: FloatRectBounds,
    val footerRect: FloatRectBounds
)

internal fun resolveBlueprintPdfLayout(
    pageWidth: Int,
    pageHeight: Int
): BlueprintPdfLayout {
    val pageMargin = 64f
    val contentRect = FloatRectBounds(
        pageMargin,
        pageMargin,
        pageWidth - pageMargin,
        pageHeight - pageMargin
    )
    val headerDividerY = contentRect.top + 118f
    val footerRect = FloatRectBounds(
        contentRect.left,
        contentRect.bottom - 118f,
        contentRect.right,
        contentRect.bottom
    )
    val drawingFrameRect = FloatRectBounds(
        contentRect.left,
        headerDividerY + 36f,
        contentRect.right,
        footerRect.top - 36f
    )
    return BlueprintPdfLayout(
        contentRect = contentRect,
        headerAccentRect = FloatRectBounds(
            contentRect.left,
            contentRect.top + 10f,
            contentRect.left + 12f,
            contentRect.top + 78f
        ),
        titleBaseline = contentRect.top + 46f,
        subtitleBaseline = contentRect.top + 84f,
        headerDividerY = headerDividerY,
        drawingFrameRect = drawingFrameRect,
        drawingViewportRect = FloatRectBounds(
            drawingFrameRect.left + 54f,
            drawingFrameRect.top + 60f,
            drawingFrameRect.right - 92f,
            drawingFrameRect.bottom - 52f
        ),
        footerRect = footerRect
    )
}

private fun insetRect(
    rect: RectF,
    dx: Float,
    dy: Float = dx
): RectF {
    return RectF(rect.left + dx, rect.top + dy, rect.right - dx, rect.bottom - dy)
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
    val layout = resolveBlueprintPdfLayout(pageWidth = pageWidth, pageHeight = pageHeight)
    val points = document.worldPoints()
    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    val contentRect = layout.contentRect.toRectF()
    val headerAccentRect = layout.headerAccentRect.toRectF()
    val drawingFrameRect = layout.drawingFrameRect.toRectF()
    val drawingViewportRect = layout.drawingViewportRect.toRectF()
    val footerRect = layout.footerRect.toRectF()

    val pageBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 134, 167, 205)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(242, 196, 59)
        style = Paint.Style.FILL
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        isFakeBoldText = true
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(188, 207, 235)
        textSize = 20f
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(130, 102, 129, 170)
        strokeWidth = 2f
    }
    val frameFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 40, 61)
        style = Paint.Style.FILL
    }
    val frameStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 124, 147, 166)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val paperFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(246, 243, 233)
        style = Paint.Style.FILL
    }
    val paperStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 70, 89, 106)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val footerSummaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(205, 222, 245)
        textSize = 18f
    }
    val footerCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(166, 190, 219)
        textSize = 17f
    }
    val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(196, 208, 229)
        textSize = 34f
    }

    canvas.drawColor(Color.rgb(10, 19, 35))
    canvas.drawRect(contentRect, pageBorderPaint)
    canvas.drawRect(headerAccentRect, accentPaint)
    val titleLeft = layout.headerAccentRect.right + 20f
    canvas.drawText("$projectName Blueprint", titleLeft, layout.titleBaseline, titlePaint)
    canvas.drawText("Exported $stamp", titleLeft, layout.subtitleBaseline, subtitlePaint)
    canvas.drawLine(
        layout.contentRect.left,
        layout.headerDividerY,
        layout.contentRect.right,
        layout.headerDividerY,
        dividerPaint
    )

    canvas.drawRoundRect(drawingFrameRect, 22f, 22f, frameFillPaint)
    canvas.drawRoundRect(drawingFrameRect, 22f, 22f, frameStrokePaint)
    val drawingPaperRect = insetRect(drawingFrameRect, 10f)
    canvas.drawRoundRect(drawingPaperRect, 16f, 16f, paperFillPaint)
    canvas.drawRoundRect(drawingPaperRect, 16f, 16f, paperStrokePaint)

    if (points.isEmpty()) {
        canvas.drawText(
            "No blueprint geometry to render",
            drawingPaperRect.left + 42f,
            drawingPaperRect.centerY(),
            emptyPaint
        )
    } else {
        drawBlueprintScene(
            canvas = canvas,
            document = document,
            points = points,
            viewportRect = drawingViewportRect,
            includeGrid = includeGrid
        )
    }

    canvas.drawLine(
        layout.footerRect.left,
        layout.footerRect.top,
        layout.footerRect.right,
        layout.footerRect.top,
        dividerPaint
    )
    val totalWallFeet = document.walls.sumOf { Millimeters(it.lengthMillimeters()).toFeet() }
    canvas.drawText(
        "Rooms ${document.rooms.size}  •  Walls ${document.walls.size}  •  Openings ${document.openings.size}  •  Wall length ${"%.1f".format(totalWallFeet)} ft",
        footerRect.left,
        footerRect.top + 40f,
        footerSummaryPaint
    )
    canvas.drawText(
        "Generated by TradeSketch",
        footerRect.left,
        footerRect.top + 76f,
        footerCaptionPaint
    )

    pdfDocument.finishPage(page)
    return ByteArrayOutputStream().use { output ->
        try {
            pdfDocument.writeTo(output)
            output.toByteArray()
        } finally {
            pdfDocument.close()
        }
    }
}

internal data class FittedRectBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

internal fun aspectFitBounds(
    sourceWidth: Int,
    sourceHeight: Int,
    boundsLeft: Int,
    boundsTop: Int,
    boundsRight: Int,
    boundsBottom: Int
): FittedRectBounds {
    val boundsWidth = boundsRight - boundsLeft
    val boundsHeight = boundsBottom - boundsTop
    if (
        sourceWidth <= 0 ||
        sourceHeight <= 0 ||
        boundsWidth <= 0 ||
        boundsHeight <= 0
    ) {
        return FittedRectBounds(
            left = boundsLeft,
            top = boundsTop,
            right = boundsRight,
            bottom = boundsBottom
        )
    }
    val widthScale = boundsWidth.toFloat() / sourceWidth.toFloat()
    val heightScale = boundsHeight.toFloat() / sourceHeight.toFloat()
    val scale = min(widthScale, heightScale)
    val fittedWidth = (sourceWidth * scale).roundToLong().toInt().coerceAtMost(boundsWidth)
    val fittedHeight = (sourceHeight * scale).roundToLong().toInt().coerceAtMost(boundsHeight)
    val left = boundsLeft + ((boundsWidth - fittedWidth) / 2)
    val top = boundsTop + ((boundsHeight - fittedHeight) / 2)
    return FittedRectBounds(
        left = left,
        top = top,
        right = left + fittedWidth,
        bottom = top + fittedHeight
    )
}

internal fun aspectFitRect(
    sourceWidth: Int,
    sourceHeight: Int,
    bounds: Rect
): Rect {
    val fitted = aspectFitBounds(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        boundsLeft = bounds.left,
        boundsTop = bounds.top,
        boundsRight = bounds.right,
        boundsBottom = bounds.bottom
    )
    return Rect(fitted.left, fitted.top, fitted.right, fitted.bottom)
}

internal data class WorldBounds(
    val minX: Long,
    val maxX: Long,
    val minY: Long,
    val maxY: Long
)

internal data class BlueprintRenderFrame(
    val frameLeft: Float,
    val frameTop: Float,
    val frameRight: Float,
    val frameBottom: Float,
    val geometryLeft: Float,
    val geometryTop: Float,
    val geometryRight: Float,
    val geometryBottom: Float,
    val geometryBounds: WorldBounds,
    val worldMinX: Double,
    val worldMaxX: Double,
    val worldMinY: Double,
    val worldMaxY: Double,
    val scale: Float
) {
    val frameRect: RectF
        get() = RectF(frameLeft, frameTop, frameRight, frameBottom)

    val geometryRect: RectF
        get() = RectF(geometryLeft, geometryTop, geometryRight, geometryBottom)

    fun toScreen(point: PointMm): ScreenPoint {
        return ScreenPoint(
            x = frameLeft + ((point.x - worldMinX).toFloat() * scale),
            y = frameBottom - ((point.y - worldMinY).toFloat() * scale)
        )
    }
}

internal data class ScreenPoint(
    val x: Float,
    val y: Float
)

internal fun resolveBlueprintRenderFrame(
    contentLeft: Float,
    contentTop: Float,
    contentRight: Float,
    contentBottom: Float,
    points: List<PointMm>
): BlueprintRenderFrame {
    val geometryBounds = points.bounds()
    val spanX = max(1.0, (geometryBounds.maxX - geometryBounds.minX).toDouble())
    val spanY = max(1.0, (geometryBounds.maxY - geometryBounds.minY).toDouble())
    val pad = max(1500.0, max(spanX, spanY) * 0.1)

    val worldMinX = geometryBounds.minX - pad
    val worldMaxX = geometryBounds.maxX + pad
    val worldMinY = geometryBounds.minY - pad
    val worldMaxY = geometryBounds.maxY + pad
    val worldWidth = max(1.0, worldMaxX - worldMinX)
    val worldHeight = max(1.0, worldMaxY - worldMinY)
    val contentWidth = contentRight - contentLeft
    val contentHeight = contentBottom - contentTop
    val scale = min(
        contentWidth / worldWidth.toFloat(),
        contentHeight / worldHeight.toFloat()
    )

    val fittedWidth = worldWidth.toFloat() * scale
    val fittedHeight = worldHeight.toFloat() * scale
    val frameLeft = contentLeft + ((contentWidth - fittedWidth) / 2f)
    val frameTop = contentTop + ((contentHeight - fittedHeight) / 2f)
    val frameRight = frameLeft + fittedWidth
    val frameBottom = frameTop + fittedHeight

    val frame = BlueprintRenderFrame(
        frameLeft,
        frameTop,
        frameRight,
        frameBottom,
        geometryLeft = 0f,
        geometryTop = 0f,
        geometryRight = 0f,
        geometryBottom = 0f,
        geometryBounds = geometryBounds,
        worldMinX = worldMinX,
        worldMaxX = worldMaxX,
        worldMinY = worldMinY,
        worldMaxY = worldMaxY,
        scale = scale
    )
    val topLeft = frame.toScreen(PointMm(geometryBounds.minX, geometryBounds.maxY))
    val bottomRight = frame.toScreen(PointMm(geometryBounds.maxX, geometryBounds.minY))

    return frame.copy(
        geometryLeft = min(topLeft.x, bottomRight.x),
        geometryTop = min(topLeft.y, bottomRight.y),
        geometryRight = max(topLeft.x, bottomRight.x),
        geometryBottom = max(topLeft.y, bottomRight.y)
    )
}

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





