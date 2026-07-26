package com.example.blue.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.blue.core.util.SleepDateRules
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.repository.SleepRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SleepEditorUiState(
    val recordDate: LocalDate,
    val sleepTimeText: String = "",
    val wakeTimeText: String = "",
    val note: String = "",
    val source: SleepSource = SleepSource.MANUAL,
    val isEstimated: Boolean = false,
    val existingRecord: SleepRecordEntity? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val estimateMessage: String? = null,
    val usageAccessRequired: Boolean = false,
)

sealed interface SleepEditorEvent {
    data class Message(val text: String, val isError: Boolean) : SleepEditorEvent
    data object Saved : SleepEditorEvent
    data object Deleted : SleepEditorEvent
}

class SleepEditorViewModel(
    private val repository: SleepRepository,
    private val estimator: SleepTimeEstimator,
    initialDate: LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SleepEditorUiState(recordDate = initialDate))
    val uiState: StateFlow<SleepEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SleepEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        load(initialDate, attemptEstimateWhenEmpty = true)
    }

    fun selectDate(date: LocalDate) {
        if (date == _uiState.value.recordDate) return
        load(date, attemptEstimateWhenEmpty = true)
    }

    fun selectSleepTime(time: LocalTime) {
        updateSleepTime(time.format(timeFormatter))
    }

    fun updateSleepTime(value: String) {
        val current = _uiState.value
        val hasSelectedTime = value.asTimeOrNull() != null
        _uiState.value = current.copy(
            sleepTimeText = value.take(5),
            source = if (current.source == SleepSource.SYSTEM_ESTIMATE) {
                SleepSource.MANUAL_CONFIRMED
            } else {
                current.source
            },
            isEstimated = false,
            estimateMessage = when {
                current.source == SleepSource.SYSTEM_ESTIMATE -> "已修改并标记为手动确认"
                hasSelectedTime -> "已手动选择入睡时间"
                else -> current.estimateMessage
            },
            usageAccessRequired = if (hasSelectedTime) false else current.usageAccessRequired,
        )
    }

    fun updateWakeTime(value: String) {
        _uiState.value = _uiState.value.copy(wakeTimeText = value.take(5))
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value.take(MAX_NOTE_LENGTH))
    }

    fun confirmEstimate() {
        val current = _uiState.value
        if (current.source == SleepSource.SYSTEM_ESTIMATE) {
            _uiState.value = current.copy(
                source = SleepSource.MANUAL_CONFIRMED,
                isEstimated = false,
                estimateMessage = "系统推测已由你手动确认",
            )
        }
    }

    fun attemptEstimate() {
        val current = _uiState.value
        if (current.sleepTimeText.isNotBlank() && current.source != SleepSource.SYSTEM_ESTIMATE) return
        viewModelScope.launch {
            when (val result = estimator.estimate(_uiState.value.recordDate)) {
                is SleepEstimateResult.Available -> {
                    if (_uiState.value.recordDate == SleepDateRules.recordDateFor(result.dateTime)) {
                        _uiState.value = _uiState.value.copy(
                            sleepTimeText = result.dateTime.toLocalTime().format(timeFormatter),
                            source = SleepSource.SYSTEM_ESTIMATE,
                            isEstimated = true,
                            estimateMessage = "根据夜间最后一次屏幕关闭时间推测",
                            usageAccessRequired = false,
                        )
                    } else {
                        showUnavailableEstimate()
                    }
                }
                SleepEstimateResult.UsageAccessRequired -> {
                    _uiState.value = _uiState.value.copy(
                        estimateMessage = "未获得使用情况访问权限，无法读取屏幕关闭事件",
                        usageAccessRequired = true,
                    )
                }
                is SleepEstimateResult.Unavailable -> {
                    _uiState.value = _uiState.value.copy(
                        estimateMessage = result.message,
                        usageAccessRequired = false,
                    )
                }
            }
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val sleepTime = current.sleepTimeText.asTimeOrNull()
        val wakeTime = current.wakeTimeText.takeIf(String::isNotBlank)?.asTimeOrNull()
        when {
            current.recordDate.isAfter(LocalDate.now()) -> sendMessage("不能记录未来日期", true)
            sleepTime == null -> sendMessage("请选择入睡时间", true)
            current.wakeTimeText.isNotBlank() && wakeTime == null -> sendMessage("请按 HH:mm 填写起床时间", true)
            else -> viewModelScope.launch {
                _uiState.value = current.copy(isSaving = true)
                val sleepDateTime = SleepDateRules.sleepDateTimeFor(current.recordDate, sleepTime)
                val wakeDateTime = wakeTime?.let {
                    SleepDateRules.wakeDateTimeFor(current.recordDate, sleepDateTime, it)
                }
                if (wakeDateTime != null && Duration.between(sleepDateTime, wakeDateTime).toHours() > 24) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    sendMessage("起床时间与睡觉时间相隔超过 24 小时，请检查", true)
                    return@launch
                }
                val now = System.currentTimeMillis()
                runCatching {
                    repository.saveRecord(
                        SleepRecordEntity(
                            id = current.existingRecord?.id ?: UUID.randomUUID().toString(),
                            recordDate = current.recordDate,
                            sleepDateTime = sleepDateTime,
                            wakeDateTime = wakeDateTime,
                            source = current.source,
                            isEstimated = current.isEstimated,
                            note = current.note.trim().ifBlank { null },
                            createdAt = current.existingRecord?.createdAt ?: now,
                            updatedAt = now,
                        ),
                    )
                }.onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    eventChannel.send(SleepEditorEvent.Message("睡眠记录已保存", false))
                    eventChannel.send(SleepEditorEvent.Saved)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    eventChannel.send(SleepEditorEvent.Message(error.message ?: "保存失败，请重试", true))
                }
            }
        }
    }

    fun delete() {
        val record = _uiState.value.existingRecord ?: return
        viewModelScope.launch {
            runCatching { repository.deleteRecord(record.id) }
                .onSuccess { eventChannel.send(SleepEditorEvent.Deleted) }
                .onFailure { eventChannel.send(SleepEditorEvent.Message(it.message ?: "删除失败", true)) }
        }
    }

    private fun load(date: LocalDate, attemptEstimateWhenEmpty: Boolean) {
        loadJob?.cancel()
        _uiState.value = SleepEditorUiState(recordDate = date, isLoading = true)
        loadJob = viewModelScope.launch {
            val record = repository.getRecord(date)
            _uiState.value = if (record == null) {
                SleepEditorUiState(recordDate = date, isLoading = false)
            } else {
                SleepEditorUiState(
                    recordDate = date,
                    sleepTimeText = record.sleepDateTime.toLocalTime().format(timeFormatter),
                    wakeTimeText = record.wakeDateTime?.toLocalTime()?.format(timeFormatter).orEmpty(),
                    note = record.note.orEmpty(),
                    source = record.source,
                    isEstimated = record.isEstimated,
                    existingRecord = record,
                    isLoading = false,
                    estimateMessage = when (record.source) {
                        SleepSource.SYSTEM_ESTIMATE -> "这条时间来自系统推测，可修改或手动确认"
                        SleepSource.MANUAL_CONFIRMED -> "这条时间已由你手动确认"
                        SleepSource.MANUAL -> null
                    },
                )
            }
            if (record == null && attemptEstimateWhenEmpty) attemptEstimate()
        }
    }

    private fun showUnavailableEstimate() {
        _uiState.value = _uiState.value.copy(
            estimateMessage = "暂无系统推测数据，请选择入睡时间",
            usageAccessRequired = false,
        )
    }

    private fun sendMessage(text: String, isError: Boolean) {
        viewModelScope.launch { eventChannel.send(SleepEditorEvent.Message(text, isError)) }
    }

    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private const val MAX_NOTE_LENGTH = 1000

        fun factory(
            repository: SleepRepository,
            estimator: SleepTimeEstimator,
            initialDate: LocalDate,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SleepEditorViewModel(repository, estimator, initialDate) as T
        }
    }
}

private fun String.asTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this, DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: DateTimeParseException) {
    null
}
