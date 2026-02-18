package com.tradesketch.estimator.domain.model

data class Opening(
    val width: Millimeters,
    val height: Millimeters,
    val count: Int
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
    val openings: List<Opening> = emptyList(),
    val transform: SpaceTransform = SpaceTransform()
)

fun Space.openingsAreaSqFt(): Double {
    return openings.sumOf { opening ->
        val area = areaSqFt(opening.width, opening.height)
        area * opening.count
    }
}
