package com.example.blue.feature.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryWithCategory
import com.example.blue.data.repository.AccountBrowseFilter
import com.example.blue.data.repository.AccountBrowseSort
import com.example.blue.data.repository.AccountBrowseSortField
import com.example.blue.data.repository.AccountRepository
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class BrowseLoadDirection {
    INITIAL,
    PREVIOUS,
    NEXT,
    REFRESH,
}

internal data class AccountingBrowseUiState(
    val items: List<AccountEntryWithCategory> = emptyList(),
    val categories: List<AccountCategoryEntity> = emptyList(),
    val selectedYear: Int? = LocalDate.now().year,
    val selectedMonth: Int? = null,
    val selectedCategoryId: String? = null,
    val selectedType: AccountType? = null,
    val searchText: String = "",
    val appliedKeyword: String = "",
    val sortField: AccountBrowseSortField = AccountBrowseSortField.TIME,
    val ascending: Boolean = false,
    val windowStartOffset: Int = 0,
    val totalCount: Int = 0,
    val initialized: Boolean = false,
    val isLoading: Boolean = false,
    val loadDirection: BrowseLoadDirection? = null,
    val errorMessage: String? = null,
) {
    val canLoadPrevious: Boolean
        get() = initialized && windowStartOffset > 0

    val canLoadNext: Boolean
        get() = initialized && windowStartOffset + items.size < totalCount
}

