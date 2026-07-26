package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryMoodEntity
import com.example.blue.data.local.entity.DiaryWithImages
import com.example.blue.data.repository.DiaryContentBatchItem
import com.example.blue.data.repository.DiaryMonthAggregate
import com.example.blue.data.repository.DiaryMonthlyMoodAggregate
import com.example.blue.data.repository.DiaryMoodAggregate
import com.example.blue.data.repository.DiaryPeriodSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM diaries
        WHERE diaryDate >= :startDate AND diaryDate < :endDateExclusive
        ORDER BY diaryDate DESC, diaryTime DESC
        """,
    )
    fun observeDiaries(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<DiaryWithImages>>

    @Transaction
    @Query("SELECT * FROM diaries WHERE id = :id")
    fun observeDiary(id: String): Flow<DiaryWithImages?>

    @Transaction
    @Query(
        """
        SELECT * FROM diaries
        WHERE diaryDate = :date
        ORDER BY updatedAt DESC, diaryTime DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestDiaryOnDate(date: LocalDate): DiaryWithImages?

    @Query(
        """
        SELECT COUNT(*) FROM diaries
        WHERE (:startDate IS NULL OR diaryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR diaryDate < :endDateExclusive)
        """,
    )
    suspend fun countDiaries(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
    ): Int

    @Transaction
    @Query(
        """
        SELECT * FROM diaries
        WHERE (:startDate IS NULL OR diaryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR diaryDate < :endDateExclusive)
        ORDER BY diaryDate, diaryTime, id
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getDiaryPageAscending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        limit: Int,
        offset: Int,
    ): List<DiaryWithImages>

    @Transaction
    @Query(
        """
        SELECT * FROM diaries
        WHERE (:startDate IS NULL OR diaryDate >= :startDate)
          AND (:endDateExclusive IS NULL OR diaryDate < :endDateExclusive)
        ORDER BY diaryDate DESC, diaryTime DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getDiaryPageDescending(
        startDate: LocalDate?,
        endDateExclusive: LocalDate?,
        limit: Int,
        offset: Int,
    ): List<DiaryWithImages>

    @Query(
        """
        SELECT
            COUNT(DISTINCT diaryDate) AS recordDays,
            COUNT(*) AS diaryCount,
            COALESCE(SUM(
                LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(content, ' ', ''), CHAR(10), ''), CHAR(13), ''), CHAR(9), ''))
            ), 0) AS totalCharacterCount,
            COALESCE(AVG(
                LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(content, ' ', ''), CHAR(10), ''), CHAR(13), ''), CHAR(9), ''))
            ), 0.0) AS averageCharacterCount,
            COALESCE(MAX(
                LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(content, ' ', ''), CHAR(10), ''), CHAR(13), ''), CHAR(9), ''))
            ), 0) AS longestCharacterCount
        FROM diaries
        WHERE diaryDate >= :startDate AND diaryDate < :endDateExclusive
        """,
    )
    fun observePeriodSummary(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<DiaryPeriodSummary>

    @Query(
        """
        SELECT
            CAST(SUBSTR(d.diaryDate, 6, 2) AS INTEGER) AS month,
            COUNT(DISTINCT d.diaryDate) AS recordDays,
            COUNT(*) AS diaryCount,
            COALESCE(SUM(
                LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(d.content, ' ', ''), CHAR(10), ''), CHAR(13), ''), CHAR(9), ''))
            ), 0) AS totalCharacterCount,
            MAX(d.diaryDate) AS lastDiaryDate,
            (
                SELECT image.localPath
                FROM diaries AS latest
                JOIN diary_images AS image ON image.diaryId = latest.id
                WHERE latest.diaryDate >= :startDate
                  AND latest.diaryDate < :endDateExclusive
                  AND SUBSTR(latest.diaryDate, 6, 2) = SUBSTR(d.diaryDate, 6, 2)
                ORDER BY latest.diaryDate DESC, latest.diaryTime DESC, image.sortOrder, image.id
                LIMIT 1
            ) AS thumbnailPath
        FROM diaries AS d
        WHERE d.diaryDate >= :startDate AND d.diaryDate < :endDateExclusive
        GROUP BY SUBSTR(d.diaryDate, 6, 2)
        ORDER BY month
        """,
    )
    fun observeMonthAggregates(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<DiaryMonthAggregate>>

    @Query(
        """
        SELECT diary_moods.mood AS mood, COUNT(*) AS count
        FROM diary_moods
        INNER JOIN diaries ON diaries.id = diary_moods.diaryId
        WHERE diaries.diaryDate >= :startDate
          AND diaries.diaryDate < :endDateExclusive
        GROUP BY diary_moods.mood
        ORDER BY count DESC, diary_moods.mood
        """,
    )
    fun observeMoodCounts(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<DiaryMoodAggregate>>

    @Query(
        """
        SELECT
            CAST(SUBSTR(diaries.diaryDate, 6, 2) AS INTEGER) AS month,
            diary_moods.mood AS mood,
            COUNT(*) AS count
        FROM diary_moods
        INNER JOIN diaries ON diaries.id = diary_moods.diaryId
        WHERE diaries.diaryDate >= :startDate
          AND diaries.diaryDate < :endDateExclusive
        GROUP BY SUBSTR(diaries.diaryDate, 6, 2), diary_moods.mood
        ORDER BY month, diary_moods.mood
        """,
    )
    fun observeMonthlyMoodCounts(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<DiaryMonthlyMoodAggregate>>

    @Query(
        """
        SELECT DISTINCT diaryDate
        FROM diaries
        WHERE diaryDate >= :startDate AND diaryDate < :endDateExclusive
        ORDER BY diaryDate
        """,
    )
    suspend fun getDistinctDiaryDates(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): List<LocalDate>

    @Query(
        """
        SELECT id, diaryDate, content
        FROM diaries
        WHERE diaryDate >= :startDate AND diaryDate < :endDateExclusive
        ORDER BY diaryDate, diaryTime, id
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getContentBatch(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
        limit: Int,
        offset: Int,
    ): List<DiaryContentBatchItem>

    @Upsert
    suspend fun upsertDiary(diary: DiaryEntity)

    @Upsert
    suspend fun upsertImages(images: List<DiaryImageEntity>)

    @Upsert
    suspend fun upsertMoods(moods: List<DiaryMoodEntity>)

    @Query("SELECT * FROM diary_images WHERE diaryId = :diaryId ORDER BY sortOrder")
    suspend fun getImages(diaryId: String): List<DiaryImageEntity>

    @Query("DELETE FROM diary_images WHERE diaryId = :diaryId")
    suspend fun deleteImages(diaryId: String)

    @Query("DELETE FROM diary_moods WHERE diaryId = :diaryId")
    suspend fun deleteMoods(diaryId: String)

    @Query("DELETE FROM diaries WHERE id = :id")
    suspend fun deleteDiary(id: String): Int

    @Query("SELECT * FROM diaries ORDER BY diaryDate, diaryTime")
    suspend fun getAllDiaries(): List<DiaryEntity>

    @Query("SELECT * FROM diary_images ORDER BY diaryId, sortOrder")
    suspend fun getAllImages(): List<DiaryImageEntity>

    @Query("SELECT * FROM diary_moods ORDER BY diaryId, mood")
    suspend fun getAllMoods(): List<DiaryMoodEntity>

    @Query("DELETE FROM diaries")
    suspend fun clearAllDiaries()
}
