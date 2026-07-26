package com.example.blue.feature.time

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.blue.R
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.data.repository.TimeRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private val EventBackground = Color(0xFFF5F6F7)
private val EventSurface = Color(0xFFFEFFFF)
private val EventTitle = Color(0xFF202D33)
private val EventBody = Color(0xFF7C878C)
private val EventAccent = Color(0xFF627F89)
private val EventAccentSoft = Color(0xFFE8EFF1)
private val EventExpired = Color(0xFFA2A9AC)
private val EventDanger = Color(0xFFC86C6C)
private val EventCardShape = RoundedCornerShape(26.dp)

private data class EventPalette(val accent: Color, val soft: Color, val surface: Color)

private fun eventPalette(type: TimeEventType): EventPalette = when (type) {
    TimeEventType.COUNTDOWN -> EventPalette(
        accent = Color(0xFF597985),
        soft = Color(0xFFE8F0F2),
        surface = Color(0xFFFCFEFF),
    )
    TimeEventType.ANNIVERSARY -> EventPalette(
        accent = Color(0xFF967267),
        soft = Color(0xFFF3EBE7),
        surface = Color(0xFFFFFDFC),
    )
}

private enum class EventFilter(val label: String) {
    ALL("全部"),
    COUNTDOWN("倒数日"),
    ANNIVERSARY("纪念日"),
}

internal data class TimeEventStatus(
    val days: Long,
    val unit: String,
    val expired: Boolean,
)

internal fun timeEventStatus(
    event: TimeEventEntity,
    today: LocalDate,
): TimeEventStatus {
    val untilEvent = ChronoUnit.DAYS.between(today, event.eventDate)
    return when (event.type) {
        TimeEventType.COUNTDOWN -> {
            if (untilEvent >= 0) TimeEventStatus(untilEvent, "天后", false)
            else TimeEventStatus(0, "天了", true)
        }
        TimeEventType.ANNIVERSARY -> {
            if (untilEvent > 0) TimeEventStatus(untilEvent, "天后", false)
            else TimeEventStatus(ChronoUnit.DAYS.between(event.eventDate, today), "天了", false)
        }
    }
}

