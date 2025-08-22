package com.example.hashcatgui

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HashtopolisApiClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getAgents(
        serverUrl: String,
        apiKey: String,
        agentsEndpoint: String = "/api/v2/agents"
    ): List<Agent> {
        // The endpoint is configurable via agentsEndpoint parameter.
        // Error handling is added to prevent failures if the endpoint is incorrect.
        return try {
            client.get("$serverUrl$agentsEndpoint") {
                header("Authorization", "Bearer $apiKey")
            }.body()
        } catch (e: Exception) {
            // Log the error and return an empty list if the endpoint is incorrect or request fails
            // You may want to use a logging framework here
            println("Error fetching agents: ${e.message}")
            emptyList()
        }
    }
}
