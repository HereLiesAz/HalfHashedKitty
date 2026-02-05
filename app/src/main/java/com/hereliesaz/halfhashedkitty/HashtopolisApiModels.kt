package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.Serializable

/**
 * Data model representing a Hashtopolis Agent.
 * <p>
 * This class corresponds to the JSON structure returned by the Hashtopolis API for agent details.
 * </p>
 *
 * @property id The unique numeric ID of the agent.
 * @property name The display name of the agent.
 * @property status The current status (e.g., "Active", "Inactive").
 * @property lastActivity Timestamp or string description of the last time the agent checked in.
 */
@Serializable
data class Agent(
    val id: Int,
    val name: String,
    val status: String,
    val lastActivity: String
)
