package com.example.blue.data.repository

import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

enum class AccountBrowseSortField {
    TIME,
    AMOUNT,
}

data class AccountBrowseFilter(
    val startDate: LocalDate? = null,
    val endDateExclusive: LocalDate? = null,
    val categoryId: String? = null,
    val type: AccountType? = null,
    val keyword: String = "",
)

data class AccountBrowseSort(
    val field: AccountBrowseSortField = AccountBrowseSortField.TIME,
    val ascending: Boolean = false,
)

data class AccountPeriodSummary(
    val incomeInCents: Long = 0L,
    val expenseInCents: Long = 0L,
    val entryCount: Int = 0,
    val recordDays: Int = 0,
    val largestExpenseInCents: Long = 0L,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
}

data class AccountMonthlyAggregate(
    val month: Int,
    val incomeInCents: Long,
    val expenseInCents: Long,
    val entryCount: Int,
    val recordDays: Int,
) {
    val balanceInCents: Long get() = incomeInCents - expenseInCents
}

data class AccountCategoryAggregate(
    val categoryId: String,
    val categoryName: String,
    val type: AccountType,
    val totalInCents: Long,
    val entryCount: Int,
)

interface AccountRepository {
    fun observeYear(year: Int): Flow<List<AccountEntryWithCategory>>

    fun observeMonth(yearMonth: YearMonth): Flow<List<AccountEntryWithCategory>>

    fun observeEntry(id: String): Flow<AccountEntryWithCategory?>

    fun observeEntriesForDate(date: LocalDate): Flow<List<AccountEntryWithCategory>>

    suspend fun countEntries(filter: AccountBrowseFilter): Int

    suspend fun loadEntryPage(
        filter: AccountBrowseFilter,
        sort: AccountBrowseSort,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory>

    fun observeYearSummary(year: Int): Flow<AccountPeriodSummary>

    fun observeMonthSummary(yearMonth: YearMonth): Flow<AccountPeriodSummary>

    fun observeMonthlyAggregates(year: Int): Flow<List<AccountMonthlyAggregate>>

    fun observeCategoryAggregates(year: Int): Flow<List<AccountCategoryAggregate>>

    fun observeCategories(
        type: AccountType,
        includeInactive: Boolean = false,
    ): Flow<List<AccountCategoryEntity>>

    suspend fun getAllCategories(): List<AccountCategoryEntity>

    suspend fun saveEntry(entry: AccountEntryEntity)

    suspend fun deleteEntry(id: String)

    suspend fun saveCategory(category: AccountCategoryEntity)

    suspend fun setCustomCategoryActive(
        id: String,
        isActive: Boolean,
    )

    suspend fun ensureDefaultCategories()
}
