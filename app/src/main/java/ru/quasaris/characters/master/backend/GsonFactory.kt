package ru.quasaris.characters.master.backend

import com.google.gson.*
import ru.quasaris.characters.master.Character
import java.lang.reflect.Type

object GsonFactory {
    fun create(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Character::class.java, CharacterDeserializer())
            .create()
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
        
        // Fallback for other potential missing fields if necessary
        // ...
        
        return Gson().fromJson(jsonObject, Character::class.java)
    }
}
