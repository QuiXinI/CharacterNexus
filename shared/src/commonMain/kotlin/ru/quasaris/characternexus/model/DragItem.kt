package ru.quasaris.characternexus.model

typealias CharacterData = CharacterSummary
typealias FolderData = CharacterFolder

sealed interface DragItem {
    val id: String

    data class Character(
        override val id: String,
        val data: CharacterData
    ) : DragItem

    data class Folder(
        override val id: String,
        val data: FolderData
    ) : DragItem
}
