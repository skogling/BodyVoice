package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.testing.measured
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementHistoryTest {
    @Test
    fun empty_input_yields_no_sections() {
        assertTrue(MeasurementHistory.groupByMonthDescending(emptyList()).isEmpty())
    }

    @Test
    fun groups_months_newest_first_and_days_descending() {
        val measurements = listOf(
            measured("w", LocalDate.of(2026, 9, 30), 80.0),
            measured("w", LocalDate.of(2026, 10, 2), 79.5),
            measured("w", LocalDate.of(2026, 10, 29), 78.5),
            measured("w", LocalDate.of(2025, 12, 31), 82.0),
        )

        val sections = MeasurementHistory.groupByMonthDescending(measurements)

        assertEquals(
            listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9), YearMonth.of(2025, 12)),
            sections.map { it.month },
        )
        assertEquals("2026年10月", sections[0].title)
        assertEquals(listOf(78.5, 79.5), sections[0].items.map { it.value })
        assertEquals(listOf(80.0), sections[1].items.map { it.value })
    }
}
