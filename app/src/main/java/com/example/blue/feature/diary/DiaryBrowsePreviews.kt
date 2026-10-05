package com.example.blue.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryMoodEntity
import com.example.blue.data.local.entity.DiaryMoodIds
import com.example.blue.data.local.entity.DiaryWithImages
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.FeatureHubTab
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime

private val BrowsePreviewTabs = listOf(
    FeatureHubTab("archive", "年月"),
    FeatureHubTab("browse", "浏览"),
    FeatureHubTab("summary", "总结"),
)

private fun previewDiary(
    id: String,
    date: LocalDate,
    time: LocalTime,
    content: String,
    moods: List<Int>,
    photoCount: Int,
): DiaryWithImages = DiaryWithImages(
    diary = DiaryEntity(id, date, time, content, createdAt = 0, updatedAt = 0),
    images = List(photoCount) { index ->
        DiaryImageEntity(
            id = "$id-photo-$index",
            diaryId = id,
            localPath = "diary_images/preview-$id-$index.jpg",
            sortOrder = index,
            createdAt = 0,
        )
    },
    moods = moods.map { DiaryMoodEntity(id, it) },
)

private val BrowsePreviewDiaries = listOf(
    previewDiary(
        id = "first",
        date = LocalDate.of(2026, 9, 27),
        time = LocalTime.of(0, 4),
        content = "昨晚睡得有些晚，今天想慢慢找回自己的节奏。\n" +
            "上午完成了几件计划里的事，也给自己留了一点放空的时间。\n" +
            "下午和朋友散步，晚饭吃到了喜欢的菜。\n" +
            "把普通的一天记下来，也是一件很温柔的事。",
        moods = listOf(DiaryMoodIds.TIRED),
        photoCount = 5,
    ),
    previewDiary(
        id = "second",
        date = LocalDate.of(2026, 9, 24),
        time = LocalTime.of(21, 59),
        content = "今天有点累，不过还是想记录下窗外的晚霞。",
        moods = listOf(DiaryMoodIds.LOW, DiaryMoodIds.TIRED, DiaryMoodIds.ANXIOUS),
        photoCount = 2,
    ),
)

@Composable
internal fun DiaryBrowsePreviewContent() {
    val context = LocalContext.current
    val imageStorage = remember(context) { DiaryImageStorage(context) }
    val state = remember {
        DiaryBrowseUiState(items = BrowsePreviewDiaries, totalCount = 428, isRefreshing = false)
    }
    BlueTheme(darkTheme = false) {
        FeatureHubScreen(
            tabs = BrowsePreviewTabs,
            accentColor = DiaryBrowseAccent,
            tabStyle = DiaryBrowseTabStyle,
            initialPage = 1,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { DiaryBrowseControls(state, onDateChange = { _, _, _ -> }, onOrderChange = {}) }
                items(BrowsePreviewDiaries, key = { it.diary.id }) { diary ->
                    DiaryBrowseCard(diary, imageStorage, onOpenDiary = {}, onPreview = { _, _ -> })
                }
            }
        }
    }
}
