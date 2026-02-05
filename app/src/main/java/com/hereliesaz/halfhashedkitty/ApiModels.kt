package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard WebSocket Message Envelope.
 * Represents the top-level structure of all messages exchanged with the relay server.
 *
 * @property type The type of message (e.g., "join", "attack", "status_update", "cracked").
 * @property payload The raw string payload (often a nested JSON string) specific to the message type.
 * @property room_id The unique room ID this message targets or originates from.
 */
@Serializable
data class WebSocketMessage(
    val type: String,
    val payload: String,
    @SerialName("room_id") val room_id: String
)

/**
 * Payload for initiating an attack job.
 * Serialized into JSON and sent as the payload of an "attack" message.
 *
 * @property jobId Unique identifier for tracking this job.
 * @property file Path to the hash file (on the desktop machine).
 * @property mode Hashcat hash mode ID (e.g., "0" for MD5).
 * @property attackMode Hashcat attack mode ID (e.g., "0" for Dictionary, "3" for Mask).
 * @property wordlist Path to the wordlist file (optional).
 * @property rules Path to the rules file (optional).
 */
@Serializable
data class AttackParams(
    @SerialName("job_id") val jobId: String,
    val file: String,
    val mode: String,
    @SerialName("attack_mode") val attackMode: String,
    val wordlist: String? = null,
    val rules: String? = null
)

/**
 * Payload received when confirming a room join.
 *
 * @property id The room ID joined.
 */
@Serializable
data class RoomInfo(
    val id: String
)

/**
 * Payload for status updates from the desktop worker.
 *
 * @property jobId The job ID this update refers to.
 * @property status Current status (e.g., "running", "completed", "failed").
 * @property output Optional stdout text from the process.
 * @property error Optional stderr text or error message.
 */
@Serializable
data class StatusUpdatePayload(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val output: String? = null,
    val error: String? = null
)

/**
 * Payload for starting a remote sniff session.
 *
 * @property host The IP/Hostname of the target.
 * @property username SSH username.
 * @property password SSH password.
 */
@Serializable
data class SniffStartPayload(
    val host: String,
    val username: String,
    val password: String
)

/**
 * Payload for sniffing output updates.
 *
 * @property output The captured text output (tcpdump logs).
 */
@Serializable
data class SniffOutputPayload(
    val output: String
)

/**
 * Data model for a Hashcat Hash Mode (UI helper).
 *
 * @property mode The numeric ID (e.g., "0").
 * @property description The human-readable name (e.g., "MD5").
 */
@Serializable
data class HashModeInfo(
    val mode: String,
    val description: String
)

/**
 * Data model for a Hashcat Attack Mode (UI helper).
 *
 * @property id The numeric ID (e.g., 0, 3).
 * @property name The human-readable name (e.g., "Straight", "Brute-force").
 */
@Serializable
data class AttackMode(
    val id: Int,
    val name: String
)
