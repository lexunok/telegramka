package ru.jarvis.telegramka.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.jarvis.telegramka.data.remote.RealtimeChatManager
import ru.jarvis.telegramka.data.repository.ChatRepository
import ru.jarvis.telegramka.domain.model.Message
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

sealed interface ChatUiState {
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
    object Loading : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val realtimeChatManager: RealtimeChatManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)
    private var currentUserId: String? = null
    private var messageCollectorJob: Job? = null

    fun initialize(id: String, currentUserId: String) {
        if (_chatId.value == id && this.currentUserId == currentUserId) return

        _chatId.value = id
        this.currentUserId = currentUserId
        loadMessages(id)

        messageCollectorJob?.cancel()
        messageCollectorJob = viewModelScope.launch {
            realtimeChatManager.incomingMessages.collect { message ->
                try {
                    if (message.chatId == _chatId.value) {
                        val currentState = _uiState.value
                        if (currentState is ChatUiState.Success) {
                            val updatedMessages = currentState.messages.reconcileIncomingMessage(message)
                            if (updatedMessages !== currentState.messages) {
                                _uiState.value = currentState.copy(messages = updatedMessages)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process incoming message")
                }
            }
        }
    }

    private fun loadMessages(id: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                val result = chatRepository.getMessages(id)
                result.fold(
                    onSuccess = { _uiState.value = ChatUiState.Success(it) },
                    onFailure = {
                        Timber.w(it, "Failed to load messages for chat id: %s", id)
                        _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load messages for chat id: %s", id)
                _uiState.value = ChatUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun sendMessage(text: String) {
        val idToSend = _chatId.value ?: return
        val senderId = currentUserId ?: return
        val messageId = UUID.randomUUID().toString()
        val optimisticMessage = Message(
            id = messageId,
            chatId = idToSend,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isPending = true,
            isFailed = false
        )

        appendOptimisticMessage(optimisticMessage)

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(idToSend, messageId, text)
                result.fold(
                    onSuccess = { sentMessage ->
                        replaceMessage(optimisticMessage.id, sentMessage)
                        if (idToSend != sentMessage.chatId) {
                            _chatId.value = sentMessage.chatId
                        }
                    },
                    onFailure = { exception ->
                        markMessageAsFailed(optimisticMessage.id)
                        Timber.w(exception, "Failed to send message to id: %s", idToSend)
                    }
                )
            } catch (e: Exception) {
                markMessageAsFailed(optimisticMessage.id)
                Timber.e(e, "Failed to send message to id: %s", idToSend)
            }
        }
    }

    fun consumeErrorMessage() {
        val currentState = _uiState.value
        if (currentState is ChatUiState.Error) {
            _uiState.value = ChatUiState.Loading
            loadMessages(_chatId.value ?: return)
        }
    }

    private fun appendOptimisticMessage(message: Message) {
        val currentState = _uiState.value
        if (currentState is ChatUiState.Success) {
            _uiState.value = currentState.copy(messages = currentState.messages + message)
        }
    }

    private fun replaceMessage(targetId: String, replacement: Message) {
        val currentState = _uiState.value
        if (currentState is ChatUiState.Success) {
            _uiState.value = currentState.copy(
                messages = currentState.messages.map { message ->
                    if (message.id == targetId) replacement else message
                }
            )
        }
    }

    private fun markMessageAsFailed(targetId: String) {
        val currentState = _uiState.value
        if (currentState is ChatUiState.Success) {
            _uiState.value = currentState.copy(
                messages = currentState.messages.map { message ->
                    if (message.id == targetId) {
                        message.copy(isPending = false, isFailed = true)
                    } else {
                        message
                    }
                }
            )
        }
    }
}

private fun List<Message>.reconcileIncomingMessage(incomingMessage: Message): List<Message> {
    val existingIndex = indexOfFirst { it.id == incomingMessage.id }
    if (existingIndex >= 0) {
        return toMutableList().also { it[existingIndex] = incomingMessage }
    }

    return this + incomingMessage
}
