package me.kartikarora.klaat.evals

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object DocUpdater {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun writeJsonReport(report: EvalReport, targetFile: File, maxHistory: Int = 10) {
        targetFile.parentFile?.mkdirs()

        val existingHistory = if (targetFile.exists()) {
            runCatching { json.decodeFromString<EvalReport>(targetFile.readText()).history }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val currentEntry = EvalHistoryEntry(
            timestamp = report.timestamp,
            model = report.model,
            triggerRecallPct = report.metrics.triggerRecallPct,
            falsePositiveRatePct = report.metrics.falsePositiveRatePct,
            agentPassRatePct = report.metrics.agentPassRatePct,
            baselinePassRatePct = report.metrics.baselinePassRatePct
        )

        val mergedHistory = (listOf(currentEntry) + existingHistory.filterNot { it.timestamp == currentEntry.timestamp })
            .take(maxHistory)

        val reportWithHistory = report.copy(history = mergedHistory)
        targetFile.writeText(json.encodeToString(reportWithHistory) + "\n")
        println("Saved JSON evaluation report to: ${targetFile.absolutePath}")
    }

    fun writeMarkdownSummary(report: EvalReport, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        val m = report.metrics
        val sb = StringBuilder()

        sb.appendLine("# Klaat AI Agent Skills Benchmark")
        sb.appendLine()
        sb.appendLine("- **Model:** `${report.model}`")
        sb.appendLine("- **Mode:** ${report.mode}")
        sb.appendLine("- **Timestamp:** ${report.timestamp}")
        sb.appendLine("- **Prompts:** ${m.totalPrompts} (${m.positivePrompts} positive, ${m.negativePrompts} distractors)")
        sb.appendLine()
        sb.appendLine("## Overall Scorecard")
        sb.appendLine()
        sb.appendLine("| Metric | Baseline | With Skill | Delta |")
        sb.appendLine("| --- | ---: | ---: | ---: |")
        sb.appendLine("| Overall Pass Rate | ${"%.1f".format(m.baselinePassRatePct)}% | ${"%.1f".format(m.agentPassRatePct)}% | ${formatDelta(m.agentPassRatePct - m.baselinePassRatePct)} |")
        sb.appendLine("| Markdown Compilation | ${"%.1f".format(m.baselineCompileRatePct)}% | ${"%.1f".format(m.agentCompileRatePct)}% | ${formatDelta(m.agentCompileRatePct - m.baselineCompileRatePct)} |")
        sb.appendLine("| Duration Tag Adherence | ${"%.1f".format(m.baselineDurationCompliancePct)}% | ${"%.1f".format(m.agentDurationCompliancePct)}% | ${formatDelta(m.agentDurationCompliancePct - m.baselineDurationCompliancePct)} |")
        sb.appendLine("| Callout Marker Adherence | ${"%.1f".format(m.baselineCalloutCompliancePct)}% | ${"%.1f".format(m.agentCalloutCompliancePct)}% | ${formatDelta(m.agentCalloutCompliancePct - m.baselineCalloutCompliancePct)} |")
        if (m.avgAgentJudgeScore != null && m.avgBaselineJudgeScore != null) {
            val delta = m.avgAgentJudgeScore - m.avgBaselineJudgeScore
            val sign = if (delta >= 0) "+" else ""
            sb.appendLine("| LLM Judge Score (`${report.judgeModel ?: "gemini-4-hydrogen"}`) | ${"%.2f".format(m.avgBaselineJudgeScore)}/5 | ${"%.2f".format(m.avgAgentJudgeScore)}/5 | $sign${"%.2f".format(delta)} |")
        }
        sb.appendLine()
        sb.appendLine("**Triggering:** Recall `${"%.1f".format(m.triggerRecallPct)}%` · Precision `${"%.1f".format(m.triggerPrecisionPct)}%` · F1 `${"%.1f".format(m.triggerF1Pct)}%` · False Positive Rate `${"%.1f".format(m.falsePositiveRatePct)}%`")
        sb.appendLine()

        if (m.perSkill.isNotEmpty()) {
            sb.appendLine("## Per-Skill Breakdown")
            sb.appendLine()
            sb.appendLine("| Skill | Cases | Trigger Recall | Baseline Pass | Agent Pass | Avg Agent Latency | Avg Tokens |")
            sb.appendLine("| --- | ---: | ---: | ---: | ---: | ---: | ---: |")
            for (skill in m.perSkill) {
                sb.appendLine("| `${skill.skillName}` | ${skill.totalCases} | ${"%.1f".format(skill.triggerRecallPct)}% | ${"%.1f".format(skill.baselinePassRatePct)}% | ${"%.1f".format(skill.agentPassRatePct)}% | ${skill.avgAgentLatencyMs} ms | ${skill.avgAgentTotalTokens} |")
            }
            sb.appendLine()
        }

        val failures = report.results.filter { !it.triggerSuccess || (it.isPositive && !it.withAgent.checklist.passed) }
        if (failures.isNotEmpty()) {
            sb.appendLine("## Failures & Diagnostics")
            sb.appendLine()
            for (f in failures) {
                val reason = buildList {
                    if (!f.triggerSuccess) {
                        add("trigger=${f.withAgent.invokedSkillName ?: "none"} (expected ${f.expectedSkill ?: "none"})")
                    }
                    if (f.isPositive && !f.withAgent.checklist.passed) {
                        add("grade=${f.withAgent.checklist.error ?: "failed checks"}")
                    }
                }.joinToString("; ")
                sb.appendLine("- **`${f.id}`**: $reason")
            }
        }

        targetFile.writeText(sb.toString())
        println("Saved Markdown evaluation summary to: ${targetFile.absolutePath}")
    }

    private fun formatDelta(delta: Double): String {
        val sign = if (delta >= 0) "+" else ""
        return "$sign${"%.1f".format(delta)}%"
    }
}
