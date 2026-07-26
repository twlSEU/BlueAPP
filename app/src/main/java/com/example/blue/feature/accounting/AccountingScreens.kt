package com.example.blue.feature.accounting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.data.repository.AccountRepository
import com.example.blue.data.repository.AccountPeriodSummary
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppAnimatedFloatingAction
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.AppDateTimeSelectorRow
import com.example.blue.feature.common.AppTimePickerDialog
import com.example.blue.feature.common.DeleteConfirmationDialog
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.model.AccountSummary
import com.example.blue.model.AccountType
import com.example.blue.model.toAccountSummary
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val accountDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val accountTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val AccountingBackground = Color(0xFFF6F8FC)
private val AccountingSurface = Color(0xFFFEFFFF)
private val AccountingText = Color(0xFF2D4555)
private val AccountingMuted = Color(0xFF748895)
private val AccountingFogBlue = Color(0xFF86A5BA)
private val AccountingAccent = Color(0xFF3D7BE5)
private val AccountingAccentSoft = Color(0xFFEAF3FF)
private val AccountingIncome = Color(0xFF3F8D78)
private val AccountingIncomeSoft = Color(0xFFEAF6F1)
private val AccountingExpense = Color(0xFFC96868)
private val AccountingExpenseSoft = Color(0xFFFFEEEE)
private val AccountingBorder = Color(0xFFDCE7EE)
private val AccountingCardShape = RoundedCornerShape(24.dp)

