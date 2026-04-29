package ru.jarvis.telegramka.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.domain.model.Chat
import ru.jarvis.telegramka.domain.model.User
import ru.jarvis.telegramka.data.remote.RealtimeChatManager
import ru.jarvis.telegramka.data.remote.RealtimeSnapshot
import ru.jarvis.telegramka.data.repository.ChatRepository
import ru.jarvis.telegramka.data.repository.ProfileRepository
import ru.jarvis.telegramka.data.repository.TokenRepository
import ru.jarvis.telegramka.data.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

sealed interface ChatsUiState {
    data class Success(val chats: List<Chat>, val currentUser: User) : ChatsUiState
    data class Error(val message: String) : ChatsUiState
    object Loading : ChatsUiState
}

sealed interface ChatsNavigationEvent {
    data class NavigateToChat(
        val chatId: String? = null,
        val userId: String? = null,
        val name: String,
        val nickname: String,
        val avatarUrl: String? = null
    ) : ChatsNavigationEvent
}

data class AppUpdateState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null
) {
    val isUpdateAvailable: Boolean
        get() = latestVersion != null && latestVersion != currentVersion
}

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val realtimeChatManager: RealtimeChatManager,
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

    private val _appUpdateState = MutableStateFlow(AppUpdateState(isChecking = true))
    val appUpdateState = _appUpdateState.asStateFlow()
    val realtimeSnapshot: StateFlow<RealtimeSnapshot> = realtimeChatManager.state

    init {
        loadData()
        checkAppVersion()
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
                                    chat.copy(
                                        lastMessage = message.text,
                                        lastMessageTime = message.timestamp
                                    )
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
        viewModelScope.launch {
            realtimeChatManager.state.collect { snapshot ->
                val currentState = _uiState.value as? ChatsUiState.Success ?: return@collect
                val updatedChats = currentState.chats.map { chat ->
                    chat.copy(unread = snapshot.unreadByChat[chat.id] ?: chat.unread)
                }.sortedByDescending { it.lastMessageTime }
                _uiState.value = currentState.copy(chats = updatedChats)
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

                val chats = chatsResult.getOrNull()
                val profile = profileResult.getOrNull()

                if (chats != null && profile != null) {
                    realtimeChatManager.setCurrentUserId(profile.id)
                    _uiState.value = ChatsUiState.Success(
                        chats.sortedByDescending { it.lastMessageTime },
                        profile
                    )
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
            tokenRepository.clearTokens()
            realtimeChatManager.disconnect()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun findUser(nickname: String) {
        val currentState = _uiState.value
        if (currentState is ChatsUiState.Success) {
            val enteredNickname = nickname.removePrefix("@")
            val ownNickname = currentState.currentUser.nickname
            if (ownNickname.equals(enteredNickname, ignoreCase = true)) {
                _searchUserError.value = "Да это же вы)"
                return
            }
            val chatAlreadyExists = currentState.chats.any {
                it.nickname.equals(enteredNickname, ignoreCase = true)
            }
            if (chatAlreadyExists) {
                _searchUserError.value = "Такой чат уже существует"
                return
            }
        }

        viewModelScope.launch {
            _isSearchingUser.value = true
            _searchUserError.value = null
            try {
                val cleanedNickname = nickname.removePrefix("@")
                val result = userRepository.findUserByNickname(cleanedNickname)

                result.fold(
                    onSuccess = { foundUser ->
                        _navigationEvent.value = ChatsNavigationEvent.NavigateToChat(
                            userId = foundUser.id,
                            name = foundUser.name,
                            nickname = foundUser.nickname,
                            avatarUrl = foundUser.avatarUrl
                        )
                    },
                    onFailure = {
                        Timber.w(it, "User not found for nickname: %s", cleanedNickname)
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

    fun checkAppVersion() {
        viewModelScope.launch {
            _appUpdateState.update {
                it.copy(isChecking = true, errorMessage = null)
            }
            val result = chatRepository.getLatestAppVersion()
            result.fold(
                onSuccess = { latestVersion ->
                    _appUpdateState.update {
                        it.copy(
                            latestVersion = latestVersion,
                            isChecking = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { exception ->
                    Timber.w(exception, "Failed to check app version")
                    _appUpdateState.update {
                        it.copy(
                            isChecking = false,
                            errorMessage = exception.message ?: "Не удалось проверить обновление"
                        )
                    }
                }
            )
        }
    }

    fun onAppUpdateDownloadStarted() {
        _appUpdateState.update {
            it.copy(isDownloading = true, errorMessage = null)
        }
    }

    fun onAppUpdateDownloadFinished() {
        _appUpdateState.update {
            it.copy(isDownloading = false)
        }
    }

    fun onAppUpdateDownloadFailed(message: String) {
        _appUpdateState.update {
            it.copy(isDownloading = false, errorMessage = message)
        }
    }

    fun consumeAppUpdateError() {
        _appUpdateState.update {
            it.copy(errorMessage = null)
        }
    }

    fun updateAvatar(avatar: ByteArray) {
        viewModelScope.launch {
            val result = profileRepository.updateAvatar(avatar)
            result.fold(
                onSuccess = { newAvatarUrl ->
                    val currentState = _uiState.value
                    if (currentState is ChatsUiState.Success) {
                        val updatedUser = currentState.currentUser.copy(avatarUrl = newAvatarUrl)
                        _uiState.value = currentState.copy(currentUser = updatedUser)
                    }
                },
                onFailure = {
                    Timber.e(it, "Failed to update avatar")
                    // Optionally, expose an error state to the UI
                }
            )
        }
    }
}
