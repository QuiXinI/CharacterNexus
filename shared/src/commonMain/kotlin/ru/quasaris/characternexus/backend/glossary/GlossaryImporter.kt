package ru.quasaris.characternexus.backend.glossary

import kotlinx.coroutines.withContext
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.backend.ModuleManager
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.model.ModuleManifest
import ru.quasaris.characternexus.model.SpellCard
import ru.quasaris.characternexus.util.*

/** Результат импорта: UI показывает, что импортировано и что пропущено. */
sealed interface GlossaryImportResult {
    data class Success(val imported: Map<GlossaryType, Int>, val skipped: Int) : GlossaryImportResult
    data object Cancelled : GlossaryImportResult
    data class Failed(val reason: String) : GlossaryImportResult
}

/**
 * Импорт модуля из zip-архива (.databook / .db / .zip) с manifest.json в корне.
 * Файл сущности ищется по ModuleContent.file как точное имя, как суффикс после "/"
 * либо как "<папка типа>/<file>", поэтому вручную собранные архивы тоже подходят.
 */
class GlossaryImporter(
    private val spellbookManager: SpellbookManager,
    private val moduleManager: ModuleManager,
    private val store: GlossaryStore = GlossaryStore()
) {

    suspend fun importModule(
        bytes: ByteArray,
        onProgress: (current: Int, total: Int) -> Unit,
        onDowngradeConfirm: suspend (name: String, oldVersion: String, newVersion: String) -> Boolean
    ): GlossaryImportResult = withContext(ioDispatcher) {
        try {
            val files = ZipUtils.unzip(bytes)
            if (files.isEmpty()) return@withContext GlossaryImportResult.Failed("Файл не является архивом модуля")

            val manifestData = files.entries.find {
                it.key.equals("manifest.json", ignoreCase = true) || it.key.endsWith("/manifest.json", ignoreCase = true)
            }?.value ?: return@withContext GlossaryImportResult.Failed("В архиве нет manifest.json")

            val manifest = GlossaryJson.decodeFromString<ModuleManifest>(manifestData.decodeToString())

            val existing = moduleManager.getInstalledModules().find { it.manifest.id == manifest.id }
            if (existing != null && moduleManager.compareVersions(manifest.version, existing.manifest.version) < 0) {
                if (!onDowngradeConfirm(manifest.name, existing.manifest.version, manifest.version)) {
                    return@withContext GlossaryImportResult.Cancelled
                }
            }

            val imported = mutableMapOf<GlossaryType, Int>()
            var skipped = 0
            val total = manifest.contents.size

            manifest.contents.forEachIndexed { index, content ->
                val type = GlossaryType.fromWireName(content.type)
                val data = findEntry(files, content.file, type)
                val ok = type != null && data != null && importOne(type, data, manifest)
                if (ok && type != null) imported[type] = (imported[type] ?: 0) + 1 else skipped++
                onProgress(index + 1, total)
            }

            if (imported.isEmpty()) {
                return@withContext GlossaryImportResult.Failed("Ни один элемент модуля не удалось импортировать")
            }
            moduleManager.addOrUpdateModule(manifest)
            GlossaryImportResult.Success(imported, skipped)
        } catch (e: Exception) {
            e.log()
            GlossaryImportResult.Failed(e.message ?: "Неизвестная ошибка импорта")
        }
    }

    private fun findEntry(files: Map<String, ByteArray>, path: String, type: GlossaryType?): ByteArray? {
        val candidates = buildList {
            add(path)
            if (type != null) add("${type.folder}/$path")
        }
        for (candidate in candidates) {
            val found = files.entries.find {
                it.key.equals(candidate, ignoreCase = true) || it.key.endsWith("/$candidate", ignoreCase = true)
            }
            if (found != null) return found.value
        }
        return null
    }

    private fun importOne(type: GlossaryType, data: ByteArray, manifest: ModuleManifest): Boolean {
        val text = data.decodeToString()
        return runCatching {
            when (type) {
                GlossaryType.SPELL -> {
                    val spell = GlossaryJson.decodeFromString<SpellCard>(text)
                    spellbookManager.addOrUpdateSpell(spell.copy(source = manifest.name, sourceModuleId = manifest.id))
                }
                GlossaryType.CLASS -> GlossaryJson.decodeFromString(ClassDefinition.serializer(), text)
                    .copy(source = manifest.name, sourceModuleId = manifest.id)
                    .let { store.save(type, it.id, it, ClassDefinition.serializer()) }
                GlossaryType.SUBCLASS -> GlossaryJson.decodeFromString(SubclassDefinition.serializer(), text)
                    .copy(source = manifest.name, sourceModuleId = manifest.id)
                    .let { store.save(type, it.id, it, SubclassDefinition.serializer()) }
                GlossaryType.SPECIES -> GlossaryJson.decodeFromString(SpeciesDefinition.serializer(), text)
                    .copy(source = manifest.name, sourceModuleId = manifest.id)
                    .let { store.save(type, it.id, it, SpeciesDefinition.serializer()) }
                GlossaryType.FEAT -> GlossaryJson.decodeFromString(FeatDefinition.serializer(), text)
                    .copy(source = manifest.name, sourceModuleId = manifest.id)
                    .let { store.save(type, it.id, it, FeatDefinition.serializer()) }
                GlossaryType.MAGIC_ITEM -> GlossaryJson.decodeFromString(MagicItem.serializer(), text)
                    .copy(source = manifest.name, sourceModuleId = manifest.id)
                    .let { store.save(type, it.id, it, MagicItem.serializer()) }
            }
            true
        }.getOrElse {
            it.log()
            false
        }
    }
}
