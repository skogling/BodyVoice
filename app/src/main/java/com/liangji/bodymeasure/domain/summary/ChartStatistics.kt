package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.domain.model.Measurement
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

enum class ChartRange(val label: String, val days: Int?) {
    ONE_MONTH("1月", 30),
    THREE_MONTHS("3月", 90),
    SIX_MONTHS("6月", 180),
    ONE_YEAR("1年", 365),
    ALL("全部", null),
}

data class DailyValue(val date: LocalDate, val value: Double)

data class TrendPoint(val date: LocalDate, val value: Double)

data class TrendChart(
    val points: List<TrendPoint>,
    val averagePoints: List<TrendPoint>,
    val minPoint: TrendPoint?,
    val maxPoint: TrendPoint?,
    val meanValue: Double?,
    val latestValue: Double?,
    val firstValue: Double?,
    val count: Int,
) {
    val change: Double? = if (latestValue != null && firstValue != null) {
        latestValue - firstValue
    } else {
        null
    }

    val isEmpty: Boolean = points.isEmpty()
}

data class DistributionBin(
    val lower: Double,
    val upper: Double,
    val count: Int,
)

data class DistributionChart(
    val bins: List<DistributionBin>,
    val mean: Double,
    val median: Double,
    val minimum: Double,
    val maximum: Double,
    val count: Int,
) {
    val binCount: Int get() = bins.size
}

data class CalendarCell(
    val date: LocalDate,
    val value: Double?,
)

data class CalendarMonth(
    val month: YearMonth,
    val weeks: List<List<CalendarCell?>>,
    val count: Int,
    val meanValue: Double?,
    val minimum: Double?,
    val maximum: Double?,
)

enum class ComparisonSpan(val label: String, val days: Int) {
    WEEK("7天", 7),
    MONTH("30天", 30),
    QUARTER("90天", 90),
}

data class PeriodStats(
    val start: LocalDate,
    val end: LocalDate,
    val average: Double?,
    val minimum: Double?,
    val maximum: Double?,
    val count: Int,
)

data class PeriodComparison(
    val current: PeriodStats,
    val previous: PeriodStats,
) {
    val averageChange: Double? = if (current.average != null && previous.average != null) {
        current.average - previous.average
    } else {
        null
    }

    val averageChangeRatio: Double? = averageChange?.let { change ->
        previous.average?.takeIf { it != 0.0 }?.let { change / abs(it) }
    }
}

object ChartStatistics {
    const val MAX_TREND_POINTS = 20

    fun dailyValues(measurements: List<Measurement>): List<DailyValue> =
        measurements
            .groupBy { it.date }
            .map { (date, items) ->
                val latest = items.maxWithOrNull(
                    compareBy<Measurement> { it.measuredAt }
                        .thenBy { it.createdAt }
                        .thenBy { it.id },
                )!!
                DailyValue(date = date, value = latest.value)
            }
            .sortedBy { it.date }

    fun withinRange(daily: List<DailyValue>, range: ChartRange, today: LocalDate): List<DailyValue> {
        val days = range.days ?: return daily.filter { !it.date.isAfter(today) }
        val start = today.minusDays(days - 1L)
        return daily.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
    }

    fun trend(
        daily: List<DailyValue>,
        range: ChartRange,
        today: LocalDate,
        maxPoints: Int = MAX_TREND_POINTS,
    ): TrendChart {
        val filtered = withinRange(daily, range, today)
        if (filtered.isEmpty()) {
            return TrendChart(
                points = emptyList(),
                averagePoints = emptyList(),
                minPoint = null,
                maxPoint = null,
                meanValue = null,
                latestValue = null,
                firstValue = null,
                count = 0,
            )
        }

        val rolling = rollingAverage(daily, filtered)
        val indices = lttbIndices(filtered, maxPoints.coerceAtLeast(3))
        val points = indices.map { TrendPoint(filtered[it].date, filtered[it].value) }
        val averagePoints = indices.map { TrendPoint(filtered[it].date, rolling[it]) }
        val minPoint = filtered.minByOrNull { it.value }?.let { TrendPoint(it.date, it.value) }
        val maxPoint = filtered.maxByOrNull { it.value }?.let { TrendPoint(it.date, it.value) }

        return TrendChart(
            points = points,
            averagePoints = averagePoints,
            minPoint = minPoint,
            maxPoint = maxPoint,
            meanValue = filtered.map { it.value }.average(),
            latestValue = filtered.last().value,
            firstValue = filtered.first().value,
            count = filtered.size,
        )
    }

    private fun rollingAverage(all: List<DailyValue>, filtered: List<DailyValue>): List<Double> {
        val byDate = all.associate { it.date to it.value }
        return filtered.map { point ->
            val start = point.date.minusDays(6)
            val window = mutableListOf<Double>()
            var day = start
            while (!day.isAfter(point.date)) {
                byDate[day]?.let { window.add(it) }
                day = day.plusDays(1)
            }
            window.average()
        }
    }

    fun distribution(
        daily: List<DailyValue>,
        targetBins: Int = 6,
    ): DistributionChart? {
        if (daily.isEmpty()) return null
        val values = daily.map { it.value }
        val minimum = values.min()
        val maximum = values.max()
        val bins = if (maximum - minimum < 1e-9) {
            listOf(DistributionBin(minimum, maximum, values.size))
        } else {
            buildBins(values, minimum, maximum, targetBins)
        }
        val sorted = values.sorted()
        val middle = sorted.size / 2
        val median = if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
        return DistributionChart(
            bins = bins,
            mean = values.average(),
            median = median,
            minimum = minimum,
            maximum = maximum,
            count = values.size,
        )
    }

