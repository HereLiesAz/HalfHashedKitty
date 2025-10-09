package com.hereliesaz.halfhashedkitty

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hereliesaz.halfhashedkitty.models.RemoteConnection
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SniffViewModel(
    private val apiClient: HashcatApiClient,
    private val mainViewModel: MainViewModel // To get roomID. A better solution would be a shared repository.
) : ViewModel() {

    val remoteConnections = mutableStateOf(
        listOf(
            RemoteConnection("pwn-pi", "pi@192.168.1.10"),
            RemoteConnection("cloud-cracker", "user@some-vps.com"),
            RemoteConnection("office-router", "admin@192.168.0.1")
        )
    )

    val selectedConnection = mutableStateOf<RemoteConnection?>(null)
    val sniffOutput = mutableStateOf("")

    init {
        listenForSnifferMessages()
    }

    private fun listenForSnifferMessages() {
        viewModelScope.launch {
            apiClient.incomingMessages.collect { jsonString ->
                try {
                    val message = Json.decodeFromString<WebSocketMessage>(jsonString)
                    when (message.type) {
                        "sniff_output" -> {
                            val outputPayload = Json.decodeFromString<SniffOutputPayload>(message.payload)
                            appendSniffOutput(outputPayload.output)
                        }
                        "sniff_stopped" -> {
                            appendSniffOutput("\n--- Sniffing Stopped by Server ---\n")
                        }
                    }
                } catch (e: Exception) {
                    // This listener will catch all messages, so we expect parsing errors for non-sniffer messages.
                    // We can ignore them silently or log them if needed for debugging.
                }
            }
        }
    }

    fun onConnectionSelected(connection: RemoteConnection) {
        selectedConnection.value = connection
    }

    fun appendSniffOutput(output: String) {
        sniffOutput.value += output
    }

    fun clearSniffOutput() {
        sniffOutput.value = ""
    }

    fun startSniffing(connection: RemoteConnection, password: String) {
        val currentRoomId = mainViewModel.roomID.value ?: return
        clearSniffOutput()
        appendSniffOutput("Sending start sniff command for ${connection.name}...\n")
        viewModelScope.launch {
            try {
                val parts = connection.connectionString.split("@")
                if (parts.size != 2) {
                    appendSniffOutput("Error: Invalid connection string format. Expected 'user@host'.\n")
                    return@launch
                }
                val username = parts[0]
                val host = parts[1]

                val payload = SniffStartPayload(
                    host = host,
                    username = username,
                    password = password
                )
                val payloadJson = Json.encodeToString(payload)
                val message = WebSocketMessage("start_sniff", payloadJson, currentRoomId)
                apiClient.sendMessage(message)
            } catch (e: Exception) {
                appendSniffOutput("Error sending command: ${e.message}\n")
            }
        }
    }

    fun stopSniffing() {
        val currentRoomId = mainViewModel.roomID.value ?: return
        appendSniffOutput("\nSending stop sniff command...\n")
        viewModelScope.launch {
            try {
                val message = WebSocketMessage("stop_sniff", "", currentRoomId)
                apiClient.sendMessage(message)
            } catch (e: Exception) {
                appendSniffOutput("Error sending command: ${e.message}\n")
            }
        }
    }

    class SniffViewModelFactory(
        private val apiClient: HashcatApiClient,
        private val mainViewModel: MainViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SniffViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SniffViewModel(apiClient, mainViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}