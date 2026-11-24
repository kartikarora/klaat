package me.kartikarora.klaat.evals

object EvalMetrics {

    fun compute(results: List<PromptEvalResult>): AggregateMetrics {
        val positiveCases = results.filter { it.isPositive }
        val negativeCases = results.filter { !it.isPositive }

        val positiveCount = positiveCases.size.coerceAtLeast(1)
        val negativeCount = negativeCases.size.coerceAtLeast(1)

        val truePositives = positiveCases.count { it.triggerSuccess }
        val falsePositives = negativeCases.count { it.withAgent.invokedSkill } +
            positiveCases.count { it.withAgent.invokedSkill && !it.triggerSuccess }
        val totalTriggered = truePositives + falsePositives

        val triggerRecall = (truePositives.toDouble() / positiveCount) * 100.0
        val triggerPrecision = if (totalTriggered == 0) 0.0 else (truePositives.toDouble() / totalTriggered) * 100.0
        val triggerF1 = if (triggerPrecision + triggerRecall == 0.0) 0.0 else {
            2.0 * (triggerPrecision * triggerRecall) / (triggerPrecision + triggerRecall)
        }
        val falsePositiveRate = (negativeCases.count { it.withAgent.invokedSkill }.toDouble() / negativeCount) * 100.0

        val baselinePassRate = (positiveCases.count { it.baseline.checklist.passed }.toDouble() / positiveCount) * 100.0
        val agentPassRate = (positiveCases.count { it.withAgent.checklist.passed }.toDouble() / positiveCount) * 100.0

        val markdownCases = positiveCases.filter {
            it.expectedSkill == null || it.expectedSkill == "klaat-markdown-author"
        }
        val markdownCount = markdownCases.size.coerceAtLeast(1)

        val baselineCompileRate = (markdownCases.count { it.baseline.checklist.compiles }.toDouble() / markdownCount) * 100.0
        val agentCompileRate = (markdownCases.count { it.withAgent.checklist.compiles }.toDouble() / markdownCount) * 100.0

        val baselineDurationRate = (markdownCases.count { it.baseline.checklist.hasValidDurations }.toDouble() / markdownCount) * 100.0
        val agentDurationRate = (markdownCases.count { it.withAgent.checklist.hasValidDurations }.toDouble() / markdownCount) * 100.0

        val baselineCalloutRate = (markdownCases.count { it.baseline.checklist.hasCallouts }.toDouble() / markdownCount) * 100.0
        val agentCalloutRate = (markdownCases.count { it.withAgent.checklist.hasCallouts }.toDouble() / markdownCount) * 100.0

        val perSkill = positiveCases
            .groupBy { it.expectedSkill ?: "unknown" }
            .entries
            .sortedBy { it.key }
            .map { (skillName, cases) ->
                val count = cases.size.coerceAtLeast(1)
                SkillMetrics(
                    skillName = skillName,
                    totalCases = cases.size,
                    triggerRecallPct = (cases.count { it.triggerSuccess }.toDouble() / count) * 100.0,
                    baselinePassRatePct = (cases.count { it.baseline.checklist.passed }.toDouble() / count) * 100.0,
                    agentPassRatePct = (cases.count { it.withAgent.checklist.passed }.toDouble() / count) * 100.0,
                    avgBaselineLatencyMs = cases.map { it.baseline.latencyMs }.average().toLong(),
                    avgAgentLatencyMs = cases.map { it.withAgent.latencyMs }.average().toLong(),
                    avgAgentTotalTokens = cases.map { it.withAgent.tokenUsage.totalTokens }.average().toInt(),
                    avgBaselineJudgeScore = cases.mapNotNull { it.baseline.llmJudge?.score }.takeIf { it.isNotEmpty() }?.average(),
                    avgAgentJudgeScore = cases.mapNotNull { it.withAgent.llmJudge?.score }.takeIf { it.isNotEmpty() }?.average()
                )
            }

        val avgBaselineJudgeScore = positiveCases.mapNotNull { it.baseline.llmJudge?.score }.takeIf { it.isNotEmpty() }?.average()
        val avgAgentJudgeScore = positiveCases.mapNotNull { it.withAgent.llmJudge?.score }.takeIf { it.isNotEmpty() }?.average()

        return AggregateMetrics(
            totalPrompts = results.size,
            positivePrompts = positiveCases.size,
            negativePrompts = negativeCases.size,
            triggerRecallPct = triggerRecall,
            triggerPrecisionPct = triggerPrecision,
            triggerF1Pct = triggerF1,
            falsePositiveRatePct = falsePositiveRate,
            baselinePassRatePct = baselinePassRate,
            agentPassRatePct = agentPassRate,
            baselineCompileRatePct = baselineCompileRate,
            agentCompileRatePct = agentCompileRate,
            baselineDurationCompliancePct = baselineDurationRate,
            agentDurationCompliancePct = agentDurationRate,
            baselineCalloutCompliancePct = baselineCalloutRate,
            agentCalloutCompliancePct = agentCalloutRate,
            avgBaselineJudgeScore = avgBaselineJudgeScore,
            avgAgentJudgeScore = avgAgentJudgeScore,
            perSkill = perSkill
        )
    }
}
