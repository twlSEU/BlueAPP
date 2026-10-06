package com.example.blue.feature.accounting

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.data.repository.AccountBrowseSortField
import com.example.blue.data.repository.AccountRepository
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.FeatureHubTab
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedBlue
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedCoral
import com.example.blue.feature.common.RefinedInk
import com.example.blue.feature.common.RefinedLine
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedPeriodSelector
import com.example.blue.feature.common.RefinedSegmentedControl
import com.example.blue.feature.common.RefinedShadow
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.feature.common.RefinedTopBar
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.model.AccountType
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private val BrowseBackground = RefinedBackground
private val BrowseSurface = Color(0xFFFEFFFF)
private val BrowseText = RefinedInk
private val BrowseMuted = RefinedMuted
private val BrowseAccent = RefinedBlue
private val BrowseAccentSoft = Color(0xFFEAF3FF)
private val BrowseBorder = RefinedLine
private val BrowseExpense = RefinedCoral
private val BrowseIncome = RefinedTeal

@Composable
fun AccountingBrowseScreen(
    repository: AccountRepository,
    onOpenEntry: (String) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val factory = remember(repository) { AccountingBrowseViewModel.factory(repository) }
    val browseViewModel: AccountingBrowseViewModel = viewModel(
        factory = factory,
    )
    val uiState by browseViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        browseViewModel.onScreenVisible()
    }
    LaunchedEffect(listState, uiState.items.size, uiState.canLoadPrevious, uiState.canLoadNext) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val first = layout.visibleItemsInfo.firstOrNull()?.index ?: 0
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearTop = first <= 3
            val nearBottom = layout.totalItemsCount > 0 && last >= layout.totalItemsCount - 3
            nearTop to nearBottom
        }
            .distinctUntilChanged()
            .collect { (nearTop, nearBottom) ->
                when {
                    nearTop && uiState.canLoadPrevious -> browseViewModel.loadPrevious()
                    nearBottom && uiState.canLoadNext -> browseViewModel.loadNext()
                }
            }
    }

    AccountingBrowseContent(
        uiState = uiState, listState = listState,
        actions = BrowseUiActions(
            onAllYears = { browseViewModel.selectYear(null) },
            onCurrentYear = { browseViewModel.selectYear(LocalDate.now().year) },
            onMoveYear = browseViewModel::moveYear,
            onMonthSelected = browseViewModel::selectMonth,
            onTypeSelected = browseViewModel::selectType,
            onCategorySelected = browseViewModel::selectCategory,
            onSearchChanged = browseViewModel::updateSearchText,
            onSortSelected = browseViewModel::selectSort,
            onRetry = browseViewModel::retry,
            onLoadPrevious = browseViewModel::loadPrevious,
            onLoadNext = browseViewModel::loadNext,
            onOpenEntry = onOpenEntry,
        ),
        onBack = onBack, showTopBar = showTopBar,
    )
}

private data class BrowseUiActions(
    val onAllYears: () -> Unit = {},
    val onCurrentYear: () -> Unit = {},
    val onMoveYear: (Int) -> Unit = {},
    val onMonthSelected: (Int?) -> Unit = {},
    val onTypeSelected: (AccountType?) -> Unit = {},
    val onCategorySelected: (String?) -> Unit = {},
    val onSearchChanged: (String) -> Unit = {},
    val onSortSelected: (AccountBrowseSortField, Boolean) -> Unit = { _, _ -> },
    val onRetry: () -> Unit = {},
    val onLoadPrevious: () -> Unit = {},
    val onLoadNext: () -> Unit = {},
    val onOpenEntry: (String) -> Unit = {},
)

