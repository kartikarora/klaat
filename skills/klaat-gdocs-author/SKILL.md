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
