package com.example.blue.feature.accounting

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.core.util.AmountUtils
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.data.repository.AccountCategoryAggregate
import com.example.blue.data.repository.AccountMonthlyAggregate
import com.example.blue.data.repository.AccountRepository
import com.example.blue.model.AccountSummary
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.Year

private val YearSummaryBackground = Color(0xFFF6F8FC)
private val YearSummarySurface = Color(0xFFFEFFFF)
private val YearSummaryText = Color(0xFF2D4555)
private val YearSummaryMuted = Color(0xFF748895)
private val YearSummaryAccent = Color(0xFF3D7BE5)
private val YearSummaryBorder = Color(0xFFDCE7EE)
private val YearSummaryIncome = Color(0xFF3F8D78)
private val YearSummaryExpense = Color(0xFFC96868)

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

    Scaffold(
        containerColor = YearSummaryBackground,
        topBar = { if (showTopBar) AccountTopBar(title = "年度总结", onBack = onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "year-selector") {
                AccountYearSelector(
                    year = uiState.year,
                    canMoveForward = uiState.year < LocalDate.now().year,
                    onYearChange = { selected ->
                        if (selected >= MIN_SUPPORTED_YEAR) summaryViewModel.selectYear(selected)
                    },
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
                            onRetry = summaryViewModel::retry,
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
                        AccountSummaryCard(
                            title = "全年汇总",
                            supportingText = "${summary.entryCount} 笔账目 · ${summary.recordDays} 个记账日",
                            summary = AccountSummary(
                                incomeInCents = summary.incomeInCents,
                                expenseInCents = summary.expenseInCents,
                            ),
                        )
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
            drawLine(
                color = YearSummaryBorder,
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 1.dp.toPx(),
            )
            allMonths.forEachIndexed { index, item ->
                val center = groupWidth * index + groupWidth / 2f
                val incomeHeight = chartHeight * (item.incomeInCents.toDouble() / maximum.toDouble()).toFloat() * reveal
                val expenseHeight = chartHeight * (item.expenseInCents.toDouble() / maximum.toDouble()).toFloat() * reveal
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = YearSummaryIncome,
                        topLeft = Offset(center - barWidth - 1.dp.toPx(), baselineY - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                    )
                }
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = YearSummaryExpense,
                        topLeft = Offset(center + 1.dp.toPx(), baselineY - expenseHeight),
                        size = Size(barWidth, expenseHeight),
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
    Column(
        modifier = modifier
            .background(Color(0xFFF6F9FB), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = YearSummaryMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = YearSummaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
                        .height(8.dp)
                        .background(accent.copy(alpha = 0.10f), CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.012f))
                            .height(8.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = YearSummarySurface),
        border = BorderStroke(1.dp, YearSummaryBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 19.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = YearSummaryText,
                )
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = YearSummaryMuted)
            }
            HorizontalDivider(color = Color(0xFFEDF1F4))
            content()
        }
    }
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
        border = BorderStroke(1.dp, YearSummaryBorder),
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
        border = BorderStroke(1.dp, YearSummaryExpense.copy(alpha = 0.25f)),
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
                colors = ButtonDefaults.buttonColors(containerColor = YearSummaryExpense),
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

private const val MIN_SUPPORTED_YEAR = 1900
private const val MAX_VISIBLE_CATEGORIES = 6
