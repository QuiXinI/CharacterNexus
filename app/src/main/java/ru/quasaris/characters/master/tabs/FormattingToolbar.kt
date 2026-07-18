package ru.quasaris.characters.master.tabs

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

@Composable
fun FormattingToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isFocused: Boolean,
    isSelectionActive: Boolean,
    onLinkRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    if (!isFocused) return

    val configuration = LocalConfiguration.current
    val hasPhysicalKeyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY || 
                             configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    var yOffset by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
            .onGloballyPositioned { coordinates ->
                val layoutParent = coordinates.parentLayoutCoordinates ?: return@onGloballyPositioned
                val parentY = layoutParent.positionInWindow().y
                val parentHeight = layoutParent.size.height
                val myHeight = coordinates.size.height
                
                val targetOffset = if (parentY < 0) -parentY else 0f
                yOffset = targetOffset.coerceAtMost((parentHeight - myHeight).toFloat())
            }
            .graphicsLayer { 
                translationY = yOffset 
            }
            .run {
                if (hazeState != null) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 15.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
                    }
                } else this
            }
            .then(if (hasPhysicalKeyboard) Modifier.graphicsLayer { alpha = 0.5f } else Modifier),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                FormattingButton(
                    icon = Icons.Default.FormatBold,
                    isActive = MarkdownHelper.isFormatActive(value, "**", "**"),
                    enabled = isSelectionActive,
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "**", "**")) }
                )
                FormattingButton(
                    icon = Icons.Default.FormatItalic,
                    isActive = MarkdownHelper.isFormatActive(value, "_", "_"),
                    enabled = isSelectionActive,
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "_", "_")) }
                )
                FormattingButton(
                    icon = Icons.Default.FormatStrikethrough,
                    isActive = MarkdownHelper.isFormatActive(value, "~~", "~~"),
                    enabled = isSelectionActive,
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "~~", "~~")) }
                )
                FormattingButton(
                    icon = Icons.Default.VisibilityOff,
                    isActive = MarkdownHelper.isFormatActive(value, "::", "::"),
                    enabled = isSelectionActive,
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "::", "::")) }
                )
                FormattingButton(
                    icon = Icons.Default.FormatQuote,
                    isActive = MarkdownHelper.isFormatActive(value, "> ", ""),
                    enabled = isSelectionActive,
                    onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, "> ", "")) }
                )
                FormattingButton(
                    icon = Icons.Default.Link,
                    isActive = MarkdownHelper.isFormatActive(value, "[", "]("),
                    enabled = isSelectionActive,
                    onClick = onLinkRequest
                )
                
                VerticalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp).height(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )

                FormattingButton(
                    icon = Icons.Default.HorizontalRule,
                    isActive = false,
                    enabled = true,
                    onClick = { 
                        val prefix = if (value.text.isNotEmpty() && !value.text.endsWith("\n")) "\n" else ""
                        val suffix = "\n"
                        val insert = prefix + "---" + suffix
                        val newText = value.text.substring(0, value.selection.start) + insert + value.text.substring(value.selection.end)
                        onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.start + insert.length)))
                    }
                )
                FormattingButton(
                    icon = Icons.Default.AddBox,
                    isActive = false,
                    enabled = true,
                    onClick = {
                        val prefix = if (value.text.isNotEmpty() && !value.text.endsWith("\n")) "\n" else ""
                        val insert = prefix + "[Ресурс: Новый ресурс | cur=0 | max=0 | sr=0 | lr=all]\n"
                        val newText = value.text.substring(0, value.selection.start) + insert + value.text.substring(value.selection.end)
                        onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.start + insert.length)))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun FormattingButton(
    icon: ImageVector,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
