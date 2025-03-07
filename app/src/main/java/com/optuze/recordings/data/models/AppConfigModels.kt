package com.optuze.recordings.data.models

data class AppLoadResponse(
    val success: Boolean,
    val data: AppLoadData
)

data class AppLoadData(
    val user: UserProfile,
    val app: AppConfig
)

data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val picture: String,
    val accessLevel: String,
    val wallet: Wallet,
    val createdAt: String
)

data class Wallet(
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