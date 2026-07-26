package com.example.blue.feature.diary

import android.graphics.Paint
import android.icu.text.BreakIterator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.data.repository.DiaryMonthAggregate
import com.example.blue.data.repository.DiaryMonthlyMoodAggregate
import com.example.blue.data.repository.DiaryMoodAggregate
import com.example.blue.data.repository.DiaryPeriodSummary
import com.example.blue.data.repository.DiaryRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SummaryBackground = Color(0xFFF6F8FC)
private val SummarySurface = Color(0xFFFEFFFF)
private val SummaryTitle = Color(0xFF2D4555)
private val SummaryBody = Color(0xFF647A88)
private val SummaryMuted = Color(0xFF8798A6)
private val SummaryBlue = Color(0xFF4F88C6)
private val SummaryPurple = Color(0xFF7B77B8)
private val SummaryGreen = Color(0xFF4D9A83)
private val SummaryOrange = Color(0xFFC68B4F)
private val summaryChartColors = listOf(
    Color(0xFF5C8FB6),
    Color(0xFF7B77B8),
    Color(0xFF4D9A83),
    Color(0xFFC68B4F),
    Color(0xFFB56F7D),
    Color(0xFF758896),
)

data class DiaryWordFrequency(val word: String, val count: Int)

data class DiaryYearSummaryUiState(
    val year: Int = LocalDate.now().year,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val summary: DiaryPeriodSummary? = null,
    val months: List<DiaryMonthAggregate> = emptyList(),
    val moods: List<DiaryMoodAggregate> = emptyList(),
    val monthlyMoods: List<DiaryMonthlyMoodAggregate> = emptyList(),
    val longestStreak: Int = 0,
    val wordFrequencies: List<DiaryWordFrequency> = emptyList(),
    val acceptedWordCount: Int = 0,
    val isAnalyzingWords: Boolean = true,
    val analysisMessage: String? = null,
) {
    val hasData: Boolean get() = (summary?.diaryCount ?: 0) > 0
}

