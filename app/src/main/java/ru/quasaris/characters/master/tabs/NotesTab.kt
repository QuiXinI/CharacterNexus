package ru.quasaris.characters.master.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characters.master.DynamicNoteState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale

@Composable
fun NotesTab(
    notes: List<DynamicNoteState>,
    onNotesChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    
    var fullscreenNoteIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                val isDragging = draggedItemIndex == index
                
                NoteItem(
                    note = note,
                    onNoteChange = { updatedNote ->
                        val newList = notes.toMutableList()
                        newList[index] = updatedNote
                        onNotesChange(newList)
                    },
                    onFullscreenRequest = { fullscreenNoteIndex = index },
                    modifier = Modifier
                        .shadow(if (isDragging) 8.dp else 0.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    draggedItemIndex = index
                                    draggingOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggingOffset += dragAmount.y
                                    
                                    val threshold = 100f
                                    if (kotlin.math.abs(draggingOffset) > threshold) {
                                        val direction = if (draggingOffset > 0) 1 else -1
                                        val targetIndex = (index + direction).coerceIn(0, notes.size - 1)
                                        
                                        if (targetIndex != index) {
                                            val newList = notes.toMutableList()
                                            val item = newList.removeAt(index)
                                            newList.add(targetIndex, item)
                                            onNotesChange(newList)
                                            draggedItemIndex = targetIndex
                                            draggingOffset = 0f
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
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onNotesChange(notes + DynamicNoteState())
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
                    Text("ДОБАВИТЬ ЗАМЕТКУ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    fullscreenNoteIndex?.let { index ->
        if (index in notes.indices) {
            val note = notes[index]
            NoteFullscreenDialog(
                note = note,
                onNoteChange = { updatedNote ->
                    val newList = notes.toMutableList()
                    newList[index] = updatedNote
                    onNotesChange(newList)
                },
                onDelete = {
                    val newList = notes.toMutableList()
                    newList.removeAt(index)
                    onNotesChange(newList)
                    fullscreenNoteIndex = null
                },
                onDismiss = { fullscreenNoteIndex = null },
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled
            )
        }
    }
}

@Composable
fun NoteItem(
    note: DynamicNoteState,
    onNoteChange: (DynamicNoteState) -> Unit,
    onFullscreenRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (note.isExpanded) 0f else 180f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (note.isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                else Color.Transparent,
        shadowElevation = if (note.isExpanded) 1.dp else 0.dp,
        border = if (note.isExpanded) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                 else null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = note.title,
                    onValueChange = { onNoteChange(note.copy(title = it)) },
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
                        if (note.title.isEmpty()) {
                            Text(
                                "Заголовок",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                // Larger hitbox for expand/collapse
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNoteChange(note.copy(isExpanded = !note.isExpanded)) },
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
            }

            AnimatedVisibility(visible = note.isExpanded) {
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
                        value = note.content,
                        onValueChange = { onNoteChange(note.copy(content = it)) },
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
                                    val annotatedContent = remember(note.content) { MarkdownHelper.parseMarkdown(note.content) }
                                    Text(
                                        text = annotatedContent,
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                if (note.content.isEmpty() && !isFocused) {
                                    Text(
                                        "Текст заметки...",
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Column {
                                    innerTextField()
                                    Spacer(modifier = Modifier.height(16.dp)) // Small gap so text doesn't hit the bottom edge
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFullscreenDialog(
    note: DynamicNoteState,
    onNoteChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
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
                                    Text("Заголовок", color = colorScheme.onSurface.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
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
                                        Text("Начните писать...", fontSize = 18.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить заметку", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // Extra padding to scroll above the fixed Save button
                }

                // Fixed Save Button at the bottom
                Button(
                    onClick = {
                        onNoteChange(note.copy(title = title, content = content))
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Это действие нельзя будет отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("УДАЛИТЬ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }
}
