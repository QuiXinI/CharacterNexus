package ru.quasaris.characternexus.ui.glossary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.SpellCard
import ru.quasaris.characternexus.tabs.spells.SpellCardItem

private val LEVEL_W = 44.dp
private val SLOTS_W = 96.dp

private fun widthFor(column: ProgressionColumn): Dp = when {
    column.key == "features" -> 180.dp
    column.kind == ColumnKind.TEXT -> 120.dp
    else -> 84.dp
}

private data class DetailTab(val key: String, val title: String)

/**
 * Страница класса или выбранного подкласса. Вкладки: основная (для класса — таблица
 * прогрессии с липким двухстрочным заголовком), кастомные, «Заклинания» (по тогглу)
 * и «Подклассы».
 */
@Composable
fun ClassDetailScreen(
    cls: ClassDefinition,
    subclasses: List<SubclassDefinition>,
    classSpells: List<SpellCard>,
    subclassSpells: List<SpellCard>,
    allSpells: List<SpellCard>,
    selectedSubclass: SubclassDefinition?,
    onSelectSubclass: (SubclassDefinition?) -> Unit
) {
    val tabs = remember(cls, subclasses, selectedSubclass) {
        buildList {
            if (selectedSubclass == null) {
                add(DetailTab("main", "Класс"))
                cls.customTabs.forEach { add(DetailTab("custom:${it.id}", it.title.ifBlank { "Вкладка" })) }
                if (cls.spellcasting.showSpellListTab) add(DetailTab("spells", "Заклинания"))
                if (subclasses.isNotEmpty()) {
                    add(DetailTab("subclasses", if (cls.subclassLabel == "Подкласс") "Подклассы" else cls.subclassLabel.ifBlank { "Подклассы" }))
                }
            } else {
                add(DetailTab("main", "Подкласс"))
                selectedSubclass.customTabs.forEach { add(DetailTab("custom:${it.id}", it.title.ifBlank { "Вкладка" })) }
                if (selectedSubclass.showSpellListTab) add(DetailTab("spells", "Заклинания"))
            }
        }
    }
    var selectedKey by remember(cls.id, selectedSubclass?.id) { mutableStateOf("main") }
    val index = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val key = tabs[index].key

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedSubclass != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSelectSubclass(null) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "К классу") }
                Column {
                    Text(selectedSubclass.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(cls.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (tabs.size > 1) {
            PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 8.dp) {
                tabs.forEach { tab ->
                    Tab(selected = tab.key == key, onClick = { selectedKey = tab.key }, text = { Text(tab.title, maxLines = 1) })
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val customTabs = selectedSubclass?.customTabs ?: cls.customTabs
            when {
                key == "main" && selectedSubclass == null -> ClassMainTab(cls)
                key == "main" && selectedSubclass != null -> SubclassMainTab(selectedSubclass, allSpells)
                key.startsWith("custom:") -> {
                    val tab = customTabs.find { "custom:${it.id}" == key }
                    if (tab != null) {
                        LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp)) { item { TabBlocksView(tab.blocks) } }
                    }
                }
                key == "spells" -> SpellListTab(if (selectedSubclass == null) classSpells else subclassSpells)
                key == "subclasses" -> SubclassListTab(cls, subclasses, onSelectSubclass)
            }
        }
    }
}

/* ---------- Основная вкладка класса ---------- */

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ClassMainTab(cls: ClassDefinition) {
    val columns = cls.progression.columns
    val rows = remember(cls.progression) { cls.progression.paddedRows() }
    val slotsColumn = columns.firstOrNull { it.kind == ColumnKind.SLOTS }
    val dataColumns = columns.filter { it.kind != ColumnKind.SLOTS }
    val hScroll = rememberScrollState()
    var expandedLevels by remember(cls.id) { mutableStateOf(setOf<Int>()) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        if (columns.isNotEmpty()) {
            stickyHeader(key = "table-head") { ProgressionHeader(dataColumns, slotsColumn, hScroll) }
            items(rows, key = { "row-${it.level}" }) { row ->
                ProgressionRowView(
                    row = row, cls = cls, dataColumns = dataColumns, slotsColumn = slotsColumn, hScroll = hScroll,
                    zebra = row.level % 2 == 0,
                    expanded = row.level in expandedLevels,
                    onToggle = { expandedLevels = if (row.level in expandedLevels) expandedLevels - row.level else expandedLevels + row.level }
                )
            }
            // Нулевой липкий элемент: после таблицы он вытесняет закреплённый заголовок.
            stickyHeader(key = "table-end") { Spacer(Modifier.height(0.dp)) }
        }
        item(key = "info") {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chips = listOfNotNull(
                    cls.hitDie.takeIf { it.isNotBlank() }?.let { "Кость хитов: $it" },
                    cls.primaryAbility.takeIf { it.isNotBlank() }?.let { "Основная характеристика: $it" },
                    cls.spellcasting.ability.takeIf { it.isNotBlank() && cls.spellcasting.type != SpellcastingType.NONE }?.let { "Заклинания: $it" }
                )
                if (chips.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { SmallBadge(it) }
                    }
                }
                GlossaryRichText(cls.description)
                if (cls.features.isNotEmpty()) DisplaySectionTitle("Умения класса", Modifier.padding(top = 4.dp))
            }
        }
        items(cls.features.sortedBy { it.level ?: 0 }, key = { it.id }) { feature ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { FeatureCard(feature, showLevel = true) }
        }
    }
}

