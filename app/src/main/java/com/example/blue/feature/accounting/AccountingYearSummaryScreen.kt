package com.example.blue.feature.accounting

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.repository.AccountCategoryAggregate
import com.example.blue.data.repository.AccountMonthlyAggregate
import com.example.blue.data.repository.AccountPeriodSummary
import com.example.blue.data.repository.AccountRepository
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.FeatureHubTab
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedCoral
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedLine
import com.example.blue.feature.common.RefinedMetric
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedPeriodSelector
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.model.AccountType
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.Year

private val YearSummaryBackground = RefinedBackground
private val YearSummarySurface = Color(0xFFFEFFFF)
private val YearSummaryText = RefinedInk
private val YearSummaryMuted = RefinedMuted
private val YearSummaryAccent = RefinedBlue
private val YearSummaryBorder = RefinedLine
private val YearSummaryIncome = RefinedTeal
private val YearSummaryExpense = RefinedCoral

@Composable
fun AccountingYearSummaryScreen(
    repository: AccountRepository,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val factory = remember(repository) { AccountingYearSummaryViewModel.factory(repository) }
    val summaryViewModel: AccountingYearSummaryViewModel = viewModel(
        factory = factory,
    )
    val uiState by summaryViewModel.uiState.collectAsStateWithLifecycle()

    AccountingYearSummaryContent(
        uiState = uiState,
        onYearChange = { if (it >= MIN_SUPPORTED_YEAR) summaryViewModel.selectYear(it) },
        onRetry = summaryViewModel::retry, onBack = onBack, showTopBar = showTopBar,
    )
}

@Composable
private fun AccountingYearSummaryContent(
    uiState: AccountingYearSummaryUiState,
    onYearChange: (Int) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean,
) {
    Scaffold(
        containerColor = YearSummaryBackground,
        topBar = { if (showTopBar) RefinedTopBar(title = "年度总结", onBack = onBack) },
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
                        label = "${uiState.year}年",
                        canMoveForward = uiState.year < LocalDate.now().year,
                        onPrevious = { onYearChange(uiState.year - 1) },
                        onNext = { onYearChange(uiState.year + 1) },
                    )
                }

                when {
                    uiState.isLoading -> {
                        item(key = "year-loading") {
                            YearSummaryStateCard(
                                title = "正在生成年度总结",
                                message = "聚合全年账目，不会读取无关历史记录。",
                                loading = true,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                ),
                            )
                        }
                    }
                    uiState.errorMessage != null -> {
                        item(key = "year-error") {
                            YearSummaryErrorCard(
                                message = uiState.errorMessage.orEmpty(),
                                onRetry = onRetry,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                ),
                            )
                        }
                    }
                    uiState.isEmpty -> {
                        item(key = "year-empty") {
                            YearSummaryStateCard(
                                title = "${uiState.year}年还没有账目",
                                message = "记录第一笔收支后，这里会自动生成总结。",
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                ),
                            )
                        }
                    }
                    else -> {
                        val summary = requireNotNull(uiState.summary)
                        item(key = "year-total") {
                            YearTotalsCard(summary)
                        }
                        item(key = "year-month-chart") {
                            MonthlyCashFlowCard(months = uiState.months)
                        }
                        item(key = "year-highlights") {
                            val highestExpenseMonth = uiState.months
                                .filter { it.expenseInCents > 0L }
                                .maxByOrNull { it.expenseInCents }
                            val largestExpenseCategory = uiState.categories
                                .filter { it.type == AccountType.EXPENSE && it.totalInCents > 0L }
                                .maxByOrNull { it.totalInCents }
                            YearHighlightsCard(
                                highestExpenseMonth = highestExpenseMonth,
                                largestExpenseCategory = largestExpenseCategory,
                                largestExpenseInCents = summary.largestExpenseInCents,
                            )
                        }
                        item(key = "year-record-stats") {
                            val monthDivisor = if (uiState.year == LocalDate.now().year) {
                                LocalDate.now().monthValue
                            } else {
                                12
                            }
                            val dayDivisor = if (uiState.year == LocalDate.now().year) {
                                LocalDate.now().dayOfYear
                            } else {
                                Year.of(uiState.year).length()
                            }
                            YearRecordStatsCard(
                                entryCount = summary.entryCount,
                                recordDays = summary.recordDays,
                                monthlyAverageInCents = roundedAverage(summary.expenseInCents, monthDivisor),
                                dailyAverageInCents = roundedAverage(summary.expenseInCents, dayDivisor),
                            )
                        }
                        item(key = "expense-categories") {
                            CategoryShareCard(
                                title = "支出分类占比",
                                emptyText = "本年没有支出记录",
                                categories = uiState.categories.filter { it.type == AccountType.EXPENSE },
                                accent = YearSummaryExpense,
                            )
                        }
                        item(key = "income-categories") {
                            CategoryShareCard(
                                title = "收入分类占比",
                                emptyText = "本年没有收入记录",
                                categories = uiState.categories.filter { it.type == AccountType.INCOME },
                                accent = YearSummaryIncome,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearTotalsCard(summary: AccountPeriodSummary) {
    val expense = "¥${AmountUtils.formatCents(summary.expenseInCents)}"
    val longAmount = expense.length > 14
    RefinedCard(
        title = "全年汇总", subtitle = "${summary.entryCount} 笔账目 · ${summary.recordDays} 个记账日",
        iconRes = R.drawable.ic_accounting,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("全年支出", fontSize = 12.sp, color = YearSummaryMuted)
            Text(
                if (longAmount) expense.replace(",", ",\u200B") else expense,
                modifier = Modifier.fillMaxWidth(), fontSize = 38.sp, lineHeight = 46.sp,
                fontWeight = FontWeight.SemiBold, color = YearSummaryText,
                maxLines = if (longAmount) 2 else 1, softWrap = longAmount,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 38.sp, stepSize = 0.5.sp),
            )
        }
        HorizontalDivider(color = YearSummaryBorder)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RefinedMetric("全年收入", "¥${AmountUtils.formatCents(summary.incomeInCents)}", Modifier.weight(1f), YearSummaryIncome)
            val balance = summary.incomeInCents - summary.expenseInCents
            RefinedMetric("全年结余", "¥${AmountUtils.formatCents(balance)}", Modifier.weight(1f), if (balance < 0) YearSummaryExpense else YearSummaryText)
        }
    }
}

