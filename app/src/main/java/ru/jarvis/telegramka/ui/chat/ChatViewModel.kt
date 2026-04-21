package ru.jarvis.telegramka.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.Message
import ru.jarvis.telegramka.data.remote.RealtimeChatManager
import ru.jarvis.telegramka.data.repository.ChatRepository
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val realtimeChatManager: RealtimeChatManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var chatId: String? = null
    private var currentUserId: String? = null

    fun initialize(id: String, currentUserId: String) {
        this.chatId = id
        this.currentUserId = currentUserId
        loadMessages(id)

        viewModelScope.launch {
            realtimeChatManager.incomingMessages.collect { message ->
                if (message.chatId == id) {
                    _messages.value = _messages.value + message
                }
            }
        }
    }

    private fun loadMessages(id: String) {
        viewModelScope.launch {
            chatRepository.getMessages(id).getOrNull()?.let {
                _messages.value = it
            }
        }
    }

    fun sendMessage(text: String) {
        val currentChatId = chatId ?: return

        viewModelScope.launch {
            val result = chatRepository.sendMessage(currentChatId, text)
            if (result.isFailure) {
                _errorMessage.value = "Ошибка отправки сообщения: ${result.exceptionOrNull()?.message ?: "Неизвестная ошибка"}"
            }
            // Message will be echoed by the websocket, so no need to manually add it here
        }
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}