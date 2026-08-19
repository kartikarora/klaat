---
id: klaat-feature-showcase
summary: A comprehensive, interactive tour showcasing all rich elements, typography, code blocks, infoboxes, surveys, media, and video embeds supported by Klaat.
authors: Kartik Arora
categories: demo, markdown, kotlin, tutorial
environments: web
status: published
feedback_link: https://github.com/kartikarora/klaat/issues
tags: demo, features, components, markdown, showcase, video, youtube
duration: 20
---

# Klaat Feature Showcase & Interactive Demo

## Overview & Metadata
Duration: 0:01:00

Welcome to the **Klaat Feature Showcase**! This tutorial serves as a living, interactive demonstration of all the markdown features, components, typography styles, and interactive elements supported by Klaat.

### What You'll Explore

In this codelab, each step demonstrates a distinct capability and provides the exact source Markdown used to generate it:

1. **Typography & Formatting**: Fluid headings, text weights, blockquotes, and inline styling.
2. **Semantic Infoboxes**: Alert callouts (Positive, Negative, Info, Warning).
3. **Code Highlighting & Copying**: Multi-language blocks, filenames, and copy-to-clipboard.
4. **Action Buttons**: Action pill buttons with hover states.
5. **Tables & Structured Data**: Clean tables with code tags and alignment.
6. **List Hierarchies**: Ordered, unordered, nested, and mixed task workflows.
7. **Interactive Surveys**: Embedded knowledge checks with custom radio selectors.
8. **Images & Visual Assets**: Centered, responsive image galleries.
9. **Video Embedding**: Responsive YouTube video embeds with 16:9 aspect ratios.
10. **Complete Source Markdown**: View and copy the full raw markdown document.

> **Tip**: Look at the drawer on the left. Notice how each step automatically tracks your reading progress and estimates time remaining dynamically! {.positive}

---

### Source Markdown for this Step

```markdown
---
id: klaat-feature-showcase
summary: A comprehensive, interactive tour showcasing all rich elements.
authors: Kartik Arora
categories: demo, markdown, tutorial
environments: web
status: published
feedback_link: https://github.com/kartikarora/klaat/issues
tags: demo, features, markdown
duration: 20
---

# Klaat Feature Showcase & Interactive Demo

## Overview & Metadata
Duration: 0:01:00

Welcome to the **Klaat Feature Showcase**!

> **Tip**: Look at the drawer on the left for dynamic progress tracking! {.positive}
```

---

## Typography & Inline Styling
Duration: 0:02:00

Klaat provides clean, modern typography for headings, paragraphs, emphasis, and quotes.

### Heading Hierarchy

Headings scale smoothly across screen sizes:

### Subsection Heading (H3)
Used for primary sub-topics and conceptual divisions.

#### Minor Heading (H4)
Used for granular steps, technical notes, or component properties.

---

### Text Formatting & Emphasis

You can emphasize text using standard Markdown constructs:

- **Bold Text**: Use strong emphasis for key terms or action targets.
- *Italic Text*: Use subtle emphasis for definitions and terminology.
- ***Bold & Italic***: Combine both for high-contrast callouts.
- ~~Strikethrough~~: Indicate deprecated flags or legacy syntax.
- `Inline Code`: Highlight commands like `klaat export`, variables, or file paths like `src/main/resources`.

---

### High-Impact Blockquotes

Blockquotes feature accent borders and subtle background tinting:

> "Klaat enables technical writers, software engineers, and educators to craft polished, interactive tutorials from Markdown or Google Docs with zero friction."

---

### Source Markdown for this Step

```markdown
## Typography & Inline Styling
Duration: 0:02:00

### Subsection Heading (H3)
#### Minor Heading (H4)

- **Bold Text**: `**text**`
- *Italic Text*: `*text*`
- ***Bold & Italic***: `***text***`
- ~~Strikethrough~~: `~~text~~`
- `Inline Code`: `` `val codelab = parse()` ``

> "Klaat enables technical writers and educators to craft polished tutorials."
```

---

## Semantic Infoboxes (Asides)
Duration: 0:02:00

Klaat supports semantic infoboxes for callouts, tips, cautions, and important notices.

### Positive / Tip / Success (Green)
Use for best practices, success confirmations, or helpful hints.

> **Best Practice**: Running `klaat serve` starts a local preview server with live reloading and auto-browser launching. {.positive}

---

### Special / Supplementary Info (Blue)
Use for helpful recommendations, deep dives, or extra context.

