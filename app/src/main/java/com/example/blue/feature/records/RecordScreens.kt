package com.example.blue.feature.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blue.R
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import com.example.blue.data.local.entity.RecordTypeOverview
import com.example.blue.data.repository.RecordRepository
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RecordListScreen(repository: RecordRepository, onOpenType: (String) -> Unit, onCreateType: () -> Unit, onBack: () -> Unit) {
    val types by remember(repository) { repository.observeTypes() }.collectAsStateWithLifecycle(initialValue = null)
    RecordListContent(types, onOpenType, onCreateType, onBack)
}

@Composable
private fun RecordListContent(types: List<RecordTypeOverview>?, onOpenType: (String) -> Unit, onCreateType: () -> Unit, onBack: () -> Unit) {
    RecordPage("记录", onBack, onCreateType, "新增记录项目") { padding ->
        LazyColumn(
            Modifier.widthIn(max = 600.dp).fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("看见每一点变化", fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk)
                    Text("体重、饮水、运动，为日常留下刻度", fontSize = 12.sp, lineHeight = 19.sp, color = RefinedMuted)
                }
            }
            when {
                types == null -> item { RecordLoading() }
                types.isEmpty() -> item {
                    RecordEmptyState("从一个小记录开始", "创建一个项目，添加数据后就能看到变化趋势。", "新增记录项目", onCreateType)
                }
                else -> items(types, key = { it.id }) { type ->
                    RefinedCard(Modifier.clickable { onOpenType(type.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(46.dp).background(RefinedTeal.copy(alpha = 0.08f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_records), null, Modifier.size(25.dp), tint = RefinedTeal)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(type.name, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${type.recordCount} 次记录", fontSize = 12.sp, lineHeight = 18.sp, color = RefinedMuted)
                            }
                            RecordChevron()
                        }
                        val value = type.latestValue
                        if (value == null) {
                            Text("还没有数据，点击记录第一次", fontSize = 13.sp, lineHeight = 20.sp, color = RefinedMuted)
                        } else {
                            RecordValue(value, type.unit)
                            Text("最近记录 · ${recordDateLabel(requireNotNull(type.latestRecordedAt))}", fontSize = 12.sp, lineHeight = 18.sp, color = RefinedMuted)
                        }
                    }
                }
            }
        }
    }
}

private data class RecordDetailState(val type: RecordTypeEntity?, val records: List<NumericRecordEntity>)

@Composable
fun RecordDetailScreen(
    repository: RecordRepository,
    typeId: String,
    onCreateRecord: () -> Unit,
    onEditRecord: (String) -> Unit,
    onEditType: () -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String, Boolean) -> Unit,
) {
    val state by remember(repository, typeId) {
        kotlinx.coroutines.flow.combine(repository.observeType(typeId), repository.observeRecords(typeId)) { type, records -> RecordDetailState(type, records) }
    }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    RecordDetailContent(
        loading = state == null, type = state?.type, records = state?.records.orEmpty(),
        onCreateRecord = onCreateRecord, onEditRecord = onEditRecord, onEditType = onEditType,
        onDeleteType = { confirmDelete = true }, onBack = onBack,
    )
    if (confirmDelete) {
        DeleteConfirmationDialog(
            title = "删除这个记录项目？", message = "“${state?.type?.name.orEmpty()}”中的所有数据也会一并删除。",
            onDismiss = { if (!deleting) confirmDelete = false },
            onConfirm = {
                if (!deleting) {
                    deleting = true
                    scope.launch {
                        try {
                            repository.deleteType(typeId)
                            confirmDelete = false
                            onShowMessage("记录项目已删除", false)
                            onBack()
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            onShowMessage(error.message ?: "删除失败，请重试", true)
                        } finally { deleting = false }
                    }
                }
            },
        )
    }
}

