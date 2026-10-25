package ru.quasaris.characternexus.ui.glossary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.SpellCard

/* ---------- Умения ---------- */

@Composable
fun FeatureListEditor(
    features: List<FeatureDef>,
    onChange: (List<FeatureDef>) -> Unit,
    showLevel: Boolean,
    addLabel: String = "Добавить умение"
) {
    var editing by remember { mutableStateOf<FeatureDef?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        features.forEachIndexed { index, feature ->
            EditableItemRow(
                title = feature.name.ifBlank { "Без названия" },
                badge = if (showLevel) feature.level?.let { "$it ур." } else null,
                subtitle = feature.requirement.ifBlank { null },
                onEdit = { editing = feature },
                onDelete = { onChange(features.filterNot { it.id == feature.id }) },
                onMoveUp = if (index > 0) ({ onChange(features.moved(index, index - 1)) }) else null,
                onMoveDown = if (index < features.lastIndex) ({ onChange(features.moved(index, index + 1)) }) else null
            )
        }
        AddButton(addLabel) { editing = FeatureDef(level = if (showLevel) 1 else null) }
    }
    editing?.let { current ->
        FeatureEditDialog(
            feature = current,
            showLevel = showLevel,
            onDismiss = { editing = null },
            onSave = { updated ->
                val i = features.indexOfFirst { it.id == updated.id }
                onChange(if (i >= 0) features.replacedAt(i, updated) else features + updated)
                editing = null
            }
        )
    }
}

@Composable
fun FeatureEditDialog(feature: FeatureDef, showLevel: Boolean, onDismiss: () -> Unit, onSave: (FeatureDef) -> Unit) {
    var name by remember { mutableStateOf(feature.name) }
    var level by remember { mutableStateOf(feature.level) }
    var requirement by remember { mutableStateOf(feature.requirement) }
    var description by remember { mutableStateOf(feature.description) }
    FullDialog(
        title = "Умение",
        onDismiss = onDismiss,
        onConfirm = {
            onSave(feature.copy(name = name.trim(), level = if (showLevel) level else null, requirement = requirement.trim(), description = description))
        },
        confirmEnabled = name.isNotBlank()
    ) {
        GlossaryTextField("Название", name, { name = it })
        if (showLevel) GlossaryIntField("Уровень", level, { level = it })
        GlossaryTextField("Условие", requirement, { requirement = it }, singleLine = false)
        GlossaryRichField("Описание", description, { description = it }, resetKey = feature.id)
    }
}

/* ---------- Записи списков (воззвания и т.п.) ---------- */

@Composable
fun EntryListEditor(entries: List<GlossaryEntry>, onChange: (List<GlossaryEntry>) -> Unit) {
    var editing by remember { mutableStateOf<GlossaryEntry?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { index, entry ->
            EditableItemRow(
                title = entry.name.ifBlank { "Без названия" },
                badge = if (entry.repeatable) "Повторяемо" else null,
                subtitle = entry.requirement.ifBlank { null },
                onEdit = { editing = entry },
                onDelete = { onChange(entries.filterNot { it.id == entry.id }) },
                onMoveUp = if (index > 0) ({ onChange(entries.moved(index, index - 1)) }) else null,
                onMoveDown = if (index < entries.lastIndex) ({ onChange(entries.moved(index, index + 1)) }) else null
            )
        }
        AddButton("Добавить запись") { editing = GlossaryEntry() }
    }
    editing?.let { current ->
        var name by remember(current.id) { mutableStateOf(current.name) }
        var requirement by remember(current.id) { mutableStateOf(current.requirement) }
        var repeatable by remember(current.id) { mutableStateOf(current.repeatable) }
        var description by remember(current.id) { mutableStateOf(current.description) }
        FullDialog(
            title = "Запись",
            onDismiss = { editing = null },
            onConfirm = {
                val updated = current.copy(name = name.trim(), requirement = requirement.trim(), repeatable = repeatable, description = description)
                val i = entries.indexOfFirst { it.id == updated.id }
                onChange(if (i >= 0) entries.replacedAt(i, updated) else entries + updated)
                editing = null
            },
            confirmEnabled = name.isNotBlank()
        ) {
            GlossaryTextField("Название", name, { name = it })
            GlossaryTextField("Условие", requirement, { requirement = it }, singleLine = false)
            SwitchRow("Можно брать несколько раз", repeatable, { repeatable = it })
            GlossaryRichField("Описание", description, { description = it }, resetKey = current.id)
        }
    }
}

