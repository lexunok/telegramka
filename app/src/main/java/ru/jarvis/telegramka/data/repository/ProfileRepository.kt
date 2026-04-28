package ru.jarvis.telegramka.data.repository

import ru.jarvis.telegramka.data.mapper.toDomain
import ru.jarvis.telegramka.data.remote.api.ProfileService
import ru.jarvis.telegramka.domain.model.User
import timber.log.Timber
import javax.inject.Inject


class ProfileRepository @Inject constructor(private val profileService: ProfileService) {

    suspend fun getProfile(): Result<User> {
        return try {
            val userDto = profileService.getProfile()
            Result.success(userDto.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to get profile")
            Result.failure(e)
        }
    }

    suspend fun updateAvatar(avatar: ByteArray): Result<String> {
        return try {
            val response = profileService.updateAvatar(avatar)
            Result.success(response.path)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update avatar")
            Result.failure(e)
        }
    }
}
