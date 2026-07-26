package com.example.blue.feature.sleep

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.repository.SleepRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.AppTimePickerDialog
import com.example.blue.feature.common.AppTimeSelector
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private val SleepBackground = Color(0xFFF6F8FC)
private val SleepSurface = Color(0xFFFEFFFF)
private val SleepText = Color(0xFF2D4555)
private val SleepMuted = Color(0xFF748895)
private val SleepAccent = Color(0xFF637FC3)
private val SleepAccentSoft = Color(0xFFEAF0FC)
private val SleepEstimated = Color(0xFFB87B50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepEditorScreen(
    repository: SleepRepository,
    estimator: SleepTimeEstimator,
    initialDate: LocalDate,
    onOpenUsageAccessSettings: () -> Unit,
    onShowMessage: (String, Boolean) -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factory = remember(repository, estimator, initialDate) {
        SleepEditorViewModel.factory(repository, estimator, initialDate)
    }
    val editorViewModel: SleepEditorViewModel = viewModel(
        key = "sleep-editor-$initialDate",
        factory = factory,
    )
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editorViewModel) {
        editorViewModel.events.collect { event ->
            when (event) {
                is SleepEditorEvent.Message -> onShowMessage(event.text, event.isError)
                SleepEditorEvent.Saved -> onSaved()
                SleepEditorEvent.Deleted -> {
                    onShowMessage("睡眠记录已删除", false)
                    onSaved()
                }
            }
        }
    }

    SleepEditorContent(
        state = state,
        onBack = onBack,
        onDelete = { showDeleteDialog = true },
        onSelectDate = { showDatePicker = true },
        onSelectSleepTime = { showTimePicker = true },
        onRetryEstimate = editorViewModel::attemptEstimate,
        onConfirmEstimate = editorViewModel::confirmEstimate,
        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
        onNoteChange = editorViewModel::updateNote,
        onSave = editorViewModel::save,
        modifier = modifier,
    )

    if (showDatePicker) {
        val allowedMonth = YearMonth.from(state.recordDate)
        AppDatePickerDialog(
            selectedDate = state.recordDate,
            lockedMonth = allowedMonth,
            latestDate = LocalDate.now(),
            helperText = "${allowedMonth.year}年${allowedMonth.monthValue}月 · 仅过去的日期",
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                editorViewModel.selectDate(selectedDate)
                showDatePicker = false
            },
        )
    }
    if (showTimePicker) {
        AppTimePickerDialog(
            selectedTime = state.sleepTimeText.toLocalTimeOrNull() ?: LocalTime.now(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { selectedTime ->
                editorViewModel.selectSleepTime(selectedTime)
                showTimePicker = false
            },
        )
    }
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "删除这条睡眠记录？",
            message = "删除后将无法恢复。",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                editorViewModel.delete()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepEditorContent(
    state: SleepEditorUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSelectDate: () -> Unit,
    onSelectSleepTime: () -> Unit,
    onRetryEstimate: () -> Unit,
    onConfirmEstimate: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleepBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (state.existingRecord == null) "记录睡眠" else "编辑睡眠",
                        fontWeight = FontWeight.SemiBold,
                        color = SleepText,
                    )
                },
                navigationIcon = { AppBackButton(onClick = onBack) },
                actions = {
                    if (state.existingRecord != null) {
                        TextButton(onClick = onDelete) {
                            Text("删除", color = Color(0xFFC45D5D))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleepBackground,
                    scrolledContainerColor = SleepBackground,
                ),
            )
        },
        bottomBar = {
            SleepSaveBar(
                saving = state.isSaving,
                onSave = onSave,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SleepAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "date") {
                    SleepDateCard(date = state.recordDate, onClick = onSelectDate)
                }
                item(key = "sleep-time") {
                    SleepTimeCard(
                        state = state,
                        onSelectSleepTime = onSelectSleepTime,
                        onRetryEstimate = onRetryEstimate,
                        onConfirm = onConfirmEstimate,
                        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                    )
                }
                item(key = "note") {
                    SleepSectionCard {
                        SleepSectionHeader("备注")
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = onNoteChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例如：睡前喝了咖啡") },
                            minLines = 2,
                            maxLines = 2,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleepAccent.copy(alpha = 0.72f),
                                unfocusedBorderColor = Color(0xFFDCE7EE),
                                focusedContainerColor = Color(0xFFFBFCFE),
                                unfocusedContainerColor = Color(0xFFFBFCFE),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepDateCard(date: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SleepSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(SleepAccentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = SleepAccent,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "记录归属日",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = SleepMuted,
                )
                Text(
                    text = date.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SleepText,
                )
            }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = "选择记录日期",
                    modifier = Modifier.size(23.dp),
                    tint = SleepMuted,
                )
            }
        }
    }
}

