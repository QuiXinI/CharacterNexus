package ru.quasaris.characternexus.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual object ZipUtils {
    actual fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        // Try UTF-8 first
        var result = unzipWithCharset(bytes, StandardCharsets.UTF_8)
        
        // If empty or seems wrong, try IBM866 (common on Russian Windows)
        if (result.isEmpty() || result.keys.none { it.contains(".json", ignoreCase = true) }) {
            try {
                val altResult = unzipWithCharset(bytes, Charset.forName("IBM866"))
                if (altResult.isNotEmpty()) {
                    result = altResult
                }
            } catch (e: Exception) {
                // Ignore if charset not found
            }
        }
        return result
    }

    private fun unzipWithCharset(bytes: ByteArray, charset: Charset): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes), charset).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        result[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            // Log or ignore
        }
        return result
    }

    actual fun zip(files: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos, StandardCharsets.UTF_8).use { zos ->
            files.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
