package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Millimeters
import kotlin.math.roundToLong

object DimensionParser {
    private val mixedFeetPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*'\s*(\d+(?:\.\d+)?)?\s*(?:"|in)?\s*$""")
    private val millimeterPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*mm\s*$""", RegexOption.IGNORE_CASE)
    private val meterPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*m\s*$""", RegexOption.IGNORE_CASE)
    private val feetPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*(?:ft|feet|')\s*$""", RegexOption.IGNORE_CASE)
    private val inchPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*(?:in|")\s*$""", RegexOption.IGNORE_CASE)
    private val plainNumberPattern = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*$""")

    fun parseLengthToMillimeters(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        mixedFeetPattern.matchEntire(trimmed)?.let { match ->
            val feet = match.groupValues[1].toDoubleOrNull() ?: return null
            val inches = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
            return Millimeters.fromFeet(feet).value + Millimeters.fromInches(inches).value
        }

        millimeterPattern.matchEntire(trimmed)?.let { match ->
            val mm = match.groupValues[1].toDoubleOrNull() ?: return null
            return mm.roundToLong()
        }

        meterPattern.matchEntire(trimmed)?.let { match ->
            val meters = match.groupValues[1].toDoubleOrNull() ?: return null
            return (meters * 1000.0).roundToLong()
        }

        feetPattern.matchEntire(trimmed)?.let { match ->
            val feet = match.groupValues[1].toDoubleOrNull() ?: return null
            return Millimeters.fromFeet(feet).value
        }

        inchPattern.matchEntire(trimmed)?.let { match ->
            val inches = match.groupValues[1].toDoubleOrNull() ?: return null
            return Millimeters.fromInches(inches).value
        }

        plainNumberPattern.matchEntire(trimmed)?.let { match ->
            val feet = match.groupValues[1].toDoubleOrNull() ?: return null
            return Millimeters.fromFeet(feet).value
        }

        return null
    }

    fun parseLengthToFeet(input: String): Double? {
        val mm = parseLengthToMillimeters(input) ?: return null
        return Millimeters(mm).toFeet()
    }

    fun parseAngleDegrees(input: String): Double? {
        val value = input.trim().removeSuffix("deg").removeSuffix("°").trim().toDoubleOrNull() ?: return null
        return value
    }
}
