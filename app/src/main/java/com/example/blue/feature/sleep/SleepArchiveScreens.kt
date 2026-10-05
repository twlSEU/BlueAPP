package com.example.blue.feature.sleep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blue.R
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.repository.SleepRepository
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.map

private val ArchiveBackground = Color(0xFFF4F8FD)
private val ArchiveSurface = Color(0xFFFEFFFF)
private val ArchiveText = Color(0xFF0B1F42)
private val ArchiveMuted = Color(0xFF8499B7)
private val ArchiveAccent = Color(0xFF4D82F6)
private val ArchiveDivider = Color(0xFFDCE7F6)
private val ArchiveShadow = Color(0x146889BD)
private val ArchiveCardShape = RoundedCornerShape(24.dp)
private val ArchiveBedtimeBackgrounds = listOf(
    Color(0xFFE7F6EF), Color(0xFFE1F3EC), Color(0xFFFFF6E3),
    Color(0xFFFFF0E5), Color(0xFFFFEBE3), Color(0xFFFCE8E9),
)
private val ArchiveBedtimeColors = listOf(
    Color(0xFF359371), Color(0xFF288967), Color(0xFFB68936),
    Color(0xFFCD752E), Color(0xFFDD511C), Color(0xFFCE5260),
)
private val archiveTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SleepArchiveYearScreen(
    repository: SleepRepository,
    onOpenMonth: (Int, Int) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val today = remember { LocalDate.now() }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    val recordsFlow = remember(repository, year) {
        repository.observeYear(year).map<List<SleepRecordEntity>, List<SleepRecordEntity>?> { it }
    }
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = null)
    SleepArchiveYearContent(
        year = year,
        today = today,
        records = records,
        onPreviousYear = { year-- },
        onNextYear = { if (year < today.year) year++ },
        onOpenMonth = onOpenMonth,
        onBack = onBack,
        showTopBar = showTopBar,
    )
}

@Composable
fun SleepArchiveMonthScreen(
    repository: SleepRepository,
    year: Int,
    month: Int,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val yearMonth = remember(year, month) { YearMonth.of(year, month) }
    val today = remember { LocalDate.now() }
    val recordsFlow = remember(repository, yearMonth) {
        repository.observeMonth(yearMonth).map<List<SleepRecordEntity>, List<SleepRecordEntity>?> { it }
    }
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = null)
    SleepArchiveMonthContent(
        yearMonth = yearMonth,
        today = today,
        records = records,
        onOpenDay = onOpenDay,
        onBack = onBack,
    )
}

@Composable
private fun SleepArchiveMonthContent(
    yearMonth: YearMonth,
    today: LocalDate,
    records: List<SleepRecordEntity>?,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val byDate = remember(records) { records.orEmpty().associateBy { it.recordDate } }
    val days = remember(yearMonth, today) {
        val lastDay = if (yearMonth == YearMonth.from(today)) today.dayOfMonth else yearMonth.lengthOfMonth()
        if (yearMonth > YearMonth.from(today)) emptyList() else (lastDay downTo 1).map(yearMonth::atDay)
    }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = ArchiveBackground,
        topBar = { SleepArchiveMonthTopBar(yearMonth = yearMonth, onBack = onBack) },
    ) { padding ->
        if (records == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArchiveAccent)
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().align(Alignment.TopCenter),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "summary", contentType = "month-summary") {
                        SleepArchiveMonthSummary(records.orEmpty())
                    }
                    items(days, key = LocalDate::toString, contentType = { "day-record" }) { date ->
                        SleepArchiveDayCard(
                            date = date,
                            record = byDate[date],
                            onClick = { onOpenDay(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepArchiveMonthTopBar(yearMonth: YearMonth, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(ArchiveBackground)
            .statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 56.dp),
            fontSize = 26.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Bold,
            color = ArchiveText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp, stepSize = 0.5.sp),
        )
        Surface(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).size(48.dp).dropShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 12.dp, color = ArchiveShadow, offset = DpOffset(0.dp, 4.dp)),
            ),
            shape = CircleShape,
            color = ArchiveSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_chevron_right),
                    contentDescription = "返回",
                    tint = ArchiveText,
                    modifier = Modifier.size(width = 18.dp, height = 26.dp).graphicsLayer { rotationZ = 180f },
                )
            }
        }
    }
}

