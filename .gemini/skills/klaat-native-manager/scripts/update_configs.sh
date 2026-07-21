#!/bin/bash
set -e

# Script to automate GraalVM native-image configuration generation for Klaat.
# This runs the application with the tracing agent across all major features.

echo "🚀 Starting GraalVM Tracing Agent for Klaat..."

# 1. Build the project first
./gradlew buildDev

# 2. Run the agent for various commands
echo "📝 Running version command..."
./gradlew -Pagent run --args="version"

echo "📝 Running help command..."
./gradlew -Pagent run --args="help"

echo "📝 Running export help..."
./gradlew -Pagent run --args="export --help"

# Create a temporary output directory
mkdir -p build/agent-test-output

echo "📝 Running export (Markdown to HTML)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f html"

echo "📝 Running export (Markdown to MD)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f md"

echo "📝 Running export (Markdown to Offline)..."
./gradlew -Pagent run --args="export examples/simple-test.md -o build/agent-test-output -f offline"

echo "📝 Running serve help..."
./gradlew -Pagent run --args="serve --help"

echo "📝 Running update help..."
./gradlew -Pagent run --args="update --help"

# Serve command needs to be run and then stopped
echo "🌐 Running serve command (briefly)..."
# Use a custom port to avoid conflicts
./gradlew -Pagent run --args="serve -d examples -addr localhost:9876 --no-browser" &
SERVE_PID=$!
sleep 10
kill $SERVE_PID || true
echo "✅ Serve command stopped."

# 3. Copy metadata to resources
echo "📂 Copying agent metadata to resources..."
./gradlew metadataCopy --task run

echo "✨ Native image configurations updated in src/main/resources/META-INF/native-image/"
echo "🛠️ You can now build the native binary with: ./gradlew nativeCompile"
