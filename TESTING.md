# Testing Guide

This document describes the testing strategy and test coverage for the Klaat project.

## Overview

Klaat employs a comprehensive multi-level testing strategy to ensure reliability and correctness across all components. The test suite includes unit tests, integration tests, and end-to-end tests.

## Test Levels

### 1. Unit Tests

Unit tests verify individual components in isolation. These tests focus on:

- **Parser components** (`MarkdownParser`, `GoogleDocParser`)
  - Parsing markdown frontmatter and content
  - Extracting metadata from codelabs
  - Handling various markdown constructs (code blocks, lists, links, etc.)
  - Edge cases and error conditions

- **Renderer components** (`HtmlRenderer`, `MarkdownRenderer`, `OfflineRenderer`)
  - Template rendering
  - Content transformation
  - Asset handling and prefixing
  - Metadata JSON generation
  - Special elements (info boxes, code blocks, surveys, FAQs)

- **Data models** (`Codelab`, `Step`, `Node`)
  - Data structure validation
  - Metadata handling
  - Duration calculations

**Location:** `src/test/kotlin/me/kartikarora/klaat/parser/`, `src/test/kotlin/me/kartikarora/klaat/renderer/`

**Coverage:** ~130+ unit tests across parser and renderer components

### 2. Integration Tests

Integration tests verify how components work together:

- Parser → Renderer pipeline
- File I/O operations
- Template engine integration
- Asset copying and management

These tests use temporary directories and real file system operations to ensure components integrate correctly.

### 3. End-to-End Tests

End-to-end tests validate complete user workflows from the command-line interface:

#### ExportCommand Tests (`EndToEndTest.kt`)

Tests the complete export workflow:

```kotlin
✓ Export Markdown to HTML
✓ Export Markdown to Markdown format
✓ Export multiple codelabs simultaneously
✓ Export offline format with embedded assets
✓ Export with custom Google Analytics ID
✓ Export with custom asset prefix
✓ Export with environment flag (web, kiosk, etc.)
✓ Export with extra metadata (-extra flag)
✓ Export with pass_metadata flag
✓ Version command
```

**Location:** `src/test/kotlin/me/kartikarora/klaat/EndToEndTest.kt`

**Test Count:** 10 tests

#### ServeCommand Tests (`ServeCommandTest.kt`)

Tests the local preview server:

```kotlin
✓ Server starts and serves files correctly
✓ Server serves files from subdirectories
✓ Server handles custom ports
✓ Server handles invalid directory gracefully
```

**Location:** `src/test/kotlin/me/kartikarora/klaat/ServeCommandTest.kt`

**Test Count:** 3 tests

**Note:** Serve tests use background threads and HTTP clients to verify server behavior, with proper timeout handling and cleanup.

### 4. AI Agent Skills Evaluation (`evalSkills`)

Evaluates the autonomous invocation, triggering sensitivity, and compilation adherence of packaged AI agent skills (e.g. `klaat-markdown-author`) using deterministic compiler ground truth:

```bash
# Run evaluation benchmark (reads configuration and API key from evals.properties)
./gradlew evalSkills
```

**Configuration via `evals.properties`:**
The Gemini API key and evaluation settings are strictly configured via an `evals.properties` file in the project root:
```properties
geminiKey=your_gemini_api_key
model=gemini-3.8-flash
updateDocs=true
```
*(Note: `evals.properties` is included in `.gitignore` so secrets are never committed).*

**What it tests:**
- **Autonomous Trigger Recall:** Verifies whether an LLM agent inspecting the skill catalog decides to issue a `read_skill` tool call when given a relevant tutorial authoring request.
- **False-Positive Resistance:** Verifies whether the agent avoids triggering on unrelated distractor prompts (e.g. general READMEs, blog posts).
- **Deterministic Compiler Validation:** Passes agent markdown output directly into `MarkdownParser` to ensure YAML frontmatter, step durations, and callout nodes parse without errors.

## Test Fixtures

### Example Codelabs

The `examples/` directory contains sample codelabs used for testing and validation:

- `feature-showcase.md` - Complete interactive showcase featuring all Klaat capabilities
- `comprehensive-example.md` - Full-featured codelab demonstrating all supported elements
- `infobox-example.md` - Callout and alert box variants
- `images-example.md` - Embedded local and remote images

### Test Resources

Tests create synthetic codelabs with various features:

- YAML frontmatter with all metadata fields
- Multiple steps with duration
- Code blocks (with and without language)
- Lists (ordered and unordered)
- Links and images
- Info boxes (positive, negative, warning)
- Special elements (surveys, FAQs, download buttons)

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Classes

```bash
# Run only E2E tests
./gradlew test --tests "me.kartikarora.klaat.EndToEndTest"
./gradlew test --tests "me.kartikarora.klaat.ServeCommandTest"

# Run parser tests
./gradlew test --tests "me.kartikarora.klaat.parser.*"

# Run renderer tests
./gradlew test --tests "me.kartikarora.klaat.renderer.*"
```

### Run Specific Test Methods

