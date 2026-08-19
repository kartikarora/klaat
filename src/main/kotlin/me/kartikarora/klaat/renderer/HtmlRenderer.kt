package me.kartikarora.klaat.renderer

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Node
import java.io.File
import java.io.StringWriter
import java.time.Instant

/**
 * Renders a Codelab to HTML format matching the original claat output exactly
 */
class HtmlRenderer {

    private val freemarkerConfig = Configuration(Configuration.VERSION_2_3_32).apply {
        setClassForTemplateLoading(HtmlRenderer::class.java, "/templates")
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
    }

    companion object {
        const val DEFAULT_FOOTER = """Codelab generated using <a href="https://klaat.kartikarora.me" target="_blank" rel="noopener">Klaat</a> - A tool made by <a href="https://kartikarora.me" target="_blank" rel="noopener">Kartik Arora</a>"""
    }

    fun render(
        codelab: Codelab,
        outputDir: File,
        ga: String? = null,
        prefix: String? = null,
        environment: String = "web",
        customCss: String? = null,
        favicon: String? = null,
        googleFonts: String? = null,
        footer: String? = null
    ) {
        val codelabDir = File(outputDir, codelab.id)
        codelabDir.mkdirs()

        // Determine asset prefixes - always use CDN URL by default unless a prefix is provided
        val (jsPrefix, cssPrefix) = when {
            prefix != null -> {
                val trimmed = prefix.trimEnd('/')
                if (trimmed.isEmpty()) {
                    Pair("js", "css")
                } else {
                    Pair("$trimmed/js", "$trimmed/css")
                }
            }
            else -> Pair("https://distribute.kartikarora.me/js", "https://distribute.kartikarora.me/css")
        }

        val isCustomCssUrl = customCss?.let { it.startsWith("http://") || it.startsWith("https://") } ?: false
        val customCssUrl = if (isCustomCssUrl) customCss else null

        // Create index.html using FreeMarker template
        val indexHtml = codelabDir.resolve("index.html")
        indexHtml.writeText(renderIndexHtml(codelab, ga, jsPrefix, cssPrefix, customCssUrl, favicon, googleFonts, footer))

        // Write codelab.json metadata
        val metadataFile = codelabDir.resolve("codelab.json")
        metadataFile.writeText(renderMetadataJson(codelab, environment))

        println("✓ Exported: ${codelab.id}")
    }

    private fun renderIndexHtml(
        codelab: Codelab,
        ga: String?,
        jsPrefix: String,
        cssPrefix: String,
        customCssUrl: String?,
        favicon: String?,
        googleFonts: String?,
        footer: String?
    ): String {
        val effectiveFooter = when {
            !footer.isNullOrBlank() -> footer
            codelab.metadata.footer.isNotBlank() -> codelab.metadata.footer
            else -> DEFAULT_FOOTER
        }

        val template = freemarkerConfig.getTemplate("kodelab.html")
        val dataModel = mapOf(
            "title" to codelab.title,
            "id" to codelab.id,
            "environment" to codelab.metadata.environment.joinToString(","),
            "feedbackLink" to codelab.metadata.feedbackLink,
            "globalGA" to (ga ?: ""),
            "codelabGA" to codelab.metadata.analyticsAccount,
            "jsPrefix" to jsPrefix,
            "cssPrefix" to cssPrefix,
            "customCssUrl" to (customCssUrl ?: ""),
            "favicon" to (if (!favicon.isNullOrBlank()) favicon else codelab.metadata.favicon),
            "footer" to effectiveFooter,
            "googleFonts" to (googleFonts ?: ""),
            "theme" to codelab.metadata.theme,
            "timestamp" to System.currentTimeMillis().toString(),
            "steps" to codelab.steps.map { step ->
                mapOf(
                    "title" to step.title,
                    "duration" to step.duration,
                    "hasDuration" to (step.duration != null),
                    "excludeDuration" to step.excludeDurationFromTotal,
                    "content" to renderStepContent(step.content)
                )
            }
        )

        val writer = StringWriter()
        template.process(dataModel, writer)
        return writer.toString()
    }

