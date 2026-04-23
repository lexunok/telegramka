package ru.jarvis.telegramka.data.repository

import io.ktor.client.plugins.auth.providers.BearerTokens
import ru.jarvis.telegramka.data.mapper.toDomain
import ru.jarvis.telegramka.data.remote.api.AuthResult
import ru.jarvis.telegramka.data.remote.api.AuthService
import ru.jarvis.telegramka.data.storage.ITokenManager
import ru.jarvis.telegramka.domain.model.User
import javax.inject.Inject

sealed class LoginResult {
    object Success : LoginResult()
    object UserNotFound : LoginResult()
    data class Error(val message: String) : LoginResult()
    object NetworkError : LoginResult()
}

sealed class RegisterResult {
    object Success : RegisterResult()
    data class Error(val message: String) : RegisterResult()
    object NetworkError : RegisterResult()
}

sealed class VerifyCodeResult {
    data class Success(val user: User) : VerifyCodeResult()
    data class Error(val message: String) : VerifyCodeResult()
    object NetworkError : VerifyCodeResult()
}

sealed class RefreshResult {
    data class Success(val tokens: BearerTokens) : RefreshResult()
    data class Error(val message: String) : RefreshResult()
    object NetworkError : RefreshResult()
}

class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val tokenManager: ITokenManager
) {

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
            is AuthResult.Error -> RegisterResult.Error(result.message)
            is AuthResult.NetworkError -> RegisterResult.NetworkError
            else -> RegisterResult.Error("Неизвестная ошибка регистрации")
        }
    }

    suspend fun verifyCode(email: String, code: String): VerifyCodeResult {
        return when (val result = authService.verifyCode(email, code)) {
            is AuthResult.Success -> {
                result.data.user?.let { userDto ->
                    tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    VerifyCodeResult.Success(userDto.toDomain())
                } ?: VerifyCodeResult.Error("Пользовательские данные отсутствуют в ответе")
            }
            is AuthResult.Error -> VerifyCodeResult.Error(result.message)
            is AuthResult.NetworkError -> VerifyCodeResult.NetworkError
            else -> VerifyCodeResult.Error("Неизвестная ошибка верификации кода")
        }
    }

    suspend fun refreshToken(oldRefreshToken: String): RefreshResult {
        return when (val result = authService.refreshToken(oldRefreshToken)) {
            is AuthResult.Success -> {
                val newAccessToken = result.data.accessToken
                val newRefreshToken = result.data.refreshToken
                tokenManager.saveTokens(newAccessToken, newRefreshToken)
                RefreshResult.Success(BearerTokens(newAccessToken, newRefreshToken))
            }
            is AuthResult.Error -> RefreshResult.Error(result.message)
            is AuthResult.NetworkError -> RefreshResult.NetworkError
            else -> RefreshResult.Error("Неизвестная ошибка обновления токена")
        }
    }
}
