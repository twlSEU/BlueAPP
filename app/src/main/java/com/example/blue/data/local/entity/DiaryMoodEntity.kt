package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * A selected mood for a diary.
 *
 * Keeping moods in a child table allows one diary to have multiple moods while
 * the legacy `diaries.mood` column remains available for existing databases.
 */
@Entity(
    tableName = "diary_moods",
    primaryKeys = ["diaryId", "mood"],
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["mood"])],
)
data class DiaryMoodEntity(
    val diaryId: String,
    val mood: Int,
)

/**
 * Stable mood IDs. Values 1–6 keep their historical meaning so old diary data
 * can be migrated without rewriting or losing the user's selection.
 */
object DiaryMoodIds {
    const val LOW = 1
    const val SAD = 2
    const val CALM = 3
    const val PLEASANT = 4
    const val ROMANTIC = 5
    const val ANGRY = 6
    const val TIRED = 7
    const val UNWELL = 8
    const val ANXIOUS = 9
    const val BORED = 10

    val validRange: IntRange = LOW..BORED

    fun isValid(value: Int): Boolean = value in validRange
}
