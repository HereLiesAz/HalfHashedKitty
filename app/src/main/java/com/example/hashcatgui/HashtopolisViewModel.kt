package com.example.hashcatgui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HashtopolisViewModel : ViewModel() {

    private val apiClient = HashtopolisApiClient()

    val serverUrl = mutableStateOf("")
    val apiKey = mutableStateOf("")
    val agents = mutableStateListOf<Agent>()
    val errorMessage = mutableStateOf<String?>(null)

    fun getAgents() {
        viewModelScope.launch {
            try {
                errorMessage.value = null
                val agentList = apiClient.getAgents(serverUrl.value, apiKey.value)
                agents.clear()
                agents.addAll(agentList)
            } catch (e: Exception) {
                errorMessage.value = "Error fetching agents: ${e.message}"
            }
        }
    }
}
