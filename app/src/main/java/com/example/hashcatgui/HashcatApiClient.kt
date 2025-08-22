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
}
