package com.clamit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayTemplate(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val repeatDays: List<Int> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val icon: String,
    val repeatDays: List<Int>
)

@Serializable
data class UpdateTemplateRequest(
    val name: String? = null,
    val icon: String? = null,
    val repeatDays: List<Int>? = null
)

@Serializable
data class TimeBlock(
    val id: String = "",
    val templateId: String = "",
    val name: String = "",
    val icon: String = "",
    val mode: String = "", // "start_end" | "start_duration"
    val startTime: String = "",
    val endTime: String? = null,
    val durationMin: Int? = null,
    val blockOrder: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class CreateBlockRequest(
    val name: String,
    val icon: String,
    val mode: String,
    val startTime: String,
    val endTime: String? = null,
    val durationMin: Int? = null,
    val subtasks: List<SubtaskRequest> = emptyList()
)

@Serializable
data class SubtaskRequest(
    val name: String
)

@Serializable
data class Subtask(
    val id: String = "",
    val timeBlockId: String = "",
    val name: String = "",
    val subtaskOrder: Int = 0
)

@Serializable
data class ScheduleEntry(
    val id: String = "",
    val date: String = "",
    val templateId: String? = null,
    val templateName: String? = null,
    val isSpecial: Boolean = false,
    val blocks: List<BlockState> = emptyList(),
    val createdAt: String = ""
)

@Serializable
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

@Serializable
data class SubtaskState(
    val id: String = "",
    val subtaskId: String = "",
    val name: String = "",
    val done: Boolean = false,
    val order: Int = 0
)

@Serializable
data class SetTemplateRequest(
    val templateId: String? = null
)

@Serializable
data class ToggleSubtaskRequest(
    val subtaskId: String
)

@Serializable
data class UpdateManualStatusRequest(
    val status: String // "completed" | "not_completed"
)

@Serializable
data class AddBlockRequest(
    val blockId: String
)
