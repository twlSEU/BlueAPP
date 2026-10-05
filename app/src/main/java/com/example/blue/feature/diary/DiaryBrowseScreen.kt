package com.example.blue.feature.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.blue.R
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.entity.DiaryWithImages
import com.example.blue.data.local.entity.selectedMoodIds
import com.example.blue.data.repository.DiaryBrowseFilter
import com.example.blue.data.repository.DiaryBrowseOrder
import com.example.blue.data.repository.DiaryRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.FeatureHubTabStyle
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DIARY_BROWSE_PAGE_SIZE = 20
private const val DIARY_BROWSE_MAX_ITEMS = 80
private enum class DiaryBrowseContent { LOADING, EMPTY, ERROR, LIST }
private val browseDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val browseTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val BrowseBackground = Color(0xFFF5F8FD)
private val BrowseSurface = Color(0xFFFCFDFF)
private val BrowseTitle = Color(0xFF172338)
private val BrowseBody = Color(0xFF54657E)
private val BrowseMuted = Color(0xFF7F90AB)
internal val DiaryBrowseAccent = Color(0xFF2F86F6)
private val BrowseBlue = DiaryBrowseAccent
private val BrowseBlueSoft = Color(0xFFEAF4FF)
private val BrowseControl = Color(0xFFF0F5FB)
private val BrowseBorder = Color(0xFFE7EEF8)
private val BrowseCardShape = RoundedCornerShape(24.dp)
internal val DiaryBrowseTabStyle = FeatureHubTabStyle(
    backgroundColor = BrowseBackground,
    selectedColor = BrowseTitle,
    unselectedColor = BrowseMuted,
    fontSize = 16.sp,
    indicatorWidth = 36.dp,
    showDivider = false,
)

data class DiaryBrowseUiState(
    val items: List<DiaryWithImages> = emptyList(),
    val windowStartOffset: Int = 0,
    val totalCount: Int = 0,
    val filter: DiaryBrowseFilter = DiaryBrowseFilter(),
    val order: DiaryBrowseOrder = DiaryBrowseOrder.DESCENDING,
    val selectedYear: Int? = null,
    val selectedMonth: Int? = null,
    val selectedDay: Int? = null,
    val isRefreshing: Boolean = true,
    val isLoadingPrevious: Boolean = false,
    val isLoadingNext: Boolean = false,
    val errorMessage: String? = null,
    val failedAction: DiaryBrowseLoadAction? = null,
) {
    val canLoadPrevious: Boolean get() = windowStartOffset > 0
    val canLoadNext: Boolean get() = windowStartOffset + items.size < totalCount
    val isEmpty: Boolean get() = !isRefreshing && errorMessage == null && totalCount == 0

    val filterLabel: String
        get() = when {
            selectedYear == null -> "全部日期"
            selectedMonth == null -> "${selectedYear}年"
            selectedDay == null -> "${selectedYear}年${selectedMonth}月"
            else -> "%d年%d月%d日".format(selectedYear, selectedMonth, selectedDay)
        }
}

enum class DiaryBrowseLoadAction { REFRESH, PREVIOUS, NEXT }

