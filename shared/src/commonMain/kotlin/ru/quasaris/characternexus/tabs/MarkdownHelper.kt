package ru.quasaris.characternexus.tabs

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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import ru.quasaris.characternexus.model.DynamicContentBlock

object MarkdownHelper {
    private val AUTO_URL_REGEX = Regex("(?:https?://|www\\.)[\\w:#@%/;$()~_?+\\-=\\.&]+[\\w#@%/;$()~_?+\\-=]")

    fun getHeaderLevelOfLine(line: String): Int {
        val clean = line.trimStart()
        return when {
            clean.startsWith("### ") || clean.startsWith("###\n") ||
            clean.startsWith("**### ") || clean.startsWith("_### ") || clean.startsWith("~~### ") -> 3

            clean.startsWith("## ") || clean.startsWith("##\n") ||
            clean.startsWith("**## ") || clean.startsWith("_## ") || clean.startsWith("~~## ") -> 2

            clean.startsWith("# ") || clean.startsWith("#\n") ||
            clean.startsWith("**# ") || clean.startsWith("_# ") || clean.startsWith("~~# ") -> 1

            else -> 0
        }
    }

    fun getHeaderLevel(value: TextFieldValue): Int {
        val text = value.text
        if (text.isEmpty()) return 0
        val selection = value.selection
        val pos = selection.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        var lineEnd = text.indexOf('\n', pos)
        if (lineEnd == -1) lineEnd = text.length
        val start = minOf(lineStart, lineEnd).coerceIn(0, text.length)
        val end = maxOf(lineStart, lineEnd).coerceIn(start, text.length)
        val line = if (start < end) text.substring(start, end) else ""
        return getHeaderLevelOfLine(line)
    }

