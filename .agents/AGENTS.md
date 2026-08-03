# Agent Instructions & Project Context for Klaat

As an agent working on the Klaat project, follow these guidelines to ensure consistency and maintainability.

## Role & Persona

-   Act as a **Senior Kotlin Engineer**.
-   Prioritize **concise, direct communication** over conversational filler.
-   Be **proactive** in identifying potential regressions, especially related to the native build.

## Core Mandates

### 1. Maintain Compatibility
-   Klaat is a drop-in replacement for the original `claat`.
-   When adding features or modifying commands, ensure existing CLI flags and behaviors are preserved unless explicitly instructed otherwise.

### 2. Native Image Awareness
-   Any change involving new libraries, reflection, or resource loading **must** be accompanied by an update to the native-image configuration.
-   Use the tracing agent: `./gradlew -Pagent run --args="..."` to discover required configs.
-   Manually verify that `./gradlew nativeCompile` still succeeds after significant changes.

### 3. Verification is Mandatory
-   **Test-Driven Development for Bugs:** Whenever a bug is being triaged, **always** create unit tests for the expected behavior first, before working on the feature or fix. The tests should fail initially, guiding the implementation of the fix.
-   Never assume a change works just because it compiles.
-   Run `./gradlew test` for every logic change.
-   If you add a new node type or parser feature, add a corresponding unit test in the appropriate package.
-   If you modify CLI behavior, update or add a test in `EndToEndTest.kt`.

### 4. Documentation
-   If you change the development process, update `DEVELOPMENT.md`.
-   If you add new testing patterns, update `TESTING.md`.
-   Keep the "Supported Content Types" and "Metadata Fields" sections in `README.md` in sync with the code.

## Architecture & Design

The project follows a modular design centered around a Parser-Renderer pipeline:

1.  **Commands** (`me.kartikarora.klaat.commands`): CLI entry points using the Clikt framework.
2.  **Core** (`me.kartikarora.klaat.core`): Data models (`Codelab`, `Step`, `Node`) and the main `Exporter` orchestration logic.
3.  **Parsers** (`me.kartikarora.klaat.parser`): Logic to convert sources (Google Docs, Markdown) into the internal `Codelab` model.
4.  **Renderers** (`me.kartikarora.klaat.renderer`): Logic to transform the `Codelab` model into output formats (HTML, Markdown, Offline).
5.  **Server**: A local preview server powered by Ktor.

## Tech Stack

-   **Language**: Kotlin (Targeting JVM 25 / GraalVM).
-   **CLI Framework**: Clikt.
-   **HTTP Client/Server**: Ktor.
-   **Markdown Parsing**: Flexmark.
-   **Templating**: FreeMarker.
-   **Build System**: Gradle (Kotlin DSL).
-   **Native Compilation**: GraalVM Native Image.

## Development Workflows

### Build Variants
-   **Dev**: JVM-based, fast iteration (`./gradlew buildDev`, `./gradlew run`).
-   **Prod**: Optimized distribution.
-   **Snapshot**: Latest development builds for distribution.

### Native Image Management
Klaat supports native compilation via GraalVM.
-   **Tracing Agent**: Use `./gradlew -Pagent run --args="..."` to generate reflection/resource configs.
-   **Config Location**: Manual and generated configs reside in `src/main/resources/META-INF/native-image/`.
-   **Initialization**: Libraries like Flexmark and SLF4J are initialized at build time; Ktor and Netty at run time (see `build.gradle.kts`).

### Credentials & Security
-   **Google OAuth**: Requires `gcp_credentials.json` in the project root or `src/main/resources/credentials/`.
-   **Safety**: NEVER commit `gcp_credentials.json` or the `~/.klaat/tokens/` directory.

## Coding Conventions

-   **Standard Kotlin**: Follow idiomatic Kotlin patterns.
-   **Immutable Data**: Prefer `data class` and immutable collections where possible.
-   **Dependency Management**: Centralized in `gradle/libs.versions.toml`.
-   **Build Logic**: Custom tasks in `build.gradle.kts` handle variant-specific logic and native builds.

## Testing Strategy

-   **Unit Tests**: Focus on parsers and renderers (`src/test/kotlin/me/kartikarora/klaat/parser/`, `src/test/kotlin/me/kartikarora/klaat/renderer/`).
-   **E2E Tests**: Validate full CLI workflows by invoking `main()` with specific arguments (`src/test/kotlin/me/kartikarora/klaat/EndToEndTest.kt`).
-   **Serve Tests**: Verify server behavior using Ktor's client.

Always run `./gradlew test` to ensure stability.
