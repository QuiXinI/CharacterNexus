package ru.quasaris.characters.master

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

import ru.quasaris.characters.master.utils.GsonFactory

class CharacterRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    private val gson = GsonFactory.create()
    private val charactersKey = "CHARACTERS_LIST"

    fun loadCharacters(): MutableList<Character> {
        val json = sharedPreferences.getString(charactersKey, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Character>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveCharacters(characters: List<Character>) {
        val json = gson.toJson(characters)
        sharedPreferences.edit { putString(charactersKey, json) }
    }
}