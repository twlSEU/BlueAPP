package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

enum class SleepSource {
    MANUAL,
    SYSTEM_ESTIMATE,
    MANUAL_CONFIRMED,
}

@Entity(
    tableName = "sleep_records",
    indices = [Index(value = ["recordDate"], unique = true)],
)
data class SleepRecordEntity(
    @PrimaryKey val id: String,
    val recordDate: LocalDate,
    val sleepDateTime: LocalDateTime,
    val wakeDateTime: LocalDateTime?,
    val source: SleepSource,
    val isEstimated: Boolean,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
