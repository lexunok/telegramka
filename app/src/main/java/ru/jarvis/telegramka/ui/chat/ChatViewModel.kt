package ru.jarvis.telegramka.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.domain.model.Message
import ru.jarvis.telegramka.data.remote.RealtimeChatManager
import ru.jarvis.telegramka.data.repository.ChatRepository
import timber.log.Timber
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

    // TODO: Use SavedStateHandle to receive navigation arguments instead of this manual method.
    // This will make the ViewModel more robust and survive process death correctly.
    fun initialize(id: String, currentUserId: String) {
        this.chatId = id
        this.currentUserId = currentUserId
        loadMessages(id)

        viewModelScope.launch {
            realtimeChatManager.incomingMessages.collect { message ->
                try {
                    if (message.chatId == id) {
                        // Add the message only if it's not already in the list
                        if (_messages.value.none { it.id == message.id }) {
                            _messages.value += message
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process incoming message")
                    // Log the error, but don't crash the collector
                    _errorMessage.value = "Error processing incoming message."
                }
            }
        }
    }

    private fun loadMessages(id: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.getMessages(id)
                result.fold(
                    onSuccess = { _messages.value = it },
                    onFailure = {
                        Timber.w(it, "Failed to load messages for chat id: %s", id)
                        _errorMessage.value = it.message ?: "Failed to load messages"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load messages for chat id: %s", id)
                _errorMessage.value = e.message ?: "An unexpected error occurred"
            }
        }
    }

    fun sendMessage(text: String) {
        val idToSend = chatId ?: return

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(idToSend, text)
                result.fold(
                    onSuccess = { sentMessage ->
                        if (idToSend != sentMessage.chatId) {
                            chatId = sentMessage.chatId
                        }
                    },
                    onFailure = { exception ->
                        Timber.w(exception, "Failed to send message to id: %s", idToSend)
                        _errorMessage.value = "Ошибка отправки сообщения: ${exception.message ?: "Неизвестная ошибка"}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to send message to id: %s", idToSend)
                _errorMessage.value = "Ошибка отправки сообщения: ${e.message ?: "Неизвестная ошибка"}"
            }
        }
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}