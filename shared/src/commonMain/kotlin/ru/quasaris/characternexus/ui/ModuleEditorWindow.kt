package ru.quasaris.characternexus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.ModuleManager
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.InstalledModule
import ru.quasaris.characternexus.model.ModuleContent
import ru.quasaris.characternexus.model.SpellCard
import ru.quasaris.characternexus.ui.editors.SpellEditorWindow
import ru.quasaris.characternexus.ui.glossary.*
import ru.quasaris.characternexus.util.generateUuid

/** Что сейчас редактируется. null внутри означает создание новой сущности. */
private sealed interface EditorTarget {
    data class SpellTarget(val spell: SpellCard) : EditorTarget
    data class ClassTarget(val value: ClassDefinition?) : EditorTarget
    data class SubclassTarget(val value: SubclassDefinition?) : EditorTarget
    data class SpeciesTarget(val value: SpeciesDefinition?) : EditorTarget
    data class FeatTarget(val value: FeatDefinition?) : EditorTarget
    data class ItemTarget(val value: MagicItem?) : EditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorWindow(
    moduleManager: ModuleManager,
    spellbookManager: SpellbookManager,
    repository: GlossaryRepository,
    initialModule: InstalledModule,
    onBack: () -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    var manifest by remember { mutableStateOf(initialModule.manifest) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ModuleContent?>(null) }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    val allSpells = remember(editor) { if (editor != null) repository.allSpells() else emptyList() }
    val classes = remember(editor) { if (editor != null) repository.classes() else emptyList() }

    LaunchedEffect(manifest) { moduleManager.updateModule(initialModule.manifest.id, manifest) }

    fun upsertContent(type: GlossaryType, id: String) {
        if (manifest.contents.none { it.type == type.wireName && it.id == id }) {
            manifest = manifest.copy(
                contents = manifest.contents + ModuleContent(type.wireName, id, "${GlossaryStore.safeFileName(id)}.json")
            )
        }
    }

    fun removeContent(type: GlossaryType, id: String) {
        manifest = manifest.copy(contents = manifest.contents.filterNot { it.type == type.wireName && it.id == id })
    }

    // Открытый редактор занимает весь экран, как и раньше.
    when (val target = editor) {
        is EditorTarget.SpellTarget -> {
            SpellEditorWindow(
                spell = target.spell,
                onDismiss = { editor = null },
                onSave = { updated ->
                    val saved = spellbookManager.addOrUpdateSpell(updated)
                    upsertContent(GlossaryType.SPELL, saved.id)
                    editor = null
                },
                onDelete = { spell ->
                    spellbookManager.deleteSpell(spell.id)
                    removeContent(GlossaryType.SPELL, spell.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = settingsViewModel
            )
            return
        }
        is EditorTarget.ClassTarget -> {
            ClassEditorWindow(
                initial = target.value,
                allSpells = allSpells,
                onDismiss = { editor = null },
                onSave = { updated ->
                    repository.save(updated.copy(source = manifest.name, sourceModuleId = manifest.id))
                    upsertContent(GlossaryType.CLASS, updated.id)
                    editor = null
                },
                onDelete = { cls ->
                    repository.subclassesOf(cls.id).forEach { removeContent(GlossaryType.SUBCLASS, it.id) }
                    repository.delete(GlossaryType.CLASS, cls.id)
                    removeContent(GlossaryType.CLASS, cls.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled
            )
            return
        }
        is EditorTarget.SubclassTarget -> {
            SubclassEditorWindow(
                initial = target.value,
                classes = classes,
                allSpells = allSpells,
                onDismiss = { editor = null },
                onSave = { updated ->
                    repository.save(updated.copy(source = manifest.name, sourceModuleId = manifest.id))
                    upsertContent(GlossaryType.SUBCLASS, updated.id)
                    editor = null
                },
                onDelete = { sub ->
                    repository.delete(GlossaryType.SUBCLASS, sub.id)
                    removeContent(GlossaryType.SUBCLASS, sub.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled
            )
            return
        }
        is EditorTarget.SpeciesTarget -> {
            SpeciesEditorWindow(
                initial = target.value,
                onDismiss = { editor = null },
                onSave = { updated ->
                    repository.save(updated.copy(source = manifest.name, sourceModuleId = manifest.id))
                    upsertContent(GlossaryType.SPECIES, updated.id)
                    editor = null
                },
                onDelete = { species ->
                    repository.delete(GlossaryType.SPECIES, species.id)
                    removeContent(GlossaryType.SPECIES, species.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled
            )
            return
        }
        is EditorTarget.FeatTarget -> {
            FeatEditorWindow(
                initial = target.value,
                onDismiss = { editor = null },
                onSave = { updated ->
                    repository.save(updated.copy(source = manifest.name, sourceModuleId = manifest.id))
                    upsertContent(GlossaryType.FEAT, updated.id)
                    editor = null
                },
                onDelete = { feat ->
                    repository.delete(GlossaryType.FEAT, feat.id)
                    removeContent(GlossaryType.FEAT, feat.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled
            )
            return
        }
        is EditorTarget.ItemTarget -> {
            MagicItemEditorWindow(
                initial = target.value,
                classes = classes,
                onDismiss = { editor = null },
                onSave = { updated ->
                    repository.save(updated.copy(source = manifest.name, sourceModuleId = manifest.id))
                    upsertContent(GlossaryType.MAGIC_ITEM, updated.id)
                    editor = null
                },
                onDelete = { item ->
                    repository.delete(GlossaryType.MAGIC_ITEM, item.id)
                    removeContent(GlossaryType.MAGIC_ITEM, item.id)
                    editor = null
                },
                forceBlurEnabled = forceBlurEnabled
            )
            return
        }
        null -> {}
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(manifest.name) {
        NavigationPathManager.updatePath(
            "modules",
            listOf(
                NavNode("modules", "Модули", 0) { onBack() },
                NavNode("editor", manifest.name.ifBlank { "Новый" }, 1)
            )
        )
    }

    // Названия компонентов; пересчитываются после закрытия редактора.
    val names = remember(manifest.contents, editor) {
        buildMap<String, String> {
            GlossaryType.entries.forEach { type ->
                repository.entries(type).forEach { put("${type.wireName}:${it.id}", it.name) }
            }
        }
    }

    PredictiveBackBox(onBack = onBack, modifier = Modifier.fillMaxSize()) { _ ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Редактор модуля", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EditorSection("Основная информация") {
                    GlossaryTextField("Название", manifest.name, { manifest = manifest.copy(name = it) })
                    GlossaryTextField("ID модуля", manifest.id, {}, enabled = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlossaryTextField("Версия", manifest.version, { manifest = manifest.copy(version = it) }, Modifier.weight(1f))
                        GlossaryTextField("Система", manifest.system, { manifest = manifest.copy(system = it) }, Modifier.weight(1f))
                    }
                    GlossaryTextField("Описание", manifest.description, { manifest = manifest.copy(description = it) }, singleLine = false, minLines = 3)
                }

                EditorSection("Компоненты") {
                    Box {
                        FilledTonalButton(onClick = { showAddMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить компонент")
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            @Composable
                            fun item(label: String, create: () -> EditorTarget) = DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { showAddMenu = false; editor = create() }
                            )
                            item("Заклинание") {
                                EditorTarget.SpellTarget(SpellCard(id = generateUuid(), source = manifest.name, sourceModuleId = manifest.id))
                            }
                            item("Магический предмет") { EditorTarget.ItemTarget(null) }
                            item("Класс") { EditorTarget.ClassTarget(null) }
                            item("Подкласс") { EditorTarget.SubclassTarget(null) }
                            item("Вид") { EditorTarget.SpeciesTarget(null) }
                            item("Черта") { EditorTarget.FeatTarget(null) }
                        }
                    }

                    if (manifest.contents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("Нет компонентов", color = colorScheme.onSurfaceVariant)
                        }
                    } else {
                        manifest.contents.forEach { content ->
                            ComponentItem(
                                content = content,
                                displayName = names["${content.type}:${content.id}"] ?: content.id,
                                onEdit = {
                                    when (GlossaryType.fromWireName(content.type)) {
                                        GlossaryType.SPELL -> repository.allSpells().find { it.id == content.id }?.let { editor = EditorTarget.SpellTarget(it) }
                                        GlossaryType.CLASS -> repository.classById(content.id)?.let { editor = EditorTarget.ClassTarget(it) }
                                        GlossaryType.SUBCLASS -> repository.subclasses().find { it.id == content.id }?.let { editor = EditorTarget.SubclassTarget(it) }
                                        GlossaryType.SPECIES -> repository.species().find { it.id == content.id }?.let { editor = EditorTarget.SpeciesTarget(it) }
                                        GlossaryType.FEAT -> repository.feats().find { it.id == content.id }?.let { editor = EditorTarget.FeatTarget(it) }
                                        GlossaryType.MAGIC_ITEM -> repository.magicItems().find { it.id == content.id }?.let { editor = EditorTarget.ItemTarget(it) }
                                        null -> {}
                                    }
                                },
                                onDelete = { deleteTarget = content }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    deleteTarget?.let { content ->
        ConfirmDialog(
            title = "Удалить компонент?",
            text = "«${names["${content.type}:${content.id}"] ?: content.id}» будет удалён из модуля и глоссария.",
            confirmLabel = "Удалить",
            onConfirm = {
                GlossaryType.fromWireName(content.type)?.let { type ->
                    repository.delete(type, content.id)
                    removeContent(type, content.id)
                }
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun ComponentItem(content: ModuleContent, displayName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val typeLabel = when (content.type) {
        "spell" -> "Заклинание"
        "magic_item" -> "Магический предмет"
        "class" -> "Класс"
        "subclass" -> "Подкласс"
        "species" -> "Вид"
        "feat" -> "Черта"
        else -> content.type
    }
    Surface(onClick = onEdit, shape = RoundedCornerShape(20.dp), color = colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (content.type) {
                    "spell" -> Icons.Default.AutoFixHigh
                    "magic_item" -> Icons.Default.Science
                    "class" -> Icons.Default.Shield
                    "subclass" -> Icons.Default.KeyboardDoubleArrowDown
                    "feat" -> Icons.Default.Star
                    "species" -> Icons.Default.Groups
                    else -> Icons.Default.Extension
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить", Modifier.size(20.dp), tint = colorScheme.error) }
        }
    }
}
