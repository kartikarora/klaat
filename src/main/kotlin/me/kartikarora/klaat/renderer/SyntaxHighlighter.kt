package me.kartikarora.klaat.renderer

/**
 * Deterministic, build-time syntax highlighter that emits semantic CSS token spans
 * compatible with the `@kartikarora` `.highlight` stylesheet rules.
 *
 * Guarantees round-trip text fidelity: stripping all emitted `<span ...>`/`</span>` tags
 * and unescaping HTML entities always reproduces the exact input source code.
 */
object SyntaxHighlighter {

    private const val PH_START = '\uFDD0'
    private const val PH_END = '\uFDDB'
    private const val PH_DIGIT_BASE = 0xFDD1
    private val PH_REGEX = Regex("""\uFDD0([\uFDD1-\uFDDA]+)\uFDDB""")

    private val NON_PH_NUMBER_REGEX = Regex(
        """\b(0x[0-9a-fA-F_]+[uUlL]*|0b[01_]+[uUlL]*|0o[0-7_]+|\d[\d_]*\.\d[\d_]*(?:[eE][+-]?\d+)?[fFdDmM]?|\d[\d_]*[uUlLfFdDmM]?)\b"""
    )

    private val LANGUAGE_ALIASES = mapOf(
        "kotlin" to "kotlin", "kt" to "kotlin", "kts" to "kotlin", "gradle.kts" to "kotlin",
        "java" to "java",
        "js" to "javascript", "javascript" to "javascript", "jsx" to "javascript", "mjs" to "javascript", "cjs" to "javascript",
        "ts" to "typescript", "typescript" to "typescript", "tsx" to "typescript",
        "swift" to "swift",
        "py" to "python", "python" to "python", "python3" to "python",
        "sh" to "bash", "bash" to "bash", "shell" to "bash", "zsh" to "bash", "console" to "bash", "terminal" to "bash",
        "html" to "html", "htm" to "html", "xhtml" to "html", "ftl" to "html",
        "xml" to "xml", "svg" to "xml", "plist" to "xml",
        "css" to "css", "scss" to "css", "sass" to "css", "less" to "css",
        "json" to "json", "jsonc" to "json", "json5" to "json",
        "yaml" to "yaml", "yml" to "yaml",
        "md" to "markdown", "markdown" to "markdown",
        "sql" to "sql", "sqlite" to "sql", "postgres" to "sql", "postgresql" to "sql", "mysql" to "sql",
        "go" to "go", "golang" to "go",
        "rs" to "rust", "rust" to "rust",
        "dart" to "dart",
        "groovy" to "groovy", "gradle" to "groovy",
        "c" to "cpp", "h" to "cpp", "cpp" to "cpp", "c++" to "cpp", "cc" to "cpp", "cxx" to "cpp", "hpp" to "cpp",
        "cs" to "csharp", "csharp" to "csharp", "c#" to "csharp",
        "toml" to "toml", "ini" to "toml", "properties" to "toml",
        "diff" to "diff", "patch" to "diff"
    )

    private val DISPLAY_NAMES = mapOf(
        "kt" to "kotlin", "kts" to "kotlin", "gradle.kts" to "kotlin",
        "js" to "javascript", "mjs" to "javascript", "cjs" to "javascript",
        "ts" to "typescript",
        "py" to "python", "python3" to "python",
        "sh" to "bash", "zsh" to "bash", "shell" to "bash", "console" to "bash", "terminal" to "bash",
        "md" to "markdown",
        "yml" to "yaml",
        "rs" to "rust",
        "golang" to "go",
        "htm" to "html",
        "cpp" to "c++", "c++" to "c++",
        "cs" to "c#", "csharp" to "c#", "c#" to "c#"
    )

    private val PLAIN_ALIASES = setOf("", "text", "txt", "plain", "plaintext")

    private data class LanguageSpec(
        val commentPattern: String,
        val stringPattern: String,
        val keywords: Set<String>,
        val builtins: Set<String> = emptySet(),
        val supportsAtAnnotations: Boolean = false,
        val supportsHashPreprocessor: Boolean = false,
        val supportsRustAttrs: Boolean = false,
        val declPatterns: List<Pair<Regex, String>> = emptyList()
    )

