package ru.quasaris.characternexus.ui.glossary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.SpellCard

/** Редактор класса: основное, заклинательство, таблица прогрессии, умения, кастомные вкладки. */
@Composable
fun ClassEditorWindow(
    initial: ClassDefinition?,
    allSpells: List<SpellCard>,
    onDismiss: () -> Unit,
    onSave: (ClassDefinition) -> Unit,
    onDelete: ((ClassDefinition) -> Unit)? = null,
    forceBlurEnabled: Boolean = false
) {
    val original = remember { initial ?: ClassDefinition(id = newGlossaryId("class")) }
    var draft by remember { mutableStateOf(original) }
    var showSlotsDialog by remember { mutableStateOf(false) }
    val casting = draft.spellcasting

    EditorScaffold(
        title = "Редактор класса",
        itemName = draft.name,
        isDirty = draft != original,
        canSave = draft.name.isNotBlank() && draft.id.isNotBlank(),
        onSave = {
            onSave(draft.copy(name = draft.name.trim(), id = draft.id.trim(), progression = draft.progression.copy(rows = draft.progression.paddedRows())))
        },
        onDismiss = onDismiss,
        onDelete = if (initial != null && onDelete != null) ({ onDelete(initial) }) else null,
        forceBlurEnabled = forceBlurEnabled
    ) {
        EditorSection("Основное") {
            GlossaryTextField("Название", draft.name, { draft = draft.copy(name = it) })
            GlossaryTextField(
                "ID", draft.id,
                { draft = draft.copy(id = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }) },
                enabled = initial == null,
                supportingText = if (initial == null) "Буквы, цифры, _ и -. После сохранения не меняется" else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlossaryTextField("Основная характеристика", draft.primaryAbility, { draft = draft.copy(primaryAbility = it) }, Modifier.weight(1f))
                GlossaryTextField("Кость хитов", draft.hitDie, { draft = draft.copy(hitDie = it) }, Modifier.weight(1f))
            }
            GlossaryTextField("Название раздела подклассов", draft.subclassLabel, { draft = draft.copy(subclassLabel = it) })
            GlossaryRichField("Описание", draft.description, { draft = draft.copy(description = it) }, resetKey = draft.id)
        }

        EditorSection("Заклинательство") {
            ChoiceChips(
                options = SpellcastingType.entries,
                selected = casting.type,
                label = {
                    when (it) {
                        SpellcastingType.NONE -> "Нет"
                        SpellcastingType.FULL -> "Полный"
                        SpellcastingType.HALF -> "Половинный"
                        SpellcastingType.CUSTOM -> "Особый"
                    }
                },
                onSelect = { draft = draft.copy(spellcasting = casting.copy(type = it)) }
            )
            when (casting.type) {
                SpellcastingType.HALF -> Text(
                    "Половинный заклинатель: уровень округляется вверх (1 ур. = 1, 3 ур. = 2, 5 ур. = 3).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SpellcastingType.CUSTOM -> FilledTonalButton(onClick = { showSlotsDialog = true }) {
                    Icon(Icons.Default.GridOn, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ячейки по уровням (${casting.customSlots.size} ур.)")
                }
                else -> {}
            }
            if (casting.type != SpellcastingType.NONE) {
                GlossaryTextField("Заклинательная характеристика", casting.ability, { draft = draft.copy(spellcasting = casting.copy(ability = it)) })
            }
            SwitchRow(
                "Вкладка «Заклинания»", casting.showSpellListTab,
                { draft = draft.copy(spellcasting = casting.copy(showSpellListTab = it)) },
                supporting = "Список заклинаний класса на странице класса"
            )
            SpellIdsEditor("Заклинания класса", casting.spellIds, allSpells) {
                draft = draft.copy(spellcasting = casting.copy(spellIds = it))
            }
            Text(
                "Заклинания, у которых указан этот класс, добавляются в список автоматически.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EditorSection("Таблица прогрессии", subtitle = "Заголовок колонки занимает до двух строк") {
            ProgressionEditor(draft.progression, draft.features, casting.type) { draft = draft.copy(progression = it) }
        }

        EditorSection("Умения класса") {
            FeatureListEditor(draft.features, { draft = draft.copy(features = it) }, showLevel = true)
        }

        EditorSection("Дополнительные вкладки", subtitle = "Воззвания, ритуалы и любые другие списки") {
            CustomTabsEditor(draft.customTabs) { draft = draft.copy(customTabs = it) }
        }
    }

    if (showSlotsDialog) {
        CustomSlotsDialog(
            initial = casting.customSlots,
            onDismiss = { showSlotsDialog = false },
            onSave = {
                draft = draft.copy(spellcasting = casting.copy(customSlots = it))
                showSlotsDialog = false
            }
        )
    }
}

/* ---------- Таблица прогрессии ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressionEditor(
    progression: ClassProgression,
    features: List<FeatureDef>,
    castingType: SpellcastingType,
    onChange: (ClassProgression) -> Unit
) {
    val columns = progression.columns
    val rows = progression.paddedRows()

    fun withColumn(column: ProgressionColumn, fill: (Int) -> String?): ClassProgression = progression.copy(
        columns = columns + column,
        rows = rows.map { row -> fill(row.level)?.let { row.copy(values = row.values + (column.key to it)) } ?: row }
    )

    fun featuresAt(level: Int): String =
        features.filter { it.level == level }.joinToString(", ") { it.name }.ifBlank { "—" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        columns.forEachIndexed { index, column ->
            ColumnEditorCard(
                column = column,
                onChange = { onChange(progression.copy(columns = columns.replacedAt(index, it))) },
                onDelete = {
                    onChange(progression.copy(
                        columns = columns.filterNot { it.key == column.key },
                        rows = rows.map { it.copy(values = it.values - column.key) }
                    ))
                },
                onMoveLeft = if (index > 0) ({ onChange(progression.copy(columns = columns.moved(index, index - 1))) }) else null,
                onMoveRight = if (index < columns.lastIndex) ({ onChange(progression.copy(columns = columns.moved(index, index + 1))) }) else null
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (columns.none { it.key == "proficiency" }) {
                AssistChip(onClick = {
                    onChange(withColumn(ProgressionColumn("proficiency", "Бонус\nмастерства", ColumnKind.TEXT)) { "+${2 + (it - 1) / 4}" })
                }, label = { Text("+ Бонус мастерства") })
            }
            if (columns.none { it.key == "features" }) {
                AssistChip(onClick = {
                    onChange(withColumn(ProgressionColumn("features", "Умения", ColumnKind.TEXT)) { featuresAt(it) })
                }, label = { Text("+ Умения") })
            }
            if (columns.none { it.kind == ColumnKind.SLOTS }) {
                AssistChip(onClick = {
                    onChange(withColumn(ProgressionColumn("slots", "Ячейки", ColumnKind.SLOTS)) { null })
                }, label = { Text("+ Ячейки") })
            }
            AssistChip(onClick = {
                onChange(withColumn(ProgressionColumn(newGlossaryId("col"), "Колонка", ColumnKind.NUMBER)) { null })
            }, label = { Text("+ Своя колонка") })
        }

        if (columns.any { it.key == "features" }) {
            OutlinedButton(onClick = {
                onChange(progression.copy(rows = rows.map { it.copy(values = it.values + ("features" to featuresAt(it.level))) }))
            }) { Text("Обновить «Умения» из списка умений") }
        }
        if (castingType != SpellcastingType.NONE && columns.none { it.kind == ColumnKind.SLOTS }) {
            Text(
                "Класс заклинатель, но колонки «Ячейки» нет: добавьте её, чтобы ячейки показывались справа от таблицы.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
            )
        }

        if (columns.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(36.dp)) { Text("Ур.", style = MaterialTheme.typography.labelSmall) }
                        columns.forEach { col ->
                            Box(Modifier.width(96.dp).padding(horizontal = 4.dp)) {
                                Text(col.label.replace("\n", " "), style = MaterialTheme.typography.labelSmall, maxLines = 2, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    rows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(36.dp)) { Text("${row.level}", style = MaterialTheme.typography.bodySmall) }
                            columns.forEach { col ->
                                if (col.kind == ColumnKind.SLOTS) {
                                    Box(Modifier.width(96.dp).padding(4.dp)) {
                                        Text("авто", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    CellField(row.values[col.key].orEmpty(), Modifier.width(96.dp).padding(2.dp)) { text ->
                                        onChange(progression.copy(rows = rows.map { r ->
                                            if (r.level == row.level) r.copy(values = r.values + (col.key to text)) else r
                                        }))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellField(value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ColumnEditorCard(
    column: ProgressionColumn,
    onChange: (ProgressionColumn) -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?
) {
    val parts = column.label.split("\n", limit = 2)
    val line1 = parts[0]
    val line2 = parts.getOrElse(1) { "" }
    fun relabel(l1: String, l2: String) = onChange(column.copy(label = if (l2.isBlank()) l1 else "$l1\n$l2"))

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlossaryTextField("Строка 1", line1, { relabel(it, line2) }, Modifier.weight(1f))
                GlossaryTextField("Строка 2", line2, { relabel(line1, it) }, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    ChoiceChips(
                        options = ColumnKind.entries, selected = column.kind,
                        label = { when (it) { ColumnKind.TEXT -> "Текст"; ColumnKind.NUMBER -> "Число"; ColumnKind.SLOTS -> "Ячейки" } },
                        onSelect = { onChange(column.copy(kind = it)) }
                    )
                }
                if (onMoveLeft != null) IconButton(onClick = onMoveLeft, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, "Левее", Modifier.size(18.dp)) }
                if (onMoveRight != null) IconButton(onClick = onMoveRight, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "Правее", Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Удалить", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/* ---------- Ячейки особого заклинателя ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomSlotsDialog(initial: List<CustomSlotRow>, onDismiss: () -> Unit, onSave: (List<CustomSlotRow>) -> Unit) {
    var rows by remember { mutableStateOf(initial) }
    var level by remember { mutableIntStateOf(1) }

    fun countOf(lvl: Int, slotLevel: Int): Int =
        rows.find { it.level == lvl }?.slots?.find { it.slotLevel == slotLevel }?.count ?: 0

    fun setCount(lvl: Int, slotLevel: Int, count: Int) {
        val others = rows.find { it.level == lvl }?.slots.orEmpty().filter { it.slotLevel != slotLevel }
        val updated = (if (count > 0) others + SlotCount(slotLevel, count) else others).sortedBy { it.slotLevel }
        rows = (rows.filter { it.level != lvl } + CustomSlotRow(lvl, updated)).sortedBy { it.level }
    }

    FullDialog(
        title = "Ячейки по уровням",
        onDismiss = onDismiss,
        onConfirm = { onSave(rows.filter { it.slots.isNotEmpty() }.sortedBy { it.level }) }
    ) {
        Text("Уровень класса", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..MAX_CLASS_LEVEL).forEach { l ->
                val filled = rows.any { it.level == l && it.slots.isNotEmpty() }
                FilterChip(
                    selected = l == level,
                    onClick = { level = l },
                    label = { Text(if (filled) "$l •" else "$l") }
                )
            }
        }
        for (slotLevel in 1..9) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ячейки $slotLevel-го уровня", modifier = Modifier.weight(1f))
                IconButton(onClick = { setCount(level, slotLevel, (countOf(level, slotLevel) - 1).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.Remove, "Меньше")
                }
                Text("${countOf(level, slotLevel)}", modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { setCount(level, slotLevel, countOf(level, slotLevel) + 1) }) {
                    Icon(Icons.Default.Add, "Больше")
                }
            }
        }
        if (level > 1) {
            OutlinedButton(onClick = {
                val prev = rows.find { it.level == level - 1 }?.slots.orEmpty()
                rows = (rows.filter { it.level != level } + CustomSlotRow(level, prev)).sortedBy { it.level }
            }) { Text("Скопировать с ${level - 1} уровня") }
        }
    }
}
