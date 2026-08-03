#!/bin/bash
set -e

# Script to automate GraalVM native-image configuration generation for Klaat.
# This runs the application with the tracing agent across all major features.

echo "🚀 Starting GraalVM Tracing Agent for Klaat..."

# 1. Clean and build the project first
./gradlew clean buildDev

# Define executable path for dynamic command discovery
DEV_BIN="./build/install/klaat-dev/bin/klaat-dev"

# 2. Run the agent for specific base commands to capture actual execution logic
echo "📝 Running version command..."
./gradlew -Pagent run --args="version"

echo "📝 Running help command..."
./gradlew -Pagent run --args="help"

# 3. Discover subcommands dynamically and run the agent on their help screens
echo "🔍 Discovering subcommands dynamically..."
subcommands=$($DEV_BIN --help | awk '/Commands:/{flag=1;next}/^$/{flag=0}flag' | sed 's/^[[:space:]]*//' | grep -v '^$')

for cmd in $subcommands; do
    echo "📝 Running agent for: $cmd --help..."
    ./gradlew -Pagent run --args="$cmd --help"
done

# Create a temporary output directory
mkdir -p build/agent-test-output

echo "📝 Running export (Markdown to HTML)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f html"

echo "📝 Running export (Markdown to MD)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f md"

echo "📝 Running export (Markdown to Offline)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f offline"

# Serve command needs to be run and then stopped
echo "🌐 Running serve command (briefly)..."
# Use a custom port to avoid conflicts
./gradlew -Pagent run --args="serve -d examples -addr localhost:9876 --no-browser" &
SERVE_PID=$!
sleep 10
kill $SERVE_PID || true
echo "✅ Serve command stopped."

# 4. Tracing invalid inputs, error paths, and specific flags to maximize reflection coverage
echo "📝 Running invalid export (no arguments)..."
./gradlew -Pagent run --args="export" || true

echo "📝 Running invalid export (non-existent file)..."
./gradlew -Pagent run --args="export non_existent_file.md" || true

echo "📝 Running invalid export (invalid format)..."
./gradlew -Pagent run --args="export examples/simple-test.md -f invalid_format" || true

echo "📝 Running install-skills (project scope)..."
./gradlew -Pagent run --args="install-skills" || true

echo "📝 Running install-skills (global scope)..."
./gradlew -Pagent run --args="install-skills -g" || true

echo "📝 Running install-skills (invalid flag)..."
./gradlew -Pagent run --args="install-skills --invalid-flag" || true

echo "📝 Running update (no arguments)..."
./gradlew -Pagent run --args="update" || true

echo "📝 Running update (non-existent folder)..."
./gradlew -Pagent run --args="update non_existent_folder" || true

# 5. Copy metadata to resources
echo "📂 Copying agent metadata to resources..."
./gradlew metadataCopy --task run

echo "✨ Native image configurations updated in src/main/resources/META-INF/native-image/"
echo "🛠️ You can now build the native binary with: ./gradlew nativeCompile"
