package me.kartikarora.klaat.parser

import com.google.api.services.docs.v1.model.Document
import com.google.api.services.docs.v1.model.EmbeddedObject
import com.google.api.services.docs.v1.model.ImageProperties
import com.google.api.services.docs.v1.model.InlineObject
import com.google.api.services.docs.v1.model.InlineObjectElement
import com.google.api.services.docs.v1.model.InlineObjectProperties
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class PosterAutoDetectionTest {

    private val parser = MarkdownParser()
    private val gDocParser = GoogleDocParser()

    @Test
    fun `test poster automatically set to first image in markdown`() {
        val markdown = """
            # Title
            
            ## Step 1
            
            Some text.
            
            ![First Image](https://example.com/first.png)
            
            ![Second Image](https://example.com/second.png)
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("https://example.com/first.png", codelab.metadata.image)
    }

    @Test
    fun `test image from metadata is ignored in markdown`() {
        val markdown = """
            ---
            image: https://example.com/explicit.png
            ---
            # Title
            
            ## Step 1
            
            ![First Image](https://example.com/first.png)
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("https://example.com/first.png", codelab.metadata.image)
    }

    @Test
    fun `test poster automatically set to first image in paragraph in markdown`() {
        val markdown = """
            # Title
            
            ## Step 1
            
            Text and ![Embedded Image](https://example.com/embedded.png)
        """.trimIndent()

        val codelab = parser.parse(ByteArrayInputStream(markdown.toByteArray()))

        assertEquals("https://example.com/embedded.png", codelab.metadata.image)
    }

    @Test
    fun `test poster automatically set to first image in google doc`() {
        val document = Document().apply {
            title = "Title"
            documentId = "doc-id"
            body = com.google.api.services.docs.v1.model.Body().apply {
                content = listOf(
                    com.google.api.services.docs.v1.model.StructuralElement().apply {
                        paragraph = com.google.api.services.docs.v1.model.Paragraph().apply {
                            paragraphStyle = com.google.api.services.docs.v1.model.ParagraphStyle().apply {
                                namedStyleType = "HEADING_1"
                            }
                            elements = listOf(
                                com.google.api.services.docs.v1.model.ParagraphElement().apply {
                                    textRun = com.google.api.services.docs.v1.model.TextRun().apply {
                                        content = "Step 1"
                                    }
                                }
                            )
                        }
                    },
                    com.google.api.services.docs.v1.model.StructuralElement().apply {
                        paragraph = com.google.api.services.docs.v1.model.Paragraph().apply {
                            elements = listOf(
                                com.google.api.services.docs.v1.model.ParagraphElement().apply {
                                    inlineObjectElement = InlineObjectElement().apply {
                                        inlineObjectId = "obj1"
                                    }
                                }
                            )
                        }
                    }
                )
            }
            inlineObjects = mapOf(
                "obj1" to InlineObject().apply {
                    inlineObjectProperties = InlineObjectProperties().apply {
                        embeddedObject = EmbeddedObject().apply {
                            imageProperties = ImageProperties().apply {
                                contentUri = "https://example.com/gdoc-image.png"
                            }
                        }
                    }
                }
            )
        }

        val codelab = gDocParser.parse(document)
        assertEquals("https://example.com/gdoc-image.png", codelab.metadata.image)
    }
}
