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
            modules.add(InstalledModule(manifest, installTimestamp = 0L)) // Add timestamp if needed
        }
        saveModules(modules)
    }

    fun deleteModule(moduleId: String) {
        val modules = getInstalledModules().toMutableList()
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
                // Ignore errors
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
