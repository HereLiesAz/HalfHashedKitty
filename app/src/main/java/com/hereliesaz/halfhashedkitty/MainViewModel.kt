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
import kotlinx.serialization.InternalSerializationApi
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(InternalSerializationApi::class) // Added OptIn here
class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient,
    private val toolManager: ToolManager
) : ViewModel() {

    val serverUrl = mutableStateOf("http://10.0.2.2:8080") // Default for Android emulator
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPassword = mutableStateOf<String?>(null)
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
        // Mock data for now
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
                            try {
                                hashModes.add(HashModeInfo(parts[0].toInt(), parts[1]))
                            } catch (e: NumberFormatException) {
                                android.util.Log.e("MainViewModel", "Failed to parse hash mode id: '${parts[0]}' in line: '$line'", e)
                            }
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

    // Placeholder functions for capture logic
    fun startCapture() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                isCapturing.value = true
                captureOutput.clear()

                captureOutput.add("Checking for root access...")
                val rootCheck = RootUtils.executeAsRoot("id")
                if (rootCheck.exitCode != 0) {
                    captureOutput.add("[ERROR] Root access check failed.")
                    if(rootCheck.stderr.isNotBlank()) {
                        captureOutput.add("[STDERR] ${rootCheck.stderr}")
                    }
                    isCapturing.value = false
                    return@launch
                }
                captureOutput.add("Root access granted.")

                captureOutput.add("Preparing tools...")
                if (!toolManager.installTools()) {
                    captureOutput.add("[ERROR] Failed to install tools. Check logs for details.")
                    isCapturing.value = false
                    return@launch
                }
                captureOutput.add("Tools are ready.")

                captureOutput.add("Finding wireless interface...")
                val wifiInterface = findWirelessInterface()
                if (wifiInterface == null) {
                    captureOutput.add("[ERROR] No wireless interface (wlanX) found.")
                    isCapturing.value = false
                    return@launch
                }
                captureOutput.add("Found wireless interface: $wifiInterface")

                captureOutput.add("Starting monitor mode on $wifiInterface...")
                val airmonResult = RootUtils.executeAsRoot(toolManager.getToolPath("airmon-ng") + " start " + wifiInterface)
                val airmonStdoutLines = airmonResult.stdout.lines()
                airmonStdoutLines.forEach { if(it.isNotBlank()) captureOutput.add(it) }
                airmonResult.stderr.lines().forEach { if(it.isNotBlank()) captureOutput.add("[STDERR] $it") }

                val monitorInterface = airmonResult.stdout.let {
                    val regex = """monitor mode enabled on\s+(\w+)""".toRegex()
                    regex.find(it)?.groups?.get(1)?.value
                }

                if (monitorInterface == null) {
                    captureOutput.add("[ERROR] Failed to start monitor mode. Check dmesg or logcat for driver errors.")
                    isCapturing.value = false
                    return@launch
                }

                captureOutput.add("Monitor mode enabled on $monitorInterface. Starting capture...")
                captureOutput.add("... (airodump-ng execution logic to be implemented) ...")

            } catch (e: java.io.IOException) {
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
        // In the next step, this will:
        // 1. Kill the airodump-ng process
        // 2. Stop monitor mode
        // 3. Process the capture file
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
                        identifyHash() // Automatically identify the hash type
                    } else {
                        terminalOutput.add("[ERROR] Failed to extract hash from file. The service might be down or the file is invalid.")
                    }
                } else {
                    terminalOutput.add("[ERROR] Error reading file.")
                }
            } catch (e: java.io.IOException) {
                terminalOutput.add("[ERROR] File I/O error: ${e.message}")
            } catch (e: Exception) {
                terminalOutput.add("[ERROR] An unexpected error occurred during upload: ${e.message}")
            }
        }
    }

    fun identifyHash() {
        viewModelScope.launch {
            try {
                val response = apiClient.identifyHash(serverUrl.value, hashToCrack.value)
                hashModes.clear()
                hashModes.addAll(response.hashModes)
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
                // TODO: Implement ZIP file upload. This is currently blocked pending the
                // addition of a `uploadZipFile(fileBytes)` method to the `HashcatApiClient`.
                // Once that method is available, this function should call it and handle
                // the response, likely updating the `hashToCrack` and `terminalOutput`
                // states, similar to how `uploadPcapngFile` is handled.
                terminalOutput.add("File upload functionality is not yet implemented.")
            } catch (e: java.io.IOException) {
                terminalOutput.add("[ERROR] File I/O error while reading ZIP file: ${e.message}")
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
        crackedPassword.value = null
        terminalOutput.add("Starting remote attack...")
        viewModelScope.launch {
            try {
                val request = AttackRequest(
                    hash = hashToCrack.value,
                    hashType = selectedHashMode.value!!.id,
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
                    if (response.status == "Cracked") {
                        crackedPassword.value = response.crackedPassword
                        terminalOutput.add("Password found: ${response.crackedPassword}")
                        break
                    } else if (response.status == "Exhausted" || response.status == "Aborted") {
                        terminalOutput.add("Attack finished. Password not found.")
                        break
                    }
                } catch (e: Exception) {
                    terminalOutput.add("Error getting status: ${e.message}")
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
