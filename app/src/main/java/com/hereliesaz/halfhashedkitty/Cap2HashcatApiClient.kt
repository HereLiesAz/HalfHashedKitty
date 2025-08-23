package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.jsoup.Jsoup

class Cap2HashcatApiClient {

    private val client = HttpClient(CIO)

    fun close() {
        client.close()
    }

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
        val responseBody = response.body<String>()

        // Use Jsoup to reliably parse the HTML response
        val doc = Jsoup.parse(responseBody)
        val hash = doc.select("textarea").text()

        return hash.trim()
    }
}
