package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val text: String,
    val created_at: String
)
