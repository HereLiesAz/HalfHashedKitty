package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.Serializable

@Serializable
data class HashModeInfo(
    val mode: String,
    val name: String
)

@Serializable
data class HashIdentificationResponse(
    val modes: List<HashModeInfo>
)

@Serializable
data class AttackRequest(
    val hash: String,
    val hashType: String, // Changed to String to match the server's expectation
    val wordlist: String,
    val rules: String? = null,
    val mask: String? = null
)

@Serializable
data class AttackResponse(
    val jobId: String,
    val status: String,
    val cracked: List<String>? = null,
    val output: String? = null,
    val error: String? = null
)

@Serializable
data class UploadResponse(
    val hash: String
)

@Serializable
data class AttackMode(
    val id: Int,
    val name: String
)