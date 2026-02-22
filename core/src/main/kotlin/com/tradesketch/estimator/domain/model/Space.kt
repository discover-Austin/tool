package com.tradesketch.estimator.domain.model

import java.util.UUID

enum class OpeningType {
    DOOR,
    WINDOW,
    STAIR_UP,
    STAIR_DOWN
}

data class Opening(
    val width: Millimeters,
    val height: Millimeters,
    val count: Int = 1,
    val type: OpeningType = if (height.toFeet() >= 6.0) OpeningType.DOOR else OpeningType.WINDOW,
    val wallPositionT: Double = 0.5,
    val sillHeight: Millimeters = Millimeters(0),
    val id: String = UUID.randomUUID().toString()
)

data class SpaceTransform(
    val xFeet: Double = 0.0,
    val yFeet: Double = 0.0,
    val zFeet: Double = 0.0,
    val yawDegrees: Double = 0.0,
    val colorHex: Long = 0xFF4E79A7
)

data class Space(
    val id: String,
    val name: String,
    val geometry: Geometry,
    val tags: Set<String> = emptySet(),
    val openings: List<Opening> = emptyList(),
    val transform: SpaceTransform = SpaceTransform()
)

fun Space.openingsAreaSqFt(): Double {
    return openings.sumOf { opening ->
        val area = areaSqFt(opening.width, opening.height)
        area * opening.count.coerceAtLeast(0)
    }
}
