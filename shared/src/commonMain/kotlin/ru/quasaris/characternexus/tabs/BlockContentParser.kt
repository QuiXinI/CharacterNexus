package ru.quasaris.characternexus.tabs

import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.NoteBlockState

object BlockContentParser {

    fun toBlocks(text: String): List<DynamicContentBlock> {
        if (text.isEmpty()) return listOf(DynamicContentBlock.Text(""))

        val initial = DynamicContentParser.parse(text)
        val result = mutableListOf<DynamicContentBlock>()

        initial.forEachIndexed { i, block ->
            if (block is DynamicContentBlock.Text) {
                var lines = block.content.split("\n")

                if (i > 0 && initial[i-1] !is DynamicContentBlock.Text) {
                    if (lines.firstOrNull()?.isEmpty() == true) {
                        lines = lines.drop(1)
                    }
                }
                if (i < initial.size - 1 && initial[i+1] !is DynamicContentBlock.Text) {
                    if (lines.lastOrNull()?.isEmpty() == true) {
                        lines = lines.dropLast(1)
                    }
                }

                lines.forEach { result.add(DynamicContentBlock.Text(it)) }
            } else {
                result.add(block)
            }
        }

        return if (result.isEmpty()) listOf(DynamicContentBlock.Text("")) else result
    }

    fun toNoteBlocks(text: String): List<NoteBlockState> = toBlocks(text).map { NoteBlockState(block = it) }

    fun toText(blocks: List<DynamicContentBlock>): String =
        blocks.joinToString("\n") { renderLine(it) }

    fun renderLine(block: DynamicContentBlock): String = when (block) {
        is DynamicContentBlock.Text -> block.content
        is DynamicContentBlock.Divider -> "---"
        is DynamicContentBlock.Spoiler -> "::${block.content}::"
        is DynamicContentBlock.Quote -> ">> ${block.content} <<"
        is DynamicContentBlock.Resource -> block.toTag()
        is DynamicContentBlock.ResourceRef -> block.toTag()
    }

    fun reconcile(previous: List<NoteBlockState>, freshBlocks: List<DynamicContentBlock>): List<NoteBlockState> {
        val bucket = previous.groupByTo(HashMap()) { signature(it.block) }
        return freshBlocks.map { b ->
            val list = bucket[signature(b)]
            val reused = list?.removeFirstOrNull()
            reused?.copy(block = b) ?: NoteBlockState(block = b)
        }
    }

    private fun signature(block: DynamicContentBlock): String = when (block) {
        is DynamicContentBlock.Text -> "T:${block.content}"
        is DynamicContentBlock.Divider -> "D"
        is DynamicContentBlock.Spoiler -> "S:${block.content}"
        is DynamicContentBlock.Quote -> "Q:${block.content}"
        is DynamicContentBlock.Resource -> "R:${block.id}:${block.name}:${block.current}:${block.max}"
        is DynamicContentBlock.ResourceRef -> "RR:${block.id}"
    }
}
