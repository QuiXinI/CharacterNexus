package ru.quasaris.characternexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.*
import ru.quasaris.characternexus.model.*

@Composable
fun TabManagementDialog(
    state: CharacterDetailState,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val initialEffectiveTabs = remember(state.customTabs, state.tabOrder, state.customTabTitles, state.customTabIcons) {
        (state.initialCharacter ?: Character()).copy(
            customTabs = state.customTabs,
            tabOrder = state.tabOrder,
            customTabTitles = state.customTabTitles,
            customTabIcons = state.customTabIcons
        ).getEffectiveTabs()
    }
    var tabs by remember { mutableStateOf(initialEffectiveTabs) }
    var tabForIconPicker by remember { mutableStateOf<DisplayTab?>(null) }
    var newTabTitle by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        tabs = tabs.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        state.tabOrder = tabs.map { it.key }
    }

    val saveChanges = {
        val newTitles = state.customTabTitles.toMutableMap()
        val newIcons = state.customTabIcons.toMutableMap()
        val newCustomTabs = state.customTabs.toMutableList()

        tabs.forEach { displayTab ->
            when (displayTab) {
                is DisplayTab.BuiltIn -> {
                    if (displayTab.customTitle != null) {
                        newTitles[displayTab.tab.name] = displayTab.customTitle
                    }
                    if (displayTab.customIcon != null) {
                        newIcons[displayTab.tab.name] = displayTab.customIcon
                    }
                }
                is DisplayTab.Custom -> {
                    val idx = newCustomTabs.indexOfFirst { it.id == displayTab.customTab.id }
                    if (idx != -1) {
                        newCustomTabs[idx] = displayTab.customTab
                    }
                }
            }
        }
        state.tabOrder = tabs.map { it.key }
        state.customTabTitles = newTitles
        state.customTabIcons = newIcons
        state.customTabs = newCustomTabs
    }

    AlertDialog(
        onDismissRequest = {
            saveChanges()
            onDismiss()
        },
        title = { Text("Управление вкладками") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Зажмите и перетаскивайте вкладки для изменения порядка. Кликните по названию или карандашу для редактирования текста.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(tabs, key = { _, item -> item.key }) { _, tab ->
                        ReorderableItem(reorderableState, key = tab.key) { isDragging ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDragging) colorScheme.primaryContainer.copy(alpha = 0.6f) else colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Drag handle
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Перетащить",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.draggableHandle().size(20.dp)
                                    )

                                    // Gear / Icon selector button
                                    IconButton(
                                        onClick = { tabForIconPicker = tab },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = displayTabIcon(tab.iconName),
                                            contentDescription = "Настройка иконки",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Inline Editable Title
                                    InlineEditableTabTitle(
                                        title = tab.title,
                                        onSave = { newTitle ->
                                            tabs = tabs.map { t ->
                                                if (t.key == tab.key) {
                                                    when (t) {
                                                        is DisplayTab.BuiltIn -> t.copy(customTitle = newTitle)
                                                        is DisplayTab.Custom -> t.copy(customTab = t.customTab.copy(title = newTitle))
                                                    }
                                                } else t
                                            }
                                            saveChanges()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (tab.isCustom) {
                                        IconButton(
                                            onClick = {
                                                tabs = tabs.filter { it.key != tab.key }
                                                state.customTabs = state.customTabs.filter { it.id != tab.key }
                                                saveChanges()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline Add Tab input
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                val newCustom = state.addCustomTab(newTabTitle)
                                if (newCustom != null) {
                                    tabs = tabs + DisplayTab.Custom(newCustom)
                                }
                                newTabTitle = ""
                                saveChanges()
                            }
                        })
                    )
                    Button(
                        onClick = {
                            if (newTabTitle.isNotBlank()) {
                                val newCustom = state.addCustomTab(newTabTitle)
                                if (newCustom != null) {
                                    tabs = tabs + DisplayTab.Custom(newCustom)
                                }
                                newTabTitle = ""
                                saveChanges()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saveChanges()
                    onDismiss()
                }
            ) {
                Text("Готово")
            }
        }
    )

    // Icon Picker Dialog
    if (tabForIconPicker != null) {
        val target = tabForIconPicker!!
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
            onDismissRequest = { tabForIconPicker = null },
            title = { Text("Выберите иконку для «${target.title}»") },
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
                        val isSelected = target.iconName.equals(iconName, ignoreCase = true)
                        Surface(
                            onClick = {
                                tabs = tabs.map { t ->
                                    if (t.key == target.key) {
                                        when (t) {
                                            is DisplayTab.BuiltIn -> t.copy(customIcon = iconName)
                                            is DisplayTab.Custom -> t.copy(customTab = t.customTab.copy(iconName = iconName))
                                        }
                                    } else t
                                }
                                saveChanges()
                                tabForIconPicker = null
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
                TextButton(onClick = { tabForIconPicker = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun InlineEditableTabTitle(
    title: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember(title) { mutableStateOf(title) }
    val focusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (isEditing) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (!it.isFocused && isEditing) {
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
                singleLine = true
            )
            IconButton(
                onClick = {
                    isEditing = false
                    onSave(value)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Сохранить", tint = colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isEditing = true }
            )
            IconButton(
                onClick = { isEditing = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
}