class DiaryBrowseViewModel(
    private val repository: DiaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiaryBrowseUiState())
    val uiState: StateFlow<DiaryBrowseUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var hasBeenVisible = false

    init {
        refresh()
    }

    /** Re-reads the retained window after returning from an editor without resetting its anchor. */
    fun onScreenVisible() {
        if (!hasBeenVisible) {
            hasBeenVisible = true
            return
        }
        val state = _uiState.value
        if (state.isRefreshing || state.isLoadingNext || state.isLoadingPrevious) return
        if (state.items.isEmpty()) refresh() else reloadCurrentWindow()
    }

    fun setOrder(order: DiaryBrowseOrder) {
        if (_uiState.value.order == order) return
        _uiState.update { it.copy(order = order) }
        refresh()
    }

    fun applyDateFilter(year: Int?, month: Int?, day: Int?) {
        val filter = when {
            year == null -> DiaryBrowseFilter()
            month == null -> DiaryBrowseFilter(
                startDate = LocalDate.of(year, 1, 1),
                endDateExclusive = LocalDate.of(year + 1, 1, 1),
            )
            day == null -> YearMonth.of(year, month).let { selectedMonth ->
                DiaryBrowseFilter(
                    startDate = selectedMonth.atDay(1),
                    endDateExclusive = selectedMonth.plusMonths(1).atDay(1),
                )
            }
            else -> LocalDate.of(year, month, day).let { selectedDate ->
                DiaryBrowseFilter(
                    startDate = selectedDate,
                    endDateExclusive = selectedDate.plusDays(1),
                )
            }
        }
        _uiState.update {
            it.copy(
                filter = filter,
                selectedYear = year,
                selectedMonth = month,
                selectedDay = day,
            )
        }
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val snapshot = _uiState.value
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLoadingPrevious = false,
                    isLoadingNext = false,
                    errorMessage = null,
                    failedAction = null,
                )
            }
            runCatching {
                val total = repository.countDiaries(snapshot.filter)
                val firstPage = if (total == 0) {
                    emptyList()
                } else {
                    repository.loadDiaryPage(
                        filter = snapshot.filter,
                        order = snapshot.order,
                        limit = DIARY_BROWSE_PAGE_SIZE,
                        offset = 0,
                    )
                }
                total to firstPage
            }.onSuccess { (total, page) ->
                if (_uiState.value.filter == snapshot.filter && _uiState.value.order == snapshot.order) {
                    _uiState.update {
                        it.copy(
                            items = page,
                            windowStartOffset = 0,
                            totalCount = total,
                            isRefreshing = false,
                            errorMessage = null,
                            failedAction = null,
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) current else current.copy(
                        items = emptyList(),
                        windowStartOffset = 0,
                        totalCount = 0,
                        isRefreshing = false,
                        errorMessage = error.message ?: "日记加载失败",
                        failedAction = DiaryBrowseLoadAction.REFRESH,
                    )
                }
            }
        }
    }

    private fun reloadCurrentWindow() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val desiredSize = snapshot.items.size.coerceAtLeast(DIARY_BROWSE_PAGE_SIZE)
            runCatching {
                val total = repository.countDiaries(snapshot.filter)
                val safeStart = snapshot.windowStartOffset.coerceAtMost((total - 1).coerceAtLeast(0))
                val page = if (total == 0) emptyList() else repository.loadDiaryPage(
                    filter = snapshot.filter,
                    order = snapshot.order,
                    limit = desiredSize.coerceAtMost(DIARY_BROWSE_MAX_ITEMS),
                    offset = safeStart,
                )
                Triple(total, safeStart, page)
            }.onSuccess { (total, start, page) ->
                _uiState.update {
                    it.copy(
                        items = page,
                        windowStartOffset = if (total == 0) 0 else start,
                        totalCount = total,
                        errorMessage = null,
                        failedAction = null,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) current else current.copy(
                        errorMessage = error.message ?: "日记刷新失败",
                        failedAction = DiaryBrowseLoadAction.REFRESH,
                    )
                }
            }
        }
    }

    fun loadNext() {
        val snapshot = _uiState.value
        if (!snapshot.canLoadNext || snapshot.isLoadingNext || snapshot.isRefreshing) return
        _uiState.update { it.copy(isLoadingNext = true, errorMessage = null, failedAction = null) }
        viewModelScope.launch {
            val offset = snapshot.windowStartOffset + snapshot.items.size
            runCatching {
                repository.loadDiaryPage(
                    filter = snapshot.filter,
                    order = snapshot.order,
                    limit = DIARY_BROWSE_PAGE_SIZE,
                    offset = offset,
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) return@update current
                    val combined = (current.items + page).distinctBy { it.diary.id }
                    val overflow = (combined.size - DIARY_BROWSE_MAX_ITEMS).coerceAtLeast(0)
                    current.copy(
                        items = if (overflow == 0) combined else combined.drop(overflow),
                        windowStartOffset = current.windowStartOffset + overflow,
                        isLoadingNext = false,
                        errorMessage = null,
                        failedAction = null,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) current else current.copy(
                        isLoadingNext = false,
                        errorMessage = error.message ?: "下一页加载失败",
                        failedAction = DiaryBrowseLoadAction.NEXT,
                    )
                }
            }
        }
    }

    fun loadPrevious() {
        val snapshot = _uiState.value
        if (!snapshot.canLoadPrevious || snapshot.isLoadingPrevious || snapshot.isRefreshing) return
        _uiState.update { it.copy(isLoadingPrevious = true, errorMessage = null, failedAction = null) }
        viewModelScope.launch {
            val newStart = (snapshot.windowStartOffset - DIARY_BROWSE_PAGE_SIZE).coerceAtLeast(0)
            val limit = snapshot.windowStartOffset - newStart
            runCatching {
                repository.loadDiaryPage(
                    filter = snapshot.filter,
                    order = snapshot.order,
                    limit = limit,
                    offset = newStart,
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) return@update current
                    val combined = (page + current.items).distinctBy { it.diary.id }
                    current.copy(
                        items = combined.take(DIARY_BROWSE_MAX_ITEMS),
                        windowStartOffset = newStart,
                        isLoadingPrevious = false,
                        errorMessage = null,
                        failedAction = null,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.filter != snapshot.filter || current.order != snapshot.order) current else current.copy(
                        isLoadingPrevious = false,
                        errorMessage = error.message ?: "上一页加载失败",
                        failedAction = DiaryBrowseLoadAction.PREVIOUS,
                    )
                }
            }
        }
    }

    fun retry() {
        when (_uiState.value.failedAction) {
            DiaryBrowseLoadAction.NEXT -> loadNext()
            DiaryBrowseLoadAction.PREVIOUS -> loadPrevious()
            else -> refresh()
        }
    }

    companion object {
        fun factory(repository: DiaryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DiaryBrowseViewModel::class.java))
                    return DiaryBrowseViewModel(repository) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryBrowseScreen(
    repository: DiaryRepository,
    imageStorage: DiaryImageStorage,
    onOpenDiary: (String) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val factory = remember(repository) { DiaryBrowseViewModel.factory(repository) }
    val browseViewModel: DiaryBrowseViewModel = viewModel(factory = factory)
    val state by browseViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var preview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    val queryKey = "${state.filter.startDate}|${state.filter.endDateExclusive}|${state.order}"
    var retainedQueryKey by rememberSaveable { mutableStateOf(queryKey) }

    LaunchedEffect(Unit) { browseViewModel.onScreenVisible() }
    LaunchedEffect(queryKey) {
        if (retainedQueryKey != queryKey) {
            retainedQueryKey = queryKey
            listState.scrollToItem(0)
        }
    }
    LaunchedEffect(listState, state.items.size, state.canLoadPrevious, state.canLoadNext) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            Pair(visible.firstOrNull()?.index ?: 0, visible.lastOrNull()?.index ?: 0)
        }.distinctUntilChanged().collect { (first, last) ->
            if (first <= 2) browseViewModel.loadPrevious()
            if (state.items.isNotEmpty() && last >= state.items.lastIndex - 3) browseViewModel.loadNext()
        }
    }

    Scaffold(
        containerColor = BrowseBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("全局浏览", color = BrowseTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { AppBackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrowseBackground,
                        scrolledContainerColor = BrowseBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        val content = when {
            state.isRefreshing -> DiaryBrowseContent.LOADING
            state.isEmpty && state.selectedYear == null -> DiaryBrowseContent.EMPTY
            state.items.isEmpty() && state.errorMessage != null -> DiaryBrowseContent.ERROR
            else -> DiaryBrowseContent.LIST
        }
        AnimatedContent(
            targetState = content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(160)) },
            label = "Diary browse state",
        ) { displayedContent ->
            when (displayedContent) {
                DiaryBrowseContent.LOADING -> DiaryBrowseLoading(Modifier.padding(padding))
                DiaryBrowseContent.EMPTY -> DiaryBrowseEmpty(
                    modifier = Modifier.padding(padding),
                    filterLabel = state.filterLabel,
                    onClearFilter = { browseViewModel.applyDateFilter(null, null, null) },
                )
                DiaryBrowseContent.ERROR -> DiaryBrowseError(
                    modifier = Modifier.padding(padding),
                    message = state.errorMessage.orEmpty(),
                    onRetry = browseViewModel::retry,
                )
                DiaryBrowseContent.LIST -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "browse-controls", contentType = "browse-controls") {
                    DiaryBrowseControls(
                        state = state,
                        onDateChange = browseViewModel::applyDateFilter,
                        onOrderChange = browseViewModel::setOrder,
                    )
                }
                if (state.isEmpty) {
                    item(key = "filtered-empty", contentType = "browse-status") {
                        DiaryBrowseFilteredEmpty(
                            filterLabel = state.filterLabel,
                            onClearFilter = { browseViewModel.applyDateFilter(null, null, null) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = tween(220),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }
                }
                if (state.isLoadingPrevious) {
                    item(key = "loading-previous", contentType = "browse-status") {
                        DiaryInlineLoading("正在载入较近的日记…")
                    }
                }
                if (state.errorMessage != null && state.failedAction == DiaryBrowseLoadAction.PREVIOUS) {
                    item(key = "error-previous", contentType = "browse-status") {
                        DiaryInlineError(state.errorMessage.orEmpty(), browseViewModel::retry)
                    }
                }
                items(
                    items = state.items,
                    key = { it.diary.id },
                    contentType = { "diary-card" },
                ) { diary ->
                    DiaryBrowseCard(
                        diary = diary,
                        imageStorage = imageStorage,
                        onOpenDiary = { onOpenDiary(diary.diary.id) },
                        onPreview = { paths, index -> preview = paths to index },
                    )
                }
                when {
                    state.isLoadingNext -> item(key = "loading-next", contentType = "browse-status") {
                        DiaryInlineLoading("正在载入更多日记…")
                    }
                    state.errorMessage != null -> item(key = "error-next", contentType = "browse-status") {
                        DiaryInlineError(state.errorMessage.orEmpty(), browseViewModel::retry)
                    }
                    !state.canLoadNext && !state.isEmpty -> item(key = "no-more", contentType = "browse-status") {
                        Text(
                            "已经浏览到这里了",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrowseMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
        }
    }

    preview?.let { (paths, index) ->
        DiaryStoredImagePreviewDialog(
            paths = paths,
            initialIndex = index,
            imageStorage = imageStorage,
            onDismiss = { preview = null },
        )
    }
}

@Composable
internal fun DiaryBrowseControls(
    state: DiaryBrowseUiState,
    onDateChange: (Int?, Int?, Int?) -> Unit,
    onOrderChange: (DiaryBrowseOrder) -> Unit,
) {
    val currentYear = remember { LocalDate.now().year }
    val yearOptions = remember(currentYear) { (currentYear downTo 1970).map(Int::toString) }
    val monthOptions = remember { (1..12).map(Int::toString) }
    val daysInMonth = remember(state.selectedYear, state.selectedMonth) {
        state.selectedYear?.let { year ->
            state.selectedMonth?.let { month -> YearMonth.of(year, month).lengthOfMonth() }
        } ?: 31
    }
    val dayOptions = remember(daysInMonth) { (1..daysInMonth).map(Int::toString) }
    Surface(
        modifier = Modifier.fillMaxWidth().dropShadow(
            shape = BrowseCardShape,
            shadow = Shadow(radius = 18.dp, color = Color(0xFF7497C5).copy(alpha = 0.09f), offset = DpOffset(0.dp, 6.dp)),
        ),
        shape = BrowseCardShape,
        color = BrowseSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF6FAFF),
                    border = BorderStroke(1.dp, Color.White),
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                            tint = BrowseBlue,
                        )
                    }
                }
                BrowseDateDropdown(
                    label = "年",
                    value = state.selectedYear?.toString() ?: "全部",
                    options = yearOptions,
                    enabled = true,
                    lazyOptions = true,
                    onSelect = { value -> onDateChange(value?.toInt(), null, null) },
                )
                BrowseDateDropdown(
                    label = "月",
                    value = state.selectedMonth?.toString() ?: "全部",
                    options = monthOptions,
                    enabled = state.selectedYear != null,
                    onSelect = { value -> onDateChange(state.selectedYear, value?.toInt(), null) },
                )
                BrowseDateDropdown(
                    label = "日",
                    value = state.selectedDay?.toString() ?: "全部",
                    options = dayOptions,
                    enabled = state.selectedMonth != null,
                    onSelect = { value -> onDateChange(state.selectedYear, state.selectedMonth, value?.toInt()) },
                )
            }
            HorizontalDivider(color = BrowseBorder.copy(alpha = 0.72f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrowseOrderOption(
                    modifier = Modifier.weight(1f),
                    label = "最新在前",
                    selected = state.order == DiaryBrowseOrder.DESCENDING,
                    onClick = { onOrderChange(DiaryBrowseOrder.DESCENDING) },
                )
                BrowseOrderOption(
                    modifier = Modifier.weight(1f),
                    label = "最早在前",
                    selected = state.order == DiaryBrowseOrder.ASCENDING,
                    onClick = { onOrderChange(DiaryBrowseOrder.ASCENDING) },
                )
                Surface(
                    modifier = Modifier.widthIn(max = 88.dp),
                    shape = CircleShape,
                    color = BrowseControl,
                ) {
                    Text(
                        "共 ${state.totalCount} 篇",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Medium,
                        color = BrowseMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BrowseDateDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    lazyOptions: Boolean = false,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val selected = value != "全部"
    Box(modifier = Modifier.weight(1f)) {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).onSizeChanged { anchorHeightPx = it.height },
            shape = RoundedCornerShape(14.dp),
            color = if (selected) BrowseBlueSoft.copy(alpha = 0.72f) else BrowseControl,
            border = if (selected) BorderStroke(1.dp, BrowseBlue.copy(alpha = 0.28f)) else null,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    color = if (selected) BrowseBlue else BrowseMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value,
                        modifier = Modifier.weight(1f),
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            selected -> BrowseBlue
                            else -> BrowseTitle
                        },
                        maxLines = 1,
                        softWrap = false,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_down),
                        contentDescription = "选择$label",
                        modifier = Modifier.size(13.dp),
                        tint = when {
                            !enabled -> BrowseMuted.copy(alpha = 0.45f)
                            selected -> BrowseBlue
                            else -> BrowseMuted
                        },
                    )
                }
            }
        }
        if (expanded && lazyOptions) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeightPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.height(260.dp).widthIn(min = 112.dp, max = 280.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BrowseSurface,
                    border = BorderStroke(1.dp, BrowseBorder),
                    shadowElevation = 8.dp,
                ) {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        item(key = "all", contentType = "date-option") {
                            BrowseDateDropdownItem(
                                option = null,
                                selectedValue = value,
                                onClick = { onSelect(null); expanded = false },
                            )
                        }
                        items(
                            items = options,
                            key = { it },
                            contentType = { "date-option" },
                        ) { option ->
                            BrowseDateDropdownItem(
                                option = option,
                                selectedValue = value,
                                onClick = { onSelect(option); expanded = false },
                            )
                        }
                    }
                }
            }
        } else {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 260.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = BrowseSurface,
                border = BorderStroke(1.dp, BrowseBorder),
            ) {
                BrowseDateDropdownItem(
                    option = null,
                    selectedValue = value,
                    onClick = { onSelect(null); expanded = false },
                )
                options.forEach { option ->
                    BrowseDateDropdownItem(
                        option = option,
                        selectedValue = value,
                        onClick = { onSelect(option); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseDateDropdownItem(
    option: String?,
    selectedValue: String,
    onClick: () -> Unit,
) {
    val label = option ?: "全部"
    DropdownMenuItem(
        text = {
            Text(
                label,
                fontWeight = if (selectedValue == label) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selectedValue == label) BrowseBlue else BrowseTitle,
            )
        },
        trailingIcon = { if (selectedValue == label) BrowseSelectedDot() },
        onClick = onClick,
    )
}

@Composable
private fun BrowseSelectedDot() {
    Box(
        modifier = Modifier.size(7.dp).background(BrowseBlue, CircleShape),
    )
}

@Composable
private fun BrowseOrderOption(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 38.dp),
        shape = CircleShape,
        color = if (selected) Color(0xFFE2F1FF) else Color(0xFFF7FAFF),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) BrowseBlue.copy(alpha = 0.72f) else Color.Transparent,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(14.dp),
                    shape = CircleShape,
                    color = if (selected) BrowseBlue else Color.Transparent,
                    border = if (selected) null else BorderStroke(1.2.dp, BrowseMuted.copy(alpha = 0.55f)),
                ) {}
                if (selected) {
                    Box(Modifier.size(5.dp).background(Color.White, CircleShape))
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                modifier = Modifier.weight(1f, fill = false),
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) BrowseBlue else BrowseTitle,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
internal fun DiaryBrowseCard(
    diary: DiaryWithImages,
    imageStorage: DiaryImageStorage,
    onOpenDiary: () -> Unit,
    onPreview: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = diary.diary
    val dateLabel = remember(entry.diaryDate) { entry.diaryDate.format(browseDateFormatter) }
    val timeLabel = remember(entry.diaryDate, entry.diaryTime) {
        "${entry.diaryTime.format(browseTimeFormatter)} · ${entry.diaryDate.chineseWeekdayForDiary()}"
    }
    val characterCount = remember(entry.content) { entry.content.countDiaryCharacters() }
    Card(
        modifier = modifier.fillMaxWidth().dropShadow(
            shape = BrowseCardShape,
            shadow = Shadow(radius = 20.dp, color = Color(0xFF7497C5).copy(alpha = 0.08f), offset = DpOffset(0.dp, 6.dp)),
        ),
        shape = BrowseCardShape,
        colors = CardDefaults.cardColors(containerColor = BrowseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDiary).padding(horizontal = 20.dp),
            ) {
                val moodWidth = maxWidth * 0.38f
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            dateLabel,
                            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 26.sp, stepSize = 0.5.sp),
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = BrowseTitle,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            timeLabel,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                            color = BrowseMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val selectedMoods = diary.selectedMoodIds
                    if (selectedMoods.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.widthIn(max = moodWidth),
                            shape = CircleShape,
                            color = BrowseBlueSoft,
                        ) {
                            Text(
                                diaryMoodLabels(selectedMoods),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                                lineHeight = 18.sp,
                                letterSpacing = 0.sp,
                                color = BrowseBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BrowseBorder)
            Text(
                text = entry.content.ifBlank { "这是一篇照片日记" },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDiary).padding(horizontal = 20.dp),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                color = BrowseBody,
            )
            if (diary.images.isNotEmpty()) {
                val sortedImages = remember(diary.images) { diary.images.sortedBy { it.sortOrder } }
                val paths = remember(sortedImages) { sortedImages.map { it.localPath } }
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    val density = LocalDensity.current
                    val gap = 8.dp
                    val gapPx = with(density) { gap.roundToPx() }
                    val photoSpacePx = (constraints.maxWidth - 2 * gapPx).coerceAtLeast(3)
                    val photoWidthPx = photoSpacePx / 3
                    val remainderPx = photoSpacePx % 3
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        itemsIndexed(
                            items = sortedImages,
                            key = { _, image -> image.id },
                            contentType = { _, _ -> "diary-thumbnail" },
                        ) { index, image ->
                            // Distribute rounding pixels so three complete photos fill the viewport.
                            val widthPx = photoWidthPx + if (index % 3 < remainderPx) 1 else 0
                            DiaryThumbnail(
                                localPath = image.localPath,
                                imageStorage = imageStorage,
                                size = with(density) { widthPx.toDp() },
                                photoNumber = index + 1,
                                contentDescription = "${entry.diaryDate} 的第 ${index + 1} 张日记照片",
                                onClick = { onPreview(paths, index) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$characterCount 字",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = BrowseMuted,
                )
                Row(
                    modifier = Modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClick = onOpenDiary),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("点击继续编辑", fontSize = 13.sp, lineHeight = 20.sp, color = BrowseBlue, fontWeight = FontWeight.Medium)
                    Icon(
                        painter = painterResource(R.drawable.ic_home_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(width = 10.dp, height = 16.dp),
                        tint = BrowseBlue,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryThumbnail(
    localPath: String,
    imageStorage: DiaryImageStorage,
    size: Dp,
    photoNumber: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnailModifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    if (LocalInspectionMode.current) {
        val tint = listOf(Color(0xFFD8E9FA), Color(0xFFEDE2D5), Color(0xFFD5EAE6))[(photoNumber - 1) % 3]
        Box(
            modifier = thumbnailModifier.background(Brush.linearGradient(listOf(tint, BrowseBlueSoft))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_image_placeholder), contentDescription, tint = BrowseMuted, modifier = Modifier.size(32.dp))
            Text(
                "$photoNumber",
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                color = BrowseBody,
                fontSize = 12.sp,
            )
        }
        return
    }
    val sizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val request = remember(context, imageStorage, localPath, sizePx) {
        ImageRequest.Builder(context)
            .data(imageStorage.fileFor(localPath))
            .size(sizePx, sizePx)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = thumbnailModifier.background(BrowseControl),
        placeholder = painterResource(R.drawable.ic_image_placeholder),
        error = painterResource(R.drawable.ic_image_placeholder),
        contentScale = ContentScale.Crop,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryStoredImagePreviewDialog(
    paths: List<String>,
    initialIndex: Int,
    imageStorage: DiaryImageStorage,
    onDismiss: () -> Unit,
) {
    if (paths.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(paths.indices),
        pageCount = { paths.size },
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
            ) { page ->
                AsyncImage(
                    model = imageStorage.fileFor(paths[page]),
                    contentDescription = "日记照片原图",
                    modifier = Modifier.fillMaxSize().padding(vertical = 56.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
            ) { Text("关闭", color = Color.White) }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${paths.size}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DiaryBrowseLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = BrowseBlue)
            Text("正在整理日记…", color = BrowseMuted)
        }
    }
}

@Composable
private fun DiaryBrowseEmpty(
    modifier: Modifier = Modifier,
    filterLabel: String,
    onClearFilter: () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("这里还没有日记", style = MaterialTheme.typography.titleLarge, color = BrowseTitle)
            Text("$filterLabel 暂无记录", color = BrowseMuted)
            if (filterLabel != "全部日期") OutlinedButton(onClick = onClearFilter) { Text("查看全部日期") }
        }
    }
}

@Composable
private fun DiaryBrowseFilteredEmpty(
    filterLabel: String,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            "这个日期还没有日记",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = BrowseTitle,
        )
        Text(
            "$filterLabel 暂无记录，可以换个日期看看",
            style = MaterialTheme.typography.bodyMedium,
            color = BrowseMuted,
        )
        OutlinedButton(
            onClick = onClearFilter,
            shape = CircleShape,
            border = BorderStroke(1.dp, BrowseBorder),
        ) {
            Text("查看全部日期", color = BrowseBlue)
        }
    }
}

@Composable
private fun DiaryBrowseError(modifier: Modifier = Modifier, message: String, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("加载失败", style = MaterialTheme.typography.titleLarge, color = BrowseTitle)
            Text(message, modifier = Modifier.padding(horizontal = 28.dp), color = BrowseMuted)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BrowseBlue)) { Text("重试") }
        }
    }
}

@Composable
private fun DiaryInlineLoading(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrowseBlue)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BrowseMuted)
    }
}

@Composable
private fun DiaryInlineError(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF4F2),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = Color(0xFF9A5550), maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

private fun String.countDiaryCharacters(): Int = count { !it.isWhitespace() }

private fun LocalDate.chineseWeekdayForDiary(): String = when (dayOfWeek.value) {
    1 -> "星期一"
    2 -> "星期二"
    3 -> "星期三"
    4 -> "星期四"
    5 -> "星期五"
    6 -> "星期六"
    else -> "星期日"
}

@Preview(name = "日记浏览 · 设计稿", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, locale = "zh-rCN")
@Preview(name = "日记浏览 · 手机", showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 800, fontScale = 1.15f, locale = "zh-rCN")
@Preview(name = "日记浏览 · 窄屏", showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 800, locale = "zh-rCN")
@Preview(name = "日记浏览 · 大字体", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 1000, fontScale = 1.5f, locale = "zh-rCN")
@Composable
private fun DiaryBrowseScreenPreview() {
    DiaryBrowsePreviewContent()
}
