package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintOpening
import com.tradesketch.estimator.domain.model.BlueprintSnapSettings
import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.OpeningType
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sin

object BlueprintSnapMath {
    private const val CONNECTED_ENDPOINT_TOLERANCE_MM = 120L
    private const val RIGHT_ANGLE_FAVOR_DEGREES = 14.0
    private const val PARALLEL_ANGLE_FAVOR_DEGREES = 8.0
    private val PRIORITIZED_ANGLE_STEPS_DEGREES = listOf(90.0, 45.0, 22.5, 11.25)

    fun distanceMillimeters(a: PointMm, b: PointMm): Long {
        return hypot(
            (a.x - b.x).toDouble(),
            (a.y - b.y).toDouble()
        ).roundToLong()
    }

    fun roomClosureSnap(
        candidateEnd: PointMm,
        roomStart: PointMm,
        thresholdMm: Long,
        walls: List<WallSegment> = emptyList()
    ): PointMm? {
        if (thresholdMm <= 0L) return null
        if (distanceMillimeters(candidateEnd, roomStart) <= thresholdMm) {
            return roomStart
        }

        if (walls.isEmpty()) return null
        val endpointJoinToleranceMm = (thresholdMm / 2L).coerceAtLeast(25L)
        val closureToleranceMm = max(thresholdMm.toDouble() * 1.2, thresholdMm.toDouble()).roundToLong()
        val clusters = mutableListOf<MutableList<PointMm>>()
        walls.asSequence()
            .flatMap { wall -> sequenceOf(wall.start, wall.end) }
            .forEach { endpoint ->
                val index = clusters.indexOfFirst { cluster ->
                    cluster.any { point ->
                        distanceMillimeters(point, endpoint) <= endpointJoinToleranceMm
                    }
                }
                if (index >= 0) {
                    clusters[index].add(endpoint)
                } else {
                    clusters += mutableListOf(endpoint)
                }
            }

        var nearestDangling: PointMm? = null
        var nearestDistance = Long.MAX_VALUE
        clusters.forEach { cluster ->
            val degree = cluster.size
            if (degree > 1) return@forEach
            val center = centroid(cluster)
            val distance = distanceMillimeters(candidateEnd, center)
            if (distance <= closureToleranceMm && distance < nearestDistance) {
                nearestDangling = center
                nearestDistance = distance
            }
        }
        return nearestDangling
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
        return distanceMillimeters(point, pointOnWall(wall, t))
    }

