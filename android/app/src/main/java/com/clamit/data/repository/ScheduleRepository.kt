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

    // Blocks — the library
    suspend fun createBlock(req: CreateBlockRequest) = api.createBlock(req)
    suspend fun listBlocks() = api.listBlocks()
    suspend fun updateBlock(id: String, req: UpdateBlockRequest) = api.updateBlock(id, req)
    suspend fun deleteBlock(id: String) = api.deleteBlock(id)

    // Template ↔ block associations
    suspend fun addTemplateBlock(templateId: String, blockId: String) =
        api.addTemplateBlock(templateId, AddBlockRequest(blockId))
    suspend fun removeTemplateBlock(templateId: String, blockId: String) =
        api.removeTemplateBlock(templateId, blockId)

    // Subtasks
    suspend fun createSubtask(blockId: String, req: SubtaskRequest) = api.createSubtask(blockId, req)
    suspend fun deleteSubtask(id: String) = api.deleteSubtask(id)

    // Schedule
    suspend fun getEntry(date: String) = api.getEntry(date)
    suspend fun createEntry(date: String) = api.createEntry(date)
    suspend fun setEntryTemplate(date: String, templateId: String?) =
        api.setEntryTemplate(date, SetTemplateRequest(templateId))
    suspend fun addSpecialBlock(date: String, blockId: String) =
        api.addSpecialBlock(date, AddBlockRequest(blockId))
    suspend fun updateEntryBlock(date: String, blockId: String, req: UpdateBlockRequest) =
        api.updateEntryBlock(date, blockId, req)
    suspend fun removeSpecialBlock(date: String, blockId: String) =
        api.removeSpecialBlock(date, blockId)

    // Status
    suspend fun toggleSubtask(date: String, blockId: String, subtaskId: String) =
        api.toggleSubtask(date, blockId, ToggleSubtaskRequest(subtaskId))
    suspend fun updateManualStatus(date: String, blockId: String, status: String) =
        api.updateManualStatus(date, blockId, UpdateManualStatusRequest(status))
}
