package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.serialization.InternalSerializationApi
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context
import android.net.Uri


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

    fun processEvidenceFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            terminalOutput.add("Processing evidence file: $uri")
            terminalOutput.add("MIME type: $mimeType")

            when {
                mimeType?.startsWith("image/") == true -> {
                    try {
                        val image = InputImage.fromFilePath(context, uri)
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val resultText = visionText.text
                                if (resultText.isNotBlank()) {
                                    terminalOutput.add("--- OCR Result ---")
                                    terminalOutput.add(resultText)
                                    terminalOutput.add("--------------------")
                                } else {
                                    terminalOutput.add("No text found in the image.")
                                }
                            }
                            .addOnFailureListener { e ->
                                terminalOutput.add("[ERROR] Text recognition failed: ${e.localizedMessage}")
                            }
                    } catch (e: java.io.IOException) {
                        terminalOutput.add("[ERROR] Failed to load image: ${e.localizedMessage}")
                    }
                }
                mimeType?.startsWith("audio/") == true -> {
                    terminalOutput.add("Audio processing is not yet implemented.")
                    // TODO: Implement audio to text transcription
                }
                mimeType?.startsWith("video/") == true -> {
                    terminalOutput.add("Video processing is not yet implemented.")
                    // TODO: Implement video to text (audio + OCR)
                }
                else -> {
                    terminalOutput.add("Unsupported file type: $mimeType. File saved as is.")
                }
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
    fun onQrCodeScanned(qrCodeValue: String) {
        terminalOutput.add("QR Code scanned. Room ID: $qrCodeValue")
        this.room_id = qrCodeValue
        viewModelScope.launch {
            try {
                // First, connect to the relay server
                apiClient.connect(RELAY_URL)
                // Then, join the specific room for this desktop client
                apiClient.joinRoom(qrCodeValue)

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