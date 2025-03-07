package com.optuze.recordings.data.models

data class AppLoadResponse(
    val success: Boolean,
    val data: AppLoadData
)

data class AppLoadData(
    val user: UserProfile,
    val app: AppConfig,
    val userRewards: List<UserReward>? = null,
    val rewardsApplied: Boolean = false
)

data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val picture: String,
    val accessLevel: String,
    val wallet: Wallet,
    val lastRewardTime: String? = null,
    val createdAt: String
)

data class Wallet(
    val gold: Int
)

data class UserReward(
    val rewardId: String,
    val tokens: RewardTokens
)

data class RewardTokens(
    val gold: Int
)

data class AppConfig(
    val costs: AppCosts,
    val createdAt: String,
    val appId: String,
    val description: String,
    val name: String
)

data class AppCosts(
    val transcription: TranscriptionCost
)

data class TranscriptionCost(
    val tokenName: String,
    val costThresholdValues: List<List<Int>>
) 