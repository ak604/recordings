package com.optuze.recordings.data.models

import com.google.gson.annotations.SerializedName

data class CallsResponse(
    val success: Boolean,
    val data: List<Call>,
    val nextToken: String?
)

data class Call(
    val templates: Map<String, String>?,
    val fileName: String,
    val contextId: String,
    val transcription: String?,
    val callId: String,
    val userId: String,
    val createdAt: String,
    val fileType: String
) {
    fun getFormattedDate(): String {
        // Simple date formatting - in a real app, you'd want to parse and format properly
        return createdAt.split("T")[0]
    }
    
    fun getShortTranscription(): String {
        // Completely null-safe implementation
        if (transcription == null) {
            return "No transcription available"
        }
        
        return if (transcription.length > 50) {
            "${transcription.substring(0, 50)}..."
        } else {
            transcription
        }
    }
} 