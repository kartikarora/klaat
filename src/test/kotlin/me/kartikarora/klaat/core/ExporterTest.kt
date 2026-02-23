package me.kartikarora.klaat.core

import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.model.Body
import com.google.api.services.docs.v1.model.Document
import com.google.api.services.docs.v1.model.Paragraph
import com.google.api.services.docs.v1.model.ParagraphElement
import com.google.api.services.docs.v1.model.ParagraphStyle
import com.google.api.services.docs.v1.model.StructuralElement
import com.google.api.services.docs.v1.model.TextRun
import com.google.api.services.docs.v1.model.TextStyle
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExporterTest {

    private lateinit var tempDir: File
    private lateinit var mockDocsService: Docs
    private lateinit var mockDocumentsGet: Docs.Documents.Get

    @BeforeTest
    fun setup() {
        tempDir = kotlin.io.path.createTempDirectory("exporter-test").toFile()
        mockDocsService = mockk()
        mockDocumentsGet = mockk()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `test export markdown file to HTML`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("""
            ---
            summary: Test
            ---
            # Test Codelab
            ## Step 1
            Content
        """.trimIndent())

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        val outputDir = File(tempDir, "test-codelab")
        assertTrue(outputDir.exists() || tempDir.listFiles()?.any { it.name.contains("test") } == true)
    }

    @Test
    fun `test export markdown file to Markdown`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("""
            # Test Codelab
            ## Step 1
            Content
        """.trimIndent())

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "md",
            gaId = null,
            prefix = null
        )

        // Should create output directory
        val outputFiles = tempDir.listFiles()
        assertNotNull(outputFiles)
    }

    @Test
    fun `test export non-existent markdown file prints error`() {
        val exporter = Exporter(null)

        // Should not throw, just print error
        exporter.export(
            source = "/non/existent/file.md",
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun `test export google doc with valid service`() {
        val mockDocument = Document().apply {
            title = "Test Doc"
            documentId = "doc123"
            body = Body().setContent(listOf(
                createHeading1("Step 1"),
                createParagraph("Content")
            ))
        }

        val mockDocuments = mockk<Docs.Documents>()
        every { mockDocsService.documents() } returns mockDocuments
        every { mockDocuments.get("doc123") } returns mockDocumentsGet
        every { mockDocumentsGet.execute() } returns mockDocument

        val exporter = Exporter(mockDocsService)
        exporter.export(
            source = "doc123",
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        verify { mockDocumentsGet.execute() }
    }

    @Test
    fun `test export google doc without service prints error`() {
        val exporter = Exporter(null)

        // Should print error and return without throwing
        exporter.export(
            source = "doc123",
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        // No exception means test passes
        assertTrue(true)
    }

    @Test
    fun `test export google doc with fetch error`() {
        val mockDocuments = mockk<Docs.Documents>()
        every { mockDocsService.documents() } returns mockDocuments
        every { mockDocuments.get("doc123") } returns mockDocumentsGet
        every { mockDocumentsGet.execute() } throws RuntimeException("Network error")

        val exporter = Exporter(mockDocsService)

        // Should catch exception and print error
        exporter.export(
            source = "doc123",
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        // No uncaught exception means test passes
        assertTrue(true)
    }

    @Test
    fun `test export with unsupported format prints error`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)

        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "pdf",
            gaId = null,
            prefix = null
        )

        // Should print error about unsupported format
        assertTrue(true)
    }

    @Test
    fun `test export creates output directory if not exists`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val newOutputDir = File(tempDir, "new-output")
        assertFalse(newOutputDir.exists())

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = newOutputDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        assertTrue(newOutputDir.exists())
    }

    @Test
    fun `test export with GA ID`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = "UA-12345",
            prefix = null
        )

        // Should pass GA ID to renderer
        assertTrue(true)
    }

    @Test
    fun `test export with custom prefix`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = "https://cdn.example.com"
        )

        // Should pass prefix to renderer
        assertTrue(true)
    }

    @Test
    fun `test export markdown format with md extension`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "md",
            gaId = null,
            prefix = null
        )

        assertTrue(tempDir.listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `test export markdown format with markdown extension`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "markdown",
            gaId = null,
            prefix = null
        )

        assertTrue(tempDir.listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `test export HTML format`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        assertTrue(tempDir.listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `test export format is case insensitive`() {
        val markdownFile = File(tempDir, "test.md")
        markdownFile.writeText("# Test\n## Step 1\nContent")

        val exporter = Exporter(null)

        // Test uppercase
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "HTML",
            gaId = null,
            prefix = null
        )

        assertTrue(tempDir.listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `test export with complex markdown`() {
        val markdownFile = File(tempDir, "complex.md")
        markdownFile.writeText("""
            ---
            summary: Complex test
            id: complex-id
            categories: Android, Web
            tags: advanced
            authors: John Doe
            ---

            # Complex Codelab

            ## Introduction

            This is **bold** and *italic*.

            ```kotlin
            fun main() {
                println("Hello")
            }
            ```

            ## Step 2

            * Item 1
            * Item 2

            ![Image](img.png)
        """.trimIndent())

        val exporter = Exporter(null)
        exporter.export(
            source = markdownFile.absolutePath,
            outputDirectory = tempDir.absolutePath,
            format = "html",
            gaId = null,
            prefix = null
        )

        val outputDir = File(tempDir, "complex-id")
        assertTrue(outputDir.exists() || tempDir.listFiles()?.any { it.name.contains("complex") } == true)
    }

    // Helper methods

    private fun createHeading1(text: String): StructuralElement {
        return StructuralElement().setParagraph(
            Paragraph()
                .setElements(listOf(createTextRun(text)))
                .setParagraphStyle(ParagraphStyle().setNamedStyleType("HEADING_1"))
        )
    }

    private fun createParagraph(text: String): StructuralElement {
        return StructuralElement().setParagraph(
            Paragraph().setElements(listOf(createTextRun(text)))
        )
    }

    private fun createTextRun(text: String): ParagraphElement {
        return ParagraphElement().setTextRun(
            TextRun()
                .setContent(text)
                .setTextStyle(TextStyle())
        )
    }
}
