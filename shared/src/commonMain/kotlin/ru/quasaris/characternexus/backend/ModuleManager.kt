package ru.quasaris.characternexus.backend

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.quasaris.characternexus.backend.glossary.GlossaryStore
import ru.quasaris.characternexus.backend.glossary.GlossaryType
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.util.log

/**
 * Учёт установленных модулей. Данные глоссария (классы, подклассы, виды, черты, предметы)
 * хранит GlossaryStore; этот менеджер отвечает только за список модулей и их удаление.
 */
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

    /** Удаляет модуль вместе со всеми сущностями глоссария, которые он добавил. */
    fun deleteModule(moduleId: String, spellbookManager: SpellbookManager? = null) {
        val modules = getInstalledModules().toMutableList()
        val module = modules.find { it.manifest.id == moduleId }

        val store = GlossaryStore()
        module?.manifest?.contents?.forEach { content ->
            val type = GlossaryType.fromWireName(content.type)
            if (type != null && type != GlossaryType.SPELL) store.delete(type, content.id)
        }

        // Заклинания удаляются по sourceModuleId.
        spellbookManager?.let { sm ->
            sm.loadSpells().filter { it.sourceModuleId == moduleId }.forEach { sm.deleteSpell(it.id) }
        }

        modules.removeAll { it.manifest.id == moduleId }
        saveModules(modules)
    }

    /** Возвращает 1 если v1 > v2, -1 если v1 < v2, 0 если равны. */
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
