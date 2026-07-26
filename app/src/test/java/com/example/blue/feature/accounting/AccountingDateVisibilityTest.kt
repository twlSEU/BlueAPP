package com.example.blue.feature.accounting

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingDateVisibilityTest {
    private val today = LocalDate.of(2026, 7, 13)

    @Test
    fun currentYearShowsElapsedMonthsNewestFirst() {
        assertEquals(
            listOf(7, 6, 5, 4, 3, 2, 1),
            accountingMonthsForYear(year = 2026, today = today),
        )
    }

    @Test
    fun pastYearShowsEveryMonthOldestFirst() {
        assertEquals(
            (1..12).toList(),
            accountingMonthsForYear(year = 2025, today = today),
        )
    }

    @Test
    fun currentMonthShowsElapsedDaysNewestFirst() {
        assertEquals(
            (13 downTo 1).toList(),
            accountingDaysForMonth(YearMonth.of(2026, 7), today),
        )
    }

    @Test
    fun pastMonthShowsEveryDayOldestFirst() {
        assertEquals(
            (1..31).toList(),
            accountingDaysForMonth(YearMonth.of(2026, 5), today),
        )
    }

    @Test
    fun pastFebruaryUsesItsActualLength() {
        assertEquals(
            (1..28).toList(),
            accountingDaysForMonth(YearMonth.of(2025, 2), today),
        )
    }

    @Test
    fun newEntryUsesTheDayOpenedFromMonthScreen() {
        val selectedDate = LocalDate.of(2026, 5, 18)

        assertEquals(selectedDate, defaultAccountEntryDate(selectedDate, today))
    }

    @Test
    fun editingAmountDoesNotUseGroupingSeparators() {
        assertEquals("1234.56", accountAmountForEditing(123_456L))
    }
}
