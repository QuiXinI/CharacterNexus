package ru.quasaris.characternexus.ui.colourpicker

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class ColorPickerMode(val label: String) {
    RGB("RGB"),
    HSL("HSL"),
    HSV("HSV"),
    OKLCH("OKLCH")
}

@Stable
class ColorPickerState(
    initialColor: Color,
    initialMode: ColorPickerMode = ColorPickerMode.RGB
) {
    var color by mutableStateOf(initialColor)
        private set

    var mode by mutableStateOf(initialMode)

    var hexText by mutableStateOf(ColourUtils.colorToHex(initialColor, includeAlpha = false))

    fun updateColor(newColor: Color, updateHex: Boolean = true) {
        color = newColor
        if (updateHex) {
            hexText = ColourUtils.colorToHex(newColor, includeAlpha = false)
        }
    }

    fun updateHexInput(input: String) {
        hexText = input
        val parsed = ColourUtils.parseHexColor(input, currentAlpha = color.alpha)
        if (parsed != null) {
            color = parsed
        }
    }

    fun updateRgb(r: Float, g: Float, b: Float, alpha: Float = color.alpha) {
        updateColor(Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha.coerceIn(0f, 1f)))
    }

    fun updateHsl(h: Float, s: Float, l: Float, alpha: Float = color.alpha) {
        val newColor = ColourUtils.hslToColor(h, s, l, alpha)
        updateColor(newColor)
    }

    fun updateHsv(h: Float, s: Float, v: Float, alpha: Float = color.alpha) {
        val newColor = ColourUtils.hsvToColor(h, s, v, alpha)
        updateColor(newColor)
    }

    fun updateOklch(l: Float, c: Float, h: Float, alpha: Float = color.alpha) {
        val newColor = ColourUtils.oklchToColor(l, c, h, alpha)
        updateColor(newColor)
    }

    fun updateAlpha(alpha: Float) {
        updateColor(color.copy(alpha = alpha.coerceIn(0f, 1f)))
    }
}

@Composable
fun rememberColorPickerState(
    initialColor: Color,
    initialMode: ColorPickerMode = ColorPickerMode.RGB
): ColorPickerState {
    return remember(initialColor) {
        ColorPickerState(initialColor, initialMode)
    }
}
