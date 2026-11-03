package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.domain.model.Measurement
import kotlin.math.abs

object AnomalyDetector {
    const val BIG_JUMP_RATIO = 0.2
    const val MEDIAN_MULTIPLE = 10.0
    const val HISTORY_WINDOW = 30

    data class Warning(
        val previous: Double,
        val median: Double,
        val changeRatio: Double?,
        val multipleOfMedian: Double?,
    ) {
        val isBigJump: Boolean = changeRatio != null && abs(changeRatio) > BIG_JUMP_RATIO
        val isFarFromMedian: Boolean = multipleOfMedian != null &&
            (multipleOfMedian > MEDIAN_MULTIPLE || multipleOfMedian < 1 / MEDIAN_MULTIPLE)
    }

    fun check(value: Double, history: List<Measurement>): Warning? {
        if (!value.isFinite() || history.isEmpty()) return null
        val recent = history.sortedByDescending { it.date }.take(HISTORY_WINDOW)
        val previous = recent.first().value
        val median = medianOf(recent.map { it.value })
        val changeRatio = if (previous != 0.0) (value - previous) / abs(previous) else null
        val multipleOfMedian = if (median > 0.0) value / median else null

        val warning = Warning(
            previous = previous,
            median = median,
            changeRatio = changeRatio,
            multipleOfMedian = multipleOfMedian,
        )
        return warning.takeIf { it.isBigJump || it.isFarFromMedian }
    }

    private fun medianOf(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}
