package ru.jarvis.telegramka.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val nickname: String,
    val avatar_url: String?,
    val created_at: String
)
