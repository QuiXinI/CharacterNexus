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

@Composable
fun FormattingToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textLayoutResult: TextLayoutResult?,
    onLinkRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val hasPhysicalKeyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY || 
                             configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    val isEnabled = value.selection.length > 0
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
        onDismissRequest = null,
        properties = PopupProperties(
            focusable = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = if (!hasPhysicalKeyboard) WindowInsets.ime.asPaddingValues().calculateBottomPadding() else 0.dp)
                .then(if (hasPhysicalKeyboard) Modifier.graphicsLayer { alpha = 0.5f } else Modifier),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "**", "**")) },
                    enabled = isEnabled
                )
                ToolbarIconButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "_", "_")) },
                    enabled = isEnabled
                )
                ToolbarIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "~~", "~~")) },
                    enabled = isEnabled
                )
                ToolbarIconButton(
                    icon = Icons.Default.FormatQuote,
                    contentDescription = "Quote",
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "> ", "")) },
                    enabled = isEnabled
                )
                ToolbarIconButton(
                    icon = Icons.Default.Link,
                    contentDescription = "Link",
                    onClick = onLinkRequest,
                    enabled = isEnabled
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
