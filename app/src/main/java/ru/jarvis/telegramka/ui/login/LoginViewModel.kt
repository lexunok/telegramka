package ru.jarvis.telegramka.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import ru.jarvis.telegramka.data.MockData

class LoginViewModel : ViewModel() {
    fun userExists(email: String): Boolean {
        return MockData.allUsers.any { it.email.equals(email, ignoreCase = true) }
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
