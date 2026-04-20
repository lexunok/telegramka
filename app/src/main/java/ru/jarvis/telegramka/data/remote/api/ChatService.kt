package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import ru.jarvis.telegramka.data.remote.model.ChatDto
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import javax.inject.Inject

class ChatService @Inject constructor(
    private val client: HttpClient
) {
    private val baseUrl = "http://10.0.2.2:3000/api"

    suspend fun getChats(): List<ChatDto> {
        val response = client.get("$baseUrl/chats")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }
}
