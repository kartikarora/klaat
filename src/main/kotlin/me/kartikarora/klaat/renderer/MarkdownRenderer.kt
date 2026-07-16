package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Node
import java.io.File

/**
 * Renders a Codelab to Markdown format
 */
class MarkdownRenderer {

    fun render(codelab: Codelab, outputDir: File, environment: String = "web") {
        val codelabDir = File(outputDir, codelab.id)
        codelabDir.mkdirs()

        // Create index.md
        val indexMd = codelabDir.resolve("index.md")
        indexMd.writeText(renderMarkdown(codelab))

        // Write codelab.json metadata
        val metadataFile = codelabDir.resolve("codelab.json")
        metadataFile.writeText(renderMetadataJson(codelab, environment))

        println("✓ Exported: ${codelab.id}")
    }

    private fun renderMarkdown(codelab: Codelab): String {
        return buildString {
            // Frontmatter
            appendLine("---")
            appendLine("id: ${codelab.id}")
            appendLine("summary: ${codelab.summary}")
            if (codelab.metadata.category.isNotEmpty()) {
                appendLine("categories: ${codelab.metadata.category.joinToString(", ")}")
            }
            if (codelab.metadata.environment.isNotEmpty()) {
                appendLine("environments: ${codelab.metadata.environment.joinToString(", ")}")
            }
            if (codelab.metadata.status.isNotEmpty()) {
                appendLine("status: ${codelab.metadata.status.joinToString(", ")}")
            }
            if (codelab.metadata.feedbackLink.isNotEmpty()) {
                appendLine("feedback_link: ${codelab.metadata.feedbackLink}")
            }
            if (codelab.metadata.analyticsAccount.isNotEmpty()) {
                appendLine("analytics_account: ${codelab.metadata.analyticsAccount}")
            }
            if (codelab.metadata.tags.isNotEmpty()) {
                appendLine("tags: ${codelab.metadata.tags.joinToString(", ")}")
            }
            if (codelab.metadata.authors.isNotEmpty()) {
                appendLine("authors: ${codelab.metadata.authors.joinToString(", ")}")
            }
            appendLine("---")
            appendLine()

            // Title
            appendLine("# ${codelab.title}")
            appendLine()

            // Steps
            codelab.steps.forEach { step ->
                appendLine("## ${step.title}")
                appendLine()
                if (step.duration != null) {
                    val ignoreFlag = if (step.excludeDurationFromTotal) " ignore" else ""
                    appendLine("Duration: ${step.duration}$ignoreFlag")
                    appendLine()
                }
                appendLine(renderStepContent(step.content))
                appendLine()
            }
        }
    }

    private fun renderStepContent(nodes: List<Node>): String {
        return nodes.joinToString("\n\n") { node ->
            when (node) {
                is Node.Text -> renderText(node)
                is Node.Heading -> renderHeading(node)
                is Node.Paragraph -> renderParagraph(node)
                is Node.CodeBlock -> renderCodeBlock(node)
                is Node.CommandBlock -> renderCommandBlock(node)
                is Node.InfoBox -> renderInfoBox(node)
                is Node.BulletList -> renderBulletList(node)
                is Node.NumberedList -> renderNumberedList(node)
                is Node.Image -> renderImage(node)
                is Node.YoutubeVideo -> renderYoutubeVideo(node)
                is Node.IFrame -> renderIFrame(node)
                is Node.Link -> renderLink(node)
                is Node.Button -> renderButton(node)
                is Node.Survey -> renderSurvey(node)
                is Node.FAQ -> renderFAQ(node)
                is Node.Table -> renderTable(node)
            }
        }
    }

    private fun renderText(node: Node.Text): String {
        var text = node.content
        if (node.code) text = "`$text`"
        if (node.bold) text = "**$text**"
        if (node.italic) text = "*$text*"
        return text
    }

    private fun renderHeading(node: Node.Heading): String {
        val hashes = "#".repeat(node.level)
        return if (node.link != null) {
            "$hashes [${ node.content}](${node.link})"
        } else {
            "$hashes ${node.content}"
        }
    }

    private fun renderParagraph(node: Node.Paragraph): String {
        return renderStepContent(node.nodes)
    }

    private fun renderCodeBlock(node: Node.CodeBlock): String {
        val filename = if (node.filename != null) {
            if (node.githubLink != null) {
                "### [${node.filename}](${node.githubLink})\n\n"
            } else {
                "### ${node.filename}\n\n"
            }
        } else ""

        return "${filename}```${node.language}\n${node.code}\n```"
    }

    private fun renderCommandBlock(node: Node.CommandBlock): String {
        return "```bash\n${node.commands}\n```"
    }

