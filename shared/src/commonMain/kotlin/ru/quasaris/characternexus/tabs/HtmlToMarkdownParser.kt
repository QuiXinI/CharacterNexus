package ru.quasaris.characternexus.tabs

/**
 * Lightweight HTML to Markdown converter designed for pasting rich text from
 * Google Docs, MS Word, web browsers, and other rich text editors into [NotionBlockEditor].
 */
object HtmlToMarkdownParser {

    private data class FormattingState(
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isStrike: Boolean = false,
        val href: String? = null,
        val headerLevel: Int = 0,
        val isBlock: Boolean = false
    )

    fun toMarkdown(html: String): String {
        if (html.isBlank()) return ""

        // 1. Clean HTML structure
        var cleanHtml = html
            .replace(Regex("(?s)<!--.*?-->"), "") // Remove HTML comments
            .replace(Regex("(?is)<head.*?>.*?</head>"), "") // Remove head
            .replace(Regex("(?is)<style.*?>.*?</style>"), "") // Remove style tags
            .replace(Regex("(?is)<script.*?>.*?</script>"), "") // Remove scripts

        // Fast path: if no HTML tags present, return unescaped plain text
        if (!cleanHtml.contains("<")) {
            return decodeHtmlEntities(cleanHtml)
        }

        val result = StringBuilder()

        // Stack to track active formatting styles
        val stateStack = mutableListOf(FormattingState())

        var i = 0
        val len = cleanHtml.length

        while (i < len) {
            val ch = cleanHtml[i]

            if (ch == '<') {
                val tagEnd = cleanHtml.indexOf('>', i)
                if (tagEnd == -1) {
                    // Unclosed angle bracket, treat as raw text
                    result.append('<')
                    i++
                    continue
                }

                val rawTag = cleanHtml.substring(i + 1, tagEnd).trim()
                val isClosing = rawTag.startsWith("/")
                val tagNameWithAttrs = if (isClosing) rawTag.substring(1).trim() else rawTag

                val parts = tagNameWithAttrs.split(Regex("\\s+"), limit = 2)
                val tagName = parts.firstOrNull()?.lowercase() ?: ""
                val attrsStr = parts.getOrNull(1) ?: ""

                i = tagEnd + 1

                if (tagName == "br") {
                    result.append("\n")
                    continue
                }

                if (isClosing) {
                    // Pop state if available
                    if (stateStack.size > 1) {
                        val closingState = stateStack.removeAt(stateStack.lastIndex)
                        if (closingState.isBlock && result.isNotEmpty() && !result.endsWith("\n")) {
                            result.append("\n")
                        }
                    }
                } else {
                    // Process opening tag and attrs
                    val currentState = stateStack.last()

                    val styleAttr = extractAttribute(attrsStr, "style")?.lowercase() ?: ""
                    val hrefAttr = if (tagName == "a") extractAttribute(attrsStr, "href") else null

                    val hasExplicitBoldTrue = styleAttr.contains("font-weight:700") ||
                            styleAttr.contains("font-weight: 700") ||
                            styleAttr.contains("font-weight:bold") ||
                            styleAttr.contains("font-weight: bold") ||
                            styleAttr.contains("font-weight:600") ||
                            styleAttr.contains("font-weight: 800") ||
                            styleAttr.contains("font-weight: 900")

                    val hasExplicitBoldFalse = styleAttr.contains("font-weight:400") ||
                            styleAttr.contains("font-weight: 400") ||
                            styleAttr.contains("font-weight:normal") ||
                            styleAttr.contains("font-weight: normal")

                    val resolvedBold = when {
                        hasExplicitBoldTrue -> true
                        hasExplicitBoldFalse -> false
                        tagName in setOf("b", "strong") -> true
                        else -> currentState.isBold
                    }

                    val hasExplicitItalicTrue = styleAttr.contains("font-style:italic") ||
                            styleAttr.contains("font-style: italic") ||
                            styleAttr.contains("font-style:oblique")

                    val hasExplicitItalicFalse = styleAttr.contains("font-style:normal") ||
                            styleAttr.contains("font-style: normal")

                    val resolvedItalic = when {
                        hasExplicitItalicTrue -> true
                        hasExplicitItalicFalse -> false
                        tagName in setOf("i", "em") -> true
                        else -> currentState.isItalic
                    }

                    val hasExplicitStrikeTrue = styleAttr.contains("text-decoration:line-through") ||
                            styleAttr.contains("text-decoration: line-through")

                    val hasExplicitStrikeFalse = styleAttr.contains("text-decoration:none") ||
                            styleAttr.contains("text-decoration: none")

                    val resolvedStrike = when {
                        hasExplicitStrikeTrue -> true
                        hasExplicitStrikeFalse -> false
                        tagName in setOf("s", "strike", "del") -> true
                        else -> currentState.isStrike
                    }

                    val headerLevel = when (tagName) {
                        "h1" -> 1
                        "h2" -> 2
                        "h3" -> 3
                        "h4", "h5", "h6" -> 3
                        else -> 0
                    }

                    val isBlockTag = tagName in setOf("p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "tr")

                    if (isBlockTag && result.isNotEmpty() && !result.endsWith("\n")) {
                        result.append("\n")
                    }

                    if (headerLevel > 0) {
                        repeat(headerLevel) { result.append("#") }
                        result.append(" ")
                    }

                    val newState = currentState.copy(
                        isBold = resolvedBold,
                        isItalic = resolvedItalic,
                        isStrike = resolvedStrike,
                        href = hrefAttr ?: currentState.href,
                        headerLevel = headerLevel,
                        isBlock = isBlockTag
                    )

                    stateStack.add(newState)
                }
            } else {
                // Collect text segment until next tag
                val textStart = i
                while (i < len && cleanHtml[i] != '<') {
                    i++
                }
                val textSegment = cleanHtml.substring(textStart, i)
                if (textSegment.isNotEmpty()) {
                    val decodedText = decodeHtmlEntities(textSegment)
                    val currentState = stateStack.last()

                    val trimmed = decodedText.trim()
                    if (trimmed.isEmpty()) {
                        // Preserve spaces if non-empty
                        if (decodedText.contains(" ")) {
                            result.append(" ")
                        }
                    } else {
                        val leadingSpace = if (decodedText.startsWith(" ")) " " else ""
                        val trailingSpace = if (decodedText.endsWith(" ")) " " else ""

                        result.append(leadingSpace)

                        var formattedText = trimmed

                        if (currentState.isBold) {
                            formattedText = "**$formattedText**"
                        }
                        if (currentState.isItalic) {
                            formattedText = "_${formattedText}_"
                        }
                        if (currentState.isStrike) {
                            formattedText = "~~$formattedText~~"
                        }
                        if (!currentState.href.isNullOrBlank()) {
                            formattedText = "[$formattedText](${currentState.href})"
                        }

                        result.append(formattedText)
                        result.append(trailingSpace)
                    }
                }
            }
        }

        // Post-processing: normalize consecutive newlines (max 2) and clean spaces around newlines
        return result.toString()
            .replace(Regex("\n{3,}"), "\n\n")
            .replace(Regex("[ \t]+\n"), "\n")
            .trim()
    }

    private fun extractAttribute(attrsStr: String, attrName: String): String? {
        if (attrsStr.isBlank()) return null
        val regex = Regex("(?i)\\b$attrName\\s*=\\s*([\"'])(.*?)\\1")
        val match = regex.find(attrsStr)
        if (match != null) {
            return match.groupValues[2]
        }
        // Fallback for unquoted attributes
        val unquotedRegex = Regex("(?i)\\b$attrName\\s*=\\s*([^\\s>]+)")
        return unquotedRegex.find(attrsStr)?.groupValues?.get(1)
    }

    fun decodeHtmlEntities(text: String): String {
        if (!text.contains("&")) return text

        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else match.value
            }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                if (code != null) code.toChar().toString() else match.value
            }
    }
}
