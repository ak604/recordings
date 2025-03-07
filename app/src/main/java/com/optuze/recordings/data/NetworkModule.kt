package com.optuze.recordings.data

import android.util.Log
import com.optuze.recordings.data.api.S3Service
import com.optuze.recordings.data.api.TemplateService
import com.optuze.recordings.data.api.CallService
import com.optuze.recordings.data.api.AppConfigService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "http://192.168.0.165:3000/"
    private const val TAG = "NetworkModule"

    fun createAuthenticatedClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, "Network Log: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val token = sessionManager.getAuthToken()

                val request = original.newBuilder().apply {
                    header("Content-Type", "application/json")
                    header("Accept", "application/json")
                    if (token != null) {
                        header("Authorization", "Bearer $token")
                    }
                }.build()

                Log.d(TAG, "Making request to: ${request.url}")
                chain.proceed(request)
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createS3Service(sessionManager: SessionManager): S3Service {
        val client = createAuthenticatedClient(sessionManager)
        val retrofit = createRetrofit(client)
        return retrofit.create(S3Service::class.java)
    }

    fun createTemplateService(sessionManager: SessionManager): TemplateService {
        val client = createAuthenticatedClient(sessionManager)
        val retrofit = createRetrofit(client)
        return retrofit.create(TemplateService::class.java)
    }

    fun createCallService(sessionManager: SessionManager): CallService {
        val client = createAuthenticatedClient(sessionManager)
        val retrofit = createRetrofit(client)
        return retrofit.create(CallService::class.java)
    }

    fun createAppConfigService(sessionManager: SessionManager): AppConfigService {
        val client = createAuthenticatedClient(sessionManager)
        val retrofit = createRetrofit(client)
        return retrofit.create(AppConfigService::class.java)
    }

} 