package com.liangji.bodymeasure.domain.formula

import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.testing.rawMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FormulaZeroGuardTest {

    private val weight = rawMetric("weight", name = "体重", unit = "kg")
    private val heightCm = rawMetric("height", name = "身高", unit = "cm")
        .copy(unitDimension = UnitDimension.LENGTH)
    private val heightM = heightCm.copy(unit = "m")
    private val metricsById = listOf(weight, heightCm).associateBy { it.id }
    private val names = mapOf("weight" to "体重", "height" to "身高")

    @Test
    fun centimetreHeightIsRewrittenSoBmiSurvivesRounding() {
        val expression = parse("[体重] / [身高] ^ 2")
        val raw = 71.2 / (172.0 * 172.0)

        val hint = FormulaZeroGuard.check(
            expression = expression,
            sample = raw,
            precision = 1,
            metrics = metricsById,
            resolveName = names::get,
        )

        assertNotNull(hint)
        assertEquals("[体重] / ([身高] / 100) ^ 2", hint?.suggestion)

        val fixed = parse(hint?.suggestion.orEmpty())
        val value = FormulaEvaluator.evaluate(fixed) {
            if (it == "weight") 71.2 else 172.0
        }
        assertEquals("24.1", NumberFormats.format(value ?: 0.0, 1))
    }

    @Test
    fun metreHeightNeedsNoHintWhenResultIsNormal() {
        val hint = FormulaZeroGuard.check(
            expression = parse("[体重] / [身高] ^ 2"),
            sample = 71.2 / (1.72 * 1.72),
            precision = 1,
            metrics = listOf(weight, heightM).associateBy { it.id },
            resolveName = names::get,
        )

        assertNull(hint)
    }

    @Test
    fun tinyRatioWithoutLengthReferenceStillWarnsWithoutSuggestion() {
        val hint = FormulaZeroGuard.check(
            expression = parse("[体重] / 100000"),
            sample = 0.0007,
            precision = 1,
            metrics = metricsById,
            resolveName = names::get,
        )

        assertNotNull(hint)
        assertNull(hint?.suggestion)
    }

    @Test
    fun zeroAndNormalValuesAreIgnored() {
        assertNull(
            FormulaZeroGuard.check(
                expression = parse("[体重] / [身高] ^ 2"),
                sample = 0.0,
                precision = 1,
                metrics = metricsById,
                resolveName = names::get,
            ),
        )
        assertNull(
            FormulaZeroGuard.check(
                expression = parse("[体重] / [身高] ^ 2"),
                sample = 24.06,
                precision = 1,
                metrics = metricsById,
                resolveName = names::get,
            ),
        )
    }

    @Test
    fun thresholdFollowsPrecision() {
        val expression = parse("[体重] / [身高] ^ 2")
        assertNotNull(
            FormulaZeroGuard.check(expression, 0.4, 0, metricsById, names::get),
        )
        assertNull(
            FormulaZeroGuard.check(expression, 0.6, 0, metricsById, names::get),
        )
    }

    private fun parse(text: String): FormulaExpr {
        val parsed = FormulaParser.parse(text) { name ->
            names.entries.firstOrNull { it.value == name }?.key
        }
        return (parsed as FormulaParseResult.Valid).expression
    }
}
