package com.tradesketch.estimator.domain.model

import kotlin.math.roundToLong

@JvmInline
value class Millimeters(val value: Long) {
    fun toFeet(): Double = value.toDouble() / 304.8
    fun toInches(): Double = value.toDouble() / 25.4

    companion object {
        fun fromFeet(value: Double): Millimeters = Millimeters((value * 304.8).roundToLong())
        fun fromInches(value: Double): Millimeters = Millimeters((value * 25.4).roundToLong())
    }
}

fun areaSqFt(length: Millimeters, width: Millimeters): Double {
    return (length.toFeet() * width.toFeet())
}

fun volumeCuFt(length: Millimeters, width: Millimeters, height: Millimeters): Double {
    return areaSqFt(length, width) * height.toFeet()
}
