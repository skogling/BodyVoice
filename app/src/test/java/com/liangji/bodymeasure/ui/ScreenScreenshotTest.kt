package com.liangji.bodymeasure.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricIconKeys
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricPalette
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.formula.FormulaPresetCatalog
import com.liangji.bodymeasure.domain.formula.FormulaZeroGuard
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.domain.summary.DerivedSeriesCalculator
import com.liangji.bodymeasure.domain.summary.MetricSummaries
import com.liangji.bodymeasure.testing.derivedMetric
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import com.liangji.bodymeasure.ui.metrics.ArchivedMetricsContent
import com.liangji.bodymeasure.ui.metrics.HomeUiState
import com.liangji.bodymeasure.ui.metrics.MetricChartSection
import com.liangji.bodymeasure.ui.metrics.MetricDetailContent
import com.liangji.bodymeasure.ui.metrics.MetricDetailState
import com.liangji.bodymeasure.ui.metrics.MetricEditorContent
import com.liangji.bodymeasure.ui.metrics.MetricEditorState
import com.liangji.bodymeasure.ui.metrics.MetricsContent
import com.liangji.bodymeasure.ui.metrics.metricDetailState
import com.liangji.bodymeasure.ui.settings.BackupUiState
import com.liangji.bodymeasure.ui.settings.SettingsContent
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import kotlin.math.sin
import kotlin.math.roundToInt
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
class ScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeLight() = capture("home-light", ThemeMode.LIGHT) {
        Home(state = homeState())
    }

    @Test
    fun homeDark() = capture("home-dark", ThemeMode.DARK) {
        Home(state = homeState())
    }

    @Test
    @Config(qualifiers = "w360dp-h801dp-xxhdpi")
    fun homePhone() = capture("home-phone", ThemeMode.LIGHT) {
        Home(state = homeState())
    }

    @Test
    fun homeDerivedLight() = capture("home-derived-light", ThemeMode.LIGHT) {
        Home(state = derivedHomeState())
    }

    @Test
    fun homeDerivedDark() = capture("home-derived-dark", ThemeMode.DARK) {
        Home(state = derivedHomeState())
    }

    @Test
    fun settingsLight() = capture("settings-light", ThemeMode.LIGHT) {
        Settings()
    }

    @Test
    fun settingsDark() = capture("settings-dark", ThemeMode.DARK) {
        Settings(themeMode = ThemeMode.DARK, archivedCount = 0)
    }

    @Test
    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    fun settingsFullLight() = capture("settings-full-light", ThemeMode.LIGHT) {
        Settings()
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp-xxhdpi")
    fun settingsTablet() = capture("settings-tablet", ThemeMode.LIGHT) {
        Settings()
    }

    @Test
    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    fun settingsRewardDialogLight() {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground {
                    Box(Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                        Settings()
                    }
                }
            }
        }
        composeRule.onNodeWithText("赞赏作者").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/screenshots/settings-reward-light.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h1100dp-xxhdpi")
    fun homeTablet() = capture("home-tablet", ThemeMode.LIGHT) {
        Home(state = homeState())
    }

    @Test
    fun homeLargeFont() = capture("home-font-200", ThemeMode.LIGHT, fontScale = 2f) {
        Home(state = homeState())
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun homeLargeFontFullPage() = capture("home-font-200-full", ThemeMode.LIGHT, fontScale = 2f) {
        Home(state = homeState())
    }

    @Test
    fun homeEmptyDark() = capture("home-empty-dark", ThemeMode.DARK) {
        Home(state = HomeUiState(today = TODAY, isLoading = false))
    }

    @Test
    fun homeLoadingLight() = capture("home-loading-light", ThemeMode.LIGHT) {
        Home(state = HomeUiState(today = TODAY, isLoading = true))
    }

    @Test
    fun editorLight() = capture("metric-editor-light", ThemeMode.LIGHT) {
        Editor(
            MetricEditorState(
                isNew = true,
                name = "腰围",
                unit = "cm",
                precision = 1,
                colorArgb = MetricPalette.OCHRE,
                iconKey = MetricIconKeys.WAIST,
            ),
        )
    }

    @Test
    fun editorDark() = capture("metric-editor-dark", ThemeMode.DARK) {
        Editor(
            MetricEditorState(
                isNew = false,
                name = "体重",
                unit = "kg",
                precision = 1,
                colorArgb = MetricPalette.CLAY,
                iconKey = MetricIconKeys.WEIGHT,
            ),
        )
    }

    @Test
    fun editorDerivedLight() = capture("metric-editor-derived-light", ThemeMode.LIGHT) {
        Editor(derivedEditorState())
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun editorDerivedFullPageDark() = capture("metric-editor-derived-dark-full", ThemeMode.DARK) {
        Editor(derivedEditorState())
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun editorFormulaZeroLight() = capture("metric-editor-zero-light", ThemeMode.LIGHT) {
        Editor(
            derivedEditorState().copy(
                formula = "[体重] / [身高] ^ 2",
                formulaPreview = null,
                formulaSample = 0.0024,
                formulaZeroHint = FormulaZeroGuard.Hint(
                    rawValue = 0.0024,
                    suggestion = "[体重] / ([身高] / 100) ^ 2",
                ),
            ),
        )
    }

    @Test
    fun archivedLight() = capture("archived-light", ThemeMode.LIGHT) {
        ArchivedMetricsContent(
            metrics = listOf(
                rawMetric("weight", name = "体重", unit = "kg"),
                rawMetric("chest", name = "胸围", unit = "cm"),
            ),
            onBack = {},
            onOpenMetric = {},
            onRestore = {},
            onDeleteRequest = {},
        )
    }

    @Test
    fun archivedEmptyDark() = capture("archived-empty-dark", ThemeMode.DARK) {
        ArchivedMetricsContent(
            metrics = emptyList(),
            onBack = {},
            onOpenMetric = {},
            onRestore = {},
            onDeleteRequest = {},
        )
    }

    @Test
    fun detailLight() = capture("metric-detail-light", ThemeMode.LIGHT) {
        Detail(detailState(archived = false))
    }

    @Test
    fun detailDark() = capture("metric-detail-dark", ThemeMode.DARK) {
        Detail(detailState(archived = false))
    }

    @Test
    @Config(qualifiers = "w840dp-h1100dp-xxhdpi")
    fun detailTablet() = capture("metric-detail-tablet", ThemeMode.LIGHT) {
        Detail(detailState(archived = false))
    }

    @Test
    fun detailArchived() = capture("metric-detail-archived", ThemeMode.LIGHT) {
        Detail(detailState(archived = true))
    }

    @Test
    fun detailEmptyDark() = capture("metric-detail-empty-dark", ThemeMode.DARK) {
        Detail(detailState(archived = false, withRecords = false))
    }

    @Test
    fun detailDerivedLight() = capture("metric-detail-derived-light", ThemeMode.LIGHT) {
        Detail(derivedDetailState())
    }

    @Test
    fun detailDerivedDark() = capture("metric-detail-derived-dark", ThemeMode.DARK) {
        Detail(derivedDetailState())
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun detailDerivedFullPage() = capture("metric-detail-derived-full", ThemeMode.LIGHT) {
        Detail(derivedDetailState())
    }

    @Test
    fun detailLargeFont() = capture("metric-detail-font-200", ThemeMode.LIGHT, fontScale = 2f) {
        Detail(detailState(archived = false))
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun detailTwoMonthsFullPage() = capture("metric-detail-full", ThemeMode.LIGHT) {
        Detail(detailState(archived = false, extended = true))
    }

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun detailChartsFullPage() = capture("metric-detail-charts-full", ThemeMode.LIGHT) {
        Detail(chartDetailState())
    }

    @Test
    fun chartTrendLight() = captureCharts("charts-trend-light", ThemeMode.LIGHT)

    @Test
    fun chartTrendDark() = captureCharts("charts-trend-dark", ThemeMode.DARK)

    @Test
    fun chartTrendScrubLight() = captureCharts("charts-trend-scrub-light", ThemeMode.LIGHT) {
        composeRule.onNode(hasContentDescription("趋势图", substring = true))
            .performTouchInput { click(center) }
    }

    @Test
    fun chartDistributionLight() = captureCharts("charts-distribution-light", ThemeMode.LIGHT) {
        composeRule.onNodeWithText("分布").performClick()
    }

    @Test
    fun chartCalendarLight() = captureCharts("charts-calendar-light", ThemeMode.LIGHT) {
        composeRule.onNodeWithText("日历").performClick()
    }

    @Test
    fun chartComparisonLight() = captureCharts("charts-comparison-light", ThemeMode.LIGHT) {
        composeRule.onNodeWithText("对比").performClick()
    }

    @Test
    @Config(qualifiers = "w393dp-h1400dp-xxhdpi")
    fun chartTrendLargeFont() = captureCharts(
        name = "charts-trend-font-200",
        theme = ThemeMode.LIGHT,
        fontScale = 2f,
    )

    @Test
    @Config(qualifiers = "w393dp-h1500dp-xxhdpi")
    fun chartCalendarLargeFont() = captureCharts(
        name = "charts-calendar-font-200",
        theme = ThemeMode.LIGHT,
        fontScale = 2f,
    ) {
        composeRule.onNodeWithText("日历").performClick()
    }

    @Composable
    private fun Editor(state: MetricEditorState) {
        MetricEditorContent(
            state = state,
            onBack = {},
            onNameChange = {},
            onUnitChange = {},
            onPrecisionChange = {},
            onColorChange = {},
            onIconChange = {},
            onSave = {},
        )
    }

    @Composable
    private fun Settings(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        archivedCount: Int = 2,
        backup: BackupUiState = BackupUiState(),
    ) {
        SettingsContent(
            themeMode = themeMode,
            archivedCount = archivedCount,
            backup = backup,
            onSelectTheme = {},
            onOpenArchive = {},
            onExportBackup = {},
            onExportCsv = {},
            onImportBackup = {},
            onConfirmImport = {},
            onCancelImport = {},
            onDismissNotice = {},
            onBack = {},
        )
    }

    @Composable
    private fun Home(state: HomeUiState) {
        MetricsContent(
            state = state,
            prompt = null,
            onOpenSettings = {},
            onCreateMetric = {},
            onOpenTemplates = {},
            onOpenMetric = {},
            onLongPressMetric = {},
            onQuickRecord = {},
            onUndo = {},
            onDismissPrompt = {},
        )
    }

    @Composable
    private fun Detail(state: MetricDetailState) {
        MetricDetailContent(
            state = state,
            prompt = null,
            onBack = {},
            onEditMetric = {},
            onArchive = {},
            onRestore = {},
            onRecord = {},
            onEditMeasurement = {},
            onUndo = {},
            onDismissPrompt = {},
        )
    }

    private fun detailState(
        archived: Boolean,
        withRecords: Boolean = true,
        extended: Boolean = false,
    ): MetricDetailState {
        val waist = rawMetric("waist", name = "腰围", unit = "cm", precision = 1).copy(
            colorArgb = MetricPalette.OCHRE,
            iconKey = MetricIconKeys.WAIST,
            isArchived = archived,
        )
        val measurements = when {
            !withRecords -> emptyList()
            extended -> listOf(
                measured("waist", TODAY, 78.5, note = "早晨空腹"),
                measured("waist", TODAY.minusDays(7), 79.7),
                measured("waist", TODAY.minusDays(14), 79.2),
                measured("waist", TODAY.minusDays(21), 80.1, note = "晚饭后"),
                measured("waist", TODAY.minusDays(35), 80.4),
                measured("waist", TODAY.minusDays(45), 79.9),
                measured("waist", TODAY.minusDays(58), 80.8, note = "换新尺子"),
            )
            else -> listOf(
                measured("waist", TODAY, 78.5, note = "早晨空腹"),
                measured("waist", TODAY.minusDays(7), 79.7),
                measured("waist", TODAY.minusDays(14), 79.2),
                measured("waist", TODAY.minusDays(21), 80.1, note = "晚饭后"),
                measured("waist", TODAY.minusDays(35), 80.4),
            )
        }
        return metricDetailState(waist, measurements, TODAY)
    }

    private fun captureCharts(
        name: String,
        theme: ThemeMode,
        fontScale: Float = 1f,
        interact: () -> Unit = {},
    ) {
        val metric = rawMetric("waist", name = "腰围", unit = "cm", precision = 1).copy(
            colorArgb = MetricPalette.OCHRE,
            iconKey = MetricIconKeys.WAIST,
        )
        composeRule.setContent {
            BodyMeasureTheme(themeMode = theme) {
                SoftGradientBackground {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                        ) {
                            MetricChartSection(
                                metric = metric,
                                measurements = chartMeasurements(),
                                today = TODAY,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        interact()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun chartDetailState(): MetricDetailState {
        val waist = rawMetric("waist", name = "腰围", unit = "cm", precision = 1).copy(
            colorArgb = MetricPalette.OCHRE,
            iconKey = MetricIconKeys.WAIST,
        )
        val measurements = chartMeasurements().map { measurement ->
            if (measurement.date == TODAY) measurement.copy(note = "早晨空腹") else measurement
        }
        return metricDetailState(waist, measurements, TODAY)
    }

    private fun chartMeasurements(): List<Measurement> =
        (0..44).map { step ->
            val daysAgo = 88L - step * 2
            val raw = 78.5 + daysAgo / 88.0 * 1.8 + sin(daysAgo * 0.42) * 0.25
            measured(
                "waist",
                TODAY.minusDays(daysAgo),
                (raw * 10).roundToInt() / 10.0,
            )
        }

    private fun derivedDetailState(): MetricDetailState {
        val (metrics, measurements) = derivedScenario()
        val bmi = metrics.last()
        val series = DerivedSeriesCalculator.build(metrics, measurements)[bmi.id].orEmpty()
        val history = listOf(
            FormulaHistory(
                id = "bmi-v1",
                metricId = bmi.id,
                version = 1,
                formula = "{weight} / {height} ^ 2",
                formulaDisplay = "[体重] / [身高] ^ 2",
                unit = "",
                changeNote = null,
                createdAt = TODAY.minusDays(20).toEpochDay() * 86_400_000,
                originDeviceId = "test-device",
            ),
            FormulaHistory(
                id = "bmi-v2",
                metricId = bmi.id,
                version = 2,
                formula = "{weight} / {height} ^ 2",
                formulaDisplay = "[体重] / [身高] ^ 2",
                unit = "",
                changeNote = "改用最新身高",
                createdAt = TODAY.minusDays(3).toEpochDay() * 86_400_000,
                originDeviceId = "test-device",
            ),
        )
        return metricDetailState(bmi, series, TODAY).copy(formulaHistory = history)
    }

    private fun derivedHomeState(): HomeUiState {
        val (metrics, measurements) = derivedScenario()
        val derived = DerivedSeriesCalculator.build(metrics, measurements)
        val merged = measurements + derived.values.flatten()
        return HomeUiState(
            today = TODAY,
            snapshots = MetricSummaries.build(metrics, merged, TODAY),
            measurementsByMetric = merged.groupBy { it.metricId },
            lastMetricId = metrics.first().id,
            isLoading = false,
        )
    }

    private fun derivedEditorState(): MetricEditorState {
        val (metrics, _) = derivedScenario()
        val weight = metrics[0]
        val height = metrics[1]
        return MetricEditorState(
            isNew = true,
            name = "BMI",
            unit = "",
            precision = 1,
            colorArgb = MetricPalette.TEAL,
            iconKey = MetricIconKeys.GENERIC,
            kind = MetricKind.DERIVED,
            formula = "[体重] / [身高] ^ 2",
            formulaPreview = "按最近记录试算：24.1",
            formulaSample = 24.1,
            formulaPresets = FormulaPresetCatalog.suggest(listOf(weight, height)),
            selectedPresetId = "bmi",
            referencedIds = listOf(weight.id, height.id),
            pairingPolicy = PairingPolicy.NEAREST,
            pairingWindowDays = 2,
            availableMetrics = listOf(weight, height),
            loading = false,
        )
    }

    private fun derivedScenario(): Pair<List<Metric>, List<Measurement>> {
        val weight = rawMetric("weight", name = "体重", unit = "kg", precision = 1).copy(
            colorArgb = MetricPalette.CLAY,
            iconKey = MetricIconKeys.WEIGHT,
        )
        val height = rawMetric("height", name = "身高", unit = "m", precision = 2, isConstant = true).copy(
            unitDimension = UnitDimension.LENGTH,
            colorArgb = MetricPalette.SAGE,
            iconKey = MetricIconKeys.HEIGHT,
        )
        val bmi = derivedMetric(
            id = "bmi",
            formula = "{weight} / {height} ^ 2",
            name = "BMI",
            unit = "",
            precision = 1,
        ).copy(
            colorArgb = MetricPalette.TEAL,
            iconKey = MetricIconKeys.GENERIC,
            formulaDisplay = "[体重] / [身高] ^ 2",
            formulaVersion = 2,
        )
        val measurements = listOf(
            measured("weight", TODAY, 71.2),
            measured("weight", TODAY.minusDays(2), 71.6),
            measured("weight", TODAY.minusDays(6), 71.9),
            measured("weight", TODAY.minusDays(11), 72.4),
            measured("weight", TODAY.minusDays(18), 72.1),
            measured("height", TODAY.minusDays(30), 1.72, note = "固定值"),
        )
        return listOf(weight, height, bmi) to measurements
    }

    private fun capture(
        name: String,
        theme: ThemeMode,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = theme) {
                SoftGradientBackground {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 24.dp),
                        ) {
                            content()
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun homeState(): HomeUiState {
        val weight = rawMetric("weight", name = "体重", unit = "kg", precision = 1)
        val waist = rawMetric("waist", name = "腰围", unit = "cm", precision = 1)
        val chest = rawMetric("chest", name = "胸围", unit = "cm", precision = 1)
        val arm = rawMetric("arm", name = "上臂围", unit = "cm", precision = 1)
        val hip = rawMetric("hip", name = "臀围", unit = "cm", precision = 1)
        val metrics = listOf(weight, waist, chest, arm, hip)
        val measurements = listOf(
            measured("weight", TODAY, 71.2),
            measured("weight", TODAY.minusDays(2), 71.6),
            measured("weight", TODAY.minusDays(6), 71.9),
            measured("weight", TODAY.minusDays(11), 72.4),
            measured("weight", TODAY.minusDays(18), 72.1),
            measured("waist", TODAY, 78.5),
            measured("waist", TODAY.minusDays(7), 79.1),
            measured("waist", TODAY.minusDays(15), 79.8),
            measured("chest", TODAY.minusDays(2), 96.0),
            measured("chest", TODAY.minusDays(12), 95.4),
            measured("arm", TODAY.minusDays(1), 30.6),
            measured("arm", TODAY.minusDays(9), 30.9),
            measured("hip", TODAY.minusDays(3), 92.4),
            measured("hip", TODAY.minusDays(13), 92.9),
        )
        return HomeUiState(
            today = TODAY,
            snapshots = MetricSummaries.build(metrics, measurements, TODAY),
            measurementsByMetric = measurements.groupBy { it.metricId },
            lastMetricId = weight.id,
            isLoading = false,
        )
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
    }
}
