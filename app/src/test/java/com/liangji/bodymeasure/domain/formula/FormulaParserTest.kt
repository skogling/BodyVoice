package com.liangji.bodymeasure.domain.formula

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaParserTest {
    @Test
    fun respects_operator_precedence() {
        val expression = valid("1 + 2 * 3").expression

        assertEquals(
            FormulaExpr.Binary(
                Operator.ADD,
                FormulaExpr.Number(1.0),
                FormulaExpr.Binary(
                    Operator.MULTIPLY,
                    FormulaExpr.Number(2.0),
                    FormulaExpr.Number(3.0),
                ),
            ),
            expression,
        )
    }

    @Test
    fun power_is_right_associative() {
        val expression = valid("2 ^ 3 ^ 2").expression

        assertEquals(
            FormulaExpr.Binary(
                Operator.POWER,
                FormulaExpr.Number(2.0),
                FormulaExpr.Binary(
                    Operator.POWER,
                    FormulaExpr.Number(3.0),
                    FormulaExpr.Number(2.0),
                ),
            ),
            expression,
        )
    }

    @Test
    fun unary_minus_binds_looser_than_power() {
        val expression = valid("-2 ^ 2").expression

        assertEquals(
            FormulaExpr.Negate(
                FormulaExpr.Binary(
                    Operator.POWER,
                    FormulaExpr.Number(2.0),
                    FormulaExpr.Number(2.0),
                ),
            ),
            expression,
        )
    }

    @Test
    fun parses_functions_and_nested_calls() {
        val expression = valid("round(abs(-2.5), 0)").expression

        assertEquals(
            FormulaExpr.Call(
                "round",
                listOf(
                    FormulaExpr.Call("abs", listOf(FormulaExpr.Negate(FormulaExpr.Number(2.5)))),
                    FormulaExpr.Number(0.0),
                ),
            ),
            expression,
        )
    }

    @Test
    fun resolves_display_names_to_metric_ids() {
        val names = mapOf("体重" to "w", "身高" to "h")

        val result = valid("[体重] - [身高]", names::get)

        assertEquals(listOf("w", "h"), result.references.map { it.metricId })
        assertEquals(7, result.references[1].position)
    }

    @Test
    fun keeps_reference_order_and_deduplicates() {
        val result = valid("{a} + {b} * {a}")

        assertEquals(listOf("a", "b"), result.references.map { it.metricId })
    }

    @Test
    fun reports_empty_formula() {
        assertEquals(FormulaErrorCode.EMPTY, invalid("   ").code)
    }

    @Test
    fun reports_missing_operand() {
        assertEquals(FormulaErrorCode.MISSING_OPERAND, invalid("1 + ").code)
    }

    @Test
    fun reports_unexpected_character() {
        val error = invalid("1 $ 2")

        assertEquals(FormulaErrorCode.UNEXPECTED_CHAR, error.code)
        assertEquals(2, error.position)
    }

    @Test
    fun reports_unknown_function() {
        assertEquals(FormulaErrorCode.UNKNOWN_FUNCTION, invalid("foo(1)").code)
    }

    @Test
    fun reports_wrong_argument_count() {
        assertEquals(FormulaErrorCode.WRONG_ARGUMENT_COUNT, invalid("abs(1, 2)").code)
        assertEquals(FormulaErrorCode.WRONG_ARGUMENT_COUNT, invalid("abs()").code)
    }

    @Test
    fun reports_missing_right_parenthesis() {
        assertEquals(FormulaErrorCode.MISSING_RIGHT_PAREN, invalid("(1 + 2").code)
    }

    @Test
    fun reports_missing_reference_bracket() {
        assertEquals(FormulaErrorCode.MISSING_BRACKET, invalid("{abc").code)
        assertEquals(FormulaErrorCode.MISSING_BRACKET, invalid("[体重").code)
    }

    @Test
    fun reports_unknown_metric_name() {
        val names = mapOf("体重" to "w")

        val error = invalid("[腰围]", names::get)

        assertEquals(FormulaErrorCode.UNKNOWN_METRIC, error.code)
        assertTrue(error.displayMessage().startsWith("第 1 位"))
    }

    @Test
    fun reports_too_many_references() {
        val text = (0..16).joinToString(" + ") { "{$it}" }

        assertEquals(FormulaErrorCode.TOO_MANY_REFERENCES, invalid(text).code)
    }

    @Test
    fun reports_too_many_nodes() {
        val text = List(129) { "1" }.joinToString("+")

        assertEquals(FormulaErrorCode.TOO_MANY_NODES, invalid(text).code)
    }

    @Test
    fun reports_nesting_too_deep() {
        val text = "(".repeat(33) + "1" + ")".repeat(33)

        assertEquals(FormulaErrorCode.NESTING_TOO_DEEP, invalid(text).code)
    }

    @Test
    fun reports_too_long_formula() {
        val text = "1" + " ".repeat(FormulaParser.MAX_LENGTH)

        assertEquals(FormulaErrorCode.TOO_LONG, invalid(text).code)
    }

    private fun valid(
        text: String,
        resolve: ((String) -> String?)? = null,
    ): FormulaParseResult.Valid =
        FormulaParser.parse(text, resolve) as FormulaParseResult.Valid

    private fun invalid(
        text: String,
        resolve: ((String) -> String?)? = null,
    ): FormulaError {
        val result = FormulaParser.parse(text, resolve)
        assertTrue("expected invalid formula: $text", result is FormulaParseResult.Invalid)
        return (result as FormulaParseResult.Invalid).error
    }
}
