package me.kartikarora.klaat.evals

import kotlinx.serialization.Serializable

@Serializable
data class PromptCase(
    val id: String,
    val category: String,
    val tier: String = "medium",
    val prompt: String,
    val expectedSkill: String? = null,
    val isPositive: Boolean,
    val expectedPatterns: List<String> = emptyList()
)

@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

@Serializable
data class GradedChecklist(
    val passed: Boolean = false,
    val compiles: Boolean,
    val hasValidMetadata: Boolean,
    val hasValidDurations: Boolean,
    val hasCallouts: Boolean,
    val stepCount: Int,
    val extractedId: String? = null,
    val extractedSummary: String? = null,
    val error: String? = null,
    val checks: Map<String, Boolean> = emptyMap()
)

@Serializable
data class LlmJudgeScore(
    val score: Int,
    val passed: Boolean,
    val adherenceScore: Int = score,
    val clarityScore: Int = score,
    val reasoning: String = "",
    val judgeModel: String = "gemini-4-neon",
    val thinkingLevel: String = "low"
)

@Serializable
data class ConditionResult(
    val invokedSkill: Boolean,
    val invokedSkillName: String? = null,
    val checklist: GradedChecklist,
    val rawOutputSnippet: String,
    val latencyMs: Long = 0,
    val tokenUsage: TokenUsage = TokenUsage(),
    val llmJudge: LlmJudgeScore? = null
)

@Serializable
data class PromptEvalResult(
    val id: String,
    val category: String,
    val tier: String = "medium",
    val isPositive: Boolean,
    val expectedSkill: String? = null,
    val prompt: String,
    val baseline: ConditionResult,
    val withAgent: ConditionResult,
    val triggerSuccess: Boolean
)

@Serializable
data class SkillMetrics(
    val skillName: String,
    val totalCases: Int,
    val triggerRecallPct: Double,
    val baselinePassRatePct: Double,
    val agentPassRatePct: Double,
    val avgBaselineLatencyMs: Long = 0,
    val avgAgentLatencyMs: Long = 0,
    val avgAgentTotalTokens: Int = 0,
    val avgBaselineJudgeScore: Double? = null,
    val avgAgentJudgeScore: Double? = null
)

@Serializable
data class AggregateMetrics(
    val totalPrompts: Int,
    val positivePrompts: Int,
    val negativePrompts: Int,
    val triggerRecallPct: Double,
    val triggerPrecisionPct: Double = 0.0,
    val triggerF1Pct: Double = 0.0,
    val falsePositiveRatePct: Double,
    val baselinePassRatePct: Double = 0.0,
    val agentPassRatePct: Double = 0.0,
    val baselineCompileRatePct: Double,
    val agentCompileRatePct: Double,
    val baselineDurationCompliancePct: Double,
    val agentDurationCompliancePct: Double,
    val baselineCalloutCompliancePct: Double,
    val agentCalloutCompliancePct: Double,
    val avgBaselineJudgeScore: Double? = null,
    val avgAgentJudgeScore: Double? = null,
    val perSkill: List<SkillMetrics> = emptyList()
)

@Serializable
data class EvalHistoryEntry(
    val timestamp: String,
    val model: String,
    val triggerRecallPct: Double,
    val falsePositiveRatePct: Double,
    val agentPassRatePct: Double,
    val baselinePassRatePct: Double
)

@Serializable
data class EvalReport(
    val timestamp: String,
    val model: String,
    val mode: String,
    val judgeModel: String? = null,
    val judgeThinkingLevel: String? = null,
    val metrics: AggregateMetrics,
    val results: List<PromptEvalResult>,
    val history: List<EvalHistoryEntry> = emptyList()
)
