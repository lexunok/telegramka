package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.Chat
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.model.ChatDto
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ChatRepository(private val chatService: ChatService) {

    suspend fun getChats(): Result<List<Chat>> {
        return try {
            val chatDtos = chatService.getChats()
            val chats = chatDtos.map { it.toChat() }
            Result.success(chats)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun ChatDto.toChat(): Chat {
        return Chat(
            id = this.id,
            name = this.name,
            nickname = this.nickname,
            lastMessage = this.lastMessage,
            lastMessageTime = this.lastMessageTime?.let { parseRfc3339(it) },
            unread = this.unread,
            avatarUrl = this.avatarUrl
        )
    }

    private fun parseRfc3339(timestamp: String): Long {
        return try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            // Handle parsing error, maybe return current time or a default value
            System.currentTimeMillis()
        }
    }
}
