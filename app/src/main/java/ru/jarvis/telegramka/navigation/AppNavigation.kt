package ru.jarvis.telegramka.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import ru.jarvis.telegramka.ui.verify.VerifyCodeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(180)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(180)
            )
        }
    ) {
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
        composable(
            route = Screen.Verify.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("email")?.let { email ->
                VerifyCodeScreen(navController, email)
            }
        }
        composable(
            route = Screen.Chats.route,
            exitTransition = { null },
            popEnterTransition = { null }
        ) {
            ChatsScreen(navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; nullable = true },
                navArgument("nickname") { type = NavType.StringType; nullable = true },
                navArgument("currentUserId") { type = NavType.StringType; nullable = true }
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val name = backStackEntry.arguments?.getString("name")
            val nickname = backStackEntry.arguments?.getString("nickname")
            val currentUserId = backStackEntry.arguments?.getString("currentUserId")
            if (id != null && name != null && nickname != null && currentUserId != null) {
                ChatScreen(navController, id, name, nickname, currentUserId)
            }
        }
    }
}
