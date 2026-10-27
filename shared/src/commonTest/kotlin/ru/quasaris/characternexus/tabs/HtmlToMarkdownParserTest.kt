package ru.quasaris.characternexus.tabs

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlToMarkdownParserTest {

    @Test
    fun testParseSimpleFormatting() {
        val html = "<b>Bold</b> and <i>Italic</i> and <s>Strikethrough</s>"
        val markdown = HtmlToMarkdownParser.toMarkdown(html)
        assertEquals("**Bold** and _Italic_ and ~~Strikethrough~~", markdown)
    }

    @Test
    fun testParseGoogleDocsSpanStyles() {
        val html = "<span style=\"font-weight:700;\">Google Bold</span> <span style=\"font-style:italic;\">Google Italic</span>"
        val markdown = HtmlToMarkdownParser.toMarkdown(html)
        assertEquals("**Google Bold** _Google Italic_", markdown)
    }

    @Test
    fun testParseHyperlink() {
        val html = "Visit <a href=\"https://example.com\">Example Website</a> for details."
        val markdown = HtmlToMarkdownParser.toMarkdown(html)
        assertEquals("Visit [Example Website](https://example.com) for details.", markdown)
    }

    @Test
    fun testParseHeaders() {
        val html = "<h1>Title 1</h1><p>Paragraph text</p>"
        val markdown = HtmlToMarkdownParser.toMarkdown(html)
        assertEquals("# Title 1\nParagraph text", markdown)
    }

    @Test
    fun testDecodeEntities() {
        val encoded = "Hello &amp; Welcome &nbsp; &lt;Test&gt;"
        val decoded = HtmlToMarkdownParser.decodeHtmlEntities(encoded)
        assertEquals("Hello & Welcome   <Test>", decoded)
    }

    @Test
    fun testParseGoogleDocsOuterBTag() {
        val html = "<b style=\"font-weight:normal;\" id=\"docs-internal-guid-12345\">" +
                "<span style=\"font-weight:700;font-style:italic;\">Владение Инструментом.</span>" +
                "<span style=\"font-weight:400;font-style:normal;\"> Вы получаете владение </span>" +
                "<a href=\"https://example.com\"><span style=\"font-weight:400;\">Инструментами Кузнеца</span></a>" +
                "<span style=\"font-weight:400;\">.</span></b>"
        val markdown = HtmlToMarkdownParser.toMarkdown(html)
        assertEquals("_**Владение Инструментом.**_ Вы получаете владение [Инструментами Кузнеца](https://example.com).", markdown)
    }
}
