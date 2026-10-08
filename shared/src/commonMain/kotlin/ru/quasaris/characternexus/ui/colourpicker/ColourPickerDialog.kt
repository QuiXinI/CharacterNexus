package ru.quasaris.characternexus.ui.colourpicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@Composable
fun ColourPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorConfirmed: (Color) -> Unit,
    showAlpha: Boolean = true,
    title: String = "Выберите цвет",
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    initialMode: ColorPickerMode = ColorPickerMode.RGB,
    modifier: Modifier = Modifier
) {
    val state = rememberColorPickerState(initialColor = initialColor, initialMode = initialMode)
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val colorScheme = MaterialTheme.colorScheme

    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)

        Surface(
            modifier = modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .wrapContentHeight()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                }
                .outerShadow(RoundedCornerShape(24.dp), blur = 12.dp)
                .hazePopover(
                    state = hazeState,
                    blurRadius = blurRadius,
                    isOled = isOled
                ),
            shape = RoundedCornerShape(24.dp),
            color = when {
                isOled -> Color.Black
                hazeState != null -> colorScheme.surface.copy(alpha = 0.85f)
                else -> colorScheme.surface
            },
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Color Picker Content
                ColorPicker(
                    state = state,
                    showAlpha = showAlpha,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onColorConfirmed(state.color)
                            onDismiss()
                        }
                    ) {
                        Text("Применить")
                    }
                }
            }
        }
    }
}
