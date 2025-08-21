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

    suspend fun getAgents(serverUrl: String, apiKey: String): List<Agent> {
        // TODO: Find the correct endpoint for getting agents
        // This is a placeholder implementation
        return client.get("$serverUrl/api/v2/agents") {
            header("Authorization", "Bearer $apiKey")
        }.body()
    }
}
