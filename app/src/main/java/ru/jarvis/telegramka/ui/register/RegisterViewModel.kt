package ru.jarvis.telegramka.ui.register

import androidx.lifecycle.ViewModel
import ru.jarvis.telegramka.data.MockData
import ru.jarvis.telegramka.data.User

class RegisterViewModel : ViewModel() {
    fun registerUser(name: String, email: String, nickname: String) {
        // In a real app, this would be a network call to create a new user.
        // For mock purposes, we can't easily modify the currentUser singleton from MockData.
        // We will just assume the navigation to ChatsScreen implies a successful "login"
        // with the new user details. The mock data source is not designed to be mutable here.
        println("Registering user: $name, $email, $nickname")
    }
}
