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
*   **Positive (Tips/Info):** Start blockquote with `**Note:**` or `**Pro Tip:**`
    ```markdown
    > **Note:** Best practice tips go here.
    ```
*   **Negative (Warnings/Caution):** Start blockquote with `**Warning:**` or `**Caution:**`
    ```markdown
    > **Warning:** Critical alerts or safety warnings go here.
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

### 4. Interactive Surveys
Use checklist bullet blocks immediately following a bold question:
```markdown
**How would you rate your Kotlin experience?**
- [ ] Beginner
- [ ] Intermediate
- [ ] Expert
```

### 5. Frequently Asked Questions (FAQs)
Format Q&A sections using bold `Q:` and plain `A:` pairs:
```markdown
**Q: What is the default port?**

A: The default port is 9090.
```

### 6. Action / Download Buttons
Write a single link alone on its own line:
```markdown
[Download Starter Code](https://example.com/starter.zip)
```