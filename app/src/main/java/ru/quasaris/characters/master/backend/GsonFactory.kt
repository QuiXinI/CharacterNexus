package ru.quasaris.characters.master.backend

import com.google.gson.*
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.DamageType
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.SpellCard
import java.lang.reflect.Type

object GsonFactory {
    fun create(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Character::class.java, CharacterDeserializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardSerializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardDeserializer())
            .create()
    }

    fun createPretty(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Character::class.java, CharacterDeserializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardSerializer())
            .registerTypeAdapter(SpellCard::class.java, SpellCardDeserializer())
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
        json.addProperty("isRitual", src.isRitual)
        json.addProperty("isCircle", src.isCircle)
        json.add("materialComponentType", context.serialize(src.materialComponentType))
        json.addProperty("materialComponents", src.materialComponents)
        json.addProperty("hasConcentration", src.hasConcentration)
        json.addProperty("durationValue", src.durationValue)
        json.add("durationUnit", context.serialize(src.durationUnit))
        json.addProperty("description", src.description)
        json.addProperty("hasDamage", src.hasDamage)
        json.addProperty("damageFormula", src.damageFormula)
        json.addProperty("damageType", src.damageType)
        json.add("damageTypes", context.serialize(src.damageTypes))
        json.add("additionalDamageFormulas", context.serialize(src.additionalDamageFormulas))
        json.add("additionalDamageTypesList", context.serialize(src.additionalDamageTypesList))
        json.add("attackType", context.serialize(src.attackType))
        json.add("attackTypes", context.serialize(src.attackTypes))
        json.add("savingThrowAttributes", context.serialize(src.savingThrowAttributes))
        json.addProperty("distance", src.distance)
        json.addProperty("notes", src.notes)
        json.addProperty("link", src.link)
        json.add("additionalLinks", context.serialize(src.additionalLinks))
        json.addProperty("id", src.id)
        return json
    }
}

class SpellCardDeserializer : JsonDeserializer<SpellCard> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SpellCard {
        val jsonObject = json.asJsonObject
        
        // Migration for attackType -> attackTypes
        if (!jsonObject.has("attackTypes") || jsonObject.get("attackTypes").isJsonNull) {
            val attackTypes = JsonArray()
            if (jsonObject.has("attackType") && !jsonObject.get("attackType").isJsonNull) {
                attackTypes.add(jsonObject.get("attackType"))
            }
            jsonObject.add("attackTypes", attackTypes)
        }

        // Migration for damageType (String) -> damageTypes (List<DamageType>)
        if (!jsonObject.has("damageTypes") || jsonObject.get("damageTypes").isJsonNull) {
            val damageTypesList = JsonArray()
            if (jsonObject.has("damageType") && !jsonObject.get("damageType").isJsonNull) {
                val oldType = jsonObject.get("damageType").asString
                if (oldType.isNotBlank()) {
                    val parts = oldType.split(Regex("[/,]")).map { it.trim() }.filter { it.isNotBlank() }
                    parts.forEach { part ->
                        val enumType = DamageType.fromDisplayName(part)
                        if (enumType != null) {
                            damageTypesList.add(part) // GSON will handle string to enum if it matches displayName or name? No, usually it matches name.
                        } else {
                            damageTypesList.add(DamageType.OTHER.name)
                        }
                    }
                }
            }
            // Actually it's better to use names for enums in JSON
            val damageTypesNamesList = JsonArray()
            if (jsonObject.has("damageType") && !jsonObject.get("damageType").isJsonNull) {
                val oldType = jsonObject.get("damageType").asString
                if (oldType.isNotBlank()) {
                    val parts = oldType.split(Regex("[/,]")).map { it.trim() }.filter { it.isNotBlank() }
                    parts.forEach { part ->
                        val enumType = DamageType.fromDisplayName(part)
                        if (enumType != null) {
                            damageTypesNamesList.add(enumType.name)
                        } else {
                            damageTypesNamesList.add(DamageType.OTHER.name)
                        }
                    }
                }
            }
            jsonObject.add("damageTypes", damageTypesNamesList)
        }

        // Initialize other new lists if missing
        if (!jsonObject.has("additionalDamageFormulas") || jsonObject.get("additionalDamageFormulas").isJsonNull) {
            jsonObject.add("additionalDamageFormulas", JsonArray())
        }
        if (!jsonObject.has("additionalDamageTypesList") || jsonObject.get("additionalDamageTypesList").isJsonNull) {
            jsonObject.add("additionalDamageTypesList", JsonArray())
        }
        
        return Gson().fromJson(jsonObject, SpellCard::class.java)
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
