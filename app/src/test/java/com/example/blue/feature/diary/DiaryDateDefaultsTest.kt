package com.example.blue.feature.diary

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryDateDefaultsTest {
    private val today = LocalDate.of(2026, 7, 12)

    @Test
    fun currentMonthDefaultsToToday() {
        assertEquals(today, defaultNewDiaryDate(YearMonth.of(2026, 7), today))
    }

    @Test
    fun otherMonthDefaultsToFirstDay() {
        assertEquals(
            LocalDate.of(2026, 1, 1),
            defaultNewDiaryDate(YearMonth.of(2026, 1), today),
        )
    }

    @Test
    fun currentYearShowsElapsedMonthsNewestFirst() {
        assertEquals(
            listOf(7, 6, 5, 4, 3, 2, 1),
            diaryMonthsForYear(year = 2026, today = today),
        )
    }

    @Test
    fun pastYearShowsEveryMonthOldestFirst() {
        assertEquals(
            (1..12).toList(),
            diaryMonthsForYear(year = 2025, today = today),
        )
    }
}
