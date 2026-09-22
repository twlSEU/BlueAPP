package com.example.blue.feature.accounting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.model.AccountType
import com.example.blue.model.toAccountSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayInk = Color(0xFF273C48)
private val DayMuted = Color(0xFF75828B)
private val DayExpense = Color(0xFFB85A59)
private val DayIncome = Color(0xFF32836D)
private val DayLine = Color(0xFFE7ECEF)
private val dayTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayDateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE", Locale.CHINA)

@Composable
internal fun AccountingDayContent(
    date: LocalDate,
    entries: List<AccountEntryWithCategory>,
    onEdit: (String?) -> Unit,
    onDelete: (AccountEntryWithCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = remember(entries) { entries.map { it.entry }.toAccountSummary() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "day-summary") {
            Column(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(date.format(dayDateFormat), style = MaterialTheme.typography.bodySmall, color = DayMuted)
                Spacer(Modifier.height(2.dp))
                Text("当日支出", style = MaterialTheme.typography.labelLarge, color = DayMuted)
                BasicText(
                    "¥ ${AmountUtils.formatCents(summary.expenseInCents)}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = DayInk, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 36.sp),
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DayLine)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    DaySecondaryAmount("收入", summary.incomeInCents, DayIncome, Modifier.weight(1f))
                    DaySecondaryAmount(
                        "结余", summary.balanceInCents,
                        if (summary.balanceInCents < 0L) DayExpense else DayInk,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        item(key = "day-detail-heading") {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("收支明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DayInk)
                Text("${entries.size} 笔", style = MaterialTheme.typography.labelMedium, color = DayMuted)
            }
        }
        if (entries.isEmpty()) {
            item(key = "day-empty") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_wallet), null, Modifier.size(36.dp), tint = DayMuted)
                    Text("当天暂无账目", style = MaterialTheme.typography.bodyMedium, color = DayMuted)
                    TextButton(onClick = { onEdit(null) }) { Text("记一笔") }
                }
            }
        } else {
            items(entries, key = { it.entry.id }, contentType = { "day-entry" }) { item ->
                DayEntryRow(
                    item, onEdit = { onEdit(item.entry.id) }, onDelete = { onDelete(item) },
                    modifier = Modifier.padding(horizontal = 20.dp).animateItem(),
                )
            }
        }
    }
}

@Composable
private fun DaySecondaryAmount(label: String, cents: Long, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DayMuted)
        BasicText(
            "¥ ${AmountUtils.formatCents(cents)}", modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 18.sp),
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
    val tint = if (income) DayIncome else DayExpense
    val icon = when (item.category.name) {
        "餐饮" -> R.drawable.ic_food
        "交通" -> R.drawable.ic_transport
        "购物" -> R.drawable.ic_shopping
        else -> if (income) R.drawable.ic_trending_up else R.drawable.ic_wallet
    }
    Card(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).combinedClickable(
            onClick = onEdit,
            onLongClickLabel = "删除账目",
            onLongClick = onDelete,
        ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DayLine),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(
                        if (income) Color(0xFFEDF6F1) else Color(0xFFFAF0ED), RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            item.entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, color = DayInk,
                        )
                    Text(
                        "${item.category.name} · ${item.entry.entryTime.format(dayTimeFormat)}",
                        style = MaterialTheme.typography.bodySmall, color = DayMuted,
                    )
                    item.entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp), color = DayMuted,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                BasicText(
                    "${if (income) "+" else "−"} ¥${AmountUtils.formatCents(item.entry.amountInCents)}",
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = tint, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 22.sp),
                )
            }
    }
}
