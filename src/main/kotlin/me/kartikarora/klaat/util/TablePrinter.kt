package me.kartikarora.klaat.util

import me.kartikarora.klaat.BuildConfig

object TablePrinter {

    private fun printTable(title: String, rows: List<String>) {
        val maxWidth = (rows.maxOfOrNull { it.length } ?: 0).coerceAtLeast(title.length)
        val topBorder = "╔" + "═".repeat(maxWidth + 2) + "╗"
        val middleBorder = "╠" + "═".repeat(maxWidth + 2) + "╣"
        val bottomBorder = "╚" + "═".repeat(maxWidth + 2) + "╝"

        println()
        println(topBorder)
        println("║ ${title.padEnd(maxWidth)} ║")
        println(middleBorder)
        rows.forEach { row ->
            println("║ ${row.padEnd(maxWidth)} ║")
        }
        println(bottomBorder)
        println()
    }

    fun printInfoTable() {
        printTable(
            "Klaat - Kodelabs As A Thing",
            listOf(
                "Version: ${BuildConfig.VERSION_NAME}",
                "Binary: ${BuildConfig.BINARY_NAME}",
                "Variant: ${BuildConfig.BUILD_VARIANT}"
            )
        )
    }
    fun printServeTable(directory: String, url: String) {
        printTable(
            "Klaat Codelab Server",
            listOf(
                "Serving: $directory",
                "URL: $url",
                "Press Ctrl+C to stop"
            )
        )
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
        val rows = mutableListOf(
            "Exporting $targets codelab(s)",
            "  Format: $format",
            "  Output: $outDir",
            "  Environment: $environment",
            "  Analytics: $ga",
            "  Asset Prefix: $prefix"
        )
        if (auth != null) rows.add("  Auth: provided")
        if (extraMetadata.isNotEmpty()) {
            rows.add("  Extra metadata: ${extraMetadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
        }
        if (passMetadata != null) rows.add("  Pass metadata: $passMetadata")

        printTable("Klaat Codelab Exporter", rows)
    }

    fun printUpdateInfoTable(codelabCount: Int, passMetadata: String?) {
        val rows = mutableListOf("Found $codelabCount codelab(s) to update")
        if (passMetadata != null) rows.add("  Pass metadata: $passMetadata")

        printTable("Klaat Codelab Updater", rows)
    }

    fun printSummaryTable(successCount: Int, failureCount: Int, action: String = "Exported") {
        val rows = mutableListOf("✓ $action: $successCount")
        if (failureCount > 0) {
            rows.add("✗ Failed: $failureCount")
        }

        printTable("Summary", rows)
    }
}
