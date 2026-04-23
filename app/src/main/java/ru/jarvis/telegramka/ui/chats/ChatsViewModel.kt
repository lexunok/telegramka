package ru.jarvis.telegramka.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.domain.model.Chat
import ru.jarvis.telegramka.domain.model.User
import ru.jarvis.telegramka.data.remote.RealtimeChatManager
import ru.jarvis.telegramka.data.remote.model.UserDto
import ru.jarvis.telegramka.data.repository.ChatRepository
import ru.jarvis.telegramka.data.repository.ProfileRepository
import ru.jarvis.telegramka.data.repository.UserRepository
import ru.jarvis.telegramka.data.storage.ITokenManager
import timber.log.Timber
import javax.inject.Inject

sealed interface ChatsUiState {
    data class Success(val chats: List<Chat>, val currentUser: User) : ChatsUiState
    data class Error(val message: String) : ChatsUiState
    object Loading : ChatsUiState
}

sealed interface ChatsNavigationEvent {
    data class NavigateToChat(val id: String, val name: String, val nickname: String) : ChatsNavigationEvent
}

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val realtimeChatManager: RealtimeChatManager,
    private val tokenManager: ITokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchingUser = MutableStateFlow(false)
    val isSearchingUser = _isSearchingUser.asStateFlow()

    private val _searchUserError = MutableStateFlow<String?>(null)
    val searchUserError = _searchUserError.asStateFlow()

    private val _navigationEvent = MutableStateFlow<ChatsNavigationEvent?>(null)
    val navigationEvent = _navigationEvent.asStateFlow()

    init {
        loadData()
        realtimeChatManager.connect()
        viewModelScope.launch {
            realtimeChatManager.incomingMessages.collect { message ->
                try {
                    val currentState = _uiState.value
                    if (currentState is ChatsUiState.Success) {
                        val chatExists = currentState.chats.any { it.id == message.chatId }
                        if (chatExists) {
                            val updatedChats = currentState.chats.map { chat ->
                                if (chat.id == message.chatId) {
                                    chat.copy(lastMessage = message.text, lastMessageTime = message.timestamp)
                                } else {
                                    chat
                                }
                            }.sortedByDescending { it.lastMessageTime }
                            _uiState.value = currentState.copy(chats = updatedChats)
                        } else {
                            loadData()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process incoming message")
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ChatsUiState.Loading
            try {
                val chatsDeferred = async { chatRepository.getChats() }
                val profileDeferred = async { profileRepository.getProfile() }

                val chatsResult = chatsDeferred.await()
                val profileResult = profileDeferred.await()

                val chats = chatsResult.getOrNull()?.sortedByDescending { it.lastMessageTime }
                val profile = profileResult.getOrNull()

                if (chats != null && profile != null) {
                    _uiState.value = ChatsUiState.Success(chats, profile)
                } else {
                    val errorMessage = chatsResult.exceptionOrNull()?.message
                        ?: profileResult.exceptionOrNull()?.message
                        ?: "Unknown error"
                    _uiState.value = ChatsUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load data")
                _uiState.value = ChatsUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            realtimeChatManager.disconnect()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun findUser(nickname: String) {
        viewModelScope.launch {
            _isSearchingUser.value = true
            _searchUserError.value = null
            try {
                val cleanedNickname = nickname.removePrefix("@")
                val result = userRepository.findUserByNickname(cleanedNickname)

                result.fold(
                    onSuccess = { foundUser ->
                        _navigationEvent.value = ChatsNavigationEvent.NavigateToChat(foundUser.id, foundUser.name, foundUser.nickname)
                    },
                    onFailure = {
                        Timber.w(it, "User not found for nickname: %s", cleanedNickname)
                        // TODO: Use string resources
                        _searchUserError.value = "Пользователь не найден"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to find user")
                _searchUserError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isSearchingUser.value = false
            }
        }
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }

    fun clearSearchError() {
        _searchUserError.value = null
    }
}
