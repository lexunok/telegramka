package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    val id: String,
    val name: String,
    val nickname: String,
    val lastMessage: String?,
    val lastMessageTime: String?,
    val unread: Int,
    val avatarUrl: String?,
    val userId: String? = null
)
