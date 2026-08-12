package ru.quasaris.characters.master.backend

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.quasaris.characters.master.ModuleManifest
import ru.quasaris.characters.master.SpellCard
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class GlossaryImporter(
    private val context: Context,
    private val spellbookManager: SpellbookManager,
    private val moduleManager: ModuleManager
) {
    private val gson = GsonFactory.create()

    suspend fun importModule(
        uri: Uri,
        onProgress: (Int, Int) -> Unit,
        onDowngradeConfirm: suspend (String, String, String) -> Boolean, // moduleName, currentVersion, newVersion
        onError: (String, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return@withContext false

        val filesMap = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    // Normalize entry name to remove leading directories if any
                    val name = entry.name.split("/").last()
                    if (name.isNotEmpty()) {
                         filesMap[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            onError("Архив", "Ошибка чтения ZIP: ${e.message}")
            return@withContext false
        }

        // Let's try to find manifest.json anywhere in the archive
        val actualManifestKey = filesMap.keys.find { it.endsWith("manifest.json") }
        if (actualManifestKey == null) {
            onError("Манифест", "Файл manifest.json не найден в архиве")
            return@withContext false
        }

        val manifest = try {
            gson.fromJson(String(filesMap[actualManifestKey]!!), ModuleManifest::class.java)
        } catch (e: Exception) {
            onError("Манифест", "Ошибка парсинга manifest.json")
            null
        } ?: return@withContext false

        val installedModules = moduleManager.getInstalledModules()
        val existingModule = installedModules.find { it.manifest.id == manifest.id }
        
        if (existingModule != null) {
            val cmp = moduleManager.compareVersions(manifest.version, existingModule.manifest.version)
            if (cmp < 0) {
                val confirmed = onDowngradeConfirm(manifest.name, existingModule.manifest.version, manifest.version)
                if (!confirmed) return@withContext false
            }
        }

        val total = manifest.contents.size
        manifest.contents.forEachIndexed { index, content ->
            onProgress(index + 1, total)
            
            // Try to find the file in the archive (might be relative to manifest or absolute)
            val baseDir = actualManifestKey.substringBeforeLast("/", "")
            val fullPath = if (baseDir.isEmpty()) content.file else "$baseDir/${content.file}"
            val fileBytes = filesMap[fullPath] ?: filesMap[content.file]
            
            if (fileBytes == null) {
                onError(content.id, "Файл ${content.file} не найден в архиве")
                return@forEachIndexed
            }

            val json = String(fileBytes)
            when (content.type) {
                "spell" -> {
                    try {
                        val spell = gson.fromJson(json, SpellCard::class.java)
                        if (spell != null) {
                            val updatedSpell = spell.copy(
                                sourceModuleId = manifest.id,
                                sourceModuleVersion = manifest.version
                            )
                            
                            val allSpells = spellbookManager.loadSpells()
                            val existingSpell = allSpells.find { it.id == spell.id || (it.englishName == spell.englishName && it.englishName.isNotBlank()) }
                            if (existingSpell != null) {
                                val existingVersion = existingSpell.sourceModuleVersion ?: "0.0.0"
                                if (moduleManager.compareVersions(manifest.version, existingVersion) >= 0) {
                                    spellbookManager.addOrUpdateSpell(updatedSpell)
                                }
                            } else {
                                spellbookManager.addOrUpdateSpell(updatedSpell)
                            }
                        }
                    } catch (e: Exception) {
                        onError(content.id, "Ошибка импорта заклинания: ${e.message}")
                    }
                }
                "species", "class", "subclass", "feat" -> {
                    val dirName = when (content.type) {
                        "class" -> "classes"
                        "subclass" -> "subclasses"
                        "species" -> "species"
                        else -> "${content.type}s"
                    }
                    val dir = File(context.filesDir, "glossary/$dirName").apply { mkdirs() }
                    val destFile = File(dir, "${content.id}.json")
                    destFile.writeBytes(fileBytes)
                }
            }
        }

        moduleManager.addOrUpdateModule(manifest)
        true
    }
}
