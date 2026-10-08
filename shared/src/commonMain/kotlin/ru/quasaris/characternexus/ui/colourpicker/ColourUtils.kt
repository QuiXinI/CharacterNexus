package ru.quasaris.characternexus.ui.colourpicker

import androidx.compose.ui.graphics.Color
import kotlin.math.*

object ColourUtils {

    // --- HSL ---
    fun colorToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val l = (max + min) / 2f
        var h = 0f
        var s = 0f

        if (delta != 0f) {
            s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)

            h = when (max) {
                r -> (g - b) / delta + (if (g < b) 6f else 0f)
                g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }
            h *= 60f
        }

        return floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    // --- HSV ---
    fun colorToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val v = max
        val s = if (max == 0f) 0f else delta / max
        var h = 0f

        if (delta != 0f) {
            h = when (max) {
                r -> (g - b) / delta + (if (g < b) 6f else 0f)
                g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }
            h *= 60f
        }

        return floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    }

    fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
        val c = v * s
        val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    // --- OKLCH ---
    private fun gammaToLinear(c: Float): Float {
        return if (c > 0.04045f) ((c + 0.055f) / 1.055f).pow(2.4f) else c / 12.92f
    }

    private fun linearToGamma(c: Float): Float {
        return if (c > 0.0031308f) 1.055f * c.pow(1f / 2.4f) - 0.055f else 12.92f * c
    }

    private fun cbrt(x: Float): Float {
        return if (x < 0f) -(-x).pow(1f / 3f) else x.pow(1f / 3f)
    }

    fun colorToOklch(color: Color): FloatArray {
        val r = gammaToLinear(color.red)
        val g = gammaToLinear(color.green)
        val b = gammaToLinear(color.blue)

        val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
        val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
        val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)

        val L = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
        val a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
        val bOk = 0.0259040371f * l + 0.7827717662f * m - 0.8086758033f * s

        val C = sqrt(a * a + bOk * bOk)
        var H = (atan2(bOk, a) * 180f / PI.toFloat())
        if (H < 0f) H += 360f

        return floatArrayOf(L.coerceIn(0f, 1f), C.coerceIn(0f, 0.4f), H.coerceIn(0f, 360f))
    }

    fun oklchToColor(L: Float, C: Float, H: Float, alpha: Float = 1f): Color {
        val hRad = H * PI.toFloat() / 180f
        val a = C * cos(hRad)
        val bOk = C * sin(hRad)

        val l_ = L + 0.3963377774f * a + 0.2158037573f * bOk
        val m_ = L - 0.1055613458f * a - 0.0638541728f * bOk
        val s_ = L - 0.0894841775f * a - 1.2914855480f * bOk

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        val rLin = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        val gLin = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        val bLin = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s

        return Color(
            red = linearToGamma(rLin).coerceIn(0f, 1f),
            green = linearToGamma(gLin).coerceIn(0f, 1f),
            blue = linearToGamma(bLin).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    // --- HEX ---
    fun colorToHex(color: Color, includeAlpha: Boolean = false): String {
        val a = (color.alpha * 255f).roundToInt().coerceIn(0, 255)
        val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)

        return if (includeAlpha) {
            "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}${a.toString(16).padStart(2, '0')}".uppercase()
        } else {
            "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
        }
    }

    fun parseHexColor(hexInput: String, currentAlpha: Float = 1f): Color? {
        val cleanHex = hexInput.trim().removePrefix("#")
        return try {
            when (cleanHex.length) {
                6 -> {
                    val r = cleanHex.substring(0, 2).toInt(16) / 255f
                    val g = cleanHex.substring(2, 4).toInt(16) / 255f
                    val b = cleanHex.substring(4, 6).toInt(16) / 255f
                    Color(r, g, b, currentAlpha)
                }
                8 -> {
                    val r = cleanHex.substring(0, 2).toInt(16) / 255f
                    val g = cleanHex.substring(2, 4).toInt(16) / 255f
                    val b = cleanHex.substring(4, 6).toInt(16) / 255f
                    val a = cleanHex.substring(6, 8).toInt(16) / 255f
                    Color(r, g, b, a)
                }
                3 -> {
                    val r = "${cleanHex[0]}${cleanHex[0]}".toInt(16) / 255f
                    val g = "${cleanHex[1]}${cleanHex[1]}".toInt(16) / 255f
                    val b = "${cleanHex[2]}${cleanHex[2]}".toInt(16) / 255f
                    Color(r, g, b, currentAlpha)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
