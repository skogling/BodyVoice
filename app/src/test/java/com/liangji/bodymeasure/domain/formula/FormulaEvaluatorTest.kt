package com.liangji.bodymeasure.domain.formula

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormulaEvaluatorTest {
    @Test
    fun evaluates_arithmetic_with_precedence() {
        assertEquals(7.0, evaluate("1 + 2 * 3")!!, 1e-9)
    }

    @Test
    fun evaluates_references() {
        assertEquals(-20.0, evaluate("({a} - {b}) / {b} * 100", "a" to 80.0, "b" to 100.0)!!, 1e-9)
    }

    @Test
    fun propagates_missing_values() {
        assertNull(evaluate("{a} + 1", "a" to null))
    }

    @Test
    fun division_by_zero_is_missing() {
        assertNull(evaluate("{a} / {b}", "a" to 1.0, "b" to 0.0))
    }

    @Test
    fun applies_functions() {
        assertEquals(3.0, evaluate("sqrt(9)")!!, 1e-9)
        assertEquals(2.35, evaluate("round(2.345, 2)")!!, 1e-9)
        assertEquals(3.0, evaluate("clamp(5, 1, 3)")!!, 1e-9)
        assertEquals(1.0, evaluate("min(3, 1, 2)")!!, 1e-9)
        assertEquals(4.0, evaluate("max(3, 4)")!!, 1e-9)
        assertEquals(8.0, evaluate("pow(2, 3)")!!, 1e-9)
    }

    @Test
    fun invalid_math_results_become_missing() {
        assertNull(evaluate("sqrt(-1)"))
        assertNull(evaluate("(-1) ^ 0.5"))
        assertNull(evaluate("1 / 0"))
    }

    @Test
    fun overflow_becomes_missing() {
        assertNull(evaluate("{a} * {a}", "a" to 1.0e308))
    }

    @Test
    fun clamp_with_inverted_bounds_is_missing() {
        assertNull(evaluate("clamp(2, 3, 1)"))
    }

    private fun evaluate(
        text: String,
        vararg values: Pair<String, Double?>,
    ): Double? {
        val parsed = FormulaParser.parse(text) as FormulaParseResult.Valid
        val lookup = values.toMap()
        return FormulaEvaluator.evaluate(parsed.expression) { lookup[it] }
    }
}
