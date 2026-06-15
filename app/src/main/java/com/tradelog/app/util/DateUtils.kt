package com.tradelog.app.util

import com.tradelog.app.data.entity.GoalType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val niceDayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dayTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

    fun today(): LocalDate = LocalDate.now(zone)

    fun todayKey(): String = today().format(dayFmt)

    fun dateKey(date: LocalDate): String = date.format(dayFmt)

    /** Epoch-millis bounds [startInclusive, endInclusive] of a local date. */
    fun dayEpochBounds(date: LocalDate = today()): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Monday..Sunday of the week containing [date]. */
    fun weekDates(date: LocalDate = today()): Pair<LocalDate, LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday to monday.plusDays(6)
    }

    fun weekEpochBounds(date: LocalDate = today()): Pair<Long, Long> {
        val (mon, sun) = weekDates(date)
        val start = mon.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = sun.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Period key for resets: daily -> yyyy-MM-dd, weekly -> yyyy-Www. */
    fun periodKey(type: GoalType, date: LocalDate = today()): String = when (type) {
        GoalType.DAILY -> date.format(dayFmt)
        GoalType.WEEKLY -> "${date.get(IsoFields.WEEK_BASED_YEAR)}-W${
            date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
        }"
    }

    /** String-date range (yyyy-MM-dd) for the current period; used for date-string columns. */
    fun periodDateRange(type: GoalType, date: LocalDate = today()): Pair<String, String> = when (type) {
        GoalType.DAILY -> date.format(dayFmt) to date.format(dayFmt)
        GoalType.WEEKLY -> {
            val (mon, sun) = weekDates(date)
            mon.format(dayFmt) to sun.format(dayFmt)
        }
    }

    /** Epoch-millis range for the current period; used for openedAt columns. */
    fun periodEpochRange(type: GoalType, date: LocalDate = today()): Pair<Long, Long> = when (type) {
        GoalType.DAILY -> dayEpochBounds(date)
        GoalType.WEEKLY -> weekEpochBounds(date)
    }

    fun formatNiceDate(date: LocalDate): String = date.format(niceDayFmt)

    fun formatEpochDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(niceDayFmt)

    fun formatEpochTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFmt)

    fun formatEpochDateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dayTimeFmt)

    fun isSameLocalDay(epochMillis: Long, date: LocalDate = today()): Boolean =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate() == date

    /** Whole days from today until the given date (negative if in the past). */
    fun daysUntil(epochMillis: Long): Int {
        val target = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(today(), target).toInt()
    }
}
