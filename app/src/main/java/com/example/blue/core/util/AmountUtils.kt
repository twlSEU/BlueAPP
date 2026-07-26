package com.example.blue.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object AmountUtils {
    private val formatterForThread = ThreadLocal<NumberFormat>()

    fun parseToCents(text: String): Long? = runCatching {
        val normalized = text.trim()
        if (normalized.isEmpty() || normalized.startsWith('-')) return null
        val value = BigDecimal(normalized)
        if (value <= BigDecimal.ZERO || value.scale() > 2) return null
        value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()

    fun formatCents(cents: Long): String {
        val amount = BigDecimal.valueOf(cents, 2)
        // NumberFormat is relatively expensive and not thread-safe, so cache one per caller thread.
        val formatter = formatterForThread.get() ?: NumberFormat.getNumberInstance(Locale.CHINA).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            formatterForThread.set(this)
        }
        return formatter.format(amount)
    }
}
