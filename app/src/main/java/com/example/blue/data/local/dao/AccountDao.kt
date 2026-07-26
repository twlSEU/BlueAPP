package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.model.AccountType
import com.example.blue.data.repository.AccountCategoryAggregate
import com.example.blue.data.repository.AccountMonthlyAggregate
import com.example.blue.data.repository.AccountPeriodSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE entryDate >= :startDate AND entryDate < :endDateExclusive
        ORDER BY entryDate DESC, entryTime DESC
        """,
    )
    fun observeEntries(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<AccountEntryWithCategory>>

    @Transaction
    @Query("SELECT * FROM account_entries WHERE id = :id")
    fun observeEntry(id: String): Flow<AccountEntryWithCategory?>

    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE entryDate = :date
        ORDER BY entryTime DESC, id DESC
        """,
    )
    fun observeEntriesForDate(date: LocalDate): Flow<List<AccountEntryWithCategory>>

    @Query(
        """
        SELECT COUNT(*) FROM account_entries
        WHERE (:startDate IS NULL OR entryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR entryDate < :endDateExclusive)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:keyword = '' OR INSTR(COALESCE(note, ''), :keyword) > 0)
        """,
    )
    suspend fun countEntries(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        categoryId: String?,
        type: AccountType?,
        keyword: String,
    ): Int

    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE (:startDate IS NULL OR entryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR entryDate < :endDateExclusive)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:keyword = '' OR INSTR(COALESCE(note, ''), :keyword) > 0)
        ORDER BY entryDate, entryTime, id
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getEntryPageByTimeAscending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        categoryId: String?,
        type: AccountType?,
        keyword: String,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory>

    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE (:startDate IS NULL OR entryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR entryDate < :endDateExclusive)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:keyword = '' OR INSTR(COALESCE(note, ''), :keyword) > 0)
        ORDER BY entryDate DESC, entryTime DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getEntryPageByTimeDescending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        categoryId: String?,
        type: AccountType?,
        keyword: String,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory>

    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE (:startDate IS NULL OR entryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR entryDate < :endDateExclusive)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:keyword = '' OR INSTR(COALESCE(note, ''), :keyword) > 0)
        ORDER BY amountInCents, entryDate, entryTime, id
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getEntryPageByAmountAscending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        categoryId: String?,
        type: AccountType?,
        keyword: String,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory>

    @Transaction
    @Query(
        """
        SELECT * FROM account_entries
        WHERE (:startDate IS NULL OR entryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR entryDate < :endDateExclusive)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:keyword = '' OR INSTR(COALESCE(note, ''), :keyword) > 0)
        ORDER BY amountInCents DESC, entryDate DESC, entryTime DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getEntryPageByAmountDescending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        categoryId: String?,
        type: AccountType?,
        keyword: String,
        limit: Int,
        offset: Int,
    ): List<AccountEntryWithCategory>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountInCents ELSE 0 END), 0) AS incomeInCents,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountInCents ELSE 0 END), 0) AS expenseInCents,
            COUNT(*) AS entryCount,
            COUNT(DISTINCT entryDate) AS recordDays,
            COALESCE(MAX(CASE WHEN type = 'EXPENSE' THEN amountInCents ELSE 0 END), 0) AS largestExpenseInCents
        FROM account_entries
        WHERE entryDate >= :startDate AND entryDate < :endDateExclusive
        """,
    )
    fun observePeriodSummary(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<AccountPeriodSummary>

    @Query(
        """
        SELECT
            CAST(SUBSTR(entryDate, 6, 2) AS INTEGER) AS month,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountInCents ELSE 0 END), 0) AS incomeInCents,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountInCents ELSE 0 END), 0) AS expenseInCents,
            COUNT(*) AS entryCount,
            COUNT(DISTINCT entryDate) AS recordDays
        FROM account_entries
        WHERE entryDate >= :startDate AND entryDate < :endDateExclusive
        GROUP BY SUBSTR(entryDate, 6, 2)
        ORDER BY month
        """,
    )
    fun observeMonthlyAggregates(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<AccountMonthlyAggregate>>

    @Query(
        """
        SELECT
            category.id AS categoryId,
            category.name AS categoryName,
            entry.type AS type,
            COALESCE(SUM(entry.amountInCents), 0) AS totalInCents,
            COUNT(*) AS entryCount
        FROM account_entries AS entry
        JOIN account_categories AS category ON category.id = entry.categoryId
        WHERE entry.entryDate >= :startDate AND entry.entryDate < :endDateExclusive
        GROUP BY entry.categoryId, entry.type
        ORDER BY entry.type, totalInCents DESC, category.name
        """,
    )
    fun observeCategoryAggregates(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<AccountCategoryAggregate>>

    @Query(
        """
        SELECT * FROM account_categories
        WHERE type = :type AND (:includeInactive OR isActive = 1)
        ORDER BY isDefault DESC, createdAt, name
        """,
    )
    fun observeCategories(
        type: AccountType,
        includeInactive: Boolean = false,
    ): Flow<List<AccountCategoryEntity>>

    @Upsert
    suspend fun upsertEntry(entry: AccountEntryEntity)

    @Upsert
    suspend fun upsertCategory(category: AccountCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<AccountCategoryEntity>)

    @Query("UPDATE account_categories SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id AND isDefault = 0")
    suspend fun setCustomCategoryActive(
        id: String,
        isActive: Boolean,
        updatedAt: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM account_entries WHERE categoryId = :categoryId")
    suspend fun countCategoryUsage(categoryId: String): Int

    @Query("DELETE FROM account_entries WHERE id = :id")
    suspend fun deleteEntry(id: String): Int

    @Query("SELECT * FROM account_entries ORDER BY entryDate, entryTime")
    suspend fun getAllEntries(): List<AccountEntryEntity>

    @Query("SELECT * FROM account_categories ORDER BY type, createdAt, name")
    suspend fun getAllCategories(): List<AccountCategoryEntity>

    @Query("DELETE FROM account_entries")
    suspend fun clearAllEntries()

    @Query("DELETE FROM account_categories")
    suspend fun clearAllCategories()
}
