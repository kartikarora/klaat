---
name: graalvm-native-agent
description: Instrument Java/Kotlin applications with the GraalVM Native Image Tracing Agent to discover, capture, and merge dynamic reflection, JNI, resource, and serialization configurations.
---

# GraalVM Native Image Tracing Agent Skill (Local Copy)

This local skill provides automation scripts and guidelines for running the codebase under the GraalVM Native Image Tracing Agent. This is essential to collect reflection, resource access, and JNI configs and merge them into the codebase resources to prevent native binary crashes.

---

## 1. Automated Execution (Recommended)

To run a full, automated trace across all CLI commands (`version`, `help`, `export`, `update`, `serve`), execute the local helper script:

```bash
# Set GRAALVM_HOME to point to your GraalVM distribution
export GRAALVM_HOME=/Users/kartikarora/.sdkman/candidates/java/25.0.2-graalce

# Run the automated tracer
./.gemini/skills/graalvm-native-agent/scripts/run-agent.sh
```

This script:
1. Compiles the project using `installDist` to generate classpaths.
2. Sequential executes `version`, `help`, `export`, and `update` commands under the tracing agent.
3. Launches the `serve` engine, waits for Ktor initialization, and cleanly shuts it down.
4. Merges all dynamic trace files into `src/main/resources/META-INF/native-image/me.kartikarora/klaat/`.
5. Removes temporary lock files automatically.

---

## 2. Manual Tracing Steps

If you need to trace custom commands manually, execute the distribution binary using the following configurations:

### Step 1: Install Distribution Binaries
```bash
./gradlew installDist
```

### Step 2: Run Agent Merge
Point `config-merge-dir` to the native-image resources folder:
```bash
JAVA_HOME=$GRAALVM_HOME \
JAVA_OPTS="-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/me.kartikarora/klaat" \
build/install/klaat-dev/bin/klaat-dev export -css <url> examples/demo-tutorial.md
```

---

## 3. Best Practices & Troubleshooting

Refer to the local [graalvm-reference.md](file:///.gemini/skills/graalvm-native-agent/references/graalvm-reference.md) for detailed reference docs, options, and troubleshooting strategies.
