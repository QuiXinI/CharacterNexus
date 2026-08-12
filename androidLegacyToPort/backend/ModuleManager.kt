package ru.quasaris.characters.master.backend

import android.content.Context
import com.google.gson.reflect.TypeToken
import ru.quasaris.characters.master.InstalledModule
import ru.quasaris.characters.master.ModuleManifest
import java.io.File

class ModuleManager(private val context: Context) {
    private val gson = GsonFactory.create()
    private val modulesFile = File(context.filesDir, "installed_modules.json")
    private var cachedModules: MutableList<InstalledModule>? = null

    fun getInstalledModules(): List<InstalledModule> {
        val currentCached = cachedModules
        if (currentCached != null) return currentCached

        if (!modulesFile.exists()) {
            cachedModules = mutableListOf()
            return cachedModules!!
        }

        return try {
            val json = modulesFile.readText()
            val type = object : TypeToken<List<InstalledModule>>() {}.type
            val modules: List<InstalledModule> = gson.fromJson(json, type)
            cachedModules = modules.toMutableList()
            cachedModules!!
        } catch (e: Exception) {
            e.printStackTrace()
            cachedModules = mutableListOf()
            cachedModules!!
        }
    }

    private fun saveModules(modules: List<InstalledModule>) {
        cachedModules = modules.toMutableList()
        try {
            modulesFile.writeText(gson.toJson(modules))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateModule(manifest: ModuleManifest) {
        val modules = getInstalledModules().toMutableList()
        val index = modules.indexOfFirst { it.manifest.id == manifest.id }
        if (index != -1) {
            modules[index] = InstalledModule(manifest)
        } else {
            modules.add(InstalledModule(manifest))
        }
        saveModules(modules)
    }

    fun deleteModule(moduleId: String) {
        val modules = getInstalledModules().toMutableList()
        modules.removeAll { it.manifest.id == moduleId }
        saveModules(modules)
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

    fun getSubclassesForClass(context: Context, classId: String): List<GameSubclass> {
        val dir = File(context.filesDir, "glossary/subclasses")
        if (!dir.exists()) return emptyList()
        
        return dir.listFiles()?.filter { it.extension == "json" }?.mapNotNull { file ->
            try {
                val sub = gson.fromJson(file.readText(), GameSubclass::class.java)
                if (sub.classId == classId) sub else null
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
}