/* ---------- Таблицы ---------- */

private fun parseHeaders(text: String): List<String> = if (text.isBlank()) emptyList() else parseCells(text)
private fun parseRows(text: String): List<List<String>> = text.lines().filter { it.isNotBlank() }.map { parseCells(it) }

/** Таблица в текстовом виде: первая строка — заголовки, далее строки; ячейки разделяются символом |. */
@Composable
fun TableTextEditor(
    resetKey: String,
    headers: List<String>,
    rows: List<List<String>>,
    onChange: (headers: List<String>, rows: List<List<String>>) -> Unit
) {
    var headerText by remember(resetKey) { mutableStateOf(headers.toCellsLine()) }
    var rowsText by remember(resetKey) { mutableStateOf(rows.joinToString("\n") { it.toCellsLine() }) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlossaryTextField(
            "Заголовки", headerText,
            { headerText = it; onChange(parseHeaders(it), parseRows(rowsText)) },
            supportingText = "Ячейки разделяются символом |"
        )
        GlossaryTextField(
            "Строки", rowsText,
            { rowsText = it; onChange(parseHeaders(headerText), parseRows(it)) },
            singleLine = false, minLines = 4,
            supportingText = "Каждая строка таблицы — с новой строки"
        )
    }
}

@Composable
fun TablesEditor(tables: List<GlossaryTable>, onChange: (List<GlossaryTable>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tables.forEachIndexed { index, table ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlossaryTextField("Название таблицы", table.title, { onChange(tables.replacedAt(index, table.copy(title = it))) }, Modifier.weight(1f))
                        IconButton(onClick = { onChange(tables.filterNot { it.id == table.id }) }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TableTextEditor(table.id, table.headers, table.rows) { h, r ->
                        onChange(tables.replacedAt(index, table.copy(headers = h, rows = r)))
                    }
                }
            }
        }
        AddButton("Добавить таблицу") { onChange(tables + GlossaryTable(title = "Таблица")) }
    }
}

/* ---------- Кастомные вкладки ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTabsEditor(tabs: List<GlossaryCustomTab>, onChange: (List<GlossaryCustomTab>) -> Unit) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, tab ->
            val expanded = expandedId == tab.id
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedId = if (expanded) null else tab.id }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        Spacer(Modifier.width(8.dp))
                        Text(tab.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (index > 0) IconButton(onClick = { onChange(tabs.moved(index, index - 1)) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowUpward, "Выше", Modifier.size(18.dp)) }
                        if (index < tabs.lastIndex) IconButton(onClick = { onChange(tabs.moved(index, index + 1)) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowDownward, "Ниже", Modifier.size(18.dp)) }
                        IconButton(onClick = { onChange(tabs.filterNot { it.id == tab.id }) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, "Удалить", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    AnimatedVisibility(visible = expanded) {
                        TabContentEditor(tab) { onChange(tabs.replacedAt(index, it)) }
                    }
                }
            }
        }
        AddButton("Добавить вкладку") {
            val created = GlossaryCustomTab()
            onChange(tabs + created)
            expandedId = created.id
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabContentEditor(tab: GlossaryCustomTab, onChange: (GlossaryCustomTab) -> Unit) {
    val blocks = tab.blocks
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlossaryTextField("Название вкладки", tab.title, { onChange(tab.copy(title = it)) })
        blocks.forEachIndexed { index, block ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (block) { is TabBlock.Text -> "Текст"; is TabBlock.Table -> "Таблица"; is TabBlock.Entries -> "Список" },
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (index > 0) IconButton(onClick = { onChange(tab.copy(blocks = blocks.moved(index, index - 1))) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowUpward, "Выше", Modifier.size(16.dp)) }
                        if (index < blocks.lastIndex) IconButton(onClick = { onChange(tab.copy(blocks = blocks.moved(index, index + 1))) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowDownward, "Ниже", Modifier.size(16.dp)) }
                        IconButton(onClick = { onChange(tab.copy(blocks = blocks.filterNot { it.id == block.id })) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Удалить", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    val update: (TabBlock) -> Unit = { onChange(tab.copy(blocks = blocks.replacedAt(index, it))) }
                    when (block) {
                        is TabBlock.Text -> GlossaryRichField("Текст", block.text, { update(block.copy(text = it)) }, resetKey = block.id)
                        is TabBlock.Entries -> {
                            GlossaryTextField("Заголовок списка", block.title, { update(block.copy(title = it)) })
                            EntryListEditor(block.items) { update(block.copy(items = it)) }
                        }
                        is TabBlock.Table -> {
                            GlossaryTextField("Название таблицы", block.title, { update(block.copy(title = it)) })
                            TableTextEditor(block.id, block.headers, block.rows) { h, r -> update(block.copy(headers = h, rows = r)) }
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { onChange(tab.copy(blocks = blocks + TabBlock.Text())) }) { Text("+ Текст") }
            OutlinedButton(onClick = { onChange(tab.copy(blocks = blocks + TabBlock.Entries(title = "Список"))) }) { Text("+ Список") }
            OutlinedButton(onClick = { onChange(tab.copy(blocks = blocks + TabBlock.Table())) }) { Text("+ Таблица") }
        }
    }
}

/* ---------- Выбор заклинаний ---------- */

