package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class InstallSkillsCommand : CliktCommand("install-skills") {
    private val repository by argument(
        help = "GitHub repository owner/name to install skills from (default: kartikarora/klaat)"
    ).default("kartikarora/klaat")

    override fun run() {
        val parts = repository.split("/")
        if (parts.size != 2) {
            System.err.println("✖ Invalid repository format. Use owner/repo (e.g., kartikarora/klaat).")
            return
        }

        val owner = parts[0]
        val repoName = parts[1]
        val targetDir = File(".gemini/skills")

        println("Installing skills from GitHub: $owner/$repoName into ${targetDir.absolutePath}...")

        try {
            // 1. Get default branch
            val repoUrl = "https://api.github.com/repos/$owner/$repoName"
            val repoJson = fetchJson(repoUrl)
            var branch = repoJson.get("default_branch")?.asString ?: "main"

            // Fallback for klaat repository during development
            if (owner == "kartikarora" && repoName == "klaat") {
                branch = "develop"
            }

            println("Using branch: $branch")

            // 2. Fetch recursive git tree
            val treeUrl = "https://api.github.com/repos/$owner/$repoName/git/trees/$branch?recursive=1"
            val treeJson = fetchJson(treeUrl)
            val treeArray = treeJson.getAsJsonArray("tree") ?: JsonArray()

            // 3. Find all directories containing SKILL.md
            val skillFiles = mutableListOf<String>()
            for (i in 0 until treeArray.size()) {
                val element = treeArray.get(i).asJsonObject
                val path = element.get("path")?.asString ?: continue
                val type = element.get("type")?.asString ?: continue
                
                if (type == "blob" && path.endsWith("SKILL.md")) {
                    skillFiles.add(path)
                }
            }

            if (skillFiles.isEmpty()) {
                println("No skill definitions (SKILL.md) found in this repository.")
                return
            }

            println("Found ${skillFiles.size} skills. Downloading files...")

            for (skillPath in skillFiles) {
                // Determine the skill folder name
                val skillFile = File(skillPath)
                val skillFolder = skillFile.parentFile ?: continue
                val skillFolderName = skillFolder.name

                println("\nInstalling skill: $skillFolderName...")

                // Find all sibling/child files belonging to the same skill directory
                val skillPrefix = skillFolder.path.replace('\\', '/') + "/"
                
                for (i in 0 until treeArray.size()) {
                    val element = treeArray.get(i).asJsonObject
                    val path = element.get("path")?.asString ?: continue
                    val type = element.get("type")?.asString ?: continue

                    if (type == "blob" && (path == skillPath || path.startsWith(skillPrefix))) {
                        // Determine relative path inside the skill folder
                        val relativeSubPath = if (path == skillPath) {
                            "SKILL.md"
                        } else {
                            path.substring(skillPrefix.length)
                        }

                        val localDestFile = File(File(targetDir, skillFolderName), relativeSubPath)
                        localDestFile.parentFile.mkdirs()

                        val downloadUrl = "https://raw.githubusercontent.com/$owner/$repoName/$branch/$path"
                        print("  Downloading $path -> ${localDestFile.name}... ")
                        
                        try {
                            val data = downloadBytes(downloadUrl)
                            localDestFile.writeBytes(data)
                            println("✔")
                        } catch (de: Exception) {
                            println("✖ (Failed: ${de.message})")
                        }
                    }
                }
            }

            println("\n🎉 Successfully installed all KLAAT skills from $repository!")
        } catch (e: Exception) {
            System.err.println("✖ Error installing skills: ${e.message}")
        }
    }

    private fun fetchJson(urlString: String): JsonObject {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "klaat-cli")
        conn.setRequestProperty("Accept", "application/json")

        val token = System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }

        if (conn.responseCode != 200) {
            throw Exception("HTTP ${conn.responseCode} response from $urlString")
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        return Gson().fromJson(jsonString, JsonObject::class.java)
    }

    private fun downloadBytes(urlString: String): ByteArray {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "klaat-cli")

        val token = System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }

        if (conn.responseCode != 200) {
            throw Exception("HTTP ${conn.responseCode} response from $urlString")
        }

        return conn.inputStream.use { it.readBytes() }
    }
}
