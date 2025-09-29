package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HashcatApiClient {

    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    // A wrapper to match the expected Socket.IO message structure
    @Serializable
    data class SocketIOMessage(val event: String, val data: Map<String, String>)

    suspend fun connect(relayUrl: String) {
        try {
            session?.close()
            session = client.webSocketSession(relayUrl.replace("http", "ws"))

            // Launch a coroutine to listen for incoming messages
            session?.launch {
                while (isActive) {
                    try {
                        val frame = incoming.receive()
                        if (frame is Frame.Text) {
                            _incomingMessages.emit(frame.readText())
                        }
                    } catch (e: Exception) {
                        println("Error receiving message: ${e.message}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            println("Error connecting to relay: ${e.message}")
        }
    }

    suspend fun joinRoom(roomId: String) {
        val event = "join_room"
        val data = mapOf("room_id" to roomId)
        send(event, data)
    }

    suspend fun sendMessageToDesktop(roomId: String, payload: Map<String, String>) {
        val event = "message_from_mobile"
        val data = mapOf("room_id" to roomId, "payload" to Json.encodeToString(payload))
        send(event, data)
    }

    private suspend fun send(event: String, data: Map<String, String>) {
        session?.let {
            if (it.isActive) {
                // To communicate with a python-socketio server, we need to send a message
                // in a format it understands. The server is expecting named events.
                // We will emulate the message format: `[event, data]`
                // Ktor's `sendSerialized` can handle a list of mixed types if configured properly.
                // Let's send a custom list structure.
                 val message = listOf(event, data)
                 it.sendSerialized(message)
            } else {
                println("Cannot send message, session is not active.")
            }
        }
    }

    fun close() {
        client.close()
    }
}