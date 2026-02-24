package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand

class VersionCommand : CliktCommand("version") {
    override fun run() {
        echo("klaat version ${me.kartikarora.klaat.BuildConfig.VERSION}")
    }
}