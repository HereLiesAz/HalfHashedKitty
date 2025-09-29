package com.hereliesaz.halfhashedkitty

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HashcatApiClient {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    suspend fun connect(relayUrl: String, roomId: String, scope: CoroutineScope) {
        try {
            session?.cancel()
            val urlWithRoom = "$relayUrl?room=$roomId"
            session = client.webSocketSession(urlWithRoom)

            scope.launch {
                try {
                    for (frame in session!!.incoming) {
                        if (frame is Frame.Text) {
                            _incomingMessages.emit(frame.readText())
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Log.d("HashcatApiClient", "WebSocket session closed.")
                } catch (e: Exception) {
                    Log.e("HashcatApiClient", "Error receiving message", e)
                }
            }
            // Send the initial join message
            val joinMessage = WebSocketMessage(type = "join", payload = "", room_id = roomId)
            sendMessage(joinMessage)

        } catch (e: Exception) {
            Log.e("HashcatApiClient", "Error connecting to relay", e)
            throw e
        }
    }

    suspend fun sendMessage(message: WebSocketMessage) {
        session?.let {
            if (it.isActive) {
                try {
                    val jsonString = Json.encodeToString(message)
                    it.send(Frame.Text(jsonString))
                } catch (e: Exception) {
                    Log.e("HashcatApiClient", "Error sending message", e)
                }
            } else {
                Log.w("HashcatApiClient", "Cannot send message, session is not active.")
            }
        }
    }

    fun close() {
        session?.cancel()
        client.close()
    }
}