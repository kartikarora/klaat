---
id: klaat-feature-showcase
summary: A comprehensive, living guide and interactive showcase demonstrating all metadata schemas, document formats, typography, code blocks, semantic infoboxes, action buttons, surveys, tables, media, and Web Components supported by Klaat.
authors: Kartik Arora
categories: guide, showcase, markdown, kotlin, tutorial
environments: web
status: published
feedback_link: mailto:dev&#64;kartikarora&#46;me
tags: format-guide, showcase, components, markdown, gdocs, buttons, infoboxes, video, web-components
duration: 25
---

# Klaat Format Guide & Feature Showcase

## Overview & Codelab Architecture
Duration: 0:02:00

Welcome to the **Klaat Format Guide & Feature Showcase**! This interactive tutorial serves as both an exhaustive technical reference manual and a living, hands-on demonstration of every visual element, metadata property, and interactive component supported by Klaat.

### Codelab Architecture

Klaat translates structured documents (Markdown or Google Docs) into interactive web tutorials centered around **Steps**:

- **Navigation Drawer**: Tracks current progress, estimated remaining time, and allows instant jumping between steps.
- **Dynamic Countdown**: Computes reading time per step and total remaining duration on the fly.
- **Keyboard Navigation**: Press <kbd>&larr;</kbd> (Left Arrow) and <kbd>&rarr;</kbd> (Right Arrow) to advance seamlessly.
- **Responsive Layout**: Fluidly adapts across mobile devices, tablets, and widescreen desktop monitors.

> **Tip**: Notice the left-hand drawer. As you read through each step, the timer and progress bar update automatically in real-time! {.positive}

---

### Source Markdown for this Step

```markdown
# Klaat Format Guide & Feature Showcase

## Overview & Codelab Architecture
Duration: 0:02:00

Welcome to the **Klaat Format Guide & Feature Showcase**!

> **Tip**: Progress updates automatically in real-time! {.positive}
```

---

## Metadata & Frontmatter Schema
Duration: 0:02:00

Every codelab begins with a metadata block that configures runtime settings, categorization, author attribution, and search indexing.

### Configuration Fields Reference

| YAML Field | Required | Data Type | Default Behavior | Description |
| :--- | :---: | :--- | :--- | :--- |
| `id` | Optional | String | Sluggified `summary` | Unique URL-safe directory identifier. |
| `summary` | **Required** | String | `""` | Short description used for cards and catalogs. |
| `categories` | Optional | List / CSV | `[]` | Topic categories for filtering. |
| `environments` | Optional | List / CSV | `["Web"]` | Target environments where the codelab runs. |
| `status` | Optional | List / CSV | `["Published"]` | Publication status (`Published`, `Draft`, `Deprecated`). |
| `authors` | Optional | List / CSV | `[]` | Author names responsible for content. |
| `feedback_link` | Optional | String | `""` | Feedback URL or mailto link in the drawer footer. |
| `duration` | Optional | Integer | Computed sum | Total duration in minutes (overrides step sum). |
| `image` | Optional | String | First image | Cover/hero image URL for catalog preview cards. |
| `favicon` | Optional | String | Brand icon | Favicon URL for exported HTML pages. |
| `footer` | Optional | String | Brand credit | Custom footer text or HTML on every step. |

---

### Source Markdown for this Step

```markdown
---
id: my-codelab
summary: Learn Kotlin development with Klaat
authors: Kartik Arora
categories: kotlin, tutorial
environments: web
status: published
feedback_link: mailto:dev@kartikarora.me
duration: 20
---
```

---

## Document Formats (Markdown & GDocs)
Duration: 0:02:00

Klaat supports both **Markdown** files and **Google Docs** documents as first-class authoring formats.

### Markdown Structure Rules

