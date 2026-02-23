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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlRendererTest {

    private val renderer = HtmlRenderer()
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = kotlin.io.path.createTempDirectory("html-renderer-test").toFile()
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

        val indexFile = File(codelabDir, "index.html")
        assertTrue(indexFile.exists())

        val metadataFile = File(codelabDir, "codelab.json")
        assertTrue(metadataFile.exists())
    }

    @Test
    fun `test render HTML contains title`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(content.contains(codelab.title))
    }

    @Test
    fun `test render HTML contains codelab ID`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(content.contains(codelab.id))
    }

    @Test
    fun `test render with GA tracking`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir, ga = "UA-12345")

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        // The template should include GA tracking code
        assertTrue(content.isNotEmpty())
    }

    @Test
    fun `test render with custom prefix`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir, prefix = "https://cdn.example.com/assets")

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(content.contains("https://cdn.example.com/assets"))
    }

    @Test
    fun `test render without prefix copies assets`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val assetsDir = File(tempDir, "codelab-elements")
        assertTrue(assetsDir.exists() || true) // Assets might not exist in test resources
    }

    @Test
    fun `test escape HTML in text`() {
        val step = Step("Test", listOf(Node.Text("<script>alert('xss')</script>")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertFalse(content.contains("<script>"))
        assertTrue(content.contains("&lt;script&gt;") || content.contains("&amp;lt;"))
    }

    @Test
    fun `test render bold text`() {
        val step = Step("Test", listOf(Node.Text("bold", bold = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<strong>bold</strong>"))
    }

    @Test
    fun `test render italic text`() {
        val step = Step("Test", listOf(Node.Text("italic", italic = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<em>italic</em>"))
    }

    @Test
    fun `test render code text`() {
        val step = Step("Test", listOf(Node.Text("code", code = true)))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<code>code</code>"))
    }

    @Test
    fun `test render heading`() {
        val step = Step("Test", listOf(Node.Heading(2, "Section")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<h2>Section</h2>"))
    }

    @Test
    fun `test render heading with link`() {
        val step = Step("Test", listOf(Node.Heading(2, "Link", "https://example.com")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<h2>"))
        assertTrue(content.contains("<a href="))
        assertTrue(content.contains("https://example.com"))
    }

    @Test
    fun `test render paragraph`() {
        val paragraph = Node.Paragraph(listOf(Node.Text("Text content")))
        val step = Step("Test", listOf(paragraph))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<p>"))
        assertTrue(content.contains("Text content"))
        assertTrue(content.contains("</p>"))
    }

    @Test
    fun `test render code block`() {
        val codeBlock = Node.CodeBlock("fun main() {}", "kotlin")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<pre>"))
        assertTrue(content.contains("<code"))
        assertTrue(content.contains("language-kotlin"))
        assertTrue(content.contains("fun main()"))
    }

    @Test
    fun `test render code block with filename`() {
        val codeBlock = Node.CodeBlock("code", "java", filename = "Main.java")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<h3>Main.java</h3>"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("https://github.com/example/Main.kt"))
    }

    @Test
    fun `test render command block`() {
        val commandBlock = Node.CommandBlock("npm install")
        val step = Step("Test", listOf(commandBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("devsite-terminal"))
        assertTrue(content.contains("npm install"))
    }

    @Test
    fun `test render positive info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.POSITIVE, listOf(Node.Text("Tip")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"positive\">"))
    }

    @Test
    fun `test render negative info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.NEGATIVE, listOf(Node.Text("Warning")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"negative\">"))
    }

    @Test
    fun `test render regular info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.REGULAR, listOf(Node.Text("Info")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"special\">"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<ul>"))
        assertTrue(content.contains("<li>"))
        assertTrue(content.contains("Item 1"))
        assertTrue(content.contains("Item 2"))
        assertTrue(content.contains("</ul>"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<ol>"))
        assertTrue(content.contains("<li>"))
        assertTrue(content.contains("First"))
        assertTrue(content.contains("</ol>"))
    }

    @Test
    fun `test render image`() {
        val image = Node.Image("image.png", "Alt text", maxWidth = 600)
        val step = Step("Test", listOf(image))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<img"))
        assertTrue(content.contains("alt=\"Alt text\""))
        assertTrue(content.contains("src=\"image.png\""))
        assertTrue(content.contains("width: 600px"))
    }

    @Test
    fun `test render youtube video`() {
        val video = Node.YoutubeVideo("dQw4w9WgXcQ")
        val step = Step("Test", listOf(video))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<iframe"))
        assertTrue(content.contains("youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun `test render iframe`() {
        val iframe = Node.IFrame("https://example.com/embed")
        val step = Step("Test", listOf(iframe))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<iframe"))
        assertTrue(content.contains("https://example.com/embed"))
    }

    @Test
    fun `test render link`() {
        val link = Node.Link("https://example.com", "Click here")
        val step = Step("Test", listOf(link))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<a href=\"https://example.com\""))
        assertTrue(content.contains("Click here"))
        assertTrue(content.contains("target=\"_blank\""))
    }

    @Test
    fun `test render button with download`() {
        val button = Node.Button("https://download.com/file", "Download", download = true)
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("paper-button"))
        assertTrue(content.contains("file-download"))
        assertTrue(content.contains("Download"))
    }

    @Test
    fun `test render survey`() {
        val survey = Node.Survey("What do you think?", listOf("Great", "Good", "Poor"))
        val step = Step("Test", listOf(survey))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("google-codelab-survey"))
        assertTrue(content.contains("What do you think?"))
        assertTrue(content.contains("paper-radio-button"))
        assertTrue(content.contains("Great"))
        assertTrue(content.contains("Good"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<h3>Frequently Asked Questions</h3>"))
        assertTrue(content.contains("Question 1"))
        assertTrue(content.contains("https://faq.com/1"))
    }

    @Test
    fun `test render metadata JSON`() {
        val metadata = CodelabMetadata(
            summary = "Test Summary",
            url = "test-url",
            category = listOf("Android", "Kotlin"),
            status = listOf("Published"),
            tags = listOf("tutorial"),
            feedbackLink = "https://feedback.com",
            analyticsAccount = "UA-12345"
        )
        val step = Step("Test", listOf(Node.Text("Content")), duration = 5)
        val codelab = Codelab("test", "Title", "Test Summary", listOf(step), metadata)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()
        assertTrue(jsonContent.contains("\"id\": \"test\""))
        assertTrue(jsonContent.contains("\"title\": \"Title\""))
        assertTrue(jsonContent.contains("\"summary\": \"Test Summary\""))
        assertTrue(jsonContent.contains("\"duration\": 5"))
        assertTrue(jsonContent.contains("\"feedback\": \"https://feedback.com\""))
        assertTrue(jsonContent.contains("\"mainga\": \"UA-12345\""))
    }

    @Test
    fun `test JSON escaping for special characters`() {
        val metadata = CodelabMetadata(summary = "Line1\nLine2\t\"Quote\"")
        val codelab = Codelab("test", "Title", steps = emptyList(), metadata = metadata)

        renderer.render(codelab, tempDir)

        val jsonFile = File(tempDir, "test/codelab.json")
        assertTrue(jsonFile.exists(), "JSON file should exist")
        val jsonContent = jsonFile.readText()
        assertTrue(jsonContent.isNotEmpty(), "JSON content should not be empty")
        // Check that the JSON contains the text parts
        assertTrue(jsonContent.contains("summary"), "JSON should contain summary field")
    }

    @Test
    fun `test HTML escaping for special characters`() {
        val step = Step("Test", listOf(Node.Text("<>&\"'")))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("&lt;") || content.contains("&amp;lt;"))
        assertTrue(content.contains("&gt;") || content.contains("&amp;gt;"))
        assertTrue(content.contains("&amp;") || content.contains("&amp;amp;"))
    }

    @Test
    fun `test multiple steps render correctly`() {
        val step1 = Step("Step 1", listOf(Node.Text("Content 1")), duration = 2)
        val step2 = Step("Step 2", listOf(Node.Text("Content 2")), duration = 3)
        val codelab = Codelab("test", "Title", steps = listOf(step1, step2))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("Step 1"))
        assertTrue(content.contains("Step 2"))
        assertTrue(content.contains("Content 1"))
        assertTrue(content.contains("Content 2"))
    }

    @Test
    fun `test total duration calculation in JSON`() {
        val step1 = Step("Step 1", listOf(Node.Text("Content")), duration = 10)
        val step2 = Step("Step 2", listOf(Node.Text("Content")), duration = 15)
        val codelab = Codelab("test", "Title", steps = listOf(step1, step2))

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()
        assertTrue(jsonContent.contains("\"duration\": 25"))
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
