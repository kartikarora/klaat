package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import me.kartikarora.klaat.core.Exporter
import me.kartikarora.klaat.util.TablePrinter
import java.io.File

/**
 * Command to extract the bundled showcase demo codelab into the current working directory,
 * export it, and start the local preview server.
 */
class DemoCommand(
    private val workingDir: File = File("."),
    private val startServer: Boolean = true
) : CliktCommand("demo") {

    private val addr by option(
        "-addr",
        help = "hostname:port to bind to for preview server"
    ).default("localhost:9090")

    private val outDir by option(
        "-o", "--out",
        help = "Output directory for exported demo"
    ).default("output")

    private val format by option(
        "-f", "--format",
        help = "Output format (html, md, offline)"
    ).choice("html", "md", "offline").default("html")

    private val prefix by option(
        "-prefix",
        help = "Asset prefix URL"
    ).default("https://distribute.kartikarora.me")

    private val noBrowser by option(
        "--no-browser",
        help = "Don't open browser automatically"
    ).flag()

    private val noServe by option(
        "--no-serve",
        help = "Only extract and export, do not start preview server"
    ).flag()

    override fun run() {
        TablePrinter.printInfoTable()

        echo(
            """
╔═══════════════════════════════════════════════════╗
║ Klaat Demo Generator                              ║
╠═══════════════════════════════════════════════════╣
║ Extracting and launching interactive showcase demo║
╚═══════════════════════════════════════════════════╝
""".trimIndent()
        )

        // 1. Extract bundled demo files into working directory
        val mdFile = File(workingDir, "feature-showcase.md")
        val imagesDir = File(workingDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "klaat.png")

        extractResource("/demo/feature-showcase.md", mdFile)
        extractResource("/demo/images/klaat.png", imageFile)

        echo("✓ Extracted demo source: ${mdFile.name}")
        echo("✓ Extracted demo image: images/${imageFile.name}")

        // 2. Export the demo codelab
        val targetOutDir = File(workingDir, outDir)
        val exporter = Exporter(docsService = null)
        try {
            exporter.export(
                source = mdFile.absolutePath,
                outputDirectory = targetOutDir.absolutePath,
                format = format,
                gaId = null,
                prefix = prefix,
                environment = "web"
            )
            echo("✓ Demo codelab exported to: ${targetOutDir.path}/klaat-feature-showcase")
        } catch (e: Exception) {
            echo("✗ Error exporting demo codelab: ${e.message}")
            return
        }

        // 3. Serve the demo codelab (unless --no-serve is set or startServer is false)
        if (startServer && !noServe) {
            echo("🚀 Launching preview server on http://$addr ...")
            val serveArgs = mutableListOf("-d", targetOutDir.absolutePath, "-addr", addr)
            if (noBrowser) {
                serveArgs.add("--no-browser")
            }
            val serveCommand = ServeCommand()
            serveCommand.parse(serveArgs)
        }
    }

    private fun extractResource(resourcePath: String, targetFile: File) {
        val stream = DemoCommand::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Bundled resource not found: $resourcePath")
        targetFile.parentFile?.mkdirs()
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
