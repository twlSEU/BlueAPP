package com.example.blue.data.repository

import com.example.blue.data.local.dao.HomeDao
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

data class HomeMetricsSnapshot(
    val diaryMonthCount: Int = 0,
    val accountingMonthCount: Int = 0,
    val sleepMonthCount: Int = 0,
    val timeEventCount: Int = 0,
)

interface HomeRepository {
    fun observeMetrics(yearMonth: YearMonth): Flow<HomeMetricsSnapshot>
}

class OfflineHomeRepository(
    private val homeDao: HomeDao,
) : HomeRepository {
    override fun observeMetrics(yearMonth: YearMonth): Flow<HomeMetricsSnapshot> =
        homeDao.observeMetrics(
            startDate = yearMonth.atDay(1),
            endDateExclusive = yearMonth.plusMonths(1).atDay(1),
        )
}
