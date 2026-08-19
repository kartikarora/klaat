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