package ru.jarvis.telegramka.data.remote

import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.domain.model.Message
import ru.jarvis.telegramka.data.remote.model.MessageDto
import ru.jarvis.telegramka.data.storage.ITokenManager
import ru.jarvis.telegramka.di.WebSocketHolder
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeChatManager @Inject constructor(
    webSocketHolder: WebSocketHolder,
    private val tokenManager: ITokenManager,
    private val json: Json
) {
    private val client = webSocketHolder.client
    private val _incomingMessages = MutableSharedFlow<Message>()
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                        delay(10000) // Wait before retrying
                        continue
                    }

                    Timber.d("Attempting to connect...")
                    val webSocketBlock: suspend DefaultClientWebSocketSession.() -> Unit = {
                        session = this
                        Timber.d("Connection established.")
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                Timber.d("Received: %s", text)
                                try {
                                    val messageDto = json.decodeFromString<MessageDto>(text)
                                    _incomingMessages.emit(messageDto.toMessage())
                                } catch (e: SerializationException) {
                                    Timber.e(e, "Failed to decode message")
                                }
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
                    Timber.d("Closing session and cleaning up.")
                    session?.close(CloseReason(CloseReason.Codes.NORMAL, "Ending session"))
                    if (isActive) delay(1000) // Brief delay before retry loop continues
                }
            }
        }
    }

    fun disconnect() {
        Timber.d("Disconnect requested.")
        connectionJob?.cancel()
        connectionJob = null
    }

    private fun MessageDto.toMessage(): Message {
        return Message(
            id = this.id,
            chatId = this.chatId,
            senderId = this.senderId,
            text = this.text,
            timestamp = parseRfc3339(this.createdAt)
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