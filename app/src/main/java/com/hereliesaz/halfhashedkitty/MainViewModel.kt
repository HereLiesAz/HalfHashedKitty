package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.UUID

class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    private val RELAY_URL = "ws://10.0.2.2:5001" // Default for emulator. Change to your relay's public IP.
    private var room_id: String? = null

    val hashToCrack = mutableStateOf("C:\\Users\\user\\Desktop\\hashes.txt") // Placeholder
    val wordlistPath = mutableStateOf("C:\\Users\\user\\Desktop\\wordlist.txt") // Placeholder
    val terminalOutput = mutableStateListOf<String>()
    val crackedPasswords = mutableStateListOf<String>()
    val hashModes = mutableStateListOf<HashModeInfo>()
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)
    val attackModes = mutableStateListOf<AttackMode>()
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))
    val rulesFile = mutableStateOf("")
    val customMask = mutableStateOf("")
    val force = mutableStateOf(false)
    val isConnected = mutableStateOf(false)

    init {
        loadHashModes()
        loadAttackModes()
        listenForMessages()
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            apiClient.incomingMessages.collect { jsonString ->
                // The python-socketio library sends messages in a specific format.
                // We need to parse it to get to our payload.
                // Example: 42["message_to_mobile",{"jobId":"...","status":"..."}]
                // We will do a simple string manipulation to extract the JSON part.
                val jsonPart = jsonString.substringAfter(",").dropLast(1)
                terminalOutput.add("Received: $jsonPart")

                try {
                    val json = Json.parseToJsonElement(jsonPart).jsonObject
                    val status = json["status"]?.jsonPrimitive?.content
                    val jobId = json["jobId"]?.jsonPrimitive?.content ?: "unknown"

                    when (status) {
                        "running" -> terminalOutput.add("Job $jobId is now running on desktop.")
                        "completed" -> {
                            terminalOutput.add("Job $jobId completed.")
                            val cracked = json["cracked"]?.jsonObject?.values?.map { it.jsonPrimitive.content }
                            if (cracked != null && cracked.isNotEmpty()) {
                                crackedPasswords.addAll(cracked)
                                terminalOutput.add("--- Cracked Passwords ---")
                                cracked.forEach { terminalOutput.add(it) }
                            } else {
                                terminalOutput.add("No new passwords were cracked for job $jobId.")
                            }
                            json["output"]?.jsonPrimitive?.content?.let {
                                terminalOutput.add("--- Full Output ---")
                                terminalOutput.addAll(it.lines())
                            }
                        }
                        "failed" -> {
                            terminalOutput.add("[ERROR] Job $jobId failed.")
                            json["error"]?.jsonPrimitive?.content?.let { terminalOutput.add(it) }
                        }
                    }
                } catch (e: Exception) {
                    terminalOutput.add("[ERROR] Failed to parse message from server: ${e.message}")
                }
            }
        }
    }

    private fun loadAttackModes() {
        attackModes.addAll(
            listOf(
                AttackMode(0, "Straight"),
                AttackMode(3, "Brute-force")
            )
        )
    }

    private fun loadHashModes() {
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
        this.room_id = qrCodeValue
        viewModelScope.launch {
            try {
                // First, connect to the relay server
                apiClient.connect(RELAY_URL)
                // Then, join the specific room for this desktop client
                apiClient.joinRoom(qrCodeValue)

                isConnected.value = true
                terminalOutput.add("Connected to desktop client via relay!")
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] Failed to connect to relay: ${e.message}")
                isConnected.value = false
            }
        }
    }

    fun startAttack() {
        val currentRoomId = room_id
        if (currentRoomId == null) {
            terminalOutput.add("Not connected to a desktop client. Please scan the QR code first.")
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
                val jobId = UUID.randomUUID().toString()
                val payload = mapOf(
                    "jobId" to jobId,
                    "file" to hashToCrack.value,
                    "mode" to selectedHashMode.value!!.mode,
                    "wordlist" to wordlistPath.value,
                    "rules" to rulesFile.value
                )

                apiClient.sendMessageToDesktop(currentRoomId, payload)
                terminalOutput.add("Attack command sent for job ID: $jobId")
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