@Composable
private fun SleepTimeCard(
    state: SleepEditorUiState,
    onSelectSleepTime: () -> Unit,
    onRetryEstimate: () -> Unit,
    onConfirm: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
) {
    val estimated = state.source == SleepSource.SYSTEM_ESTIMATE
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleepSurface),
        border = BorderStroke(1.dp, Color(0xFFE0E8EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).background(SleepAccentSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("☾", style = MaterialTheme.typography.titleLarge, color = SleepAccent)
                }
                Text(
                    text = "入睡时间",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SleepText,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (estimated) Color(0xFFFFF0E5) else SleepAccentSoft,
                ) {
                    Text(
                        when (state.source) {
                            SleepSource.MANUAL -> "手动"
                            SleepSource.SYSTEM_ESTIMATE -> "系统推测"
                            SleepSource.MANUAL_CONFIRMED -> "已确认"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (estimated) SleepEstimated else SleepAccent,
                    )
                }
            }
            AppTimeSelector(
                time = state.sleepTimeText,
                onClick = onSelectSleepTime,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "选择入睡时间",
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (estimated) Color(0xFFFFF7F1) else Color(0xFFF5F7FA),
            ) {
                Text(
                    state.estimateMessage ?: "点击上方选择入睡时间",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = SleepMuted,
                )
            }
            if (estimated) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleepAccent),
                ) {
                    Text("确认时间", fontWeight = FontWeight.SemiBold)
                }
            }
            if (state.sleepTimeText.isBlank() || state.usageAccessRequired) {
                OutlinedButton(
                    onClick = onRetryEstimate,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("尝试系统推测")
                }
            }
            if (state.usageAccessRequired) {
                TextButton(
                    onClick = onOpenUsageAccessSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打开使用情况访问")
                }
            }
        }
    }
}

@Composable
private fun SleepSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SleepSurface),
        border = BorderStroke(1.dp, Color(0xFFDCE7EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun SleepSectionHeader(title: String) = Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = SleepText,
)

@Composable
private fun SleepSaveBar(saving: Boolean, onSave: () -> Unit) {
    Surface(color = SleepBackground) {
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 14.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleepAccent),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_save),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(if (saving) "正在保存…" else "保存睡眠记录", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(
    name = "记录睡眠 · 系统推测",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun SleepEditorScreenPreview() {
    BlueTheme(dynamicColor = false) {
        SleepEditorContent(
            state = SleepEditorUiState(
                recordDate = LocalDate.of(2026, 7, 16),
                sleepTimeText = "23:42",
                note = "睡前阅读，入睡状态平稳",
                source = SleepSource.SYSTEM_ESTIMATE,
                isEstimated = true,
                isLoading = false,
                estimateMessage = "根据夜间最后一次屏幕关闭时间推测",
            ),
            onBack = {},
            onDelete = {},
            onSelectDate = {},
            onSelectSleepTime = {},
            onRetryEstimate = {},
            onConfirmEstimate = {},
            onOpenUsageAccessSettings = {},
            onNoteChange = {},
            onSave = {},
        )
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching {
    LocalTime.parse(this)
}.getOrNull()
