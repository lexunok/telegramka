package ru.jarvis.telegramka.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register/{email}") {
        fun createRoute(email: String) = "register/${Uri.encode(email)}"
    }
    object Verify : Screen("verify/{email}") {
        fun createRoute(email: String) = "verify/${Uri.encode(email)}"
    }
    object Chats : Screen("chats")
    object Chat : Screen("chat/{id}?name={name}&nickname={nickname}&currentUserId={currentUserId}&avatarUrl={avatarUrl}") {
        fun createRoute(id: String, name: String, nickname: String, currentUserId: String, avatarUrl: String?): String {
            val encodedName = Uri.encode(name)
            val encodedNickname = Uri.encode(nickname)
            val encodedAvatarUrl = avatarUrl?.let { Uri.encode(it) } ?: "null"
            return "chat/$id?name=$encodedName&nickname=$encodedNickname&currentUserId=$currentUserId&avatarUrl=$encodedAvatarUrl"
        }
    }
}
