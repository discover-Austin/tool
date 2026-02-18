package com.tradesketch.estimator.utils

/**
 * Input validation utilities.
 */
object Validators {
    
    /**
     * Validate and parse dimension input (feet and inches format).
     * Supports formats: "10", "10.5", "10'", "10' 6\"", "10'6\"", etc.
     * Returns value in feet or null if invalid.
     */
    fun parseDimensionToFeet(input: String): Double? {
        if (input.isBlank()) return null
        
        val trimmed = input.trim()
        
        // Try feet and inches format first (e.g., "10' 6\"" or "10'6\"")
        val feetInchesRegex = """(\d+(?:\.\d+)?)'?\s*(\d+(?:\.\d+)?)"?""".toRegex()
        feetInchesRegex.matchEntire(trimmed)?.let { match ->
            val feet = match.groupValues[1].toDoubleOrNull() ?: return null
            val inches = match.groupValues[2].toDoubleOrNull() ?: return null
            return feet + (inches / 12.0)
        }
        
        // Try feet only format (e.g., "10'" or "10")
        val feetOnlyRegex = """(\d+(?:\.\d+)?)'?""".toRegex()
        feetOnlyRegex.matchEntire(trimmed)?.let { match ->
            return match.groupValues[1].toDoubleOrNull()
        }
        
        return null
    }
    
    /**
     * Validate positive number input.
     */
    fun parsePositiveDouble(input: String): Double? {
        if (input.isBlank()) return null
        val value = input.trim().toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }
    
    /**
     * Validate non-negative number input.
     */
    fun parseNonNegativeDouble(input: String): Double? {
        if (input.isBlank()) return null
        val value = input.trim().toDoubleOrNull() ?: return null
        return if (value >= 0) value else null
    }
    
    /**
     * Validate positive integer input.
     */
    fun parsePositiveInt(input: String): Int? {
        if (input.isBlank()) return null
        val value = input.trim().toIntOrNull() ?: return null
        return if (value > 0) value else null
    }
    
    /**
     * Validate project name.
     */
    fun isValidProjectName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 100
    }
    
    /**
     * Validate space name.
     */
    fun isValidSpaceName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 50
    }
}
