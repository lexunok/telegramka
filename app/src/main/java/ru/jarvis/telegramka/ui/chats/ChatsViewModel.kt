package ru.jarvis.telegramka.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.Chat
import ru.jarvis.telegramka.data.User
import ru.jarvis.telegramka.data.remote.api.ChatService
import ru.jarvis.telegramka.data.remote.api.ProfileService
import ru.jarvis.telegramka.data.repository.ChatRepository
import ru.jarvis.telegramka.data.repository.ProfileRepository

sealed interface ChatsUiState {
    data class Success(val chats: List<Chat>, val currentUser: User) : ChatsUiState
    data class Error(val message: String) : ChatsUiState
    object Loading : ChatsUiState
}

class ChatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val chatRepository: ChatRepository
    private val profileRepository: ProfileRepository

    init {
        // This is not ideal, a proper DI framework should be used.
        val chatService = ChatService()
        chatRepository = ChatRepository(chatService)
        val profileService = ProfileService()
        profileRepository = ProfileRepository(profileService)
        
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ChatsUiState.Loading
            
            val chatsDeferred = async { chatRepository.getChats() }
            val profileDeferred = async { profileRepository.getProfile() }

            val chatsResult = chatsDeferred.await()
            val profileResult = profileDeferred.await()

            val chats = chatsResult.getOrNull()
            val profile = profileResult.getOrNull()

            if (chats != null && profile != null) {
                _uiState.value = ChatsUiState.Success(chats, profile)
            } else {
                val errorMessage = chatsResult.exceptionOrNull()?.message 
                    ?: profileResult.exceptionOrNull()?.message 
                    ?: "Unknown error"
                _uiState.value = ChatsUiState.Error(errorMessage)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
