package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import me.kartikarora.klaat.util.TablePrinter
import java.io.File
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Command to serve codelabs locally for preview
 */
class ServeCommand : CliktCommand("serve") {
    private val addr by option(
        "-addr",
        help = "hostname:port to bind to"
    ).default("localhost:9090")

    private val dir by option(
        "-d", "--dir",
        help = "Directory to serve"
    ).default(".")

    private val noBrowser by option(
        "--no-browser",
        help = "Don't open browser automatically"
    ).flag()

    private val freemarkerConfig = Configuration(Configuration.VERSION_2_3_32).apply {
        setClassForTemplateLoading(ServeCommand::class.java, "/templates")
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
    }

    override fun run() {
        val directory = File(dir).absoluteFile
        if (!directory.exists() || !directory.isDirectory) {
            echo("✗ Error: Directory not found: ${directory.absolutePath}")
            return
        }

        // Parse addr into host and port
        val (host, port) = parseAddr(addr)
        val url = "http://$host:$port"

        TablePrinter.printServeTable(directory.absolutePath, url)
        echo()

        // Open browser if not disabled
        if (!noBrowser) {
            openBrowser(url)
        }

        // Start server
        embeddedServer(Netty, port = port, host = host) {
            routing {
                get("{path...}") {
                    val pathParts = call.parameters.getAll("path") ?: emptyList()
                    val path = pathParts.joinToString("/")
                    val file = File(directory, path)

                    if (file.exists() && file.isDirectory) {
                        val indexHtml = File(file, "index.html")
                        if (!indexHtml.exists()) {
                            val fileList = file.listFiles()?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name }) ?: emptyList<File>()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            
                            val items = fileList.map { f ->
                                mapOf(
                                    "name" to f.name,
                                    "isDirectory" to f.isDirectory,
                                    "lastModified" to dateFormat.format(Date(f.lastModified())),
                                    "size" to if (f.isDirectory) "-" else formatSize(f.length())
                                )
                            }

                            val template = freemarkerConfig.getTemplate("directory_listing.ftl")
                            val dataModel = mapOf(
                                "path" to path,
                                "items" to items
                            )

                            val writer = StringWriter()
                            template.process(dataModel, writer)
                            call.respondText(writer.toString(), ContentType.Text.Html)
                        } else {
                            call.respondFile(indexHtml)
                        }
                    } else if (file.exists() && file.isFile) {
                        call.respondFile(file)
                    }
                }
            }
        }.start(wait = true)
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun parseAddr(addr: String): Pair<String, Int> {
        val parts = addr.split(":")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1].toIntOrNull() ?: 9090)
        } else {
            Pair("localhost", 9090)
        }
    }

    private fun openBrowser(url: String) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            when {
                osName.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", url))
                    echo("🚀 Opening browser...")
                }
                osName.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url))
                    echo("🚀 Opening browser...")
                }
                osName.contains("nix") || osName.contains("nux") -> {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                    echo("🚀 Opening browser...")
                }
                osName.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url))
                    echo("🚀 Opening browser...")
                }
                else -> {
                    echo("ℹ️  Could not auto-open browser. Please navigate to: $url")
                }
            }
        } catch (e: Exception) {
            echo("ℹ️  Could not auto-open browser. Please navigate to: $url")
        }
    }
}
