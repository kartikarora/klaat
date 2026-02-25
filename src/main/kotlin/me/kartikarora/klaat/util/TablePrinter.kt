package me.kartikarora.klaat.util

import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import me.kartikarora.klaat.BuildConfig

object TablePrinter {
    private val terminal = Terminal()

    fun printInfoTable() {
        val t = table {
            header {
                row(bold(brightBlue("Klaat - Kodelabs As A Thing")))
            }
            body {
                row("Version: ${cyan(BuildConfig.VERSION_NAME)}")
                row("Binary: ${blue(BuildConfig.BINARY_NAME)}")
                row("Variant: ${blue(BuildConfig.BUILD_VARIANT)}")
            }
        }
        terminal.println(t)
    }
}
