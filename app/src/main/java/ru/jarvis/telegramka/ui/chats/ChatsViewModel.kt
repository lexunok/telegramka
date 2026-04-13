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

    fun onAddChat(name: String, phone: String) {
        val newChat = Chat(
            id = (chats.value.size + 1).toString(),
            name = name,
            phone = phone,
            avatarUrl = "https://i.pravatar.cc/150?u=$name"
        )
        _chats.value = _chats.value + newChat
    }
}
