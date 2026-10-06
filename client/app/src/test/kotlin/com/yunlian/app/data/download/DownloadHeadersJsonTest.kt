package com.yunlian.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadHeadersJsonTest {
    @Test
    fun roundTripsSensitiveDownloadHeaders() {
        val headers = linkedMapOf(
            "Cookie" to "__puus=a=b; token=secret",
            "Referer" to "https://pan.example/path?q=1",
            "User-Agent" to "yunlian Test"
        )

        assertEquals(headers, DownloadHeadersJson.decode(DownloadHeadersJson.encode(headers)))
    }
}
