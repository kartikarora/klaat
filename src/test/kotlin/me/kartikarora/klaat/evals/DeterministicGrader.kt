package me.kartikarora.klaat.evals

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.parser.MarkdownParser

object DeterministicGrader {

    private val parser = MarkdownParser()

    private val cliHallucinatedFlags = listOf("--watch", "--port ", "--output-dir", "--theme ")

    /**
     * Extracts the outer markdown document from an LLM response without truncating
     * at inner fenced code blocks (such as ```kotlin or ```bash).
     */
    fun extractMarkdown(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        val openingMatch = Regex("""^```(?:markdown|md)?\s*\r?\n""").find(trimmed)
        if (openingMatch != null) {
            val afterOpening = trimmed.substring(openingMatch.range.last + 1)
            val closingMatch = Regex("""\r?\n```\s*$""").find(afterOpening)
            if (closingMatch != null) {
                return afterOpening.substring(0, closingMatch.range.first).trim()
            }
        }
        return trimmed
    }

    /**
     * Grades an LLM response using skill-appropriate deterministic checks.
     */
    fun grade(
        rawResponse: String,
        expectedSkill: String? = "klaat-markdown-author",
        expectedPatterns: List<String> = emptyList()
    ): GradedChecklist {
        if (rawResponse.isBlank()) {
            return GradedChecklist(
                passed = false,
                compiles = false,
                hasValidMetadata = false,
                hasValidDurations = false,
                hasCallouts = false,
                stepCount = 0,
                error = "Response was blank or empty"
            )
        }

        return when (expectedSkill) {
            "klaat-cli" -> gradeCli(rawResponse, expectedPatterns)
            "klaat-gdocs-author" -> gradeGdocs(rawResponse, expectedPatterns)
            else -> gradeMarkdown(rawResponse)
        }
    }

    private fun gradeMarkdown(rawResponse: String): GradedChecklist {
        val cleanMarkdown = extractMarkdown(rawResponse)
        if (cleanMarkdown.isBlank()) {
            return GradedChecklist(
                passed = false,
                compiles = false,
                hasValidMetadata = false,
                hasValidDurations = false,
                hasCallouts = false,
                stepCount = 0,
                error = "Response was blank or empty"
            )
        }

        return try {
            val codelab: Codelab = parser.parse(cleanMarkdown.byteInputStream())

            val hasValidMetadata = codelab.id.isNotBlank() &&
                codelab.title.isNotBlank() &&
                codelab.metadata.summary.isNotBlank() &&
                codelab.metadata.category.isNotEmpty()

            val hasValidDurations = codelab.steps.isNotEmpty() &&
                codelab.steps.all { (it.duration ?: 0) > 0 }

            val hasCallouts = codelab.steps.any { step ->
                hasSpecificInfoBoxRecursive(step.content)
            }

            val compiles = codelab.id.isNotBlank() &&
                codelab.title.isNotBlank() &&
                codelab.steps.isNotEmpty()

            GradedChecklist(
                passed = compiles,
                compiles = compiles,
                hasValidMetadata = hasValidMetadata,
                hasValidDurations = hasValidDurations,
                hasCallouts = hasCallouts,
                stepCount = codelab.steps.size,
                extractedId = codelab.id.ifBlank { null },
                extractedSummary = codelab.summary.ifBlank { null },
                checks = mapOf(
                    "metadata" to hasValidMetadata,
                    "steps" to (codelab.steps.isNotEmpty()),
                    "durations" to hasValidDurations,
                    "callouts" to hasCallouts
                )
            )
        } catch (e: Exception) {
            GradedChecklist(
                passed = false,
                compiles = false,
                hasValidMetadata = false,
                hasValidDurations = false,
                hasCallouts = false,
                stepCount = 0,
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun gradeCli(rawResponse: String, expectedPatterns: List<String>): GradedChecklist {
        val normalized = rawResponse.lowercase()
        val hasValidCommand = Regex("""klaat\s+(export|serve|update|demo|version|help|install-skills|completion)\b""")
            .containsMatchIn(normalized) || normalized.contains("brew install kartikarora/tap/klaat")
        val noHallucinatedFlags = cliHallucinatedFlags.none { normalized.contains(it) }
        val patternsMatched = expectedPatterns.all { normalized.contains(it.lowercase()) }
        val passed = hasValidCommand && noHallucinatedFlags && patternsMatched

        val missingPatterns = expectedPatterns.filterNot { normalized.contains(it.lowercase()) }
        val error = when {
            !hasValidCommand -> "Missing valid klaat CLI command or Homebrew tap install"
            !noHallucinatedFlags -> "Contains hallucinated CLI flag"
            missingPatterns.isNotEmpty() -> "Missing expected CLI patterns: ${missingPatterns.joinToString(", ")}"
            else -> null
        }

        return GradedChecklist(
            passed = passed,
            compiles = passed,
            hasValidMetadata = hasValidCommand,
            hasValidDurations = patternsMatched,
            hasCallouts = noHallucinatedFlags,
            stepCount = expectedPatterns.size,
            error = error,
            checks = mapOf(
                "validCommand" to hasValidCommand,
                "noHallucinatedFlags" to noHallucinatedFlags,
                "expectedPatterns" to patternsMatched
            )
        )
    }

    private fun gradeGdocs(rawResponse: String, expectedPatterns: List<String>): GradedChecklist {
        val normalized = rawResponse.lowercase()
        val hasMetadataTable = normalized.contains("summary") &&
            (normalized.contains("id") || normalized.contains("categories") || normalized.contains("author"))
        val hasHeadingHierarchy = (normalized.contains("heading 1") || normalized.contains("h1")) &&
            (normalized.contains("heading 2") || normalized.contains("h2"))
        val hasDurationSyntax = normalized.contains("duration:")
        val patternsMatched = expectedPatterns.all { normalized.contains(it.lowercase()) }
        val passed = hasMetadataTable && hasHeadingHierarchy && hasDurationSyntax && patternsMatched

        val missing = buildList {
            if (!hasMetadataTable) add("metadata table")
            if (!hasHeadingHierarchy) add("Heading 1/Heading 2 hierarchy")
            if (!hasDurationSyntax) add("Duration: syntax")
            expectedPatterns.filterNot { normalized.contains(it.lowercase()) }.forEach { add("pattern '$it'") }
        }

        return GradedChecklist(
            passed = passed,
            compiles = passed,
            hasValidMetadata = hasMetadataTable,
            hasValidDurations = hasDurationSyntax,
            hasCallouts = hasHeadingHierarchy,
            stepCount = expectedPatterns.size,
            error = if (missing.isEmpty()) null else "Missing Google Docs elements: ${missing.joinToString(", ")}",
            checks = mapOf(
                "metadataTable" to hasMetadataTable,
                "headingHierarchy" to hasHeadingHierarchy,
                "durationSyntax" to hasDurationSyntax,
                "expectedPatterns" to patternsMatched
            )
        )
    }

    private fun hasSpecificInfoBoxRecursive(nodes: List<Node>): Boolean {
        for (node in nodes) {
            if (node is Node.InfoBox && node.type != InfoBoxType.DEFAULT) return true
            if (node is Node.Paragraph && hasSpecificInfoBoxRecursive(node.nodes)) return true
        }
        return false
    }
}
