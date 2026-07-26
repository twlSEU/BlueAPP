package com.example.blue.feature.sleep

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.repository.SleepRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SleepCalendarBackground = Color(0xFFF6F8FC)
private val SleepCalendarSurface = Color(0xFFFEFFFF)
private val SleepCalendarText = Color(0xFF2D4555)
private val SleepCalendarMuted = Color(0xFF748895)
private val SleepCalendarAccent = Color(0xFF637FC3)
private val SleepHeatColors = listOf(
    Color(0xFFDDECE4),
    Color(0xFFCEE3D8),
    Color(0xFFFFE2B8),
    Color(0xFFF5C68E),
    Color(0xFFE99B79),
    Color(0xFFD86F6F),
)
private val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
private val sleepTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private data class SleepPeriodLabel(val key: Long, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSummaryScreen(
    repository: SleepRepository,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    val factory = remember(repository) { SleepSummaryViewModel.factory(repository) }
    val summaryViewModel: SleepSummaryViewModel = viewModel(
        factory = factory,
    )
    val state by summaryViewModel.uiState.collectAsStateWithLifecycle()
    val selection by summaryViewModel.selection.collectAsStateWithLifecycle()
    val period = remember(selection.mode, selection.selectedMonth, selection.selectedYear) {
        if (selection.mode == SleepSummaryMode.MONTH) {
            SleepPeriodLabel(
                key = selection.selectedMonth.year * 12L + selection.selectedMonth.monthValue,
                text = "${selection.selectedMonth.year}年${selection.selectedMonth.monthValue}月",
            )
        } else {
            SleepPeriodLabel(
                key = selection.selectedYear.toLong(),
                text = "${selection.selectedYear}年",
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleepCalendarBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("睡眠总结", fontWeight = FontWeight.SemiBold, color = SleepCalendarText) },
                    navigationIcon = { AppBackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SleepCalendarBackground,
                        scrolledContainerColor = SleepCalendarBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            SleepModeSegment(
                selected = selection.mode,
                onSelected = summaryViewModel::setMode,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            SleepPeriodSelector(
                period = period,
                onPrevious = summaryViewModel::previousPeriod,
                onNext = summaryViewModel::nextPeriod,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            AnimatedContent(
                targetState = selection.mode,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "Sleep summary mode",
            ) { mode ->
                when (val contentState = state) {
                    SleepSummaryUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SleepCalendarAccent)
                        }
                    }
                    is SleepSummaryUiState.Error -> {
                        SummaryErrorState(contentState.message, summaryViewModel::retry)
                    }
                    is SleepSummaryUiState.Ready -> {
                        if (mode == SleepSummaryMode.MONTH && contentState.mode == SleepSummaryMode.MONTH) {
                            SleepMonthlyContent(contentState, onOpenDay)
                        } else if (contentState.mode == SleepSummaryMode.YEAR) {
                            SleepAnnualContent(
                                year = contentState.selectedYear,
                                records = contentState.records,
                                onOpenDay = onOpenDay,
                                onOpenMonth = summaryViewModel::openMonth,
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SleepCalendarAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepModeSegment(
    selected: SleepSummaryMode,
    onSelected: (SleepSummaryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE8EDF4),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            SleepModeButton(
                label = "月度",
                selected = selected == SleepSummaryMode.MONTH,
                onClick = { onSelected(SleepSummaryMode.MONTH) },
                modifier = Modifier.weight(1f),
            )
            SleepModeButton(
                label = "年度",
                selected = selected == SleepSummaryMode.YEAR,
                onClick = { onSelected(SleepSummaryMode.YEAR) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SleepModeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) SleepCalendarText else SleepCalendarMuted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SleepPeriodSelector(
    period: SleepPeriodLabel,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Text("‹", style = MaterialTheme.typography.headlineMedium, color = SleepCalendarAccent)
        }
        AnimatedContent(
            targetState = period,
            transitionSpec = {
                val direction = if (targetState.key > initialState.key) 1 else -1
                (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { width -> direction * width / 4 })
                    .togetherWith(
                        fadeOut(tween(160)) +
                            slideOutHorizontally(tween(200)) { width -> -direction * width / 4 },
                    )
            },
            label = "Sleep period",
        ) { displayedPeriod ->
            Text(
                displayedPeriod.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = SleepCalendarText,
            )
        }
        TextButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Text("›", style = MaterialTheme.typography.headlineMedium, color = SleepCalendarAccent)
        }
    }
}

@Composable
private fun SleepMonthlyContent(
    state: SleepSummaryUiState.Ready,
    onOpenDay: (LocalDate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "calendar") {
            SleepMonthCalendar(
                yearMonth = state.selectedMonth,
                records = state.records,
                onOpenDay = onOpenDay,
            )
        }
        item(key = "legend") { SleepHeatLegend() }
        if (state.records.isEmpty()) {
            item(key = "empty") {
                SleepInfoCard(
                    title = "这个月还没有记录",
                    body = "点击日历中的日期即可开始记录。",
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(180),
                        placementSpec = tween(220),
                        fadeOutSpec = tween(180),
                    ),
                )
            }
        }
        item(key = "stats") { SleepMonthSummaryCard(state.monthStatistics) }
    }
}

@Composable
private fun SleepMonthCalendar(
    yearMonth: YearMonth,
    records: List<SleepRecordEntity>,
    onOpenDay: (LocalDate) -> Unit,
) {
    val byDate = remember(records) { records.associateBy { it.recordDate } }
    val today = remember { LocalDate.now() }
    val cells = remember(yearMonth) {
        val blanks = List(yearMonth.atDay(1).dayOfWeek.value - 1) { null }
        (blanks + (1..yearMonth.lengthOfMonth()).map(yearMonth::atDay)).padCalendarCells()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCalendarSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth()) {
                weekLabels.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = SleepCalendarMuted,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).aspectRatio(0.76f))
                        } else {
                            SleepCalendarDayCell(
                                date = date,
                                record = byDate[date],
                                today = today,
                                onClick = { onOpenDay(date) },
                                modifier = Modifier.weight(1f).aspectRatio(0.76f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepCalendarDayCell(
    date: LocalDate,
    record: SleepRecordEntity?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val future = date.isAfter(today)
    val level = record?.sleepDateTime?.toLocalTime()?.let(SleepDateRules::latenessLevel)
    val background = level?.let(SleepHeatColors::get) ?: Color.Transparent
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(background, RoundedCornerShape(10.dp))
            .clickable(enabled = !future, onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 1.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (future) Color(0xFFC6D0D7) else SleepCalendarText,
            )
            record?.let {
                Text(
                    it.sleepDateTime.toLocalTime().format(sleepTimeFormatter),
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = if (level != null && level >= 4) Color.White else SleepCalendarText,
                )
            }
        }
    }
}

@Composable
private fun SleepMonthSummaryCard(stats: SleepMonthStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCalendarSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("本月总结", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
            SleepMetricRow("记录天数", "${stats.recordedDays} 天", "平均睡觉时间", stats.averageBedtime.displayTime())
            SleepMetricRow("最早睡觉", stats.earliestBedtime.displayTime(), "最晚睡觉", stats.latestBedtime.displayTime())
            SleepMetricRow("熬夜天数", "${stats.lateNightDays} 天", "判定标准", "00:00 后")
        }
    }
}

@Composable
private fun SleepMetricRow(labelA: String, valueA: String, labelB: String, valueB: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SleepMetric(labelA, valueA, Modifier.weight(1f))
        SleepMetric(labelB, valueB, Modifier.weight(1f))
    }
}

@Composable
private fun SleepMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color(0xFFF3F6FA), RoundedCornerShape(16.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SleepCalendarMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
    }
}

@Composable
private fun SleepAnnualContent(
    year: Int,
    records: List<SleepRecordEntity>,
    onOpenDay: (LocalDate) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val byMonth = remember(records) { records.groupBy { YearMonth.from(it.recordDate) } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "annual-note") {
            SleepInfoCard(
                title = if (records.isEmpty()) "这一年还没有记录" else "全年记录 ${records.size} 天",
                body = "颜色等级与月度日历一致；点击月份查看大日历，点击日期直接编辑。",
            )
        }
        item(key = "annual-legend") { SleepHeatLegend() }
        items((1..12).toList(), key = { month -> "$year-$month" }) { month ->
            val yearMonth = YearMonth.of(year, month)
            AnnualMonthHeatmap(
                yearMonth = yearMonth,
                records = byMonth[yearMonth].orEmpty(),
                onOpenDay = onOpenDay,
                onOpenMonth = { onOpenMonth(yearMonth) },
            )
        }
    }
}

@Composable
private fun AnnualMonthHeatmap(
    yearMonth: YearMonth,
    records: List<SleepRecordEntity>,
    onOpenDay: (LocalDate) -> Unit,
    onOpenMonth: () -> Unit,
) {
    val byDate = remember(records) { records.associateBy { it.recordDate } }
    val cells = remember(yearMonth) {
        (List(yearMonth.atDay(1).dayOfWeek.value - 1) { null } +
            (1..yearMonth.lengthOfMonth()).map(yearMonth::atDay)).padCalendarCells()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCalendarSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMonth).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${yearMonth.monthValue}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
                Spacer(Modifier.weight(1f))
                Text("${records.size} 天  ›", style = MaterialTheme.typography.bodySmall, color = SleepCalendarMuted)
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val record = byDate[date]
                            val level = record?.sleepDateTime?.toLocalTime()?.let(SleepDateRules::latenessLevel)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(level?.let(SleepHeatColors::get) ?: Color(0xFFF1F4F6), RoundedCornerShape(6.dp))
                                    .clickable(enabled = !date.isAfter(LocalDate.now())) { onOpenDay(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontSize = 9.sp,
                                    color = if (level != null && level >= 4) Color.White else SleepCalendarText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepHeatLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleepCalendarSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("入睡时间等级", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("23前", "23–00", "00–01", "01–02", "02–03", "03后").forEachIndexed { index, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(18.dp).background(SleepHeatColors[index], RoundedCornerShape(5.dp)))
                        Text(label, fontSize = 9.sp, color = SleepCalendarMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5FA)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
            Text(body, style = MaterialTheme.typography.bodySmall, color = SleepCalendarMuted)
        }
    }
}

@Composable
private fun SummaryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, color = SleepCalendarMuted)
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 14.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleepCalendarAccent),
        ) { Text("重新加载") }
    }
}

private fun List<LocalDate?>.padCalendarCells(): List<LocalDate?> =
    this + List((7 - size % 7) % 7) { null }

private fun LocalTime?.displayTime(): String = this?.format(sleepTimeFormatter) ?: "—"
