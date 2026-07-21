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

/**
 * End-to-end integration tests for serve command
 */
class ServeCommandTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var testHtmlFile: File

    @BeforeEach
    fun setUp() {
        // Create a test HTML file to serve
        testHtmlFile = tempDir.resolve("index.html").toFile()
        testHtmlFile.writeText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Codelab</title>
            </head>
            <body>
                <h1>Test Codelab Server</h1>
                <p>This is a test page for the serve command.</p>
            </body>
            </html>
        """.trimIndent())

        // Create a codelab directory structure
        val codelabDir = tempDir.resolve("my-codelab").toFile()
        codelabDir.mkdirs()
        codelabDir.resolve("index.html").writeText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>My Codelab</title>
            </head>
            <body>
                <h1>My Test Codelab</h1>
                <p>Codelab content here.</p>
            </body>
            </html>
        """.trimIndent())
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    fun `test serve command starts server and serves files`() {
        val port = 9091 // Use non-default port to avoid conflicts
        val host = "localhost"
        
        // Start server in a separate thread with timeout
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

            // Wait for server to start (with timeout)
            var attempts = 0
            val maxAttempts = 30 // 3 seconds max
            while (!serverStarted && attempts < maxAttempts) {
                Thread.sleep(100)
                try {
                    val url = URI("http://$host:$port/").toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 1000
                    connection.readTimeout = 1000
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        serverStarted = true
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    // Server not ready yet
                }
                attempts++
            }

            assertTrue(serverStarted, "Server should start successfully")

            // Test fetching the root index.html
            val rootUrl = URI("http://$host:$port/index.html").toURL()
            val rootConnection = rootUrl.openConnection() as HttpURLConnection
            rootConnection.requestMethod = "GET"
            val rootContent = rootConnection.inputStream.bufferedReader().use { it.readText() }
            rootConnection.disconnect()

            assertTrue(rootContent.contains("Test Codelab Server"), "Should serve root index.html")
            assertTrue(rootContent.contains("test page for the serve command"), "Should contain expected content")

            // Test fetching codelab directory
            val codelabUrl = URI("http://$host:$port/my-codelab/index.html").toURL()
            val codelabConnection = codelabUrl.openConnection() as HttpURLConnection
            codelabConnection.requestMethod = "GET"
            val codelabContent = codelabConnection.inputStream.bufferedReader().use { it.readText() }
            codelabConnection.disconnect()

            assertTrue(codelabContent.contains("My Test Codelab"), "Should serve codelab index.html")
            assertTrue(codelabContent.contains("Codelab content here"), "Should contain codelab content")

        } finally {
            // Stop the server thread
            serverThread?.interrupt()
            // Give it a moment to clean up
            Thread.sleep(500)
        }
    }

    @Test
    fun `test serve command with invalid directory`() {
        // Given an invalid directory path
        val invalidDir = tempDir.resolve("non-existent-directory").toFile()
        
        val args = arrayOf(
            "serve",
            "-d", invalidDir.absolutePath,
            "--no-browser"
        )

        // When attempting to serve
        // Then it should handle the error gracefully (no exception thrown)
        try {
            main(args)
            // If we get here, the command handled the error properly
        } catch (e: Exception) {
            // Should not throw exception, should print error message instead
            assertTrue(false, "Serve command should handle invalid directory gracefully")
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    fun `test serve command with custom port`() {
        val port = 9092
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
                    connection.connectTimeout = 1000
                    connection.readTimeout = 1000
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        serverStarted = true
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    // Server not ready yet
                }
                attempts++
            }

            assertTrue(serverStarted, "Server should start on custom port")

            // Verify we can reach the server on the custom port
            val url = URI("http://$host:$port/index.html").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            connection.disconnect()

            assertEquals(200, responseCode, "Should return 200 OK")

        } finally {
            serverThread?.interrupt()
            Thread.sleep(500)
        }
    }
}
