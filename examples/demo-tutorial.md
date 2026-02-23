---
id: klaat-quickstart
summary: Get started with Klaat in 10 minutes
categories: Tutorial, Getting Started
environments: Web
status: Published
authors: Klaat Team
tags: quickstart, tutorial, beginner
feedback_link: https://github.com/yourusername/klaat/issues
---

# Klaat Quickstart

## Welcome!
Duration: 1

Welcome to Klaat! In this 10-minute tutorial, you'll learn how to:

* Create a codelab using Markdown
* Export it to HTML
* Serve it locally
* Share it with others

Let's get started! 🚀

## Prerequisites
Duration: 2

Before you begin, make sure you have:

### Required

* Java 21 or higher
* A text editor
* A modern web browser

### Installation

Download and install Klaat:

```bash
# Clone the repository
git clone https://github.com/yourusername/klaat.git
cd klaat

# Build the project
./gradlew installDist

# Add to PATH (optional)
export PATH=$PATH:$(pwd)/build/install/klaat/bin
```

Verify installation:

```bash
klaat version
```

> **Note:** You can also run Klaat via Gradle: `./gradlew run --args="version"`

## Create Your First Codelab
Duration: 3

Create a new file called `my-first-codelab.md`:

```markdown
---
id: my-first-codelab
summary: My first awesome codelab
categories: Tutorial
environments: Web
status: Published
authors: Your Name
---

# My First Codelab

## Introduction
Duration: 2

Welcome to my codelab!

## Step 1
Duration: 3

Your content here...

## Conclusion
Duration: 1

Congratulations!
```

### Key Elements

* **YAML Frontmatter:** Metadata at the top
* **H1 Heading:** Codelab title
* **H2 Headings:** Individual steps
* **Duration:** Time estimate for each step

## Export to HTML
Duration: 2

Export your codelab:

```bash
klaat export my-first-codelab.md -o output/
```

This creates:

```
output/
├── codelab-elements/    # Shared assets
│   ├── codelab-elements.css
│   ├── codelab-elements.js
│   └── ...
└── my-first-codelab/    # Your codelab
    ├── index.html
    └── codelab.json
```

### Options

```bash
# Specify format
klaat export tutorial.md -f html

# Set output directory
klaat export tutorial.md -o /path/to/output

# Add Google Analytics
klaat export tutorial.md -ga UA-XXXXX-Y

# Use CDN for assets
klaat export tutorial.md -p https://cdn.example.com/
```

## Preview Locally
Duration: 2

Start the local server:

```bash
klaat serve -d output/
```

This will:
* Start a server on `http://localhost:9090`
* Automatically open your browser
* Serve all codelabs in the output directory

### Server Options

```bash
# Custom port
klaat serve -d output/ -p 8080

# Custom host
klaat serve -d output/ -h 0.0.0.0

# Don't open browser
klaat serve -d output/ --no-browser
```

> **Tip:** Press Ctrl+C to stop the server

## Update Existing Codelabs
Duration: 1

Made changes to your codelab? Update it:

```bash
# Update all codelabs in a directory
klaat update output/

# Update specific codelabs
klaat update output/my-first-codelab
```

This re-exports codelabs from their original sources while preserving settings.

## Share Your Codelab
Duration: 2

### Option 1: Static Hosting

Deploy to any static hosting service:

* **GitHub Pages:** Free hosting for public repos
* **Netlify:** Easy continuous deployment
* **Firebase Hosting:** Google's hosting platform
* **AWS S3:** Scalable cloud storage

Example for GitHub Pages:

```bash
# In your output directory
git init
git add .
git commit -m "Add codelabs"
git branch -M gh-pages
git remote add origin https://github.com/username/codelabs.git
git push -u origin gh-pages
```

### Option 2: Self-Hosted

Use any web server:

```bash
# Python
python3 -m http.server 8000

# Node.js
npx http-server

# PHP
php -S localhost:8000
```

## Advanced Features
Duration: 3

### Multiple Formats

Export to Markdown:

```bash
klaat export tutorial.md -f md
```

### Batch Export

Export multiple codelabs at once:

```bash
klaat export codelab1.md codelab2.md codelab3.md -o output/
```

### Google Docs Support

Export from Google Docs:

```bash
# Use the document ID from the URL
klaat export 1ABC_YOUR_DOC_ID -o output/
```

> **Note:** First-time use requires Google OAuth authentication

### Custom Asset CDN

Use your own CDN for assets:

```bash
klaat export tutorial.md -p https://your-cdn.com/codelab-elements
```

## Tips and Best Practices
Duration: 2

### Writing Tips

* **Keep steps focused:** One concept per step
* **Use descriptive titles:** Make navigation easy
* **Set realistic durations:** Help users plan their time
* **Add images:** Visual aids improve understanding
* **Include code examples:** Show, don't just tell

### Organization

```
project/
├── codelabs/           # Source markdown files
│   ├── tutorial-1.md
│   ├── tutorial-2.md
│   └── tutorial-3.md
├── output/             # Generated HTML
└── README.md
```

### Testing

Always test your codelabs:

1. Export locally
2. Review in browser
3. Check all links
4. Verify code blocks
5. Test on mobile

## What's Next?
Duration: 1

Congratulations! You now know how to:

* ✅ Install Klaat
* ✅ Create codelabs in Markdown
* ✅ Export to HTML
* ✅ Serve locally
* ✅ Share with others

### Continue Learning

* Explore the [comprehensive example](./comprehensive-example.md)
* Read the [FORMAT-GUIDE](https://github.com/googlecodelabs/tools/FORMAT-GUIDE.md)
* Check the [Klaat README](../README.md)
* Join the community

### Get Help

* [GitHub Issues](https://github.com/yourusername/klaat/issues)
* [Discussions](https://github.com/yourusername/klaat/discussions)
* [Original Codelabs Tools](https://github.com/googlecodelabs/tools)

Happy codelab authoring! 🎉
