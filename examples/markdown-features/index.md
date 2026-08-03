---
id: markdown-features-demo
summary: A comprehensive demonstration of klaat Markdown parsing features including buttons, infoboxes, and nested lists.
categories: Markdown, klaat, Demo
environments: Web
status: Published
authors: klaat team
feedback_link: https://github.com/kartikarora/klaat/issues
tags: demo, features
---

# Klaat Markdown Features Demo

## Buttons and InfoBoxes
Duration: 3

This step demonstrates the new Button and InfoBox features!

### Buttons
A button is created by placing a single link on a line by itself.

[Download Klaat](https://github.com/kartikarora/klaat)

It shouldn't work if there's other text on the line:
This is a [link](https://github.com/kartikarora/klaat).

### InfoBoxes (Alerts)
InfoBoxes are created using blockquotes.

Positive InfoBox (Note/Tip):
> **Note:** Klaat now supports InfoBoxes natively!
> You can even put multiple lines inside.

Negative InfoBox (Warning/Caution):
> **Warning:** Be careful not to use unsupported Markdown features.

## Nested Flow in Lists and Tables
Duration: 5

Here we demonstrate nested markdown features, like lists inside lists, code inside lists, and formatting inside tables.

### Nested Lists
* List Item 1
  * Nested Item A
  * Nested Item B
    ```kotlin
    fun helloKlaat() {
        println("Nested code blocks work perfectly!")
    }
    ```
* List Item 2
  > **Note:** An InfoBox inside a list!

### Tables with inline styling
Images and bold text now work properly inside table cells:

| Element | Example | Notes |
| :--- | :---: | --- |
| **Bold** inside table | This is **bold** text | Works! |
| Image inside table | ![Logo](https://github.com/kartikarora.png?size=50) | Works! |
| Code inside table | Use `klaat serve` | Works! |
