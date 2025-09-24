package com.inventorystore.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Utility object for formatting currency values in the application
 * Supports Kenyan Shilling (KES) and other currencies
 */
object CurrencyFormatter {

    // Default currency formatter for Kenyan Shilling
    private val kesFormatter = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
        currency = Currency.getInstance("KES")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    // Alternative simple formatter for cases where currency symbol might not display
    private val simpleFormatter = DecimalFormat("#,##0.00")

    // USD formatter for international transactions
    private val usdFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        currency = Currency.getInstance("USD")
    }

    /**
     * Format amount in Kenyan Shillings (KES)
     * @param amount The amount to format
     * @return Formatted currency string (e.g., "KSh 1,500.00")
     */
    fun format(amount: Double): String {
        return try {
            kesFormatter.format(amount)
        } catch (e: Exception) {
            // Fallback to simple format if currency formatting fails
            "Ksh ${simpleFormatter.format(amount)}"
        }
    }

    /**
     * Format amount in Kenyan Shillings with custom decimal places
     * @param amount The amount to format
     * @param decimalPlaces Number of decimal places (default: 2)
     * @return Formatted currency string
     */
    fun format(amount: Double, decimalPlaces: Int = 2): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
                currency = Currency.getInstance("KES")
                minimumFractionDigits = decimalPlaces
                maximumFractionDigits = decimalPlaces
            }
            formatter.format(amount)
        } catch (e: Exception) {
            val pattern = if (decimalPlaces > 0) {
                "#,##0.${"0".repeat(decimalPlaces)}"
            } else {
                "#,##0"
            }
            val customFormatter = DecimalFormat(pattern)
            "Ksh ${customFormatter.format(amount)}"
        }
    }

    /**
     * Format amount in US Dollars
     * @param amount The amount to format
     * @return Formatted USD string (e.g., "$1,500.00")
     */
    fun formatUSD(amount: Double): String {
        return try {
            usdFormatter.format(amount)
        } catch (e: Exception) {
            "$${simpleFormatter.format(amount)}"
        }
    }

    /**
     * Format amount without currency symbol (just number with commas)
     * @param amount The amount to format
     * @return Formatted number string (e.g., "1,500.00")
     */
    fun formatNumber(amount: Double): String {
        return simpleFormatter.format(amount)
    }

    /**
     * Format amount with custom currency symbol
     * @param amount The amount to format
     * @param currencySymbol Custom currency symbol
     * @return Formatted currency string
     */
    fun formatWithSymbol(amount: Double, currencySymbol: String): String {
        return "$currencySymbol ${simpleFormatter.format(amount)}"
    }

    /**
     * Parse currency string back to double value
     * @param currencyString The currency string to parse
     * @return Double value or null if parsing fails
     */
    fun parse(currencyString: String): Double? {
        return try {
            // Remove currency symbols and parse
            val cleanString = currencyString
                .replace("KSh", "")
                .replace("Ksh", "")
                .replace("$", "")
                .replace(",", "")
                .trim()
            cleanString.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if amount is zero
     * @param amount The amount to check
     * @return True if amount is zero or very close to zero
     */
    fun isZero(amount: Double): Boolean {
        return Math.abs(amount) < 0.01
    }

    /**
     * Format amount for display in lists or compact spaces
     * Uses abbreviated format for large numbers (e.g., "1.5K", "2.3M")
     * @param amount The amount to format
     * @return Compact formatted string
     */
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "Ksh ${String.format("%.1f", amount / 1_000_000)}M"
            amount >= 1_000 -> "Ksh ${String.format("%.1f", amount / 1_000)}K"
            else -> format(amount)
        }
    }

    /**
     * Format percentage value
     * @param percentage The percentage to format (as decimal, e.g., 0.15 for 15%)
     * @return Formatted percentage string (e.g., "15.0%")
     */
    fun formatPercentage(percentage: Double): String {
        val percentageFormatter = DecimalFormat("#0.0%")
        return percentageFormatter.format(percentage)
    }

    /**
     * Calculate and format profit margin
     * @param revenue Total revenue
     * @param cost Total cost
     * @return Formatted profit margin string with currency and percentage
     */
    fun formatProfitMargin(revenue: Double, cost: Double): String {
        val profit = revenue - cost
        val margin = if (revenue > 0) profit / revenue else 0.0
        return "${format(profit)} (${formatPercentage(margin)})"
    }

    /**
     * Format currency range (e.g., for price ranges)
     * @param minAmount Minimum amount
     * @param maxAmount Maximum amount
     * @return Formatted range string (e.g., "Ksh 100.00 - Ksh 500.00")
     */
    fun formatRange(minAmount: Double, maxAmount: Double): String {
        return "${format(minAmount)} - ${format(maxAmount)}"
    }

    /**
     * Get currency symbol for KES
     * @return KES currency symbol
     */
    fun getCurrencySymbol(): String {
        return try {
            Currency.getInstance("KES").symbol
        } catch (e: Exception) {
            "Ksh"
        }
    }

    /**
     * Validate if a string represents a valid currency amount
     * @param input String to validate
     * @return True if valid currency format
     */
    fun isValidCurrencyInput(input: String): Boolean {
        return parse(input) != null
    }

    /**
     * Round amount to nearest currency unit (cents)
     * @param amount Amount to round
     * @return Rounded amount
     */
    fun roundToCurrency(amount: Double): Double {
        return Math.round(amount * 100.0) / 100.0
    }
}