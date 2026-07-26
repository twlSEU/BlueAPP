package com.example.blue.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R
import java.time.LocalTime

private val TimeSelectorCard = Color(0xFFFFFFFF)
private val TimeSelectorTitle = Color(0xFF20384A)
private val TimeSelectorBody = Color(0xFF465E70)
private val TimeSelectorMuted = Color(0xFF8798A6)
private val TimeSelectorBlue = Color(0xFF3D7BE5)

@Composable
fun AppDateTimeSelectorRow(
    date: String,
    time: String,
    onSelectDate: () -> Unit,
    onSelectTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppDateSelector(
            date = date,
            onClick = onSelectDate,
            modifier = Modifier.weight(1.25f),
        )
        AppTimeSelector(
            time = time,
            onClick = onSelectTime,
            modifier = Modifier.weight(0.85f),
        )
    }
}

@Composable
fun AppDateSelector(
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "选择日期",
) {
    AppDateTimeSelector(
        value = date,
        placeholder = "---- -- --",
        iconRes = R.drawable.ic_calendar,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun AppTimeSelector(
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "选择时间",
) {
    AppDateTimeSelector(
        value = time,
        placeholder = "--:--",
        iconRes = R.drawable.ic_clock,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AppDateTimeSelector(
    value: String,
    placeholder: String,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = TimeSelectorCard,
        border = BorderStroke(1.dp, Color(0xFFE1EAF0)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TimeSelectorBody,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = value.ifBlank { placeholder },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.SemiBold,
                color = TimeSelectorTitle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
                tint = TimeSelectorMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    selectedTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true,
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "选择时间",
                modifier = Modifier.padding(bottom = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TimeSelectorTitle,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                },
            ) {
                Text("确定", color = TimeSelectorBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TimeSelectorMuted)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = TimeSelectorCard,
    ) {
        TimePicker(state = pickerState)
    }
}
