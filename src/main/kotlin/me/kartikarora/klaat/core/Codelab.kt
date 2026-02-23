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
    val extraMetadata: Map<String, String> = emptyMap()
)

data class Step(
    val title: String,
    val content: List<Node>,
    val duration: Int = 1,
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
    data class NumberedList(val items: List<List<Node>>) : Node()
    data class Image(val src: String, val alt: String = "", val maxWidth: Int? = null) : Node()
    data class YoutubeVideo(val videoId: String) : Node()
    data class IFrame(val src: String) : Node()
    data class Link(val url: String, val text: String) : Node()
    data class Button(val url: String, val text: String, val download: Boolean = false) : Node()
    data class Survey(val question: String, val options: List<String>) : Node()
    data class FAQ(val links: List<Link>) : Node()
}

enum class InfoBoxType {
    POSITIVE,  // Green - tips, best practices
    NEGATIVE,  // Orange - warnings, restrictions
    REGULAR    // Default
}