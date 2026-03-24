package com.tradesketch.estimator.utils

import com.tradesketch.estimator.domain.model.Millimeters
import com.tradesketch.estimator.domain.model.Money
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for formatting values for display.
 */
object Formatters {

    private const val FEET_TO_METERS = 0.3048
    private const val FEET_TO_CENTIMETERS = 30.48
    private const val SQUARE_FEET_TO_SQUARE_METERS = 0.09290304

    private val quantityFormat = DecimalFormat("#,##0.##")
    private val currencyFormat = DecimalFormat("$#,##0.00")
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)

    /**
     * Format a quantity with 2 decimal places and thousand separators.
     */
    fun formatQuantity(value: Double): String {
        return quantityFormat.format(value)
    }

    /**
     * Format money as USD currency.
     */
    fun formatMoney(money: Money): String {
        return currencyFormat.format(money.toDollars())
    }

    /**
     * Format dollars as USD currency.
     */
    fun formatMoney(amount: Double): String {
        return currencyFormat.format(amount.coerceAtLeast(0.0))
    }

    /**
     * Format money from cents.
     */
    fun formatMoney(cents: Long): String {
        return formatMoney(Money(cents))
    }

    /**
     * Format millimeters as feet and inches.
     * Example: 3048mm -> 10' 0"
     */
    fun formatDimension(mm: Millimeters): String {
        val totalInches = mm.toInches()
        val feet = (totalInches / 12).toInt()
        val inches = (totalInches % 12).toInt()
        return if (inches == 0) {
            "$feet'"
        } else {
            "$feet' $inches\""
        }
    }

    /**
     * Format millimeters as decimal feet.
     * Example: 3048mm -> 10.0 ft
     */
    fun formatDimensionDecimal(mm: Millimeters): String {
        return "${quantityFormat.format(mm.toFeet())} ft"
    }

    fun formatLength(mm: Millimeters, useMetric: Boolean): String {
        return formatLength(mm.toFeet(), useMetric)
    }

    fun formatLength(feet: Double, useMetric: Boolean): String {
        return if (useMetric) {
            "${quantityFormat.format(feet * FEET_TO_METERS)} m"
        } else {
            "${quantityFormat.format(feet)} ft"
        }
    }

    fun formatSnapDistance(feet: Double, useMetric: Boolean): String {
        return if (useMetric) {
            "${quantityFormat.format(feet * FEET_TO_CENTIMETERS)} cm"
        } else {
            "${quantityFormat.format(feet)} ft"
        }
    }

    /**
     * Format area in square feet.
     */
    fun formatArea(sqFt: Double): String {
        return "${quantityFormat.format(sqFt)} sq ft"
    }

    fun formatArea(sqFt: Double, useMetric: Boolean): String {
        return if (useMetric) {
            "${quantityFormat.format(sqFt * SQUARE_FEET_TO_SQUARE_METERS)} sq m"
        } else {
            formatArea(sqFt)
        }
    }

    /**
     * Format volume in cubic yards.
     */
    fun formatVolume(cuYards: Double): String {
        return "${quantityFormat.format(cuYards)} cu yd"
    }

    /**
     * Format date from timestamp.
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format date and time from timestamp.
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * Format percentage.
     */
    fun formatPercent(percent: Double): String {
        return "${quantityFormat.format(percent)}%"
    }
}
