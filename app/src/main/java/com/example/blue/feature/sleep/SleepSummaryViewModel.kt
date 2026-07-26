package com.example.blue.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.repository.SleepRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

enum class SleepSummaryMode { MONTH, YEAR }

data class SleepMonthStatistics(
    val recordedDays: Int = 0,
    val averageBedtime: LocalTime? = null,
    val earliestBedtime: LocalTime? = null,
    val latestBedtime: LocalTime? = null,
    val lateNightDays: Int = 0,
)

sealed interface SleepSummaryUiState {
    data object Loading : SleepSummaryUiState
    data class Ready(
        val mode: SleepSummaryMode,
        val selectedMonth: YearMonth,
        val selectedYear: Int,
        val records: List<SleepRecordEntity>,
        val monthStatistics: SleepMonthStatistics,
    ) : SleepSummaryUiState
    data class Error(val message: String) : SleepSummaryUiState
}

data class SleepSummarySelection(
    val mode: SleepSummaryMode,
    val selectedMonth: YearMonth,
    val selectedYear: Int,
    val refreshKey: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SleepSummaryViewModel(
    private val repository: SleepRepository,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val query = MutableStateFlow(
        SleepSummarySelection(
            mode = SleepSummaryMode.MONTH,
            selectedMonth = YearMonth.from(today),
            selectedYear = today.year,
        ),
    )
    val selection: StateFlow<SleepSummarySelection> = query.asStateFlow()
    private val currentMonth = YearMonth.from(today)
    private val currentYear = today.year

    val uiState = query
        .flatMapLatest(::stateForQuery)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SleepSummaryUiState.Loading,
        )

    fun setMode(mode: SleepSummaryMode) {
        val current = query.value
        query.value = if (mode == SleepSummaryMode.MONTH && current.mode == SleepSummaryMode.YEAR) {
            val month = if (current.selectedYear == currentYear) {
                currentMonth.monthValue
            } else {
                current.selectedMonth.monthValue
            }
            current.copy(
                mode = mode,
                selectedMonth = YearMonth.of(current.selectedYear, month),
            )
        } else {
            current.copy(mode = mode)
        }
    }

    fun previousPeriod() {
        val current = query.value
        query.value = when (current.mode) {
            SleepSummaryMode.MONTH -> {
                val month = current.selectedMonth.minusMonths(1)
                current.copy(selectedMonth = month, selectedYear = month.year)
            }
            SleepSummaryMode.YEAR -> current.copy(selectedYear = current.selectedYear - 1)
        }
    }

    fun nextPeriod() {
        val current = query.value
        query.value = when (current.mode) {
            SleepSummaryMode.MONTH -> {
                val month = current.selectedMonth.plusMonths(1).coerceAtMost(currentMonth)
                current.copy(selectedMonth = month, selectedYear = month.year)
            }
            SleepSummaryMode.YEAR -> current.copy(
                selectedYear = (current.selectedYear + 1).coerceAtMost(currentYear),
            )
        }
    }

    fun openMonth(yearMonth: YearMonth) {
        query.value = query.value.copy(
            mode = SleepSummaryMode.MONTH,
            selectedMonth = yearMonth,
            selectedYear = yearMonth.year,
        )
    }

    fun retry() {
        query.value = query.value.copy(refreshKey = query.value.refreshKey + 1)
    }

    private fun stateForQuery(query: SleepSummarySelection): Flow<SleepSummaryUiState> {
        val records = when (query.mode) {
            SleepSummaryMode.MONTH -> repository.observeMonth(query.selectedMonth)
            SleepSummaryMode.YEAR -> repository.observeYear(query.selectedYear)
        }
        return records
            .map<List<SleepRecordEntity>, SleepSummaryUiState> { items ->
                SleepSummaryUiState.Ready(
                    mode = query.mode,
                    selectedMonth = query.selectedMonth,
                    selectedYear = query.selectedYear,
                    records = items,
                    monthStatistics = if (query.mode == SleepSummaryMode.MONTH) {
                        items.toMonthStatistics()
                    } else {
                        SleepMonthStatistics()
                    },
                )
            }
            .onStart { emit(SleepSummaryUiState.Loading) }
            .catch { emit(SleepSummaryUiState.Error(it.message ?: "睡眠记录加载失败")) }
    }

    companion object {
        fun factory(repository: SleepRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SleepSummaryViewModel(repository) as T
            }
    }
}

private fun List<SleepRecordEntity>.toMonthStatistics(): SleepMonthStatistics {
    val times = map { it.sleepDateTime.toLocalTime() }
    val sorted = times.sortedBy(SleepDateRules::continuousMinutes)
    return SleepMonthStatistics(
        recordedDays = map { it.recordDate }.distinct().size,
        averageBedtime = SleepDateRules.averageBedtime(times),
        earliestBedtime = sorted.firstOrNull(),
        latestBedtime = sorted.lastOrNull(),
        lateNightDays = times.count(SleepDateRules::isLateNight),
    )
}

private fun YearMonth.coerceAtMost(maximum: YearMonth): YearMonth = if (this > maximum) maximum else this
