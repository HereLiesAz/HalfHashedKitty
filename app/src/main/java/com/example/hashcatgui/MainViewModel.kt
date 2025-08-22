package com.example.hashcatgui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(private val application: Application) : ViewModel() {

    private val apiClient = HashcatApiClient()

    val serverUrl = mutableStateOf("http://10.0.2.2:8080") // Default for Android emulator
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPassword = mutableStateOf<String?>(null)
    val hashModes = mutableStateListOf<Pair<Int, String>>()
    val selectedHashMode = mutableStateOf<Pair<Int, String>?>(null)

    // Command Builder properties
    val attackModes = listOf(
        Pair(0, "Straight"),
        Pair(1, "Combination"),
        Pair(3, "Brute-force"),
        Pair(6, "Hybrid Wordlist + Mask"),
        Pair(7, "Hybrid Mask + Wordlist")
    )
    val selectedAttackMode = mutableStateOf(attackModes[0])
    val rulesFile = mutableStateOf("")
    val customMask = mutableStateOf("")
    val force = mutableStateOf(false)

    init {
        loadHashModes()
    }

    internal fun loadHashModes() {
        viewModelScope.launch {
            try {
                val inputStream = application.resources.openRawResource(R.raw.modes)
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach {
                        val parts = it.split(" ".toRegex(), 2)
                        if (parts.size == 2) {
                            hashModes.add(Pair(parts[0].toInt(), parts[1]))
                        }
                    }
                }
                if (hashModes.isNotEmpty()) {
                    selectedHashMode.value = hashModes[0]
                }
            } catch (e: Exception) {
                terminalOutput.add("Error loading hash modes: ${e.message}")
            }
        }
    }

    fun identifyHash() {
        viewModelScope.launch {
            try {
                terminalOutput.add("Identifying hash...")
                val response = apiClient.identifyHash(serverUrl.value, hashToCrack.value)
                if (response.hashModes.isNotEmpty()) {
                    val bestGuess = response.hashModes[0]
                    selectedHashMode.value = hashModes.find { it.first == bestGuess.first }
                    terminalOutput.add("Hash identified as: ${selectedHashMode.value?.second}")
                } else {
                    terminalOutput.add("Could not identify hash type.")
                }
            } catch (e: Exception) {
                terminalOutput.add("Error identifying hash: ${e.message}")
            }
        }
    }

    fun uploadZipFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                terminalOutput.add("Uploading ZIP file...")
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.readBytes()
                inputStream?.close()

                if (fileBytes != null) {
                    val response = apiClient.uploadZipFile(serverUrl.value, fileBytes)
                    hashToCrack.value = response.hash
                    terminalOutput.add("Hash extracted from ZIP file: ${response.hash}")
                } else {
                    terminalOutput.add("Error reading file.")
                }
            } catch (e: Exception) {
                terminalOutput.add("Error uploading file: ${e.message}")
            }
        }
    }

    fun startAttack() {
        val currentHashMode = selectedHashMode.value
        if (currentHashMode == null) {
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
                    hashType = currentHashMode.first,
                    attackMode = selectedAttackMode.value.first,
                    wordlist = wordlistPath.value.ifEmpty { null },
                    rules = rulesFile.value.ifEmpty { null },
                    mask = customMask.value.ifEmpty { null },
                    force = force.value
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

    class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
