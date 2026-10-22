package me.kartikarora.klaat.evals

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File

data class AgentExecutionResult(
    val invokedSkill: Boolean,
    val invokedSkillName: String?,
    val output: String
)

class LlmClient(
    private val apiKey: String,
    private val model: String = "gemini-3.5-flash-lite",
    private val skillsDir: File = File("skills")
) : AutoCloseable {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 120_000
        }
    }

    override fun close() {
        httpClient.close()
    }

    /**
     * Executes the baseline run (standard coding prompt, no skills provided, no tools).
     */
    fun runBaseline(prompt: String): String {
        val systemInstruction = "You are a helpful AI coding assistant. Help the user author technical tutorials and documents."
        val requestBody = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
        }

        val responseJson = postGemini(requestBody)
        return extractTextFromResponse(responseJson)
    }

    /**
     * Executes the agent simulation (skills available in catalog, agent can autonomously invoke read_skill tool).
     */
    fun runWithAgent(prompt: String): AgentExecutionResult {
        val availableSkillsDescription = buildSkillCatalog()
        val systemInstruction = """
            You are an expert AI software engineering agent.
            You have access to specialized skills in this project:
            $availableSkillsDescription
            
            Before answering, evaluate if any available skill is directly relevant to the user request.
            If a skill is relevant, you MUST call the `read_skill` tool with its exact skill_name to read its guidelines before generating your final response.
            If no skill is relevant, answer directly using standard best practices without calling `read_skill`.
        """.trimIndent()

        val toolsDeclaration = buildJsonArray {
            add(buildJsonObject {
                put("function_declarations", buildJsonArray {
                    add(buildJsonObject {
                        put("name", "read_skill")
                        put("description", "Reads the official authoring guide, metadata rules, and templates for an installed agent skill.")
                        put("parameters", buildJsonObject {
                            put("type", "OBJECT")
                            put("properties", buildJsonObject {
                                put("skill_name", buildJsonObject {
                                    put("type", "STRING")
                                    put("description", "Name of skill (e.g. 'klaat-markdown-author', 'klaat-cli', 'klaat-gdocs-author')")
                                })
                            })
                            put("required", buildJsonArray { add(JsonPrimitive("skill_name")) })
                        })
                    })
                })
            })
        }

        val conversation = mutableListOf<JsonObject>()
        conversation.add(buildJsonObject {
            put("role", "user")
            put("parts", buildJsonArray {
                add(buildJsonObject { put("text", prompt) })
            })
        })

        val initialRequestBody = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("tools", toolsDeclaration)
            put("contents", JsonArray(conversation))
        }

        val turn1Response = postGemini(initialRequestBody)
        val functionCall = extractFunctionCall(turn1Response)

        if (functionCall != null && functionCall.first == "read_skill") {
            val skillName = functionCall.second
            val skillContent = loadSkillContent(skillName)

            val modelCandidate = turn1Response["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            val modelContent = modelCandidate?.get("content")?.jsonObject
            if (modelContent != null) {
                conversation.add(modelContent)
            }

            conversation.add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("function_response", buildJsonObject {
                            put("name", "read_skill")
                            put("response", buildJsonObject {
                                put("content", skillContent)
                            })
                        })
                    })
                })
            })

            val followUpRequestBody = buildJsonObject {
                put("system_instruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", systemInstruction) })
                    })
                })
                put("contents", JsonArray(conversation))
            }

            val turn2Response = postGemini(followUpRequestBody)
            val finalText = extractTextFromResponse(turn2Response)
            return AgentExecutionResult(
                invokedSkill = true,
                invokedSkillName = skillName,
                output = finalText
            )
        } else {
            val directText = extractTextFromResponse(turn1Response)
            return AgentExecutionResult(
                invokedSkill = false,
                invokedSkillName = null,
                output = directText
            )
        }
    }

    private fun buildSkillCatalog(): String {
        val catalog = StringBuilder()
        val skills = listOf("klaat-markdown-author", "klaat-cli", "klaat-gdocs-author")
        for (skillName in skills) {
            val skillFile = File(skillsDir, "$skillName/SKILL.md")
            if (skillFile.exists()) {
                val description = extractFrontmatterDescription(skillFile.readText())
                catalog.append("- `$skillName`: $description\n")
            }
        }
        return catalog.toString()
    }

    private fun extractFrontmatterDescription(content: String): String {
        val match = Regex("""(?m)^description:\s*(?:>)?\s*(.*?)(?=\n[a-z_-]+:|\n---|\z)""", RegexOption.DOT_MATCHES_ALL).find(content)
        return match?.groupValues?.get(1)?.replace("\n", " ")?.trim() ?: "Specialized Klaat skill."
    }

    private fun loadSkillContent(skillName: String): String {
        val skillFile = File(skillsDir, "$skillName/SKILL.md")
        if (!skillFile.exists()) return "Skill '$skillName' not found."

        val sb = StringBuilder(skillFile.readText())
        val refDir = File(skillsDir, "$skillName/references")
        if (refDir.exists() && refDir.isDirectory) {
            refDir.listFiles()?.filter { it.extension == "md" }?.forEach { ref ->
                sb.append("\n\n--- Reference: ${ref.name} ---\n")
                sb.append(ref.readText())
            }
        }
        return sb.toString()
    }

    private fun postGemini(payload: JsonObject): JsonObject = runBlocking {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        var retries = 3
        var backoffMs = 2000L

        while (true) {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val statusCode = response.status.value
            val responseBody = response.bodyAsText()

            if (statusCode in 200..299) {
                return@runBlocking json.parseToJsonElement(responseBody).jsonObject
            }

            if ((statusCode == 503 || statusCode == 429) && retries > 0) {
                println("\n[Ktor CIO Client] HTTP $statusCode from Gemini API. Retrying in ${backoffMs / 1000}s... ($retries retries left)")
                delay(backoffMs)
                retries--
                backoffMs *= 2
                continue
            }

            throw RuntimeException("Gemini API error HTTP $statusCode: $responseBody")
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }

    private fun extractTextFromResponse(res: JsonObject): String {
        val parts = res["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray ?: return ""
        val sb = StringBuilder()
        for (part in parts) {
            val text = part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            if (text != null) sb.append(text)
        }
        return sb.toString()
    }

    private fun extractFunctionCall(res: JsonObject): Pair<String, String>? {
        val parts = res["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray ?: return null
        for (part in parts) {
            val call = part.jsonObject["functionCall"]?.jsonObject ?: continue
            val name = call["name"]?.jsonPrimitive?.content ?: continue
            val args = call["args"]?.jsonObject
            val skillName = args?.get("skill_name")?.jsonPrimitive?.content ?: ""
            return Pair(name, skillName)
        }
        return null
    }
}
