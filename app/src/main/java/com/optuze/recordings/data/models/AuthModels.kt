package com.optuze.recordings.data.models

data class GoogleVerifyRequest(
    val token: String,
    val packageId: String
)

data class AuthResponse(
    val success: Boolean,
    val data: AuthData
)

data class AuthData(
    val token: String,
    val user: User
)

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val picture: String,
    val accessLevel: String
) 