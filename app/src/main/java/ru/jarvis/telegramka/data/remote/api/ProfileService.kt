package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import ru.jarvis.telegramka.data.remote.KtorClient
import ru.jarvis.telegramka.data.remote.model.UserDto

class ProfileService {
    private val client = KtorClient.httpClient
    private val baseUrl = KtorClient.getBaseUrl()

    suspend fun getProfile(): UserDto {
        return client.get("$baseUrl/profile").body()
    }
}
