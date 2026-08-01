package com.example.blue.feature.accounting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blue.R
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.data.repository.AccountBrowseSortField
import com.example.blue.data.repository.AccountRepository
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.model.AccountType
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

private val BrowseBackground = Color(0xFFF6F8FC)
private val BrowseSurface = Color(0xFFFEFFFF)
private val BrowseText = Color(0xFF2D4555)
private val BrowseMuted = Color(0xFF748895)
private val BrowseAccent = Color(0xFF3D7BE5)
private val BrowseAccentSoft = Color(0xFFEAF3FF)
private val BrowseBorder = Color(0xFFDCE7EE)
private val BrowseExpense = Color(0xFFC96868)
private val BrowseIncome = Color(0xFF3F8D78)

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

    Scaffold(
        containerColor = BrowseBackground,
        topBar = { if (showTopBar) BrowseTopBar(onBack = onBack) },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "browse-filters") {
                AccountingBrowseFilters(
                    uiState = uiState,
                    onAllYears = { browseViewModel.selectYear(null) },
                    onCurrentYear = { browseViewModel.selectYear(LocalDate.now().year) },
                    onMoveYear = browseViewModel::moveYear,
                    onMonthSelected = browseViewModel::selectMonth,
                    onTypeSelected = browseViewModel::selectType,
                    onCategorySelected = browseViewModel::selectCategory,
                    onSearchChanged = browseViewModel::updateSearchText,
                    onSortSelected = browseViewModel::selectSort,
                )
            }

            item(key = "browse-count") {
                BrowseResultHeader(uiState)
            }

            if (uiState.isLoading && uiState.items.isEmpty()) {
                item(key = "browse-initial-loading") {
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
                item(key = "browse-initial-error") {
                    BrowseErrorCard(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = browseViewModel::retry,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(180),
                            placementSpec = tween(220),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }
            } else if (uiState.initialized && uiState.items.isEmpty()) {
                item(key = "browse-empty") {
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
                    item(key = "browse-load-previous") {
                        BrowsePageControl(
                            label = if (
                                uiState.isLoading && uiState.loadDirection == BrowseLoadDirection.PREVIOUS
                            ) {
                                "正在加载上一页…"
                            } else {
                                "加载上一页"
                            },
                            enabled = !uiState.isLoading,
                            onClick = browseViewModel::loadPrevious,
                        )
                    }
                }

                items(
                    items = uiState.items,
                    key = { it.entry.id },
                ) { item ->
                    BrowseEntryCard(
                        item = item,
                        onClick = { onOpenEntry(item.entry.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(180),
                            placementSpec = tween(220),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }

                if (uiState.errorMessage != null) {
                    item(key = "browse-page-error") {
                        BrowseErrorCard(message = uiState.errorMessage.orEmpty(), onRetry = browseViewModel::retry)
                    }
                }

                item(key = "browse-footer") {
                    when {
                        uiState.isLoading && uiState.loadDirection != BrowseLoadDirection.REFRESH -> {
                            BrowsePageControl(label = "正在加载更多…", enabled = false, onClick = {})
                        }
                        uiState.canLoadNext -> {
                            BrowsePageControl(label = "加载下一页", onClick = browseViewModel::loadNext)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "全局浏览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrowseText,
            )
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = 12.dp), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = BrowseSurface,
                    shadowElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp),
                            tint = BrowseText,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrowseBackground,
            scrolledContainerColor = BrowseBackground,
        ),
    )
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = BrowseSurface),
        border = BorderStroke(1.dp, BrowseBorder.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
    Row(
        modifier = Modifier
            .width(152.dp)
            .background(Color(0xFFF0F3F6), RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BrowseSegmentOption("全部", allSelected, Modifier.weight(1f), onAllYears)
        BrowseSegmentOption("今年", currentYearSelected, Modifier.weight(1f), onCurrentYear)
    }
}

@Composable
private fun BrowseSegmentOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) BrowseAccentSoft else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) BrowseAccent else BrowseMuted,
            )
        }
    }
}

