package ru.jarvis.telegramka.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import ru.jarvis.telegramka.data.storage.ITokenManager
import javax.inject.Inject

enum class SessionState {
    Loading,
    Authorized,
    Unauthorized
}

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    tokenManager: ITokenManager
) : ViewModel() {

    private val _sessionState = MutableStateFlow(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSession = combine(
                tokenManager.getAccessToken(),
                tokenManager.getRefreshToken()
            ) { accessToken, refreshToken ->
                !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
            }.first()

            _sessionState.value = if (hasSession) {
                SessionState.Authorized
            } else {
                SessionState.Unauthorized
            }
        }
    }
}
