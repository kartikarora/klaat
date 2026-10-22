package me.kartikarora.klaat.evals

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.parser.MarkdownParser

object DeterministicGrader {

    private val parser = MarkdownParser()

    /**
     * Cleans common LLM wrappers (such as markdown code fences) from the response text.
     */
    fun extractMarkdown(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        val fencedMatch = Regex("""(?s)```(?:markdown)?\s*\n(.*?)\n```""").find(trimmed)
        return if (fencedMatch != null) {
            fencedMatch.groupValues[1].trim()
        } else {
            trimmed
        }
    }

    /**
     * Parses the markdown text using Klaat's official MarkdownParser and scores criteria.
     */
    fun grade(rawResponse: String): GradedChecklist {
        val cleanMarkdown = extractMarkdown(rawResponse)
        if (cleanMarkdown.isBlank()) {
            return GradedChecklist(
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

            // A codelab compiles successfully if it has an id, title, and at least one step
            val compiles = codelab.id.isNotBlank() && codelab.title.isNotBlank() && codelab.steps.isNotEmpty()

            GradedChecklist(
                compiles = compiles,
                hasValidMetadata = hasValidMetadata,
                hasValidDurations = hasValidDurations,
                hasCallouts = hasCallouts,
                stepCount = codelab.steps.size,
                extractedId = codelab.id.ifBlank { null },
                extractedSummary = codelab.summary.ifBlank { null }
            )
        } catch (e: Exception) {
            GradedChecklist(
                compiles = false,
                hasValidMetadata = false,
                hasValidDurations = false,
                hasCallouts = false,
                stepCount = 0,
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun hasSpecificInfoBoxRecursive(nodes: List<Node>): Boolean {
        for (node in nodes) {
            if (node is Node.InfoBox && node.type != me.kartikarora.klaat.core.InfoBoxType.DEFAULT) return true
            if (node is Node.Paragraph && hasSpecificInfoBoxRecursive(node.nodes)) return true
        }
        return false
    }
}
