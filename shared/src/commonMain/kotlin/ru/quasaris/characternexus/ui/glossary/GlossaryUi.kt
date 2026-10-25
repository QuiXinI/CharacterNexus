package ru.quasaris.characternexus.ui.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.DynamicNoteState
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.NavNode
import ru.quasaris.characternexus.ui.NavigationPathManager

/* ---------- Общие утилиты ---------- */

fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}

fun <T> List<T>.replacedAt(index: Int, value: T): List<T> =
    toMutableList().also { if (index in it.indices) it[index] = value }

/* ---------- Поля ввода ---------- */

@Composable
fun GlossaryTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        supportingText = supportingText?.let { { Text(it) } }
    )
}

@Composable
fun GlossaryIntField(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() }.take(3).toIntOrNull()) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp)
    )
}

/** Редактор форматированного текста: проприетарный компонент проекта (как в SpellConfigDialog). */
@Composable
fun GlossaryRichField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    resetKey: Any,
    placeholder: String = ""
) {
    val colorScheme = MaterialTheme.colorScheme
    // Ключ включает value, а id стабилен, как в SpellConfigDialog: редактор получает свежее состояние.
    val state = remember(resetKey, value) { DynamicNoteState(id = "glossary_${label}_$resetKey", content = value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                ru.quasaris.characternexus.tabs.NotionBlockEditor(
                    field = state,
                    onFieldChange = { updated -> onValueChange(updated.content) },
                    canEdit = true,
                    isReorderMode = false,
                    contentPlaceholder = placeholder,
                    isBasicMode = true
                )
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, supporting: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
}

/* ---------- Контейнеры ---------- */

@Composable
fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
fun SmallBadge(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Строка элемента списка в редакторе: название, бейдж, сдвиг вверх/вниз, удаление. */
@Composable
fun EditableItemRow(
    title: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    badge: String? = null,
    subtitle: String? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (badge != null) {
                SmallBadge(badge)
                Spacer(Modifier.width(4.dp))
            }
            if (onMoveUp != null) IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowUpward, "Выше", Modifier.size(18.dp)) }
            if (onMoveDown != null) IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowDownward, "Ниже", Modifier.size(18.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Удалить", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

/* ---------- Диалоги ---------- */

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/** Крупный диалог с заголовком, прокручиваемым содержимым и кнопками. */
@Composable
fun FullDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)?,
    confirmEnabled: Boolean = true,
    confirmLabel: String = "Готово",
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
                val bodyModifier = Modifier.weight(1f).padding(horizontal = 20.dp)
                Column(
                    modifier = if (scrollable) bodyModifier.verticalScroll(rememberScrollState()) else bodyModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    if (onConfirm != null) {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}

/* ---------- Каркас окна редактора ---------- */

/**
 * Общий каркас всех редакторов глоссария: верхняя панель, кнопка «Сохранить»,
 * подтверждение при выходе с несохранёнными изменениями и удалении.
 * «Сохранить» вызывает onSave; закрытие после сохранения выполняет вызывающий код.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScaffold(
    title: String,
    itemName: String,
    isDirty: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    forceBlurEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    var showDiscard by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val requestClose: () -> Unit = { if (isDirty) showDiscard = true else onDismiss() }
    val currentClose by rememberUpdatedState(requestClose)

    BackHandler(onBack = requestClose)

    LaunchedEffect(title, itemName) {
        NavigationPathManager.updatePath(
            "modules",
            listOf(
                NavNode("modules", "Модули", 0) { currentClose() },
                NavNode("editor", title, 1) { currentClose() },
                NavNode("comp", itemName.ifBlank { "Новый" }, 2)
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = requestClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (canSave) {
                ExtendedFloatingActionButton(
                    onClick = onSave,
                    icon = { Icon(Icons.Default.Save, null) },
                    text = { Text("Сохранить") },
                    containerColor = if (isDirty) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                    contentColor = if (isDirty) colorScheme.onPrimaryContainer else colorScheme.onSurface
                )
            }
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Несохранённые изменения") },
            text = { Text(if (canSave) "Сохранить изменения перед выходом?" else "Выйти без сохранения? Заполните обязательные поля, чтобы сохранить.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    if (canSave) onSave() else onDismiss()
                }) { Text(if (canSave) "Сохранить" else "Выйти") }
            },
            dismissButton = {
                Row {
                    if (canSave) {
                        TextButton(onClick = { showDiscard = false; onDismiss() }) { Text("Не сохранять") }
                    }
                    TextButton(onClick = { showDiscard = false }) { Text("Отмена") }
                }
            }
        )
    }

    if (showDelete && onDelete != null) {
        ConfirmDialog(
            title = "Удалить?",
            text = "«${itemName.ifBlank { "Без названия" }}» будет удалено безвозвратно.",
            confirmLabel = "Удалить",
            onConfirm = { showDelete = false; onDelete() },
            onDismiss = { showDelete = false }
        )
    }
}
