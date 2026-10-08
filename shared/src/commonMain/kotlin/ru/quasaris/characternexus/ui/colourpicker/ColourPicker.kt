package ru.quasaris.characternexus.ui.colourpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ColorPicker(
    state: ColorPickerState,
    showAlpha: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Color Wheel
        ColorWheel(
            color = state.color,
            onColorChange = { state.updateColor(it) },
            modifier = Modifier.widthIn(max = 240.dp)
        )

        // 2. Preview & HEX Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview box with checkerboard background
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val squareSize = 8.dp.toPx()
                    val cols = (size.width / squareSize).toInt() + 1
                    val rows = (size.height / squareSize).toInt() + 1
                    for (i in 0 until cols) {
                        for (j in 0 until rows) {
                            val color = if ((i + j) % 2 == 0) Color.LightGray else Color.White
                            drawRect(
                                color = color,
                                topLeft = Offset(i * squareSize, j * squareSize),
                                size = Size(squareSize, squareSize)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(state.color)
                )
            }

            // HEX Input Field
            OutlinedTextField(
                value = state.hexText,
                onValueChange = { input ->
                    if (input.length <= 9) {
                        state.updateHexInput(input)
                    }
                },
                label = { Text("HEX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Mode Switcher
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ColorPickerMode.entries.forEachIndexed { index, modeOption ->
                SegmentedButton(
                    selected = state.mode == modeOption,
                    onClick = { state.mode = modeOption },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ColorPickerMode.entries.size)
                ) {
                    Text(modeOption.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Mode Sliders + Inputs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.mode) {
                ColorPickerMode.RGB -> {
                    val r = (state.color.red * 255f)
                    val g = (state.color.green * 255f)
                    val b = (state.color.blue * 255f)

                    ColorChannelSliderRow(
                        label = "R",
                        value = r,
                        range = 0f..255f,
                        displayValue = r.roundToInt().toString(),
                        onValueChange = { state.updateRgb(it / 255f, state.color.green, state.color.blue) },
                        onTextChange = { str ->
                            str.toIntOrNull()?.let {
                                state.updateRgb(it / 255f, state.color.green, state.color.blue)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "G",
                        value = g,
                        range = 0f..255f,
                        displayValue = g.roundToInt().toString(),
                        onValueChange = { state.updateRgb(state.color.red, it / 255f, state.color.blue) },
                        onTextChange = { str ->
                            str.toIntOrNull()?.let {
                                state.updateRgb(state.color.red, it / 255f, state.color.blue)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "B",
                        value = b,
                        range = 0f..255f,
                        displayValue = b.roundToInt().toString(),
                        onValueChange = { state.updateRgb(state.color.red, state.color.green, it / 255f) },
                        onTextChange = { str ->
                            str.toIntOrNull()?.let {
                                state.updateRgb(state.color.red, state.color.green, it / 255f)
                            }
                        }
                    )
                }

                ColorPickerMode.HSL -> {
                    val hsl = remember(state.color) { ColourUtils.colorToHsl(state.color) }
                    val h = hsl[0]
                    val s = hsl[1]
                    val l = hsl[2]

                    ColorChannelSliderRow(
                        label = "H",
                        value = h,
                        range = 0f..360f,
                        displayValue = "${h.roundToInt()}°",
                        onValueChange = { state.updateHsl(it, s, l) },
                        onTextChange = { str ->
                            str.removeSuffix("°").trim().toFloatOrNull()?.let {
                                state.updateHsl(it.coerceIn(0f, 360f), s, l)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "S",
                        value = s * 100f,
                        range = 0f..100f,
                        displayValue = "${(s * 100f).roundToInt()}%",
                        onValueChange = { state.updateHsl(h, it / 100f, l) },
                        onTextChange = { str ->
                            str.removeSuffix("%").trim().toFloatOrNull()?.let {
                                state.updateHsl(h, (it / 100f).coerceIn(0f, 1f), l)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "L",
                        value = l * 100f,
                        range = 0f..100f,
                        displayValue = "${(l * 100f).roundToInt()}%",
                        onValueChange = { state.updateHsl(h, s, it / 100f) },
                        onTextChange = { str ->
                            str.removeSuffix("%").trim().toFloatOrNull()?.let {
                                state.updateHsl(h, s, (it / 100f).coerceIn(0f, 1f))
                            }
                        }
                    )
                }

                ColorPickerMode.HSV -> {
                    val hsv = remember(state.color) { ColourUtils.colorToHsv(state.color) }
                    val h = hsv[0]
                    val s = hsv[1]
                    val v = hsv[2]

                    ColorChannelSliderRow(
                        label = "H",
                        value = h,
                        range = 0f..360f,
                        displayValue = "${h.roundToInt()}°",
                        onValueChange = { state.updateHsv(it, s, v) },
                        onTextChange = { str ->
                            str.removeSuffix("°").trim().toFloatOrNull()?.let {
                                state.updateHsv(it.coerceIn(0f, 360f), s, v)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "S",
                        value = s * 100f,
                        range = 0f..100f,
                        displayValue = "${(s * 100f).roundToInt()}%",
                        onValueChange = { state.updateHsv(h, it / 100f, v) },
                        onTextChange = { str ->
                            str.removeSuffix("%").trim().toFloatOrNull()?.let {
                                state.updateHsv(h, (it / 100f).coerceIn(0f, 1f), v)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "V",
                        value = v * 100f,
                        range = 0f..100f,
                        displayValue = "${(v * 100f).roundToInt()}%",
                        onValueChange = { state.updateHsv(h, s, it / 100f) },
                        onTextChange = { str ->
                            str.removeSuffix("%").trim().toFloatOrNull()?.let {
                                state.updateHsv(h, s, (it / 100f).coerceIn(0f, 1f))
                            }
                        }
                    )
                }

                ColorPickerMode.OKLCH -> {
                    val oklch = remember(state.color) { ColourUtils.colorToOklch(state.color) }
                    val l = oklch[0]
                    val c = oklch[1]
                    val h = oklch[2]

                    ColorChannelSliderRow(
                        label = "L",
                        value = l * 100f,
                        range = 0f..100f,
                        displayValue = "${(l * 100f).roundToInt()}%",
                        onValueChange = { state.updateOklch(it / 100f, c, h) },
                        onTextChange = { str ->
                            str.removeSuffix("%").trim().toFloatOrNull()?.let {
                                state.updateOklch((it / 100f).coerceIn(0f, 1f), c, h)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "C",
                        value = c,
                        range = 0f..0.4f,
                        displayValue = (c * 1000f).roundToInt().let { (it / 1000f).toString() },
                        onValueChange = { state.updateOklch(l, it, h) },
                        onTextChange = { str ->
                            str.toFloatOrNull()?.let {
                                state.updateOklch(l, it.coerceIn(0f, 0.4f), h)
                            }
                        }
                    )

                    ColorChannelSliderRow(
                        label = "H",
                        value = h,
                        range = 0f..360f,
                        displayValue = "${h.roundToInt()}°",
                        onValueChange = { state.updateOklch(l, c, it) },
                        onTextChange = { str ->
                            str.removeSuffix("°").trim().toFloatOrNull()?.let {
                                state.updateOklch(l, c, it.coerceIn(0f, 360f))
                            }
                        }
                    )
                }
            }

            // Optional Alpha Channel
            if (showAlpha) {
                val alpha = state.color.alpha
                ColorChannelSliderRow(
                    label = "A",
                    value = alpha * 100f,
                    range = 0f..100f,
                    displayValue = "${(alpha * 100f).roundToInt()}%",
                    onValueChange = { state.updateAlpha(it / 100f) },
                    onTextChange = { str ->
                        str.removeSuffix("%").trim().toFloatOrNull()?.let {
                            state.updateAlpha((it / 100f).coerceIn(0f, 1f))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorChannelSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    onTextChange: (String) -> Unit
) {
    var textInput by remember(displayValue) { mutableStateOf(displayValue) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )

        Slider(
            value = value.coerceIn(range),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                onTextChange(it)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(72.dp)
        )
    }
}
