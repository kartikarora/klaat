package me.kartikarora.klaat.util

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.CodelabMetadata
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.core.Step
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ImageDownloaderTest {
    private lateinit var tempDir: File
    private lateinit var outputDir: File
    private lateinit var sourceDir: File

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("klaat-downloader-test").toFile()
        outputDir = File(tempDir, "output")
        outputDir.mkdirs()
        sourceDir = File(tempDir, "source")
        sourceDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test download remote image`() {
        val codelab = Codelab(
            id = "remote-test",
            title = "Remote Test",
            steps = listOf(
                Step(
                    title = "Step 1",
                    content = listOf(
                        Node.Image(src = "https://placeholdit.com/10x10/dddddd/999999?text=Test", alt = "Remote Image")
                    )
                )
            )
        )

        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        
        val imageNode = processedCodelab.steps[0].content[0] as Node.Image
        assertTrue(imageNode.src.startsWith("images/"), "Image src should be updated to local path: ${imageNode.src}")
        
        val imageFile = File(outputDir, "remote-test/${imageNode.src}")
        assertTrue(imageFile.exists(), "Downloaded image should exist at ${imageFile.absolutePath}")
        assertTrue(imageFile.length() > 0, "Downloaded image should not be empty")
    }

    @Test
    fun `test copy local image`() {
        val imagesDir = File(sourceDir, "images")
        imagesDir.mkdirs()
        val localImage = File(imagesDir, "local.png")
        localImage.writeText("local image content")

        val codelab = Codelab(
            id = "local-test",
            title = "Local Test",
            steps = listOf(
                Step(
                    title = "Step 1",
                    content = listOf(
                        Node.Image(src = "images/local.png", alt = "Local Image")
                    )
                )
            ),
            metadata = CodelabMetadata(
                source = File(sourceDir, "test.md").absolutePath
            )
        )
        File(sourceDir, "test.md").writeText("# Test")

        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        
        val imageNode = processedCodelab.steps[0].content[0] as Node.Image
        assertEquals("images/local.png", imageNode.src)
        
        val imageFile = File(outputDir, "local-test/images/local.png")
        assertTrue(imageFile.exists(), "Copied image should exist")
        assertEquals("local image content", imageFile.readText())
    }

    @Test
    fun `test download poster image`() {
        val codelab = Codelab(
            id = "image-test",
            title = "Poster Test",
            steps = emptyList(),
            metadata = CodelabMetadata(
                image = "https://placeholdit.com/10x10/dddddd/999999?text=Poster"
            )
        )

        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        
        assertTrue(processedCodelab.metadata.image.startsWith("images/"), "Poster src should be updated")
        val posterFile = File(outputDir, "image-test/${processedCodelab.metadata.image}")
        assertTrue(posterFile.exists(), "Downloaded image should exist")
    }

    @Test
    fun `test data URI image is preserved without downloading or copying`() {
        val dataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        val codelab = Codelab(
            id = "data-uri-test",
            title = "Data URI Test",
            steps = listOf(
                Step(
                    title = "Step 1",
                    content = listOf(
                        Node.Image(src = dataUri, alt = "Data URI Image")
                    )
                )
            ),
            metadata = CodelabMetadata(
                source = File(sourceDir, "test.md").absolutePath,
                image = dataUri
            )
        )
        File(sourceDir, "test.md").writeText("# Test")

        val processedCodelab = ImageDownloader.processImages(codelab, outputDir)
        val imageNode = processedCodelab.steps[0].content[0] as Node.Image
        assertEquals(dataUri, imageNode.src)
        assertEquals(dataUri, processedCodelab.metadata.image)
    }
}
