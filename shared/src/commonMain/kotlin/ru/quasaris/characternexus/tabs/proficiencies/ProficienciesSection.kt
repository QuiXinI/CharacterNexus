package ru.quasaris.characternexus.tabs.proficiencies

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.reorder.reorderableItem
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.CharacterDetailState
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class ProficiencyDragItem(
    val sectionIndex: Int,
    val item: ProficiencyItem
)

@Composable
fun ProficienciesSection(
    state: CharacterDetailState,
    isExpanded: Boolean
) {
    if (!isExpanded) return

    val dndState = rememberDragAndDropState<ProficiencyDragItem>(
        dragAfterLongPress = true
    )
    var isProficienciesEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Edit Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isProficienciesEditMode = !isProficienciesEditMode },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isProficienciesEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isProficienciesEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        DragAndDropContainer(
            state = dndState,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Общий LookaheadScope на все секции — именно он определяет
            // "систему координат", в которой animateBounds отслеживает сдвиги
            // чипов и плавно их анимирует вместо мгновенного прыжка.
            LookaheadScope {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.proficiencies.sections.forEachIndexed { sectionIndex, section ->
                        ProficiencySectionRow(
                            section = section,
                            sectionIndex = sectionIndex,
                            dndState = dndState,
                            lookaheadScope = this@LookaheadScope,
                            isEditMode = isProficienciesEditMode,
                            onSectionChange = { updated ->
                                val newSections = state.proficiencies.sections.toMutableList()
                                newSections[sectionIndex] = updated
                                state.proficiencies = state.proficiencies.copy(sections = newSections)
                            },
                            onDeleteSection = {
                                val newSections = state.proficiencies.sections.toMutableList()
                                newSections.removeAt(sectionIndex)
                                state.proficiencies = state.proficiencies.copy(sections = newSections)
                            },
                            onMoveItem = { draggedData, targetSecIdx, targetItemIdx ->
                                val itemToMove = draggedData.item
                                val currentSections = state.proficiencies.sections

                                var sourceSecIdx = -1
                                var sourceItemIdx = -1
                                for (i in currentSections.indices) {
                                    val idx = currentSections[i].items.indexOfFirst { it.id == itemToMove.id }
                                    if (idx != -1) {
                                        sourceSecIdx = i
                                        sourceItemIdx = idx
                                        break
                                    }
                                }

                                if (sourceSecIdx != -1) {
                                    if (!(sourceSecIdx == targetSecIdx && sourceItemIdx == targetItemIdx)) {
                                        val newSections = currentSections.map { s ->
                                            s.copy(items = s.items.toMutableList())
                                        }.toMutableList()

                                        (newSections[sourceSecIdx].items as MutableList<ProficiencyItem>).removeAt(sourceItemIdx)

                                        val targetList = newSections[targetSecIdx].items as MutableList<ProficiencyItem>
                                        val finalIndex = if (targetItemIdx == -1) {
                                            targetList.size
                                        } else {
                                            targetItemIdx.coerceAtMost(targetList.size)
                                        }
                                        targetList.add(finalIndex, itemToMove)

                                        state.proficiencies = state.proficiencies.copy(sections = newSections)
                                    }
                                }
                            }
                        )
                    }

                    // Add Section - Always visible
                    KeepStyleInput(
                        placeholder = "Добавить свой раздел",
                        onSave = { title ->
                            if (title.isNotBlank()) {
                                state.proficiencies = state.proficiencies.copy(
                                    sections = state.proficiencies.sections + ProficiencySection(title = title)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProficiencySectionRow(
    section: ProficiencySection,
    sectionIndex: Int,
    dndState: DragAndDropState<ProficiencyDragItem>,
    lookaheadScope: LookaheadScope,
    isEditMode: Boolean,
    onSectionChange: (ProficiencySection) -> Unit,
    onDeleteSection: () -> Unit,
    onMoveItem: (ProficiencyDragItem, Int, Int) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .dropTarget(
                key = section.id,
                state = dndState,
                canDrop = section.items.isEmpty(),
                onDragEnter = { draggedItem ->
                    onMoveItem(draggedItem.data, sectionIndex, -1)
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        // Title Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(top = 7.dp, bottom = 7.dp, end = 4.dp)
        ) {
            EditableText(
                text = section.title,
                isEditMode = isEditMode,
                onSave = { onSectionChange(section.copy(title = it)) },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (isEditMode) {
                IconButton(onClick = onDeleteSection, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // Chips
        section.items.forEachIndexed { itemIndex, item ->
            val isActualDragging = dndState.draggedItem?.key == item.id

            // key(item.id) — стабильная идентичность чипа в composition.
            // Без него Compose сопоставляет чипы по позиции в forEachIndexed,
            // а не по item.id: при reorder элемент на позиции N молча
            // подменяется данными другого элемента вместо того, чтобы
            // считаться "тем же самым, но подвинувшимся" — из-за этого
            // и внутреннее состояние чипов может путаться, и animateBounds
            // не понимает, что именно куда переместилось.
            key(item.id) {
                ProficiencyChip(
                    item = item,
                    sectionIndex = sectionIndex,
                    itemIndex = itemIndex,
                    dndState = dndState,
                    lookaheadScope = lookaheadScope,
                    isEditMode = isEditMode,
                    isActualDragging = isActualDragging,
                    onToggle = {
                        val newItems = section.items.toMutableList()
                        newItems[itemIndex] = item.copy(isActive = !item.isActive)
                        onSectionChange(section.copy(items = newItems))
                    },
                    onNameChange = { newName ->
                        val newItems = section.items.toMutableList()
                        newItems[itemIndex] = item.copy(name = newName)
                        onSectionChange(section.copy(items = newItems))
                    },
                    onDelete = {
                        val newItems = section.items.toMutableList()
                        newItems.removeAt(itemIndex)
                        onSectionChange(section.copy(items = newItems))
                    },
                    onDragEnter = { draggedData ->
                        if (draggedData.item.id != item.id) {
                            onMoveItem(draggedData, sectionIndex, itemIndex)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        // Add Item button - Always visible
        KeepStyleInput(
            placeholder = "Добавить",
            onSave = { name ->
                if (name.isNotBlank()) {
                    onSectionChange(section.copy(items = section.items + ProficiencyItem(name = name)))
                }
            },
            isChip = true,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProficiencyChip(
    item: ProficiencyItem,
    sectionIndex: Int,
    itemIndex: Int,
    dndState: DragAndDropState<ProficiencyDragItem>? = null,
    lookaheadScope: LookaheadScope? = null,
    isEditMode: Boolean = false,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDragEnter: (ProficiencyDragItem) -> Unit = {},
    isDragging: Boolean = false,
    isActualDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = if (item.isActive) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (item.isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val glowColor = if (item.isActive) accentColor else Color.Gray.copy(alpha = 0.5f)

    var chipModifier = if (dndState != null && !isDragging) {
        var m = Modifier as Modifier
        if (lookaheadScope != null) {
            // animateBounds должен стоять ДО reorderableItem в цепочке —
            // так модификатор видит и анимирует именно ту позицию, в которую
            // reorderableItem перекладывает чип при reorder, а не наоборот.
            m = m.animateBounds(lookaheadScope)
        }
        m
            // reorderableItem сам совмещает draggableItem + dropTarget в одном
            // колбэке onDragEnter — то, что раньше делали два отдельных
            // модификатора (и конфликтовали друг с другом при переносе между
            // секциями/внутри секции).
            .reorderableItem(
                key = item.id,
                data = ProficiencyDragItem(sectionIndex, item),
                state = dndState,
                onDragEnter = { draggedItem ->
                    onDragEnter(draggedItem.data)
                },
                draggableContent = {
                    ProficiencyChip(
                        item = item,
                        sectionIndex = sectionIndex,
                        itemIndex = itemIndex,
                        onToggle = {},
                        onNameChange = {},
                        onDelete = {},
                        isDragging = true
                    )
                }
            )
    } else Modifier

    if (isDragging) {
        chipModifier = chipModifier.graphicsLayer { alpha = 0.7f; scaleX = 1.1f; scaleY = 1.1f }
    } else {
        chipModifier = chipModifier
            .graphicsLayer { alpha = if (isActualDragging) 0f else 1f }
            .clickable { onToggle() }
    }

    Surface(
        modifier = modifier.then(chipModifier),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, if (item.isActive) accentColor.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Glow circle
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(glowColor, CircleShape)
            )

            EditableText(
                text = item.name,
                isEditMode = isEditMode,
                onSave = onNameChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                ),
                isSimple = true
            )

            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDelete() },
                    tint = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EditableText(
    text: String,
    isEditMode: Boolean,
    onSave: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    isSimple: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    // Флаг, позволяющий игнорировать расфокус в первые мгновения после открытия
    var focusReady by remember { mutableStateOf(false) }

    if (isEditing) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = textStyle,
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (focusReady && !it.isFocused && isEditing) {
                        if (value.isNotBlank()) {
                            isEditing = false
                            onSave(value)
                        }
                    }
                }
                .onKeyEvent {
                    if ((it.key == Key.Escape || it.key == Key.Enter) && it.type == KeyEventType.KeyUp) {
                        isEditing = false
                        onSave(value)
                        true
                    } else false
                },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                isEditing = false
                onSave(value)
            })
        )
        LaunchedEffect(isEditing) {
            if (isEditing) {
                focusReady = false
                repeat(3) {
                    focusRequester.requestFocus()
                    kotlinx.coroutines.delay(50.milliseconds)
                }
                focusReady = true
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier.run {
                if (isEditMode) clickable { isEditing = true; value = text } else this
            }
        ) {
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier
                        .size(if (isSimple) 14.dp else 16.dp)
                        .padding(end = 2.dp),
                    tint = textStyle.color.copy(alpha = 0.5f)
                )
            }
            Text(
                text = text,
                style = textStyle
            )
        }
    }
}

@Composable
fun KeepStyleInput(
    placeholder: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    isChip: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var focusReady by remember { mutableStateOf(false) }

    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val inputTextStyle = if (isChip) {
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    } else {
        textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    }

    if (isEditing) {
        val inputModifier = if (isChip) {
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        } else {
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        }

        Row(
            modifier = modifier.then(inputModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = inputTextStyle,
                modifier = Modifier
                    .weight(1f, fill = !isChip)
                    .widthIn(min = 60.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (focusReady && !it.isFocused && isEditing) {
                            if (value.isNotBlank()) {
                                isEditing = false
                                onSave(value)
                                value = ""
                            }
                        }
                    }
                    .onKeyEvent {
                        if ((it.key == Key.Escape || it.key == Key.Enter) && it.type == KeyEventType.KeyUp) {
                            isEditing = false
                            if (value.isNotBlank()) onSave(value)
                            value = ""
                            true
                        } else false
                    },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    isEditing = false
                    if (value.isNotBlank()) onSave(value)
                    value = ""
                })
            )

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        isEditing = false
                        if (value.isNotBlank()) onSave(value)
                        value = ""
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        LaunchedEffect(isEditing) {
            if (isEditing) {
                focusReady = false
                repeat(3) {
                    focusRequester.requestFocus()
                    kotlinx.coroutines.delay(50.milliseconds)
                }
                focusReady = true
            }
        }
    } else {
        Surface(
            onClick = { isEditing = true },
            shape = RoundedCornerShape(16.dp),
            color = if (isChip) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isChip) 8.dp else 12.dp, 
                    vertical = if (isChip) 6.dp else 8.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null, 
                    modifier = Modifier.size(if (isChip) 16.dp else 18.dp),
                    tint = if (isChip) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                Text(
                    placeholder, 
                    fontSize = if (isChip) 13.sp else 14.sp,
                    fontWeight = if (isChip) FontWeight.Medium else FontWeight.Bold,
                    color = if (isChip) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
