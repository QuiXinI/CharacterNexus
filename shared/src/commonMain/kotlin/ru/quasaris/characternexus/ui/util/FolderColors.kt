package ru.quasaris.characternexus.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp

data class FolderColor(
    val light: Color,
    val dark: Color,
    val isPremium: Boolean = true
)

object FolderColors {
    // New palette based on ColorBrewer "Paired" (Pastel/Strong pairs)
    // Indices: 2, 4, 6, 11 are Free. Others are Premium.
    val predefinedColors = listOf(
        FolderColor(Color(0xFFA6CEE3), Color(0xFF1F4E6B)), // 1. Light Blue (P)
        FolderColor(Color(0xFF1F78B4), Color(0xFF90CAF9), isPremium = false), // 2. Dark Blue (F)
        FolderColor(Color(0xFFB2DF8A), Color(0xFF3D611E)), // 3. Light Green (P)
        FolderColor(Color(0xFF33A02C), Color(0xFFA5D6A7), isPremium = false), // 4. Dark Green (F)
        FolderColor(Color(0xFFFB9A99), Color(0xFF7A201F)), // 5. Light Red/Pink (P)
        FolderColor(Color(0xFFE31A1C), Color(0xFFEF9A9A), isPremium = false), // 6. Dark Red (F)
        FolderColor(Color(0xFFFDBF6F), Color(0xFF8C5D1E)), // 7. Light Orange (P)
        FolderColor(Color(0xFFFF7F00), Color(0xFFFFCC80)), // 8. Dark Orange (P)
        FolderColor(Color(0xFFCAB2D6), Color(0xFF5A3E6B)), // 9. Light Purple (P)
        FolderColor(Color(0xFF6A3D9A), Color(0xFFCE93D8)), // 10. Dark Purple (P)
        FolderColor(Color(0xFFFFFF99), Color(0xFF6B6B00), isPremium = false), // 11. Yellow (F)
        FolderColor(Color(0xFFB15928), Color(0xFFE0A080))  // 12. Brown (P)
    )

    fun getThemeAdaptedColor(argb: Int?, isDark: Boolean): Color? {
        if (argb == null) return null
        
        predefinedColors.forEach { folderColor ->
            if (folderColor.light.toArgb() == argb || folderColor.dark.toArgb() == argb) {
                return if (isDark) folderColor.dark else folderColor.light
            }
        }
        
        return Color(argb)
    }

    /**
     * Returns a color suitable for card background (container).
     * Typically very desaturated and semi-transparent.
     */
    fun getCardContainerColor(baseColor: Color, isDark: Boolean): Color {
        return if (isDark) {
            lerp(baseColor, Color.Black, 0.85f).copy(alpha = 0.4f)
        } else {
            lerp(baseColor, Color.White, 0.85f).copy(alpha = 0.6f)
        }
    }

    /**
     * Returns a color for progress bars, related to the base color but distinct.
     */
    fun getProgressBarColor(baseColor: Color, isDark: Boolean): Color {
        return baseColor.copy(alpha = 0.4f)
    }
    
    fun getBestIconTint(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    }
}
