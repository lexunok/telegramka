package ru.jarvis.telegramka.ui.login

import androidx.lifecycle.ViewModel
import ru.jarvis.telegramka.data.MockData

class LoginViewModel : ViewModel() {
    fun userExists(phone: String): Boolean {
        // In a real app, this would be a network call to your backend
        return MockData.chats.any { it.phone == phone }
    }
}