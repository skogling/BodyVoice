package com.liangji.bodymeasure.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object NumberFormats {
    const val MAX_PRECISION = 4
    const val MAX_ABS_VALUE = 1_000_000_000.0
    const val MINUS = "\u2212"

    private val DECIMAL = Regex("""^[+-]?(\d+(\.\d*)?|\.\d+)$""")

    fun format(value: Double, precision: Int): String =
        rounded(value, precision)?.toPlainString() ?: "—"

    fun formatSigned(value: Double, precision: Int): String {
        val rounded = rounded(value, precision) ?: return "—"
        return when {
            rounded.signum() > 0 -> "+${rounded.toPlainString()}"
            rounded.signum() < 0 -> "$MINUS${rounded.abs().toPlainString()}"
            else -> rounded.toPlainString()
        }
    }

    fun formatPercent(ratio: Double, precision: Int = 1): String {
        val rounded = rounded(ratio * 100.0, precision) ?: return "—"
        val text = if (rounded.signum() == 0) {
            "0"
        } else {
            rounded.abs().stripTrailingZeros().toPlainString()
        }
        return when {
            rounded.signum() > 0 -> "+$text%"
            rounded.signum() < 0 -> "$MINUS$text%"
            else -> "$text%"
        }
    }

    fun parse(raw: String): Double? {
        val text = raw.trim()
        if (!DECIMAL.matches(text)) return null
        val value = text.toDoubleOrNull() ?: return null
        if (!value.isFinite() || abs(value) >= MAX_ABS_VALUE) return null
        return value
    }

    private fun rounded(value: Double, precision: Int): BigDecimal? {
        if (!value.isFinite()) return null
        val scaled = BigDecimal.valueOf(value)
            .setScale(precision.coerceIn(0, MAX_PRECISION), RoundingMode.HALF_UP)
        return if (scaled.signum() == 0) scaled.abs() else scaled
    }
}
