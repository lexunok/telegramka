package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String
)