private data class DayAmountItem(
    val label: String,
    val value: String,
    val color: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingYearScreen(
    repository: AccountRepository,
    onOpenMonth: (Int, Int) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val today = remember { LocalDate.now() }
    val currentYear = today.year
    var year by rememberSaveable { mutableIntStateOf(currentYear) }
    val monthlyAggregates by remember(repository, year) {
        repository.observeMonthlyAggregates(year)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val grouped = remember(monthlyAggregates) { monthlyAggregates.associateBy { it.month } }
    val displayedMonths = remember(year, today) { accountingMonthsForYear(year, today) }

    Scaffold(
        containerColor = AccountingBackground,
        topBar = { if (showTopBar) AccountTopBar(title = "按年月查看", onBack = onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AccountYearSelector(
                    year = year,
                    canMoveForward = year < currentYear,
                    onYearChange = { year = it },
                )
            }
            items(
                items = displayedMonths,
                key = { month -> "$year-$month" },
            ) { month ->
                val monthAggregate = grouped[month]
                val summary = remember(monthAggregate) {
                    AccountSummary(
                        incomeInCents = monthAggregate?.incomeInCents ?: 0L,
                        expenseInCents = monthAggregate?.expenseInCents ?: 0L,
                    )
                }
                AccountMonthCard(
                    month = month,
                    entryCount = monthAggregate?.entryCount ?: 0,
                    summary = summary,
                    onClick = { onOpenMonth(year, month) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingMonthScreen(
    repository: AccountRepository,
    year: Int,
    month: Int,
    onOpenDay: (Int) -> Unit,
    onCreateEntry: () -> Unit,
    onBack: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val yearMonth = remember(year, month) { YearMonth.of(year, month) }
    val entries by remember(repository, yearMonth) {
        repository.observeMonth(yearMonth)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val monthSummary by remember(repository, yearMonth) {
        repository.observeMonthSummary(yearMonth)
    }.collectAsStateWithLifecycle(initialValue = emptyAccountPeriodSummary())

    AccountingMonthContent(
        yearMonth = yearMonth,
        today = today,
        entries = entries,
        monthSummary = monthSummary,
        onOpenDay = onOpenDay,
        onCreateEntry = onCreateEntry,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountingMonthContent(
    yearMonth: YearMonth,
    today: LocalDate,
    entries: List<AccountEntryWithCategory>,
    monthSummary: AccountPeriodSummary,
    onOpenDay: (Int) -> Unit,
    onCreateEntry: () -> Unit,
    onBack: () -> Unit,
) {
    val grouped = remember(entries) { entries.groupBy { it.entry.entryDate } }
    val summary = remember(monthSummary) {
        AccountSummary(
            incomeInCents = monthSummary.incomeInCents,
            expenseInCents = monthSummary.expenseInCents,
        )
    }
    val displayedDays = remember(grouped) { grouped.keys.map { it.dayOfMonth }.sortedDescending() }

    Scaffold(
        containerColor = AccountingBackground,
        topBar = {
            AccountTopBar(
                title = "${yearMonth.year}年${yearMonth.monthValue}月",
                onBack = onBack,
            )
        },
        floatingActionButton = {
            AppAnimatedFloatingAction {
                FloatingActionButton(
                    onClick = onCreateEntry,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = AccountingAccent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                    ),
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AccountSummaryCard(
                    title = "本月汇总",
                    supportingText = if (monthSummary.entryCount == 0) {
                        "还没有账目记录"
                    } else {
                        buildString {
                            append("共 ${monthSummary.entryCount} 笔 · ${monthSummary.recordDays} 个记账日")
                            if (monthSummary.largestExpenseInCents > 0L) {
                                append(" · 最大支出 ¥${AmountUtils.formatCents(monthSummary.largestExpenseInCents)}")
                            }
                        }
                    },
                    summary = summary,
                )
            }
            items(
                items = displayedDays,
                key = { day -> yearMonth.atDay(day) },
            ) { day ->
                val date = yearMonth.atDay(day)
                val dayEntries = grouped[date].orEmpty()
                val dailySummary = remember(dayEntries) { dayEntries.map { it.entry }.toAccountSummary() }
                AccountDayCard(
                    date = date,
                    isToday = date == today,
                    entryCount = dayEntries.size,
                    summary = dailySummary,
                    onClick = { onOpenDay(day) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingDayScreen(
    repository: AccountRepository,
    year: Int,
    month: Int,
    day: Int,
    onEdit: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val yearMonth = remember(year, month) { YearMonth.of(year, month) }
    val date = remember(yearMonth, day) { yearMonth.atDay(day) }
    val daily by remember(repository, date) {
        repository.observeEntriesForDate(date)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var deleting by remember { mutableStateOf<AccountEntryWithCategory?>(null) }
    val scope = rememberCoroutineScope()
    val dailySummary = remember(daily) { daily.map { it.entry }.toAccountSummary() }

    Scaffold(
        containerColor = AccountingBackground,
        topBar = { AccountTopBar(title = "${month}月${day}日", onBack = onBack) },
        floatingActionButton = {
            AppAnimatedFloatingAction {
                FloatingActionButton(
                    onClick = { onEdit(null) },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = AccountingAccent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                    ),
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AccountSummaryCard(
                    title = "当日汇总",
                    supportingText = "${date.accountingWeekday()} · ${daily.size} 笔账目",
                    summary = dailySummary,
                )
            }
            if (daily.isEmpty()) {
                item(key = "empty-accounts") {
                    EmptyAccountsCard(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(180),
                            placementSpec = tween(220),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }
            } else {
                items(daily, key = { it.entry.id }) { item ->
                    AccountEntryCard(
                        item = item,
                        onClick = { onEdit(item.entry.id) },
                        onDelete = { deleting = item },
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

    deleting?.let { target ->
        DeleteConfirmationDialog(
            title = "删除账目？",
            message = "“${target.entry.name}”删除后将无法恢复。",
            onDismiss = { deleting = null },
            onConfirm = {
                scope.launch {
                    repository.deleteEntry(target.entry.id)
                    deleting = null
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorScreen(
    repository: AccountRepository,
    entryId: String?,
    initialDate: LocalDate? = null,
    onManageCategories: (AccountType) -> Unit,
    onShowMessage: (String, Boolean) -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    var loaded by remember { mutableStateOf<AccountEntryWithCategory?>(null) }
    var initialized by rememberSaveable { mutableStateOf(entryId == null) }
    var typeName by rememberSaveable { mutableStateOf(AccountType.EXPENSE.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable {
        mutableStateOf(defaultAccountEntryDate(initialDate).format(accountDateFormatter))
    }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(accountTimeFormatter)) }
    var note by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val type = AccountType.valueOf(typeName)
    val categories by remember(repository, type) {
        repository.observeCategories(type)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val categoryListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(entryId, initialDate, type) {
        categoryListState.scrollToItem(0)
    }

    LaunchedEffect(entryId) {
        if (entryId != null) loaded = repository.observeEntry(entryId).first()
    }
    LaunchedEffect(loaded, initialized) {
        loaded?.let { item ->
            if (!initialized) {
                typeName = item.entry.type.name
                amount = accountAmountForEditing(item.entry.amountInCents)
                name = item.entry.name
                categoryId = item.entry.categoryId
                date = item.entry.entryDate.format(accountDateFormatter)
                time = item.entry.entryTime.format(accountTimeFormatter)
                note = item.entry.note.orEmpty().take(100)
                initialized = true
            }
        }
    }
    LaunchedEffect(categories, type) {
        if (categories.any { it.type != type }) return@LaunchedEffect
        if (categoryId !in categories.map { it.id }) {
            categoryId = categories.firstOrNull()?.id.orEmpty()
        }
    }

    fun saveEntry() {
        if (saving) return
        val cents = AmountUtils.parseToCents(amount)
        val parsedDate = date.asDate()
        val parsedTime = time.asTime()
        when {
            cents == null -> onShowMessage("金额必须大于 0，且最多两位小数", true)
            name.isBlank() -> onShowMessage("名称不能为空", true)
            categoryId.isBlank() -> onShowMessage("请选择分类", true)
            parsedDate == null || parsedTime == null -> onShowMessage("账目日期或时间无效", true)
            else -> scope.launch {
                saving = true
                runCatching {
                    repository.saveEntry(
                        AccountEntryEntity(
                            id = entryId ?: UUID.randomUUID().toString(),
                            entryDate = parsedDate,
                            entryTime = parsedTime,
                            type = type,
                            amountInCents = cents,
                            name = name.trim(),
                            categoryId = categoryId,
                            note = note.trim().ifBlank { null },
                            createdAt = loaded?.entry?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }.onSuccess {
                    saving = false
                    onShowMessage("账目已保存", false)
                    onSaved()
                }.onFailure { throwable ->
                    saving = false
                    onShowMessage(throwable.message ?: "保存失败，请重试", true)
                }
            }
        }
    }

    Scaffold(
        containerColor = AccountingBackground,
        topBar = {
            AccountEditorTopBar(
                title = if (entryId == null) "新增账目" else "编辑账目",
                onBack = onBack,
            )
        },
        bottomBar = { AccountSaveBar(saving = saving, onSave = ::saveEntry) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "type") {
                AccountTypeSelector(
                    selected = type,
                    onSelected = { typeName = it.name },
                )
            }
            item(key = "main-form") {
                AccountEditorFormCard {
                    AccountAmountField(
                        value = amount,
                        onValueChange = { amount = it },
                    )
                    AccountEditorTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "账目名称",
                        leadingIconRes = R.drawable.ic_tag,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppDateTimeSelectorRow(
                        date = date,
                        time = time,
                        onSelectDate = { showDatePicker = true },
                        onSelectTime = { showTimePicker = true },
                    )
                }
            }
            item(key = "category") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccountEditorSectionTitle("分类")
                    LazyRow(
                        state = categoryListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categories, key = { it.id }) { category ->
                            AccountCategoryChip(
                                category = category,
                                selected = category.id == categoryId,
                                onClick = { categoryId = category.id },
                            )
                        }
                        item(key = "more-categories") {
                            AccountMoreCategoryChip(onClick = { onManageCategories(type) })
                        }
                    }
                }
            }
            item(key = "note") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccountEditorSectionTitle("备注")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(100) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入备注（可选）") },
                        minLines = 3,
                        maxLines = 3,
                        supportingText = {
                            Text(
                                text = "${note.length}/100",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                color = AccountingMuted,
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AccountingText),
                        colors = accountEditorTextFieldColors(),
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        val selectedDate = date.asDate() ?: LocalDate.now()
        AppDatePickerDialog(
            selectedDate = selectedDate,
            helperText = "所有日期均可选择",
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                date = selectedDate.format(accountDateFormatter)
                showDatePicker = false
            },
        )
    }
    if (showTimePicker) {
        AppTimePickerDialog(
            selectedTime = time.asTime() ?: LocalTime.now(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { selectedTime ->
                time = selectedTime.format(accountTimeFormatter)
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    repository: AccountRepository,
    type: AccountType,
    onBack: () -> Unit,
) {
    val categories by remember(repository, type) {
        repository.observeCategories(type, includeInactive = true)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var newName by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun addCategory() {
        if (newName.isNotBlank()) {
            scope.launch {
                repository.saveCategory(
                    AccountCategoryEntity(
                        id = UUID.randomUUID().toString(),
                        name = newName.trim(),
                        type = type,
                        isDefault = false,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                newName = ""
            }
        }
    }

    Scaffold(
        containerColor = AccountingBackground,
        topBar = {
            AccountTopBar(
                title = if (type == AccountType.INCOME) "收入分类" else "支出分类",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AccountSectionCard {
                    AccountSectionTitle(title = "新增分类", supportingText = "创建适合自己的记账分类")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = "分类名称",
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = ::addCategory,
                            enabled = newName.isNotBlank(),
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccountingAccent,
                                contentColor = Color.White,
                                disabledContainerColor = AccountingAccentSoft,
                                disabledContentColor = AccountingFogBlue,
                            ),
                        ) {
                            Text("添加", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            items(categories, key = AccountCategoryEntity::id) { category ->
                AccountCategoryCard(
                    category = category,
                    onActiveChange = { active ->
                        scope.launch { repository.setCustomCategoryActive(category.id, active) }
                    },
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

@Composable
internal fun AccountYearSelector(
    year: Int,
    canMoveForward: Boolean,
    onYearChange: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = AccountingSurface,
        border = BorderStroke(1.dp, AccountingBorder),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onYearChange(year - 1) },
                modifier = Modifier.size(48.dp).semantics { contentDescription = "上一年" },
            ) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = AccountingFogBlue)
            }
            AnimatedContent(
                targetState = year,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { width -> direction * width / 5 })
                        .togetherWith(
                            fadeOut(tween(160)) +
                                slideOutHorizontally(tween(200)) { width -> -direction * width / 5 },
                        )
                },
                label = "Accounting year transition",
            ) { displayedYear ->
                Text(
                    "${displayedYear}年",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AccountingText,
                )
            }
            IconButton(
                onClick = { onYearChange(year + 1) },
                enabled = canMoveForward,
                modifier = Modifier.size(48.dp).semantics { contentDescription = "下一年" },
            ) {
                Text(
                    "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (canMoveForward) AccountingFogBlue else Color(0xFFCBD6DD),
                )
            }
        }
    }
}

@Composable
private fun AccountMonthCard(
    month: Int,
    entryCount: Int,
    summary: AccountSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "Account month card press scale",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = AccountingCardShape,
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A8FE7)),
            )
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${month}月",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AccountingText,
                )
                Text(
                    if (entryCount == 0) "等待第一笔账目" else "$entryCount 笔账目",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccountingMuted,
                )
            }
            AccountCompactSummary(
                summary = summary,
                modifier = Modifier.weight(1.25f),
            )
        }
    }
}

@Composable
private fun AccountDayCard(
    date: LocalDate,
    isToday: Boolean,
    entryCount: Int,
    summary: AccountSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "Account day card press scale",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 126.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = AccountingCardShape,
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp),
        border = if (isToday) BorderStroke(1.dp, AccountingAccent.copy(alpha = 0.18f)) else null,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(width = 50.dp, height = 92.dp),
                shape = RoundedCornerShape(17.dp),
                color = if (isToday) AccountingAccent else Color(0xFFF0F5F8),
                border = if (isToday) null else BorderStroke(1.dp, Color(0xFFE5ECF1)),
                shadowElevation = if (isToday) 5.dp else 0.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) Color.White else AccountingText,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        date.accountingWeekdayShort(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Color.White.copy(alpha = 0.84f) else AccountingFogBlue,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) Color.White else AccountingFogBlue.copy(alpha = 0.72f),
                            ),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (entryCount == 0) "暂无账目" else "$entryCount 笔账目",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entryCount == 0) AccountingMuted else AccountingText,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccountingAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            val strokeWidth = 1.8.dp.toPx()
                            val center = Offset(size.width * 0.52f, size.height * 0.5f)
                            drawLine(
                                color = AccountingAccent,
                                start = Offset(size.width * 0.32f, size.height * 0.22f),
                                end = center,
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = AccountingAccent,
                                start = center,
                                end = Offset(size.width * 0.32f, size.height * 0.78f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEF2F5))
                Spacer(Modifier.height(10.dp))
                AccountDayAmounts(summary = summary)
            }
        }
    }
}

@Composable
internal fun AccountSummaryCard(
    title: String,
    supportingText: String,
    summary: AccountSummary,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AccountingCardShape,
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, AccountingBorder.copy(alpha = 0.72f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AccountingAccent.copy(alpha = 0.82f)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccountingText,
                    )
                    Text(
                        supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccountingMuted,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEF2F5))
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AccountSummaryMetric(
                    label = "收入",
                    amount = summary.incomeInCents,
                    color = AccountingIncome,
                    modifier = Modifier.weight(1f),
                )
                AccountMetricDivider()
                AccountSummaryMetric(
                    label = "支出",
                    amount = summary.expenseInCents,
                    color = AccountingExpense,
                    modifier = Modifier.weight(1f),
                )
                AccountMetricDivider()
                AccountSummaryMetric(
                    label = "结余",
                    amount = summary.balanceInCents,
                    color = if (summary.balanceInCents < 0) AccountingExpense else AccountingText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountSummaryMetric(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AccountingMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "¥${AmountUtils.formatCents(amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccountMetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(42.dp)
            .background(Color(0xFFE9EEF2)),
    )
}

@Composable
private fun AccountCompactSummary(summary: AccountSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF6F9FB))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AccountCompactSummaryLine("收入", summary.incomeInCents, AccountingIncome)
        AccountCompactSummaryLine("支出", summary.expenseInCents, AccountingExpense)
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5EBEF))
        AccountCompactSummaryLine("结余", summary.balanceInCents, AccountingText, emphasize = true)
    }
}

@Composable
private fun AccountCompactSummaryLine(
    label: String,
    amount: Long,
    color: Color,
    emphasize: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AccountingMuted)
        Spacer(Modifier.weight(1f))
        Text(
            text = "¥${AmountUtils.formatCents(amount)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccountDayAmounts(summary: AccountSummary) {
    val amounts = listOf(
        DayAmountItem("收入", "¥${AmountUtils.formatCents(summary.incomeInCents)}", AccountingIncome),
        DayAmountItem("支出", "¥${AmountUtils.formatCents(summary.expenseInCents)}", AccountingExpense),
        DayAmountItem(
            "结余",
            "¥${AmountUtils.formatCents(summary.balanceInCents)}",
            if (summary.balanceInCents < 0) AccountingExpense else AccountingText,
        ),
    )
    val longestAmount = amounts.maxOf { it.value.length }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val useStackedLayout = availableWidth < 205.dp || longestAmount > 11
        if (useStackedLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                amounts.forEach { item ->
                    AccountDayAmountRow(item = item, extraCompact = availableWidth < 170.dp)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                amounts.forEach { item ->
                    AccountDayAmountColumn(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDayAmountColumn(item: DayAmountItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = AccountingMuted,
        )
        Text(
            text = item.value.withAmountBreakOpportunities(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = item.color,
            softWrap = true,
        )
    }
}

@Composable
private fun AccountDayAmountRow(item: DayAmountItem, extraCompact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            item.label,
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AccountingMuted,
        )
        Text(
            text = item.value.withAmountBreakOpportunities(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (extraCompact) 10.sp else 12.sp,
                lineHeight = if (extraCompact) 13.sp else 16.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = item.color,
            textAlign = TextAlign.End,
            softWrap = true,
        )
    }
}

private fun String.withAmountBreakOpportunities(): String = replace(",", ",\u200B")

@Composable
internal fun AccountEntryCard(
    item: AccountEntryWithCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    showDate: Boolean = false,
) {
    val entry = item.entry
    val isIncome = entry.type == AccountType.INCOME
    val amountText = if (isIncome) {
        "+¥${AmountUtils.formatCents(entry.amountInCents)}"
    } else {
        "-¥${AmountUtils.formatCents(entry.amountInCents)}"
    }
    val amountColor = if (isIncome) AccountingIncome else AccountingExpense
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "Account entry card press scale",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE7EDF1)),
        interactionSource = interactionSource,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 18.dp, end = 18.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useStackedHeader = maxWidth < 270.dp || amountText.length > 13
                    if (useStackedHeader) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AccountEntryIdentity(item = item, isIncome = isIncome, showDate = showDate)
                            Spacer(Modifier.height(13.dp))
                            Text(
                                text = amountText.withAmountBreakOpportunities(),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 21.sp,
                                    lineHeight = 26.sp,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = amountColor,
                                textAlign = TextAlign.End,
                                softWrap = true,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            AccountEntryIdentity(
                                item = item,
                                isIncome = isIncome,
                                showDate = showDate,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = amountColor,
                                softWrap = false,
                            )
                        }
                    }
                }
                entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(15.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFFF7F9FB),
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                            color = AccountingMuted.copy(alpha = 0.86f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEDF1F4), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "轻点卡片查看详情",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccountingMuted.copy(alpha = 0.68f),
                )
                Spacer(Modifier.weight(1f))
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AccountingMuted.copy(alpha = 0.72f),
                        ),
                    ) {
                        Text(
                            "删除",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountEntryIdentity(
    item: AccountEntryWithCategory,
    isIncome: Boolean,
    showDate: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = item.entry.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = AccountingText,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isIncome) AccountingIncomeSoft else AccountingExpenseSoft,
            ) {
                Text(
                    text = if (isIncome) "收入" else "支出",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isIncome) AccountingIncome else AccountingExpense,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = buildString {
                    if (showDate) append("${item.entry.entryDate} · ")
                    append("${item.category.name} · ${item.entry.entryTime.format(accountTimeFormatter)}")
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = AccountingMuted.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyAccountsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AccountingCardShape,
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(AccountingAccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text("¥", style = MaterialTheme.typography.titleMedium, color = AccountingAccent)
            }
            Text(
                "当天暂无账目",
                style = MaterialTheme.typography.bodyMedium,
                color = AccountingMuted,
            )
        }
    }
}

@Composable
private fun AccountSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AccountingCardShape,
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun AccountEditorFormCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color(0xFF57758A).copy(alpha = 0.10f),
                spotColor = Color(0xFF57758A).copy(alpha = 0.14f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF7FAFE)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.92f), shape),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 19.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun AccountSectionTitle(title: String, supportingText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AccountingText,
        )
        Text(
            supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = AccountingMuted,
        )
    }
}

@Composable
private fun AccountTypeSelector(selected: AccountType, onSelected: (AccountType) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFEFF3F7),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AccountTypeOption(
                label = "支出",
                selected = selected == AccountType.EXPENSE,
                iconRes = R.drawable.ic_trending_down,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(AccountType.EXPENSE) },
            )
            AccountTypeOption(
                label = "收入",
                selected = selected == AccountType.INCOME,
                iconRes = R.drawable.ic_trending_up,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(AccountType.INCOME) },
            )
        }
    }
}

@Composable
private fun AccountTypeOption(
    label: String,
    selected: Boolean,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = if (selected) 2.dp else 0.dp,
                shape = shape,
                ambientColor = AccountingAccent.copy(alpha = 0.12f),
                spotColor = AccountingAccent.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = if (selected) {
                        listOf(Color(0xFF6A98EA), AccountingAccent)
                    } else {
                        listOf(Color.White, Color.White)
                    },
                ),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selected) Color.White else AccountingMuted,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color.White else AccountingMuted,
            )
        }
    }
}

