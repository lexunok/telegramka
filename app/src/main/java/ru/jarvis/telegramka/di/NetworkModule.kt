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
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ru.jarvis.telegramka.data.remote.api.AuthService
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.api.ProfileService
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
    @AuthenticatedClient
    fun provideAuthenticatedHttpClient(
        json: Json,
        authService: AuthService
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
                        val accessToken = runBlocking { TokenManager.getAccessToken().first() }
                        val refreshToken = runBlocking { TokenManager.getRefreshToken().first() }
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }
                    refreshTokens {
                        val oldRefreshToken = runBlocking { TokenManager.getRefreshToken().first() }
                        if (oldRefreshToken != null) {
                            val result = runBlocking { authService.refreshToken(oldRefreshToken) }
                            if (result is ru.jarvis.telegramka.data.remote.api.AuthResult.Success) {
                                TokenManager.saveTokens(result.data.access_token, result.data.refresh_token)
                                BearerTokens(result.data.access_token, result.data.refresh_token)
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
    fun provideChatService(@AuthenticatedClient client: HttpClient): ChatService {
        return ChatService(client)
    }

    @Provides
    @Singleton
    fun provideProfileService(@AuthenticatedClient client: HttpClient): ProfileService {
        return ProfileService(client)
    }
}
