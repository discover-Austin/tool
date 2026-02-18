package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

object BlueprintSnapMath {
    fun distanceMillimeters(a: PointMm, b: PointMm): Long {
        return hypot(
            (a.x - b.x).toDouble(),
            (a.y - b.y).toDouble()
        ).roundToLong()
    }

    fun roomClosureSnap(
        candidateEnd: PointMm,
        roomStart: PointMm,
        thresholdMm: Long
    ): PointMm? {
        if (thresholdMm <= 0L) return null
        return if (distanceMillimeters(candidateEnd, roomStart) <= thresholdMm) {
            roomStart
        } else {
            null
        }
    }

    fun projectToWallT(point: PointMm, wall: WallSegment): Double {
        val ax = wall.start.x.toDouble()
        val ay = wall.start.y.toDouble()
        val bx = wall.end.x.toDouble()
        val by = wall.end.y.toDouble()
        val abx = bx - ax
        val aby = by - ay
        val lengthSquared = (abx * abx) + (aby * aby)
        if (lengthSquared <= 0.0001) return 0.0
        val apx = point.x.toDouble() - ax
        val apy = point.y.toDouble() - ay
        return ((apx * abx) + (apy * aby)) / lengthSquared
    }

    fun pointToWallDistanceMm(point: PointMm, wall: WallSegment): Long {
        val t = projectToWallT(point, wall).coerceIn(0.0, 1.0)
        val x = wall.start.x + ((wall.end.x - wall.start.x) * t).roundToLong()
        val y = wall.start.y + ((wall.end.y - wall.start.y) * t).roundToLong()
        return distanceMillimeters(point, PointMm(x, y))
    }

    fun chainStart(lastWall: WallSegment?, detachedMode: Boolean): PointMm? {
        if (detachedMode) return null
        return lastWall?.end
    }

    fun applySnapping(
        rawPoint: PointMm,
        drawingStart: PointMm?,
        settings: BlueprintSnapSettings,
        walls: List<WallSegment>
    ): PointMm {
        var snapped = rawPoint
        val thresholdMm = Millimeters.fromFeet(settings.thresholdFeet).value.coerceAtLeast(1L)

        if (settings.gridEnabled) {
            val gridStepMm = Millimeters.fromFeet(settings.gridStepFeet).value.coerceAtLeast(1L)
            snapped = PointMm(
                x = snapToStep(snapped.x, gridStepMm),
                y = snapToStep(snapped.y, gridStepMm)
            )
        }

        if (settings.endpointEnabled || settings.midpointEnabled) {
            val candidatePoints = buildList {
                walls.forEach { wall ->
                    if (settings.endpointEnabled) {
                        add(wall.start)
                        add(wall.end)
                    }
                    if (settings.midpointEnabled) {
                        add(wall.midpoint())
                    }
                }
            }
            candidatePoints.minByOrNull { point -> distanceMillimeters(point, snapped) }?.let { nearest ->
                if (distanceMillimeters(nearest, snapped) <= thresholdMm) {
                    snapped = nearest
                }
            }
        }

        if (drawingStart != null && settings.angleEnabled) {
            val dx = (snapped.x - drawingStart.x).toDouble()
            val dy = (snapped.y - drawingStart.y).toDouble()
            val length = hypot(dx, dy)
            if (length > 0.0) {
                val currentAngle = Math.toDegrees(kotlin.math.atan2(dy, dx))
                val increment = settings.angleIncrementDegrees.coerceAtLeast(1)
                val snappedAngle = (currentAngle / increment).roundToLong() * increment.toDouble()
                val radians = Math.toRadians(snappedAngle)
                val snappedX = drawingStart.x + (cos(radians) * length).roundToLong()
                val snappedY = drawingStart.y + (sin(radians) * length).roundToLong()
                val anglePoint = PointMm(snappedX, snappedY)
                if (distanceMillimeters(anglePoint, snapped) <= thresholdMm * 2) {
                    snapped = anglePoint
                }
            }
        }

        return snapped
    }

    fun placeOpeningAlongWall(
        wall: WallSegment,
        tapPointMm: PointMm,
        widthMm: Long,
        heightMm: Long,
        sillMm: Long,
        type: OpeningType,
        openingId: String
    ): BlueprintOpening {
        val t = projectToWallT(tapPointMm, wall).coerceIn(0.0, 1.0)
        return BlueprintOpening(
            id = openingId,
            wallId = wall.id,
            t = t,
            widthMm = widthMm.coerceAtLeast(1L),
            heightMm = heightMm.coerceAtLeast(1L),
            sillMm = sillMm.coerceAtLeast(0L),
            type = type
        ).normalized()
    }

    private fun snapToStep(value: Long, step: Long): Long {
        if (step <= 0L) return value
        return ((value.toDouble() / step.toDouble()).roundToLong()) * step
    }
}
