package ru.jarvis.telegramka.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.repository.AuthRepository
import ru.jarvis.telegramka.data.repository.RegisterResult
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableStateFlow<RegisterNavigationEvent?>(null)
    val navigationEvent: StateFlow<RegisterNavigationEvent?> = _navigationEvent.asStateFlow()

    fun registerUser(name: String, email: String, nickname: String) {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            try {
                when (val result = authRepository.register(name, email, nickname)) {
                    is RegisterResult.Success -> {
                        _navigationEvent.value = RegisterNavigationEvent.NavigateToVerifyCode(email)
                    }
                    is RegisterResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is RegisterResult.NetworkError -> {
                        // TODO: Use string resources
                        _errorMessage.value = "Ошибка сети. Проверьте подключение к интернету."
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to register user")
                // TODO: Use string resources
                _errorMessage.value = "Произошла непредвиденная ошибка"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}

sealed class RegisterNavigationEvent {
    data class NavigateToVerifyCode(val email: String) : RegisterNavigationEvent()
}
