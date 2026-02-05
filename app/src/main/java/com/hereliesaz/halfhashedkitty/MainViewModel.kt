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

/**
 * The primary ViewModel for the Android application.
 * <p>
 * This class serves as the central hub for managing the UI state and business logic of the app.
 * It is responsible for:
 * <ul>
 *     <li>Managing the connection state (Connected/Disconnected) to the Relay Server or Direct Server.</li>
 *     <li>Holding the mutable state for UI elements (User inputs, selections, logs).</li>
 *     <li>Handling WebSocket communication via the {@link HashcatApiClient}.</li>
 *     <li>Loading static data resources (like hash modes).</li>
 *     <li>Executing attack commands by sending messages to the desktop client.</li>
 * </ul>
 * </p>
 */
class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient,
    private val cap2hashcatApiClient: Cap2HashcatApiClient
) : ViewModel() {

    /**
     * Enum representing the type of connection the user wants to establish.
     */
    enum class ConnectionType {
        /** Connect via the Relay Server (for remote access over internet). */
        RELAY,
        /** Connect directly to the Desktop App's IP (for local LAN access). */
        DIRECT
    }

    /** The hardcoded URL of the public Relay Server (defined in BuildConfig). */
    private val RELAY_URL = BuildConfig.RELAY_URL

    /** The ID of the room currently joined. Null if not joined. */
    internal var roomID = mutableStateOf<String?>(null)

    // UI State Holders (Jetpack Compose MutableState)

    /** Holds the text input for manual connection (IP or Room ID). */
    val manualInput = mutableStateOf("")

    /** Holds the currently selected connection type (Default: RELAY). */
    val connectionType = mutableStateOf(ConnectionType.RELAY)

    /**
     * Placeholder for the hash file path on the DESKTOP machine.
     * In the future, this should be selected via a file browser on the desktop side or uploaded.
     */
    val hashToCrack = mutableStateOf("/path/to/hashes.txt")

    /**
     * Placeholder for the wordlist path on the DESKTOP machine.
     */
    val wordlistPath = mutableStateOf("/path/to/wordlist.txt")

    /** A list of log messages to display in the "Terminal" tab. */
    val terminalOutput = mutableStateListOf<String>()

    /** A list of passwords that have been successfully cracked in this session. */
    val crackedPasswords = mutableStateListOf<String>()

    /** The list of available Hashcat hash modes (loaded from resources). */
    val hashModes = mutableStateListOf<HashModeInfo>()

    /** A set of valid mode IDs for validation purposes. */
    private val validHashModes = mutableSetOf<String>()

    /** The currently selected hash mode. */
    val selectedHashMode = mutableStateOf<HashModeInfo?>(null)

    /** The list of available attack modes (Dictionary, Brute-force, etc.). */
    val attackModes = mutableStateListOf<AttackMode>()

    /** The currently selected attack mode. Default is Straight (Dictionary). */
    val selectedAttackMode = mutableStateOf(AttackMode(0, "Straight"))

    /** The rules file to use (optional). */
    val rulesFile = mutableStateOf("")

    /** Boolean flag indicating if the WebSocket connection is active. */
    val isConnected = mutableStateOf(false)

    /** Boolean flag indicating if an attack job is currently running. */
    val isAttackRunning = mutableStateOf(false)

    /**
     * Initialization block.
     * Loads local resources and starts listening for incoming WebSocket messages.
     */
    init {
        loadLocalData()
        listenForServerMessages()
    }

    /**
     * Launches a coroutine to listen for incoming messages from the WebSocket client.
     * This flows updates from the desktop app (via relay) to the Android UI.
     */
    private fun listenForServerMessages() {
        viewModelScope.launch {
            // Collect messages from the Flow exposed by the API client.
            apiClient.incomingMessages.collect { jsonString ->
                try {
                    // Deserialize the JSON message.
                    val message = Json.decodeFromString(WebSocketMessage.serializer(), jsonString)

                    // Handle different message types.
                    when (message.type) {
                        "room_id" -> {
                            // Server confirmed room join.
                            val roomInfo = Json.decodeFromString(RoomInfo.serializer(), message.payload)
                            terminalOutput.add("Successfully connected to relay and joined room: ${roomInfo.id}")
                            isConnected.value = true
                        }
                        "status_update" -> {
                            // Status update from the desktop app (e.g., progress, logs).
                            val statusUpdate = Json.decodeFromString(StatusUpdatePayload.serializer(), message.payload)
                            val statusText = "Job ${statusUpdate.jobId}: ${statusUpdate.status}"
                            terminalOutput.add(statusText)

                            // Append optional output/logs.
                            statusUpdate.output?.let {
                                if (it.isNotBlank()) terminalOutput.add(it)
                            }
                            // Append optional error messages.
                            statusUpdate.error?.let {
                                terminalOutput.add("[ERROR] ${it}")
                            }

                            // Check if the job has finished.
                            if (statusUpdate.status == "completed" || statusUpdate.status == "failed") {
                                terminalOutput.add("--- Job Finished ---")
                                isAttackRunning.value = false
                            }
                        }
                        // Note: "cracked" type should also be handled here eventually.
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to parse message from server: $jsonString", e)
                }
            }
        }
    }

    /**
     * Initiates a connection based on manual user input (IP or Room ID).
     */
    fun connectManually() {
        val input = manualInput.value
        // Basic validation.
        if (input.isBlank()) {
            terminalOutput.add("Please enter an IP address or a Room ID.")
            return
        }

        when (connectionType.value) {
            ConnectionType.RELAY -> {
                // Connect via Relay Server (input is Room ID).
                terminalOutput.add("Attempting to connect to relay with Room ID: $input")
                // Reuse the QR code logic which expects a Room ID.
                onQrCodeScanned(input)
            }
            ConnectionType.DIRECT -> {
                // Connect directly via WebSocket (input is IP/URL).
                // Ensure proper WebSocket scheme.
                val url = if (input.startsWith("ws://") || input.startsWith("wss://")) {
                    input
                } else {
                    "ws://$input"
                }
                terminalOutput.add("Attempting direct connection to: $url")
                // For direct connections, we don't need a specific room ID, but we set a placeholder.
                this.roomID.value = "direct_connection"
                viewModelScope.launch {
                    try {
                        // Connect directly to the provided URL.
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

    /**
     * Loads static data (Attack Modes, Hash Modes) from local resources.
     */
    private fun loadLocalData() {
        // Initialize hardcoded attack modes.
        attackModes.addAll(
            listOf(
                AttackMode(0, "Straight"),
                AttackMode(3, "Brute-force")
            )
        )
        // Load hash modes from raw resource file asynchronously.
        viewModelScope.launch {
            try {
                // Read lines from res/raw/modes.txt
                application.resources.openRawResource(R.raw.modes).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        // Format expected: "CODE Name" (e.g., "0 MD5")
                        val parts = line.split(" ".toRegex(), 2)
                        if (parts.size == 2) {
                            val mode = parts[0]
                            // Verify the mode ID is numeric.
                            if (mode.all { it.isDigit() }) {
                                hashModes.add(HashModeInfo(mode, parts[1]))
                                validHashModes.add(mode)
                            } else {
                                android.util.Log.w("MainViewModel", "Mode string is not numeric: '$mode' in line: '$line'")
                            }
                        }
                    }
                }
                // Select the first mode by default if available.
                if (hashModes.isNotEmpty()) {
                    selectedHashMode.value = hashModes.first()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading hash modes", e)
            }
        }
    }

    /**
     * Handles the result of a QR code scan.
     * Expects the scanned content to be a Room ID (or full URL, but currently treating as Room ID for Relay).
     * Note: The Desktop App generates a full URL. This logic might need adjustment to parse URL params.
     *
     * @param scannedRoomId The content scanned from the QR code.
     */
    fun onQrCodeScanned(scannedRoomId: String) {
        // TODO: Parse full URL from QR code if it contains ws:// schema.
        // Current implementation assumes QR code contains just the Room ID for Relay.
        terminalOutput.add("QR Code scanned. Room ID: $scannedRoomId")
        this.roomID.value = scannedRoomId
        viewModelScope.launch {
            try {
                // Connect to the hardcoded Relay URL with the scanned Room ID.
                apiClient.connect(RELAY_URL, scannedRoomId, viewModelScope)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to connect to relay", e)
                isConnected.value = false
            }
        }
    }

    /**
     * Exposes the underlying API client.
     */
    fun getApiClient(): HashcatApiClient {
        return apiClient
    }

    /**
     * Sends the configured attack parameters to the connected desktop client.
     */
    fun startAttack() {
        // Validation: Must be connected.
        val currentRoomId = roomID.value
        if (currentRoomId == null) {
            terminalOutput.add("Not connected. Please scan the QR code from the desktop client.")
            return
        }
        // Validation: Must have a hash mode selected.
        val currentHashMode = selectedHashMode.value
        if (currentHashMode == null) {
            terminalOutput.add("Please select a hash mode.")
            return
        }

        if (!validHashModes.contains(currentHashMode.mode)) {
            terminalOutput.add("Invalid hash mode selected. Please select a valid mode from the list.")
            return
        }

        // Clear previous session data.
        terminalOutput.clear()
        crackedPasswords.clear()
        terminalOutput.add("Sending attack command...")
        isAttackRunning.value = true

        // Send the command asynchronously.
        viewModelScope.launch {
            try {
                // Construct the payload object.
                val attackParams = AttackParams(
                    jobId = UUID.randomUUID().toString(),
                    file = hashToCrack.value,
                    mode = currentHashMode.mode,
                    attackMode = selectedAttackMode.value.id.toString(),
                    wordlist = wordlistPath.value,
                    rules = rulesFile.value
                )

                // Serialize payload to JSON.
                val payloadJson = Json.encodeToString(attackParams)

                // Wrap in the standard WebSocket message envelope.
                val message = WebSocketMessage(
                    type = "attack",
                    payload = payloadJson,
                    room_id = currentRoomId
                )

                // Send.
                apiClient.sendMessage(message)
                terminalOutput.add("Attack command sent for job ID: ${attackParams.jobId}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to send attack command", e)
            }
        }
    }

    /**
     * Factory class for creating instances of MainViewModel with dependencies.
     */
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

    /**
     * Cleanup when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        // Close network connections to prevent leaks.
        apiClient.close()
        cap2hashcatApiClient.close()
    }
}
