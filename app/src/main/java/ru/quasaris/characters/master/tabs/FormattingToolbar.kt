package ru.quasaris.characters.master.tabs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

@Composable
fun FormattingToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textLayoutResult: TextLayoutResult?,
    onLinkRequest: () -> Unit,
    hazeState: HazeState? = null
) {
    val configuration = LocalConfiguration.current
    val hasPhysicalKeyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY || 
                             configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    val density = LocalDensity.current

    Popup(
        alignment = if (hasPhysicalKeyboard) Alignment.TopStart else Alignment.BottomCenter,
        offset = if (hasPhysicalKeyboard) {
            textLayoutResult?.let { layout ->
                val cursorRect = try { layout.getCursorRect(value.selection.start) } catch (_: Exception) { null }
                cursorRect?.let { rect ->
                    with(density) {
                        IntOffset(
                            x = rect.left.roundToInt(),
                            y = (rect.top - 56.dp.toPx()).roundToInt()
                        )
                    }
                }
            } ?: IntOffset.Zero
        } else {
            IntOffset.Zero
        },
        properties = PopupProperties(
            focusable = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = if (!hasPhysicalKeyboard) 16.dp else 0.dp)
                .run {
                    if (hazeState != null) {
                        this.hazeEffect(state = hazeState) {
                            style = HazeStyle(blurRadius = 15.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
                        }
                    } else this
                }
                .then(if (hasPhysicalKeyboard) Modifier.graphicsLayer { alpha = 0.5f } else Modifier),
            shape = RoundedCornerShape(12.dp),
            color = if (hazeState != null) Color.Transparent else MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = if (hazeState != null) 0.4f else 1f))) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormattingButton(
                        icon = Icons.Default.FormatBold,
                        isActive = MarkdownHelper.isFormatActive(value, "**", "**"),
                        onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "**", "**")) }
                    )
                    FormattingButton(
                        icon = Icons.Default.FormatItalic,
                        isActive = MarkdownHelper.isFormatActive(value, "_", "_"),
                        onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "_", "_")) }
                    )
                    FormattingButton(
                        icon = Icons.Default.FormatStrikethrough,
                        isActive = MarkdownHelper.isFormatActive(value, "~~", "~~"),
                        onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "~~", "~~")) }
                    )
                    FormattingButton(
                        icon = Icons.Default.FormatQuote,
                        isActive = MarkdownHelper.isFormatActive(value, "> ", ""),
                        onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "> ", "")) }
                    )
                    FormattingButton(
                        icon = Icons.Default.Link,
                        isActive = MarkdownHelper.isFormatActive(value, "[", "]("),
                        onClick = onLinkRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattingButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
