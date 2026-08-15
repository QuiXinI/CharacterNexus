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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import okio.Path
import okio.use
import okio.buffer
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.tabs.glossary.*
import kotlinx.serialization.json.*
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.AppThemeMode
import ru.quasaris.characternexus.Character
import ru.quasaris.characternexus.CharacterSummary
import ru.quasaris.characternexus.DiceRollPosition
import ru.quasaris.characternexus.AdvantageLogic
import ru.quasaris.characternexus.SlotAlignment
import ru.quasaris.characternexus.SlotFillDirection
import ru.quasaris.characternexus.ExportFormat
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

@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun GlossaryWindow(
    spellbookManager: ru.quasaris.characternexus.backend.SpellbookManager,
    moduleManager: ru.quasaris.characternexus.backend.ModuleManager,
    onOpenDrawer: () -> Unit,
    onNavigateToSpells: () -> Unit,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
) {
    var currentView by remember { mutableStateOf<GlossaryView>(GlossaryView.Hub) }
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
                            is GlossaryView.Detail -> "" // Title handled in Detail views
                        }, 
                        fontWeight = FontWeight.Black 
                    ) 
                },
                navigationIcon = {
                    if (currentView is GlossaryView.Hub) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    } else {
                        IconButton(onClick = { 
                            currentView = when (val view = currentView) {
                                is GlossaryView.Detail -> GlossaryView.Category(view.category)
                                is GlossaryView.Category -> GlossaryView.Hub
                                else -> GlossaryView.Hub
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
        modifier = Modifier.run {
            if (forceBlurEnabled && hazeState != null && !isOled) {
                hazeEffect(state = hazeState) {
                    style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                }
            } else this
        }
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
                                if (cat == GlossaryCategory.SPELLS) {
                                    onNavigateToSpells()
                                } else {
                                    currentView = GlossaryView.Category(cat)
                                }
                            }
                        )
                    }
                    is GlossaryView.Category -> {
                        GlossaryCategoryList(
                            category = view.category,
                            moduleManager = moduleManager,
                            onItemClick = { file -> currentView = GlossaryView.Detail(view.category, file) }
                        )
                    }
                    is GlossaryView.Detail -> {
                         if (view.category == GlossaryCategory.CLASSES) {
                            ClassDetailWindow(
                                classFile = view.file,
                                moduleManager = moduleManager,
                                onBack = { currentView = GlossaryView.Category(GlossaryCategory.CLASSES) }
                            )
                        } else {
                            GlossaryDetailWindow(
                                file = view.file,
                                category = view.category,
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
                    .shadow(2.dp, RoundedCornerShape(16.dp))
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
    onItemClick: (Path) -> Unit
) {
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
                            .shadow(1.dp, RoundedCornerShape(16.dp)),
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
