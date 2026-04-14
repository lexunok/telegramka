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
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}
