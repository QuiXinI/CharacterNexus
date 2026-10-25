package ru.quasaris.characternexus.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.backend.glossary.GlossaryRepository
import ru.quasaris.characternexus.backend.glossary.GlossaryType
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.tabs.spells.*
import ru.quasaris.characternexus.ui.editors.SpellEditorWindow
import ru.quasaris.characternexus.ui.glossary.GlossaryDetailWindow

enum class GlossaryCategory(val title: String, val dirName: String, val icon: ImageVector) {
    SPECIES("Виды", "species", Icons.Default.Groups),
    CLASSES("Классы", "classes", Icons.Default.Shield),
    SPELLS("Заклинания", "spells", Icons.Default.AutoFixHigh),
    FEATS("Черты", "feats", Icons.Default.Star),
    MAGIC_ITEMS("Магические предметы", "magic_items", Icons.Default.AutoFixNormal)
}

/** Тип сущности глоссария для категории. Для заклинаний null: они живут в книге заклинаний. */
fun GlossaryCategory.glossaryType(): GlossaryType? = when (this) {
    GlossaryCategory.SPECIES -> GlossaryType.SPECIES
    GlossaryCategory.CLASSES -> GlossaryType.CLASS
    GlossaryCategory.FEATS -> GlossaryType.FEAT
    GlossaryCategory.MAGIC_ITEMS -> GlossaryType.MAGIC_ITEM
    GlossaryCategory.SPELLS -> null
}

sealed class GlossaryView {
    data object Hub : GlossaryView()
    data class Category(val category: GlossaryCategory) : GlossaryView()
    data class Detail(val category: GlossaryCategory, val id: String) : GlossaryView()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryWindow(
    spellbookManager: SpellbookManager,
    repository: GlossaryRepository,
    onOpenDrawer: () -> Unit,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
) {
    var currentView by remember { mutableStateOf<GlossaryView>(GlossaryView.Hub) }
    var detailTitle by remember { mutableStateOf("") }

    LaunchedEffect(currentView) {
        val path = mutableListOf<NavNode>()
        when (val view = currentView) {
            is GlossaryView.Hub -> {
                detailTitle = ""
                NavigationPathManager.updatePath("glossary", path)
            }
            is GlossaryView.Category -> {
                detailTitle = ""
                path.add(NavNode("hub", "Глоссарий", 0))
                path.add(NavNode("cat", view.category.title, 1))
                NavigationPathManager.updatePath("glossary", path)
            }
            is GlossaryView.Detail -> {
                // Путь с названием элемента выставляет окно просмотра.
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    PredictiveBackBox(
        enabled = currentView !is GlossaryView.Hub,
        onBack = {
            currentView = when (val view = currentView) {
                is GlossaryView.Detail -> GlossaryView.Category(view.category)
                is GlossaryView.Category -> GlossaryView.Hub
                else -> GlossaryView.Hub
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (val view = currentView) {
                                is GlossaryView.Hub -> "Глоссарий"
                                is GlossaryView.Category -> view.category.title
                                is GlossaryView.Detail -> detailTitle
                            },
                            fontWeight = FontWeight.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Меню") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Crossfade(targetState = currentView, animationSpec = tween(250), label = "GlossaryTransitions") { view ->
                    when (view) {
                        is GlossaryView.Hub -> GlossaryHub(
                            spellbookManager = spellbookManager,
                            repository = repository,
                            onCategoryClick = { currentView = GlossaryView.Category(it) }
                        )
                        is GlossaryView.Category -> GlossaryCategoryList(
                            category = view.category,
                            repository = repository,
                            spellbookManager = spellbookManager,
                            onItemClick = { id -> currentView = GlossaryView.Detail(view.category, id) },
                            onBack = { currentView = GlossaryView.Hub },
                            settingsViewModel = settingsViewModel
                        )
                        is GlossaryView.Detail -> {
                            val type = view.category.glossaryType()
                            if (type != null) {
                                GlossaryDetailWindow(
                                    type = type,
                                    id = view.id,
                                    categoryTitle = view.category.title,
                                    repository = repository,
                                    onTitleChange = { detailTitle = it },
                                    onBack = { currentView = GlossaryView.Category(view.category) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlossaryHub(
    spellbookManager: SpellbookManager,
    repository: GlossaryRepository,
    onCategoryClick: (GlossaryCategory) -> Unit
) {
    val counts = remember {
        GlossaryCategory.entries.associateWith { category ->
            val type = category.glossaryType()
            if (type == null) spellbookManager.loadSpells().size else repository.entries(type).size
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(GlossaryCategory.entries) { category ->
            Surface(
                onClick = { onCategoryClick(category) },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().height(104.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(category.icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Записей: ${counts[category] ?: 0}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun GlossaryCategoryList(
    category: GlossaryCategory,
    repository: GlossaryRepository,
    spellbookManager: SpellbookManager,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    val type = category.glossaryType()
    if (type == null) {
        SpellGlossaryList(spellbookManager, onBack, hazeState, settingsViewModel)
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val entries = remember(category, searchQuery) {
        repository.entries(type).filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Поиск...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { "${it.type.wireName}:${it.id}" }) { entry ->
                    Surface(
                        onClick = { onItemClick(entry.id) },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text(entry.name.ifBlank { entry.id }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (entry.subtitle.isNotBlank()) {
                                Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (entry.source.isNotBlank()) {
                                Text(entry.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpellGlossaryList(
    spellbookManager: ru.quasaris.characternexus.backend.SpellbookManager,
    onBack: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(SpellFilterState()) }
    var showFilters by remember { mutableStateOf(false) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var editingSpell by remember { mutableStateOf<SpellCard?>(null) }

    val allSpells = remember(refreshTrigger) { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState) {
        allSpells.filter { it.matches(filterState, searchQuery) }
    }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(filteredSpells) {
        // No path mirroring for spells as requested
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Поиск...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { filterState = filterState.copy(isCompact = !filterState.isCompact) }) {
                    Icon(
                        if (filterState.isCompact) Icons.Default.ViewHeadline else Icons.Default.ViewModule,
                        contentDescription = "Компактный режим",
                        tint = if (filterState.isCompact) colorScheme.primary else colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        Icons.Default.FilterList,
                        null,
                        tint = if (showFilters) colorScheme.primary else colorScheme.onSurface
                    )
                }
            }

            SpellFiltersArea(
                visible = showFilters,
                filterState = filterState,
                onFilterChange = { filterState = it }
            )

            SpellListGrid(settingsViewModel = settingsViewModel,
                spells = filteredSpells,
                filterState = filterState,
                expandedIds = expandedIds,
                onToggleExpand = { id ->
                    expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                },
                modifier = Modifier.weight(1f),
                hazeState = hazeState,
                isEditable = true,
                onEdit = { editingSpell = it },
                spellOverrides = emptyMap()
            )
        }

        FloatingActionButton(
            onClick = { editingSpell = SpellCard() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить заклинание")
        }
    }

    if (editingSpell != null) {
        SpellEditorWindow(
            spell = editingSpell!!,
            onDismiss = { editingSpell = null },
            onSave = { updated ->
                spellbookManager.addOrUpdateSpell(updated)
                refreshTrigger++
                editingSpell = null
            },
            onDelete = { deleted ->
                spellbookManager.deleteSpell(deleted.id)
                refreshTrigger++
                editingSpell = null
            },
            forceBlurEnabled = false,
            settingsViewModel = settingsViewModel,
            isDesktop = true // Glossary is mostly desktop for now, but SpellEditorWindow handles it
        )
    }
}
