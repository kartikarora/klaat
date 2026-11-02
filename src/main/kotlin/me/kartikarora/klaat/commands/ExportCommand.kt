package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import me.kartikarora.klaat.core.GoogleDocsClient
import me.kartikarora.klaat.core.Exporter
import me.kartikarora.klaat.util.TablePrinter
import java.io.File

/**
 * Command to export codelabs from Google Docs or Markdown files
 */
class ExportCommand : CliktCommand("export") {
    private val targets by argument(
        help = "Google Doc IDs or Markdown file paths to export",
        completionCandidates = CompletionCandidates.Path
    ).multiple(required = true)

    private val auth by option(
        "-auth",
        help = "OAuth2 token to use for authentication"
    )

    private val environment by option(
        "-e",
        help = "Environment to use for the codelab",
        completionCandidates = CompletionCandidates.Fixed("web", "kiosk")
    ).default("web")

    private val extra by option(
        "-extra",
        help = "Additional metadata in JSON format"
    )

    private val format by option(
        "-f",
        help = "Output format"
    ).choice("html", "md", "offline").default("html")

    private val ga by option(
        "-ga",
        help = "Global Google Analytics tracking ID"
    )

    private val outDir by option(
        "-o",
        help = "Output directory",
        completionCandidates = CompletionCandidates.Path
    ).default(".")

    private val passMetadata by option(
        "-pass_metadata",
        help = "Comma-delimited list of metadata fields to pass through to the output"
    )

    private val prefix by option(
        "-prefix",
        help = "URL prefix for assets"
    )

    private val customCss by option(
        "-css",
        help = "Custom CSS file or URL to use"
    )
    
    private val favicon by option(
        "-favicon",
        help = "Custom favicon URL or path to use"
    )

    private val fonts by option(
        "-fonts",
        help = "Google Fonts URL to load additional fonts (e.g. https://fonts.googleapis.com/css2?family=Inter)"
    )

    private val footer by option(
        "-footer",
        help = "Custom footer HTML or text to display at the bottom of the codelab"
    )

    override fun run() {
        // Parse extra metadata if provided
        val extraMetadata = if (!extra.isNullOrBlank()) {
            try {
                val gson = Gson()
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(extra, type)
            } catch (e: JsonSyntaxException) {
                echo("✗ Error: Invalid JSON in -extra flag: ${e.message}")
                echo("  Expected format: {\"key1\":\"value1\",\"key2\":\"value2\"}")
                return
            } catch (e: Exception) {
                echo("✗ Error parsing -extra metadata: ${e.message}")
                return
            }
        } else {
            emptyMap()
        }

        // Parse pass_metadata flag as comma-delimited list
        val passMetadataSet = parsePassMetadata(passMetadata)

        // Initialize Google Docs client if needed
        val needsDocsClient = targets.any { !it.endsWith(".md") && !File(it).exists() }
        val docsService = if (needsDocsClient) {
            try {
                echo("🔑 Initializing Google Docs client...")
                GoogleDocsClient().getDocsService()
            } catch (e: Exception) {
                echo("✗ Failed to initialize Google Docs client: ${e.message ?: e::class.simpleName}")
                e.printStackTrace()
                echo("  Make sure you have valid credentials")
                return
            }
        } else {
            null
        }

        val exporter = Exporter(docsService)

        val effectivePrefix = when {
            prefix != null -> prefix
            format.lowercase() == "html" -> "https://distribute.kartikarora.me"
            else -> null
        }

        TablePrinter.printExportConfigTable(
            targets.size,
            format,
            outDir,
            environment,
            ga,
            effectivePrefix ?: "(local bundle)",
            extraMetadata,
            passMetadata,
            auth
        )
        echo()

        var successCount = 0
        var failureCount = 0

        targets.forEach { target ->
            try {
                exporter.export(target, outDir, format, ga, effectivePrefix, environment, passMetadataSet, extraMetadata, customCss, favicon, fonts, footer)
                successCount++
            } catch (e: Exception) {
                echo("✗ Failed to export '$target': ${e.message}")
                failureCount++
            }
        }

        echo()
        TablePrinter.printSummaryTable(successCount, failureCount)
    }

    /**
     * Parse metadata fields to pass through that are not explicitly handled elsewhere.
     * Expects fields as a comma-separated list (extraneous spaces are auto-removed).
     * Returns a set of lowercase field names.
     */
    private fun parsePassMetadata(passMeta: String?): Set<String> {
        if (passMeta.isNullOrBlank()) {
            return emptySet()
        }
        return passMeta.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}