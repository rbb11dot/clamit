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

    /** Monotonic load generation — responses from older loads are dropped, so a fast date
     *  switch can never paint one day's date with another day's blocks. */
    private var loadGen = 0

    /** Re-entrancy guard: rapid taps must not fire duplicate mutations that cancel each
     *  other out server-side (e.g. toggle is a server-side NOT flip). */
    private var mutating = false

    init {
        load()
    }

    private fun currentDateStr(): String =
        _uiState.value.currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun load() {
        val gen = ++loadGen
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dateStr = currentDateStr()
                // First create entry if it doesn't exist
                try {
                    repository.createEntry(dateStr)
                } catch (_: Exception) { /* already exists, ignore */ }
                if (gen != loadGen) return@launch
                val entry = repository.getEntry(dateStr)
                val templates = repository.listTemplates()
                if (gen != loadGen) return@launch
                _uiState.value = _uiState.value.copy(
                    entry = entry,
                    templates = templates,
                    isLoading = false
                )
            } catch (e: retrofit2.HttpException) {
                if (gen != loadGen) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "HTTP ${e.code()}: ${e.message()}"
                )
            } catch (e: Throwable) {
                if (gen != loadGen) return@launch
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** Returns the template a new block should belong to: the current day's template,
     *  else the first template, else a freshly created catch-all template.
     *  Returns null on failure (error surfaced in uiState). */
    suspend fun ensureTemplateId(): String? {
        val st = _uiState.value
        st.entry?.templateId?.let { tid ->
            if (st.templates.any { it.id == tid }) return tid
        }
        st.templates.firstOrNull()?.let { return it.id }
        return try {
            val created = repository.createTemplate(
                CreateTemplateRequest("Genel Şablon", "calendar_today", (0..6).toList())
            )
            load()
            created.id
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Şablon oluşturulamadı"
            )
            null
        }
    }

    /** Sequential save used by BlockEditorPage: returns the created block id or null. */
    suspend fun createBlockSuspended(
        templateId: String,
        name: String,
        icon: String,
        mode: String,
        startTime: String,
        endTime: String?,
        durationMin: Int?,
        subtaskNames: List<String>
    ): String? {
        return try {
            val req = CreateBlockRequest(
                name = name,
                icon = icon,
                mode = mode,
                startTime = startTime,
                endTime = endTime,
                durationMin = durationMin,
                subtasks = subtaskNames.map { SubtaskRequest(it) }
            )
            repository.createBlock(templateId, req).id
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Blok oluşturulamadı"
            )
            null
        }
    }

    /** Adds an existing block to the current day (copy-on-write: a template-linked day
     *  detaches and becomes a standalone special day). Returns false on failure. */
    suspend fun addSpecialBlockToCurrentDaySuspended(blockId: String): Boolean {
        return try {
            repository.addSpecialBlock(currentDateStr(), blockId)
            load()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Güne eklenemedi"
            )
            false
        }
    }

    fun toggleSubtask(blockId: String, subtaskId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.toggleSubtask(currentDateStr(), blockId, subtaskId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun setManualStatus(blockId: String, status: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.updateManualStatus(currentDateStr(), blockId, status)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun setEntryTemplate(templateId: String?) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.setEntryTemplate(currentDateStr(), templateId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun createBlock(templateId: String, name: String, icon: String, mode: String, startTime: String, endTime: String?, durationMin: Int?, subtaskNames: List<String>) {
        if (mutating) return
        mutating = true
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
            } finally {
                mutating = false
            }
        }
    }

    fun createTemplate(name: String, icon: String, repeatDays: List<Int>) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.createTemplate(CreateTemplateRequest(name, icon, repeatDays))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun updateTemplate(id: String, name: String, icon: String, repeatDays: List<Int>) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.updateTemplate(id, UpdateTemplateRequest(name, icon, repeatDays))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun addBlockToDate(blockId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.addSpecialBlock(currentDateStr(), blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun deleteBlock(blockId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.deleteBlock(blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    /** Removes a block from the current (special) day only; the library block stays. */
    fun removeSpecialBlockFromDay(blockId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.removeSpecialBlock(currentDateStr(), blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.deleteTemplate(templateId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }
}
