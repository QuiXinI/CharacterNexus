package ru.quasaris.characternexus.backend

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.util.log

class ModuleManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val modulesFile = getAppDataDir().div("installed_modules.json")
    private var cachedModules: MutableList<InstalledModule>? = null

    fun getInstalledModules(): List<InstalledModule> {
        val currentCached = cachedModules
        if (currentCached != null) return currentCached

        if (!platformFileSystem.exists(modulesFile)) {
            cachedModules = mutableListOf()
            return cachedModules!!
        }

        return try {
            val content = platformFileSystem.read(modulesFile) { readUtf8() }
            val modules: List<InstalledModule> = json.decodeFromString<List<InstalledModule>>(content)
            cachedModules = modules.toMutableList()
            cachedModules!!
        } catch (e: Exception) {
            e.log()
            cachedModules = mutableListOf()
            cachedModules!!
        }
    }

    private fun saveModules(modules: List<InstalledModule>) {
        cachedModules = modules.toMutableList()
        try {
            platformFileSystem.write(modulesFile) {
                writeUtf8(json.encodeToString(modules))
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    fun addOrUpdateModule(manifest: ModuleManifest) {
        val modules = getInstalledModules().toMutableList()
        val index = modules.indexOfFirst { it.manifest.id == manifest.id }
        if (index != -1) {
            modules[index] = InstalledModule(manifest)
        } else {
            modules.add(InstalledModule(manifest, installTimestamp = 0L))
        }
        saveModules(modules)
    }

    fun updateModule(moduleId: String, updatedManifest: ModuleManifest) {
        val modules = getInstalledModules().toMutableList()
        val index = modules.indexOfFirst { it.manifest.id == moduleId }
        if (index != -1) {
            modules[index] = modules[index].copy(manifest = updatedManifest)
        } else {
            modules.add(InstalledModule(updatedManifest))
        }
        saveModules(modules)
    }

    fun addComponentToModule(moduleId: String, type: String, id: String, fileName: String) {
        val modules = getInstalledModules().toMutableList()
        val index = modules.indexOfFirst { it.manifest.id == moduleId }
        if (index != -1) {
            val manifest = modules[index].manifest
            val newContents = manifest.contents.toMutableList()
            if (newContents.none { it.id == id && it.type == type }) {
                newContents.add(ModuleContent(type, id, fileName))
                modules[index] = modules[index].copy(manifest = manifest.copy(contents = newContents))
                saveModules(modules)
            }
        }
    }

    fun removeComponentFromModule(moduleId: String, type: String, id: String) {
        val modules = getInstalledModules().toMutableList()
        val index = modules.indexOfFirst { it.manifest.id == moduleId }
        if (index != -1) {
            val manifest = modules[index].manifest
            val newContents = manifest.contents.toMutableList()
            newContents.removeAll { it.id == id && it.type == type }
            modules[index] = modules[index].copy(manifest = manifest.copy(contents = newContents))
            saveModules(modules)
        }
    }

    fun deleteModule(moduleId: String, spellbookManager: SpellbookManager? = null) {
        val modules = getInstalledModules().toMutableList()
        val module = modules.find { it.manifest.id == moduleId }
        
        module?.let { inst ->
            inst.manifest.contents.forEach { content ->
                val dir = when (content.type) {
                    "spell" -> "spells"
                    "class" -> "classes"
                    "subclass" -> "subclasses"
                    "species" -> "species"
                    "feat" -> "feats"
                    else -> null
                }
                
                if (dir != null) {
                    val file = getAppDataDir().resolve("glossary/$dir/${content.file}")
                    if (platformFileSystem.exists(file)) {
                        platformFileSystem.delete(file)
                    }
                }
            }
        }

        // Deleting spells by sourceModuleId as requested
        spellbookManager?.let { sm ->
            val spellsToDelete = sm.loadSpells().filter { it.sourceModuleId == moduleId }
            spellsToDelete.forEach { sm.deleteSpell(it.id) }
        }
        
        modules.removeAll { it.manifest.id == moduleId }
        saveModules(modules)
    }

    fun getSubclassesForClass(classId: String): List<GameSubclass> {
        val subclasses = mutableListOf<GameSubclass>()
        val baseDir = getAppDataDir().resolve("glossary/subclasses")
        if (!platformFileSystem.exists(baseDir)) return emptyList()
        
        platformFileSystem.list(baseDir).filter { it.name.endsWith(".json") }.forEach { file ->
            try {
                val jsonContent = platformFileSystem.read(file) { readUtf8() }
                val subclass = JsonConfig.json.decodeFromString<GameSubclass>(jsonContent)
                if (subclass.classId == classId) {
                    subclasses.add(subclass)
                }
            } catch (e: Exception) {
                ru.quasaris.characternexus.util.Logger.e("ModuleManager", "Error loading subclass from $file", e)
            }
        }
        return subclasses.sortedBy { it.name }
    }

    /**
     * Returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal.
     */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 > p2) return 1
            if (p1 < p2) return -1
        }
        return 0
    }
}