@Composable
private fun AccountingBrowseContent(
    uiState: AccountingBrowseUiState,
    listState: LazyListState,
    actions: BrowseUiActions,
    onBack: () -> Unit,
    showTopBar: Boolean,
) {
    Scaffold(
        containerColor = BrowseBackground,
        topBar = { if (showTopBar) RefinedTopBar("全局浏览", onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "browse-filters", contentType = "filters") {
                    AccountingBrowseFilters(
                        uiState = uiState,
                        onAllYears = actions.onAllYears,
                        onCurrentYear = actions.onCurrentYear,
                        onMoveYear = actions.onMoveYear,
                        onMonthSelected = actions.onMonthSelected,
                        onTypeSelected = actions.onTypeSelected,
                        onCategorySelected = actions.onCategorySelected,
                        onSearchChanged = actions.onSearchChanged,
                        onSortSelected = actions.onSortSelected,
                    )
                }

                item(key = "browse-count", contentType = "result-header") {
                    BrowseResultHeader(uiState)
                }

                if (uiState.isLoading && uiState.items.isEmpty()) {
                    item(key = "browse-initial-loading", contentType = "status") {
                        BrowseStatusCard(
                            title = "正在整理账目",
                            message = "稍等一下，记录很快就好。",
                            loading = true,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = tween(220),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }
                } else if (uiState.errorMessage != null && uiState.items.isEmpty()) {
                    item(key = "browse-initial-error", contentType = "status") {
                        BrowseErrorCard(
                            message = uiState.errorMessage.orEmpty(),
                            onRetry = actions.onRetry,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = tween(220),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }
                } else if (uiState.initialized && uiState.items.isEmpty()) {
                    item(key = "browse-empty", contentType = "status") {
                        BrowseStatusCard(
                            title = "没有找到账目",
                            message = "换一个年份、分类或搜索词试试。",
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = tween(220),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }
                } else {
                    if (uiState.canLoadPrevious) {
                        item(key = "browse-load-previous", contentType = "page-control") {
                            BrowsePageControl(
                                label = if (
                                    uiState.isLoading && uiState.loadDirection == BrowseLoadDirection.PREVIOUS
                                ) {
                                    "正在加载上一页…"
                                } else {
                                    "加载上一页"
                                },
                                enabled = !uiState.isLoading,
                                onClick = actions.onLoadPrevious,
                            )
                        }
                    }

                    items(
                        items = uiState.items,
                        key = { it.entry.id },
                        contentType = { "account-entry" },
                    ) { item ->
                        BrowseEntryCard(
                            item = item,
                            onClick = { actions.onOpenEntry(item.entry.id) },
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item(key = "browse-page-error", contentType = "status") {
                            BrowseErrorCard(message = uiState.errorMessage.orEmpty(), onRetry = actions.onRetry)
                        }
                    }

                    item(key = "browse-footer", contentType = "page-control") {
                        when {
                            uiState.isLoading && uiState.loadDirection != BrowseLoadDirection.REFRESH -> {
                                BrowsePageControl(label = "正在加载更多…", enabled = false, onClick = {})
                            }
                            uiState.canLoadNext -> {
                                BrowsePageControl(label = "加载下一页", onClick = actions.onLoadNext)
                            }
                            else -> {
                                Text(
                                    "已经到底了",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrowseMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountingBrowseFilters(
    uiState: AccountingBrowseUiState,
    onAllYears: () -> Unit,
    onCurrentYear: () -> Unit,
    onMoveYear: (Int) -> Unit,
    onMonthSelected: (Int?) -> Unit,
    onTypeSelected: (AccountType?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortSelected: (AccountBrowseSortField, Boolean) -> Unit,
) {
    var allMonthsVisible by remember { mutableStateOf(false) }
    var categoryMenuVisible by remember { mutableStateOf(false) }
    var sortMenuVisible by remember { mutableStateOf(false) }
    val visibleCategories = remember(uiState.categories, uiState.selectedType) {
        uiState.categories.filter { category ->
            uiState.selectedType == null || category.type == uiState.selectedType
        }
    }
    val commonCategories = remember(visibleCategories) {
        val preferredNames = listOf("餐饮", "交通", "购物")
        val preferred = preferredNames.mapNotNull { name -> visibleCategories.firstOrNull { it.name == name } }
        preferred + visibleCategories.filterNot { it in preferred }.take((3 - preferred.size).coerceAtLeast(0))
    }

    RefinedCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrowseYearModeSelector(
                        allSelected = uiState.selectedYear == null,
                        currentYearSelected = uiState.selectedYear == LocalDate.now().year,
                        onAllYears = onAllYears,
                        onCurrentYear = onCurrentYear,
                    )
                }
                BrowseYearSwitcher(
                    year = uiState.selectedYear,
                    canMoveForward = uiState.selectedYear != null && uiState.selectedYear < LocalDate.now().year,
                    onMoveYear = onMoveYear,
                )
                if (uiState.selectedYear != null) {
                    BrowseMonthSelector(
                        selectedMonth = uiState.selectedMonth,
                        expanded = allMonthsVisible,
                        onExpandedChange = { allMonthsVisible = it },
                        onMonthSelected = onMonthSelected,
                    )
                }
            }

            BrowseFilterDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseFilterTitle("收支类型")
                BrowseTypeSelector(selected = uiState.selectedType, onSelected = onTypeSelected)
            }

            BrowseFilterDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseFilterTitle("分类")
                BrowseCategorySelector(
                    categories = visibleCategories,
                    commonCategories = commonCategories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    menuVisible = categoryMenuVisible,
                    onMenuVisibleChange = { categoryMenuVisible = it },
                    onCategorySelected = onCategorySelected,
                )
            }

            BrowseFilterDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseFilterTitle("备注关键词")
                BrowseSearchField(value = uiState.searchText, onValueChange = onSearchChanged)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrowseFilterTitle("排序")
                BrowseSortSelector(
                    field = uiState.sortField,
                    ascending = uiState.ascending,
                    expanded = sortMenuVisible,
                    onExpandedChange = { sortMenuVisible = it },
                    onSortSelected = onSortSelected,
                )
            }
        }
    }
}

@Composable
private fun BrowseYearModeSelector(
    allSelected: Boolean,
    currentYearSelected: Boolean,
    onAllYears: () -> Unit,
    onCurrentYear: () -> Unit,
) {
    RefinedSegmentedControl(
        options = listOf("全部", "今年"),
        selectedIndex = when { allSelected -> 0; currentYearSelected -> 1; else -> -1 },
        onSelected = { if (it == 0) onAllYears() else onCurrentYear() },
        modifier = Modifier.width(152.dp),
    )
}

@Composable
private fun BrowseYearSwitcher(year: Int?, canMoveForward: Boolean, onMoveYear: (Int) -> Unit) {
    RefinedPeriodSelector(
        label = year?.let { "${it}年" } ?: "跨年份",
        canMoveBack = year != null, canMoveForward = canMoveForward,
        onPrevious = { onMoveYear(-1) }, onNext = { onMoveYear(1) },
    )
}

@Composable
private fun BrowseMonthSelector(
    selectedMonth: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onMonthSelected: (Int?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(tween(220))) {
        if (!expanded) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BrowseFilterChip(
                    label = "全部",
                    selected = selectedMonth == null,
                    modifier = Modifier.weight(1.2f),
                    horizontalContentPadding = 4.dp,
                ) {
                    onMonthSelected(null)
                }
                (1..4).forEach { month ->
                    BrowseFilterChip(
                        label = "${month}月",
                        selected = selectedMonth == month,
                        modifier = Modifier.weight(1f),
                        horizontalContentPadding = 4.dp,
                    ) {
                        onMonthSelected(month)
                    }
                }
                BrowseFilterChip(
                    label = "•••",
                    selected = selectedMonth != null && selectedMonth in 5..12,
                    modifier = Modifier.weight(0.8f),
                    horizontalContentPadding = 4.dp,
                ) {
                    onExpandedChange(true)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                (listOf<Int?>(null) + (1..12).toList()).chunked(5).forEach { rowMonths ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowMonths.forEach { month ->
                            BrowseFilterChip(
                                label = month?.let { "${it}月" } ?: "全部",
                                selected = selectedMonth == month,
                                modifier = Modifier.weight(1f),
                                horizontalContentPadding = 4.dp,
                                onClick = { onMonthSelected(month) },
                            )
                        }
                        repeat(5 - rowMonths.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                TextButton(
                    onClick = { onExpandedChange(false) },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("收起月份", style = MaterialTheme.typography.labelMedium, color = BrowseMuted)
                }
            }
        }
    }
}

@Composable
private fun BrowseTypeSelector(selected: AccountType?, onSelected: (AccountType?) -> Unit) {
    RefinedSegmentedControl(
        options = listOf("全部", "支出", "收入"),
        selectedIndex = when (selected) { null -> 0; AccountType.EXPENSE -> 1; AccountType.INCOME -> 2 },
        onSelected = { onSelected(when (it) { 1 -> AccountType.EXPENSE; 2 -> AccountType.INCOME; else -> null }) },
    )
}

@Composable
private fun BrowseCategorySelector(
    categories: List<AccountCategoryEntity>,
    commonCategories: List<AccountCategoryEntity>,
    selectedCategoryId: String?,
    menuVisible: Boolean,
    onMenuVisibleChange: (Boolean) -> Unit,
    onCategorySelected: (String?) -> Unit,
) {
    val commonIds = remember(commonCategories) { commonCategories.mapTo(mutableSetOf()) { it.id } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        item(key = "category-all") {
            BrowseFilterChip(
                label = "全部分类",
                selected = selectedCategoryId == null,
                iconRes = R.drawable.ic_grid,
            ) { onCategorySelected(null) }
        }
        items(commonCategories, key = AccountCategoryEntity::id) { category ->
            BrowseFilterChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                iconRes = browseCategoryIconRes(category.name),
            ) { onCategorySelected(category.id) }
        }
        item(key = "category-more") {
            Box {
                BrowseFilterChip(
                    label = "更多",
                    selected = selectedCategoryId != null && selectedCategoryId !in commonIds,
                    iconRes = R.drawable.ic_grid,
                ) { onMenuVisibleChange(true) }
                DropdownMenu(
                    expanded = menuVisible, onDismissRequest = { onMenuVisibleChange(false) },
                    shape = RoundedCornerShape(18.dp), containerColor = BrowseSurface,
                    border = null, shadowElevation = 8.dp,
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(browseCategoryIconRes(category.name)),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedCategoryId == category.id) BrowseAccent else BrowseMuted,
                                )
                            },
                            onClick = {
                                onCategorySelected(category.id)
                                onMenuVisibleChange(false)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseSearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF3F6FB),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = BrowseMuted,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = BrowseText),
                cursorBrush = SolidColor(BrowseAccent),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text("搜索备注", style = MaterialTheme.typography.bodyMedium, color = BrowseMuted)
                        }
                        innerTextField()
                    }
                },
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(38.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "清除搜索",
                        modifier = Modifier.size(16.dp),
                        tint = BrowseMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseSortSelector(
    field: AccountBrowseSortField,
    ascending: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortSelected: (AccountBrowseSortField, Boolean) -> Unit,
) {
    BoxWithConstraints {
        Surface(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (expanded) BrowseAccentSoft.copy(alpha = 0.72f) else Color(0xFFF3F6FB),
            border = if (expanded) BorderStroke(1.dp, BrowseAccent.copy(alpha = 0.12f)) else null,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    browseSortLabel(field, ascending),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expanded) BrowseAccent else BrowseText,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (expanded) BrowseAccent else BrowseMuted,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(maxWidth),
            shape = RoundedCornerShape(18.dp),
            containerColor = BrowseSurface,
            border = null,
            shadowElevation = 10.dp,
        ) {
            val options = listOf(
                Triple("时间由近到远", AccountBrowseSortField.TIME, false),
                Triple("时间由远到近", AccountBrowseSortField.TIME, true),
                Triple("金额由高到低", AccountBrowseSortField.AMOUNT, false),
                Triple("金额由低到高", AccountBrowseSortField.AMOUNT, true),
            )
            options.forEachIndexed { index, (label, sortField, sortAscending) ->
                val selected = field == sortField && ascending == sortAscending
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) BrowseAccent else BrowseText,
                        )
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) BrowseAccentSoft else Color(0xFFF3F6FB),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(
                                        if (sortField == AccountBrowseSortField.TIME) {
                                            R.drawable.ic_clock
                                        } else {
                                            R.drawable.ic_wallet
                                        },
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selected) BrowseAccent else BrowseMuted,
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (selected) Box(Modifier.size(7.dp).background(BrowseAccent, CircleShape))
                    },
                    onClick = {
                        onSortSelected(sortField, sortAscending)
                        onExpandedChange(false)
                    },
                )
                if (index == 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = BrowseBorder.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

private fun browseSortLabel(field: AccountBrowseSortField, ascending: Boolean): String = when {
    field == AccountBrowseSortField.TIME && !ascending -> "时间由近到远"
    field == AccountBrowseSortField.TIME -> "时间由远到近"
    !ascending -> "金额由高到低"
    else -> "金额由低到高"
}

@Composable
private fun BrowseFilterTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = BrowseText,
    )
}

@Composable
private fun BrowseFilterDivider() {
    HorizontalDivider(thickness = 1.dp, color = BrowseBorder.copy(alpha = 0.48f))
}

@Composable
private fun BrowseFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    horizontalContentPadding: Dp = 11.dp,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) BrowseAccentSoft else Color(0xFFF3F6FB),
        border = if (selected) BorderStroke(1.dp, BrowseAccent.copy(alpha = 0.08f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalContentPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconRes?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (selected) BrowseAccent else BrowseMuted,
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                softWrap = false,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) BrowseAccent else BrowseMuted,
                maxLines = 1,
            )
        }
    }
}

