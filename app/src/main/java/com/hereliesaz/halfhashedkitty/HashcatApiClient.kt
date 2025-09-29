package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HashcatApiClient {

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    suspend fun connect(relayUrl: String) {
        try {
            session?.close()
            session = client.webSocketSession(relayUrl)

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
            throw e
        }
    }

    suspend fun sendMessage(message: WebSocketMessage) {
        session?.let {
            if (it.isActive) {
                val jsonString = Json.encodeToString(message)
                it.send(jsonString)
            } else {
                println("Cannot send message, session is not active.")
            }
        }
    }

    fun close() {
        client.close()
    }
}