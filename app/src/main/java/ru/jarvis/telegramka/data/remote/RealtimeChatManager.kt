package ru.jarvis.telegramka.data.remote

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.jarvis.telegramka.data.Message
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.repository.ChatRepository
import ru.jarvis.telegramka.data.storage.TokenManager
import ru.jarvis.telegramka.di.WebSocketClient
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeChatManager @Inject constructor(
    @WebSocketClient private val client: HttpClient,
    private val tokenManager: TokenManager,
    private val json: Json
) {
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect() {
        if (connectionJob?.isActive == true) {
            Log.d("RealtimeChatManager", "Already connected or connecting.")
            return
        }
        connectionJob = scope.launch {
            while (isActive) {
                try {
                    val token = tokenManager.getAccessToken().first()
                    if (token == null) {
                        Log.e("RealtimeChatManager", "Not authenticated, cannot connect.")
                        delay(10000) // Wait before retrying
                        continue
                    }

                    Log.d("RealtimeChatManager", "Attempting to connect...")
                    client.webSocket(
                        method = HttpMethod.Get,
                        host = "10.0.2.2",
                        port = 3000,
                        path = "/ws",
                        request = {
                            header(HttpHeaders.Authorization, "Bearer $token")
                        }
                    ) {
                        session = this
                        Log.d("RealtimeChatManager", "Connection established.")
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                Log.d("RealtimeChatManager", "Received: $text")
                                val messageDto = json.decodeFromString<MessageDto>(text)
                                _incomingMessages.emit(messageDto.toMessage())
                            }
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Log.w("RealtimeChatManager", "Connection closed. Reconnecting...", e)
                } catch (e: Exception) {
                    Log.e("RealtimeChatManager", "Connection failed. Retrying in 5 seconds.", e)
                    delay(5000)
                } finally {
                    session = null
                    Log.d("RealtimeChatManager", "Session ended. Will attempt to reconnect if still active.")
                    if (isActive) delay(1000) // Brief delay before retry loop continues
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "User logged out"))
            session = null
        }
        Log.d("RealtimeChatManager", "Disconnected.")
    }

    private fun MessageDto.toMessage(): Message {
        return Message(
            id = this.id,
            chatId = this.chat_id,
            senderId = this.sender_id,
            text = this.text,
            timestamp = parseRfc3339(this.created_at)
        )
    }

    private fun parseRfc3339(timestamp: String): Long {
        return try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
