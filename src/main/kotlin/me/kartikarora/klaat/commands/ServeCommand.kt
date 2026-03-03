package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.io.File

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

    override fun run() {
        val directory = File(dir).absoluteFile
        if (!directory.exists() || !directory.isDirectory) {
            echo("✗ Error: Directory not found: ${directory.absolutePath}")
            return
        }

        // Parse addr into host and port
        val (host, port) = parseAddr(addr)
        val url = "http://$host:$port"

        echo("╔═══════════════════════════════════════════════╗")
        echo("║         Klaat Codelab Server                  ║")
        echo("╚═══════════════════════════════════════════════╝")
        echo()
        echo("📂 Serving: ${directory.absolutePath}")
        echo("🌐 URL: $url")
        echo("⌨️  Press Ctrl+C to stop")
        echo()

        // Open browser if not disabled
        if (!noBrowser) {
            openBrowser(url)
        }

        // Start server
        embeddedServer(Netty, port = port, host = host) {
            routing {
                staticFiles("/", directory)
            }
        }.start(wait = true)
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
        val osName = System.getProperty("os.name").lowercase()
        try {
            if (osName.contains("mac")) {
                Runtime.getRuntime().exec(arrayOf("open", url))
                echo("🚀 Opening browser...")
            } else if (osName.contains("nix") || osName.contains("nux")) {
                Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                echo("🚀 Opening browser...")
            } else if (osName.contains("win")) {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url))
                echo("🚀 Opening browser...")
            } else {
                echo("ℹ️  Could not auto-open browser. Please navigate to: $url")
            }
        } catch (e: Exception) {
            echo("ℹ️  Could not auto-open browser. Please navigate to: $url")
        }
    }
}
