package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * A base class for API clients providing shared configuration.
 * <p>
 * This class encapsulates the common setup for Ktor HTTP clients, including:
 * <ul>
 *     <li>JSON Content Negotiation (serialization/deserialization).</li>
 *     <li>Logging configuration.</li>
 *     <li>Timeouts (Connect, Socket, Request).</li>
 * </ul>
 * Subclasses can use the protected `client` property to make requests.
 * </p>
 */
open class BaseApiClient {
    /**
     * The configured Ktor HttpClient instance.
     */
    protected val client = HttpClient(CIO) {
        // Throw exceptions for non-2xx responses automatically.
        expectSuccess = true

        // Configure JSON serialization.
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // robust against API changes.
            })
        }

        // Configure logging.
        install(Logging) {
            level = LogLevel.ALL
            // Default logger uses stdout. To use Logcat:
            // logger = object : Logger {
            //     override fun log(message: String) {
            //         android.util.Log.v("KtorLogger", message)
            //     }
            // }
        }

        // Configure timeouts.
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }
}
