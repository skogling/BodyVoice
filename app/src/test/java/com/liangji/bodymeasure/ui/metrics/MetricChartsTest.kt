package com.liangji.bodymeasure.ui.metrics

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
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
class MetricChartsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchesBetweenAllFourViews() {
        show(richMeasurements())

        composeRule.onNodeWithContentDescription("趋势图", substring = true).assertExists()
        composeRule.onNodeWithText("分布").performClick()
        composeRule.onNodeWithContentDescription("分布图", substring = true).assertExists()
        composeRule.onNodeWithText("中位数").assertExists()
        composeRule.onNodeWithText("日历").performClick()
        composeRule.onNodeWithText("2026年10月").assertExists()
        composeRule.onNodeWithText("对比").performClick()
        composeRule.onNodeWithText("平均变化").assertExists()
        composeRule.onNodeWithText("最近 30 天").assertExists()
    }

    @Test
    fun emptyRangeShowsHint() {
        show(listOf(measured("waist", TODAY.minusDays(400), 80.0)))

        composeRule.onNodeWithText("这段时间还没有记录").assertExists()
    }

    @Test
    fun wideningRangeRecoversOlderRecords() {
        show(
            listOf(
                measured("waist", TODAY.minusDays(101), 80.0),
                measured("waist", TODAY.minusDays(100), 79.8),
            ),
        )

        composeRule.onNodeWithText("1月").performClick()
        composeRule.onNodeWithText("这段时间还没有记录").assertExists()
        composeRule.onNodeWithText("6月").performClick()
        composeRule.onNodeWithContentDescription("趋势图", substring = true).assertExists()
    }

    @Test
    fun comparisonWithoutPreviousPeriodExplains() {
        show(
            listOf(
                measured("waist", TODAY, 78.5),
                measured("waist", TODAY.minusDays(2), 78.7),
            ),
        )

        composeRule.onNodeWithText("对比").performClick()

        composeRule.onNodeWithText("上一个周期还没有记录，暂时无法对比").assertExists()
        composeRule.onNodeWithText("之前 30 天").assertExists()
    }

    @Test
    fun calendarMovesToPreviousMonth() {
        show(richMeasurements())

        composeRule.onNodeWithText("日历").performClick()
        composeRule.onNodeWithText("2026年10月").assertExists()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithText("2026年9月").assertExists()
    }

    private fun show(measurements: List<Measurement>) {
        val metric = rawMetric("waist", name = "腰围", unit = "cm", precision = 1)
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    ) {
                        MetricChartSection(
                            metric = metric,
                            measurements = measurements,
                            today = TODAY,
                        )
                    }
                }
            }
        }
    }

    private fun richMeasurements(): List<Measurement> =
        (0..30).map { step ->
            val daysAgo = 60L - step * 2
            measured(
                "waist",
                TODAY.minusDays(daysAgo),
                80.2 - (60L - daysAgo) * 0.04,
            )
        }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 30)
    }
}