@Composable
private fun RecordDetailContent(
    loading: Boolean, type: RecordTypeEntity?, records: List<NumericRecordEntity>,
    onCreateRecord: () -> Unit, onEditRecord: (String) -> Unit, onEditType: () -> Unit, onDeleteType: () -> Unit, onBack: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val latest = remember(records) { recentRecordPoints(records).lastOrNull() }
    RecordPage(
        title = type?.name ?: "记录详情", onBack = onBack,
        onAdd = onCreateRecord.takeIf { type != null }, addLabel = "新增数据",
        action = if (type == null) null else ({
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(painterResource(R.drawable.ic_home_edit), "管理记录项目", Modifier.size(21.dp), tint = RefinedInk)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, shape = RoundedCornerShape(18.dp), containerColor = Color.White) {
                    DropdownMenuItem(text = { Text("编辑项目") }, onClick = { menuExpanded = false; onEditType() })
                    DropdownMenuItem(text = { Text("删除项目") }, onClick = { menuExpanded = false; onDeleteType() })
                }
            }
        }),
    ) { padding ->
        LazyColumn(
            Modifier.widthIn(max = 600.dp).fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                loading -> item { RecordLoading() }
                type == null -> item { RecordEmptyState("项目已不存在", "返回后查看其他记录项目。", "返回", onBack) }
                latest == null -> item { RecordEmptyState("留下第一次记录", "添加一条数据，开始看见变化。", "新增数据", onCreateRecord) }
                else -> {
                    item {
                        RefinedCard(title = "最近记录", subtitle = recordDateLabel(latest.recordedAt, "yyyy年M月d日 HH:mm"), iconRes = R.drawable.ic_records, accent = RefinedTeal) {
                            RecordValue(latest.value, type.unit)
                        }
                    }
                    item {
                        RefinedCard(title = "最近七次", subtitle = "按记录时间展示 · ${minOf(records.size, 7)} 次记录", accent = RefinedTeal) {
                            RecordLineChart(records, type.unit)
                            if (records.size == 1) Text("已有第一次记录，继续记录即可连成趋势线。", fontSize = 12.sp, lineHeight = 18.sp, color = RefinedMuted)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("全部记录", Modifier.weight(1f), fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk)
                            Text("${records.size} 次", fontSize = 12.sp, color = RefinedMuted)
                        }
                    }
                    items(records, key = { it.id }) { record ->
                        RefinedCard(Modifier.clickable { onEditRecord(record.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(recordDateLabel(record.recordedAt, "yyyy年M月d日 HH:mm"), fontSize = 12.sp, lineHeight = 18.sp, color = RefinedMuted)
                                    Text("${formatRecordValue(record.value)} ${type.unit}".trim(), fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (record.note.isNotBlank()) Text(record.note, fontSize = 13.sp, lineHeight = 20.sp, color = RefinedMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                RecordChevron()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecordPage(
    title: String, onBack: () -> Unit, onAdd: (() -> Unit)? = null, addLabel: String = "新增",
    action: (@Composable () -> Unit)? = null, content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = RefinedBackground,
        topBar = { RefinedTopBar(title, onBack, action) },
        floatingActionButton = {
            if (onAdd != null) AppAnimatedFloatingAction {
                FloatingActionButton(onClick = onAdd, shape = CircleShape, containerColor = RefinedTeal, contentColor = Color.White, modifier = Modifier.size(58.dp)) {
                    Icon(painterResource(R.drawable.ic_home_add), addLabel, Modifier.size(26.dp))
                }
            }
        },
    ) { padding -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { content(padding) } }
}

@Composable
internal fun RecordEmptyState(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    RefinedCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(62.dp).background(RefinedTeal.copy(alpha = 0.08f), RoundedCornerShape(21.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_records), null, Modifier.size(31.dp), tint = RefinedTeal)
            }
            Text(title, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk, textAlign = TextAlign.Center)
            Text(message, fontSize = 13.sp, lineHeight = 21.sp, color = RefinedMuted, textAlign = TextAlign.Center)
            Surface(onClick = onAction, shape = CircleShape, color = RefinedTeal.copy(alpha = 0.09f)) {
                Text(actionLabel, Modifier.padding(horizontal = 23.dp, vertical = 13.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = RefinedTeal)
            }
        }
    }
}

@Composable
internal fun RecordLoading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RefinedTeal, modifier = Modifier.size(28.dp)) }
}

@Composable
private fun RecordValue(value: Double, unit: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            formatRecordValue(value), Modifier.weight(1f, fill = false), fontSize = 36.sp, lineHeight = 46.sp,
            fontWeight = FontWeight.SemiBold, color = RefinedTeal, maxLines = 1, softWrap = false,
            autoSize = TextAutoSize.StepBased(12.sp, 36.sp, 0.5.sp),
        )
        if (unit.isNotBlank()) Text(unit, Modifier.padding(bottom = 5.dp).widthIn(max = 100.dp), fontSize = 13.sp, lineHeight = 19.sp, color = RefinedMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecordChevron() { Icon(painterResource(R.drawable.ic_home_chevron_right), null, Modifier.size(14.dp), tint = RefinedMuted) }

internal fun sampleRecordType() = RecordTypeEntity("weight", "体重", "kg", 0, 0)

internal fun sampleNumericRecords(): List<NumericRecordEntity> {
    val today = LocalDateTime.of(2026, 11, 3, 8, 0)
    return listOf(65.8, 65.6, 65.9, 65.4, 65.2, 65.5, 65.1, 64.9, 65.0).mapIndexed { index, value ->
        val time = today.minusDays((8 - index).toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        NumericRecordEntity("sample-$index", "weight", value, time, if (index == 8) "晨起记录" else "", time, time)
    }.reversed()
}

@Preview(name = "记录 · 项目列表", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun RecordListPreview() {
    BlueTheme(darkTheme = false) {
        RecordListContent(listOf(RecordTypeOverview("weight", "体重", "kg", 9, 65.0, sampleNumericRecords().first().recordedAt), RecordTypeOverview("water", "饮水", "ml", 0, null, null)), {}, {}, {})
    }
}

@Preview(name = "记录 · 暂无项目", showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun RecordEmptyPreview() { BlueTheme(darkTheme = false) { RecordListContent(emptyList(), {}, {}, {}) } }

@Preview(name = "记录 · 体重趋势", showBackground = true, widthDp = 390, heightDp = 1100)
@Preview(name = "记录 · 窄屏大字体", showBackground = true, widthDp = 320, heightDp = 1100, fontScale = 1.5f)
@Composable
private fun RecordDetailPreview() {
    BlueTheme(darkTheme = false) { RecordDetailContent(false, sampleRecordType(), sampleNumericRecords(), {}, {}, {}, {}, {}) }
}

@Preview(name = "记录 · 首次数据", showBackground = true, widthDp = 390, heightDp = 950)
@Composable
private fun RecordSinglePointPreview() {
    BlueTheme(darkTheme = false) { RecordDetailContent(false, sampleRecordType(), sampleNumericRecords().take(1), {}, {}, {}, {}, {}) }
}
