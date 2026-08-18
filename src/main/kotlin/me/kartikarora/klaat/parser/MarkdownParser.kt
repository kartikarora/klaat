package me.kartikarora.klaat.parser

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.tables.TableBlock
import com.vladsch.flexmark.ext.tables.TableHead
import com.vladsch.flexmark.ext.tables.TableBody
import com.vladsch.flexmark.ext.tables.TableRow
import com.vladsch.flexmark.ext.tables.TableCell
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
import me.kartikarora.klaat.core.Node as KlaatNode
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.BulletListItem
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.ast.OrderedListItem
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.IndentedCodeBlock
import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ast.HtmlBlock
import com.vladsch.flexmark.ast.HtmlInline
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.Link

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
        return parseContent(content, passMetadata, null)
    }

    fun parse(file: File, passMetadata: Set<String> = emptySet()): Codelab {
        val content = file.readText()
        val projectDir = File(System.getProperty("user.dir"))
        val sourcePath = file.absoluteFile.relativeToOrNull(projectDir)?.path ?: file.absolutePath
        return parseContent(content, passMetadata, sourcePath)
    }

    private fun parseContent(content: String, passMetadata: Set<String>, sourcePath: String?): Codelab {
        val document = parser.parse(content)

        // Extract YAML frontmatter
        val metadata = extractMetadata(document, content, passMetadata, sourcePath)

        // Extract steps (H2 headings)
        val steps = extractSteps(document)

        val id = metadata.url.ifEmpty {
            metadata.summary.lowercase().replace(Regex("[^a-z0-9]+"), "-")
        }

        var finalMetadata = metadata
        if (finalMetadata.image.isEmpty()) {
            val firstImage = steps.asSequence()
                .flatMap { it.content.asSequence() }
                .flatMap { node ->
                    when (node) {
                        is KlaatNode.Image -> sequenceOf(node)
                        is KlaatNode.Paragraph -> node.nodes.filterIsInstance<KlaatNode.Image>().asSequence()
                        is KlaatNode.InfoBox -> node.nodes.filterIsInstance<KlaatNode.Image>().asSequence()
                        is KlaatNode.BulletList -> node.items.flatten().filterIsInstance<KlaatNode.Image>().asSequence()
                        is KlaatNode.NumberedList -> node.items.flatten().filterIsInstance<KlaatNode.Image>().asSequence()
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
            title = extractTitle(document),
            summary = metadata.summary,
            steps = steps,
            metadata = finalMetadata
        )
    }

    private fun extractMetadata(document: Document, content: String, passMetadata: Set<String>, sourcePath: String?): CodelabMetadata {
        val visitor = AbstractYamlFrontMatterVisitor()
        visitor.visit(document)
        val data = visitor.data

        // Standard metadata fields that are explicitly handled
        val standardFields = setOf(
            "summary", "id", "categories", "environments", "status",
            "feedback_link", "analytics_account", "tags", "authors",
            "source", "duration", "theme", "favicon"
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
            category = parseStringList(data["categories"]),
            environment = parseStringList(data["environments"]).ifEmpty { listOf("Web") },
            status = parseStringList(data["status"]).ifEmpty { listOf("Published") },
            feedbackLink = data["feedback_link"]?.firstOrNull() ?: "",
            analyticsAccount = data["analytics_account"]?.firstOrNull() ?: "",
            tags = parseStringList(data["tags"]),
            authors = parseStringList(data["authors"]),
            source = data["source"]?.firstOrNull() ?: sourcePath ?: "",
            theme = data["theme"]?.firstOrNull() ?: "",
            duration = data["duration"]?.firstOrNull()?.toIntOrNull() ?: 0,
            image = "",
            favicon = data["favicon"]?.firstOrNull() ?: "",
            extraMetadata = extraMetadata
        )
    }

    /**
     * Parse a list of strings from YAML frontmatter data.
     * YAML can represent lists in two ways:
     * 1. Inline with commas: "tags: foo, bar, baz" -> ["foo, bar, baz"] (single string)
     * 2. Native YAML list: "tags: foo, bar, baz" -> ["foo", "bar", "baz"] (multiple strings)
     *
     * This function handles both cases by checking if we have multiple items or need to split.
     */
    private fun parseStringList(values: List<String>?): List<String> {
        if (values.isNullOrEmpty()) return emptyList()

        // If YAML parser gave us multiple items, use them as-is (trimmed)
        if (values.size > 1) {
            return values.map { it.trim() }
        }

        // If we have a single item, check if it contains commas and split if needed
        val single = values.first()
        return if (single.contains(",")) {
            single.split(",").map { it.trim() }
        } else {
            listOf(single.trim())
        }
    }

    private fun extractTitle(document: Document): String {
        var title = "Untitled Codelab"
        var node: FlexmarkNode? = document.firstChild

        while (node != null) {
            if (node is Heading && node.level == 1) {
                title = node.text.toString()
                break
            }
            node = node.next
        }

        return title
    }

    private fun extractSteps(document: Document): List<Step> {
        val steps = mutableListOf<Step>()
        var currentStep: MutableList<KlaatNode>? = null
        var currentStepTitle = ""
        var currentStepDuration: Int? = null
        var currentStepExcludeDuration = false
        var currentStepEnv = listOf("Web", "Kiosk")

        var node: FlexmarkNode? = document.firstChild
        var skipUntilFirstH2 = false
        var foundTitle = false
        var skipNextIfDuration = false

        while (node != null) {
            when {
                node is Heading && node.level == 1 -> {
                    foundTitle = true
                    skipUntilFirstH2 = true
                }
                node is Heading && node.level == 2 -> {
                    // Save previous step if exists
                    if (currentStep != null && currentStepTitle.isNotEmpty()) {
                        steps.add(Step(
                            title = currentStepTitle,
                            content = currentStep,
                            duration = currentStepDuration,
                            excludeDurationFromTotal = currentStepExcludeDuration,
                            environment = currentStepEnv
                        ))
                    }

                    // Start new step
                    currentStepTitle = node.text.toString()
                    currentStep = mutableListOf()
                    currentStepDuration = null
                    currentStepExcludeDuration = false
                    currentStepEnv = listOf("Web", "Kiosk")
                    skipUntilFirstH2 = false
                    skipNextIfDuration = true
                }
                !skipUntilFirstH2 && currentStep != null -> {
                    // Check if this is a Duration paragraph
                    if (skipNextIfDuration && node is Paragraph) {
                        val text = node.chars.toString().trim()
                        val durationMatch = Regex("^Duration:\\s*(\\d+)(?:\\s+(?:\\(ignore\\)|ignore))?", RegexOption.IGNORE_CASE).find(text)
                        if (durationMatch != null) {
                            currentStepDuration = durationMatch.groupValues[1].toIntOrNull()
                            currentStepExcludeDuration = text.contains("ignore", ignoreCase = true)
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
                excludeDurationFromTotal = currentStepExcludeDuration,
                environment = currentStepEnv
            ))
        }

        return steps
    }

    private fun parseNode(node: FlexmarkNode): List<KlaatNode> {
        val nodes = mutableListOf<KlaatNode>()
        
        when (node) {
            is Paragraph -> {
                val textNodes = mutableListOf<KlaatNode>()
                extractTextNodes(node, textNodes)
                
                val nonWhitespaceNodes = textNodes.filter { 
                    it !is KlaatNode.Text || it.content.trim().isNotEmpty() 
                }
                
                if (nonWhitespaceNodes.size == 1 && nonWhitespaceNodes.first() is KlaatNode.Link) {
                    val link = nonWhitespaceNodes.first() as KlaatNode.Link
                    val youtubeRegex = Regex("^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/watch\\?v=|youtu\\.be\\/)([a-zA-Z0-9_-]{11})")
                    val match = youtubeRegex.find(link.url)
                    if (match != null) {
                        nodes.add(KlaatNode.YoutubeVideo(match.groupValues[1]))
                    } else {
                        nodes.add(KlaatNode.Button(url = link.url, text = link.text))
                    }
                } else if (textNodes.isNotEmpty()) {
                    nodes.add(KlaatNode.Paragraph(textNodes))
                }
            }
            is BlockQuote -> {
                val childNodes = mutableListOf<KlaatNode>()
                var bChild = node.firstChild
                while (bChild != null) {
                    childNodes.addAll(parseNode(bChild))
                    bChild = bChild.next
                }
                
                // Determine InfoBox type based on metadata attributes
                var type = InfoBoxType.DEFAULT
                
                fun cleanMetadata(nodes: MutableList<KlaatNode>) {
                    for (i in 0 until nodes.size) {
                        val node = nodes[i]
                        if (node is KlaatNode.Paragraph) {
                            val pNodes = node.nodes.toMutableList()
                            cleanMetadata(pNodes)
                            nodes[i] = KlaatNode.Paragraph(pNodes)
                        } else if (node is KlaatNode.Text) {
                            val content = node.content
                            if (content.contains("{.special}")) {
                                type = InfoBoxType.SPECIAL
                                nodes[i] = node.copy(content = content.replace("{.special}", "").trim())
                            } else if (content.contains("{.warning}")) {
                                type = InfoBoxType.WARNING
                                nodes[i] = node.copy(content = content.replace("{.warning}", "").trim())
                            } else if (content.contains("{.critical}")) {
                                type = InfoBoxType.CRITICAL
                                nodes[i] = node.copy(content = content.replace("{.critical}", "").trim())
                            } else if (content.contains("{.positive}")) {
                                type = InfoBoxType.POSITIVE
                                nodes[i] = node.copy(content = content.replace("{.positive}", "").trim())
                            }
                        }
                    }
                }
                
                cleanMetadata(childNodes)
                nodes.add(KlaatNode.InfoBox(type, childNodes))
            }
            is Heading -> {
                if (node.level >= 2) {
                    nodes.add(KlaatNode.Heading(
                        level = node.level,
                        content = node.text.toString()
                    ))
                }
            }
            is BulletList -> {
                val items = mutableListOf<List<KlaatNode>>()
                var listItem: FlexmarkNode? = node.firstChild
                while (listItem != null) {
                    if (listItem is BulletListItem) {
                        val itemNodes = mutableListOf<KlaatNode>()
                        var itemChild = listItem.firstChild
                        while (itemChild != null) {
                            itemNodes.addAll(parseNode(itemChild))
                            itemChild = itemChild.next
                        }
                        items.add(itemNodes)
                    }
                    listItem = listItem.next
                }
                nodes.add(KlaatNode.BulletList(items))
            }
            is OrderedList -> {
                val items = mutableListOf<List<KlaatNode>>()
                var listItem: FlexmarkNode? = node.firstChild
                while (listItem != null) {
                    if (listItem is OrderedListItem) {
                        val itemNodes = mutableListOf<KlaatNode>()
                        var itemChild = listItem.firstChild
                        while (itemChild != null) {
                            itemNodes.addAll(parseNode(itemChild))
                            itemChild = itemChild.next
                        }
                        items.add(itemNodes)
                    }
                    listItem = listItem.next
                }
                nodes.add(KlaatNode.NumberedList(items, start = node.startNumber))
            }
            is FencedCodeBlock -> {
                nodes.add(KlaatNode.CodeBlock(
                    code = node.contentChars.toString(),
                    language = node.info.toString()
                ))
            }
            is IndentedCodeBlock -> {
                nodes.add(KlaatNode.CodeBlock(
                    code = node.contentChars.toString()
                ))
            }
            is Image -> {
                val url = node.url.toString()
                nodes.add(KlaatNode.Image(
                    src = url,
                    alt = node.text.toString()
                ))
            }
            is HtmlBlock -> {
                val html = node.chars.toString()
                parseHtmlForMedia(html, nodes)
            }
            is HtmlInline -> {
                val html = node.chars.toString()
                parseHtmlForMedia(html, nodes)
            }
            is TableBlock -> {
                val header = mutableListOf<List<KlaatNode>>()
                val alignments = mutableListOf<String?>()
                val rows = mutableListOf<List<List<KlaatNode>>>()

                var child = node.firstChild
                while (child != null) {
                    when (child) {
                        is TableHead -> {
                            var row = child.firstChild
                            while (row != null) {
                                if (row is TableRow) {
                                    var cell = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            val cellNodes = mutableListOf<KlaatNode>()
                                            extractTextNodes(cell, cellNodes)
                                            header.add(cellNodes)
                                            // Capture column alignment from the header cell
                                            val align = when (cell.alignment) {
                                                TableCell.Alignment.LEFT -> "left"
                                                TableCell.Alignment.CENTER -> "center"
                                                TableCell.Alignment.RIGHT -> "right"
                                                else -> null
                                            }
                                            alignments.add(align)
                                        }
                                        cell = cell.next
                                    }
                                }
                                row = row.next
                            }
                        }
                        is TableBody -> {
                            var row = child.firstChild
                            while (row != null) {
                                if (row is TableRow) {
                                    val rowCells = mutableListOf<List<KlaatNode>>()
                                    var cell = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            val cellNodes = mutableListOf<KlaatNode>()
                                            extractTextNodes(cell, cellNodes)
                                            rowCells.add(cellNodes)
                                        }
                                        cell = cell.next
                                    }
                                    rows.add(rowCells)
                                }
                                row = row.next
                            }
                        }
                    }
                    child = child.next
                }
                nodes.add(KlaatNode.Table(header, rows, alignments))
            }
        }
        
        return nodes
    }

    private fun extractTextNodes(
        node: FlexmarkNode,
        output: MutableList<KlaatNode>,
        bold: Boolean = false,
        italic: Boolean = false,
        code: Boolean = false
    ) {
        var child: FlexmarkNode? = node.firstChild
        
        while (child != null) {
            when (child) {
                is Text -> {
                    output.add(KlaatNode.Text(child.chars.toString(), bold = bold, italic = italic, code = code))
                }
                is StrongEmphasis -> {
                    extractTextNodes(child, output, bold = true, italic = italic, code = code)
                }
                is Emphasis -> {
                    extractTextNodes(child, output, bold = bold, italic = true, code = code)
                }
                is Code -> {
                    extractTextNodes(child, output, bold = bold, italic = italic, code = true)
                }
                is Link -> {
                    output.add(KlaatNode.Link(
                        url = child.url.toString(),
                        text = child.text.toString()
                    ))
                }
                is Image -> {
                    output.add(KlaatNode.Image(
                        src = child.url.toString(),
                        alt = child.text.toString()
                    ))
                }
                else -> {
                    extractTextNodes(child, output, bold, italic, code)
                }
            }
            child = child.next
        }
    }

    private fun parseHtmlForMedia(html: String, nodes: MutableList<KlaatNode>) {
        val videoRegex = Regex("<video\\s+[^>]*id=[\"']([a-zA-Z0-9_-]{11})[\"'][^>]*>(?:<\\/video>)?", RegexOption.IGNORE_CASE)
        var match = videoRegex.find(html)
        if (match != null) {
            nodes.add(KlaatNode.YoutubeVideo(match.groupValues[1]))
            return
        }

        val iframeRegex = Regex("<iframe\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>(?:<\\/iframe>)?", RegexOption.IGNORE_CASE)
        match = iframeRegex.find(html)
        if (match != null) {
            val src = match.groupValues[1]
            val ytMatch = Regex("youtube\\.com\\/embed\\/([a-zA-Z0-9_-]{11})").find(src)
            if (ytMatch != null) {
                nodes.add(KlaatNode.YoutubeVideo(ytMatch.groupValues[1]))
            } else {
                nodes.add(KlaatNode.IFrame(src))
            }
            return
        }
    }
}
