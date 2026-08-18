<div align="center">
  <img src="docs/images/klaat.png" alt="Klaat Logo" width="200">

# Klaat - Kodelabs As A Thing

**Klaat** (Kodelabs As A Thing) is a Kotlin-based CLI tool for authoring and serving interactive codelabs. This is a modern reimplementation of the archived [Google Codelabs Tools](https://github.com/googlecodelabs/tools) project.

</div>

## Features

✅ **Export codelabs** from Google Docs or Markdown files  
✅ **Multiple output formats** - HTML and Markdown  
✅ **Local preview server** with auto-browser opening  
✅ **Update existing codelabs** from their sources  
✅ **Google Analytics integration**  
✅ **Asset prefix support** for CDN hosting (Default: `https://distribute.kartikarora.me`)  
✅ **Rich formatting** - code blocks, info boxes, images, videos, surveys, and more

## Installation

> **Platform Support**: Klaat supports macOS (Apple Silicon only), Linux ARM64, and Linux x64. Intel Macs are **not supported**.

### Homebrew (Recommended for macOS & Linux)

```bash
brew tap kartikarora/klaat
brew install klaat
```

Get snapshot builds (latest from develop):

```bash
brew tap kartikarora/klaat-snapshots
brew install klaat-snapshot
```

### Download Binary

> **Alpha Status**: Klaat is currently in alpha. Stable binaries are not yet provided. Please use snapshot builds for testing.

Download the latest snapshot from the [CDN](https://distribute.kartikarora.me/):

```bash
# macOS ARM64 (Apple Silicon M1/M2/M3/M4)
curl -L https://distribute.kartikarora.me/bin/klaat/snapshot/COMMIT_HASH/klaat-snapshot-macos-arm64-COMMIT_HASH -o klaat

# Linux ARM64
curl -L https://distribute.kartikarora.me/bin/klaat/snapshot/COMMIT_HASH/klaat-snapshot-linux-arm64-COMMIT_HASH -o klaat

# Linux x64
curl -L https://distribute.kartikarora.me/bin/klaat/snapshot/COMMIT_HASH/klaat-snapshot-linux-x64-COMMIT_HASH -o klaat

# Make it executable
chmod +x klaat

# Move to PATH
sudo mv klaat /usr/local/bin/

# Verify installation
klaat version
```

### Build from Source

```bash
git clone https://github.com/kartikarora/klaat.git
cd klaat
./gradlew build
./gradlew installDist
```

The executable will be in `build/install/klaat/bin/klaat`

### Add to PATH (Optional)

```bash
# On macOS/Linux
export PATH=$PATH:$(pwd)/build/install/klaat/bin

# Or create an alias
alias klaat='$(pwd)/build/install/klaat/bin/klaat'
```

## Quick Start

### 1. Export from Markdown

Create a markdown file `tutorial.md`:

```markdown
---
id: my-first-codelab
summary: Learn how to create codelabs
categories: Tutorial
environments: Web
status: Published
authors: Your Name
---

# My First Codelab

## Introduction
Duration: 5

Welcome to your first codelab!

## Step 2
Duration: 10

More content here...
```

Export it:

```bash
klaat-snapshot export tutorial.md -o codelabs/
```

### 2. Export from Google Docs

```bash
klaat-snapshot export 1ABC-YOUR-DOC-ID -o codelabs/
```

### 3. Serve Locally

```bash
klaat-snapshot serve -d codelabs/
```

Opens `http://localhost:9090` in your browser automatically.

## Commands

### `help`

Show usage instructions and option summaries for klaat or any subcommand.

```bash
klaat-snapshot --help
klaat-snapshot export --help
klaat-snapshot serve -h
```

### `version`

Print the version and build metadata.

```bash
klaat-snapshot version
```

### `install-skills`

Install Klaat AI codelab authoring and formatting guidelines directly into your coding agent's project or global workspace. KLAAT scans the workspace and only writes the skills to agent folders that exist (supporting over 55+ distinct coding agents, including Claude Code, Gemini CLI, Cursor, Copilot, Cline, Zed, Windsurf, etc.).

```bash
klaat-snapshot install-skills [OPTIONS]

Options:
  -g, --global      Install to the global user home directories (~/) instead of the project root
  -h, --help        Show help

Examples:
  klaat-snapshot install-skills           # Installs locally to detected agent directories (e.g. .agents/, .claude/, .continue/)
  klaat-snapshot install-skills -g        # Installs globally to active user agent directories (e.g. ~/.agents/, ~/.claude/)
```

### `export`

Export codelabs from Google Docs or Markdown files.

```bash
klaat-snapshot export [OPTIONS] SOURCES...

Options:
  -f, --format TEXT       Output format (html|md|offline) [default: html]
  -o, --out TEXT          Output directory [default: .]
  -ga TEXT                Google Analytics tracking ID (optional)
  -p, --prefix TEXT       URL prefix for assets [default: https://distribute.kartikarora.me]
  -css TEXT               Custom CSS stylesheet file or URL to inject
  -favicon TEXT           Custom favicon URL or file path to use
  -fonts TEXT             Google Fonts link URL to load additional font families
  -footer TEXT            Custom footer HTML or text to display at the bottom of the codelab
  -extra TEXT             JSON string representing additional metadata to inject
  -pass_metadata TEXT     Comma-delimited list of metadata keys to preserve in output
  -auth TEXT              OAuth2 token to use for Google Docs API authentication
  -e TEXT                 Codelab environment filter [default: web]
  -h, --help              Show help

Examples:
  # Export Markdown file to HTML
  klaat-snapshot export tutorial.md

  # Export Google Doc with custom output directory
  klaat-snapshot export 1ABC-YOUR-DOC-ID -o ./output

  # Export to offline bundle format
  klaat-snapshot export -f offline tutorial.md
```

### `serve`

Start a local web server to preview codelabs.

```bash
klaat-snapshot serve [OPTIONS]

Options:
  -p, --port INT        Port to listen on [default: 9090]
  -h, --host TEXT       Host to bind to [default: localhost]
  -d, --dir TEXT        Directory to serve [default: .]
  --no-browser          Don't open browser automatically
  -h, --help            Show help

Examples:
  klaat-snapshot serve
  klaat-snapshot serve -p 8080 -d ./codelabs
  klaat-snapshot serve --no-browser
```

### `update`

Update existing codelabs from their original sources.

```bash
klaat-snapshot update [OPTIONS] [PATHS...]

Options:
  -ga TEXT              Override Google Analytics tracking ID
  -p, --prefix TEXT     Override URL prefix for assets
  -h, --help            Show help

Examples:
  klaat-snapshot update                    # Update all in current directory
  klaat-snapshot update ./codelabs         # Update specific directory
  klaat-snapshot update codelab1 codelab2  # Update specific codelabs
```

### `demo`

Extract the bundled showcase demo codelab into the current working directory, export it to HTML, and immediately launch the live preview server.

```bash
klaat-snapshot demo [OPTIONS]

Options:
  -addr TEXT              hostname:port to bind for preview server [default: localhost:9090]
  -o, --out TEXT          Output directory for exported demo [default: output]
  -f, --format TEXT       Output format (html|md|offline) [default: html]
  -prefix TEXT            Asset prefix URL [default: https://distribute.kartikarora.me]
  --no-browser            Don't open browser automatically
  --no-serve              Only extract and export, do not start preview server
  -h, --help              Show help

Examples:
  klaat-snapshot demo
  klaat-snapshot demo --no-serve
  klaat-snapshot demo -addr localhost:8080
```

## Codelab Format

### Markdown Format

Codelabs use standard Markdown with YAML frontmatter:

```markdown
---
id: unique-codelab-id
summary: Brief description
categories: Category1, Category2
environments: Web, Kiosk
status: Published
feedback_link: https://example.com/feedback
analytics_account: UA-12345-1
tags: kotlin, android
authors: John Doe, Jane Smith
---

# Codelab Title

## Step 1: Introduction
Duration: 5

Content for step 1...

### Code Example

```kotlin
fun hello() {
    println("Hello, World!")
}
```

## Step 2: Next Steps
Duration: 10

More content...
```

### Google Docs Format

See the [original format guide](https://github.com/googlecodelabs/tools/blob/master/FORMAT-GUIDE.md) for detailed instructions on formatting Google Docs.

Key elements:
- **Heading 1** - Codelab steps
- **Heading 2-4** - Section headers within steps
- **Two-column table** (at start) - Metadata
- **Single-cell table (green)** - Positive info boxes / Tips
- **Single-cell table (orange/red)** - Negative info boxes / Warnings
- **Single-cell table (black/light gray)** - Regular info boxes / Asides
- **Single-cell table (Courier New)** - Code blocks
- Duration annotations in grey text

## Features

### Supported Content Types

- ✅ Text formatting (bold, italic, inline code)
- ✅ Headers (H2, H3, H4)
- ✅ Paragraphs
- ✅ Code blocks with syntax highlighting, filenames, and one-click copy button
- ✅ Command/terminal blocks
- ✅ Info boxes (special/blue, positive/green, warning/amber, critical/red, default/neutral)
- ✅ Bullet lists and numbered lists
- ✅ Images with captions and width controls
- ✅ YouTube videos (responsive embed)
- ✅ IFrames
- ✅ External and relative links
- ✅ Action and download buttons
- ✅ Interactive surveys
- ✅ FAQ sections

### Metadata Fields

- `id` - Unique identifier
- `summary` - Brief description
- `category` - Comma-separated categories
- `environment` - Target environments (Web, Kiosk, etc.)
- `status` - Published, Draft, Deprecated, Hidden
- `feedback_link` - URL for bug reports
- `analytics_account` - Google Analytics ID
- `tags` - Comma-separated tags
- `authors` - Comma-separated author names
- `duration` - Total duration in minutes
- `favicon` - Custom favicon URL or icon path
- `footer` - Custom footer HTML or text

## Google Docs Setup

### 1. Configure OAuth Credentials

Create a `gcp_credentials.json` file in the project root or in `src/main/resources/credentials/`:

```bash
# Copy the template to the root
cp gcp_credentials.json.template gcp_credentials.json

# OR copy to resources
mkdir -p src/main/resources/credentials
cp gcp_credentials.json.template src/main/resources/credentials/gcp_credentials.json
```

Then add your Google OAuth credentials:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or use existing)
3. Enable **Google Docs API**:
   - APIs & Services → Library → Search "Google Docs API" → Enable
4. Create **OAuth 2.0 Client ID**:
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: **Desktop app**
   - Name it "Klaat"
5. Download the credentials JSON or copy the **Client ID** and **Client Secret** to `gcp_credentials.json` (root) or `src/main/resources/credentials/gcp_credentials.json`:

```json
{
  "installed": {
    "client_id": "YOUR_CLIENT_ID.apps.googleusercontent.com",
    "client_secret": "YOUR_CLIENT_SECRET",
    "redirect_uris": ["http://localhost"],
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token"
  }
}
```

**Note:** The `gcp_credentials.json` file is git-ignored for security and should be placed in the project root or `src/main/resources/credentials/`.

### 2. Build the Project

```bash
./gradlew build
./gradlew installDist
```

### 3. Authenticate

On first use with a Google Doc ID, Klaat will:

1. Open your browser for Google authentication
2. Ask you to grant read-only access to Google Docs
3. Save the access token to `~/.klaat/tokens/`

**First time usage:**

```bash
klaat export YOUR_DOC_ID -o output/

# Browser opens automatically
# → Sign in with Google
# → Grant "Read Google Docs" permission
# → Token is saved for future use
```

**Subsequent usage:**

The saved token is reused automatically. No browser authentication needed unless token expires.

**Token location:** `~/.klaat/tokens/StoredCredential`

## Project Structure

```
klaat/
├── src/main/kotlin/me/kartikarora/klaat/
│   ├── commands/          # CLI commands
│   │   ├── ExportCommand.kt
│   │   ├── ServeCommand.kt
│   │   ├── UpdateCommand.kt
│   │   └── VersionCommand.kt
│   ├── core/              # Core functionality
│   │   ├── Codelab.kt     # Data models
│   │   ├── DocParser.kt   # Google Docs parser
│   │   ├── Exporter.kt    # Main exporter
│   │   └── GoogleDocsClient.kt
│   ├── parser/            # Parsers
│   │   └── MarkdownParser.kt
│   ├── renderer/          # Renderers
│   │   ├── HtmlRenderer.kt
│   │   └── MarkdownRenderer.kt
│   └── Main.kt
├── build.gradle.kts
└── README.md
```

## Development

### Requirements

- JDK 21 or higher
- Gradle 8.0 or higher

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew run --args="export tutorial.md"
```

### Test

```bash
./gradlew test
```

### CI/CD and Releases

#### GitHub Actions

The project uses GitHub Actions for CI/CD:

- **Snapshot Release Workflow** (`.github/workflows/native-snapshot-release.yml`): Triggers manually (`workflow_dispatch`)
  - Builds development snapshot binaries
  - Publishes them to CDN and updates the Homebrew snapshot tap

- **Stable Release Workflow** (`.github/workflows/native-stable-release.yml`): Triggers on push to `main`
  - Builds stable release binaries
  - Publishes to R2 CDN
  - Auto-submits PR to Homebrew Core and creates a GitHub Release

#### Creating a Release

1. **Set GitHub Secret** (one-time setup):

   Create a secret named `GOOGLE_CREDENTIALS_JSON` containing your entire `credentials.json` file content:

   ```json
   {
     "installed": {
       "client_id": "YOUR_CLIENT_ID.apps.googleusercontent.com",
       "client_secret": "YOUR_CLIENT_SECRET",
       "redirect_uris": ["http://localhost"],
       "auth_uri": "https://accounts.google.com/o/oauth2/auth",
       "token_uri": "https://oauth2.googleapis.com/token"
     }
   }
   ```

2. **Trigger Release**:
   Commit the version change (e.g. to `1.0.0`) on `develop`, then raise a PR and merge `develop` into the `main` branch.

3. **Automated steps**:
   - GitHub Actions builds native binaries for macOS and Linux using GraalVM.
   - Publishes binaries to Cloudflare R2 CDN.
   - Automatically submits a PR to Homebrew Core to update the `klaat` formula.
   - Creates a GitHub Release with compiled binaries.

#### Credentials Configuration

**Local development**: Create `gcp_credentials.json` in the project root or `src/main/resources/credentials/`

**CI/CD**: Set `GOOGLE_CREDENTIALS_JSON` GitHub secret - workflow creates `src/main/resources/credentials/gcp_credentials.json`

## Differences from Original claat

Klaat is designed as a **spiritual successor** to the original claat tool with broad command-line compatibility.

### ✅ Full Compatibility

All claat commands and flags work identically in klaat:

- ✅ **Export command** - All flags supported: `-f`, `-o`, `-ga`, `-prefix`, `-e`, `-auth`, `-extra`, `-pass_metadata`
- ✅ **Serve command** - Full compatibility with `-addr` flag
- ✅ **Update command** - All flags supported
- ✅ **Version command** - Identical behavior
- ✅ **Help command** - Available as both `klaat help` and `klaat -h`
- ✅ **Output formats** - HTML, Markdown (md), and Offline formats supported
- ✅ **Input sources** - Google Docs (by ID) and Markdown files
- ✅ **Google Analytics integration** - Full support
- ✅ **Environment targeting** - `-e` flag for web, kiosk, etc.
- ✅ **Extra metadata** - `-extra` flag for JSON key-value pairs
- ✅ **Pass metadata** - `-pass_metadata` flag for custom fields

### Notable Differences

**Implementation:**
- Written in Kotlin instead of Go
- Modern Kotlin/JVM stack with Gradle build system
- Uses Ktor for web serving
- OAuth 2.0 integration for Google Docs API

**Enhanced Features:**
- Auto-browser opening for serve command (can be disabled with `--no-browser`)
- Better error messages and colored output
- Modern CLI framework (Clikt) with improved help text
- `-d/--dir` flag in serve command for directory selection

**Configuration Differences:**
- **Asset CDN**: Default prefix is `https://distribute.kartikarora.me` (claat uses `https://storage.googleapis.com`)
  - Use `-prefix` flag to override
- **Authentication**: Uses OAuth 2.0 with credentials in `gcp_credentials.json` or `src/main/resources/credentials/gcp_credentials.json` (claat uses `-auth` bearer token)
  - Tokens stored in `~/.klaat/tokens/`
  - First run opens browser for Google authentication

**Limitations:**
- **Custom template files**: Unlike claat (which accepts Go template file paths via `-f /path/to/template.tmpl`), klaat only supports the three built-in formats: `html`, `md`, and `offline`. Custom templates are not supported.

All standard claat workflows using built-in formats are fully supported. If you have scripts or documentation using claat commands with `html`, `md`, or `offline` formats, you can replace `claat` with `klaat` without modifications.

## License

Apache License 2.0 - See LICENSE file for details.

## Credits

This project is inspired by and based on the archived [Google Codelabs Tools](https://github.com/googlecodelabs/tools) project.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please use the [GitHub Issues](https://github.com/yourusername/klaat/issues) page.
