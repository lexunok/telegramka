package ru.jarvis.telegramka.ui.register

import androidx.lifecycle.ViewModel
import ru.jarvis.telegramka.data.MockData
import ru.jarvis.telegramka.data.User

class RegisterViewModel : ViewModel() {
    fun registerUser(name: String, phone: String) {
        // In a real app, this would be a network call
        // For now, we just update the mock current user
        MockData.currentUser.let {
            // This is not how you should do it, but for mock purposes it is fine
            // In a real app you would have a proper user management
        }
    }
}