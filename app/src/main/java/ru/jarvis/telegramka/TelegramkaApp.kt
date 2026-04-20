package ru.jarvis.telegramka

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.jarvis.telegramka.data.storage.TokenManager

@HiltAndroidApp
class TelegramkaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.initialize(this)
    }
}
