package com.optuze.recordings.data.api

import com.optuze.recordings.data.models.PresignedUrlResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface S3Service {
    @GET("s3/generate-presigned-url")
    suspend fun getPresignedUrl(
        @Query("fileName") fileName: String,
        @Query("fileType") fileType: String,
        @Query("durationSeconds") durationSeconds: Int? = null
    ): Response<PresignedUrlResponse>
} 