package com.liangji.bodymeasure.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import com.liangji.bodymeasure.ui.metrics.MeasurementEditSheet
import com.liangji.bodymeasure.ui.metrics.QuickRecordSheet
import com.liangji.bodymeasure.ui.metrics.TemplateResult
import com.liangji.bodymeasure.ui.metrics.TemplateSheet
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.time.LocalDate
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
class SheetScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickRecordLight() = capture("quick-record-light", ThemeMode.LIGHT, input = "71.2") {
        QuickRecordSheet(
            metrics = METRICS,
            today = TODAY,
            initialMetricId = WEIGHT.id,
            measurementsByMetric = MEASUREMENTS.groupBy { it.metricId },
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
        )
    }

    @Test
    fun quickRecordDark() = capture("quick-record-dark", ThemeMode.DARK, input = "78.5") {
        QuickRecordSheet(
            metrics = METRICS,
            today = TODAY,
            initialMetricId = WAIST.id,
            measurementsByMetric = MEASUREMENTS.groupBy { it.metricId },
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
        )
    }

    @Test
    fun templateLight() = capture("template-light", ThemeMode.LIGHT, input = null) {
        TemplateSheet(
            existingNames = setOf("体重"),
            onDismiss = {},
            onCreate = { TemplateResult(0, emptyList()) },
            onCreated = {},
            onCreateCustom = {},
        )
    }

    @Test
    fun templateDark() = capture("template-dark", ThemeMode.DARK, input = null) {
        TemplateSheet(
            existingNames = emptySet(),
            onDismiss = {},
            onCreate = { TemplateResult(0, emptyList()) },
            onCreated = {},
            onCreateCustom = {},
        )
    }

    @Test
    fun measurementEditLight() = capture("measurement-edit-light", ThemeMode.LIGHT, input = null) {
        MeasurementEditSheet(
            measurement = measured("waist", TODAY.minusDays(7), 79.7, note = "早晨空腹"),
            metric = WAIST,
            today = TODAY,
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
            onDelete = {},
        )
    }

    @Test
    fun measurementEditDark() = capture("measurement-edit-dark", ThemeMode.DARK, input = null) {
        MeasurementEditSheet(
            measurement = measured("weight", TODAY.minusDays(5), 71.9),
            metric = WEIGHT,
            today = TODAY,
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
            onDelete = {},
        )
    }

    @Test
    fun quickRecordLargeFont() = capture(
        "quick-record-font-200",
        ThemeMode.LIGHT,
        input = "71.2",
        fontScale = 2f,
    ) {
        QuickRecordSheet(
            metrics = METRICS,
            today = TODAY,
            initialMetricId = WEIGHT.id,
            measurementsByMetric = MEASUREMENTS.groupBy { it.metricId },
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
        )
    }

    @Test
    fun templateLargeFont() = capture(
        "template-font-200",
        ThemeMode.LIGHT,
        input = null,
        fontScale = 2f,
    ) {
        TemplateSheet(
            existingNames = setOf("体重"),
            onDismiss = {},
            onCreate = { TemplateResult(0, emptyList()) },
            onCreated = {},
            onCreateCustom = {},
        )
    }

    @Test
    fun measurementEditLargeFont() = capture(
        "measurement-edit-font-200",
        ThemeMode.LIGHT,
        input = null,
        fontScale = 2f,
    ) {
        MeasurementEditSheet(
            measurement = measured("waist", TODAY.minusDays(7), 79.7, note = "早晨空腹"),
            metric = WAIST,
            today = TODAY,
            onDismiss = {},
            onSave = { _, _ -> MeasurementWriteResult.Invalid },
            onSaved = {},
            onDelete = {},
        )
    }

    @Test
    fun quickRecordSavesEnteredValue() {
        var saved: MeasurementChange? = null
        var received: MeasurementInput? = null
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground {
                    QuickRecordSheet(
                        metrics = METRICS,
                        today = TODAY,
                        initialMetricId = WEIGHT.id,
                        measurementsByMetric = MEASUREMENTS.groupBy { it.metricId },
                        onDismiss = {},
                        onSave = { input, replace ->
                            received = input
                            if (!replace) MeasurementWriteResult.Saved(
                                MeasurementChange(
                                    saved = measured("weight", input.date, input.value),
                                    previous = null,
                                    replaced = null,
                                ),
                            ) else MeasurementWriteResult.Invalid
                        },
                        onSaved = { saved = it },
                    )
                }
            }
        }
        enterValue("71.2")
        composeRule.onNodeWithText("保存记录").performClick()
        composeRule.waitForIdle()

        check(received?.value == 71.2)
        check(received?.metricId == "weight")
        check(saved != null)
    }

    private fun capture(
        name: String,
        theme: ThemeMode,
        input: String?,
        fontScale: Float = 1f,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = theme) {
                SoftGradientBackground {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale),
                    ) {
                        content()
                    }
                }
            }
        }
        if (input != null) {
            enterValue(input)
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("build/screenshots/$name.png")
    }

    private fun enterValue(value: String) {
        value.forEach { char ->
            val tag = if (char == '.') "QuickRecordKey-dot" else "QuickRecordKey-$char"
            composeRule.onNodeWithTag(tag).performClick()
        }
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
        val WEIGHT: Metric = rawMetric("weight", name = "体重", unit = "kg", precision = 1)
        val WAIST: Metric = rawMetric("waist", name = "腰围", unit = "cm", precision = 1)
        val CHEST: Metric = rawMetric("chest", name = "胸围", unit = "cm", precision = 1)
        val METRICS = listOf(WEIGHT, WAIST, CHEST)
        val MEASUREMENTS = listOf(
            measured("weight", TODAY.minusDays(1), 71.6),
            measured("weight", TODAY.minusDays(5), 71.9),
            measured("waist", TODAY.minusDays(2), 79.1),
            measured("waist", TODAY.minusDays(9), 79.6),
            measured("waist", TODAY, 78.5),
            measured("chest", TODAY.minusDays(2), 96.0),
        )
    }
}
