# Contributing to Klaat

Thank you for your interest in contributing to Klaat! Please read this guide before opening an issue or submitting a pull request.

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Scope & Before You Start

- **Bug fixes, documentation improvements, and parser/renderer enhancements** are always welcome.
- **Large features or architectural changes:** Please open an issue first to discuss your proposal before investing significant time in implementation.
- **Supported native targets:** macOS (ARM64) and Linux (x64, ARM64).

---

## Prerequisites

- **GraalVM JDK 25** (matching [`.sdkmanrc`](.sdkmanrc), e.g., `25.2.4-graalce` via [SDKMAN!](https://sdkman.io/)):
  ```bash
  sdk env install
  ```
- **Git**
- **Gitleaks** (recommended for local secret scanning):
  ```bash
  brew install gitleaks
  ```
- **Google OAuth Credentials** *(optional)*: Only needed if you are working on Google Docs export features. If omitted, Gradle uses [`gcp_credentials.fake.json`](gcp_credentials.fake.json) (a fake placeholder credential that allows building/testing without a GCP project, but **cannot read any Google Doc**). See [GCP_CREDENTIALS.md](GCP_CREDENTIALS.md).

---

## Development Workflow

1. **Fork** the repository and create your branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feat/your-feature-name
   ```
2. **Run Klaat locally** via Gradle:
   ```bash
   ./gradlew run --args="export --help"
   ```
3. **Build the JVM distribution**:
   ```bash
   ./gradlew buildDev
   ```

See [DEVELOPMENT.md](DEVELOPMENT.md) for additional build variants and troubleshooting tips.

---

## Testing & TDD Policy

- **Bug fixes:** Write a failing unit test reproducing the expected behavior **before** implementing the fix.
- **Parser/renderer changes:** Add unit tests under `src/test/kotlin/me/kartikarora/klaat/parser/` or `src/test/kotlin/me/kartikarora/klaat/renderer/`.
- **CLI changes:** Add or update tests in `src/test/kotlin/me/kartikarora/klaat/EndToEndTest.kt`.
- Run the full test suite before opening a PR:
  ```bash
  ./gradlew test
  ```

See [TESTING.md](TESTING.md) for full testing conventions.

---

## GraalVM Native Image Guidelines

Klaat compiles to a standalone native binary using GraalVM Native Image. Any change that introduces new libraries, reflection, or classpath resources **must** keep native-image metadata up to date:

1. Run the tracing agent script to update configs under `src/main/resources/META-INF/native-image/`:
   ```bash
   bash skills/klaat-native-manager/scripts/update_configs.sh
   ```
2. Verify the native binary compiles cleanly:
   ```bash
   ./gradlew nativeCompile
   ```

---

## Secret Scanning (Gitleaks)

Never commit `gcp_credentials.json`, OAuth tokens, or personal API keys. Before pushing, run:

```bash
gitleaks detect --no-banner --redact
```

Or scan staged changes before committing:

```bash
gitleaks protect --staged --no-banner --redact
```

---

## Commit & Pull Request Guidelines

- Follow **Conventional Commits** (e.g., `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`).
- Target the **`develop`** branch for pull requests.
- Ensure CI (`./gradlew test` and `Gitleaks Secret Scan`) passes on your PR.

---

## License

By contributing to Klaat, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