@Composable
private fun MonthlyCashFlowCard(months: List<AccountMonthlyAggregate>) {
    val monthMap = remember(months) { months.associateBy { it.month } }
    val allMonths = remember(monthMap) {
        (1..12).map { month ->
            monthMap[month] ?: AccountMonthlyAggregate(
                month = month,
                incomeInCents = 0L,
                expenseInCents = 0L,
                entryCount = 0,
                recordDays = 0,
            )
        }
    }
    var entered by remember(months) { mutableStateOf(false) }
    LaunchedEffect(months) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(280),
        label = "Annual accounting chart reveal",
    )
    val visibleReveal = if (LocalInspectionMode.current) 1f else reveal
    val maximum = allMonths.maxOfOrNull { maxOf(it.incomeInCents, it.expenseInCents) }?.coerceAtLeast(1L) ?: 1L

    YearSummaryCard(title = "每月收支", supportingText = "绿色为收入，红色为支出；下方显示每月结余") {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .semantics {
                    contentDescription = allMonths.joinToString(separator = "；") {
                        "${it.month}月收入${AmountUtils.formatCents(it.incomeInCents)}元，支出${AmountUtils.formatCents(it.expenseInCents)}元"
                    }
                },
        ) {
            val baselineY = size.height - 18.dp.toPx()
            val chartHeight = baselineY - 10.dp.toPx()
            val groupWidth = size.width / 12f
            val barWidth = (groupWidth * 0.24f).coerceAtLeast(3.dp.toPx())
            repeat(3) { level ->
                val y = baselineY - chartHeight * (level + 1) / 3f
                drawLine(YearSummaryBorder.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            drawLine(
                color = YearSummaryBorder,
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 1.dp.toPx(),
            )
            allMonths.forEachIndexed { index, item ->
                val center = groupWidth * index + groupWidth / 2f
                val incomeHeight = chartHeight * (item.incomeInCents.toDouble() / maximum.toDouble()).toFloat() * visibleReveal
                val expenseHeight = chartHeight * (item.expenseInCents.toDouble() / maximum.toDouble()).toFloat() * visibleReveal
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = YearSummaryIncome.copy(alpha = 0.76f),
                        topLeft = Offset(center - barWidth - 1.dp.toPx(), baselineY - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = YearSummaryExpense.copy(alpha = 0.76f),
                        topLeft = Offset(center + 1.dp.toPx(), baselineY - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            (1..12).forEach { month ->
                Text(
                    text = month.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = YearSummaryMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allMonths, key = { it.month }) { item ->
                Column(
                    modifier = Modifier
                        .background(Color(0xFFF6F9FB), RoundedCornerShape(13.dp))
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("${item.month}月结余", style = MaterialTheme.typography.labelSmall, color = YearSummaryMuted)
                    Text(
                        "¥${AmountUtils.formatCents(item.balanceInCents)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.balanceInCents < 0L) YearSummaryExpense else YearSummaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun YearHighlightsCard(
    highestExpenseMonth: AccountMonthlyAggregate?,
    largestExpenseCategory: AccountCategoryAggregate?,
    largestExpenseInCents: Long,
) {
    YearSummaryCard(title = "年度亮点", supportingText = "快速定位主要支出") {
        YearDetailRow(
            label = "最高支出月份",
            value = highestExpenseMonth?.let {
                "${it.month}月 · ¥${AmountUtils.formatCents(it.expenseInCents)}"
            } ?: "—",
        )
        YearDetailRow(
            label = "最大支出类别",
            value = largestExpenseCategory?.let {
                "${it.categoryName} · ¥${AmountUtils.formatCents(it.totalInCents)}"
            } ?: "—",
        )
        YearDetailRow(
            label = "最大单笔支出",
            value = if (largestExpenseInCents > 0L) {
                "¥${AmountUtils.formatCents(largestExpenseInCents)}"
            } else {
                "—"
            },
            showDivider = false,
        )
    }
}

@Composable
private fun YearRecordStatsCard(
    entryCount: Int,
    recordDays: Int,
    monthlyAverageInCents: Long,
    dailyAverageInCents: Long,
) {
    YearSummaryCard(
        title = "记录概览",
        supportingText = "日均按自然日计算，当前年份按已过去天数计算",
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YearMetricTile("记账笔数", "$entryCount 笔", Modifier.weight(1f))
            YearMetricTile("记账天数", "$recordDays 天", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YearMetricTile(
                "月均支出",
                "¥${AmountUtils.formatCents(monthlyAverageInCents)}",
                Modifier.weight(1f),
            )
            YearMetricTile(
                "日均支出",
                "¥${AmountUtils.formatCents(dailyAverageInCents)}",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun YearMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    RefinedMetric(label, value, modifier)
}

@Composable
private fun CategoryShareCard(
    title: String,
    emptyText: String,
    categories: List<AccountCategoryAggregate>,
    accent: Color,
) {
    val slices = remember(categories) { categorySlices(categories) }
    val total = remember(categories) { categories.sumOf { it.totalInCents } }
    YearSummaryCard(title = title, supportingText = if (total > 0L) "按金额统计" else emptyText) {
        if (total <= 0L) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = YearSummaryMuted)
        } else {
            slices.forEachIndexed { index, slice ->
                val fraction = (slice.amountInCents.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        slice.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = YearSummaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${(fraction * 1000).toInt() / 10f}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(accent.copy(alpha = 0.07f), CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.012f))
                            .height(6.dp)
                            .background(accent.copy(alpha = 0.82f - index * 0.08f), CircleShape),
                    )
                }
                if (index != slices.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private data class CategorySlice(val label: String, val amountInCents: Long)

private fun categorySlices(categories: List<AccountCategoryAggregate>): List<CategorySlice> {
    val sorted = categories.filter { it.totalInCents > 0L }.sortedByDescending { it.totalInCents }
    if (sorted.size <= MAX_VISIBLE_CATEGORIES) {
        return sorted.map { CategorySlice(it.categoryName, it.totalInCents) }
    }
    val visible = sorted.take(MAX_VISIBLE_CATEGORIES - 1).map {
        CategorySlice(it.categoryName, it.totalInCents)
    }
    val other = sorted.drop(MAX_VISIBLE_CATEGORIES - 1).sumOf { it.totalInCents }
    return visible + CategorySlice("其他", other)
}

@Composable
private fun YearSummaryCard(
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    RefinedCard(
        title = title, subtitle = supportingText,
        iconRes = when (title) {
            "每月收支" -> R.drawable.ic_calendar
            "年度亮点" -> R.drawable.ic_tag
            else -> R.drawable.ic_accounting
        },
        accent = when {
            title.startsWith("收入") -> YearSummaryIncome
            title.startsWith("支出") -> YearSummaryExpense
            else -> YearSummaryAccent
        },
        content = content,
    )
}

@Composable
private fun YearDetailRow(label: String, value: String, showDivider: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = YearSummaryMuted)
        Text(
            value,
            modifier = Modifier.weight(1.15f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = YearSummaryText,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (showDivider) HorizontalDivider(color = Color(0xFFEEF2F5))
}

@Composable
private fun YearSummaryStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = YearSummarySurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp), color = YearSummaryAccent, strokeWidth = 2.5.dp)
            } else {
                Box(
                    modifier = Modifier.size(44.dp).background(YearSummaryAccent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("¥", color = YearSummaryAccent, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = YearSummaryText)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = YearSummaryMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun YearSummaryErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F3)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = YearSummaryExpense, textAlign = TextAlign.Center)
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YearSummaryExpense.copy(alpha = 0.10f), contentColor = YearSummaryExpense),
            ) {
                Text("重新加载")
            }
        }
    }
}

private fun roundedAverage(totalInCents: Long, divisor: Int): Long {
    if (totalInCents <= 0L || divisor <= 0) return 0L
    val quotient = totalInCents / divisor
    val remainder = totalInCents % divisor
    return quotient + if (remainder * 2L >= divisor) 1L else 0L
}

private fun accountingSummaryPreviewState(): AccountingYearSummaryUiState {
    val months = (1..9).map { month ->
        AccountMonthlyAggregate(month, 640_000L + month * 8_000L, 220_000L + month * 17_000L, 16 + month, 10 + month)
    }
    val income = months.sumOf { it.incomeInCents }
    val expense = months.sumOf { it.expenseInCents }
    val categories = listOf(
        AccountCategoryAggregate("dining", "餐饮", AccountType.EXPENSE, expense * 30 / 100, 68),
        AccountCategoryAggregate("housing", "居住", AccountType.EXPENSE, expense * 27 / 100, 9),
        AccountCategoryAggregate("shopping", "购物", AccountType.EXPENSE, expense * 18 / 100, 24),
        AccountCategoryAggregate("transport", "交通", AccountType.EXPENSE, expense * 12 / 100, 42),
        AccountCategoryAggregate("other", "其他", AccountType.EXPENSE, expense * 13 / 100, 18),
        AccountCategoryAggregate("salary", "工资", AccountType.INCOME, income * 95 / 100, 9),
        AccountCategoryAggregate("bonus", "奖金", AccountType.INCOME, income * 5 / 100, 2),
    )
    return AccountingYearSummaryUiState(
        year = 2026, isLoading = false,
        summary = AccountPeriodSummary(income, expense, months.sumOf { it.entryCount }, months.sumOf { it.recordDays }, 168_000L),
        months = months, categories = categories,
    )
}

@Preview(name = "记账总结 · 标准手机", widthDp = 390, heightDp = 1050, showBackground = true)
@Preview(name = "记账总结 · 窄屏", widthDp = 320, heightDp = 1050, showBackground = true)
@Preview(name = "记账总结 · 大字体", widthDp = 390, heightDp = 1300, fontScale = 1.5f, showBackground = true)
@Composable
private fun AccountingYearSummaryScreenPreview() {
    BlueTheme(darkTheme = false) {
        FeatureHubScreen(
            tabs = listOf(FeatureHubTab("archive", "年月"), FeatureHubTab("browse", "浏览"), FeatureHubTab("summary", "总结")),
            initialPage = 2, accentColor = AccountingArchiveAccent, tabStyle = AccountingArchiveTabStyle,
        ) {
            AccountingYearSummaryContent(accountingSummaryPreviewState(), {}, {}, {}, showTopBar = false)
        }
    }
}

@Preview(name = "记账总结 · 分类与记录", widthDp = 390, heightDp = 1100, showBackground = true)
@Preview(name = "记账总结 · 分类大字体", widthDp = 320, heightDp = 1300, fontScale = 1.5f, showBackground = true)
@Composable
private fun AccountingSummaryCategoriesPreview() {
    val state = accountingSummaryPreviewState()
    val summary = requireNotNull(state.summary)
    BlueTheme(darkTheme = false) {
        LazyColumn(
            Modifier.fillMaxSize().background(YearSummaryBackground),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { YearHighlightsCard(state.months.maxByOrNull { it.expenseInCents }, state.categories.filter { it.type == AccountType.EXPENSE }.maxByOrNull { it.totalInCents }, summary.largestExpenseInCents) }
            item { YearRecordStatsCard(summary.entryCount, summary.recordDays, roundedAverage(summary.expenseInCents, 9), roundedAverage(summary.expenseInCents, 273)) }
            item { CategoryShareCard("支出分类占比", "本年没有支出记录", state.categories.filter { it.type == AccountType.EXPENSE }, YearSummaryExpense) }
            item { CategoryShareCard("收入分类占比", "本年没有收入记录", state.categories.filter { it.type == AccountType.INCOME }, YearSummaryIncome) }
        }
    }
}

@Preview(name = "记账总结 · 暂无记录", widthDp = 390, heightDp = 720, showBackground = true)
@Composable
private fun AccountingYearSummaryEmptyPreview() {
    BlueTheme(darkTheme = false) {
        AccountingYearSummaryContent(AccountingYearSummaryUiState(year = 2026, isLoading = false), {}, {}, {}, showTopBar = true)
    }
}

private const val MIN_SUPPORTED_YEAR = 1900
private const val MAX_VISIBLE_CATEGORIES = 6
