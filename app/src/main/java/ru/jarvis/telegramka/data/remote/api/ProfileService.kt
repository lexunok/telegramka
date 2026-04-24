package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import ru.jarvis.telegramka.data.remote.model.UserDto
import ru.jarvis.telegramka.di.AuthClient
import javax.inject.Inject

class ProfileService @Inject constructor(
    @AuthClient private val client: HttpClient
) {
    private val baseUrl = BuildConfig.API_BASE_URL

    suspend fun getProfile(): UserDto {
        val response = client.get("$baseUrl/profile")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }
}
