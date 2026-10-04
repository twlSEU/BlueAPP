package com.example.blue.feature.sleep

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val YearBackground = Color(0xFFF6F8FC)
private val YearText = Color(0xFF0B1633)
private val YearMuted = Color(0xFF7485A6)
private val YearDataLabel = Color(0xFF8493B2)
private val YearAccent = Color(0xFF7065D8)
private val YearArrow = Color(0xFF97A5BE)
private val YearTrack = Color(0xFFEDF1F8)
private val YearShadow = Color(0xFF536787)
private val YearSelectorShape = RoundedCornerShape(32.dp)
private val YearMonthShape = RoundedCornerShape(28.dp)
private val YearProgressShape = RoundedCornerShape(4.dp)
private val YearProgressBrush = Brush.horizontalGradient(
    listOf(Color(0xFF6595ED), Color(0xFF8374E8)),
)
private val CurrentMonthBrush = Brush.linearGradient(
    listOf(Color(0xFFF5F3FF), Color(0xFFF9FAFF), Color.White),
)
private val yearTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** The archive UI takes plain data so previews never need Room or a live repository. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepArchiveYearContent(
    year: Int,
    today: LocalDate,
    records: List<SleepRecordEntity>?,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onOpenMonth: (Int, Int) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val grouped = remember(records) { records.orEmpty().groupBy { it.recordDate.monthValue } }
    val months = remember(year, today) {
        if (year == today.year) (today.monthValue downTo 1).toList() else (1..12).toList()
    }

    Scaffold(
        containerColor = YearBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Text("按年月查看", fontWeight = FontWeight.SemiBold, color = YearText)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .semantics { contentDescription = "返回" },
                        ) {
                            ArchiveChevron(color = YearText, pointsLeft = true)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = YearBackground,
                        scrolledContainerColor = YearBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "year") {
                // 24dp separates the selector from the month cards; cards use 16dp.
                Box(Modifier.padding(bottom = 8.dp)) {
                    SleepArchiveYearSelector(
                        year = year,
                        canMoveForward = year < today.year,
                        onPrevious = onPreviousYear,
                        onNext = onNextYear,
                    )
                }
            }
            if (records == null) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(128.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = YearAccent,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            } else {
                items(months, key = { month -> "$year-$month" }) { month ->
                    SleepArchiveMonthCard(
                        yearMonth = YearMonth.of(year, month),
                        records = grouped[month].orEmpty(),
                        isCurrentMonth = year == today.year && month == today.monthValue,
                        onClick = { onOpenMonth(year, month) },
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .dropShadow(
                shape = YearSelectorShape,
                shadow = Shadow(
                    radius = 16.dp,
                    color = YearShadow.copy(alpha = 0.04f),
                    offset = DpOffset(0.dp, 4.dp),
                ),
            ),
        shape = YearSelectorShape,
        color = Color.White,
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .semantics { contentDescription = "上一年" },
            ) {
                ArchiveChevron(color = YearArrow, pointsLeft = true)
            }
            AnimatedContent(
                targetState = year,
                modifier = Modifier.align(Alignment.Center),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { direction * it / 5 })
                        .togetherWith(
                            fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { -direction * it / 5 },
                        )
                },
                label = "Sleep archive year",
            ) { selectedYear ->
                Text(
                    text = "${selectedYear}年",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                    color = YearText,
                )
            }
            IconButton(
                onClick = onNext,
                enabled = canMoveForward,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp)
                    .semantics { contentDescription = "下一年" },
            ) {
                ArchiveChevron(color = if (canMoveForward) YearArrow else Color(0xFFD4DCEB))
            }
        }
    }
}

