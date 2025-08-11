package com.example.hashcatgui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val pythonCracker = PythonCracker()
    private val hashQueue = HashQueue(pythonCracker)

    val hashes = mutableStateListOf<Hash>()
    val wordlistPath = mutableStateOf("")
    val terminalOutput = mutableStateListOf<String>()

    init {
        hashes.addAll(hashQueue.hashes)
    }

    fun addHash(hash: String) {
        hashQueue.addHash(hash)
        hashes.clear()
        hashes.addAll(hashQueue.hashes)
    }

    fun setWordlistPath(path: String) {
        wordlistPath.value = path
    }

    fun startAttack() {
        terminalOutput.add("Starting attack...")
        viewModelScope.launch {
            for (hash in hashes) {
                if (hash.password == null) {
                    terminalOutput.add("Attacking hash: ${hash.hash}")
                    // TODO: Allow user to select hash algorithm
                    hashQueue.attack(hash, wordlistPath.value, "md5")
                    if (hash.password != null) {
                        terminalOutput.add("Password found: ${hash.password}")
                    } else {
                        terminalOutput.add("Password not found for hash: ${hash.hash}")
                    }
                }
            }
            terminalOutput.add("Attack finished.")
        }
    }
}
