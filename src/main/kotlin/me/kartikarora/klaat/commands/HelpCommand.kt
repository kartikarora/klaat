package me.kartikarora.klaat.commands

import com.github.ajalt.clikt.core.CliktCommand

class HelpCommand : CliktCommand("help") {
    override fun run() {
        echo("""
klaat - Kodelabs As A Thing

A drop-in replacement for claat written in Kotlin.
Generate codelabs from Google Docs or Markdown files.

Usage: klaat <cmd> [options] src [src ...]

Available commands are: export, serve, update, version, help.

## Export command

Export takes one or more 'src' documents and converts them
to the format specified with -f option.

The following formats are supported:

- html (Polymer-based web components)
- md (Markdown)
- offline (plain HTML markup for offline consumption)

Note: Unlike the original claat, klaat only supports these three built-in
formats. Custom template files are not supported. The built-in FreeMarker
templates can be found in src/main/resources/templates/.

Each 'src' can be either a remote HTTP resource or a local file.
Source formats currently supported are:

- Google Doc (Codelab Format)
- Markdown

When 'src' is a Google Doc, it must be specified as a doc ID,
omitting https://docs.google.com/... part.

Instead of writing to an output directory, use "-o -" to specify
stdout. In this case images and metadata are not exported.
When writing to a directory, existing files will be overwritten.

The program exits with non-zero code if at least one src could not be exported.

## Serve command

Serve provides a simple web server for viewing exported codelabs.
It takes no arguments and presents the current directory contents.
Clicking on a directory representing an exported codelab will load
all the required dependencies and render the generated codelab as
it would appear in production.

The serve command takes a -addr host:port option, to specify the
desired hostname or IP address and port number to bind to.

## Update command

Update scans one or more 'src' local directories for codelab.json metadata
files, recursively. A directory containing the metadata file is expected
to be a codelab previously created with the export command.

Current directory is assumed if no 'src' argument is given.

Each found codelab is then re-exported using parameters from the metadata file.
Unused codelab assets will be deleted, as well as the entire codelab directory,
if codelab ID has changed since last update or export.

While -prefix and -ga can override existing codelab metadata, the other
arguments have no effect during update.

## Version command

Prints the version of klaat.

Use "klaat <command> --help" for more information about a command.
        """.trimIndent())
    }
}