@Composable
private fun SleepArchiveMonthCard(
    yearMonth: YearMonth,
    records: List<SleepRecordEntity>,
    isCurrentMonth: Boolean,
    onClick: () -> Unit,
) {
    val average = remember(records) {
        SleepDateRules.averageBedtime(records.map { it.sleepDateTime.toLocalTime() })
    }
    // Record dates are unique. Coverage is independent of bedtime and uses the real month length.
    val progress = (records.size.toFloat() / yearMonth.lengthOfMonth()).coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .dropShadow(
                shape = YearMonthShape,
                shadow = Shadow(
                    radius = 20.dp,
                    color = YearShadow.copy(alpha = 0.06f),
                    offset = DpOffset(0.dp, 6.dp),
                ),
            )
            .semantics { if (isCurrentMonth) stateDescription = "当前月份" },
        shape = YearMonthShape,
        color = Color.White,
    ) {
        val highlight = if (isCurrentMonth) Modifier.background(CurrentMonthBrush) else Modifier
        BoxWithConstraints(highlight.padding(24.dp)) {
            // Preserve room for the empty-state caption; the data area shrinks on small phones.
            val dataWidth = (maxWidth - 148.dp).coerceIn(96.dp, 152.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 38.sp, fontWeight = FontWeight.SemiBold)) {
                                append(yearMonth.monthValue.toString())
                            }
                            withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)) {
                                append("月")
                            }
                        },
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 44.sp,
                        letterSpacing = 0.sp,
                        color = if (isCurrentMonth) YearAccent else YearText,
                    )
                    Text(
                        text = if (records.isEmpty()) "还没有睡眠记录" else "记录 ${records.size} 天",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp,
                        color = YearMuted,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.width(dataWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "平均睡眠",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp,
                        color = YearDataLabel,
                    )
                    Text(
                        text = average?.format(yearTimeFormatter) ?: "-",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        color = YearText,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(YearProgressShape)
                            .background(YearTrack)
                            .semantics {
                                contentDescription = "本月记录进度"
                                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                            },
                    ) {
                        if (progress > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(YearProgressShape)
                                    .background(YearProgressBrush),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                ArchiveChevron(color = YearArrow)
            }
        }
    }
}

/** All chevrons share the same 1.75dp rounded stroke instead of font glyphs. */
@Composable
private fun ArchiveChevron(color: Color, pointsLeft: Boolean = false) {
    Canvas(Modifier.size(width = 12.dp, height = 24.dp)) {
        val direction = if (pointsLeft) -1f else 1f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val halfWidth = 3.dp.toPx()
        val halfHeight = 6.dp.toPx()
        val path = Path().apply {
            moveTo(centerX - direction * halfWidth, centerY - halfHeight)
            lineTo(centerX + direction * halfWidth, centerY)
            lineTo(centerX - direction * halfWidth, centerY + halfHeight)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview(name = "年月 · 当前月与历史记录", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "年月 · 窄屏", widthDp = 320, heightDp = 740, showBackground = true)
@Preview(name = "年月 · 大字体", widthDp = 390, heightDp = 844, fontScale = 1.3f, showBackground = true)
@Composable
private fun SleepArchiveYearPreview() {
    SleepArchivePreviewContent(empty = false)
}

@Preview(name = "年月 · 无睡眠记录", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SleepArchiveYearEmptyPreview() {
    SleepArchivePreviewContent(empty = true)
}

@Composable
private fun SleepArchivePreviewContent(empty: Boolean) {
    val today = LocalDate.of(2026, 10, 5)
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    val records = remember(empty) {
        if (empty) emptyList() else buildList {
            fun addMonth(month: Int, days: Int, bedtime: (Int) -> LocalTime) {
                repeat(days) { index ->
                    val date = LocalDate.of(2026, month, index + 1)
                    add(
                        SleepRecordEntity(
                            id = "preview-$date",
                            recordDate = date,
                            sleepDateTime = SleepDateRules.sleepDateTimeFor(date, bedtime(index)),
                            wakeDateTime = null,
                            source = SleepSource.MANUAL,
                            isEstimated = false,
                            note = null,
                            createdAt = 0L,
                            updatedAt = 0L,
                        ),
                    )
                }
            }
            addMonth(10, 3) { LocalTime.of(2, 0).plusMinutes(it * 30L) }
            addMonth(9, 18) { LocalTime.of(23, 45) }
            addMonth(7, 12) { LocalTime.of(0, 18) }
        }
    }
    val yearRecords = remember(records, year) { records.filter { it.recordDate.year == year } }
    BlueTheme(darkTheme = false) {
        SleepArchiveYearContent(
            year = year,
            today = today,
            records = yearRecords,
            onPreviousYear = { year-- },
            onNextYear = { if (year < today.year) year++ },
            onOpenMonth = { _, _ -> },
            onBack = {},
            showTopBar = false,
        )
    }
}
