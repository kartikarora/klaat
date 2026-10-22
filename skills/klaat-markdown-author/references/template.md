---
id: sample-klaat-tutorial
summary: A sample tutorial demonstrating the correct frontmatter and structure.
categories: Kotlin, Tutorial
environments: Web, Kiosk
status: Published
authors: Klaat Author
feedback_link: https://github.com/yourusername/klaat/issues
tags: getting-started, guide, kotlin
---

# Sample Klaat Tutorial Title

## Step 1: Configuration
Duration: 5

Welcome to the tutorial! Here is a tip to help you:

> **Note:** Always keep your step durations reasonable (between 2 and 15 minutes is recommended).

### Environment Setup

Add the following dependency to your build file:

```kotlin
implementation("me.kartikarora.klaat:core:2.1.0")
```

## Step 2: Running the Command
Duration: 3

Now we are ready to run the export:

```bash
klaat export hello.md -o output/
```

**Verify the files:**
- [ ] Export directory `output` exists.
- [ ] The generated HTML file loads.
- [ ] Styles and assets load from CDN.

**Q: Can I customize the output location?**

A: Yes! Use the `-o` or `--out` flag to specify any path.