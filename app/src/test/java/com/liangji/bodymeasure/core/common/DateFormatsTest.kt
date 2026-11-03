package com.liangji.bodymeasure.core.common

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatsTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun header_formats_month_day_and_weekday() {
        assertEquals("10月29日 · 周四", DateFormats.header(LocalDate.of(2026, 10, 29)))
    }

    @Test
    fun header_keeps_single_digit_month_and_day() {
        assertEquals("1月5日 · 周一", DateFormats.header(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun month_title_includes_year() {
        assertEquals("2026年10月", DateFormats.monthTitle(YearMonth.of(2026, 10)))
        assertEquals("2027年1月", DateFormats.monthTitle(YearMonth.of(2027, 1)))
    }

    @Test
    fun display_date_omits_current_year() {
        assertEquals("10月29日", DateFormats.displayDate(today, today))
        assertEquals("2025年12月31日", DateFormats.displayDate(LocalDate.of(2025, 12, 31), today))
    }

    @Test
    fun relative_day_covers_recent_dates() {
        assertEquals("今天", DateFormats.relativeDay(today, today))
        assertEquals("昨天", DateFormats.relativeDay(today.minusDays(1), today))
        assertEquals("3 天前", DateFormats.relativeDay(today.minusDays(3), today))
        assertEquals("45 天前", DateFormats.relativeDay(today.minusDays(45), today))
    }

    @Test
    fun relative_day_falls_back_for_future_dates() {
        assertEquals("10月30日", DateFormats.relativeDay(today.plusDays(1), today))
    }
}
