package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.remote.api.AuthResult
import ru.jarvis.telegramka.data.remote.api.AuthService

sealed class LoginResult {
    object Success : LoginResult()
    object UserNotFound : LoginResult()
    data class Error(val message: String) : LoginResult()
    object NetworkError : LoginResult()
}

sealed class RegisterResult {
    object Success : RegisterResult()
    object Conflict : RegisterResult() // New state for 409 Conflict
    data class Error(val message: String) : RegisterResult()
    object NetworkError : RegisterResult()
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

    suspend fun register(name: String, email: String, nickname: String): RegisterResult {
        return when (val result = authService.register(name, email, nickname)) {
            is AuthResult.Success -> RegisterResult.Success
            is AuthResult.Conflict -> RegisterResult.Conflict
            is AuthResult.Error -> RegisterResult.Error(result.message)
            is AuthResult.NetworkError -> RegisterResult.NetworkError
            else -> RegisterResult.Error("Неизвестная ошибка регистрации") // Should not happen with current AuthService
        }
    }
}
