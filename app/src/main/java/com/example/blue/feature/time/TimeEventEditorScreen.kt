package com.example.blue.feature.time

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.blue.R
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.data.repository.TimeRepository
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedCoral
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.RefinedViolet
import com.example.blue.ui.theme.BlueTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.launch

private val EditorBackground = RefinedBackground
private val EditorSurface = Color(0xFFFEFFFF)
private val EditorTitle = RefinedInk
private val EditorBody = RefinedMuted
private val EditorAccent = RefinedBlue
private val EditorAccentSoft = Color(0xFFEEF4FF)
private val EditorDanger = RefinedCoral

@Composable
fun TimeEventEditorScreen(
    repository: TimeRepository,
    imageStorage: TimeImageStorage,
    eventId: String?,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    onShowMessage: ((String, Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadedEvent by remember { mutableStateOf<TimeEventEntity?>(null) }
    var initialized by rememberSaveable { mutableStateOf(eventId == null) }
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1)) }
    var type by rememberSaveable { mutableStateOf(TimeEventType.COUNTDOWN) }
    var existingImagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (eventId != null) {
            loadedEvent = repository.getEvent(eventId)
        }
    }
    LaunchedEffect(loadedEvent, initialized) {
        val event = loadedEvent ?: return@LaunchedEffect
        if (!initialized) {
            title = event.title
            date = event.eventDate
            type = event.type
            existingImagePath = event.imagePath
            initialized = true
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImage = uri
            errorMessage = null
        }
    }

    fun save() {
        if (title.isBlank()) {
            errorMessage = "请填写事件名称"
            return
        }
        if (saving) return
        scope.launch {
            saving = true
            errorMessage = null
            var copiedImagePath: String? = null
            runCatching {
                val now = System.currentTimeMillis()
                val resolvedImagePath = selectedImage?.let { uri ->
                    imageStorage.copyFromUri(uri).also { copiedImagePath = it }
                } ?: existingImagePath
                val id = loadedEvent?.id ?: UUID.randomUUID().toString()
                repository.saveEvent(
                    TimeEventEntity(
                        id = id,
                        title = title.trim(),
                        eventDate = date,
                        type = type,
                        imagePath = resolvedImagePath,
                        createdAt = loadedEvent?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
                loadedEvent?.imagePath
                    ?.takeIf { it != resolvedImagePath }
                    ?.let { imageStorage.delete(it) }
                id
            }.onSuccess { id ->
                onShowMessage?.invoke("重要日子已保存", false)
                onSaved(id)
            }.onFailure { error ->
                copiedImagePath?.let { imageStorage.delete(it) }
                errorMessage = error.message ?: "保存失败，请稍后重试"
                onShowMessage?.invoke(errorMessage.orEmpty(), true)
            }
            saving = false
        }
    }

    TimeEventEditorContent(
        isEditing = eventId != null, title = title, date = date, type = type,
        imageModel = selectedImage ?: existingImagePath?.let(imageStorage::fileFor),
        saving = saving, errorMessage = errorMessage,
        onTitleChange = { title = it.take(40); errorMessage = null },
        onTypeChange = { type = it },
        onPickDate = { showDatePicker = true },
        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemoveImage = { selectedImage = null; existingImagePath = null },
        onSave = ::save, onBack = onBack,
    )
    if (showDatePicker) {
        AppDatePickerDialog(
            selectedDate = date,
            helperText = "所有日期均可选择",
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                date = selectedDate
                errorMessage = null
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun TimeEventEditorContent(
    isEditing: Boolean,
    title: String,
    date: LocalDate,
    type: TimeEventType,
    imageModel: Any?,
    saving: Boolean,
    errorMessage: String?,
    onTitleChange: (String) -> Unit,
    onTypeChange: (TimeEventType) -> Unit,
    onPickDate: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = EditorBackground,
        topBar = { RefinedTopBar(if (isEditing) "编辑重要日子" else "添加重要日子", onBack) },
        bottomBar = {
            Surface(color = EditorBackground, modifier = Modifier.imePadding()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onSave, enabled = !saving,
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                            .navigationBarsPadding().padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 14.dp)
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditorAccent, contentColor = Color.White,
                            disabledContainerColor = EditorAccent.copy(alpha = 0.35f),
                            disabledContentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    ) {
                        Text(if (saving) "保存中…" else "保存", fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { EventImagePicker(imageModel, onPickImage, onRemoveImage) }
                item {
                    RefinedCard {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditorLabel("事件名称")
                                OutlinedTextField(
                                    value = title, onValueChange = onTitleChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = EditorTitle, letterSpacing = 0.sp),
                                    placeholder = { Text("例如：朋友生日", fontSize = 14.sp, color = EditorBody) },
                                    singleLine = true, shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EditorAccent.copy(alpha = 0.25f),
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color(0xFFF3F6FB), unfocusedContainerColor = Color(0xFFF3F6FB),
                                        cursorColor = EditorAccent,
                                    ),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditorLabel("类型")
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EventTypeOption(
                                        "倒数日", "等待一个日子", type == TimeEventType.COUNTDOWN,
                                        { onTypeChange(TimeEventType.COUNTDOWN) }, Modifier.weight(1f),
                                    )
                                    EventTypeOption(
                                        "纪念日", "记住已经发生", type == TimeEventType.ANNIVERSARY,
                                        { onTypeChange(TimeEventType.ANNIVERSARY) }, Modifier.weight(1f),
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditorLabel("日期")
                                Surface(
                                    onClick = onPickDate, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp), color = Color(0xFFF3F6FB),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(Modifier.size(36.dp).background(EditorAccentSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                            Icon(painterResource(R.drawable.ic_calendar), null, Modifier.size(18.dp), tint = EditorAccent)
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
                                                modifier = Modifier.fillMaxWidth(), fontSize = 16.sp, lineHeight = 22.sp,
                                                fontWeight = FontWeight.Medium, color = EditorTitle, maxLines = 1, softWrap = false,
                                                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                                            )
                                            Text("星期${weekdayLabel(date.dayOfWeek)}", fontSize = 11.sp, lineHeight = 17.sp, color = EditorBody)
                                        }
                                        Icon(painterResource(R.drawable.ic_home_chevron_right), null, Modifier.size(width = 8.dp, height = 14.dp), tint = EditorBody)
                                    }
                                }
                            }
                        }
                    }
                }
                errorMessage?.let { message ->
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = EditorDanger.copy(alpha = 0.07f)) {
                            Text(message, Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, lineHeight = 20.sp, color = EditorDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventImagePicker(model: Any?, onPick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onPick, modifier = Modifier.fillMaxWidth().height(188.dp),
        shape = RoundedCornerShape(24.dp), color = EditorSurface,
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFEDF4FF), Color(0xFFF8FBFF)))),
            contentAlignment = Alignment.Center,
        ) {
            if (model != null) {
                AsyncImage(model, "事件图片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(
                    Modifier.align(Alignment.BottomStart).padding(12.dp),
                    shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.94f),
                ) {
                    Text("更换图片", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EditorTitle)
                }
                Surface(
                    onClick = onRemove, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.94f),
                ) {
                    Text("移除", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = EditorDanger)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_image_placeholder), null, Modifier.size(23.dp), tint = EditorAccent)
                    }
                    Text("选择一张事件图片", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = EditorTitle)
                    Text("让重要日子更容易被认出", fontSize = 11.sp, lineHeight = 17.sp, color = EditorBody)
                }
            }
        }
    }
}