@Composable
private fun AccountCategoryChip(
    category: AccountCategoryEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) AccountingAccentSoft else Color(0xF2FFFFFF),
        border = BorderStroke(
            1.dp,
            if (selected) AccountingAccent.copy(alpha = 0.36f) else AccountingBorder.copy(alpha = 0.55f),
        ),
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tag),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) AccountingAccent else AccountingMuted,
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AccountingAccent else AccountingText,
            )
        }
    }
}

@Composable
private fun AccountMoreCategoryChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = Color(0xF2FFFFFF),
        border = BorderStroke(1.dp, AccountingBorder.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_grid),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AccountingMuted,
            )
            Text("更多", style = MaterialTheme.typography.bodyMedium, color = AccountingText)
        }
    }
}

@Composable
private fun AccountEditorSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AccountingText,
    )
}

@Composable
private fun AccountAmountField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        placeholder = {
            Text(
                "输入金额",
                style = MaterialTheme.typography.titleLarge,
                color = AccountingMuted.copy(alpha = 0.62f),
            )
        },
        prefix = {
            Text(
                "¥",
                modifier = Modifier.padding(end = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AccountingAccent,
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_calculator),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AccountingMuted,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(20.dp),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = AccountingText,
        ),
        colors = accountEditorTextFieldColors(),
    )
}