1. **Frontmatter Block**: Enclosed in triple dashes (`---`) at the top of the file.
2. **Codelab Title (H1)**: Single `# Title` directly after frontmatter.
3. **Step Headers (H2)**: Denoted by `## Step Title`.
4. **Step Durations**: Directly beneath each `## Step Title`, specify:
   - Integer minutes: `Duration: 5`
   - Time formatted: `Duration: 0:01:30` (1 min 30 sec)
   - Countdown exclusion: `Duration: 0:00:00 (ignore)`

---

### Google Docs Visual Format Rules

In Google Docs, document styles map directly to codelab nodes:

| Codelab Node | Google Doc Visual Style |
| :--- | :--- |
| **Metadata Block** | 2-column table at top of document (keys on left, values on right). |
| **Codelab Title** | `Title` style paragraph. |
| **Steps** | `Heading 1` paragraphs. |
| **Step Durations** | Plain text `Duration: 5` directly under Heading 1. |
| **Infoboxes** | Single-cell tables with background color (Green = Positive, Blue = Info, Yellow = Warning, Red = Critical). |
| **Code Blocks** | Single-cell table formatted in `Courier New` font. |

---

### Source Markdown for this Step

```markdown
## Step Title
Duration: 0:01:30

Step body content goes here...

## Conclusion
Duration: 0:00:00 (ignore)

Final wrap-up excluded from countdown.
```

---

## Text & Typography Formatting
Duration: 0:02:00

Klaat provides typography tokens built on Google Fonts **Space Grotesk** and **JetBrains Mono**.

### Heading Hierarchy

### Subsection Heading (H3)
Used for primary sub-topics and conceptual divisions.

#### Minor Heading (H4)
Used for granular steps, technical notes, or component properties.

---

### Text Formatting & Emphasis

You can emphasize text using standard Markdown constructs:

- **Bold Text**: Use strong emphasis for key terms (`**bold**`).
- *Italic Text*: Use subtle emphasis for definitions (`*italic*`).
- ***Bold & Italic***: High-contrast emphasis (`***bold & italic***`).
- `Inline Code`: Highlight commands like `klaat export`, variables, or file paths.

---

### High-Impact Blockquotes

Blockquotes feature accent borders and subtle background tinting:

> "Simplicity is prerequisite for reliability." - Edsger W. Dijkstra

---

### Source Markdown for this Step

```markdown
## Text & Typography Formatting
Duration: 0:02:00

### Subsection Heading (H3)
#### Minor Heading (H4)

- **Bold Text**: `**text**`
- *Italic Text*: `*text*`
- `Inline Code`: backticks
> "High impact pull quote"
```

---

## Semantic Infoboxes (Alert Callouts)
Duration: 0:02:00

Infoboxes (asides) highlight critical guidelines, warnings, tips, and achievements. Created using blockquotes with a trailing class tag:

### Positive (Success) - `{.positive}`

> **Success**: Use positive infoboxes for milestones, successful build completions, or best practice recommendations. {.positive}

---

### Special (Info / Tip) - `{.special}`

> **Information**: Use special infoboxes for non-critical contextual notes, architecture details, or background reading. {.special}

---

### Warning (Caution) - `{.warning}`

> **Caution**: Use warning infoboxes for breaking changes, required prerequisites, or common pitfalls. {.warning}

---

### Critical (Error / Security) - `{.critical}`

> **Critical**: Use critical infoboxes for security vulnerabilities, destructive operations, or data loss warnings. {.critical}

---

### Default (Neutral Aside)

> Standard aside blockquote without a specific type tag renders with a neutral gray accent border.

---

### Source Markdown for this Step

```markdown
> **Success**: Milestone completed! {.positive}
> **Info**: Contextual background details. {.special}
> **Caution**: Mind the prerequisites. {.warning}
> **Critical**: Never expose private API keys! {.critical}
> Neutral aside blockquote.
```

---

## Code Blocks & Syntax Highlighting
Duration: 0:02:00

Code blocks support multi-language syntax highlighting, one-click clipboard copying, and optional filename headers.

