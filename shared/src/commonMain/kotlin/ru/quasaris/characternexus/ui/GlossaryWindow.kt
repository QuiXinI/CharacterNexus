package ru.quasaris.characternexus.ui

import ru.quasaris.characternexus.ui.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okio.Path
import okio.use
import okio.buffer
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.tabs.glossary.*
import ru.quasaris.characternexus.tabs.spells.*
import kotlinx.serialization.json.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.backend.JsonConfig
import ru.quasaris.characternexus.ui.GlossaryCategory

sealed class GlossaryView {
    data object Hub : GlossaryView()
    data class Category(val category: GlossaryCategory) : GlossaryView()
    data class Detail(val category: GlossaryCategory, val file: Path) : GlossaryView()
}

enum class GlossaryCategory(val title: String, val dirName: String, val icon: ImageVector) {
    SPECIES("Виды", "species", Icons.Default.Groups),
    CLASSES("Классы", "classes", Icons.Default.Shield),
    SPELLS("Заклинания", "spells", Icons.Default.AutoFixHigh),
    FEATS("Черты", "feats", Icons.Default.Star)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryWindow(
    spellbookManager: ru.quasaris.characternexus.backend.SpellbookManager,
    moduleManager: ru.quasaris.characternexus.backend.ModuleManager,
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
            }
            is GlossaryView.Category -> {
                detailTitle = ""
                path.add(NavNode("hub", "Глоссарий", 0))
                path.add(NavNode("cat", view.category.title, 1))
            }
            is GlossaryView.Detail -> {
                path.add(NavNode("hub", "Глоссарий", 0))
                path.add(NavNode("cat", view.category.title, 1) { currentView = GlossaryView.Category(view.category) })
                // Item name reported by detail window
            }
        }
        NavigationPathManager.updatePath("glossary", path)
    }

    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    BackHandler(enabled = currentView !is GlossaryView.Hub) {
        currentView = when (val view = currentView) {
            is GlossaryView.Detail -> GlossaryView.Category(view.category)
            is GlossaryView.Category -> GlossaryView.Hub
            else -> GlossaryView.Hub
        }
    }

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
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    if (initialState is GlossaryView.Hub || (initialState is GlossaryView.Category && targetState is GlossaryView.Detail)) {
                        (slideInHorizontally { it } + fadeIn(tween(300))).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally { -it / 2 } + fadeIn(tween(300))).togetherWith(slideOutHorizontally { it } + fadeOut(tween(300)))
                    }.using(SizeTransform(clip = false))
                },
                label = "GlossaryTransitions"
            ) { view ->
                when (view) {
                    is GlossaryView.Hub -> {
                        GlossaryHub(
                            onCategoryClick = { cat ->
                                currentView = GlossaryView.Category(cat)
                            }
                        )
                    }
                    is GlossaryView.Category -> {
                        GlossaryCategoryList(
                            category = view.category,
                            moduleManager = moduleManager,
                            spellbookManager = spellbookManager,
                            onItemClick = { file -> currentView = GlossaryView.Detail(view.category, file) },
                            onBack = { currentView = GlossaryView.Hub },
                            hazeState = if (forceBlurEnabled) null else null, // Placeholder if needed
                            settingsViewModel = settingsViewModel
                        )
                    }
                    is GlossaryView.Detail -> {
                        if (view.category == GlossaryCategory.CLASSES) {
                            ClassDetailWindow(
                                classFile = view.file,
                                moduleManager = moduleManager,
                                spellbookManager = spellbookManager,
                                onTitleChange = { detailTitle = it },
                                onBack = { currentView = GlossaryView.Category(GlossaryCategory.CLASSES) }
                            )
                        } else {
                            GlossaryDetailWindow(
                                file = view.file,
                                category = view.category,
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

@Composable
fun GlossaryHub(onCategoryClick: (GlossaryCategory) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(GlossaryCategory.entries) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .outerShadow(RoundedCornerShape(16.dp), blur = 2.dp)
                    .clickable { onCategoryClick(category) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun GlossaryCategoryList(
    category: GlossaryCategory,
    moduleManager: ru.quasaris.characternexus.backend.ModuleManager,
    spellbookManager: ru.quasaris.characternexus.backend.SpellbookManager,
    onItemClick: (Path) -> Unit,
    onBack: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    if (category == GlossaryCategory.SPELLS) {
        SpellGlossaryList(spellbookManager, onBack, hazeState)
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme
    
    val items = remember(category, searchQuery) {
        val baseDir = getAppDataDir().resolve("glossary/${category.dirName}")
        if (!platformFileSystem.exists(baseDir)) return@remember emptyList<GlossaryListItem>()
        
        platformFileSystem.list(baseDir).filter { it.name.endsWith(".json") }.mapNotNull { file ->
            try {
                val json = platformFileSystem.read(file) { readUtf8() }
                val obj = JsonConfig.json.parseToJsonElement(json).jsonObject
                val name = obj["name"]?.jsonPrimitive?.content ?: file.name.removeSuffix(".json")
                val id = obj["id"]?.jsonPrimitive?.content ?: file.name.removeSuffix(".json")
                
                val subclasses = if (category == GlossaryCategory.CLASSES) {
                    moduleManager.getSubclassesForClass(id).map { it.name ?: "Без названия" }
                } else emptyList()
                
                GlossaryListItem(id, name, file, subclasses)
            } catch (e: Exception) {
                ru.quasaris.characternexus.util.Logger.e("Glossary", "Error loading glossary item from $file", e)
                null
            }
        }.filter { 
            it.name.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.name }
    }
    


    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Поиск...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.file.toString() }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .outerShadow(RoundedCornerShape(16.dp), blur = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceVariant
                        ),
                        onClick = { onItemClick(item.file) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                item.name, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant
                            )
                            if (item.subclasses.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Подклассы: ${item.subclasses.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class GlossaryListItem(val id: String, val name: String, val file: okio.Path, val subclasses: List<String> = emptyList())

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

    val allSpells = remember { spellbookManager.loadSpells() }
    val filteredSpells = remember(allSpells, searchQuery, filterState) {
        allSpells.filter { it.matches(filterState, searchQuery) }
    }

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(filteredSpells) {
        // No path mirroring for spells as requested
    }

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
            hazeState = hazeState
        )
    }
}
