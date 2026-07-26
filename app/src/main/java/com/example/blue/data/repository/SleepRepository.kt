package com.example.blue.data.repository

import com.example.blue.data.local.entity.SleepRecordEntity
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun observeRecord(recordDate: LocalDate): Flow<SleepRecordEntity?>

    fun observeRange(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<SleepRecordEntity>>

    fun observeMonth(yearMonth: YearMonth): Flow<List<SleepRecordEntity>>

    fun observeYear(year: Int): Flow<List<SleepRecordEntity>>

    suspend fun getRecord(recordDate: LocalDate): SleepRecordEntity?

    suspend fun saveRecord(record: SleepRecordEntity)

    suspend fun deleteRecord(id: String)
}