    fun pointOnWall(wall: WallSegment, t: Double): PointMm {
        val clamped = t.coerceIn(0.0, 1.0)
        return PointMm(
            x = wall.start.x + ((wall.end.x - wall.start.x) * clamped).roundToLong(),
            y = wall.start.y + ((wall.end.y - wall.start.y) * clamped).roundToLong()
        )
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
        var snappedToFeaturePoint = false

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
                    snappedToFeaturePoint = true
                }
            }
        }

        if (drawingStart != null && settings.angleEnabled) {
            val dx = (snapped.x - drawingStart.x).toDouble()
            val dy = (snapped.y - drawingStart.y).toDouble()
            val length = hypot(dx, dy)
            if (length > 0.0) {
                val currentAngle = Math.toDegrees(atan2(dy, dx))
                preferredAngleSnapPoint(
                    origin = drawingStart,
                    currentAngle = currentAngle,
                    lengthMm = length,
                    thresholdMm = thresholdMm,
                    currentPoint = snapped,
                    fallbackIncrement = settings.angleIncrementDegrees.coerceAtLeast(1).toDouble()
                )?.let { anglePoint ->
                    snapped = anglePoint
                }
            }
        }

        if (drawingStart != null) {
            snapped = favorPerpendicularToConnectedWall(
                candidate = snapped,
                drawingStart = drawingStart,
                walls = walls,
                thresholdMm = thresholdMm
            )
            snapped = softlyFavorWholeFeetLength(
                candidate = snapped,
                drawingStart = drawingStart,
                thresholdMm = thresholdMm
            )
            snapped = softlyFavorParallelLength(
                candidate = snapped,
                drawingStart = drawingStart,
                walls = walls,
                thresholdMm = thresholdMm
            )
        }

        if (settings.gridEnabled && !snappedToFeaturePoint) {
            val gridStepMm = Millimeters.fromFeet(settings.gridStepFeet).value.coerceAtLeast(1L)
            snapped = PointMm(
                x = snapToStep(snapped.x, gridStepMm),
                y = snapToStep(snapped.y, gridStepMm)
            )
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

    private fun favorPerpendicularToConnectedWall(
        candidate: PointMm,
        drawingStart: PointMm,
        walls: List<WallSegment>,
        thresholdMm: Long
    ): PointMm {
        val currentLength = distanceMillimeters(drawingStart, candidate).toDouble()
        if (currentLength <= 0.0) return candidate

        val endpointTolerance = max(
            CONNECTED_ENDPOINT_TOLERANCE_MM.toDouble(),
            thresholdMm.toDouble() * 0.55
        ).roundToLong()

        val referenceWall = walls
            .map { wall ->
                val endpointDistance = minOf(
                    distanceMillimeters(drawingStart, wall.start),
                    distanceMillimeters(drawingStart, wall.end)
                )
                wall to endpointDistance
            }
            .filter { (_, distance) -> distance <= endpointTolerance }
            .minByOrNull { (_, distance) -> distance }
            ?.first
            ?: return candidate

        val candidateAngle = angleDegrees(drawingStart, candidate)
        val referenceAngle = angleDegrees(referenceWall.start, referenceWall.end)
        val perpendicularA = normalizeAngleDegrees(referenceAngle + 90.0)
        val perpendicularB = normalizeAngleDegrees(referenceAngle - 90.0)
        val deltaA = absoluteAngleDeltaDegrees(candidateAngle, perpendicularA)
        val deltaB = absoluteAngleDeltaDegrees(candidateAngle, perpendicularB)
        val targetAngle = if (deltaA <= deltaB) perpendicularA else perpendicularB
        val nearestDelta = minOf(deltaA, deltaB)
        if (nearestDelta > RIGHT_ANGLE_FAVOR_DEGREES) return candidate
        return pointFromAngleAndLength(
            origin = drawingStart,
            angleDegrees = targetAngle,
            lengthMm = currentLength
        )
    }

    private fun softlyFavorWholeFeetLength(
        candidate: PointMm,
        drawingStart: PointMm,
        thresholdMm: Long
    ): PointMm {
        val lengthMm = distanceMillimeters(drawingStart, candidate).toDouble()
        if (lengthMm <= 0.0) return candidate
        val nearestWholeFeet = (lengthMm / 304.8).roundToLong()
        val targetLengthMm = nearestWholeFeet * 304.8
        val deltaMm = abs(targetLengthMm - lengthMm)
        val nudgeWindowMm = max(42.0, thresholdMm.toDouble() * 0.3)
        if (deltaMm > nudgeWindowMm) return candidate

        val angle = angleDegrees(drawingStart, candidate)
        val nudgedLength = if (deltaMm <= 12.0) {
            targetLengthMm
        } else {
            lengthMm + ((targetLengthMm - lengthMm) * 0.35)
        }
        return pointFromAngleAndLength(
            origin = drawingStart,
            angleDegrees = angle,
            lengthMm = nudgedLength
        )
    }

    private fun softlyFavorParallelLength(
        candidate: PointMm,
        drawingStart: PointMm,
        walls: List<WallSegment>,
        thresholdMm: Long
    ): PointMm {
        val candidateLength = distanceMillimeters(drawingStart, candidate).toDouble()
        if (candidateLength <= 0.0 || walls.isEmpty()) return candidate
        val candidateAngle = angleDegrees(drawingStart, candidate)

        val bestParallel = walls
            .asSequence()
            .map { wall ->
                val parallelDelta = parallelAngleDeltaDegrees(candidateAngle, angleDegrees(wall.start, wall.end))
                val lengthDelta = abs(wall.lengthMillimeters().toDouble() - candidateLength)
                Triple(wall, parallelDelta, lengthDelta)
            }
            .filter { (_, parallelDelta, _) -> parallelDelta <= PARALLEL_ANGLE_FAVOR_DEGREES }
            .minByOrNull { (_, parallelDelta, lengthDelta) ->
                parallelDelta + (lengthDelta / 650.0)
            }
            ?: return candidate

        val targetLength = bestParallel.first.lengthMillimeters().toDouble()
        val lengthDelta = abs(targetLength - candidateLength)
        val nudgeWindowMm = max(72.0, thresholdMm.toDouble() * 0.6)
        if (lengthDelta > nudgeWindowMm) return candidate

        val nudgedLength = if (lengthDelta <= 18.0) {
            targetLength
        } else {
            candidateLength + ((targetLength - candidateLength) * 0.40)
        }
        return pointFromAngleAndLength(
            origin = drawingStart,
            angleDegrees = candidateAngle,
            lengthMm = nudgedLength
        )
    }

    private fun pointFromAngleAndLength(
        origin: PointMm,
        angleDegrees: Double,
        lengthMm: Double
    ): PointMm {
        val radians = Math.toRadians(angleDegrees)
        return PointMm(
            x = origin.x + (cos(radians) * lengthMm).roundToLong(),
            y = origin.y + (sin(radians) * lengthMm).roundToLong()
        )
    }

    private fun preferredAngleSnapPoint(
        origin: PointMm,
        currentAngle: Double,
        lengthMm: Double,
        thresholdMm: Long,
        currentPoint: PointMm,
        fallbackIncrement: Double
    ): PointMm? {
        val angleFamilies = buildList {
            addAll(PRIORITIZED_ANGLE_STEPS_DEGREES)
            if (fallbackIncrement > 0.0 && PRIORITIZED_ANGLE_STEPS_DEGREES.none { abs(it - fallbackIncrement) <= 0.001 }) {
                add(fallbackIncrement)
            }
        }
        val distanceWindowMm = thresholdMm * 2L
        for (step in angleFamilies) {
            if (step <= 0.0) continue
            val snappedAngle = (currentAngle / step).roundToLong() * step
            val delta = absoluteAngleDeltaDegrees(currentAngle, snappedAngle)
            val familyWindow = preferredAngleWindowDegrees(step)
            if (delta > familyWindow) continue
            val candidate = pointFromAngleAndLength(
                origin = origin,
                angleDegrees = snappedAngle,
                lengthMm = lengthMm
            )
            if (distanceMillimeters(candidate, currentPoint) <= distanceWindowMm) {
                return candidate
            }
        }
        return null
    }

    private fun preferredAngleWindowDegrees(step: Double): Double {
        return when {
            abs(step - 90.0) <= 0.001 -> 24.0
            abs(step - 45.0) <= 0.001 -> 17.0
            abs(step - 22.5) <= 0.001 -> 11.0
            abs(step - 11.25) <= 0.001 -> 7.0
            else -> (step * 0.55).coerceIn(4.0, 18.0)
        }
    }

    private fun angleDegrees(from: PointMm, to: PointMm): Double {
        return Math.toDegrees(
            atan2(
                (to.y - from.y).toDouble(),
                (to.x - from.x).toDouble()
            )
        )
    }

    private fun parallelAngleDeltaDegrees(a: Double, b: Double): Double {
        val delta = absoluteAngleDeltaDegrees(a, b)
        return minOf(delta, abs(180.0 - delta))
    }

    private fun absoluteAngleDeltaDegrees(a: Double, b: Double): Double {
        return abs(signedAngleDeltaDegrees(a, b))
    }

    private fun signedAngleDeltaDegrees(from: Double, to: Double): Double {
        var delta = normalizeAngleDegrees(to) - normalizeAngleDegrees(from)
        while (delta > 180.0) delta -= 360.0
        while (delta < -180.0) delta += 360.0
        return delta
    }

    private fun normalizeAngleDegrees(angle: Double): Double {
        val wrapped = angle % 360.0
        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }

    private fun centroid(points: List<PointMm>): PointMm {
        val avgX = points.map { it.x }.average().roundToLong()
        val avgY = points.map { it.y }.average().roundToLong()
        return PointMm(avgX, avgY)
    }
}
