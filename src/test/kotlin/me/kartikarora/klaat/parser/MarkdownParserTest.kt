package me.kartikarora.klaat.parser

import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Node
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserTest {

    private val parser = MarkdownParser()

    @Test
    fun `test parse simple markdown with title and one step`() {
        val markdown = """
            # My Codelab

            ## Step 1

            This is the first step.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("My Codelab", codelab.title)
        assertEquals(1, codelab.steps.size)
        assertEquals("Step 1", codelab.steps[0].title)
    }

    @Test
    fun `test parse markdown with YAML frontmatter`() {
        val markdown = """
            ---
            summary: This is a test codelab
            id: test-codelab-id
            categories: Android, Kotlin
            environments: Web, Kiosk
            status: Published
            feedback_link: https://example.com/feedback
            analytics_account: UA-12345
            tags: beginner, tutorial
            authors: John Doe, Jane Smith
            duration: 30
            ---

            # Test Codelab

            ## Introduction

            Welcome to the codelab.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("Test Codelab", codelab.title)
        assertEquals("This is a test codelab", codelab.metadata.summary)
        assertEquals("test-codelab-id", codelab.id)
        assertEquals(listOf("Android", "Kotlin"), codelab.metadata.category)
        assertEquals(listOf("Web", "Kiosk"), codelab.metadata.environment)
        assertEquals(listOf("Published"), codelab.metadata.status)
        assertEquals("https://example.com/feedback", codelab.metadata.feedbackLink)
        assertEquals("UA-12345", codelab.metadata.analyticsAccount)
        assertEquals(listOf("beginner", "tutorial"), codelab.metadata.tags)
        assertEquals(listOf("John Doe", "Jane Smith"), codelab.metadata.authors)
        assertEquals(30, codelab.metadata.duration)
    }

    @Test
    fun `test parse multiple steps`() {
        val markdown = """
            # Codelab Title

            ## Step 1
            Content for step 1

            ## Step 2
            Content for step 2

            ## Step 3
            Content for step 3
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals(3, codelab.steps.size)
        assertEquals("Step 1", codelab.steps[0].title)
        assertEquals("Step 2", codelab.steps[1].title)
        assertEquals("Step 3", codelab.steps[2].title)
    }

    @Test
    fun `test parse paragraph with text`() {
        val markdown = """
            # Title

            ## Step 1

            This is a simple paragraph.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals(1, codelab.steps.size)
        val content = codelab.steps[0].content
        assertTrue(content.isNotEmpty())
        assertTrue(content[0] is Node.Paragraph)
        val paragraph = content[0] as Node.Paragraph
        assertTrue(paragraph.nodes.isNotEmpty())
    }

    @Test
    fun `test parse bold and italic text`() {
        val markdown = """
            # Title

            ## Step 1

            This is **bold** and this is *italic*.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val paragraph = codelab.steps[0].content[0] as Node.Paragraph
        val nodes = paragraph.nodes

        // Find bold and italic nodes
        val hasBold = nodes.any { it is Node.Text && it.bold }
        val hasItalic = nodes.any { it is Node.Text && it.italic }

        assertTrue(hasBold)
        assertTrue(hasItalic)
    }

    @Test
    fun `test parse inline code`() {
        val markdown = """
            # Title

            ## Step 1

            Use `println()` to print.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val paragraph = codelab.steps[0].content[0] as Node.Paragraph
        val hasCode = paragraph.nodes.any { it is Node.Text && it.code }
        assertTrue(hasCode)
    }

    @Test
    fun `test parse code block with language`() {
        val markdown = """
            # Title

            ## Step 1

            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val codeBlock = content.find { it is Node.CodeBlock } as? Node.CodeBlock
        assertNotNull(codeBlock)
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("fun main()"))
    }

    @Test
    fun `test parse code block without language`() {
        val markdown = """
            # Title

            ## Step 1

            ```
            Some code here
            ```
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val codeBlock = content.find { it is Node.CodeBlock } as? Node.CodeBlock
        assertNotNull(codeBlock)
        assertEquals("", codeBlock.language)
    }

    @Test
    fun `test parse bullet list`() {
        val markdown = """
            # Title

            ## Step 1

            * Item 1
            * Item 2
            * Item 3
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val bulletList = content.find { it is Node.BulletList } as? Node.BulletList
        assertNotNull(bulletList)
        assertEquals(3, bulletList.items.size)
    }

    @Test
    fun `test parse ordered list`() {
        val markdown = """
            # Title

            ## Step 1

            1. First item
            2. Second item
            3. Third item
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val numberedList = content.find { it is Node.NumberedList } as? Node.NumberedList
        assertNotNull(numberedList)
        assertEquals(3, numberedList.items.size)
    }

    @Test
    fun `test parse image`() {
        val markdown = """
            # Title

            ## Step 1

            ![Alt text](https://example.com/image.png)
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        // Images may be inside paragraphs or as standalone nodes
        val image = content.filterIsInstance<Node.Image>().firstOrNull()
            ?: content.filterIsInstance<Node.Paragraph>()
                .flatMap { it.nodes }
                .filterIsInstance<Node.Image>()
                .firstOrNull()

        assertNotNull(image)
        assertEquals("https://example.com/image.png", image.src)
        assertEquals("Alt text", image.alt)
    }

    @Test
    fun `test parse link`() {
        val markdown = """
            # Title

            ## Step 1

            Check out [this link](https://example.com).
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val paragraph = codelab.steps[0].content[0] as Node.Paragraph
        val link = paragraph.nodes.find { it is Node.Link } as? Node.Link
        assertNotNull(link)
        assertEquals("https://example.com", link.url)
        assertEquals("this link", link.text)
    }

    @Test
    fun `test parse heading inside step`() {
        val markdown = """
            # Title

            ## Step 1

            ### Subsection

            Content under subsection.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val heading = content.find { it is Node.Heading } as? Node.Heading
        assertNotNull(heading)
        assertEquals(3, heading.level)
        assertEquals("Subsection", heading.content)
    }

    @Test
    fun `test parse indented code block`() {
        val markdown = """
            # Title

            ## Step 1

                indented code
                more code
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        val codeBlock = content.find { it is Node.CodeBlock } as? Node.CodeBlock
        assertNotNull(codeBlock)
    }

    @Test
    fun `test parse codelab without frontmatter`() {
        val markdown = """
            # My Codelab

            ## Introduction

            This is a codelab without YAML frontmatter.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("My Codelab", codelab.title)
        assertEquals("", codelab.metadata.summary)
        assertNotNull(codelab.id)
    }

    @Test
    fun `test parse empty step content`() {
        val markdown = """
            # Title

            ## Empty Step
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals(1, codelab.steps.size)
        assertEquals("Empty Step", codelab.steps[0].title)
        assertEquals(0, codelab.steps[0].content.size)
    }

    @Test
    fun `test parse mixed content in step`() {
        val markdown = """
            # Title

            ## Complex Step

            Here's some text.

            ```kotlin
            fun test() {}
            ```

            * List item 1
            * List item 2

            ![Image](img.png)

            ### Subheading

            More text here.
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        val content = codelab.steps[0].content
        assertTrue(content.size > 1)

        val hasParagraph = content.any { it is Node.Paragraph }
        val hasCodeBlock = content.any { it is Node.CodeBlock }
        val hasBulletList = content.any { it is Node.BulletList }
        // Images may be standalone or inside paragraphs
        val hasImage = content.any { it is Node.Image } ||
            content.filterIsInstance<Node.Paragraph>().any { para ->
                para.nodes.any { it is Node.Image }
            }
        val hasHeading = content.any { it is Node.Heading }

        assertTrue(hasParagraph)
        assertTrue(hasCodeBlock)
        assertTrue(hasBulletList)
        assertTrue(hasImage)
        assertTrue(hasHeading)
    }

    @Test
    fun `test default metadata values when not specified`() {
        val markdown = """
            ---
            summary: Test
            ---
            # Title
            ## Step 1
            Content
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals(listOf("Web"), codelab.metadata.environment)
        assertEquals(listOf("Published"), codelab.metadata.status)
        assertEquals("", codelab.metadata.feedbackLink)
        assertEquals(0, codelab.metadata.duration)
    }
}
