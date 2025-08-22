package com.example.hashcatgui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HashtopolisViewModel(private val apiClient: HashtopolisApiClient) : ViewModel() {

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
                errorMessage.value = "Error fetching agents: ${e.message ?: "An unknown error occurred."}"
            }
        }
    }

    class HashtopolisViewModelFactory(private val apiClient: HashtopolisApiClient) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HashtopolisViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HashtopolisViewModel(apiClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
