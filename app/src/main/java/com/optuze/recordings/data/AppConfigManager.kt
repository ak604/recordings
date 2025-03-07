package com.optuze.recordings.data

import android.util.Log
import com.optuze.recordings.data.api.AppConfigService
import com.optuze.recordings.data.models.AppLoadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppConfigManager {
    private const val TAG = "AppConfigManager"
    
    private var appData: AppLoadData? = null
    
    fun loadAppConfig(sessionManager: SessionManager) {
        val appConfigService = NetworkModule.createAppConfigService(sessionManager)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = appConfigService.loadAppConfig()
                if (response.isSuccessful && response.body() != null) {
                    appData = response.body()!!.data
                    Log.d(TAG, "App config loaded successfully")
                } else {
                    Log.e(TAG, "Failed to load app config: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading app config", e)
            }
        }
    }
    
    fun getUserWallet(): Int? {
        return appData?.user?.wallet?.gold
    }
    
    fun getUserProfile() = appData?.user
    
    fun getTokenName(): String {
        return appData?.app?.costs?.transcription?.tokenName ?: "gold"
    }
    
    // Calculate token cost based on duration in seconds
    fun calculateTokenCost(durationSeconds: Int): Int {
        val thresholds = appData?.app?.costs?.transcription?.costThresholdValues ?: return 0
        
        // Find all thresholds with first element greater than or equal to duration
        val applicableThresholds = thresholds.filter { it[0] >= durationSeconds }
        
        return if (applicableThresholds.isEmpty()) {
            // If no applicable thresholds, use the highest one
            thresholds.maxByOrNull { it[0] }?.get(1) ?: 0
        } else {
            // Take the minimum threshold that is applicable
            applicableThresholds.minByOrNull { it[0] }?.get(1) ?: 0
        }
    }
} 