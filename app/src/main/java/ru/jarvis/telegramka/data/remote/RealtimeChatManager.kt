package ru.jarvis.telegramka.data.remote

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.storage.ITokenManager
import ru.jarvis.telegramka.di.WebSocketHolder
import ru.jarvis.telegramka.domain.model.Message
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class RealtimeSnapshot(
    val onlineUserIds: Set<String> = emptySet(),
    val typingByChat: Map<String, Set<String>> = emptyMap(),
    val messagesByChat: Map<String, List<Message>> = emptyMap(),
    val lastReadByChatUser: Map<String, Map<String, Long>> = emptyMap(),
    val unreadByChat: Map<String, Int> = emptyMap()
)

@Singleton
class RealtimeChatManager @Inject constructor(
    webSocketHolder: WebSocketHolder,
    private val tokenManager: ITokenManager,
    private val json: Json
) {
    private val client = webSocketHolder.client
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private val _state = MutableStateFlow(RealtimeSnapshot())
    val state: StateFlow<RealtimeSnapshot> = _state.asStateFlow()

    private val activeSession = MutableStateFlow<DefaultClientWebSocketSession?>(null)
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentUserId: String? = null
    private var activeChatId: String? = null

    fun connect() {
        if (connectionJob?.isActive == true) {
            Timber.d("Already connected or connecting.")
            return
        }
        connectionJob = scope.launch {
            while (isActive) {
                var session: DefaultClientWebSocketSession? = null
                try {
                    val token = tokenManager.getAccessToken().first()
                    if (token == null) {
                        Timber.e("Not authenticated, cannot connect.")
                        delay(10000)
                        continue
                    }

                    val webSocketBlock: suspend DefaultClientWebSocketSession.() -> Unit = {
                        session = this
                        activeSession.value = this
                        Timber.d("Connection established.")
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleIncomingFrame(frame.readText())
                            }
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        client.ws(
                            method = HttpMethod.Get,
                            host = BuildConfig.WS_HOST,
                            port = BuildConfig.WS_PORT,
                            path = "/ws",
                            request = {
                                header(HttpHeaders.Authorization, "Bearer $token")
                            },
                            block = webSocketBlock
                        )
                    } else {
                        client.wss(
                            method = HttpMethod.Get,
                            host = BuildConfig.WS_HOST,
                            port = BuildConfig.WS_PORT,
                            path = "/ws",
                            request = {
                                header(HttpHeaders.Authorization, "Bearer $token")
                            },
                            block = webSocketBlock
                        )
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Timber.w(e, "Connection closed. Reconnecting...")
                } catch (e: Exception) {
                    Timber.e(e, "Connection failed. Retrying in 5 seconds.")
                    delay(5000)
                } finally {
                    activeSession.value = null
                    clearVolatileState()
                    session?.close(CloseReason(CloseReason.Codes.NORMAL, "Ending session"))
                    if (isActive) delay(1000)
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        activeSession.value = null
        activeChatId = null
        currentUserId = null
        _state.value = RealtimeSnapshot()
    }

    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }

    fun setActiveChat(chatId: String?) {
        activeChatId = chatId
    }

    fun sendTyping(chatId: String, typing: Boolean) {
        scope.launch {
            sendEvent(TypingOutgoingEvent(chatId = chatId, typing = typing))
        }
    }

    fun markRead(chatId: String) {
        _state.value = _state.value.copy(
            unreadByChat = _state.value.unreadByChat + (chatId to 0)
        )
        scope.launch {
            sendEvent(MarkReadOutgoingEvent(chatId = chatId))
        }
    }

    private suspend fun handleIncomingFrame(text: String) {
        Timber.d("Received: %s", text)
        try {
            val payload = json.parseToJsonElement(text) as? JsonObject ?: return
            when (payload["type"]?.jsonPrimitive?.content) {
                PresenceSnapshotIncomingEvent.TYPE -> {
                    val event = json.decodeFromJsonElement<PresenceSnapshotIncomingEvent>(payload)
                    _state.value = _state.value.copy(onlineUserIds = event.userIds.toSet())
                }

                UserPresenceIncomingEvent.TYPE -> {
                    val event = json.decodeFromJsonElement<UserPresenceIncomingEvent>(payload)
                    val updatedOnlineUsers = _state.value.onlineUserIds.toMutableSet()
                    if (event.online) updatedOnlineUsers += event.userId else updatedOnlineUsers -= event.userId
                    _state.value = _state.value.copy(onlineUserIds = updatedOnlineUsers)
                }

                NewMessageIncomingEvent.TYPE -> {
                    val event = json.decodeFromJsonElement<NewMessageIncomingEvent>(payload)
                    handleIncomingMessage(event.message.toMessage())
                }

                TypingIncomingEvent.TYPE -> {
                    val event = json.decodeFromJsonElement<TypingIncomingEvent>(payload)
                    if (event.userId == currentUserId) return
                    val chatTyping = _state.value.typingByChat[event.chatId].orEmpty().toMutableSet()
                    if (event.typing) chatTyping += event.userId else chatTyping -= event.userId
                    val updatedTyping = _state.value.typingByChat.toMutableMap()
                    if (chatTyping.isEmpty()) updatedTyping.remove(event.chatId) else updatedTyping[event.chatId] = chatTyping
                    _state.value = _state.value.copy(typingByChat = updatedTyping)
                }

                ReadIncomingEvent.TYPE -> {
                    val event = json.decodeFromJsonElement<ReadIncomingEvent>(payload)
                    val readAt = parseRfc3339(event.readAt)
                    val chatReads = _state.value.lastReadByChatUser[event.chatId].orEmpty().toMutableMap()
                    chatReads[event.userId] = readAt
                    val updatedReads = _state.value.lastReadByChatUser.toMutableMap()
                    updatedReads[event.chatId] = chatReads

                    val updatedUnread = if (event.userId == currentUserId) {
                        _state.value.unreadByChat + (event.chatId to 0)
                    } else {
                        _state.value.unreadByChat
                    }

                    _state.value = _state.value.copy(
                        lastReadByChatUser = updatedReads,
                        unreadByChat = updatedUnread
                    )
                }

                else -> Timber.d("Ignoring unsupported realtime event: %s", payload["type"])
            }
        } catch (e: SerializationException) {
            Timber.e(e, "Failed to decode websocket event")
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        val updatedMessages = (_state.value.messagesByChat[message.chatId].orEmpty() + message)
            .distinctBy { it.id }
            .sortedBy { it.timestamp }

        val updatedTyping = _state.value.typingByChat.toMutableMap()
        val chatTyping = updatedTyping[message.chatId].orEmpty().toMutableSet()
        chatTyping.remove(message.senderId)
        if (chatTyping.isEmpty()) updatedTyping.remove(message.chatId) else updatedTyping[message.chatId] = chatTyping

        val shouldIncrementUnread = message.senderId != currentUserId && activeChatId != message.chatId
        val updatedUnread = _state.value.unreadByChat.toMutableMap()
        if (shouldIncrementUnread) {
            updatedUnread[message.chatId] = (updatedUnread[message.chatId] ?: 0) + 1
        } else if (activeChatId == message.chatId) {
            updatedUnread[message.chatId] = 0
        }

        _state.value = _state.value.copy(
            messagesByChat = _state.value.messagesByChat + (message.chatId to updatedMessages),
            typingByChat = updatedTyping,
            unreadByChat = updatedUnread
        )

        _incomingMessages.emit(message)

        if (message.senderId != currentUserId && activeChatId == message.chatId) {
            markRead(message.chatId)
        }
    }

    private suspend fun sendEvent(event: Any) {
        val session = activeSession.value ?: return
        val payload = when (event) {
            is TypingOutgoingEvent -> json.encodeToString(TypingOutgoingEvent.serializer(), event)
            is MarkReadOutgoingEvent -> json.encodeToString(MarkReadOutgoingEvent.serializer(), event)
            else -> return
        }
        runCatching {
            session.outgoing.send(Frame.Text(payload))
        }.onFailure {
            Timber.w(it, "Failed to send websocket event")
        }
    }

    private fun clearVolatileState() {
        _state.value = _state.value.copy(
            onlineUserIds = emptySet(),
            typingByChat = emptyMap()
        )
    }

    private fun MessageDto.toMessage(): Message {
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            text = text,
            timestamp = parseRfc3339(createdAt),
            isPending = false,
            isFailed = false,
            isRead = false
        )
    }

    private fun parseRfc3339(timestamp: String): Long {
        return try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse timestamp: %s", timestamp)
            0L
        }
    }
}

@Serializable
private data class PresenceSnapshotIncomingEvent(
    val type: String,
    @SerialName("user_ids")
    val userIds: List<String>
) {
    companion object {
        const val TYPE = "presence_snapshot"
    }
}

@Serializable
private data class UserPresenceIncomingEvent(
    val type: String,
    @SerialName("user_id")
    val userId: String,
    val online: Boolean
) {
    companion object {
        const val TYPE = "user_presence"
    }
}

@Serializable
private data class NewMessageIncomingEvent(
    val type: String,
    val message: MessageDto
) {
    companion object {
        const val TYPE = "new_message"
    }
}

@Serializable
private data class TypingIncomingEvent(
    val type: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
    val typing: Boolean
) {
    companion object {
        const val TYPE = "typing"
    }
}

@Serializable
private data class ReadIncomingEvent(
    val type: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("read_at")
    val readAt: String
) {
    companion object {
        const val TYPE = "read"
    }
}

@Serializable
private data class TypingOutgoingEvent(
    val type: String = "typing",
    @SerialName("chat_id")
    val chatId: String,
    val typing: Boolean
)

@Serializable
private data class MarkReadOutgoingEvent(
    val type: String = "mark_read",
    @SerialName("chat_id")
    val chatId: String
)