```bash
./gradlew test --tests "me.kartikarora.klaat.EndToEndTest.test export markdown to HTML"
```

### Run Tests with Detailed Output

```bash
./gradlew test --info
```

### Generate Test Report

Test reports are automatically generated at:
```
build/reports/tests/test/index.html
```

Open this file in a browser to see detailed test results, including:
- Pass/fail status for each test
- Execution time
- Standard output and error logs
- Stack traces for failures

## Test Coverage

### Current Coverage

- **Total Tests:** 140+ tests
- **Unit Tests:** ~130 tests
- **End-to-End Tests:** 13 tests
- **Success Rate:** 100% (all tests passing)

### Coverage by Component

| Component | Test Coverage | Notes |
|-----------|---------------|-------|
| MarkdownParser | ✅ Comprehensive | All markdown elements tested |
| GoogleDocParser | ✅ Comprehensive | Document structure parsing |
| HtmlRenderer | ✅ Comprehensive | All node types and features |
| MarkdownRenderer | ✅ Comprehensive | Markdown output generation |
| OfflineRenderer | ✅ Comprehensive | Offline format with assets |
| ExportCommand | ✅ E2E Tested | CLI integration |
| ServeCommand | ✅ E2E Tested | HTTP server behavior |
| UpdateCommand | ⚠️ Manual Testing | Requires existing codelabs |
| VersionCommand | ✅ E2E Tested | Simple command |

## Testing Best Practices

### 1. Isolation

- Each test is independent and can run in any order
- Tests use `@TempDir` for isolated file system operations
- No shared state between tests

### 2. Cleanup

- Temporary directories are automatically cleaned up
- Background threads are properly interrupted and stopped
- No test artifacts left behind

### 3. Timeouts

- Long-running tests (serve command) have explicit timeouts
- Default test timeout: 2 minutes
- Server tests timeout: 15 seconds

### 4. Assertions

- Tests use descriptive assertion messages
- Output files are verified for existence and content
- HTTP responses are validated for status codes and content

### 5. Test Data

- Tests create minimal, focused test data
- No dependency on external services (except Google Docs integration)
- Markdown examples are self-contained

## Continuous Integration

Tests run automatically on:
- Every push to the repository
- Every pull request
- Release builds

GitHub Actions workflow (`.github/workflows/ci.yml`) runs:
```yaml
- Build project
- Run all tests
- Export example codelabs
- Verify build artifacts
```

## Known Limitations

### Google Docs Integration

Google Docs parsing is not fully tested in the automated test suite because it requires:
- Valid OAuth credentials
- Active Google Docs API access
- Internet connectivity

**Recommendation:** Manually test Google Docs export before releases.

### UpdateCommand

The update command requires existing codelab directories with metadata, making it difficult to test in isolation. It's currently validated through:
- Manual testing
- Real-world usage
- Integration with export command tests

## Adding New Tests

### Unit Test Template

```kotlin
@Test
fun `test feature description`() {
    // Given
    val input = createTestInput()
    
    // When
    val result = componentUnderTest.process(input)
    
    // Then
    assertTrue(result.isSuccess, "Should process successfully")
    assertEquals(expectedValue, result.value)
}
```

### E2E Test Template

```kotlin
@Test
fun `test command with specific flags`() {
    // Given
    val testFile = createTestFile()
    val args = arrayOf("command", "arg1", "-flag", "value")
    
    // When
    main(args)
    
    // Then
    val outputFile = File("expected/output/path")
    assertTrue(outputFile.exists(), "Output should be created")
    val content = outputFile.readText()
    assertTrue(content.contains("expected"), "Should contain expected content")
}
```

## Debugging Tests

### Enable Verbose Output

```bash
./gradlew test --info --stacktrace
```

### Run Single Test with Debugging

```bash
./gradlew test --tests "ClassName.test method" --debug-jvm
```

Then attach your IDE's debugger to port 5005.

### Check Test Logs

Test output is captured in:
```
build/reports/tests/test/classes/[ClassName].html
```

## Future Improvements

### Planned Enhancements

1. **Code Coverage Reporting**
   - Add JaCoCo plugin for coverage metrics
   - Target: 80%+ line coverage

2. **Performance Tests**
   - Measure export time for large codelabs
   - Benchmark rendering performance

3. **Mutation Testing**
   - Verify test quality with mutation testing (PIT)

4. **Google Docs Mock Tests**
   - Mock Google Docs API responses
   - Test parsing without real API calls

5. **UpdateCommand Tests**
   - Create test fixtures for update scenarios
   - Test metadata merging logic

## Contributing

When adding new features:

1. **Write tests first** (TDD approach recommended)
2. **Add unit tests** for new components
3. **Add E2E tests** for new commands or major features
4. **Update this document** if adding new test categories
5. **Ensure all tests pass** before submitting PR

```bash
# Before committing
./gradlew test
```

## Support

For test-related questions or issues:
- Check test reports in `build/reports/tests/test/`
- Review test source code for examples
- Open an issue on GitHub with test failure details
