package me.kartikarora.klaat.parser

import com.google.api.services.docs.v1.model.Document
import com.google.api.services.docs.v1.model.ParagraphElement
import com.google.api.services.docs.v1.model.StructuralElement
import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.CodelabMetadata
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.core.Step

/**
 * Parser for Google Docs-based codelabs
 */
class GoogleDocParser {

    fun parse(document: Document, passMetadata: Set<String> = emptySet()): Codelab {
        val title = document.title ?: "Untitled Codelab"
        val docId = document.documentId ?: ""

        val (metadata, steps) = parseDocument(document, passMetadata)

        val id = metadata.url.ifEmpty {
            docId
        }

        var finalMetadata = metadata.copy(source = docId)
        if (finalMetadata.image.isEmpty()) {
            val firstImage = steps.asSequence()
                .flatMap { it.content.asSequence() }
                .flatMap { node ->
                    when (node) {
                        is Node.Image -> sequenceOf(node)
                        is Node.Paragraph -> node.nodes.filterIsInstance<Node.Image>().asSequence()
                        is Node.InfoBox -> node.nodes.filterIsInstance<Node.Image>().asSequence()
                        is Node.BulletList -> node.items.flatten().filterIsInstance<Node.Image>().asSequence()
                        is Node.NumberedList -> node.items.flatten().filterIsInstance<Node.Image>().asSequence()
                        else -> emptySequence()
                    }
                }
                .firstOrNull()
            if (firstImage != null) {
                finalMetadata = finalMetadata.copy(image = firstImage.src)
            }
        }

        return Codelab(
            id = id,
            title = title,
            summary = metadata.summary,
            steps = steps,
            metadata = finalMetadata
        )
    }

    private fun parseDocument(document: Document, passMetadata: Set<String>): Pair<CodelabMetadata, List<Step>> {
        val steps = mutableListOf<Step>()
        val metadataMap = mutableMapOf<String, String>()

        var currentStep: MutableList<Node>? = null
        var currentStepTitle = ""
        var currentStepDuration: Int? = null
        var currentStepExcludeDuration = false
        var foundMetadataTable = false

        document.body?.content?.forEach { element ->
            when {
                // Check for metadata table (before first H1)
                !foundMetadataTable && element.table != null && steps.isEmpty() -> {
                    parseMetadataTable(element.table, metadataMap)
                    foundMetadataTable = true
                }
                // H1 indicates a new step
                element.paragraph != null && element.paragraph.paragraphStyle?.namedStyleType == "HEADING_1" -> {
                    // Save previous step if it exists
                    if (currentStep != null && currentStepTitle.isNotEmpty()) {
                        steps.add(Step(
                            title = currentStepTitle,
                            content = currentStep,
                            duration = currentStepDuration,
                            excludeDurationFromTotal = currentStepExcludeDuration
                        ))
                    }

                    // Start new step
                    currentStepTitle = extractText(element.paragraph.elements)
                    currentStep = mutableListOf()
                    currentStepDuration = null
                    currentStepExcludeDuration = false
                }
                // Parse content within a step
                currentStep != null -> {
                    val nodes = parseElement(document, element)
                    currentStep.addAll(nodes)
                }
            }
        }

        // Add the last step
        if (currentStep != null && currentStepTitle.isNotEmpty()) {
            steps.add(Step(
                title = currentStepTitle,
                content = currentStep,
                duration = currentStepDuration,
                excludeDurationFromTotal = currentStepExcludeDuration
            ))
        }

        val metadata = buildMetadata(metadataMap, passMetadata)
        return Pair(metadata, steps)
    }

    private fun parseMetadataTable(table: com.google.api.services.docs.v1.model.Table, metadataMap: MutableMap<String, String>) {
        table.tableRows?.forEach { row ->
            val cells = row.tableCells ?: return@forEach
            if (cells.size >= 2) {
                val key = extractText(cells[0].content?.flatMap { it.paragraph?.elements ?: emptyList() } ?: emptyList()).trim()
                val value = extractText(cells[1].content?.flatMap { it.paragraph?.elements ?: emptyList() } ?: emptyList()).trim()
                if (key.isNotEmpty()) {
                    metadataMap[key.lowercase()] = value
                }
            }
        }
    }

