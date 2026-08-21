package ru.quasaris.characternexus.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Formats a condition description from Markdown-like syntax.
 * - Lines starting with "- " are converted to bullet points with [accentColor].
 * - Text wrapped in "**" is made bold and colored with [accentColor].
 */
fun formatConditionDescription(text: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { i, line ->
            var l = line.trim()
            if (l.isEmpty()) return@forEachIndexed
            
            if (l.startsWith("- ")) {
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = accentColor)) {
                    append("• ")
                }
                l = l.substring(2)
            }
            
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var last = 0
            boldRegex.findAll(l).forEach { m ->
                append(l.substring(last, m.range.first))
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )) {
                    append(m.groupValues[1])
                }
                last = m.range.last + 1
            }
            append(l.substring(last))
            
            if (i < lines.size - 1) append("\n")
        }
    }
}
