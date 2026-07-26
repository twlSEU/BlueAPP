package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.blue.data.repository.HomeMetricsSnapshot
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDao {
    /** Returns only the four counters rendered on home in a single observable query. */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM diaries
                WHERE diaryDate >= :startDate AND diaryDate < :endDateExclusive) AS diaryMonthCount,
            (SELECT COUNT(*) FROM account_entries
                WHERE entryDate >= :startDate AND entryDate < :endDateExclusive) AS accountingMonthCount,
            (SELECT COUNT(*) FROM sleep_records
                WHERE recordDate >= :startDate AND recordDate < :endDateExclusive) AS sleepMonthCount,
            (SELECT COUNT(*) FROM time_events) AS timeEventCount
        """,
    )
    fun observeMetrics(
        startDate: LocalDate,
        endDateExclusive: LocalDate,
    ): Flow<HomeMetricsSnapshot>
}
