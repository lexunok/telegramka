package ru.jarvis.telegramka.ui.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.remote.api.AuthService
import ru.jarvis.telegramka.data.repository.AuthRepository
import ru.jarvis.telegramka.data.repository.VerifyCodeResult

class VerifyCodeViewModel : ViewModel() {

    // TODO: Use a proper DI framework for AuthRepository injection
    private val authService = AuthService()
    private val authRepository = AuthRepository(authService)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableStateFlow<VerifyCodeNavigationEvent?>(null)
    val navigationEvent: StateFlow<VerifyCodeNavigationEvent?> = _navigationEvent.asStateFlow()

    fun verifyCode(email: String, code: String) {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            when (val result = authRepository.verifyCode(email, code)) {
                is VerifyCodeResult.Success -> {
                    _navigationEvent.value = VerifyCodeNavigationEvent.NavigateToChats
                }
                is VerifyCodeResult.Error -> {
                    _errorMessage.value = result.message
                }
                is VerifyCodeResult.NetworkError -> {
                    _errorMessage.value = "Ошибка сети. Проверьте подключение к интернету."
                }
            }
            _isLoading.value = false
        }
    }

    fun isCodeValid(code: String): Boolean {
        return code.length == 6 && code.all(Char::isDigit)
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}

sealed class VerifyCodeNavigationEvent {
    object NavigateToChats : VerifyCodeNavigationEvent()
}
