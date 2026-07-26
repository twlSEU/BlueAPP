package com.example.blue.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.blue.R
import com.example.blue.core.navigation.AppDestination

enum class FeatureAccent {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    QUATERNARY,
    QUINARY,
}

@Immutable
data class HomeFeature(
    val destination: AppDestination,
    @param:DrawableRes val iconRes: Int,
    val accent: FeatureAccent,
    val primaryAction: HomeAction,
    val directSecondaryAction: HomeAction? = null,
)

enum class HomeActionDestination {
    DIARY_QUICK_ADD,
    ACCOUNTING_QUICK_ENTRY,
    SLEEP_QUICK_RECORD,
    TIME_LIFE_TRACE,
    TIME_EVENTS,
    BACKUP_EXPORT,
    BACKUP_RESTORE,
}

@Immutable
data class HomeAction(
    val title: String,
    val symbol: String,
    val destination: HomeActionDestination,
)

@Immutable
data class HomeMetrics(
    val diaryMonthCount: Int = 0,
    val accountingMonthCount: Int = 0,
    val sleepMonthCount: Int = 0,
    val timeEventCount: Int = 0,
) {
    fun labelFor(destination: AppDestination): String = when (destination) {
        AppDestination.Diary -> "本月 $diaryMonthCount 篇日记"
        AppDestination.Accounting -> "本月 $accountingMonthCount 笔记录"
        AppDestination.Sleep -> "本月 $sleepMonthCount 晚记录"
        AppDestination.Time -> "$timeEventCount 个重要日子"
        AppDestination.Backup -> "本地保存"
        AppDestination.Home -> ""
    }
}

val homeFeatures = listOf(
    HomeFeature(
        destination = AppDestination.Diary,
        iconRes = R.drawable.ic_diary,
        accent = FeatureAccent.PRIMARY,
        primaryAction = HomeAction(
            title = "写日记",
            symbol = "✦",
            destination = HomeActionDestination.DIARY_QUICK_ADD,
        ),
    ),
    HomeFeature(
        destination = AppDestination.Accounting,
        iconRes = R.drawable.ic_accounting,
        accent = FeatureAccent.SECONDARY,
        primaryAction = HomeAction(
            title = "记一笔",
            symbol = "+",
            destination = HomeActionDestination.ACCOUNTING_QUICK_ENTRY,
        ),
    ),
    HomeFeature(
        destination = AppDestination.Sleep,
        iconRes = R.drawable.ic_sleep,
        accent = FeatureAccent.QUATERNARY,
        primaryAction = HomeAction(
            title = "记一晚",
            symbol = "+",
            destination = HomeActionDestination.SLEEP_QUICK_RECORD,
        ),
    ),
    HomeFeature(
        destination = AppDestination.Time,
        iconRes = R.drawable.ic_time,
        accent = FeatureAccent.QUINARY,
        primaryAction = HomeAction(
            title = "岁痕",
            symbol = "◧",
            destination = HomeActionDestination.TIME_LIFE_TRACE,
        ),
        directSecondaryAction = HomeAction(
            title = "去来",
            symbol = "⌛",
            destination = HomeActionDestination.TIME_EVENTS,
        ),
    ),
    HomeFeature(
        destination = AppDestination.Backup,
        iconRes = R.drawable.ic_backup,
        accent = FeatureAccent.TERTIARY,
        primaryAction = HomeAction(
            title = "导出",
            symbol = "↑",
            destination = HomeActionDestination.BACKUP_EXPORT,
        ),
        directSecondaryAction = HomeAction(
            title = "恢复",
            symbol = "↓",
            destination = HomeActionDestination.BACKUP_RESTORE,
        ),
    ),
)
