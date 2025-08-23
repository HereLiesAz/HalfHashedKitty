package com.hereliesaz.halfhashedkitty

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootUtils {

    data class ShellOutput(val stdout: String, val stderr: String, val exitCode: Int)

    fun isRooted(): Boolean {
        return executeAsRoot("id").exitCode == 0
    }

    fun executeAsRoot(vararg commands: String): ShellOutput {
        return try {
            val process = Runtime.getRuntime().exec("su")

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
        } catch (e: Exception) {
            ShellOutput("", e.localizedMessage ?: "Unknown error executing root command", -1)
        }
    }
}
