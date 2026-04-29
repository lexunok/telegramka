package ru.jarvis.telegramka.domain.model

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isPending: Boolean = false,
    val isFailed: Boolean = false,
    val isRead: Boolean = false
)
