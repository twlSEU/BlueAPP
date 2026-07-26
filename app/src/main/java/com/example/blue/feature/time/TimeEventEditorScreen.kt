package com.example.blue.feature.time

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.data.repository.TimeRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppDatePickerDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.launch

private val EditorBackground = Color(0xFFF5F6F7)
private val EditorSurface = Color(0xFFFEFFFF)
private val EditorTitle = Color(0xFF202D33)
private val EditorBody = Color(0xFF7C878C)
private val EditorAccent = Color(0xFF627F89)
private val EditorAccentSoft = Color(0xFFE8EFF1)
private val EditorBorder = Color(0xFFE0E5E7)
private val EditorDanger = Color(0xFFC86C6C)

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = EditorBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (eventId == null) "添加重要日子" else "编辑重要日子",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = EditorTitle,
                    )
                },
                navigationIcon = { AppBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EditorBackground,
                    scrolledContainerColor = EditorBackground,
                ),
            )
        },
        bottomBar = {
            Surface(color = EditorBackground) {
                Button(
                    onClick = ::save,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 14.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF344D56),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp, pressedElevation = 1.dp),
                ) {
                    Text(if (saving) "保存中…" else "保存", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                EventImagePicker(
                    model = selectedImage ?: existingImagePath?.let(imageStorage::fileFor),
                    onPick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = {
                        selectedImage = null
                        existingImagePath = null
                    },
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = EditorSurface,
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorLabel("事件名称")
                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it.take(40)
                                    errorMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("例如：朋友生日", color = EditorBody.copy(alpha = 0.7f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EditorAccent,
                                    unfocusedBorderColor = EditorBorder,
                                    focusedContainerColor = Color(0xFFFDFEFE),
                                    unfocusedContainerColor = Color(0xFFFDFEFE),
                                ),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorLabel("类型")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                EventTypeOption(
                                    title = "倒数日",
                                    subtitle = "等待一个日子",
                                    selected = type == TimeEventType.COUNTDOWN,
                                    onClick = { type = TimeEventType.COUNTDOWN },
                                    modifier = Modifier.weight(1f),
                                )
                                EventTypeOption(
                                    title = "纪念日",
                                    subtitle = "记住已经发生",
                                    selected = type == TimeEventType.ANNIVERSARY,
                                    onClick = { type = TimeEventType.ANNIVERSARY },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorLabel("日期")
                            Surface(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF7F9F9),
                                border = BorderStroke(1.dp, EditorBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(shape = CircleShape, color = EditorAccentSoft, modifier = Modifier.size(38.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("日", fontWeight = FontWeight.SemiBold, color = EditorAccent)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(
                                            "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = EditorTitle,
                                        )
                                        Text(
                                            "星期${weekdayLabel(date.dayOfWeek)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EditorBody,
                                        )
                                    }
                                    Text("›", style = MaterialTheme.typography.titleLarge, color = EditorAccent)
                                }
                            }
                        }
                    }
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorDanger,
                    )
                }
            }
        }
    }
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
private fun EventImagePicker(
    model: Any?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(
                8.dp,
                RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF87969B).copy(alpha = 0.10f),
                spotColor = Color(0xFF87969B).copy(alpha = 0.12f),
            )
            .clickable(onClick = onPick),
        shape = RoundedCornerShape(28.dp),
        color = EditorAccentSoft,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "事件图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", style = MaterialTheme.typography.headlineSmall, color = EditorAccent)
                        }
                    }
                    Text("选择一张事件图片", fontWeight = FontWeight.SemiBold, color = EditorTitle)
                    Text("让重要日子更容易被认出", style = MaterialTheme.typography.bodySmall, color = EditorBody)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.9f),
            ) {
                Text(
                    if (model == null) "添加图片" else "更换图片",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = EditorTitle,
                )
            }
            if (model != null) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Text("移除", color = EditorDanger, fontWeight = FontWeight.SemiBold)
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
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) EditorAccentSoft else Color(0xFFF7F9F9),
        border = BorderStroke(1.dp, if (selected) EditorAccent.copy(alpha = 0.38f) else EditorBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = EditorTitle)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = EditorBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = EditorTitle,
    )
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
