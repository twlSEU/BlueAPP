package com.example.blue.feature.diary

import android.graphics.Paint
import android.icu.text.BreakIterator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.data.repository.DiaryMonthAggregate
import com.example.blue.data.repository.DiaryMonthlyMoodAggregate
import com.example.blue.data.repository.DiaryMoodAggregate
import com.example.blue.data.repository.DiaryPeriodSummary
import com.example.blue.data.repository.DiaryRepository
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.FeatureHubTab
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedPeriodSelector
import com.example.blue.feature.common.RefinedSegmentedControl
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.RefinedViolet
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToLong
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

private val SummaryBackground = RefinedBackground
private val SummaryTitle = RefinedInk
private val SummaryBody = Color(0xFF60738D)
private val SummaryMuted = RefinedMuted
private val SummaryBlue = RefinedBlue
private val SummaryPurple = RefinedViolet
private val SummaryGreen = RefinedTeal
private val summaryChartColors = listOf(
    RefinedBlue, RefinedViolet, RefinedTeal,
    Color(0xFFE6B178), Color(0xFFE892A4), Color(0xFF99AFCA),
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

    DiaryYearSummaryContent(
        state = state,
        currentYear = currentYear,
        onYearChange = summaryViewModel::moveToYear,
        onRetry = summaryViewModel::retry,
        onBack = onBack,
        showTopBar = showTopBar,
    )
}

