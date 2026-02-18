package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.hypot
import kotlin.math.roundToLong

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
}
