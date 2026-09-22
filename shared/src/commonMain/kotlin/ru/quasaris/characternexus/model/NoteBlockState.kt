package ru.quasaris.characternexus.model

import ru.quasaris.characternexus.util.generateUuid

data class NoteBlockState(
    val key: String = generateUuid(),
    val block: DynamicContentBlock
) {
    val textContent: String?
        get() = when (block) {
            is DynamicContentBlock.Text -> block.content
            is DynamicContentBlock.Spoiler -> block.content
            is DynamicContentBlock.Quote -> block.content
            else -> null
        }

    val isTextLike: Boolean
        get() = block is DynamicContentBlock.Text || block is DynamicContentBlock.Spoiler || block is DynamicContentBlock.Quote

    fun withTextContent(newText: String): NoteBlockState = copy(
        block = when (block) {
            is DynamicContentBlock.Text -> block.copy(content = newText)
            is DynamicContentBlock.Spoiler -> block.copy(content = newText)
            is DynamicContentBlock.Quote -> block.copy(content = newText)
            else -> block
        }
    )
}
