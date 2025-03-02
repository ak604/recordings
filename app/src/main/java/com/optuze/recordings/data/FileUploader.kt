package com.optuze.recordings.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class FileUploader {
    companion object {
        private const val TAG = "FileUploader"
        
        suspend fun uploadFile(file: File, uploadUrl: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().build()
                
                val mediaType = when {
                    file.name.endsWith(".mp3") -> "audio/mpeg"
                    file.name.endsWith(".m4a") -> "audio/mp4"
                    file.name.endsWith(".wav") -> "audio/wav"
                    else -> "application/octet-stream"
                }
                
                val requestBody = file.asRequestBody(mediaType.toMediaTypeOrNull())
                
                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                val isSuccessful = response.isSuccessful
                if (isSuccessful) {
                    Log.d(TAG, "File uploaded successfully: ${file.name}")
                } else {
                    Log.e(TAG, "File upload failed: ${response.code} - ${response.message}")
                }
                
                return@withContext isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading file", e)
                return@withContext false
            }
        }
    }
} 