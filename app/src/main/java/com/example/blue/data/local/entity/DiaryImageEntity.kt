package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_images",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["diaryId"])],
)
data class DiaryImageEntity(
    @PrimaryKey val id: String,
    val diaryId: String,
    val localPath: String,
    val sortOrder: Int,
    val createdAt: Long,
)
