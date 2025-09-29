package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    val serverUrl = mutableStateOf("http://10.0.2.2:8080") // Default for emulator
    // File paths should be set by the user for portability; leave empty by default
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPasswords = mutableStateListOf<String>() // Keep for potential future use
    val hashModes = mutableStateListOf<HashModeInfo>()
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)
    val attackModes = mutableStateListOf<AttackMode>()
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))
    val rulesFile = mutableStateOf("")
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
        if (qrCodeValue.startsWith("http://") || qrCodeValue.startsWith("https://")) {
            serverUrl.value = qrCodeValue
            isConnected.value = true
            terminalOutput.add("Connected to desktop client at: $qrCodeValue")
        } else {
            terminalOutput.add("[ERROR] Invalid QR code. Expected a URL.")
        }
    }

    fun startAttack() {
        if (!isConnected.value) {
            terminalOutput.add("Not connected to a desktop client. Please scan the QR code first.")
            return
        }
        if (selectedHashMode.value == null) {
            terminalOutput.add("Please select a hash mode.")
            return
        }

        terminalOutput.clear()
        crackedPasswords.clear()
        terminalOutput.add("Sending attack command to ${serverUrl.value}...")

        viewModelScope.launch {
            try {
                val params = mapOf(
                    "file" to hashToCrack.value,
                    "mode" to selectedHashMode.value!!.mode,
                    "wordlist" to wordlistPath.value,
                    "rules" to rulesFile.value
                )

                val initialJob = apiClient.startAttack(serverUrl.value, params)
                terminalOutput.add("Attack started with job ID: ${initialJob.id}")
                pollForStatus(initialJob.id)

            } catch (e: Exception) {
                terminalOutput.add("[ERROR] Failed to start attack: ${e.message}")
            }
        }
    }

    private fun pollForStatus(jobId: String) {
        viewModelScope.launch {
            while (true) {
                try {
                    val job = apiClient.getAttackStatus(serverUrl.value, jobId)
                    terminalOutput.add("Job ${job.id} status: ${job.status}")

                    if (job.status == "completed" || job.status == "failed") {
                        terminalOutput.add("--- Job Finished ---")
                        job.output?.let { terminalOutput.addAll(it.lines()) }
                        job.error?.let { terminalOutput.add("[ERROR] ${it}") }
                        break
                    }
                } catch (e: Exception) {
                    terminalOutput.add("[ERROR] Failed to get job status: ${e.message}")
                    break
                }
                delay(5000) // Poll every 5 seconds
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