@Composable
fun TimeEventListScreen(
    repository: TimeRepository,
    imageStorage: TimeImageStorage,
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val eventsFlow = remember(repository) { repository.observeEvents() }
    val events by eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(EventFilter.ALL) }
    var menuEvent by remember { mutableStateOf<TimeEventEntity?>(null) }
    var deleteEvent by remember { mutableStateOf<TimeEventEntity?>(null) }
    val filteredEvents = remember(events, filter) {
        val categorized = when (filter) {
            EventFilter.ALL -> events
            EventFilter.COUNTDOWN -> events.filter { it.type == TimeEventType.COUNTDOWN }
            EventFilter.ANNIVERSARY -> events.filter { it.type == TimeEventType.ANNIVERSARY }
        }
        moveExpiredEventsLast(categorized)
    }

    TimeEventListContent(
        events = filteredEvents,
        filter = filter,
        onFilterChange = { filter = it },
        imageStorage = imageStorage,
        onOpenEvent = onOpenEvent,
        onCreateEvent = onCreateEvent,
        onLongPress = { menuEvent = it },
        onBack = onBack,
        showTopBar = showTopBar,
    )

    menuEvent?.let { event ->
        EventActionSheet(
            event = event,
            onEdit = {
                menuEvent = null
                onEditEvent(event.id)
            },
            onDelete = {
                menuEvent = null
                deleteEvent = event
            },
            onDismiss = { menuEvent = null },
        )
    }

    deleteEvent?.let { event ->
        DeleteConfirmationDialog(
            title = "删除“${event.title}”？",
            message = "这个重要日子会从去来中移除，操作无法撤销。",
            onConfirm = {
                deleteEvent = null
                scope.launch {
                    repository.deleteEvent(event.id)
                    event.imagePath?.let { imageStorage.delete(it) }
                }
            },
            onDismiss = { deleteEvent = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeEventListContent(
    events: List<TimeEventEntity>,
    filter: EventFilter,
    onFilterChange: (EventFilter) -> Unit,
    imageStorage: TimeImageStorage?,
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onLongPress: (TimeEventEntity) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val today = remember { LocalDate.now() }
    Scaffold(
        containerColor = EventBackground,
        topBar = { if (showTopBar) EventTopBar(title = "去来", onBack = onBack) },
        floatingActionButton = {
            AppAnimatedFloatingAction {
                FloatingActionButton(
                    onClick = onCreateEvent,
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    containerColor = Color(0xFF344D56),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 7.dp,
                        pressedElevation = 3.dp,
                    ),
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
                }
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            item(key = "event-header") {
                EventListHeader(filter = filter, onFilterChange = onFilterChange)
            }
            if (events.isEmpty()) {
                item(key = "empty-events") {
                    EmptyEventState(
                        filter = filter,
                        onCreateEvent = onCreateEvent,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(180),
                            placementSpec = tween(220),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }
            } else {
                items(events, key = TimeEventEntity::id) { event ->
                    TimeEventCard(
                        event = event,
                        status = timeEventStatus(event, today),
                        imageStorage = imageStorage,
                        onClick = { onOpenEvent(event.id) },
                        onLongClick = { onLongPress(event) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(180),
                            placementSpec = tween(220),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventListHeader(filter: EventFilter, onFilterChange: (EventFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "重要的日子",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = EventTitle,
            )
            Text(
                "记住去处，也珍藏来时",
                style = MaterialTheme.typography.bodyMedium,
                color = EventBody,
            )
        }
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(18.dp),
                color = EventSurface,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        filter.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = EventTitle,
                    )
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = EventBody,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = EventSurface,
            ) {
                EventFilter.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                fontWeight = if (option == filter) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onFilterChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeEventCard(
    event: TimeEventEntity,
    status: TimeEventStatus,
    imageStorage: TimeImageStorage?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "Event card press",
    )
    val primary = if (status.expired) EventExpired else EventTitle
    val secondary = if (status.expired) EventExpired.copy(alpha = 0.82f) else EventBody
    val palette = eventPalette(event.type)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.94f else 1f
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = EventCardShape,
        color = if (status.expired) EventSurface else palette.surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventThumbnail(
                event = event,
                imageStorage = imageStorage,
                expired = status.expired,
                palette = palette,
                modifier = Modifier.size(74.dp),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = eventDateLabel(event.eventDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (status.expired) Color(0xFFF0F1F1) else palette.soft,
                ) {
                    Text(
                        if (event.type == TimeEventType.COUNTDOWN) "倒数日" else "纪念日",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (status.expired) EventExpired else palette.accent,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (status.expired) Color(0xFFF1F2F2) else palette.soft)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val digits = status.days.toString().length
                Text(
                    text = status.days.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = when {
                            digits <= 2 -> 42.sp
                            digits == 3 -> 36.sp
                            digits == 4 -> 31.sp
                            else -> 27.sp
                        },
                        lineHeight = 44.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = status.unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = secondary,
                )
            }
        }
    }
}

@Composable
private fun EventThumbnail(
    event: TimeEventEntity,
    imageStorage: TimeImageStorage?,
    expired: Boolean,
    palette: EventPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF87969B).copy(alpha = 0.10f),
                spotColor = Color(0xFF87969B).copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (expired) Color(0xFFE6E8E9) else palette.soft),
        contentAlignment = Alignment.Center,
    ) {
        val imageFile = if (event.imagePath != null && imageStorage != null) {
            imageStorage.fileFor(event.imagePath)
        } else {
            null
        }
        if (imageFile != null) {
            AsyncImage(
                model = imageFile,
                contentDescription = event.title,
                modifier = Modifier.fillMaxSize().alpha(if (expired) 0.52f else 1f),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(if (expired) Color(0xFFE6E8E9) else palette.soft))
            Text(
                text = event.title.firstOrNull()?.toString() ?: "日",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (expired) EventExpired else palette.accent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventActionSheet(
    event: TimeEventEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = EventSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                event.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = EventTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("选择要进行的操作", style = MaterialTheme.typography.bodyMedium, color = EventBody)
            Spacer(Modifier.height(8.dp))
            Surface(onClick = onEdit, shape = RoundedCornerShape(18.dp), color = EventAccentSoft) {
                Text(
                    "编辑事件",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = EventTitle,
                )
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("删除事件", color = EventDanger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyEventState(
    filter: EventFilter,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 72.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = CircleShape, color = EventAccentSoft, modifier = Modifier.size(70.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("⌛", style = MaterialTheme.typography.headlineMedium, color = EventAccent)
            }
        }
        Text(
            if (filter == EventFilter.ALL) "还没有重要日子" else "还没有${filter.label}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = EventTitle,
        )
        Text("添加一个值得等待或纪念的日子", style = MaterialTheme.typography.bodyMedium, color = EventBody)
        TextButton(onClick = onCreateEvent) { Text("立即添加", color = EventAccent, fontWeight = FontWeight.SemiBold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTopBar(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = EventTitle,
            )
        },
        navigationIcon = { AppBackButton(onClick = onBack) },
        actions = { action?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = EventBackground,
            scrolledContainerColor = EventBackground,
        ),
    )
}

@Composable
fun TimeEventDetailScreen(
    repository: TimeRepository,
    imageStorage: TimeImageStorage,
    eventId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val eventFlow = remember(repository, eventId) { repository.observeEvent(eventId) }
    val event by eventFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = remember { LocalDate.now() }
    Scaffold(
        containerColor = EventBackground,
        topBar = {
            EventTopBar(
                title = "重要日子",
                onBack = onBack,
                action = {
                    TextButton(onClick = onEdit, enabled = event != null) {
                        Text("编辑", color = EventAccent, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        val item = event
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EventAccent)
            }
        } else {
            val status = timeEventStatus(item, today)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(
                            12.dp,
                            RoundedCornerShape(30.dp),
                            ambientColor = Color(0xFF8B969A).copy(alpha = 0.10f),
                            spotColor = Color(0xFF8B969A).copy(alpha = 0.14f),
                        ),
                        shape = RoundedCornerShape(30.dp),
                        color = EventSurface,
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            EventThumbnail(
                                event = item,
                                imageStorage = imageStorage,
                                expired = status.expired,
                                palette = eventPalette(item.type),
                                modifier = Modifier.fillMaxWidth().height(230.dp),
                            )
                            Text(
                                item.title,
                                modifier = Modifier.padding(top = 24.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (status.expired) EventExpired else EventTitle,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                eventDateLabel(item.eventDate),
                                modifier = Modifier.padding(top = 7.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (status.expired) EventExpired else EventBody,
                            )
                            Text(
                                status.days.toString(),
                                modifier = Modifier.padding(top = 24.dp),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (status.expired) EventExpired else EventTitle,
                            )
                            Text(status.unit, style = MaterialTheme.typography.bodyLarge, color = EventBody)
                            Surface(
                                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = EventAccentSoft,
                            ) {
                                Text(
                                    if (item.type == TimeEventType.COUNTDOWN) "倒数日" else "纪念日",
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EventAccent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun eventDateLabel(date: LocalDate): String =
    "%02d/%02d/%02d · 周%s".format(
        date.year % 100,
        date.monthValue,
        date.dayOfMonth,
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
            DayOfWeek.SUNDAY -> "日"
        },
    )

internal fun moveExpiredEventsLast(
    events: List<TimeEventEntity>,
    today: LocalDate = LocalDate.now(),
): List<TimeEventEntity> {
    val (expired, active) = events.partition { timeEventStatus(it, today).expired }
    return active + expired
}

@Preview(name = "去来 · 事件列表", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TimeEventListPreview() {
    val today = LocalDate.now()
    val sample = listOf(
        TimeEventEntity("1", "朋友生日", today.plusDays(3), TimeEventType.COUNTDOWN, null, 0, 0),
        TimeEventEntity("2", "第一次远行", today.minusDays(310), TimeEventType.ANNIVERSARY, null, 0, 0),
        TimeEventEntity("3", "已经结束的计划", today.minusDays(8), TimeEventType.COUNTDOWN, null, 0, 0),
    )
    BlueTheme(dynamicColor = false) {
        TimeEventListContent(
            events = sample,
            filter = EventFilter.ALL,
            onFilterChange = {},
            imageStorage = null,
            onOpenEvent = {},
            onCreateEvent = {},
            onLongPress = {},
            onBack = {},
        )
    }
}
