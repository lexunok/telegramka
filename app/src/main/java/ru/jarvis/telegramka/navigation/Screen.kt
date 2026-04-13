package ru.jarvis.telegramka.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register/{phone}") {
        fun createRoute(phone: String) = "register/$phone"
    }
    object Chats : Screen("chats")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}