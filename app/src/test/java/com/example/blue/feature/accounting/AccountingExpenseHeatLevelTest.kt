package com.example.blue.feature.accounting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountingExpenseHeatLevelTest {
    @Test
    fun noExpenseHasNoHeatColor() {
        assertNull(accountingExpenseHeatLevel(0L))
    }

    @Test
    fun thresholdsAreInclusiveAndNextCentMovesToNextBand() {
        assertEquals(0, accountingExpenseHeatLevel(1L))
        listOf(2_000L, 3_000L, 4_000L, 5_000L, 10_000L).forEachIndexed { level, cents ->
            assertEquals(level, accountingExpenseHeatLevel(cents))
            assertEquals(level + 1, accountingExpenseHeatLevel(cents + 1L))
        }
        assertEquals(5, accountingExpenseHeatLevel(Long.MAX_VALUE))
    }
}
