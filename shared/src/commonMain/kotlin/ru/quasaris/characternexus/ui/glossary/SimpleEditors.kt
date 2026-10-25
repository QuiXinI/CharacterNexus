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

private fun String.idChars(): String = filter { it.isLetterOrDigit() || it == '_' || it == '-' }

/* ---------- Вид ---------- */

@Composable
fun SpeciesEditorWindow(
    initial: SpeciesDefinition?,
    onDismiss: () -> Unit,
    onSave: (SpeciesDefinition) -> Unit,
    onDelete: ((SpeciesDefinition) -> Unit)? = null,
    forceBlurEnabled: Boolean = false
) {
    val original = remember { initial ?: SpeciesDefinition(id = newGlossaryId("species")) }
    var draft by remember { mutableStateOf(original) }

    EditorScaffold(
        title = "Редактор вида",
        itemName = draft.name,
        isDirty = draft != original,
        canSave = draft.name.isNotBlank() && draft.id.isNotBlank(),
        onSave = { onSave(draft.copy(name = draft.name.trim(), id = draft.id.trim())) },
        onDismiss = onDismiss,
        onDelete = if (initial != null && onDelete != null) ({ onDelete(initial) }) else null,
        forceBlurEnabled = forceBlurEnabled
    ) {
        EditorSection("Основное") {
            GlossaryTextField("Название", draft.name, { draft = draft.copy(name = it) })
            GlossaryTextField(
                "ID", draft.id, { draft = draft.copy(id = it.idChars()) },
                enabled = initial == null,
                supportingText = if (initial == null) "Буквы, цифры, _ и -. После сохранения не меняется" else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlossaryTextField("Тип существа", draft.creatureType, { draft = draft.copy(creatureType = it) }, Modifier.weight(1f))
                GlossaryTextField("Размер", draft.size, { draft = draft.copy(size = it) }, Modifier.weight(1f))
            }
            GlossaryTextField("Скорость", draft.speed, { draft = draft.copy(speed = it) })
            GlossaryTextField("Изображение (путь или ссылка)", draft.image, { draft = draft.copy(image = it) })
            GlossaryRichField("Описание", draft.description, { draft = draft.copy(description = it) }, resetKey = draft.id)
        }
        EditorSection("Особенности вида") {
            FeatureListEditor(draft.features, { draft = draft.copy(features = it) }, showLevel = false, addLabel = "Добавить особенность")
        }
        EditorSection("Таблицы") {
            TablesEditor(draft.tables) { draft = draft.copy(tables = it) }
        }
    }
}

/* ---------- Черта ---------- */

@Composable
fun FeatEditorWindow(
    initial: FeatDefinition?,
    onDismiss: () -> Unit,
    onSave: (FeatDefinition) -> Unit,
    onDelete: ((FeatDefinition) -> Unit)? = null,
    forceBlurEnabled: Boolean = false
) {
    val original = remember { initial ?: FeatDefinition(id = newGlossaryId("feat")) }
    var draft by remember { mutableStateOf(original) }

    EditorScaffold(
        title = "Редактор черты",
        itemName = draft.name,
        isDirty = draft != original,
        canSave = draft.name.isNotBlank() && draft.id.isNotBlank(),
        onSave = { onSave(draft.copy(name = draft.name.trim(), id = draft.id.trim())) },
        onDismiss = onDismiss,
        onDelete = if (initial != null && onDelete != null) ({ onDelete(initial) }) else null,
        forceBlurEnabled = forceBlurEnabled
    ) {
        EditorSection("Основное") {
            GlossaryTextField("Название", draft.name, { draft = draft.copy(name = it) })
            GlossaryTextField(
                "ID", draft.id, { draft = draft.copy(id = it.idChars()) },
                enabled = initial == null,
                supportingText = if (initial == null) "Буквы, цифры, _ и -. После сохранения не меняется" else null
            )
            GlossaryTextField("Категория", draft.category, { draft = draft.copy(category = it) }, supportingText = "Например: Черта происхождения, Общая черта")
            GlossaryTextField("Требования", draft.prerequisites, { draft = draft.copy(prerequisites = it) }, singleLine = false)
            SwitchRow("Можно брать несколько раз", draft.repeatable, { draft = draft.copy(repeatable = it) })
            GlossaryRichField("Описание", draft.description, { draft = draft.copy(description = it) }, resetKey = draft.id)
        }
        EditorSection("Способности черты") {
            FeatureListEditor(draft.features, { draft = draft.copy(features = it) }, showLevel = false, addLabel = "Добавить способность")
        }
    }
}

