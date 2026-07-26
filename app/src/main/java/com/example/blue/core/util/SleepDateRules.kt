package com.example.blue.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * The app's single source of truth for assigning sleep across midnight.
 * Times before 06:00 belong to the previous living day.
 */
object SleepDateRules {
    val dayBoundary: LocalTime = LocalTime.of(6, 0)
    private val quickRecordMorningBoundary: LocalTime = LocalTime.NOON
    private val continuousAxisStart: LocalTime = LocalTime.of(18, 0)

    fun recordDateFor(sleepDateTime: LocalDateTime): LocalDate =
        if (sleepDateTime.toLocalTime().isBefore(dayBoundary)) {
            sleepDateTime.toLocalDate().minusDays(1)
        } else {
            sleepDateTime.toLocalDate()
        }

    /** Opens the previous night's record during the morning, and today's record later in the day. */
    fun defaultQuickRecordDate(now: LocalDateTime = LocalDateTime.now()): LocalDate =
        if (now.toLocalTime().isBefore(quickRecordMorningBoundary)) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }

    fun sleepDateTimeFor(recordDate: LocalDate, sleepTime: LocalTime): LocalDateTime =
        LocalDateTime.of(
            if (sleepTime.isBefore(dayBoundary)) recordDate.plusDays(1) else recordDate,
            sleepTime,
        )

    fun wakeDateTimeFor(
        recordDate: LocalDate,
        sleepDateTime: LocalDateTime,
        wakeTime: LocalTime,
    ): LocalDateTime {
        var candidate = LocalDateTime.of(recordDate, wakeTime)
        while (!candidate.isAfter(sleepDateTime)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /** Maps bedtime onto a continuous 18:00 -> following-day axis. */
    fun continuousMinutes(time: LocalTime): Int {
        val minutes = time.hour * 60 + time.minute
        val axisStart = continuousAxisStart.hour * 60 + continuousAxisStart.minute
        return if (minutes < axisStart) minutes + MINUTES_PER_DAY else minutes
    }

    fun averageBedtime(times: Iterable<LocalTime>): LocalTime? {
        val values = times.map(::continuousMinutes)
        if (values.isEmpty()) return null
        val mean = (values.sumOf(Int::toLong).toDouble() / values.size).roundToInt()
        return LocalTime.ofSecondOfDay(((mean % MINUTES_PER_DAY) * 60).toLong())
    }

    fun latenessLevel(time: LocalTime): Int {
        val value = continuousMinutes(time)
        return when {
            value < 23 * 60 -> 0
            value < MINUTES_PER_DAY -> 1
            value < MINUTES_PER_DAY + 60 -> 2
            value < MINUTES_PER_DAY + 2 * 60 -> 3
            value < MINUTES_PER_DAY + 3 * 60 -> 4
            else -> 5
        }
    }

    fun isLateNight(time: LocalTime): Boolean = latenessLevel(time) >= 2

    private const val MINUTES_PER_DAY = 24 * 60
}
