package com.liangji.bodymeasure.ui.metrics

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricPreset
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [37],
    qualifiers = "w393dp-h852dp-xxhdpi",
    application = Application::class,
)
class MetricCaptureFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun templateCreatesDefaultSelection() {
        var requested: List<MetricPreset>? = null
        var created: TemplateResult? = null
        show {
            TemplateSheet(
                existingNames = emptySet(),
                onDismiss = {},
                onCreate = { presets ->
                    requested = presets
                    TemplateResult(created = presets.size, skipped = emptyList())
                },
                onCreated = { created = it },
                onCreateCustom = {},
            )
        }

        composeRule.onNodeWithText("创建 2 个指标").assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) { created != null }

        assertEquals(setOf("weight", "waist"), requested?.map { it.key }?.toSet())
        assertEquals(2, created?.created)
    }

    @Test
    fun templateSkipsExistingNamesAndCreatesChosenOnes() {
        var requested: List<MetricPreset>? = null
        show {
            TemplateSheet(
                existingNames = setOf("体重", "腰围"),
                onDismiss = {},
                onCreate = { presets ->
                    requested = presets
                    TemplateResult(created = presets.size, skipped = emptyList())
                },
                onCreated = {},
                onCreateCustom = {},
            )
        }

        composeRule.onNodeWithText("胸围").performClick()
        composeRule.onNodeWithText("创建 1 个指标").performClick()
        composeRule.waitUntil(5_000) { requested != null }

        assertEquals(listOf("chest"), requested?.map { it.key })
    }

    @Test
    fun templateReportsWhenNothingCouldBeCreated() {
        show {
            TemplateSheet(
                existingNames = setOf("体重", "腰围"),
                onDismiss = {},
                onCreate = { TemplateResult(created = 0, skipped = listOf("胸围")) },
                onCreated = {},
                onCreateCustom = {},
            )
        }

        composeRule.onNodeWithText("胸围").performClick()
        composeRule.onNodeWithText("创建 1 个指标").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("这些指标都已经在列表里了")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun quickRecordNeedsValidNumberBeforeSaving() {
        var received: MeasurementInput? = null
        var saved: MeasurementChange? = null
        show {
            QuickRecordSheet(
                metrics = listOf(WEIGHT, WAIST),
                today = TODAY,
                initialMetricId = WEIGHT.id,
                measurementsByMetric = emptyMap(),
                onDismiss = {},
                onSave = { input, _ ->
                    received = input
                    MeasurementWriteResult.Saved(
                        MeasurementChange(
                            saved = measured(WEIGHT.id, input.date, input.value),
                            previous = null,
                            replaced = null,
                        ),
                    )
                },
                onSaved = { saved = it },
            )
        }

        composeRule.onNodeWithText("保存记录").assertIsNotEnabled()
        enterValue("71.6")
        composeRule.onNodeWithText("保存记录").assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) { saved != null }

        assertEquals("weight", received?.metricId)
        assertEquals(71.6, received!!.value, 0.0)
        assertNotNull(saved)
    }

    @Test
    fun quickRecordSameDayConflictOffersReplace() {
        var calls = 0
        var replaced = false
        var saved: MeasurementChange? = null
        val existing = measured(WEIGHT.id, TODAY, 71.2)
        show {
            QuickRecordSheet(
                metrics = listOf(WEIGHT),
                today = TODAY,
                initialMetricId = WEIGHT.id,
                measurementsByMetric = emptyMap(),
                onDismiss = {},
                onSave = { input, replace ->
                    calls++
                    if (!replace) {
                        MeasurementWriteResult.Conflict(existing)
                    } else {
                        replaced = true
                        MeasurementWriteResult.Saved(
                            MeasurementChange(
                                saved = measured(WEIGHT.id, input.date, input.value),
                                previous = null,
                                replaced = existing,
                            ),
                        )
                    }
                },
                onSaved = { saved = it },
            )
        }

        enterValue("71.8")
        composeRule.onNodeWithText("保存记录").performClick()
        composeRule.onNodeWithText("这一天已有记录").assertExists()
        composeRule.onNodeWithText("替换").performClick()
        composeRule.waitUntil(5_000) { saved != null }

        assertTrue(replaced)
        assertEquals(2, calls)
    }

    @Test
    fun quickRecordLargeJumpAsksForConfirmation() {
        var saved: MeasurementChange? = null
        show {
            QuickRecordSheet(
                metrics = listOf(WEIGHT),
                today = TODAY,
                initialMetricId = WEIGHT.id,
                measurementsByMetric = mapOf(
                    WEIGHT.id to listOf(measured(WEIGHT.id, TODAY.minusDays(1), 71.0)),
                ),
                onDismiss = {},
                onSave = { input, _ ->
                    MeasurementWriteResult.Saved(
                        MeasurementChange(
                            saved = measured(WEIGHT.id, input.date, input.value),
                            previous = null,
                            replaced = null,
                        ),
                    )
                },
                onSaved = { saved = it },
            )
        }

        enterValue("120")
        composeRule.onNodeWithText("保存记录").performClick()
        composeRule.onNodeWithText("确认这次记录？").assertExists()

        composeRule.onNodeWithText("再检查一下").performClick()
        composeRule.waitForIdle()
        assertEquals(null, saved)

        composeRule.onNodeWithText("保存记录").performClick()
        composeRule.onNodeWithText("仍然保存").performClick()
        composeRule.waitUntil(5_000) { saved != null }
    }

    private fun enterValue(value: String) {
        value.forEach { char ->
            val tag = if (char == '.') "QuickRecordKey-dot" else "QuickRecordKey-$char"
            composeRule.onNodeWithTag(tag).performClick()
        }
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
        val WEIGHT: Metric = rawMetric("weight", name = "体重", unit = "kg", precision = 1)
        val WAIST: Metric = rawMetric("waist", name = "腰围", unit = "cm", precision = 1)
    }
}
