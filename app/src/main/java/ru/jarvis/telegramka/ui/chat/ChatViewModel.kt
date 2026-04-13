package ru.jarvis.telegramka.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.Chat
import ru.jarvis.telegramka.data.Message
import ru.jarvis.telegramka.data.MockData

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat = _chat.asStateFlow()

    fun loadChat(chatId: String) {
        viewModelScope.launch {
            _chat.value = MockData.chats.find { it.id == chatId }
            _messages.value = MockData.messages.filter { it.chatId == chatId }
        }
    }

    fun sendMessage(text: String) {
        val chatId = chat.value?.id ?: return
        val newMessage = Message(
            id = (messages.value.size + 1).toString(),
            chatId = chatId,
            senderId = MockData.currentUser.id,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + newMessage
    }
}