    private fun buildBins(
        values: List<Double>,
        minimum: Double,
        maximum: Double,
        targetBins: Int,
    ): List<DistributionBin> {
        val target = targetBins.coerceIn(3, 12)
        var step = niceStep((maximum - minimum) / target)
        var start = floor(minimum / step) * step
        var slots = round((ceil(maximum / step) * step - start) / step).toInt().coerceAtLeast(1)
        while (slots > target + 2) {
            step *= 2.0
            start = floor(minimum / step) * step
            slots = round((ceil(maximum / step) * step - start) / step).toInt().coerceAtLeast(1)
        }
        val counts = IntArray(slots)
        values.forEach { value ->
            val index = floor((value - start) / step + 1e-9).toInt().coerceIn(0, slots - 1)
            counts[index]++
        }
        return (0 until slots).map { index ->
            DistributionBin(
                lower = start + step * index,
                upper = start + step * (index + 1),
                count = counts[index],
            )
        }
    }

    private fun niceStep(raw: Double): Double {
        if (!raw.isFinite() || raw <= 0.0) return 1.0
        val magnitude = 10.0.pow(floor(log10(raw)))
        val residual = raw / magnitude
        val factor = when {
            residual <= 1.0 -> 1.0
            residual <= 2.0 -> 2.0
            residual <= 2.5 -> 2.5
            residual <= 5.0 -> 5.0
            else -> 10.0
        }
        return factor * magnitude
    }

    fun calendar(daily: List<DailyValue>, month: YearMonth, today: LocalDate): CalendarMonth {
        val byDate = daily
            .filter { YearMonth.from(it.date) == month && !it.date.isAfter(today) }
            .associate { it.date to it.value }
        val cells = mutableListOf<CalendarCell?>()
        repeat(month.atDay(1).dayOfWeek.value - 1) { cells.add(null) }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            cells.add(CalendarCell(date = date, value = byDate[date]))
        }
        while (cells.size % 7 != 0) cells.add(null)
        while (cells.size < 42) cells.add(null)
        val values = byDate.values
        return CalendarMonth(
            month = month,
            weeks = cells.chunked(7),
            count = values.size,
            meanValue = values.takeIf { it.isNotEmpty() }?.average(),
            minimum = values.minOrNull(),
            maximum = values.maxOrNull(),
        )
    }

    fun comparison(daily: List<DailyValue>, today: LocalDate, span: ComparisonSpan): PeriodComparison {
        fun stats(start: LocalDate, end: LocalDate): PeriodStats {
            val values = daily
                .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
                .map { it.value }
            return PeriodStats(
                start = start,
                end = end,
                average = values.takeIf { it.isNotEmpty() }?.average(),
                minimum = values.minOrNull(),
                maximum = values.maxOrNull(),
                count = values.size,
            )
        }
        val currentStart = today.minusDays(span.days - 1L)
        val previousEnd = currentStart.minusDays(1)
        val previousStart = previousEnd.minusDays(span.days - 1L)
        return PeriodComparison(
            current = stats(currentStart, today),
            previous = stats(previousStart, previousEnd),
        )
    }

    internal fun lttbIndices(points: List<DailyValue>, threshold: Int): List<Int> {
        val count = points.size
        if (threshold >= count || threshold < 3) return points.indices.toList()
        val sampled = ArrayList<Int>(threshold)
        sampled.add(0)
        val bucketSize = (count - 2).toDouble() / (threshold - 2)
        var anchor = 0
        for (i in 0 until threshold - 2) {
            val rangeStart = (floor(i * bucketSize).toInt() + 1).coerceAtMost(count - 2)
            val rangeEnd = (floor((i + 1) * bucketSize).toInt() + 1).coerceAtMost(count - 1)
            val nextStart = (floor((i + 1) * bucketSize).toInt() + 1).coerceAtMost(count - 1)
            val nextEnd = (floor((i + 2) * bucketSize).toInt() + 1).coerceAtMost(count)
            var avgX = 0.0
            var avgY = 0.0
            var avgCount = 0
            for (j in nextStart until nextEnd) {
                avgX += points[j].date.toEpochDay().toDouble()
                avgY += points[j].value
                avgCount++
            }
            if (avgCount == 0) {
                avgX = points.last().date.toEpochDay().toDouble()
                avgY = points.last().value
            } else {
                avgX /= avgCount
                avgY /= avgCount
            }
            val ax = points[anchor].date.toEpochDay().toDouble()
            val ay = points[anchor].value
            var maxArea = -1.0
            var chosen = rangeStart
            for (j in rangeStart until rangeEnd) {
                val px = points[j].date.toEpochDay().toDouble()
                val py = points[j].value
                val area = abs((ax - avgX) * (py - ay) - (ax - px) * (avgY - ay))
                if (area > maxArea) {
                    maxArea = area
                    chosen = j
                }
            }
            sampled.add(chosen)
            anchor = chosen
        }
        sampled.add(count - 1)
        return sampled.distinct().sorted()
    }
}
