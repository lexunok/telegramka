package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyCodeRequest(
    val email: String,
    val code: String
)
