package me.kartikarora.klaat.util

import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import me.kartikarora.klaat.BuildConfig

object TablePrinter {
    private val terminal = Terminal()

    fun printInfoTable() {
        val t = table {
            header {
                row(bold(brightBlue("Klaat - Kodelabs As A Thing")))
            }
            body {
                row("Version: ${cyan(BuildConfig.VERSION_NAME)}")
                row("Binary: ${blue(BuildConfig.BINARY_NAME)}")
                row("Variant: ${blue(BuildConfig.BUILD_VARIANT)}")
            }
        }
        terminal.println(t)
    }
    fun printServeTable(directory: String, url: String) {
        val t = table {
            header {
                row(bold(brightBlue("Klaat Codelab Server"))) {
                    align = TextAlign.CENTER
                }
            }
            body {
                row("📂 Serving: ${cyan(directory)}")
                row("🌐 URL: ${cyan(url)}")
                row("⌨️  Press ${yellow("Ctrl+C")} to stop")
            }
        }
        terminal.println(t)
    }

    fun printExportConfigTable(
        targets: Int,
        format: String,
        outDir: String,
        environment: String,
        ga: String,
        prefix: String,
        extraMetadata: Map<String, String>,
        passMetadata: String?,
        auth: String?
    ) {
        val t = table {
            header {
                row(bold(brightBlue("Klaat Codelab Exporter"))) {
                    align = TextAlign.CENTER
                }
            }
            body {
                row("📋 Exporting ${cyan(targets.toString())} codelab(s)")
                row("   Format: ${cyan(format)}")
                row("   Output: ${cyan(outDir)}")
                row("   Environment: ${cyan(environment)}")
                row("   Analytics: ${cyan(ga)}")
                row("   Asset Prefix: ${cyan(prefix)}")
                if (auth != null) row("   Auth: ${green("provided")}")
                if (extraMetadata.isNotEmpty()) {
                    row("   Extra metadata: ${extraMetadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
                }
                if (passMetadata != null) row("   Pass metadata: ${cyan(passMetadata)}")
            }
        }
        terminal.println(t)
    }

    fun printUpdateInfoTable(codelabCount: Int, passMetadata: String?) {
        val t = table {
            header {
                row(bold(brightBlue("Klaat Codelab Updater"))) {
                    align = TextAlign.CENTER
                }
            }
            body {
                row("📋 Found ${cyan(codelabCount.toString())} codelab(s) to update")
                if (passMetadata != null) row("   Pass metadata: ${cyan(passMetadata)}")
            }
        }
        terminal.println(t)
    }

    fun printSummaryTable(successCount: Int, failureCount: Int, action: String = "Exported") {
        val t = table {
            header {
                row(bold(brightBlue("Summary"))) {
                    align = TextAlign.CENTER
                }
            }
            body {
                row("✓ $action: ${green(successCount.toString())}")
                if (failureCount > 0) {
                    row("✗ Failed: ${red(failureCount.toString())}")
                }
            }
        }
        terminal.println(t)
    }
}
