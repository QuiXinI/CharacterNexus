package ru.quasaris.characternexus.ui.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.ui.NavNode
import ru.quasaris.characternexus.ui.NavigationPathManager

/** Точка входа для просмотра любой сущности глоссария (кроме заклинаний). */
@Composable
fun GlossaryDetailWindow(
    type: GlossaryType,
    id: String,
    categoryTitle: String,
    repository: GlossaryRepository,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    when (type) {
        GlossaryType.CLASS -> ClassDetailWindow(id, repository, onTitleChange, onBack)
        GlossaryType.SPECIES -> {
            val item = remember(id) { repository.species().find { it.id == id } }
            DetailShell(categoryTitle, item?.name, onTitleChange, onBack) { if (item != null) SpeciesContent(item) }
        }
        GlossaryType.FEAT -> {
            val item = remember(id) { repository.feats().find { it.id == id } }
            DetailShell(categoryTitle, item?.name, onTitleChange, onBack) { if (item != null) FeatContent(item) }
        }
        GlossaryType.MAGIC_ITEM -> {
            val item = remember(id) { repository.magicItems().find { it.id == id } }
            val classes = remember { repository.classes() }
            DetailShell(categoryTitle, item?.name, onTitleChange, onBack) { if (item != null) MagicItemContent(item, classes) }
        }
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Просмотр недоступен") }
    }
}

@Composable
private fun DetailShell(
    categoryTitle: String,
    name: String?,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (name == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Не найдено") }
        return
    }
    LaunchedEffect(name) {
        onTitleChange(name)
        NavigationPathManager.updatePath(
            "glossary",
            listOf(
                NavNode("hub", "Глоссарий", 0) { onBack() },
                NavNode("cat", categoryTitle, 1) { onBack() },
                NavNode("detail", name, 2)
            )
        )
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
        Spacer(Modifier.height(80.dp))
    }
}

/* ---------- Класс ---------- */

@Composable
fun ClassDetailWindow(
    classId: String,
    repository: GlossaryRepository,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val cls = remember(classId) { repository.classById(classId) }
    if (cls == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Класс не найден") }
        return
    }
    val subclasses = remember(cls.id) { repository.subclassesOf(cls.id) }
    val allSpells = remember { repository.allSpells() }
    val classSpells = remember(cls) { repository.spellsForClass(cls) }
    var selected by remember { mutableStateOf<SubclassDefinition?>(null) }
    val subclassSpells = remember(selected) { selected?.let { repository.spellsForSubclass(it) }.orEmpty() }

    LaunchedEffect(cls, selected) {
        onTitleChange(selected?.name ?: cls.name)
        val path = mutableListOf(
            NavNode("hub", "Глоссарий", 0) { onBack() },
            NavNode("cat", "Классы", 1) { onBack() },
            NavNode("class", cls.name, 2) { selected = null }
        )
        selected?.let { path += NavNode("subclass", it.name, 3) }
        NavigationPathManager.updatePath("glossary", path)
    }

    ClassDetailScreen(
        cls = cls,
        subclasses = subclasses,
        classSpells = classSpells,
        subclassSpells = subclassSpells,
        allSpells = allSpells,
        selectedSubclass = selected,
        onSelectSubclass = { selected = it }
    )
}

/* ---------- Вид ---------- */

@Composable
private fun ColumnScope.SpeciesContent(item: SpeciesDefinition) {
    HeroCard(
        title = item.name,
        chips = listOfNotNull(
            item.creatureType.takeIf { it.isNotBlank() },
            item.size.takeIf { it.isNotBlank() },
            item.speed.takeIf { it.isNotBlank() }?.let { "Скорость: $it" }
        )
    )
    GlossaryRichText(item.description)
    if (item.features.isNotEmpty()) {
        DisplaySectionTitle("Особенности")
        FeatureList(item.features, showLevel = false, initiallyExpanded = true)
    }
    item.tables.forEach { table ->
        if (table.title.isNotBlank()) DisplaySectionTitle(table.title)
        SimpleTable(table.headers, table.rows)
    }
}

/* ---------- Черта ---------- */

@Composable
private fun ColumnScope.FeatContent(item: FeatDefinition) {
    HeroCard(
        title = item.name,
        chips = listOfNotNull(item.category.takeIf { it.isNotBlank() }, if (item.repeatable) "Можно брать несколько раз" else null),
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer
    )
    if (item.prerequisites.isNotBlank()) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Требования", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(item.prerequisites, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
    GlossaryRichText(item.description)
    if (item.features.isNotEmpty()) {
        DisplaySectionTitle("Способности")
        FeatureList(item.features, showLevel = false, initiallyExpanded = true)
    }
}

/* ---------- Магический предмет ---------- */

@Composable
private fun rarityColors(rarity: ItemRarity): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (rarity) {
        ItemRarity.COMMON -> c.surfaceContainerHighest to c.onSurface
        ItemRarity.UNCOMMON -> c.secondaryContainer to c.onSecondaryContainer
        ItemRarity.RARE -> c.primaryContainer to c.onPrimaryContainer
        ItemRarity.VERY_RARE -> c.tertiaryContainer to c.onTertiaryContainer
        ItemRarity.LEGENDARY -> c.tertiary to c.onTertiary
        ItemRarity.ARTIFACT -> c.errorContainer to c.onErrorContainer
    }
}

@Composable
private fun ColumnScope.MagicItemContent(item: MagicItem, classes: List<ClassDefinition>) {
    val (container, content) = rarityColors(item.rarity)
    HeroCard(
        title = item.name,
        subtitle = item.englishName.takeIf { item.showEnglishName && it.isNotBlank() },
        chips = listOfNotNull(item.kind.displayName, item.rarity.displayName, item.weight.takeIf { it.isNotBlank() }?.let { "Вес: $it" }),
        container = container,
        content = content
    )
    if (item.attunement.required) {
        val names = item.attunement.classIds.mapNotNull { id -> classes.find { it.id == id }?.name }
        val who = when {
            classes.isNotEmpty() && names.size == classes.size -> "любой класс"
            names.isNotEmpty() -> names.joinToString(", ")
            else -> ""
        }
        InfoCard(
            "Требует настройки",
            listOf(who, item.attunement.requirement).filter { it.isNotBlank() }.joinToString("\n")
        )
    }
    if (item.formula.isNotBlank()) InfoCard("Формула", item.formula)
    GlossaryRichText(item.description)
}

@Composable
private fun InfoCard(title: String, text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
