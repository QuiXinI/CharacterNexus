package ru.quasaris.characternexus.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual object ZipUtils {
    actual fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                result[entry.name] = zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    actual fun zip(files: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            files.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