class DiaryYearSummaryViewModel(
    private val repository: DiaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiaryYearSummaryUiState())
    val uiState: StateFlow<DiaryYearSummaryUiState> = _uiState.asStateFlow()

    private var aggregateJob: Job? = null
    private var analysisJob: Job? = null

    init {
        loadYear(LocalDate.now().year)
    }

    fun moveToYear(year: Int) {
        if (year == _uiState.value.year || year > LocalDate.now().year) return
        loadYear(year)
    }

    fun retry() = loadYear(_uiState.value.year)

    private fun loadYear(year: Int) {
        aggregateJob?.cancel()
        analysisJob?.cancel()
        _uiState.value = DiaryYearSummaryUiState(year = year)
        aggregateJob = viewModelScope.launch {
            try {
                combine(
                    repository.observeYearSummary(year),
                    repository.observeMonthAggregates(year),
                    repository.observeYearMoodCounts(year),
                    repository.observeMonthlyMoodCounts(year),
                ) { summary, months, moods, monthlyMoods ->
                    DiaryAggregateSnapshot(summary, months, moods, monthlyMoods)
                }.collect { data ->
                    _uiState.update { current ->
                        if (current.year != year) current else current.copy(
                            isLoading = false,
                            errorMessage = null,
                            summary = data.summary,
                            months = data.months,
                            moods = data.moods,
                            monthlyMoods = data.monthlyMoods,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update { current ->
                    if (current.year != year) current else current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "年度统计加载失败",
                    )
                }
            }
        }
        analysisJob = viewModelScope.launch {
            try {
                val dates = repository.getDistinctDiaryDates(year)
                val streak = longestDiaryStreak(dates)
                _uiState.update { current ->
                    if (current.year == year) current.copy(longestStreak = streak) else current
                }
                val words = analyzeDiaryWords(repository, year)
                _uiState.update { current ->
                    if (current.year != year) current else current.copy(
                        longestStreak = streak,
                        wordFrequencies = words.frequencies,
                        acceptedWordCount = words.acceptedWordCount,
                        isAnalyzingWords = false,
                        analysisMessage = if (words.acceptedWordCount < MIN_WORDS_FOR_CLOUD || words.frequencies.size < 3) {
                            "文字样本较少，暂不足以生成可靠词云"
                        } else {
                            null
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { current ->
                    if (current.year != year) current else current.copy(
                        isAnalyzingWords = false,
                        analysisMessage = "词频分析暂时不可用",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: DiaryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DiaryYearSummaryViewModel::class.java))
                    return DiaryYearSummaryViewModel(repository) as T
                }
            }
    }
}

private data class DiaryAggregateSnapshot(
    val summary: DiaryPeriodSummary,
    val months: List<DiaryMonthAggregate>,
    val moods: List<DiaryMoodAggregate>,
    val monthlyMoods: List<DiaryMonthlyMoodAggregate>,
)

private data class DiaryWordAnalysis(
    val frequencies: List<DiaryWordFrequency>,
    val acceptedWordCount: Int,
)

private const val WORD_BATCH_SIZE = 40
private const val MIN_WORDS_FOR_CLOUD = 12
private const val MAX_TRACKED_WORDS = 4_000

private suspend fun analyzeDiaryWords(repository: DiaryRepository, year: Int): DiaryWordAnalysis {
    val counts = HashMap<String, Int>()
    var accepted = 0
    var offset = 0
    while (true) {
        coroutineContext.ensureActive()
        val batch = repository.loadDiaryContentBatch(year, WORD_BATCH_SIZE, offset)
        if (batch.isEmpty()) break
        val batchAccepted = withContext(Dispatchers.Default) {
            var localAccepted = 0
            batch.forEach { item ->
                coroutineContext.ensureActive()
                tokenizeDiaryText(item.content).forEach { word ->
                    counts[word] = (counts[word] ?: 0) + 1
                    localAccepted += 1
                }
            }
            localAccepted
        }
        accepted += batchAccepted
        if (counts.size > MAX_TRACKED_WORDS) {
            val retained = counts.entries.sortedByDescending { it.value }.take(MAX_TRACKED_WORDS / 2)
            counts.clear()
            retained.forEach { counts[it.key] = it.value }
        }
        offset += batch.size
        if (batch.size < WORD_BATCH_SIZE) break
    }
    val frequencies = counts.entries
        .asSequence()
        .filter { it.value >= 2 }
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(50)
        .map { DiaryWordFrequency(it.key, it.value) }
        .toList()
    return DiaryWordAnalysis(frequencies, accepted)
}

private fun tokenizeDiaryText(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val iterator = BreakIterator.getWordInstance(Locale.CHINA)
    iterator.setText(text)
    val result = ArrayList<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val raw = text.substring(start, end).trim().lowercase(Locale.ROOT)
        val normalized = raw.filter { it.isLetter() }
        val codePointCount = normalized.codePointCount(0, normalized.length)
        val isChinese = normalized.any { it.code in 0x3400..0x9FFF }
        val minimumLength = if (isChinese) 2 else 3
        if (
            codePointCount >= minimumLength &&
            normalized !in diaryStopWords &&
            normalized.none(Char::isDigit)
        ) {
            result += normalized
        }
        start = end
        end = iterator.next()
    }
    return result
}

private val diaryStopWords = setOf(
    "今天", "昨天", "明天", "现在", "然后", "因为", "所以", "但是", "还是", "已经", "没有", "一个", "一些",
    "这个", "那个", "自己", "觉得", "感觉", "真的", "就是", "可以", "可能", "非常", "比较", "时候", "事情",
    "我们", "你们", "他们", "她们", "它们", "我的", "你的", "他的", "她的", "以及", "如果", "而且", "不过",
    "the", "and", "that", "this", "with", "from", "have", "was", "were", "are", "for", "but", "not",
)

internal fun longestDiaryStreak(dates: List<LocalDate>): Int {
    val sorted = dates.distinct().sorted()
    if (sorted.isEmpty()) return 0
    var longest = 1
    var current = 1
    for (index in 1 until sorted.size) {
        if (sorted[index] == sorted[index - 1].plusDays(1)) {
            current += 1
            longest = max(longest, current)
        } else {
            current = 1
        }
    }
    return longest
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryYearSummaryScreen(
    repository: DiaryRepository,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val factory = remember(repository) { DiaryYearSummaryViewModel.factory(repository) }
    val summaryViewModel: DiaryYearSummaryViewModel = viewModel(factory = factory)
    val state by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val currentYear = remember { LocalDate.now().year }

    Scaffold(
        containerColor = SummaryBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("日记年度总结", color = SummaryTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { AppBackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SummaryBackground,
                        scrolledContainerColor = SummaryBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "year-selector") {
                YearSelector(
                    year = state.year,
                    canMoveForward = state.year < currentYear,
                    onYearChange = summaryViewModel::moveToYear,
                )
            }
            item(key = "summary-content-${state.year}") {
                AnimatedContent(
                    targetState = state.year,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { width -> direction * width / 7 })
                            .togetherWith(
                                fadeOut(tween(160)) +
                                    slideOutHorizontally(tween(200)) { width -> -direction * width / 7 },
                            )
                    },
                    label = "Diary summary year content",
                ) { displayedYear ->
                    val displayedState = state.takeIf { it.year == displayedYear }
                    when {
                        displayedState == null || displayedState.isLoading -> DiarySummaryLoading()
                        displayedState.errorMessage != null && displayedState.summary == null -> DiarySummaryError(
                            displayedState.errorMessage.orEmpty(),
                            summaryViewModel::retry,
                        )
                        !displayedState.hasData -> DiarySummaryEmpty(displayedYear)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DiaryHeadlineMetrics(displayedState)
                            DiaryMonthlyChartCard(displayedState.months)
                            DiaryMoodSummaryCard(displayedState.moods)
                            DiaryMoodTrendCard(displayedState.monthlyMoods)
                            DiaryWordSummaryCard(displayedState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryHeadlineMetrics(state: DiaryYearSummaryUiState) {
    val summary = requireNotNull(state.summary)
    DiarySummarySection(title = "这一年的记录") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiaryMetric("记录天数", summary.recordDays.toString(), "天", Modifier.weight(1f))
                DiaryMetric("日记篇数", summary.diaryCount.toString(), "篇", Modifier.weight(1f))
                DiaryMetric("最长连续", state.longestStreak.toString(), "天", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DiaryMetric("全年总字数", summary.totalCharacterCount.compactNumber(), "字", Modifier.weight(1f))
                DiaryMetric("平均每篇", summary.averageCharacterCount.roundToLong().toString(), "字", Modifier.weight(1f))
                DiaryMetric("最长一篇", summary.longestCharacterCount.toString(), "字", Modifier.weight(1f))
            }
        }
    }
}

private enum class MonthChartMetric { COUNT, CHARACTERS }

@Composable
private fun DiaryMonthlyChartCard(months: List<DiaryMonthAggregate>) {
    DiaryMonthlyChartContent(months = months)
}

@Composable
private fun DiaryMonthlyChartContent(months: List<DiaryMonthAggregate>) {
    var metric by rememberSaveable { androidx.compose.runtime.mutableStateOf(MonthChartMetric.COUNT) }
    val byMonth = remember(months) { months.associateBy { it.month } }
    val values = remember(months, metric) {
        (1..12).map { month ->
            val aggregate = byMonth[month]
            when (metric) {
                MonthChartMetric.COUNT -> aggregate?.diaryCount?.toLong() ?: 0L
                MonthChartMetric.CHARACTERS -> aggregate?.totalCharacterCount ?: 0L
            }
        }
    }
    DiarySummarySection(title = "每月记录") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = metric == MonthChartMetric.COUNT,
                    onClick = { metric = MonthChartMetric.COUNT },
                    label = { Text("日记篇数") },
                )
                FilterChip(
                    selected = metric == MonthChartMetric.CHARACTERS,
                    onClick = { metric = MonthChartMetric.CHARACTERS },
                    label = { Text("文字数量") },
                )
            }
            DiaryMonthlyBarChart(values = values, color = if (metric == MonthChartMetric.COUNT) SummaryBlue else SummaryPurple)
            val highest = values.indices.maxByOrNull { values[it] } ?: 0
            Text(
                if (values[highest] == 0L) "本年度暂无月度记录" else "${highest + 1}月最高：${values[highest].compactNumber()}${if (metric == MonthChartMetric.COUNT) "篇" else "字"}",
                style = MaterialTheme.typography.bodyMedium,
                color = SummaryMuted,
            )
        }
    }
}

@Composable
private fun DiaryMoodSummaryCard(moods: List<DiaryMoodAggregate>) {
    val total = moods.sumOf { it.count }
    DiarySummarySection(title = "心情分布") {
        if (total == 0) {
            Text("这一年还没有心情记录", color = SummaryMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DiaryMoodDonut(moods = moods, modifier = Modifier.size(132.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    moods.sortedByDescending { it.count }.forEachIndexed { index, mood ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).background(summaryChartColors[index % summaryChartColors.size], CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(diaryMoodLabel(mood.mood), modifier = Modifier.weight(1f), color = SummaryBody)
                            Text("${mood.count} · ${mood.count * 100 / total}%", color = SummaryMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryMoodTrendCard(monthlyMoods: List<DiaryMonthlyMoodAggregate>) {
    val points = remember(monthlyMoods) {
        val grouped = monthlyMoods.groupBy { it.month }
        (1..12).map { month ->
            val values = grouped[month].orEmpty()
            val count = values.sumOf { it.count }
            if (count == 0) null else values.sumOf { diaryMoodScore(it.mood) * it.count } / count
        }
    }
    DiarySummarySection(title = "心情变化趋势") {
        if (points.all { it == null }) {
            Text("心情数据不足，暂时无法绘制趋势", color = SummaryMuted)
        } else {
            DiaryMoodLineChart(points)
            Text("曲线越高代表当月整体心情越轻快；空缺月份不会参与连线。", style = MaterialTheme.typography.bodySmall, color = SummaryMuted)
        }
    }
}

@Composable
private fun DiaryWordSummaryCard(state: DiaryYearSummaryUiState) {
    DiarySummarySection(title = "高频词与词云") {
        when {
            state.isAnalyzingWords -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SummaryPurple)
                Spacer(Modifier.width(10.dp))
                Text("正在后台分批分析文字…", color = SummaryMuted)
            }
            state.analysisMessage != null -> Text(state.analysisMessage, color = SummaryMuted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DiaryWordCloud(state.wordFrequencies.take(18))
                Text(
                    state.wordFrequencies.take(10).joinToString("  ·  ") { "${it.word} ${it.count}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SummaryBody,
                )
                Text("已过滤标点、数字、过短词语与常见停用词。", style = MaterialTheme.typography.bodySmall, color = SummaryMuted)
            }
        }
    }
}

@Composable
private fun DiarySummarySection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SummarySurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SummaryTitle)
            content()
        }
    }
}

@Composable
private fun DiaryMetric(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(17.dp), color = Color(0xFFF3F7FA)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = SummaryMuted, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = SummaryTitle, maxLines = 1)
                Spacer(Modifier.width(3.dp))
                Text(suffix, style = MaterialTheme.typography.labelSmall, color = SummaryMuted, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun DiaryMonthlyBarChart(values: List<Long>, color: Color) {
    val maxValue = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val chartTop = 8.dp.toPx()
        val chartBottom = size.height - 24.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val slot = size.width / 12f
        val barWidth = slot * 0.52f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            this.color = SummaryMuted.toArgb()
        }
        values.forEachIndexed { index, value ->
            val height = chartHeight * (value.toFloat() / maxValue.toFloat())
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (value == 0L) color.copy(alpha = 0.12f) else color.copy(alpha = 0.82f),
                topLeft = Offset(left, chartBottom - max(height, 3.dp.toPx())),
                size = androidx.compose.ui.geometry.Size(barWidth, max(height, 3.dp.toPx())),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            drawContext.canvas.nativeCanvas.drawText("${index + 1}", slot * index + slot / 2f, size.height - 5.dp.toPx(), labelPaint)
        }
    }
}

@Composable
private fun DiaryMoodDonut(moods: List<DiaryMoodAggregate>, modifier: Modifier = Modifier) {
    val total = moods.sumOf { it.count }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        var start = -90f
        moods.sortedByDescending { it.count }.forEachIndexed { index, mood ->
            val sweep = mood.count * 360f / total
            drawArc(
                color = summaryChartColors[index % summaryChartColors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

@Composable
private fun DiaryMoodLineChart(points: List<Double?>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(164.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val xStep = (right - left) / 11f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.CENTER
            color = SummaryMuted.toArgb()
        }
        var previous: Offset? = null
        points.forEachIndexed { index, score ->
            val x = left + index * xStep
            drawContext.canvas.nativeCanvas.drawText("${index + 1}", x, size.height - 7.dp.toPx(), labelPaint)
            if (score == null) {
                previous = null
            } else {
                val normalized = ((score - 1.0) / 4.0).coerceIn(0.0, 1.0).toFloat()
                val point = Offset(x, bottom - normalized * (bottom - top))
                previous?.let { drawLine(SummaryGreen, it, point, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round) }
                drawCircle(SummaryGreen, radius = 4.dp.toPx(), center = point)
                previous = point
            }
        }
    }
}

@Composable
private fun DiaryWordCloud(words: List<DiaryWordFrequency>) {
    val maxCount = words.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val positions = remember {
        listOf(
            0.50f to 0.48f, 0.25f to 0.28f, 0.74f to 0.25f, 0.22f to 0.70f, 0.76f to 0.70f,
            0.48f to 0.16f, 0.50f to 0.82f, 0.10f to 0.47f, 0.90f to 0.48f, 0.36f to 0.34f,
            0.65f to 0.38f, 0.35f to 0.62f, 0.64f to 0.61f, 0.13f to 0.15f, 0.86f to 0.14f,
            0.12f to 0.86f, 0.87f to 0.86f, 0.50f to 0.30f,
        )
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFF4F7FA), RoundedCornerShape(18.dp))) {
        words.forEachIndexed { index, item ->
            if (index >= positions.size) return@forEachIndexed
            val scale = item.count.toFloat() / maxCount
            val textSize = (12f + 13f * scale).sp.toPx()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                color = summaryChartColors[index % summaryChartColors.size].toArgb()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (index < 4) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
            val (x, y) = positions[index]
            drawContext.canvas.nativeCanvas.drawText(item.word, size.width * x, size.height * y, paint)
        }
    }
}

@Composable
private fun DiarySummaryLoading() {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = SummaryBlue)
            Text("正在汇总这一年…", color = SummaryMuted)
        }
    }
}

@Composable
private fun DiarySummaryEmpty(year: Int) {
    DiarySummarySection(title = "${year}年") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp)) {
            Text("这一年还没有日记", style = MaterialTheme.typography.titleLarge, color = SummaryTitle)
            Spacer(Modifier.height(7.dp))
            Text("写下第一篇后，年度总结会在这里出现", color = SummaryMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DiarySummaryError(message: String, onRetry: () -> Unit) {
    DiarySummarySection(title = "加载失败") {
        Text(message, color = SummaryMuted)
        Button(onClick = onRetry) { Text("重试") }
    }
}

private fun diaryMoodScore(mood: Int): Double = when (mood) {
    2 -> 1.0
    1 -> 1.4
    6 -> 1.8
    3 -> 3.0
    4 -> 4.2
    5 -> 5.0
    else -> 3.0
}

private fun Long.compactNumber(): String = when {
    this >= 100_000 -> "%.1f万".format(Locale.CHINA, this / 10_000.0)
    this >= 10_000 -> "%.2f万".format(Locale.CHINA, this / 10_000.0)
    else -> toString()
}
