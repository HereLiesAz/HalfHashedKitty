package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi @Serializable
data class HashModeInfo(
    val id: Int,
    val name: String
)

@InternalSerializationApi @Serializable
data class HashIdentificationResponse(
    val hashModes: List<HashModeInfo>
)

@InternalSerializationApi @Serializable
data class AttackRequest(
    val hash: String,
    val hashType: Int,
    val wordlist: String,
    val rules: String? = null,
    val mask: String? = null
)

@InternalSerializationApi @Serializable
data class AttackResponse(
    val jobId: String,
    val status: String,
    val crackedPassword: String? = null
)

@InternalSerializationApi @Serializable
data class UploadResponse(
    val hash: String
)

@InternalSerializationApi @Serializable
data class AttackMode(
    val id: Int,
    val name: String
)
