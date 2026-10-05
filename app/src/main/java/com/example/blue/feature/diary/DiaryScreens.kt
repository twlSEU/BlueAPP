package com.example.blue.feature.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.blue.R
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryMoodEntity
import com.example.blue.data.local.entity.DiaryMoodIds
import com.example.blue.data.local.entity.DiaryWithImages
import com.example.blue.data.local.entity.selectedMoodIds
import com.example.blue.data.repository.DiaryRepository
import com.example.blue.data.repository.DiaryMoodAggregate
import com.example.blue.data.repository.DiaryPeriodSummary
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.AppDateTimeSelectorRow
import com.example.blue.feature.common.AppTimePickerDialog
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DiaryEditorBackground = Color(0xFFF4F8FC)
private val DiaryEditorCard = Color(0xFFFFFFFF)
private val DiaryEditorTitle = Color(0xFF20384A)
private val DiaryEditorBody = Color(0xFF465E70)
private val DiaryEditorMuted = Color(0xFF8798A6)
private val DiaryEditorBlue = Color(0xFF3D7BE5)
private val DiaryEditorLightBlue = Color(0xFFEAF3FF)
private val DiaryEditorDanger = Color(0xFFE05252)
private val DiaryEditorShape = RoundedCornerShape(24.dp)
private const val DiaryContentCharacterLimit = 5000
private val diaryPickerWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")
private val DiaryYearBackground = Color(0xFFF6F8FC)
private val DiaryYearSurface = Color(0xFFFEFFFF)
private val DiaryYearFogBlue = Color(0xFF86A5BA)
private val DiaryYearText = Color(0xFF2D4555)
private val DiaryYearMuted = Color(0xFF748895)
private val DiaryMonthBackground = Color(0xFFF5F9FE)
private val DiaryMonthSurface = Color(0xFFFEFFFF)
private val DiaryMonthTitle = Color(0xFF0D2146)
private val DiaryMonthMuted = Color(0xFF7C90AE)
private val DiaryMonthTile = Color(0xFFF1F7FE)
private val DiaryMonthBlue = Color(0xFF4B9BFF)
private val DiaryMonthDivider = Color(0xFFE8EFF8)
private val DiaryMonthShadow = Color(0xFF7D9FC4).copy(alpha = 0.09f)
private val DiaryMonthShape = RoundedCornerShape(24.dp)
private val moodIdSetSaver = listSaver<Set<Int>, Int>(
    save = { selected -> selected.toList() },
    restore = { saved -> saved.toSet() },
)
private data class MoodOption(
    val value: Int,
    val label: String,
    val imageRes: Int,
)
private val moodOptions = listOf(
    MoodOption(DiaryMoodIds.SAD, "伤心", R.drawable.mood_shangxin),
    MoodOption(DiaryMoodIds.LOW, "低落", R.drawable.mood_diluo),
    MoodOption(DiaryMoodIds.ANGRY, "愤怒", R.drawable.mood_fennv),
    MoodOption(DiaryMoodIds.CALM, "平静", R.drawable.mood_pingjing),
    MoodOption(DiaryMoodIds.PLEASANT, "愉快", R.drawable.mood_yukuai),
    MoodOption(DiaryMoodIds.TIRED, "疲惫", R.drawable.mood_pibei),
    MoodOption(DiaryMoodIds.UNWELL, "不适", R.drawable.mood_bushi),
    MoodOption(DiaryMoodIds.ANXIOUS, "焦虑", R.drawable.mood_jiaolv),
    MoodOption(DiaryMoodIds.BORED, "无聊", R.drawable.mood_wuliao),
    MoodOption(DiaryMoodIds.ROMANTIC, "恋爱", R.drawable.mood_lianai),
)

internal fun diaryMoodLabel(value: Int): String =
    moodOptions.firstOrNull { it.value == value }?.label ?: "未记录心情"

