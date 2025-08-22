package com.example.hashcatgui

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
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

    suspend fun startAttack(serverUrl: String, request: AttackRequest): AttackResponse {
        return client.post("$serverUrl/attack") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getAttackStatus(serverUrl: String, jobId: String): AttackResponse {
        return client.get("$serverUrl/attack/$jobId").body()
    }

    suspend fun identifyHash(serverUrl: String, hash: String): HashIdentificationResponse {
        return client.post("$serverUrl/identify") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("hash" to hash))
        }.body()
    }

    suspend fun uploadZipFile(serverUrl: String, file: ByteArray): UploadResponse {
        return client.post("$serverUrl/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", file, Headers.build {
                            append(HttpHeaders.ContentType, "application/zip")
                            append(HttpHeaders.ContentDisposition, "filename=\"hash.zip\"")
                        })
                    }
                )
            )
        }.body()
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
