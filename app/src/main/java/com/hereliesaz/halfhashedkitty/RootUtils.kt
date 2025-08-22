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
            val os = DataOutputStream(process.outputStream)
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            for (command in commands) {
                os.writeBytes("$command\n")
                os.flush()
            }

            os.writeBytes("exit\n")
            os.flush()
            os.close()

            val stdout = stdoutReader.readText()
            val stderr = stderrReader.readText()

            val exitCode = process.waitFor()

            stdoutReader.close()
            stderrReader.close()

            ShellOutput(stdout, stderr, exitCode)
        } catch (e: Exception) {
            ShellOutput("", e.message ?: "Error executing root command", -1)
        }
    }
}
