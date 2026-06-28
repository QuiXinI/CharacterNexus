package ru.quasaris.characters.master

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.quasaris.characters.master.utils.GsonFactory
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveManager {
    private const val EXPORT_EXTENSION = "lsskiller"
    private val gson = GsonFactory.create()

    suspend fun exportCharacter(context: Context, character: Character, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    // 1. Save JSON (stripped of Base64 if it was there, but our model uses IDs now)
                    val json = gson.toJson(character)
                    zos.putNextEntry(ZipEntry("character.json"))
                    zos.write(json.toByteArray())
                    zos.closeEntry()

                    // 2. Save Portrait if exists
                    character.imageData?.let { portraitId ->
                        val portraitFile = ImageManager.getPortraitFile(context, portraitId)
                        if (portraitFile.exists()) {
                            zos.putNextEntry(ZipEntry("portrait.webp"))
                            portraitFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun importCharacter(context: Context, uri: Uri): Character? = withContext(Dispatchers.IO) {
        var character: Character? = null
        var portraitBytes: ByteArray? = null

        try {
            context.contentResolver.openInputStream(uri)?.use { `is` ->
                ZipInputStream(BufferedInputStream(`is`)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            "character.json" -> {
                                val reader = InputStreamReader(zis)
                                character = gson.fromJson(reader, Character::class.java)
                            }
                            "portrait.webp" -> {
                                portraitBytes = zis.readBytes()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        character?.let { char ->
            val finalChar = if (portraitBytes != null) {
                val newId = char.imageData ?: java.util.UUID.randomUUID().toString()
                val portraitFile = ImageManager.getPortraitFile(context, newId)
                portraitFile.parentFile?.mkdirs()
                try {
                    portraitFile.outputStream().use { it.write(portraitBytes) }
                    // Regenerate thumbnail immediately
                    ImageManager.generateThumbnailFromPortrait(context, newId)
                    char.copy(imageData = newId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    char
                }
            } else char
            
            return@withContext finalChar
        }
        return@withContext null
    }
}
