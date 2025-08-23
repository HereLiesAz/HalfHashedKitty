package com.hereliesaz.halfhashedkitty

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class ToolManager(private val context: Context) {

    companion object {
        private const val TAG = "ToolManager"
    }

    private val tools = listOf("aircrack-ng", "airodump-ng", "airmon-ng", "hcxdumptool")
    private val supportedAbis = listOf("arm64-v8a", "armeabi-v7a") // Add more as you bundle them
    private val binDir: File = File(context.filesDir, "bin")

    private fun getBestSupportedAbi(): String? {
        val deviceAbis = Build.SUPPORTED_ABIS
        return supportedAbis.firstOrNull { deviceAbis.contains(it) }
    }

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

    fun installTools(): Boolean {
        if (areToolsInstalled()) {
            // Optional: could add a force reinstall flag
            return true
        }

        val abi = getBestSupportedAbi()
        if (abi == null) {
            Log.e(TAG, "Device architecture is not supported.")
            return false
        }
        Log.i(TAG, "Using ABI: $abi")

        // Ensure the bin directory exists and is clean
        if (binDir.exists()) {
            binDir.deleteRecursively()
        }
        binDir.mkdirs()

        // Copy tools from assets
        tools.forEach { toolName ->
            val assetPath = "$abi/$toolName"
            val destinationFile = File(binDir, toolName)
            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy tool '$toolName'", e)
                return false
            }
        }

        // Make the tools executable
        val chmodCommands = tools.map { "chmod 755 ${getToolPath(it)}" }
        val result = RootUtils.executeAsRoot(*chmodCommands.toTypedArray())
        if (result.exitCode != 0) {
            Log.e(TAG, "Failed to make tools executable: ${result.stderr}")
            return false
        }
        return true
    }
}
