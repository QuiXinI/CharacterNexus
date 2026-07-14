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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun DynamicFieldsTab(
    fields: List<DynamicNoteState>,
    onFieldsChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    isEditMode: Boolean = false,
    addButtonText: String = "ДОБАВИТЬ ПОЛЕ",
    emptyListText: String = "Список пуст",
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    settingsViewModel: SettingsViewModel? = null,
    isCollapsible: Boolean = true,
    isTitleReadOnly: Boolean = false,
    isAddButtonVisible: Boolean = true,
    isReorderButtonVisible: Boolean = true,
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    val items = remember(fields) { mutableStateListOf<DynamicNoteState>().apply { addAll(fields) } }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    
    var fullscreenFieldIndex by remember { mutableStateOf<Int?>(null) }
    var fieldToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .imePadding()
    ) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyListText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    isReorderButtonVisible = isReorderButtonVisible
                )
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
                settingsViewModel = settingsViewModel
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
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val isExpanded = if (isCollapsible) field.isExpanded else true
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 0f else 180f)
    val scale by animateFloatAsState(targetValue = if (isEditMode) 0.95f else 1f)
    val padding by animateDpAsState(targetValue = if (isEditMode) 8.dp else 0.dp)

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
                    .run {
                        if (isEditMode || !isCollapsible) this else this.clickable { onFieldChange(field.copy(isExpanded = !isExpanded)) }
                    }
                    .padding(vertical = if (isEditMode) 12.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = field.title,
                        onValueChange = { onFieldChange(field.copy(title = it)) },
                        enabled = !isEditMode && !isTitleReadOnly,
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
                                .padding(12.dp)
                        ) {
                            var isFocused by remember { mutableStateOf(false) }

                            BasicTextField(
                                value = field.content,
                                onValueChange = { onFieldChange(field.copy(content = it)) },
                                enabled = !isEditMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isFocused = it.isFocused },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp,
                                    color = if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (!isFocused) {
                                            val annotatedContent = remember(field.content) { MarkdownHelper.parseMarkdown(field.content) }
                                            Text(
                                                text = annotatedContent,
                                                fontSize = 17.sp,
                                                lineHeight = 24.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        
                                        if (field.content.isEmpty() && !isFocused) {
                                            Text(
                                                contentPlaceholder,
                                                fontSize = 17.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                        Column {
                                            innerTextField()
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            )

                            IconButton(
                                onClick = { onFullscreenRequest() },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
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
    settingsViewModel: SettingsViewModel? = null
) {
    var title by remember { mutableStateOf(field.title) }
    var content by remember { mutableStateOf(field.content) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

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
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
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
            modifier = Modifier.run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
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
                            .fillMaxWidth()
                    ) {
                        if (isPreviewMode) {
                            val annotated = remember(content) { MarkdownHelper.parseMarkdown(content) }
                            Text(
                                text = annotated,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                color = colorScheme.onSurface
                            )
                        } else {
                            BasicTextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (content.isEmpty()) {
                                        Text(contentPlaceholder, fontSize = 18.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

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

                    Spacer(modifier = Modifier.height(80.dp))
                }

                Button(
                    onClick = {
                        onFieldChange(field.copy(title = title, content = content))
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
