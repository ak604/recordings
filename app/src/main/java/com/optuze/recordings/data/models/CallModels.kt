package com.optuze.recordings.data.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    val fileType: String,
    val durationSeconds: Int? = null
) {

    fun getFormattedDuration(): String {
        if (durationSeconds == null) return "00:00"

        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getFormattedDate(): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // The original time is in UTC

            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // Convert to local time zone

            val date = inputFormat.parse(createdAt)
            return date?.let { outputFormat.format(it) } ?: createdAt
        } catch (e: Exception) {
            // Fallback to simple split if parsing fails
            return createdAt.split("T")[0]
        }
    }
    
    fun getShortTranscription(): String {
        // Completely null-safe implementation
        if (transcription == null) {
            return ""
        }
        
        return if (transcription.length > 20) {
            "${transcription.substring(0, 20)}..."
        } else {
            transcription
        }
    }
} 