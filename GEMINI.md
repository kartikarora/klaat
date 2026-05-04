# Klaat - Kodelabs As A Thing

Klaat is a Kotlin-based CLI tool for authoring and serving interactive codelabs, designed as a modern, drop-in replacement for the archived Google Codelabs Tools (`claat`).

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
