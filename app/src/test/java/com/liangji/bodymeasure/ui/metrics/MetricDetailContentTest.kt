package com.liangji.bodymeasure.ui.metrics

import android.app.Application
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.testing.derivedMetric
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.LocalDate
import org.junit.Assert.assertEquals
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
class MetricDetailContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var editedId: String? = null
    private var archived = false
    private var restored = false
    private var recorded = false
    private var editRequested = false

    @Test
    fun tappingHistoryRowStartsEditing() {
        show(state(archived = false))

        composeRule.onNodeWithText("10月30日").performClick()

        assertEquals("waist-2026-10-30", editedId)
    }

    @Test
    fun menuArchivesActiveMetric() {
        show(state(archived = false))

        composeRule.onNodeWithContentDescription("更多操作").performClick()
        composeRule.onNodeWithText("归档").performClick()

        assertTrue(archived)
    }

    @Test
    fun archivedMetricShowsBannerAndRestores() {
        show(state(archived = true))

        composeRule.onNodeWithText("已归档，仅历史可见").assertExists()
        composeRule.onNodeWithText("恢复").performClick()

        assertTrue(restored)
    }

    @Test
    fun recordButtonRequestsQuickRecord() {
        show(state(archived = false))

        composeRule.onNodeWithText("记下此刻").performClick()

        assertTrue(recorded)
    }

    @Test
    fun derivedDetailShowsFormulaAndEditsInsteadOfRecording() {
        show(derivedState())

        composeRule.onNodeWithText("派生公式").assertExists()
        composeRule.onNodeWithText("[体重] / [身高] ^ 2").assertExists()
        composeRule.onNodeWithText("记下此刻").assertDoesNotExist()
        composeRule.onNodeWithText("编辑公式").performClick()

        assertTrue(editRequested)
    }

    @Test
    fun derivedHistoryRowDoesNotStartEditing() {
        show(derivedState())

        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText("10月30日", substring = true))
        composeRule.onNodeWithText("10月30日").performClick()

        assertEquals(null, editedId)
    }

    private fun show(state: MetricDetailState) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground {
                    MetricDetailContent(
                        state = state,
                        prompt = null,
                        onBack = {},
                        onEditMetric = { editRequested = true },
                        onArchive = { archived = true },
                        onRestore = { restored = true },
                        onRecord = { recorded = true },
                        onEditMeasurement = { editedId = it.id },
                        onUndo = {},
                        onDismissPrompt = {},
                    )
                }
            }
        }
    }

    private fun state(archived: Boolean): MetricDetailState {
        val metric = rawMetric("waist", name = "腰围", unit = "cm").copy(isArchived = archived)
        val measurements = listOf(
            measured("waist", TODAY, 78.5, note = "早晨空腹"),
            measured("waist", TODAY.minusDays(7), 79.7),
        )
        return metricDetailState(metric, measurements, TODAY)
    }

    private fun derivedState(): MetricDetailState {
        val metric = derivedMetric(
            id = "bmi",
            formula = "{weight} / {height} ^ 2",
            name = "BMI",
            unit = "",
        ).copy(formulaDisplay = "[体重] / [身高] ^ 2")
        val series = listOf(
            measured("bmi", TODAY, 24.1),
            measured("bmi", TODAY.minusDays(7), 24.4),
        )
        return metricDetailState(metric, series, TODAY)
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
    }
}