> **Information**: You can customize branding, Google Fonts, and external stylesheets seamlessly with the `-p`, `-fonts`, and `-css` flags. {.special}

---

### Warning / Caution (Amber / Orange)
Use to draw attention to common pitfalls, security notices, or important requirements.

> **Caution**: Make sure `gcp_credentials.json` is never committed to your public Git repository. {.warning}

---

### Critical / Danger (Red)
Use to alert users to breaking changes, dangerous commands, or critical errors.

> **Critical**: Ensure target export directories have appropriate write permissions before running automated build scripts. {.critical}

---

### Source Markdown for this Step

```markdown
## Semantic Infoboxes (Asides)
Duration: 0:02:00

> **Best Practice**: Running `klaat serve` starts a local preview server. {.positive}

> **Information**: Custom styles and fonts can be linked via flags. {.special}

> **Caution**: Make sure `gcp_credentials.json` is never committed. {.warning}

> **Critical**: Ensure write permissions on output directories. {.critical}
```

---

## Code Blocks & Syntax Highlighting
Duration: 0:02:00

Klaat provides syntax highlighting for programming languages, complete with optional filename headers and a built-in copy-to-clipboard button.

### Kotlin Implementation with Filename

```kotlin:CodelabExporter.kt
package me.kartikarora.klaat.core

import me.kartikarora.klaat.parser.MarkdownParser
import me.kartikarora.klaat.renderer.HtmlRenderer
import java.io.File

class CodelabExporter(private val targetDir: File) {
    fun export(source: File): File {
        val codelab = MarkdownParser().parse(source)
        val rendered = HtmlRenderer.render(codelab)
        val output = File(targetDir, "${codelab.id}/index.html")
        output.parentFile.mkdirs()
        output.writeText(rendered)
        return output
    }
}
```

---

### JSON Configuration with Filename

```json:wrangler.jsonc
{
  "name": "klaat-cdn",
  "compatibility_date": "2026-08-18",
  "r2_buckets": [
    {
      "binding": "DISTRIBUTE_BUCKET",
      "bucket_name": "distribute-cdn"
    }
  ]
}
```

---

### Shell / Bash Command

```bash
# Build native binary and test locally
./gradlew nativeCompile
./build/native/nativeCompile/klaat --help
```

---

### Source Markdown for this Step

````markdown
## Code Blocks & Syntax Highlighting
Duration: 0:02:00

```kotlin:CodelabExporter.kt
package me.kartikarora.klaat.core
class CodelabExporter(private val targetDir: File)
```

```json:wrangler.jsonc
{
  "name": "klaat-cdn"
}
```

```bash
./gradlew nativeCompile
```
````

---

## Action Buttons & Interactive Links
Duration: 0:01:30

Action buttons and call-to-actions are styled as rounded pill buttons with hover states. Add `{.buttonPrimary}` (or `{.button}`) for solid buttons, and `{.buttonSecondary}` for outlined buttons.

### Primary Action Button

Download the sample codelab starter project:

