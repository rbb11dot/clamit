package com.clamit.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clamit.data.model.*
import com.clamit.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ScheduleUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val entry: ScheduleEntry? = null,
    val templates: List<DayTemplate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dateStr = _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                // First create entry if it doesn't exist
                try {
                    repository.createEntry(dateStr)
                } catch (_: Exception) { /* already exists, ignore */ }
                val entry = repository.getEntry(dateStr)
                val templates = repository.listTemplates()
                _uiState.value = _uiState.value.copy(
                    entry = entry,
                    templates = templates,
                    isLoading = false
                )
            } catch (e: retrofit2.HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "HTTP ${e.code()}: ${e.message()}"
                )
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun goToPreviousDay() {
        _uiState.value = _uiState.value.copy(
            currentDate = _uiState.value.currentDate.minusDays(1)
        )
        load()
    }

    fun goToNextDay() {
        _uiState.value = _uiState.value.copy(
            currentDate = _uiState.value.currentDate.plusDays(1)
        )
        load()
    }

    fun goToToday() {
        _uiState.value = _uiState.value.copy(
            currentDate = LocalDate.now()
        )
        load()
    }

    fun setDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(currentDate = date)
        load()
    }

    fun toggleSubtask(blockId: String, subtaskId: String) {
        viewModelScope.launch {
            try {
                val dateStr = _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.toggleSubtask(dateStr, blockId, subtaskId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setManualStatus(blockId: String, status: String) {
        viewModelScope.launch {
            try {
                val dateStr = _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.updateManualStatus(dateStr, blockId, status)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setEntryTemplate(templateId: String?) {
        viewModelScope.launch {
            try {
                val dateStr = _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.setEntryTemplate(dateStr, templateId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createBlock(templateId: String, name: String, icon: String, mode: String, startTime: String, endTime: String?, durationMin: Int?, subtaskNames: List<String>) {
        viewModelScope.launch {
            try {
                val req = CreateBlockRequest(
                    name = name,
                    icon = icon,
                    mode = mode,
                    startTime = startTime,
                    endTime = endTime,
                    durationMin = durationMin,
                    subtasks = subtaskNames.map { SubtaskRequest(it) }
                )
                repository.createBlock(templateId, req)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createTemplate(name: String, icon: String, repeatDays: List<Int>) {
        viewModelScope.launch {
            try {
                repository.createTemplate(CreateTemplateRequest(name, icon, repeatDays))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    fun updateTemplate(id: String, name: String, icon: String, repeatDays: List<Int>) {
        viewModelScope.launch {
            try {
                repository.updateTemplate(id, UpdateTemplateRequest(name, icon, repeatDays))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun addBlockToDate(blockId: String) {
        viewModelScope.launch {
            try {
                val dateStr = _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.addSpecialBlock(dateStr, blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteBlock(blockId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBlock(blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTemplate(templateId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
