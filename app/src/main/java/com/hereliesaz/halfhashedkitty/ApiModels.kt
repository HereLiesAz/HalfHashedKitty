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

// --- WebSocket Communication Models ---

@Serializable
data class WebSocketMessage(
    val type: String,
    val payload: kotlinx.serialization.json.JsonElement, // Can be a JSON object or string
    val roomId: String
)

@Serializable
data class AttackParams(
    val jobId: String,
    val file: String,
    val mode: String,
    val wordlist: String,
    val rules: String? = null
)

@Serializable
data class StatusUpdatePayload(
    val jobId: String,
    val status: String,
    val output: String? = null,
    val error: String? = null
)

@Serializable
data class RoomInfo(
    val type: String,
    val id: String
)