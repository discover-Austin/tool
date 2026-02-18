package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Millimeters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DimensionParserTest {

    @Test
    fun `parse mixed feet and inches formats`() {
        // 12' 6" format
        val result1 = DimensionParser.parseLengthToMillimeters("12' 6\"")
        assertNotNull(result1)
        val expected1 = Millimeters.fromFeet(12.0).value + Millimeters.fromInches(6.0).value
        assertEquals(expected1, result1)

        // 12' 6 format (without quotes on inches)
        val result2 = DimensionParser.parseLengthToMillimeters("12' 6")
        assertNotNull(result2)
        assertEquals(expected1, result2)

        // 12'6" format (no spaces)
        val result3 = DimensionParser.parseLengthToMillimeters("12'6\"")
        assertNotNull(result3)
        assertEquals(expected1, result3)

        // 10' 0" format
        val result4 = DimensionParser.parseLengthToMillimeters("10' 0\"")
        assertNotNull(result4)
        assertEquals(Millimeters.fromFeet(10.0).value, result4)

        // Just feet: 12'
        val result5 = DimensionParser.parseLengthToMillimeters("12'")
        assertNotNull(result5)
        assertEquals(Millimeters.fromFeet(12.0).value, result5)
    }

    @Test
    fun `parse decimal feet formats`() {
        // 12.5ft format
        val result1 = DimensionParser.parseLengthToMillimeters("12.5ft")
        assertNotNull(result1)
        assertEquals(Millimeters.fromFeet(12.5).value, result1)

        // 12.5 format (plain number interpreted as feet)
        val result2 = DimensionParser.parseLengthToMillimeters("12.5")
        assertNotNull(result2)
        assertEquals(Millimeters.fromFeet(12.5).value, result2)

        // 10.0 feet
        val result3 = DimensionParser.parseLengthToMillimeters("10.0 feet")
        assertNotNull(result3)
        assertEquals(Millimeters.fromFeet(10.0).value, result3)
    }

    @Test
    fun `parse millimeter formats`() {
        // 3800mm format
        val result1 = DimensionParser.parseLengthToMillimeters("3800mm")
        assertNotNull(result1)
        assertEquals(3800L, result1)

        // 3800MM (uppercase)
        val result2 = DimensionParser.parseLengthToMillimeters("3800MM")
        assertNotNull(result2)
        assertEquals(3800L, result2)

        // 3800.5mm (decimal)
        val result3 = DimensionParser.parseLengthToMillimeters("3800.5mm")
        assertNotNull(result3)
        assertEquals(3801L, result3) // rounded

        // 1000 mm (with space)
        val result4 = DimensionParser.parseLengthToMillimeters("1000 mm")
        assertNotNull(result4)
        assertEquals(1000L, result4)
    }

    @Test
    fun `parse meter formats`() {
        // 3.8m format
        val result1 = DimensionParser.parseLengthToMillimeters("3.8m")
        assertNotNull(result1)
        assertEquals(3800L, result1)

        // 3.8M (uppercase)
        val result2 = DimensionParser.parseLengthToMillimeters("3.8M")
        assertNotNull(result2)
        assertEquals(3800L, result2)

        // 1m
        val result3 = DimensionParser.parseLengthToMillimeters("1m")
        assertNotNull(result3)
        assertEquals(1000L, result3)

        // 5.25m
        val result4 = DimensionParser.parseLengthToMillimeters("5.25m")
        assertNotNull(result4)
        assertEquals(5250L, result4)

        // 10 m (with space)
        val result5 = DimensionParser.parseLengthToMillimeters("10 m")
        assertNotNull(result5)
        assertEquals(10000L, result5)
    }

    @Test
    fun `parse inch formats`() {
        // 36in
        val result1 = DimensionParser.parseLengthToMillimeters("36in")
        assertNotNull(result1)
        assertEquals(Millimeters.fromInches(36.0).value, result1)

        // 36"
        val result2 = DimensionParser.parseLengthToMillimeters("36\"")
        assertNotNull(result2)
        assertEquals(Millimeters.fromInches(36.0).value, result2)

        // 48 in (with space)
        val result3 = DimensionParser.parseLengthToMillimeters("48 in")
        assertNotNull(result3)
        assertEquals(Millimeters.fromInches(48.0).value, result3)
    }

    @Test
    fun `parse angle formats`() {
        // 45deg
        val result1 = DimensionParser.parseAngleDegrees("45deg")
        assertNotNull(result1)
        assertEquals(45.0, result1)

        // 90°
        val result2 = DimensionParser.parseAngleDegrees("90°")
        assertNotNull(result2)
        assertEquals(90.0, result2)

        // Plain number
        val result3 = DimensionParser.parseAngleDegrees("180")
        assertNotNull(result3)
        assertEquals(180.0, result3)

        // Decimal
        val result4 = DimensionParser.parseAngleDegrees("22.5")
        assertNotNull(result4)
        assertEquals(22.5, result4)
    }

    @Test
    fun `reject invalid formats`() {
        assertNull(DimensionParser.parseLengthToMillimeters(""))
        assertNull(DimensionParser.parseLengthToMillimeters("abc"))
        assertNull(DimensionParser.parseLengthToMillimeters("12x"))
        assertNull(DimensionParser.parseLengthToMillimeters("' 6\"")) // Missing feet
        assertNull(DimensionParser.parseAngleDegrees("abc"))
        assertNull(DimensionParser.parseAngleDegrees(""))
    }

    @Test
    fun `handle whitespace gracefully`() {
        assertEquals(
            Millimeters.fromFeet(10.0).value,
            DimensionParser.parseLengthToMillimeters("  10ft  ")
        )
        assertEquals(
            3800L,
            DimensionParser.parseLengthToMillimeters("  3800mm  ")
        )
        assertEquals(
            45.0,
            DimensionParser.parseAngleDegrees("  45deg  ")
        )
    }

    @Test
    fun `parse length to feet helper`() {
        val result = DimensionParser.parseLengthToFeet("12.5ft")
        assertNotNull(result)
        assertEquals(12.5, result, 0.001)

        val result2 = DimensionParser.parseLengthToFeet("3.8m")
        assertNotNull(result2)
        assertEquals(Millimeters(3800L).toFeet(), result2, 0.001)
    }
}
