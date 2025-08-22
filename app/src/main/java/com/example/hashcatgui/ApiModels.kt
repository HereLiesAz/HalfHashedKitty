package com.example.hashcatgui

import kotlinx.serialization.Serializable

@Serializable
data class AttackRequest(
    val hash: String,
    val hashType: Int,
    val attackMode: Int,
    val wordlist: String? = null,
    val rules: String? = null,
    val mask: String? = null,
    val force: Boolean = false
)

@Serializable
data class AttackResponse(
    val jobId: String,
    val status: String,
    val crackedPassword: String? = null
)

@Serializable
data class HashIdentificationResponse(
    val hashModes: List<HashMode>
)

@Serializable
data class UploadResponse(
    val hash: String
)