/* ---------- Магический предмет ---------- */

@Composable
fun MagicItemEditorWindow(
    initial: MagicItem?,
    classes: List<ClassDefinition>,
    onDismiss: () -> Unit,
    onSave: (MagicItem) -> Unit,
    onDelete: ((MagicItem) -> Unit)? = null,
    forceBlurEnabled: Boolean = false
) {
    val original = remember { initial ?: MagicItem(id = newGlossaryId("item")) }
    var draft by remember { mutableStateOf(original) }
    val attunement = draft.attunement

    EditorScaffold(
        title = "Редактор предмета",
        itemName = draft.name,
        isDirty = draft != original,
        canSave = draft.name.isNotBlank() && draft.id.isNotBlank(),
        onSave = { onSave(draft.copy(name = draft.name.trim(), id = draft.id.trim())) },
        onDismiss = onDismiss,
        onDelete = if (initial != null && onDelete != null) ({ onDelete(initial) }) else null,
        forceBlurEnabled = forceBlurEnabled
    ) {
        EditorSection("Основное") {
            GlossaryTextField("Название", draft.name, { draft = draft.copy(name = it) })
            GlossaryTextField(
                "ID", draft.id, { draft = draft.copy(id = it.idChars()) },
                enabled = initial == null,
                supportingText = if (initial == null) "Буквы, цифры, _ и -. После сохранения не меняется" else null
            )
            GlossaryTextField("Английское название", draft.englishName, { draft = draft.copy(englishName = it) })
            SwitchRow("Показывать английское название", draft.showEnglishName, { draft = draft.copy(showEnglishName = it) })
        }

        EditorSection("Вид и редкость") {
            Text("Вид предмета", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(MagicItemKind.entries, draft.kind, { it.displayName }) { draft = draft.copy(kind = it) }
            Spacer(Modifier.height(4.dp))
            Text("Редкость", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(ItemRarity.entries, draft.rarity, { it.displayName }) { draft = draft.copy(rarity = it) }
            GlossaryTextField("Вес", draft.weight, { draft = draft.copy(weight = it) })
            if (draft.kind == MagicItemKind.POTION || draft.kind == MagicItemKind.SCROLL) {
                GlossaryTextField("Формула", draft.formula, { draft = draft.copy(formula = it) }, supportingText = "Например: 2d4+2")
            }
        }

        EditorSection("Настройка") {
            SwitchRow(
                "Требует настройки", attunement.required,
                { draft = draft.copy(attunement = attunement.copy(required = it)) }
            )
            if (attunement.required) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Классы, которые могут настроиться", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    val allSelected = classes.isNotEmpty() && classes.all { it.id in attunement.classIds }
                    FilterChip(
                        selected = allSelected,
                        onClick = {
                            draft = draft.copy(attunement = attunement.copy(classIds = if (allSelected) emptyList() else classes.map { it.id }))
                        },
                        label = { Text("Все") }
                    )
                }
                if (classes.isEmpty()) {
                    Text("Классов пока нет. Создайте классы, чтобы выбрать их здесь.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ClassMultiChips(classes, attunement.classIds) { draft = draft.copy(attunement = attunement.copy(classIds = it)) }
                }
                GlossaryTextField(
                    "Дополнительное условие", attunement.requirement,
                    { draft = draft.copy(attunement = attunement.copy(requirement = it)) },
                    supportingText = "Например: только заклинатели"
                )
            }
        }

        EditorSection("Описание") {
            GlossaryRichField("Описание", draft.description, { draft = draft.copy(description = it) }, resetKey = draft.id)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassMultiChips(classes: List<ClassDefinition>, selected: List<String>, onChange: (List<String>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        classes.forEach { cls ->
            val on = cls.id in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - cls.id else selected + cls.id) },
                label = { Text(cls.name.ifBlank { cls.id }) }
            )
        }
    }
}
