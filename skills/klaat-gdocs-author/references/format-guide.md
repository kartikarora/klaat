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
*   **Step Duration**: Specify step length directly beneath the H2 using any of the following formats:
    *   `Duration: 5` or `Duration: 5 min` (integer minutes)
    *   `Duration: 0:01:30` or `Duration: 1:30` (colon-separated `HH:MM:SS` / `MM:SS`)
    *   `Duration: 0:00:00 (ignore)` (excludes this step's duration from the total codelab duration calculation)

*Example:*
```markdown
# Building a Kotlin App

## Step 1: Getting Started
Duration: 0:01:30

Step content goes here...

## Step 2: Overview & Concepts
Duration: 0:00:00 (ignore)

This overview does not add to the remaining tutorial time.
```

### Google Docs Structure
*   **Codelab Title**: Styled using the Google Docs **Title** style.
*   **Step Headers**: Styled using the Google Docs **Heading 1** style.
*   **Step Duration**: Write `Duration: X` in plain text directly under the Heading 1 step header (supports `Duration: 5`, `Duration: 1:30`, and `(ignore)` modifiers).

---

## 3. Inline & Block Formatting Elements

Use these syntax conventions to format various rich widgets:

### Info Boxes (Alerts & Callouts)
Info boxes are created using blockquotes with a specific metadata attribute tag or standard markdown bold labels:

*   **Special Box (Blue - Tips/Info)**:
    ```markdown
    > This is an informative tip or note. {.special}
    ```
*   **Positive Box (Green - Best Practices)**:
    ```markdown
    > **Tip**: This is a recommended best practice. {.positive}
    ```
*   **Warning Box (Yellow/Orange - Warnings/Caution)**:
    ```markdown
    > **Caution**: Take note of potential pitfalls. {.warning}
    ```
*   **Critical Box (Red - Critical Alerts)**:
    ```markdown
    > **Critical**: High severity alert or breaking change. {.critical}
    ```
*   **Default Box (Gray - General Asides)**:
    ```markdown
    > This is a general aside or contextual note. {.default}
    ```

### Code & Commands
*   **Inline Code**: Surround with single backticks: `val name = "Klaat"`.
*   **Multi-line Code Blocks**: Use triple backticks with syntax language:
    ```kotlin
    fun main() {
        println("Hello World")
    }
    ```
*   **Code Block with Filename**: Put file names in bold immediately above the code block or use language header (e.g. `kotlin:Main.kt`):
    ```markdown
    **Main.kt**
    ```kotlin
    fun main() = println("Hello!")
    ```
    ```
*   **Google Docs Code Block**: Place code in a single-cell table, set font to **Courier New**. For terminal commands, set the table background to **black** and font to white.

### Tables & Structured Data
Render standard Markdown tables with column alignments:
```markdown
| Command | Arguments | Description |
| :--- | :---: | ---: |
| `export` | `-f html` | Exports tutorial to web HTML |
| `serve` | `-p 9090` | Starts local preview server |
```

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

### Action / Call-to-Action Buttons
A link transforms into an interactive pill button when appended with a button tag attribute:
*   **Primary Solid Button (Default)**: Use `{.buttonPrimary}` or `{.button}`:
    ```markdown
    [Explore Live Demo](https://klaat.kartikarora.me/demo/index.html){.buttonPrimary icon=play_arrow}
    ```
*   **Secondary Outlined Button**: Use `{.buttonSecondary}`:
    ```markdown
    [Send Feedback](mailto:dev&#64;kartikarora&#46;me){.buttonSecondary icon=mail}
    ```
*   **Custom Material Symbol Icon**: Add the `icon=<name>` attribute:
    ```markdown
    [Watch Demo Video](https://example.com/video){.buttonPrimary icon=play_arrow}
    [View Documentation](https://klaat.kartikarora.me/docs/index.html){.buttonSecondary icon=launch}
    ```
*   *Note:* Standard links without `{.button*}` tags remain clean inline text links. Download icons are automatically attached when text/URL contains "download" or file archive extensions unless a custom icon is specified.

### Media & Embeds
*   **Images**: Render images with standard syntax and an optional caption underneath:
    ```markdown
    ![Klaat Architecture Overview](images/architecture.png)
    ```
*   **YouTube Videos**: Embed a responsive YouTube player by putting its link on its own line:
    ```markdown
    https://www.youtube.com/watch?v=dQw4w9WgXcQ
    ```