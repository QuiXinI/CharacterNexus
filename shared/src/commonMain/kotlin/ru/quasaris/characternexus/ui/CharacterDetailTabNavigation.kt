package ru.quasaris.characternexus.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import sh.calvin.reorderable.*
import ru.quasaris.characternexus.model.*

/**
 * Стиль размытия для нижней панели выбора вкладок.
 */
val TabSheetHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.25f)))
)

fun displayTabIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "person" -> Icons.Default.Person
        "gavel" -> Icons.Default.Gavel
        "star" -> Icons.Default.Star
        "inventory" -> Icons.Default.Inventory
        "autofixhigh" -> Icons.Default.AutoFixHigh
        "book" -> Icons.Default.Book
        "note" -> Icons.AutoMirrored.Filled.Note
        "bookmark" -> Icons.Default.Bookmark
        "folder" -> Icons.Default.Folder
        "scroll", "description" -> Icons.Default.Description
        "shield" -> Icons.Default.Shield
        "backpack" -> Icons.Default.Backpack
        "map" -> Icons.Default.Map
        "pets" -> Icons.Default.Pets
        "palette", "brush" -> Icons.Default.Palette
        else -> Icons.Default.Bookmark
    }
}

@Composable
fun IconPickerDialog(
    targetTab: DisplayTab?,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
    if (targetTab == null) return
    val colorScheme = MaterialTheme.colorScheme
    val iconsList = listOf(
        "Person" to "Персонаж",
        "Gavel" to "Атаки",
        "Star" to "Умения",
        "Inventory" to "Инвентарь",
        "AutoFixHigh" to "Заклинания",
        "Book" to "Книга",
        "Note" to "Заметка",
        "Bookmark" to "Закладка",
        "Folder" to "Папка",
        "Description" to "Свиток",
        "Shield" to "Щит",
        "Backpack" to "Рюкзак",
        "Map" to "Карта",
        "Pets" to "Питомец",
        "Palette" to "Палитра"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите иконку для «${targetTab.title}»") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                items(iconsList) { (iconName, label) ->
                    val isSelected = targetTab.iconName.equals(iconName, ignoreCase = true)
                    Surface(
                        onClick = {
                            onSelectIcon(iconName)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(displayTabIcon(iconName), contentDescription = label, tint = if (isSelected) colorScheme.primary else colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp, color = colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditableTabTitle(
    text: String,
    isEditMode: Boolean,
    onSave: (String) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember(text) { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    var focusReady by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    if (isEditing && isEditMode) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = textStyle.copy(color = colorScheme.onSurface),
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
            cursorBrush = SolidColor(colorScheme.primary),
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
                if (isEditMode) clickable {
                    focusReady = false
                    isEditing = true
                    value = text
                } else this
            }
        ) {
            Text(
                text = text,
                style = textStyle,
                maxLines = 1,
                softWrap = false
            )
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    modifier = Modifier.size(14.dp),
                    tint = LocalContentColor.current
                )
            }
        }
    }
}

@Composable
fun TabInlineAddButton(
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var focusReady by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    if (isEditing) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
            modifier = modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 160.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusReady && !focusState.isFocused && isEditing) {
                                if (value.isNotBlank()) {
                                    onSave(value)
                                }
                                isEditing = false
                                value = ""
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if ((keyEvent.key == Key.Escape || keyEvent.key == Key.Enter) && keyEvent.type == KeyEventType.KeyUp) {
                                if (value.isNotBlank()) {
                                    onSave(value)
                                }
                                isEditing = false
                                value = ""
                                true
                            } else false
                        },
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (value.isNotBlank()) {
                            onSave(value)
                        }
                        isEditing = false
                        value = ""
                    })
                )

                IconButton(
                    onClick = {
                        if (value.isNotBlank()) {
                            onSave(value)
                        }
                        isEditing = false
                        value = ""
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Сохранить",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f)),
            modifier = modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить вкладку",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Вкладка",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DesktopTabNavigationBar(
    currentTab: DisplayTab,
    tabs: List<DisplayTab>,
    onTabSelected: (DisplayTab) -> Unit,
    state: CharacterDetailState,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // to/from могут указывать на не-вкладку (например, кнопку «+») — не падаем и не двигаем.
        if (from.index in tabs.indices && to.index in tabs.indices) {
            val newList = tabs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            state.reorderTabs(newList)
        }
    }
    var tabForIconPicker by remember { mutableStateOf<DisplayTab?>(null) }
    var tabToDelete by remember { mutableStateOf<DisplayTab?>(null) }

    // Вне режима редактирования отключённые вкладки не показываем: в pager их нет,
    // и клик по ним раньше выкидывал выбор на первую вкладку.
    val visibleTabs = remember(tabs, state.isTabEditMode) {
        if (state.isTabEditMode) tabs else tabs.filter { !it.isDisabled }.ifEmpty { tabs }
    }

    // Держим выбранную вкладку в зоне видимости селектора.
    LaunchedEffect(currentTab.key, state.isTabEditMode, visibleTabs.size) {
        if (!state.isTabEditMode) {
            val idx = visibleTabs.indexOfFirst { it.key == currentTab.key }
            if (idx >= 0) {
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.index == idx }
                val fullyVisible = item != null &&
                        item.offset >= info.viewportStartOffset &&
                        item.offset + item.size <= info.viewportEndOffset
                if (!fullyVisible) listState.animateScrollToItem(idx)
            }
        }
    }

    Surface(
        color = colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(visibleTabs, key = { _, item -> item.key }) { _, tab ->
                        ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                            val isSelected = tab.key == currentTab.key
                            val textColor by animateColorAsState(
                                if (tab.isDisabled) colorScheme.onSurface.copy(alpha = 0.38f)
                                else if (isSelected) colorScheme.primary
                                else colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            val iconColor by animateColorAsState(
                                if (tab.isDisabled) colorScheme.onSurface.copy(alpha = 0.38f)
                                else if (isSelected) colorScheme.primary
                                else colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Surface(
                                onClick = {
                                    if (!state.isTabEditMode) onTabSelected(tab)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDragging) colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else if (tab.isDisabled) colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                else if (isSelected) colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                                contentColor = textColor,
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (state.isTabEditMode) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Перетащить",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.draggableHandle().size(18.dp)
                                        )
                                    }

                                    if (state.isTabEditMode) {
                                        IconButton(
                                            onClick = { tabForIconPicker = tab },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = displayTabIcon(tab.iconName),
                                                contentDescription = "Сменить иконку",
                                                modifier = Modifier.size(18.dp),
                                                tint = iconColor
                                            )
                                        }
                                    } else {
                                        // Не IconButton: он гасил клик, и по иконке вкладка не выбиралась.
                                        Box(
                                            modifier = Modifier.size(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = displayTabIcon(tab.iconName),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = iconColor
                                            )
                                        }
                                    }

                                    EditableTabTitle(
                                        text = tab.title,
                                        isEditMode = state.isTabEditMode,
                                        onSave = { newTitle -> state.updateTabTitle(tab, newTitle) },
                                        textStyle = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    )

                                    if (state.isTabEditMode) {
                                        IconButton(
                                            onClick = { state.toggleTabDisabled(tab) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        color = if (tab.isDisabled) colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else colorScheme.primary,
                                                        shape = RoundedCornerShape(50)
                                                    )
                                            )
                                        }

                                        if (tab.isCustom) {
                                            IconButton(
                                                onClick = { tabToDelete = tab },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Удалить вкладку",
                                                    tint = colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isTabEditMode) {
                        item {
                            TabInlineAddButton(
                                onSave = { newTitle ->
                                    // Не выбираем новую вкладку: в режиме редактирования pager её ещё не содержит,
                                    // и выбор «срабатывал» только при выходе из режима — вкладка внезапно прыгала.
                                    state.addCustomTab(newTitle)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colorScheme.onSurface.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .width(184.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                actions()
            }
        }
    }

    if (tabForIconPicker != null) {
        IconPickerDialog(
            targetTab = tabForIconPicker,
            onDismiss = { tabForIconPicker = null },
            onSelectIcon = { chosenIcon ->
                state.updateTabIcon(tabForIconPicker!!, chosenIcon)
            }
        )
    }

    if (tabToDelete != null) {
        AlertDialog(
            onDismissRequest = { tabToDelete = null },
            title = { Text("Удаление вкладки") },
            text = { Text("Вы точно хотите удалить вкладку «${tabToDelete?.title}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tabToDelete?.let { state.deleteCustomTab(it) }
                        tabToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tabToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun DesktopTabActions(
    tab: DisplayTab,
    state: CharacterDetailState,
    onShowSpellSettings: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { state.isTabEditMode = !state.isTabEditMode },
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Редактировать список вкладок",
                tint = if (state.isTabEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        val showEdit = when (tab) {
            is DisplayTab.BuiltIn -> tab.tab != CharacterTab.STATS
            is DisplayTab.Custom -> true
        }

        if (showEdit) {
            IconButton(
                onClick = { state.isEditMode = !state.isEditMode },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Редактировать содержимое вкладки",
                    tint = if (state.isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        val isSpells = tab is DisplayTab.BuiltIn && tab.tab == CharacterTab.SPELLS
        if (isSpells && onShowSpellSettings != null) {
            IconButton(
                onClick = onShowSpellSettings,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        val supportExpansion = when (tab) {
            is DisplayTab.BuiltIn -> tab.tab in listOf(
                CharacterTab.BIO, CharacterTab.SKILLS_FEATS,
                CharacterTab.INVENTORY, CharacterTab.SPELLS, CharacterTab.NOTES
            )
            is DisplayTab.Custom -> true
        }

        if (supportExpansion) {
            val anyCollapsed = when (tab) {
                is DisplayTab.BuiltIn -> when (tab.tab) {
                    CharacterTab.BIO -> state.bioLongSections.any { !it.isExpanded }
                    CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                    CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                    CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                    CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                    else -> false
                }
                is DisplayTab.Custom -> {
                    val content = state.customTabs.find { it.id == tab.customTab.id }?.content ?: tab.customTab.content
                    content.any { !it.isExpanded }
                }
            }

            IconButton(
                onClick = {
                    when (tab) {
                        is DisplayTab.BuiltIn -> {
                            when (tab.tab) {
                                CharacterTab.BIO -> state.bioLongSections = state.bioLongSections.map { it.copy(isExpanded = anyCollapsed) }
                                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = state.skillsAndTraits.map { it.copy(isExpanded = anyCollapsed) }
                                CharacterTab.INVENTORY -> state.inventory = state.inventory.map { it.copy(isExpanded = anyCollapsed) }
                                CharacterTab.SPELLS -> state.spells = state.spells.map { it.copy(isExpanded = anyCollapsed) }
                                CharacterTab.NOTES -> state.notes = state.notes.map { it.copy(isExpanded = anyCollapsed) }
                                else -> {}
                            }
                        }
                        is DisplayTab.Custom -> {
                            val content = state.customTabs.find { it.id == tab.customTab.id }?.content ?: tab.customTab.content
                            val newContent = content.map { it.copy(isExpanded = anyCollapsed) }
                            state.customTabs = state.customTabs.map {
                                if (it.id == tab.customTab.id) it.copy(content = newContent) else it
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TabNavigationBar(
    currentTab: DisplayTab,
    onShowTabSheet: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f))

        Surface(
            color = colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onShowTabSheet() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTab.title.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            actions()
        }
    }
}

@Composable
fun TabActions(
    tab: DisplayTab,
    state: CharacterDetailState,
    onShowSpellSettings: (() -> Unit)? = null,
    isDesktop: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        val isSpells = tab is DisplayTab.BuiltIn && tab.tab == CharacterTab.SPELLS
        if (isSpells && onShowSpellSettings != null) {
            IconButton(onClick = onShowSpellSettings) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        val isStats = tab is DisplayTab.BuiltIn && tab.tab == CharacterTab.STATS
        if (isStats && !isDesktop) {
            IconButton(onClick = { state.isAdvancedMode = !state.isAdvancedMode }) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Toggle Skills",
                    tint = colorScheme.primary
                )
            }
        }

        val supportExpansion = when (tab) {
            is DisplayTab.BuiltIn -> tab.tab in listOf(
                CharacterTab.BIO, CharacterTab.SKILLS_FEATS,
                CharacterTab.INVENTORY, CharacterTab.SPELLS, CharacterTab.NOTES
            )
            is DisplayTab.Custom -> true
        }

        if (supportExpansion) {
            val anyCollapsed = when (tab) {
                is DisplayTab.BuiltIn -> when (tab.tab) {
                    CharacterTab.BIO -> state.bioLongSections.any { !it.isExpanded }
                    CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                    CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                    CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                    CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                    else -> false
                }
                is DisplayTab.Custom -> {
                    val content = state.customTabs.find { it.id == tab.customTab.id }?.content ?: tab.customTab.content
                    content.any { !it.isExpanded }
                }
            }

            IconButton(onClick = {
                when (tab) {
                    is DisplayTab.BuiltIn -> {
                        when (tab.tab) {
                            CharacterTab.BIO -> state.bioLongSections = state.bioLongSections.map { it.copy(isExpanded = anyCollapsed) }
                            CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = state.skillsAndTraits.map { it.copy(isExpanded = anyCollapsed) }
                            CharacterTab.INVENTORY -> state.inventory = state.inventory.map { it.copy(isExpanded = anyCollapsed) }
                            CharacterTab.SPELLS -> state.spells = state.spells.map { it.copy(isExpanded = anyCollapsed) }
                            CharacterTab.NOTES -> state.notes = state.notes.map { it.copy(isExpanded = anyCollapsed) }
                            else -> {}
                        }
                    }
                    is DisplayTab.Custom -> {
                        val content = state.customTabs.find { it.id == tab.customTab.id }?.content ?: tab.customTab.content
                        val newContent = content.map { it.copy(isExpanded = anyCollapsed) }
                        state.customTabs = state.customTabs.map {
                            if (it.id == tab.customTab.id) it.copy(content = newContent) else it
                        }
                    }
                }
            }) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }

        val showEdit = when (tab) {
            is DisplayTab.BuiltIn -> tab.tab != CharacterTab.STATS
            is DisplayTab.Custom -> true
        }

        if (showEdit) {
            IconButton(onClick = { state.isEditMode = !state.isEditMode }) {
                Icon(
                    if (state.isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Toggle Edit Mode",
                    tint = if (state.isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TabControlHeader(
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    hasContentToEdit: Boolean = true,
    isCollapsible: Boolean = false,
    anyCollapsed: Boolean = false,
    onToggleAllExpansion: () -> Unit = {},
    onShowSpellSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (onShowSpellSettings != null) {
            IconButton(onClick = onShowSpellSettings) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        if (isCollapsible) {
            IconButton(onClick = onToggleAllExpansion) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }

        if (hasContentToEdit) {
            IconButton(onClick = onToggleEditMode) {
                Icon(
                    if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Toggle Edit Mode",
                    tint = if (isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSelectionSheet(
    showTabSheet: Boolean,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    currentTab: DisplayTab,
    tabs: List<DisplayTab>,
    pagerState: PagerState,
    scope: CoroutineScope,
    hazeState: HazeState?,
    blurPopups: Boolean,
    state: CharacterDetailState,
    onSelectTab: (DisplayTab) -> Unit = {}
) {
    if (!showTabSheet) return

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    var tabForIconPicker by remember { mutableStateOf<DisplayTab?>(null) }
    var tabToDelete by remember { mutableStateOf<DisplayTab?>(null) }
    var newTabTitle by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // to/from могут указывать на не-вкладку (например, кнопку «+») — не падаем и не двигаем.
        if (from.index in tabs.indices && to.index in tabs.indices) {
            val newList = tabs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            state.reorderTabs(newList)
        }
    }

    CompositionLocalProvider(LocalHazeStyle provides ru.quasaris.characternexus.ui.TabSheetHazeStyle) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = if (isOled) Color.Black
            else if (blurPopups) colorScheme.surface.copy(alpha = 0.1f)
            else colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .run {
                        if (blurPopups && hazeState != null && !isOled) {
                            this.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .hazeEffect(state = hazeState) {
                                    style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                                    inputScale = HazeInputScale.Fixed(0.6f)
                                }
                        } else this
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Вкладки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { state.isTabEditMode = !state.isTabEditMode }) {
                            Icon(
                                if (state.isTabEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                contentDescription = "Режим редактирования вкладок",
                                tint = if (state.isTabEditMode) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(tabs, key = { _, item -> item.key }) { index, tab ->
                            ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                                val textColor = if (tab.isDisabled) colorScheme.onSurface.copy(alpha = 0.38f)
                                else if (tab.key == currentTab.key) colorScheme.primary
                                else colorScheme.onSurface

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDragging) colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else if (tab.isDisabled) colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    else if (tab.key == currentTab.key) colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent,
                                    onClick = {
                                        if (!state.isTabEditMode) {
                                            onSelectTab(tab)
                                            scope.launch {
                                                sheetState.hide()
                                                onDismissRequest()
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (state.isTabEditMode) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Перетащить",
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.draggableHandle().size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (state.isTabEditMode) tabForIconPicker = tab
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                displayTabIcon(tab.iconName),
                                                contentDescription = null,
                                                tint = if (tab.isDisabled) colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                else if (tab.key == currentTab.key) colorScheme.primary
                                                else colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        EditableTabTitle(
                                            text = tab.title,
                                            isEditMode = state.isTabEditMode,
                                            onSave = { newTitle -> state.updateTabTitle(tab, newTitle) },
                                            textStyle = TextStyle(
                                                fontSize = 16.sp,
                                                fontWeight = if (tab.key == currentTab.key) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (state.isTabEditMode) {
                                            IconButton(
                                                onClick = { state.toggleTabDisabled(tab) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(
                                                            color = if (tab.isDisabled) colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else colorScheme.primary,
                                                            shape = RoundedCornerShape(50)
                                                        )
                                                )
                                            }

                                            if (tab.isCustom) {
                                                IconButton(
                                                    onClick = { tabToDelete = tab },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Удалить вкладку",
                                                        tint = colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isTabEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTabTitle,
                                onValueChange = { newTabTitle = it },
                                placeholder = { Text("Название новой вкладки") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newTabTitle.isNotBlank()) {
                                        state.addCustomTab(newTabTitle)
                                        newTabTitle = ""
                                    }
                                })
                            )
                            Button(
                                onClick = {
                                    if (newTabTitle.isNotBlank()) {
                                        state.addCustomTab(newTabTitle)
                                        newTabTitle = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (tabForIconPicker != null) {
        IconPickerDialog(
            targetTab = tabForIconPicker,
            onDismiss = { tabForIconPicker = null },
            onSelectIcon = { chosenIcon ->
                state.updateTabIcon(tabForIconPicker!!, chosenIcon)
            }
        )
    }

    if (tabToDelete != null) {
        AlertDialog(
            onDismissRequest = { tabToDelete = null },
            title = { Text("Удаление вкладки") },
            text = { Text("Вы точно хотите удалить вкладку «${tabToDelete?.title}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tabToDelete?.let { state.deleteCustomTab(it) }
                        tabToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tabToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}