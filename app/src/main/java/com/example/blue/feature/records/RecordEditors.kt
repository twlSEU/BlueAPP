package com.example.blue.feature.records

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import com.example.blue.data.repository.RecordRepository
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.AppDateTimeSelectorRow
import com.example.blue.feature.common.AppTimePickerDialog
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedCoral
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.ui.theme.BlueTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RecordTypeEditorScreen(
    repository: RecordRepository, typeId: String?, onSaved: (String) -> Unit,
    onBack: () -> Unit, onShowMessage: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<RecordTypeEntity?>(null) }
    var loading by remember { mutableStateOf(typeId != null) }
    var initialized by rememberSaveable { mutableStateOf(typeId == null) }
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val draftId = rememberSaveable { UUID.randomUUID().toString() }
    val draftCreatedAt = rememberSaveable { System.currentTimeMillis() }
    LaunchedEffect(repository, typeId) {
        if (typeId != null) {
            try {
                original = repository.getType(typeId)
                if (!initialized) original?.let { name = it.name; unit = it.unit; initialized = true }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                error = exception.message ?: "读取项目失败"
            } finally { loading = false }
        }
    }
    BackHandler(enabled = saving) {}
    if (!loading && typeId != null && original == null) {
        RecordPage("编辑记录项目", onBack) { padding ->
            Column(Modifier.widthIn(max = 600.dp).padding(padding).padding(16.dp)) {
                RecordEmptyState("无法打开这个项目", error ?: "项目已不存在，返回后查看其他项目。", "返回", onBack)
            }
        }
        return
    }
    RecordTypeEditorContent(
        isEditing = typeId != null, loading = loading, name = name, unit = unit, saving = saving, error = error,
        onNameChange = { name = it.take(24); error = null }, onUnitChange = { unit = it.take(12); error = null },
        onPreset = { presetName, presetUnit -> name = presetName; unit = presetUnit; error = null },
        onBack = { if (!saving) onBack() },
        onSave = {
            if (!saving && !loading) {
                if (name.isBlank()) error = "请填写项目名称" else {
                    saving = true
                    scope.launch {
                        try {
                            val now = System.currentTimeMillis()
                            val id = original?.id ?: draftId
                            repository.saveType(RecordTypeEntity(id, name, unit, original?.createdAt ?: draftCreatedAt, now))
                            onShowMessage("记录项目已保存", false)
                            onSaved(id)
                        } catch (exception: Exception) {
                            if (exception is CancellationException) throw exception
                            error = exception.message ?: "保存失败，请重试"
                        } finally { saving = false }
                    }
                }
            }
        },
    )
}

