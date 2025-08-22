package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class Cap2HashcatApiClient {

    private val client = HttpClient(CIO)

    suspend fun uploadPcapngFile(file: ByteArray): String {
        val response = client.post("https://hashcat.net/cap2hashcat/") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("pcap", file, Headers.build {
                            append(HttpHeaders.ContentType, "application/vnd.tcpdump.pcap")
                            append(HttpHeaders.ContentDisposition, "filename=\"capture.pcapng\"")
                        })
                    }
                )
            )
        }
        // The response is an HTML page with the hash inside a <textarea>
        val responseBody = response.body<String>()
        // A simple and brittle way to extract the hash.
        // A better solution would use a proper HTML parser.
        val hash = responseBody.substringAfter("<textarea>", "").substringBefore("</textarea>", "")
        return hash.trim()
    }
}
