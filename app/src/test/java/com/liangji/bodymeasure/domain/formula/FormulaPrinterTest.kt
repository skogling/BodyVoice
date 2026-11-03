package com.liangji.bodymeasure.domain.formula

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaPrinterTest {
    @Test
    fun keeps_required_parentheses() {
        assertEquals("(1 + 2) * 3", canonical("(1 + 2) * 3"))
        assertEquals("1 - (2 - 3)", canonical("1 - (2 - 3)"))
        assertEquals("(2 ^ 3) ^ 2", canonical("(2 ^ 3) ^ 2"))
        assertEquals("-(2 * 3)", canonical("-(2 * 3)"))
    }

    @Test
    fun omits_unnecessary_parentheses() {
        assertEquals("1 + 2 * 3", canonical("1 + 2 * 3"))
        assertEquals("1 + 2 - 3", canonical("1 + 2 - 3"))
        assertEquals("1 - 2 - 3", canonical("1 - 2 - 3"))
        assertEquals("2 ^ 3 ^ 2", canonical("2 ^ 3 ^ 2"))
        assertEquals("-2 ^ 2", canonical("-2 ^ 2"))
    }

    @Test
    fun canonical_uses_metric_ids() {
        val names = mapOf("体重" to "w")

        val parsed = FormulaParser.parse("[体重] + 1", names::get) as FormulaParseResult.Valid

        assertEquals("{w} + 1", FormulaPrinter.canonical(parsed.expression))
    }

    @Test
    fun display_uses_metric_names() {
        val names = mapOf("体重" to "w", "身高" to "h")
        val ids = names.entries.associate { (name, id) -> id to name }
        val parsed = FormulaParser.parse("[体重] - [身高]", names::get) as FormulaParseResult.Valid

        assertEquals(
            "[体重] - [身高]",
            FormulaPrinter.display(parsed.expression) { ids[it] },
        )
    }

    @Test
    fun display_falls_back_to_ids_for_unprintable_names() {
        val parsed = FormulaParser.parse("{w} + 1") as FormulaParseResult.Valid

        assertEquals(
            "{w} + 1",
            FormulaPrinter.display(parsed.expression) { "体]重" },
        )
        assertEquals("{w} + 1", FormulaPrinter.display(parsed.expression) { null })
    }

    @Test
    fun canonical_output_reparses_to_the_same_formula() {
        val formulas = listOf(
            "((1 + 2) * (3 - 4)) / 5",
            "2 ^ 3 ^ 2",
            "(2 ^ 3) ^ 2",
            "-(-2)",
            "1 + (2 + 3)",
            "1 - (2 + 3)",
            "round(abs(-2.345), 2) * 100",
        )

        formulas.forEach { formula ->
            val first = canonical(formula)
            val second = canonical(first)
            assertEquals(first, second)
            assertTrue(first.isNotBlank())
        }
    }

    private fun canonical(text: String): String {
        val parsed = FormulaParser.parse(text) as FormulaParseResult.Valid
        return FormulaPrinter.canonical(parsed.expression)
    }
}