    private fun parseElement(document: Document, element: StructuralElement): List<Node> {
        val nodes = mutableListOf<Node>()

        when {
            element.paragraph != null -> {
                val paragraph = element.paragraph
                val style = paragraph.paragraphStyle?.namedStyleType

                when (style) {
                    "HEADING_2" -> nodes.add(Node.Heading(2, extractText(paragraph.elements)))
                    "HEADING_3" -> nodes.add(Node.Heading(3, extractText(paragraph.elements)))
                    "HEADING_4" -> nodes.add(Node.Heading(4, extractText(paragraph.elements)))
                    else -> {
                        val textNodes = parseTextElements(document, paragraph.elements)
                        if (textNodes.isNotEmpty()) {
                            nodes.add(Node.Paragraph(textNodes))
                        }
                    }
                }
            }
            element.table != null -> {
                // Tables could be code blocks, info boxes, or surveys
                // This is a simplified implementation
                val textContent = element.table.tableRows?.joinToString("\n") { row ->
                    row.tableCells?.joinToString(" | ") { cell ->
                        extractText(cell.content?.flatMap { it.paragraph?.elements ?: emptyList() } ?: emptyList())
                    } ?: ""
                } ?: ""

                nodes.add(Node.Paragraph(listOf(Node.Text(textContent))))
            }
        }

        return nodes
    }

    private fun parseTextElements(document: Document, elements: List<ParagraphElement>?): List<Node> {
        val nodes = mutableListOf<Node>()

        elements?.forEach { element ->
            if (element.inlineObjectElement != null) {
                // Handle images
                val objectId = element.inlineObjectElement.inlineObjectId
                val inlineObject = document.inlineObjects?.get(objectId)
                val embeddedObject = inlineObject?.inlineObjectProperties?.embeddedObject
                val src = embeddedObject?.imageProperties?.contentUri ?: ""
                val alt = embeddedObject?.description ?: embeddedObject?.title ?: ""
                nodes.add(Node.Image(src = src, alt = alt))
                return@forEach
            }

            val textRun = element.textRun ?: return@forEach
            val content = textRun.content ?: ""

            if (content.isBlank()) return@forEach

            val style = textRun.textStyle
            val bold = style?.bold == true
            val italic = style?.italic == true
            // Note: TextStyle doesn't have fontFamily in v1 API, using weightedFontFamily instead
            val code = false // Simplified for now

            nodes.add(Node.Text(content, bold, italic, code))
        }

        return nodes
    }

    private fun extractText(elements: List<ParagraphElement>?): String {
        return elements?.joinToString("") { it.textRun?.content ?: "" } ?: ""
    }

    private fun buildMetadata(map: Map<String, String>, passMetadata: Set<String>): CodelabMetadata {
        // Standard metadata fields that are explicitly handled
        val standardFields = setOf(
            "summary", "url", "category", "environment", "status",
            "feedback link", "feedback_link", "analytics account", "analytics_account",
            "tags", "authors", "source", "duration", "theme"
        )

        // Collect extra metadata fields that are in passMetadata but not in standard fields
        val extraMetadata = mutableMapOf<String, String>()
        map.forEach { (key, value) ->
            val lowerKey = key.lowercase()
            if (lowerKey in passMetadata && lowerKey !in standardFields) {
                extraMetadata[key] = value
            }
        }

        return CodelabMetadata(
            summary = map["summary"] ?: "",
            url = map["url"] ?: "",
            category = map["category"]?.split(",")?.map { it.trim() } ?: emptyList(),
            environment = map["environment"]?.split(",")?.map { it.trim() } ?: listOf("Web"),
            status = map["status"]?.split(",")?.map { it.trim() } ?: listOf("Published"),
            feedbackLink = map["feedback link"] ?: map["feedback_link"] ?: "",
            analyticsAccount = map["analytics account"] ?: map["analytics_account"] ?: "",
            tags = map["tags"]?.split(",")?.map { it.trim() } ?: emptyList(),
            authors = map["authors"]?.split(",")?.map { it.trim() } ?: emptyList(),
            source = map["source"] ?: "",
            theme = map["theme"] ?: "",
            duration = map["duration"]?.toIntOrNull() ?: 0,
            image = "",
            extraMetadata = extraMetadata
        )
    }
}