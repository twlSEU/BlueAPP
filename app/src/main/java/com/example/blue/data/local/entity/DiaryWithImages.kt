package com.example.blue.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DiaryWithImages(
    @Embedded val diary: DiaryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "diaryId",
    )
    val images: List<DiaryImageEntity>,
)
