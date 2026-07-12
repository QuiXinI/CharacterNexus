package ru.quasaris.characters.master.tabs

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object MarkdownHelper {
    fun parseMarkdown(text: String): AnnotatedString {
        val result = StringBuilder()
        val boldStarts = mutableListOf<Int>()
        val italicStarts = mutableListOf<Int>()
        val strikeStarts = mutableListOf<Int>()
        
        val boldRanges = mutableListOf<IntRange>()
        val italicRanges = mutableListOf<IntRange>()
        val strikeRanges = mutableListOf<IntRange>()

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
                else -> {
                    result.append(text[i])
                    i++
                }
            }
        }
        
        // Handle unclosed tags by extending them to the end of the text
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
        }
    }
}
