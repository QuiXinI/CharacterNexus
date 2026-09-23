package ru.quasaris.characternexus.tabs

import ru.quasaris.characternexus.model.DynamicContentBlock

object DynamicContentParser {
    private val dividerRegex = Regex("^[ \\t\\u200B\\uFEFF]*---[ \\t\\u200B\\uFEFF]*$", RegexOption.MULTILINE)
    private val spoilerRegex = Regex("(?s)::(.*?)::")
    private val quoteRegex = Regex("(?s)>> (.*?)(?: <<|$)")
    private val resourceRegex = Regex("(?s)\\{(?:Ресурс|Resource)[:=]\\s*(.*?)\\}", RegexOption.IGNORE_CASE)

    fun parse(text: String, resources: Map<String, DynamicContentBlock.Resource> = emptyMap()): List<DynamicContentBlock> {
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
            val params = mutableMapOf<String, String>()
            var namePart = ""
            
            parts.forEachIndexed { index, part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0].trim().lowercase()] = kv[1].trim()
                } else if (index == 0) {
                    namePart = part
                }
            }

            val id = params["id"] ?: ""
            
            // If it only has an ID and no other data, it's a Ref
            val isRef = id.isNotEmpty() && params.size == 1 && namePart.isEmpty()
            
            if (isRef) {
                allMatches.add(match.range to DynamicContentBlock.ResourceRef(id))
            } else {
                val block = DynamicContentBlock.Resource(
                    name = if (namePart.isNotEmpty()) namePart else params["name"] ?: "Ресурс",
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
                    id = id
                )
                allMatches.add(match.range to block)
            }
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
                is DynamicContentBlock.ResourceRef -> block.toTag()
            }
        }
    }

    fun resourceIds(text: String): Set<String> {
        return resourceRegex.findAll(text).mapNotNull { match ->
            val content = match.groupValues[1]
            content.split("|").map { it.trim() }
                .firstOrNull { it.startsWith("id=") }
                ?.substringAfter("=")
        }.toSet()
    }

    fun rewriteInlineResources(text: String, transform: (DynamicContentBlock.Resource) -> DynamicContentBlock): String {
        val blocks = parse(text)
        val updated = blocks.map { block ->
            if (block is DynamicContentBlock.Resource) transform(block) else block
        }
        return render(updated)
    }

    fun relinkResource(text: String, index: Int, newId: String, targetResourceId: String? = null): String {
        val blocks = BlockContentParser.toBlocks(text).toMutableList()
        var targetIndex = if (index in blocks.indices && isResourceOrRef(blocks[index], targetResourceId)) index else -1
        if (targetIndex == -1 && !targetResourceId.isNullOrEmpty()) {
            targetIndex = blocks.indexOfFirst { isResourceOrRef(it, targetResourceId) }
        }
        if (targetIndex in blocks.indices) {
            blocks[targetIndex] = DynamicContentBlock.ResourceRef(newId)
        }
        return BlockContentParser.toText(blocks)
    }

    fun removeResource(text: String, index: Int, targetResourceId: String? = null): String {
        val blocks = BlockContentParser.toBlocks(text).toMutableList()
        var targetIndex = if (index in blocks.indices && isResourceOrRef(blocks[index], targetResourceId)) index else -1
        if (targetIndex == -1 && !targetResourceId.isNullOrEmpty()) {
            targetIndex = blocks.indexOfFirst { isResourceOrRef(it, targetResourceId) }
        }
        if (targetIndex in blocks.indices) {
            blocks.removeAt(targetIndex)
        }
        return BlockContentParser.toText(blocks)
    }

    private fun isResourceOrRef(block: DynamicContentBlock, targetResourceId: String? = null): Boolean = when (block) {
        is DynamicContentBlock.Resource -> targetResourceId.isNullOrEmpty() || block.id == targetResourceId
        is DynamicContentBlock.ResourceRef -> targetResourceId.isNullOrEmpty() || block.id == targetResourceId
        else -> false
    }

    fun removeResourceById(text: String, id: String): String {
        val blocks = BlockContentParser.toBlocks(text).filterNot {
            (it is DynamicContentBlock.Resource && it.id == id) ||
            (it is DynamicContentBlock.ResourceRef && it.id == id)
        }
        return BlockContentParser.toText(blocks)
    }
}