    private val JS_KEYWORDS = setOf(
        "async", "await", "break", "case", "catch", "class", "const", "continue",
        "debugger", "default", "delete", "do", "else", "export", "extends", "false",
        "finally", "for", "from", "function", "if", "import", "in", "instanceof",
        "let", "new", "null", "of", "return", "static", "super", "switch", "this",
        "throw", "true", "try", "typeof", "undefined", "var", "void", "while",
        "with", "yield"
    )

    private val TS_KEYWORDS = JS_KEYWORDS + setOf(
        "abstract", "any", "as", "boolean", "declare", "enum", "implements",
        "interface", "is", "keyof", "namespace", "never", "number", "override",
        "private", "protected", "public", "readonly", "satisfies", "string",
        "type", "unknown"
    )

    private val LANGUAGE_SPECS: Map<String, LanguageSpec> = mapOf(
        "kotlin" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """"{3}[\s\S]*?"{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
                "companion", "const", "constructor", "continue", "crossinline", "data",
                "do", "else", "enum", "expect", "false", "final", "finally", "for", "fun",
                "if", "in", "infix", "inner", "interface", "internal", "is", "it",
                "lateinit", "noinline", "null", "object", "open", "operator", "out",
                "override", "package", "private", "protected", "public", "reified",
                "return", "sealed", "super", "suspend", "tailrec", "this", "throw",
                "true", "try", "typealias", "val", "value", "var", "when", "where", "while"
            ),
            builtins = setOf(
                "println", "print", "listOf", "mapOf", "setOf", "mutableListOf",
                "mutableMapOf", "mutableSetOf", "arrayOf", "sequenceOf", "require",
                "check", "error", "TODO", "buildString", "buildList", "buildMap",
                "repeat", "run", "apply", "also", "with", "let", "takeIf", "takeUnless"
            ),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(fun)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nf",
                Regex("""\b(class|object|interface|enum)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc",
                Regex("""\b(val|var)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nv"
            )
        ),
        "java" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """"{3}[\s\S]*?"{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "false", "final", "finally", "float", "for", "if",
                "implements", "import", "instanceof", "int", "interface", "long", "native",
                "new", "null", "package", "permits", "private", "protected", "public",
                "record", "return", "sealed", "short", "static", "strictfp", "super",
                "switch", "synchronized", "this", "throw", "throws", "transient", "true",
                "try", "var", "void", "volatile", "while", "yield"
            ),
            builtins = setOf("System", "Override", "SuppressWarnings"),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(class|interface|enum|record)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc"
            )
        ),
        "javascript" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """`[^`\\]*(?:\\.[^`\\]*)*`|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = JS_KEYWORDS,
            builtins = setOf("console", "Promise", "Array", "Object", "JSON", "Math", "window", "document"),
            declPatterns = listOf(
                Regex("""\b(function)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nf",
                Regex("""\b(class)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nc",
                Regex("""\b(const|let|var)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nv"
            )
        ),
        "typescript" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """`[^`\\]*(?:\\.[^`\\]*)*`|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = TS_KEYWORDS,
            builtins = setOf("console", "Promise", "Array", "Object", "Record", "Partial", "Pick", "Omit"),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(function)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nf",
                Regex("""\b(class|interface|type|enum)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nc",
                Regex("""\b(const|let|var)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)""") to "nv"
            )
        ),
        "swift" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """#+"[\s\S]*?"#+|"{3}[\s\S]*?"{3}|"[^"\\]*(?:\\.[^"\\]*)*"""",
            keywords = setOf(
                "actor", "any", "as", "associatedtype", "async", "await", "borrowing",
                "break", "case", "catch", "class", "consuming", "continue", "convenience",
                "default", "defer", "deinit", "do", "dynamic", "each", "else", "enum",
                "extension", "fallthrough", "false", "fileprivate", "final", "for", "func",
                "guard", "if", "import", "in", "indirect", "init", "inout", "internal",
                "is", "isolated", "lazy", "let", "macro", "mutating", "nil", "nonisolated",
                "nonmutating", "open", "operator", "optional", "override", "package",
                "private", "protocol", "public", "repeat", "required", "rethrows",
                "return", "self", "Self", "some", "static", "struct", "subscript", "super",
                "switch", "throw", "throws", "true", "try", "typealias", "unowned", "var",
                "weak", "where", "while"
            ),
            builtins = setOf("print", "fatalError", "precondition", "assert", "zip", "min", "max", "abs"),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(func)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nf",
                Regex("""\b(struct|class|enum|protocol|actor|extension|typealias)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc",
                Regex("""\b(let|var)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nv"
            )
        ),
        "python" to LanguageSpec(
            commentPattern = """#[^\n]*""",
            stringPattern = """[rRfFbBuU]{0,2}(?:"{3}[\s\S]*?"{3}|'{3}[\s\S]*?'{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*')""",
            keywords = setOf(
                "and", "as", "assert", "async", "await", "break", "class", "continue",
                "def", "del", "elif", "else", "except", "False", "finally", "for", "from",
                "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not",
                "or", "pass", "raise", "return", "True", "try", "while", "with", "yield"
            ),
            builtins = setOf(
                "print", "range", "len", "str", "int", "float", "bool", "list", "dict",
                "set", "tuple", "enumerate", "zip", "isinstance", "open", "super"
            ),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(def)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nf",
                Regex("""\b(class)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc"
            )
        ),
        "go" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """`[^`]*`|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "break", "case", "chan", "const", "continue", "default", "defer", "else",
                "fallthrough", "false", "for", "func", "go", "goto", "if", "import",
                "interface", "iota", "map", "nil", "package", "range", "return", "select",
                "struct", "switch", "true", "type", "var"
            ),
            builtins = setOf("fmt", "make", "len", "cap", "append", "copy", "delete", "new", "panic", "recover", "error"),
            declPatterns = listOf(
                Regex("""\b(func)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nf",
                Regex("""\b(type)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc",
                Regex("""\b(var|const)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nv"
            )
        ),
        "rust" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """r#*"[\s\S]*?"#*|"[^"\\]*(?:\\.[^"\\]*)*"|'(?:\\.|[^'\\])'""",
            keywords = setOf(
                "as", "async", "await", "break", "const", "continue", "crate", "dyn",
                "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
                "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
                "self", "Self", "static", "struct", "super", "trait", "true", "type",
                "unsafe", "use", "where", "while"
            ),
            builtins = setOf("Option", "Some", "None", "Result", "Ok", "Err", "Vec", "String", "Box", "println", "format"),
            supportsRustAttrs = true,
            declPatterns = listOf(
                Regex("""\b(fn)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nf",
                Regex("""\b(struct|enum|trait|type)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc"
            )
        ),
        "dart" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """[rR]?(?:"{3}[\s\S]*?"{3}|'{3}[\s\S]*?'{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*')""",
            keywords = setOf(
                "abstract", "as", "assert", "async", "await", "base", "break", "case",
                "catch", "class", "const", "continue", "covariant", "default", "deferred",
                "do", "dynamic", "else", "enum", "export", "extends", "extension",
                "external", "factory", "false", "final", "finally", "for", "get", "hide",
                "if", "implements", "import", "in", "interface", "is", "late", "library",
                "mixin", "new", "null", "on", "operator", "part", "required", "rethrow",
                "return", "sealed", "set", "show", "static", "super", "switch", "sync",
                "this", "throw", "true", "try", "typedef", "var", "void", "when", "while",
                "with", "yield"
            ),
            builtins = setOf("print", "Future", "Stream", "List", "Map", "Set", "Widget", "BuildContext"),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(class|enum|mixin|extension|typedef)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc",
                Regex("""\b(var|final|const)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nv"
            )
        ),
        "groovy" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """"{3}[\s\S]*?"{3}|'{3}[\s\S]*?'{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "as", "assert", "break", "case", "catch", "class", "const", "continue",
                "def", "default", "do", "else", "enum", "extends", "false", "final",
                "finally", "for", "if", "implements", "import", "in", "instanceof",
                "interface", "new", "null", "package", "private", "protected", "public",
                "return", "static", "super", "switch", "this", "throw", "throws",
                "trait", "true", "try", "void", "while"
            ),
            builtins = setOf("plugins", "dependencies", "repositories", "android", "tasks", "implementation", "testImplementation", "println"),
            supportsAtAnnotations = true,
            declPatterns = listOf(
                Regex("""\b(class|interface|enum|trait)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc",
                Regex("""\b(def)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nv"
            )
        ),
        "cpp" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "alignas", "alignof", "auto", "bool", "break", "case", "catch", "char",
                "class", "const", "constexpr", "consteval", "continue", "default",
                "delete", "do", "double", "else", "enum", "explicit", "export", "extern",
                "false", "float", "for", "friend", "goto", "if", "inline", "int", "long",
                "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
                "override", "private", "protected", "public", "return", "short", "signed",
                "sizeof", "static", "struct", "switch", "template", "this", "throw",
                "true", "try", "typedef", "typename", "union", "unsigned", "using",
                "virtual", "void", "volatile", "while"
            ),
            builtins = setOf("std", "cout", "cin", "endl", "vector", "string", "printf", "malloc", "free"),
            supportsHashPreprocessor = true,
            declPatterns = listOf(
                Regex("""\b(class|struct|enum|namespace)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc"
            )
        ),
        "csharp" to LanguageSpec(
            commentPattern = """//[^\n]*|/\*[\s\S]*?\*/""",
            stringPattern = """@"[^"]*(?:""[^"]*)*"|"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*'""",
            keywords = setOf(
                "abstract", "as", "async", "await", "base", "bool", "break", "byte",
                "case", "catch", "char", "checked", "class", "const", "continue",
                "decimal", "default", "delegate", "do", "double", "else", "enum", "event",
                "explicit", "extern", "false", "finally", "fixed", "float", "for",
                "foreach", "goto", "if", "implicit", "in", "int", "interface", "internal",
                "is", "lock", "long", "namespace", "new", "null", "object", "operator",
                "out", "override", "params", "private", "protected", "public", "readonly",
                "record", "ref", "return", "sbyte", "sealed", "short", "sizeof", "static",
                "string", "struct", "switch", "this", "throw", "true", "try", "typeof",
                "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "var",
                "virtual", "void", "volatile", "while"
            ),
            builtins = setOf("Console", "Task", "List", "Dictionary", "IEnumerable"),
            supportsHashPreprocessor = true,
            declPatterns = listOf(
                Regex("""\b(class|interface|struct|enum|record|namespace)\s+([a-zA-Z_][a-zA-Z0-9_]*)""") to "nc"
            )
        )
    )

    private class PlaceholderTable {
        private val entries = mutableListOf<String>()

        fun put(cssClass: String, rawText: String): String =
            putRaw("""<span class="$cssClass">${escapeHtml(rawText)}</span>""")

        fun putRaw(html: String): String {
            val index = entries.size
            entries.add(html)
            val encoded = index.toString().map { (PH_DIGIT_BASE + (it - '0')).toChar() }.joinToString("")
            return "$PH_START$encoded$PH_END"
        }

        fun resolve(text: String): String {
            var result = text
            while (PH_REGEX.containsMatchIn(result)) {
                result = PH_REGEX.replace(result) { match ->
                    val idxStr = match.groupValues[1].map { ('0'.code + (it.code - PH_DIGIT_BASE)).toChar() }.joinToString("")
                    entries[idxStr.toInt()]
                }
            }
            return result
        }
    }

    /** Escapes HTML special characters for safe embedding inside `<code>` elements. */
    fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    /** Normalizes raw code fence info strings to a canonical highlighter language key, or `""` if plain. */
    fun normalizeLanguage(raw: String): String {
        val cleaned = extractPrimaryToken(raw)
        if (cleaned in PLAIN_ALIASES) return ""
        return LANGUAGE_ALIASES[cleaned] ?: ""
    }

    /** Formats a clean language label suitable for the top-left badge on modern code blocks. */
    fun displayLanguage(raw: String): String {
        val cleaned = extractPrimaryToken(raw)
        if (cleaned.isEmpty()) return ""
        return DISPLAY_NAMES[cleaned] ?: cleaned
    }

    /** Highlights [code] according to [language] using build-time semantic token spans. */
    fun highlight(code: String, language: String): String {
        if (code.isEmpty()) return ""
        val lang = normalizeLanguage(language)
        if (lang.isEmpty()) return escapeHtml(code)

        return when (lang) {
            "html", "xml" -> highlightMarkup(code)
            "markdown" -> highlightMarkdown(code)
            "json" -> highlightJson(code)
            "yaml" -> highlightYaml(code)
            "bash" -> highlightBash(code)
            "css" -> highlightCss(code)
            "diff" -> highlightDiff(code)
            "toml" -> highlightToml(code)
            "sql" -> highlightSql(code)
            else -> LANGUAGE_SPECS[lang]?.let { highlightCLike(code, it) } ?: escapeHtml(code)
        }
    }

    private fun extractPrimaryToken(raw: String): String =
        raw.trim().substringBefore(":").substringBefore(" ").trim().lowercase()

    private fun finalizeWithPlaceholders(working: String, table: PlaceholderTable): String {
        return table.resolve(escapeHtml(working))
    }

    private fun replaceIfClean(
        working: String,
        regex: Regex,
        transform: (MatchResult) -> String
    ): String = regex.replace(working) { match ->
        if (match.value.any { it in PH_START..PH_END }) match.value else transform(match)
    }

    private fun highlightCLike(code: String, spec: LanguageSpec): String {
        val table = PlaceholderTable()
        val combinedRegex = Regex("""(${spec.commentPattern})|(${spec.stringPattern})""")
        var working = combinedRegex.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }

        if (spec.supportsHashPreprocessor) {
            working = replaceIfClean(working, Regex("""^\s*#[^\n]*""", RegexOption.MULTILINE)) {
                table.put("cp", it.value)
            }
        }
        if (spec.supportsRustAttrs) {
            working = replaceIfClean(working, Regex("""#!\[[^\]\n]+\]|#\[[^\]\n]+\]""")) {
                table.put("nd", it.value)
            }
            working = replaceIfClean(working, Regex("""\b[a-zA-Z_][a-zA-Z0-9_]*!""")) {
                table.put("nf", it.value)
            }
        }
        if (spec.supportsAtAnnotations) {
            working = replaceIfClean(working, Regex("""@[a-zA-Z_][a-zA-Z0-9_.]*(?::[a-zA-Z_][a-zA-Z0-9_]*)?""")) {
                table.put("nd", it.value)
            }
        }

        for ((declRegex, targetClass) in spec.declPatterns) {
            working = replaceIfClean(working, declRegex) { match ->
                val kw = match.groupValues[1]
                val name = match.groupValues[2]
                val full = match.value
                val firstRange = match.groups[1]!!.range
                val secondRange = match.groups[2]!!.range
                val startOffset = match.range.first
                val between = full.substring(firstRange.last - startOffset + 1, secondRange.first - startOffset)
                "${table.put("k", kw)}$between${table.put(targetClass, name)}"
            }
        }

        if (spec.builtins.isNotEmpty()) {
            val builtinRegex = Regex("""\b(?:${spec.builtins.joinToString("|") { Regex.escape(it) }})\b""")
            working = replaceIfClean(working, builtinRegex) { table.put("nb", it.value) }
        }

        if (spec.keywords.isNotEmpty()) {
            val kwRegex = Regex("""\b(?:${spec.keywords.joinToString("|") { Regex.escape(it) }})\b""")
            working = replaceIfClean(working, kwRegex) { table.put("k", it.value) }
        }

        working = replaceIfClean(working, Regex("""\b([a-zA-Z_$][a-zA-Z0-9_$]*)(?=\s*\()""")) {
            table.put("nf", it.groupValues[1])
        }

        working = replaceIfClean(working, Regex("""\b[A-Z][a-zA-Z0-9_]*\b""")) {
            table.put("nc", it.value)
        }

        working = replaceIfClean(working, NON_PH_NUMBER_REGEX) {
            table.put("m", it.value)
        }

        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightMarkup(code: String): String {
        val table = PlaceholderTable()
        val commentAndPi = Regex("""(<!--[\s\S]*?-->)|(<!DOCTYPE\b[^>]*>|<\?[\s\S]*?\?>|<!\[CDATA\[[\s\S]*?\]\]>)""", RegexOption.IGNORE_CASE)
        var working = commentAndPi.replace(code) { match ->
            when {
                match.groups[1] != null -> table.put("c", match.value)
                match.value.startsWith("<![CDATA[") -> table.put("s", match.value)
                else -> table.put("cp", match.value)
            }
        }

        val tagRegex = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9:_-]*)((?:[^<>"']|"[^"]*"|'[^']*')*?)(/?)>""")
        val attrRegex = Regex("""([a-zA-Z_:][a-zA-Z0-9_.:-]*)(\s*=\s*)("[^"]*"|'[^']*'|[^\s"'=<>`]+)|([a-zA-Z_:][a-zA-Z0-9_.:-]*)""")
        working = replaceIfClean(working, tagRegex) { match ->
            val slash = match.groupValues[1]
            val tagName = match.groupValues[2]
            val attrsRaw = match.groupValues[3]
            val selfClose = match.groupValues[4]
            val renderedAttrs = attrRegex.replace(attrsRaw) { attrMatch ->
                if (attrMatch.groups[1] != null) {
                    val name = attrMatch.groupValues[1]
                    val eq = attrMatch.groupValues[2]
                    val value = attrMatch.groupValues[3]
                    """<span class="na">${escapeHtml(name)}</span>${escapeHtml(eq)}<span class="s">${escapeHtml(value)}</span>"""
                } else {
                    """<span class="na">${escapeHtml(attrMatch.groupValues[4])}</span>"""
                }
            }
            table.putRaw("""${escapeHtml("<$slash")}<span class="nt">${escapeHtml(tagName)}</span>$renderedAttrs${escapeHtml("$selfClose>")}""")
        }

        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightMarkdown(code: String): String {
        val lines = code.split('\n')
        var frontmatterPossible = true
        var inFrontmatter = false
        var inNestedFence = false

        return lines.mapIndexed { idx, rawLine ->
            val hasCr = rawLine.endsWith("\r")
            val line = if (hasCr) rawLine.dropLast(1) else rawLine
            val trimmed = line.trim()

            val rendered = when {
                idx == 0 && frontmatterPossible && trimmed == "---" -> {
                    inFrontmatter = true
                    frontmatterPossible = false
                    """<span class="cp">${escapeHtml(line)}</span>"""
                }
                inFrontmatter -> {
                    if (trimmed == "---") {
                        inFrontmatter = false
                        """<span class="cp">${escapeHtml(line)}</span>"""
                    } else {
                        val kvMatch = Regex("""^(\s*)([a-zA-Z0-9_.-]+)(\s*:)(.*)$""").matchEntire(line)
                        if (kvMatch != null) {
                            val indent = kvMatch.groupValues[1]
                            val key = kvMatch.groupValues[2]
                            val colon = kvMatch.groupValues[3]
                            val rest = kvMatch.groupValues[4]
                            val restHtml = if (rest.isBlank()) escapeHtml(rest) else {
                                val leadSpace = rest.takeWhile { it.isWhitespace() }
                                val valTrimmed = rest.substring(leadSpace.length)
                                val cls = when {
                                    Regex("""^(?:true|false|yes|no|null)$""", RegexOption.IGNORE_CASE).matches(valTrimmed) -> "k"
                                    Regex("""^\d+(?:\.\d+)?$""").matches(valTrimmed) -> "m"
                                    else -> "s"
                                }
                                """${escapeHtml(leadSpace)}<span class="$cls">${escapeHtml(valTrimmed)}</span>"""
                            }
                            """${escapeHtml(indent)}<span class="na">${escapeHtml(key)}</span>${escapeHtml(colon)}$restHtml"""
                        } else {
                            escapeHtml(line)
                        }
                    }
                }
                Regex("""^\s*\\?`{3,}.*$""").matches(line) -> {
                    frontmatterPossible = false
                    if (!trimmed.startsWith("\\")) {
                        inNestedFence = !inNestedFence
                    }
                    """<span class="cp">${escapeHtml(line)}</span>"""
                }
                inNestedFence -> escapeHtml(line)
                else -> {
                    if (trimmed.isNotEmpty()) frontmatterPossible = false
                    when {
                        Regex("""^\s*#{1,6}(?:\s+.*|\s*)$""").matches(line) ->
                            """<span class="k">${escapeHtml(line)}</span>"""
                        Regex("""^\s*>.*$""").matches(line) ->
                            """<span class="c">${escapeHtml(line)}</span>"""
                        else -> {
                            val durationMatch = Regex("""^(\s*Duration)(\s*:\s*)(\d+(?::\d+)*)(.*)$""").matchEntire(line)
                            if (durationMatch != null) {
                                """<span class="na">${escapeHtml(durationMatch.groupValues[1])}</span>${escapeHtml(durationMatch.groupValues[2])}<span class="m">${escapeHtml(durationMatch.groupValues[3])}</span>${highlightMarkdownInline(durationMatch.groupValues[4])}"""
                            } else {
                                highlightMarkdownInline(line)
                            }
                        }
                    }
                }
            }

            if (hasCr) "$rendered\r" else rendered
        }.joinToString("\n")
    }

    private fun highlightMarkdownInline(line: String): String {
        val table = PlaceholderTable()
        var working = Regex("""`[^`\n]+`""").replace(line) { table.put("s", it.value) }
        working = replaceIfClean(working, Regex("""\[([^\]\n]+)\]\(([^)\n]+)\)""")) { match ->
            val label = match.groupValues[1]
            val url = match.groupValues[2]
            table.putRaw("""[<span class="nf">${escapeHtml(label)}</span>](<span class="s">${escapeHtml(url)}</span>)""")
        }
        working = replaceIfClean(working, Regex("""\*\*[^*\n]+\*\*""")) {
            table.put("nv", it.value)
        }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightJson(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""(//[^\n]*|/\*[\s\S]*?\*/)|("[^"\\]*(?:\\.[^"\\]*)*")(?=\s*:)|("[^"\\]*(?:\\.[^"\\]*)*")""")
        var working = combined.replace(code) { match ->
            when {
                match.groups[1] != null -> table.put("c", match.value)
                match.groups[2] != null -> table.put("na", match.value)
                else -> table.put("s", match.value)
            }
        }
        working = replaceIfClean(working, Regex("""\b(?:true|false|null)\b""")) { table.put("k", it.value) }
        working = replaceIfClean(working, Regex("""(?<![\w.])-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?\b""")) {
            table.put("m", it.value)
        }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightYaml(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""((?:^|(?<=\s))#[^\n]*)|("[^"\\]*(?:\\.[^"\\]*)*"|'[^']*')""")
        var working = combined.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }
        working = replaceIfClean(working, Regex("""(?<=^|\s)(?:![a-zA-Z0-9_.-]+|&[a-zA-Z0-9_.-]+|\*[a-zA-Z0-9_.-]+)\b""")) {
            table.put("cp", it.value)
        }
        working = replaceIfClean(working, Regex("""^(\s*(?:-\s+)?)([a-zA-Z0-9_.-]+)(?=\s*:)""", RegexOption.MULTILINE)) {
            "${it.groupValues[1]}${table.put("na", it.groupValues[2])}"
        }
        working = replaceIfClean(working, Regex("""(?<=:\s|-\s)\b(?:true|false|null|yes|no|on|off|TRUE|FALSE|NULL|True|False|None)\b""")) {
            table.put("k", it.value)
        }
        working = replaceIfClean(working, Regex("""(?<=:\s|-\s)\b(?:0x[0-9a-fA-F_]+|0o[0-7_]+|\d[\d_]*\.\d[\d_]*|\d[\d_]*)\b""")) {
            table.put("m", it.value)
        }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightBash(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""((?:^|(?<=[\s;|&(]))#[^\n]*)|("[^"\\]*(?:\\.[^"\\]*)*"|'[^']*')""")
        var working = combined.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }
        working = replaceIfClean(working, Regex("""\$\{[a-zA-Z_][a-zA-Z0-9_:-]*\}|\$[a-zA-Z_][a-zA-Z0-9_]*|\$[0-9@*?#!-]""")) {
            table.put("nv", it.value)
        }
        working = replaceIfClean(working, Regex("""(?<=^|\s)(--?[a-zA-Z0-9][a-zA-Z0-9_-]*)(?==|\s|$)""", RegexOption.MULTILINE)) {
            table.put("na", it.groupValues[1])
        }
        val bashKeywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "in", "do", "done", "while",
            "until", "case", "esac", "function", "export", "local", "readonly",
            "return", "exit", "source", "true", "false"
        )
        val bashBuiltins = setOf(
            "echo", "printf", "cd", "pwd", "ls", "mkdir", "rmdir", "rm", "cp", "mv",
            "cat", "head", "tail", "grep", "egrep", "sed", "awk", "find", "chmod",
            "chown", "curl", "wget", "git", "klaat", "klaat-snapshot", "npm", "npx",
            "node", "brew", "docker", "kubectl", "java", "javac", "gradle", "gradlew",
            "sdk", "sudo"
        )
        working = replaceIfClean(working, Regex("""(?<![.\-/\w])(?:${bashKeywords.joinToString("|")})(?![.\-/\w])""")) {
            table.put("k", it.value)
        }
        working = replaceIfClean(working, Regex("""(?<![.\-/\w])(?:${bashBuiltins.joinToString("|")})(?![.\-/\w])""")) {
            table.put("nb", it.value)
        }
        working = replaceIfClean(working, Regex("""(?<=\s|=)\d+\b""")) {
            table.put("m", it.value)
        }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightCss(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""(/\*[\s\S]*?\*/)|("[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\]*)*')""")
        var working = combined.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }
        working = replaceIfClean(working, Regex("""@[a-zA-Z_-]+""")) { table.put("k", it.value) }
        working = replaceIfClean(working, Regex("""--[a-zA-Z0-9_-]+""")) { table.put("nv", it.value) }
        working = replaceIfClean(working, Regex("""(?<=[{;\s]|^)([a-z-]+)(?=\s*:)""")) {
            table.put("na", it.groupValues[1])
        }
        working = replaceIfClean(working, Regex("""^(\s*)([.#]?[a-zA-Z0-9_:-]+)(\s*\{)""", RegexOption.MULTILINE)) {
            "${it.groupValues[1]}${table.put("nc", it.groupValues[2])}${it.groupValues[3]}"
        }
        working = replaceIfClean(working, Regex("""#[0-9a-fA-F]{3,8}\b""")) { table.put("m", it.value) }
        working = replaceIfClean(working, Regex("""\b\d+(?:\.\d+)?(?:px|rem|em|vh|vw|%|ms|s|deg|fr)?\b""")) {
            table.put("m", it.value)
        }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightDiff(code: String): String {
        return code.split('\n').joinToString("\n") { rawLine ->
            val hasCr = rawLine.endsWith("\r")
            val line = if (hasCr) rawLine.dropLast(1) else rawLine
            val rendered = when {
                line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@") ||
                    line.startsWith("diff --git") || line.startsWith("index ") ->
                    """<span class="cp">${escapeHtml(line)}</span>"""
                line.startsWith("+") -> """<span class="gi">${escapeHtml(line)}</span>"""
                line.startsWith("-") -> """<span class="gd">${escapeHtml(line)}</span>"""
                else -> escapeHtml(line)
            }
            if (hasCr) "$rendered\r" else rendered
        }
    }

    private fun highlightToml(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""((?:^|(?<=\s))#[^\n]*)|("{3}[\s\S]*?"{3}|'{3}[\s\S]*?'{3}|"[^"\\]*(?:\\.[^"\\]*)*"|'[^']*')""")
        var working = combined.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }
        working = replaceIfClean(working, Regex("""^(\s*)(\[[^\]\n]+\])""", RegexOption.MULTILINE)) {
            "${it.groupValues[1]}${table.put("nc", it.groupValues[2])}"
        }
        working = replaceIfClean(working, Regex("""^(\s*)([a-zA-Z0-9_.-]+)(?=\s*=)""", RegexOption.MULTILINE)) {
            "${it.groupValues[1]}${table.put("na", it.groupValues[2])}"
        }
        working = replaceIfClean(working, Regex("""\b(?:true|false)\b""")) { table.put("k", it.value) }
        working = replaceIfClean(working, NON_PH_NUMBER_REGEX) { table.put("m", it.value) }
        return finalizeWithPlaceholders(working, table)
    }

    private fun highlightSql(code: String): String {
        val table = PlaceholderTable()
        val combined = Regex("""(--[^\n]*|/\*[\s\S]*?\*/)|('[^'\\]*(?:(?:\\.|'')[^'\\]*)*'|`[^`]*`)""")
        var working = combined.replace(code) { match ->
            if (match.groups[1] != null) table.put("c", match.value) else table.put("s", match.value)
        }
        val sqlKeywords = listOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "JOIN",
            "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "ON", "AS", "AND", "OR", "NOT",
            "IN", "IS", "NULL", "TRUE", "FALSE", "ORDER", "BY", "GROUP", "HAVING",
            "LIMIT", "OFFSET", "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX",
            "UNION", "ALL", "CASE", "WHEN", "THEN", "ELSE", "END", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "DEFAULT", "UNIQUE", "CHECK", "CONSTRAINT",
            "ASC", "DESC", "LIKE", "BETWEEN", "EXISTS"
        )
        working = replaceIfClean(working, Regex("""\b(?:${sqlKeywords.joinToString("|")})\b""", RegexOption.IGNORE_CASE)) {
            table.put("k", it.value)
        }
        working = replaceIfClean(working, NON_PH_NUMBER_REGEX) { table.put("m", it.value) }
        return finalizeWithPlaceholders(working, table)
    }
}
