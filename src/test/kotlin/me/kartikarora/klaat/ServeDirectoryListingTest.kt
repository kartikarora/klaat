package me.kartikarora.klaat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServeDirectoryListingTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // Create a directory without index.html
        val emptyDir = tempDir.resolve("no-index").toFile()
        emptyDir.mkdirs()
        emptyDir.resolve("some-file.txt").writeText("Hello World")
        
        // Create another directory with a subdirectory
        val subDir = emptyDir.resolve("sub-folder")
        subDir.mkdirs()
        subDir.resolve("another-file.md").writeText("# Markdown File")
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    fun `test serve command shows directory listing when index html is missing`() {
        val port = 9094
        val host = "localhost"
        
        var serverThread: Thread? = null
        var serverStarted = false
        
        try {
            serverThread = thread {
                val args = arrayOf(
                    "serve",
                    "-addr", "$host:$port",
                    "-d", tempDir.toFile().absolutePath,
                    "--no-browser"
                )
                main(args)
            }

            // Wait for server to start
            var attempts = 0
            val maxAttempts = 30
            while (!serverStarted && attempts < maxAttempts) {
                Thread.sleep(100)
                try {
                    val url = URI("http://$host:$port/").toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    val responseCode = connection.responseCode
                    // Returns 200 for root directory listing
                    if (responseCode == 200) {
                        serverStarted = true
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                }
                attempts++
            }

            assertTrue(serverStarted, "Server should start")

            // Test fetching directory without index.html
            val dirUrl = URI("http://$host:$port/no-index/").toURL()
            val connection = dirUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            val content = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
            connection.disconnect()

            assertEquals(200, responseCode, "Should return 200 OK for directory listing")
            assertTrue(content.contains("some-file.txt"), "Should contain 'some-file.txt' in directory listing")
            assertTrue(content.contains("sub-folder"), "Should contain 'sub-folder' in directory listing")

        } finally {
            serverThread?.interrupt()
            Thread.sleep(500)
        }
    }
}
