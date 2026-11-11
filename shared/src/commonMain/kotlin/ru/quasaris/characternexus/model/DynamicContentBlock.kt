package ru.quasaris.characternexus.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import ru.quasaris.characternexus.util.generateUuid

@Immutable
@Serializable
sealed class DynamicContentBlock {
    @Immutable
    @Serializable
    data class Text(val content: String) : DynamicContentBlock()
    
    @Immutable
    @Serializable
    data object Divider : DynamicContentBlock()
    
    @Immutable
    @Serializable
    data class Spoiler(val content: String) : DynamicContentBlock()
    
    @Immutable
    @Serializable
    data class Quote(val content: String) : DynamicContentBlock()
    
    @Immutable
    @Serializable
    data class ResourceRef(val id: String) : DynamicContentBlock() {
        fun toTag(): String = "{Ресурс: id=$id}"
    }
    
    @Immutable
    @Serializable
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
            val actualId = id.ifEmpty { generateUuid() }
            parts.add("id=$actualId")
            
            return "{Ресурс: ${parts.joinToString(" | ")}}"
        }
    }

    @Immutable
    @Serializable
    data class InfoBlockRef(val id: String) : DynamicContentBlock() {
        fun toTag(): String = "{Инфоблок: id=$id}"
    }

    @Immutable
    @Serializable
    data class InfoBlock(
        val title: String,
        val link: String? = null,
        val description: String = "",
        val id: String = ""
    ) : DynamicContentBlock() {
        fun toTag(): String {
            val parts = mutableListOf<String>()
            parts.add(title)
            if (!link.isNullOrBlank()) parts.add("link=$link")
            if (description.isNotEmpty()) parts.add("desc=$description")
            val actualId = id.ifEmpty { generateUuid() }
            parts.add("id=$actualId")
            return "{Инфоблок: ${parts.joinToString(" | ")}}"
        }
    }

    @Immutable
    @Serializable
    data class AttackRef(val id: String) : DynamicContentBlock() {
        fun toTag(): String = "{Атака: id=$id}"
    }

    @Immutable
    @Serializable
    data class Attack(
        val name: String,
        val id: String = "",
        val notes: String = ""
    ) : DynamicContentBlock() {
        fun toTag(): String {
            val parts = mutableListOf<String>()
            parts.add(name)
            if (notes.isNotEmpty()) parts.add("notes=$notes")
            val actualId = id.ifEmpty { generateUuid() }
            parts.add("id=$actualId")
            return "{Атака: ${parts.joinToString(" | ")}}"
        }
    }

    @Immutable
    @Serializable
    data class SpellRef(
        val id: String = "",
        val spellId: String? = null
    ) : DynamicContentBlock() {
        fun toTag(): String {
            val parts = mutableListOf<String>()
            if (!spellId.isNullOrBlank()) parts.add("spellId=$spellId")
            val actualId = id.ifEmpty { generateUuid() }
            parts.add("id=$actualId")
            return "{Заклинание: ${parts.joinToString(" | ")}}"
        }
    }

    @Immutable
    @Serializable
    data class FolderRef(
        val id: String = ""
    ) : DynamicContentBlock() {
        fun toTag(): String = "{Папка: id=${id.ifEmpty { generateUuid() }}}"
    }

    @Immutable
    @Serializable
    data class TableCell(
        val text: String = "",
        val colSpan: Int = 1,
        val rowSpan: Int = 1,
        val isHeader: Boolean = false
    )

    @Immutable
    @Serializable
    data class Table(
        val rows: List<List<TableCell>>,
        val id: String = ""
    ) : DynamicContentBlock() {
        fun toTag(): String {
            val actualId = id.ifEmpty { generateUuid() }
            val jsonPayload = Json.encodeToString(rows)
            val base64Data = jsonPayload.encodeUtf8().base64()
            return "{Таблица: id=$actualId | data=$base64Data}"
        }
    }
}
