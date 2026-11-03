package com.liangji.bodymeasure.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateFormats {
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun timeOfDay(epochMillis: Long): String? {
        if (epochMillis <= 0L) return null
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(TIME)
    }

    fun stamp(date: LocalDate, measuredAt: Long, today: LocalDate): String {
        val day = if (date == today) "今天" else displayDate(date, today)
        val time = if (date == today) timeOfDay(measuredAt) else null
        return if (time == null) day else "$day $time"
    }
    fun header(date: LocalDate): String =
        "${shortDate(date)} · ${weekday(date)}"

    fun shortDate(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

    fun fullDate(date: LocalDate): String =
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    fun displayDate(date: LocalDate, today: LocalDate): String =
        if (date.year == today.year) shortDate(date) else fullDate(date)

    fun monthTitle(month: YearMonth): String = "${month.year}年${month.monthValue}月"

    fun relativeDay(date: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(date, today)
        return when {
            days == 0L -> "今天"
            days == 1L -> "昨天"
            days > 1L -> "$days 天前"
            else -> shortDate(date)
        }
    }

    fun weekday(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
