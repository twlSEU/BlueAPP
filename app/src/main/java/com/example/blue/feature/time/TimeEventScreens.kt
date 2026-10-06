package com.example.blue.feature.time

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.blue.R
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.data.repository.TimeRepository
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedCoral
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.RefinedViolet
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private val EventBackground = RefinedBackground
private val EventSurface = Color(0xFFFEFFFF)
private val EventTitle = RefinedInk
private val EventBody = RefinedMuted
private val EventAccent = RefinedBlue
private val EventAccentSoft = Color(0xFFEEF4FF)
private val EventExpired = Color(0xFF9DADC1)
private val EventDanger = RefinedCoral
private val EventCardShape = RoundedCornerShape(24.dp)
private val EventCardAmbientShadow = Shadow(
    radius = 7.dp,
    spread = 1.dp,
    color = Color(0xFF26313D).copy(alpha = 0.10f),
    offset = DpOffset.Zero,
)
private val EventCardContactShadow = Shadow(
    radius = 4.dp,
    color = Color(0xFF26313D).copy(alpha = 0.09f),
    offset = DpOffset(0.dp, 2.dp),
)

private data class EventPalette(val accent: Color, val soft: Color, val surface: Color)

private fun eventPalette(type: TimeEventType): EventPalette = when (type) {
    TimeEventType.COUNTDOWN -> EventPalette(RefinedBlue, Color(0xFFEEF4FF), Color.White)
    TimeEventType.ANNIVERSARY -> EventPalette(RefinedViolet, Color(0xFFF3F0FC), Color.White)
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
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    containerColor = EventAccent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 7.dp,
                        pressedElevation = 3.dp,
                    ),
                ) {
                    Icon(painterResource(R.drawable.ic_home_add), "添加重要日子", Modifier.size(26.dp))
                }
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                item(key = "event-header", contentType = "event-header") {
                    EventListHeader(filter = filter, onFilterChange = onFilterChange)
                }
                if (events.isEmpty()) {
                    item(key = "empty-events", contentType = "event-status") {
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
                    items(
                        items = events,
                        key = TimeEventEntity::id,
                        contentType = { "time-event" },
                    ) { event ->
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
}

@Composable
private fun EventListHeader(filter: EventFilter, onFilterChange: (EventFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "重要的日子", fontSize = 23.sp, lineHeight = 31.sp,
                fontWeight = FontWeight.SemiBold, color = EventTitle,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 23.sp, stepSize = 0.5.sp),
            )
            Text("记住去处，也珍藏来时", fontSize = 12.sp, lineHeight = 19.sp, color = EventBody)
        }
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(18.dp),
                color = EventSurface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        filter.label, fontSize = 13.sp, lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold, color = EventTitle,
                        maxLines = 1, softWrap = false,
                    )
                    Icon(
                        painterResource(R.drawable.ic_arrow_down), contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = EventBody,
                    )
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
                                option.label, color = EventTitle,
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
    val indication = LocalIndication.current
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
            .dropShadow(EventCardShape, EventCardAmbientShadow)
            .dropShadow(EventCardShape, EventCardContactShadow)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.94f else 1f
            }
            .clip(EventCardShape)
            .combinedClickable(
                interactionSource = interactionSource, indication = indication,
                onClick = onClick, onLongClick = onLongClick,
            ),
        shape = EventCardShape,
        color = if (status.expired) EventSurface else palette.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val compact = maxWidth < 280.dp
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp),
            ) {
                EventThumbnail(
                    event = event, imageStorage = imageStorage, expired = status.expired, palette = palette,
                    modifier = Modifier.size(if (compact) 48.dp else 64.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        event.title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
                        color = primary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        eventDateLabel(event.eventDate), modifier = Modifier.fillMaxWidth(),
                        fontSize = 11.sp, lineHeight = 16.sp, color = secondary, maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp, stepSize = 0.5.sp),
                    )
                    EventTypeBadge(event.type, if (status.expired) EventExpired else palette.accent, if (status.expired) Color(0xFFF1F4F8) else palette.soft)
                }
                Column(
                    Modifier.width(if (compact) 70.dp else 86.dp)
                        .background(if (status.expired) Color(0xFFF2F5F9) else palette.soft, RoundedCornerShape(18.dp))
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        status.days.toString(), modifier = Modifier.fillMaxWidth(),
                        fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.sp,
                        fontWeight = FontWeight.Medium, color = if (status.expired) EventExpired else palette.accent,
                        textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 34.sp, stepSize = 0.5.sp),
                    )
                    Text(status.unit, fontSize = 11.sp, lineHeight = 17.sp, color = secondary)
                }
            }
        }
    }
}

@Composable
private fun EventTypeBadge(type: TimeEventType, accent: Color, background: Color) {
    Surface(shape = RoundedCornerShape(9.dp), color = background) {
        Text(
            if (type == TimeEventType.COUNTDOWN) "倒数日" else "纪念日",
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = accent,
        )
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
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(if (expired) Color(0xFFECF1F7) else palette.soft, if (expired) Color(0xFFF5F7FB) else palette.soft.copy(alpha = 0.5f)))),
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
        EventActionSheetContent(event, onEdit, onDelete)
    }
}

