package ru.quasaris.characternexus.util

import okio.Path
import ru.quasaris.characternexus.platformFileSystem

fun Path.ensureNomedia() {
    try {
        if (!platformFileSystem.exists(this)) {
            platformFileSystem.createDirectories(this)
        }
        val nomediaFile = this / ".nomedia"
        if (!platformFileSystem.exists(nomediaFile)) {
            platformFileSystem.write(nomediaFile) { writeUtf8("") }
        }
    } catch (_: Exception) {
        // Silently fail if we can't write .nomedia
    }
}
