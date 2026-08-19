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
Duration: 0:02:00

This step demonstrates interactive button variants and rich InfoBoxes!

### Action Buttons
A button is created by adding the `{.buttonPrimary}` or `{.buttonSecondary}` tag attribute to a link:

[Read Documentation](https://klaat.kartikarora.me/docs/index.html){.buttonPrimary icon=menu_book}

[Send Feedback](mailto:dev&#64;kartikarora&#46;me){.buttonSecondary icon=mail}

Standard links without button attributes remain clean inline links:
Visit the official [documentation](https://klaat.kartikarora.me/docs/index.html) to learn more.

### InfoBoxes (Alerts)
InfoBoxes are created using blockquotes with attribute tags:

Positive InfoBox (Note/Tip):
> **Note:** Klaat now supports InfoBoxes natively! You can even put multiple lines inside. {.positive}

Negative InfoBox (Warning/Caution):
> **Warning:** Be careful to specify button tags explicitly for pill rendering. {.warning}

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
