package me.kartikarora.klaat.util

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.Node
import java.io.File
import java.net.URL
import java.util.UUID

object ImageDownloader {

    /**
     * Processes all images in a codelab, downloading remote ones and copying local ones to the output directory.
     * Returns a new Codelab object with updated image paths.
     */
    fun processImages(codelab: Codelab, outputDir: File): Codelab {
        val codelabDir = File(outputDir, codelab.id)
        val imagesDir = File(codelabDir, "images")
        imagesDir.mkdirs()

        val sourceBaseDir = if (codelab.metadata.source.isNotEmpty()) {
            val sourceFile = File(codelab.metadata.source)
            if (sourceFile.exists()) sourceFile.absoluteFile.parentFile else null
        } else null

        val updatedSteps = codelab.steps.map { step ->
            step.copy(content = processNodes(step.content, imagesDir, sourceBaseDir))
        }

        // Also process the image image if it's a URL
        var updatedMetadata = codelab.metadata
        if (codelab.metadata.image.isNotEmpty()) {
            val posterUrl = codelab.metadata.image
            if (posterUrl.startsWith("http://") || posterUrl.startsWith("https://")) {
                val localPosterPath = downloadImage(posterUrl, imagesDir)
                if (localPosterPath != null) {
                    updatedMetadata = updatedMetadata.copy(image = "images/$localPosterPath")
                }
            } else if (sourceBaseDir != null) {
                val sourceImage = File(sourceBaseDir, posterUrl)
                if (sourceImage.exists()) {
                    val targetImage = File(codelabDir, posterUrl)
                    targetImage.parentFile.mkdirs()
                    sourceImage.copyTo(targetImage, overwrite = true)
                }
            }
        }

        return codelab.copy(steps = updatedSteps, metadata = updatedMetadata)
    }

    private fun processNodes(nodes: List<Node>, imagesDir: File, sourceBaseDir: File?): List<Node> {
        return nodes.map { node ->
            when (node) {
                is Node.Image -> {
                    val src = node.src
                    if (src.startsWith("http://") || src.startsWith("https://")) {
                        // Download remote image
                        val localName = downloadImage(src, imagesDir)
                        if (localName != null) {
                            node.copy(src = "images/$localName")
                        } else {
                            node
                        }
                    } else if (sourceBaseDir != null) {
                        // Copy local image
                        val sourceImage = File(sourceBaseDir, src)
                        if (sourceImage.exists()) {
                            val targetImage = File(imagesDir.parentFile, src)
                            targetImage.parentFile.mkdirs()
                            sourceImage.copyTo(targetImage, overwrite = true)
                        }
                        node
                    } else {
                        node
                    }
                }
                is Node.Paragraph -> node.copy(nodes = processNodes(node.nodes, imagesDir, sourceBaseDir))
                is Node.InfoBox -> node.copy(nodes = processNodes(node.nodes, imagesDir, sourceBaseDir))
                is Node.BulletList -> node.copy(items = node.items.map { processNodes(it, imagesDir, sourceBaseDir) })
                is Node.NumberedList -> node.copy(items = node.items.map { processNodes(it, imagesDir, sourceBaseDir) })
                is Node.Table -> node.copy(
                    header = node.header.map { processNodes(it, imagesDir, sourceBaseDir) },
                    rows = node.rows.map { row -> row.map { processNodes(it, imagesDir, sourceBaseDir) } }
                )
                else -> node
            }
        }
    }

    private fun downloadImage(urlStr: String, imagesDir: File): String? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.connect()
            
            val contentType = connection.contentType ?: ""
            val extension = when {
                contentType.contains("image/png") -> "png"
                contentType.contains("image/jpeg") -> "jpg"
                contentType.contains("image/gif") -> "gif"
                contentType.contains("image/svg+xml") -> "svg"
                urlStr.contains(".png") -> "png"
                urlStr.contains(".jpg") || urlStr.contains(".jpeg") -> "jpg"
                urlStr.contains(".gif") -> "gif"
                urlStr.contains(".svg") -> "svg"
                else -> "png" // Default to png
            }
            
            val fileName = "${UUID.randomUUID()}.$extension"
            val targetFile = File(imagesDir, fileName)
            
            url.openStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        } catch (e: Exception) {
            println("⚠️  Failed to download image $urlStr: ${e.message}")
            null
        }
    }
}
