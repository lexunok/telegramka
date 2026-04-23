package ru.jarvis.telegramka.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.repository.AuthRepository
import ru.jarvis.telegramka.data.repository.LoginResult
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableStateFlow<LoginNavigationEvent?>(null)
    val navigationEvent: StateFlow<LoginNavigationEvent?> = _navigationEvent.asStateFlow()

    fun onLoginClicked(email: String) {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            try {
                when (val result = authRepository.login(email)) {
                    is LoginResult.Success -> {
                        _navigationEvent.value = LoginNavigationEvent.NavigateToVerifyCode(email)
                    }
                    is LoginResult.UserNotFound -> {
                        _navigationEvent.value = LoginNavigationEvent.NavigateToRegister(email)
                    }
                    is LoginResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is LoginResult.NetworkError -> {
                        // TODO: Use string resources
                        _errorMessage.value = "Ошибка сети. Проверьте подключение к интернету."
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to login")
                // TODO: Use string resources
                _errorMessage.value = "Произошла непредвиденная ошибка"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}

sealed class LoginNavigationEvent {
    data class NavigateToVerifyCode(val email: String) : LoginNavigationEvent()
    data class NavigateToRegister(val email: String) : LoginNavigationEvent()
}
