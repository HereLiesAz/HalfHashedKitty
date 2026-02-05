package com.hereliesaz.halfhashedkitty

import java.io.DataOutputStream
import java.io.File

/**
 * Utility object for handling root access and executing shell commands on the Android device.
 * <p>
 * This class provides methods to check for root availability and to execute shell commands
 * with elevated privileges using the `su` binary. This is primarily used for advanced features
 * like direct packet capture or system-level tool installation.
 * </p>
 */
object RootUtils {

    /**
     * Data class to hold the result of a shell command execution.
     *
     * @property stdout The standard output stream content.
     * @property stderr The standard error stream content.
     * @property exitCode The process exit code (0 usually means success).
     */
    data class ShellOutput(val stdout: String, val stderr: String, val exitCode: Int)

    /**
     * Checks if the device appears to be rooted by verifying access to the root shell.
     *
     * @return true if `id` command returns 0 (success) when run as root, false otherwise.
     */
    fun isRooted(): Boolean {
        // Attempt to run a simple command ('id') as root.
        return executeAsRoot("id").exitCode == 0
    }

    /**
     * Locates the `su` binary in common system paths.
     *
     * @return The absolute path to the `su` binary, or null if not found.
     */
    private fun findSuBinary(): String? {
        val paths = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return path
        }
        return null
    }

    /**
     * Executes one or more commands in a root shell.
     *
     * @param commands Varargs of command strings to execute sequentially.
     * @return A {@link ShellOutput} object containing the results.
     */
    fun executeAsRoot(vararg commands: String): ShellOutput {
        // Locate the su binary.
        val suPath = findSuBinary()
        if (suPath == null) {
            return ShellOutput("", "Could not find 'su' binary on this device.", -1)
        }

        return try {
            // Start the root shell process.
            val process = Runtime.getRuntime().exec(suPath)

            // Write commands to the process's standard input.
            DataOutputStream(process.outputStream).use { os ->
                for (command in commands) {
                    os.writeBytes("$command\n")
                    os.flush()
                }
                // Send 'exit' to close the shell after commands are done.
                os.writeBytes("exit\n")
                os.flush()
            }

            // Read stdout and stderr.
            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            // Wait for process to terminate and get exit code.
            val exitCode = process.waitFor()

            ShellOutput(stdout, stderr, exitCode)
        } catch (e: java.io.IOException) {
            ShellOutput("", "I/O error executing root command: ${e.localizedMessage}", -1)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Restore interrupted status.
            ShellOutput("", "Command execution was interrupted: ${e.localizedMessage}", -1)
        } catch (e: Exception) {
            // Catch-all for other runtime exceptions.
            ShellOutput("", "An unexpected error occurred: ${e.localizedMessage}", -1)
        }
    }
}
