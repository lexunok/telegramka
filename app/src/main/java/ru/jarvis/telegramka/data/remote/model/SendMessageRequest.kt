package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val id: String,
    @SerialName("chat_id")
    val chatId: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val text: String
)
