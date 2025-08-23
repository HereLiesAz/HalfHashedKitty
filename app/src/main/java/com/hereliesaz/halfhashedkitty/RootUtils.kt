package com.hereliesaz.halfhashedkitty

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootUtils {

    data class ShellOutput(val stdout: String, val stderr: String, val exitCode: Int)

    fun isRooted(): Boolean {
        return executeAsRoot("id").exitCode == 0
    }

    private fun findSuBinary(): String? {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return path
        }
        return null
    }

    fun executeAsRoot(vararg commands: String): ShellOutput {
        val suPath = findSuBinary()
        if (suPath == null) {
            return ShellOutput("", "Could not find 'su' binary on this device.", -1)
        }

        return try {
            val process = Runtime.getRuntime().exec(suPath)

            DataOutputStream(process.outputStream).use { os ->
                for (command in commands) {
                    os.writeBytes("$command\n")
                    os.flush()
                }
                os.writeBytes("exit\n")
                os.flush()
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()

            ShellOutput(stdout, stderr, exitCode)
        } catch (e: java.io.IOException) {
            ShellOutput("", "I/O error executing root command: ${e.localizedMessage}", -1)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Preserve the interrupted status
            ShellOutput("", "Command execution was interrupted: ${e.localizedMessage}", -1)
        } catch (e: Exception) {
            // Catch any other unexpected exceptions
            ShellOutput("", "An unexpected error occurred: ${e.localizedMessage}", -1)
        }
    }
}
