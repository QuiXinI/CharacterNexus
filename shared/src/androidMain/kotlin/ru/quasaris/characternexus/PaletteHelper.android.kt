package ru.quasaris.characternexus

import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object PaletteHelper {
    actual suspend fun extractSeedColor(bytes: ByteArray): Int? = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch 
            ?: palette.darkVibrantSwatch 
            ?: palette.mutedSwatch 
            ?: palette.dominantSwatch
        
        swatch?.rgb
    }

    @Composable
    actual fun rememberSeedColor(bytes: ByteArray?): Int? {
        var seedColor by remember(bytes) { mutableStateOf<Int?>(null) }
        
        LaunchedEffect(bytes) {
            if (bytes != null) {
                seedColor = extractSeedColor(bytes)
            } else {
                seedColor = null
            }
        }
        
        return seedColor
    }
}
