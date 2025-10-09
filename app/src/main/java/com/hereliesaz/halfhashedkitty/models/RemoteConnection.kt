package com.hereliesaz.halfhashedkitty.models

/**
 * A simple data class to represent a remote SSH connection target.
 *
 * @param name A user-friendly name for the connection (e.g., "pwn-pi").
 * @param connectionString The actual connection string (e.g., "user@192.168.1.100").
 */
data class RemoteConnection(
    val name: String,
    val connectionString: String
)