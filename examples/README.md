# Klaat Example Codelabs

This directory contains example codelabs for testing, demonstration, and learning purposes.

## Examples

### 1. Simple Test (`simple-test.md`)

**Purpose:** Automated testing and CI/CD

A minimal codelab that covers basic features:
- Text formatting (bold, italic)
- Code blocks
- Lists
- Duration tracking

**Use for:**
- Quick smoke tests
- CI/CD pipeline validation
- Verifying basic export functionality

**Export:**
```bash
klaat export examples/simple-test.md -o output/
```

---

### 2. Comprehensive Example (`comprehensive-example.md`)

**Purpose:** Full feature demonstration and manual testing

A complete codelab showcasing ALL Klaat features:
- All text formatting options
- Multiple code blocks (Kotlin, JavaScript, Python, Bash)
- Bullet and numbered lists
- Images and media
- Info boxes (positive and negative)
- Tables
- Links and buttons
- Surveys
- FAQs
- Nested content
- Complex formatting

**Use for:**
- Feature demonstrations
- Manual testing of all capabilities
- Reference implementation
- Debugging rendering issues

**Export:**
```bash
klaat export examples/comprehensive-example.md -o output/
```

---

### 3. Demo Tutorial (`demo-tutorial.md`)

**Purpose:** User onboarding and demos

A realistic quickstart tutorial showing how to use Klaat:
- Clear learning objectives
- Step-by-step instructions
- Practical examples
- Best practices
- Tips and troubleshooting

**Use for:**
- Live demos
- User onboarding
- Documentation screenshots
- Training materials

**Export:**
```bash
klaat export examples/demo-tutorial.md -o output/
```

---

### 4. Google Docs Examples

**Purpose:** Demonstrate Google Docs format and integration

Google Docs versions of the examples above, demonstrating the full Google Docs format support:

