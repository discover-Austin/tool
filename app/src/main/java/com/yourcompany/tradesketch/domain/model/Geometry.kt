package com.yourcompany.tradesketch.domain.model

sealed class Geometry {
    data class Rect(val length: Millimeters, val width: Millimeters) : Geometry()
    data class LShape(val rectA: Rect, val rectB: Rect) : Geometry()
    data class Circle(val radius: Millimeters) : Geometry()
    data class Wall(val length: Millimeters, val height: Millimeters) : Geometry()
    data class Slab(val length: Millimeters, val width: Millimeters, val thickness: Millimeters) : Geometry()
}

fun Geometry.areaSqFt(): Double {
    return when (this) {
        is Geometry.Rect -> areaSqFt(length, width)
        is Geometry.LShape -> areaSqFt(rectA.length, rectA.width) + areaSqFt(rectB.length, rectB.width)
        is Geometry.Circle -> {
            val radiusFeet = radius.toFeet()
            Math.PI * radiusFeet * radiusFeet
        }
        is Geometry.Wall -> areaSqFt(length, height)
        is Geometry.Slab -> areaSqFt(length, width)
    }
}

fun Geometry.volumeCuFt(): Double {
    return when (this) {
        is Geometry.Slab -> volumeCuFt(length, width, thickness)
        else -> 0.0
    }
}
