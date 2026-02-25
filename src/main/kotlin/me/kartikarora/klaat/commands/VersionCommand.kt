package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand
import me.kartikarora.klaat.BuildConfig

class VersionCommand : CliktCommand("version") {
    override fun run() {
        echo("${BuildConfig.BINARY_NAME} version ${BuildConfig.VERSION_NAME}")
    }
}