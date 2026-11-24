package me.kartikarora.klaat.renderer

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyntaxHighlighterTest {

    private fun stripHighlightMarkup(html: String): String {
        val noSpans = html.replace(Regex("""</?span(?:\s+[^>]*)?>"""), "")
        return noSpans
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&gt;", ">")
            .replace("&lt;", "<")
            .replace("&amp;", "&")
    }

    @Test
    fun `test language normalization and display aliases`() {
        assertEquals("kotlin", SyntaxHighlighter.normalizeLanguage("Kotlin"))
        assertEquals("kotlin", SyntaxHighlighter.normalizeLanguage("kt"))
        assertEquals("kotlin", SyntaxHighlighter.normalizeLanguage("kts"))
        assertEquals("kotlin", SyntaxHighlighter.normalizeLanguage("gradle.kts"))
        assertEquals("kotlin", SyntaxHighlighter.displayLanguage("kt"))
        assertEquals("kotlin", SyntaxHighlighter.displayLanguage("kts"))

        assertEquals("python", SyntaxHighlighter.normalizeLanguage("py"))
        assertEquals("python", SyntaxHighlighter.displayLanguage("py"))

        assertEquals("bash", SyntaxHighlighter.normalizeLanguage("sh"))
        assertEquals("bash", SyntaxHighlighter.normalizeLanguage("zsh"))
        assertEquals("bash", SyntaxHighlighter.displayLanguage("zsh"))

        assertEquals("markdown", SyntaxHighlighter.normalizeLanguage("md"))
        assertEquals("markdown", SyntaxHighlighter.displayLanguage("md"))
        assertEquals("markdown", SyntaxHighlighter.normalizeLanguage("markdown:examples/demo.md"))
        assertEquals("markdown", SyntaxHighlighter.displayLanguage("markdown:examples/demo.md"))

        assertEquals("yaml", SyntaxHighlighter.normalizeLanguage("yml"))
        assertEquals("yaml", SyntaxHighlighter.displayLanguage("yml"))

        assertEquals("cpp", SyntaxHighlighter.normalizeLanguage("c++"))
        assertEquals("c++", SyntaxHighlighter.displayLanguage("c++"))
        assertEquals("csharp", SyntaxHighlighter.normalizeLanguage("c#"))
        assertEquals("c#", SyntaxHighlighter.displayLanguage("csharp"))

        assertEquals("", SyntaxHighlighter.normalizeLanguage("plaintext"))
        assertEquals("", SyntaxHighlighter.normalizeLanguage("unknownlang"))
        assertEquals("unknownlang", SyntaxHighlighter.displayLanguage("unknownlang"))
        assertEquals("", SyntaxHighlighter.displayLanguage(""))
    }

    @Test
    fun `test kotlin highlighting preserves comments with quotes and highlights constructs`() {
        val code = """
            @Composable
            fun Greeting(name: String) {
                // ❌ User hears "button", they don't know what it does
                val count = 42
                println("Hello ${'$'}name")
            }
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "kotlin")
        assertTrue(highlighted.contains("""<span class="nd">@Composable</span>"""))
        assertTrue(highlighted.contains("""<span class="k">fun</span> <span class="nf">Greeting</span>"""))
        assertTrue(highlighted.contains("""<span class="nc">String</span>"""))
        assertTrue(highlighted.contains("""<span class="c">// ❌ User hears &quot;button&quot;, they don&#39;t know what it does</span>"""))
        assertTrue(highlighted.contains("""<span class="k">val</span> <span class="nv">count</span>"""))
        assertTrue(highlighted.contains("""<span class="m">42</span>"""))
        assertTrue(highlighted.contains("""<span class="nb">println</span>"""))
        assertTrue(highlighted.contains("""<span class="s">&quot;Hello ${'$'}name&quot;</span>"""))
        assertEquals(code, stripHighlightMarkup(highlighted))
    }

    @Test
    fun `test java highlighting uses java keywords instead of kotlin keywords`() {
        val code = """
            public class Main {
                public static final void main(String[] args) {
                    int val = 10; // val is not a keyword in Java
                    funHelper();
                }
            }
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "java")
        assertTrue(highlighted.contains("""<span class="k">public</span>"""))
        assertTrue(highlighted.contains("""<span class="k">static</span>"""))
        assertTrue(highlighted.contains("""<span class="k">final</span>"""))
        assertTrue(highlighted.contains("""<span class="k">void</span>"""))
        assertTrue(highlighted.contains("""<span class="k">class</span> <span class="nc">Main</span>"""))
        assertFalse(highlighted.contains("""<span class="k">val</span>"""))
        assertFalse(highlighted.contains("""<span class="k">fun</span>"""))
        assertEquals(code, stripHighlightMarkup(highlighted))
    }

    @Test
    fun `test html and xml highlighting does not corrupt urls into comments`() {
        val code = """
            <!-- Brand CSS -->
            <link rel="stylesheet" href="https://distribute.kartikarora.me/css/kartikarora.2.0.0.css" defer>
            <p>Visit https://example.com &amp; enjoy</p>
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "html")
        assertTrue(highlighted.contains("""<span class="c">&lt;!-- Brand CSS --&gt;</span>"""))
        assertTrue(highlighted.contains("""<span class="nt">link</span>"""))
        assertTrue(highlighted.contains("""<span class="na">href</span>=<span class="s">&quot;https://distribute.kartikarora.me/css/kartikarora.2.0.0.css&quot;</span>"""))
        assertTrue(highlighted.contains("""<span class="na">defer</span>"""))
        assertFalse(highlighted.contains("""<span class="c">//"""))
        assertEquals(code, stripHighlightMarkup(highlighted))
    }

    @Test
    fun `test markdown highlighting preserves urls and highlights frontmatter and headings`() {
        val code = """
            ---
            id: my-codelab
            duration: 15
            ---
            # Main Title
            Duration: 0:02:00
            > Note with `inline` code
            Read [Docs](https://klaat.kartikarora.me/docs/) & **bold text**.
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "markdown:examples/demo.md")
        assertTrue(highlighted.contains("""<span class="cp">---</span>"""))
        assertTrue(highlighted.contains("""<span class="na">id</span>:"""))
        assertTrue(highlighted.contains("""<span class="k"># Main Title</span>"""))
        assertTrue(highlighted.contains("""<span class="na">Duration</span>: <span class="m">0:02:00</span>"""))
        assertTrue(highlighted.contains("""<span class="c">&gt; Note with `inline` code</span>"""))
        assertTrue(highlighted.contains("""<span class="nf">Docs</span>"""))
        assertTrue(highlighted.contains("""<span class="s">https://klaat.kartikarora.me/docs/</span>"""))
        assertTrue(highlighted.contains("""<span class="nv">**bold text**</span>"""))
        assertFalse(highlighted.contains("""<span class="c">//"""))
        assertEquals(code, stripHighlightMarkup(highlighted))
    }

    @Test
    fun `test json highlighting handles keys strings booleans numbers and urls`() {
        val code = """
            {
              "id": "demo-39",
              "url": "https://example.com",
              "steps": 12,
              "enabled": true,
              "meta": null
            }
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "JSON")
        assertTrue(highlighted.contains("""<span class="na">&quot;url&quot;</span>"""))
        assertTrue(highlighted.contains("""<span class="s">&quot;https://example.com&quot;</span>"""))
        assertTrue(highlighted.contains("""<span class="m">12</span>"""))
        assertTrue(highlighted.contains("""<span class="k">true</span>"""))
        assertTrue(highlighted.contains("""<span class="k">null</span>"""))
        assertFalse(highlighted.contains("""<span class="c">//"""))
        assertEquals(code, stripHighlightMarkup(highlighted))
    }

    @Test
    fun `test bash highlighting handles commands flags env vars and preserves paths`() {
        val code = """
            # Run export in /usr/local/bin
            export OUT_DIR="./output"
            klaat export --legacy -f offline "${'$'}OUT_DIR/guide.md"
        """.trimIndent()

        val highlighted = SyntaxHighlighter.highlight(code, "zsh")
        assertTrue(highlighted.contains("""<span class="c"># Run export in /usr/local/bin</span>"""))
        assertTrue(highlighted.contains("""<span class="k">export</span>"""))
        assertTrue(highlighted.contains("""<span class="na">--legacy</span>"""))
        assertTrue(highlighted.contains("""<span class="na">-f</span>"""))
        assertTrue(highlighted.contains("""<span class="nb">klaat</span>"""))
        assertEquals(code, stripHighlightMarkup(highlighted))

        val withVar = """echo ${'$'}HOME --verbose"""
        val varHighlighted = SyntaxHighlighter.highlight(withVar, "bash")
        assertTrue(varHighlighted.contains("""<span class="nb">echo</span>"""))
        assertTrue(varHighlighted.contains("""<span class="nv">${'$'}HOME</span>"""))
        assertTrue(varHighlighted.contains("""<span class="na">--verbose</span>"""))
        assertEquals(withVar, stripHighlightMarkup(varHighlighted))
    }

    @Test
    fun `test css yaml sql diff swift python typescript and toml highlighting`() {
        val samples = listOf(
            "css" to """
                @media (max-width: 900px) {
                  .btn {
                    color: #00f0ff;
                    --gap: 12px;
                  }
                }
            """.trimIndent(),
            "yml" to """
                name: ci
                enabled: true
                count: 5 # build count
            """.trimIndent(),
            "sql" to """
                -- Query codelabs
                SELECT id, title FROM codelabs WHERE duration >= 10 AND title = 'It''s great';
            """.trimIndent(),
            "diff" to """
                --- a/Main.kt
                +++ b/Main.kt
                @@ -1,2 +1,2 @@
                -val old = 1
                +val modern = 2
            """.trimIndent(),
            "swift" to """
                @MainActor
                struct ContentView {
                    let count: Int = 3
                    func render() { print("Hi") }
                }
            """.trimIndent(),
            "py" to """
                @cache
                def greet(n: int):
                    # print items
                    return list(range(n))
            """.trimIndent(),
            "ts" to """
                interface User { id: number; }
                export async function fetchUser(id: number): Promise<User> {
                    return { id };
                }
            """.trimIndent(),
            "toml" to """
                [versions]
                kotlin = "2.2.0"
                enabled = true
                port = 9090 # server
            """.trimIndent(),
            "go" to """
                package main
                func serve(port int) { fmt.Println(`listening`, port) }
            """.trimIndent(),
            "rust" to """
                #[derive(Debug)]
                struct Point<'a> { label: &'a str, x: i32 }
                fn main() { println!("ok"); }
            """.trimIndent(),
            "dart" to """
                @override
                Future<void> build(BuildContext context) async { print('ok'); }
            """.trimIndent(),
            "groovy" to """
                plugins { id 'org.jetbrains.kotlin.jvm' }
            """.trimIndent(),
            "c++" to """
                #include <iostream>
                int main() { std::cout << "hi" << std::endl; return 0; }
            """.trimIndent(),
            "c#" to """
                #region Demo
                public record Item(string Name) { public void Print() => Console.WriteLine(Name); }
            """.trimIndent()
        )

        samples.forEach { (lang, snippet) ->
            val highlighted = SyntaxHighlighter.highlight(snippet, lang)
            assertEquals(snippet, stripHighlightMarkup(highlighted), "Round-trip mismatch for $lang")
            assertTrue(highlighted.contains("""<span class=""""), "Expected spans for $lang")
        }
    }

    @Test
    fun `test plain or unknown language returns escaped text without comment corruption`() {
        val code = """See https://example.com & check '39' <tag> // plain note"""
        for (lang in listOf("", "text", "plaintext", "unknownlang")) {
            val highlighted = SyntaxHighlighter.highlight(code, lang)
            assertFalse(highlighted.contains("<span"), "Did not expect token spans for '$lang'")
            assertFalse(highlighted.contains("39</span>"), "Must not corrupt &#39; entity")
            assertEquals(code, stripHighlightMarkup(highlighted))
        }
    }
}