[Download Starter Kit](https://github.com/kartikarora/klaat/archive/refs/heads/develop.zip){.buttonPrimary}

---

### Secondary Action Button

View the source repository on GitHub:

[Browse GitHub Repository](https://github.com/kartikarora/klaat){.buttonSecondary}

---

### External Navigation Links

Links without button attributes remain clean inline navigation links:
- Visit the official [Klaat Documentation](https://klaat.kartikarora.me/docs/index.html).
- View the [Klaat GitHub Repository](https://github.com/kartikarora/klaat).

---

### Source Markdown for this Step

```markdown
## Action Buttons & Interactive Links
Duration: 0:01:30

# Primary solid button:
[Download Starter Kit](https://github.com/kartikarora/klaat/archive/refs/heads/develop.zip){.buttonPrimary}

# Secondary outlined button:
[Browse GitHub Repository](https://github.com/kartikarora/klaat){.buttonSecondary}

# Standard inline links (no tag):
Visit the official [Klaat Documentation](https://klaat.kartikarora.me/docs/index.html).
```

---

## Tables & Structured Data
Duration: 0:01:30

Markdown tables feature clean dividers, distinct headers, and monospace formatting for technical parameters.

### CLI Command Reference

| Command | Arguments / Flags | Output Format | Description |
| :--- | :--- | :--- | :--- |
| `export` | `-f html -o output <src>` | HTML / Web Components | Converts source Markdown or Google Doc to a web tutorial. |
| `demo` | `-o output -addr localhost:9090` | HTML / Web Server | Extracts bundled showcase, exports it, and launches live preview. |
| `serve` | `-d output -addr localhost:9090` | HTTP Web Server | Hosts codelabs locally with live browser preview. |
| `update` | `-d output` | Refresh Sources | Re-fetches and updates existing Google Doc codelabs. |
| `version` | `--format json` | Console Text / JSON | Displays build variant, version, and binary metadata. |

---

### Source Markdown for this Step

```markdown
## Tables & Structured Data
Duration: 0:01:30

| Command | Arguments / Flags | Output Format | Description |
| :--- | :--- | :--- | :--- |
| `export` | `-f html -o output <src>` | HTML | Converts sources to a web tutorial. |
| `demo` | `-o output` | HTML / Server | Extracts demo and launches preview. |
| `serve` | `-d output -p 9090` | HTTP Server | Hosts codelabs locally. |
```

---

## Lists & Nested Task Hierarchies
Duration: 0:01:30

Klaat handles unordered, ordered, mixed, and multi-level nested lists with consistent indentation and typography.

### Unordered Feature Checklist

* **Zero Native Dependencies**:
  * Compiled directly to native machine code with GraalVM
  * Sub-millisecond startup times
  * Low memory footprint under 30MB
* **Extensible Architecture**:
  * Pluggable Markdown and Google Docs parsers
  * Standalone HTML, pure Markdown, and offline package renderers
* **Theme-Aware Styling**:
  * Automatic dark mode adaptation
  * Modern CSS custom properties

---

### Multi-Step Deployment Workflow

1. Author your tutorial in Markdown or Google Docs:
   1. Set required frontmatter metadata (`id`, `summary`, `authors`)
   2. Organize content into level-2 headings (`## Step Title`)
   3. Assign duration for each step (`Duration: 5`)
2. Export with the Klaat CLI:
   ```bash
   klaat export -o output tutorial.md
   ```
3. Preview locally:
   ```bash
   klaat serve -d output
   ```
4. Deploy to your preferred web host or Cloudflare Pages.

---

### Source Markdown for this Step

```markdown
## Lists & Nested Task Hierarchies
Duration: 0:01:30

* **Zero Native Dependencies**:
  * Compiled directly to native code
  * Sub-millisecond startup times
* **Extensible Architecture**:
  * Pluggable Markdown and Google Docs parsers

1. Author your tutorial in Markdown:
   1. Set required frontmatter metadata
   2. Organize content into level-2 headings
2. Export with Klaat CLI:
   ```bash
   klaat export -o output tutorial.md
   ```
```

---

## Interactive Knowledge Check (Surveys)
Duration: 0:01:30

Engage readers with embedded multiple-choice quizzes and survey checkpoints directly in the flow of the tutorial.

### Quick Knowledge Check

Which command is used to extract the demo showcase, export it, and start the local preview server in one step?

- [ ] `klaat export --preview`
- [x] `klaat demo`
- [ ] `klaat serve -d output`
- [ ] `klaat preview tutorial.md`

> **Correct!** `klaat demo` extracts the bundled demo files to your current directory, exports them to HTML, and starts the local preview server automatically. {.positive}

---

### Source Markdown for this Step

```markdown
## Interactive Knowledge Check (Surveys)
Duration: 0:01:30

### Quick Knowledge Check

Which command extracts the demo and starts preview in one step?

- [ ] `klaat export --preview`
- [x] `klaat demo`
- [ ] `klaat serve -d output`
- [ ] `klaat preview tutorial.md`

> **Correct!** `klaat demo` runs extraction, export, and preview in one step. {.positive}
```

---

## Images & Visual Assets
Duration: 0:01:30

Images in Klaat are centered by default, responsive across mobile and desktop, and styled with rounded corners.

### Responsive Image with Caption

![Klaat Architecture & Native Engine](images/klaat.png)

---

### Source Markdown for this Step

```markdown
## Images & Visual Assets
Duration: 0:01:30

![Klaat Architecture & Native Engine](images/klaat.png)
```

---

## Video Embedding (YouTube)
Duration: 0:01:30

Klaat makes embedding video tutorials effortless. Embed YouTube videos using standard Markdown image link syntax or standalone URLs:

### Responsive 16:9 Video Player

[![Watch the Video](https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg)](https://www.youtube.com/watch?v=dQw4w9WgXcQ)

---

### Supported Video Formats & Syntax

You can embed YouTube videos using standard watch URLs, shortened links, or thumbnail links:

- `[![Watch the Video](https://img.youtube.com/vi/VIDEO_ID/0.jpg)](https://www.youtube.com/watch?v=VIDEO_ID)`
- `[Watch on YouTube](https://www.youtube.com/watch?v=VIDEO_ID)`
- `https://www.youtube.com/watch?v=VIDEO_ID`

Klaat automatically converts YouTube references into a responsive video player (`<iframe class="youtube-video">`) that adapts fluidly across mobile, tablet, and desktop screens with a native 16:9 aspect ratio.

---

### Source Markdown for this Step

```markdown
## Video Embedding (YouTube)
Duration: 0:01:30

[![Watch the Video](https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg)](https://www.youtube.com/watch?v=dQw4w9WgXcQ)
```

---

## Complete Source Markdown
Duration: 0:03:00

Below is the complete, raw Markdown source used to produce this interactive codelab. You can use the copy button in the top right corner to copy the full template for your own projects:

```markdown:examples/feature-showcase.md
---
id: klaat-feature-showcase
summary: A comprehensive, interactive tour showcasing all rich elements, typography, code blocks, infoboxes, surveys, media, and video embeds supported by Klaat.
authors: Kartik Arora
categories: demo, markdown, kotlin, tutorial
environments: web
status: published
feedback_link: https://github.com/kartikarora/klaat/issues
tags: demo, features, components, markdown, showcase, video, youtube
duration: 20
---

# Klaat Feature Showcase & Interactive Demo

## Overview & Metadata
Duration: 0:01:00

Welcome to the **Klaat Feature Showcase**!

> **Tip**: Dynamic progress tracking! {.positive}

## Typography & Inline Styling
Duration: 0:02:00

### Subsection Heading (H3)
#### Minor Heading (H4)

- **Bold Text**: `**text**`
- *Italic Text*: `*text*`
- `Inline Code`: backticks
> "High impact pull quote"

## Semantic Infoboxes (Asides)
Duration: 0:02:00

> **Best Practice**: Green positive box. {.positive}
> **Information**: Blue special box. {.special}
> **Caution**: Amber warning box. {.warning}
> **Critical**: Red critical box. {.critical}

## Code Blocks & Syntax Highlighting
Duration: 0:02:00

\```kotlin:CodelabExporter.kt
package me.kartikarora.klaat.core
class CodelabExporter(private val targetDir: File)
\```

## Action Buttons & Interactive Links
Duration: 0:01:30

[Download Starter Kit](https://github.com/kartikarora/klaat/archive/refs/heads/develop.zip){.buttonPrimary}
[Browse GitHub Repository](https://github.com/kartikarora/klaat){.buttonSecondary}

## Tables & Structured Data
Duration: 0:01:30

| Command | Arguments | Format |
| :--- | :--- | :--- |
| `export` | `-f html -o output` | HTML |
| `demo` | `-o output` | HTML / Server |

## Lists & Nested Task Hierarchies
Duration: 0:01:30

* Feature Item 1
  * Sub Item 1.1
1. Step One
2. Step Two

## Interactive Knowledge Check (Surveys)
Duration: 0:01:30

### Quick Knowledge Check
Which command starts demo?
- [ ] `klaat export --preview`
- [x] `klaat demo`

## Images & Visual Assets
Duration: 0:01:30

![Klaat Architecture Diagram](images/klaat.png)

## Video Embedding (YouTube)
Duration: 0:01:30

[![Watch the Video](https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg)](https://www.youtube.com/watch?v=dQw4w9WgXcQ)
```

---

## Summary & Next Steps
Duration: 0:01:00

Congratulations! You have completed the **Klaat Feature Showcase**.

### What You Explored
- ✅ Frontmatter metadata and dynamic drawer duration calculation
- ✅ Fluid typography with headings, bold, italics, and blockquotes
- ✅ Semantic aside infobox styles (Positive, Negative, Info, Caution)
- ✅ Multi-language syntax highlighting with filename pills and copy buttons
- ✅ Rounded action buttons and navigation styling
- ✅ Clean tabular data layouts
- ✅ Nested list hierarchies
- ✅ Interactive surveys and knowledge check blocks
- ✅ Responsive images with automatic centering and captions
- ✅ Seamless YouTube video embeds with responsive 16:9 aspect ratios
- ✅ In-step source Markdown snippets for every single feature

### Next Steps
- 📖 Read the complete [Klaat Documentation](https://klaat.kartikarora.me/docs/index.html)
- 🚀 Install Klaat skills for your AI agent with `klaat install-skills`
- ⭐ Star the project on [GitHub](https://github.com/kartikarora/klaat)
