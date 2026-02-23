package me.kartikarora.klaat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import me.kartikarora.klaat.commands.ExportCommand
import me.kartikarora.klaat.commands.HelpCommand
import me.kartikarora.klaat.commands.ServeCommand
import me.kartikarora.klaat.commands.UpdateCommand
import me.kartikarora.klaat.commands.VersionCommand

class Klaat : CliktCommand("""
    klaat - Kodelabs As A Thing

    A drop-in replacement for claat written in Kotlin.
    Generate codelabs from Google Docs or Markdown files.
    """.trimIndent()
) {
    override fun run() = Unit
}

fun main(args: Array<String>) = Klaat()
    .subcommands(ExportCommand(), HelpCommand(), ServeCommand(), UpdateCommand(), VersionCommand())
    .main(args)