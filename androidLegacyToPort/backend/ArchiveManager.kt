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
    const val EXPORT_EXTENSION = "charbook"
    private val gson = GsonFactory.create()
    private val exportGson = GsonFactory.createPretty()

    suspend fun exportCharacter(context: Context, character: Character, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val json = exportGson.toJson(character)
                    zos.putNextEntry(ZipEntry("character.json"))
                    zos.write(json.toByteArray())
                    zos.closeEntry()

                    character.imageData?.let { portraitId ->
                        // Add Original if exists
                        val originalFile = ImageManager.getOriginalFile(context, portraitId)
                        if (originalFile.exists()) {
                            zos.putNextEntry(ZipEntry("original.webp"))
                            originalFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }

                        // Add Portrait (cropped)
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
        var originalBytes: ByteArray? = null

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
                            "original.webp" -> {
                                originalBytes = zis.readBytes()
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
            val finalChar = if (portraitBytes != null || originalBytes != null) {
                val newId = char.imageData ?: UUID.randomUUID().toString()
                
                // Save Original
                if (originalBytes != null) {
                    val originalFile = ImageManager.getOriginalFile(context, newId)
                    originalFile.parentFile?.mkdirs()
                    try { originalFile.outputStream().use { it.write(originalBytes!!) } } catch (e: Exception) { e.printStackTrace() }
                } else if (portraitBytes != null) {
                    // If no original, save portrait as original for fallback
                    val originalFile = ImageManager.getOriginalFile(context, newId)
                    originalFile.parentFile?.mkdirs()
                    try { originalFile.outputStream().use { it.write(portraitBytes!!) } } catch (e: Exception) { e.printStackTrace() }
                }

                // Save Portrait
                if (portraitBytes != null) {
                    val portraitFile = ImageManager.getPortraitFile(context, newId)
                    portraitFile.parentFile?.mkdirs()
                    try { portraitFile.outputStream().use { it.write(portraitBytes!!) } } catch (e: Exception) { e.printStackTrace() }
                } else if (originalBytes != null) {
                    // If only original, save it as portrait too
                    val portraitFile = ImageManager.getPortraitFile(context, newId)
                    portraitFile.parentFile?.mkdirs()
                    try { portraitFile.outputStream().use { it.write(originalBytes!!) } } catch (e: Exception) { e.printStackTrace() }
                }

                // Regenerate thumbnail
                ImageManager.generateThumbnailFromPortrait(context, newId)
                char.copy(imageData = newId)
            } else char

            return@withContext finalChar
        }
        return@withContext null
    }
}
