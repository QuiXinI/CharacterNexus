package ru.quasaris.characternexus.tabs

sealed class DynamicContentBlock {
    data class Text(val content: String) : DynamicContentBlock()
    object Divider : DynamicContentBlock()
    data class Spoiler(val content: String) : DynamicContentBlock()
    data class Quote(val content: String) : DynamicContentBlock()
    data class Resource(
        val name: String,
        val current: String,
        val max: String,
        val shortRest: String = "0",
        val longRest: String = "0",
        val dawnRest: String = "0",
        val link: String? = null,
        val notes: String = "",
        val showNotes: Boolean = false,
        val useSlider: Boolean = false,
        val sliderStep: Double? = null,
        val id: String = ""
    ) : DynamicContentBlock() {
        fun toTag(): String {
            val parts = mutableListOf<String>()
            parts.add(name)
            parts.add("cur=$current")
            parts.add("max=$max")
            if (shortRest != "0") parts.add("sr=$shortRest")
            if (longRest != "0") parts.add("lr=$longRest")
            if (dawnRest != "0") parts.add("dr=$dawnRest")
            if (link != null) parts.add("link=$link")
            if (notes.isNotEmpty()) parts.add("notes=$notes")
            if (showNotes) parts.add("showNotes=true")
            if (useSlider) parts.add("slider=true")
            if (sliderStep != null) parts.add("step=$sliderStep")
            
            // Ensure we always have an ID when rendering back to text
            val actualId = id.ifEmpty { ru.quasaris.characternexus.util.generateUuid() }
            parts.add("id=$actualId")
            
            return "{Ресурс: ${parts.joinToString(" | ")}}"
        }
    }
}

object DynamicContentParser {
    private val dividerRegex = Regex("^---$", RegexOption.MULTILINE)
    private val spoilerRegex = Regex("(?s)::(.*?)::")
    private val quoteRegex = Regex("(?s)>> (.*?)(?: <<|$)")
    private val resourceRegex = Regex("(?s)\\{(?:Ресурс|Resource)[:=]\\s*(.*?)\\}", RegexOption.IGNORE_CASE)

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
        
        // Find quotes
        quoteRegex.findAll(text).forEach { match ->
            allMatches.add(match.range to DynamicContentBlock.Quote(match.groupValues[1]))
        }

        // Find resources using flexible parser
        resourceRegex.findAll(text).forEach { match ->
            val content = match.groupValues[1]
            val parts = content.split("|").map { it.trim() }
            val name = if (parts.isNotEmpty() && parts[0].isNotEmpty()) parts[0] else "Ресурс"
            val params = mutableMapOf<String, String>()
            
            parts.drop(1).forEach { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0].trim().lowercase()] = kv[1].trim()
                }
            }

            val block = DynamicContentBlock.Resource(
                name = name,
                current = params["cur"] ?: "0",
                max = params["max"] ?: "0",
                shortRest = params["sr"] ?: params["shortrest"] ?: "0",
                longRest = params["lr"] ?: params["longrest"] ?: "0",
                dawnRest = params["dr"] ?: params["dawnrest"] ?: params["dawn"] ?: "0",
                link = params["link"],
                notes = params["notes"] ?: "",
                showNotes = params["shownotes"]?.toBoolean() ?: false,
                useSlider = params["slider"]?.toBoolean() ?: false,
                sliderStep = params["step"]?.toDoubleOrNull(),
                id = params["id"] ?: ""
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

    /**
     * Prepares blocks for visual display by stripping technical newlines 
     * that are necessary for data parsing but create excessive vertical space.
     */
    fun getDisplayBlocks(blocks: List<DynamicContentBlock>): List<Pair<Int, DynamicContentBlock>> {
        return blocks.mapIndexedNotNull { index, block ->
            if (block is DynamicContentBlock.Text) {
                var content = block.content
                // Strip one leading newline if preceded by a non-text block
                if (index > 0 && blocks[index - 1] !is DynamicContentBlock.Text) {
                    if (content.startsWith("\n")) content = content.substring(1)
                }
                // Strip one trailing newline if followed by a non-text block
                if (index < blocks.size - 1 && blocks[index + 1] !is DynamicContentBlock.Text) {
                    if (content.endsWith("\n")) content = content.substring(0, content.length - 1)
                }
                
                if (content.isEmpty()) null else index to block.copy(content = content)
            } else index to block
        }
    }
    
    fun render(blocks: List<DynamicContentBlock>): String {
        return blocks.joinToString("") { block ->
            when (block) {
                is DynamicContentBlock.Text -> block.content
                is DynamicContentBlock.Divider -> "---"
                is DynamicContentBlock.Spoiler -> "::${block.content}::"
                is DynamicContentBlock.Quote -> ">> ${block.content} <<"
                is DynamicContentBlock.Resource -> block.toTag()
            }
        }
    }
}
