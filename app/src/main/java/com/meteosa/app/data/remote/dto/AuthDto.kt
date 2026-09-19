package com.meteosa.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("displayName") val displayName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    @SerializedName("userId") val userId: String,
    val email: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("preferredLanguage") val preferredLanguage: String = "en",
    val points: Int = 0
)
