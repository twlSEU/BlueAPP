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
    @Relation(
        parentColumn = "id",
        entityColumn = "diaryId",
    )
    val moods: List<DiaryMoodEntity> = emptyList(),
)

/**
 * The relation is the source of truth for multi-select moods. Falling back to
 * the legacy column keeps records readable while an old database is upgrading.
 */
val DiaryWithImages.selectedMoodIds: Set<Int>
    get() {
        val related = moods
            .map(DiaryMoodEntity::mood)
            .filter(DiaryMoodIds::isValid)
            .toSet()
        return if (related.isNotEmpty()) {
            related
        } else {
            diary.mood
                ?.takeIf(DiaryMoodIds::isValid)
                ?.let(::setOf)
                .orEmpty()
        }
    }
