package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import java.io.File

/**
 * Renders a Codelab to offline HTML format, bundling the brand CSS/JS and
 * custom-CSS bridge assets locally into /css and /js directories.
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
        footer: String? = null,
        legacy: Boolean = false
    ) {
        val codelabDir = File(outputDir, codelab.id)
        codelabDir.mkdirs()

        // Copy assets locally into /css and /js unless an external prefix was explicitly provided
        val effectivePrefix = if (!prefix.isNullOrEmpty()) {
            prefix
        } else {
            copyAssets(codelabDir, customCss, legacy)
            ""
        }

        // Render using the HTML renderer with local or specified prefix
        htmlRenderer.render(
            codelab = codelab,
            outputDir = outputDir,
            ga = ga,
            prefix = effectivePrefix,
            environment = environment,
            customCss = customCss,
            favicon = favicon,
            googleFonts = googleFonts,
            footer = footer,
            legacy = legacy
        )
    }

    private fun copyAssets(codelabDir: File, customCss: String? = null, legacy: Boolean = false) {
        val cssDir = File(codelabDir, "css")
        val jsDir = File(codelabDir, "js")
        cssDir.mkdirs()
        jsDir.mkdirs()

        // Copy Klaat bridge asset from resources (used when customCssUrl is set)
        val bridgeResource = javaClass.getResourceAsStream("/assets/klaat-bridge.js")
        if (bridgeResource != null) {
            File(jsDir, "klaat-bridge.js").writeBytes(bridgeResource.readBytes())
        }

        if (legacy) {
            val legacyCss = javaClass.getResourceAsStream("/assets/klaat-elements.css")
            if (legacyCss != null) {
                File(cssDir, "klaat-elements.css").writeBytes(legacyCss.readBytes())
            }
            val legacyJs = javaClass.getResourceAsStream("/assets/klaat-elements.js")
            if (legacyJs != null) {
                File(jsDir, "klaat-elements.js").writeBytes(legacyJs.readBytes())
            }
        }

        // Copy or fetch brand assets (kartikarora.2.0.0.css / js) for offline consumption
        copyOrDownloadAsset("https://distribute.kartikarora.me/css/kartikarora.2.0.0.css", "/assets/kartikarora.2.0.0.css", File(cssDir, "kartikarora.2.0.0.css"))
        copyOrDownloadAsset("https://distribute.kartikarora.me/js/kartikarora.2.0.0.js", "/assets/kartikarora.2.0.0.js", File(jsDir, "kartikarora.2.0.0.js"))

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
