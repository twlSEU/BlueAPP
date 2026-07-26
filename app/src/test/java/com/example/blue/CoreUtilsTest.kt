package com.example.blue

import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.model.AccountType
import com.example.blue.model.toAccountSummary
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreUtilsTest {
    @Test
    fun amountTextConvertsToCentsPrecisely() {
        assertEquals(1234L, AmountUtils.parseToCents("12.34"))
        assertEquals(100L, AmountUtils.parseToCents("1"))
        assertNull(AmountUtils.parseToCents("1.234"))
        assertNull(AmountUtils.parseToCents("-1"))
    }

    @Test
    fun accountSummarySeparatesIncomeAndExpense() {
        val entries = listOf(entry(AccountType.INCOME, 5000), entry(AccountType.EXPENSE, 1250))
        val summary = entries.toAccountSummary()
        assertEquals(5000L, summary.incomeInCents)
        assertEquals(1250L, summary.expenseInCents)
        assertEquals(3750L, summary.balanceInCents)
    }

    @Test
    fun leapYearFebruaryHasCorrectNumberOfDays() {
        assertEquals(29, YearMonth.of(2024, 2).lengthOfMonth())
        assertEquals(28, YearMonth.of(2025, 2).lengthOfMonth())
    }

    private fun entry(type: AccountType, cents: Long) = AccountEntryEntity(
        id = "$type-$cents",
        entryDate = LocalDate.of(2026, 7, 1),
        entryTime = LocalTime.NOON,
        type = type,
        amountInCents = cents,
        name = "测试",
        categoryId = "category",
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
