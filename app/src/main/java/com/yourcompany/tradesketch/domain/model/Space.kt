package com.yourcompany.tradesketch.domain.model

data class Opening(
    val width: Millimeters,
    val height: Millimeters,
    val count: Int
)

data class Space(
    val id: String,
    val name: String,
    val geometry: Geometry,
    val openings: List<Opening> = emptyList()
)

fun Space.openingsAreaSqFt(): Double {
    return openings.sumOf { opening ->
        val area = areaSqFt(opening.width, opening.height)
        area * opening.count
    }
}