internal fun diaryMoodLabels(values: Collection<Int>): String {
    val labels = moodOptions
        .filter { option -> option.value in values }
        .map(MoodOption::label)
    return labels.takeIf { it.isNotEmpty() }?.joinToString("、") ?: "未记录心情"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryYearScreen(
    repository: DiaryRepository,
    imageStorage: DiaryImageStorage,
    onOpenMonth: (Int, Int) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val today = remember { LocalDate.now() }
    val currentYear = today.year
    var year by rememberSaveable { mutableIntStateOf(currentYear) }
    val monthAggregateFlow = remember(repository, year) { repository.observeMonthAggregates(year) }
    val monthAggregates by monthAggregateFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val byMonth = remember(monthAggregates) { monthAggregates.associateBy { it.month } }
    val displayedMonths = remember(year, today) { diaryMonthsForYear(year, today) }
    Scaffold(
        containerColor = DiaryYearBackground,
        topBar = { if (showTopBar) DiaryYearTopBar(onBack = onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "year-selector", contentType = "year-selector") {
                YearSelector(
                    year = year,
                    canMoveForward = year < currentYear,
                    onYearChange = { year = it },
                )
            }
            items(
                items = displayedMonths,
                key = { month -> "$year-$month" },
                contentType = { "diary-month-card" },
            ) { month ->
                val aggregate = byMonth[month]
                MonthCard(
                    month = month,
                    count = aggregate?.diaryCount ?: 0,
                    lastDate = aggregate?.lastDiaryDate,
                    thumbnail = aggregate?.thumbnailPath?.let(imageStorage::fileFor),
                    onClick = { onOpenMonth(year, month) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(180),
                        placementSpec = tween(220),
                        fadeOutSpec = tween(180),
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryYearTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "日记",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = DiaryYearText,
            )
        },
        navigationIcon = { AppBackButton(onClick = onBack) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DiaryYearBackground,
            scrolledContainerColor = DiaryYearBackground,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryMonthScreen(
    repository: DiaryRepository,
    year: Int,
    month: Int,
    onOpenDiary: (String) -> Unit,
    onCreateDiary: () -> Unit,
    onBack: () -> Unit,
    onCreateDiaryForDate: (LocalDate) -> Unit = { onCreateDiary() },
) {
    val yearMonth = remember(year, month) { YearMonth.of(year, month) }
    // Keep the lazy list out of layout until Room has emitted real data. On a back
    // navigation the flow otherwise starts as an empty list, which clamps the restored
    // LazyListState to index 0 before the month's entries arrive.
    val diaryFlow = remember(repository, yearMonth) {
        repository.observeMonth(yearMonth)
            .map<List<DiaryWithImages>, List<DiaryWithImages>?> { it }
    }
    val summaryFlow = remember(repository, yearMonth) { repository.observeMonthSummary(yearMonth) }
    val moodFlow = remember(repository, yearMonth) { repository.observeMonthMoodCounts(yearMonth) }
    val diaries by diaryFlow.collectAsStateWithLifecycle(initialValue = null)
    val summary by summaryFlow.collectAsStateWithLifecycle(
        initialValue = DiaryPeriodSummary(
            recordDays = 0,
            diaryCount = 0,
            totalCharacterCount = 0L,
            averageCharacterCount = 0.0,
            longestCharacterCount = 0,
        ),
    )
    val moods by moodFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    DiaryMonthContent(
        yearMonth = yearMonth,
        diaries = diaries,
        summary = summary,
        moods = moods,
        onOpenDiary = onOpenDiary,
        onCreateDiary = onCreateDiary,
        onBack = onBack,
        onCreateDiaryForDate = onCreateDiaryForDate,
    )
}

@Composable
private fun DiaryMonthContent(
    yearMonth: YearMonth,
    diaries: List<DiaryWithImages>?,
    summary: DiaryPeriodSummary,
    moods: List<DiaryMoodAggregate>,
    onOpenDiary: (String) -> Unit,
    onCreateDiary: () -> Unit,
    onBack: () -> Unit,
    onCreateDiaryForDate: (LocalDate) -> Unit,
) {
    val diariesByDate = remember(diaries) { diaries?.groupBy { it.diary.diaryDate } }
    val recordedDates = remember(diariesByDate) { diariesByDate?.keys?.sortedDescending() }
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = DiaryMonthBackground,
        topBar = { DiaryMonthTopBar(yearMonth = yearMonth, onBack = onBack) },
        floatingActionButton = { DiaryMonthFloatingAction(onClick = onCreateDiary) },
    ) { padding ->
        if (recordedDates == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = DiaryMonthBlue)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().align(Alignment.TopCenter),
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item(key = "month-summary-${yearMonth.year}-${yearMonth.monthValue}", contentType = "month-summary") {
                        DiaryMonthSummaryCard(summary = summary, moods = moods)
                    }
                    if (recordedDates.isEmpty()) {
                        item(key = "empty-month", contentType = "empty-state") {
                            EmptyDiaryMonth(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                ),
                                message = "这个月还没有日记。",
                            )
                        }
                    } else {
                        items(
                            items = recordedDates,
                            key = { it.toString() },
                            contentType = { "diary-day-section" },
                        ) { date ->
                            DiaryDaySection(
                                date = date,
                                diaries = diariesByDate?.get(date).orEmpty(),
                                onOpenDiary = onOpenDiary,
                                onCreateDiary = { onCreateDiaryForDate(date) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "日记 · 月份 · 设计", showBackground = true, widthDp = 390, heightDp = 790)
@Preview(name = "日记 · 月份 · 手机", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "日记 · 月份 · 窄屏", showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 800)
@Preview(name = "日记 · 月份 · 大字体", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun DiaryMonthScreenPreview() {
    val diaries = listOf(
        diaryMonthPreviewEntry(27, "00:04", "昨晚熬夜到两点半 就是不想睡\n上午完成了几道练习题，下午和朋友出去散步。晚饭吃了喜欢的菜，记下这些普通的小事，也想提醒自己明天早点休息。", setOf(DiaryMoodIds.TIRED)),
        diaryMonthPreviewEntry(24, "21:59", "秋招\n好累", setOf(DiaryMoodIds.LOW, DiaryMoodIds.TIRED, DiaryMoodIds.ANXIOUS)),
        diaryMonthPreviewEntry(16, "22:18", "今天有些疲惫，事情比想象中多。整理了房间，也给自己泡了一杯热茶。希望慢慢找到合适的节奏，把生活过得更从容一点。", setOf(DiaryMoodIds.LOW, DiaryMoodIds.TIRED, DiaryMoodIds.ANXIOUS)),
        diaryMonthPreviewEntry(12, "20:36", "下午下了一场雨，出门的时候空气很清新。最近一直在准备面试，偶尔会担心自己的进度。今晚把想做的事情列下来，先从最小的一件开始。", setOf(DiaryMoodIds.LOW, DiaryMoodIds.ANXIOUS), withPhoto = false),
        diaryMonthPreviewEntry(9, "23:10", "认真准备了一整天，还是有一点紧张。路过常去的小店，买了喜欢的面包。生活里这些小小的片刻，让今天变得没有那么难。", setOf(DiaryMoodIds.LOW, DiaryMoodIds.ANXIOUS), withPhoto = false),
        diaryMonthPreviewEntry(5, "22:05", "今天忙到很晚，终于完成了计划中的练习。接下来还要继续复习，不过先好好睡一觉。希望醒来之后能有新的精神。", setOf(DiaryMoodIds.TIRED, DiaryMoodIds.ANXIOUS), withPhoto = false),
        diaryMonthPreviewEntry(2, "19:42", "新的一月开始了。给自己定了几个小目标，也留一些时间休息。面对不确定的事情会焦虑，但今天还是往前走了一点。", setOf(DiaryMoodIds.ANXIOUS), withPhoto = false),
    )
    val summary = DiaryPeriodSummary(
        recordDays = diaries.map { it.diary.diaryDate }.distinct().size,
        diaryCount = diaries.size,
        totalCharacterCount = diaries.sumOf { it.diary.content.count { character -> !character.isWhitespace() }.toLong() },
    )
    val moods = diaries.flatMap { it.selectedMoodIds }.groupingBy { it }.eachCount()
        .map { (mood, count) -> DiaryMoodAggregate(mood, count) }

    BlueTheme(darkTheme = false, dynamicColor = false) {
        DiaryMonthContent(
            yearMonth = YearMonth.of(2026, 9),
            diaries = diaries,
            summary = summary,
            moods = moods,
            onOpenDiary = {},
            onCreateDiary = {},
            onBack = {},
            onCreateDiaryForDate = {},
        )
    }
}

@Preview(name = "日记 · 月份 · 空状态", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiaryMonthEmptyPreview() {
    BlueTheme(darkTheme = false, dynamicColor = false) {
        DiaryMonthContent(
            yearMonth = YearMonth.of(2026, 9),
            diaries = emptyList(),
            summary = DiaryPeriodSummary(),
            moods = emptyList(),
            onOpenDiary = {},
            onCreateDiary = {},
            onBack = {},
            onCreateDiaryForDate = {},
        )
    }
}

private fun diaryMonthPreviewEntry(
    day: Int,
    time: String,
    content: String,
    moods: Set<Int>,
    withPhoto: Boolean = true,
): DiaryWithImages {
    val id = "month-preview-$day"
    return DiaryWithImages(
        diary = DiaryEntity(id, LocalDate.of(2026, 9, day), LocalTime.parse(time), content, createdAt = 0, updatedAt = 0),
        images = if (withPhoto) listOf(
            DiaryImageEntity("$id-photo", id, "diary_images/$id.jpg", sortOrder = 0, createdAt = 0),
        ) else emptyList(),
        moods = moods.map { DiaryMoodEntity(id, it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    repository: DiaryRepository,
    imageStorage: DiaryImageStorage,
    diaryId: String?,
    initialYearMonth: YearMonth? = null,
    initialDate: LocalDate? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onShowMessage: ((String, Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loadedDiary by remember { mutableStateOf<DiaryWithImages?>(null) }
    var isLoading by remember { mutableStateOf(diaryId != null) }
    var dateText by rememberSaveable {
        mutableStateOf(
            (initialDate ?: defaultNewDiaryDate(initialYearMonth)).format(dateFormatter),
        )
    }
    var timeText by rememberSaveable { mutableStateOf(LocalTime.now().format(timeFormatter)) }
    var content by rememberSaveable { mutableStateOf("") }
    var selectedMoods by rememberSaveable(stateSaver = moodIdSetSaver) {
        mutableStateOf<Set<Int>>(emptySet())
    }
    var photos by rememberSaveable(stateSaver = photoReferenceListSaver) {
        mutableStateOf<List<PhotoReference>>(emptyList())
    }
    var initialized by rememberSaveable { mutableStateOf(diaryId == null) }
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var photoRemovalTarget by remember { mutableStateOf<PhotoReference?>(null) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            isLoading = true
            loadedDiary = repository.observeDiary(diaryId).first()
            isLoading = false
        }
    }
    LaunchedEffect(loadedDiary, initialized) {
        val item = loadedDiary ?: return@LaunchedEffect
        if (!initialized) {
            dateText = item.diary.diaryDate.format(dateFormatter)
            timeText = item.diary.diaryTime.format(timeFormatter)
            content = item.diary.content
            selectedMoods = item.selectedMoodIds
            photos = item.images.sortedBy { it.sortOrder }.map { PhotoReference.Existing(it.localPath) }
            initialized = true
        }
    }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris ->
        photos = photos + uris.map { PhotoReference.New(it) }
    }

    fun saveDiary() {
        val diaryDate = dateText.toLocalDateOrNull()
        val diaryTime = timeText.toLocalTimeOrNull()
        val allowedMonth = loadedDiary?.diary?.diaryDate?.let(YearMonth::from)
            ?: initialDate?.let(YearMonth::from)
            ?: initialYearMonth
            ?: YearMonth.now()
        when {
            diaryDate == null -> scope.launch { snackbarHostState.showSnackbar("请输入正确的日期") }
            diaryTime == null -> scope.launch { snackbarHostState.showSnackbar("请输入正确的时间") }
            YearMonth.from(diaryDate) != allowedMonth -> scope.launch { snackbarHostState.showSnackbar("请选择当月日期") }
            diaryDate.isAfter(LocalDate.now()) -> scope.launch { snackbarHostState.showSnackbar("不能选择未来日期") }
            content.isBlank() && photos.isEmpty() -> scope.launch { snackbarHostState.showSnackbar("请填写正文或至少添加一张照片") }
            else -> {
                if (saving) return
                saving = true
                scope.launch {
                    val newlyCopiedPaths = mutableListOf<String>()
                    runCatching {
                    val id = diaryId ?: UUID.randomUUID().toString()
                    val oldPaths = loadedDiary?.images.orEmpty().map { it.localPath }.toSet()
                    val resolvedPaths = photos.map { photo ->
                        when (photo) {
                            is PhotoReference.Existing -> photo.localPath
                            is PhotoReference.New -> imageStorage.copyFromUri(context, photo.uri).also {
                                newlyCopiedPaths += it
                            }
                        }
                    }
                    repository.saveDiary(
                        diary = DiaryEntity(
                            id = id,
                            diaryDate = diaryDate,
                            diaryTime = diaryTime,
                            content = content.trim(),
                            createdAt = loadedDiary?.diary?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            mood = selectedMoods.minOrNull(),
                        ),
                        images = resolvedPaths.mapIndexed { index, path ->
                            DiaryImageEntity(
                                id = UUID.randomUUID().toString(),
                                diaryId = id,
                                localPath = path,
                                sortOrder = index,
                                createdAt = System.currentTimeMillis(),
                            )
                        },
                        moods = selectedMoods,
                    )
                    for (path in oldPaths - resolvedPaths.toSet()) {
                        imageStorage.delete(path)
                    }
                    }.onSuccess {
                        saving = false
                        onShowMessage?.invoke("日记已保存", false)
                        onSaved()
                    }.onFailure { error ->
                        newlyCopiedPaths.forEach { path -> runCatching { imageStorage.delete(path) } }
                        saving = false
                        val message = error.message ?: "保存失败，请重试"
                        onShowMessage?.invoke(message, true) ?: snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = DiaryEditorBackground,
        topBar = {
            DiaryEditorTopBar(
                title = if (diaryId == null) "写日记" else "编辑日记",
                onBack = onBack,
                showDelete = diaryId != null,
                deleteEnabled = !saving,
                onDelete = { deleteConfirmation = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isLoading) {
                SaveDiaryBar(
                    saving = saving,
                    onSave = ::saveDiary,
                )
            }
        },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("正在加载…") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "date-time", contentType = "editor-section") {
                    AppDateTimeSelectorRow(
                        date = dateText,
                        time = timeText,
                        onSelectDate = { showDatePicker = true },
                        onSelectTime = { showTimePicker = true },
                    )
                }
                item(key = "moods", contentType = "editor-section") {
                    MoodSelector(selected = selectedMoods, onSelected = { selectedMoods = it })
                }
                item(key = "content", contentType = "editor-section") {
                    DiaryTextEditor(value = content, onValueChange = { content = it.take(DiaryContentCharacterLimit) })
                }
                item(key = "photos", contentType = "editor-section") {
                    DiaryPhotoSection(
                        photos = photos,
                        imageStorage = imageStorage,
                        onAddPhoto = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPreview = { photo -> previewIndex = photos.indexOf(photo) },
                        onRemove = { photo -> photoRemovalTarget = photo },
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        val selectedDate = dateText.toLocalDateOrNull() ?: LocalDate.now()
        val allowedMonth = loadedDiary?.diary?.diaryDate?.let(YearMonth::from)
            ?: initialDate?.let(YearMonth::from)
            ?: initialYearMonth
            ?: YearMonth.now()
        AppDatePickerDialog(
            selectedDate = selectedDate,
            lockedMonth = allowedMonth,
            latestDate = LocalDate.now(),
            helperText = "${allowedMonth.year}年${allowedMonth.monthValue}月 · 仅过去的日期",
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                dateText = selectedDate.format(dateFormatter)
                showDatePicker = false
            },
        )
    }
    if (showTimePicker) {
        AppTimePickerDialog(
            selectedTime = timeText.toLocalTimeOrNull() ?: LocalTime.now(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { selectedTime ->
                timeText = selectedTime.format(timeFormatter)
                showTimePicker = false
            },
        )
    }
    if (deleteConfirmation) {
        DeleteConfirmationDialog(
            title = "删除这篇日记？",
            message = "删除后将无法恢复，请确认是否继续。",
            onDismiss = { deleteConfirmation = false },
            onConfirm = {
                scope.launch {
                    val paths = repository.deleteDiary(requireNotNull(diaryId))
                    for (path in paths) {
                        imageStorage.delete(path)
                    }
                    onSaved()
                }
            },
        )
    }
    photoRemovalTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { photoRemovalTarget = null },
            title = { Text("移除这张照片？") },
            text = { Text("确认后照片会从当前日记中移除，保存日记后生效。") },
            confirmButton = {
                TextButton(onClick = {
                    photos = photos - target
                    photoRemovalTarget = null
                }) { Text("移除") }
            },
            dismissButton = { TextButton(onClick = { photoRemovalTarget = null }) { Text("取消") } },
        )
    }
    previewIndex?.let { index ->
        ImagePreviewDialog(
            photos = photos,
            initialIndex = index,
            imageStorage = imageStorage,
            onDismiss = { previewIndex = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryEditorTopBar(
    title: String,
    onBack: () -> Unit,
    showDelete: Boolean,
    deleteEnabled: Boolean,
    onDelete: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = DiaryEditorTitle,
            )
        },
        navigationIcon = { AppBackButton(onClick = onBack) },
        actions = {
            if (showDelete) {
                TextButton(onClick = onDelete, enabled = deleteEnabled) {
                    Text(
                        "删除",
                        color = if (deleteEnabled) DiaryEditorDanger else DiaryEditorDanger.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DiaryEditorBackground,
            scrolledContainerColor = DiaryEditorBackground,
        ),
    )
}

@Composable
private fun DiaryDatePickerDialog(
    selectedDate: LocalDate,
    allowedMonth: YearMonth,
    latestDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val calendarCells = remember(allowedMonth) {
        List(allowedMonth.atDay(1).dayOfWeek.value - 1) { null } +
            (1..allowedMonth.lengthOfMonth()).map(allowedMonth::atDay)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFEFFFF),
            shadowElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAF2F8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = DiaryEditorBody,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "选择日期",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryEditorTitle,
                        )
                        Text(
                            "${allowedMonth.year}年${allowedMonth.monthValue}月 · 仅过去的日期",
                            style = MaterialTheme.typography.labelMedium,
                            color = DiaryEditorMuted,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("取消", color = DiaryEditorMuted) }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF4F8FB),
                ) {
                    Text(
                        selectedDate.format(dateFormatter),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryEditorTitle,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    diaryPickerWeekdays.forEach { weekday ->
                        Text(
                            weekday,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = DiaryEditorMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    calendarCells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (date != null) {
                                        val isSelectable = !date.isAfter(latestDate)
                                        val isSelected = date == selectedDate && isSelectable
                                        Surface(
                                            onClick = { onDateSelected(date) },
                                            enabled = isSelectable,
                                            modifier = Modifier.fillMaxSize(),
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFFDCEBFA) else Color.Transparent,
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    date.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> DiaryEditorBlue
                                                        isSelectable -> DiaryEditorBody
                                                        else -> Color(0xFFC7D1D8)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiarySectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DiaryEditorShape,
        colors = CardDefaults.cardColors(containerColor = DiaryEditorCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { content() },
    )
}

@Composable
private fun DiaryTextEditor(value: String, onValueChange: (String) -> Unit) {
    DiarySectionCard {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "写下这一刻",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = DiaryEditorTitle,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                FeatherPenIcon(modifier = Modifier.size(24.dp), color = Color(0xFF7EACE8))
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEF1F4))
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 284.dp),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 254.dp)
                        .padding(bottom = 30.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = DiaryEditorBody,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    ),
                    cursorBrush = SolidColor(DiaryEditorBlue),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                Text(
                                    "写下此刻的想法…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 24.sp,
                                    ),
                                    color = DiaryEditorMuted.copy(alpha = 0.82f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Text(
                    text = "${value.length} / $DiaryContentCharacterLimit",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryEditorMuted.copy(alpha = 0.68f),
                )
            }
        }
    }
}

@Composable
private fun FeatherPenIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val feather = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.83f)
            cubicTo(
                size.width * 0.34f, size.height * 0.34f,
                size.width * 0.68f, size.height * 0.10f,
                size.width * 0.88f, size.height * 0.12f,
            )
            cubicTo(
                size.width * 0.90f, size.height * 0.40f,
                size.width * 0.64f, size.height * 0.72f,
                size.width * 0.18f, size.height * 0.83f,
            )
        }
        drawPath(feather, color = color.copy(alpha = 0.16f))
        drawPath(feather, color = color, style = Stroke(width = 1.7.dp.toPx()))
        drawLine(
            color = color,
            start = Offset(size.width * 0.12f, size.height * 0.91f),
            end = Offset(size.width * 0.70f, size.height * 0.30f),
            strokeWidth = 1.7.dp.toPx(),
        )
    }
}

@Composable
private fun DiaryPhotoSection(
    photos: List<PhotoReference>,
    imageStorage: DiaryImageStorage,
    onAddPhoto: () -> Unit,
    onPreview: (PhotoReference) -> Unit,
    onRemove: (PhotoReference) -> Unit,
) {
    DiarySectionCard(modifier = Modifier.animateContentSize(tween(220))) {
        Column(
            modifier = Modifier.padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "照片",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryEditorTitle,
                )
                OutlinedButton(
                    onClick = onAddPhoto,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DiaryEditorBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text(
                        "添加照片",
                        style = MaterialTheme.typography.labelLarge,
                        color = DiaryEditorBlue,
                    )
                }
            }
            if (photos.isEmpty()) {
                EmptyDiaryPhotoState()
            } else {
                LazyRow(
                    contentPadding = PaddingValues(start = 18.dp, top = 2.dp, end = 22.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = photos,
                        key = { it.key },
                        contentType = { "editor-photo" },
                    ) { photo ->
                        Box(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                )
                                .size(116.dp),
                        ) {
                            AsyncImage(
                                model = photo.model(imageStorage),
                                contentDescription = "日记照片",
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(108.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onPreview(photo) },
                                contentScale = ContentScale.Crop,
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).clickable { onRemove(photo) },
                                shape = CircleShape,
                                color = Color(0xFF2A4151),
                                shadowElevation = 4.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("×", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDiaryPhotoState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .padding(horizontal = 18.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFB8CBD8),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F6FA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = Color(0xFF7C9AB0),
                )
            }
            Text(
                "还没有添加照片",
                style = MaterialTheme.typography.bodyMedium,
                color = DiaryEditorMuted,
            )
        }
    }
}

@Composable
private fun SaveDiaryBar(saving: Boolean, onSave: () -> Unit) {
    Surface(color = Color.Transparent) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (saving) 0.58f else 1f)
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = DiaryEditorBlue.copy(alpha = 0.22f), spotColor = DiaryEditorBlue.copy(alpha = 0.28f))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF68A7F4), Color(0xFF3D7BE5)),
                    ),
                )
                .clickable(enabled = !saving, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (saving) "正在保存…" else "保存日记",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(
    name = "写日记 · 日期与时间",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryEditorScreenPreview() {
    val context = LocalContext.current
    val imageStorage = remember(context) { DiaryImageStorage(context) }

    BlueTheme(dynamicColor = false) {
        Scaffold(
            containerColor = DiaryEditorBackground,
            topBar = {
                DiaryEditorTopBar(
                    title = "写日记",
                    onBack = {},
                    showDelete = false,
                    deleteEnabled = true,
                    onDelete = {},
                )
            },
            bottomBar = {
                SaveDiaryBar(
                    saving = false,
                    onSave = {},
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AppDateTimeSelectorRow(
                        date = "2026-07-19",
                        time = "21:30",
                        onSelectDate = {},
                        onSelectTime = {},
                    )
                }
                item {
                    MoodSelector(selected = setOf(DiaryMoodIds.PLEASANT), onSelected = {})
                }
                item {
                    DiaryTextEditor(
                        value = "今天的晚风很舒服，晚霞很浪漫，我想把这一刻记录下来。",
                        onValueChange = {},
                    )
                }
                item {
                    DiaryPhotoSection(
                        photos = emptyList(),
                        imageStorage = imageStorage,
                        onAddPhoto = {},
                        onPreview = {},
                        onRemove = {},
                    )
                }
            }
        }
    }
}

@Composable
internal fun YearSelector(
    year: Int,
    canMoveForward: Boolean,
    onYearChange: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = DiaryYearSurface,
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onYearChange(year - 1) },
                modifier = Modifier.size(48.dp),
            ) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = DiaryYearFogBlue)
            }
            AnimatedContent(
                targetState = year,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { width -> direction * width / 5 })
                        .togetherWith(
                            fadeOut(tween(160)) +
                                slideOutHorizontally(tween(200)) { width -> -direction * width / 5 },
                        )
                },
                label = "Diary year transition",
            ) { displayedYear ->
                Text(
                    "${displayedYear}年",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryYearText,
                )
            }
            IconButton(
                onClick = { onYearChange(year + 1) },
                enabled = canMoveForward,
                modifier = Modifier.size(48.dp),
            ) {
                Text(
                    "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (canMoveForward) DiaryYearFogBlue else Color(0xFFCBD6DD),
                )
            }
        }
    }
}

@Composable
private fun MonthCard(
    month: Int,
    count: Int,
    lastDate: LocalDate?,
    thumbnail: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "Month card press scale",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 122.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DiaryYearSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A8FE7)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${month}月",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryYearText,
                )
                Text(
                    if (count == 0) "等待这个月的第一段故事" else "$count 篇日记",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiaryYearMuted,
                )
                if (lastDate != null) {
                    Text(
                        "最近记录 ${lastDate.monthValue}月${lastDate.dayOfMonth}日",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF97A8B2),
                    )
                }
            }
            if (thumbnail != null) {
                DiaryFileThumbnail(
                    file = thumbnail,
                    contentDescription = "${month}月日记照片",
                    size = 82.dp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Box(
                    modifier = Modifier.size(82.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF0F5F8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        month.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        color = DiaryYearFogBlue,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryRow(diary: DiaryWithImages, onClick: () -> Unit) {
    val entry = diary.diary
    val wordCount = remember(entry.content) { entry.content.count { !it.isWhitespace() } }
    val firstImage = remember(diary.images) { diary.images.minByOrNull { it.sortOrder } }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().dropShadow(
            shape = DiaryMonthShape,
            shadow = Shadow(radius = 18.dp, color = DiaryMonthShadow, offset = DpOffset(0.dp, 7.dp)),
        ),
        shape = DiaryMonthShape,
        colors = CardDefaults.cardColors(containerColor = DiaryMonthSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().heightIn(min = 138.dp).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val thumbnailSize = (maxWidth * 0.32f).coerceIn(80.dp, 108.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (maxWidth < 280.dp) 10.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${entry.diaryDate.monthValue}月${entry.diaryDate.dayOfMonth}日",
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiaryMonthTitle,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 26.sp, stepSize = 0.5.sp),
                    )
                    Text(
                        text = "${entry.diaryDate.chineseWeekday()} · $wordCount 字 · ${diaryMoodLabels(diary.selectedMoodIds)}",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        color = DiaryMonthMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    HorizontalDivider(thickness = 1.dp, color = DiaryMonthDivider)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = entry.diaryTime.format(timeFormatter),
                            modifier = Modifier.width(46.dp),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.Medium,
                            color = DiaryMonthTitle,
                            maxLines = 1,
                            softWrap = false,
                            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                        )
                        Text(
                            text = entry.content.ifBlank { "这是一篇照片日记" },
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            letterSpacing = 0.sp,
                            color = DiaryMonthMuted,
                        )
                    }
                }
                if (firstImage != null) {
                    DiaryMonthThumbnail(
                        file = File(LocalContext.current.filesDir, firstImage.localPath),
                        size = thumbnailSize,
                        previewTint = listOf(Color(0xFFF2E2D3), Color(0xFFE0E9F0), Color(0xFFF1DFEB))[entry.diaryDate.dayOfMonth % 3],
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryMonthThumbnail(file: File, size: Dp, previewTint: Color) {
    val modifier = Modifier.size(size).clip(RoundedCornerShape(18.dp))
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Brush.linearGradient(listOf(previewTint, DiaryMonthTile))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_image_placeholder),
                contentDescription = "日记照片",
                tint = DiaryMonthMuted.copy(alpha = 0.65f),
                modifier = Modifier.size(34.dp),
            )
        }
    } else {
        DiaryFileThumbnail(
            file = file,
            contentDescription = "日记照片",
            size = size,
            modifier = modifier.background(DiaryMonthTile),
        )
    }
}

@Composable
private fun DiaryFileThumbnail(
    file: File,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val request = remember(file.path, sizePx) {
        ImageRequest.Builder(context)
            .data(file)
            .size(sizePx, sizePx)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun DiaryMonthSummaryCard(
    summary: DiaryPeriodSummary,
    moods: List<DiaryMoodAggregate>,
) {
    val totalMoodCount = moods.sumOf { it.count }
    val primaryMood = moods.maxByOrNull { it.count }
    Card(
        modifier = Modifier.fillMaxWidth().dropShadow(
            shape = DiaryMonthShape,
            shadow = Shadow(radius = 18.dp, color = DiaryMonthShadow, offset = DpOffset(0.dp, 7.dp)),
        ),
        shape = DiaryMonthShape,
        colors = CardDefaults.cardColors(containerColor = DiaryMonthSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "本月小结",
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DiaryMonthTitle,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiaryMonthMetric("记录天数", "${summary.recordDays} 天", R.drawable.ic_calendar, DiaryMonthBlue, Modifier.weight(1f))
                DiaryMonthMetric("日记篇数", "${summary.diaryCount} 篇", R.drawable.ic_home_diary, Color(0xFF687BFF), Modifier.weight(1f))
                DiaryMonthMetric("总字数", "${summary.totalCharacterCount} 字", R.drawable.ic_home_edit, Color(0xFF35B4A3), Modifier.weight(1f))
            }
            HorizontalDivider(color = DiaryMonthDivider)
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiaryMonthMoodBadge(mood = primaryMood?.mood)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.width(56.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "主要心情",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = DiaryMonthMuted,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                    )
                    Text(
                        primaryMood?.let { diaryMoodLabel(it.mood) } ?: "未记录",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryMonthTitle,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 18.sp, stepSize = 0.5.sp),
                    )
                }
                if (totalMoodCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(1.dp).height(24.dp).background(DiaryMonthDivider))
                    Spacer(Modifier.width(12.dp))
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        moods.sortedByDescending { it.count }.forEach { mood ->
                            Text(
                                text = "${diaryMoodLabel(mood.mood)} ${mood.count * 100 / totalMoodCount}%",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.sp,
                                color = DiaryMonthMuted,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryMonthMetric(
    label: String,
    value: String,
    iconRes: Int,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = DiaryMonthTile) {
        Column(
            modifier = Modifier.background(Brush.linearGradient(listOf(DiaryMonthTile, Color(0xFFF5FAFF))))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                color = DiaryMonthMuted,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
            )
            Text(
                text = value,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold,
                color = DiaryMonthTitle,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 19.sp, stepSize = 0.5.sp),
            )
        }
    }
}

@Composable
private fun DiaryMonthMoodBadge(mood: Int?) {
    val positive = mood == DiaryMoodIds.PLEASANT || mood == DiaryMoodIds.ROMANTIC
    val neutral = mood == null || mood == DiaryMoodIds.CALM || mood == DiaryMoodIds.BORED
    val background = when {
        positive -> Color(0xFFE1F5EF)
        neutral -> Color(0xFFEAF2FD)
        else -> Color(0xFFFFE7E6)
    }
    val ink = when {
        positive -> Color(0xFF399C88)
        neutral -> DiaryMonthMuted
        else -> Color(0xFFB25C59)
    }
    Canvas(modifier = Modifier.size(34.dp).clip(CircleShape).background(background)) {
        val stroke = size.width * 0.045f
        drawLine(ink, Offset(size.width * 0.29f, size.height * 0.43f), Offset(size.width * 0.38f, size.height * 0.36f), stroke, StrokeCap.Round)
        drawLine(ink, Offset(size.width * 0.62f, size.height * 0.36f), Offset(size.width * 0.71f, size.height * 0.43f), stroke, StrokeCap.Round)
        val mouth = Path().apply {
            moveTo(size.width * 0.37f, size.height * 0.65f)
            quadraticTo(
                size.width * 0.5f,
                size.height * when { positive -> 0.8f; neutral -> 0.65f; else -> 0.5f },
                size.width * 0.63f,
                size.height * 0.65f,
            )
        }
        drawPath(mouth, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun DiaryDaySection(
    date: LocalDate,
    diaries: List<DiaryWithImages>,
    onOpenDiary: (String) -> Unit,
    onCreateDiary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.animateContentSize(tween(220)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${date.monthValue}月${date.dayOfMonth}日",
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold,
                color = DiaryMonthTitle,
            )
            Spacer(Modifier.width(8.dp))
            Text(date.chineseWeekday(), fontSize = 12.sp, color = DiaryMonthMuted)
            Spacer(Modifier.weight(1f))
            if (diaries.isEmpty()) Text("未记录", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA4B1BA))
        }
        if (diaries.isEmpty()) {
            Surface(
                onClick = onCreateDiary,
                modifier = Modifier.fillMaxWidth().height(82.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF9FBFC),
                border = BorderStroke(1.dp, Color(0xFFE2EAF0)),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("这一天还没有日记", modifier = Modifier.weight(1f), color = DiaryYearMuted)
                    Text("添加 ›", color = DiaryEditorBlue, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            diaries.forEach { diary ->
                key(diary.diary.id) {
                    DiaryRow(diary = diary, onClick = { onOpenDiary(diary.diary.id) })
                }
            }
        }
    }
}

@Composable
private fun MoodSelector(selected: Set<Int>, onSelected: (Set<Int>) -> Unit) {
    DiarySectionCard {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "此日心情",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryEditorTitle,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (selected.isEmpty()) "可多选" else "已选 ${selected.size} 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiaryEditorMuted,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                moodOptions.chunked(5).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = option.value in selected
                            Card(
                                onClick = {
                                    onSelected(
                                        if (isSelected) selected - option.value else selected + option.value,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) DiaryEditorLightBlue else Color.Transparent,
                                ),
                                border = if (isSelected) BorderStroke(1.dp, DiaryEditorBlue.copy(alpha = 0.45f)) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Image(
                                        painter = painterResource(option.imageRes),
                                        contentDescription = "${option.label}心情",
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDiaryMonth(modifier: Modifier = Modifier, message: String = "这个月还没有日记。") {
    Box(modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Text(message, modifier = Modifier.padding(32.dp), color = DiaryYearMuted)
    }
}

@Composable
private fun DiaryMonthTopBar(yearMonth: YearMonth, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(DiaryMonthBackground)
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(44.dp).dropShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 12.dp, color = DiaryMonthShadow, offset = DpOffset(0.dp, 5.dp)),
            ),
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_chevron_right),
                    contentDescription = "返回",
                    tint = DiaryMonthTitle,
                    modifier = Modifier.size(width = 18.dp, height = 26.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }
        }
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月日记",
            modifier = Modifier.weight(1f),
            fontSize = 26.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Bold,
            color = DiaryMonthTitle,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 17.sp, maxFontSize = 26.sp, stepSize = 0.5.sp),
        )
    }
}

@Composable
private fun DiaryMonthFloatingAction(onClick: () -> Unit) {
    val button: @Composable () -> Unit = {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            containerColor = DiaryMonthBlue,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_home_add), contentDescription = "写日记", modifier = Modifier.size(28.dp))
        }
    }
    if (LocalInspectionMode.current) {
        button()
    } else {
        AppAnimatedFloatingAction(content = button)
    }
}

