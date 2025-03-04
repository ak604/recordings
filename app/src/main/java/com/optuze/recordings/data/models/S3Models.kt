package com.optuze.recordings.data.models

data class PresignedUrlResponse(
    val uploadURL: String,
    val fileName: String,
    val durationSeconds: Int? = null
)

data class DownloadUrlResponse(
    val success: Boolean,
    val data: DownloadUrlData
)

data class DownloadUrlData(
    val downloadURL: String,
    val fileName: String,
    val fileType: String,
    val callId: String
) 