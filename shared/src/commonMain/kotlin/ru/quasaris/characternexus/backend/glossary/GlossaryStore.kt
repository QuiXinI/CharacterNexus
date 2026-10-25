package ru.quasaris.characternexus.backend.glossary

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem

/**
 * Хранилище сущностей глоссария в папке приложения.
 *
 * Структура: <appData>/glossary/<folder>/<id>.json, где folder берётся из GlossaryType.
 * Папки совпадают с теми, что использовал прежний импорт, поэтому уже сохранённые
 * данные остаются доступны без миграции.
 *
 * Все операции блокирующие, их нужно вызывать с ioDispatcher.
 */
class GlossaryStore(
    private val root: Path = getAppDataDir().resolve("glossary")
) {

    fun folderOf(type: GlossaryType): Path = root.resolve(type.folder)

    fun fileOf(type: GlossaryType, id: String): Path =
        folderOf(type).resolve("${safeFileName(id)}.json")

    inline fun <reified T> loadAll(type: GlossaryType): List<T> =
        loadAll(type, serializer<T>())

    fun <T> loadAll(type: GlossaryType, serializer: KSerializer<T>): List<T> {
        val dir = folderOf(type)
        if (!platformFileSystem.exists(dir)) return emptyList()
        return platformFileSystem.list(dir)
            .filter { it.name.endsWith(".json") }
            .mapNotNull { file ->
                runCatching { GlossaryJson.decodeFromString(serializer, readText(file)) }
                    .getOrNull()
            }
    }

    inline fun <reified T> save(type: GlossaryType, id: String, value: T) =
        save(type, id, value, serializer<T>())

    fun <T> save(type: GlossaryType, id: String, value: T, serializer: KSerializer<T>) {
        val file = fileOf(type, id)
        file.parent?.let { parent ->
            if (!platformFileSystem.exists(parent)) platformFileSystem.createDirectories(parent)
        }
        val text = GlossaryJson.encodeToString(serializer, value)
        platformFileSystem.write(file) { writeUtf8(text) }
    }

    fun delete(type: GlossaryType, id: String) {
        val file = fileOf(type, id)
        if (platformFileSystem.exists(file)) platformFileSystem.delete(file)
    }

    private fun readText(file: Path): String =
        platformFileSystem.read(file) { readUtf8() }

    companion object {
        /** Заменяет небезопасные для имени файла символы; буквы (в т.ч. кириллица) сохраняются. */
        fun safeFileName(id: String): String =
            id.map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }
                .joinToString("")
                .ifBlank { "item" }
    }
}