private fun browseCategoryIconRes(name: String): Int = when (name) {
    "餐饮" -> R.drawable.ic_food
    "交通" -> R.drawable.ic_transport
    "购物" -> R.drawable.ic_shopping
    else -> R.drawable.ic_tag
}

@Composable
private fun BrowseEntryCard(
    item: AccountEntryWithCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = item.entry.type == AccountType.INCOME
    val amountColor = if (isIncome) BrowseIncome else BrowseExpense
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().dropShadow(shape, Shadow(radius = 14.dp, color = RefinedShadow, offset = DpOffset(0.dp, 4.dp))),
        shape = shape, color = BrowseSurface,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(14.dp)) {
            val amountWidth = (maxWidth * 0.34f).coerceIn(78.dp, 138.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(40.dp).background(amountColor.copy(alpha = 0.07f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(browseCategoryIconRes(item.category.name)), null, Modifier.size(19.dp), tint = amountColor)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.entry.name, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = BrowseText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${item.category.name} · ${item.entry.entryDate}", fontSize = 11.sp, lineHeight = 16.sp, color = BrowseMuted)
                }
                val amount = (if (isIncome) "+¥" else "−¥") + AmountUtils.formatCents(item.entry.amountInCents)
                val longAmount = amount.length > 14
                Text(
                    text = if (longAmount) amount.replace(",", ",\u200B") else amount,
                    modifier = Modifier.width(amountWidth),
                    fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold,
                    color = amountColor, textAlign = TextAlign.End,
                    maxLines = if (longAmount) 2 else 1, softWrap = longAmount,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 19.sp, stepSize = 0.5.sp),
                )
                Icon(painterResource(R.drawable.ic_home_chevron_right), null, Modifier.size(width = 8.dp, height = 13.dp), tint = BrowseMuted.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun BrowseResultHeader(uiState: AccountingBrowseUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "账目记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrowseText,
            )
            val rangeText = if (uiState.items.isEmpty()) {
                "共 ${uiState.totalCount} 笔"
            } else {
                val start = uiState.windowStartOffset + 1
                val end = uiState.windowStartOffset + uiState.items.size
                "共 ${uiState.totalCount} 笔 · 当前 $start–$end"
            }
            Text(rangeText, style = MaterialTheme.typography.bodySmall, color = BrowseMuted)
        }
        if (uiState.isLoading && uiState.items.isNotEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = BrowseAccent,
            )
        }
    }
}

