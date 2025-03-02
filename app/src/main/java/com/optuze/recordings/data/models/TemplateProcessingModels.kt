package com.optuze.recordings.data.models

import com.google.gson.annotations.SerializedName

data class TemplateProcessingResponse(
    val success: Boolean,
    val data: TemplateProcessingData
)

data class TemplateProcessingData(
    val templates: Map<String, String>,
    val fileName: String,
    val contextId: String,
    val transcription: String,
    val callId: String,
    val userId: String,
    val createdAt: String,
    val fileType: String
) 