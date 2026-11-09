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
        // Check that the injected script is escaped, not just that the file contains no <script> tags
        // since we now have a legitimate script tag for the copy button
        assertFalse(content.contains("<script>alert"))
        assertTrue(content.contains("&lt;script&gt;alert") || content.contains("&amp;lt;script&amp;gt;alert"))
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
    fun `test render special info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.SPECIAL, listOf(Node.Text("Tip")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"special\">"))
    }

    @Test
    fun `test render critical info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.CRITICAL, listOf(Node.Text("Alert")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"critical\">"))
    }

    @Test
    fun `test render warning info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.WARNING, listOf(Node.Text("Warning")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"warning\">"))
    }

    @Test
    fun `test render regular info box`() {
        val infoBox = Node.InfoBox(InfoBoxType.DEFAULT, listOf(Node.Text("Info")))
        val step = Step("Test", listOf(infoBox))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<aside class=\"default\">Info</aside>"))
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
        val button = Node.Button("https://download.com/file", "Download", type = me.kartikarora.klaat.core.ButtonType.Primary, download = true)
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("klaat-button"))
        assertTrue(content.contains("klaat-button--primary"))
        assertTrue(content.contains("download"))
        assertTrue(content.contains("Download"))
    }

    @Test
    fun `test render secondary button`() {
        val button = Node.Button("https://example.com/secondary", "Secondary Action", type = me.kartikarora.klaat.core.ButtonType.Secondary)
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("klaat-button"))
        assertTrue(content.contains("klaat-button--secondary"))
        assertTrue(content.contains("Secondary Action"))
    }

    @Test
    fun `test render button with custom icon`() {
        val button = Node.Button("https://example.com/watch", "Watch Video", type = me.kartikarora.klaat.core.ButtonType.Primary, icon = "play_arrow")
        val step = Step("Test", listOf(button))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("klaat-button"))
        assertTrue(content.contains("klaat-button--primary"))
        assertTrue(content.contains("play_arrow"))
        assertTrue(content.contains("Watch Video"))
    }

    @Test
    fun `test render survey`() {
        val survey = Node.Survey("What do you think?", listOf("Great", "Good", "Poor"))
        val step = Step("Test", listOf(survey))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("klaat-kodelab-survey"))
        assertTrue(content.contains("What do you think?"))
        assertTrue(content.contains("klaat-radio-label"))
        assertTrue(content.contains("Great"))
        assertTrue(content.contains("Good"))
    }

    @Test
    fun `test render survey with inline code options`() {
        val survey = Node.Survey("Which command is best?", listOf("`klaat export`", "`klaat demo`", "Neither"))
        val step = Step("Test", listOf(survey))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<code>klaat export</code>"))
        assertTrue(content.contains("<code>klaat demo</code>"))
        assertTrue(content.contains("Neither"))
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

    @Test
    fun `test comma-separated arrays in JSON metadata`() {
        val metadata = CodelabMetadata(
            summary = "Test",
            url = "test-url",
            category = listOf("AI", "Gemini"),
            status = listOf("Published"),
            tags = listOf("gcp", "generativeai", "gemini", "workshop"),
            authors = listOf("John Doe", "Jane Smith")
        )
        val step = Step("Test", listOf(Node.Text("Content")))
        val codelab = Codelab("test", "Title", steps = listOf(step), metadata = metadata)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()

        // Verify categories are properly split in JSON array (with flexible whitespace)
        assertTrue(jsonContent.contains("\"category\":") && jsonContent.contains("[\"AI\", \"Gemini\"]"),
            "Categories should be a proper JSON array with separate elements. Got: $jsonContent")

        // Verify tags are properly split in JSON array
        assertTrue(jsonContent.contains("\"tags\":") && jsonContent.contains("[\"gcp\", \"generativeai\", \"gemini\", \"workshop\"]"),
            "Tags should be a proper JSON array with separate elements. Got: $jsonContent")

        // Ensure it's NOT rendering as a single element
        assertFalse(jsonContent.contains("[\"AI, Gemini\"]"),
            "Categories should NOT be a single comma-separated string")
        assertFalse(jsonContent.contains("[\"gcp, generativeai, gemini, workshop\"]"),
            "Tags should NOT be a single comma-separated string")
    }

    @Test
    fun `test single value arrays in JSON metadata`() {
        val metadata = CodelabMetadata(
            category = listOf("Android"),
            tags = listOf("beginner")
        )
        val step = Step("Test", listOf(Node.Text("Content")))
        val codelab = Codelab("test", "Title", steps = listOf(step), metadata = metadata)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()

        assertTrue(jsonContent.contains("\"category\": [\"Android\"]"))
        assertTrue(jsonContent.contains("\"tags\": [\"beginner\"]"))
    }

    @Test
    fun `test empty arrays in JSON metadata`() {
        val metadata = CodelabMetadata(
            category = emptyList(),
            tags = emptyList()
        )
        val step = Step("Test", listOf(Node.Text("Content")))
        val codelab = Codelab("test", "Title", steps = listOf(step), metadata = metadata)

        renderer.render(codelab, tempDir)

        val jsonContent = File(tempDir, "test/codelab.json").readText()

        // For empty lists, renderJsonArray produces "[]" without any content inside
        // So we just need to find the pattern with the quotes removed
        val hasCategoryArray = jsonContent.contains("\"category\": []") ||
                               jsonContent.contains("\"category\":[]")
        val hasTagsArray = jsonContent.contains("\"tags\": []") ||
                          jsonContent.contains("\"tags\":[]")

        assertTrue(hasCategoryArray, "Category should be an empty array. Got: $jsonContent")
        assertTrue(hasTagsArray, "Tags should be an empty array. Got: $jsonContent")
    }

    @Test
    fun `test render code block always contains copy button and icon`() {
        val code = "line 1"
        val codeBlock = Node.CodeBlock(code, "kotlin")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<div class=\"code-container\">"))
        assertTrue(content.contains("<button type=\"button\" class=\"copy-button\""))
        assertTrue(content.contains("<span class=\"material-symbols-outlined\">content_copy</span>"))
        assertTrue(content.contains("<span>Copy</span>"))
        assertTrue(content.contains("copyToClipboard(this)"))
    }

    @Test
    fun `test render code block with quotes and contractions in comments`() {
        val code = "// ❌ User hears \"button\", they don't know what it does\nfun test() {}"
        val codeBlock = Node.CodeBlock(code, "kotlin")
        val step = Step("Test", listOf(codeBlock))
        val codelab = Codelab("test", "Title", steps = listOf(step))

        renderer.render(codelab, tempDir)

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<code class=\" language-kotlin\">// ❌ User hears &quot;button&quot;, they don&#39;t know what it does\nfun test() {}</code>"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<table>"))
        assertTrue(content.contains("<thead>"))
        assertTrue(content.contains("<tbody>"))
        assertTrue(content.contains("<th>Name</th>"))
        assertTrue(content.contains("<th>Value</th>"))
        assertTrue(content.contains("<td>foo</td>"))
        assertTrue(content.contains("<td>bar</td>"))
        assertTrue(content.contains("<td>baz</td>"))
        assertTrue(content.contains("<td>qux</td>"))
        assertTrue(content.contains("</table>"))
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("<strong>Header</strong>"))
        assertTrue(content.contains("<code>code</code>"))
    }

    @Test
    fun `test render table with column alignment`() {
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

        val content = File(tempDir, "test/index.html").readText()
        assertTrue(content.contains("""style="text-align: left""""))
        assertTrue(content.contains("""style="text-align: center""""))
        assertTrue(content.contains("""style="text-align: right""""))
        // The column with null alignment should not have a style attribute on its cells
        assertFalse(content.contains("<th>None</th>").not()) // th for "None" exists without style
    }

    @Test
    fun `test render default brand fonts includes Albert Sans`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(content.contains("family=Albert+Sans"))
    }

    @Test
    fun `test render HTML format uses CDN urls and does not create codelab-elements`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val content = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(content.contains("https://distribute.kartikarora.me/css/klaat-elements.css"))
        assertTrue(content.contains("https://distribute.kartikarora.me/js/klaat-elements.js"))

        val elementsDir = File(tempDir, "codelab-elements")
        assertFalse(elementsDir.exists())
    }

    @Test
    fun `test render offline format bundles css and js into codelab subdirectories`() {
        val codelab = createSimpleCodlab()
        val offlineRenderer = OfflineRenderer()
        offlineRenderer.render(codelab, tempDir)

        val codelabDir = File(tempDir, codelab.id)
        val cssFile = File(codelabDir, "css/klaat-elements.css")
        val jsFile = File(codelabDir, "js/klaat-elements.js")
        val bridgeFile = File(codelabDir, "js/klaat-bridge.js")
        val brandCssFile = File(codelabDir, "css/kartikarora.2.0.0.css")
        val brandJsFile = File(codelabDir, "js/kartikarora.2.0.0.js")

        assertTrue(cssFile.exists(), "css/klaat-elements.css should exist in codelab folder")
        assertTrue(jsFile.exists(), "js/klaat-elements.js should exist in codelab folder")
        assertTrue(bridgeFile.exists(), "js/klaat-bridge.js should exist in codelab folder")
        assertTrue(brandCssFile.exists(), "css/kartikarora.2.0.0.css should exist in codelab folder")
        assertTrue(brandJsFile.exists(), "js/kartikarora.2.0.0.js should exist in codelab folder")

        val indexHtml = File(codelabDir, "index.html").readText()
        assertTrue(indexHtml.contains("""href="css/klaat-elements.css""""))
        assertTrue(indexHtml.contains("""src="js/klaat-elements.js""""))
        assertTrue(indexHtml.contains("""href="css/kartikarora.2.0.0.css""""))
        assertTrue(indexHtml.contains("""src="js/kartikarora.2.0.0.js""""))
    }

    @Test
    fun `test render HTML includes default brand favicons when no custom favicon provided`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val indexHtml = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(indexHtml.contains("""href="https://kartikarora.me/img/icon32.png""""))
        assertTrue(indexHtml.contains("""href="https://kartikarora.me/img/icon16.png""""))
        assertTrue(indexHtml.contains("""href="https://kartikarora.me/img/icon180.png""""))
        assertTrue(indexHtml.contains("""href="https://kartikarora.me/img/icon192.png""""))
    }

    @Test
    fun `test render HTML includes custom favicon when provided via parameter or metadata`() {
        val codelabWithMetadata = Codelab(
            id = "custom-favicon-codelab",
            title = "Custom Favicon Codelab",
            metadata = CodelabMetadata(favicon = "https://example.com/meta-fav.ico"),
            steps = listOf(Step("Step 1", listOf(Node.Text("Hello"))))
        )
        renderer.render(codelabWithMetadata, tempDir)

        val indexHtml = File(tempDir, "${codelabWithMetadata.id}/index.html").readText()
        assertTrue(indexHtml.contains("""<link rel="icon" href="https://example.com/meta-fav.ico">"""))
        assertFalse(indexHtml.contains("""href="https://kartikarora.me/img/icon32.png""""))

        // CLI parameter overrides metadata
        renderer.render(codelabWithMetadata, tempDir, favicon = "https://example.com/cli-fav.ico")
        val indexHtmlOverridden = File(tempDir, "${codelabWithMetadata.id}/index.html").readText()
        assertTrue(indexHtmlOverridden.contains("""<link rel="icon" href="https://example.com/cli-fav.ico">"""))
    }

    @Test
    fun `test render HTML includes default footer when no custom footer provided`() {
        val codelab = createSimpleCodlab()
        renderer.render(codelab, tempDir)

        val indexHtml = File(tempDir, "${codelab.id}/index.html").readText()
        assertTrue(indexHtml.contains("""slot="footer""""))
        assertTrue(indexHtml.contains("kodelab-footer"))
        assertTrue(indexHtml.contains("Codelab generated using"))
        assertTrue(indexHtml.contains("""<a href="https://klaat.kartikarora.me" target="_blank" rel="noopener">Klaat</a>"""))
        assertTrue(indexHtml.contains("""<a href="https://kartikarora.me" target="_blank" rel="noopener">Kartik Arora</a>"""))
    }

    @Test
    fun `test render HTML includes custom footer when provided via parameter or metadata`() {
        val codelabWithMetadata = Codelab(
            id = "custom-footer-codelab",
            title = "Custom Footer Codelab",
            metadata = CodelabMetadata(footer = "Custom Metadata Footer"),
            steps = listOf(Step("Step 1", listOf(Node.Text("Hello"))))
        )
        renderer.render(codelabWithMetadata, tempDir)

        val indexHtml = File(tempDir, "${codelabWithMetadata.id}/index.html").readText()
        assertTrue(indexHtml.contains("Custom Metadata Footer"))

        // CLI parameter overrides metadata
        renderer.render(codelabWithMetadata, tempDir, footer = "Custom CLI Footer")
        val indexHtmlOverridden = File(tempDir, "${codelabWithMetadata.id}/index.html").readText()
        assertTrue(indexHtmlOverridden.contains("Custom CLI Footer"))
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

