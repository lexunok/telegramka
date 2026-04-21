package ru.jarvis.telegramka.data.repository

import jakarta.inject.Inject
import ru.jarvis.telegramka.data.Chat
import ru.jarvis.telegramka.data.Message
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.model.ChatDto
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.remote.model.SendMessageRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ChatRepository @Inject constructor(private val chatService: ChatService) {

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

    suspend fun getMessages(chatId: String): Result<List<Message>> {
        return try {
            val messageDtos = chatService.getMessages(chatId)
            val messages = messageDtos.map { it.toMessage() }
            Result.success(messages)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, text: String): Result<Message> {
        return try {
            val request = SendMessageRequest(text)
            val sentMessageDto = chatService.sendMessage(chatId, request)
            Result.success(sentMessageDto.toMessage())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun MessageDto.toMessage(): Message {
        return Message(
            id = this.id,
            chatId = this.chat_id,
            senderId = this.sender_id,
            text = this.text,
            timestamp = parseRfc3339(this.created_at)
        )
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
