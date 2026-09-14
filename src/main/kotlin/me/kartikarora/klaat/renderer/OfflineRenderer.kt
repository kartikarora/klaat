package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import java.io.File

/**
 * Renders a Codelab to offline HTML format using the modern <klaat-kodelab> component suite,
 * bundling all CSS and JS assets locally into /css and /js directories.
 */
class OfflineRenderer {

    private val htmlRenderer = HtmlRenderer()

    fun render(
        codelab: Codelab,
        outputDir: File,
        ga: String? = null,
        prefix: String? = null,
        environment: String = "web",
        customCss: String? = null,
        favicon: String? = null,
        googleFonts: String? = null,
        footer: String? = null
    ) {
        val codelabDir = File(outputDir, codelab.id)
        codelabDir.mkdirs()

        // Copy assets locally into /css and /js unless an external prefix was explicitly provided
        val effectivePrefix = if (!prefix.isNullOrEmpty()) {
            prefix
        } else {
            copyAssets(codelabDir, customCss)
            ""
        }

        // Render using the modern HTML renderer with local or specified prefix
        htmlRenderer.render(
            codelab = codelab,
            outputDir = outputDir,
            ga = ga,
            prefix = effectivePrefix,
            environment = environment,
            customCss = customCss,
            favicon = favicon,
            googleFonts = googleFonts,
            footer = footer
        )
    }

    private fun copyAssets(codelabDir: File, customCss: String? = null) {
        val cssDir = File(codelabDir, "css")
        val jsDir = File(codelabDir, "js")
        cssDir.mkdirs()
        jsDir.mkdirs()

        // Copy Klaat assets from resources
        val cssResource = javaClass.getResourceAsStream("/assets/klaat-elements.css")
        if (cssResource != null) {
            File(cssDir, "klaat-elements.css").writeBytes(cssResource.readBytes())
        }

        val jsResource = javaClass.getResourceAsStream("/assets/klaat-elements.js")
        if (jsResource != null) {
            File(jsDir, "klaat-elements.js").writeBytes(jsResource.readBytes())
        }

        val bridgeResource = javaClass.getResourceAsStream("/assets/klaat-bridge.js")
        if (bridgeResource != null) {
            File(jsDir, "klaat-bridge.js").writeBytes(bridgeResource.readBytes())
        }

        // Copy or fetch brand assets (kartikarora.1.8.0.css / js) for offline consumption
        copyOrDownloadAsset("https://distribute.kartikarora.me/css/kartikarora.1.8.0.css", "/assets/kartikarora.1.8.0.css", File(cssDir, "kartikarora.1.8.0.css"))
        copyOrDownloadAsset("https://distribute.kartikarora.me/js/kartikarora.1.8.0.js", "/assets/kartikarora.1.8.0.js", File(jsDir, "kartikarora.1.8.0.js"))

        // Copy custom CSS if provided and it's a local file
        val customCssFile = customCss?.let { File(it) }
        if (customCssFile != null && customCssFile.exists()) {
            customCssFile.copyTo(File(cssDir, customCssFile.name), overwrite = true)
        }
    }

    private fun copyOrDownloadAsset(url: String, resourcePath: String, destFile: File) {
        val resource = javaClass.getResourceAsStream(resourcePath)
        if (resource != null) {
            destFile.writeBytes(resource.readBytes())
            return
        }
        try {
            java.net.URI(url).toURL().openStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {
            // Best effort if offline or network unreachable
        }
    }
}
