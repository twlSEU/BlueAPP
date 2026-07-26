package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "diaries",
    indices = [Index(value = ["diaryDate", "diaryTime"])],
)
data class DiaryEntity(
    @PrimaryKey val id: String,
    val diaryDate: LocalDate,
    val diaryTime: LocalTime,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val mood: Int? = null,
)
