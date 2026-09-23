package ru.quasaris.characternexus.model

import androidx.compose.runtime.Immutable
import ru.quasaris.characternexus.util.generateUuid

@Immutable
data class NoteBlockState(
    val key: String = generateUuid(),
    val block: DynamicContentBlock
) {
    val textContent: String?
        get() = when (block) {
            is DynamicContentBlock.Text -> block.content
            is DynamicContentBlock.Spoiler -> block.content
            is DynamicContentBlock.Quote -> block.content
            is DynamicContentBlock.Divider -> "---"
            else -> null
        }

    val isTextLike: Boolean
        get() = block is DynamicContentBlock.Text || block is DynamicContentBlock.Spoiler || block is DynamicContentBlock.Quote || block is DynamicContentBlock.Divider

    fun withTextContent(newText: String): NoteBlockState {
        val trimmed = newText.trim().replace("\u200B", "").replace("\uFEFF", "")
        val isDivider = trimmed == "---"
        return copy(
            block = when (block) {
                is DynamicContentBlock.Text -> if (isDivider) DynamicContentBlock.Divider else block.copy(content = newText)
                is DynamicContentBlock.Spoiler -> if (isDivider) DynamicContentBlock.Divider else block.copy(content = newText)
                is DynamicContentBlock.Quote -> if (isDivider) DynamicContentBlock.Divider else block.copy(content = newText)
                is DynamicContentBlock.Divider -> if (isDivider) block else DynamicContentBlock.Text(newText)
                else -> block
            }
        )
    }
}
