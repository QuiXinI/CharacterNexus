package ru.quasaris.characters.master.backend

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.quasaris.characters.master.Character
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveManager {
    const val EXPORT_EXTENSION = "lsskiller"
    private val gson = GsonFactory.create()

    suspend fun exportCharacter(context: Context, character: Character, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val json = gson.toJson(character)
                    zos.putNextEntry(ZipEntry("character.json"))
                    zos.write(json.toByteArray())
                    zos.closeEntry()

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
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@withContext null

        var character: Character? = null
        var portraitBytes: ByteArray? = null

        // Detect if it's a ZIP file (PK header)
        if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
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
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        } else {
            // Handle JSON formats
            val jsonString = String(bytes)
            val jsonElement = try { JsonParser.parseString(jsonString) } catch (e: Exception) { null }

            if (jsonElement != null && LongStoryShortImporter.isLongStoryShort(jsonElement)) {
                character = LongStoryShortImporter.parse(jsonElement)
            } else {
                // Legacy JSON (mp_old)
                character = try {
                    gson.fromJson(jsonString, Character::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            character?.let { char ->
                // Check if imageData is a Base64 string (longer than a UUID)
                if (char.imageData != null && char.imageData.length > 100) {
                    try {
                        portraitBytes = Base64.decode(char.imageData, Base64.DEFAULT)
                        // Reset imageData to null or a new UUID so the logic below saves it properly
                        character = char.copy(imageData = UUID.randomUUID().toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        character?.let { char ->
            val finalChar = if (portraitBytes != null) {
                val newId = char.imageData ?: UUID.randomUUID().toString()
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
