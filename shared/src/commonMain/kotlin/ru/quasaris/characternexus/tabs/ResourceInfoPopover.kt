package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.*

import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ResourceInfoPopover(
    title: String,
    notes: String,
    anchorPosition: Offset,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 260.dp)
                .hazePopover(
                    state = hazeState,
                    blurRadius = blurRadius,
                    isOled = isOled,
                    forceBlurEnabled = forceBlurEnabled
                )
                .clickable { onDismiss() },
            shape = RoundedCornerShape(16.dp),
            color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = 0.2f),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = if (isOled) 0.3f else 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = notes.ifBlank { "Нет описания" },
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
