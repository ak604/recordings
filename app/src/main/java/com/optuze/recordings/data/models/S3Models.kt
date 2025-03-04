package com.optuze.recordings.data.models

data class PresignedUrlResponse(
    val uploadURL: String,
    val fileName: String,
    val durationSeconds: Int? = null
) 