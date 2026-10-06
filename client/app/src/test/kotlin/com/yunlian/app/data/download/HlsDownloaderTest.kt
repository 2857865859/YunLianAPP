package com.yunlian.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class HlsDownloaderTest {

    @Test
    fun `resolveUri handles root relative segment`() {
        assertEquals(
            "https://cdn.example.com/video/segment.ts",
            HlsDownloader.resolveUri("https://cdn.example.com/master/playlist/", "/video/segment.ts")
        )
    }

    @Test
    fun `resolveUri normalizes parent directory segment`() {
        assertEquals(
            "https://cdn.example.com/master/segment.ts",
            HlsDownloader.resolveUri("https://cdn.example.com/master/playlist/", "../segment.ts")
        )
    }

    @Test
    fun `resolveUri handles protocol relative segment`() {
        assertEquals(
            "https://media.example.com/segment.ts",
            HlsDownloader.resolveUri("https://cdn.example.com/master/", "//media.example.com/segment.ts")
        )
    }
}
