package com.yunlian.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException

class DownloadSaverTest {
    @Test
    fun addsConflictNumberBeforeExtension() {
        assertEquals("movie.mp4", DownloadSaver.numberedName("movie.mp4", 0))
        assertEquals("movie (1).mp4", DownloadSaver.numberedName("movie.mp4", 1))
        assertEquals("archive.tar (2).gz", DownloadSaver.numberedName("archive.tar.gz", 2))
    }

    @Test
    fun addsConflictNumberToExtensionlessAndHiddenNames() {
        assertEquals("README (1)", DownloadSaver.numberedName("README", 1))
        assertEquals(".nomedia (1)", DownloadSaver.numberedName(".nomedia", 1))
    }

    @Test
    fun copiesChunksInGivenOrderAndReturnsWrittenSize() {
        val temp = TemporaryFolder().apply { create() }
        try {
            val first = temp.newFile("part_0").apply { writeText("ABC") }
            val second = temp.newFile("part_1").apply { writeText("DEF") }
            val output = ByteArrayOutputStream()

            val written = DownloadSaver.copySources(listOf(first, second), output)

            assertEquals(6L, written)
            assertEquals("ABCDEF", output.toString(Charsets.UTF_8.name()))
        } finally {
            temp.delete()
        }
    }

    @Test(expected = InterruptedIOException::class)
    fun stopsCopyWhenCancellationIsRequested() {
        val temp = TemporaryFolder().apply { create() }
        try {
            val source = temp.newFile("part").apply { writeBytes(ByteArray(32)) }
            DownloadSaver.copySources(listOf(source), ByteArrayOutputStream()) { true }
        } finally {
            temp.delete()
        }
    }
}
