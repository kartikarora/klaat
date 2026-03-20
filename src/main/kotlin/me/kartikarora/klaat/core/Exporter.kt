package me.kartikarora.klaat.core

import com.google.api.services.docs.v1.Docs
import me.kartikarora.klaat.parser.GoogleDocParser
import me.kartikarora.klaat.parser.MarkdownParser
import me.kartikarora.klaat.renderer.HtmlRenderer
import me.kartikarora.klaat.renderer.MarkdownRenderer
import me.kartikarora.klaat.renderer.OfflineRenderer
import me.kartikarora.klaat.util.ImageDownloader
import java.io.File

/**
 * Main exporter for codelabs from various sources
 */
class Exporter(private val docsService: Docs?) {

    private val docParser = GoogleDocParser()
    private val markdownParser = MarkdownParser()
    private val htmlRenderer = HtmlRenderer()
    private val markdownRenderer = MarkdownRenderer()
    private val offlineRenderer = OfflineRenderer()

    /**
     * Extract document ID from a Google Docs URL or return the input if it's already an ID
     */
    private fun extractDocumentId(source: String): String {
        // Pattern: https://docs.google.com/document/d/DOCUMENT_ID/...
        val regex = Regex("docs\\.google\\.com/document/d/([a-zA-Z0-9_-]+)")
        val match = regex.find(source)
        return match?.groupValues?.get(1) ?: source
    }

    /**
     * Export a codelab from a source (Google Doc ID or Markdown file path)
     */
    fun export(
        source: String,
        outputDirectory: String,
        format: String,
        gaId: String?,
        prefix: String?,
        environment: String = "web",
        passMetadata: Set<String> = emptySet(),
        extraMetadata: Map<String, String> = emptyMap()
    ) {
        val codelab = when {
            source.endsWith(".md") -> {
                // Parse markdown file
                val file = File(source)
                if (!file.exists()) {
                    println("✗ Error: File not found: $source")
                    return
                }
                markdownParser.parse(file, passMetadata)
            }
            else -> {
                // Assume it's a Google Doc ID or URL
                if (docsService == null) {
                    println("✗ Error: Google Docs service not initialized")
                    return
                }
                val documentId = extractDocumentId(source)
                val document = try {
                    docsService.documents().get(documentId).execute()
                } catch (e: Exception) {
                    println("✗ Error fetching document: ${e.message}")
                    return
                }
                docParser.parse(document, passMetadata)
            }
        }

        // Merge extra metadata into the codelab metadata
        val updatedCodelab = if (extraMetadata.isNotEmpty()) {
            codelab.copy(
                metadata = codelab.metadata.copy(
                    extraMetadata = codelab.metadata.extraMetadata + extraMetadata
                )
            )
        } else {
            codelab
        }

        // Render based on format
        val outputDir = File(outputDirectory)
        outputDir.mkdirs()

        // Process images (download remote ones and copy local ones)
        val finalCodelab = ImageDownloader.processImages(updatedCodelab, outputDir)

        when (format.lowercase()) {
            "html" -> htmlRenderer.render(finalCodelab, outputDir, gaId, prefix, environment)
            "md", "markdown" -> markdownRenderer.render(finalCodelab, outputDir, environment)
            "offline" -> offlineRenderer.render(finalCodelab, outputDir, gaId, prefix, environment)
            else -> {
                println("✗ Error: Unsupported format: $format. Use 'html', 'md', or 'offline'")
            }
        }
    }
}