package com.hereliesaz.halfhashedkitty

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.jsoup.Jsoup

/**
 * API client for interacting with the hashcat.net/cap2hashcat/ service.
 * <p>
 * This service converts .pcap/.pcapng capture files containing WPA handshakes
 * into the .hc22000 format required by modern Hashcat versions.
 * </p>
 */
class Cap2HashcatApiClient {

    companion object {
        /** The URL of the conversion service. */
        private const val CAP2HASHCAT_URL = "https://hashcat.net/cap2hashcat/"
    }

    /** Dedicated HTTP client for this service. */
    private val client = HttpClient(CIO)

    /**
     * Closes the underlying HTTP client resources.
     */
    fun close() {
        client.close()
    }

    /**
     * Uploads a PCAP file to the conversion service and retrieves the converted hash string.
     *
     * @param file The byte array content of the .pcap file.
     * @return The converted hash string (or status message) extracted from the response.
     * @throws Exception if upload or parsing fails.
     */
    suspend fun uploadPcapngFile(file: ByteArray): String {
        // Perform a multipart POST request.
        val response = client.post(CAP2HASHCAT_URL) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        // Append the file part.
                        append("pcap", file, Headers.build {
                            append(HttpHeaders.ContentType, "application/vnd.tcpdump.pcap")
                            append(HttpHeaders.ContentDisposition, "filename=\"capture.pcapng\"")
                        })
                    }
                )
            )
        }
        val responseBody = response.body<String>()

        // The service returns an HTML page. We must parse it to find the output.
        // The output is typically inside a <textarea> element.
        val doc = Jsoup.parse(responseBody)
        val textarea = doc.select("textarea").firstOrNull()

        return if (textarea != null) {
            // Success: Return the content of the textarea.
            textarea.text().trim()
        } else {
            // Failure: Return empty string (or ideally parse error message from HTML).
            ""
        }
    }
}