@Composable
private fun SleepArchiveMonthSummary(records: List<SleepRecordEntity>) {
    val times = remember(records) { records.map { it.sleepDateTime.toLocalTime() } }
    val average = remember(times) { SleepDateRules.averageBedtime(times) }
    val late = remember(times) { times.count(SleepDateRules::isLateNight) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).dropShadow(
            shape = ArchiveCardShape,
            shadow = Shadow(radius = 16.dp, color = ArchiveShadow, offset = DpOffset(0.dp, 5.dp)),
        ),
        shape = ArchiveCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFFFBFDFF), Color(0xFFEDF5FF))),
            ).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("本月概览", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = ArchiveText)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ArchiveMetric("记录天数", "${records.size} 天", Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(36.dp).background(ArchiveDivider))
                ArchiveMetric("平均入睡", average.displayArchiveTime(), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(36.dp).background(ArchiveDivider))
                ArchiveMetric("熬夜天数", "$late 天", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ArchiveMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 23.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Bold,
            color = ArchiveText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 23.sp, stepSize = 0.5.sp),
        )
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            color = ArchiveMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
        )
    }
}

@Composable
private fun SleepArchiveDayCard(date: LocalDate, record: SleepRecordEntity?, onClick: () -> Unit) {
    val bedtimeLevel = record?.sleepDateTime?.toLocalTime()?.let(SleepDateRules::latenessLevel)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().dropShadow(
            shape = ArchiveCardShape,
            shadow = Shadow(radius = 14.dp, color = ArchiveShadow, offset = DpOffset(0.dp, 4.dp)),
        ),
        shape = ArchiveCardShape,
        colors = CardDefaults.cardColors(containerColor = ArchiveSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier.size(54.dp).background(
                    color = bedtimeLevel?.let { ArchiveBedtimeBackgrounds[it] } ?: Color(0xFFEDF3FA),
                    shape = RoundedCornerShape(14.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = bedtimeLevel?.let { ArchiveBedtimeColors[it] } ?: ArchiveText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp, stepSize = 0.5.sp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${date.monthValue}月${date.dayOfMonth}日",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArchiveText,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 17.sp, stepSize = 0.5.sp),
                )
                Text(
                    record?.let { "${it.sleepDateTime.toLocalTime().format(archiveTimeFormatter)} 入睡${it.wakeDateTime?.let { wake -> " · ${wake.toLocalTime().format(archiveTimeFormatter)} 起床" }.orEmpty()}" }
                        ?: "未记录 · 点击添加",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = ArchiveMuted,
                )
                record?.let {
                    Text(
                        when (it.source) {
                            SleepSource.MANUAL -> "手动记录"
                            SleepSource.SYSTEM_ESTIMATE -> "系统推测"
                            SleepSource.MANUAL_CONFIRMED -> "手动确认"
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp,
                        color = if (it.isEstimated) Color(0xFFB87B50) else ArchiveAccent,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_home_chevron_right),
                contentDescription = null,
                tint = ArchiveMuted,
                modifier = Modifier.size(width = 12.dp, height = 20.dp),
            )
        }
    }
}

private fun LocalTime?.displayArchiveTime(): String = this?.format(archiveTimeFormatter) ?: "—"

@Preview(name = "睡眠 · 月份 · 设计", group = "睡眠月份", showBackground = true, widthDp = 390, heightDp = 690)
@Preview(name = "睡眠 · 月份 · 手机", group = "睡眠月份", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "睡眠 · 月份 · 窄屏", group = "睡眠月份", showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 800)
@Preview(name = "睡眠 · 月份 · 大字体", group = "睡眠月份", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun SleepArchiveMonthPreview() {
    val yearMonth = YearMonth.of(2026, 10)
    val records = remember {
        listOf(LocalTime.of(3, 6), LocalTime.of(2, 12), LocalTime.of(2, 12), LocalTime.of(2, 10))
            .mapIndexed { index, bedtime ->
                val date = yearMonth.atDay(index + 1)
                SleepRecordEntity(
                    id = "preview-$date",
                    recordDate = date,
                    sleepDateTime = SleepDateRules.sleepDateTimeFor(date, bedtime),
                    wakeDateTime = null,
                    source = SleepSource.MANUAL_CONFIRMED,
                    isEstimated = false,
                    note = null,
                    createdAt = 0L,
                    updatedAt = 0L,
                )
            }
    }
    BlueTheme(darkTheme = false) {
        SleepArchiveMonthContent(
            yearMonth = yearMonth,
            today = yearMonth.atDay(6),
            records = records,
            onOpenDay = {},
            onBack = {},
        )
    }
}

@Preview(name = "睡眠 · 月份 · 无记录", group = "睡眠月份", showBackground = true, widthDp = 390, heightDp = 690)
@Composable
private fun SleepArchiveMonthEmptyPreview() {
    val yearMonth = YearMonth.of(2026, 10)
    BlueTheme(darkTheme = false) {
        SleepArchiveMonthContent(
            yearMonth = yearMonth,
            today = yearMonth.atDay(6),
            records = emptyList(),
            onOpenDay = {},
            onBack = {},
        )
    }
}
