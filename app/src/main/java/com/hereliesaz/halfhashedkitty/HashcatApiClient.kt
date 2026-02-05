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

/**
 * Network client for communicating with the Relay Server (or Direct Server).
 * <p>
 * This class uses Ktor's WebSocket client to establish a persistent connection.
 * It manages the session lifecycle and exposes incoming messages as a Kotlin Flow
 * for reactive consumption by the ViewModel.
 * </p>
 */
class HashcatApiClient {

    /**
     * Ktor HttpClient configured with the CIO engine and WebSockets plugin.
     */
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    /** The active WebSocket session. Null if disconnected. */
    private var session: DefaultClientWebSocketSession? = null

    /**
     * Internal mutable shared flow for buffering incoming messages.
     * We use SharedFlow to allow multiple subscribers if needed (though typically just one ViewModel).
     */
    private val _incomingMessages = MutableSharedFlow<String>()

    /** Public read-only view of the message flow. */
    val incomingMessages = _incomingMessages.asSharedFlow()

    /**
     * Connects to the specified Relay URL and joins the given room.
     *
     * @param relayUrl The WebSocket endpoint (e.g., "ws://192.168.1.5:5001/ws").
     * @param roomId   The room ID to join.
     * @param scope    The CoroutineScope in which to run the listener loop.
     * @throws Exception If connection fails.
     */
    suspend fun connect(relayUrl: String, roomId: String, scope: CoroutineScope) {
        try {
            // Cancel any existing session to ensure a clean state.
            session?.cancel()

            // Establish the WebSocket connection.
            // Appends the room ID as a query parameter (legacy support) or just connects to endpoint.
            // Note: Desktop server parses JSON "join" message, but query param might be used by proxies.
            val newSession = client.webSocketSession("$relayUrl?room=$roomId")
            session = newSession

            // Launch a coroutine to listen for incoming frames indefinitely.
            scope.launch {
                try {
                    for (frame in newSession.incoming) {
                        if (frame is Frame.Text) {
                            // Emit the text content to the flow.
                            _incomingMessages.emit(frame.readText())
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Log.d("HashcatApiClient", "WebSocket session closed.")
                } catch (e: Exception) {
                    Log.e("HashcatApiClient", "Error receiving message", e)
                }
            }

            // Immediately send the "join" message to the server to subscribe to the room.
            val joinMessage = WebSocketMessage(type = "join", payload = "", room_id = roomId)
            sendMessage(joinMessage)

        } catch (e: Exception) {
            Log.e("HashcatApiClient", "Error connecting to relay", e)
            throw e
        }
    }

    /**
     * Sends a structured message to the server.
     *
     * @param message The message object to serialize and send.
     */
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

    /**
     * Closes the active session and the underlying HTTP client.
     * Should be called when the app is destroyed.
     */
    fun close() {
        session?.cancel()
        client.close()
    }
}
