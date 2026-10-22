package me.kartikarora.klaat.evals

import kotlinx.serialization.Serializable

@Serializable
data class PromptCase(
    val id: String,
    val category: String,
    val tier: String = "medium",
    val prompt: String,
    val expectedSkill: String? = null,
    val isPositive: Boolean
)

@Serializable
data class GradedChecklist(
    val compiles: Boolean,
    val hasValidMetadata: Boolean,
    val hasValidDurations: Boolean,
    val hasCallouts: Boolean,
    val stepCount: Int,
    val extractedId: String? = null,
    val extractedSummary: String? = null,
    val error: String? = null
)

@Serializable
data class ConditionResult(
    val invokedSkill: Boolean,
    val invokedSkillName: String? = null,
    val checklist: GradedChecklist,
    val rawOutputSnippet: String
)

@Serializable
data class PromptEvalResult(
    val id: String,
    val category: String,
    val tier: String = "medium",
    val isPositive: Boolean,
    val prompt: String,
    val baseline: ConditionResult,
    val withAgent: ConditionResult,
    val triggerSuccess: Boolean
)

@Serializable
data class AggregateMetrics(
    val totalPrompts: Int,
    val positivePrompts: Int,
    val negativePrompts: Int,
    val triggerRecallPct: Double,
    val falsePositiveRatePct: Double,
    val baselineCompileRatePct: Double,
    val agentCompileRatePct: Double,
    val baselineDurationCompliancePct: Double,
    val agentDurationCompliancePct: Double,
    val baselineCalloutCompliancePct: Double,
    val agentCalloutCompliancePct: Double
)

@Serializable
data class EvalReport(
    val timestamp: String,
    val model: String,
    val mode: String,
    val metrics: AggregateMetrics,
    val results: List<PromptEvalResult>
)
