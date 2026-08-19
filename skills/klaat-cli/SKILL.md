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

Klaat is a spiritual successor to the original `claat` tool and supports all standard commands.
For detailed instructions, flags, and examples for each command, please read the corresponding reference file:

- [Help Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/help-command.md): `skills/klaat-cli/references/help-command.md`
- [Version Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/version-command.md): `skills/klaat-cli/references/version-command.md`
- [Install Skills Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/install-skills-command.md): `skills/klaat-cli/references/install-skills-command.md`
- [Export Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/export-command.md): `skills/klaat-cli/references/export-command.md`
- [Serve Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/serve-command.md): `skills/klaat-cli/references/serve-command.md`
- [Update Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/update-command.md): `skills/klaat-cli/references/update-command.md`
- [Demo Command](file:///Users/kartikarora/Workspace/Personal/klaat/skills/klaat-cli/references/demo-command.md): `skills/klaat-cli/references/demo-command.md`
