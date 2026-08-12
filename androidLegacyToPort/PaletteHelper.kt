package ru.quasaris.characters.master

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteHelper {
    suspend fun extractSeedColor(bitmap: Bitmap): Int? = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch 
            ?: palette.darkVibrantSwatch 
            ?: palette.mutedSwatch 
            ?: palette.dominantSwatch
        
        swatch?.rgb
    }
}

@Composable
fun rememberSeedColor(bitmap: Bitmap?): Int? {
    var seedColor by remember(bitmap) { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(bitmap) {
        if (bitmap != null) {
            seedColor = PaletteHelper.extractSeedColor(bitmap)
        } else {
            seedColor = null
        }
    }
    
    return seedColor
}
