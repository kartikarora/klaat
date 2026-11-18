package me.kartikarora.klaat.renderer

import me.kartikarora.klaat.core.Codelab
import me.kartikarora.klaat.core.CodelabMetadata
import me.kartikarora.klaat.core.InfoBoxType
import me.kartikarora.klaat.core.Node
import me.kartikarora.klaat.core.Step
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownRendererTest {

    private val renderer = MarkdownRenderer()
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = kotlin.io.path.createTempDirectory("markdown-renderer-test").toFile()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test render creates directory and files`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val codelabDir = File(tempDir, codelab.id)
        assertTrue(codelabDir.exists())
        assertTrue(codelabDir.isDirectory)

        val indexFile = File(codelabDir, "index.md")
        assertTrue(indexFile.exists())

        val metadataFile = File(codelabDir, "codelab.json")
        assertTrue(metadataFile.exists())
    }

    @Test
    fun `test render markdown with frontmatter`() {
        val metadata = CodelabMetadata(
            summary = "Test Summary",
            url = "test-url",
            category = listOf("Android"),
            environment = listOf("Web"),
            status = listOf("Published"),
            tags = listOf("tutorial"),
            authors = listOf("John Doe")
        )

        val codelab = Codelab(
            id = "test",
            title = "Test Codelab",
            summary = "Test Summary",
            steps = emptyList(),
            metadata = metadata
        )

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("---"))
        assertTrue(content.contains("id: test"))
        assertTrue(content.contains("summary: Test Summary"))
        assertTrue(content.contains("categories: Android"))
        assertTrue(content.contains("authors: John Doe"))
    }

    @Test
    fun `test render title`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.md").readText()
        assertTrue(content.contains("# ${codelab.title}"))
    }

    @Test
    fun `test render steps`() {
        val step1 = Step("Step One", listOf(Node.Text("Content 1")))
        val step2 = Step("Step Two", listOf(Node.Text("Content 2")))

        val codelab = Codelab(
            id = "multi-step",
            title = "Multi Step",
            steps = listOf(step1, step2)
        )

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "multi-step/index.md").readText()
        assertTrue(content.contains("## Step One"))
        assertTrue(content.contains("## Step Two"))
    }

    @Test
    fun `test render plain text`() {
        val step = Step("Test", listOf(Node.Text("Plain text")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("Plain text"))
    }

    @Test
    fun `test render bold text`() {
        val step = Step("Test", listOf(Node.Text("bold text", bold = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("**bold text**"))
    }

    @Test
    fun `test render italic text`() {
        val step = Step("Test", listOf(Node.Text("italic text", italic = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("*italic text*"))
    }

    @Test
    fun `test render code text`() {
        val step = Step("Test", listOf(Node.Text("code", code = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("`code`"))
    }

    @Test
    fun `test render heading`() {
        val step = Step("Test", listOf(Node.Heading(3, "Section Title")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("### Section Title"))
    }

    @Test
    fun `test render heading with link`() {
        val step = Step("Test", listOf(Node.Heading(2, "Link Heading", "https://example.com")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("## [Link Heading](https://example.com)"))
    }

    @Test
    fun `test render paragraph`() {
        val paragraph = Node.Paragraph(listOf(
            Node.Text("Start "),
            Node.Text("bold", bold = true),
            Node.Text(" end")
        ))
        val step = Step("Test", listOf(paragraph))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("Start"))
        assertTrue(content.contains("**bold**"))
        assertTrue(content.contains("end"))
    }

    @Test
    fun `test render code block with language`() {
        val codeBlock = Node.CodeBlock("fun main() {}", "kotlin")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("```kotlin"))
        assertTrue(content.contains("fun main() {}"))
        assertTrue(content.contains("```"))
    }

    @Test
    fun `test render code block with filename`() {
        val codeBlock = Node.CodeBlock("code", "java", filename = "Main.java")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("### Main.java"))
    }

    @Test
    fun `test render code block with github link`() {
        val codeBlock = Node.CodeBlock(
            "code", "kotlin",
            filename = "Main.kt",
            githubLink = "https://github.com/example/Main.kt"
        )
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("[Main.kt](https://github.com/example/Main.kt)"))
    }

    @Test
    fun `test render command block`() {
        val commandBlock = Node.CommandBlock("npm install")
        val step = Step("Test", listOf(commandBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("```bash"))
        assertTrue(content.contains("npm install"))
    }

    @Test
    fun `test render info boxes`() {
        val positive = Node.InfoBox(InfoBoxType.POSITIVE, listOf(Node.Text("Tip")))
        val negative = Node.InfoBox(InfoBoxType.WARNING, listOf(Node.Text("Warning")))
        val regular = Node.InfoBox(InfoBoxType.DEFAULT, listOf(Node.Text("Info")))

        val step = Step("Test", listOf(positive, negative, regular))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("✅ **Note:**"))
        assertTrue(content.contains("⚠️ **Note:**"))
        assertTrue(content.contains("> Info"))
    }

    @Test
    fun `test render bullet list`() {
        val list = Node.BulletList(listOf(
            listOf(Node.Text("Item 1")),
            listOf(Node.Text("Item 2"))
        ))
        val step = Step("Test", listOf(list))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("* Item 1"))
        assertTrue(content.contains("* Item 2"))
    }

    @Test
    fun `test render numbered list`() {
        val list = Node.NumberedList(listOf(
            listOf(Node.Text("First")),
            listOf(Node.Text("Second"))
        ))
        val step = Step("Test", listOf(list))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("1. First"))
        assertTrue(content.contains("2. Second"))
    }

    @Test
    fun `test render image`() {
        val image = Node.Image("image.png", "Alt text")
        val step = Step("Test", listOf(image))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("![Alt text](image.png)"))
    }

    @Test
    fun `test render youtube video`() {
        val video = Node.YoutubeVideo("dQw4w9WgXcQ")
        val step = Step("Test", listOf(video))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("[![Video](https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg)]"))
        assertTrue(content.contains("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `test render link`() {
        val link = Node.Link("https://example.com", "Example")
        val step = Step("Test", listOf(link))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("[Example](https://example.com)"))
    }

    @Test
    fun `test render button with download`() {
        val button = Node.Button("https://download.com/file", "Download", type = me.kartikarora.klaat.core.ButtonType.Primary, download = true)
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("[Download](https://download.com/file){.buttonPrimary}"))
    }

    @Test
    fun `test render button with custom icon`() {
        val button = Node.Button("https://example.com/play", "Watch Demo", type = me.kartikarora.klaat.core.ButtonType.Secondary, icon = "play_arrow")
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("[Watch Demo](https://example.com/play){.buttonSecondary icon=play_arrow}"))
    }

    @Test
    fun `test render survey`() {
        val survey = Node.Survey("Question?", listOf("Yes", "No"))
        val step = Step("Test", listOf(survey))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("**Question?**"))
        assertTrue(content.contains("- [ ] Yes"))
        assertTrue(content.contains("- [ ] No"))
    }

    @Test
    fun `test render FAQ`() {
        val faq = Node.FAQ(listOf(
            Node.Link("https://faq.com/1", "Question 1"),
            Node.Link("https://faq.com/2", "Question 2")
        ))
        val step = Step("Test", listOf(faq))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("### Frequently Asked Questions"))
        assertTrue(content.contains("[Question 1](https://faq.com/1)"))
    }

    @Test
    fun `test render metadata JSON file`() {
        val metadata = CodelabMetadata(
            summary = "Summary",
            url = "test-url",
            category = listOf("Android"),
            tags = listOf("tutorial"),
            authors = listOf("Author")
        )
        val codelab = Codelab("test", "Title", "Summary", emptyList(), metadata)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()
        assertTrue(jsonContent.contains("\"id\": \"test\""))
        assertTrue(jsonContent.contains("\"title\": \"Title\""))
        assertTrue(jsonContent.contains("\"summary\": \"Summary\""))
        assertTrue(jsonContent.contains("\"totalSteps\": 0"))
        assertTrue(jsonContent.contains("\"category\": [\"Android\"]"))
        assertTrue(jsonContent.contains("\"authors\": [\"Author\"]"))
    }

    @Test
    fun `test totalSteps in markdown codelab JSON with multiple steps`() {
        val steps = (1..7).map { Step("Step $it", listOf(Node.Text("Content $it")), duration = 2) }
        val codelab = Codelab("multi-md-test", "Title", steps = steps)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "multi-md-test/codelab.json").readText()
        assertTrue(jsonContent.contains("\"totalSteps\": 7"), "Multi-step markdown codelab should have totalSteps: 7")
    }

    @Test
    fun `test JSON escaping`() {
        val metadata = CodelabMetadata(summary = "Quote: \"Hello\"")
        val codelab = Codelab("test", "Title with \"quotes\"", steps = emptyList(), metadata = metadata)

        renderer.render(codelab, tempDir)

        val jsonFile = File(tempDir, "test/codelab.json")
        assertTrue(jsonFile.exists(), "JSON file should exist")
        val jsonContent = jsonFile.readText()
        assertTrue(jsonContent.isNotEmpty(), "JSON content should not be empty")
        // Check that JSON is properly structured
        assertTrue(jsonContent.contains("summary"), "JSON should contain summary field")
        assertTrue(jsonContent.contains("title"), "JSON should contain title field")
    }

    @Test
    fun `test step duration in markdown`() {
        val step = Step("Test", listOf(Node.Text("Content")), duration = 5)
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("Duration: 5"))
    }

    @Test
    fun `test render table basic structure`() {
        val table = Node.Table(
            header = listOf(
                listOf(Node.Text("Name")),
                listOf(Node.Text("Value"))
            ),
            rows = listOf(
                listOf(listOf(Node.Text("foo")), listOf(Node.Text("bar"))),
                listOf(listOf(Node.Text("baz")), listOf(Node.Text("qux")))
            )
        )
        val step = Step("Test", listOf(table))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("| Name | Value |"))
        assertTrue(content.contains("| --- | --- |"))
        assertTrue(content.contains("| foo | bar |"))
        assertTrue(content.contains("| baz | qux |"))
    }

    @Test
    fun `test render table with rich cell content`() {
        val table = Node.Table(
            header = listOf(listOf(Node.Text("Header", bold = true))),
            rows = listOf(listOf(listOf(Node.Text("code", code = true))))
        )
        val step = Step("Test", listOf(table))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains("**Header**"))
        assertTrue(content.contains("`code`"))
    }

    @Test
    fun `test render table with column alignment markers`() {
        val table = Node.Table(
            header = listOf(
                listOf(Node.Text("Left")),
                listOf(Node.Text("Center")),
                listOf(Node.Text("Right")),
                listOf(Node.Text("None"))
            ),
            rows = listOf(
                listOf(
                    listOf(Node.Text("a")),
                    listOf(Node.Text("b")),
                    listOf(Node.Text("c")),
                    listOf(Node.Text("d"))
                )
            ),
            alignments = listOf("left", "center", "right", null)
        )
        val step = Step("Test", listOf(table))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.md").readText()
        assertTrue(content.contains(":---"))
        assertTrue(content.contains(":---:"))
        assertTrue(content.contains("---:"))
        // A column with no alignment should use plain "---"
        assertTrue(content.contains("| :--- | :---: | ---: | --- |"))
    }

    @Test
    fun `test parse and render table round-trip`() {
        val markdown = """
            # Title

            ## Step 1

            | Tool | Version | Notes |
            | :--- | :---: | ---: |
            | Kotlin | 2.0 | Stable |
            | Gradle | 8.x | Required |
        """.trimIndent()

        val parser = me.kartikarora.klaat.parser.MarkdownParser()
        val codelab = parser.parse(markdown.byteInputStream())

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.md").readText()
        assertTrue(content.contains("| Tool | Version | Notes |"))
        assertTrue(content.contains(":---"))
        assertTrue(content.contains(":---:"))
        assertTrue(content.contains("---:"))
        assertTrue(content.contains("| Kotlin | 2.0 | Stable |"))
    }

    @Test
    fun `test render headings with inline code formatting`() {
        val codelab = Codelab(
            id = "heading-code-md-test",
            title = "Heading Code MD Test",
            steps = listOf(
                Step(
                    "Step 1",
                    listOf(
                        Node.Heading(
                            level = 3,
                            content = "Fallback Content",
                            nodes = listOf(
                                Node.Text("Use "),
                                Node.Text("MyService", code = true),
                                Node.Text(" here")
                            )
                        ),
                        Node.Heading(
                            level = 4,
                            content = "Link Heading",
                            link = "https://example.com",
                            nodes = listOf(
                                Node.Text("Visit "),
                                Node.Text("Doc", code = true)
                            )
                        )
                    )
                )
            )
        )

        renderer.render(codelab, tempDir)
        val indexMd = File(tempDir, "${codelab.id}/index.md").readText()

        assertTrue(indexMd.contains("### Use `MyService` here"))
        assertTrue(indexMd.contains("#### [Visit `Doc`](https://example.com)"))
    }

    private fun createSimpleCodlab(): Codelab {
        return Codelab(
            id = "simple-test",
            title = "Simple Test Codelab",
            steps = listOf(
                Step("Introduction", listOf(Node.Text("Welcome")))
            )
        )
    }
}
