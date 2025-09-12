package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

open class BaseApiClient {
    protected val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            // You can also customize the logger, for example, to use Android's Logcat:
            // logger = object : Logger {
            //     override fun log(message: String) {
            //         android.util.Log.v("KtorLogger", message)
            //     }
            // }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }
}
