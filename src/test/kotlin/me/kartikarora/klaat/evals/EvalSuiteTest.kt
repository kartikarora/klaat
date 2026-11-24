package me.kartikarora.klaat.evals

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvalSuiteTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `extractMarkdown preserves inner fenced code blocks`() {
        val raw = """
            ```markdown
            ---
            id: nested-code-demo
            summary: Demo with inner code blocks
            categories: kotlin
            ---

            # Nested Code Demo

            ## Step 1
            Duration: 2:00

            ```kotlin
            fun main() {
                println("hello")
            }
            ```

            Positive
            : Done!
            ```
        """.trimIndent()

        val extracted = DeterministicGrader.extractMarkdown(raw)
        assertTrue(extracted.contains("Positive"), "Should not truncate at inner kotlin fence")
        val graded = DeterministicGrader.grade(raw, expectedSkill = "klaat-markdown-author")
        assertTrue(graded.passed, "Expected codelab with inner code blocks to compile: ${graded.error}")
        assertTrue(graded.hasValidDurations)
        assertTrue(graded.hasCallouts)
    }

    @Test
    fun `grade supports klaat-cli and rejects hallucinated flags`() {
        val valid = DeterministicGrader.grade(
            rawResponse = "Run `klaat export --legacy -o out guide.md` after `brew install kartikarora/tap/klaat`.",
            expectedSkill = "klaat-cli",
            expectedPatterns = listOf("klaat export", "--legacy", "-o")
        )
        assertTrue(valid.passed, "Valid CLI response should pass: ${valid.error}")

        val hallucinated = DeterministicGrader.grade(
            rawResponse = "Run `klaat export --watch -o out guide.md`.",
            expectedSkill = "klaat-cli",
            expectedPatterns = listOf("klaat export", "-o")
        )
        assertFalse(hallucinated.passed, "Hallucinated --watch flag should fail")
    }

    @Test
    fun `grade supports klaat-gdocs-author`() {
        val valid = DeterministicGrader.grade(
            rawResponse = """
                Add a metadata table with Summary, ID, Categories, and Author.
                Use Heading 1 for each step and set Duration: 5:00 under the heading.
            """.trimIndent(),
            expectedSkill = "klaat-gdocs-author",
            expectedPatterns = listOf("Summary", "Duration:")
        )
        assertTrue(valid.passed, "Valid GDocs guide should pass: ${valid.error}")
    }

    @Test
    fun `EvalMetrics computes precision recall F1 and per-skill stats`() {
        val passCheck = GradedChecklist(
            passed = true, compiles = true, hasValidMetadata = true,
            hasValidDurations = true, hasCallouts = true, stepCount = 2
        )
        val failCheck = GradedChecklist(
            passed = false, compiles = false, hasValidMetadata = false,
            hasValidDurations = false, hasCallouts = false, stepCount = 0
        )

        val results = listOf(
            PromptEvalResult(
                id = "p1", category = "cli", isPositive = true, expectedSkill = "klaat-cli",
                prompt = "p1",
                baseline = ConditionResult(false, null, failCheck, "", latencyMs = 100),
                withAgent = ConditionResult(true, "klaat-cli", passCheck, "", latencyMs = 200, tokenUsage = TokenUsage(10, 20, 30)),
                triggerSuccess = true
            ),
            PromptEvalResult(
                id = "p2", category = "gdocs", isPositive = true, expectedSkill = "klaat-gdocs-author",
                prompt = "p2",
                baseline = ConditionResult(false, null, failCheck, "", latencyMs = 100),
                withAgent = ConditionResult(true, "klaat-cli", failCheck, "", latencyMs = 200),
                triggerSuccess = false // wrong skill invoked
            ),
            PromptEvalResult(
                id = "d1", category = "distractor", isPositive = false, expectedSkill = null,
                prompt = "d1",
                baseline = ConditionResult(false, null, failCheck, ""),
                withAgent = ConditionResult(false, null, failCheck, ""),
                triggerSuccess = true
            )
        )

        val metrics = EvalMetrics.compute(results)
        assertEquals(50.0, metrics.triggerRecallPct)
        assertEquals(50.0, metrics.triggerPrecisionPct)
        assertEquals(0.0, metrics.falsePositiveRatePct)
        assertEquals(2, metrics.perSkill.size)
        assertEquals("klaat-cli", metrics.perSkill[0].skillName)
        assertEquals(100.0, metrics.perSkill[0].triggerRecallPct)
    }

    @Test
    fun `dataset and docs eval-report deserialize and cover all 3 bundled skills`() {
        val dataset = json.decodeFromString<List<PromptCase>>(File("evals/dataset.json").readText())
        val skillsCovered = dataset.mapNotNull { it.expectedSkill }.toSet()
        assertEquals(setOf("klaat-markdown-author", "klaat-cli", "klaat-gdocs-author"), skillsCovered)
        assertTrue(dataset.any { !it.isPositive }, "Dataset should include distractor cases")

        val report = json.decodeFromString<EvalReport>(File("docs/docs/eval-report.json").readText())
        assertTrue(report.results.isNotEmpty())
    }

    @Test
    fun `DocUpdater writes JSON report with capped history and markdown summary`() {
        val tempDir = createTempDirectory("klaat-eval-doc-test").toFile()
        try {
            val jsonFile = File(tempDir, "eval-report.json")
            val mdFile = File(tempDir, "eval-summary.md")

            val dummyMetrics = EvalMetrics.compute(emptyList())
            val r1 = EvalReport("2026-11-24T00:00:00Z", "gemini-2.5-flash", "Live", metrics = dummyMetrics, results = emptyList())
            val r2 = EvalReport("2026-11-24T01:00:00Z", "gemini-2.5-flash", "Live", metrics = dummyMetrics, results = emptyList())

            DocUpdater.writeJsonReport(r1, jsonFile, maxHistory = 2)
            DocUpdater.writeJsonReport(r2, jsonFile, maxHistory = 2)
            DocUpdater.writeMarkdownSummary(r2, mdFile)

            val saved = json.decodeFromString<EvalReport>(jsonFile.readText())
            assertEquals(2, saved.history.size)
            assertEquals("2026-11-24T01:00:00Z", saved.history.first().timestamp)
            assertTrue(mdFile.readText().contains("## Overall Scorecard"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `LlmJudge builds gemini-4-hydrogen low thinking payload and parses response`() {
        val judge = LlmJudge(apiKey = "test-key")
        val promptCase = PromptCase(
            id = "cli-test",
            category = "cli-usage",
            prompt = "How do I run klaat serve?",
            expectedSkill = "klaat-cli",
            isPositive = true
        )
        val checklist = GradedChecklist(
            passed = true, compiles = true, hasValidMetadata = true,
            hasValidDurations = true, hasCallouts = true, stepCount = 1
        )

        val payload = judge.buildRequestPayload(promptCase, "Run `klaat serve`.", checklist)
        val genConfig = payload["generationConfig"]!!.jsonObject
        assertEquals("application/json", genConfig["responseMimeType"]!!.jsonPrimitive.content)
        assertEquals("low", genConfig["thinkingConfig"]!!.jsonObject["thinkingLevel"]!!.jsonPrimitive.content)

        val parsed = judge.parseJudgeResponse(
            """
            ```json
            {"score": 5, "passed": true, "adherenceScore": 5, "clarityScore": 4, "reasoning": "Accurate and concise."}
            ```
            """.trimIndent()
        )
        assertEquals(5, parsed.score)
        assertTrue(parsed.passed)
        assertEquals("gemini-4-neon", parsed.judgeModel)
        assertEquals("low", parsed.thinkingLevel)
        judge.close()
    }
}
