package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.CodelabMetadata
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.core.Step
import me.kartikarora.klaat.util.ImageDownloader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ImageCopyTest {
    private lateinit var tempDir: File
    private lateinit var outputDir: File
    private lateinit var sourceDir: File
    private val htmlRenderer = HtmlRenderer()

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("klaat-test").toFile()
        outputDir = File(tempDir, "output")
        sourceDir = File(tempDir, "source")
        sourceDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test render copies local images`() {
        // Create a dummy image file
        val imagesDir = File(sourceDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "test.png")
        imageFile.writeText("dummy image content")

        val codelab = Codelab(
            id = "test-codelab",
            title = "Test Codelab",
            steps = listOf(
                Step(
                    title = "Step 1",
                    content = listOf(
                        Node.Image(src = "images/test.png", alt = "Test Image")
                    )
                )
            ),
            metadata = CodelabMetadata(
                source = File(sourceDir, "test.md").absolutePath,
                summary = "Test summary"
            )
        )

        File(sourceDir, "test.md").writeText("# Test") // Make sure source file exists

        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        htmlRenderer.render(processedCodelab, outputDir)

        val exportedImage = File(outputDir, "test-codelab/images/test.png")
        assertTrue(exportedImage.exists(), "Exported image should exist at ${exportedImage.absolutePath}")
        assertTrue(exportedImage.readText() == "dummy image content")
    }

    @Test
    fun `test render copies local images in nested nodes`() {
        // Create a dummy image file
        val imagesDir = File(sourceDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "nested.png")
        imageFile.writeText("nested image content")

        val codelab = Codelab(
            id = "nested-test",
            title = "Nested Test",
            steps = listOf(
                Step(
                    title = "Step 1",
                    content = listOf(
                        Node.BulletList(
                            listOf(
                                listOf(Node.Image(src = "images/nested.png", alt = "Nested Image"))
                            )
                        )
                    )
                )
            ),
            metadata = CodelabMetadata(
                source = File(sourceDir, "nested.md").absolutePath,
                summary = "Test summary"
            )
        )

        File(sourceDir, "nested.md").writeText("# Nested")
        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        htmlRenderer.render(processedCodelab, outputDir)

        val exportedImage = File(outputDir, "nested-test/images/nested.png")
        assertTrue(exportedImage.exists(), "Exported nested image should exist")
    }
}
