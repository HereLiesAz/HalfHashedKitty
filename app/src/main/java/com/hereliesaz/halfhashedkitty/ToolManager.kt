package com.hereliesaz.halfhashedkitty

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class ToolManager(private val context: Context) {

    private val tools = listOf("aircrack-ng", "airodump-ng", "airmon-ng", "hcxdumptool")
    private val arch: String = "arm64-v8a" // In a real app, this should be detected dynamically
    private val binDir: File = File(context.filesDir, "bin")

    fun getToolPath(toolName: String): String {
        return File(binDir, toolName).absolutePath
    }

    fun areToolsInstalled(): Boolean {
        if (!binDir.exists()) return false
        tools.forEach {
            if (!File(binDir, it).exists()) return false
        }
        return true
    }

    fun installTools() {
        if (areToolsInstalled()) {
            // Optional: could add a force reinstall flag
            return
        }

        // Ensure the bin directory exists and is clean
        if (binDir.exists()) {
            binDir.deleteRecursively()
        }
        binDir.mkdirs()

        // Copy tools from assets
        tools.forEach { toolName ->
            val assetPath = "$arch/$toolName"
            val destinationFile = File(binDir, toolName)
            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Handle error, e.g., log it or notify user
                // For now, we'll just print to stderr for debugging
                System.err.println("Failed to copy tool '$toolName': ${e.message}")
            }
        }

        // Make the tools executable
        val chmodCommands = tools.map { "chmod 755 ${getToolPath(it)}" }
        val result = RootUtils.executeAsRoot(*chmodCommands.toTypedArray())
        if (result.exitCode != 0) {
            // Handle error, e.g., throw an exception or return a status
            System.err.println("Failed to make tools executable: ${result.stderr}")
        }
    }
}