    private fun renderInfoBox(node: Node.InfoBox): String {
        val prefix = when (node.type) {
            InfoBoxType.POSITIVE -> "✅"
            InfoBoxType.NEGATIVE -> "⚠️"
            InfoBoxType.REGULAR -> "ℹ️"
        }
        return "$prefix **Note:** ${renderStepContent(node.nodes)}"
    }

    private fun renderBulletList(node: Node.BulletList): String {
        return node.items.joinToString("\n") { item ->
            "* ${renderStepContent(item)}"
        }
    }

    private fun renderNumberedList(node: Node.NumberedList): String {
        return node.items.mapIndexed { index, item ->
            "${node.start + index}. ${renderStepContent(item)}"
        }.joinToString("\n")
    }

    private fun renderImage(node: Node.Image): String {
        return "![${node.alt}](${node.src})"
    }

    private fun renderYoutubeVideo(node: Node.YoutubeVideo): String {
        return "[![Video](https://img.youtube.com/vi/${node.videoId}/0.jpg)](https://www.youtube.com/watch?v=${node.videoId})"
    }

    private fun renderIFrame(node: Node.IFrame): String {
        return "[Embedded content](${node.src})"
    }

    private fun renderLink(node: Node.Link): String {
        return "[${node.text}](${node.url})"
    }

    private fun renderButton(node: Node.Button): String {
        val icon = if (node.download) "⬇️ " else ""
        return "[$icon${node.text}](${node.url})"
    }

    private fun renderSurvey(node: Node.Survey): String {
        val options = node.options.joinToString("\n") { option ->
            "- [ ] $option"
        }
        return "**${node.question}**\n\n$options"
    }

    private fun renderFAQ(node: Node.FAQ): String {
        val links = node.links.joinToString("\n") { link ->
            "* [${link.text}](${link.url})"
        }
        return "### Frequently Asked Questions\n\n$links"
    }

    private fun renderTable(node: Node.Table): String {
        return buildString {
            if (node.header.isNotEmpty()) {
                append("| ")
                append(node.header.joinToString(" | ") { renderStepContent(it) })
                append(" |\n")
                // Separator row with alignment markers
                append("| ")
                append(node.header.mapIndexed { colIdx, _ ->
                    when (node.alignments.getOrNull(colIdx)) {
                        "left" -> ":---"
                        "center" -> ":---:"
                        "right" -> "---:"
                        else -> "---"
                    }
                }.joinToString(" | "))
                append(" |\n")
            }
            node.rows.forEach { row ->
                append("| ")
                append(row.joinToString(" | ") { renderStepContent(it) })
                append(" |\n")
            }
        }
    }

    private fun renderMetadataJson(codelab: Codelab, environment: String): String {
        val baseJson = buildString {
            appendLine("{")
            appendLine("""  "environment": "$environment",""")
            appendLine("""  "format": "md",""")
            appendLine("""  "prefix": "../../",""")
            appendLine("""  "mainga": "",""")
            appendLine("""  "updated": "${java.time.Instant.ofEpochMilli(codelab.lastUpdated)}",""")
            appendLine("""  "id": "${escapeJson(codelab.id)}",""")
            appendLine("""  "title": "${escapeJson(codelab.title)}",""")
            appendLine("""  "summary": "${escapeJson(codelab.summary)}",""")
            appendLine("""  "source": "${escapeJson(codelab.metadata.source)}",""")
            appendLine("""  "category": ${renderJsonArray(codelab.metadata.category)},""")
            appendLine("""  "status": ${renderJsonArray(codelab.metadata.status)},""")
            appendLine("""  "tags": ${renderJsonArray(codelab.metadata.tags)},""")
            appendLine("""  "authors": ${renderJsonArray(codelab.metadata.authors)},""")
            appendLine("""  "feedback": "${escapeJson(codelab.metadata.feedbackLink)}",""")
            appendLine("""  "image": "${escapeJson(codelab.metadata.image)}",""")

            // Add extra metadata fields
            if (codelab.metadata.extraMetadata.isNotEmpty()) {
                codelab.metadata.extraMetadata.entries.forEach { (key, value) ->
                    appendLine("""  "${escapeJson(key)}": "${escapeJson(value)}",""")
                }
            }

            // url is the last field, no comma
            append("""  "url": "${escapeJson(codelab.metadata.url)}"""")
            appendLine()
            append("}")
        }

        return baseJson
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun renderJsonArray(items: List<String>): String {
        return if (items.isEmpty()) {
            "[]"
        } else {
            items.joinToString("\", \"", "[\"", "\"]") { escapeJson(it) }
        }
    }
}
