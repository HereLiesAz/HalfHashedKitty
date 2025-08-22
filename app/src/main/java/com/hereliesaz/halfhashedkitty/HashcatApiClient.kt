package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HashcatApiClient {

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
        // This is a placeholder implementation. The exact endpoint for hash identification
        // is unknown. This is a reasonable guess.
        return client.post("$serverUrl/identify") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("hash" to hash))
        }.body()
    }
}
