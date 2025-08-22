package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(InternalSerializationApi::class) // Added OptIn here
class MainViewModel(
    private val application: Application,
    private val apiClient: HashcatApiClient
) : ViewModel() {

    val serverUrl = mutableStateOf("http://10.0.2.2:8080") // Default for Android emulator
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPassword = mutableStateOf<String?>(null)
    val hashModes = mutableStateListOf<Pair<Int, String>>()
    val selectedHashMode = mutableStateOf<Pair<Int, String>?>(null)

    init {
        loadHashModes()
    }

    private fun loadHashModes() {
        viewModelScope.launch {
            try {
                application.resources.openRawResource(R.raw.modes).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(" ".toRegex(), 2)
                        if (parts.size == 2) {
                            try {
                                hashModes.add(Pair(parts[0].toInt(), parts[1]))
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

    fun uploadZipFile(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                terminalOutput.add("Uploading ZIP file...")
                val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                // TODO: Add the rest of the file upload logic
                terminalOutput.add("File upload functionality is not yet implemented.")
            } catch (e: Exception) {
                terminalOutput.add("Error uploading file: ${e.message}")
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
                    hashType = selectedHashMode.value!!.first,
                    wordlist = wordlistPath.value
                )
                val response = apiClient.startAttack(serverUrl.value, request)
                terminalOutput.add("Attack started with job ID: ${response.jobId}")
                pollForStatus(response.jobId)
            } catch (e: Exception) {
                terminalOutput.add("Error starting attack: ${e.message}")
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
        private val apiClient: HashcatApiClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, apiClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
