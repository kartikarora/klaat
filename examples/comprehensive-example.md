---
id: comprehensive-codelab-example
summary: A comprehensive example demonstrating all Klaat/Codelab features
categories: Tutorial, Demo, Testing
environments: Web, Kiosk
status: Published
authors: Klaat Team
tags: example, demo, testing, comprehensive
feedback_link: https://github.com/yourusername/klaat/issues
analytics_account: UA-XXXXX-Y
---

# Comprehensive Codelab Example

## Introduction
Duration: 2

Welcome! This codelab demonstrates **all features** supported by Klaat.

### What you'll learn

* Text formatting (bold, italic, code)
* Code blocks with syntax highlighting
* Lists (bullet and numbered)
* Images and media
* Info boxes (positive and negative)
* Links and buttons
* Surveys and interactive elements

### What you'll need

* A modern web browser
* 15 minutes
* Basic knowledge of Markdown

## Text Formatting
Duration: 3

You can use various text formatting options:

**Bold text** for emphasis

*Italic text* for subtle emphasis

`Inline code` for variable names or short commands

You can also combine them: **bold with *italic* inside**

### Special Characters

Test special characters: & < > " ' 

Test double curly braces: {{variable}}

## Code Blocks
Duration: 5

### Kotlin Example

```kotlin
fun main() {
    println("Hello, Codelab!")
    
    val numbers = listOf(1, 2, 3, 4, 5)
    numbers
        .filter { it % 2 == 0 }
        .map { it * 2 }
        .forEach { println(it) }
}
```

### JavaScript Example

```javascript
// Async/await example
async function fetchData(url) {
    try {
        const response = await fetch(url);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error:', error);
    }
}
```

### Python Example

```python
def fibonacci(n):
    """Generate Fibonacci sequence up to n terms"""
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b

# Print first 10 Fibonacci numbers
for num in fibonacci(10):
    print(num)
```

### Shell Commands

```bash
# Install dependencies
npm install

# Run tests
npm test

# Build for production
npm run build
```

## Lists and Structure
Duration: 2

### Bullet Lists

* First level item
* Another first level item
  * Nested item
  * Another nested item
* Back to first level

### Numbered Lists

1. First step
2. Second step
3. Third step
   1. Sub-step A
   2. Sub-step B
4. Fourth step

### Mixed Content

You can mix lists with other elements:

* **Step 1:** Install the CLI
  ```bash
  npm install -g klaat
  ```
* **Step 2:** Create a codelab
* **Step 3:** Export it
  ```bash
  klaat export my-codelab.md
  ```

## Images and Media
Duration: 3

### Basic Image

