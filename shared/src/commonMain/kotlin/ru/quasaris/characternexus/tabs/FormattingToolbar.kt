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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

val InfoBoxIcon: ImageVector = ImageVector.Builder(
    name = "InfoBox",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        lineTo(3f, 19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        lineTo(19f, 21f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        lineTo(21f, 5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()

        moveTo(11f, 6.5f)
        lineTo(13f, 6.5f)
        lineTo(13f, 8.5f)
        lineTo(11f, 8.5f)
        close()

        moveTo(11f, 10f)
        lineTo(13f, 10f)
        lineTo(13f, 17f)
        lineTo(11f, 17f)
        close()
    }
}.build()

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
    onInsertInfoBlock: (() -> Unit)? = null,
    onInsertDivider: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    settingsViewModel: SettingsViewModel? = null,
    isOled: Boolean = false,
    editorWidthPx: Int = 0,
    editorLeftPx: Float = 0f,
    editorTopPx: Float = 0f,
    editorHeightPx: Int = 0,
    isBasicMode: Boolean = false,
    isPlainTextMode: Boolean = false,
    onTogglePlainText: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isFocused) return

    val density = LocalDensity.current
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val useBlur = hazeState != null && !isOled && masterBlurEnabled && blurRadius > 0.dp

    var myHeightPx by remember { mutableIntStateOf(0) }
    val toolbarHeight = if (myHeightPx > 0) myHeightPx else with(density) { 44.dp.roundToPx() }

    val rawOffset = if (editorTopPx < viewportTopY) {
        viewportTopY - editorTopPx
    } else {
        with(density) { 4.dp.toPx() }
    }

    val maxOffset = (editorHeightPx - toolbarHeight).coerceAtLeast(0)
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
                    HeaderDropdownMenu(
                        value = value,
                        onValueChange = onValueChange
                    )
                    VerticalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp).height(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
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
                    if (!isBasicMode) {
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
                    }
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
                    if (!isBasicMode) {
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
                                    val insert = prefix + "{Ресурс: id=$id}\n"
                                    val newText = value.text.substring(0, value.selection.min) + insert + value.text.substring(value.selection.max)
                                    onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.min + insert.length)))
                                }
                            }
                        )
                        FormattingButton(
                            icon = InfoBoxIcon,
                            isActive = false,
                            enabled = true,
                            onClick = {
                                if (onInsertInfoBlock != null) {
                                    onInsertInfoBlock()
                                } else {
                                    val id = ru.quasaris.characternexus.util.generateUuid()
                                    val prefix = if (value.text.isNotEmpty() && !value.text.endsWith("\n")) "\n" else ""
                                    val insert = prefix + "{Инфоблок: id=$id}\n"
                                    val newText = value.text.substring(0, value.selection.min) + insert + value.text.substring(value.selection.max)
                                    onValueChange(value.copy(text = newText, selection = androidx.compose.ui.text.TextRange(value.selection.min + insert.length)))
                                }
                            }
                        )

                        VerticalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp).height(24.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }

                    if (onTogglePlainText != null) {
                        FormattingButton(
                            icon = Icons.Default.Code,
                            isActive = isPlainTextMode,
                            enabled = true,
                            onClick = onTogglePlainText
                        )
                    }

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

@Composable
private fun HeaderDropdownMenu(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLevel = remember(value.text, value.selection) {
        MarkdownHelper.getHeaderLevel(value)
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (currentLevel > 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (currentLevel > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .height(36.dp)
                .focusProperties { canFocus = false }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Title,
                    contentDescription = "Заголовки",
                    modifier = Modifier.size(20.dp)
                )
                if (currentLevel > 0) {
                    Text(
                        text = "$currentLevel",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Обычный текст",
                        fontWeight = if (currentLevel == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                },
                onClick = {
                    onValueChange(MarkdownHelper.applyHeader(value, 0))
                    expanded = false
                },
                leadingIcon = {
                    if (currentLevel == 0) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Заголовок 1 (#)",
                        fontWeight = if (currentLevel == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 20.sp
                    )
                },
                onClick = {
                    onValueChange(MarkdownHelper.applyHeader(value, 1))
                    expanded = false
                },
                leadingIcon = {
                    if (currentLevel == 1) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Заголовок 2 (##)",
                        fontWeight = if (currentLevel == 2) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 18.sp
                    )
                },
                onClick = {
                    onValueChange(MarkdownHelper.applyHeader(value, 2))
                    expanded = false
                },
                leadingIcon = {
                    if (currentLevel == 2) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Заголовок 3 (###)",
                        fontWeight = if (currentLevel == 3) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                },
                onClick = {
                    onValueChange(MarkdownHelper.applyHeader(value, 3))
                    expanded = false
                },
                leadingIcon = {
                    if (currentLevel == 3) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    }
}
