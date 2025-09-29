package com.hereliesaz.halfhashedkitty

import android.app.Application
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
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    private val RELAY_URL = "ws://10.0.2.2:5001/ws" // Default for emulator. Change to your relay's public IP.
    private var roomID: String? = null

    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPasswords = mutableStateListOf<String>()
    val hashModes = mutableStateListOf<HashModeInfo>()
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)
    val attackModes = mutableStateListOf<AttackMode>()
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))
    val rulesFile = mutableStateOf("")
    val isConnected = mutableStateOf(false)

    init {
        loadLocalData()
        listenForServerMessages()
    }

    private fun listenForServerMessages() {
        viewModelScope.launch {
            apiClient.incomingMessages.collect { jsonString ->
                try {
                    // First, try to parse as a RoomInfo message
                    try {
                        val roomInfo = Json.decodeFromString(RoomInfo.serializer(), jsonString)
                        if (roomInfo.type == "room_id") {
                            terminalOutput.add("Successfully connected to relay and joined room: ${roomInfo.id}")
                            isConnected.value = true
                            return@collect
                        }
                    } catch (e: Exception) {
                        // Not a RoomInfo message, proceed to next type
                    }

                    // Next, try to parse as a StatusUpdate message
                    val statusUpdate = Json.decodeFromString(StatusUpdatePayload.serializer(), jsonString)
                    terminalOutput.add("Status for job ${statusUpdate.jobId}: ${statusUpdate.status}")
                    statusUpdate.output?.let {
                        terminalOutput.add("--- Output ---")
                        terminalOutput.addAll(it.lines())
                    }
                    statusUpdate.error?.let {
                        terminalOutput.add("[ERROR] ${it}")
                    }
                    if (statusUpdate.status == "completed" || statusUpdate.status == "failed") {
                        terminalOutput.add("--- Job Finished ---")
                    }

                } catch (e: Exception) {
                    terminalOutput.add("[ERROR] Failed to parse message from server: $jsonString")
                }
            }
        }
    }

    private fun loadLocalData() {
        // Load static data that doesn't require network
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
                            hashModes.add(HashModeInfo(parts[0], parts[1]))
                        }
                    }
                }
                if (hashModes.isNotEmpty()) {
                    selectedHashMode.value = hashModes.first()
                }
            } catch (e: Exception) {
                terminalOutput.add("Error loading hash modes: ${e.message}")
            }
        }
    }

    fun onQrCodeScanned(qrCodeValue: String) {
        terminalOutput.add("QR Code scanned. Room ID: $qrCodeValue")
        this.roomID = qrCodeValue
        viewModelScope.launch {
            try {
                apiClient.connect(RELAY_URL)
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] Failed to connect to relay: ${e.message}")
                isConnected.value = false
            }
        }
    }

    fun startAttack() {
        val currentRoomId = roomID
        if (currentRoomId == null) {
            terminalOutput.add("Not connected. Please scan the QR code from the desktop client.")

            return
        }
        if (selectedHashMode.value == null) {
            terminalOutput.add("Please select a hash mode.")
            return
        }

        terminalOutput.clear()
        crackedPasswords.clear()
        terminalOutput.add("Sending attack command...")

        viewModelScope.launch {
            try {
                val attackParams = AttackParams(
                    jobId = UUID.randomUUID().toString(),
                    file = hashToCrack.value,
                    mode = selectedHashMode.value!!.mode,
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
                terminalOutput.add("[ERROR] Failed to send attack command: ${e.message}")
            }
        }
    }

    class MainViewModelFactory(
        private val application: Application,
        private val apiClient: HashcatApiClient,
        private val cap2hashcatApiClient: Cap2HashcatApiClient,
        private val toolManager: ToolManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, apiClient, cap2hashcatApiClient, toolManager) as T
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