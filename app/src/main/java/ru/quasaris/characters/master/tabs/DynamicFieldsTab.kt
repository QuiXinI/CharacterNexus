package ru.quasaris.characters.master.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.ui.DeleteConfirmationDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch

@Composable
fun HyperlinkDialog(
    initialText: String,
    initialUrl: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вставить гиперссылку") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Ссылка (URL)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, url) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DynamicFieldsTab(
    fields: List<DynamicNoteState>,
    onFieldsChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    addButtonText: String = "ДОБАВИТЬ ПОЛЕ",
    emptyListText: String = "Список пуст",
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    isCollapsible: Boolean = true,
    isTitleReadOnly: Boolean = false,
    isAddButtonVisible: Boolean = true,
    isReorderButtonVisible: Boolean = true,
    isScrollEnabled: Boolean = true,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    val items = remember { mutableStateListOf<DynamicNoteState>().apply { addAll(fields) } }

    LaunchedEffect(fields) {
        if (items.size != fields.size || items.indices.any { items[it].id != fields[it].id }) {
            items.clear()
            items.addAll(fields)
        } else {
            fields.forEachIndexed { index, field ->
                if (items[index] != field) {
                    items[index] = field
                }
            }
        }
    }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    
    var fullscreenFieldIndex by remember { mutableStateOf<Int?>(null) }
    var fieldToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    
    val fullscreenEditingOnly by settingsViewModel?.fullscreenEditingOnly?.collectAsState() ?: remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (items.isEmpty() && isScrollEnabled) { // Show empty text only if it's the main scrolling content
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyListText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            userScrollEnabled = isScrollEnabled,
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                header()
            }

            itemsIndexed(items, key = { _, field -> field.id }) { index, field ->
                val isDragging = draggedItemIndex == index
                
                val dragModifier = if (isEditMode && isReorderButtonVisible) {
                    Modifier.pointerInput(index) {
                        detectDragGestures(
                            onDragStart = { 
                                draggedItemIndex = index
                                draggingOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingOffset += dragAmount.y
                                
                                val layoutInfo = listState.layoutInfo
                                val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                                
                                if (draggedItemInfo != null) {
                                    val currentCenter = draggedItemInfo.offset + (draggedItemInfo.size / 2) + draggingOffset.toInt()
                                    val targetItem = layoutInfo.visibleItemsInfo.find { item ->
                                        item.index != index && currentCenter in item.offset..(item.offset + item.size)
                                    }

                                    if (targetItem != null) {
                                        val targetIndex = targetItem.index
                                        if (targetIndex in items.indices) {
                                            items.add(targetIndex, items.removeAt(index))
                                            draggingOffset += (draggedItemInfo.offset - targetItem.offset)
                                            draggedItemIndex = targetIndex
                                            onFieldsChange(items.toList())
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedItemIndex = null
                                draggingOffset = 0f
                            },
                            onDragCancel = {
                                draggedItemIndex = null
                                draggingOffset = 0f
                            }
                        )
                    }
                } else Modifier

                DynamicFieldItem(
                    field = field,
                    isEditMode = isEditMode,
                    isDragging = isDragging,
                    titlePlaceholder = titlePlaceholder,
                    contentPlaceholder = contentPlaceholder,
                    onFieldChange = { updatedField ->
                        items[index] = updatedField
                        onFieldsChange(items.toList())
                    },
                    onDelete = { fieldToDeleteIndex = index },
                    onFullscreenRequest = { fullscreenFieldIndex = index },
                    dragModifier = dragModifier,
                    modifier = Modifier.animateItem(),
                    extraContent = extraContent,
                    isCollapsible = isCollapsible,
                    isTitleReadOnly = isTitleReadOnly,
                    isReorderButtonVisible = isReorderButtonVisible,
                    isLockedGlobal = fullscreenEditingOnly,
                    hazeState = hazeState,
                    forceBlurEnabled = forceBlurEnabled,
                    blurPopups = blurPopups,
                    settingsViewModel = settingsViewModel,
                    statsMap = statsMap
                )
            }

            item {
                footer()
            }

            if (isAddButtonVisible) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val newFields = items.toList() + DynamicNoteState()
                            onFieldsChange(newFields)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(addButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    DeleteConfirmationDialog(
        showDialog = fieldToDeleteIndex != null,
        onDismiss = { fieldToDeleteIndex = null },
        onConfirm = {
            fieldToDeleteIndex?.let { index ->
                if (index in items.indices) {
                    items.removeAt(index)
                    onFieldsChange(items.toList())
                }
            }
            fieldToDeleteIndex = null
        },
        settingsViewModel = settingsViewModel
    )

    fullscreenFieldIndex?.let { index ->
        if (index in items.indices) {
            val field = items[index]
            DynamicFieldFullscreenDialog(
                field = field,
                titlePlaceholder = titlePlaceholder,
                contentPlaceholder = contentPlaceholder,
                onFieldChange = { updatedField ->
                    val newList = items.toMutableList()
                    newList[index] = updatedField
                    onFieldsChange(newList)
                },
                onDelete = {
                    val newList = items.toMutableList()
                    newList.removeAt(index)
                    onFieldsChange(newList)
                    fullscreenFieldIndex = null
                },
                onDismiss = { fullscreenFieldIndex = null },
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = settingsViewModel,
                statsMap = statsMap
            )
        }
    }
}

@Composable
fun DynamicFieldItem(
    field: DynamicNoteState,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit = {},
    onFullscreenRequest: () -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    isCollapsible: Boolean = true,
    isTitleReadOnly: Boolean = false,
    isReorderButtonVisible: Boolean = true,
    isLockedGlobal: Boolean = false,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val isExpanded = if (isCollapsible) field.isExpanded else true
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 0f else 180f)
    val scale by animateFloatAsState(targetValue = if (isEditMode) 0.95f else 1f)
    val padding by animateDpAsState(targetValue = if (isEditMode) 8.dp else 0.dp)
    
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val canEdit = !isEditMode && !isLockedGlobal && !field.isLocked

    var contentValue by remember { mutableStateOf(TextFieldValue(field.content)) }
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    
    // Debounced formatting state
    var toolbarState by remember { mutableStateOf(Triple(contentValue, false, false)) } // (value, isFocused, isSelectionActive)
    
    LaunchedEffect(contentValue, isFocused) {
        if (!isFocused) {
            toolbarState = Triple(contentValue, false, false)
            return@LaunchedEffect
        }
        // Debounce selection changes (0.2s)
        kotlinx.coroutines.delay(200)
        toolbarState = Triple(contentValue, true, contentValue.selection.length > 0)
    }

    LaunchedEffect(contentValue.selection, contentValue.text, isFocused) {
        val layoutResult = textLayoutResult
        if (isFocused && layoutResult != null && contentValue.selection.collapsed) {
            val cursorRect = layoutResult.getCursorRect(contentValue.selection.start)
            coroutineScope.launch {
                bringIntoViewRequester.bringIntoView(
                    cursorRect.copy(
                        top = cursorRect.top - 40f, // Margin for toolbar
                        bottom = cursorRect.bottom + 40f
                    )
                )
            }
        }
    }

    LaunchedEffect(field.content) {
        if (field.content != contentValue.text) {
            contentValue = contentValue.copy(text = field.content)
        }
    }
    var showLinkDialog by remember { mutableStateOf(false) }

    if (showLinkDialog) {
        val selection = contentValue.selection
        val selectedText = contentValue.text.substring(selection.start, selection.end)
        HyperlinkDialog(
            initialText = selectedText,
            initialUrl = "",
            onConfirm = { text, url ->
                val prefix = "["
                val middle = "]("
                val suffix = ")"
                val newText = contentValue.text.substring(0, selection.start) + prefix + text + middle + url + suffix + contentValue.text.substring(selection.end)
                val newSelection = TextRange(selection.start + prefix.length + text.length + middle.length + url.length + suffix.length)
                val new = contentValue.copy(text = newText, selection = newSelection)
                contentValue = new
                onFieldChange(field.copy(content = new.text))
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(padding),
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded || isEditMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEditMode) 0.6f else 0.3f) 
                else Color.Transparent,
        shadowElevation = if (isDragging) 8.dp else (if (isExpanded || isEditMode) 1.dp else 0.dp),
        border = if (isExpanded || isEditMode) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                 else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode && isReorderButtonVisible) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Drag",
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(32.dp)
                        .then(dragModifier),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (isEditMode) 12.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (isEditMode || !isCollapsible) this else this.clickable { onFieldChange(field.copy(isExpanded = !isExpanded)) }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = field.title,
                        onValueChange = { onFieldChange(field.copy(title = it)) },
                        enabled = canEdit && !isTitleReadOnly,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (field.title.isEmpty()) {
                                    Text(
                                        titlePlaceholder,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                                innerTextField()
                                if (field.isLocked || isLockedGlobal) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    )

                    if (!isEditMode) {
                        if (isCollapsible) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onFieldChange(field.copy(isExpanded = !isExpanded)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .rotate(rotation),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            // Slot trackers or other content can be placed here if we move them
                            extraContent(field)
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded && !isEditMode) {
                    Column {
                        if (isCollapsible) {
                            extraContent(field)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            FormattingToolbar(
                                value = toolbarState.first,
                                onValueChange = {
                                    contentValue = it
                                    onFieldChange(field.copy(content = it.text))
                                },
                                isFocused = toolbarState.second,
                                isSelectionActive = toolbarState.third,
                                onLinkRequest = { showLinkDialog = true },
                                onSave = { focusManager.clearFocus() },
                                hazeState = hazeState
                            )

                            key(field.id) {
                                BasicTextField(
                                    value = contentValue,
                                    onValueChange = { 
                                        val oldText = contentValue.text
                                        contentValue = it
                                        if (oldText != it.text) {
                                            onFieldChange(field.copy(content = it.text))
                                        }
                                    },
                                    enabled = canEdit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isFocused = it.isFocused },
                                    onTextLayout = { textLayoutResult = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp,
                                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            val onSurface = MaterialTheme.colorScheme.onSurface
                                            val uriHandler = LocalUriHandler.current
                                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                            val blocks = remember(field.content) {
                                                DynamicContentParser.parse(field.content)
                                            }

                                            val marginStep by settingsViewModel?.topMarginStep?.collectAsState() ?: remember { mutableStateOf(2) }
                                            val customMargin by settingsViewModel?.customTopMargin?.collectAsState() ?: remember { mutableStateOf(96) }
                                            val topMargin = if (marginStep == 5) customMargin.dp else (marginStep * 48).dp

                                            if (isFocused) {
                                                Column {
                                                    if (toolbarState.second) {
                                                        Spacer(modifier = Modifier.height(topMargin))
                                                    }
                                                    innerTextField()
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }
                                            } else {
                                                // Raw text field must still be present for focus/IME, but we hide it and remove from layout height
                                                Box(
                                                    Modifier
                                                        .alpha(0f)
                                                        .layout { measurable, constraints ->
                                                            val placeable = measurable.measure(constraints)
                                                            layout(placeable.width, 0) {
                                                                placeable.place(0, 0)
                                                            }
                                                        }
                                                ) {
                                                    innerTextField()
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                ) {
                                                    blocks.forEach { block ->
                                                        when (block) {
                                                            is DynamicContentBlock.Text -> {
                                                                val annotated = remember(block.content, onSurface) {
                                                                    MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                }
                                                                Text(
                                                                    text = annotated,
                                                                    fontSize = 17.sp,
                                                                    lineHeight = 24.sp,
                                                                    color = onSurface,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .pointerInput(annotated) {
                                                                            detectTapGestures(
                                                                                onTap = { offset ->
                                                                                    layoutResult?.let { textLayoutResult ->
                                                                                        val position = textLayoutResult.getOffsetForPosition(offset)
                                                                                        val line = textLayoutResult.getLineForOffset(position)
                                                                                        val isWithinBounds = offset.x <= textLayoutResult.getLineRight(line)

                                                                                        if (isWithinBounds) {
                                                                                            val annotation = annotated.getLinkAnnotations(position, position)
                                                                                                .firstOrNull()
                                                                                            if (annotation != null && annotation.item is LinkAnnotation.Url) {
                                                                                                uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                                            } else {
                                                                                                if (canEdit) focusRequester.requestFocus()
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                        },
                                                                    onTextLayout = { layoutResult = it }
                                                                )
                                                            }
                                                            is DynamicContentBlock.Divider -> {
                                                                HorizontalDivider(
                                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                                    thickness = 1.dp,
                                                                    color = onSurface.copy(alpha = 0.1f)
                                                                )
                                                            }
                                                            is DynamicContentBlock.Spoiler -> {
                                                                val annotated = remember(block.content, onSurface) {
                                                                    MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                }
                                                                SpoilerComponent(content = annotated)
                                                            }
                                                            is DynamicContentBlock.Resource -> {
                                                                ResourceBlock(
                                                                    resource = block,
                                                                    statsMap = statsMap,
                                                                    onUpdate = { updatedResource ->
                                                                        val newBlocks = blocks.toMutableList()
                                                                        val blockIndex = newBlocks.indexOf(block)
                                                                        if (blockIndex != -1) {
                                                                            newBlocks[blockIndex] = updatedResource
                                                                            val newContent = DynamicContentParser.render(newBlocks)
                                                                            onFieldChange(field.copy(content = newContent))
                                                                        }
                                                                    },
                                                                    hazeState = hazeState,
                                                                    forceBlurEnabled = forceBlurEnabled,
                                                                    blurPopups = blurPopups,
                                                                    settingsViewModel = settingsViewModel,
                                                                    onDeleteRequest = {
                                                                        val newBlocks = blocks.toMutableList()
                                                                        val blockIndex = newBlocks.indexOf(block)
                                                                        if (blockIndex != -1) {
                                                                            newBlocks.removeAt(blockIndex)
                                                                            val newContent = DynamicContentParser.render(newBlocks)
                                                                            onFieldChange(field.copy(content = newContent))
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }

                                                if (field.content.isEmpty()) {
                                                    Text(
                                                        contentPlaceholder,
                                                        fontSize = 17.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    )
                                }

                            Row(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canEdit) {
                                    IconButton(
                                        onClick = { 
                                            if (isFocused) focusManager.clearFocus() 
                                            else focusRequester.requestFocus()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFocused) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = "Edit/Save",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { onFullscreenRequest() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Fullscreen",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFieldFullscreenDialog(
    field: DynamicNoteState,
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap()
) {
    var title by remember { mutableStateOf(field.title) }
    var contentValue by remember { mutableStateOf(TextFieldValue(field.content)) }
    
    var isLocked by remember { mutableStateOf(field.isLocked) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(true) }
    var showLinkDialog by remember { mutableStateOf(false) }

    if (showLinkDialog) {
        val selection = contentValue.selection
        val selectedText = contentValue.text.substring(selection.start, selection.end)
        HyperlinkDialog(
            initialText = selectedText,
            initialUrl = "",
            onConfirm = { text, url ->
                val prefix = "["
                val middle = "]("
                val suffix = ")"
                val newText = contentValue.text.substring(0, selection.start) + prefix + text + middle + url + suffix + contentValue.text.substring(selection.end)
                val newSelection = TextRange(selection.start + prefix.length + text.length + middle.length + url.length + suffix.length)
                contentValue = contentValue.copy(text = newText, selection = newSelection)
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var isFocused by remember { mutableStateOf(false) }

        // Debounced formatting state for fullscreen
        var toolbarState by remember { mutableStateOf(Triple(contentValue, false, false)) }
        LaunchedEffect(contentValue, isFocused, isPreviewMode) {
            if (!isFocused || isPreviewMode) {
                toolbarState = Triple(contentValue, false, false)
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(200)
            toolbarState = Triple(contentValue, true, contentValue.selection.length > 0)
        }

        LaunchedEffect(contentValue.selection, contentValue.text, isFocused) {
            val layoutResult = textLayoutResult
            if (isFocused && layoutResult != null && contentValue.selection.collapsed && !isPreviewMode) {
                val cursorRect = layoutResult.getCursorRect(contentValue.selection.start)
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView(
                        cursorRect.copy(
                            top = cursorRect.top - 60f,
                            bottom = cursorRect.bottom + 60f
                        )
                    )
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(titlePlaceholder, color = colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                }
                                innerTextField()
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            focusManager.clearFocus()
                            onDismiss() 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            if (isPreviewMode) {
                                isPreviewMode = false
                                // Request focus on next frame
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    focusRequester.requestFocus()
                                }
                            } else {
                                isPreviewMode = true
                            }
                        }) {
                            Icon(
                                if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = "Toggle Preview"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier
                .imePadding()
                .run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                        inputScale = HazeInputScale.Fixed(0.7f)
                    }
                } else this
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            val onSurface = colorScheme.onSurface
                            val uriHandler = LocalUriHandler.current
                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            
                            val blocks = remember(contentValue.text) {
                                DynamicContentParser.parse(contentValue.text)
                            }

                            if (isPreviewMode) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    blocks.forEach { block ->
                                        when (block) {
                                            is DynamicContentBlock.Text -> {
                                                val annotated = remember(block.content, onSurface) {
                                                    MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                }
                                                Text(
                                                    text = annotated,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .pointerInput(annotated) {
                                                            detectTapGestures { offset ->
                                                                layoutResult?.let { textLayoutResult ->
                                                                    val position = textLayoutResult.getOffsetForPosition(offset)
                                                                    annotated.getLinkAnnotations(position, position)
                                                                        .firstOrNull()?.let { annotation ->
                                                                            if (annotation.item is LinkAnnotation.Url) {
                                                                                uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                            }
                                                                        }
                                                                }
                                                            }
                                                        },
                                                    fontSize = 18.sp,
                                                    lineHeight = 26.sp,
                                                    color = colorScheme.onSurface,
                                                    onTextLayout = { layoutResult = it }
                                                )
                                            }
                                            is DynamicContentBlock.Divider -> {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    thickness = 1.dp,
                                                    color = onSurface.copy(alpha = 0.1f)
                                                )
                                            }
                                            is DynamicContentBlock.Spoiler -> {
                                                val annotated = remember(block.content, onSurface) {
                                                    MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                }
                                                SpoilerComponent(content = annotated)
                                            }
                                            is DynamicContentBlock.Resource -> {
                                                ResourceBlock(
                                                    resource = block,
                                                    statsMap = statsMap,
                                                    onUpdate = { updatedResource ->
                                                        val newBlocks = blocks.toMutableList()
                                                        val blockIndex = newBlocks.indexOf(block)
                                                        if (blockIndex != -1) {
                                                            newBlocks[blockIndex] = updatedResource
                                                            val newContent = DynamicContentParser.render(newBlocks)
                                                            contentValue = contentValue.copy(text = newContent)
                                                        }
                                                    },
                                                    hazeState = hazeState,
                                                    forceBlurEnabled = blurPopups,
                                                    settingsViewModel = settingsViewModel,
                                                    onDeleteRequest = {
                                                        val newBlocks = blocks.toMutableList()
                                                        val blockIndex = newBlocks.indexOf(block)
                                                        if (blockIndex != -1) {
                                                            newBlocks.removeAt(blockIndex)
                                                            val newContent = DynamicContentParser.render(newBlocks)
                                                            contentValue = contentValue.copy(text = newContent)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                FormattingToolbar(
                                    value = toolbarState.first,
                                    onValueChange = { 
                                        contentValue = it
                                    },
                                    isFocused = toolbarState.second,
                                    isSelectionActive = toolbarState.third,
                                    onLinkRequest = { showLinkDialog = true },
                                    onSave = {
                                        focusManager.clearFocus()
                                        isPreviewMode = true
                                        onFieldChange(field.copy(title = title, content = contentValue.text, isLocked = isLocked))
                                    },
                                    hazeState = hazeState
                                )

                                key(field.id + "_fullscreen") {
                                    BasicTextField(
                                        value = contentValue,
                                        onValueChange = { 
                                            contentValue = it 
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { isFocused = it.isFocused },
                                        onTextLayout = { textLayoutResult = it },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 18.sp,
                                                lineHeight = 26.sp,
                                                color = if (toolbarState.second) colorScheme.onSurface else Color.Transparent
                                            ),
                                        cursorBrush = SolidColor(colorScheme.primary),
                                        decorationBox = { innerTextField ->
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                val onSurface = colorScheme.onSurface
                                                val uriHandler = LocalUriHandler.current
                                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                                val blocks = remember(contentValue.text) {
                                                    DynamicContentParser.parse(contentValue.text)
                                                }

                                                val marginStep by settingsViewModel?.topMarginStep?.collectAsState() ?: remember { mutableStateOf(2) }
                                                val customMargin by settingsViewModel?.customTopMargin?.collectAsState() ?: remember { mutableStateOf(96) }
                                                val topMargin = if (marginStep == 5) customMargin.dp else (marginStep * 48).dp

                                                if (isFocused) {
                                                    Column {
                                                        if (toolbarState.second) {
                                                            Spacer(modifier = Modifier.height(topMargin))
                                                        }
                                                        innerTextField()
                                                        Spacer(modifier = Modifier.height(32.dp))
                                                    }
                                                } else {
                                                    // Raw text field must still be present for focus/IME, but we hide it and remove from layout height
                                                    Box(
                                                        Modifier
                                                            .alpha(0f)
                                                            .layout { measurable, constraints ->
                                                                val placeable = measurable.measure(constraints)
                                                                layout(placeable.width, 0) {
                                                                    placeable.place(0, 0)
                                                                }
                                                            }
                                                    ) {
                                                        innerTextField()
                                                    }

                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                    ) {
                                                        blocks.forEach { block ->
                                                            when (block) {
                                                                is DynamicContentBlock.Text -> {
                                                                    val annotated = remember(block.content, onSurface) {
                                                                        MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                    }
                                                                    Text(
                                                                        text = annotated,
                                                                        fontSize = 18.sp,
                                                                        lineHeight = 26.sp,
                                                                        color = colorScheme.onSurface,
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .pointerInput(annotated) {
                                                                                detectTapGestures(
                                                                                    onTap = { offset ->
                                                                                        layoutResult?.let { textLayoutResult ->
                                                                                            val position = textLayoutResult.getOffsetForPosition(offset)
                                                                                            val line = textLayoutResult.getLineForOffset(position)
                                                                                            val isWithinBounds = offset.x <= textLayoutResult.getLineRight(line)

                                                                                            if (isWithinBounds) {
                                                                                                val annotation = annotated.getLinkAnnotations(position, position)
                                                                                                    .firstOrNull()
                                                                                                if (annotation != null && annotation.item is LinkAnnotation.Url) {
                                                                                                    uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                                                } else {
                                                                                                    focusRequester.requestFocus()
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                )
                                                                            },
                                                                        onTextLayout = { layoutResult = it }
                                                                    )
                                                                }
                                                                is DynamicContentBlock.Divider -> {
                                                                    HorizontalDivider(
                                                                        modifier = Modifier.padding(vertical = 12.dp),
                                                                        thickness = 1.dp,
                                                                        color = onSurface.copy(alpha = 0.1f)
                                                                    )
                                                                }
                                                                is DynamicContentBlock.Spoiler -> {
                                                                    val annotated = remember(block.content, onSurface) {
                                                                        MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                    }
                                                                    SpoilerComponent(content = annotated)
                                                                }
                                                                is DynamicContentBlock.Resource -> {
                                                                    ResourceBlock(
                                                                        resource = block,
                                                                        statsMap = statsMap,
                                                                        onUpdate = { updatedResource ->
                                                                            val newBlocks = blocks.toMutableList()
                                                                            val blockIndex = newBlocks.indexOf(block)
                                                                            if (blockIndex != -1) {
                                                                                newBlocks[blockIndex] = updatedResource
                                                                                val newContent = DynamicContentParser.render(newBlocks)
                                                                                contentValue = contentValue.copy(text = newContent)
                                                                            }
                                                                        },
                                                                        hazeState = hazeState,
                                                                        forceBlurEnabled = blurPopups,
                                                                        settingsViewModel = settingsViewModel,
                                                                        onDeleteRequest = {
                                                                            val newBlocks = blocks.toMutableList()
                                                                            val blockIndex = newBlocks.indexOf(block)
                                                                            if (blockIndex != -1) {
                                                                                newBlocks.removeAt(blockIndex)
                                                                                val newContent = DynamicContentParser.render(newBlocks)
                                                                                contentValue = contentValue.copy(text = newContent)
                                                                            }
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(80.dp))
                                                    }

                                                    if (contentValue.text.isEmpty()) {
                                                        Text(contentPlaceholder, fontSize = 18.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isLocked) colorScheme.primary else colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isLocked) colorScheme.primary else colorScheme.outline),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLocked) "Заблокировано" else "Разблокировано",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onFieldChange(field.copy(title = title, content = contentValue.text, isLocked = isLocked))
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    DeleteConfirmationDialog(
        showDialog = showDeleteConfirm,
        onDismiss = { showDeleteConfirm = false },
        onConfirm = {
            onDelete()
            showDeleteConfirm = false
        },
        settingsViewModel = settingsViewModel
    )
}
