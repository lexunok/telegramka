package ru.jarvis.telegramka.data

data class Chat(
    val id: String,
    val name: String,
    val nickname: String,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unread: Int? = null,
    val avatarUrl: String? = null
)
