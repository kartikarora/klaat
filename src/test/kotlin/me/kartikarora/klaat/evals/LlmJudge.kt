package me.kartikarora.klaat.evals

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

class LlmJudge(
    private val apiKey: String,
    val model: String = DEFAULT_JUDGE_MODEL,
    val thinkingLevel: String = DEFAULT_THINKING_LEVEL
) : AutoCloseable {

    companion object {
        const val DEFAULT_JUDGE_MODEL = "gemini-4-neon"
        const val DEFAULT_THINKING_LEVEL = "low"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 120_000
        }
    }

    override fun close() {
        httpClient.close()
    }

    internal fun buildRequestPayload(
        promptCase: PromptCase,
        output: String,
        checklist: GradedChecklist
    ): JsonObject {
        val systemInstruction = """
            You are an expert technical documentation evaluator grading an AI assistant's response for Klaat.
            Score the response from 1 to 5 on:
            - adherenceScore (1-5): how well it followed the prompt requirements and Klaat conventions
            - clarityScore (1-5): technical accuracy, structure, and readability
            - score (1-5): overall quality score
            - passed (boolean): true if score >= 4 and no major inaccuracies
            - reasoning (string): 1-2 concise sentences explaining the grade
            Return ONLY valid JSON with keys: score, passed, adherenceScore, clarityScore, reasoning.
        """.trimIndent()

        val userPrompt = """
            Prompt ID: ${promptCase.id}
            Expected Skill: ${promptCase.expectedSkill ?: "none"}
            User Prompt: ${promptCase.prompt}
            Deterministic Checks: passed=${checklist.passed}, checks=${checklist.checks}, error=${checklist.error ?: "none"}

            Assistant Output:
            ${output.take(8000)}
        """.trimIndent()

        return buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("thinkingConfig", buildJsonObject {
                    put("thinkingLevel", thinkingLevel)
                })
            })
        }
    }

    internal fun parseJudgeResponse(rawText: String): LlmJudgeScore {
        val cleaned = DeterministicGrader.extractMarkdown(rawText)
        val obj = json.parseToJsonElement(cleaned).jsonObject
        val score = (obj["score"]?.jsonPrimitive?.intOrNull ?: 1).coerceIn(1, 5)
        val adherence = (obj["adherenceScore"]?.jsonPrimitive?.intOrNull ?: score).coerceIn(1, 5)
        val clarity = (obj["clarityScore"]?.jsonPrimitive?.intOrNull ?: score).coerceIn(1, 5)
        val passed = obj["passed"]?.jsonPrimitive?.booleanOrNull ?: (score >= 4)
        val reasoning = obj["reasoning"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

        return LlmJudgeScore(
            score = score,
            passed = passed,
            adherenceScore = adherence,
            clarityScore = clarity,
            reasoning = reasoning,
            judgeModel = model,
            thinkingLevel = thinkingLevel
        )
    }

    fun judge(
        promptCase: PromptCase,
        output: String,
        checklist: GradedChecklist
    ): LlmJudgeScore {
        val payload = buildRequestPayload(promptCase, output, checklist)
        val responseJson = postGemini(payload)
        val text = responseJson["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            .orEmpty()

        return parseJudgeResponse(text)
    }

    private fun postGemini(payload: JsonObject): JsonObject = runBlocking {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        var retries = 3
        var backoffMs = 2000L

        while (true) {
            val response: HttpResponse = httpClient.post(url) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val statusCode = response.status.value
            val responseBody = response.bodyAsText()

            if (statusCode in 200..299) {
                return@runBlocking json.parseToJsonElement(responseBody).jsonObject
            }

            if ((statusCode == 503 || statusCode == 429) && retries > 0) {
                delay(backoffMs)
                retries--
                backoffMs *= 2
                continue
            }

            throw RuntimeException("Gemini Judge API error HTTP $statusCode: $responseBody")
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }
}