@Composable
private fun EventTypeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anniversary = title == "纪念日"
    val accent = if (anniversary) RefinedViolet else EditorAccent
    Surface(
        onClick = onClick, modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.08f) else Color(0xFFF3F6FB),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painterResource(if (anniversary) R.drawable.ic_clock else R.drawable.ic_calendar), null,
                    Modifier.size(16.dp), tint = if (selected) accent else EditorBody,
                )
                Text(title, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = if (selected) accent else EditorTitle)
            }
            Text(
                subtitle, modifier = Modifier.fillMaxWidth(), fontSize = 10.sp, lineHeight = 16.sp, color = EditorBody,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 10.sp, stepSize = 0.5.sp),
            )
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = EditorBody)
}

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}

@Composable
private fun TimeEventEditorPreviewContent(
    isEditing: Boolean = false,
    error: String? = null,
    saving: Boolean = false,
) {
    BlueTheme(darkTheme = false) {
        TimeEventEditorContent(
            isEditing = isEditing, title = if (isEditing) "第一次远行" else "",
            date = if (isEditing) LocalDate.now().minusDays(310) else LocalDate.now().plusDays(23),
            type = if (isEditing) TimeEventType.ANNIVERSARY else TimeEventType.COUNTDOWN,
            imageModel = null, saving = saving, errorMessage = error,
            onTitleChange = {}, onTypeChange = {}, onPickDate = {}, onPickImage = {},
            onRemoveImage = {}, onSave = {}, onBack = {},
        )
    }
}

@Preview(name = "添加重要日子 · 标准手机", widthDp = 390, heightDp = 1000, showBackground = true)
@Preview(name = "添加重要日子 · 窄屏", widthDp = 320, heightDp = 1000, showBackground = true)
@Preview(name = "添加重要日子 · 大字体", widthDp = 390, heightDp = 1200, fontScale = 1.5f, showBackground = true)
@Composable
private fun TimeEventEditorPreview() {
    TimeEventEditorPreviewContent()
}

@Preview(name = "编辑重要日子 · 纪念日", widthDp = 390, heightDp = 1000, showBackground = true)
@Composable
private fun TimeEventEditPreview() {
    TimeEventEditorPreviewContent(isEditing = true)
}

@Preview(name = "添加重要日子 · 校验提示", widthDp = 390, heightDp = 1000, showBackground = true)
@Composable
private fun TimeEventEditorErrorPreview() {
    TimeEventEditorPreviewContent(error = "请填写事件名称")
}

@Preview(name = "添加重要日子 · 保存中", widthDp = 390, heightDp = 1000, showBackground = true)
@Composable
private fun TimeEventEditorSavingPreview() {
    TimeEventEditorPreviewContent(isEditing = true, saving = true)
}
