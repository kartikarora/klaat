package me.kartikarora.klaat.parser

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.CodelabMetadata
import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Step
import java.io.File
import java.io.InputStream
import com.vladsch.flexmark.util.ast.Node as FlexmarkNode
import me.kartikarora.klaat.core.Node as CodelabNode

/**
 * Parser for Markdown-based codelabs following the Google Codelabs format
 */
class MarkdownParser {

    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            YamlFrontMatterExtension.create()
        ))
    }

    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    /**
     * Parse a markdown file into a Codelab object
     */
    fun parse(input: InputStream, passMetadata: Set<String> = emptySet()): Codelab {
        val content = input.bufferedReader().readText()
        return parseContent(content, passMetadata)
    }

    fun parse(file: File, passMetadata: Set<String> = emptySet()): Codelab {
        val content = file.readText()
        return parseContent(content, passMetadata)
    }

    private fun parseContent(content: String, passMetadata: Set<String>): Codelab {
        val document = parser.parse(content)

        // Extract YAML frontmatter
        val metadata = extractMetadata(document, content, passMetadata)

        // Extract steps (H2 headings)
        val steps = extractSteps(document)

        val id = metadata.url.ifEmpty {
            metadata.summary.lowercase().replace(Regex("[^a-z0-9]+"), "-")
        }

        return Codelab(
            id = id,
            title = extractTitle(document),
            summary = metadata.summary,
            steps = steps,
            metadata = metadata
        )
    }

    private fun extractMetadata(document: Document, content: String, passMetadata: Set<String>): CodelabMetadata {
        val visitor = AbstractYamlFrontMatterVisitor()
        visitor.visit(document)
        val data = visitor.data

        // Standard metadata fields that are explicitly handled
        val standardFields = setOf(
            "summary", "id", "categories", "environments", "status",
            "feedback_link", "analytics_account", "tags", "authors",
            "source", "duration"
        )

        // Collect extra metadata fields that are in passMetadata but not in standard fields
        val extraMetadata = mutableMapOf<String, String>()
        data.forEach { (key, values) ->
            val lowerKey = key.lowercase()
            if (lowerKey in passMetadata && lowerKey !in standardFields) {
                // Join multiple values with comma if present
                extraMetadata[key] = values.joinToString(", ")
            }
        }

        return CodelabMetadata(
            summary = data["summary"]?.firstOrNull() ?: "",
            url = data["id"]?.firstOrNull() ?: "",
            category = data["categories"]?.firstOrNull()?.split(",")?.map { it.trim() } ?: emptyList(),
            environment = data["environments"]?.firstOrNull()?.split(",")?.map { it.trim() } ?: listOf("Web"),
            status = data["status"]?.firstOrNull()?.split(",")?.map { it.trim() } ?: listOf("Published"),
            feedbackLink = data["feedback_link"]?.firstOrNull() ?: "",
            analyticsAccount = data["analytics_account"]?.firstOrNull() ?: "",
            tags = data["tags"]?.firstOrNull()?.split(",")?.map { it.trim() } ?: emptyList(),
            authors = data["authors"]?.firstOrNull()?.split(",")?.map { it.trim() } ?: emptyList(),
            source = data["source"]?.firstOrNull() ?: "",
            duration = data["duration"]?.firstOrNull()?.toIntOrNull() ?: 0,
            extraMetadata = extraMetadata
        )
    }

    private fun extractTitle(document: Document): String {
        var title = "Untitled Codelab"
        var node: FlexmarkNode? = document.firstChild
        
        while (node != null) {
            if (node is com.vladsch.flexmark.ast.Heading && node.level == 1) {
                title = node.text.toString()
                break
            }
            node = node.next
        }
        
        return title
    }

    private fun extractSteps(document: Document): List<Step> {
        val steps = mutableListOf<Step>()
        var currentStep: MutableList<CodelabNode>? = null
        var currentStepTitle = ""
        var currentStepDuration = 1
        var currentStepEnv = listOf("Web", "Kiosk")

        var node: FlexmarkNode? = document.firstChild
        var skipUntilFirstH2 = false
        var foundTitle = false
        var skipNextIfDuration = false

        while (node != null) {
            when {
                node is com.vladsch.flexmark.ast.Heading && node.level == 1 -> {
                    foundTitle = true
                    skipUntilFirstH2 = true
                }
                node is com.vladsch.flexmark.ast.Heading && node.level == 2 -> {
                    // Save previous step if exists
                    if (currentStep != null && currentStepTitle.isNotEmpty()) {
                        steps.add(Step(
                            title = currentStepTitle,
                            content = currentStep,
                            duration = currentStepDuration,
                            environment = currentStepEnv
                        ))
                    }

                    // Start new step
                    currentStepTitle = node.text.toString()
                    currentStep = mutableListOf()
                    currentStepDuration = 1
                    currentStepEnv = listOf("Web", "Kiosk")
                    skipUntilFirstH2 = false
                    skipNextIfDuration = true
                }
                !skipUntilFirstH2 && currentStep != null -> {
                    // Check if this is a Duration paragraph
                    if (skipNextIfDuration && node is com.vladsch.flexmark.ast.Paragraph) {
                        val text = node.chars.toString().trim()
                        val durationMatch = Regex("^Duration:\\s*(\\d+)$").find(text)
                        if (durationMatch != null) {
                            currentStepDuration = durationMatch.groupValues[1].toIntOrNull() ?: 1
                            skipNextIfDuration = false
                            node = node.next
                            continue
                        }
                    }
                    skipNextIfDuration = false

                    // Parse node content and add to current step
                    val parsedNodes = parseNode(node)
                    currentStep.addAll(parsedNodes)
                }
            }

            node = node.next
        }

        // Add last step
        if (currentStep != null && currentStepTitle.isNotEmpty()) {
            steps.add(Step(
                title = currentStepTitle,
                content = currentStep,
                duration = currentStepDuration,
                environment = currentStepEnv
            ))
        }

        return steps
    }

    private fun parseNode(node: FlexmarkNode): List<CodelabNode> {
        val nodes = mutableListOf<CodelabNode>()
        
        when (node) {
            is com.vladsch.flexmark.ast.Paragraph -> {
                val textNodes = mutableListOf<CodelabNode>()
                extractTextNodes(node, textNodes)
                if (textNodes.isNotEmpty()) {
                    nodes.add(CodelabNode.Paragraph(textNodes))
                }
            }
            is com.vladsch.flexmark.ast.Heading -> {
                if (node.level >= 2) {
                    nodes.add(CodelabNode.Heading(
                        level = node.level,
                        content = node.text.toString()
                    ))
                }
            }
            is com.vladsch.flexmark.ast.BulletList -> {
                val items = mutableListOf<List<CodelabNode>>()
                var listItem: FlexmarkNode? = node.firstChild
                while (listItem != null) {
                    if (listItem is com.vladsch.flexmark.ast.BulletListItem) {
                        val itemNodes = mutableListOf<CodelabNode>()
                        extractTextNodes(listItem, itemNodes)
                        items.add(itemNodes)
                    }
                    listItem = listItem.next
                }
                nodes.add(CodelabNode.BulletList(items))
            }
            is com.vladsch.flexmark.ast.OrderedList -> {
                val items = mutableListOf<List<CodelabNode>>()
                var listItem: FlexmarkNode? = node.firstChild
                while (listItem != null) {
                    if (listItem is com.vladsch.flexmark.ast.OrderedListItem) {
                        val itemNodes = mutableListOf<CodelabNode>()
                        extractTextNodes(listItem, itemNodes)
                        items.add(itemNodes)
                    }
                    listItem = listItem.next
                }
                nodes.add(CodelabNode.NumberedList(items, start = node.startNumber))
            }
            is com.vladsch.flexmark.ast.FencedCodeBlock -> {
                nodes.add(CodelabNode.CodeBlock(
                    code = node.contentChars.toString(),
                    language = node.info.toString()
                ))
            }
            is com.vladsch.flexmark.ast.IndentedCodeBlock -> {
                nodes.add(CodelabNode.CodeBlock(
                    code = node.contentChars.toString()
                ))
            }
            is com.vladsch.flexmark.ast.Image -> {
                val url = node.url.toString()
                nodes.add(CodelabNode.Image(
                    src = url,
                    alt = node.text.toString()
                ))
            }
        }
        
        return nodes
    }

    private fun extractTextNodes(node: FlexmarkNode, output: MutableList<CodelabNode>) {
        var child: FlexmarkNode? = node.firstChild
        
        while (child != null) {
            when (child) {
                is com.vladsch.flexmark.ast.Text -> {
                    output.add(CodelabNode.Text(child.chars.toString()))
                }
                is com.vladsch.flexmark.ast.StrongEmphasis -> {
                    val text = child.text.toString()
                    output.add(CodelabNode.Text(text, bold = true))
                }
                is com.vladsch.flexmark.ast.Emphasis -> {
                    val text = child.text.toString()
                    output.add(CodelabNode.Text(text, italic = true))
                }
                is com.vladsch.flexmark.ast.Code -> {
                    output.add(CodelabNode.Text(child.text.toString(), code = true))
                }
                is com.vladsch.flexmark.ast.Link -> {
                    output.add(CodelabNode.Link(
                        url = child.url.toString(),
                        text = child.text.toString()
                    ))
                }
                is com.vladsch.flexmark.ast.Image -> {
                    output.add(CodelabNode.Image(
                        src = child.url.toString(),
                        alt = child.text.toString()
                    ))
                }
                else -> {
                    // Recursively extract from nested nodes
                    extractTextNodes(child, output)
                }
            }
            child = child.next
        }
    }
}