fun spellLevelLabel(spell: SpellCard): String = if (spell.level == "0") "Заговор" else "${spell.level} ур."

@Composable
fun SpellPickerDialog(
    title: String,
    allSpells: List<SpellCard>,
    selected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(selected.toSet()) }
    val filtered = remember(allSpells, query) {
        allSpells.filter { it.name.contains(query, ignoreCase = true) }
            .sortedWith(compareBy({ it.levelInt() }, { it.name }))
    }
    FullDialog(title = title, onDismiss = onDismiss, onConfirm = { onConfirm(current.toList()) }, scrollable = false) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Поиск...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Выбрано: ${current.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { spell ->
                val checked = spell.id in current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { current = if (checked) current - spell.id else current + spell.id }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text(spell.name, modifier = Modifier.weight(1f))
                    Text(spellLevelLabel(spell), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellIdsEditor(label: String, ids: List<String>, allSpells: List<SpellCard>, onChange: (List<String>) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val named = ids.map { id -> id to (allSpells.find { it.id == id }?.name ?: "Нет в книге: $id") }
        if (named.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                named.forEach { (id, name) ->
                    InputChip(
                        selected = false,
                        onClick = { onChange(ids - id) },
                        label = { Text(name) },
                        trailingIcon = { Icon(Icons.Default.Close, "Убрать", Modifier.size(16.dp)) }
                    )
                }
            }
        }
        FilledTonalButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.AutoFixHigh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Выбрать заклинания (${ids.size})")
        }
    }
    if (showPicker) {
        SpellPickerDialog(label, allSpells, ids, onDismiss = { showPicker = false }, onConfirm = { onChange(it); showPicker = false })
    }
}

@Composable
fun SpellGrantsEditor(grants: List<SpellGrant>, allSpells: List<SpellCard>, onChange: (List<SpellGrant>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        grants.forEachIndexed { index, grant ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlossaryIntField("Уровень класса", grant.classLevel, { onChange(grants.replacedAt(index, grant.copy(classLevel = (it ?: 1).coerceIn(1, MAX_CLASS_LEVEL)))) }, Modifier.weight(1f))
                        IconButton(onClick = { onChange(grants.filterIndexed { i, _ -> i != index }) }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    SpellIdsEditor("Всегда подготовленные", grant.spellIds, allSpells) {
                        onChange(grants.replacedAt(index, grant.copy(spellIds = it)))
                    }
                }
            }
        }
        AddButton("Добавить уровень") { onChange((grants + SpellGrant(classLevel = 3)).sortedBy { it.classLevel }) }
    }
}
