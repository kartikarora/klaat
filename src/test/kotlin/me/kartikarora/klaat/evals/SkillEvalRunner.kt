package me.kartikarora.klaat.evals

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

fun main() {
    println("=================================================================")
    println("              Klaat AI Agent Skills Evaluation Suite             ")
    println("=================================================================")

    val evalsPropsFile = File("evals.properties")
    val evalsProps = java.util.Properties()
    if (evalsPropsFile.exists()) {
        evalsPropsFile.inputStream().use { evalsProps.load(it) }
    }

    val apiKey = evalsProps.getProperty("geminiKey")?.trim()?.ifBlank { null }
        ?: System.getenv("GEMINI_API_KEY")?.trim()?.ifBlank { null }

    if (apiKey.isNullOrBlank()) {
        System.err.println(
            """
            Error: Gemini API key not found.
            Provide it either via 'evals.properties' (copy evals.properties.template) or GEMINI_API_KEY env var:
              geminiKey=your_gemini_api_key
              model=gemini-2.5-flash
              updateDocs=true
            """.trimIndent()
        )
        System.exit(1)
        return
    }

    val model = evalsProps.getProperty("model")?.trim()?.ifBlank { null }
        ?: System.getenv("GEMINI_MODEL")?.trim()?.ifBlank { null }
        ?: "gemini-4-hydrogen"

    val modelThinkingLevel = evalsProps.getProperty("modelThinkingLevel")?.trim()?.ifBlank { null }
        ?: System.getenv("GEMINI_MODEL_THINKING_LEVEL")?.trim()?.ifBlank { null }
        ?: "low"

    val judgeModel = evalsProps.getProperty("judgeModel")?.trim()?.ifBlank { null }
        ?: System.getenv("GEMINI_JUDGE_MODEL")?.trim()?.ifBlank { null }
        ?: LlmJudge.DEFAULT_JUDGE_MODEL

    val judgeThinkingLevel = evalsProps.getProperty("judgeThinkingLevel")?.trim()?.ifBlank { null }
        ?: System.getenv("GEMINI_JUDGE_THINKING_LEVEL")?.trim()?.ifBlank { null }
        ?: LlmJudge.DEFAULT_THINKING_LEVEL

    val enableLlmJudge = evalsProps.getProperty("enableLlmJudge")?.trim()?.toBooleanStrictOrNull()
        ?: System.getenv("ENABLE_LLM_JUDGE")?.trim()?.toBooleanStrictOrNull()
        ?: true

    val updateDocs = System.getProperty("klaat.eval.updateDocs")?.trim()?.toBooleanStrictOrNull()
        ?: evalsProps.getProperty("updateDocs")?.trim()?.toBooleanStrictOrNull()
        ?: (System.getenv("UPDATE_DOCS")?.trim()?.toBooleanStrictOrNull() ?: false)

    val docsJsonPath = System.getProperty("klaat.eval.docs.json.path") ?: "docs/docs/eval-report.json"
    val reportPath = System.getProperty("klaat.eval.report.path") ?: "build/reports/evals/eval-report.json"
    val summaryPath = System.getProperty("klaat.eval.summary.path") ?: "build/reports/evals/eval-summary.md"
    val datasetPath = System.getProperty("klaat.eval.dataset.path") ?: "evals/dataset.json"

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    println("Model: $model (thinkingLevel=$modelThinkingLevel, Live Evaluation via Ktor CIO)")
    if (enableLlmJudge) {
        println("Judge: $judgeModel (thinkingLevel=$judgeThinkingLevel)")
    }

    val datasetFile = File(datasetPath)
    if (!datasetFile.exists()) {
        System.err.println("Dataset file not found at: ${datasetFile.absolutePath}")
        System.exit(1)
        return
    }

    val dataset: List<PromptCase> = json.decodeFromString(datasetFile.readText())
    println("Loaded ${dataset.size} evaluation prompt cases.\n")

    val results = mutableListOf<PromptEvalResult>()

    LlmClient(apiKey = apiKey, model = model, thinkingLevel = modelThinkingLevel).use { client ->
        val judge = if (enableLlmJudge) LlmJudge(apiKey = apiKey, model = judgeModel, thinkingLevel = judgeThinkingLevel) else null
        try {
            for ((index, promptCase) in dataset.withIndex()) {
                print("[${index + 1}/${dataset.size}] Testing prompt: '${promptCase.id}' (${promptCase.tier} / ${promptCase.category})... ")

                // 1. Baseline Run (no skill)
                val baselineExec = client.runBaseline(promptCase.prompt)
                val baselineGrades = DeterministicGrader.grade(
                    rawResponse = baselineExec.output,
                    expectedSkill = promptCase.expectedSkill,
                    expectedPatterns = promptCase.expectedPatterns
                )
                val baselineJudge = if (judge != null && promptCase.isPositive) {
                    runCatching { judge.judge(promptCase, baselineExec.output, baselineGrades) }
                        .onFailure { System.err.println("\n⚠️ Judge skipped (baseline): ${it.message}") }
                        .getOrNull()
                } else null
                val baselineResult = ConditionResult(
                    invokedSkill = false,
                    invokedSkillName = null,
                    checklist = baselineGrades,
                    rawOutputSnippet = baselineExec.output.take(150).replace("\n", " "),
                    latencyMs = baselineExec.latencyMs,
                    tokenUsage = baselineExec.tokenUsage,
                    llmJudge = baselineJudge
                )

                // 2. Agent Run (skills available in catalog, tool calling enabled)
                val agentExec = client.runWithAgent(promptCase.prompt)
                val agentGrades = DeterministicGrader.grade(
                    rawResponse = agentExec.output,
                    expectedSkill = promptCase.expectedSkill,
                    expectedPatterns = promptCase.expectedPatterns
                )
                val agentJudge = if (judge != null && promptCase.isPositive) {
                    runCatching { judge.judge(promptCase, agentExec.output, agentGrades) }
                        .onFailure { System.err.println("\n⚠️ Judge skipped (agent): ${it.message}") }
                        .getOrNull()
                } else null
                val agentResult = ConditionResult(
                    invokedSkill = agentExec.invokedSkill,
                    invokedSkillName = agentExec.invokedSkillName,
                    checklist = agentGrades,
                    rawOutputSnippet = agentExec.output.take(150).replace("\n", " "),
                    latencyMs = agentExec.latencyMs,
                    tokenUsage = agentExec.tokenUsage,
                    llmJudge = agentJudge
                )

                // 3. Trigger Success Evaluation
                val triggerSuccess = if (promptCase.isPositive) {
                    agentExec.invokedSkill && agentExec.invokedSkillName == promptCase.expectedSkill
                } else {
                    !agentExec.invokedSkill
                }

                results.add(
                    PromptEvalResult(
                        id = promptCase.id,
                        category = promptCase.category,
                        tier = promptCase.tier,
                        isPositive = promptCase.isPositive,
                        expectedSkill = promptCase.expectedSkill,
                        prompt = promptCase.prompt,
                        baseline = baselineResult,
                        withAgent = agentResult,
                        triggerSuccess = triggerSuccess
                    )
                )

                val triggerStatus = if (triggerSuccess) "PASS" else "FAIL"
                val gradeStatus = if (agentGrades.passed) "PASSED" else "FAIL"
                val invokedDesc = if (agentExec.invokedSkill) "Triggered (${agentExec.invokedSkillName})" else "Skipped"
                val judgeDesc = agentJudge?.let { " | Judge: ${it.score}/5" }.orEmpty()
                println("Done. [$triggerStatus] $invokedDesc | Agent: $gradeStatus$judgeDesc (${agentExec.latencyMs}ms)")
                if (index < dataset.lastIndex) {
                    Thread.sleep(2000)
                }
            }
        } finally {
            judge?.close()
        }
    }

    val metrics = EvalMetrics.compute(results)
    val report = EvalReport(
        timestamp = Instant.now().toString(),
        model = model,
        mode = "Live API Evaluation",
        judgeModel = if (enableLlmJudge) judgeModel else null,
        judgeThinkingLevel = if (enableLlmJudge) judgeThinkingLevel else null,
        metrics = metrics,
        results = results
    )

    // Write Build Report JSON & Markdown Summary
    val reportFile = File(reportPath)
    reportFile.parentFile?.mkdirs()
    reportFile.writeText(json.encodeToString(report) + "\n")
    println("\nSaved detailed JSON evaluation report to: ${reportFile.absolutePath}")

    DocUpdater.writeMarkdownSummary(report, File(summaryPath))

    // Print summary to console
    println("\n======================= BENCHMARK SUMMARY =======================")
    println("Autonomous Trigger Recall:      ${"%.1f".format(metrics.triggerRecallPct)}%")
    println("Trigger Precision / F1:         ${"%.1f".format(metrics.triggerPrecisionPct)}% / ${"%.1f".format(metrics.triggerF1Pct)}%")
    println("False Trigger Resistance:       ${"%.1f".format(100.0 - metrics.falsePositiveRatePct)}%")
    println("Overall Agent Pass Rate:        ${"%.1f".format(metrics.agentPassRatePct)}% (vs. ${"%.1f".format(metrics.baselinePassRatePct)}% baseline)")
    println("Markdown Compilation Pass Rate: ${"%.1f".format(metrics.agentCompileRatePct)}% (vs. ${"%.1f".format(metrics.baselineCompileRatePct)}% baseline)")
    println("Duration Tag Adherence:         ${"%.1f".format(metrics.agentDurationCompliancePct)}% (vs. ${"%.1f".format(metrics.baselineDurationCompliancePct)}% baseline)")
    println("Callout Marker Adherence:       ${"%.1f".format(metrics.agentCalloutCompliancePct)}% (vs. ${"%.1f".format(metrics.baselineCalloutCompliancePct)}% baseline)")
    if (metrics.avgAgentJudgeScore != null && metrics.avgBaselineJudgeScore != null) {
        println("LLM Judge Score ($judgeModel): ${"%.2f".format(metrics.avgAgentJudgeScore)}/5 (vs. ${"%.2f".format(metrics.avgBaselineJudgeScore)}/5 baseline)")
    }
    println("=================================================================\n")

    if (updateDocs) {
        DocUpdater.writeJsonReport(report, File(docsJsonPath))
    }
}
