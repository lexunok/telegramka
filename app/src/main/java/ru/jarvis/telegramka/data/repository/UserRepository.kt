package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.remote.api.UserService
import ru.jarvis.telegramka.data.remote.model.UserDto
import javax.inject.Inject

class UserRepository @Inject constructor(private val userService: UserService) {

    suspend fun findUserByNickname(nickname: String): Result<UserDto> {
        return try {
            val user = userService.findUserByNickname(nickname)
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