    private fun renderStepContent(nodes: List<Node>): String {
        return buildString {
            nodes.forEach { node ->
                append(renderNode(node))
                append("\n")
            }
        }
    }

    private fun renderNode(node: Node): String {
        return when (node) {
            is Node.Text -> renderText(node)
            is Node.Heading -> renderHeading(node)
            is Node.Paragraph -> {
                val isImageWithCaption = node.nodes.count { it is Node.Image } == 1 &&
                        node.nodes.all { it is Node.Image || (it is Node.Text && (it.italic || it.content.isBlank())) }
                if (isImageWithCaption && node.nodes.any { it is Node.Text && it.italic }) {
                    val imageNode = node.nodes.first { it is Node.Image }
                    val captionNodes = node.nodes.filter { it != imageNode && (it is Node.Text && it.content.isNotBlank()) }
                    "<figure>\n${renderNode(imageNode)}\n<figcaption>${renderNodes(captionNodes)}</figcaption>\n</figure>"
                } else {
                    "<p>${renderNodes(node.nodes)}</p>"
                }
            }
            is Node.CodeBlock -> renderCodeBlock(node)
            is Node.CommandBlock -> """<pre class="devsite-terminal"><code>${escape(node.commands)}</code></pre>"""
            is Node.InfoBox -> renderInfoBox(node)
            is Node.BulletList -> "<ul>\n${node.items.joinToString("\n") { "<li>${renderNodes(it)}</li>" }}\n</ul>"
            is Node.NumberedList -> {
                val startAttr = if (node.start > 1) """ start="${node.start}"""" else ""
                "<ol$startAttr>\n${node.items.joinToString("\n") { "<li>${renderNodes(it)}</li>" }}\n</ol>"
            }
            is Node.Image -> renderImage(node)
            is Node.YoutubeVideo -> """<iframe class="youtube-video" src="https://www.youtube.com/embed/${node.videoId}" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>"""
            is Node.IFrame -> """<iframe src="${escape(node.src)}"></iframe>"""
            is Node.Link -> """<a href="${escape(node.url)}" target="_blank">${escape(node.text)}</a>"""
            is Node.Button -> renderButton(node)
            is Node.Survey -> renderSurvey(node)
            is Node.FAQ -> renderFAQ(node)
            is Node.Table -> renderTable(node)
        }
    }

    private fun renderNodes(nodes: List<Node>): String {
        return nodes.joinToString("") { renderNode(it) }
    }

    private fun renderText(node: Node.Text): String {
        var text = escape(node.content)
        if (node.code) text = "<code>$text</code>"
        if (node.bold) text = "<strong>$text</strong>"
        if (node.italic) text = "<em>$text</em>"
        return text
    }

    private fun renderHeading(node: Node.Heading): String {
        val tag = "h${node.level}"
        return if (node.link != null) {
            """<$tag><a href="${escape(node.link)}" target="_blank">${escape(node.content)}</a></$tag>"""
        } else {
            "<$tag>${escape(node.content)}</$tag>"
        }
    }

    private fun renderCodeBlock(node: Node.CodeBlock): String {
        val header = if (node.filename != null) {
            if (node.githubLink != null) {
                """<h3><a href="${escape(node.githubLink)}" target="_blank">${escape(node.filename)}</a></h3>"""
            } else {
                "<h3>${escape(node.filename)}</h3>"
            }
        } else ""

        val lang = if (node.language.isNotEmpty()) " language-${node.language}" else ""
        val codeContent = escape(node.code)

        return """$header<div class="code-container"><pre><code class="$lang">$codeContent</code></pre><button type="button" class="copy-button" onclick="copyToClipboard(this)"><span class="material-symbols-outlined">content_copy</span><span>Copy</span></button></div>"""
    }

    private fun renderInfoBox(node: Node.InfoBox): String {
        val cssClass = when (node.type) {
            InfoBoxType.SPECIAL -> "special"
            InfoBoxType.WARNING -> "warning"
            InfoBoxType.POSITIVE -> "positive"
            InfoBoxType.CRITICAL -> "critical"
            else -> "default"
        }
        return """<aside class="$cssClass">${renderNodes(node.nodes)}</aside>"""
    }