@Composable
private fun AccountEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIconRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF7F9FC),
        border = BorderStroke(1.dp, Color(0xFFDDE6ED).copy(alpha = 0.82f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(leadingIconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AccountingMuted,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = AccountingText,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(AccountingAccent),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.titleLarge,
                                color = AccountingMuted.copy(alpha = 0.62f),
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun accountEditorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFF5F8FC),
    unfocusedContainerColor = Color(0xFFF7F9FC),
    focusedBorderColor = AccountingAccent.copy(alpha = 0.46f),
    unfocusedBorderColor = Color(0xFFDDE6ED).copy(alpha = 0.82f),
    focusedLabelColor = AccountingAccent,
    unfocusedLabelColor = AccountingMuted,
    cursorColor = AccountingAccent,
)

@Composable
private fun AccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AccountingText),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFFCFCFE),
            unfocusedContainerColor = Color(0xFFFCFCFE),
            focusedBorderColor = AccountingAccent.copy(alpha = 0.62f),
            unfocusedBorderColor = Color(0xFFE3E9EE),
            focusedLabelColor = AccountingAccent,
            unfocusedLabelColor = AccountingMuted,
            cursorColor = AccountingAccent,
        ),
    )
}

@Composable
private fun AccountCategoryCard(
    category: AccountCategoryEntity,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AccountingSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(AccountingAccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    category.name.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AccountingAccent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AccountingText,
                )
                Text(
                    if (category.isDefault) "系统默认分类" else "自定义分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccountingMuted,
                )
            }
            if (category.isDefault) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F4F7)) {
                    Text(
                        "默认",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccountingMuted,
                    )
                }
            } else {
                Switch(
                    checked = category.isActive,
                    onCheckedChange = onActiveChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccountingAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD9E1E7),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AccountSaveBar(saving: Boolean, onSave: () -> Unit) {
    Surface(color = Color.Transparent) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = AccountingAccent.copy(alpha = 0.10f),
                    spotColor = AccountingAccent.copy(alpha = 0.14f),
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF5F91E8), AccountingAccent),
                    ),
                )
                .clickable(enabled = !saving, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (saving) "正在保存…" else "保存账目",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditorTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccountingText,
                textAlign = TextAlign.Center,
            )
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = 12.dp), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = Color(0xF5FFFFFF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                    shadowElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp),
                            tint = AccountingText,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AccountingBackground,
            scrolledContainerColor = AccountingBackground,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AccountingText,
                textAlign = TextAlign.Center,
            )
        },
        navigationIcon = { AppBackButton(onClick = onBack) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AccountingBackground,
            scrolledContainerColor = AccountingBackground,
        ),
    )
}

