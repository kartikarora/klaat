# @kartikarora Brand Migration Guide: v1.8.0 &rarr; v2.0.0

This guide provides step-by-step instructions and automated tooling for migrating consumer applications, websites, and native themes from **v1.8.0** to **v2.0.0** of the `@kartikarora` design system without breaking layouts, corrupting custom CSS, or losing backward compatibility.

---

## 🌟 What's New in v2.0.0 (Tokyo Neo-Brutalist Electric Aesthetic)

1. **Tokyo Neo-Brutalist Color Architecture:**
   - **Akihabara Laser Cyan (`--color-signal`):** `#00f0ff` (dark canvas) and `#0084c7` (high-contrast light canvas).
   - **Caution Yellow Secondary Pop (`--color-signal-secondary`):** `#ffe600` (dark) and `#e6b800` (light). High-contrast electric accent for badges, highlights, and status tags.
   - **Deep Space Obsidian Surfaces (Dark):** `#070a10` canvas, `#121824` cards, and `#04060a` sunken terminals.
   - **Architectural Concrete / White Surfaces (Light):** `#ffffff` canvas, `#f4f6fa` elevated wells, and `#0a111c` deep ink.
2. **Distinctive Typography Standards:**
   - **Chivo (`wght@300..900`):** Authoritative, heavy industrial grotesque powering both display headlines (Black 900 with `-0.04em` tracking) and editorial body prose (300/400/500). Replaces legacy Albert Sans.
   - **JetBrains Mono (`wght@400..800`):** Permanently locked monospace face for code blocks, CLI snippets, telemetry, and technical tags (`--font-mono`).
3. **The Tokyo Visual Punch (Hardened Shadows & Micro-Kinetic Physics):**
   - **Solid Offset Block Shadows:** Replaces soft blurred drop shadows with crisp `2px 2px 0px`, `4px 4px 0px`, or `6px 6px 0px` solid unblurred block shadows.
   - **Kinetic Diagonal Hover:** Cards and buttons spring diagonally with `transform: translate(-2px, -2px)` on hover while expanding their solid block shadow (`6px 6px 0px var(--accent)`).
   - **Headline Highlighting (`.tokyo-highlight`):** Focal headline phrases receive high-visibility laser cyan backgrounding with a caution yellow drop-shadow offset.
   - **Designated Contrast Punch Cards (`.tokyo-card-contrast`):** One designated artifact per section punches through with caution yellow tag borders and active hover glow.
   - **Sunken CLI Terminals (`.tokyo-cli-pill`):** Commands and terminal inputs rendered in obsidian sunken wells with 4px signal left-borders.
4. **Semantic Token Names:**
   - Standardized CSS tokens (`--color-paper`, `--color-ink`, `--color-signal`, `--color-signal-secondary`, `--color-rule`, `--radius-md`).
   - Legacy short aliases (`--bg`, `--primary`, `--accent`, `--border`) remain fully supported in `:root` for backward compatibility.
5. **Native Multiplatform Parity:**
   - **Jetpack Compose / Compose Multiplatform:** Material 3 semantic color mapping in [`Color.kt`](../../kartikarora-compose-theme/references/Color.kt) with `LightSignalSecondary` and `DarkSignalSecondary`.
   - **Apple SwiftUI:** Native semantic tokens with dynamic `UIColor`/`NSColor` light/dark support in [`Color.swift`](../../kartikarora-swiftui/references/Color.swift) with `Color.brand.signalSecondary`.
6. **Hallmark-Grade Floor Standards:**
   - `overflow-x: clip` (never `hidden`).
   - Instant `:focus-visible` outlines.
   - Zero catch-all transitions (`transition: all` is prohibited; use specific properties or `var(--transition)`).

---

## 🔄 Token Mapping Reference

