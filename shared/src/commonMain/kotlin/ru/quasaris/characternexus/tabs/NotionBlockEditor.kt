package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.model.NoteBlockState
import ru.quasaris.characternexus.tabs.resources.ResourceBlock
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.outerShadow
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun NotionBlockEditor(
    field: DynamicNoteState,
    onFieldChange: (DynamicNoteState) -> Unit,
    canEdit: Boolean,
    isReorderMode: Boolean,
    contentPlaceholder: String,
    statsMap: Map<String, String> = emptyMap(),
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    settingsViewModel: SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    val blocks = remember(field.id) {
        mutableStateListOf<NoteBlockState>().apply { addAll(BlockContentParser.toNoteBlocks(field.content)) }
    }
    var lastEmittedText by remember(field.id) { mutableStateOf(field.content) }

    var activeKey by remember { mutableStateOf<String?>(null) }
    var isAnyFocused by remember { mutableStateOf(false) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }
    var pendingFocusOffset by remember { mutableIntStateOf(0) }
    
    var blockToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    fun focusRequesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }

    val liveSelections = remember { mutableStateMapOf<String, TextRange>() }

    LaunchedEffect(isAnyFocused, activeKey, pendingFocusKey) {
        if (!isAnyFocused && activeKey != null && pendingFocusKey == null) {
            kotlinx.coroutines.delay(100.milliseconds)
            if (!isAnyFocused && pendingFocusKey == null) {
                activeKey = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeKey = null
        }
    }

    LaunchedEffect(field.content) {
        if (field.content != lastEmittedText && activeKey == null) {
            val fresh = BlockContentParser.toBlocks(field.content)
            val reconciled = BlockContentParser.reconcile(blocks, fresh)
            
            if (blocks.size == reconciled.size) {
                reconciled.forEachIndexed { i, r ->
                    if (blocks[i] != r) blocks[i] = r
                }
            } else {
                blocks.clear()
                blocks.addAll(reconciled)
            }
            lastEmittedText = field.content
        }
    }

    fun emit() {
        val text = BlockContentParser.toText(blocks.map { it.block })
        lastEmittedText = text
        onFieldChange(field.copy(content = text))
    }

    fun deleteBlockAt(idx: Int) {
        if (idx !in blocks.indices) return
        val removed = blocks.removeAt(idx)
        if (activeKey == removed.key) {
            activeKey = null
            liveSelections.remove(removed.key)
        }
        emit()
    }

    fun requestDelete(idx: Int) {
        if (idx !in blocks.indices) return
        val item = blocks[idx]
        val text = item.textContent
        val shouldDeleteImmediately = item.isTextLike && text?.isBlank() == true
        if (shouldDeleteImmediately) {
            deleteBlockAt(idx)
        } else {
            blockToDeleteIndex = idx
        }
    }

    fun mergeIntoPrevious(key: String) {
        val idx = blocks.indexOfFirst { it.key == key }
        if (idx <= 0) return
        val current = blocks[idx]
        val previous = blocks[idx - 1]

        if (current.isTextLike && previous.isTextLike) {
            val prevText = previous.textContent ?: ""
            val currText = current.textContent ?: ""
            val combinedText = prevText + currText

            blocks[idx - 1] = previous.withTextContent(combinedText)
            blocks.removeAt(idx)

            activeKey = previous.key
            pendingFocusKey = previous.key
            pendingFocusOffset = prevText.length
            liveSelections[previous.key] = TextRange(prevText.length)
            emit()
        } else if (current.textContent?.isEmpty() == true) {
            blocks.removeAt(idx)
            emit()
        }
    }

    LaunchedEffect(pendingFocusKey) {
        val key = pendingFocusKey ?: return@LaunchedEffect
        activeKey = key
        liveSelections[key] = TextRange(pendingFocusOffset)
        runCatching { focusRequesterFor(key).requestFocus() }
        pendingFocusKey = null
    }

    fun spliceBlockValue(key: String, newValue: TextFieldValue) {
        val idx = blocks.indexOfFirst { it.key == key }
        if (idx == -1) return
        val originalBlock = blocks[idx].block

        if (!newValue.text.contains('\n')) {
            blocks[idx] = blocks[idx].withTextContent(newValue.text)
            liveSelections[key] = newValue.selection
            emit()
            return
        }

        val lines = newValue.text.split('\n')
        val newStates = mutableListOf<NoteBlockState>()
        
        lines.forEachIndexed { i, line ->
            val block = when {
                i == 0 -> originalBlock
                i == lines.lastIndex && line.isEmpty() -> DynamicContentBlock.Text("")
                else -> originalBlock
            }
            
            val finalBlock = when (block) {
                is DynamicContentBlock.Spoiler -> block.copy(content = line)
                is DynamicContentBlock.Quote -> block.copy(content = line)
                else -> DynamicContentBlock.Text(line)
            }
            newStates.add(if (i == 0) blocks[idx].copy(block = finalBlock) else NoteBlockState(block = finalBlock))
        }

        blocks.removeAt(idx)
        blocks.addAll(idx, newStates)

        var consumed = 0
        var targetSubIndex = 0
        var targetOffset = 0
        for ((i, line) in lines.withIndex()) {
            val lineLen = line.length
            if (newValue.selection.start <= consumed + lineLen) {
                targetSubIndex = i
                targetOffset = (newValue.selection.start - consumed).coerceIn(0, lineLen)
                break
            }
            consumed += lineLen + 1
        }
        pendingFocusKey = newStates[targetSubIndex].key
        pendingFocusOffset = targetOffset
        emit()
    }

    fun insertAfterActive(newBlock: DynamicContentBlock) {
        val idx = activeKey?.let { k -> blocks.indexOfFirst { it.key == k } } ?: (blocks.size - 1)
        val insertAt = (idx + 1).coerceIn(0, blocks.size)
        val inserted = NoteBlockState(block = newBlock)
        blocks.add(insertAt, inserted)
        if (inserted.isTextLike) {
            pendingFocusKey = inserted.key
            pendingFocusOffset = 0
        }
        emit()
    }

    val activeValue = activeKey?.let { key ->
        val idx = blocks.indexOfFirst { it.key == key }
        if (idx == -1) null else {
            val text = blocks[idx].textContent ?: return@let null
            TextFieldValue(text = text, selection = liveSelections[key] ?: TextRange(text.length))
        }
    }
    var showLinkDialog by remember { mutableStateOf(false) }

    if (showLinkDialog && activeKey != null && activeValue != null) {
        val selection = activeValue.selection
        val selectedText = activeValue.text.substring(selection.min, selection.max)
        HyperlinkDialog(
            initialText = selectedText,
            initialUrl = "",
            onConfirm = { text, url ->
                val newText = activeValue.text.substring(0, selection.min) + "[" + text + "](" + url + ")" + activeValue.text.substring(selection.max)
                val newSelection = TextRange(selection.min + text.length + url.length + 4)
                spliceBlockValue(activeKey!!, activeValue.copy(text = newText, selection = newSelection))
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxWidth().onFocusChanged { isAnyFocused = it.hasFocus }) {
        if (canEdit && activeKey != null && activeValue != null) {
            FormattingToolbar(
                value = activeValue,
                onValueChange = { spliceBlockValue(activeKey!!, it) },
                isFocused = true,
                isSelectionActive = activeValue.selection.length > 0,
                onLinkRequest = { showLinkDialog = true },
                onSave = { activeKey = null }
            )
        }

        DeleteConfirmationDialog(
            showDialog = blockToDeleteIndex != null,
            onDismiss = { blockToDeleteIndex = null },
            onConfirm = {
                blockToDeleteIndex?.let { idx -> deleteBlockAt(idx) }
                blockToDeleteIndex = null
            },
            settingsViewModel = settingsViewModel
        )

        BlockDragColumn(
            blocks = blocks,
            isReorderMode = isReorderMode,
            onReordered = { emit() }
        ) { index, item, dragHandleModifier ->
            val absoluteIndex = index
            key(item.key) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isReorderMode) {
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* swallow clicks */ }
                            } else Modifier
                        )
                ) {
                    if (isReorderMode) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Reorder",
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(20.dp)
                                .then(dragHandleModifier)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (val block = item.block) {
                            is DynamicContentBlock.Text -> LineTextBlockRow(
                                item = item,
                                text = block.content,
                                placeholder = if (blocks.size == 1) contentPlaceholder else null,
                                isActive = item.key == activeKey && canEdit && !isReorderMode,
                                canEdit = canEdit && !isReorderMode,
                                selection = liveSelections[item.key] ?: TextRange(block.content.length),
                                focusRequester = focusRequesterFor(item.key),
                                onActivate = { offset ->
                                    activeKey = item.key
                                    pendingFocusKey = item.key
                                    pendingFocusOffset = offset ?: block.content.length
                                    liveSelections[item.key] = TextRange(pendingFocusOffset)
                                },
                                onValueChange = { spliceBlockValue(item.key, it) },
                                onBackspaceEmpty = { mergeIntoPrevious(item.key) }
                            )

                            is DynamicContentBlock.Spoiler -> LineTextBlockRow(
                                item = item,
                                text = block.content,
                                placeholder = null,
                                isActive = item.key == activeKey && canEdit && !isReorderMode,
                                canEdit = canEdit && !isReorderMode,
                                selection = liveSelections[item.key] ?: TextRange(block.content.length),
                                focusRequester = focusRequesterFor(item.key),
                                prefixIcon = Icons.Default.VisibilityOff,
                                previewWrapper = { content -> SpoilerComponent(content = content) },
                                onActivate = { offset ->
                                    activeKey = item.key
                                    pendingFocusKey = item.key
                                    pendingFocusOffset = offset ?: block.content.length
                                    liveSelections[item.key] = TextRange(pendingFocusOffset)
                                },
                                onValueChange = { spliceBlockValue(item.key, it) },
                                onBackspaceEmpty = { mergeIntoPrevious(item.key) }
                            )

                            is DynamicContentBlock.Quote -> LineTextBlockRow(
                                item = item,
                                text = block.content,
                                placeholder = null,
                                isActive = item.key == activeKey && canEdit && !isReorderMode,
                                canEdit = canEdit && !isReorderMode,
                                selection = liveSelections[item.key] ?: TextRange(block.content.length),
                                focusRequester = focusRequesterFor(item.key),
                                prefixIcon = Icons.Default.FormatQuote,
                                previewWrapper = { content -> QuoteComponent(content = content) },
                                onActivate = { offset ->
                                    activeKey = item.key
                                    pendingFocusKey = item.key
                                    pendingFocusOffset = offset ?: block.content.length
                                    liveSelections[item.key] = TextRange(pendingFocusOffset)
                                },
                                onValueChange = { spliceBlockValue(item.key, it) },
                                onBackspaceEmpty = { mergeIntoPrevious(item.key) }
                            )

                            is DynamicContentBlock.Divider -> DividerBlockRow()

                            is DynamicContentBlock.Resource -> ResourceBlock(
                                resource = block,
                                statsMap = statsMap,
                                onUpdate = { updated ->
                                    val idx = blocks.indexOfFirst { it.key == item.key }
                                    if (idx != -1) {
                                        blocks[idx] = blocks[idx].copy(block = updated)
                                        emit()
                                        if (updated.id.isNotEmpty()) state?.resourceManager?.upsert(updated)
                                    }
                                },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurDynamicFields = blurDynamicFields,
                                settingsViewModel = settingsViewModel,
                                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                onSubDialogOpenChange = { },
                                state = state,
                                onOpenConfig = state?.let { s ->
                                    { res: DynamicContentBlock.Resource ->
                                        s.activeResourceConfig = res
                                        s.activeResourceIndex = absoluteIndex
                                        s.activeNoteId = field.id
                                        s.isResourceConfigOpen = true
                                    }
                                },
                                onDeleteRequest = { requestDelete(absoluteIndex) }
                            )

                            is DynamicContentBlock.ResourceRef -> {
                                val resource = state?.resourceManager?.get(block.id)
                                    ?: DynamicContentBlock.Resource(name = "Загрузка...", current = "0", max = "0", id = block.id)
                                ResourceBlock(
                                    resource = resource,
                                    statsMap = statsMap,
                                    onUpdate = { updated -> state?.resourceManager?.upsert(updated) },
                                    hazeState = hazeState,
                                    forceBlurEnabled = forceBlurEnabled,
                                    blurDynamicFields = blurDynamicFields,
                                    settingsViewModel = settingsViewModel,
                                    onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                    onSubDialogOpenChange = { },
                                    state = state,
                                    onOpenConfig = state?.let { s ->
                                        { res: DynamicContentBlock.Resource ->
                                            s.activeResourceConfig = res
                                            s.activeResourceIndex = absoluteIndex
                                            s.activeNoteId = field.id
                                            s.isResourceConfigOpen = true
                                        }
                                    },
                                    onDeleteRequest = { requestDelete(absoluteIndex) }
                                )
                            }
                        }
                    }

                    if (isReorderMode) {
                        IconButton(onClick = { requestDelete(absoluteIndex) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Block",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        if (canEdit && !isReorderMode) {
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(
                    onClick = { insertAfterActive(DynamicContentBlock.Text("")) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Строка", fontSize = 13.sp)
                }
                TextButton(
                    onClick = { insertAfterActive(DynamicContentBlock.Divider) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Разделитель", fontSize = 13.sp)
                }
                TextButton(
                    onClick = {
                        insertAfterActive(DynamicContentBlock.Resource(name = "Новый ресурс", current = "0", max = "0"))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ресурс", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LineTextBlockRow(
    item: NoteBlockState,
    text: String,
    placeholder: String?,
    isActive: Boolean,
    canEdit: Boolean,
    selection: TextRange,
    focusRequester: FocusRequester,
    prefixIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    previewWrapper: (@Composable (androidx.compose.ui.text.AnnotatedString) -> Unit)? = null,
    onActivate: (offset: Int?) -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onBackspaceEmpty: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    if (isActive) {
        val initialInternalText = "\u200B" + text
        val initialInternalSelection = TextRange(
            (selection.start + 1).coerceIn(1, initialInternalText.length),
            (selection.end + 1).coerceIn(1, initialInternalText.length)
        )

        var internalValue by remember {
            mutableStateOf(TextFieldValue(text = initialInternalText, selection = initialInternalSelection))
        }

        val currentCleanText = internalValue.text.removePrefix("\u200B")
        val currentCleanSelection = TextRange(
            (internalValue.selection.start - 1).coerceIn(0, currentCleanText.length),
            (internalValue.selection.end - 1).coerceIn(0, currentCleanText.length)
        )

        if (currentCleanText != text || currentCleanSelection != selection) {
            internalValue = TextFieldValue(text = initialInternalText, selection = initialInternalSelection)
        }

        BasicTextField(
            value = internalValue,
            onValueChange = { newValue ->
                if (!newValue.text.startsWith("\u200B")) {
                    onBackspaceEmpty()
                } else {
                    val adjustedSelection = TextRange(
                        newValue.selection.start.coerceAtLeast(1),
                        newValue.selection.end.coerceAtLeast(1)
                    )
                    val finalValue = if (adjustedSelection != newValue.selection) {
                        newValue.copy(selection = adjustedSelection)
                    } else {
                        newValue
                    }

                    internalValue = finalValue
                    val cleanText = finalValue.text.removePrefix("\u200B")
                    val cleanSelection = TextRange(
                        (finalValue.selection.start - 1).coerceIn(0, cleanText.length),
                        (finalValue.selection.end - 1).coerceIn(0, cleanText.length)
                    )
                    onValueChange(TextFieldValue(text = cleanText, selection = cleanSelection))
                }
            },
            enabled = canEdit,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp, color = colorScheme.onSurface),
            cursorBrush = SolidColor(colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.Top) {
                    if (prefixIcon != null) {
                        Icon(prefixIcon, contentDescription = null, modifier = Modifier.padding(top = 3.dp, end = 6.dp).size(16.dp), tint = colorScheme.onSurfaceVariant)
                    }
                    Box {
                        if (text.isEmpty() && placeholder != null) {
                            Text(placeholder, fontSize = 17.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        inner()
                    }
                }
            }
        )
        LaunchedEffect(item.key) { runCatching { focusRequester.requestFocus() } }
    } else {
        val onSurface = colorScheme.onSurface
        val annotated = remember(text, onSurface) { MarkdownHelper.parseMarkdown(text, onSurface, isEditing = false) }
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        val rowModifier = Modifier
            .fillMaxWidth()
            .pointerInput(annotated, canEdit) {
                detectTapGestures(onTap = { offset ->
                    val layout = layoutResult
                    if (layout != null) {
                        val pos = layout.getOffsetForPosition(offset)
                        val link = annotated.getLinkAnnotations(pos, pos).firstOrNull()
                        if (link != null && link.item is LinkAnnotation.Url) {
                            uriHandler.openUri((link.item as LinkAnnotation.Url).url)
                            return@detectTapGestures
                        }
                    }
                    if (canEdit) onActivate(layout?.getOffsetForPosition(offset))
                })
            }

        if (previewWrapper != null) {
            Box(modifier = rowModifier) { previewWrapper(annotated) }
        } else if (text.isEmpty() && placeholder != null) {
            Text(placeholder, fontSize = 17.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = rowModifier)
        } else {
            Text(
                text = annotated,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = onSurface,
                modifier = rowModifier,
                onTextLayout = { layoutResult = it }
            )
        }
    }
}

@Composable
private fun DividerBlockRow() {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f).padding(vertical = 8.dp), thickness = 1.dp, color = colorScheme.outlineVariant)
    }
}

@Composable
private fun BlockDragColumn(
    blocks: SnapshotStateList<NoteBlockState>,
    isReorderMode: Boolean,
    onReordered: () -> Unit,
    content: @Composable (index: Int, item: NoteBlockState, dragHandleModifier: Modifier) -> Unit
) {
    val rowHeights = remember { mutableStateMapOf<String, Int>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val spacingPx = with(density) { 4.dp.toPx() }

    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, item ->
            val isDragged = isReorderMode && item.key == draggingKey
            key(item.key) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords -> rowHeights[item.key] = coords.size.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { if (isDragged) translationY = dragOffsetY }
                ) {
                    val dragHandleModifier = if (isReorderMode) {
                        Modifier.pointerInput(item.key, blocks.size) {
                            detectDragGestures(
                                onDragStart = { draggingKey = item.key; dragOffsetY = 0f },
                                onDragEnd = {
                                    val moved = draggingKey != null
                                    draggingKey = null
                                    dragOffsetY = 0f
                                    if (moved) onReordered()
                                },
                                onDragCancel = { draggingKey = null; dragOffsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val myKey = item.key
                                    val myIndex = blocks.indexOfFirst { it.key == myKey }
                                    if (myIndex == -1) return@detectDragGestures
                                    val myHeight = (rowHeights[myKey] ?: 0) + spacingPx
                                    if (myHeight <= 0f) return@detectDragGestures
                                    if (dragOffsetY > myHeight / 2f && myIndex + 1 < blocks.size) {
                                        val nextHeight = (rowHeights[blocks[myIndex + 1].key] ?: 0) + spacingPx
                                        blocks.add(myIndex, blocks.removeAt(myIndex + 1))
                                        dragOffsetY -= nextHeight
                                    } else if (dragOffsetY < -myHeight / 2f && myIndex - 1 >= 0) {
                                        val prevHeight = (rowHeights[blocks[myIndex - 1].key] ?: 0) + spacingPx
                                        blocks.add(myIndex, blocks.removeAt(myIndex - 1))
                                        dragOffsetY += prevHeight
                                    }
                                }
                            )
                        }
                    } else Modifier

                    content(index, item, dragHandleModifier)
                }
            }
            if (index != blocks.lastIndex) Spacer(Modifier.height(4.dp))
        }
    }
}
