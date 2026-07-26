package com.example.blue.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepDateRulesTest {
    private val july15 = LocalDate.of(2026, 7, 15)

    @Test
    fun earlyMorningSleepBelongsToPreviousLivingDay() {
        assertEquals(
            july15,
            SleepDateRules.recordDateFor(LocalDateTime.of(2026, 7, 16, 1, 20)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 16),
            SleepDateRules.recordDateFor(LocalDateTime.of(2026, 7, 16, 6, 0)),
        )
    }

    @Test
    fun quickRecordOpensLastNightDuringMorning() {
        assertEquals(
            july15,
            SleepDateRules.defaultQuickRecordDate(LocalDateTime.of(2026, 7, 16, 9, 30)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 16),
            SleepDateRules.defaultQuickRecordDate(LocalDateTime.of(2026, 7, 16, 14, 0)),
        )
    }

    @Test
    fun recordDateAndTimeCreateTheExpectedDateTime() {
        assertEquals(
            LocalDateTime.of(2026, 7, 16, 0, 35),
            SleepDateRules.sleepDateTimeFor(july15, LocalTime.of(0, 35)),
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 15, 23, 35),
            SleepDateRules.sleepDateTimeFor(july15, LocalTime.of(23, 35)),
        )
    }

    @Test
    fun averageBedtimeUsesContinuousMidnightAxis() {
        assertEquals(
            LocalTime.of(0, 23),
            SleepDateRules.averageBedtime(
                listOf(LocalTime.of(23, 50), LocalTime.of(0, 20), LocalTime.of(1, 0)),
            ),
        )
    }

    @Test
    fun latenessThresholdsAreCentralized() {
        assertEquals(0, SleepDateRules.latenessLevel(LocalTime.of(22, 59)))
        assertEquals(1, SleepDateRules.latenessLevel(LocalTime.of(23, 42)))
        assertEquals(2, SleepDateRules.latenessLevel(LocalTime.of(0, 35)))
        assertEquals(5, SleepDateRules.latenessLevel(LocalTime.of(3, 0)))
        assertFalse(SleepDateRules.isLateNight(LocalTime.of(23, 59)))
        assertTrue(SleepDateRules.isLateNight(LocalTime.of(0, 0)))
    }
}
