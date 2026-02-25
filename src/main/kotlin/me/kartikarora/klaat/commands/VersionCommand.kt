package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import me.kartikarora.klaat.BuildConfig
import me.kartikarora.klaat.util.TablePrinter

class VersionCommand : CliktCommand("version") {
    override fun run() {
        TablePrinter.printInfoTable()
    }
}