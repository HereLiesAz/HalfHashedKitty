package com.hereliesaz.halfhashedkitty

import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: Int,
    val name: String,
    val status: String,
    val lastActivity: String
)
