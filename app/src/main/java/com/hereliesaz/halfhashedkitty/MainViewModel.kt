package com.hereliesaz.halfhashedkitty

import android.app.Application
import android.content.Context
import android.net.Uri
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
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    private val RELAY_URL = BuildConfig.RELAY_URL
    private var roomID: String? = null

    // Restored properties
    val captureOutput = mutableStateListOf<String>()
    val isCapturing = mutableStateOf(false)
    val customMask = mutableStateOf("")
    val force = mutableStateOf(false)
    val serverUrl = mutableStateOf("")
    val crackedPassword = mutableStateOf("")

    val hashToCrack = mutableStateOf("C:\\Users\\user\\Desktop\\hashes.txt") // Placeholder for the file path on the desktop
    val wordlistPath = mutableStateOf("C:\\Users\\user\\Desktop\\wordlist.txt") // Placeholder
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
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to parse message from server: $jsonString", e)
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
                            if (parts[0].all { it.isDigit() }) {
                                hashModes.add(HashModeInfo(parts[0], parts[1]))
                            } else {
                                android.util.Log.w("MainViewModel", "Mode string is not numeric: '${parts[0]}' in line: '$line'")
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
        this.roomID = scannedRoomId
        viewModelScope.launch {
            try {
                apiClient.connect(RELAY_URL, scannedRoomId, viewModelScope)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to connect to relay", e)
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

    // Restored functions
    fun stopCapture() {}
    fun startCapture() {}
    fun uploadZipFile(context: Context, uri: Uri) {}
    fun uploadPcapngFile(context: Context, uri: Uri) {}
    fun identifyHash() {}

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