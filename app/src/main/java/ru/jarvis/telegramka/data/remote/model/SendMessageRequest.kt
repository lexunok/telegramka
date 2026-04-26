package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val id: String,
    val chatId: String,
    val text: String
)
