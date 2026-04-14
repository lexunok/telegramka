package ru.jarvis.telegramka.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

private val NavigationEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val NavigationDuration = 280
private const val NavigationFadeDuration = 180
private const val BackNavigationDuration = 180

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = NavigationDuration, easing = NavigationEasing),
                initialOffset = { fullWidth -> fullWidth / 6 }
            ) + fadeIn(
                animationSpec = tween(durationMillis = NavigationFadeDuration, easing = NavigationEasing),
                initialAlpha = 0.92f
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(durationMillis = NavigationFadeDuration, easing = NavigationEasing),
                targetAlpha = 0.98f
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = BackNavigationDuration, easing = NavigationEasing),
                targetOffset = { fullWidth -> fullWidth / 5 }
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
            route = Screen.Chats.route,
            exitTransition = { null },
            popEnterTransition = { null }
        ) {
            ChatsScreen(navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = 240, easing = NavigationEasing),
                    initialOffset = { fullWidth -> fullWidth / 5 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 160, easing = NavigationEasing),
                    initialAlpha = 0.96f
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = BackNavigationDuration, easing = NavigationEasing),
                    targetOffset = { fullWidth -> fullWidth / 5 }
                )
            }
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("chatId")?.let { chatId ->
                ChatScreen(navController, chatId)
            }
        }
    }
}
