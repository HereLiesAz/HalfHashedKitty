package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.Serializable

// --- Local UI Models ---

@Serializable
data class HashModeInfo(
    val mode: String,
    val name: String
)

@Serializable
data class AttackMode(
    val id: Int,
    val name: String
)

// --- API Models ---

@Serializable
data class Job(
    val id: String,
    val status: String,
    val output: String? = null,
    val error: String? = null
)