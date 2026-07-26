package com.example.blue.core.navigation

import androidx.compose.runtime.Immutable

@Immutable
sealed class AppDestination(
    val route: String,
    val title: String,
) {
    data object Home : AppDestination("home", "首页")
    data object Diary : AppDestination("diary", "日记")
    data object Accounting : AppDestination("accounting", "记账")
    data object Sleep : AppDestination("sleep", "睡眠")
    data object Time : AppDestination("time", "时光")
    data object Backup : AppDestination("backup", "数据管理")
}
