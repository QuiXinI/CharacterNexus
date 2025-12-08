package com.example.myapplication

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class CharacterRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
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
        sharedPreferences.edit().putString(charactersKey, json).apply()
    }

    fun saveImagePermanently(uri: Uri): Uri? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Создаем уникальное имя файла
        val fileName = "char_image_${UUID.randomUUID()}.jpg"
        // Получаем доступ к приватной папке files внутри вашего приложения
        val file = File(context.filesDir, fileName)

        // Копируем данные из inputStream в новый файл
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        // Возвращаем URI для нашего локального файла
        return Uri.fromFile(file)
    }
}