@Composable
private fun HeaderCell(label: String, width: Dp) {
    val lines = label.split("\n", limit = 2)
    Box(modifier = Modifier.width(width).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(lines[0], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
            if (lines.size > 1) {
                Text(lines[1], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Двухстрочный заголовок таблицы фиксированной высоты. Липкий: фон непрозрачный. */
@Composable
private fun ProgressionHeader(dataColumns: List<ProgressionColumn>, slotsColumn: ProgressionColumn?, hScroll: ScrollState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderCell("Ур.", LEVEL_W)
            Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll), verticalAlignment = Alignment.CenterVertically) {
                dataColumns.forEach { HeaderCell(it.label, widthFor(it)) }
            }
            if (slotsColumn != null) HeaderCell(slotsColumn.label, SLOTS_W)
        }
    }
}

@Composable
private fun ProgressionRowView(
    row: ProgressionRow,
    cls: ClassDefinition,
    dataColumns: List<ProgressionColumn>,
    slotsColumn: ProgressionColumn?,
    hScroll: ScrollState,
    zebra: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val totals = remember(cls.spellcasting, row.level) { SpellSlotCalculator.totalsByLevel(cls.spellcasting, row.level) }
    val total = totals.values.sum()
    val hasSlots = slotsColumn != null && total > 0
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "slots-chevron")

    Column(modifier = Modifier.fillMaxWidth().background(if (zebra) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasSlots) Modifier.clickable(onClick = onToggle) else Modifier)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(LEVEL_W), contentAlignment = Alignment.Center) {
                Text("${row.level}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll), verticalAlignment = Alignment.CenterVertically) {
                dataColumns.forEach { col ->
                    Box(modifier = Modifier.width(widthFor(col)).padding(horizontal = 6.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(row.values[col.key].orEmpty().ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }
            if (slotsColumn != null) {
                Row(modifier = Modifier.width(SLOTS_W), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (hasSlots) {
                        Text("$total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ExpandMore, "Показать ячейки", modifier = Modifier.rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("—", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        AnimatedVisibility(visible = expanded && hasSlots) { SlotBreakdown(totals) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotBreakdown(totals: Map<Int, Int>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = LEVEL_W, end = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        totals.forEach { (level, count) ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    "$level ур. — $count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/* ---------- Подкласс ---------- */

@Composable
private fun SubclassMainTab(sub: SubclassDefinition, allSpells: List<SpellCard>) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { GlossaryRichText(sub.description) }
        if (sub.spellGrants.isNotEmpty()) {
            item { DisplaySectionTitle("Всегда подготовленные заклинания") }
            item {
                SimpleTable(
                    headers = listOf("Уровень класса", "Заклинания"),
                    rows = sub.spellGrants.sortedBy { it.classLevel }.map { grant ->
                        listOf("${grant.classLevel}", grant.spellIds.joinToString(", ") { id -> allSpells.find { it.id == id }?.name ?: id })
                    }
                )
            }
        }
        if (sub.features.isNotEmpty()) item { DisplaySectionTitle("Умения подкласса") }
        items(sub.features.sortedBy { it.level ?: 0 }, key = { it.id }) { FeatureCard(it, showLevel = true) }
    }
}

@Composable
private fun SubclassListTab(cls: ClassDefinition, subclasses: List<SubclassDefinition>, onSelect: (SubclassDefinition) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(subclasses, key = { it.id }) { sub ->
            Surface(
                onClick = { onSelect(sub) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sub.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${cls.name} · умений: ${sub.features.size}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/* ---------- Вкладка заклинаний ---------- */

@Composable
private fun SpellListTab(spells: List<SpellCard>) {
    if (spells.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Заклинаний нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var query by remember { mutableStateOf("") }
    val groups = remember(spells, query) {
        spells.filter { it.name.contains(query, ignoreCase = true) }
            .groupBy { it.levelInt() }
            .entries.sortedBy { it.key }
    }
    LazyColumn(contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "search") {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        groups.forEach { (level, list) ->
            item(key = "level-$level") { DisplaySectionTitle(if (level == 0) "Заговоры" else "$level уровень") }
            items(list, key = { it.id }) { spell ->
                val isOpen = spell.id in expanded
                SpellCardItem(
                    spell = spell,
                    isExpanded = isOpen,
                    onToggleExpand = { expanded = if (isOpen) expanded - spell.id else expanded + spell.id }
                )
            }
        }
    }
}
