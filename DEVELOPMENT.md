# Local Development Guide

## Prerequisites

- **GraalVM JDK 21** (for native builds)
  ```bash
  # Install via SDKMAN
  sdk install java 21-graalce
  sdk use java 21-graalce
  ```

  Or download from: https://www.graalvm.org/downloads/

- **Google OAuth Credentials** in `gcp_credentials.json` (root) or `src/main/resources/credentials/gcp_credentials.json`:
  ```json
  {
    "installed": {
      "client_id": "your-client-id",
      "client_secret": "your-client-secret",
      "redirect_uris": ["http://localhost"],
      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
      "token_uri": "https://oauth2.googleapis.com/token"
    }
  }
  ```

## Build Commands

### JVM Development (Faster iteration)

```bash
# Build JVM distribution
./gradlew buildDev

# Run directly (fastest for development)
./gradlew run --args="export --help"

# Run with JVM
./build/install/klaat/bin/klaat --help
```

### Native Binary Development

```bash
# Build native binary (takes 5-10 minutes)
./gradlew nativeCompile

# Run the native binary
./build/native/nativeCompile/klaat --help

# Build and run in one command
./gradlew runNative -Pargs="export --help"
```

### Native Binary Tasks

- `./gradlew nativeDev` - Build development native binary
- `./gradlew nativeProd` - Build production native binary
- `./gradlew nativeCompile` - Standard native compile
- `./gradlew runNative` - Build and run native binary

### Quick Build Mode

Local native builds use `-Ob` (quick build) for faster iteration.
CI builds use full optimization for production binaries.

## Generating Native Image Configs

GraalVM needs to know about reflection, resources, and JNI usage. You can generate these configs:

```bash
# Run with the tracing agent
./gradlew -Pagent run --args="export --help"

# This generates configs in build/native/agent-output/

# Copy them to src/main/resources/META-INF/native-image/
cp build/native/agent-output/run/*.json src/main/resources/META-INF/native-image/
```

## Testing Native Builds

```bash
# Build native binary
./gradlew nativeCompile

# Test it works
./build/native/nativeCompile/klaat version
./build/native/nativeCompile/klaat export --help

# Check binary size
ls -lh build/native/nativeCompile/klaat

# Check no JRE dependency
ldd build/native/nativeCompile/klaat  # Linux
otool -L build/native/nativeCompile/klaat  # macOS
```

## Common Issues

### Build Fails with Missing Reflection Config

If native build fails with errors about missing classes, you need to update reflection configs:

1. Run with agent: `./gradlew -Pagent run --args="your command"`
2. Copy generated configs to `src/main/resources/META-INF/native-image/`
3. Rebuild: `./gradlew nativeCompile`

### Build is Slow

- First native build takes 5-10 minutes (builds entire dependency graph)
- Incremental builds are faster
- Use JVM builds (`./gradlew run`) for rapid iteration
- Only test native builds when needed

### Memory Issues

GraalVM native-image needs lots of memory. If you get OOM errors:

```bash
export GRADLE_OPTS="-Xmx8g"
./gradlew nativeCompile
```

## Recommended Workflow

1. **Daily development**: Use JVM builds (`./gradlew run`)
2. **Testing native features**: Use `./gradlew nativeCompile` periodically
3. **Before commits**: Test native build works
4. **CI/CD**: Handles production native builds for all platforms (`IS_SNAPSHOT=true` for develop branch)

### Google Docs API Credentials

For local development, you can place `gcp_credentials.json` in:
- The project root directory
- `src/main/resources/credentials/gcp_credentials.json`

The project root is recommended for local development as it's easier to manage and already in `.gitignore`.

## IDE Setup

### IntelliJ IDEA

1. Import as Gradle project
2. Set Project SDK to GraalVM 21
3. Enable Gradle auto-import
4. Run configurations will use JVM (fast)
5. For native testing, use terminal: `./gradlew nativeCompile`

### VS Code

Install extensions:
- Kotlin
- Gradle for Java

Use integrated terminal for native builds.
