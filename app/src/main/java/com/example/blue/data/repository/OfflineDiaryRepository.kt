package com.example.blue.data.repository

import androidx.room.withTransaction
import com.example.blue.core.database.AppDatabase
import com.example.blue.data.local.dao.DiaryDao
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryMoodEntity
import com.example.blue.data.local.entity.DiaryMoodIds
import com.example.blue.data.local.entity.DiaryWithImages
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

class OfflineDiaryRepository(
    private val database: AppDatabase,
    private val diaryDao: DiaryDao,
) : DiaryRepository {
    override fun observeYear(year: Int): Flow<List<DiaryWithImages>> =
        diaryDao.observeDiaries(
            startDate = LocalDate.of(year, 1, 1),
            endDateExclusive = LocalDate.of(year + 1, 1, 1),
        )

    override fun observeMonth(yearMonth: YearMonth): Flow<List<DiaryWithImages>> =
        diaryDao.observeDiaries(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeDiary(id: String): Flow<DiaryWithImages?> = diaryDao.observeDiary(id)

    override suspend fun findLatestDiaryOnDate(date: LocalDate): DiaryWithImages? =
        diaryDao.getLatestDiaryOnDate(date)

    override suspend fun countDiaries(filter: DiaryBrowseFilter): Int {
        validateFilter(filter)
        return diaryDao.countDiaries(filter.startDate, filter.endDateExclusive)
    }

    override suspend fun loadDiaryPage(
        filter: DiaryBrowseFilter,
        order: DiaryBrowseOrder,
        limit: Int,
        offset: Int,
    ): List<DiaryWithImages> {
        validateFilter(filter)
        validatePage(limit, offset)
        return when (order) {
            DiaryBrowseOrder.ASCENDING -> diaryDao.getDiaryPageAscending(
                startDate = filter.startDate,
                endDateExclusive = filter.endDateExclusive,
                limit = limit,
                offset = offset,
            )
            DiaryBrowseOrder.DESCENDING -> diaryDao.getDiaryPageDescending(
                startDate = filter.startDate,
                endDateExclusive = filter.endDateExclusive,
                limit = limit,
                offset = offset,
            )
        }
    }

    override fun observeYearSummary(year: Int): Flow<DiaryPeriodSummary> {
        val (start, end) = yearRange(year)
        return diaryDao.observePeriodSummary(start, end)
    }

    override fun observeMonthSummary(yearMonth: YearMonth): Flow<DiaryPeriodSummary> =
        diaryDao.observePeriodSummary(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeMonthAggregates(year: Int): Flow<List<DiaryMonthAggregate>> {
        val (start, end) = yearRange(year)
        return diaryDao.observeMonthAggregates(start, end)
    }

    override fun observeYearMoodCounts(year: Int): Flow<List<DiaryMoodAggregate>> {
        val (start, end) = yearRange(year)
        return diaryDao.observeMoodCounts(start, end)
    }

    override fun observeMonthMoodCounts(yearMonth: YearMonth): Flow<List<DiaryMoodAggregate>> =
        diaryDao.observeMoodCounts(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeMonthlyMoodCounts(year: Int): Flow<List<DiaryMonthlyMoodAggregate>> {
        val (start, end) = yearRange(year)
        return diaryDao.observeMonthlyMoodCounts(start, end)
    }

    override suspend fun getDistinctDiaryDates(year: Int): List<LocalDate> {
        val (start, end) = yearRange(year)
        return diaryDao.getDistinctDiaryDates(start, end)
    }

    override suspend fun loadDiaryContentBatch(
        year: Int,
        limit: Int,
        offset: Int,
    ): List<DiaryContentBatchItem> {
        validatePage(limit, offset)
        val (start, end) = yearRange(year)
        return diaryDao.getContentBatch(start, end, limit, offset)
    }

    override suspend fun saveDiary(
        diary: DiaryEntity,
        images: List<DiaryImageEntity>,
        moods: Set<Int>,
    ) {
        require(diary.content.isNotBlank() || images.isNotEmpty()) {
            "日记正文和照片不能同时为空"
        }
        require(images.all { it.diaryId == diary.id }) {
            "照片必须属于当前日记"
        }
        require(moods.all(DiaryMoodIds::isValid)) {
            "日记包含无效的心情"
        }

        database.withTransaction {
            // Keep one value in the legacy column for compatibility; the child
            // table below is the source of truth for all selected moods.
            diaryDao.upsertDiary(diary.copy(mood = moods.minOrNull()))
            diaryDao.deleteImages(diary.id)
            if (images.isNotEmpty()) {
                diaryDao.upsertImages(images.sortedBy(DiaryImageEntity::sortOrder))
            }
            diaryDao.deleteMoods(diary.id)
            if (moods.isNotEmpty()) {
                diaryDao.upsertMoods(
                    moods.sorted().map { mood ->
                        DiaryMoodEntity(diaryId = diary.id, mood = mood)
                    },
                )
            }
        }
    }

    override suspend fun deleteDiary(id: String): List<String> = database.withTransaction {
        val imagePaths = diaryDao.getImages(id).map(DiaryImageEntity::localPath)
        diaryDao.deleteDiary(id)
        imagePaths
    }

    private fun validateFilter(filter: DiaryBrowseFilter) {
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
