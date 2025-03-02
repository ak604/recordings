package com.optuze.recordings.data.api

import com.optuze.recordings.data.models.TemplateProcessingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TemplateService {
    @GET("calls/{callId}/process")
    suspend fun processTemplate(
        @Path("callId") callId: String,
        @Query("templateName") templateName: String,
        @Query("contextId") contextId: String
    ): Response<TemplateProcessingResponse>
} 