package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
    suspend fun startAttack(serverUrl: String, params: Map<String, String>): Job {
        return client.post("$serverUrl/attack") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()
    }

    suspend fun getAttackStatus(serverUrl: String, jobId: String): Job {
        return client.get("$serverUrl/attack/$jobId").body()
    }
}