@Composable
private fun RecordTypeEditorContent(
    isEditing: Boolean, loading: Boolean, name: String, unit: String, saving: Boolean, error: String?,
    onNameChange: (String) -> Unit, onUnitChange: (String) -> Unit, onPreset: (String, String) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit,
) {
    RecordPage(if (isEditing) "编辑记录项目" else "新增记录项目", onBack) { padding ->
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (loading) RecordLoading() else {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("给日常一个刻度", fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk)
                    Text("自定义项目和单位，记录属于你的变化。", fontSize = 13.sp, lineHeight = 20.sp, color = RefinedMuted)
                }
                RefinedCard {
                    RecordField("项目名称", name, onNameChange, "例如：体重", enabled = !saving)
                    RecordField("单位（选填）", unit, onUnitChange, "例如：kg、ml、分钟", enabled = !saving)
                }
                if (!isEditing) {
                    RefinedCard(title = "从常用项目开始") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("体重" to "kg", "饮水" to "ml", "运动" to "分钟").forEach { (presetName, presetUnit) ->
                                Surface(
                                    onClick = { onPreset(presetName, presetUnit) }, enabled = !saving,
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = RefinedTeal.copy(alpha = 0.06f),
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(presetName, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = RefinedTeal)
                                        Text(presetUnit, fontSize = 11.sp, lineHeight = 17.sp, color = RefinedMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                RecordSaveButton(saving, error, onSave)
            }
        }
    }
}

@Composable
fun NumericRecordEditorScreen(
    repository: RecordRepository, typeId: String, recordId: String?, onSaved: () -> Unit,
    onBack: () -> Unit, onShowMessage: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf<RecordTypeEntity?>(null) }
    var original by remember { mutableStateOf<NumericRecordEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var initialized by rememberSaveable { mutableStateOf(recordId == null) }
    var value by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var recordedAt by rememberSaveable {
        mutableLongStateOf(LocalDateTime.now().withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val draftId = rememberSaveable { UUID.randomUUID().toString() }
    val draftCreatedAt = rememberSaveable { System.currentTimeMillis() }
    LaunchedEffect(repository, typeId, recordId) {
        try {
            type = repository.getType(typeId)
            original = recordId?.let { repository.getRecord(it) }?.takeIf { it.typeId == typeId }
            if (!initialized) original?.let {
                value = formatRecordValue(it.value); note = it.note; recordedAt = it.recordedAt; initialized = true
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            error = exception.message ?: "读取记录失败"
        } finally { loading = false }
    }
    BackHandler(enabled = saving) {}
    val missing = !loading && (type == null || (recordId != null && original == null))
    if (missing) {
        RecordPage("记录数据", onBack) { padding ->
            Column(Modifier.widthIn(max = 600.dp).padding(padding).padding(16.dp)) {
                RecordEmptyState("无法打开这条记录", error ?: "项目或记录已不存在，返回后重试。", "返回", onBack)
            }
        }
        return
    }
    NumericRecordEditorContent(
        type = type, isEditing = recordId != null, loading = loading, value = value, note = note,
        recordedAt = recordedAt, saving = saving, error = error,
        onValueChange = { value = it.take(24); error = null }, onNoteChange = { note = it.take(200); error = null },
        onPickDate = { datePicker = true }, onPickTime = { timePicker = true }, onDelete = { confirmDelete = true },
        onBack = { if (!saving) onBack() },
        onSave = {
            if (!saving && !loading) {
                val parsed = parseRecordValue(value)
                if (parsed == null) error = "请填写有效数字，支持小数和负数，绝对值不超过一万亿" else {
                    saving = true
                    scope.launch {
                        try {
                            val now = System.currentTimeMillis()
                            repository.saveRecord(NumericRecordEntity(original?.id ?: draftId, typeId, parsed, recordedAt, note, original?.createdAt ?: draftCreatedAt, now))
                            onShowMessage("数据已保存", false)
                            onSaved()
                        } catch (exception: Exception) {
                            if (exception is CancellationException) throw exception
                            error = exception.message ?: "保存失败，请重试"
                        } finally { saving = false }
                    }
                }
            }
        },
    )
    val localDateTime = Instant.ofEpochMilli(recordedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
    if (datePicker) AppDatePickerDialog(
        selectedDate = localDateTime.toLocalDate(), onDismiss = { datePicker = false },
        onDateSelected = {
            recordedAt = LocalDateTime.of(it, localDateTime.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            datePicker = false
        },
    )
    if (timePicker) AppTimePickerDialog(
        selectedTime = localDateTime.toLocalTime(), onDismiss = { timePicker = false },
        onTimeSelected = {
            recordedAt = LocalDateTime.of(localDateTime.toLocalDate(), it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            timePicker = false
        },
    )
    if (confirmDelete) DeleteConfirmationDialog(
        title = "删除这条数据？", message = "删除后，趋势图会根据剩余记录更新。",
        onDismiss = { if (!saving) confirmDelete = false },
        onConfirm = {
            if (!saving && recordId != null) {
                saving = true
                scope.launch {
                    try {
                        repository.deleteRecord(recordId)
                        confirmDelete = false
                        onShowMessage("数据已删除", false)
                        onSaved()
                    } catch (exception: Exception) {
                        if (exception is CancellationException) throw exception
                        error = exception.message ?: "删除失败，请重试"
                        confirmDelete = false
                    } finally { saving = false }
                }
            }
        },
    )
}

@Composable
private fun NumericRecordEditorContent(
    type: RecordTypeEntity?, isEditing: Boolean, loading: Boolean, value: String, note: String,
    recordedAt: Long, saving: Boolean, error: String?, onValueChange: (String) -> Unit, onNoteChange: (String) -> Unit,
    onPickDate: () -> Unit, onPickTime: () -> Unit, onDelete: () -> Unit, onSave: () -> Unit, onBack: () -> Unit,
) {
    RecordPage(if (isEditing) "编辑数据" else "新增数据", onBack) { padding ->
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (loading) RecordLoading() else {
                RefinedCard(title = type?.name, subtitle = "记录每一次真实的变化", accent = RefinedTeal) {
                    RecordField(if (type?.unit.isNullOrBlank()) "数值" else "数值 · ${type?.unit}", value, onValueChange, "填写本次数据", keyboardType = KeyboardType.Decimal, enabled = !saving)
                }
                RefinedCard(title = "记录时间") {
                    AppDateTimeSelectorRow(
                        date = recordDateLabel(recordedAt, "yyyy-MM-dd"), time = recordDateLabel(recordedAt, "HH:mm"),
                        onSelectDate = { if (!saving) onPickDate() }, onSelectTime = { if (!saving) onPickTime() },
                    )
                }
                RefinedCard {
                    RecordField("备注（选填）", note, onNoteChange, "比如：晨起、运动后……", singleLine = false, enabled = !saving)
                }
                RecordSaveButton(saving, error, onSave)
                if (isEditing) TextButton(onClick = onDelete, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("删除这条数据", color = RefinedCoral, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RecordField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = RefinedMuted)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
            placeholder = { Text(placeholder, fontSize = 15.sp, color = RefinedMuted) },
            textStyle = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, color = RefinedInk),
            singleLine = singleLine, minLines = if (singleLine) 1 else 3, maxLines = if (singleLine) 1 else 6,
            shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RefinedBackground, unfocusedContainerColor = RefinedBackground,
                disabledContainerColor = RefinedBackground, focusedBorderColor = RefinedTeal.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.Transparent, disabledBorderColor = Color.Transparent, cursorColor = RefinedTeal,
            ),
        )
    }
}

@Composable
private fun RecordSaveButton(saving: Boolean, error: String?, onSave: () -> Unit) {
    if (error != null) Text(error, fontSize = 13.sp, lineHeight = 20.sp, color = RefinedCoral)
    Button(
        onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = RefinedTeal),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) { Text(if (saving) "正在保存…" else "保存", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
}

@Preview(name = "记录 · 新增项目", showBackground = true, widthDp = 390, heightDp = 900)
@Preview(name = "记录 · 新增项目窄屏大字体", showBackground = true, widthDp = 320, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun RecordTypeEditorPreview() {
    BlueTheme(darkTheme = false) { RecordTypeEditorContent(false, false, "体重", "kg", false, null, {}, {}, { _, _ -> }, {}, {}) }
}

@Preview(name = "记录 · 新增数据", showBackground = true, widthDp = 390, heightDp = 1000)
@Preview(name = "记录 · 新增数据窄屏大字体", showBackground = true, widthDp = 320, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun NumericRecordEditorPreview() {
    BlueTheme(darkTheme = false) {
        NumericRecordEditorContent(sampleRecordType(), false, false, "65.0", "晨起记录", sampleNumericRecords().first().recordedAt, false, null, {}, {}, {}, {}, {}, {}, {})
    }
}
