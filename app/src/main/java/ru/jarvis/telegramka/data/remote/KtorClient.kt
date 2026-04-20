package ru.jarvis.telegramka.data.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import ru.jarvis.telegramka.data.repository.AuthRepository
import ru.jarvis.telegramka.data.repository.RefreshResult
import ru.jarvis.telegramka.data.storage.TokenManager

object KtorClient {
    private const val BASE_URL = "http://10.0.2.2:3000/api" // Assuming default Ktor server port

    // TODO: Use a proper DI framework for AuthRepository injection
    private val authService = ru.jarvis.telegramka.data.remote.api.AuthService()
    private val authRepository = AuthRepository(authService)

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                loadTokens {
                    // Load tokens from TokenManager
                    val accessToken = TokenManager.getAccessToken().first()
                    val refreshToken = TokenManager.getRefreshToken().first()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else {
                        null
                    }
                }
                refreshTokens {
                    // Get the current refresh token
                    val oldRefreshToken = TokenManager.getRefreshToken().first()

                    if (oldRefreshToken != null) {
                        when (val result = authRepository.refreshToken(oldRefreshToken)) {
                            is RefreshResult.Success -> result.tokens
                            is RefreshResult.Error, is RefreshResult.NetworkError -> {
                                // Refresh failed, clear tokens and force re-login
                                TokenManager.clearTokens()
                                null
                            }
                        }
                    } else {
                        // No refresh token available, force re-login
                        TokenManager.clearTokens()
                        null
                    }
                }
            }
        }
    }

    fun getBaseUrl(): String = BASE_URL
}