internal class AccountingBrowseViewModel(
    private val repository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountingBrowseUiState())
    val uiState: StateFlow<AccountingBrowseUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var queryRevision = 0L
    private var failedDirection: BrowseLoadDirection? = null
    private var hasBeenVisible = false

    init {
        viewModelScope.launch {
            runCatching { repository.getAllCategories() }
                .onSuccess { categories -> _uiState.update { it.copy(categories = categories) } }
        }
        reload()
    }

    fun onScreenVisible() {
        if (hasBeenVisible) {
            refreshWindow()
        } else {
            hasBeenVisible = true
        }
    }

    fun selectYear(year: Int?) {
        if (_uiState.value.selectedYear == year) return
        _uiState.update {
            it.copy(
                selectedYear = year,
                selectedMonth = if (year == null) null else it.selectedMonth,
            )
        }
        reload()
    }

    fun moveYear(delta: Int) {
        val todayYear = LocalDate.now().year
        val current = _uiState.value.selectedYear ?: todayYear
        selectYear((current + delta).coerceAtMost(todayYear))
    }

    fun selectMonth(month: Int?) {
        if (_uiState.value.selectedYear == null || _uiState.value.selectedMonth == month) return
        _uiState.update { it.copy(selectedMonth = month) }
        reload()
    }

    fun selectType(type: AccountType?) {
        if (_uiState.value.selectedType == type) return
        _uiState.update { state ->
            val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
            state.copy(
                selectedType = type,
                selectedCategoryId = if (type != null && selectedCategory?.type != type) null else state.selectedCategoryId,
            )
        }
        reload()
    }

    fun selectCategory(categoryId: String?) {
        if (_uiState.value.selectedCategoryId == categoryId) return
        _uiState.update { state ->
            val category = state.categories.firstOrNull { it.id == categoryId }
            state.copy(
                selectedCategoryId = categoryId,
                selectedType = if (category != null && state.selectedType != null && state.selectedType != category.type) {
                    null
                } else {
                    state.selectedType
                },
            )
        }
        reload()
    }

    fun updateSearchText(value: String) {
        _uiState.update { it.copy(searchText = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            val normalized = value.trim()
            if (_uiState.value.appliedKeyword != normalized) {
                _uiState.update { it.copy(appliedKeyword = normalized) }
                reload()
            }
        }
    }

    fun selectSort(field: AccountBrowseSortField, ascending: Boolean) {
        val current = _uiState.value
        if (current.sortField == field && current.ascending == ascending) return
        _uiState.update { it.copy(sortField = field, ascending = ascending) }
        reload()
    }

    fun loadNext() {
        val snapshot = _uiState.value
        if (snapshot.isLoading || !snapshot.canLoadNext) return
        val offset = snapshot.windowStartOffset + snapshot.items.size
        loadPage(
            direction = BrowseLoadDirection.NEXT,
            offset = offset,
            limit = PAGE_SIZE,
        ) { current, page ->
            if (page.isEmpty()) {
                current.copy(totalCount = minOf(current.totalCount, offset))
            } else {
                val combined = (current.items + page).distinctBy { it.entry.id }
                val dropCount = (combined.size - MAX_WINDOW_SIZE).coerceAtLeast(0)
                current.copy(
                    items = combined.drop(dropCount),
                    windowStartOffset = current.windowStartOffset + dropCount,
                )
            }
        }
    }

    fun loadPrevious() {
        val snapshot = _uiState.value
        if (snapshot.isLoading || !snapshot.canLoadPrevious) return
        val offset = (snapshot.windowStartOffset - PAGE_SIZE).coerceAtLeast(0)
        val limit = snapshot.windowStartOffset - offset
        loadPage(
            direction = BrowseLoadDirection.PREVIOUS,
            offset = offset,
            limit = limit,
        ) { current, page ->
            if (page.isEmpty()) {
                current.copy(windowStartOffset = 0)
            } else {
                val combined = (page + current.items).distinctBy { it.entry.id }
                current.copy(
                    items = combined.take(MAX_WINDOW_SIZE),
                    windowStartOffset = offset,
                )
            }
        }
    }

    fun retry() {
        when (failedDirection) {
            BrowseLoadDirection.PREVIOUS -> loadPrevious()
            BrowseLoadDirection.NEXT -> loadNext()
            BrowseLoadDirection.REFRESH -> refreshWindow()
            BrowseLoadDirection.INITIAL, null -> reload()
        }
    }

    private fun reload() {
        queryRevision += 1L
        val revision = queryRevision
        loadJob?.cancel()
        failedDirection = null
        _uiState.update {
            it.copy(
                items = emptyList(),
                windowStartOffset = 0,
                totalCount = 0,
                initialized = false,
                isLoading = true,
                loadDirection = BrowseLoadDirection.INITIAL,
                errorMessage = null,
            )
        }
        val filter = _uiState.value.repositoryFilter()
        val sort = _uiState.value.repositorySort()
        loadJob = viewModelScope.launch {
            try {
                val total = repository.countEntries(filter)
                val page = if (total == 0) {
                    emptyList()
                } else {
                    repository.loadEntryPage(filter, sort, PAGE_SIZE, 0)
                }
                if (revision != queryRevision) return@launch
                _uiState.update {
                    it.copy(
                        items = page,
                        totalCount = total,
                        initialized = true,
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                if (revision != queryRevision) return@launch
                failedDirection = BrowseLoadDirection.INITIAL
                _uiState.update {
                    it.copy(
                        initialized = true,
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = throwable.message ?: "账目加载失败，请重试",
                    )
                }
            }
        }
    }

    private fun refreshWindow() {
        val snapshot = _uiState.value
        if (!snapshot.initialized || snapshot.items.isEmpty()) {
            reload()
            return
        }
        queryRevision += 1L
        val revision = queryRevision
        loadJob?.cancel()
        failedDirection = null
        _uiState.update {
            it.copy(
                isLoading = true,
                loadDirection = BrowseLoadDirection.REFRESH,
                errorMessage = null,
            )
        }
        val filter = snapshot.repositoryFilter()
        val sort = snapshot.repositorySort()
        loadJob = viewModelScope.launch {
            try {
                val total = repository.countEntries(filter)
                val desiredSize = snapshot.items.size.coerceIn(PAGE_SIZE, MAX_WINDOW_SIZE)
                val lastFullWindowOffset = (total - desiredSize).coerceAtLeast(0)
                val offset = snapshot.windowStartOffset.coerceAtMost(lastFullWindowOffset)
                val page = if (total == 0) {
                    emptyList()
                } else {
                    repository.loadEntryPage(filter, sort, desiredSize, offset)
                }
                if (revision != queryRevision) return@launch
                _uiState.update {
                    it.copy(
                        items = page,
                        windowStartOffset = if (page.isEmpty()) 0 else offset,
                        totalCount = total,
                        initialized = true,
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                if (revision != queryRevision) return@launch
                failedDirection = BrowseLoadDirection.REFRESH
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = throwable.message ?: "账目刷新失败，请重试",
                    )
                }
            }
        }
    }

    private fun loadPage(
        direction: BrowseLoadDirection,
        offset: Int,
        limit: Int,
        merge: (AccountingBrowseUiState, List<AccountEntryWithCategory>) -> AccountingBrowseUiState,
    ) {
        val revision = queryRevision
        val snapshot = _uiState.value
        val filter = snapshot.repositoryFilter()
        val sort = snapshot.repositorySort()
        failedDirection = null
        _uiState.update { it.copy(isLoading = true, loadDirection = direction, errorMessage = null) }
        loadJob = viewModelScope.launch {
            try {
                val page = repository.loadEntryPage(filter, sort, limit, offset)
                if (revision != queryRevision) return@launch
                _uiState.update { current ->
                    merge(current, page).copy(
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                if (revision != queryRevision) return@launch
                failedDirection = direction
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadDirection = null,
                        errorMessage = throwable.message ?: "账目加载失败，请重试",
                    )
                }
            }
        }
    }

    private fun AccountingBrowseUiState.repositoryFilter(): AccountBrowseFilter {
        val range = selectedYear?.let { year ->
            selectedMonth?.let { month ->
                val yearMonth = YearMonth.of(year, month)
                yearMonth.atDay(1) to yearMonth.plusMonths(1).atDay(1)
            } ?: (LocalDate.of(year, 1, 1) to LocalDate.of(year + 1, 1, 1))
        }
        return AccountBrowseFilter(
            startDate = range?.first,
            endDateExclusive = range?.second,
            categoryId = selectedCategoryId,
            type = selectedType,
            keyword = appliedKeyword,
        )
    }

    private fun AccountingBrowseUiState.repositorySort(): AccountBrowseSort = AccountBrowseSort(
        field = sortField,
        ascending = ascending,
    )

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_WINDOW_SIZE = 80
        private const val SEARCH_DEBOUNCE_MILLIS = 350L

        fun factory(repository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AccountingBrowseViewModel::class.java))
                    return AccountingBrowseViewModel(repository) as T
                }
            }
    }
}
