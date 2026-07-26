package com.example.blue.data.repository

import com.example.blue.data.local.dao.AccountDao
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

class OfflineAccountRepository(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeYear(year: Int): Flow<List<AccountEntryWithCategory>> =
        accountDao.observeEntries(
            startDate = LocalDate.of(year, 1, 1),
            endDateExclusive = LocalDate.of(year + 1, 1, 1),
        )

    override fun observeMonth(yearMonth: YearMonth): Flow<List<AccountEntryWithCategory>> =
        accountDao.observeEntries(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeEntry(id: String): Flow<AccountEntryWithCategory?> =
        accountDao.observeEntry(id)

    override fun observeEntriesForDate(date: LocalDate): Flow<List<AccountEntryWithCategory>> =
        accountDao.observeEntriesForDate(date)

    override suspend fun countEntries(filter: AccountBrowseFilter): Int {
        validateFilter(filter)
        return accountDao.countEntries(
            startDate = filter.startDate,
            endDateExclusive = filter.endDateExclusive,
            categoryId = filter.categoryId,
            type = filter.type,
            keyword = filter.keyword.trim(),
        )
    }

    override suspend fun loadEntryPage(
        filter: AccountBrowseFilter,
        sort: AccountBrowseSort,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory> {
        validateFilter(filter)
        validatePage(limit, offset)
        val keyword = filter.keyword.trim()
        return when (sort.field) {
            AccountBrowseSortField.TIME -> if (sort.ascending) {
                accountDao.getEntryPageByTimeAscending(
                    filter.startDate,
                    filter.endDateExclusive,
                    filter.categoryId,
                    filter.type,
                    keyword,
                    limit,
                    offset,
                )
            } else {
                accountDao.getEntryPageByTimeDescending(
                    filter.startDate,
                    filter.endDateExclusive,
                    filter.categoryId,
                    filter.type,
                    keyword,
                    limit,
                    offset,
                )
            }
            AccountBrowseSortField.AMOUNT -> if (sort.ascending) {
                accountDao.getEntryPageByAmountAscending(
                    filter.startDate,
                    filter.endDateExclusive,
                    filter.categoryId,
                    filter.type,
                    keyword,
                    limit,
                    offset,
                )
            } else {
                accountDao.getEntryPageByAmountDescending(
                    filter.startDate,
                    filter.endDateExclusive,
                    filter.categoryId,
                    filter.type,
                    keyword,
                    limit,
                    offset,
                )
            }
        }
    }

    override fun observeYearSummary(year: Int): Flow<AccountPeriodSummary> {
        val (start, end) = yearRange(year)
        return accountDao.observePeriodSummary(start, end)
    }

    override fun observeMonthSummary(yearMonth: YearMonth): Flow<AccountPeriodSummary> =
        accountDao.observePeriodSummary(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeMonthlyAggregates(year: Int): Flow<List<AccountMonthlyAggregate>> {
        val (start, end) = yearRange(year)
        return accountDao.observeMonthlyAggregates(start, end)
    }

    override fun observeCategoryAggregates(year: Int): Flow<List<AccountCategoryAggregate>> {
        val (start, end) = yearRange(year)
        return accountDao.observeCategoryAggregates(start, end)
    }

    override fun observeCategories(
        type: AccountType,
        includeInactive: Boolean,
    ): Flow<List<AccountCategoryEntity>> = accountDao.observeCategories(type, includeInactive)

    override suspend fun getAllCategories(): List<AccountCategoryEntity> =
        accountDao.getAllCategories()

    override suspend fun saveEntry(entry: AccountEntryEntity) {
        require(entry.amountInCents > 0L) { "金额必须大于 0" }
        require(entry.name.isNotBlank()) { "账目名称不能为空" }
        accountDao.upsertEntry(entry)
    }

    override suspend fun deleteEntry(id: String) {
        accountDao.deleteEntry(id)
    }

    override suspend fun saveCategory(category: AccountCategoryEntity) {
        require(category.name.isNotBlank()) { "分类名称不能为空" }
        accountDao.upsertCategory(category)
    }

    override suspend fun setCustomCategoryActive(
        id: String,
        isActive: Boolean,
    ) {
        accountDao.setCustomCategoryActive(
            id = id,
            isActive = isActive,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun ensureDefaultCategories() {
        val timestamp = System.currentTimeMillis()
        accountDao.insertCategories(defaultCategories(timestamp))
    }

    private fun validateFilter(filter: AccountBrowseFilter) {
        require(
            filter.startDate == null ||
                filter.endDateExclusive == null ||
                filter.startDate < filter.endDateExclusive,
        ) { "结束日期必须晚于开始日期" }
    }

    private fun validatePage(limit: Int, offset: Int) {
        require(limit in 1..MAX_PAGE_SIZE) { "每页数量必须在 1 到 $MAX_PAGE_SIZE 之间" }
        require(offset >= 0) { "分页偏移不能为负数" }
    }

    private fun yearRange(year: Int): Pair<LocalDate, LocalDate> =
        LocalDate.of(year, 1, 1) to LocalDate.of(year + 1, 1, 1)

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}

private fun defaultCategories(timestamp: Long): List<AccountCategoryEntity> {
    val expense = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "通讯", "人情", "其他")
    val income = listOf("工资", "奖金", "兼职", "理财", "报销", "礼金", "其他")

    return buildList {
        expense.forEachIndexed { index, name ->
            add(
                AccountCategoryEntity(
                    id = "default-expense-$index",
                    name = name,
                    type = AccountType.EXPENSE,
                    isDefault = true,
                    isActive = true,
                    createdAt = timestamp + index,
                    updatedAt = timestamp,
                ),
            )
        }
        income.forEachIndexed { index, name ->
            add(
                AccountCategoryEntity(
                    id = "default-income-$index",
                    name = name,
                    type = AccountType.INCOME,
                    isDefault = true,
                    isActive = true,
                    createdAt = timestamp + index,
                    updatedAt = timestamp,
                ),
            )
        }
    }
}