private sealed interface PhotoReference {
    val key: String
    fun model(storage: DiaryImageStorage): Any

    data class Existing(val localPath: String) : PhotoReference {
        override val key = "file:$localPath"
        override fun model(storage: DiaryImageStorage): Any = storage.fileFor(localPath)
    }

    data class New(
        val uri: Uri,
        val selectionId: String = UUID.randomUUID().toString(),
    ) : PhotoReference {
        override val key = "uri:$selectionId"
        override fun model(storage: DiaryImageStorage): Any = uri
    }
}

private val photoReferenceListSaver = listSaver<List<PhotoReference>, String>(
    save = { photos ->
        photos.map { photo ->
            when (photo) {
                is PhotoReference.Existing -> "existing:${Uri.encode(photo.localPath)}"
                is PhotoReference.New -> "new:${photo.selectionId}:${Uri.encode(photo.uri.toString())}"
            }
        }
    },
    restore = { values ->
        values.mapNotNull { value ->
            when {
                value.startsWith("existing:") -> PhotoReference.Existing(
                    Uri.decode(value.removePrefix("existing:")),
                )
                value.startsWith("new:") -> {
                    val payload = value.removePrefix("new:")
                    val separator = payload.indexOf(':')
                    if (separator <= 0) null else PhotoReference.New(
                        uri = Uri.decode(payload.substring(separator + 1)).toUri(),
                        selectionId = payload.substring(0, separator),
                    )
                }
                else -> null
            }
        }
    },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePreviewDialog(
    photos: List<PhotoReference>,
    initialIndex: Int,
    imageStorage: DiaryImageStorage,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(photos.indices),
        pageCount = { photos.size },
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
            ) { page ->
                PreviewImage(model = photos[page].model(imageStorage))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
            ) {
                Text("关闭", color = Color.White)
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${pagerState.currentPage + 1} / ${photos.size}", color = Color.White)
                    if (photos.size > 1) Text("左右滑动切换", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                }
            }
        }
    }
}

@Composable
private fun PreviewImage(model: Any) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = "照片原图",
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 56.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this, dateFormatter)
} catch (_: DateTimeParseException) {
    null
}

internal fun defaultNewDiaryDate(
    targetMonth: YearMonth?,
    today: LocalDate = LocalDate.now(),
): LocalDate = when {
    targetMonth == null -> today
    targetMonth == YearMonth.from(today) -> today
    else -> targetMonth.atDay(1)
}

internal fun diaryMonthsForYear(
    year: Int,
    today: LocalDate = LocalDate.now(),
): List<Int> = when {
    year == today.year -> (today.monthValue downTo 1).toList()
    year < today.year -> (1..12).toList()
    else -> emptyList()
}

private fun String.toLocalTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this, timeFormatter)
} catch (_: DateTimeParseException) {
    null
}

private fun LocalDate.chineseWeekday(): String = when (dayOfWeek.value) {
    1 -> "星期一"
    2 -> "星期二"
    3 -> "星期三"
    4 -> "星期四"
    5 -> "星期五"
    6 -> "星期六"
    else -> "星期日"
}
