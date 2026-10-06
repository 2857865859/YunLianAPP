package com.yunlian.app.data.download

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ChunkDownloaderTest {
    private lateinit var server: MockWebServer
    private lateinit var tempDir: File
    private lateinit var downloader: ChunkDownloader

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tempDir = Files.createTempDirectory("chunk-downloader-test").toFile()
        downloader = ChunkDownloader { OkHttpClient() }
    }

    @After
    fun tearDown() {
        server.shutdown()
        tempDir.deleteRecursively()
    }

    @Test
    fun freshDownloadAccepts200() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ABCDEF"))
        val file = File(tempDir, "part"); var progress = 0L

        val ok = downloader.downloadFull(1, server.url("/file").toString(), file, emptyMap(), 6) { progress += it }

        assertTrue(ok)
        assertEquals("ABCDEF", file.readText())
        assertEquals(6L, progress)
        assertEquals(null, server.takeRequest().getHeader("Range"))
    }

    @Test
    fun resumeAppendsValid206Response() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(206).addHeader("Content-Range", "bytes 3-5/6").setBody("DEF"))
        val file = File(tempDir, "part").apply { writeText("ABC") }

        val ok = downloader.downloadFull(2, server.url("/file").toString(), file, emptyMap(), 6) {}

        assertTrue(ok)
        assertEquals("ABCDEF", file.readText())
        assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
    }

    @Test
    fun resumeRestartsWhenServerIgnoresRange() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ABCDEF"))
        val file = File(tempDir, "part").apply { writeText("ABC") }; var progress = 3L

        val ok = downloader.downloadFull(
            3,
            server.url("/file").toString(),
            file,
            emptyMap(),
            6,
            onReset = { progress = 0 }
        ) { progress += it }

        assertTrue(ok)
        assertEquals("ABCDEF", file.readText())
        assertEquals(6L, progress)
        assertEquals("bytes=3-", server.takeRequest().getHeader("Range"))
    }

    @Test
    fun interruptedResponseReturnsFalseWithoutPassingLengthCheck() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(206)
                .addHeader("Content-Range", "bytes 3-5/6")
                .setBody("DEF")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )
        val file = File(tempDir, "part").apply { writeText("ABC") }

        val ok = downloader.downloadFull(4, server.url("/file").toString(), file, emptyMap(), 6) {}

        assertFalse(ok)
        assertTrue(file.length() < 6L)
    }
}
