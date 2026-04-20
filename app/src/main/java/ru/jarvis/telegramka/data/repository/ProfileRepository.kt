package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.User
import ru.jarvis.telegramka.data.remote.api.ProfileService
import ru.jarvis.telegramka.data.remote.model.UserDto

class ProfileRepository @Inject constructor(private val profileService: ProfileService) {

    suspend fun getProfile(): Result<User> {
        return try {
            val userDto = profileService.getProfile()
            Result.success(userDto.toUser())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun UserDto.toUser(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            nickname = this.nickname
        )
    }
}
