package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val nickname: String
)
