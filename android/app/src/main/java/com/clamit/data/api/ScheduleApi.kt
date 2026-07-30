package com.clamit.data.api

import com.clamit.data.model.*
import retrofit2.http.*

interface ScheduleApi {

    // Templates
    @POST("api/templates")
    suspend fun createTemplate(@Body req: CreateTemplateRequest): DayTemplate

    @GET("api/templates")
    suspend fun listTemplates(): List<DayTemplate>

    @GET("api/templates/{id}")
    suspend fun getTemplate(@Path("id") id: String): DayTemplate

    @PUT("api/templates/{id}")
    suspend fun updateTemplate(@Path("id") id: String, @Body req: UpdateTemplateRequest): DayTemplate

    @DELETE("api/templates/{id}")
    suspend fun deleteTemplate(@Path("id") id: String)

    // Blocks
    @POST("api/templates/{tid}/blocks")
    suspend fun createBlock(@Path("tid") tid: String, @Body req: CreateBlockRequest): TimeBlock

    @DELETE("api/blocks/{bid}")
    suspend fun deleteBlock(@Path("bid") bid: String)

    // Subtasks
    @POST("api/blocks/{bid}/subtasks")
    suspend fun createSubtask(@Path("bid") bid: String, @Body req: SubtaskRequest): Subtask

    @DELETE("api/subtasks/{sid}")
    suspend fun deleteSubtask(@Path("sid") sid: String)

    // Schedule
    @GET("api/schedule/{date}")
    suspend fun getEntry(@Path("date") date: String): ScheduleEntry

    @PUT("api/schedule/{date}/template")
    suspend fun setEntryTemplate(@Path("date") date: String, @Body req: SetTemplateRequest): ScheduleEntry

    @POST("api/schedule/{date}/blocks")
    suspend fun addSpecialBlock(@Path("date") date: String, @Body req: AddBlockRequest)

    @DELETE("api/schedule/{date}/blocks/{bid}")
    suspend fun removeSpecialBlock(@Path("date") date: String, @Path("bid") bid: String)

    // Status
    @PUT("api/schedule/{date}/block/{bid}/toggle")
    suspend fun toggleSubtask(@Path("date") date: String, @Path("bid") bid: String, @Body req: ToggleSubtaskRequest)

    @PATCH("api/schedule/{date}/block/{bid}/manual")
    suspend fun updateManualStatus(@Path("date") date: String, @Path("bid") bid: String, @Body req: UpdateManualStatusRequest)
}