@Composable
private fun BrowsePageControl(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = BrowseAccent),
    ) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BrowseStatusCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BrowseSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = BrowseAccent)
            } else {
                Box(
                    modifier = Modifier.size(42.dp).background(BrowseAccentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("¥", color = BrowseAccent, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BrowseText)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = BrowseMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BrowseErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F4)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = BrowseExpense,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrowseExpense.copy(alpha = 0.10f), contentColor = BrowseExpense),
            ) {
                Text("重试")
            }
        }
    }
}

private fun accountingBrowsePreviewState(): AccountingBrowseUiState {
    val dining = AccountCategoryEntity(
        id = "preview-dining",
        name = "餐饮",
        type = AccountType.EXPENSE,
        isDefault = true,
        isActive = true,
        createdAt = 0L,
        updatedAt = 0L,
    )
    val transport = AccountCategoryEntity(
        id = "preview-transport",
        name = "交通",
        type = AccountType.EXPENSE,
        isDefault = true,
        isActive = true,
        createdAt = 1L,
        updatedAt = 0L,
    )
    val salary = AccountCategoryEntity(
        id = "preview-salary",
        name = "工资",
        type = AccountType.INCOME,
        isDefault = true,
        isActive = true,
        createdAt = 2L,
        updatedAt = 0L,
    )
    val previewDate = LocalDate.of(2026, 7, 17)
    val entries = listOf(
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-entry-1",
                entryDate = previewDate,
                entryTime = LocalTime.of(12, 30),
                type = AccountType.EXPENSE,
                amountInCents = 2_860L,
                name = "午餐",
                categoryId = dining.id,
                note = "和朋友一起吃饭",
                createdAt = 0L,
                updatedAt = 0L,
            ),
            category = dining,
        ),
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-entry-2",
                entryDate = previewDate.minusDays(1),
                entryTime = LocalTime.of(8, 20),
                type = AccountType.EXPENSE,
                amountInCents = 600L,
                name = "地铁",
                categoryId = transport.id,
                note = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            category = transport,
        ),
        AccountEntryWithCategory(
            entry = AccountEntryEntity(
                id = "preview-entry-3",
                entryDate = previewDate.minusDays(2),
                entryTime = LocalTime.of(9, 0),
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
    return AccountingBrowseUiState(
        items = entries, categories = listOf(dining, transport, salary),
        selectedYear = 2026, selectedMonth = 7, totalCount = entries.size, initialized = true,
    )
}

@Preview(name = "记账浏览 · 标准手机", widthDp = 390, heightDp = 1100, showBackground = true)
@Preview(name = "记账浏览 · 窄屏", widthDp = 320, heightDp = 1100, showBackground = true)
@Preview(name = "记账浏览 · 大字体", widthDp = 390, heightDp = 1300, fontScale = 1.5f, showBackground = true)
@Composable
private fun AccountingBrowseScreenPreview() {
    BlueTheme(darkTheme = false) {
        FeatureHubScreen(
            tabs = listOf(FeatureHubTab("archive", "年月"), FeatureHubTab("browse", "浏览"), FeatureHubTab("summary", "总结")),
            initialPage = 1, accentColor = AccountingArchiveAccent, tabStyle = AccountingArchiveTabStyle,
        ) {
            AccountingBrowseContent(accountingBrowsePreviewState(), rememberLazyListState(), BrowseUiActions(), {}, showTopBar = false)
        }
    }
}

@Preview(name = "记账浏览 · 记录列表", widthDp = 390, heightDp = 500, showBackground = true)
@Preview(name = "记账浏览 · 长金额大字体", widthDp = 320, heightDp = 650, fontScale = 1.5f, showBackground = true)
@Composable
private fun AccountingBrowseEntriesPreview() {
    val state = accountingBrowsePreviewState()
    BlueTheme(darkTheme = false) {
        LazyColumn(
            Modifier.fillMaxSize().background(BrowseBackground),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { BrowseResultHeader(state) }
            items(state.items, key = { it.entry.id }) { BrowseEntryCard(it, {}) }
            item { BrowseEntryCard(state.items.last().let { it.copy(entry = it.entry.copy(amountInCents = 9_876_543_210_000L, name = "年度项目收入")) }, {}) }
        }
    }
}

@Preview(name = "记账浏览 · 暂无结果", widthDp = 390, heightDp = 1100, showBackground = true)
@Composable
private fun AccountingBrowseEmptyPreview() {
    BlueTheme(darkTheme = false) {
        AccountingBrowseContent(
            accountingBrowsePreviewState().copy(items = emptyList(), totalCount = 0, selectedYear = null),
            rememberLazyListState(), BrowseUiActions(), {}, showTopBar = true,
        )
    }
}
