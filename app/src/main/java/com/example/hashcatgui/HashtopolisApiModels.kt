package com.example.hashcatgui

import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: Int,
    val name: String,
    val status: String,
    val lastActivity: String
)
