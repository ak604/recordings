package com.optuze.recordings.data.api

import com.optuze.recordings.data.models.AuthResponse
import com.optuze.recordings.data.models.GoogleVerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("auth/google/verify")
    suspend fun verifyGoogleToken(
        @Body request: GoogleVerifyRequest
    ): Response<AuthResponse>
} 