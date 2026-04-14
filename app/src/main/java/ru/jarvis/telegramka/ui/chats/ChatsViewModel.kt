package ru.jarvis.telegramka.ui.chats

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.jarvis.telegramka.data.Chat
import ru.jarvis.telegramka.data.MockData

class ChatsViewModel : ViewModel() {
    private val _chats = MutableStateFlow(MockData.chats)
    val chats = _chats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAddChat(nickname: String) : Boolean {
        val userToAdd = MockData.allUsers.find { it.nickname.equals(nickname, ignoreCase = true) }
        if (userToAdd != null) {
            val chatExists = _chats.value.any { it.nickname.equals(nickname, ignoreCase = true) }
            if (!chatExists) {
                val newChat = Chat(
                    id = userToAdd.id,
                    name = userToAdd.name,
                    nickname = userToAdd.nickname,
                    avatarUrl = "https://i.pravatar.cc/150?u=${userToAdd.name}"
                )
                _chats.value = _chats.value + newChat
                return true
            }
        }
        return false
    }
}
