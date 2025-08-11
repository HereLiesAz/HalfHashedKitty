package com.example.hashcatgui

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class HashcatMode(val id: Int, val name: String)

class HashTypeIdentifier(private val context: Context) {

    private val hashcatModes: List<HashcatMode> by lazy {
        loadHashcatModes()
    }

    private fun loadHashcatModes(): List<HashcatMode> {
        val modes = mutableListOf<HashcatMode>()
        val inputStream = context.resources.openRawResource(R.raw.modes)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(" ", limit = 2)
                if (parts.size == 2) {
                    val id = parts[0].toIntOrNull()
                    if (id != null) {
                        modes.add(HashcatMode(id, parts[1]))
                    }
                }
            }
        }
        return modes
    }

    fun identify(hash: String): List<HashcatMode> {
        val possibleModes = mutableListOf<HashcatMode>()

        // Basic identification logic based on hash length
        when (hash.length) {
            32 -> possibleModes.add(hashcatModes.find { it.name == "MD5" }!!)
            40 -> possibleModes.add(hashcatModes.find { it.name == "SHA1" }!!)
            64 -> possibleModes.add(hashcatModes.find { it.name == "SHA2-256" }!!)
            128 -> possibleModes.add(hashcatModes.find { it.name == "SHA2-512" }!!)
        }

        // more advanced identification logic will be added here

        return possibleModes
    }

    fun getModeById(id: Int): HashcatMode? {
        return hashcatModes.find { it.id == id }
    }

    fun getAllModes(): List<HashcatMode> {
        return hashcatModes
    }
}
