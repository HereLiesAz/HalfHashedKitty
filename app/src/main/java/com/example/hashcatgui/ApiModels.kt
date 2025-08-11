package com.example.hashcatgui

import kotlinx.serialization.Serializable

@Serializable
data class AttackRequest(
    val hash: String,
    val hashType: Int,
    val wordlist: String,
    val rules: String? = null,
    val mask: String? = null
)

@Serializable
data class AttackResponse(
    val jobId: String,
    val status: String,
    val crackedPassword: String? = null
)
