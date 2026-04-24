package ru.jarvis.telegramka.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.plugin
import ru.jarvis.telegramka.data.storage.ITokenManager
import ru.jarvis.telegramka.di.AuthClient
import javax.inject.Inject

class TokenRepository @Inject constructor(
    @AuthClient private val client: HttpClient,
    private val tokenManager: ITokenManager
) {
    suspend fun clearTokens() {
        tokenManager.clearTokens()
        client.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>().firstOrNull()?.clearToken()
    }
}
