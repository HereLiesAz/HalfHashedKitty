package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val payload: String,
    @SerialName("room_id") val room_id: String
)

@Serializable
data class AttackParams(
    @SerialName("job_id") val jobId: String,
    val file: String,
    val mode: String,
    @SerialName("attack_mode") val attackMode: String,
    val wordlist: String? = null,
    val rules: String? = null
)

@Serializable
data class RoomInfo(
    val id: String
)

@Serializable
data class StatusUpdatePayload(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val output: String? = null,
    val error: String? = null
)

@Serializable
data class SniffStartPayload(
    val host: String,
    val username: String,
    val password: String
)

@Serializable
data class SniffOutputPayload(
    val output: String
)

@Serializable
data class HashModeInfo(
    val mode: String,
    val description: String
)

@Serializable
data class AttackMode(
    val id: Int,
    val name: String
)