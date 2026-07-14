package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

class MarkdownTextToolbar(
    private val onBold: () -> Unit,
    private val onItalic: () -> Unit,
    private val onStrike: () -> Unit,
    private val onLink: () -> Unit,
    private val onQuote: () -> Unit
) : TextToolbar {
    
    private var _status by mutableStateOf(TextToolbarStatus.Hidden)
    override val status: TextToolbarStatus get() = _status
    
    private var menuRect by mutableStateOf(Rect.Zero)
    private var onCopy: (() -> Unit)? = null
    private var onPaste: (() -> Unit)? = null
    private var onCut: (() -> Unit)? = null
    private var onSelectAll: (() -> Unit)? = null

    override fun hide() {
        _status = TextToolbarStatus.Hidden
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        menuRect = rect
        onCopy = onCopyRequested
        onPaste = onPasteRequested
        onCut = onCutRequested
        onSelectAll = onSelectAllRequested
        _status = TextToolbarStatus.Shown
    }

    @Composable
    fun Content() {
        if (_status == TextToolbarStatus.Shown) {
            val popupOffsetY = if (menuRect.top > 120) menuRect.top - 110 else menuRect.bottom + 10
            
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(menuRect.left.toInt(), popupOffsetY.toInt()),
                onDismissRequest = { hide() },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true, dismissOnBackPress = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ToolbarButton(text = "Копировать", onClick = { onCopy?.invoke(); hide() })
                            ToolbarButton(text = "Вставить", onClick = { onPaste?.invoke(); hide() })
                            ToolbarButton(text = "Вырезать", onClick = { onCut?.invoke(); hide() })
                            ToolbarButton(text = "Все", onClick = { onSelectAll?.invoke(); hide() })
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { onBold(); hide() }) { Icon(Icons.Default.FormatBold, "Bold") }
                            IconButton(onClick = { onItalic(); hide() }) { Icon(Icons.Default.FormatItalic, "Italic") }
                            IconButton(onClick = { onStrike(); hide() }) { Icon(Icons.Default.FormatStrikethrough, "Strike") }
                            IconButton(onClick = { onLink(); hide() }) { Icon(Icons.Default.Link, "Link") }
                            IconButton(onClick = { onQuote(); hide() }) { Icon(Icons.Default.FormatQuote, "Quote") }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ToolbarButton(text: String, onClick: () -> Unit) {
        Text(
            text = text,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
