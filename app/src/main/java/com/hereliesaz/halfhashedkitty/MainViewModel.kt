package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    val serverUrl = mutableStateOf("http://10.0.2.2:5000") // Default for Android emulator
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPasswords = mutableStateListOf<String>() // Changed to a list
    val hashModes = mutableStateListOf<HashModeInfo>()
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)
    val attackModes = mutableStateListOf<AttackMode>()
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))
    val rulesFile = mutableStateOf("")
    val customMask = mutableStateOf("")
    val force = mutableStateOf(false)

    // State for packet capturing
    val isCapturing = mutableStateOf(false)
    val captureOutput = mutableStateListOf<String>()

    init {
        loadHashModes()
        loadAttackModes()
    }

    private fun loadAttackModes() {
        attackModes.addAll(
            listOf(
                AttackMode(0, "Straight"),
                AttackMode(1, "Combination"),
                AttackMode(3, "Brute-force"),
                AttackMode(6, "Hybrid Wordlist + Mask"),
                AttackMode(7, "Hybrid Mask + Wordlist")
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
                            // The mode is now a string
                            hashModes.add(HashModeInfo(parts[0], parts[1]))
                        } else {
                            android.util.Log.w("MainViewModel", "Line does not match expected format: '$line'")
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

    fun startCapture() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                isCapturing.value = true
                captureOutput.clear()
                // ... (existing capture logic remains the same)
            } catch (e: IOException) {
                captureOutput.add("[FATAL] An I/O error occurred: ${e.message}")
                isCapturing.value = false
            } catch (e: Exception) {
                captureOutput.add("[FATAL] An unexpected error occurred: ${e.message}")
                isCapturing.value = false
            }
        }
    }

    private fun findWirelessInterface(): String? {
        val result = RootUtils.executeAsRoot("ls /sys/class/net")
        return result.stdout.lines().find { it.startsWith("wlan") }
    }

    fun stopCapture() {
        isCapturing.value = false
        captureOutput.add("Stopping capture... (Not implemented yet)")
    }

    fun uploadPcapngFile(context: android.content.Context, uri: android.net.Uri): Job {
        return viewModelScope.launch {
            terminalOutput.add("Uploading PCAPNG file to cap2hashcat...")
            try {
                val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (fileBytes != null) {
                    val hash = cap2hashcatApiClient.uploadPcapngFile(fileBytes)
                    if (hash.isNotEmpty()) {
                        hashToCrack.value = hash
                        terminalOutput.add("Extracted hash: $hash")
                        identifyHash()
                    } else {
                        terminalOutput.add("[ERROR] Failed to extract hash from file.")
                    }
                } else {
                    terminalOutput.add("[ERROR] Error reading file.")
                }
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] An unexpected error occurred during upload: ${e.message}")
            }
        }
    }

    fun onQrCodeScanned(qrCodeValue: String) {
        // Ensure the URL starts with http:// or https://
        if (qrCodeValue.startsWith("http://") || qrCodeValue.startsWith("https://")) {
            serverUrl.value = qrCodeValue
            terminalOutput.add("Server URL set to: $qrCodeValue")
        } else {
            terminalOutput.add("Invalid QR code. Expected a URL.")
        }
    }

    fun identifyHash() {
        viewModelScope.launch {
            try {
                val response = apiClient.identifyHash(serverUrl.value, hashToCrack.value)
                hashModes.clear()
                hashModes.addAll(response.modes) // Use the new 'modes' field
                if (hashModes.isNotEmpty()) {
                    selectedHashMode.value = hashModes.first()
                }
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] Error identifying hash: ${e.message}")
            }
        }
    }

    fun uploadZipFile(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            terminalOutput.add("Uploading ZIP file...")
            try {
                val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (fileBytes != null) {
                    val response = apiClient.uploadZipFile(serverUrl.value, fileBytes)
                    terminalOutput.add("File uploaded successfully. Hash file path: ${response.hash}")
                    // You might want to automatically set the hash file path for an attack
                }
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] An unexpected error occurred during ZIP upload: ${e.message}")
            }
        }
    }

    fun startAttack() {
        if (selectedHashMode.value == null) {
            terminalOutput.add("Please select a hash mode.")
            return
        }
        terminalOutput.clear()
        crackedPasswords.clear()
        terminalOutput.add("Starting remote attack...")
        viewModelScope.launch {
            try {
                // Use the new AttackRequest with a String for hashType
                val request = AttackRequest(
                    hash = hashToCrack.value,
                    hashType = selectedHashMode.value!!.mode,
                    wordlist = wordlistPath.value
                )
                val response = apiClient.startAttack(serverUrl.value, request)
                terminalOutput.add("Attack started with job ID: ${response.jobId}")
                pollForStatus(response.jobId)
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] Error starting attack: ${e.message}")
            }
        }
    }

    private fun pollForStatus(jobId: String) {
        viewModelScope.launch {
            while (true) {
                try {
                    val response = apiClient.getAttackStatus(serverUrl.value, jobId)
                    terminalOutput.add("Job ${response.jobId}: ${response.status}")

                    if (response.status == "completed") {
                        terminalOutput.add("Attack finished.")
                        response.cracked?.let {
                            if (it.isNotEmpty()) {
                                terminalOutput.add("--- Cracked Passwords ---")
                                it.forEach { pass -> terminalOutput.add(pass) }
                                crackedPasswords.addAll(it)
                            } else {
                                terminalOutput.add("No passwords were cracked.")
                            }
                        }
                        response.output?.let {
                            terminalOutput.add("--- Full Output ---")
                            terminalOutput.addAll(it.lines())
                        }
                        break // Exit the loop
                    } else if (response.status == "failed") {
                        terminalOutput.add("[ERROR] Attack failed.")
                        response.error?.let { terminalOutput.add(it) }
                        break // Exit the loop
                    }
                    // If status is "running" or "queued", just keep polling

                } catch (e: Exception) {
                    terminalOutput.add("[ERROR] Error getting status: ${e.message}")
                    break // Exit loop on error
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