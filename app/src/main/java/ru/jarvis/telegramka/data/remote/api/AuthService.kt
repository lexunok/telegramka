package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ru.jarvis.telegramka.data.remote.KtorClient
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import ru.jarvis.telegramka.data.remote.model.LoginRequest
import ru.jarvis.telegramka.data.remote.model.AuthResponse
import ru.jarvis.telegramka.data.remote.model.RegisterRequest
import ru.jarvis.telegramka.data.remote.model.VerifyCodeRequest

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
    object UserNotFound : AuthResult<Nothing>()
    object NetworkError : AuthResult<Nothing>()
}

class AuthService {
    private val client = KtorClient.httpClient
    private val baseUrl = KtorClient.getBaseUrl()

    suspend fun login(email: String): AuthResult<Unit> {
        return try {
            val response = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email))
            }

            when (response.status) {
                HttpStatusCode.OK -> AuthResult.Success(Unit)
                HttpStatusCode.NotFound -> AuthResult.UserNotFound
                else -> {
                    val errorResponse = response.body<ErrorResponse>()
                    AuthResult.Error(errorResponse.error, response.status.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.NetworkError
        }
    }

    suspend fun register(name: String, email: String, nickname: String): AuthResult<Unit> {
        return try {
            val response = client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(name, email, nickname))
            }

            when (response.status) {
                HttpStatusCode.OK -> AuthResult.Success(Unit)
                else -> {
                    val errorResponse = response.body<ErrorResponse>()
                    AuthResult.Error(errorResponse.error, response.status.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.NetworkError
        }
    }

    suspend fun verifyCode(email: String, code: String): AuthResult<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody(VerifyCodeRequest(email, code))
            }

            when (response.status) {
                HttpStatusCode.OK -> AuthResult.Success(response.body<AuthResponse>())
                else -> {
                    val errorResponse = response.body<ErrorResponse>()
                    AuthResult.Error(errorResponse.error, response.status.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.NetworkError
        }
    }

    suspend fun refreshToken(refreshToken: String): AuthResult<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("refresh_token" to refreshToken))
            }

            when (response.status) {
                HttpStatusCode.OK -> AuthResult.Success(response.body<AuthResponse>())
                else -> {
                    val errorResponse = response.body<ErrorResponse>()
                    AuthResult.Error(errorResponse.error, response.status.value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.NetworkError
        }
    }
}

