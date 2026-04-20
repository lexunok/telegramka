package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    val id: String,
    val name: String,
    val nickname: String,
    @SerialName("last_message")
    val lastMessage: String?,
    @SerialName("last_message_time")
    val lastMessageTime: String?,
    val unread: Int,
    @SerialName("avatar_url")
    val avatarUrl: String?
)
