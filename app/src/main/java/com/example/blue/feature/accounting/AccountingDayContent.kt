package com.example.blue.feature.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.model.AccountSummary
import com.example.blue.model.AccountType
import com.example.blue.model.toAccountSummary
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayBackground = Color(0xFFF5F9FD)
private val DayInk = Color(0xFF102F4B)
private val DayMuted = Color(0xFF8499B5)
private val DayExpense = Color(0xFFF43F57)
private val DayIncome = Color(0xFF00A582)
private val DayAccent = Color(0xFF1886FF)
private val DayLine = Color(0xFFE5EDF7)
private val DayShadow = Color(0xFF7394BE).copy(alpha = 0.06f)
private val DayCardShape = RoundedCornerShape(16.dp)
private val dayTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayTitleFormat = DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)

/** Production and previews share the same UI without requiring a repository. */
@Composable
internal fun AccountingDayScreenContent(
    date: LocalDate,
    entries: List<AccountEntryWithCategory>?,
    onEdit: (String?) -> Unit,
    onDelete: (AccountEntryWithCategory) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = DayBackground,
        topBar = { AccountingDayTopBar(date = date, onBack = onBack) },
        floatingActionButton = { AccountingDayFloatingAction(onClick = { onEdit(null) }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (entries == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DayAccent)
            } else {
                val summary = remember(entries) { entries.map { it.entry }.toAccountSummary() }
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().align(Alignment.TopCenter),
                    state = listState,
                    contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "day-summary", contentType = "day-summary") {
                        AccountingDaySummary(summary)
                    }
                    item(key = "day-detail-heading", contentType = "heading") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("收支明细", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = DayInk)
                            Text("${entries.size} 笔", fontSize = 13.sp, lineHeight = 20.sp, color = DayMuted)
                        }
                    }
                    if (entries.isEmpty()) {
                        item(key = "day-empty") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(painterResource(R.drawable.ic_wallet), null, Modifier.size(36.dp), tint = DayMuted)
                                Text("当天暂无账目", fontSize = 14.sp, color = DayMuted)
                                TextButton(onClick = { onEdit(null) }) { Text("记一笔", color = DayAccent) }
                            }
                        }
                    } else {
                        items(entries, key = { it.entry.id }, contentType = { "day-entry" }) { item ->
                            DayEntryRow(
                                item = item,
                                onEdit = { onEdit(item.entry.id) },
                                onDelete = { onDelete(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountingDayTopBar(date: LocalDate, onBack: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(DayBackground)
            .statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = date.format(dayTitleFormat),
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 52.dp),
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = DayInk,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
        )
        Surface(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).size(44.dp).dropShadow(
                CircleShape, Shadow(radius = 12.dp, color = DayShadow, offset = DpOffset(0.dp, 4.dp)),
            ),
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_chevron_right),
                    contentDescription = "返回",
                    tint = DayInk,
                    modifier = Modifier.size(width = 18.dp, height = 26.dp).graphicsLayer { rotationZ = 180f },
                )
            }
        }
    }
}

@Composable
private fun AccountingDayFloatingAction(onClick: () -> Unit) {
    val button: @Composable () -> Unit = {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(60.dp).dropShadow(
                CircleShape, Shadow(radius = 14.dp, color = DayAccent.copy(alpha = 0.24f), offset = DpOffset(0.dp, 6.dp)),
            ),
            shape = CircleShape,
            containerColor = DayAccent,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) {
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3296FF), Color(0xFF0074FF)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_home_add), contentDescription = "记一笔", modifier = Modifier.size(32.dp))
            }
        }
    }
    if (LocalInspectionMode.current) button() else AppAnimatedFloatingAction(content = button)
}

@Composable
private fun AccountingDaySummary(summary: AccountSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().dropShadow(
            DayCardShape, Shadow(radius = 18.dp, color = DayShadow, offset = DpOffset(0.dp, 6.dp)),
        ),
        shape = DayCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("当日支出", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = DayMuted)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("¥", Modifier.alignByBaseline(), fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = DayInk)
                Text(
                    AmountUtils.formatCents(summary.expenseInCents),
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    fontSize = 46.sp,
                    lineHeight = 56.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold,
                    color = DayInk,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 46.sp, stepSize = 0.5.sp),
                )
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = DayLine)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DaySecondaryAmount("收入", summary.incomeInCents, DayIncome, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(40.dp).background(DayLine))
                DaySecondaryAmount(
                    "结余", summary.balanceInCents,
                    when {
                        summary.balanceInCents < 0L -> DayExpense
                        summary.balanceInCents > 0L -> DayIncome
                        else -> DayInk
                    },
                    Modifier.weight(1f).padding(start = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun DaySecondaryAmount(label: String, cents: Long, color: Color, modifier: Modifier) {
    val amount = "¥ ${AmountUtils.formatCents(cents)}"
    val longAmount = amount.length > 15
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 13.sp, lineHeight = 18.sp, color = DayMuted)
        Text(
            if (longAmount) amount.withDayAmountBreaks() else amount,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = if (longAmount) 2 else 1,
            softWrap = longAmount,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
        )
    }
}

