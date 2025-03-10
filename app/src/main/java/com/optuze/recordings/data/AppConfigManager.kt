package com.optuze.recordings.data

import android.util.Log
import com.optuze.recordings.data.api.AppConfigService
import com.optuze.recordings.data.models.AppLoadData
import com.optuze.recordings.data.models.UserProfile
import com.optuze.recordings.data.models.UserReward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AppConfigManager {
    private const val TAG = "AppConfigManager"
    
    private var appData: AppLoadData? = null
    
    // Make this a nullable StateFlow with initial value null
    private val _rewardsFlow = MutableStateFlow<List<UserReward>?>(null)
    val rewardsFlow: StateFlow<List<UserReward>?> = _rewardsFlow
    
    // Add this property to track shown rewards
    private var rewardsShownIds = mutableSetOf<String>()
    
    // Add this property to track shown reward timestamps
    private var rewardShownTimestamps = mutableMapOf<String, Long>()
    
    fun loadAppConfig(sessionManager: SessionManager) {
        val appConfigService = NetworkModule.createAppConfigService(sessionManager)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = appConfigService.loadAppConfig()
                if (response.isSuccessful && response.body() != null) {
                    appData = response.body()!!.data
                    
                    // Process rewards only if they exist AND rewardsApplied is true
                    val rewards = appData?.userRewards
                    if (!rewards.isNullOrEmpty() && appData?.rewardsApplied == true) {
                        val now = System.currentTimeMillis()
                        val newRewards = rewards.filter { 
                            // Check both ID and timestamp (60 second cooldown)
                            val lastShown = rewardShownTimestamps[it.rewardId] ?: 0
                            !rewardsShownIds.contains(it.rewardId) && (now - lastShown > 60000)
                        }
                        
                        if (newRewards.isNotEmpty()) {
                            _rewardsFlow.value = newRewards
                            newRewards.forEach {
                                rewardsShownIds.add(it.rewardId)
                                rewardShownTimestamps[it.rewardId] = now
                            }
                        } else {
                            _rewardsFlow.value = emptyList()
                        }
                    } else {
                        // Set explicitly to empty list (not null) to indicate rewards were checked but none available
                        _rewardsFlow.value = emptyList()
                        Log.d(TAG, "No rewards to apply")
                    }
                    
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
    
    fun getUserProfile(): UserProfile? {
        return appData?.user
    }
    
    fun getRewards(): List<UserReward>? {
        return appData?.userRewards
    }
    
    fun hasAppliedRewards(): Boolean {
        return appData?.rewardsApplied == true
    }
    
    fun getTokenName(): String {
        return appData?.app?.costs?.transcription?.tokenName ?: "gold"
    }
    
    // Calculate token cost based on duration in seconds
    fun calculateTokenCost(durationSeconds: Int): Int {
        val thresholds = appData?.app?.costs?.transcription?.costThresholdValues ?: return 0
        
        // Find all threshold values where the first element is greater than or equal to the duration
        val applicableThresholds = thresholds.filter { it[0] >= durationSeconds }
        
        // If no applicable thresholds, use the highest one
        return if (applicableThresholds.isEmpty()) {
            thresholds.maxByOrNull { it[0] }?.get(1) ?: 0
        } else {
            // Take the minimum of these values (the least expensive applicable threshold)
            applicableThresholds.minByOrNull { it[0] }?.get(1) ?: 0
        }
    }
    
    fun clearShownRewards() {
        // Clear the flow value
        _rewardsFlow.value = emptyList()
        
        // Clear tracking data
        rewardsShownIds.clear()
        rewardShownTimestamps.clear()
        
        Log.d(TAG, "Cleared shown rewards tracking data")
    }
} 