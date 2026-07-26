package com.example.blue.feature.time

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LifeTraceCalculationTest {
    @Test
    fun birthdayCalculatesCompletedYearsAndElapsedDays() {
        val birthday = LocalDate.of(2000, 1, 1)
        val today = LocalDate.of(2026, 1, 2)

        assertEquals(26, lifeAge(birthday, today))
        assertEquals(9_498L, lifeElapsedDays(birthday, today))
    }

    @Test
    fun lifeGridCapsAtEightyYears() {
        assertEquals(
            80,
            lifeAge(LocalDate.of(1900, 1, 1), LocalDate.of(2026, 1, 1)),
        )
    }

    @Test
    fun progressUsesElapsedDaysAndKeepsOneDecimal() {
        val birthday = LocalDate.of(2000, 1, 1)
        val today = LocalDate.of(2020, 1, 1)

        assertEquals(
            "25.0",
            lifeProgressPercent(birthday, lifeElapsedDays(birthday, today)),
        )
        assertEquals("0.0", lifeProgressPercent(null, 0L))
    }
}
