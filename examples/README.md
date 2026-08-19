# Klaat Example Codelabs

This directory contains example codelabs for testing, demonstration, and learning purposes.

## Examples

### 1. Feature Showcase (`feature-showcase.md`)

**Purpose:** Comprehensive interactive showcase of all Klaat features

A complete codelab demonstrating every feature supported by Klaat:
- Brand styling, custom CSS hooks, Space Grotesk typography
- Code blocks with syntax highlighting, filenames, and copy button
- Info boxes (`{.positive}`, `{.special}`, `{.warning}`, `{.critical}`, default)
- Pill action buttons (`{.buttonPrimary}`, `{.buttonSecondary}`, `icon=<name>`, automatic download icon)
- Interactive surveys and FAQs
- Data tables with alignments
- Responsive YouTube videos and images

**Export:**
```bash
klaat export examples/feature-showcase.md -o output/
```

---

### 2. Comprehensive Example (`comprehensive-example.md`)

**Purpose:** Full feature demonstration and manual testing

A complete tutorial demonstrating core authoring capabilities:
- Text formatting options
- Multi-language code snippets (Kotlin, JavaScript, Python, Bash)
- Structured lists, tables, and surveys
- Action buttons and callouts

**Export:**
```bash
klaat export examples/comprehensive-example.md -o output/
```

---

### 3. Infobox Example (`infobox-example.md`)

**Purpose:** Focused demonstration of callout and alert box variants

Covers the 5 standard infobox variants:
- Special/Blue (`{.special}`)
- Warning/Amber (`{.warning}`)
- Critical/Red (`{.critical}`)
- Positive/Green (`{.positive}`)
- Default/Neutral blockquote

**Export:**
```bash
klaat export examples/infobox-example.md -o output/
```

---

### 4. Images Example (`images-example.md`)

**Purpose:** Focused demonstration of local and remote image embeds with captions

**Export:**
```bash
klaat export examples/images-example.md -o output/
```

---

### 5. Google Docs Examples

**Purpose:** Demonstrate Google Docs format and integration

Google Docs versions demonstrating full Google Docs format support:

#### Comprehensive Example (Google Docs)
- **Document ID:** `1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM`
- **View:** [Open in Google Docs](https://docs.google.com/document/d/1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM)
- Full-featured example with all formatting options

#### Image Example (Google Docs)
- **Document ID:** `1vH7gJRjdDge7c8T-_FHKzUB6SBBk2Xy3Qd-oAdMG_xg`
- **View:** [Open in Google Docs](https://docs.google.com/document/d/1vH7gJRjdDge7c8T-_FHKzUB6SBBk2Xy3Qd-oAdMG_xg)
- Example with images and media

**Export from Google Docs:**
```bash
# Export Comprehensive
klaat export 1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM -o output/

# Export Image Example
klaat export 1vH7gJRjdDge7c8T-_FHKzUB6SBBk2Xy3Qd-oAdMG_xg -o output/
```

---

## Testing All Examples

### Export All

```bash
# Export all examples
klaat export examples/*.md -o output/
```

### Serve Locally

```bash
# Serve all exported codelabs
klaat serve -d output/
```

Then visit:
- http://localhost:9090/klaat-feature-showcase/
- http://localhost:9090/comprehensive-codelab-example/
- http://localhost:9090/infobox-example/

---

## Creating Your Own Examples

### Template Structure

```markdown
---
id: unique-codelab-id
summary: Brief description (1-2 sentences)
categories: Category1, Category2
environments: Web
status: Published
authors: Your Name
tags: tag1, tag2, tag3
---

# Codelab Title

## First Step
Duration: 2

Step content...

## Second Step
Duration: 3

More content...

## Conclusion
Duration: 1

Wrap up...
```

### Best Practices

1. **Use descriptive IDs:** `my-feature-tutorial` not `tutorial1`
2. **Set realistic durations:** Test how long steps actually take
3. **Keep steps focused:** One concept per step
4. **Add context:** Explain why, not just what
5. **Test thoroughly:** Export and review before sharing

---

## Troubleshooting

### Example doesn't export

Check YAML frontmatter syntax:
```bash
# Should have three dashes before and after
---
id: my-id
---
```

### Missing assets

Assets are automatically copied. Check:
```bash
ls output/codelab-elements/
# Should show: codelab-elements.css, codelab-elements.js, etc.
```

### Broken formatting

Validate your Markdown:
- Use H2 (`##`) for steps, not H1 (`#`)
- Close all code blocks with three backticks
- Escape special characters if needed

---

## Additional Resources

- [Klaat README](../README.md)
- [Original Format Guide](https://github.com/googlecodelabs/tools/FORMAT-GUIDE.md)
- [Markdown Guide](https://www.markdownguide.org/)
