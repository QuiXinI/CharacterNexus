package ru.quasaris.characternexus.tabs

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@Composable
fun FormattingToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isFocused: Boolean,
    isSelectionActive: Boolean,
    onLinkRequest: () -> Unit,
    onSave: () -> Unit = {},
    viewportTopY: Float = 0f,
    onInsertResource: (() -> Unit)? = null,
    onInsertDivider: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    settingsViewModel: SettingsViewModel? = null,
    isOled: Boolean = false,
    editorWidthPx: Int = 0,
    editorLeftPx: Float = 0f,
    editorTopPx: Float = 0f,
    editorHeightPx: Int = 0,
    modifier: Modifier = Modifier
) {
    if (!isFocused) return

    val density = LocalDensity.current
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val useBlur = hazeState != null && !isOled && masterBlurEnabled && blurRadius > 0.dp

    var myHeightPx by remember { mutableIntStateOf(0) }

    val rawOffset = if (editorTopPx < viewportTopY) viewportTopY - editorTopPx else 0f

    val maxOffset = (editorHeightPx - myHeightPx).coerceAtLeast(0)
    val popupOffsetY = rawOffset.roundToInt().coerceAtMost(maxOffset)

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, popupOffsetY),
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = modifier
                .width(with(density) { editorWidthPx.toDp() })
                .onGloballyPositioned { coords ->
                    myHeightPx = coords.size.height
                }
                .run {
                    if (useBlur) {
                        this.clip(RoundedCornerShape(8.dp))
                            .hazePopover(
                                state = hazeState!!,
                                blurRadius = blurRadius,
                                isOled = false
                            )
                    } else {
                        this.outerShadow(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            blur = 4.dp,
                            offsetY = 4.dp
                        )
                    }
                },
            shape = RoundedCornerShape(8.dp),
            color = if (isOled) Color.Black else if (useBlur) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            val scrollState = rememberScrollState()
            val canScrollStart = scrollState.value > 0
            val canScrollEnd = scrollState.value < scrollState.maxValue
            val surfaceColor = if (isOled) Color.Black else MaterialTheme.colorScheme.surfaceVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
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
                        isActive = MarkdownHelper.isFormatActive(value, ">> ", " <<"),
                        enabled = isSelectionActive,
                        onClick = { onValueChange(MarkdownHelper.applyMarkdown(value, ">> ", " <<")) }
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
                            if (onInsertDivider != null) {
                                onInsertDivider()
                            } else {
                                val prefix = if (value.text.isNotEmpty() && !value.text.endsWith("\n")) "\n" else ""
                                val suffix = "\n"
                                val insert = prefix + "---" + suffix
                                val newText = value.text.substring(0, value.selection.min) + insert + value.text.substring(value.selection.max)
                                onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.min + insert.length)))
                            }
                        }
                    )
                    FormattingButton(
                        icon = Icons.Default.AddBox,
                        isActive = false,
                        enabled = true,
                        onClick = {
                            if (onInsertResource != null) {
                                onInsertResource()
                            } else {
                                val id = ru.quasaris.characternexus.util.generateUuid()
                                val prefix = if (value.text.isNotEmpty() && !value.text.endsWith("\n")) "\n" else ""
                                val insert = prefix + "{Ресурс: Новый ресурс | cur=0 | max=0 | id=$id}\n"
                                val newText = value.text.substring(0, value.selection.min) + insert + value.text.substring(value.selection.max)
                                onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.min + insert.length)))
                            }
                        }
                    )

                    VerticalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp).height(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )

                    FormattingButton(
                        icon = Icons.Default.Done,
                        isActive = false,
                        enabled = true,
                        onClick = onSave
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canScrollStart) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(32.dp)
                            .matchParentSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f))
                                )
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Прокрутка влево",
                            modifier = Modifier.padding(start = 2.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                if (canScrollEnd) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(32.dp)
                            .matchParentSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(surfaceColor.copy(alpha = 0f), surfaceColor)
                                )
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Прокрутка вправо",
                            modifier = Modifier.padding(end = 2.dp).size(18.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
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
        modifier = Modifier
            .size(36.dp)
            .focusProperties { canFocus = false }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