@Composable
private fun BrowseYearSwitcher(year: Int?, canMoveForward: Boolean, onMoveYear: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BrowseArrowButton(enabled = year != null, mirrored = false) { onMoveYear(-1) }
        AnimatedContent(
            targetState = year,
            transitionSpec = {
                val initialYear = initialState ?: targetState ?: 0
                val targetYear = targetState ?: initialState ?: 0
                val direction = if (targetYear >= initialYear) 1 else -1
                (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { width -> direction * width / 4 })
                    .togetherWith(
                        fadeOut(tween(160)) +
                            slideOutHorizontally(tween(200)) { width -> -direction * width / 4 },
                    )
            },
            label = "Accounting browse year",
        ) { displayedYear ->
            Text(
                text = displayedYear?.let { "${it}年" } ?: "跨年份",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (displayedYear == null) BrowseMuted else BrowseText,
            )
        }
        BrowseArrowButton(enabled = canMoveForward, mirrored = true) { onMoveYear(1) }
    }
}

@Composable
private fun BrowseArrowButton(enabled: Boolean, mirrored: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = Color(0xFFF3F5F7),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (mirrored) "›" else "‹",
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) BrowseText else BrowseMuted.copy(alpha = 0.35f),
            )
        }
    }
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
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F3F6), RoundedCornerShape(15.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BrowseTypeOption("全部", R.drawable.ic_wallet, selected == null, Modifier.weight(1f)) { onSelected(null) }
        BrowseTypeOption(
            "支出",
            R.drawable.ic_trending_down,
            selected == AccountType.EXPENSE,
            Modifier.weight(1f),
        ) { onSelected(AccountType.EXPENSE) }
        BrowseTypeOption(
            "收入",
            R.drawable.ic_trending_up,
            selected == AccountType.INCOME,
            Modifier.weight(1f),
        ) { onSelected(AccountType.INCOME) }
    }
}

@Composable
private fun BrowseTypeOption(
    label: String,
    iconRes: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BrowseAccentSoft else Color.Transparent,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) BrowseAccent else BrowseMuted,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) BrowseAccent else BrowseMuted,
            )
        }
    }
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
                DropdownMenu(expanded = menuVisible, onDismissRequest = { onMenuVisibleChange(false) }) {
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
        color = Color(0xFFF1F4F7),
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BrowseText),
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
            color = if (expanded) BrowseAccentSoft.copy(alpha = 0.72f) else Color(0xFFF1F4F7),
            border = if (expanded) BorderStroke(1.dp, BrowseAccent.copy(alpha = 0.30f)) else null,
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
            border = BorderStroke(1.dp, BrowseBorder.copy(alpha = 0.85f)),
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
                            color = if (selected) BrowseAccentSoft else Color(0xFFF3F5F7),
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
        color = if (selected) BrowseAccentSoft else Color(0xFFF3F5F7),
        border = if (selected) BorderStroke(1.dp, BrowseAccent.copy(alpha = 0.14f)) else null,
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
                style = MaterialTheme.typography.labelMedium,
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
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = BrowseSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(BrowseAccentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(browseCategoryIconRes(item.category.name)),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = BrowseAccent,
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${item.category.name} · ${item.entry.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BrowseText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.entry.entryDate.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = BrowseMuted,
                )
            }
            Text(
                text = buildString {
                    append(if (isIncome) "+¥" else "-¥")
                    append(AmountUtils.formatCents(item.entry.amountInCents))
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
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
        border = BorderStroke(1.dp, BrowseBorder),
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
        border = BorderStroke(1.dp, BrowseExpense.copy(alpha = 0.24f)),
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
                colors = ButtonDefaults.buttonColors(containerColor = BrowseExpense),
            ) {
                Text("重试")
            }
        }
    }
}

@Preview(
    name = "记账 · 全部账目",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 1000,
)
@Composable
private fun AccountingBrowseScreenPreview() {
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
    val uiState = AccountingBrowseUiState(
        items = entries,
        categories = listOf(dining, transport, salary),
        selectedYear = 2026,
        selectedMonth = 7,
        totalCount = entries.size,
        initialized = true,
    )

    BlueTheme(dynamicColor = false) {
        Scaffold(
            containerColor = BrowseBackground,
            topBar = { BrowseTopBar(onBack = {}) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AccountingBrowseFilters(
                        uiState = uiState,
                        onAllYears = {},
                        onCurrentYear = {},
                        onMoveYear = {},
                        onMonthSelected = {},
                        onTypeSelected = {},
                        onCategorySelected = {},
                        onSearchChanged = {},
                        onSortSelected = { _, _ -> },
                    )
                }
                item { BrowseResultHeader(uiState) }
                items(entries, key = { it.entry.id }) { item ->
                    BrowseEntryCard(item = item, onClick = {})
                }
                item { Text("已经到底了", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            }
        }
    }
}
