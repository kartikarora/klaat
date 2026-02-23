package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand

class VersionCommand : CliktCommand("version") {
    override fun run() {
        echo("klaat version 1.0-SNAPSHOT")
    }
}