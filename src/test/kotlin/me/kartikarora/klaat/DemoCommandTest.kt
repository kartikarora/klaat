package me.kartikarora.klaat

import com.github.ajalt.clikt.core.parse
import me.kartikarora.klaat.commands.DemoCommand
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class DemoCommandTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var workingDir: File

    @BeforeEach
    fun setUp() {
        workingDir = tempDir.toFile()
    }

    @Test
    fun `demo command extracts files and exports codelab`() {
        val demoCommand = DemoCommand(workingDir = workingDir, startServer = false)
        demoCommand.parse(listOf("-o", "output", "--no-serve"))

        // Verify extracted files in PWD
        val mdFile = File(workingDir, "feature-showcase.md")
        val imgFile = File(workingDir, "images/klaat.png")
        assertTrue(mdFile.exists(), "feature-showcase.md should be extracted to PWD")
        assertTrue(imgFile.exists(), "images/klaat.png should be extracted to PWD")

        // Verify exported codelab in output directory
        val exportedHtml = File(workingDir, "output/klaat-feature-showcase/index.html")
        assertTrue(exportedHtml.exists(), "Exported demo index.html should exist")
        val content = exportedHtml.readText()
        assertTrue(content.contains("Feature Showcase"), "Exported HTML should contain showcase title")
        assertTrue(content.contains("klaat-kodelab"), "Exported HTML should contain <klaat-kodelab> custom element")
    }
}
