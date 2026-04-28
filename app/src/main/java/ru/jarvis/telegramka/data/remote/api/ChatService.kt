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
import timber.log.Timber
import java.time.Instant
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

    suspend fun getMessages(chatId: String, before: Long? = null, limit: Int? = null): List<MessageDto> {
        val response = client.get("$baseUrl/chats/$chatId/messages") {
            before?.let {
                parameter("before", Instant.ofEpochMilli(it).toString())
            }
            limit?.let {
                parameter("limit", it)
            }
        }
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
