package ru.quasaris.characters.master.backend

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import ru.quasaris.characters.master.CharacterClass
import ru.quasaris.characters.master.MaterialComponentType
import ru.quasaris.characters.master.SpellCard
import ru.quasaris.characters.master.SpellSchool
import ru.quasaris.characters.master.SpellVersion
import ru.quasaris.characters.master.MagicAttackType
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SpellbookManager(private val context: Context) {
    private val gson = GsonFactory.create()
    private val exportGson = GsonFactory.createPretty()
    private val spellbookFile = File(context.filesDir, "spellbook.json")

    private var cachedSpells: MutableList<SpellCard>? = null

    enum class ImportAction {
        REPLACE, RENAME, SKIP, CANCEL
    }

    fun loadSpells(): List<SpellCard> {
        val currentCached = cachedSpells
        if (currentCached != null) return currentCached
        
        if (!spellbookFile.exists()) {
            cachedSpells = mutableListOf()
            return cachedSpells!!
        }

        return try {
            val json = spellbookFile.readText()
            val type = object : com.google.gson.reflect.TypeToken<List<SpellCard>>() {}.type
            val spells: List<SpellCard> = gson.fromJson(json, type)
            cachedSpells = spells.toMutableList()
            cachedSpells!!
        } catch (e: Exception) {
            e.printStackTrace()
            cachedSpells = mutableListOf()
            cachedSpells!!
        }
    }

    fun saveSpells(spells: List<SpellCard>) {
        cachedSpells = spells.toMutableList()
        try {
            val json = gson.toJson(spells)
            spellbookFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateSpell(spell: SpellCard) {
        val spells = loadSpells().toMutableList()
        val index = spells.indexOfFirst { it.id == spell.id }
        if (index != -1) {
            spells[index] = spell
        } else {
            spells.add(spell)
        }
        saveSpells(spells)
    }

    fun deleteSpell(spellId: String) {
        val spells = loadSpells().toMutableList()
        spells.removeAll { it.id == spellId }
        saveSpells(spells)
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

    suspend fun exportSpellbook(uri: Uri, spellIds: List<String>? = null) = withContext(Dispatchers.IO) {
        val spells = if (spellIds == null) loadSpells() else loadSpells().filter { it.id in spellIds }
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    spells.forEach { spell ->
                        val json = exportGson.toJson(spell)
                        val safeName = spell.name.ifBlank { spell.id }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        zos.putNextEntry(ZipEntry("$safeName.json"))
                        zos.write(json.toByteArray())
                        zos.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportSingleSpell(uri: Uri, spell: SpellCard) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                val json = exportGson.toJson(spell)
                os.write(json.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun importSpells(
        uri: Uri, 
        onProgress: (Int, Int) -> Unit, 
        onError: (String, String, (ImportAction) -> Unit) -> Unit
    ) = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return@withContext

        val rawJsonList = mutableListOf<Pair<String, String>>() // filename, content

        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".json")) {
                            val content = String(zis.readBytes())
                            rawJsonList.add(entry.name to content)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            rawJsonList.add("imported_spell.json" to String(bytes))
        }

        val total = rawJsonList.size
        rawJsonList.forEachIndexed { index, (filename, content) ->
            onProgress(index + 1, total)
            
            try {
                val spell = gson.fromJson(content, SpellCard::class.java)
                val isValid = spell != null && (spell.name.isNotBlank() || spell.englishName.isNotBlank())
                
                if (!isValid) {
                    val result = kotlinx.coroutines.CompletableDeferred<ImportAction>()
                    onError(spell?.name ?: filename, "Отсутствует название заклинания") { result.complete(it) }
                    val action = result.await()
                    if (action == ImportAction.CANCEL) return@withContext
                } else {
                    val currentSpells = loadSpells()
                    val duplicate = currentSpells.find { it.name == spell.name && it.englishName == spell.englishName }
                    
                    var spellToSave = spell!!
                    if (duplicate != null) {
                        val result = kotlinx.coroutines.CompletableDeferred<ImportAction>()
                        onError(spell.name, "Заклиние с такими названиями уже существует") { result.complete(it) }
                        val action = result.await()
                        when (action) {
                            ImportAction.REPLACE -> { /* Duplicate handling logic if needed, here we just update */ }
                            ImportAction.RENAME -> {
                                spellToSave = spell.copy(name = "${spell.name} (Копия)", id = java.util.UUID.randomUUID().toString())
                            }
                            ImportAction.SKIP -> return@forEachIndexed
                            ImportAction.CANCEL -> return@withContext
                        }
                    }
                    
                    addOrUpdateSpell(spellToSave)
                }
            } catch (e: Exception) {
                val result = kotlinx.coroutines.CompletableDeferred<ImportAction>()
                onError(filename, e.message ?: "Ошибка чтения файла") { result.complete(it) }
                val action = result.await()
                if (action == ImportAction.CANCEL) return@withContext
            }
        }
    }
}
