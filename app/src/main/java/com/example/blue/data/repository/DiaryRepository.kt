package com.example.blue.data.repository

import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryWithImages
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

enum class DiaryBrowseOrder {
    ASCENDING,
    DESCENDING,
}

data class DiaryBrowseFilter(
    val startDate: LocalDate? = null,
    val endDateExclusive: LocalDate? = null,
)

data class DiaryPeriodSummary(
    val recordDays: Int = 0,
    val diaryCount: Int = 0,
    val totalCharacterCount: Long = 0L,
    val averageCharacterCount: Double = 0.0,
    val longestCharacterCount: Int = 0,
)

data class DiaryMonthAggregate(
    val month: Int,
    val recordDays: Int,
    val diaryCount: Int,
    val totalCharacterCount: Long,
    val lastDiaryDate: LocalDate?,
    val thumbnailPath: String?,
)

data class DiaryMoodAggregate(
    val mood: Int,
    val count: Int,
)

data class DiaryMonthlyMoodAggregate(
    val month: Int,
    val mood: Int,
    val count: Int,
)

data class DiaryContentBatchItem(
    val id: String,
    val diaryDate: LocalDate,
    val content: String,
)

interface DiaryRepository {
    fun observeYear(year: Int): Flow<List<DiaryWithImages>>

    fun observeMonth(yearMonth: YearMonth): Flow<List<DiaryWithImages>>

    fun observeDiary(id: String): Flow<DiaryWithImages?>

    suspend fun findLatestDiaryOnDate(date: LocalDate): DiaryWithImages?

    suspend fun countDiaries(filter: DiaryBrowseFilter = DiaryBrowseFilter()): Int

    suspend fun loadDiaryPage(
        filter: DiaryBrowseFilter = DiaryBrowseFilter(),
        order: DiaryBrowseOrder = DiaryBrowseOrder.DESCENDING,
        limit: Int,
        offset: Int,
    ): List<DiaryWithImages>

    fun observeYearSummary(year: Int): Flow<DiaryPeriodSummary>

    fun observeMonthSummary(yearMonth: YearMonth): Flow<DiaryPeriodSummary>

    fun observeMonthAggregates(year: Int): Flow<List<DiaryMonthAggregate>>

    fun observeYearMoodCounts(year: Int): Flow<List<DiaryMoodAggregate>>

    fun observeMonthMoodCounts(yearMonth: YearMonth): Flow<List<DiaryMoodAggregate>>

    fun observeMonthlyMoodCounts(year: Int): Flow<List<DiaryMonthlyMoodAggregate>>

    suspend fun getDistinctDiaryDates(year: Int): List<LocalDate>

    suspend fun loadDiaryContentBatch(
        year: Int,
        limit: Int,
        offset: Int,
    ): List<DiaryContentBatchItem>

    suspend fun saveDiary(
        diary: DiaryEntity,
        images: List<DiaryImageEntity>,
        moods: Set<Int>,
    )

    suspend fun deleteDiary(id: String): List<String>
}
