package ru.quasaris.characters.master.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

        // 1. Cursor case (selection length is 0)
        if (selection.length == 0) {
            // Check if cursor is immediately surrounded by tags: **|**
            if (selection.start >= prefix.length && selection.end <= text.length - suffix.length) {
                val before = text.substring(selection.start - prefix.length, selection.start)
                val after = text.substring(selection.end, selection.end + suffix.length)
                if (before == prefix && after == suffix) {
                    // Toggle OFF: Remove tags
                    val newText = text.substring(0, selection.start - prefix.length) + text.substring(selection.end + suffix.length)
                    val newPos = selection.start - prefix.length
                    return value.copy(text = newText, selection = TextRange(newPos))
                }
            }
            // Toggle ON: Insert empty tags and place cursor between them
            val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
            val newPos = selection.start + prefix.length
            return value.copy(text = newText, selection = TextRange(newPos))
        }

        val selectedText = text.substring(selection.start, selection.end)

        // 2. Selection matches exactly or is wrapped: **word**
        if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
            val unwrappedText = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            val newText = text.substring(0, selection.start) + unwrappedText + text.substring(selection.end)
            return value.copy(text = newText, selection = TextRange(selection.start, selection.start + unwrappedText.length))
        }

        // 3. Selection is immediately inside tags: **|word|**
        if (selection.start >= prefix.length && selection.end <= text.length - suffix.length) {
            val before = text.substring(selection.start - prefix.length, selection.start)
            val after = text.substring(selection.end, selection.end + suffix.length)
            if (before == prefix && after == suffix) {
                // Toggle OFF: Remove surrounding tags
                val newText = text.substring(0, selection.start - prefix.length) + selectedText + text.substring(selection.end + suffix.length)
                return value.copy(text = newText, selection = TextRange(selection.start - prefix.length, selection.end - prefix.length))
            }
        }

        // 4. Selection contains tags anywhere: [**word**](url)
        // We look for any occurrences of prefix and suffix within the selection and remove them.
        if (selectedText.contains(prefix) && (suffix.isEmpty() || selectedText.contains(suffix))) {
            val newSelectedText = if (suffix.isNotEmpty()) {
                selectedText.replace(prefix, "").replace(suffix, "")
            } else {
                selectedText.replace(prefix, "")
            }
            val newText = text.substring(0, selection.start) + newSelectedText + text.substring(selection.end)
            return value.copy(text = newText, selection = TextRange(selection.start, selection.start + newSelectedText.length))
        }

        // 5. Default: Toggle ON (Apply tags)
        val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
        return value.copy(text = newText, selection = TextRange(selection.start, selection.start + prefix.length + selectedText.length + suffix.length))
    }

    fun isFormatActive(value: TextFieldValue, prefix: String, suffix: String): Boolean {
        val selection = value.selection
        val text = value.text
        if (selection.length == 0) {
            // Special case for links [text](url)
            if (prefix == "[" && suffix == "](") {
                // Check if cursor is inside [text](url) or surrounding it
                // This is a bit complex, but let's do a simple check
                val before = text.substring(0, selection.start)
                val after = text.substring(selection.end)
                return before.contains("[") && after.contains(")")
            }

            if (selection.start >= prefix.length && selection.end <= text.length - suffix.length) {
                val before = text.substring(selection.start - prefix.length, selection.start)
                val after = text.substring(selection.end, selection.end + suffix.length)
                return before == prefix && (suffix.isEmpty() || after.startsWith(suffix))
            }
            return false
        }
        val selectedText = text.substring(selection.start, selection.end)
        
        // Special case for links [text](url)
        if (prefix == "[" && suffix == "](") {
            return selectedText.startsWith("[") && selectedText.contains("](") && selectedText.endsWith(")") ||
                   (selection.start > 0 && text.substring(0, selection.start).contains("[") && 
                    text.substring(selection.end).contains(")"))
        }

        return (selectedText.startsWith(prefix) && (suffix.isEmpty() || selectedText.endsWith(suffix))) || 
               (selection.start >= prefix.length && selection.end <= text.length - suffix.length && 
                text.substring(selection.start - prefix.length, selection.start) == prefix && 
                (suffix.isEmpty() || text.substring(selection.end, selection.end + suffix.length) == suffix)) ||
               (selectedText.contains(prefix) && (suffix.isEmpty() || selectedText.contains(suffix)))
    }

    fun parseMarkdown(text: String, onSurface: Color = Color.Unspecified, isEditing: Boolean = false): AnnotatedString {
        val result = StringBuilder()
        val boldStarts = mutableListOf<Int>()
        val italicStarts = mutableListOf<Int>()
        val strikeStarts = mutableListOf<Int>()
        
        val boldRanges = mutableListOf<IntRange>()
        val italicRanges = mutableListOf<IntRange>()
        val strikeRanges = mutableListOf<IntRange>()
        val linkRanges = mutableListOf<Triple<IntRange, String, String>>() // Range, Text, URL
        val quoteRanges = mutableListOf<IntRange>()
        val markerRanges = mutableListOf<IntRange>()

        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    val markerVisible = isEditing
                    if (boldStarts.isNotEmpty()) {
                        boldRanges.add(boldStarts.removeAt(boldStarts.size - 1) until result.length)
                    } else {
                        boldStarts.add(result.length + if (markerVisible) 2 else 0)
                    }
                    if (markerVisible) result.append("**")
                    i += 2
                }
                text.startsWith("_", i) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 1)
                    val markerVisible = isEditing
                    if (italicStarts.isNotEmpty()) {
                        italicRanges.add(italicStarts.removeAt(italicStarts.size - 1) until result.length)
                    } else {
                        italicStarts.add(result.length + if (markerVisible) 1 else 0)
                    }
                    if (markerVisible) result.append("_")
                    i += 1
                }
                text.startsWith("~~", i) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    val markerVisible = isEditing
                    if (strikeStarts.isNotEmpty()) {
                        strikeRanges.add(strikeStarts.removeAt(strikeStarts.size - 1) until result.length)
                    } else {
                        strikeStarts.add(result.length + if (markerVisible) 2 else 0)
                    }
                    if (markerVisible) result.append("~~")
                    i += 2
                }
                text.startsWith("::", i) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    // We don't apply a SpanStyle for the content of the spoiler in the editor, 
                    // just highlight the markers
                    if (isEditing) result.append("::")
                    i += 2
                }
                text.startsWith("[", i) -> {
                    val endBracket = text.indexOf("]", i)
                    if (endBracket != -1 && endBracket + 1 < text.length && text[endBracket + 1] == '(') {
                        val endParen = text.indexOf(")", endBracket + 2)
                        if (endParen != -1) {
                            if (isEditing) markerRanges.add(result.length until result.length + 1) // [
                            if (isEditing) result.append("[")
                            
                            val linkText = text.substring(i + 1, endBracket)
                            val url = text.substring(endBracket + 2, endParen)
                            val start = result.length
                            result.append(linkText)
                            val linkEnd = result.length
                            
                            if (isEditing) {
                                markerRanges.add(linkEnd until linkEnd + 2) // ](
                                result.append("](")
                                markerRanges.add(result.length until result.length + url.length) // url
                                result.append(url)
                                markerRanges.add(result.length until result.length + 1) // )
                                result.append(")")
                            }
                            
                            linkRanges.add(Triple(start until linkEnd, linkText, url))
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
                (i == 0 || (i > 0 && text[i-1] == '\n')) && text.startsWith("> ", i) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2) // > 
                    if (isEditing) result.append("> ")
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
            
            // Apply 50% opacity to markers
            val markerStyle = if (onSurface != Color.Unspecified) SpanStyle(color = onSurface.copy(alpha = 0.5f)) else SpanStyle(color = Color.Unspecified.copy(alpha = 0.5f))
            markerRanges.forEach { range ->
                addStyle(markerStyle, range.first, range.last + 1)
            }

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
                        background = Color.Gray.copy(alpha = 0.1f),
                        fontStyle = FontStyle.Italic
                    ),
                    it.first, it.last + 1
                )
            }

            val urlAccent = Color(0xFF2196F3)
            val finalLinkColor = if (onSurface != Color.Unspecified) lerp(onSurface, urlAccent, 0.8f) else urlAccent
            
            linkRanges.forEach { (range, _, url) ->
                val validatedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url

                addLink(
                    LinkAnnotation.Url(
                        url = validatedUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = finalLinkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    ),
                    range.first, range.last + 1
                )
            }
            
            // Automatic link detection (improved regex)
            val urlRegex = Regex("(?:https?://|www\\.)[\\w:#@%/;$()~_?+\\-=\\.&]+[\\w#@%/;$()~_?+\\-=]")
            urlRegex.findAll(result.toString()).forEach { match ->
                // Check if this range is already covered by a markdown link
                if (linkRanges.none { it.first.first <= match.range.first && it.first.last >= match.range.last }) {
                    val url = match.value
                    val validatedUrl = if (url.startsWith("www.")) "https://$url" else url
                    
                    addLink(
                        LinkAnnotation.Url(
                            url = validatedUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = finalLinkColor,
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
