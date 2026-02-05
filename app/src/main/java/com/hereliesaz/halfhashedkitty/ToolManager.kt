package com.hereliesaz.halfhashedkitty

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Manages the installation and setup of binary tools (e.g., aircrack-ng) bundled with the app.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Checking the device architecture (ABI) to select the correct binaries.</li>
 *     <li>Extracting binary assets to the app's internal files directory.</li>
 *     <li>Setting executable permissions (chmod) on the extracted files.</li>
 * </ul>
 * </p>
 *
 * @param context Android Application Context used for accessing assets and file storage.
 */
class ToolManager(private val context: Context) {

    companion object {
        private const val TAG = "ToolManager"
    }

    // List of tool filenames to manage.
    private val tools = listOf("aircrack-ng", "airodump-ng", "airmon-ng", "hcxdumptool")
    // List of supported ABIs (Architecture Binary Interfaces).
    private val supportedAbis = listOf("arm64-v8a", "armeabi-v7a") // Ensure these match your asset structure.
    // The target directory where binaries will be installed.
    private val binDir: File = File(context.filesDir, "bin")

    /**
     * Determines the best matching ABI for the current device from the supported list.
     *
     * @return The ABI string (e.g., "arm64-v8a") or null if no match found.
     */
    private fun getBestSupportedAbi(): String? {
        val deviceAbis = Build.SUPPORTED_ABIS
        // Find the first supported ABI that exists in the device's list.
        return supportedAbis.firstOrNull { deviceAbis.contains(it) }
    }

    /**
     * Gets the full absolute path to an installed tool.
     *
     * @param toolName The name of the tool (e.g., "aircrack-ng").
     * @return The absolute path string.
     */
    fun getToolPath(toolName: String): String {
        return File(binDir, toolName).absolutePath
    }

    /**
     * Checks if all required tools are present in the installation directory.
     *
     * @return true if all tools exist, false otherwise.
     */
    fun areToolsInstalled(): Boolean {
        if (!binDir.exists()) return false
        tools.forEach {
            if (!File(binDir, it).exists()) return false
        }
        return true
    }

    /**
     * Installs the tools by extracting them from the assets folder.
     *
     * @return true if installation was successful, false otherwise.
     */
    fun installTools(): Boolean {
        // Skip if already installed (simplification; might want version checks in future).
        if (areToolsInstalled()) {
            return true
        }

        val abi = getBestSupportedAbi()
        if (abi == null) {
            Log.e(TAG, "Device architecture is not supported.")
            return false
        }
        Log.i(TAG, "Using ABI: $abi")

        // Ensure the bin directory exists and is clean.
        if (binDir.exists()) {
            binDir.deleteRecursively()
        }
        binDir.mkdirs()

        // Copy each tool from the matching asset subfolder.
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

        // Make the tools executable using root (chmod).
        // Note: In non-rooted context, standard Java File.setExecutable() might work if internal storage is used,
        // but often Android restrictions require shell chmod.
        val chmodCommands = tools.map { "chmod 755 ${getToolPath(it)}" }
        val result = RootUtils.executeAsRoot(*chmodCommands.toTypedArray())

        if (result.exitCode != 0) {
            Log.e(TAG, "Failed to make tools executable: ${result.stderr}")
            // Fallback: Try Java API just in case root failed but we are owner.
            var allJavaChmodSuccess = true
            tools.forEach {
                 if (!File(binDir, it).setExecutable(true)) allJavaChmodSuccess = false
            }
            // Return success if Java fallback worked or root worked.
            return allJavaChmodSuccess
        }
        return true
    }
}
