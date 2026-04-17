package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.remote.api.AuthResult
import ru.jarvis.telegramka.data.remote.api.AuthService

sealed class LoginResult {
    object Success : LoginResult()
    object UserNotFound : LoginResult()
    data class Error(val message: String) : LoginResult()
    object NetworkError : LoginResult()
}

class AuthRepository(private val authService: AuthService) {

    suspend fun login(email: String): LoginResult {
        return when (val result = authService.login(email)) {
            is AuthResult.Success -> LoginResult.Success
            is AuthResult.UserNotFound -> LoginResult.UserNotFound
            is AuthResult.Error -> LoginResult.Error(result.message)
            is AuthResult.NetworkError -> LoginResult.NetworkError
        }
    }
}
