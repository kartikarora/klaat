package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.gson.Gson
import com.google.gson.JsonObject
import me.kartikarora.klaat.core.Exporter
import me.kartikarora.klaat.core.GoogleDocsClient
import java.io.File

/**
 * Command to update existing codelabs
 */
class UpdateCommand : CliktCommand("update") {
    private val paths by argument(
        help = "Directories or codelab IDs to update (default: current directory)"
    ).multiple()

    private val auth by option(
        "-auth",
        help = "OAuth2 token to use for authentication"
    )

    private val environment by option(
        "-e",
        help = "Environment to use for the codelab"
    ).default("web")

    private val extra by option(
        "-extra",
        help = "Additional metadata in JSON format"
    )

    private val format by option(
        "-f",
        help = "Output format"
    )

    private val ga by option(
        "-ga",
        help = "Global Google Analytics tracking ID"
    ).default("UA-49880327-14")

    private val outDir by option(
        "-o",
        help = "Output directory"
    )

    private val passMetadata by option(
        "-pass_metadata",
        help = "Comma-delimited list of metadata fields to pass through to the output"
    )

    private val prefix by option(
        "-prefix",
        help = "URL prefix for assets"
    ).default("https://distribute.kartikarora.me")

    override fun run() {
        echo("╔═══════════════════════════════════════════════╗")
        echo("║         Klaat Codelab Updater                ║")
        echo("╚═══════════════════════════════════════════════╝")
        echo()

        // Parse pass_metadata flag as comma-delimited list
        val passMetadataSet = parsePassMetadata(passMetadata)

        val searchPaths = if (paths.isEmpty()) listOf(".") else paths

        // Find all codelab.json files
        val codelabDirs = mutableListOf<File>()
        searchPaths.forEach { path ->
            val dir = File(path)
            if (dir.exists()) {
                findCodelabDirs(dir, codelabDirs)
            }
        }

        if (codelabDirs.isEmpty()) {
            echo("ℹ️  No codelabs found in ${searchPaths.joinToString(", ")}")
            return
        }

        echo("📋 Found ${codelabDirs.size} codelab(s) to update")
        if (passMetadata != null) echo("   Pass metadata: $passMetadata")
        echo()

        // Initialize Google Docs client
        val docsService = try {
            echo("🔑 Initializing Google Docs client...")
            GoogleDocsClient().getDocsService()
        } catch (e: Exception) {
            echo("⚠️  Warning: Could not initialize Google Docs client")
            echo("   Only markdown-based codelabs will be updated")
            null
        }

        val exporter = Exporter(docsService)
        val gson = Gson()

        var successCount = 0
        var failureCount = 0

        codelabDirs.forEach { codelabDir ->
            try {
                val metadataFile = File(codelabDir, "codelab.json")
                val metadata = gson.fromJson(metadataFile.readText(), JsonObject::class.java)
                val source = metadata.get("source")?.asString

                if (source.isNullOrEmpty()) {
                    echo("⚠️  Skipping ${codelabDir.name}: No source found in metadata")
                    return@forEach
                }

                echo("🔄 Updating ${codelabDir.name}...")

                // Determine format from option or existing files
                val exportFormat = format ?: when {
                    File(codelabDir, "index.html").exists() -> "html"
                    File(codelabDir, "index.md").exists() -> "md"
                    else -> "html"
                }

                val outputDir = outDir?.let { File(it) } ?: codelabDir.parentFile
                exporter.export(source, outputDir.absolutePath, exportFormat, ga, prefix, environment, passMetadataSet)

                successCount++
            } catch (e: Exception) {
                echo("✗ Failed to update ${codelabDir.name}: ${e.message}")
                failureCount++
            }
        }

        echo()
        echo("═══════════════════════════════════════════════")
        echo("✓ Updated: $successCount")
        if (failureCount > 0) {
            echo("✗ Failed: $failureCount")
        }
        echo("═══════════════════════════════════════════════")
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

    private fun findCodelabDirs(dir: File, result: MutableList<File>) {
        if (!dir.isDirectory) return

        val metadataFile = File(dir, "codelab.json")
        if (metadataFile.exists()) {
            result.add(dir)
            return // Don't recurse into codelab directories
        }

        // Recurse into subdirectories
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                findCodelabDirs(child, result)
            }
        }
    }
}