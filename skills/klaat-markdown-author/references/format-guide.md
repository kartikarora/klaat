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

*   **Positive Box (Tips/Info)**:
    *   *Markdown*: Blockquote starting with `**Note:**` or `**Pro Tip:**`
    *   *Google Docs*: Single-cell table with a **light green** background.
    *   *Example*:
        ```markdown
        > **Note:** This is a tip/recommendation.
        ```
*   **Negative Box (Warnings/Alerts)**:
    *   *Markdown*: Blockquote starting with `**Warning:**` or `**Caution:**`
    *   *Google Docs*: Single-cell table with a **light red/orange** background.
    *   *Example*:
        ```markdown
        > **Warning:** This is a warning or critical notice.
        ```

### Code & Commands
*   **Inline Code**: Surround with single backticks: `` `val name = "Klaat"` ``.
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
