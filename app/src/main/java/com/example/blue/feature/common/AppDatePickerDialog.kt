package com.example.blue.feature.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.blue.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val PickerTitle = Color(0xFF20384A)
private val PickerBody = Color(0xFF465E70)
private val PickerMuted = Color(0xFF8798A6)
private val PickerBlue = Color(0xFF3D7BE5)
private val PickerWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")
private val PickerDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@Composable
fun AppDatePickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    lockedMonth: YearMonth? = null,
    earliestDate: LocalDate? = null,
    latestDate: LocalDate? = null,
    helperText: String? = null,
) {
    var browsedMonth by remember(selectedDate, lockedMonth) {
        mutableStateOf(lockedMonth ?: YearMonth.from(selectedDate))
    }
    val visibleMonth = lockedMonth ?: browsedMonth
    val canMovePrevious = lockedMonth == null &&
        (earliestDate == null || visibleMonth > YearMonth.from(earliestDate))
    val canMoveNext = lockedMonth == null &&
        (latestDate == null || visibleMonth < YearMonth.from(latestDate))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFEFFFF),
            shadowElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEAF2F8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = PickerBody,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "选择日期",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PickerTitle,
                        )
                        Text(
                            helperText ?: "${visibleMonth.year}年${visibleMonth.monthValue}月",
                            style = MaterialTheme.typography.labelMedium,
                            color = PickerMuted,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("取消", color = PickerMuted) }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF4F8FB)) {
                    Text(
                        selectedDate.format(PickerDateFormatter),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PickerTitle,
                        textAlign = TextAlign.Center,
                    )
                }
                if (lockedMonth == null) {
                    MonthNavigator(
                        month = visibleMonth,
                        canMovePrevious = canMovePrevious,
                        canMoveNext = canMoveNext,
                        onPreviousYear = {
                            browsedMonth = earliestDate?.let { maxOf(visibleMonth.minusYears(1), YearMonth.from(it)) }
                                ?: visibleMonth.minusYears(1)
                        },
                        onPreviousMonth = { browsedMonth = visibleMonth.minusMonths(1) },
                        onNextMonth = { browsedMonth = visibleMonth.plusMonths(1) },
                        onNextYear = {
                            browsedMonth = latestDate?.let { minOf(visibleMonth.plusYears(1), YearMonth.from(it)) }
                                ?: visibleMonth.plusYears(1)
                        },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    PickerWeekdays.forEach { weekday ->
                        Text(
                            weekday,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = PickerMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                AnimatedContent(
                    targetState = visibleMonth,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        ((fadeIn(tween(180)) + slideInHorizontally(tween(220)) { width -> direction * width / 4 })
                            .togetherWith(
                                fadeOut(tween(160)) +
                                    slideOutHorizontally(tween(200)) { width -> -direction * width / 4 },
                            )).using(
                            SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ -> tween(220) },
                            ),
                        )
                    },
                    label = "Date picker month",
                ) { displayedMonth ->
                    val calendarCells = remember(displayedMonth) {
                        List(displayedMonth.atDay(1).dayOfWeek.value - 1) { null } +
                            (1..displayedMonth.lengthOfMonth()).map(displayedMonth::atDay)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        calendarCells.chunked(7).forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                repeat(7) { index ->
                                    val date = week.getOrNull(index)
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (date != null) {
                                            val isSelectable =
                                                (earliestDate == null || !date.isBefore(earliestDate)) &&
                                                    (latestDate == null || !date.isAfter(latestDate))
                                            val isSelected = date == selectedDate && isSelectable
                                            Surface(
                                                onClick = { onDateSelected(date) },
                                                enabled = isSelectable,
                                                modifier = Modifier.fillMaxSize(),
                                                shape = CircleShape,
                                                color = if (isSelected) Color(0xFFDCEBFA) else Color.Transparent,
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        date.dayOfMonth.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) {
                                                            FontWeight.SemiBold
                                                        } else {
                                                            FontWeight.Medium
                                                        },
                                                        color = when {
                                                            isSelected -> PickerBlue
                                                            isSelectable -> PickerBody
                                                            else -> Color(0xFFC7D1D8)
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
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
private fun MonthNavigator(
    month: YearMonth,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onPreviousYear: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNextYear: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF7F9FA)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerNavButton("«", canMovePrevious, onPreviousYear)
            PickerNavButton("‹", canMovePrevious, onPreviousMonth)
            Text(
                "${month.year}年 ${month.monthValue}月",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PickerTitle,
                textAlign = TextAlign.Center,
            )
            PickerNavButton("›", canMoveNext, onNextMonth)
            PickerNavButton("»", canMoveNext, onNextYear)
        }
    }
}

@Composable
private fun PickerNavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = if (enabled) PickerBody else PickerMuted.copy(alpha = 0.3f))
    }
}
