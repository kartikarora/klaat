package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AgentMapping(val indicator: String, val path: String)

@Serializable
data class MappingsConfig(
    val standard: List<AgentMapping>,
    val global_overrides: List<AgentMapping> = emptyList(),
    val local_overrides: List<AgentMapping> = emptyList()
)

/**
 * Command to discover active agent skill directories and install packaged Klaat skills.
 */
class InstallSkillsCommand : CliktCommand("install-skills") {
    private val global by option("-g", "--global", help = "Install to global user directory instead of project").flag(default = false)

    override fun run() {
        val detectedDirs = mutableListOf<File>()
        val baseDir = if (global) File(System.getProperty("user.home")) else File(".")

        // Load configuration mappings from JSON resource file
        val mappingsStream = javaClass.classLoader.getResourceAsStream("agent-mappings.json")
            ?: throw Exception("Klaat configuration error: 'agent-mappings.json' not found on classpath.")
        val mappingsJson = mappingsStream.bufferedReader().use { it.readText() }
        val mappings = Json.decodeFromString<MappingsConfig>(mappingsJson)

        val potentialAgentDirs = mutableMapOf<String, File>()

        // 1. Load standard mappings
        for (mapping in mappings.standard) {
            potentialAgentDirs[mapping.indicator] = File(baseDir, mapping.path)
        }

        // 2. Load overrides depending on scope
        val overrides = if (global) mappings.global_overrides else mappings.local_overrides
        for (mapping in overrides) {
            potentialAgentDirs[mapping.indicator] = File(baseDir, mapping.path)
        }

        for ((indicator, target) in potentialAgentDirs) {
            val indicatorFile = File(baseDir, indicator)
            if (indicatorFile.exists()) {
                detectedDirs.add(target)
            }
        }

        // Also check if data, agent, or skills folders exist relative to baseDir
        if (File(baseDir, "data").exists()) detectedDirs.add(File(baseDir, "data/skills"))
        if (File(baseDir, "agent").exists()) detectedDirs.add(File(baseDir, "agent/skills"))
        if (File(baseDir, "skills").exists()) detectedDirs.add(File(baseDir, "skills"))

        val scopeName = if (global) "global user" else "project"
        println("Installing Klaat AI formatting skills into your $scopeName workspace...")

        val skillFiles = listOf(
            // 1. Markdown Author Skill
            "klaat-markdown-author/SKILL.md",
            "klaat-markdown-author/references/template.md",
            "klaat-markdown-author/references/format-guide.md",

            // 2. Google Docs Author Skill
            "klaat-gdocs-author/SKILL.md",
            "klaat-gdocs-author/references/gdoc-mapping-reference.md",
            "klaat-gdocs-author/references/format-guide.md",

            // 3. Klaat CLI Skill
            "klaat-cli/SKILL.md",
            "klaat-cli/references/export-command.md",
            "klaat-cli/references/demo-command.md",
            "klaat-cli/references/serve-command.md",
            "klaat-cli/references/update-command.md",
            "klaat-cli/references/version-command.md",
            "klaat-cli/references/help-command.md",
            "klaat-cli/references/install-skills-command.md"
        )

        var installedCount = 0
        for (targetDir in detectedDirs) {
            try {
                for (relPath in skillFiles) {
                    val destFile = File(targetDir, relPath)
                    destFile.parentFile.mkdirs()
                    val resourceStream = javaClass.classLoader.getResourceAsStream("skills/$relPath")
                        ?: throw IllegalStateException("Bundled skill resource not found: skills/$relPath")
                    resourceStream.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                println("✔ Installed skills in: ${targetDir.path}/")
                installedCount++
            } catch (e: Exception) {
                System.err.println("⚠ Failed writing to ${targetDir.path}: ${e.message}")
            }
        }

        if (installedCount > 0) {
            println("\n🎉 Successfully installed Klaat AI skills to your active agent workspaces! Your AI assistant is now ready to parse, format, and generate codelabs following the format guides.")
        } else {
            System.err.println("\n✖ Error: Could not install skills into any workspace directories.")
        }
    }
}
