package com.example.blue.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiaryStopWordPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("diary_summary", Context.MODE_PRIVATE)
    private val _words = MutableStateFlow(preferences.getStringSet(CUSTOM_WORDS_KEY, null).orEmpty().toSet())
    val words = _words.asStateFlow()

    fun replace(words: Set<String>) {
        if (words == _words.value) return
        val saved = words.toSet()
        preferences.edit { putStringSet(CUSTOM_WORDS_KEY, saved) }
        _words.value = saved
    }

    private companion object {
        const val CUSTOM_WORDS_KEY = "custom_stop_words"
    }
}
