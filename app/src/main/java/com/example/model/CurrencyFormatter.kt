package com.example.model

import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    /**
     * Formats Indian rupee amounts cleanly: ₹1,250 for whole rupees and ₹1,250.50 when paise exist
     * (or when [alwaysShowPaise] is true).
     */
    fun formatInr(amount: Double, alwaysShowPaise: Boolean = false): String {
        val isNegative = amount < 0
        val absolute = abs(amount)
        val roundedPaise = (absolute * 100.0).roundToLong()
        val whole = roundedPaise / 100L
        val paise = (roundedPaise % 100L).toInt()

        val formattedWhole = formatIndianInteger(whole)
        val prefix = if (isNegative) "-₹" else "₹"
        return if (alwaysShowPaise || paise != 0) {
            val paiseStr = paise.toString().padStart(2, '0')
            "$prefix$formattedWhole.$paiseStr"
        } else {
            "$prefix$formattedWhole"
        }
    }

    fun formatInrInt(amount: Int): String = formatInr(amount.toDouble(), alwaysShowPaise = false)

    private fun formatIndianInteger(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val groupedRest = rest.reversed()
            .chunked(2)
            .joinToString(",")
            .reversed()
        return "$groupedRest,$lastThree"
    }
}
