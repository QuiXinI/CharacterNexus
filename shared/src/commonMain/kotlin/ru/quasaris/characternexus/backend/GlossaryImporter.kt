package ru.quasaris.characternexus.backend

import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.util.*

class GlossaryImporter(
    private val spellbookManager: SpellbookManager,
    private val moduleManager: ModuleManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun importModule(
        bytes: ByteArray,
        onProgress: (Int, Int) -> Unit,
        onDowngradeConfirm: suspend (String, String, String) -> Boolean,
        onError: (String, String) -> Unit
    ): Boolean = withContext(ioDispatcher) {
        try {
            var files = ZipUtils.unzip(bytes)
            
            // Handle plain JSON if zip fails or is empty
            if (files.isEmpty()) {
                val content = bytes.decodeToString()
                if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                    files = mapOf("module.json" to bytes)
                }
            }
            
            if (files.isEmpty()) return@withContext false

            val manifestEntry = files.entries.find { it.key.equals("manifest.json", ignoreCase = true) || it.key.endsWith("/manifest.json", ignoreCase = true) }
            val manifest = if (manifestEntry != null) {
                json.decodeFromString<ModuleManifest>(manifestEntry.value.decodeToString())
            } else {
                // Generate virtual manifest
                ModuleManifest(
                    id = "imported_${generateUuid().take(8)}",
                    name = "Импортированный модуль",
                    version = "1.0.0"
                )
            }
            
            // Version check
            val existing = moduleManager.getInstalledModules().find { it.manifest.id == manifest.id }
            if (existing != null) {
                val cmp = moduleManager.compareVersions(manifest.version, existing.manifest.version)
                if (cmp < 0) {
                    val confirm = onDowngradeConfirm(manifest.name, existing.manifest.version, manifest.version)
                    if (!confirm) return@withContext false
                }
            }

            var current = 0
            val total = if (manifestEntry != null) {
                manifest.contents.size
            } else {
                files.keys.count { it.endsWith(".json", ignoreCase = true) && !it.equals("manifest.json", ignoreCase = true) }
            }
            
            if (total == 0) return@withContext false
            
            var itemsImported = 0

            if (manifestEntry != null) {
                // Follow manifest
                manifest.contents.forEach { content ->
                    val fileData = files.entries.find { it.key.equals(content.file, ignoreCase = true) || it.key.endsWith("/${content.file}", ignoreCase = true) }?.value
                    if (fileData != null) {
                        if (importContent(content.type, fileData, manifest)) {
                            itemsImported++
                        }
                    }
                    onProgress(++current, total)
                }
            } else {
                // Auto-detect types
                files.forEach { (name, data) ->
                    if (name.endsWith(".json", ignoreCase = true) && !name.equals("manifest.json", ignoreCase = true)) {
                        if (autoImport(data, manifest)) {
                            itemsImported++
                        }
                        onProgress(++current, total)
                    }
                }
            }
            
            if (itemsImported > 0) {
                moduleManager.addOrUpdateModule(manifest)
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            e.log()
            onError("Ошибка импорта", e.message ?: "Неизвестная ошибка")
            false
        }
    }

    private fun importContent(type: String, data: ByteArray, manifest: ModuleManifest): Boolean {
        return try {
            val content = data.decodeToString()
            when (type) {
                "spell" -> {
                    val spell = json.decodeFromString<SpellCard>(content)
                    spellbookManager.addOrUpdateSpell(spell.copy(source = manifest.name, sourceModuleId = manifest.id))
                }
                "species" -> saveItem("species", extractId(content), content)
                "feat" -> saveItem("feats", extractId(content), content)
                "class" -> saveItem("classes", extractId(content), content)
                "subclass" -> saveItem("subclasses", extractId(content), content)
                else -> return false
            }
            true
        } catch (e: Exception) { false }
    }

    private fun autoImport(data: ByteArray, manifest: ModuleManifest): Boolean {
        val content = data.decodeToString()
        return try {
            val obj = json.parseToJsonElement(content).jsonObject
            when {
                obj.containsKey("castingTimeType") || obj.containsKey("school") -> {
                    val spell = json.decodeFromString<SpellCard>(content)
                    spellbookManager.addOrUpdateSpell(spell.copy(source = manifest.name, sourceModuleId = manifest.id))
                    true
                }
                obj.containsKey("creature_type") -> {
                    saveItem("species", extractId(content), content); true
                }
                obj.containsKey("prerequisites") -> {
                    saveItem("feats", extractId(content), content); true
                }
                obj.containsKey("primary_ability") || obj.containsKey("class_tab") -> {
                    saveItem("classes", extractId(content), content); true
                }
                obj.containsKey("class_id") -> {
                    saveItem("subclasses", extractId(content), content); true
                }
                else -> false
            }
        } catch (e: Exception) { false }
    }

    private fun extractId(jsonStr: String): String {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            obj["id"]?.jsonPrimitive?.content ?: generateUuid().take(8)
        } catch (e: Exception) { generateUuid().take(8) }
    }

    private fun saveItem(dir: String, id: String, jsonContent: String) {
        val path = getAppDataDir().resolve("glossary/$dir/$id.json")
        val parent = path.parent
        if (parent != null && !platformFileSystem.exists(parent)) {
            platformFileSystem.createDirectories(parent)
        }
        platformFileSystem.write(path) {
            writeUtf8(jsonContent)
        }
    }
}
