package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.mapper.toDomain
import ru.jarvis.telegramka.data.remote.api.UserService
import ru.jarvis.telegramka.domain.model.User
import timber.log.Timber
import javax.inject.Inject

class UserRepository @Inject constructor(private val userService: UserService) {

    suspend fun findUserByNickname(nickname: String): Result<User> {
        return try {
            val userDto = userService.findUserByNickname(nickname)
            Result.success(userDto.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to find user by nickname: %s", nickname)
            Result.failure(e)
        }
    }
}
