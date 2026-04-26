package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.data.remote.model.ChatDto
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.remote.model.SendMessageRequest
import ru.jarvis.telegramka.di.AuthClient
import javax.inject.Inject

class ChatService @Inject constructor(
    @AuthClient private val client: HttpClient
) {
    private val baseUrl = BuildConfig.API_BASE_URL

    suspend fun getChats(): List<ChatDto> {
        val response = client.get("$baseUrl/chats")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }

    suspend fun getMessages(chatId: String): List<MessageDto> {
        val response = client.get("$baseUrl/chats/$chatId/messages")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }

    suspend fun sendMessage(request: SendMessageRequest): MessageDto {
        val response = client.post("$baseUrl/chats/messages") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }
}
