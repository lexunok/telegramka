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
    data class Success(
        val messages: List<Message>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
    object Loading : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val realtimeChatManager: RealtimeChatManager
) : ViewModel() {
    private companion object {
        const val PAGE_SIZE = 500
    }

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)
    private var targetUserId: String? = null
    private var currentUserId: String? = null
    private var messageCollectorJob: Job? = null
    private var isLoadingMoreMessages = false

    fun initialize(chatId: String?, userId: String?, currentUserId: String) {
        if (_chatId.value == chatId && targetUserId == userId && this.currentUserId == currentUserId) return

        _chatId.value = chatId
        targetUserId = userId
        this.currentUserId = currentUserId
        isLoadingMoreMessages = false
        if (chatId != null) {
            loadInitialMessages(chatId)
        } else {
            _uiState.value = ChatUiState.Success(
                messages = emptyList(),
                isLoadingMore = false,
                hasMore = false
            )
        }

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

    private fun loadInitialMessages(id: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                val result = chatRepository.getMessages(id, limit = PAGE_SIZE)
                result.fold(
                    onSuccess = {
                        _uiState.value = ChatUiState.Success(
                            messages = it,
                            isLoadingMore = false,
                            hasMore = it.size >= PAGE_SIZE
                        )
                    },
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

    fun loadOlderMessages() {
        val id = _chatId.value ?: return
        val currentState = _uiState.value as? ChatUiState.Success ?: return
        if (currentState.isLoadingMore || !currentState.hasMore || isLoadingMoreMessages) return

        val oldestTimestamp = currentState.messages.firstOrNull()?.timestamp ?: return
        isLoadingMoreMessages = true
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val result = chatRepository.getMessages(
                    chatId = id,
                    before = oldestTimestamp,
                    limit = PAGE_SIZE
                )
                result.fold(
                    onSuccess = { olderMessages ->
                        val latestState = _uiState.value as? ChatUiState.Success
                        if (latestState != null) {
                            val mergedMessages = olderMessages.mergeOlderMessages(latestState.messages)
                            _uiState.value = latestState.copy(
                                messages = mergedMessages,
                                isLoadingMore = false,
                                hasMore = olderMessages.size >= PAGE_SIZE
                            )
                        }
                    },
                    onFailure = { exception ->
                        val latestState = _uiState.value as? ChatUiState.Success
                        if (latestState != null) {
                            _uiState.value = latestState.copy(isLoadingMore = false)
                        }
                        Timber.w(exception, "Failed to load older messages for chat id: %s", id)
                    }
                )
            } catch (e: Exception) {
                val latestState = _uiState.value as? ChatUiState.Success
                if (latestState != null) {
                    _uiState.value = latestState.copy(isLoadingMore = false)
                }
                Timber.e(e, "Failed to load older messages for chat id: %s", id)
            } finally {
                isLoadingMoreMessages = false
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = _chatId.value
        val userId = targetUserId
        if (chatId == null && userId == null) return
        val senderId = currentUserId ?: return
        val messageId = UUID.randomUUID().toString()
        val optimisticMessage = Message(
            id = messageId,
            chatId = chatId ?: userId.orEmpty(),
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isPending = true,
            isFailed = false
        )

        appendOptimisticMessage(optimisticMessage)

        viewModelScope.launch {
            try {
                val result = chatRepository.sendMessage(chatId, userId, messageId, text)
                result.fold(
                    onSuccess = { sentMessage ->
                        replaceMessage(optimisticMessage.id, sentMessage)
                        if (chatId != sentMessage.chatId) {
                            _chatId.value = sentMessage.chatId
                        }
                        targetUserId = null
                    },
                    onFailure = { exception ->
                        markMessageAsFailed(optimisticMessage.id)
                        Timber.w(exception, "Failed to send message to chatId=%s userId=%s", chatId, userId)
                    }
                )
            } catch (e: Exception) {
                markMessageAsFailed(optimisticMessage.id)
                Timber.e(e, "Failed to send message to chatId=%s userId=%s", chatId, userId)
            }
        }
    }

    fun consumeErrorMessage() {
        val currentState = _uiState.value
        if (currentState is ChatUiState.Error) {
            _uiState.value = ChatUiState.Loading
            loadInitialMessages(_chatId.value ?: return)
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
        return toMutableList().also { it[existingIndex] = incomingMessage }.sortedBy { it.timestamp }
    }

    return (this + incomingMessage).sortedBy { it.timestamp }
}

private fun List<Message>.mergeOlderMessages(currentMessages: List<Message>): List<Message> {
    if (isEmpty()) return currentMessages
    val existingIds = currentMessages.asSequence().map { it.id }.toHashSet()
    val uniqueOlderMessages = filterNot { it.id in existingIds }
    if (uniqueOlderMessages.isEmpty()) return currentMessages
    return (uniqueOlderMessages + currentMessages).sortedBy { it.timestamp }
}