### Standard Kotlin Code Block

```kotlin
package me.kartikarora.klaat.core

data class Step(
    val title: String,
    val duration: Duration,
    val nodes: List<Node>,
    val excludeDuration: Boolean = false
)
```

---

### Code Block with Filename Pill

Specify a bold title line immediately before the code block:

**Exporter.kt**
```kotlin
package me.kartikarora.klaat.core

class Exporter(private val targetDir: File) {
    fun export(codelab: Codelab, format: OutputFormat) {
        // Orchestrates parser-renderer pipeline
    }
}
```

---

### Shell / Terminal Commands

```bash
# Export codelab to HTML with local asset prefix
klaat export -f html -o output examples/feature-showcase.md

# Launch local preview server on port 9090
klaat serve -d output -p 9090
```

---

### Source Markdown for this Step

```markdown
**Exporter.kt**
\```kotlin
package me.kartikarora.klaat.core
class Exporter(private val targetDir: File)
\```
```

---

## Action Buttons & Interactive Links
Duration: 0:02:00

Action buttons and call-to-actions are styled as rounded pill buttons with hover states. Add `{.buttonPrimary}` (or `{.button}`) for solid buttons, `{.buttonSecondary}` for outlined buttons, and `icon=<name>` for custom Material Symbol icons.

### Primary Action Button with Custom Icon

Explore the official Klaat documentation:

