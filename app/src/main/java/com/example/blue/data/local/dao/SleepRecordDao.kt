package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.blue.data.local.entity.SleepRecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {
    @Query(
        """
        SELECT * FROM sleep_records
        WHERE recordDate >= :startDate AND recordDate < :endDateExclusive
        ORDER BY recordDate
        """,
    )
    fun observeRange(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records WHERE recordDate = :recordDate LIMIT 1")
    fun observeRecord(recordDate: LocalDate): Flow<SleepRecordEntity?>

    @Query("SELECT * FROM sleep_records WHERE recordDate = :recordDate LIMIT 1")
    suspend fun getRecord(recordDate: LocalDate): SleepRecordEntity?

    @Query("SELECT * FROM sleep_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): SleepRecordEntity?

    @Upsert
    suspend fun upsertRecord(record: SleepRecordEntity)

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteRecord(id: String): Int

    @Query("SELECT * FROM sleep_records ORDER BY recordDate")
    suspend fun getAllRecords(): List<SleepRecordEntity>

    @Query("DELETE FROM sleep_records")
    suspend fun clearAllRecords()
}
