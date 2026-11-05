package com.example.blue.feature.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiaryWordFilterTest {
    @Test
    fun customWordsMatchNormalizedChineseAndEnglishTokens() {
        val custom = setOfNotNull(normalizeCustomDiaryStopWord(" 工作 "), normalizeCustomDiaryStopWord(" Coffee "))
        val excluded = defaultDiaryStopWords + custom
        assertEquals(setOf("工作", "coffee"), custom)
        assertNull(diaryWordForFrequency("工作", excluded))
        assertNull(diaryWordForFrequency("COFFEE!", excluded))
        assertEquals("工作室", diaryWordForFrequency("工作室", excluded))
        assertEquals("散步", diaryWordForFrequency("散步", excluded))
    }

    @Test
    fun removingCustomWordRestoresItsCountWithoutRestoringDefaultWords() {
        val custom = setOf("工作", "散步") - "工作"
        val excluded = defaultDiaryStopWords + custom
        assertEquals("工作", diaryWordForFrequency("工作", excluded))
        assertNull(diaryWordForFrequency("散步", excluded))
        assertNull(diaryWordForFrequency("今天", excluded))
        assertNull(diaryWordForFrequency("THE", excluded))
    }

    @Test
    fun invalidStopWordInputAndPreviouslyIgnoredTokensAreRejected() {
        listOf("", "   ", "工作 散步", "工作、散步", "2026", "a".repeat(25)).forEach {
            assertNull(normalizeCustomDiaryStopWord(it))
        }
        listOf("2026", "。", "我", "OK").forEach {
            assertNull(diaryWordForFrequency(it, defaultDiaryStopWords))
        }
    }
}
