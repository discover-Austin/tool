package com.tradesketch.estimator.domain.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

data class PointMm(
    val x: Long,
    val y: Long
)

enum class WallType {
    INTERIOR,
    EXTERIOR,
    FOUNDATION,
    PARTITION
}

data class WallSegment(
    val id: String,
    val start: PointMm,
    val end: PointMm,
    val thickness: Millimeters = Millimeters.fromInches(4.5),
    val height: Millimeters = Millimeters.fromFeet(9.0),
    val type: WallType = WallType.INTERIOR
) {
    fun lengthMillimeters(): Long {
        return hypot(
            (end.x - start.x).toDouble(),
            (end.y - start.y).toDouble()
        ).roundToLong()
    }

    fun lengthFeet(): Double = Millimeters(lengthMillimeters()).toFeet()

    fun angleDegrees(): Double {
        return Math.toDegrees(
            atan2(
                (end.y - start.y).toDouble(),
                (end.x - start.x).toDouble()
            )
        )
    }
}

data class CeilingSpec(
    val enabled: Boolean = true,
    val height: Millimeters = Millimeters.fromFeet(9.0)
)

data class Room(
    val id: String,
    val name: String,
    val polygon: List<PointMm>,
    val wallSegmentIds: List<String> = emptyList(),
    val tags: Set<String> = emptySet(),
    val ceiling: CeilingSpec = CeilingSpec()
) {
    fun areaSquareMillimeters(): Long {
        if (polygon.size < 3) return 0L
        var twiceArea = 0.0
        for (index in polygon.indices) {
            val a = polygon[index]
            val b = polygon[(index + 1) % polygon.size]
            twiceArea += (a.x.toDouble() * b.y.toDouble()) - (b.x.toDouble() * a.y.toDouble())
        }
        return (abs(twiceArea) / 2.0).roundToLong()
    }

    fun perimeterMillimeters(): Long {
        if (polygon.size < 2) return 0L
        var total = 0.0
        for (index in polygon.indices) {
            val a = polygon[index]
            val b = polygon[(index + 1) % polygon.size]
            total += hypot(
                (b.x - a.x).toDouble(),
                (b.y - a.y).toDouble()
            )
        }
        return total.roundToLong()
    }

    fun areaSqFt(): Double {
        return areaSquareMillimeters().toDouble() / 92_903.04
    }

    fun perimeterFeet(): Double {
        return Millimeters(perimeterMillimeters()).toFeet()
    }

    fun wallSurfaceAreaSqFt(openingAreaSqFt: Double = 0.0): Double {
        val gross = perimeterFeet() * ceiling.height.toFeet()
        return (gross - openingAreaSqFt).coerceAtLeast(0.0)
    }
}

data class MaterialParams(
    val paintCoats: Int = 2,
    val drywallSheetAreaSqFt: Double = 32.0,
    val wasteFactorPercent: Double = 10.0,
    val concreteThicknessFeet: Double = 0.33
)

enum class BlueprintTool {
    SELECT,
    DRAW_WALL,
    PLACE_DOOR,
    PLACE_WINDOW,
    PAN,
    MEASURE
}

data class BlueprintSnapSettings(
    val gridEnabled: Boolean = true,
    val endpointEnabled: Boolean = true,
    val midpointEnabled: Boolean = true,
    val angleEnabled: Boolean = true,
    val closureEnabled: Boolean = true,
    val gridStepFeet: Double = 1.0,
    val angleIncrementDegrees: Int = 15,
    val thresholdFeet: Double = 0.75
)

fun Space.toWallSegmentOrNull(): WallSegment? {
    val wall = geometry as? Geometry.Wall ?: return null
    val halfLength = wall.length.toFeet() / 2.0
    val yawRadians = Math.toRadians(transform.yawDegrees)
    val dx = cos(yawRadians) * halfLength
    val dy = sin(yawRadians) * halfLength
    val centerX = Millimeters.fromFeet(transform.xFeet).value
    val centerY = Millimeters.fromFeet(transform.zFeet).value
    val start = PointMm(
        x = (centerX - Millimeters.fromFeet(dx).value),
        y = (centerY - Millimeters.fromFeet(dy).value)
    )
    val end = PointMm(
        x = (centerX + Millimeters.fromFeet(dx).value),
        y = (centerY + Millimeters.fromFeet(dy).value)
    )
    return WallSegment(
        id = id,
        start = start,
        end = end,
        height = wall.height
    )
}

fun Project.wallSegments(): List<WallSegment> {
    return spaces.mapNotNull { it.toWallSegmentOrNull() }
}