    private fun renderImage(node: Node.Image): String {
        val style = if (node.maxWidth != null) """ style="width: ${node.maxWidth}px"""" else ""
        val alt = if (node.alt.isNotEmpty()) """ alt="${escape(node.alt)}"""" else ""
        return """<img$alt$style src="${escape(node.src)}">"""
    }

    private fun renderButton(node: Node.Button): String {
        val iconSpan = when {
            node.icon != null -> """<span class="material-symbols-outlined">${escape(node.icon)}</span>"""
            node.download -> """<span class="material-symbols-outlined">download</span>"""
            else -> ""
        }
        val typeClass = when (node.type) {
            is me.kartikarora.klaat.core.ButtonType.Primary -> "klaat-button--primary klaat-button--colored"
            is me.kartikarora.klaat.core.ButtonType.Secondary -> "klaat-button--secondary"
        }
        return """<a class="klaat-button $typeClass" href="${escape(node.url)}" target="_blank">$iconSpan${escape(node.text)}</a>"""
    }

    private fun renderSurvey(node: Node.Survey): String {
        val options = node.options.joinToString("\n") { option ->
            """<label class="klaat-radio-label"><input type="radio" name="survey-${node.question.hashCode()}"> ${escape(option)}</label>"""
        }
        return """
<klaat-kodelab-survey survey-id="survey-${node.question.hashCode()}">
<h4>${escape(node.question)}</h4>
<div class="klaat-radio-group">
$options
</div>
</klaat-kodelab-survey>""".trimIndent()
    }

    private fun renderFAQ(node: Node.FAQ): String {
        val links = node.links.joinToString("\n") { link ->
            """<li><a href="${escape(link.url)}" target="_blank">${escape(link.text)}</a></li>"""
        }
        return """<h3>Frequently Asked Questions</h3><ul>$links</ul>"""
    }
    
    private fun renderTable(node: Node.Table): String {
        return buildString {
            append("<table>\n")
            if (node.header.isNotEmpty()) {
                append("<thead>\n<tr>\n")
                node.header.forEachIndexed { colIdx, cell ->
                    val align = node.alignments.getOrNull(colIdx)
                    val style = if (align != null) """ style="text-align: $align"""" else ""
                    append("<th$style>${renderNodes(cell)}</th>")
                }
                append("\n</tr>\n</thead>\n")
            }
            append("<tbody>\n")
            node.rows.forEach { row ->
                append("<tr>\n")
                row.forEachIndexed { colIdx, cell ->
                    val align = node.alignments.getOrNull(colIdx)
                    val style = if (align != null) """ style="text-align: $align"""" else ""
                    append("<td$style>${renderNodes(cell)}</td>")
                }
                append("\n</tr>\n")
            }
            append("</tbody>\n")
            append("</table>")
        }
    }

    private fun renderMetadataJson(codelab: Codelab, environment: String): String {
        val baseJson = buildString {
            appendLine("{")
            appendLine("""  "environment": "$environment",""")
            appendLine("""  "format": "html",""")
            appendLine("""  "prefix": "../../",""")
            appendLine("""  "mainga": "${codelab.metadata.analyticsAccount}",""")
            appendLine("""  "updated": "${Instant.ofEpochMilli(codelab.lastUpdated)}",""")
            appendLine("""  "id": "${escapeJson(codelab.id)}",""")
            appendLine("""  "duration": ${codelab.steps.sumOf { if (it.excludeDurationFromTotal) 0 else (it.duration ?: 0) }},""")
            appendLine("""  "title": "${escapeJson(codelab.title)}",""")
            appendLine("""  "summary": "${escapeJson(codelab.summary)}",""")
            appendLine("""  "source": "${escapeJson(codelab.metadata.source)}",""")
            appendLine("""  "theme": "${escapeJson(codelab.metadata.theme)}",""")
            appendLine("""  "status": ${renderJsonArray(codelab.metadata.status)},""")
            appendLine("""  "category": ${renderJsonArray(codelab.metadata.category)},""")
            appendLine("""  "tags": ${renderJsonArray(codelab.metadata.tags)},""")
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

    private fun escape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
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