@Preview(
    name = "记账 · 月度账目",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun AccountingMonthScreenPreview() {
    val today = LocalDate.of(2026, 7, 17)
    val dining = AccountCategoryEntity(
        id = "preview-dining",
        name = "餐饮",
        type = AccountType.EXPENSE,
        isDefault = true,
        isActive = true,
        createdAt = 0L,
        updatedAt = 0L,
    )
    val salary = AccountCategoryEntity(
        id = "preview-salary",
        name = "工资",
        type = AccountType.INCOME,
        isDefault = true,
        isActive = true,
        createdAt = 0L,
        updatedAt = 0L,
    )
    val entries = listOf(
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-1",
                entryDate = today,
                entryTime = LocalTime.of(12, 30),
                type = AccountType.EXPENSE,
                amountInCents = 2_860L,
                name = "午餐",
                categoryId = dining.id,
                note = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            category = dining,
        ),
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-2",
                entryDate = today.minusDays(2),
                entryTime = LocalTime.of(9, 15),
                type = AccountType.EXPENSE,
                amountInCents = 1_280L,
                name = "早餐与咖啡",
                categoryId = dining.id,
                note = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            category = dining,
        ),
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-3",
                entryDate = today.minusDays(2),
                entryTime = LocalTime.of(8, 0),
                type = AccountType.INCOME,
                amountInCents = 850_000L,
                name = "七月工资",
                categoryId = salary.id,
                note = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            category = salary,
        ),
    )

    BlueTheme(dynamicColor = false) {
        AccountingMonthContent(
            yearMonth = YearMonth.of(2026, 7),
            today = today,
            entries = entries,
            monthSummary = AccountPeriodSummary(
                incomeInCents = 850_000L,
                expenseInCents = 4_140L,
                entryCount = entries.size,
                recordDays = 2,
                largestExpenseInCents = 2_860L,
            ),
            onOpenDay = {},
            onCreateEntry = {},
            onBack = {},
        )
    }
}

