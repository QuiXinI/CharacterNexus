package ru.quasaris.characternexus

import androidx.compose.runtime.Composable

actual object PaletteHelper {
    actual suspend fun extractSeedColor(bytes: ByteArray): Int? {
        return null
    }

    @Composable
    actual fun rememberSeedColor(bytes: ByteArray?): Int? {
        return null
    }
}
