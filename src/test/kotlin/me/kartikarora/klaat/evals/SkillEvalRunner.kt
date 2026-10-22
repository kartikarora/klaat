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
    if (!evalsPropsFile.exists()) {
        System.err.println("""
            Error: 'evals.properties' file not found in project root.
            The Gemini API key can only be provided via 'evals.properties'.
            Please create an 'evals.properties' file with:
              geminiKey=your_gemini_api_key
              model=gemini-3.8-flash
              updateDocs=true
        """.trimIndent())
        System.exit(1)
        return
    }

    val evalsProps = java.util.Properties().apply {
        evalsPropsFile.inputStream().use { load(it) }
    }

    val apiKey = evalsProps.getProperty("geminiKey")?.trim()
    if (apiKey.isNullOrBlank()) {
        System.err.println("""
            Error: 'geminiKey' property is missing or blank in 'evals.properties'.
            Please specify in 'evals.properties':
              geminiKey=your_gemini_api_key
        """.trimIndent())
        System.exit(1)
        return
    }

    val model = evalsProps.getProperty("model")?.trim()?.ifBlank { null } ?: "gemini-3.8-flash"
    val updateDocs = evalsProps.getProperty("updateDocs")?.trim() == "true"
    val docsPath = System.getProperty("klaat.eval.docs.path") ?: "docs/docs/skills.html"
    val reportPath = System.getProperty("klaat.eval.report.path") ?: "build/reports/evals/eval-report.json"
    val datasetPath = System.getProperty("klaat.eval.dataset.path") ?: "evals/dataset.json"

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    println("Model: $model (Live Evaluation via Ktor CIO)")

    val datasetFile = File(datasetPath)
    if (!datasetFile.exists()) {
        System.err.println("Dataset file not found at: ${datasetFile.absolutePath}")
        System.exit(1)
        return
    }

    val dataset: List<PromptCase> = json.decodeFromString(datasetFile.readText())
    println("Loaded ${dataset.size} evaluation prompt cases.\n")

    val results = mutableListOf<PromptEvalResult>()

    LlmClient(apiKey = apiKey, model = model).use { client ->
        for ((index, promptCase) in dataset.withIndex()) {
            print("[${index + 1}/${dataset.size}] Testing prompt: '${promptCase.id}' (${promptCase.tier} / ${promptCase.category})... ")

            // 1. Baseline Run (no skill)
            val baselineOutput = client.runBaseline(promptCase.prompt)
            val baselineGrades = DeterministicGrader.grade(baselineOutput)
            val baselineResult = ConditionResult(
                invokedSkill = false,
                invokedSkillName = null,
                checklist = baselineGrades,
                rawOutputSnippet = baselineOutput.take(150).replace("\n", " ")
            )

            // 2. Agent Run (skills available in catalog, tool calling enabled)
            val agentExec = client.runWithAgent(promptCase.prompt)
            val agentGrades = DeterministicGrader.grade(agentExec.output)
            val agentResult = ConditionResult(
                invokedSkill = agentExec.invokedSkill,
                invokedSkillName = agentExec.invokedSkillName,
                checklist = agentGrades,
                rawOutputSnippet = agentExec.output.take(150).replace("\n", " ")
            )

            // 3. Trigger Success Evaluation
            val triggerSuccess = if (promptCase.isPositive) {
                agentExec.invokedSkill && agentExec.invokedSkillName == promptCase.expectedSkill
            } else {
                !agentExec.invokedSkill
            }

            val promptResult = PromptEvalResult(
                id = promptCase.id,
                category = promptCase.category,
                tier = promptCase.tier,
                isPositive = promptCase.isPositive,
                prompt = promptCase.prompt,
                baseline = baselineResult,
                withAgent = agentResult,
                triggerSuccess = triggerSuccess
            )
            results.add(promptResult)

            val triggerStatus = if (triggerSuccess) "PASS" else "FAIL"
            val compileStatus = if (agentGrades.compiles) "COMPILED" else "FAIL"
            val invokedDesc = if (agentExec.invokedSkill) "Triggered (${agentExec.invokedSkillName})" else "Skipped"
            println("Done. [$triggerStatus] $invokedDesc | Agent: $compileStatus")
        }
    }

    // Compute Aggregate Metrics
    val positiveCases = results.filter { it.isPositive }
    val negativeCases = results.filter { !it.isPositive }

    val positiveCount = positiveCases.size.coerceAtLeast(1)
    val negativeCount = negativeCases.size.coerceAtLeast(1)

    val triggerRecall = (positiveCases.count { it.withAgent.invokedSkill }.toDouble() / positiveCount) * 100.0
    val falsePositiveRate = (negativeCases.count { it.withAgent.invokedSkill }.toDouble() / negativeCount) * 100.0

    val baselineCompileRate = (positiveCases.count { it.baseline.checklist.compiles }.toDouble() / positiveCount) * 100.0
    val agentCompileRate = (positiveCases.count { it.withAgent.checklist.compiles }.toDouble() / positiveCount) * 100.0

    val baselineDurationRate = (positiveCases.count { it.baseline.checklist.hasValidDurations }.toDouble() / positiveCount) * 100.0
    val agentDurationRate = (positiveCases.count { it.withAgent.checklist.hasValidDurations }.toDouble() / positiveCount) * 100.0

    val baselineCalloutRate = (positiveCases.count { it.baseline.checklist.hasCallouts }.toDouble() / positiveCount) * 100.0
    val agentCalloutRate = (positiveCases.count { it.withAgent.checklist.hasCallouts }.toDouble() / positiveCount) * 100.0

    val metrics = AggregateMetrics(
        totalPrompts = results.size,
        positivePrompts = positiveCases.size,
        negativePrompts = negativeCases.size,
        triggerRecallPct = triggerRecall,
        falsePositiveRatePct = falsePositiveRate,
        baselineCompileRatePct = baselineCompileRate,
        agentCompileRatePct = agentCompileRate,
        baselineDurationCompliancePct = baselineDurationRate,
        agentDurationCompliancePct = agentDurationRate,
        baselineCalloutCompliancePct = baselineCalloutRate,
        agentCalloutCompliancePct = agentCalloutRate
    )

    val report = EvalReport(
        timestamp = Instant.now().toString(),
        model = model,
        mode = "Live API Evaluation",
        metrics = metrics,
        results = results
    )

    // Write Report JSON
    val reportFile = File(reportPath)
    reportFile.parentFile?.mkdirs()
    reportFile.writeText(json.encodeToString(report))
    println("\nSaved detailed JSON evaluation report to: ${reportFile.absolutePath}")

    // Print summary to console
    println("\n======================= BENCHMARK SUMMARY =======================")
    println("Autonomous Trigger Recall:      ${"%.1f".format(metrics.triggerRecallPct)}%")
    println("False Trigger Resistance:       ${"%.1f".format(100.0 - metrics.falsePositiveRatePct)}%")
    println("Klaat Compilation Pass Rate:    ${"%.1f".format(metrics.agentCompileRatePct)}% (vs. ${"%.1f".format(metrics.baselineCompileRatePct)}% baseline)")
    println("Duration Tag Adherence:         ${"%.1f".format(metrics.agentDurationCompliancePct)}% (vs. ${"%.1f".format(metrics.baselineDurationCompliancePct)}% baseline)")
    println("Callout Marker Adherence:       ${"%.1f".format(metrics.agentCalloutCompliancePct)}% (vs. ${"%.1f".format(metrics.baselineCalloutCompliancePct)}% baseline)")
    println("=================================================================\n")

    // Update Docs if requested
    if (updateDocs) {
        val docsFile = File(docsPath)
        DocUpdater.updateDocs(report, docsFile)
    }
}