internal fun accountingMonthsForYear(
    year: Int,
    today: LocalDate = LocalDate.now(),
): List<Int> = when {
    year == today.year -> (today.monthValue downTo 1).toList()
    year < today.year -> (1..12).toList()
    else -> emptyList()
}

internal fun accountingDaysForMonth(
    yearMonth: YearMonth,
    today: LocalDate = LocalDate.now(),
): List<Int> {
    val currentMonth = YearMonth.from(today)
    return when {
        yearMonth == currentMonth -> (today.dayOfMonth downTo 1).toList()
        yearMonth.isBefore(currentMonth) -> (1..yearMonth.lengthOfMonth()).toList()
        else -> emptyList()
    }
}

internal fun defaultAccountEntryDate(
    selectedDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
): LocalDate = selectedDate ?: today

internal fun accountAmountForEditing(amountInCents: Long): String =
    AmountUtils.formatCents(amountInCents).replace(",", "")

private fun emptyAccountPeriodSummary(): AccountPeriodSummary = AccountPeriodSummary(
    incomeInCents = 0L,
    expenseInCents = 0L,
    entryCount = 0,
    recordDays = 0,
    largestExpenseInCents = 0L,
)

private fun LocalDate.accountingWeekday(): String = when (dayOfWeek.value) {
    1 -> "星期一"
    2 -> "星期二"
    3 -> "星期三"
    4 -> "星期四"
    5 -> "星期五"
    6 -> "星期六"
    else -> "星期日"
}

private fun LocalDate.accountingWeekdayShort(): String = when (dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}

private fun String.asDate(): LocalDate? = try {
    LocalDate.parse(this, accountDateFormatter)
} catch (_: DateTimeParseException) {
    null
}

private fun String.asTime(): LocalTime? = try {
    LocalTime.parse(this, accountTimeFormatter)
} catch (_: DateTimeParseException) {
    null
}
