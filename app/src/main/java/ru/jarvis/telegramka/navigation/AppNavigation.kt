package ru.jarvis.telegramka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.jarvis.telegramka.ui.chat.ChatScreen
import ru.jarvis.telegramka.ui.chats.ChatsScreen
import ru.jarvis.telegramka.ui.login.LoginScreen
import ru.jarvis.telegramka.ui.register.RegisterScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(
            route = Screen.Register.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("email")?.let { email ->
                RegisterScreen(navController, email)
            }
        }
        composable(Screen.Chats.route) {
            ChatsScreen(navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("chatId")?.let { chatId ->
                ChatScreen(navController, chatId)
            }
        }
    }
}
