package ru.quasaris.characternexus.model

import kotlinx.serialization.Serializable
import ru.quasaris.characternexus.util.generateUuid

@Serializable
sealed class DynamicContentBlock {
    @Serializable
    data class Text(val content: String) : DynamicContentBlock()
    
    @Serializable
    data object Divider : DynamicContentBlock()
    
    @Serializable
    data class Spoiler(val content: String) : DynamicContentBlock()
    
    @Serializable
    data class Quote(val content: String) : DynamicContentBlock()
    
    @Serializable
    data class ResourceRef(val id: String) : DynamicContentBlock() {
        fun toTag(): String = "{Ресурс: id=$id}"
    }
    
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
}