#### Simple Test Codelab (Google Docs)
- **Document ID:** `129dvGyiAfsHb5kTU6hUWpflfu_eLfabH1Pj5zkaN580`
- **View:** [Open in Google Docs](https://docs.google.com/document/d/129dvGyiAfsHb5kTU6hUWpflfu_eLfabH1Pj5zkaN580)
- Minimal example with basic formatting

#### Klaat Quickstart (Google Docs)
- **Document ID:** `1Q0x_U07QUXHCsS6Kg1RJgnooiaTodNZsT7U1Z48EiRA`
- **View:** [Open in Google Docs](https://docs.google.com/document/d/1Q0x_U07QUXHCsS6Kg1RJgnooiaTodNZsT7U1Z48EiRA)
- Complete quickstart tutorial

#### Comprehensive Example (Google Docs)
- **Document ID:** `1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM`
- **View:** [Open in Google Docs](https://docs.google.com/document/d/1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM)
- Full-featured example with all formatting options

**Export from Google Docs:**
```bash
# Export Simple Test
klaat export 129dvGyiAfsHb5kTU6hUWpflfu_eLfabH1Pj5zkaN580 -o output/

# Export Quickstart
klaat export 1Q0x_U07QUXHCsS6Kg1RJgnooiaTodNZsT7U1Z48EiRA -o output/

# Export Comprehensive
klaat export 1h8Z_JEruw6MrlXoBjdVNH37GOb7u3Fn0A2sct-tnzYM -o output/
```

**Use for:**
- Testing Google Docs API integration
- Verifying OAuth authentication flow
- Demonstrating Google Docs format parsing
- Comparing Markdown vs Google Docs output

**Note:** First-time export from Google Docs requires OAuth authentication. The browser will open automatically for authorization.

---

## Testing All Examples

### Export All

```bash
# Export all examples
klaat export examples/*.md -o output/
```

### Serve Locally

```bash
# Serve all exported codelabs
klaat serve -d output/
```

Then visit:
- http://localhost:9090/simple-test-codelab/
- http://localhost:9090/comprehensive-codelab-example/
- http://localhost:9090/klaat-quickstart/

---

## Automated Testing

### Test Script

```bash
#!/bin/bash

echo "Testing Klaat Examples..."

# Clean output
rm -rf test-output/

# Test 1: Simple export
echo "Test 1: Exporting simple-test.md..."
klaat export examples/simple-test.md -o test-output/
if [ -f "test-output/simple-test-codelab/index.html" ]; then
    echo "✓ Simple test passed"
else
    echo "✗ Simple test failed"
    exit 1
fi

# Test 2: Comprehensive export
echo "Test 2: Exporting comprehensive-example.md..."
klaat export examples/comprehensive-example.md -o test-output/
if [ -f "test-output/comprehensive-codelab-example/index.html" ]; then
    echo "✓ Comprehensive test passed"
else
    echo "✗ Comprehensive test failed"
    exit 1
fi

# Test 3: Multiple exports
echo "Test 3: Batch export..."
klaat export examples/*.md -o test-output/
count=$(find test-output -name "index.html" | wc -l)
if [ $count -eq 3 ]; then
    echo "✓ Batch export passed ($count files)"
else
    echo "✗ Batch export failed (expected 3, got $count)"
    exit 1
fi

# Test 4: Asset copying
echo "Test 4: Checking assets..."
if [ -d "test-output/codelab-elements" ]; then
    asset_count=$(ls test-output/codelab-elements/*.js test-output/codelab-elements/*.css 2>/dev/null | wc -l)
    if [ $asset_count -ge 4 ]; then
        echo "✓ Assets copied ($asset_count files)"
    else
        echo "✗ Assets incomplete"
        exit 1
    fi
else
    echo "✗ Assets directory not found"
    exit 1
fi

echo ""
echo "All tests passed! ✓"
```

Save as `test-examples.sh` and run:
```bash
chmod +x test-examples.sh
./test-examples.sh
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Test Klaat Examples

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build Klaat
      run: ./gradlew installDist
    
    - name: Export Examples
      run: |
        ./build/install/klaat/bin/klaat export examples/*.md -o test-output/
    
    - name: Verify Output
      run: |
        test -f test-output/simple-test-codelab/index.html
        test -f test-output/comprehensive-codelab-example/index.html
        test -f test-output/klaat-quickstart/index.html
        test -d test-output/codelab-elements
    
    - name: Upload Artifacts
      uses: actions/upload-artifact@v3
      with:
        name: exported-codelabs
        path: test-output/
```

---

## Creating Your Own Examples

### Template Structure

```markdown
---
id: unique-codelab-id
summary: Brief description (1-2 sentences)
categories: Category1, Category2
environments: Web
status: Published
authors: Your Name
tags: tag1, tag2, tag3
---

# Codelab Title

## First Step
Duration: 2

Step content...

## Second Step
Duration: 3

More content...

## Conclusion
Duration: 1

Wrap up...
```

### Best Practices

1. **Use descriptive IDs:** `my-feature-tutorial` not `tutorial1`
2. **Set realistic durations:** Test how long steps actually take
3. **Keep steps focused:** One concept per step
4. **Add context:** Explain why, not just what
5. **Test thoroughly:** Export and review before sharing

---

## Troubleshooting

### Example doesn't export

Check YAML frontmatter syntax:
```bash
# Should have three dashes before and after
---
id: my-id
---
```

### Missing assets

Assets are automatically copied. Check:
```bash
ls output/codelab-elements/
# Should show: codelab-elements.css, codelab-elements.js, etc.
```

### Broken formatting

Validate your Markdown:
- Use H2 (`##`) for steps, not H1 (`#`)
- Close all code blocks with three backticks
- Escape special characters if needed

---

## Additional Resources

- [Klaat README](../README.md)
- [Original Format Guide](https://github.com/googlecodelabs/tools/FORMAT-GUIDE.md)
- [Markdown Guide](https://www.markdownguide.org/)