[Read Documentation](https://klaat.kartikarora.me/docs/index.html){.buttonPrimary icon=menu_book}

---

### Automatic Download Detection (Safe Demo)

Links with `"download"` in their text or targeting archive files automatically display the download symbol without requiring a custom icon tag:

[Download Sample Asset](#){.buttonPrimary}

---

### Secondary Action Button with Custom Icon

Send feedback or reach out with suggestions:

[Send Feedback](mailto:dev&#64;kartikarora&#46;me){.buttonSecondary icon=mail}

---

### External Navigation Links

Links without button attributes remain clean inline navigation links:
- Visit the official [Klaat Documentation](https://klaat.kartikarora.me/docs/index.html).
- Explore the [CLI Man Pages](https://klaat.kartikarora.me/docs/index.html).

---

### Source Markdown for this Step

```markdown
## Action Buttons & Interactive Links
Duration: 0:02:00

# Primary solid button with custom icon:
[Read Documentation](https://klaat.kartikarora.me/docs/index.html){.buttonPrimary icon=menu_book}

# Automatic download detection (no custom icon needed):
[Download Sample Asset](#){.buttonPrimary}

# Secondary outlined button with email icon:
[Send Feedback](mailto:dev&#64;kartikarora&#46;me){.buttonSecondary icon=mail}

# Standard inline links (no tag):
Visit the official [Klaat Documentation](https://klaat.kartikarora.me/docs/index.html).
```

---

## Tables & Structured Data
Duration: 0:02:00

Tables format structured tabular data with alignment support:

| Command | Arguments | Format | Description |
| :--- | :--- | :---: | :--- |
| `export` | `[-f format] [-o dir]` | `html` / `md` | Compiles Markdown/GDocs into static codelabs. |
| `serve` | `[-d dir] [-p port]` | Server | Starts local live-reloading preview server. |
| `demo` | `[-o dir] [-p port]` | Demo | Extracts showcase, exports, and opens browser. |
| `install-skills` | `[-t target]` | Skills | Installs Klaat authoring skills for AI agents. |
| `version` | `--format json` | CLI | Prints version metadata and runtime information. |

---

### Source Markdown for this Step

```markdown
| Command | Arguments | Format |
| :--- | :--- | :---: |
| `export` | `[-f format] [-o dir]` | `html` |
| `serve` | `[-d dir] [-p port]` | Server |
```

---

## Lists & Nested Task Hierarchies
Duration: 0:02:00

Klaat renders bulleted lists, numbered lists, nested sub-items, and task checklists cleanly:

### Feature Checklist
* Native Compilation Architecture
  * GraalVM Native Image for Apple Silicon & Linux x86_64
  * Sub-millisecond startup times with zero JVM overhead
* Modern Multi-Format Exporter
  * Static HTML with custom Web Components
  * Clean Markdown export with frontmatter preservation
  * Offline self-contained distribution bundles

### Numbered Workflow
1. Author your codelab in Markdown or Google Docs.
2. Run `klaat export -o output tutorial.md`.
3. Preview locally with `klaat serve -d output`.
4. Deploy static output to Cloudflare Pages, Firebase Hosting, or GitHub Pages.

---

### Source Markdown for this Step

```markdown
* Feature Item 1
  * Sub Item 1.1
1. Step One
2. Step Two
```

---

## Interactive Knowledge Checks (Surveys)
Duration: 0:02:00

Embed interactive quizzes and knowledge checks inside any step. Options persist across reloads using `localStorage`.

### Quick Knowledge Check

Which command extracts the bundled demo showcase, exports it, and opens the live browser preview in a single step?

- [ ] `klaat export --preview`
- [x] `klaat demo`
- [ ] `klaat serve -d output`
- [ ] `klaat preview tutorial.md`

> **Correct!** `klaat demo` extracts the bundled demo files to your current directory, exports them to HTML, and starts the local preview server automatically. {.positive}

---

### Source Markdown for this Step

```markdown
### Quick Knowledge Check
Which command extracts the demo and starts preview in one step?
- [ ] `klaat export --preview`
- [x] `klaat demo`
- [ ] `klaat serve -d output`

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

- `[![Watch the Video](https://img.youtube.com/vi/VIDEO_ID/0.jpg)](https://www.youtube.com/watch?v=VIDEO_ID)`
- `[Watch on YouTube](https://www.youtube.com/watch?v=VIDEO_ID)`
- `https://www.youtube.com/watch?v=VIDEO_ID`

Klaat automatically converts YouTube references into a responsive video player (`<iframe class="youtube-video">`) that adapts fluidly with a native 16:9 aspect ratio.

---

### Source Markdown for this Step

```markdown
## Video Embedding (YouTube)
Duration: 0:01:30

[![Watch the Video](https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg)](https://www.youtube.com/watch?v=dQw4w9WgXcQ)
```

---

## Web Components & CDN Architecture
Duration: 0:02:00

Klaat HTML exports are powered by dependency-free Web Components. Both `<kodelab-*>` and `<klaat-kodelab*>` tags are registered in the runtime.

### Custom Element Tags

| Tag Name | Key Attributes | Purpose |
| :--- | :--- | :--- |
| `<kodelab-codelab>` / `<klaat-kodelab>` | `id`, `title`, `environment`, `feedback-link` | Main container orchestrating drawer, timer, and keyboard shortcuts. |
| `<kodelab-step>` / `<klaat-kodelab-step>` | `label`, `duration`, `exclude-duration` | Slide container with label and countdown duration. |
| `<kodelab-survey>` / `<klaat-kodelab-survey>` | `survey-id` | Interactive questionnaire with localStorage persistence. |
| `<kodelab-analytics>` / `<klaat-kodelab-analytics>` | `gaid` | Google Analytics transition tracking. |

---

### CDN Integration Boilerplate

To use the custom elements in standalone web pages, include the CDN assets:

```html
<!-- CDN Stylesheets -->
<link rel="stylesheet" href="https://distribute.kartikarora.me/css/kartikarora.1.7.0.css">
<link rel="stylesheet" href="https://distribute.kartikarora.me/css/klaat-elements.css">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">

<!-- CDN Scripts -->
<script src="https://distribute.kartikarora.me/js/kartikarora.1.7.0.js"></script>
<script src="https://distribute.kartikarora.me/js/klaat-elements.js"></script>
```

---

## Complete Source Markdown
Duration: 0:03:00

Below is the complete, raw Markdown source used to produce this interactive codelab. You can use the copy button in the top right corner to copy the full template for your own projects:

```markdown:examples/feature-showcase.md
---
id: klaat-feature-showcase
summary: A comprehensive, living guide and interactive showcase demonstrating all metadata schemas, document formats, typography, code blocks, semantic infoboxes, action buttons, surveys, tables, media, and Web Components supported by Klaat.
authors: Kartik Arora
categories: guide, showcase, markdown, kotlin, tutorial
environments: web
status: published
feedback_link: mailto:dev@kartikarora.me
tags: format-guide, showcase, components, markdown, gdocs, buttons, infoboxes, video
duration: 25
---

# Klaat Format Guide & Feature Showcase

## Overview & Codelab Architecture
Duration: 0:02:00

Welcome to the **Klaat Format Guide & Feature Showcase**!

> **Tip**: Look at the drawer on the left for dynamic progress tracking! {.positive}

## Metadata & Frontmatter Schema
Duration: 0:02:00

| YAML Field | Required | Data Type | Description |
| :--- | :---: | :--- | :--- |
| `id` | Optional | String | URL-safe identifier. |
| `summary` | **Required** | String | Short description. |

## Typography & Formatting
Duration: 0:02:00

- **Bold Text**: `**text**`
- *Italic Text*: `*text*`
- `Inline Code`: backticks
> "High impact pull quote"

## Semantic Infoboxes (Alerts)
Duration: 0:02:00

> **Positive**: Milestones completed. {.positive}
> **Special**: Useful tips. {.special}
> **Warning**: Cautionary notes. {.warning}
> **Critical**: Security warnings. {.critical}

## Code Blocks & Highlighting
Duration: 0:02:00

\```kotlin:CodelabExporter.kt
package me.kartikarora.klaat.core
class CodelabExporter(private val targetDir: File)
\```

## Action Buttons & Interactive Links
Duration: 0:02:00

[Read Documentation](https://klaat.kartikarora.me/docs/index.html){.buttonPrimary icon=menu_book}
[Download Sample Asset](#){.buttonPrimary}
[Send Feedback](mailto:dev@kartikarora.me){.buttonSecondary icon=mail}

## Tables & Structured Data
Duration: 0:02:00

| Command | Arguments | Format |
| :--- | :--- | :--- |
| `export` | `-f html -o output` | HTML |
| `demo` | `-o output` | HTML / Server |

## Interactive Knowledge Checks (Surveys)
Duration: 0:02:00

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

Congratulations! You have completed the **Klaat Format Guide & Feature Showcase**.

### What You Explored
- ✅ Frontmatter metadata schema and dynamic drawer duration calculation
- ✅ Markdown and Google Docs source formats and mapping rules
- ✅ Fluid typography with headings, bold, italics, and pull quotes
- ✅ Semantic aside infobox styles (Positive, Info, Warning, Critical, Default)
- ✅ Multi-language syntax highlighting with filename pills and copy buttons
- ✅ Rounded action pill buttons, custom Material Symbols icons, and auto-download detection
- ✅ Tabular data layouts with column alignments
- ✅ Nested list hierarchies and workflow steps
- ✅ Interactive surveys with `localStorage` persistence
- ✅ Responsive images with automatic centering and captions
- ✅ Seamless YouTube video embeds with responsive 16:9 aspect ratios
- ✅ Kodelab Web Components architecture (`<kodelab-*>`) and CDN integration
- ✅ In-step source Markdown snippets for every single feature

### Next Steps
- 📖 Read the complete [CLI Command Reference](https://klaat.kartikarora.me/docs/index.html)
- 🚀 Install Klaat skills for your AI agent with `klaat install-skills`
- 💬 Reach out with feedback, thoughts, or suggestions: `dev [at] kartikarora [dot] me` (<a href="mailto:dev&#64;kartikarora&#46;me">send email</a>)
