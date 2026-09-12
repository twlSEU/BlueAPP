package com.example.blue.data.local.entity

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryWithImagesTest {
    @Test
    fun relatedMoodsRemainSourceOfTruthAndIgnoreInvalidValues() {
        val diary = diaryWithMoods(
            legacyMood = DiaryMoodIds.LOW,
            relatedMoods = listOf(DiaryMoodIds.CALM, 99, DiaryMoodIds.CALM, DiaryMoodIds.TIRED),
        )

        assertEquals(setOf(DiaryMoodIds.CALM, DiaryMoodIds.TIRED), diary.selectedMoodIds)
    }

    @Test
    fun legacyMoodIsUsedWhenThereAreNoValidRelatedMoods() {
        val diary = diaryWithMoods(
            legacyMood = DiaryMoodIds.PLEASANT,
            relatedMoods = listOf(-1, 99),
        )

        assertEquals(setOf(DiaryMoodIds.PLEASANT), diary.selectedMoodIds)
    }

    private fun diaryWithMoods(
        legacyMood: Int?,
        relatedMoods: List<Int>,
    ) = DiaryWithImages(
        diary = DiaryEntity(
            id = "diary",
            diaryDate = LocalDate.of(2026, 8, 12),
            diaryTime = LocalTime.NOON,
            content = "测试",
            createdAt = 0L,
            updatedAt = 0L,
            mood = legacyMood,
        ),
        images = emptyList(),
        moods = relatedMoods.map { mood -> DiaryMoodEntity(diaryId = "diary", mood = mood) },
    )
}
