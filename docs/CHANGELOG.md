# Changelog

All notable changes to Klaat will be documented in this file and rendered on the docs site.

## Releases (Stable)

### [2.0.1] - 2026-09-14
- **feat**: Upgrade to `@kartikarora/brand` 1.8.0 design system across all web components and docs.
- **fix**: Prevent regression when parsing code comments containing quotes and contractions.
- **chore**: Bump Gradle dependencies and library versions to latest stable.

### [2.0.0] - 2026-08-19
- **feat!**: Native Klaat web components (`<klaat-codelab>`, `<klaat-step>`, action buttons) with CDN delivery.
- **feat**: Action Buttons syntax (`.buttonPrimary`, `.buttonSecondary`, `.button`) with custom icon support.
- **feat**: Interactive codelab showcase and format guide preview directly inside browser.
- **refactor**: Modernize duration parsing with `kotlin.time.Duration` API and dynamic remaining time calculation.
- **fix**: Fix survey inline code formatting and button step fence rendering.

### [1.0.0] - 2026-07-21
- **feat**: Initial production release of Klaat.
- **feat**: Google Docs and Markdown parser pipelines.
- **feat**: HTML, Markdown, and self-contained Offline export formats.
- **feat**: Embedded local Ktor preview server with live reloading.
- **feat**: GraalVM native image binaries for macOS (ARM64) and Linux (x64, ARM64).
- **feat**: AI agent skills package (`klaat-cli`, `klaat-markdown-author`, `klaat-gdocs-author`, `klaat-native-manager`).

---

## Snapshots (Development & Pre-release)

### [2.2.0.49-ebcf5fe5] - 2026-11-18
- **feat**: Bump base version to 2.2.0.
- **feat**: Support backticks and inline code formatting (`<code>`) inside headings across Markdown parser, HTML renderer, and Markdown renderer.
- **feat**: Expose `totalSteps` in `codelab.json` and provide JavaScript metadata API on `<klaat-kodelab>` (`totalSteps`, `totalDuration`, `currentStep`, `progressPercent`, `remainingTime`).

### [2.1.0.47-6a3be6f9] - 2026-11-09
- **feat**: Upgrade to `@kartikarora/brand` 2.0.0 design system and migrate templates, offline exporter, and element stylesheets to versioned assets.

### [2.1.0.46-802d7100] - 2026-11-03
- **fix(brew)**: Ensure binary has executable permissions before running completion generation in Homebrew formulas.

### [2.1.0.45-ec4a5649] - 2026-11-02
- **feat(brew)**: Add automatic shell completion generation to Homebrew formulas.

### [2.1.0.44-bd0cfd4e] - 2026-11-02
- **feat(cli)**: Add `completion` command and argument completion candidates (`CompletionCandidates.Path`, `CompletionCandidates.Fixed`).

### [2.1.0.43-63624aae] - 2026-11-02
- **fix(native)**: Enable complete reflection access for all Google Docs model classes.

### [2.1.0.42-f5171060] - 2026-11-02
- **fix(native)**: Register default constructor reflection for all Google Docs model classes.

### [2.1.0.41-b17006e9] - 2026-11-02
- **fix(native)**: Register `commentsViewMode` reflection field on `Docs$Documents$Get`.

### [2.1.0.40-1cb93ca2] - 2026-11-02
- **fix(native)**: Register `TokenResponse` reflection and trace Google Docs auth lifecycle.

### [2.1.0.39-9062ea4a] - 2026-10-22
- **fix(build)**: Remove unsupported GraalVM 25 SubstrateVM flags.

### [2.0.0.37-b12beda8] - 2026-08-19
- **chore**: Sync export metadata.

### [2.0.0.36-96f9efb3] - 2026-08-19
- **build**: Track skills and examples inputs on `processResources` and update demo test title check.

### [1.2.0.35-2b20dfc5] - 2026-08-18
- **build**: Update native-image metadata and verify native compilation.

### [1.2.0.34-a5b0725e] - 2026-08-18
- **feat**: Add in-step source markdown snippets and update feature showcase demo.

### [1.2.0.33-2235c4da] - 2026-08-17
- **chore**: Update CDN stylesheet path.

### [1.2.0.31-fa7d3cad] - 2026-08-14
- **feat(skills,styles)**: Unify native manager skill and update custom infobox styles.

### [1.2.0.30-948ded73] - 2026-08-14
- **style**: Update infobox styles.

### [1.2.0.29-44d4bd21] - 2026-08-12
- **style**: Add aside success theme styling.

### [1.2.0.28-705f2a0d] - 2026-08-03
- **refactor**: Replace remaining inline fully qualified classes with proper imports.

### [1.2.0.27-449fbf0b] - 2026-08-03
- **chore**: Update native image configurations for recent changes.

### [1.2.0.26-7fe31ad0] - 2026-07-31
- **chore**: Bump version to 1.2.0 across project config, MCP server, and skills command.

### [1.1.0.25-b7055c17] - 2026-07-31
- **docs**: Remove manual download section and replace overview blockquote with paragraph.

### [0.4.0.24-12c70992] - 2026-07-17
- **chore**: Update GitHub Actions workflows and custom actions to use latest versions.

### [0.3.0.23-9cb4ff8f] - 2026-05-04
- **build**: Build script fix attempt.

### [0.2.0.20-2aa8c697] - 2026-03-20
- **feat**: Bash completion update.

### [0.1.0.18-5fa238b8] - 2026-03-12
- **ci**: CI workflow updates.

### [0.1.0.16-818b1f4c] - 2026-03-03
- **chore**: Incremental improvements.