@Composable
private fun EventActionSheetContent(event: TimeEventEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                Icon(painterResource(R.drawable.ic_time), null, Modifier.size(28.dp), tint = EventAccent)
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

@Composable
private fun EventTopBar(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    RefinedTopBar(title, onBack, action)
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
    TimeEventDetailContent(event, imageStorage, today, onEdit, onBack)
}

@Composable
private fun TimeEventDetailContent(
    event: TimeEventEntity?,
    imageStorage: TimeImageStorage?,
    today: LocalDate,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = EventBackground,
        topBar = {
            EventTopBar("重要日子", onBack) {
                TextButton(onClick = onEdit, enabled = event != null) {
                    Text("编辑", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (event != null) EventAccent else EventBody.copy(alpha = 0.4f))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (event == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EventAccent, strokeWidth = 2.5.dp)
                }
            } else {
                val status = timeEventStatus(event, today)
                val palette = eventPalette(event.type)
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 36.dp),
                ) {
                    item {
                        RefinedCard {
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                EventThumbnail(
                                    event, imageStorage, status.expired, palette,
                                    Modifier.fillMaxWidth().aspectRatio(1.9f),
                                )
                                Column(
                                    Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Text(
                                        event.title, modifier = Modifier.fillMaxWidth(),
                                        fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (status.expired) EventExpired else EventTitle, textAlign = TextAlign.Center,
                                    )
                                    Text(eventDateLabel(event.eventDate), fontSize = 12.sp, lineHeight = 19.sp, color = EventBody, textAlign = TextAlign.Center)
                                }
                                Column(
                                    Modifier.fillMaxWidth().background(palette.soft.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(vertical = 18.dp, horizontal = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        status.days.toString(), modifier = Modifier.fillMaxWidth(),
                                        fontSize = 64.sp, lineHeight = 76.sp, letterSpacing = 0.sp,
                                        fontWeight = FontWeight.Medium, color = if (status.expired) EventExpired else palette.accent,
                                        textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                                        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 64.sp, stepSize = 0.5.sp),
                                    )
                                    Text(status.unit, fontSize = 13.sp, lineHeight = 20.sp, color = EventBody)
                                }
                                EventTypeBadge(event.type, palette.accent, palette.soft)
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

private fun timeEventPreviewItems(): List<TimeEventEntity> {
    val today = LocalDate.now()
    return listOf(
        TimeEventEntity("birthday", "朋友生日", today.plusDays(23), TimeEventType.COUNTDOWN, null, 0, 0),
        TimeEventEntity("travel", "第一次远行", today.minusDays(310), TimeEventType.ANNIVERSARY, null, 0, 0),
        TimeEventEntity("life", "那些平凡而值得珍藏的日子", today.minusDays(1234), TimeEventType.ANNIVERSARY, null, 0, 0),
        TimeEventEntity("finished", "已经结束的计划", today.minusDays(8), TimeEventType.COUNTDOWN, null, 0, 0),
    )
}

@Preview(name = "去来 · 标准手机", showBackground = true, widthDp = 390, heightDp = 950)
@Preview(name = "去来 · 窄屏", showBackground = true, widthDp = 320, heightDp = 950)
@Preview(name = "去来 · 大字体", showBackground = true, widthDp = 390, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun TimeEventListPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventListContent(
            events = timeEventPreviewItems(), filter = EventFilter.ALL, onFilterChange = {},
            imageStorage = null, onOpenEvent = {}, onCreateEvent = {}, onLongPress = {}, onBack = {}, showTopBar = true,
        )
    }
}

@Preview(name = "去来 · 下拉筛选窄屏大字体", showBackground = true, widthDp = 320, heightDp = 1050, fontScale = 1.5f)
@Composable
private fun TimeEventFilterPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventListContent(
            events = timeEventPreviewItems().filter { it.type == TimeEventType.COUNTDOWN },
            filter = EventFilter.COUNTDOWN, onFilterChange = {},
            imageStorage = null, onOpenEvent = {}, onCreateEvent = {}, onLongPress = {}, onBack = {}, showTopBar = true,
        )
    }
}

@Preview(name = "去来 · 暂无日子", showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun TimeEventEmptyPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventListContent(emptyList(), EventFilter.ALL, {}, null, {}, {}, {}, {}, showTopBar = true)
    }
}

@Preview(name = "重要日子 · 倒数日", showBackground = true, widthDp = 390, heightDp = 900)
@Preview(name = "重要日子 · 窄屏大字体", showBackground = true, widthDp = 320, heightDp = 1050, fontScale = 1.5f)
@Composable
private fun TimeEventDetailPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventDetailContent(timeEventPreviewItems().first(), null, LocalDate.now(), {}, {})
    }
}

@Preview(name = "重要日子 · 纪念日", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun TimeAnniversaryDetailPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventDetailContent(timeEventPreviewItems()[2], null, LocalDate.now(), {}, {})
    }
}

@Preview(name = "重要日子 · 已结束", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun TimeExpiredDetailPreview() {
    BlueTheme(darkTheme = false) {
        TimeEventDetailContent(timeEventPreviewItems().last(), null, LocalDate.now(), {}, {})
    }
}

@Preview(name = "去来 · 长按操作", showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun TimeEventActionsPreview() {
    BlueTheme(darkTheme = false) {
        Column(Modifier.fillMaxSize().background(EventBackground), verticalArrangement = Arrangement.Bottom) {
            Surface(shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), color = EventSurface) {
                EventActionSheetContent(timeEventPreviewItems().first(), {}, {})
            }
        }
    }
}
