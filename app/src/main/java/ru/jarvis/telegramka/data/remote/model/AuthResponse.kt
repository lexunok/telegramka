package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val user: UserDto? = null // User might not be present in refresh token response
)
