package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient
) : ViewModel() {

    enum class ConnectionType {
        RELAY,
        DIRECT
    }

    private val RELAY_URL = BuildConfig.RELAY_URL
    internal var roomID = mutableStateOf<String?>(null)

    val manualInput = mutableStateOf("")
    val connectionType = mutableStateOf(ConnectionType.RELAY)
    val hashToCrack = mutableStateOf("C:\\Users\\user\\Desktop\\hashes.txt") // Placeholder for the file path on the desktop
    val wordlistPath = mutableStateOf("C:\\Users\\user\\Desktop\\wordlist.txt") // Placeholder
    val terminalOutput = mutableStateListOf<String>()
    val crackedPasswords = mutableStateListOf<String>()
    val hashModes = mutableStateListOf<HashModeInfo>()
    private val validHashModes = mutableSetOf<String>()
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)
    val attackModes = mutableStateListOf<AttackMode>()
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))
    val rulesFile = mutableStateOf("")
    val isConnected = mutableStateOf(false)
    val isAttackRunning = mutableStateOf(false)

    init {
        loadLocalData()
        listenForServerMessages()
    }

    private fun listenForServerMessages() {
        viewModelScope.launch {
            apiClient.incomingMessages.collect { jsonString ->
                try {
                    val message = Json.decodeFromString(WebSocketMessage.serializer(), jsonString)
                    when (message.type) {
                        "room_id" -> {
                            val roomInfo = Json.decodeFromString(RoomInfo.serializer(), message.payload)
                            terminalOutput.add("Successfully connected to relay and joined room: ${roomInfo.id}")
                            isConnected.value = true
                        }
                        "status_update" -> {
                            val statusUpdate = Json.decodeFromString(StatusUpdatePayload.serializer(), message.payload)
                            val statusText = "Job ${statusUpdate.jobId}: ${statusUpdate.status}"
                            terminalOutput.add(statusText)

                            statusUpdate.output?.let {
                                if (it.isNotBlank()) terminalOutput.add(it)
                            }
                            statusUpdate.error?.let {
                                terminalOutput.add("[ERROR] ${it}")
                            }

                            if (statusUpdate.status == "completed" || statusUpdate.status == "failed") {
                                terminalOutput.add("--- Job Finished ---")
                                isAttackRunning.value = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to parse message from server: $jsonString", e)
                }
            }
        }
    }

    fun connectManually() {
        val input = manualInput.value
        if (input.isBlank()) {
            terminalOutput.add("Please enter an IP address or a Room ID.")
            return
        }

        when (connectionType.value) {
            ConnectionType.RELAY -> {
                terminalOutput.add("Attempting to connect to relay with Room ID: $input")
                onQrCodeScanned(input) // Re-use existing logic
            }
            ConnectionType.DIRECT -> {
                val url = if (input.startsWith("ws://") || input.startsWith("wss://")) {
                    input
                } else {
                    "ws://$input"
                }
                terminalOutput.add("Attempting direct connection to: $url")
                this.roomID.value = "direct_connection" // Use a consistent, non-null room ID for direct connections
                viewModelScope.launch {
                    try {
                        apiClient.connect(url, roomID.value!!, viewModelScope)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to connect directly", e)
                        isConnected.value = false
                        terminalOutput.add("Error connecting directly: ${e.message}")
                    }
                }
            }
        }
    }

    private fun loadLocalData() {
        attackModes.addAll(
            listOf(
                AttackMode(0, "Straight"),
                AttackMode(3, "Brute-force")
            )
        )
        viewModelScope.launch {
            try {
                application.resources.openRawResource(R.raw.modes).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(" ".toRegex(), 2)
                        if (parts.size == 2) {
                            val mode = parts[0]
                            if (mode.all { it.isDigit() }) {
                                hashModes.add(HashModeInfo(mode, parts[1]))
                                validHashModes.add(mode)
                            } else {
                                android.util.Log.w("MainViewModel", "Mode string is not numeric: '$mode' in line: '$line'")
                            }
                        }
                    }
                }
                if (hashModes.isNotEmpty()) {
                    selectedHashMode.value = hashModes.first()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading hash modes", e)
            }
        }
    }

    fun onQrCodeScanned(scannedRoomId: String) {
        terminalOutput.add("QR Code scanned. Room ID: $scannedRoomId")
        this.roomID.value = scannedRoomId
        viewModelScope.launch {
            try {
                apiClient.connect(RELAY_URL, scannedRoomId, viewModelScope)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to connect to relay", e)
                isConnected.value = false
            }
        }
    }

    fun getApiClient(): HashcatApiClient {
        return apiClient
    }

    fun startAttack() {
        val currentRoomId = roomID.value
        if (currentRoomId == null) {
            terminalOutput.add("Not connected. Please scan the QR code from the desktop client.")
            return
        }
        val currentHashMode = selectedHashMode.value
        if (currentHashMode == null) {
            terminalOutput.add("Please select a hash mode.")
            return
        }

        if (!validHashModes.contains(currentHashMode.mode)) {
            terminalOutput.add("Invalid hash mode selected. Please select a valid mode from the list.")
            return
        }

        terminalOutput.clear()
        crackedPasswords.clear()
        terminalOutput.add("Sending attack command...")
        isAttackRunning.value = true

        viewModelScope.launch {
            try {
                val attackParams = AttackParams(
                    jobId = UUID.randomUUID().toString(),
                    file = hashToCrack.value,
                    mode = currentHashMode.mode,
                    attackMode = selectedAttackMode.value.id.toString(),
                    wordlist = wordlistPath.value,
                    rules = rulesFile.value
                )

                val payloadJson = Json.encodeToString(attackParams)
                val message = WebSocketMessage(
                    type = "attack",
                    payload = payloadJson,
                    room_id = currentRoomId
                )

                apiClient.sendMessage(message)
                terminalOutput.add("Attack command sent for job ID: ${attackParams.jobId}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to send attack command", e)
            }
        }
    }

    class MainViewModelFactory(
        private val application: Application,
        private val apiClient: HashcatApiClient,
        private val cap2hashcatApiClient: Cap2HashcatApiClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, apiClient, cap2hashcatApiClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    override fun onCleared() {
        super.onCleared()
        apiClient.close()
        cap2hashcatApiClient.close()
    }
}