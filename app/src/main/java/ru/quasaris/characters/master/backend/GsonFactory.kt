package ru.quasaris.characters.master.backend

import com.google.gson.*
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.SpellCard
import java.lang.reflect.Type

object GsonFactory {
    fun create(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Character::class.java, CharacterDeserializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardSerializer())
            .create()
    }

    fun createPretty(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Character::class.java, CharacterDeserializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardSerializer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }
}

class SpellCardSerializer : JsonSerializer<SpellCard> {
    override fun serialize(src: SpellCard, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()
        json.addProperty("name", src.name)
        json.addProperty("showEnglishName", src.showEnglishName)
        json.addProperty("englishName", src.englishName)
        json.addProperty("level", src.level)
        json.add("version", context.serialize(src.version))
        json.add("school", context.serialize(src.school))
        json.add("classes", context.serialize(src.classes))
        json.add("castingTimeType", context.serialize(src.castingTimeType))
        json.addProperty("castingTime", src.castingTime)
        json.addProperty("hasVerbalComponent", src.hasVerbalComponent)
        json.addProperty("hasSomaticComponent", src.hasSomaticComponent)
        json.add("materialComponentType", context.serialize(src.materialComponentType))
        json.addProperty("materialComponents", src.materialComponents)
        json.addProperty("hasConcentration", src.hasConcentration)
        json.addProperty("durationValue", src.durationValue)
        json.add("durationUnit", context.serialize(src.durationUnit))
        json.addProperty("isRitual", src.isRitual)
        json.addProperty("isCircle", src.isCircle)
        json.addProperty("description", src.description)
        json.addProperty("hasDamage", src.hasDamage)
        json.addProperty("damageFormula", src.damageFormula)
        json.addProperty("damageType", src.damageType)
        json.add("attackType", context.serialize(src.attackType))
        json.add("savingThrowAttributes", context.serialize(src.savingThrowAttributes))
        json.addProperty("distance", src.distance)
        json.addProperty("notes", src.notes)
        json.addProperty("link", src.link)
        json.add("additionalLinks", context.serialize(src.additionalLinks))
        json.addProperty("id", src.id)
        return json
    }
}

class CharacterDeserializer : JsonDeserializer<Character> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Character {
        val jsonObject = json.asJsonObject
        
        // Ensure attacks is never null in the resulting object
        if (!jsonObject.has("attacks") || jsonObject.get("attacks").isJsonNull) {
            jsonObject.add("attacks", JsonArray())
        }

        if (!jsonObject.has("statBonuses") || jsonObject.get("statBonuses").isJsonNull) {
            jsonObject.add("statBonuses", JsonArray())
        }

        if (!jsonObject.has("skillBonuses") || jsonObject.get("skillBonuses").isJsonNull) {
            jsonObject.add("skillBonuses", JsonArray())
        }

        if (!jsonObject.has("wallet") || jsonObject.get("wallet").isJsonNull) {
            jsonObject.add("wallet", JsonObject())
        }
        
        // Fallback for other potential missing fields if necessary
        // ...
        
        return Gson().fromJson(jsonObject, Character::class.java)
    }
}
