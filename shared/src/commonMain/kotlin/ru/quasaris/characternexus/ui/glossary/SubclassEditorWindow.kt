package ru.quasaris.characternexus.ui.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.SpellCard

/**
 * Редактор подкласса. presetClassId задаёт класс при создании подкласса из страницы класса.
 * Список заклинаний подкласса полностью обособлен от других подклассов того же класса.
 */
@Composable
fun SubclassEditorWindow(
    initial: SubclassDefinition?,
    classes: List<ClassDefinition>,
    allSpells: List<SpellCard>,
    onDismiss: () -> Unit,
    onSave: (SubclassDefinition) -> Unit,
    onDelete: ((SubclassDefinition) -> Unit)? = null,
    presetClassId: String? = null,
    forceBlurEnabled: Boolean = false
) {
    val original = remember {
        initial ?: SubclassDefinition(id = newGlossaryId("subclass"), classId = presetClassId ?: classes.firstOrNull()?.id.orEmpty())
    }
    var draft by remember { mutableStateOf(original) }

    EditorScaffold(
        title = "Редактор подкласса",
        itemName = draft.name,
        isDirty = draft != original,
        canSave = draft.name.isNotBlank() && draft.id.isNotBlank() && draft.classId.isNotBlank(),
        onSave = { onSave(draft.copy(name = draft.name.trim(), id = draft.id.trim())) },
        onDismiss = onDismiss,
        onDelete = if (initial != null && onDelete != null) ({ onDelete(initial) }) else null,
        forceBlurEnabled = forceBlurEnabled
    ) {
        EditorSection("Основное") {
            if (classes.isEmpty()) {
                Text("Сначала создайте класс, к которому будет относиться подкласс.", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Класс", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceChips(
                    options = classes,
                    selected = classes.find { it.id == draft.classId },
                    label = { it.name.ifBlank { it.id } },
                    onSelect = { draft = draft.copy(classId = it.id) }
                )
            }
            GlossaryTextField("Название", draft.name, { draft = draft.copy(name = it) })
            GlossaryTextField(
                "ID", draft.id,
                { draft = draft.copy(id = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }) },
                enabled = initial == null,
                supportingText = if (initial == null) "Буквы, цифры, _ и -. После сохранения не меняется" else null
            )
            GlossaryRichField("Описание", draft.description, { draft = draft.copy(description = it) }, resetKey = draft.id)
        }

        EditorSection("Заклинания подкласса", subtitle = "Доступны только этому подклассу") {
            SwitchRow("Вкладка «Заклинания»", draft.showSpellListTab, { draft = draft.copy(showSpellListTab = it) })
            SpellIdsEditor("Список заклинаний подкласса", draft.spellIds, allSpells) { draft = draft.copy(spellIds = it) }
        }

        EditorSection("Всегда подготовленные", subtitle = "Заклинания, которые подкласс даёт с определённого уровня класса") {
            SpellGrantsEditor(draft.spellGrants, allSpells) { draft = draft.copy(spellGrants = it) }
        }

        EditorSection("Умения подкласса") {
            FeatureListEditor(draft.features, { draft = draft.copy(features = it) }, showLevel = true)
        }

        EditorSection("Дополнительные вкладки") {
            CustomTabsEditor(draft.customTabs) { draft = draft.copy(customTabs = it) }
        }
    }
}
