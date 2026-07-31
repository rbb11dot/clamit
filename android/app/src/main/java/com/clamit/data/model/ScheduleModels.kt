package com.clamit.data.model

data class DayTemplate(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val repeatDays: List<Int> = emptyList(),
    val blocks: List<TimeBlock> = emptyList(),
    val createdAt: String = ""
)

data class CreateTemplateRequest(
    val name: String,
    val icon: String,
    val repeatDays: List<Int>
)

data class UpdateTemplateRequest(
    val name: String? = null,
    val icon: String? = null,
    val repeatDays: List<Int>? = null
)

data class TimeBlock(
    val id: String = "",
    val templateId: String = "",
    val name: String = "",
    val icon: String = "",
    val mode: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val durationMin: Int? = null,
    val blockOrder: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: String = ""
)

data class CreateBlockRequest(
    val name: String,
    val icon: String,
    val mode: String,
    val startTime: String,
    val endTime: String? = null,
    val durationMin: Int? = null,
    val subtasks: List<SubtaskRequest> = emptyList()
)

data class SubtaskRequest(
    val name: String
)

data class Subtask(
    val id: String = "",
    val timeBlockId: String = "",
    val name: String = "",
    val subtaskOrder: Int = 0
)

data class ScheduleEntry(
    val id: String = "",
    val date: String = "",
    val templateId: String? = null,
    val templateName: String? = null,
    val isSpecial: Boolean = false,
    val blocks: List<BlockState> = emptyList(),
    val createdAt: String = ""
)

data class BlockState(
    val id: String = "",
    val timeBlockId: String = "",
    val name: String = "",
    val icon: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val durationMin: Int? = null,
    val mode: String = "",
    val blockOrder: Int = 0,
    val autoStatus: String = "pending",
    val manualStatus: String = "not_completed",
    val subtaskStates: List<SubtaskState> = emptyList()
)

data class SubtaskState(
    val id: String = "",
    val subtaskId: String = "",
    val name: String = "",
    val done: Boolean = false,
    val order: Int = 0
)

data class SetTemplateRequest(
    val templateId: String? = null
)

data class ToggleSubtaskRequest(
    val subtaskId: String
)

data class UpdateManualStatusRequest(
    val status: String
)

data class AddBlockRequest(
    val blockId: String
)
