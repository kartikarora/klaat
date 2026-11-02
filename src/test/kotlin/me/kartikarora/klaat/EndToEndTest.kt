package me.kartikarora.klaat

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for klaat CLI
 */
class EndToEndTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var testMarkdownFile: File
    private lateinit var outputDir: File

    @BeforeEach
    fun setUp() {
        outputDir = tempDir.resolve("output").toFile()
        outputDir.mkdirs()

        // Create test markdown file
        testMarkdownFile = tempDir.resolve("test-codelab.md").toFile()
        testMarkdownFile.writeText("""
            ---
            id: test-codelab-id
            summary: This is a test codelab for E2E testing
            categories: Testing, Kotlin
            environments: Web
            status: Published
            authors: Test Author
            tags: test, e2e
            feedback_link: https://example.com/feedback
            ---
            
            # Test Codelab Title
            
            ## Step 1: Introduction
            Duration: 5
            
            Welcome to the test codelab! This step includes:
            
            * **Bold text**
            * *Italic text*
            * A [link](https://example.com)
            
            ### Code Example
            
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
            
            ## Step 2: Advanced Features
            Duration: 10
            
            This step demonstrates more features.
            
            #### Positive Info Box
            
            This is important information!
            
            #### Negative Info Box
            
            This is a warning!
            
            ### List Examples
            
            1. First item
            2. Second item
            3. Third item
            
            ## Step 3: Conclusion
            Duration: 3
            
            You've completed the test codelab.
            
            ### Next Steps
            
            - Try creating your own codelab
            - Explore more features
            - Share your codelabs
        """.trimIndent())
    }

    @AfterEach
    fun tearDown() {
        // Cleanup is handled by @TempDir
    }

    @Test
    fun `test export markdown to HTML`() {
        // Given a markdown file
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html"
        )

        // When exporting
        main(args)

        // Then HTML output should be created
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")
        assertTrue(codelabDir.isDirectory, "Codelab directory should be a directory")

        val indexFile = codelabDir.resolve("index.html")
        assertTrue(indexFile.exists(), "index.html should exist")

        // Verify content
        val content = indexFile.readText()
        assertTrue(content.contains("Test Codelab Title"), "Should contain codelab title")
        assertTrue(content.contains("Step 1: Introduction"), "Should contain step 1 title")
        assertTrue(content.contains("Step 2: Advanced Features"), "Should contain step 2 title")
        assertTrue(content.contains("Step 3: Conclusion"), "Should contain step 3 title")
        assertTrue(content.contains("Hello, World!"), "Should contain code content")
    }

    @Test
    fun `test export markdown to Markdown format`() {
        // Given a markdown file
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "md"
        )

        // When exporting
        main(args)

        // Then Markdown output should be created
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")

        val mdFile = codelabDir.resolve("index.md")
        assertTrue(mdFile.exists(), "index.md should exist")

        // Verify content
        val content = mdFile.readText()
        assertTrue(content.contains("Test Codelab Title"), "Should contain codelab title")
        assertTrue(content.contains("Step 1: Introduction"), "Should contain step 1")
    }

    @Test
    fun `test export with custom Google Analytics ID`() {
        // Given a markdown file and GA ID
        val gaId = "UA-12345-67"
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html",
            "-ga", gaId
        )

        // When exporting
        main(args)

        // Then HTML should contain GA ID
        val indexFile = outputDir.resolve("test-codelab-id/index.html")
        val content = indexFile.readText()
        assertTrue(content.contains(gaId), "Should contain custom GA ID")
    }

    @Test
    fun `test export with custom asset prefix`() {
        // Given a markdown file and custom prefix
        val prefix = "https://custom-cdn.example.com"
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html",
            "-prefix", prefix
        )

        // When exporting
        main(args)

        // Then HTML should use custom prefix for assets
        val indexFile = outputDir.resolve("test-codelab-id/index.html")
        val content = indexFile.readText()
        assertTrue(content.contains(prefix), "Should contain custom asset prefix")
    }

    @Test
    fun `test export offline format`() {
        // Given a markdown file
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "offline"
        )

        // When exporting
        main(args)

        // Then offline HTML with embedded assets should be created
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")

        val indexFile = codelabDir.resolve("index.html")
        assertTrue(indexFile.exists(), "index.html should exist")

        // Verify content
        val content = indexFile.readText()
        assertTrue(content.contains("Test Codelab Title"), "Should contain codelab title")
    }

    @Test
    fun `test export multiple codelabs`() {
        // Given multiple markdown files
        val testFile2 = tempDir.resolve("test-codelab-2.md").toFile()
        testFile2.writeText("""
            ---
            id: test-codelab-2
            summary: Second test codelab
            categories: Testing
            environments: Web
            status: Published
            authors: Test Author
            ---
            
            # Second Codelab
            
            ## Step 1
            Duration: 2
            
            Content for second codelab.
        """.trimIndent())

        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            testFile2.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html"
        )

        // When exporting
        main(args)

        // Then both codelabs should be created
        val codelab1Dir = outputDir.resolve("test-codelab-id")
        val codelab2Dir = outputDir.resolve("test-codelab-2")

        assertTrue(codelab1Dir.exists(), "First codelab should exist")
        assertTrue(codelab2Dir.exists(), "Second codelab should exist")
        assertTrue(codelab1Dir.resolve("index.html").exists(), "First codelab index.html should exist")
        assertTrue(codelab2Dir.resolve("index.html").exists(), "Second codelab index.html should exist")
    }

    @Test
    fun `test export with extra metadata`() {
        // Given a markdown file with extra metadata
        val extraJson = """{"custom_field":"custom_value","another_field":"another_value"}"""
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html",
            "-extra", extraJson
        )

        // When exporting
        main(args)

        // Then export should succeed
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")
        assertTrue(codelabDir.resolve("index.html").exists(), "index.html should exist")
    }

    @Test
    fun `test version command`() {
        // Given version command
        val args = arrayOf("version")

        // When executing
        // Note: This will print to stdout, we just verify it doesn't crash
        main(args)

        // Then command should execute successfully (no exception)
    }

    @Test
    fun `test export with pass_metadata flag`() {
        // Given a markdown file with pass_metadata
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html",
            "-pass_metadata", "custom_field,another_field"
        )

        // When exporting
        main(args)

        // Then export should succeed
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")
        assertTrue(codelabDir.resolve("index.html").exists(), "index.html should exist")
    }

    @Test
    fun `test export with environment flag`() {
        // Given a markdown file with environment
        val args = arrayOf(
            "export",
            testMarkdownFile.absolutePath,
            "-o", outputDir.absolutePath,
            "-f", "html",
            "-e", "kiosk"
        )

        // When exporting
        main(args)

        // Then export should succeed
        val codelabDir = outputDir.resolve("test-codelab-id")
        assertTrue(codelabDir.exists(), "Codelab directory should exist")

        val indexFile = codelabDir.resolve("index.html")
        assertTrue(indexFile.exists(), "index.html should exist")

        // Verify environment is reflected in output
        val content = indexFile.readText()
        assertTrue(content.contains("kiosk") || content.contains("environment"), "Should reference environment")
    }

    @Test
    fun `test completion command generates shell completions`() {
        val originalOut = System.out
        val outputStream = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(outputStream))
        try {
            main(arrayOf("completion", "zsh"))
            val output = outputStream.toString()
            assertTrue(output.contains("compdef") || output.contains("klaat"), "Should generate zsh completion script")
        } finally {
            System.setOut(originalOut)
        }
    }
}
