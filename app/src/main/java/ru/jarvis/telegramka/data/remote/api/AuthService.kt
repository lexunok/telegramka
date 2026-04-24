package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import ru.jarvis.telegramka.data.remote.model.LoginRequest
import ru.jarvis.telegramka.data.remote.model.AuthResponse
import ru.jarvis.telegramka.data.remote.model.RegisterRequest
import ru.jarvis.telegramka.data.remote.model.VerifyCodeRequest
import ru.jarvis.telegramka.di.BaseClient
import timber.log.Timber
import javax.inject.Inject

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
    object UserNotFound : AuthResult<Nothing>()
    object NetworkError : AuthResult<Nothing>()
}

class AuthService @Inject constructor(
    @BaseClient private val client: HttpClient
) {
    private val baseUrl = BuildConfig.API_BASE_URL

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
            Timber.e(e, "Failed to login")
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
            Timber.e(e, "Failed to register")
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
            Timber.e(e, "Failed to verify code")
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
            Timber.e(e, "Failed to refresh token")
            AuthResult.NetworkError
        }
    }
}

