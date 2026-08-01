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
    val libraryBlocks: List<TimeBlock> = emptyList(),
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
                val libraryBlocks = repository.listBlocks()
                if (gen != loadGen) return@launch
                _uiState.value = _uiState.value.copy(
                    entry = entry,
                    templates = templates,
                    libraryBlocks = libraryBlocks,
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

	/** Creates a standalone library block (no template — blocks are shared across
	 *  templates through the junction). Returns the created block id or null. */
	suspend fun createBlockSuspended(
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
			val id = repository.createBlock(req).id
			load() // refresh the library so the new block shows immediately
			id
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

	/** Full-save edit of a library block (fields + subtask sync). Returns false on failure. */
	suspend fun updateBlockSuspended(
		blockId: String,
		name: String,
		icon: String,
		mode: String,
		startTime: String,
		endTime: String?,
		durationMin: Int?,
		subtasks: List<Pair<String?, String>>
	): Boolean {
		return try {
			repository.updateBlock(blockId, buildUpdateRequest(name, icon, mode, startTime, endTime, durationMin, subtasks))
			load()
			true
		} catch (e: Exception) {
			_uiState.value = _uiState.value.copy(
				error = e.message ?: "Blok güncellenemedi"
			)
			false
		}
	}

	/** Full-save edit of a block inside the current day (template-linked days detach
	 *  first, server-side, so the day becomes special and the template is untouched).
	 *  Returns false on failure. */
	suspend fun updateEntryBlockSuspended(
		blockId: String,
		name: String,
		icon: String,
		mode: String,
		startTime: String,
		endTime: String?,
		durationMin: Int?,
		subtasks: List<Pair<String?, String>>
	): Boolean {
		return try {
			repository.updateEntryBlock(
				currentDateStr(), blockId,
				buildUpdateRequest(name, icon, mode, startTime, endTime, durationMin, subtasks)
			)
			load()
			true
		} catch (e: Exception) {
			_uiState.value = _uiState.value.copy(
				error = e.message ?: "Blok güncellenemedi"
			)
			false
		}
	}

	private fun buildUpdateRequest(
		name: String,
		icon: String,
		mode: String,
		startTime: String,
		endTime: String?,
		durationMin: Int?,
		subtasks: List<Pair<String?, String>>
	): UpdateBlockRequest = UpdateBlockRequest(
		name = name,
		icon = icon,
		mode = mode,
		startTime = startTime,
		endTime = endTime,
		durationMin = durationMin,
		subtasks = subtasks.map { (id, text) -> SubtaskSyncRequest(id = id, name = text) }
	)

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

    /** Attaches an existing library block to a template (many-to-many). */
    fun addTemplateBlock(templateId: String, blockId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.addTemplateBlock(templateId, blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    /** Detaches a block from a template. The block stays in the library. */
    fun removeTemplateBlock(templateId: String, blockId: String) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                repository.removeTemplateBlock(templateId, blockId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                mutating = false
            }
        }
    }

    /** Sequential create used by TemplateEditorPage: returns the new id or null. */
    suspend fun createTemplateSuspended(name: String, icon: String, repeatDays: List<Int>): String? {
        return try {
            val id = repository.createTemplate(CreateTemplateRequest(name, icon, repeatDays)).id
            load()
            id
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Şablon oluşturulamadı"
            )
            null
        }
    }

    /** Attaches library blocks to a template right after its creation. */
    suspend fun attachBlocksSuspended(templateId: String, blockIds: List<String>): Boolean {
        return try {
            blockIds.forEach { repository.addTemplateBlock(templateId, it) }
            load()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Bloklar eklenemedi"
            )
            false
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

    /** Full-save template edit that reports success (used by the editor to
     *  decide whether to check for adoptable special days). */
    suspend fun updateTemplateSuspended(
        id: String,
        name: String,
        icon: String,
        repeatDays: List<Int>
    ): Boolean {
        return try {
            repository.updateTemplate(id, UpdateTemplateRequest(name, icon, repeatDays))
            load()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Şablon güncellenemedi"
            )
            false
        }
    }

    /** ISO dates of special days that would adopt the template (empty on failure). */
    suspend fun fetchSpecialDaysSuspended(templateId: String): List<String> {
        return try {
            repository.getTemplateSpecialDays(templateId)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Özel günler alınamadı"
            )
            emptyList()
        }
    }

    /** Applies the template to the given dates. Returns true on success. */
    suspend fun applyTemplateToDatesSuspended(templateId: String, dates: List<String>): Boolean {
        return try {
            repository.applyTemplateToDates(templateId, dates)
            load()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Şablon uygulanamadı"
            )
            false
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
