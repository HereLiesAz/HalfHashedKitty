package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.jsoup.Jsoup

class Cap2HashcatApiClient {

    companion object {
        private const val CAP2HASHCAT_URL = "https://hashcat.net/cap2hashcat/"
    }

    private val client = HttpClient(CIO)

    fun close() {
        client.close()
    }

    suspend fun uploadPcapngFile(file: ByteArray): String {
        val response = client.post(CAP2HASHCAT_URL) {
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
        val textarea = doc.select("textarea").first() // Select the first textarea

        return if (textarea != null) {
            textarea.text().trim()
        } else {
            // If the textarea is not found, return empty string or throw an exception
            ""
        }
    }
}
