#!/bin/bash
# run-agent.sh
# Automated GraalVM Native Image Tracing Agent runner and merger.

set -e

# 1. Validation
if [ -z "$GRAALVM_HOME" ]; then
  echo "Error: GRAALVM_HOME environment variable is not set."
  echo "Please set it using: export GRAALVM_HOME=/path/to/graalvm-jdk"
  exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Starting Tracing Agent Execution"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Set up local paths
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
TARGET_DIR="${PROJECT_ROOT}/src/main/resources/META-INF/native-image/me.kartikarora/klaat"
APP_BIN="${PROJECT_ROOT}/build/install/klaat-dev/bin/klaat-dev"

# 2. Build local distribution
echo "📦 Assembling distribution binaries..."
cd "$PROJECT_ROOT"
./gradlew installDist -q

# 3. Setup Agent Options
AGENT_OPTS="-agentlib:native-image-agent=config-merge-dir=${TARGET_DIR}"

# Export Java options
export JAVA_HOME="$GRAALVM_HOME"
export JAVA_OPTS="$AGENT_OPTS"

# 4. Trace version, help, export, and update commands
echo "🔍 Tracing 'version' command..."
"$APP_BIN" version > /dev/null

echo "🔍 Tracing 'help' command..."
"$APP_BIN" help > /dev/null

echo "🔍 Tracing 'export' command..."
"$APP_BIN" export -css https://cdn.prod.website-files.com/62f44d0b3912362aa0c68151/css/bilue.webflow.shared.2892f2836.min.css examples/demo-tutorial.md > /dev/null

echo "🔍 Tracing 'update' command..."
# update will run and output metadata tracing paths even if docs authorization fails
"$APP_BIN" update klaat-quickstart > /dev/null || true

# 5. Trace server startup & Ktor routing
echo "🔍 Tracing 'serve' command (Ktor server)..."
# Run serve in background on a separate port so it doesn't conflict
"$APP_BIN" serve -d klaat-quickstart -addr localhost:9099 --no-browser &
SERVE_PID=$!

# Let the server spin up, establish Netty handles, and map routes
sleep 6

# Gracefully terminate the server to let JVM flush tracing agent outputs
echo "🛑 Gracefully stopping serve engine..."
kill $SERVE_PID
wait $SERVE_PID || true

# 6. Cleanup agent lock files
if [ -f "${TARGET_DIR}/.lock" ]; then
  rm "${TARGET_DIR}/.lock"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Traces Merged Successfully!"
echo "  Location: src/main/resources/META-INF/native-image/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
