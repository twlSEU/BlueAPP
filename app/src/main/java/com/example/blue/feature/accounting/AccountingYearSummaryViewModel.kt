package com.example.blue.feature.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.blue.data.repository.AccountCategoryAggregate
import com.example.blue.data.repository.AccountMonthlyAggregate
import com.example.blue.data.repository.AccountPeriodSummary
import com.example.blue.data.repository.AccountRepository
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AccountingYearSummaryUiState(
    val year: Int = LocalDate.now().year,
    val summary: AccountPeriodSummary? = null,
    val months: List<AccountMonthlyAggregate> = emptyList(),
    val categories: List<AccountCategoryAggregate> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && (summary?.entryCount ?: 0) == 0
}

internal class AccountingYearSummaryViewModel(
    private val repository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountingYearSummaryUiState())
    val uiState: StateFlow<AccountingYearSummaryUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeYear(_uiState.value.year)
    }

    fun selectYear(year: Int) {
        val safeYear = year.coerceAtMost(LocalDate.now().year)
        if (_uiState.value.year == safeYear && observeJob?.isActive == true) return
        observeYear(safeYear)
    }

    fun retry() {
        observeYear(_uiState.value.year)
    }

    private fun observeYear(year: Int) {
        observeJob?.cancel()
        _uiState.value = AccountingYearSummaryUiState(year = year, isLoading = true)
        observeJob = viewModelScope.launch {
            try {
                combine(
                    repository.observeYearSummary(year),
                    repository.observeMonthlyAggregates(year),
                    repository.observeCategoryAggregates(year),
                ) { summary, months, categories ->
                    AccountingYearSummaryUiState(
                        year = year,
                        summary = summary,
                        months = months,
                        categories = categories,
                        isLoading = false,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                _uiState.update {
                    AccountingYearSummaryUiState(
                        year = year,
                        isLoading = false,
                        errorMessage = throwable.message ?: "年度数据加载失败，请重试",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AccountingYearSummaryViewModel::class.java))
                    return AccountingYearSummaryViewModel(repository) as T
                }
            }
    }
}
