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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.repository.SleepRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.map

private val ArchiveBackground = Color(0xFFF6F8FC)
private val ArchiveSurface = Color(0xFFFEFFFF)
private val ArchiveText = Color(0xFF2D4555)
private val ArchiveMuted = Color(0xFF748895)
private val ArchiveAccent = Color(0xFF637FC3)
private val archiveTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
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
    val grouped = remember(records) { records.orEmpty().groupBy { it.recordDate.monthValue } }
    val months = remember(year, today) {
        if (year == today.year) (today.monthValue downTo 1).toList() else (1..12).toList()
    }

    Scaffold(
        containerColor = ArchiveBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("按年月查看", fontWeight = FontWeight.SemiBold, color = ArchiveText) },
                    navigationIcon = { AppBackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ArchiveBackground,
                        scrolledContainerColor = ArchiveBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        if (records == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArchiveAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "year") {
                    SleepArchiveYearSelector(
                        year = year,
                        canMoveForward = year < today.year,
                        onPrevious = { year-- },
                        onNext = { if (year < today.year) year++ },
                    )
                }
                items(months, key = { month -> "$year-$month" }) { month ->
                    SleepArchiveMonthCard(
                        month = month,
                        records = grouped[month].orEmpty(),
                        onClick = { onOpenMonth(year, month) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val byDate = remember(records) { records.orEmpty().associateBy { it.recordDate } }
    val days = remember(yearMonth, today) {
        val lastDay = if (yearMonth == YearMonth.from(today)) today.dayOfMonth else yearMonth.lengthOfMonth()
        if (yearMonth > YearMonth.from(today)) emptyList() else (lastDay downTo 1).map(yearMonth::atDay)
    }

    Scaffold(
        containerColor = ArchiveBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${year}年${month}月", fontWeight = FontWeight.SemiBold, color = ArchiveText) },
                navigationIcon = { AppBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArchiveBackground,
                    scrolledContainerColor = ArchiveBackground,
                ),
            )
        },
    ) { padding ->
        if (records == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArchiveAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    SleepArchiveMonthSummary(records.orEmpty())
                }
                items(days, key = LocalDate::toString) { date ->
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

@Composable
private fun SleepArchiveYearSelector(
    year: Int,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ArchiveSurface,
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = ArchiveAccent)
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
                label = "Sleep archive year",
            ) { selectedYear ->
                Text("${selectedYear}年", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ArchiveText)
            }
            TextButton(onClick = onNext, enabled = canMoveForward, modifier = Modifier.size(48.dp)) {
                Text("›", style = MaterialTheme.typography.headlineMedium, color = if (canMoveForward) ArchiveAccent else Color(0xFFC8D2D8))
            }
        }
    }
}

@Composable
private fun SleepArchiveMonthCard(month: Int, records: List<SleepRecordEntity>, onClick: () -> Unit) {
    val times = remember(records) { records.map { it.sleepDateTime.toLocalTime() } }
    val average = remember(times) { SleepDateRules.averageBedtime(times) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ArchiveSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Box(
                modifier = Modifier.size(58.dp).background(Color(0xFFEAF0FC), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(month.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge, color = ArchiveAccent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${month}月", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ArchiveText)
                Text(
                    if (records.isEmpty()) "还没有睡眠记录" else "记录 ${records.size} 天 · 平均 ${average.displayArchiveTime()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArchiveMuted,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = ArchiveAccent)
        }
    }
}

@Composable
private fun SleepArchiveMonthSummary(records: List<SleepRecordEntity>) {
    val times = remember(records) { records.map { it.sleepDateTime.toLocalTime() } }
    val average = remember(times) { SleepDateRules.averageBedtime(times) }
    val late = remember(times) { times.count(SleepDateRules::isLateNight) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FC)),
        border = BorderStroke(1.dp, Color(0xFFD7E2F3)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("本月概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ArchiveText)
            Row(Modifier.fillMaxWidth()) {
                ArchiveMetric("记录天数", "${records.size} 天", Modifier.weight(1f))
                ArchiveMetric("平均入睡", average.displayArchiveTime(), Modifier.weight(1f))
                ArchiveMetric("熬夜天数", "$late 天", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ArchiveMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ArchiveText)
        Text(label, style = MaterialTheme.typography.labelSmall, color = ArchiveMuted)
    }
}

@Composable
private fun SleepArchiveDayCard(date: LocalDate, record: SleepRecordEntity?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ArchiveSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(
                    color = record?.sleepDateTime?.toLocalTime()?.let {
                        listOf(
                            Color(0xFFDDECE4), Color(0xFFCEE3D8), Color(0xFFFFE2B8),
                            Color(0xFFF5C68E), Color(0xFFE99B79), Color(0xFFD86F6F),
                        )[SleepDateRules.latenessLevel(it)]
                    } ?: Color(0xFFF0F3F6),
                    shape = RoundedCornerShape(16.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ArchiveText)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${date.monthValue}月${date.dayOfMonth}日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ArchiveText)
                Text(
                    record?.let { "${it.sleepDateTime.toLocalTime().format(archiveTimeFormatter)} 入睡${it.wakeDateTime?.let { wake -> " · ${wake.toLocalTime().format(archiveTimeFormatter)} 起床" }.orEmpty()}" }
                        ?: "未记录 · 点击添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArchiveMuted,
                )
                record?.let {
                    Text(
                        when (it.source) {
                            SleepSource.MANUAL -> "手动记录"
                            SleepSource.SYSTEM_ESTIMATE -> "系统推测"
                            SleepSource.MANUAL_CONFIRMED -> "手动确认"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it.isEstimated) Color(0xFFB87B50) else ArchiveAccent,
                    )
                }
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = ArchiveAccent)
        }
    }
}

private fun LocalTime?.displayArchiveTime(): String = this?.format(archiveTimeFormatter) ?: "—"
