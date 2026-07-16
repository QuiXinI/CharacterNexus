package ru.quasaris.characters.master.tabs

sealed class DynamicContentBlock {
    data class Text(val content: String) : DynamicContentBlock()
    object Divider : DynamicContentBlock()
    data class Spoiler(val content: String) : DynamicContentBlock()
    data class Resource(
        val name: String,
        val current: String,
        val max: String,
        val shortRest: String, // "all" or number
        val longRest: String,  // "all" or number
        val link: String?,
        val notes: String,
        val showNotes: Boolean,
        val originalTag: String
    ) : DynamicContentBlock()
}

object DynamicContentParser {
    private val dividerRegex = Regex("^---$", RegexOption.MULTILINE)
    private val spoilerRegex = Regex("::(.*?)::", RegexOption.DOT_MATCHES_ALL)
    private val resourceRegex = Regex("\\[Ресурс:\\s*([^|]+)\\|\\s*cur=([^|]+)\\|\\s*max=([^|]+)(?:\\|\\s*sr=([^|]+))?(?:\\|\\s*lr=([^|]+))?(?:\\|\\s*link=([^|]+))?(?:\\|\\s*notes=([^|]*))?(?:\\|\\s*showNotes=(true|false))?]", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<DynamicContentBlock> {
        val blocks = mutableListOf<DynamicContentBlock>()
        var currentPos = 0

        val allMatches = mutableListOf<Pair<IntRange, DynamicContentBlock>>()

        // Find dividers
        dividerRegex.findAll(text).forEach { match ->
            allMatches.add(match.range to DynamicContentBlock.Divider)
        }

        // Find spoilers
        spoilerRegex.findAll(text).forEach { match ->
            allMatches.add(match.range to DynamicContentBlock.Spoiler(match.groupValues[1]))
        }

        // Find resources
        resourceRegex.findAll(text).forEach { match ->
            val block = DynamicContentBlock.Resource(
                name = match.groupValues[1].trim(),
                current = match.groupValues[2].trim(),
                max = match.groupValues[3].trim(),
                shortRest = match.groupValues[4].trim().ifEmpty { "0" },
                longRest = match.groupValues[5].trim().ifEmpty { "0" },
                link = match.groupValues[6].trim().takeIf { it.isNotEmpty() },
                notes = match.groupValues[7].trim(),
                showNotes = match.groupValues[8].toBoolean(),
                originalTag = match.value
            )
            allMatches.add(match.range to block)
        }

        // Sort matches by start position
        allMatches.sortBy { it.first.first }

        // Filter out overlapping matches (prefer first one)
        val nonOverlappingMatches = mutableListOf<Pair<IntRange, DynamicContentBlock>>()
        var lastEnd = -1
        for (match in allMatches) {
            if (match.first.first >= lastEnd) {
                nonOverlappingMatches.add(match)
                lastEnd = match.first.last + 1
            }
        }

        // Interleave text blocks
        for (match in nonOverlappingMatches) {
            val range = match.first
            if (range.first > currentPos) {
                val textPart = text.substring(currentPos, range.first)
                if (textPart.isNotEmpty()) {
                    blocks.add(DynamicContentBlock.Text(textPart))
                }
            }
            blocks.add(match.second)
            currentPos = range.last + 1
        }

        if (currentPos < text.length) {
            val textPart = text.substring(currentPos)
            if (textPart.isNotEmpty()) {
                blocks.add(DynamicContentBlock.Text(textPart))
            }
        }

        return if (blocks.isEmpty() && text.isNotEmpty()) listOf(DynamicContentBlock.Text(text)) else blocks
    }
}
