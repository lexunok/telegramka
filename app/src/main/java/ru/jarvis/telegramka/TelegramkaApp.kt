package ru.jarvis.telegramka

import android.app.Application
import ru.jarvis.telegramka.data.storage.TokenManager

class TelegramkaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.initialize(this)
    }
}
