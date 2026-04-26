package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.mapper.toDomain
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.model.SendMessageRequest
import ru.jarvis.telegramka.domain.model.Chat
import ru.jarvis.telegramka.domain.model.Message
import timber.log.Timber
import javax.inject.Inject

class ChatRepository @Inject constructor(private val chatService: ChatService) {

    suspend fun getChats(): Result<List<Chat>> {
        return try {
            val chatDtos = chatService.getChats()
            val chats = chatDtos.map { it.toDomain() }
            Result.success(chats)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get chats")
            Result.failure(e)
        }
    }

    suspend fun getMessages(chatId: String): Result<List<Message>> {
        return try {
            val messageDtos = chatService.getMessages(chatId)
            val messages = messageDtos.map { it.toDomain() }
            Result.success(messages)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get messages for chat id: %s", chatId)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, messageId: String, text: String): Result<Message> {
        return try {
            val request = SendMessageRequest(id = messageId, chatId = chatId, text = text)
            val sentMessageDto = chatService.sendMessage(request)
            Result.success(sentMessageDto.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message to chat id: %s", chatId)
            Result.failure(e)
        }
    }
}
