package ru.jarvis.telegramka.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import ru.jarvis.telegramka.data.remote.api.AuthService
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.api.ProfileService
import ru.jarvis.telegramka.data.remote.api.UserService
import ru.jarvis.telegramka.data.storage.TokenManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Provides
    @Singleton
    @UnauthenticatedClient
    fun provideUnauthenticatedHttpClient(json: Json): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthService(@UnauthenticatedClient client: HttpClient): AuthService {
        return AuthService(client)
    }

    @Provides
    @Singleton
    fun provideUserService(@UnauthenticatedClient client: HttpClient): UserService {
        return UserService(client)
    }

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedHttpClient(
        json: Json,
        authService: AuthService,
        tokenManager: TokenManager
    ): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenManager.getAccessToken().first()
                        val refreshToken = tokenManager.getRefreshToken().first()
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }
                    refreshTokens {
                        val oldRefreshToken = tokenManager.getRefreshToken().first()
                        if (oldRefreshToken != null) {
                            val result = authService.refreshToken(oldRefreshToken)
                            if (result is ru.jarvis.telegramka.data.remote.api.AuthResult.Success) {
                                tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                                BearerTokens(result.data.accessToken, result.data.refreshToken)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(): WebSocketHolder {
        val client = HttpClient(CIO) {
            install(WebSockets)
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
        return WebSocketHolder(client)
    }

    @Provides
    @Singleton
    fun provideChatService(@AuthenticatedClient client: HttpClient): ChatService {
        return ChatService(client)
    }

    @Provides
    @Singleton
    fun provideProfileService(@AuthenticatedClient client: HttpClient): ProfileService {
        return ProfileService(client)
    }
}
