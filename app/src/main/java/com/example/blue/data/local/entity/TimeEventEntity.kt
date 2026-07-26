package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TimeEventType {
    COUNTDOWN,
    ANNIVERSARY,
}

@Entity(
    tableName = "time_events",
    indices = [Index("type"), Index("eventDate")],
)
data class TimeEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val eventDate: LocalDate,
    val type: TimeEventType,
    val imagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
