package ru.quasaris.characternexus.backend.storage

import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterSummary

interface CharacterStorage {
    suspend fun saveCharacter(character: Character)
    suspend fun loadCharacter(uuid: String): Character?
    suspend fun deleteCharacter(uuid: String)
    suspend fun loadAllSummaries(): List<CharacterSummary>
    suspend fun saveSummaries(summaries: List<CharacterSummary>)
    suspend fun listCharacterUuids(): List<String>
    
    // Image handling
    suspend fun saveImage(uuid: String, fileName: String, bytes: ByteArray): String
    suspend fun getImagePath(uuid: String, fileName: String): String?
    suspend fun deleteImage(uuid: String, fileName: String)
}
