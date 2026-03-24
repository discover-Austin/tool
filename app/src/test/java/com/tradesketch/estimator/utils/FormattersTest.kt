package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormattersTest {

    @Test
    fun `formatQuantity formats with two decimals and commas`() {
        assertEquals("1,234.56", Formatters.formatQuantity(1234.56))
        assertEquals("1", Formatters.formatQuantity(1.0))
        assertEquals("0.5", Formatters.formatQuantity(0.5))
    }

    @Test
    fun `formatMoney formats as currency`() {
        assertEquals("$1,234.56", Formatters.formatMoney(Money(123456)))
        assertEquals("$0.00", Formatters.formatMoney(Money(0)))
        assertEquals("$10.99", Formatters.formatMoney(Money(1099)))
    }

    @Test
    fun `formatDimension converts mm to feet and inches`() {
        // 3048mm = 10 feet exactly
        val result = Formatters.formatDimension(Millimeters.fromFeet(10.0))
        assertTrue(result.contains("10'"))

        // 3353mm = 11 feet exactly
        val result2 = Formatters.formatDimension(Millimeters.fromFeet(11.0))
        assertTrue(result2.contains("11'"))
    }

    @Test
    fun `formatArea formats square feet`() {
        assertEquals("100 sq ft", Formatters.formatArea(100.0))
        assertEquals("1,234.56 sq ft", Formatters.formatArea(1234.56))
    }

    @Test
    fun `formatArea can format square meters for metric displays`() {
        assertEquals("9.29 sq m", Formatters.formatArea(100.0, useMetric = true))
    }

    @Test
    fun `formatLength can format metric and imperial displays`() {
        assertEquals("10 ft", Formatters.formatLength(10.0, useMetric = false))
        assertEquals("3.05 m", Formatters.formatLength(Millimeters.fromFeet(10.0), useMetric = true))
    }

    @Test
    fun `formatSnapDistance uses centimeters in metric mode`() {
        assertEquals("22.86 cm", Formatters.formatSnapDistance(0.75, useMetric = true))
        assertEquals("0.75 ft", Formatters.formatSnapDistance(0.75, useMetric = false))
    }

    @Test
    fun `formatPercent formats percentage`() {
        assertEquals("10%", Formatters.formatPercent(10.0))
        assertEquals("5.5%", Formatters.formatPercent(5.5))
    }
}
