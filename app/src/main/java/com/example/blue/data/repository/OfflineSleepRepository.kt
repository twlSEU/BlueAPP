package com.example.blue.data.repository

import androidx.room.withTransaction
import com.example.blue.core.database.AppDatabase
import com.example.blue.data.local.dao.SleepRecordDao
import com.example.blue.data.local.entity.SleepRecordEntity
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

class OfflineSleepRepository(
    private val database: AppDatabase,
    private val sleepRecordDao: SleepRecordDao,
) : SleepRepository {
    override fun observeRecord(recordDate: LocalDate): Flow<SleepRecordEntity?> =
        sleepRecordDao.observeRecord(recordDate)

    override fun observeRange(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<List<SleepRecordEntity>> {
        require(startDate < endDateExclusive) { "结束日期必须晚于开始日期" }
        return sleepRecordDao.observeRange(startDate, endDateExclusive)
    }

    override fun observeMonth(yearMonth: YearMonth): Flow<List<SleepRecordEntity>> =
        observeRange(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )

    override fun observeYear(year: Int): Flow<List<SleepRecordEntity>> =
        observeRange(
            startDate = LocalDate.of(year, 1, 1),
            endDateExclusive = LocalDate.of(year + 1, 1, 1),
        )

    override suspend fun getRecord(recordDate: LocalDate): SleepRecordEntity? =
        sleepRecordDao.getRecord(recordDate)

    override suspend fun saveRecord(record: SleepRecordEntity) {
        require(record.wakeDateTime == null || record.wakeDateTime > record.sleepDateTime) {
            "起床时间必须晚于睡觉时间"
        }
        database.withTransaction {
            val existingById = sleepRecordDao.getRecordById(record.id)
            val existingOnDate = sleepRecordDao.getRecord(record.recordDate)
            val resolved = when {
                existingById != null -> {
                    require(existingOnDate == null || existingOnDate.id == record.id) {
                        "该日期已经有睡眠记录"
                    }
                    record.copy(createdAt = existingById.createdAt)
                }
                existingOnDate != null -> record.copy(
                    id = existingOnDate.id,
                    createdAt = existingOnDate.createdAt,
                )
                else -> record
            }
            sleepRecordDao.upsertRecord(resolved)
        }
    }

    override suspend fun deleteRecord(id: String) {
        sleepRecordDao.deleteRecord(id)
    }
}
