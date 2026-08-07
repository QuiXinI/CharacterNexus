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
    private val glossaryDir = File(context.filesDir, "glossary/spells").apply { mkdirs() }

    private var cachedSpells: MutableList<SpellCard>? = null

    init {
        migrateIfNeeded()
    }

    private fun migrateIfNeeded() {
        if (spellbookFile.exists()) {
            try {
                val json = spellbookFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<List<SpellCard>>() {}.type
                val spells: List<SpellCard> = gson.fromJson(json, type)
                spells.forEach { saveSingleSpell(it) }
                // Rename to backup
                spellbookFile.renameTo(File(context.filesDir, "spellbook.json.bak"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun getFileForSpell(spell: SpellCard): File {
        val identifier = if (spell.englishName.isNotBlank()) slugify(spell.englishName) else spell.id
        return File(glossaryDir, "$identifier.json")
    }

    fun loadSpells(): List<SpellCard> {
        val currentCached = cachedSpells
        if (currentCached != null) return currentCached
        
        val spells = mutableListOf<SpellCard>()
        glossaryDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val spell = gson.fromJson(file.readText(), SpellCard::class.java)
                if (spell != null) spells.add(spell)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        cachedSpells = spells.sortedBy { it.name }.toMutableList()
        return cachedSpells!!
    }

    private fun saveSingleSpell(spell: SpellCard) {
        try {
            val file = getFileForSpell(spell)
            val json = exportGson.toJson(spell)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateSpell(spell: SpellCard) {
        val allSpells = loadSpells()
        val oldSpell = allSpells.find { it.id == spell.id }
        
        if (oldSpell != null) {
            val oldFile = getFileForSpell(oldSpell)
            val newFile = getFileForSpell(spell)
            if (oldFile.absolutePath != newFile.absolutePath) {
                oldFile.delete()
            }
        }

        saveSingleSpell(spell)
        cachedSpells = null // Invalidate cache
    }

    fun deleteSpell(spellId: String) {
        val spells = loadSpells()
        val spellToDelete = spells.find { it.id == spellId }
        spellToDelete?.let {
            getFileForSpell(it).delete()
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

    suspend fun exportSpellbook(uri: Uri, manifest: ru.quasaris.characters.master.ModuleManifest, spellIds: List<String>? = null) = withContext(Dispatchers.IO) {
        val allSpells = loadSpells()
        val spells = if (spellIds == null) allSpells else allSpells.filter { it.id in spellIds }
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                java.util.zip.ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    // Add manifest
                    zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                    val updatedManifest = manifest.copy(
                        contents = spells.map { spell ->
                            ru.quasaris.characters.master.ModuleContent(
                                type = "spell",
                                id = spell.id,
                                file = "${slugify(spell.englishName.ifBlank { spell.name })}.json"
                            )
                        }
                    )
                    zos.write(exportGson.toJson(updatedManifest).toByteArray())
                    zos.closeEntry()

                    spells.forEach { spell ->
                        val json = exportGson.toJson(spell.copy(sourceModuleId = manifest.id, sourceModuleVersion = manifest.version))
                        val fileName = "${slugify(spell.englishName.ifBlank { spell.name })}.json"
                        zos.putNextEntry(java.util.zip.ZipEntry(fileName))
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
