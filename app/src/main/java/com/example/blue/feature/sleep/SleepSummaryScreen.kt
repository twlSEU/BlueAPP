package com.example.blue.feature.sleep

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.repository.SleepRepository
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMetric
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedPeriodSelector
import com.example.blue.feature.common.RefinedSegmentedControl
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.RefinedViolet
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SleepCalendarBackground = RefinedBackground
private val SleepCalendarText = RefinedInk
private val SleepCalendarMuted = RefinedMuted
private val SleepCalendarAccent = RefinedViolet
private val SleepHeatColors = listOf(
    Color(0xFFDCF3E8),
    Color(0xFFC9ECDD),
    Color(0xFFFFF2C9),
    Color(0xFFFFE5CE),
    Color(0xFFFFDBCF),
    Color(0xFFF6CBD4),
)
private val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
private val sleepTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
    SleepSummaryContent(
        selection = selection, state = state,
        onSetMode = summaryViewModel::setMode,
        onPrevious = summaryViewModel::previousPeriod, onNext = summaryViewModel::nextPeriod,
        onRetry = summaryViewModel::retry, onOpenMonth = summaryViewModel::openMonth,
        onOpenDay = onOpenDay, onBack = onBack, modifier = modifier, showTopBar = showTopBar,
    )
}

@Composable
private fun SleepSummaryContent(
    selection: SleepSummarySelection,
    state: SleepSummaryUiState,
    onSetMode: (SleepSummaryMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean,
) {
    val period = if (selection.mode == SleepSummaryMode.MONTH) {
        "${selection.selectedMonth.year}年${selection.selectedMonth.monthValue}月"
    } else {
        "${selection.selectedYear}年"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleepCalendarBackground,
        topBar = { if (showTopBar) RefinedTopBar("睡眠总结", onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            ) {
                SleepModeSegment(
                    selected = selection.mode,
                    onSelected = onSetMode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                RefinedPeriodSelector(
                    label = period,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
                // Keep only the active calendar tree composed when switching modes.
                Box(modifier = Modifier.weight(1f)) {
                    when (val contentState = state) {
                        SleepSummaryUiState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SleepCalendarAccent)
                            }
                        }
                        is SleepSummaryUiState.Error -> {
                            SummaryErrorState(contentState.message, onRetry)
                        }
                        is SleepSummaryUiState.Ready -> {
                            if (
                                selection.mode == SleepSummaryMode.MONTH &&
                                contentState.mode == SleepSummaryMode.MONTH
                            ) {
                                SleepMonthlyContent(contentState, onOpenDay)
                            } else if (contentState.mode == SleepSummaryMode.YEAR) {
                                SleepAnnualContent(
                                    year = contentState.selectedYear,
                                    records = contentState.records,
                                    onOpenDay = onOpenDay,
                                    onOpenMonth = onOpenMonth,
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
}

@Composable
private fun SleepModeSegment(
    selected: SleepSummaryMode,
    onSelected: (SleepSummaryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    RefinedSegmentedControl(
        options = listOf("月度", "年度"),
        selectedIndex = if (selected == SleepSummaryMode.MONTH) 0 else 1,
        onSelected = { onSelected(if (it == 0) SleepSummaryMode.MONTH else SleepSummaryMode.YEAR) },
        modifier = modifier,
    )
}

@Composable
private fun SleepMonthlyContent(
    state: SleepSummaryUiState.Ready,
    onOpenDay: (LocalDate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "stats", contentType = "statistics") { SleepMonthSummaryCard(state.monthStatistics) }
        item(key = "calendar", contentType = "calendar") {
            SleepMonthCalendar(
                yearMonth = state.selectedMonth,
                records = state.records,
                onOpenDay = onOpenDay,
            )
        }
        item(key = "legend", contentType = "legend") { SleepHeatLegend() }
        if (state.records.isEmpty()) {
            item(key = "empty", contentType = "status") {
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
    val cellAspectRatio = if (LocalDensity.current.fontScale > 1.2f) 0.68f else 0.9f
    RefinedCard(title = "入睡日历", iconRes = R.drawable.ic_calendar, accent = SleepCalendarAccent) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Spacer(Modifier.weight(1f).aspectRatio(cellAspectRatio))
                        } else {
                            SleepCalendarDayCell(
                                date = date,
                                record = byDate[date],
                                today = today,
                                onClick = { onOpenDay(date) },
                                modifier = Modifier.weight(1f).aspectRatio(cellAspectRatio),
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
    val background = level?.let(SleepHeatColors::get) ?: Color(0xFFF3F6FB)
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (date == today) Modifier.border(1.dp, SleepCalendarAccent.copy(alpha = 0.6f), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(enabled = !future, onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 1.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                date.dayOfMonth.toString(),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp, stepSize = 0.5.sp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = if (future) Color(0xFFC6D0D7) else SleepCalendarText,
            )
            record?.let {
                Text(
                    it.sleepDateTime.toLocalTime().format(sleepTimeFormatter),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 9.sp, lineHeight = 12.sp,
                    maxLines = 1, softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 9.sp, stepSize = 0.5.sp),
                    textAlign = TextAlign.Center, color = SleepCalendarText,
                )
            }
        }
    }
}

@Composable
private fun SleepMonthSummaryCard(stats: SleepMonthStatistics) {
    RefinedCard(title = "本月总结", iconRes = R.drawable.ic_sleep, accent = SleepCalendarAccent) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("平均睡觉时间", fontSize = 12.sp, color = SleepCalendarMuted)
            Text(
                stats.averageBedtime.displayTime(),
                modifier = Modifier.fillMaxWidth(), fontSize = 36.sp, lineHeight = 44.sp,
                fontWeight = FontWeight.SemiBold, color = SleepCalendarAccent,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 36.sp, stepSize = 0.5.sp),
            )
        }
        SleepMetricRow("记录天数", "${stats.recordedDays} 天", "熬夜天数", "${stats.lateNightDays} 天")
        SleepMetricRow("最早睡觉", stats.earliestBedtime.displayTime(), "最晚睡觉", stats.latestBedtime.displayTime())
        Text("熬夜判定标准 · 00:00 后", fontSize = 11.sp, lineHeight = 17.sp, color = SleepCalendarMuted)
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
    RefinedMetric(label, value, modifier, SleepCalendarText)
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
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "annual-note", contentType = "status") {
            SleepInfoCard(
                title = if (records.isEmpty()) "这一年还没有记录" else "全年记录 ${records.size} 天",
                body = "颜色等级与月度日历一致；点击月份查看大日历，点击日期直接编辑。",
            )
        }
        item(key = "annual-legend", contentType = "legend") { SleepHeatLegend() }
        items(
            count = 12,
            key = { monthIndex -> "$year-${monthIndex + 1}" },
            contentType = { "annual-month" },
        ) { monthIndex ->
            val month = monthIndex + 1
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
    RefinedCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMonth).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${yearMonth.monthValue}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SleepCalendarText)
                Spacer(Modifier.weight(1f))
                Text("${records.size} 天", fontSize = 12.sp, color = SleepCalendarMuted)
                Icon(painterResource(R.drawable.ic_home_chevron_right), null, Modifier.padding(start = 10.dp).size(width = 8.dp, height = 14.dp), tint = SleepCalendarMuted)
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(level?.let(SleepHeatColors::get) ?: Color(0xFFF3F6FB))
                                    .clickable(enabled = !date.isAfter(LocalDate.now())) { onOpenDay(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 9.sp, maxLines = 1, softWrap = false,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 9.sp, stepSize = 0.5.sp),
                                    textAlign = TextAlign.Center,
                                    color = if (date.isAfter(LocalDate.now())) SleepCalendarMuted.copy(alpha = 0.4f) else SleepCalendarText,
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
    RefinedCard(title = "入睡时间等级", iconRes = R.drawable.ic_clock, accent = SleepCalendarAccent) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("23前", "23–00", "00–01", "01–02", "02–03", "03后").forEachIndexed { index, label ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(16.dp).background(SleepHeatColors[index], RoundedCornerShape(5.dp)))
                    Text(
                        label, modifier = Modifier.fillMaxWidth(), fontSize = 9.sp, lineHeight = 14.sp,
                        textAlign = TextAlign.Center, color = SleepCalendarMuted, maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 9.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepInfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    RefinedCard(modifier = modifier, title = title, iconRes = R.drawable.ic_sleep, accent = SleepCalendarAccent) {
        Text(body, fontSize = 12.sp, lineHeight = 19.sp, color = SleepCalendarMuted)
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
            colors = ButtonDefaults.buttonColors(containerColor = SleepCalendarAccent.copy(alpha = 0.10f), contentColor = SleepCalendarAccent),
        ) { Text("重新加载") }
    }
}

private fun List<LocalDate?>.padCalendarCells(): List<LocalDate?> =
    this + List((7 - size % 7) % 7) { null }

private fun LocalTime?.displayTime(): String = this?.format(sleepTimeFormatter) ?: "—"

private fun sleepSummaryPreviewState(mode: SleepSummaryMode, empty: Boolean = false): SleepSummaryUiState.Ready {
    val month = YearMonth.of(2026, 9)
    val times = listOf(LocalTime.of(22, 40), LocalTime.of(23, 20), LocalTime.of(0, 25), LocalTime.of(1, 10), LocalTime.of(2, 15), LocalTime.of(3, 10))
    val months = if (mode == SleepSummaryMode.MONTH) listOf(9) else (1..9).toList()
    val records = if (empty) emptyList() else months.flatMap { monthValue ->
        (1..24).filter { it % 5 != 0 }.map { day ->
            val date = YearMonth.of(2026, monthValue).atDay(day)
            SleepRecordEntity(
                id = "preview-$date", recordDate = date,
                sleepDateTime = SleepDateRules.sleepDateTimeFor(date, times[(day + monthValue) % times.size]),
                wakeDateTime = null, source = SleepSource.MANUAL_CONFIRMED, isEstimated = false,
                note = null, createdAt = 0L, updatedAt = 0L,
            )
        }
    }
    val bedtimes = records.map { it.sleepDateTime.toLocalTime() }
    return SleepSummaryUiState.Ready(
        mode, month, 2026, records,
        SleepMonthStatistics(
            recordedDays = records.size, averageBedtime = SleepDateRules.averageBedtime(bedtimes),
            earliestBedtime = bedtimes.minByOrNull(SleepDateRules::continuousMinutes),
            latestBedtime = bedtimes.maxByOrNull(SleepDateRules::continuousMinutes),
            lateNightDays = bedtimes.count(SleepDateRules::isLateNight),
        ),
    )
}

@Composable
private fun SleepSummaryPreviewHost(mode: SleepSummaryMode, empty: Boolean = false) {
    val state = sleepSummaryPreviewState(mode, empty)
    BlueTheme(darkTheme = false) {
        FeatureHubScreen(tabs = SleepHubTabs, initialPage = 1, accentColor = SleepArchiveAccent, tabStyle = SleepArchiveTabStyle) {
            SleepSummaryContent(
                selection = SleepSummarySelection(mode, state.selectedMonth, state.selectedYear), state = state,
                onSetMode = {}, onPrevious = {}, onNext = {}, onRetry = {}, onOpenMonth = {},
                onOpenDay = {}, onBack = {}, showTopBar = false,
            )
        }
    }
}

@Preview(name = "睡眠总结 · 月度", widthDp = 390, heightDp = 1100, showBackground = true)
@Preview(name = "睡眠总结 · 月度窄屏", widthDp = 320, heightDp = 1100, showBackground = true)
@Preview(name = "睡眠总结 · 月度大字体", widthDp = 390, heightDp = 1350, fontScale = 1.5f, showBackground = true)
@Composable
private fun SleepMonthlySummaryPreview() {
    SleepSummaryPreviewHost(SleepSummaryMode.MONTH)
}

@Preview(name = "睡眠总结 · 年度", widthDp = 390, heightDp = 1100, showBackground = true)
@Preview(name = "睡眠总结 · 年度大字体", widthDp = 320, heightDp = 1200, fontScale = 1.5f, showBackground = true)
@Composable
private fun SleepAnnualSummaryPreview() {
    SleepSummaryPreviewHost(SleepSummaryMode.YEAR)
}

@Preview(name = "睡眠总结 · 暂无记录", widthDp = 390, heightDp = 1200, showBackground = true)
@Composable
private fun SleepEmptySummaryPreview() {
    SleepSummaryPreviewHost(SleepSummaryMode.MONTH, empty = true)
}