@Composable
private fun DiaryYearSummaryContent(
    state: DiaryYearSummaryUiState,
    currentYear: Int,
    onYearChange: (Int) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean,
) {
    Scaffold(
        containerColor = SummaryBackground,
        topBar = { if (showTopBar) RefinedTopBar("日记年度总结", onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "year-selector") {
                    RefinedPeriodSelector(
                        label = "${state.year}年",
                        canMoveForward = state.year < currentYear,
                        onPrevious = { onYearChange(state.year - 1) },
                        onNext = { onYearChange(state.year + 1) },
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
                                onRetry,
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
}

@Composable
private fun DiaryHeadlineMetrics(state: DiaryYearSummaryUiState) {
    val summary = requireNotNull(state.summary)
    val metrics = remember(summary, state.longestStreak) {
        val totalWords = summary.totalCharacterCount.compactNumber()
        listOf(
            DiaryHeadlineMetric("记录天数", summary.recordDays.toString(), "天"),
            DiaryHeadlineMetric("日记篇数", summary.diaryCount.toString(), "篇"),
            DiaryHeadlineMetric("最长连续", state.longestStreak.toString(), "天"),
            DiaryHeadlineMetric("全年总字数", totalWords.removeSuffix("万"), if (totalWords.endsWith("万")) "万字" else "字"),
            DiaryHeadlineMetric("平均每篇", summary.averageCharacterCount.roundToLong().toString(), "字"),
            DiaryHeadlineMetric("最长一篇", summary.longestCharacterCount.toString(), "字"),
        )
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    DiarySummarySection(title = "这一年的记录") {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val (columnCount, numberStyle) = remember(metrics, maxWidth, density, textMeasurer) {
                val availableWidth = with(density) { maxWidth.toPx() }
                val tilePadding = with(density) { 24.dp.toPx() }
                val tileGap = with(density) { 10.dp.toPx() }
                val unitGap = with(density) { 4.dp.toPx() }
                fun textWidth(text: String, style: TextStyle) = textMeasurer.measure(
                    text = text, style = style, maxLines = 1, softWrap = false,
                ).size.width.toFloat()
                val unitWidths = metrics.map { textWidth(it.unit, DiaryMetricUnitStyle) }
                val minimumTileWidth = metrics.mapIndexed { index, metric ->
                    maxOf(
                        textWidth(metric.label, DiaryMetricLabelStyle),
                        textWidth(metric.value, DiaryMetricNumberStyle.copy(fontSize = 18.sp)) + unitWidths[index] + unitGap,
                    ) + tilePadding
                }.max()
                val columns = ((availableWidth + tileGap) / (minimumTileWidth + tileGap)).toInt().coerceIn(1, 3)
                val valueWidth = (availableWidth - tileGap * (columns - 1)) / columns - tilePadding
                // Fit the longest value once, then use the same size for every metric.
                val fontSize = (44 downTo 24).firstOrNull { halfSp ->
                    val style = DiaryMetricNumberStyle.copy(fontSize = (halfSp / 2f).sp)
                    metrics.indices.all { index ->
                        textWidth(metrics[index].value, style) + unitWidths[index] + unitGap <= valueWidth
                    }
                }?.let { (it / 2f).sp } ?: 12.sp
                columns to DiaryMetricNumberStyle.copy(fontSize = fontSize)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metrics.chunked(columnCount).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { metric ->
                            DiaryMetric(metric, numberStyle, Modifier.weight(1f).fillMaxHeight())
                        }
                        repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private data class DiaryHeadlineMetric(val label: String, val value: String, val unit: String)

private val DiaryMetricLabelStyle = TextStyle(
    fontSize = 11.sp, lineHeight = 17.sp, letterSpacing = 0.sp, color = SummaryMuted,
)
private val DiaryMetricNumberStyle = TextStyle(
    fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", color = SummaryTitle,
)
private val DiaryMetricUnitStyle = TextStyle(
    fontSize = 11.sp, lineHeight = 17.sp, letterSpacing = 0.sp, color = SummaryMuted,
)

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
            RefinedSegmentedControl(
                options = listOf("日记篇数", "文字数量"),
                selectedIndex = if (metric == MonthChartMetric.COUNT) 0 else 1,
                onSelected = { metric = if (it == 0) MonthChartMetric.COUNT else MonthChartMetric.CHARACTERS },
            )
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
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 290.dp || LocalDensity.current.fontScale > 1.2f
                val donut: @Composable () -> Unit = {
                    Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                        DiaryMoodDonut(moods = moods, modifier = Modifier.size(116.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(total.toString(), fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = SummaryTitle)
                            Text("次心情", fontSize = 10.sp, color = SummaryMuted)
                        }
                    }
                }
                val legend: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        moods.sortedByDescending { it.count }.forEachIndexed { index, mood ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).background(summaryChartColors[index % summaryChartColors.size], CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(diaryMoodLabel(mood.mood), modifier = Modifier.weight(1f), fontSize = 13.sp, color = SummaryBody)
                                Text("${mood.count} · ${mood.count * 100 / total}%", fontSize = 12.sp, color = SummaryMuted)
                            }
                        }
                    }
                }
                if (stacked) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        donut()
                        legend(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        donut()
                        legend(Modifier.weight(1f))
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
    val icon = when (title) {
        "这一年的记录" -> R.drawable.ic_home_diary
        "每月记录" -> R.drawable.ic_calendar
        "心情分布", "心情变化趋势" -> R.drawable.ic_mood_smile
        "高频词与词云" -> R.drawable.ic_tag
        else -> R.drawable.ic_home_diary
    }
    RefinedCard(title = title, iconRes = icon, accent = if (title.contains("心情")) SummaryGreen else SummaryBlue) {
        content()
    }
}

@Composable
private fun DiaryMetric(metric: DiaryHeadlineMetric, numberStyle: TextStyle, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                Brush.linearGradient(listOf(SummaryTitle.copy(alpha = 0.045f), SummaryTitle.copy(alpha = 0.025f))),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(metric.label, style = DiaryMetricLabelStyle, maxLines = 1, softWrap = false)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                metric.value, modifier = Modifier.alignByBaseline(), style = numberStyle,
                maxLines = 1, softWrap = false,
            )
            Text(
                metric.unit, modifier = Modifier.alignByBaseline(), style = DiaryMetricUnitStyle,
                maxLines = 1, softWrap = false,
            )
        }
    }
}

@Composable
private fun DiaryMonthlyBarChart(values: List<Long>, color: Color) {
    val maxValue = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
            this.color = SummaryMuted.toArgb()
        }
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(172.dp)) {
        val chartTop = 8.dp.toPx()
        val chartBottom = size.height - 24.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val slot = size.width / 12f
        val barWidth = slot * 0.42f
        repeat(4) { row ->
            val y = chartTop + chartHeight * row / 3f
            drawLine(Color(0xFFEFF3F9), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        values.forEachIndexed { index, value ->
            val height = chartHeight * (value.toFloat() / maxValue.toFloat())
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (value == 0L) color.copy(alpha = 0.10f) else color.copy(alpha = 0.76f),
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
    val sortedMoods = remember(moods) { moods.sortedByDescending { it.count } }
    val total = remember(sortedMoods) { sortedMoods.sumOf { it.count }.coerceAtLeast(1) }
    Canvas(modifier = modifier) {
        var start = -90f
        val inset = 8.dp.toPx()
        sortedMoods.forEachIndexed { index, mood ->
            val sweep = mood.count * 360f / total
            drawArc(
                color = summaryChartColors[index % summaryChartColors.size],
                startAngle = start,
                sweepAngle = (sweep - 3f).coerceAtLeast(0f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round),
            )
            start += sweep
        }
    }
}

@Composable
private fun DiaryMoodLineChart(points: List<Double?>) {
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            color = SummaryMuted.toArgb()
        }
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(164.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val xStep = (right - left) / 11f
        var previous: Offset? = null
        points.forEachIndexed { index, score ->
            val x = left + index * xStep
            drawContext.canvas.nativeCanvas.drawText("${index + 1}", x, size.height - 7.dp.toPx(), labelPaint)
            if (score == null) {
                previous = null
            } else {
                val normalized = ((score - 1.0) / 4.0).coerceIn(0.0, 1.0).toFloat()
                val point = Offset(x, bottom - normalized * (bottom - top))
                previous?.let { drawLine(SummaryGreen, it, point, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round) }
                drawCircle(Color.White, radius = 4.dp.toPx(), center = point)
                drawCircle(SummaryGreen, radius = 2.5.dp.toPx(), center = point)
                previous = point
            }
        }
    }
}

@Composable
private fun DiaryWordCloud(words: List<DiaryWordFrequency>) {
    val maxCount = words.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEachIndexed { index, item ->
            val color = summaryChartColors[index % summaryChartColors.size]
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.07f)) {
                Text(
                    item.word,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = (12f + 10f * item.count / maxCount).sp,
                    fontWeight = if (index < 4) FontWeight.SemiBold else FontWeight.Normal,
                    color = color,
                )
            }
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
    2 -> 1.0 // 伤心
    1 -> 1.4 // 低落
    9 -> 1.6 // 焦虑
    6 -> 1.8 // 愤怒
    8 -> 1.8 // 不适
    7 -> 2.0 // 疲惫
    10 -> 2.2 // 无聊
    3 -> 3.0 // 平静
    4 -> 4.2 // 愉快
    5 -> 5.0 // 恋爱
    else -> 3.0
}

private fun Long.compactNumber(): String = when {
    this >= 100_000 -> "%.1f万".format(Locale.CHINA, this / 10_000.0)
    this >= 10_000 -> "%.2f万".format(Locale.CHINA, this / 10_000.0)
    else -> toString()
}

private fun diarySummaryPreviewState(): DiaryYearSummaryUiState {
    val months = listOf(12, 16, 22, 25, 20, 18, 26, 24, 21, 4).mapIndexed { index, count ->
        DiaryMonthAggregate(
            month = index + 1, recordDays = count.coerceAtMost(22), diaryCount = count,
            totalCharacterCount = count * 286L, lastDiaryDate = null, thumbnailPath = null,
        )
    }
    return DiaryYearSummaryUiState(
        year = 2026, isLoading = false,
        summary = DiaryPeriodSummary(
            recordDays = months.sumOf { it.recordDays }, diaryCount = months.sumOf { it.diaryCount },
            totalCharacterCount = months.sumOf { it.totalCharacterCount }, averageCharacterCount = 286.0,
            longestCharacterCount = 1824,
        ),
        months = months,
        moods = listOf(DiaryMoodAggregate(3, 64), DiaryMoodAggregate(4, 48), DiaryMoodAggregate(7, 32), DiaryMoodAggregate(9, 20)),
        longestStreak = 17,
        monthlyMoods = (1..10).flatMap { month ->
            listOf(DiaryMonthlyMoodAggregate(month, 3, 6), DiaryMonthlyMoodAggregate(month, 4, month + 2), DiaryMonthlyMoodAggregate(month, 7, 12 - month))
        },
        wordFrequencies = listOf("生活", "朋友", "工作", "开心", "今天", "晚饭", "散步", "周末", "音乐", "旅行", "咖啡", "学习")
            .mapIndexed { index, word -> DiaryWordFrequency(word, 48 - index * 3) },
        acceptedWordCount = 1146, isAnalyzingWords = false,
    )
}

@Preview(name = "日记总结 · 标准手机", widthDp = 390, heightDp = 1000, showBackground = true)
@Preview(name = "日记总结 · 窄屏", widthDp = 320, heightDp = 1000, showBackground = true)
@Preview(name = "日记总结 · 大字体", widthDp = 390, heightDp = 1200, fontScale = 1.5f, showBackground = true)
@Composable
private fun DiaryYearSummaryPreview() {
    BlueTheme(darkTheme = false) {
        FeatureHubScreen(
            tabs = listOf(FeatureHubTab("archive", "年月"), FeatureHubTab("browse", "浏览"), FeatureHubTab("summary", "总结")),
            initialPage = 2, accentColor = DiaryBrowseAccent, tabStyle = DiaryBrowseTabStyle,
        ) {
            DiaryYearSummaryContent(diarySummaryPreviewState(), 2026, {}, {}, {}, showTopBar = false)
        }
    }
}

@Preview(name = "年度记录卡片 · 字号与对齐", widthDp = 390, heightDp = 300, showBackground = true)
@Preview(name = "年度记录卡片 · 窄屏", widthDp = 320, heightDp = 400, showBackground = true)
@Preview(name = "年度记录卡片 · 大字体", widthDp = 390, heightDp = 500, fontScale = 1.5f, showBackground = true)
@Composable
private fun DiaryHeadlineMetricsPreview() {
    BlueTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(SummaryBackground).padding(16.dp)) {
            DiaryHeadlineMetrics(diarySummaryPreviewState())
        }
    }
}

