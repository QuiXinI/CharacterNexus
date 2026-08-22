package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.util.*

class SpellbookManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val glossaryDir = getAppDataDir().div("glossary").div("spells")
    private var cachedSpells: MutableList<SpellCard>? = null

    init {
        if (!platformFileSystem.exists(glossaryDir)) {
            platformFileSystem.createDirectories(glossaryDir)
        }
    }

    enum class ImportAction {
        REPLACE, RENAME, SKIP, CANCEL
    }

    fun slugify(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_'\\-.()]"), "")
            .ifBlank { "unnamed" }
    }

    private fun getFileForSpell(spell: SpellCard): Path {
        val identifier = if (spell.englishName.isNotBlank()) slugify(spell.englishName) else spell.id
        return glossaryDir.div("$identifier.json")
    }

    fun loadSpells(): List<SpellCard> {
        val currentCached = cachedSpells
        if (currentCached != null) return currentCached
        
        val spells = mutableListOf<SpellCard>()
        try {
            if (platformFileSystem.exists(glossaryDir)) {
                platformFileSystem.list(glossaryDir).forEach { file ->
                    if (file.name.endsWith(".json")) {
                        try {
                            val content = platformFileSystem.read(file) { readUtf8() }
                            val spell = json.decodeFromString<SpellCard>(content)
                            spells.add(spell)
                        } catch (e: Exception) {
                            e.log()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.log()
        }
        
        cachedSpells = spells.sortedBy { it.name }.toMutableList()
        return cachedSpells!!
    }

    private fun saveSingleSpell(spell: SpellCard) {
        try {
            val file = getFileForSpell(spell)
            val content = json.encodeToString(spell)
            platformFileSystem.write(file) {
                writeUtf8(content)
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    fun addOrUpdateSpell(spell: SpellCard) {
        val allSpells = loadSpells()
        
        // Match logic: 
        // 1. ID match (always update)
        // 2. Composite key match (Name + Version + English Name + Source Module ID)
        val existingMatch = allSpells.find { it.id == spell.id }
            ?: allSpells.find { 
                it.name.equals(spell.name, ignoreCase = true) && 
                it.version == spell.version && 
                it.englishName.equals(spell.englishName, ignoreCase = true) &&
                it.sourceModuleId == spell.sourceModuleId
            }
        
        val spellToSave = if (existingMatch != null) {
            spell.copy(id = existingMatch.id) // Preserve existing ID
        } else {
            spell
        }

        if (existingMatch != null) {
            val oldFile = getFileForSpell(existingMatch)
            val newFile = getFileForSpell(spellToSave)
            if (oldFile != newFile) {
                platformFileSystem.delete(oldFile)
            }
        }

        saveSingleSpell(spellToSave)
        cachedSpells = null // Invalidate cache
    }

    fun deleteSpell(spellId: String) {
        val spells = loadSpells()
        val spellToDelete = spells.find { it.id == spellId }
        spellToDelete?.let {
            val file = getFileForSpell(it)
            if (platformFileSystem.exists(file)) {
                platformFileSystem.delete(file)
            }
        }
        cachedSpells = null // Invalidate cache
    }

    fun resolveRef(ref: String): SpellCard? {
        val path = ref.removePrefix("ref://spells/").lowercase()
        val spells = loadSpells()
        // Try slug match first
        return spells.find { slugify(it.englishName) == path } 
            ?: spells.find { it.id == path }
    }

    fun searchSpells(query: String): List<SpellCard> {
        val spells = loadSpells()
        if (query.isBlank()) return spells
        val q = query.lowercase().trim()
        return spells.filter {
            it.name.lowercase().contains(q) || 
            (it.showEnglishName && it.englishName.lowercase().contains(q))
        }
    }

    suspend fun importSpells(
        bytes: ByteArray,
        onProgress: (Int, Int) -> Unit,
        onError: (String, String, (ImportAction) -> Unit) -> Unit
    ) = withContext(ioDispatcher) {
        try {
            val content = bytes.decodeToString()
            val importedSpells = json.decodeFromString<List<SpellCard>>(content)
            val total = importedSpells.size
            importedSpells.forEachIndexed { index, spell ->
                addOrUpdateSpell(spell)
                onProgress(index + 1, total)
            }
        } catch (e: Exception) {
            onError("Ошибка импорта", e.message ?: "Неизвестная ошибка") { }
        }
    }

    suspend fun exportSpellbook(
        path: String,
        manifest: ModuleManifest,
        spellIds: List<String>? = null
    ) = withContext(ioDispatcher) {
        try {
            val spells = loadSpells().filter { spellIds == null || it.id in spellIds }
            val outputPath = Path.Companion.run { path.toPath() }
            
            // For now, let's just export a JSON of spells
            // In a real module, it would be a ZIP with manifest.json and contents/
            val content = json.encodeToString(spells)
            platformFileSystem.write(outputPath) {
                writeUtf8(content)
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    suspend fun exportSingleSpell(
        path: String,
        spell: SpellCard
    ) {
        // val content = json.encodeToString(spell)
    }
}