    fun applyHeader(value: TextFieldValue, level: Int): TextFieldValue {
        val text = value.text
        val selection = value.selection

        val startPos = selection.min.coerceIn(0, text.length)
        val endPos = selection.max.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (startPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        var lineEnd = text.indexOf('\n', endPos)
        if (lineEnd == -1) lineEnd = text.length
        val start = minOf(lineStart, lineEnd).coerceIn(0, text.length)
        val end = maxOf(lineStart, lineEnd).coerceIn(start, text.length)

        val linesSegment = if (start < end) text.substring(start, end) else ""
        val lines = linesSegment.split('\n')

        val targetPrefix = when (level) {
            1 -> "# "
            2 -> "## "
            3 -> "### "
            else -> ""
        }

        val allHaveTarget = level > 0 && lines.all { line ->
            getHeaderLevelOfLine(line) == level
        }

        val finalPrefix = if (allHaveTarget) "" else targetPrefix

        var deltaStart = 0
        var totalDelta = 0

        fun stripHeaderPrefix(line: String): String {
            return when {
                line.startsWith("### ") -> line.substring(4)
                line.startsWith("## ") -> line.substring(3)
                line.startsWith("# ") -> line.substring(2)
                line.startsWith("**### ") -> "**" + line.substring(6)
                line.startsWith("**## ") -> "**" + line.substring(5)
                line.startsWith("**# ") -> "**" + line.substring(4)
                line.startsWith("_### ") -> "_" + line.substring(5)
                line.startsWith("_## ") -> "_" + line.substring(4)
                line.startsWith("_# ") -> "_" + line.substring(3)
                line.startsWith("~~### ") -> "~~" + line.substring(6)
                line.startsWith("~~## ") -> "~~" + line.substring(5)
                line.startsWith("~~# ") -> "~~" + line.substring(4)
                else -> line
            }
        }

        val newLines = lines.mapIndexed { idx, line ->
            val cleanedLine = stripHeaderPrefix(line)
            val updatedLine = if (finalPrefix.isNotEmpty()) "$finalPrefix$cleanedLine" else cleanedLine
            val lineDelta = updatedLine.length - line.length

            if (idx == 0) {
                deltaStart = lineDelta
            }
            totalDelta += lineDelta

            updatedLine
        }

        val newSegment = newLines.joinToString("\n")
        val newText = text.substring(0, start) + newSegment + text.substring(end)

        val newSelStart = (startPos + deltaStart).coerceIn(start, newText.length)
        val newSelEnd = (endPos + totalDelta).coerceIn(newSelStart, newText.length)

        return value.copy(
            text = newText,
            selection = TextRange(newSelStart, newSelEnd)
        )
    }

    fun applyMarkdown(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val selection = value.selection
        val text = value.text
        val selStart = selection.min
        val selEnd = selection.max

        // 1. Cursor case (selection length is 0)
        if (selection.collapsed) {
            // Check if cursor is immediately surrounded by tags: **|**
            if (selStart >= prefix.length && selEnd <= text.length - suffix.length) {
                val before = text.substring(selStart - prefix.length, selStart)
                val after = text.substring(selEnd, selEnd + suffix.length)
                if (before == prefix && after == suffix) {
                    // Toggle OFF: Remove tags
                    val newText = text.substring(0, selStart - prefix.length) + text.substring(selEnd + suffix.length)
                    val newPos = selStart - prefix.length
                    return value.copy(text = newText, selection = TextRange(newPos))
                }
            }
            // Toggle ON: Insert empty tags and place cursor between them
            val newText = text.substring(0, selStart) + prefix + suffix + text.substring(selEnd)
            val newPos = selStart + prefix.length
            return value.copy(text = newText, selection = TextRange(newPos))
        }

        val selectedText = text.substring(selStart, selEnd)

        // 2. Selection matches exactly or is wrapped: **word**
        if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
            val unwrappedText = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            val newText = text.substring(0, selStart) + unwrappedText + text.substring(selEnd)
            return value.copy(text = newText, selection = TextRange(selStart, selStart + unwrappedText.length))
        }

        // 3. Selection is immediately inside tags: **|word|**
        if (selStart >= prefix.length && selEnd <= text.length - suffix.length) {
            val before = text.substring(selStart - prefix.length, selStart)
            val after = text.substring(selEnd, selEnd + suffix.length)
            if (before == prefix && after == suffix) {
                // Toggle OFF: Remove surrounding tags
                val newText = text.substring(0, selStart - prefix.length) + selectedText + text.substring(selEnd + suffix.length)
                return value.copy(text = newText, selection = TextRange(selStart - prefix.length, selEnd - prefix.length))
            }
        }

        // Special case for Quotes: handle >> text << or >> text
        if (prefix == ">> " && suffix == " <<") {
            if (selectedText.startsWith(">> ") && selectedText.endsWith(" <<")) {
                val unwrapped = selectedText.substring(3, selectedText.length - 3)
                val newText = text.substring(0, selStart) + unwrapped + text.substring(selEnd)
                return value.copy(text = newText, selection = TextRange(selStart, selStart + unwrapped.length))
            }
            if (selectedText.startsWith(">> ")) {
                val unwrapped = selectedText.substring(3)
                val newText = text.substring(0, selStart) + unwrapped + text.substring(selEnd)
                return value.copy(text = newText, selection = TextRange(selStart, selStart + unwrapped.length))
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
            val newText = text.substring(0, selStart) + newSelectedText + text.substring(selEnd)
            return value.copy(text = newText, selection = TextRange(selStart, selStart + newSelectedText.length))
        }

        // 5. Default: Toggle ON (Apply tags)
        val newText = text.substring(0, selStart) + prefix + selectedText + suffix + text.substring(selEnd)
        return value.copy(text = newText, selection = TextRange(selStart, selStart + prefix.length + selectedText.length + suffix.length))
    }

    fun isFormatActive(value: TextFieldValue, prefix: String, suffix: String): Boolean {
        val selection = value.selection
        val text = value.text
        val selStart = selection.min
        val selEnd = selection.max
        
        if (selection.collapsed) {
            // Special case for links [text](url)
            if (prefix == "[" && suffix == "](") {
                val before = text.substring(0, selStart)
                val after = text.substring(selEnd)
                return before.contains("[") && after.contains(")")
            }

            if (selStart >= prefix.length && selEnd <= text.length - suffix.length) {
                val before = text.substring(selStart - prefix.length, selStart)
                val after = text.substring(selEnd, selEnd + suffix.length)
                if (before == prefix && (suffix.isEmpty() || after.startsWith(suffix))) return true
            }

            // Check if cursor is anywhere inside prefix...suffix
            val before = text.substring(0, selStart)
            val after = text.substring(selEnd)
            val lastPrefix = before.lastIndexOf(prefix)
            if (lastPrefix != -1) {
                val lastSuffix = before.lastIndexOf(suffix)
                if (suffix.isNotEmpty() && lastSuffix < lastPrefix) {
                    val nextSuffix = after.indexOf(suffix)
                    if (nextSuffix != -1) return true
                }
            }
            return false
        }
        
        // Safety check for selection bounds
        val start = selStart.coerceIn(0, text.length)
        val end = selEnd.coerceIn(0, text.length)
        val selectedText = if (start < end) text.substring(start, end) else ""
        
        // Special case for links [text](url)
        if (prefix == "[" && suffix == "](") {
            return selectedText.startsWith("[") && selectedText.contains("](") && selectedText.endsWith(")") ||
                   (start > 0 && text.substring(0, start).contains("[") && 
                    text.substring(end).contains(")"))
        }

        // Special case for quotes
        if (prefix == ">> " && suffix == " <<") {
            if (selectedText.startsWith(">> ")) return true
            
            // Check if there's an unclosed quote before the cursor
            val before = text.substring(0, start)
            val lastQuoteStart = before.lastIndexOf(">> ")
            if (lastQuoteStart != -1) {
                val lastQuoteEnd = before.lastIndexOf(" <<")
                if (lastQuoteEnd < lastQuoteStart) return true
            }
            return false
        }

        return (selectedText.startsWith(prefix) && (suffix.isEmpty() || selectedText.endsWith(suffix))) || 
               (start >= prefix.length && end <= text.length - suffix.length && 
                text.substring(start - prefix.length, start) == prefix && 
                (suffix.isEmpty() || text.substring(end, end + suffix.length) == suffix)) ||
               (selectedText.contains(prefix) && (suffix.isEmpty() || selectedText.contains(suffix)))
    }

    private data class HeaderLineInfo(val range: IntRange, val level: Int)

    private fun parseInlineText(
        inlineText: String,
        result: StringBuilder,
        boldStarts: MutableList<Int>,
        boldRanges: MutableList<IntRange>,
        italicStarts: MutableList<Int>,
        italicRanges: MutableList<IntRange>,
        strikeStarts: MutableList<Int>,
        strikeRanges: MutableList<IntRange>,
        markerRanges: MutableList<IntRange>,
        isEditing: Boolean
    ) {
        var j = 0
        while (j < inlineText.length) {
            when {
                inlineText.startsWith("**", j) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    val markerVisible = isEditing
                    if (boldStarts.isNotEmpty()) {
                        boldRanges.add(boldStarts.removeAt(boldStarts.size - 1) until result.length)
                    } else {
                        boldStarts.add(result.length + if (markerVisible) 2 else 0)
                    }
                    if (markerVisible) result.append("**")
                    j += 2
                }
                inlineText.startsWith("_", j) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 1)
                    val markerVisible = isEditing
                    if (italicStarts.isNotEmpty()) {
                        italicRanges.add(italicStarts.removeAt(italicStarts.size - 1) until result.length)
                    } else {
                        italicStarts.add(result.length + if (markerVisible) 1 else 0)
                    }
                    if (markerVisible) result.append("_")
                    j += 1
                }
                inlineText.startsWith("~~", j) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    val markerVisible = isEditing
                    if (strikeStarts.isNotEmpty()) {
                        strikeRanges.add(strikeStarts.removeAt(strikeStarts.size - 1) until result.length)
                    } else {
                        strikeStarts.add(result.length + if (markerVisible) 2 else 0)
                    }
                    if (markerVisible) result.append("~~")
                    j += 2
                }
                inlineText.startsWith("::", j) -> {
                    if (isEditing) markerRanges.add(result.length until result.length + 2)
                    if (isEditing) result.append("::")
                    j += 2
                }
                else -> {
                    result.append(inlineText[j])
                    j++
                }
            }
        }
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
        val markerRanges = mutableListOf<IntRange>()
        val headerRanges = mutableListOf<HeaderLineInfo>()

        var i = 0
        var currentHeaderLevel: Int? = null
        var currentHeaderStart: Int? = null

        while (i < text.length) {
            val isAtLineStart = i == 0 || text[i - 1] == '\n'
            
            if (isAtLineStart) {
                val headerMatch = when {
                    text.startsWith("### ", i) -> Pair(3, "### ")
                    text.startsWith("## ", i) -> Pair(2, "## ")
                    text.startsWith("# ", i) -> Pair(1, "# ")
                    text.startsWith("**### ", i) -> Pair(3, "**### ")
                    text.startsWith("**## ", i) -> Pair(2, "**## ")
                    text.startsWith("**# ", i) -> Pair(1, "**# ")
                    text.startsWith("_### ", i) -> Pair(3, "_### ")
                    text.startsWith("_## ", i) -> Pair(2, "_## ")
                    text.startsWith("_# ", i) -> Pair(1, "_# ")
                    text.startsWith("~~### ", i) -> Pair(3, "~~### ")
                    text.startsWith("~~## ", i) -> Pair(2, "~~## ")
                    text.startsWith("~~# ", i) -> Pair(1, "~~# ")
                    else -> null
                }

                if (headerMatch != null) {
                    val (level, matchedPrefix) = headerMatch
                    currentHeaderLevel = level
                    currentHeaderStart = result.length

                    if (matchedPrefix.startsWith("**")) {
                        if (isEditing) markerRanges.add(result.length until result.length + 2)
                        boldStarts.add(result.length + if (isEditing) 2 else 0)
                        if (isEditing) {
                            result.append("**")
                            val hashPart = matchedPrefix.substring(2)
                            markerRanges.add(result.length until result.length + hashPart.length)
                            result.append(hashPart)
                        }
                    } else if (matchedPrefix.startsWith("_")) {
                        if (isEditing) markerRanges.add(result.length until result.length + 1)
                        italicStarts.add(result.length + if (isEditing) 1 else 0)
                        if (isEditing) {
                            result.append("_")
                            val hashPart = matchedPrefix.substring(1)
                            markerRanges.add(result.length until result.length + hashPart.length)
                            result.append(hashPart)
                        }
                    } else if (matchedPrefix.startsWith("~~")) {
                        if (isEditing) markerRanges.add(result.length until result.length + 2)
                        strikeStarts.add(result.length + if (isEditing) 2 else 0)
                        if (isEditing) {
                            result.append("~~")
                            val hashPart = matchedPrefix.substring(2)
                            markerRanges.add(result.length until result.length + hashPart.length)
                            result.append(hashPart)
                        }
                    } else {
                        if (isEditing) {
                            markerRanges.add(result.length until result.length + matchedPrefix.length)
                            result.append(matchedPrefix)
                        }
                    }
                    i += matchedPrefix.length
                    continue
                }
            }

            if (i < text.length && text[i] == '\n') {
                if (currentHeaderLevel != null && currentHeaderStart != null) {
                    headerRanges.add(HeaderLineInfo(currentHeaderStart until result.length, currentHeaderLevel))
                    currentHeaderLevel = null
                    currentHeaderStart = null
                }
                result.append(text[i])
                i++
                continue
            }

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
                            val linkText = text.substring(i + 1, endBracket)
                            val url = text.substring(endBracket + 2, endParen)
                            
                            if (isEditing) {
                                markerRanges.add(result.length until result.length + 1) // [
                                result.append("[")
                                
                                val linkContentStart = result.length
                                parseInlineText(
                                    inlineText = linkText,
                                    result = result,
                                    boldStarts = boldStarts,
                                    boldRanges = boldRanges,
                                    italicStarts = italicStarts,
                                    italicRanges = italicRanges,
                                    strikeStarts = strikeStarts,
                                    strikeRanges = strikeRanges,
                                    markerRanges = markerRanges,
                                    isEditing = true
                                )
                                val linkContentEnd = result.length
                                
                                markerRanges.add(result.length until result.length + 2) // ](
                                result.append("](")
                                markerRanges.add(result.length until result.length + url.length) // url
                                result.append(url)
                                markerRanges.add(result.length until result.length + 1) // )
                                result.append(")")
                                
                                linkRanges.add(Triple(linkContentStart until linkContentEnd, linkText, url))
                            } else {
                                val linkContentStart = result.length
                                parseInlineText(
                                    inlineText = linkText,
                                    result = result,
                                    boldStarts = boldStarts,
                                    boldRanges = boldRanges,
                                    italicStarts = italicStarts,
                                    italicRanges = italicRanges,
                                    strikeStarts = strikeStarts,
                                    strikeRanges = strikeRanges,
                                    markerRanges = markerRanges,
                                    isEditing = false
                                )
                                val linkContentEnd = result.length
                                linkRanges.add(Triple(linkContentStart until linkContentEnd, linkText, url))
                            }
                            
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
                else -> {
                    result.append(text[i])
                    i++
                }
            }
        }

        if (currentHeaderLevel != null && currentHeaderStart != null) {
            headerRanges.add(HeaderLineInfo(currentHeaderStart until result.length, currentHeaderLevel))
        }
        
        // Handle unclosed tags
        boldStarts.forEach { boldRanges.add(it until result.length) }
        italicStarts.forEach { italicRanges.add(it until result.length) }
        strikeStarts.forEach { strikeRanges.add(it until result.length) }

        return buildAnnotatedString {
            val totalLen = result.length
            append(result.toString())
            
            // Apply 50% opacity to markers
            val markerStyle = if (onSurface != Color.Unspecified) {
                SpanStyle(color = onSurface.copy(alpha = 0.5f))
            } else {
                SpanStyle()
            }
            markerRanges.forEach { range ->
                val start = range.first.coerceIn(0, totalLen)
                val end = (range.last + 1).coerceIn(start, totalLen)
                if (start < end) {
                    addStyle(markerStyle, start, end)
                }
            }

            // Apply Header styles first
            headerRanges.forEach { header ->
                val fontSize = when (header.level) {
                    1 -> 24.sp
                    2 -> 21.sp
                    3 -> 19.sp
                    else -> 17.sp
                }
                if (header.range.first <= header.range.last && header.range.first in 0..totalLen) {
                    val start = header.range.first.coerceIn(0, totalLen)
                    val end = (header.range.last + 1).coerceIn(start, totalLen)
                    if (start < end) {
                        addStyle(
                            SpanStyle(fontSize = fontSize, fontWeight = FontWeight.Bold),
                            start,
                            end
                        )
                    }
                }
            }

            boldRanges.filter { it.first < it.last + 1 }.forEach { range ->
                val isInsideHeader = headerRanges.any { h -> range.first >= h.range.first && range.last <= h.range.last }
                val weight = if (isInsideHeader) FontWeight.Black else FontWeight.Bold
                val start = range.first.coerceIn(0, totalLen)
                val end = (range.last + 1).coerceIn(start, totalLen)
                if (start < end) {
                    addStyle(SpanStyle(fontWeight = weight), start, end)
                }
            }
            italicRanges.filter { it.first < it.last + 1 }.forEach { range ->
                val start = range.first.coerceIn(0, totalLen)
                val end = (range.last + 1).coerceIn(start, totalLen)
                if (start < end) {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                }
            }
            strikeRanges.filter { it.first < it.last + 1 }.forEach { range ->
                val start = range.first.coerceIn(0, totalLen)
                val end = (range.last + 1).coerceIn(start, totalLen)
                if (start < end) {
                    addStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = if (onSurface != Color.Unspecified) onSurface else Color.Unspecified
                        ),
                        start, end
                    )
                }
            }

            val urlAccent = Color(0xFF2196F3)
            val finalLinkColor = if (onSurface != Color.Unspecified) lerp(onSurface, urlAccent, 0.8f) else urlAccent
            
            linkRanges.forEach { (range, _, url) ->
                val validatedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url

                val start = range.first.coerceIn(0, totalLen)
                val end = (range.last + 1).coerceIn(start, totalLen)
                if (start < end) {
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
                        start, end
                    )
                }
            }
            
            // Automatic link detection (improved regex)
            AUTO_URL_REGEX.findAll(result.toString()).forEach { match ->
                // Check if this range is already covered by a markdown link
                if (linkRanges.none { it.first.first <= match.range.first && it.first.last >= match.range.last }) {
                    val url = match.value
                    val validatedUrl = if (url.startsWith("www.")) "https://$url" else url
                    
                    val start = match.range.first.coerceIn(0, totalLen)
                    val end = (match.range.last + 1).coerceIn(start, totalLen)
                    if (start < end) {
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
                            start, end
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified
) {
    if (text.isBlank()) return
    val colorScheme = MaterialTheme.colorScheme
    val resolvedColor = if (color != Color.Unspecified) color else colorScheme.onSurface
    val blocks = remember(text) { BlockContentParser.toBlocks(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is DynamicContentBlock.Text -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, resolvedColor) {
                            MarkdownHelper.parseMarkdown(block.content, resolvedColor, isEditing = false)
                        }
                        Text(
                            text = annotated,
                            style = style,
                            color = resolvedColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is DynamicContentBlock.Spoiler -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, resolvedColor) {
                            MarkdownHelper.parseMarkdown(block.content, resolvedColor, isEditing = false)
                        }
                        Text(
                            text = annotated,
                            style = style,
                            color = resolvedColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is DynamicContentBlock.Quote -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, resolvedColor) {
                            MarkdownHelper.parseMarkdown(block.content, resolvedColor, isEditing = false)
                        }
                        Text(
                            text = annotated,
                            style = style,
                            color = resolvedColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is DynamicContentBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        thickness = 1.dp,
                        color = colorScheme.outlineVariant
                    )
                }
                else -> {}
            }
        }
    }
}

