package com.liangji.bodymeasure.ui.metrics

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.formula.FormulaPresetCatalog
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.summary.MetricSummaries
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import com.liangji.bodymeasure.ui.components.UndoPrompt
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
class MetricManageFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorEnablesSaveOnlyAfterTypingName() {
        var saved = false
        show {
            var state by remember { mutableStateOf(MetricEditorState(isNew = true)) }
            MetricEditorContent(
                state = state,
                onBack = {},
                onNameChange = { name -> state = state.copy(name = name, nameError = null) },
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onSave = { saved = true },
            )
        }

        composeRule.onNodeWithText("创建指标").assertIsNotEnabled()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("腰围")
        composeRule.onNodeWithText("创建指标").assertIsEnabled().performClick()

        assertTrue(saved)
    }

    @Test
    fun editorShowsDuplicateNameError() {
        show {
            MetricEditorContent(
                state = MetricEditorState(
                    isNew = true,
                    name = "体重",
                    unit = "kg",
                    nameError = "已经有同名指标「体重」",
                ),
                onBack = {},
                onNameChange = {},
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onSave = {},
            )
        }

        composeRule.onNodeWithText("已经有同名指标「体重」").assertExists()
        composeRule.onNodeWithText("创建指标").assertIsEnabled()
    }

    @Test
    fun derivedEditorShowsFormulaErrorAndInsertsReference() {
        var inserted: Metric? = null
        show {
            MetricEditorContent(
                state = MetricEditorState(
                    isNew = true,
                    name = "BMI",
                    kind = MetricKind.DERIVED,
                    formula = "[体重] / ",
                    formulaError = "第 6 位：缺少操作数",
                    referencedIds = listOf(WEIGHT.id),
                    availableMetrics = listOf(WEIGHT),
                    loading = false,
                ),
                onBack = {},
                onNameChange = {},
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onInsertReference = { inserted = it },
                onSave = {},
            )
        }

        composeRule.onNodeWithText("第 6 位：缺少操作数").assertExists()
        composeRule.onNodeWithText("创建指标").assertIsNotEnabled()
        composeRule.onNodeWithText("＋ 体重").performScrollTo().performClick()

        assertEquals("weight", inserted?.id)
    }

    @Test
    fun derivedEditorAppliesFormulaPreset() {
        val preset = FormulaPresetCatalog.Preset(
            id = "whtr",
            title = "腰高比",
            description = "腰围 ÷ 身高",
            formula = "([腰围] / 100) / [身高]",
            unit = "",
        )
        var appliedFormula: String? = null
        show {
            var state by remember {
                mutableStateOf(
                    MetricEditorState(
                        isNew = true,
                        name = "BMI",
                        kind = MetricKind.DERIVED,
                        formulaPresets = listOf(preset),
                        availableMetrics = listOf(WEIGHT),
                        loading = false,
                    ),
                )
            }
            MetricEditorContent(
                state = state,
                onBack = {},
                onNameChange = {},
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onApplyPreset = {
                    appliedFormula = it.formula
                    state = state.copy(formula = it.formula, selectedPresetId = it.id)
                },
                onSave = {},
            )
        }

        // performScrollTo only guarantees the row is inside the scroll viewport,
        // which now extends behind the floating save button; scroll it clear
        // from above the button so the drag is not captured by the button.
        composeRule.onNodeWithText("腰高比").performScrollTo()
        composeRule.onNode(hasScrollAction()).performTouchInput {
            swipeUp(startY = bottom * 0.7f, endY = bottom * 0.5f, durationMillis = 400)
        }
        composeRule.onNodeWithText("腰高比").performClick()

        assertEquals("([腰围] / 100) / [身高]", appliedFormula)
        composeRule.onNodeWithText("腰围 ÷ 身高").assertExists()
    }

    @Test
    fun measurementEditSavesChangedValueAndNote() {
        var savedValue: Double? = null
        var savedNote: String? = null
        var saved: MeasurementChange? = null
        val target = measured(WAIST.id, TODAY.minusDays(7), 79.7, note = "早晨空腹")
        show {
            MeasurementEditSheet(
                measurement = target,
                metric = WAIST,
                today = TODAY,
                onDismiss = {},
                onSave = { value, note ->
                    savedValue = value
                    savedNote = note
                    MeasurementWriteResult.Saved(
                        MeasurementChange(
                            saved = target.copy(value = value, note = note),
                            previous = target,
                            replaced = null,
                        ),
                    )
                },
                onSaved = { saved = it },
                onDelete = {},
            )
        }

        composeRule.onNodeWithText("保存修改").assertIsNotEnabled()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("79.2")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextReplacement("饭后")
        composeRule.onNodeWithText("保存修改").assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) { saved != null }

        assertEquals(79.2, savedValue!!, 0.0)
        assertEquals("饭后", savedNote)
    }

    @Test
    fun measurementEditDeletesRecord() {
        var deleted = false
        show {
            MeasurementEditSheet(
                measurement = measured(WAIST.id, TODAY, 78.5),
                metric = WAIST,
                today = TODAY,
                onDismiss = {},
                onSave = { _, _ -> MeasurementWriteResult.Invalid },
                onSaved = {},
                onDelete = { deleted = true },
            )
        }

        composeRule.onNodeWithText("删除这条记录").performClick()

        assertTrue(deleted)
    }

    @Test
    fun measurementEditReportsConflict() {
        val target = measured(WAIST.id, TODAY, 78.5)
        show {
            MeasurementEditSheet(
                measurement = target,
                metric = WAIST,
                today = TODAY,
                onDismiss = {},
                onSave = { _, _ -> MeasurementWriteResult.Conflict(target) },
                onSaved = {},
                onDelete = {},
            )
        }

        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("78.0")
        composeRule.onNodeWithText("保存修改").performClick()
        composeRule.onNodeWithText("这一天已经有一条记录").assertExists()
    }

    @Test
    fun archivedCardOpensRestoresAndRequestsDelete() {
        var opened: String? = null
        var restored: String? = null
        var deleteRequested: Metric? = null
        show {
            ArchivedMetricsContent(
                metrics = listOf(WEIGHT),
                onBack = {},
                onOpenMetric = { opened = it },
                onRestore = { restored = it },
                onDeleteRequest = { deleteRequested = it },
            )
        }

        composeRule.onNodeWithText("体重").performClick()
        composeRule.onNodeWithText("恢复到首页").performClick()
        composeRule.onNodeWithText("删除").performClick()

        assertEquals("weight", opened)
        assertEquals("weight", restored)
        assertEquals("weight", deleteRequested?.id)
    }

    @Test
    fun actionSheetArchivesMetric() {
        var archived = false
        show {
            MetricActionSheet(
                metric = WAIST,
                position = 1,
                count = 2,
                onDismiss = {},
                onMove = {},
                onEdit = {},
                onArchive = { archived = true },
            )
        }

        composeRule.onNodeWithText("置顶").assertIsNotEnabled()
        composeRule.onNodeWithText("归档").performClick()

        assertTrue(archived)
    }

    @Test
    fun emptyHomeStartsFromTemplates() {
        var templates = false
        show {
            Home(
                state = HomeUiState(today = TODAY, isLoading = false),
                onOpenTemplates = { templates = true },
            )
        }

        composeRule.onNodeWithText("从模板开始").performClick()

        assertTrue(templates)
    }

    @Test
    fun homeRecordButtonUsesLastMetric() {
        var quickRecordId: String? = null
        show {
            Home(state = homeState(), onQuickRecord = { quickRecordId = it })
        }

        composeRule.onNodeWithText("记下此刻").performClick()

        assertEquals("waist", quickRecordId)
    }

    @Test
    fun homeLongPressOpensActions() {
        var longPressed: String? = null
        show {
            Home(
                state = homeState(),
                onLongPressMetric = { longPressed = it.id },
            )
        }

        composeRule.onNodeWithText("体重").performTouchInput { longClick() }

        assertEquals("weight", longPressed)
    }

    @Test
    fun undoBannerRestoresTheRecord() {
        var undone = false
        show {
            MetricsContent(
                state = homeState(),
                prompt = UndoPrompt(
                    id = 1L,
                    message = "已记录 体重 71.2 kg",
                    onUndo = { undone = true },
                ),
                onOpenSettings = {},
                onCreateMetric = {},
                onOpenTemplates = {},
                onOpenMetric = {},
                onLongPressMetric = {},
                onQuickRecord = {},
                onUndo = { undone = true },
                onDismissPrompt = {},
            )
        }

        composeRule.onNodeWithText("已记录 体重 71.2 kg").assertExists()
        composeRule.onNodeWithText("撤销").performClick()

        assertTrue(undone)
    }

    @androidx.compose.runtime.Composable
    private fun Home(
        state: HomeUiState,
        onOpenTemplates: () -> Unit = {},
        onLongPressMetric: (Metric) -> Unit = {},
        onQuickRecord: (String) -> Unit = {},
    ) {
        MetricsContent(
            state = state,
            prompt = null,
            onOpenSettings = {},
            onCreateMetric = {},
            onOpenTemplates = onOpenTemplates,
            onOpenMetric = {},
            onLongPressMetric = onLongPressMetric,
            onQuickRecord = onQuickRecord,
            onUndo = {},
            onDismissPrompt = {},
        )
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun homeState(): HomeUiState {
        val metrics = listOf(WEIGHT, WAIST)
        val measurements = listOf(
            measured(WEIGHT.id, TODAY, 71.2),
            measured(WEIGHT.id, TODAY.minusDays(2), 71.6),
            measured(WAIST.id, TODAY.minusDays(1), 78.5),
        )
        return HomeUiState(
            today = TODAY,
            snapshots = MetricSummaries.build(metrics, measurements, TODAY),
            measurementsByMetric = measurements.groupBy(Measurement::metricId),
            lastMetricId = WAIST.id,
            isLoading = false,
        )
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
        val WEIGHT: Metric = rawMetric("weight", name = "体重", unit = "kg", precision = 1)
        val WAIST: Metric = rawMetric("waist", name = "腰围", unit = "cm", precision = 1)
    }
}
