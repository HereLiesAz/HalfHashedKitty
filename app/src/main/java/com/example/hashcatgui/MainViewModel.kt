package com.example.hashcatgui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val apiClient = HashcatApiClient()

    val serverUrl = mutableStateOf("http://10.0.2.2:8080") // Default for Android emulator
    val hashToCrack = mutableStateOf("")
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()
    val crackedPassword = mutableStateOf<String?>(null)

    fun startAttack() {
        terminalOutput.clear()
        crackedPassword.value = null
        terminalOutput.add("Starting remote attack...")
        viewModelScope.launch {
            try {
                val request = AttackRequest(
                    hash = hashToCrack.value,
                    hashType = 0, // Hardcoded for now
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
}
