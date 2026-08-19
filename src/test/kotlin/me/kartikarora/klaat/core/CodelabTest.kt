package me.kartikarora.klaat.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CodelabTest {

    @Test
    fun `test Codelab creation with required fields`() {
        val step = Step(
            title = "Step 1",
            content = listOf(Node.Text("Content"))
        )
        val codelab = Codelab(
            id = "test-codelab",
            title = "Test Codelab",
            steps = listOf(step)
        )

        assertEquals("test-codelab", codelab.id)
        assertEquals("Test Codelab", codelab.title)
        assertEquals("", codelab.summary)
        assertEquals(1, codelab.steps.size)
        assertNotNull(codelab.metadata)
        assertNotNull(codelab.lastUpdated)
    }

    @Test
    fun `test Codelab with full metadata`() {
        val metadata = CodelabMetadata(
            summary = "Test Summary",
            url = "test-url",
            category = listOf("Android", "Kotlin"),
            environment = listOf("Web", "Kiosk"),
            status = listOf("Published"),
            feedbackLink = "https://feedback.com",
            analyticsAccount = "UA-12345",
            tags = listOf("beginner", "tutorial"),
            authors = listOf("John Doe", "Jane Smith"),
            source = "google-docs-id",
            duration = 45
        )

        val codelab = Codelab(
            id = "test-codelab",
            title = "Test Codelab",
            summary = "Test Summary",
            steps = emptyList(),
            metadata = metadata
        )

        assertEquals("Test Summary", codelab.summary)
        assertEquals(metadata, codelab.metadata)
        assertEquals(2, codelab.metadata.category.size)
        assertEquals("Android", codelab.metadata.category[0])
        assertEquals(45, codelab.metadata.duration)
    }

    @Test
    fun `test Step creation with default values`() {
        val step = Step(
            title = "Introduction",
            content = listOf(Node.Text("Welcome"))
        )

        assertEquals("Introduction", step.title)
        assertNull(step.duration)
        assertEquals(2, step.environment.size)
        assertEquals("Web", step.environment[0])
        assertEquals("Kiosk", step.environment[1])
    }

    @Test
    fun `test Step with custom duration and environment`() {
        val step = Step(
            title = "Setup",
            content = emptyList(),
            duration = 5,
            environment = listOf("Android")
        )

        assertEquals(5, step.duration)
        assertEquals(1, step.environment.size)
        assertEquals("Android", step.environment[0])
    }

    @Test
    fun `test Node Text with formatting`() {
        val plainText = Node.Text("plain")
        val boldText = Node.Text("bold", bold = true)
        val italicText = Node.Text("italic", italic = true)
        val codeText = Node.Text("code", code = true)
        val mixedText = Node.Text("mixed", bold = true, italic = true)

        assertEquals("plain", plainText.content)
        assertEquals(false, plainText.bold)
        assertEquals(true, boldText.bold)
        assertEquals(true, italicText.italic)
        assertEquals(true, codeText.code)
        assertEquals(true, mixedText.bold)
        assertEquals(true, mixedText.italic)
    }

    @Test
    fun `test Node Heading with different levels`() {
        val h2 = Node.Heading(2, "Section Header")
        val h3 = Node.Heading(3, "Subsection", "https://example.com")

        assertEquals(2, h2.level)
        assertEquals("Section Header", h2.content)
        assertEquals(null, h2.link)
        assertEquals("Subsection", h3.content)
        assertEquals("https://example.com", h3.link)
    }

    @Test
    fun `test Node Paragraph with nested nodes`() {
        val paragraph = Node.Paragraph(
            listOf(
                Node.Text("This is "),
                Node.Text("bold", bold = true),
                Node.Text(" text")
            )
        )

        assertEquals(3, paragraph.nodes.size)
        assertEquals("This is ", (paragraph.nodes[0] as Node.Text).content)
        assertEquals(true, (paragraph.nodes[1] as Node.Text).bold)
    }

    @Test
    fun `test Node CodeBlock with language`() {
        val codeBlock = Node.CodeBlock(
            code = "fun main() { println(\"Hello\") }",
            language = "kotlin",
            filename = "Main.kt",
            githubLink = "https://github.com/example/Main.kt"
        )

        assertEquals("fun main() { println(\"Hello\") }", codeBlock.code)
        assertEquals("kotlin", codeBlock.language)
        assertEquals("Main.kt", codeBlock.filename)
        assertEquals("https://github.com/example/Main.kt", codeBlock.githubLink)
    }

    @Test
    fun `test Node InfoBox types`() {
        val positive = Node.InfoBox(InfoBoxType.POSITIVE, listOf(Node.Text("Tip")))
        val negative = Node.InfoBox(InfoBoxType.WARNING, listOf(Node.Text("Warning")))
        val regular = Node.InfoBox(InfoBoxType.DEFAULT, listOf(Node.Text("Info")))

        assertEquals(InfoBoxType.POSITIVE, positive.type)
        assertEquals(InfoBoxType.WARNING, negative.type)
        assertEquals(InfoBoxType.DEFAULT, regular.type)
    }

    @Test
    fun `test Node BulletList`() {
        val bulletList = Node.BulletList(
            listOf(
                listOf(Node.Text("Item 1")),
                listOf(Node.Text("Item 2")),
                listOf(Node.Text("Item "), Node.Text("3", bold = true))
            )
        )

        assertEquals(3, bulletList.items.size)
        assertEquals("Item 1", (bulletList.items[0][0] as Node.Text).content)
        assertEquals(2, bulletList.items[2].size)
    }

    @Test
    fun `test Node NumberedList`() {
        val numberedList = Node.NumberedList(
            listOf(
                listOf(Node.Text("First")),
                listOf(Node.Text("Second"))
            )
        )

        assertEquals(2, numberedList.items.size)
    }

    @Test
    fun `test Node Image`() {
        val image = Node.Image(
            src = "image.png",
            alt = "Description",
            maxWidth = 800
        )

        assertEquals("image.png", image.src)
        assertEquals("Description", image.alt)
        assertEquals(800, image.maxWidth)
    }

    @Test
    fun `test Node YoutubeVideo`() {
        val video = Node.YoutubeVideo("dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", video.videoId)
    }

    @Test
    fun `test Node Link`() {
        val link = Node.Link("https://example.com", "Click here")
        assertEquals("https://example.com", link.url)
        assertEquals("Click here", link.text)
    }

    @Test
    fun `test Node Button`() {
        val button = Node.Button("https://download.com/file", "Download", type = ButtonType.PRIMARY, download = true)
        assertEquals("https://download.com/file", button.url)
        assertEquals("Download", button.text)
        assertEquals(ButtonType.PRIMARY, button.type)
        assertEquals(true, button.download)
    }

    @Test
    fun `test Node Survey`() {
        val survey = Node.Survey(
            "What do you think?",
            listOf("Great", "Good", "Okay", "Poor")
        )

        assertEquals("What do you think?", survey.question)
        assertEquals(4, survey.options.size)
    }

    @Test
    fun `test CodelabMetadata default values`() {
        val metadata = CodelabMetadata()

        assertEquals("", metadata.summary)
        assertEquals("", metadata.url)
        assertEquals(emptyList(), metadata.category)
        assertEquals(listOf("Web"), metadata.environment)
        assertEquals(listOf("Published"), metadata.status)
        assertEquals("", metadata.feedbackLink)
        assertEquals(0, metadata.duration)
        assertEquals(emptyMap(), metadata.extraMetadata)
    }
}
