package ru.jarvis.telegramka.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val nickname: String
)
