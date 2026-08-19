package me.kartikarora.klaat.core

data class Codelab(
    val id: String,
    val title: String,
    val summary: String = "",
    val steps: List<Step>,
    val metadata: CodelabMetadata = CodelabMetadata(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CodelabMetadata(
    val summary: String = "",
    val url: String = "",
    val category: List<String> = emptyList(),
    val environment: List<String> = listOf("Web"),
    val status: List<String> = listOf("Published"),
    val feedbackLink: String = "",
    val analyticsAccount: String = "",
    val tags: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val source: String = "",
    val duration: Int = 0,
    val image: String = "",
    val favicon: String = "",
    val footer: String = "",
    val extraMetadata: Map<String, String> = emptyMap(),
    val theme: String = ""
)

data class Step(
    val title: String,
    val content: List<Node>,
    /** null = no Duration line present; positive = normal duration */
    val duration: Int? = null,
    val excludeDurationFromTotal: Boolean = false,
    val environment: List<String> = listOf("Web", "Kiosk")
)

sealed class Node {
    data class Text(val content: String, val bold: Boolean = false, val italic: Boolean = false, val code: Boolean = false) : Node()
    data class Heading(val level: Int, val content: String, val link: String? = null) : Node()
    data class Paragraph(val nodes: List<Node>) : Node()
    data class CodeBlock(val code: String, val language: String = "", val filename: String? = null, val githubLink: String? = null) : Node()
    data class CommandBlock(val commands: String) : Node()
    data class InfoBox(val type: InfoBoxType, val nodes: List<Node>) : Node()
    data class BulletList(val items: List<List<Node>>) : Node()
    data class NumberedList(val items: List<List<Node>>, val start: Int = 1) : Node()
    data class Image(val src: String, val alt: String = "", val maxWidth: Int? = null) : Node()
    data class YoutubeVideo(val videoId: String) : Node()
    data class IFrame(val src: String) : Node()
    data class Link(val url: String, val text: String) : Node()
    data class Button(
        val url: String,
        val text: String,
        val type: ButtonType = ButtonType.Primary,
        val download: Boolean = false,
        val icon: String? = null
    ) : Node()
    data class Survey(val question: String, val options: List<String>) : Node()
    data class FAQ(val links: List<Link>) : Node()
    data class Table(
        val header: List<List<Node>>,
        val rows: List<List<List<Node>>>,
        /** Per-column alignment: "left", "center", "right", or null when unspecified. */
        val alignments: List<String?> = emptyList()
    ) : Node()
}

sealed interface ButtonType {
    data object Primary : ButtonType
    data object Secondary : ButtonType
}

enum class InfoBoxType {
    POSITIVE,  // Green - tips, best practices (Google Docs) (Klaat Markdown {.positive})
    WARNING,  // Orange - warnings, restrictions (Google Docs) (Klaat Markdown {.warning})
    SPECIAL,   // Blue - tips, supplementary info (Klaat Markdown {.special})
    CRITICAL,  // Red - mistakes, breaking changes (Klaat Markdown {.critical})
    DEFAULT    // Default
}