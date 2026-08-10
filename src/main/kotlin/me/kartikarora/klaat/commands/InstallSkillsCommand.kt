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

        var installedCount = 0
        for (targetDir in detectedDirs) {
            try {
                // 1. Markdown Author Skill
                val mdAuthorDir = File(targetDir, "klaat-markdown-author")
                val mdReferencesDir = File(mdAuthorDir, "references")
                mdReferencesDir.mkdirs()

                File(mdAuthorDir, "SKILL.md").writeText(MARKDOWN_AUTHOR_SKILL)
                File(mdReferencesDir, "template.md").writeText(MARKDOWN_AUTHOR_TEMPLATE)
                File(mdReferencesDir, "format-guide.md").writeText(FORMAT_GUIDE_CONTENT)

                // 2. Google Docs Author Skill
                val gdocsAuthorDir = File(targetDir, "klaat-gdocs-author")
                val gdocsReferencesDir = File(gdocsAuthorDir, "references")
                gdocsReferencesDir.mkdirs()

                File(gdocsAuthorDir, "SKILL.md").writeText(GDOCS_AUTHOR_SKILL)
                File(gdocsReferencesDir, "gdoc-mapping-reference.md").writeText(GDOCS_AUTHOR_MAPPING)
                File(gdocsReferencesDir, "format-guide.md").writeText(FORMAT_GUIDE_CONTENT)

                // 3. Klaat CLI Skill
                val cliDir = File(targetDir, "klaat-cli")
                val cliReferencesDir = File(cliDir, "references")
                cliReferencesDir.mkdirs()

                File(cliDir, "SKILL.md").writeText(CLI_SKILL)
                File(cliReferencesDir, "export-command.md").writeText(CLI_EXPORT_REF)
                File(cliReferencesDir, "serve-command.md").writeText(CLI_SERVE_REF)
                File(cliReferencesDir, "update-command.md").writeText(CLI_UPDATE_REF)
                File(cliReferencesDir, "version-command.md").writeText(CLI_VERSION_REF)
                File(cliReferencesDir, "help-command.md").writeText(CLI_HELP_REF)
                File(cliReferencesDir, "install-skills-command.md").writeText(CLI_INSTALL_SKILLS_REF)


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

    companion object {
        private val MARKDOWN_AUTHOR_SKILL = """
            ---
            name: klaat-markdown-author
            description: Instructs the AI on how to author valid Klaat-compatible Markdown documents following the official format guide. Covers YAML frontmatter validation, steps hierarchy, step durations, and rich element syntax (info boxes, surveys, code filenames).
            ---

            # Klaat Markdown Authoring Guide

            Use this skill when the user requests to create, format, or review a Markdown document for export with Klaat.

            ## Mandatory Structure

            All Klaat Markdown files must adhere to the following layout:

            ```markdown
            ---
            id: unique-url-slug
            summary: Short single-sentence description of the tutorial.
            categories: Category1, Category2
            environments: Web
            status: Published
            authors: Your Name
            feedback_link: https://github.com/your/repo/issues
            ---

            # Codelab Title

            ## Step 1 Title
            Duration: 5

            Step content...

            ## Step 2 Title
            Duration: 10

            Step content...
            ```

            ## Frontmatter Schema

            Verify that the YAML block matches these validation rules:

            | Field Name | Type | Requirement | Fallback / Default | Description |
            | :--- | :--- | :--- | :--- | :--- |
            | `id` | String | Optional | Sluggified `summary` | URL-safe name for the output folder (lowercase, hyphens, alphanumeric). |
            | `summary` | String | **Required** | None | Short description used for cards. Falls back to `untitled-codelab` if completely missing. |
            | `categories`| List | Optional | Empty list | Comma-separated categories or YAML block list. |
            | `environments`| List | Optional | `["Web"]` | Comma-separated target environments. |
            | `status` | List | Optional | `["Published"]` | Release state: `Published`, `Draft`, `Deprecated`. |
            | `authors` | List | Optional | Empty list | Comma-separated author name(s). |
            | `feedback_link`| String| Optional | Empty string | URL for reporting bugs/issues. |
            | `analytics_account`| String| Optional| Empty string | Google Analytics property ID. |
            | `tags` | List | Optional | Empty list | Comma-separated keyword tags. |
            | `theme` | String | Optional | Empty string | Styling theme class hook. |
            | `duration` | Int | Optional | Sum of steps | Override for total tutorial duration in minutes. |
            | `image` | String | Optional | First page image | Cover image path/URL. |

            ## Content Element Syntax

            Ensure the correct Markdown markup is used for rich elements:

            ### 1. Steps & Durations
            *   Each step must start with a **Heading 2** (`## Step Title`).
            *   Directly beneath the step title, specify the step duration in minutes: `Duration: X` (e.g., `Duration: 5`). Do not add extra text on this line.

            ### 2. Info Boxes (Alerts)
            *   **Special (Tips/Info):** Include `{.special}` in the blockquote:
                ```markdown
                > This is a tip! {.special}
                ```
            *   **Warning (Alerts/Caution):** Include `{.warning}` in the blockquote:
                ```markdown
                > Critical alerts or safety warnings go here. {.warning}
                ```

            ### 3. Code Blocks
            *   Always specify the programming language (e.g., ```kotlin, ```bash).
            *   **Code block with filename:** Add bold file header immediately above the code block:
                ```markdown
                **Main.kt**
                ```kotlin
                fun main() = println("Hello!")
                ```
                ```

            ### 4. Images & Captions
            *   Load images with standard markdown syntax.
            *   **Captions:** Add a caption by placing italic text immediately below the image:
                ```markdown
                ![Alternative text](https://placehold.co/600x400/png)
                *Figure 1: Visual mockup design layout.*
                ```

            ### 5. Interactive Surveys
            Use checklist bullet blocks immediately following a bold question:
            ```markdown
            **How would you rate your Kotlin experience?**
            - [ ] Beginner
            - [ ] Intermediate
            - [ ] Expert
            ```

            ### 6. Frequently Asked Questions (FAQs)
            Format Q&A sections using bold `Q:` and plain `A:` pairs:
            ```markdown
            **Q: What is the default port?**

            A: The default port is 9090.
            ```

            ### 7. Action / Download Buttons
            Write a single link alone on its own line:
            ```markdown
            [Download Starter Code](https://example.com/starter.zip)
            ```
        """.trimIndent()

        private val MARKDOWN_AUTHOR_TEMPLATE = """
            ---
            id: sample-klaat-tutorial
            summary: A sample tutorial demonstrating the correct frontmatter and structure.
            categories: Kotlin, Tutorial
            environments: Web, Kiosk
            status: Published
            authors: Klaat Author
            feedback_link: https://github.com/yourusername/klaat/issues
            tags: getting-started, guide, kotlin
            ---

            # Sample Klaat Tutorial Title

            ## Step 1: Configuration
            Duration: 5

            Welcome to the tutorial! Here is a tip to help you:

            > **Note:** Always keep your step durations reasonable (between 2 and 15 minutes is recommended).

            ### Environment Setup

            Add the following dependency to your build file:

            ```kotlin
            implementation("me.kartikarora.klaat:core:1.2.0")
            ```

            ## Step 2: Running the Command
            Duration: 3

            Now we are ready to run the export:

            ```bash
            klaat export hello.md -o output/
            ```

            **Verify the files:**
            - [ ] Export directory `output` exists.
            - [ ] The generated HTML file loads.
            - [ ] Styles and assets load from CDN.

            **Q: Can I customize the output location?**

            A: Yes! Use the `-o` or `--out` flag to specify any path.
        """.trimIndent()

        private val GDOCS_AUTHOR_SKILL = """
            ---
            name: klaat-gdocs-author
            description: Instructs the AI on how to help users author valid Google Docs tutorials compatible with Klaat. Explains how to structure metadata tables, steps, durations, code tables, and warning cells. Guides the AI on handling Google OAuth sign-in prompts.
            ---

            # Klaat Google Docs Authoring Guide

            Use this skill when a user wants to structure or prepare a Google Document to export as a codelab using Klaat.

            ## ⚠️ Important: Drive & Sign-In Requirements

            Since the AI does not have direct write access to the user's private Google Drive account, you cannot create the Google Document for them. Instead, you must instruct the user on how to set it up:

            1.  **Creation**: The user must create a new document in their own Google Drive account.
            2.  **Authentication**: Explain to the user that when they run `klaat export [DOC_ID]` for the first time, Klaat will open their default browser to authenticate read-access to Google Docs. The user must complete this OAuth sign-in.
            3.  **Local Storage**: Klaat will save the login credentials securely on the user's system at `~/.klaat/tokens/StoredCredential` for subsequent requests.

            ## Google Doc Layout Schema

            Instruct the user to structure their Google Document with these styles:

            ### 1. Metadata Block (Document Start)
            The document must begin with a **two-column table** listing the configuration properties:

            | Left Column (Keys) | Right Column (Values) |
            | :--- | :--- |
            | `id` | `my-first-gdoc-codelab` |
            | `summary` | `A brief description of this tutorial.` |
            | `categories` | `Android, Kotlin` |
            | `status` | `Published` |
            | `authors` | `Your Name` |
            | `feedback_link` | `https://github.com/org/repo/issues` |

            ### 2. Title & Steps
            *   **Codelab Title**: Use the standard **Title** paragraph style or a large Header.
            *   **Step Headers**: Mark the beginning of each step using **Heading 1** (e.g., `1. Introduction`).
            *   **Step Duration**: Write `Duration: X` (where X is in minutes) as plain text directly below the Heading 1 step header.

            ### 3. Visual Element Formatting Maps
            To output code blocks and alerts correctly, the user must apply these table and color mappings:

            *   **Positive Info Box (Tips)**:
                *   Create a **single-cell table**.
                *   Set the cell background color to **light green** (hex `#E8F5E9` or similar).
                *   Start text with **Note:** or **Pro Tip:** in bold.
            *   **Negative Info Box (Warnings)**:
                *   Create a **single-cell table**.
                *   Set the cell background color to **light red / orange** (hex `#FFEBEE` or `#FFF3E0` or similar).
                *   Start text with **Warning:** or **Caution:** in bold.
            *   **Code Blocks**:
                *   Create a **single-cell table**.
                *   Set the font of all code contents to **Courier New**.
                *   For terminal commands, set the cell background to **black** and text to **light grey / white** in Courier New.

            ## Exporting the Codelab

            Guide the user to perform the following steps to export:

            1.  Extract the **Document ID** from the Google Docs URL in their browser:
                `https://docs.google.com/document/d/YOUR_DOCUMENT_ID_HERE/edit`
            2.  Run the export command in their terminal:
                ```bash
                klaat export YOUR_DOCUMENT_ID_HERE -o output/
                ```
            3.  Authenticate on the browser window that opens.
        """.trimIndent()

        private val GDOCS_AUTHOR_MAPPING = """
            # Google Docs Visual Formatting Mapping Reference

            When structuring a Google Doc for Klaat, apply the following visual layouts to map elements into codelab nodes correctly:

            ---

            ## 1. Metadata Block
            A **two-column table** placed at the very top of the document. Do not put any text before this table.

            | Key (Left Column) | Value (Right Column) |
            | :--- | :--- |
            | `id` | `gdoc-tutorial-sample` |
            | `summary` | `A sample showing Google Doc mapping formats.` |
            | `categories` | `Android, Kotlin` |
            | `status` | `Published` |
            | `authors` | `Klaat Exporter` |

            ---

            ## 2. Steps & Durations
            *   **Step Title**: Formatted as a **Heading 1** paragraph.
            *   **Step Duration**: Formatted as plain text immediately below the Heading 1 step header. Format: `Duration: X` (e.g., `Duration: 5`).

            ---

            ## 3. Info Boxes (Alerts)
            Info boxes must be created using **single-cell tables** with specific background shading:

            | Element | Background Shading | Prefix Content |
            | :--- | :--- | :--- |
            | **Positive Box (Info/Tip)** | Light Green (hex `#E8F5E9` or similar) | Start with **Note:** or **Pro Tip:** |
            | **Negative Box (Warning)** | Light Red (hex `#FFEBEE` or similar) | Start with **Warning:** or **Caution:** |
            | **Regular Box (Aside)** | Black/Light Gray (hex `#F5F5F5` or similar) | Any other blockquote or single-cell table |

            ---

            ## 4. Code & Commands
            Code blocks must be created using **single-cell tables** using the `Courier New` font:

            *   **Standard Code Block**:
                *   Single-cell table.
                *   Font set to `Courier New`.
                *   No custom background color.
            *   **Terminal Command Block**:
                *   Single-cell table.
                *   Cell background set to **Black**.
                *   Font set to `Courier New` (White or Light Grey text color).
        """.trimIndent()

        private val FORMAT_GUIDE_CONTENT = """
            # Klaat Codelab Formatting Guide

            This guide documents the official syntax, structure, and formatting requirements for generating codelabs with Klaat.

            ---

            ## 1. Metadata Frontmatter (YAML Block)
            Every codelab begins with a YAML block that defines the tutorial's configuration.

            ```yaml
            ---
            id: sample-codelab-id
            summary: A brief description of what this tutorial covers.
            categories: Kotlin, Setup
            environments: Web
            status: Published
            authors: Jane Doe
            feedback_link: https://github.com/username/repo/issues
            ---
            ```

            ### Metadata Fields Reference

            | YAML Field | Requirement | Data Type | Default / Fallback | Description |
            | :--- | :--- | :--- | :--- | :--- |
            | `id` | Optional | String | Sluggified `summary` | URL-safe name for the folder. Alphanumeric and dashes only. |
            | `summary` | **Required** | String | None | Short description used for lists and preview cards. |
            | `categories` | Optional | List | Empty list | Categories for directory catalog listing and filtering. |
            | `environments`| Optional | List | `["Web"]` | Environments/platforms where the codelab runs. |
            | `status` | Optional | List | `["Published"]` | Release state (e.g. `Published`, `Draft`, `Deprecated`). |
            | `authors` | Optional | List | Empty list | Codelab author name(s). |
            | `feedback_link`| Optional | String | Empty string | URL for feedback/bug reports (renders as footer link). |
            | `analytics_account`| Optional| String | Empty string | Google Analytics property ID. |
            | `theme` | Optional | String | Empty string | CSS hook class for styling customization. |
            | `duration` | Optional | Integer | Sum of step times| Manual override for total tutorial duration in minutes. |
            | `image` | Optional | String | First content image| Cover image URL for card listing catalogs. |

            *Note: Custom frontmatter variables are stripped unless the `-pass_metadata` comma-separated flag is specified during export.*

            ---

            ## 2. Document Hierarchy & Steps

            Klaat splits your tutorial into separate pages (called **Steps**) based on Heading structures.

            ### Markdown Structure
            *   **Codelab Title**: Set as a single **Heading 1** (`# Title`).
            *   **Step Headers**: Mark each step with a **Heading 2** (`## Step Title`).
            *   **Step Duration**: Specify step length in minutes using `Duration: X` directly on the line immediately below the H2.

            *Example:*
            ```markdown
            # Building a Kotlin App

            ## Step 1: Getting Started
            Duration: 5

            Step content goes here...
            ```

            ### Google Docs Structure
            *   **Codelab Title**: Styled using the Google Docs **Title** style.
            *   **Step Headers**: Styled using the Google Docs **Heading 1** style.
            *   **Step Duration**: Write `Duration: X` in plain text directly under the Heading 1 step header.

            ---

            ## 3. Inline & Block Formatting Elements

            Use these syntax conventions to format various rich widgets:

            ### Info Boxes (Alerts)
            Info boxes must be created using blockquotes with a specific prefix. In Google Docs, create a single-cell table with a background color:

            *   **Special Box (Tips/Info)**:
                *   *Markdown*: Blockquote containing the `{.special}` metadata tag.
                *   *Google Docs*: Single-cell table with a **light green** background.
                *   *Example*:
                    ```markdown
                    > This is a tip/recommendation. {.special}
                    ```
            *   **Warning Box (Warnings/Alerts)**:
                *   *Markdown*: Blockquote containing the `{.warning}` metadata tag.
                *   *Google Docs*: Single-cell table with a **light red/orange** background.
                *   *Example*:
                    ```markdown
                    > This is a warning or critical notice. {.warning}
                    ```
            *   **Regular Box (Aside)**:
                *   *Markdown*: Any other blockquote without the specific metadata tags.
                *   *Google Docs*: Single-cell table with a **black/light gray** background.
                *   *Example*:
                    ```markdown
                    > This is just a regular aside or quote.
                    ```

            ### Code & Commands
            *   **Inline Code**: Surround with single backticks: `val name = "Klaat"`.
            *   **Multi-line Code Blocks**: Use triple backticks with syntax language:
                ```kotlin
                fun main() {
                    println("Hello World")
                }
                ```
            *   **Code Block with Filename**: Put file names in bold immediately above the code block:
                ```markdown
                **Main.kt**
                ```kotlin
                fun main() = println("Hello!")
                ```
                ```
            *   **Google Docs Code Block**: Place code in a single-cell table, set font to **Courier New**. For terminal commands, set the table background to **black** and font to white.

            ### Interactive Surveys & Checklists
            Render checkboxes by putting a list immediately below a bold question:
            ```markdown
            **Which editor are you using?**
            - [ ] IntelliJ IDEA
            - [ ] Android Studio
            - [ ] VS Code
            ```

            ### Frequently Asked Questions (FAQs)
            Format FAQ lists using bold `Q:` and plain `A:` tags:
            ```markdown
            **Q: Can I build native binaries with Klaat?**

            A: Yes! Use `./gradlew nativeCompile` to build a native binary using GraalVM.
            ```

            ### Action / Download Buttons
            Write a single link alone on its own line:
            ```markdown
            [Download Starter Project](https://example.com/starter-code.zip)
            ```

            ### Media & Embeds
            *   **Images**: Render images with standard syntax and an optional caption underneath:
                ```markdown
                ![Klaat Architecture Overview](images/architecture.png)
                ```
            *   **YouTube Videos**: Embed a YouTube video by putting its link alone on its own line:
                ```markdown
                https://www.youtube.com/watch?v=dQw4w9WgXcQ
                ```
        """.trimIndent()
 
        private val CLI_SKILL = """
---
name: klaat-cli
description: >
  A skill for using the `klaat` command-line tool. This skill teaches AI agents
  how to install, execute, and use the various commands provided by Klaat.
---

# Klaat CLI

This skill instructs AI agents on how to use the `klaat` binary. **This skill is for using Klaat, not for building it from source.**

## Installation
Before running `klaat`, verify if it is installed. If it is not installed, install it using Homebrew:

```bash
brew tap kartikarora/klaat
brew install klaat
```

If you specifically need the snapshot version:
```bash
brew tap kartikarora/klaat-snapshots
brew install klaat-snapshot
```

## Running the Binary
Once installed, simply run the binary directly from the terminal:
```bash
klaat <command> [flags]
```

## Formatting References
When authoring content to be used with `klaat`, always refer to the specific formatting skills to ensure proper structure and metadata:
- For Google Docs: Use the **klaat-gdocs-author** skill (`skills/klaat-gdocs-author/SKILL.md`).
- For Markdown: Use the **klaat-markdown-author** skill (`skills/klaat-markdown-author/SKILL.md`).

## Available Commands

Klaat is a drop-in replacement for the original `claat` tool and supports all standard commands.
For detailed instructions, flags, and examples for each command, please read the corresponding reference file:

- [Export Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/export-command.md): `skills/klaat-cli/references/export-command.md`
- [Serve Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/serve-command.md): `skills/klaat-cli/references/serve-command.md`
- [Update Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/update-command.md): `skills/klaat-cli/references/update-command.md`
- [Version Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/version-command.md): `skills/klaat-cli/references/version-command.md`
- [Help Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/help-command.md): `skills/klaat-cli/references/help-command.md`
- [Install Skills Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/install-skills-command.md): `skills/klaat-cli/references/install-skills-command.md`

        """.trimIndent()

        private val CLI_EXPORT_REF = """
# Klaat Export Command

The `export` command converts a source document (Google Docs ID or Markdown file path) into a Codelab format.

## Usage
`klaat export <targets> [flags]`

## Targets
Space-separated list of Google Doc IDs or Markdown file paths to export.

## Flags

- `-f` : Output format. Choices: `html`, `md`, `offline`. Default is `html`.
- `-o` : Output directory. Default is current directory (`.`).
- `-ga` : Global Google Analytics tracking ID. Default is `UA-49880327-14`.
- `-prefix` : URL prefix for assets. Default is `https://distribute.kartikarora.me`.
- `-e` : Environment to use for the codelab. Default is `web`.
- `-auth` : OAuth2 token to use for authentication (Google Docs).
- `-extra` : Additional metadata in JSON format (e.g. `{"key1":"value1"}`).
- `-pass_metadata` : Comma-delimited list of metadata fields to pass through to the output.
- `-css` : Custom CSS file or URL to use.
- `-favicon` : Custom favicon URL or path to use.
- `-fonts` : Google Fonts URL to load additional fonts (e.g. `https://fonts.googleapis.com/css2?family=Inter`).

## Examples
```bash
klaat export -f html -o out/ 1a2b3c4d5e6f7g8h9i0j
klaat export -f md my-codelab.md
```

        """.trimIndent()

        private val CLI_SERVE_REF = """
# Klaat Serve Command

The `serve` command starts a local web server to preview generated Codelabs locally.

## Usage
`klaat serve [flags]`

## Flags

- `-addr` : `hostname:port` to bind the server to. Default is `localhost:9090`.
- `-d` or `--dir` : Directory to serve the files from. Default is the current directory (`.`).
- `--no-browser` : Prevents the tool from automatically opening the browser to preview the codelabs.

## Examples
```bash
klaat serve -addr localhost:8080 -d out/
klaat serve --no-browser
```

        """.trimIndent()

        private val CLI_UPDATE_REF = """
# Klaat Update Command

The `update` command updates existing Codelabs within a directory. It will read the metadata from existing generated output and attempt to fetch the latest changes from the original source.

## Usage
`klaat update [flags]`

## Flags
The `update` command accepts the same flags as `export`, which apply to any Codelabs updated during the run.
- `-auth` : OAuth2 token to use for authentication (Google Docs).
- `-prefix` : URL prefix for assets. Default is `https://distribute.kartikarora.me`.
- `-ga` : Global Google Analytics tracking ID. Default is `UA-49880327-14`.

## Examples
```bash
klaat update
```

        """.trimIndent()

        private val CLI_VERSION_REF = """
# Klaat Version Command

The `version` command prints the current version of the Klaat CLI installed on the system.

## Usage
`klaat version`

## Output Example
```
klaat version 1.2.3
```

        """.trimIndent()

        private val CLI_HELP_REF = """
# Klaat Help Command

The `help` command displays usage instructions and lists all available commands for the Klaat CLI.

## Usage
`klaat help`
or
`klaat -h`
or
`klaat --help`

To get help for a specific command, you can append `--help` or `-h` to the command name:
`klaat export -h`

        """.trimIndent()

        private val CLI_INSTALL_SKILLS_REF = """
# Klaat Install-Skills Command

The `install-skills` command installs Klaat's AI formatting skills into your active agent workspaces (e.g. Antigravity `.agents` folder, or `skills` folder). This enables AI assistants to natively understand how to structure, format, and generate Klaat-compatible Codelabs.

## Usage
`klaat install-skills [flags]`

## Flags
- `-g`, `--global` : Install the skills to the global user directory instead of the local project directory.

## Examples
```bash
klaat install-skills
klaat install-skills --global
```

        """.trimIndent()
    }
}