![Example diagram](https://via.placeholder.com/600x300/4F7DC9/ffffff?text=Codelab+Diagram)

### Image with Caption

![Architecture overview](https://via.placeholder.com/800x400/1e8e3e/ffffff?text=System+Architecture)

*Figure 1: System architecture showing the main components*

### Multiple Images

![Component A](https://via.placeholder.com/300x200/FF6F00/ffffff?text=Component+A)
![Component B](https://via.placeholder.com/300x200/7B1FA2/ffffff?text=Component+B)

## Positive Info Boxes
Duration: 2

Here's a positive info box for tips and best practices:

> **Note:** This is a tip!
> 
> Use positive info boxes to highlight:
> * Best practices
> * Pro tips
> * Time-saving shortcuts
> * Success indicators

> **Pro Tip:** You can nest formatting inside info boxes:
> * **Bold text**
> * `Code snippets`
> * [Links](https://example.com)

## Negative Info Boxes
Duration: 2

Here's a negative info box for warnings:

> **Warning:** Pay attention to this!
> 
> Use negative info boxes for:
> * Warnings about common mistakes
> * Breaking changes
> * Security considerations
> * Prerequisites

> **Caution:** Make sure you complete the previous steps before proceeding.

## Links and References
Duration: 2

### Basic Links

Learn more at the [official documentation](https://github.com/googlecodelabs/tools).

### Multiple Links

Check out these resources:
* [Google Codelabs](https://codelabs.developers.google.com/)
* [Markdown Guide](https://www.markdownguide.org/)
* [Klaat Repository](https://github.com/yourusername/klaat)

### Inline Links

You can read more about this in the [API documentation](https://example.com/api) or check the [troubleshooting guide](https://example.com/troubleshooting).

## Download Buttons
Duration: 1

You can download the starter project:

[Download Starter Code](https://github.com/yourusername/klaat/archive/main.zip)

Or clone the repository:

```bash
git clone https://github.com/yourusername/klaat.git
```

## Tables
Duration: 2

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `port` | number | 9090 | Server port |
| `host` | string | localhost | Server host |
| `format` | string | html | Output format |
| `ga` | string | - | Analytics ID |

### Comparison Table

| Feature | Klaat | Original |
|---------|-------|----------|
| Markdown support | ✅ | ✅ |
| Google Docs | ✅ | ✅ |
| Kotlin-based | ✅ | ❌ |
| Active development | ✅ | ❌ |

## Interactive Survey
Duration: 1

Before we continue, let's check your experience level:

**What's your experience with codelabs?**

- [ ] First time using codelabs
- [ ] Used codelabs a few times
- [ ] Regular codelab user
- [ ] Codelab author

## Frequently Asked Questions
Duration: 2

### Common Issues

**Q: How do I export a codelab?**

A: Use the export command:
```bash
klaat export your-codelab.md -o output/
```

**Q: Can I use custom styling?**

A: Yes! The generated HTML uses standard CSS classes you can override.

**Q: How do I serve codelabs locally?**

A: Use the serve command:
```bash
klaat serve -d output/
```

### Additional Resources

For more help, check:
* [GitHub Issues](https://github.com/yourusername/klaat/issues)
* [Stack Overflow](https://stackoverflow.com/questions/tagged/codelabs)
* [Community Forum](https://example.com/forum)

## Advanced Code Features
Duration: 4

### Code with Filename

**main.kt**
```kotlin
package com.example

fun main() {
    println("Hello from Klaat!")
}
```

### Multiple Code Blocks

**server.js**
```javascript
const express = require('express');
const app = express();

app.get('/', (req, res) => {
    res.send('Hello World!');
});

app.listen(3000);
```

**test.js**
```javascript
const request = require('supertest');
const app = require('./server');

describe('GET /', () => {
    it('responds with Hello World', async () => {
        const response = await request(app).get('/');
        expect(response.text).toBe('Hello World!');
    });
});
```

## Nested Content
Duration: 3

### Complex Nesting

You can nest various elements:

1. **Install Dependencies**
   
   First, install the required packages:
   ```bash
   npm install express body-parser
   ```
   
   > **Note:** Make sure you have Node.js 14+ installed

2. **Configure the Server**
   
   Create a configuration file:
   ```javascript
   module.exports = {
       port: 3000,
       host: 'localhost'
   };
   ```

3. **Start the Application**
   
   Run the server:
   ```bash
   npm start
   ```
   
   You should see:
   ```
   Server running on http://localhost:3000
   ```

## Testing Checklist
Duration: 2

### Verification Steps

Complete these steps to verify your setup:

- [ ] Klaat is installed
- [ ] Dependencies are available
- [ ] Server starts successfully
- [ ] Browser opens automatically
- [ ] Codelab renders correctly
- [ ] All assets load properly
- [ ] Navigation works
- [ ] Code highlighting works
- [ ] Images display
- [ ] Links are clickable

## What You've Learned
Duration: 1

Congratulations! You've explored all Klaat features:

### Content Features
* ✅ Text formatting (bold, italic, code)
* ✅ Headings (H2, H3, H4)
* ✅ Code blocks with syntax highlighting
* ✅ Lists (bullet and numbered)
* ✅ Images
* ✅ Links and buttons

### Special Elements
* ✅ Info boxes (positive and negative)
* ✅ Tables
* ✅ Surveys
* ✅ FAQs
* ✅ Complex nested content

### Technical Features
* ✅ Multiple programming languages
* ✅ Shell commands
* ✅ Proper escaping
* ✅ Duration tracking
* ✅ Step organization

## Next Steps
Duration: 1

Now that you understand all features, you can:

### Create Your Own Codelabs

1. Create a new markdown file
2. Add YAML frontmatter with metadata
3. Write your content using H2 for steps
4. Export with Klaat
5. Share with your audience

### Contribute

Help improve Klaat:
* Report bugs on [GitHub](https://github.com/yourusername/klaat/issues)
* Submit pull requests
* Share your codelabs
* Improve documentation

### Learn More

* [Klaat Documentation](https://github.com/yourusername/klaat/README.md)
* [Codelab Format Guide](https://github.com/googlecodelabs/tools/FORMAT-GUIDE.md)
* [Markdown Cheat Sheet](https://www.markdownguide.org/cheat-sheet/)

Thank you for completing this comprehensive example! 🎉
