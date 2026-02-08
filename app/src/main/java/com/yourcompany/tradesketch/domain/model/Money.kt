package com.yourcompany.tradesketch.domain.model

/**
 * Represents currency as cents (minor units) to avoid floating point precision issues.
 * All monetary calculations use Long integers.
 */
@JvmInline
value class Money(val cents: Long) {
    fun toDollars(): Double = cents / 100.0
    
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    operator fun times(quantity: Double): Money = Money((cents * quantity).toLong())
    
    companion object {
        val ZERO = Money(0L)
        
        fun fromDollars(dollars: Double): Money {
            require(dollars >= 0) { "Money amount cannot be negative" }
            return Money((dollars * 100).toLong())
        }
        
        fun fromCents(cents: Long): Money {
            require(cents >= 0) { "Money amount cannot be negative" }
            return Money(cents)
        }
    }
}

fun Double.toMoney(): Money = Money.fromDollars(this)
