# Klaat Export Command

The `export` command converts a source document (Google Docs ID or Markdown file path) into a Codelab format.

## Usage
`klaat export <targets> [flags]`

## Targets
Space-separated list of Google Doc IDs or Markdown file paths to export.

## Flags

- `-f` : Output format. Choices: `html`, `md`, `offline`. Default is `html`.
- `-o` : Output directory. Default is current directory (`.`).
- `-ga` : Global Google Analytics tracking ID (optional).
- `-prefix` : URL prefix for assets. Default is `https://distribute.kartikarora.me`.
- `-e` : Environment to use for the codelab. Default is `web`.
- `-auth` : OAuth2 token to use for authentication (Google Docs).
- `-extra` : Additional metadata in JSON format (e.g. `{"key1":"value1"}`).
- `-pass_metadata` : Comma-delimited list of metadata fields to pass through to the output.
- `-css` : Custom CSS file or URL to use.
- `-favicon` : Custom favicon URL or path to use.
- `-fonts` : Google Fonts URL to load additional fonts (e.g. `https://fonts.googleapis.com/css2?family=Inter`).
- `-footer` : Custom footer HTML or text to display at the bottom of the codelab.

## Examples
```bash
klaat export -f html -o out/ 1a2b3c4d5e6f7g8h9i0j
klaat export -f md my-codelab.md
```
