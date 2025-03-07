package com.optuze.recordings.data.api

import com.optuze.recordings.data.models.AppLoadResponse
import retrofit2.Response
import retrofit2.http.GET

interface AppConfigService {
    @GET("app/load")
    suspend fun loadAppConfig(): Response<AppLoadResponse>
} 