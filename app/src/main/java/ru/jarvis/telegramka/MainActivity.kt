package ru.jarvis.telegramka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.jarvis.telegramka.navigation.AppNavigation
import ru.jarvis.telegramka.ui.theme.TelegramkaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelegramkaTheme {
                AppNavigation()
            }
        }
    }
}