package ru.jarvis.telegramka.data.mapper

import ru.jarvis.telegramka.data.remote.model.ChatDto
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.remote.model.UserDto
import ru.jarvis.telegramka.domain.model.Chat
import ru.jarvis.telegramka.domain.model.Message
import ru.jarvis.telegramka.domain.model.User
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun MessageDto.toDomain(): Message {
    return Message(
        id = this.id,
        chatId = this.chatId,
        senderId = this.senderId,
        text = this.text,
        timestamp = parseRfc3339(this.createdAt),
        isPending = false,
        isFailed = false
    )
}

fun ChatDto.toDomain(): Chat {
    return Chat(
        id = this.id,
        name = this.name,
        nickname = this.nickname,
        lastMessage = this.lastMessage,
        lastMessageTime = this.lastMessageTime?.let { parseRfc3339(it) },
        unread = this.unread,
        avatarUrl = this.avatarUrl
    )
}

fun UserDto.toDomain(): User {
    return User(
        id = this.id,
        name = this.name,
        email = this.email,
        nickname = this.nickname,
        avatarUrl = this.avatarUrl
    )
}

private fun parseRfc3339(timestamp: String): Long {
    return try {
        OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        Timber.e(e, "Failed to parse timestamp: %s", timestamp)
        0L
    }
}
