package com.hereliesaz.halfhashedkitty

import com.topjohnwu.superuser.Shell

object RootUtils {
    fun execute(command: String): Shell.Result {
        return Shell.cmd(command).exec()
    }

    fun isRooted(): Boolean {
        return Shell.getShell().isRoot
    }
}
