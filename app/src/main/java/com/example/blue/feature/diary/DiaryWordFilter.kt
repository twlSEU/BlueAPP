package com.example.blue.feature.diary

import java.util.Locale

internal val defaultDiaryStopWords = setOf(
    "今天", "昨天", "明天", "现在", "然后", "因为", "所以", "但是", "还是", "已经", "没有", "一个", "一些",
    "这个", "那个", "自己", "觉得", "感觉", "真的", "就是", "可以", "可能", "非常", "比较", "时候", "事情",
    "我们", "你们", "他们", "她们", "它们", "我的", "你的", "他的", "她的", "以及", "如果", "而且", "不过",
    "the", "and", "that", "this", "with", "from", "have", "was", "were", "are", "for", "but", "not",
)

internal fun normalizeCustomDiaryStopWord(input: String): String? {
    val word = input.trim()
    if (word.isEmpty() || word.length > 24 || word.any { !it.isLetter() }) return null
    return word.lowercase(Locale.ROOT).filter(Char::isLetter)
}

internal fun diaryWordForFrequency(raw: String, stopWords: Set<String>): String? {
    val word = raw.lowercase(Locale.ROOT).filter(Char::isLetter)
    val minimumLength = if (word.any { it.code in 0x3400..0x9FFF }) 2 else 3
    return word.takeIf { it.codePointCount(0, it.length) >= minimumLength && it !in stopWords }
}
