---
name: klaat-native-manager
description: Automates GraalVM native-image configuration generation and native binary compilation for the Klaat project. Use when adding new libraries, features, or classes that require reflection or resource configuration updates for native compilation.
---

# Klaat Native Manager

This skill simplifies the process of updating GraalVM native-image configurations and building native binaries for Klaat.

## Workflow

When the codebase changes (especially adding new dependencies or reflection-heavy code), follow these steps:

1.  **Update Configurations**: Run the automation script to trigger the GraalVM Tracing Agent across all major features.
2.  **Verify Changes**: Check the updated JSON files in `src/main/resources/META-INF/native-image/me.kartikarora/klaat/`.
3.  **Build Native Binary**: Compile the production-ready native binary.

## Task: Update Native Image Configs

Use the bundled script to run Klaat with the tracing agent. This script executes multiple commands (version, help, export, serve) to ensure maximum coverage for reflection and resource discovery.

```bash
# Run the configuration update script
bash klaat-native-manager/scripts/update_configs.sh
```

**What this script does:**
- Builds the JVM distribution (`buildDev`).
- Runs the app with `-Pagent` for:
    - `version`
    - `help`
    - `export --help`
    - `export` (Markdown to HTML, MD, and Offline formats)
    - `serve --help`
    - `update --help`
    - `serve` (runs for 10 seconds to capture Ktor/Netty reflections)
- Merges and copies the captured metadata to the project resources using `./gradlew metadataCopy`.

## Task: Build Native Binary

Once the configurations are updated, build the native binary for your platform.

### Development Build (Faster)
```bash
./gradlew nativeDev
```

### Production Build (Optimized)
```bash
./gradlew nativeProd
```

The resulting binary will be located at `build/native/nativeCompile/klaat`.

## Troubleshooting

- **Missing Reflections**: If the native binary fails with `ClassNotFoundException` or similar, ensure you've run the `update_configs.sh` script and that the specific code path was executed.
- **Google Docs Reflection**: If you added new Google API features, you may need to manually run an export with a real Google Doc ID while the agent is active to capture those specific reflections.
- **Memory Issues**: Native image compilation is memory-intensive. If it fails, try increasing Gradle's heap size: `export GRADLE_OPTS="-Xmx8g"`.