@Preview(name = "年度记录卡片 · 长数值与大字体", widthDp = 320, heightDp = 850, fontScale = 1.5f, showBackground = true)
@Composable
private fun DiaryHeadlineLongValuesPreview() {
    val state = diarySummaryPreviewState()
    BlueTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(SummaryBackground).padding(16.dp)) {
            DiaryHeadlineMetrics(
                state.copy(summary = requireNotNull(state.summary).copy(
                    diaryCount = 3650, totalCharacterCount = 9_876_543_210L,
                    averageCharacterCount = 2_705_902.0, longestCharacterCount = 9_876_543,
                )),
            )
        }
    }
}

@Preview(name = "日记总结 · 心情与词云", widthDp = 390, heightDp = 1100, showBackground = true)
@Preview(name = "日记总结 · 心情大字体", widthDp = 320, heightDp = 1300, fontScale = 1.5f, showBackground = true)
@Composable
private fun DiaryYearSummaryChartsPreview() {
    val state = diarySummaryPreviewState()
    BlueTheme(darkTheme = false) {
        LazyColumn(
            Modifier.fillMaxSize().background(SummaryBackground),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DiaryMoodSummaryCard(state.moods) }
            item { DiaryMoodTrendCard(state.monthlyMoods) }
            item { DiaryWordSummaryCard(state) }
        }
    }
}

@Preview(name = "日记总结 · 暂无记录", widthDp = 390, heightDp = 720, showBackground = true)
@Composable
private fun DiaryYearSummaryEmptyPreview() {
    BlueTheme(darkTheme = false) {
        DiaryYearSummaryContent(DiaryYearSummaryUiState(year = 2026, isLoading = false), 2026, {}, {}, {}, showTopBar = true)
    }
}
