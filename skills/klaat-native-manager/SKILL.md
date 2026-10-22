---
name: klaat-native-manager
description: Comprehensive guide and automation toolkit for GraalVM Native Image Tracing Agent configuration generation, reflection/resource metadata discovery, native compilation, and release preparation for Klaat. Use when adding libraries, features, or preparing native release builds.
---

# Klaat Native Image Manager & Release Toolkit

A comprehensive guide and automation suite for managing GraalVM Native Image tracing, reflection and resource configuration discovery, native binary compilation, and release preparation in Klaat.

---

## 1. Native Image Architecture in Klaat

Klaat compiles into a standalone, ultra-fast native binary via GraalVM Native Image Ahead-Of-Time (AOT) compilation. Because AOT compilation performs static analysis of reachable code, dynamic runtime features—such as Java reflection, JNI, dynamic proxies, and resource loading—must be explicitly registered in configuration files located in:

```
src/main/resources/META-INF/native-image/me.kartikarora/klaat/
├── reflect-config.json
├── resource-config.json
├── jni-config.json
├── proxy-config.json
└── serialization-config.json
```

### Build-Time vs. Run-Time Initialization
- **Build-Time Initialized**: `kotlin`, `kotlinx`, `org.slf4j`, `ch.qos.logback`, `com.vladsch.flexmark`, `org.xml.sax`
- **Run-Time Initialized**: `io.ktor`, `io.netty`, `org.eclipse.jetty`

---

## 2. Tracing Agent Mechanics

The GraalVM Native Image Tracing Agent (`native-image-agent`) runs alongside a standard JVM execution. It intercepts all runtime calls to reflection, resource lookups, JNI, and class loading, generating the precise JSON configurations required by the native image compiler.

### Core Principles
1. **Dynamic Coverage**: The agent only records code paths that are actually executed. Every subcommand, output format (HTML, Markdown, Offline), error handling path, and network lifecycle (Ktor/Netty server) must be exercised.
2. **Incremental Merging (`config-merge-dir`)**: When running multiple commands in succession, the agent merges newly discovered metadata into existing configurations without overwriting prior entries.
3. **Graceful Shutdown**: The agent flushes recorded configurations to disk upon clean JVM termination. Background processes (such as `serve`) must receive standard termination signals (`SIGINT`/`SIGTERM`) to trigger hook execution and file flush.

---

## 3. Automated Configuration Workflow

Klaat includes an end-to-end configuration script that discovers all CLI subcommands, exercises all exporters, traces the local Ktor/Netty server, tests edge-case error paths, and merges the metadata into project resources.

### Running the Automation Script

```bash
bash skills/klaat-native-manager/scripts/update_configs.sh
```

### What the Script Executes
1. **Builds JVM Distribution**: Compiles `buildDev` to generate the latest binaries.
2. **Dynamic Subcommand Discovery**: Scans `klaat-dev --help` to dynamically discover and trace help screens for all subcommands (`export`, `serve`, `update`, `install-skills`, `version`, etc.).
3. **Format Coverage**: Runs exports for Markdown to HTML, Markdown, and Offline formats across test codelabs.
4. **Server Lifecycle Tracing**: Spawns `klaat serve` on a custom port for 10 seconds to trace Ktor/Netty event loops and network reflection, then cleanly shuts it down.
5. **Error & Fallback Handling**: Exercises invalid arguments, missing files, and unrecognised flags to capture exception-handling reflection paths.
6. **Metadata Copy**: Executes `./gradlew metadataCopy --task run` to merge and persist all configurations to `src/main/resources/META-INF/native-image/`.

---

## 4. Manual Tracing Workflow (Targeted Runs)

If you introduce a specific feature (e.g. Google Docs OAuth flow, new template rendering engine, or custom CLI flag), you can manually run targeted tracing sessions:

```bash
# 1. Run target command under tracing agent
./gradlew -Pagent run --args="export <target-doc-id> -f html"

# 2. Trace server on a specific port
./gradlew -Pagent run --args="serve -d examples -addr localhost:9876 --no-browser"

# 3. Copy captured metadata to resources
./gradlew metadataCopy --task run
```

---

## 5. Native Compilation Tasks

Once metadata configurations are updated, compile and test the native binary:

### Development Native Build (Quick Mode `-Ob` with Debug Symbols)
Fast compilation optimized for local iteration, enabling debug symbols (`debug = true`) and quick build mode (`-Ob`):
```bash
./gradlew nativeDev
# or
./gradlew nativeCompile
```

### Production Release Native Build (Optimized)
Fully optimized release binary (`debug = false`, full optimization, `-H:+PreserveFrameInformation`, and external symbol table generation via `-H:+CreateImageSymbolFile`):
```bash
./gradlew nativeRelease
```

Binary output location: `build/native/nativeCompile/klaat` (or `klaat-snapshot` for snapshot builds).

---

## 6. End-to-End Release Preparation Checklist

Follow this checklist before tagging a new release:

- [ ] **1. Run Unit Tests**: Ensure all unit and integration tests pass:
  ```bash
  ./gradlew test
  ```
- [ ] **2. Update Native Configurations**: Run the tracing agent across all features:
  ```bash
  bash skills/klaat-native-manager/scripts/update_configs.sh
  ```
- [ ] **3. Inspect Git Diff**: Review changes in `src/main/resources/META-INF/native-image/` to ensure configurations are clean and free of spurious entries. Remove any leftover `.lock` files.
- [ ] **4. Build Release Native Binary**:
  ```bash
  ./gradlew nativeRelease
  ```
- [ ] **5. Validate Native Binary Directly**: Test key CLI workflows directly with the native executable:
  ```bash
  ./build/native/nativeCompile/klaat version
  ./build/native/nativeCompile/klaat export examples/feature-showcase.md -o build/native-test-out -f html
  ./build/native/nativeCompile/klaat export examples/infobox-example.md -o build/native-test-out -f html
  ```
- [ ] **6. Version Verification**: Check that version numbers in `gradle/libs.versions.toml` and documentation (`README.md`, `DEVELOPMENT.md`) are aligned.

---

## 7. Troubleshooting & Best Practices

| Issue | Cause | Solution |
|---|---|---|
| `ClassNotFoundException` / `NoSuchMethodException` at runtime in native binary | Dynamic reflection not registered in `reflect-config.json` | Run `update_configs.sh` or a targeted `-Pagent` session exercising the failing code path. |
| Missing template/resource files | Resource file not included in `resource-config.json` | Add pattern to `src/main/resources/META-INF/native-image/me.kartikarora/klaat/resource-config.json` or run `metadataCopy`. |
| Out of Memory during `nativeCompile` | GraalVM AOT compilation requires significant RAM | Increase Gradle memory: `export GRADLE_OPTS="-Xmx8g -Xms4g"`. |
| Agent configurations not saved | JVM was force-killed (`SIGKILL` / `kill -9`) | Always use graceful termination (`SIGTERM` / `kill -15` / Ctrl+C) to allow agent shutdown hooks to flush files. |
| Leftover `.lock` files | Agent run was interrupted | Check and delete any `.lock` files in `src/main/resources/META-INF/native-image/`. |
