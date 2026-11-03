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

/** SSO (Part 3): sent after Credential Manager returns a Google-issued ID token. */
data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String
)

/** Push notifications (Part 3): the device's current Firebase Cloud Messaging token. */
data class FcmTokenRequest(
    val token: String
)

/** Profile picture upload (Part 3 User Defined feature): [imageBase64] is a downscaled,
 *  compressed JPEG/PNG, already small enough to send as JSON rather than multipart. */
data class AvatarUploadRequest(
    val imageBase64: String,
    val contentType: String
)

data class AvatarUploadResponse(
    val avatarUrl: String
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
    val points: Int = 0,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)
