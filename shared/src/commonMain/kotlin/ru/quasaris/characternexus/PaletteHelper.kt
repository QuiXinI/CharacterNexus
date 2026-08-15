package ru.quasaris.characternexus

import androidx.compose.runtime.Composable

expect object PaletteHelper {
    suspend fun extractSeedColor(bytes: ByteArray): Int?
    
    @Composable
    fun rememberSeedColor(bytes: ByteArray?): Int?
}