| 1.8.0 Token / Property | 2.0.0 Semantic Token | Light Mode Hex | Dark Mode (Tokyo) Hex | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `var(--bg)` | `var(--color-paper)` | `#FFFFFF` | `#070A10` | Canvas base background |
| `var(--bg-alt)` | `var(--color-paper-alt)` | `#F4F6FA` | `#0D121C` | Elevated surfaces / wells |
| `var(--card)` | `var(--color-card)` | `#FFFFFF` | `#121824` | Card backgrounds |
| `var(--primary)` | `var(--color-ink)` | `#0A111C` | `#F0F6FC` | Primary display & body text |
| `var(--secondary)` | `var(--color-ink-muted)` | `#49586E` | `#8A9BB4` | Secondary text & subtitles |
| `var(--tertiary)` / `var(--muted)` | `var(--color-ink-faint)` | `#7F90A6` | `#4E5F7A` | Subtle / tertiary text |
| `var(--accent)` | `var(--color-signal)` | `#0084C7` | `#00F0FF` | Primary interactive signal (Akihabara Cyan) |
| `var(--accent-hover)` | `var(--color-signal-hover)` | `#009CEB` | `#33F3FF` | Primary hover state |
| *(new)* | `var(--color-signal-secondary)` | `#E6B800` | `#FFE600` | High-contrast secondary pop (Caution Yellow) |
| `var(--border)` | `var(--color-rule)` | `#CDD8E6` | `#1A2436` | Hairline dividers |
| `var(--border-radius)` | `var(--radius-md)` | `6px` | `6px` | Standard corner radius |
| `var(--border-radius-sm)` | `var(--radius-sm)` | `4px` | `4px` | Tag / input radius |

---

## 🤖 Automated Migration Helper

The design system includes an automated migration helper script (`scripts/migrate-1.8-to-2.0.js`) that performs safe, regex-guided AST transforms with word-boundary protection.

### 1. Preview Changes (Dry Run)
Before modifying any files, run a dry-run analysis to preview planned upgrades:
```bash
make migrate ARGS="--dry-run"
# or
node scripts/migrate-1.8-to-2.0.js --dry-run
```

### 2. Apply Migration with Automatic Backup
Run the migration script on your repository or a specific directory:
```bash
make migrate
# or target a specific subfolder:
node scripts/migrate-1.8-to-2.0.js ./src
```
> **Safety Guarantee:** A timestamped backup is automatically created at `.migration-backups/backup-<timestamp>/` containing original files and a cryptographic `manifest.json`.

### 3. Rollback (Instant Revert)
If any layout or custom rule requires manual inspection, restore your previous state instantly:
```bash
make migrate ARGS="--rollback"
# or
node scripts/migrate-1.8-to-2.0.js --rollback
```

### 4. CI/CD Compliance Check
To ensure no 1.8.0 relics or deprecated CDN URLs exist in pull requests:
```bash
make migrate-check
# or
node scripts/migrate-1.8-to-2.0.js --check
```

---

## 📱 Native Kotlin Compose Theme Migration

In Jetpack Compose or Compose Multiplatform codebases:
```kotlin
// ❌ 1.8.0 Legacy Tokens
val bg = KartikAroraColors.LightBackground
val accent = KartikAroraColors.LightAccent

// ✅ 2.0.0 Semantic Tokens
val bg = KartikAroraColors.LightPaper
val signal = KartikAroraColors.LightSignal
```
*(Note: Legacy aliases like `LightBackground` and `LightAccent` are preserved in `Color.kt` to prevent compilation errors).*

---

## 🍏 Native SwiftUI Integration

To consume brand tokens in iOS and macOS projects:
1. Install the SwiftUI skill:
   ```bash
   npx skills install https://distribute.kartikarora.me/ai/kartikarora-swiftui.skill
   ```
2. Apply adaptive brand styling:
   ```swift
   import SwiftUI

   struct MyView: View {
       var body: some View {
           VStack {
               Text("Option 03 Studio")
                   .font(.brandTitle)
                   .foregroundColor(Color.brand.ink)
               BrandButton("Action", style: .primary) {
                   // action
               }
           }
           .brandStudioTheme() // or .brandTheme() for light/dark adaptive
       }
   }
   ```