@Composable
private fun DayEntryRow(
    item: AccountEntryWithCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val income = item.entry.type == AccountType.INCOME
    val amountColor = if (income) DayIncome else DayExpense
    val iconColor = when {
        income -> DayIncome
        item.category.name == "交通" -> Color(0xFF667F9D)
        item.category.name == "购物" -> Color(0xFFD29858)
        else -> DayExpense
    }
    val iconBackground = when {
        income -> Color(0xFFEAF7F1)
        item.category.name == "交通" -> Color(0xFFECF4FF)
        item.category.name == "购物" -> Color(0xFFFFF4E8)
        else -> Color(0xFFFFF2EF)
    }
    val icon = when (item.category.name) {
        "餐饮" -> R.drawable.ic_food
        "交通" -> if (item.entry.name.contains("单车") || item.entry.name.contains("自行车")) R.drawable.ic_bicycle else R.drawable.ic_transport
        "购物" -> R.drawable.ic_shopping
        else -> if (income) R.drawable.ic_trending_up else R.drawable.ic_wallet
    }
    val amount = "${if (income) "+" else "−"} ¥${AmountUtils.formatCents(item.entry.amountInCents)}"
    val longAmount = amount.length > 13
    Card(
        modifier = modifier.fillMaxWidth().dropShadow(
            DayCardShape, Shadow(radius = 14.dp, color = DayShadow, offset = DpOffset(0.dp, 4.dp)),
        ).clip(DayCardShape).combinedClickable(
            onClick = onEdit,
            onClickLabel = "编辑账目",
            onLongClickLabel = "删除账目",
            onLongClick = onDelete,
        ),
        shape = DayCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(14.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val amountWidth = (maxWidth * 0.35f).coerceIn(86.dp, 148.dp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).background(iconBackground, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = iconColor)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            item.entry.name,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DayInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.category.name} · ${item.entry.entryTime.format(dayTimeFormat)}",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            letterSpacing = 0.sp,
                            color = DayMuted,
                            maxLines = 1,
                            softWrap = false,
                            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (longAmount) amount.withDayAmountBreaks() else amount,
                        modifier = Modifier.width(amountWidth),
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        color = amountColor,
                        maxLines = if (longAmount) 2 else 1,
                        softWrap = longAmount,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = if (longAmount) 16.sp else 20.sp, stepSize = 0.5.sp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        painterResource(R.drawable.ic_home_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(width = 12.dp, height = 18.dp),
                        tint = Color(0xFFA8B4C4),
                    )
                }
            }
            item.entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    modifier = Modifier.padding(start = 56.dp, end = 22.dp, top = 6.dp),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = DayMuted,
                )
            }
        }
    }
}

private fun String.withDayAmountBreaks(): String = replace(",", ",\u200B")

@Preview(name = "记账 · 日期详情 · 设计", group = "记账日期", widthDp = 390, heightDp = 782, showBackground = true)
@Preview(name = "记账 · 日期详情 · 手机", group = "记账日期", widthDp = 390, heightDp = 844, showBackground = true, showSystemUi = true)
@Preview(name = "记账 · 日期详情 · 窄屏", group = "记账日期", widthDp = 320, heightDp = 800, showBackground = true, showSystemUi = true)
@Preview(name = "记账 · 日期详情 · 大字体", group = "记账日期", widthDp = 390, heightDp = 1000, fontScale = 1.5f, showBackground = true, showSystemUi = true)
@Composable
private fun AccountingDayPreview() {
    BlueTheme(darkTheme = false) {
        AccountingDayScreenContent(
            date = LocalDate.of(2026, 10, 5),
            entries = dayPreviewEntries(),
            onEdit = {},
            onDelete = {},
            onBack = {},
        )
    }
}

@Preview(name = "记账 · 日期详情 · 无账目", group = "记账日期", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun AccountingDayEmptyPreview() {
    BlueTheme(darkTheme = false) {
        AccountingDayScreenContent(LocalDate.of(2026, 10, 5), emptyList(), {}, {}, {})
    }
}

@Preview(name = "记账 · 日期详情 · 收入与长金额", group = "记账日期", widthDp = 320, heightDp = 900, fontScale = 1.3f, showBackground = true)
@Composable
private fun AccountingDayIncomePreview() {
    val salary = dayPreviewEntry("工资与项目奖金", 1_234_567_890L, LocalTime.of(18, 30), "工资", AccountType.INCOME)
    val entries = listOf(salary.copy(entry = salary.entry.copy(note = "项目奖金到账，包含本月工资。"))) + dayPreviewEntries()
    BlueTheme(darkTheme = false) {
        AccountingDayScreenContent(LocalDate.of(2026, 10, 5), entries, {}, {}, {})
    }
}

private fun dayPreviewEntries(): List<AccountEntryWithCategory> = listOf(
    dayPreviewEntry("共享单车", 300L, LocalTime.of(16, 6), "交通"),
    dayPreviewEntry("711葱油面", 1_490L, LocalTime.of(15, 57), "餐饮"),
    dayPreviewEntry("水 蛋挞", 700L, LocalTime.of(15, 57), "餐饮"),
    dayPreviewEntry("鸡公煲", 1_850L, LocalTime.of(10, 16), "餐饮"),
    dayPreviewEntry("水果", 2_000L, LocalTime.of(10, 11), "餐饮"),
)

private fun dayPreviewEntry(
    name: String,
    cents: Long,
    time: LocalTime,
    categoryName: String,
    type: AccountType = AccountType.EXPENSE,
): AccountEntryWithCategory {
    val category = AccountCategoryEntity(
        id = "preview-$categoryName", name = categoryName, type = type,
        isDefault = true, isActive = true, createdAt = 0L, updatedAt = 0L,
    )
    return AccountEntryWithCategory(
        entry = AccountEntryEntity(
            id = "preview-$name", entryDate = LocalDate.of(2026, 10, 5), entryTime = time,
            type = type, amountInCents = cents, name = name, categoryId = category.id,
            note = null, createdAt = 0L, updatedAt = 0L,
        ),
        category = category,
    )
}
