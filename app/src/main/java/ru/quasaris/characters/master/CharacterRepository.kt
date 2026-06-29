package ru.quasaris.characters.master

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

import ru.quasaris.characters.master.utils.GsonFactory

class CharacterRepository(
    context: Context,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val sharedPreferences = context.getSharedPreferences("character_prefs", Context.MODE_PRIVATE)
    private val gson = GsonFactory.create()
    private val charactersKey = "CHARACTERS_LIST"

    private val _charactersState = MutableStateFlow<List<Character>>(loadCharactersInternal())
    val charactersState: StateFlow<List<Character>> = _charactersState.asStateFlow()

    init {
        observeChanges()
    }

    @OptIn(FlowPreview::class)
    private fun observeChanges() {
        appScope.launch {
            _charactersState
                .drop(1) // Skip initial load
                .debounce(500)
                .distinctUntilChanged()
                .collect { characters ->
                    saveToDisk(characters, "Debounced Save")
                }
        }
    }

    private fun loadCharactersInternal(): List<Character> {
        val json = sharedPreferences.getString(charactersKey, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Character>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun loadCharacters(): List<Character> = _charactersState.value

    fun updateCharacters(characters: List<Character>) {
        _charactersState.value = characters.toList()
    }

    fun flush() {
        val currentData = _charactersState.value
        runBlocking(Dispatchers.IO) {
            saveToDisk(currentData, "Forced Flush (onStop)")
        }
    }

    private fun saveToDisk(characters: List<Character>, reason: String) {
        Log.d("CharacterRepository", "[$reason] Saving ${characters.size} characters to disk")
        val json = gson.toJson(characters)
        sharedPreferences.edit { putString(charactersKey, json) }
    }

    fun saveCharacters(characters: List<Character>) {
        updateCharacters(characters)
    }
}
