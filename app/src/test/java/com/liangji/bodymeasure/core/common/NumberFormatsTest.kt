package com.liangji.bodymeasure.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberFormatsTest {
    @Test
    fun format_rounds_half_up() {
        assertEquals("78.5", NumberFormats.format(78.45, 1))
        assertEquals("78.4", NumberFormats.format(78.44, 1))
        assertEquals("3", NumberFormats.format(2.5, 0))
        assertEquals("0.3", NumberFormats.format(0.25, 1))
    }

    @Test
    fun format_keeps_fixed_decimals() {
        assertEquals("70.0", NumberFormats.format(70.0, 1))
        assertEquals("70.00", NumberFormats.format(70.0, 2))
        assertEquals("70", NumberFormats.format(70.0, 0))
    }

    @Test
    fun format_normalizes_negative_zero() {
        assertEquals("0.0", NumberFormats.format(-0.04, 1))
        assertEquals("0.0", NumberFormats.format(-0.0, 1))
    }

    @Test
    fun format_rejects_non_finite() {
        assertEquals("—", NumberFormats.format(Double.NaN, 1))
        assertEquals("—", NumberFormats.format(Double.POSITIVE_INFINITY, 1))
    }

    @Test
    fun format_clamps_precision() {
        assertEquals("1.2346", NumberFormats.format(1.23456, 9))
        assertEquals("2", NumberFormats.format(1.6, -3))
    }

    @Test
    fun format_signed_adds_sign_only_to_non_zero() {
        assertEquals("+0.4", NumberFormats.formatSigned(0.4, 1))
        assertEquals("−0.4", NumberFormats.formatSigned(-0.4, 1))
        assertEquals("0.0", NumberFormats.formatSigned(0.0, 1))
        assertEquals("0.0", NumberFormats.formatSigned(-0.04, 1))
        assertEquals("—", NumberFormats.formatSigned(Double.NaN, 1))
    }

    @Test
    fun format_percent_trims_trailing_zeros() {
        assertEquals("+12.3%", NumberFormats.formatPercent(0.1234))
        assertEquals("−4%", NumberFormats.formatPercent(-0.04))
        assertEquals("+12%", NumberFormats.formatPercent(0.12))
        assertEquals("0%", NumberFormats.formatPercent(0.0))
        assertEquals("—", NumberFormats.formatPercent(Double.NaN))
    }

    @Test
    fun parse_accepts_plain_decimals() {
        assertEquals(78.5, NumberFormats.parse("78.5")!!, 0.0)
        assertEquals(78.0, NumberFormats.parse(" 78. ")!!, 0.0)
        assertEquals(0.5, NumberFormats.parse(".5")!!, 0.0)
        assertEquals(-12.5, NumberFormats.parse("-12.5")!!, 0.0)
        assertEquals(5.0, NumberFormats.parse("+5")!!, 0.0)
    }

    @Test
    fun parse_rejects_invalid_input() {
        assertNull(NumberFormats.parse(""))
        assertNull(NumberFormats.parse("   "))
        assertNull(NumberFormats.parse("abc"))
        assertNull(NumberFormats.parse("1.2.3"))
        assertNull(NumberFormats.parse("1e5"))
        assertNull(NumberFormats.parse("--1"))
        assertNull(NumberFormats.parse("NaN"))
        assertNull(NumberFormats.parse("Infinity"))
        assertNull(NumberFormats.parse("9999999999"))
    }
}
