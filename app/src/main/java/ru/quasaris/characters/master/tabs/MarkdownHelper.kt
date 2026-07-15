package ru.quasaris.characters.master.tabs

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownHelper {
    fun applyMarkdown(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val selection = value.selection
        val text = value.text
        
        // If no selection, just insert tags at cursor
        if (selection.length == 0) {
            val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
            val newSelection = TextRange(selection.start + prefix.length)
            return value.copy(text = newText, selection = newSelection)
        }

        val selectedText = text.substring(selection.start, selection.end)
        
        // Simple toggle logic: if already surrounded by the same prefix/suffix, remove them
        if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
            val unwrappedText = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            val newText = text.substring(0, selection.start) + unwrappedText + text.substring(selection.end)
            val newSelection = TextRange(selection.start, selection.start + unwrappedText.length)
            return value.copy(text = newText, selection = newSelection)
        }

        val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
        val newSelection = TextRange(selection.start, selection.start + prefix.length + selectedText.length + suffix.length)
        return value.copy(text = newText, selection = newSelection)
    }

    fun parseMarkdown(text: String): AnnotatedString {
        val result = StringBuilder()
        val boldStarts = mutableListOf<Int>()
        val italicStarts = mutableListOf<Int>()
        val strikeStarts = mutableListOf<Int>()
        
        val boldRanges = mutableListOf<IntRange>()
        val italicRanges = mutableListOf<IntRange>()
        val strikeRanges = mutableListOf<IntRange>()
        val linkRanges = mutableListOf<Triple<IntRange, String, String>>() // Range, Text, URL
        val quoteRanges = mutableListOf<IntRange>()

        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    if (boldStarts.isNotEmpty()) {
                        boldRanges.add(boldStarts.removeAt(boldStarts.size - 1) until result.length)
                    } else {
                        boldStarts.add(result.length)
                    }
                    i += 2
                }
                text.startsWith("*", i) || text.startsWith("_", i) -> {
                    if (italicStarts.isNotEmpty()) {
                        italicRanges.add(italicStarts.removeAt(italicStarts.size - 1) until result.length)
                    } else {
                        italicStarts.add(result.length)
                    }
                    i += 1
                }
                text.startsWith("~", i) -> {
                    if (strikeStarts.isNotEmpty()) {
                        strikeRanges.add(strikeStarts.removeAt(strikeStarts.size - 1) until result.length)
                    } else {
                        strikeStarts.add(result.length)
                    }
                    i += 1
                }
                text.startsWith("[", i) -> {
                    val endBracket = text.indexOf("]", i)
                    if (endBracket != -1 && endBracket + 1 < text.length && text[endBracket + 1] == '(') {
                        val endParen = text.indexOf(")", endBracket + 2)
                        if (endParen != -1) {
                            val linkText = text.substring(i + 1, endBracket)
                            val url = text.substring(endBracket + 2, endParen)
                            val start = result.length
                            result.append(linkText)
                            linkRanges.add(Triple(start until result.length, linkText, url))
                            i = endParen + 1
                        } else {
                            result.append(text[i])
                            i++
                        }
                    } else {
                        result.append(text[i])
                        i++
                    }
                }
                (i == 0 || text[i-1] == '\n') && text.startsWith("> ", i) -> {
                    val start = result.length
                    i += 2
                    val endOfLine = text.indexOf('\n', i)
                    val lineContent = if (endOfLine != -1) text.substring(i, endOfLine) else text.substring(i)
                    result.append(lineContent)
                    quoteRanges.add(start until result.length)
                    i = if (endOfLine != -1) endOfLine else text.length
                }
                else -> {
                    result.append(text[i])
                    i++
                }
            }
        }
        
        // Handle unclosed tags
        boldStarts.forEach { boldRanges.add(it until result.length) }
        italicStarts.forEach { italicRanges.add(it until result.length) }
        strikeStarts.forEach { strikeRanges.add(it until result.length) }

        return buildAnnotatedString {
            append(result.toString())
            boldRanges.filter { it.first < it.last + 1 }.forEach { 
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1) 
            }
            italicRanges.filter { it.first < it.last + 1 }.forEach { 
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), it.first, it.last + 1) 
            }
            strikeRanges.filter { it.first < it.last + 1 }.forEach { 
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), it.first, it.last + 1)
            }
            quoteRanges.filter { it.first < it.last + 1 }.forEach {
                addStyle(
                    SpanStyle(
                        background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.1f),
                        fontStyle = FontStyle.Italic
                    ),
                    it.first, it.last + 1
                )
            }
            linkRanges.forEach { (range, _, url) ->
                addLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    ),
                    range.first, range.last + 1
                )
            }
            
            // Automatic link detection (simple regex)
            val urlRegex = Regex("(https?://[\\w:#@%/;$()~_?+\\-=\\.&]*)")
            urlRegex.findAll(result.toString()).forEach { match ->
                // Check if this range is already covered by a markdown link
                if (linkRanges.none { it.first.first <= match.range.first && it.first.last >= match.range.last }) {
                    addLink(
                        LinkAnnotation.Url(
                            url = match.value,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ),
                        match.range.first, match.range.last + 1
                    )
                }
            }
        }
    }
}
