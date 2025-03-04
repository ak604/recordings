package com.optuze.recordings.data.api

import com.optuze.recordings.data.models.CallsResponse
import com.optuze.recordings.data.models.DeleteResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CallService {
    @GET("calls")
    suspend fun getCalls(
        @Query("limit") limit: Int = 10,
        @Query("nextToken") nextToken: String? = null
    ): Response<CallsResponse>
    
    @DELETE("calls/{callId}")
    suspend fun deleteCall(
        @Path("callId") callId: String
    ): Response<DeleteResponse>
} 