package com.clamit.data.repository

import com.clamit.data.api.ScheduleApi
import com.clamit.data.model.*

class ScheduleRepository(private val api: ScheduleApi) {

    // Templates
    suspend fun createTemplate(req: CreateTemplateRequest) = api.createTemplate(req)
    suspend fun listTemplates() = api.listTemplates()
    suspend fun getTemplate(id: String) = api.getTemplate(id)
    suspend fun updateTemplate(id: String, req: UpdateTemplateRequest) = api.updateTemplate(id, req)
    suspend fun deleteTemplate(id: String) = api.deleteTemplate(id)

    // Blocks
    suspend fun createBlock(templateId: String, req: CreateBlockRequest) = api.createBlock(templateId, req)
    suspend fun deleteBlock(id: String) = api.deleteBlock(id)

    // Subtasks
    suspend fun createSubtask(blockId: String, req: SubtaskRequest) = api.createSubtask(blockId, req)
    suspend fun deleteSubtask(id: String) = api.deleteSubtask(id)

    // Schedule
    suspend fun getEntry(date: String) = api.getEntry(date)
    suspend fun setEntryTemplate(date: String, templateId: String?) =
        api.setEntryTemplate(date, SetTemplateRequest(templateId))
    suspend fun addSpecialBlock(date: String, blockId: String) =
        api.addSpecialBlock(date, AddBlockRequest(blockId))
    suspend fun removeSpecialBlock(date: String, blockId: String) =
        api.removeSpecialBlock(date, blockId)

    // Status
    suspend fun toggleSubtask(date: String, blockId: String, subtaskId: String) =
        api.toggleSubtask(date, blockId, ToggleSubtaskRequest(subtaskId))
    suspend fun updateManualStatus(date: String, blockId: String, status: String) =
        api.updateManualStatus(date, blockId, UpdateManualStatusRequest(status))
}
